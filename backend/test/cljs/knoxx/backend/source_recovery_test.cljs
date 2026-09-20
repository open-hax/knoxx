(ns knoxx.backend.source-recovery-test
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :as test]
            [knoxx.backend.domain.source-authoring :as authoring-domain]
            [knoxx.backend.domain.source-review :as domain]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.extern.source-authoring :as files]
            [knoxx.backend.infra.clio-application-store :as clio]
            [knoxx.backend.infra.clio-source-authoring-store :as sources]
            [knoxx.backend.infra.clio-source-review-store :as reviews]
            [knoxx.backend.infra.publication-source-revision :as revisions]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.source-authoring :as authoring]
            [knoxx.backend.infra.source-authoring-store :as source-store]
            [knoxx.backend.infra.source-review :as review]
            [knoxx.backend.infra.source-review-store :as review-store]))

(def ^:private scope {:org-id "org-a" :project "wiki"})
(def ^:private actor {:id "principal-a" :kind :human})
(def ^:private creation {:operation-id "create-one" :title "Source recovery" :content "Original source." :source-locale :en
               :garden :test/garden :target-locales [:es]})
(defn- ^:async refused [f]
  (try (await (f)) nil (catch :default error (ex-data error))))
(defn- ^:async fixture! [f]
  (let [root (disk/temp-directory!) contracts (str root "/contracts")]
    (try
      (fs/ensure-dir! contracts)
      (await (files/write-text! contracts "namespaces/garden.edn"
                                "{:namespace :test :resources [{:garden/id :garden :garden/title \"Garden\" :garden/status :active :garden/locales [:en :es]}]}"))
      (let [options {:source {:directory (str root "/source")} :review {:directory (str root "/review")}}
            dependencies {:source-provider (sources/open! (:source options)) :provider (reviews/open! (:review options))
                          :now! (constantly "2026-09-12T12:00:00.000Z")}]
        (await (f {:contracts-dir contracts} dependencies options)))
      (catch :default error (test/is false (str "Unexpected recovered source workflow error: " error)))
      (finally (fs/remove-tree! root)))))
(defn- command [id action current]
  {:operation-id id :revision (:revision current) :source-locale (:source-locale current)
   :expected-head (:head current) :action action})

(test/deftest ^:async document-sequencing-survives-a-failed-predecessor
  (let [key (str (random-uuid))
        first-gate (disk/deferred)
        second-gate (disk/deferred)
        started (atom [])
        first-task (files/with-document-lock! key #(do (swap! started conj :first) (:promise first-gate)))
        first-outcome (refused (fn [] first-task))
        second-task (files/with-document-lock! key #(do (swap! started conj :second) (:promise second-gate)))]
    (test/is (= [:first] @started))
    ((:reject! first-gate) (ex-info "Rejected source operation" {:status 409}))
    (test/is (= 409 (:status (await first-outcome))))
    (await (disk/drain!))
    (test/is (= [:first :second] @started))
    (let [third-task (files/with-document-lock! key #(do (swap! started conj :third) :third-result))]
      (test/is (= [:first :second] @started) "An old cleanup cannot remove the still-pending successor")
      ((:resolve! second-gate) :second-result)
      (test/is (= :second-result (await second-task)))
      (test/is (= :third-result (await third-task)))
      (test/is (= [:first :second :third] @started)))))

(defn- ^:async accept! [config document dependencies]
  (let [current (await (review/read! config document dependencies))
        submitted (await (review/command! config document actor (command "submit-one" :submit current) dependencies))]
    (await (review/command! config document actor (command "accept-one" :accept (:review submitted)) dependencies))))

(test/deftest ^:async real-source-and-review-ledgers-recover-exact-revision-acceptance
  (await (fixture!
    (^:async fn [config dependencies options]
      (let [created (await (authoring/create! config scope actor creation dependencies))
            document (assoc scope :document (get-in created [:review :document]))
            accepted (await (accept! config document dependencies))
            restarted (assoc dependencies :source-provider (sources/open! (:source options)) :provider (reviews/open! (:review options)))
            revision (get-in accepted [:review :revision])]
        (test/is (true? (get-in accepted [:review :accepted])))
        (test/is (true? (:accepted (await (review/read! config document restarted)))))
        (let [saved (await (authoring/save! config document actor {:operation-id "save-one" :expected-revision revision :content "Revised source."} restarted))]
          (test/is (false? (get-in saved [:review :accepted])))
          (test/is (true? (get-in saved [:review :stale])))
          (test/is (= 2 (count (get-in saved [:review :history]))))
          (test/is (= "source_review_stale_revision"
                 (:code (await (refused #(review/command! config document actor (command "stale-accept" :accept (:review accepted)) restarted))))))
          (test/is (= "Revised source." (get-in (await (authoring/create! config scope actor creation restarted)) [:review :content])))))))))

(test/deftest ^:async pending-projection-blocks-old-acceptance-and-exact-save-retry-repairs
  (await (fixture!
    (^:async fn [config dependencies _options]
      (let [created (await (authoring/create! config scope actor creation dependencies))
            document (assoc scope :document (get-in created [:review :document]))
            accepted (await (accept! config document dependencies))
            save {:operation-id "repair-one" :expected-revision (get-in accepted [:review :revision]) :content "Durable revised content."}]
        (with-redefs [files/write-text! (fn [_ _ _] (throw (ex-info "disk full" {:status 503 :code "test_disk_full"})))]
          (test/is (= "test_disk_full" (:code (await (refused #(authoring/save! config document actor save dependencies)))))))
        (test/is (= "source_projection_repair_required" (:code (await (refused #(review/read! config document dependencies))))))
        (let [repaired (await (authoring/save! config document actor save dependencies))]
          (test/is (true? (:existing? repaired)))
          (test/is (= "Durable revised content." (get-in repaired [:review :content])))
          (test/is (false? (get-in repaired [:review :accepted]))))
        (test/is (= "source_authoring_operation_conflict"
               (:code (await (refused #(authoring/save! config document actor (assoc save :content "Conflicting bytes") dependencies)))))))))))

(test/deftest ^:async review-provider-rejects-stale-head-and-retains-first-retry-fact
  (await (fixture!
    (^:async fn [_config dependencies options]
      (let [document-scope (assoc scope :document :docs/one)
            operation {:operation-id "one" :revision "sha256-one" :source-locale :en :expected-head nil :action :submit}
            event (domain/event-for-command document-scope actor operation "2026-09-12T12:00:00Z")
            provider (:provider dependencies)]
        (test/is (false? (:existing? (await (review-store/admit-source-review! provider document-scope nil event)))))
        (test/is (true? (:existing? (await (review-store/admit-source-review! (reviews/open! (:review options)) document-scope nil event)))))
        (test/is (= "source_review_stale_head"
               (:code (await (refused #(review-store/admit-source-review! provider document-scope nil
                                              (assoc event :review/id "two" :review/action :accept))))))))))))

(test/deftest ^:async dispatch-context-reads-only-its-canonical-source-review-identity
  (await (fixture!
    (^:async fn [config dependencies options]
      (let [created (await (authoring/create! config scope actor creation dependencies))
            document-scope (assoc scope :document (get-in created [:review :document]))
            accepted (await (accept! config document-scope dependencies))
            restarted (assoc dependencies :source-provider (sources/open! (:source options))
                                          :provider (reviews/open! (:review options)))
            index (:index (await (review/observed-source! config document-scope)))
            intent {:publication/document (:document document-scope)}
            revision (get-in accepted [:review :revision])]
        (doseq [[acting-context expected]
                [[(assoc scope :membership-id "member-a" :document :unrelated/selection) true]
                 [(assoc scope :membership-id "member-b") true]
                 [(assoc scope :membership-id "member-a" :project "other-project") false]
                 [(dissoc (assoc scope :membership-id "member-a") :project) false]]]
          (let [attempt (try {:facts (await (review/acceptance-facts! config index acting-context restarted))}
                             (catch :default error {:error (str error)}))
                source-accepted? (get-in attempt [:facts :source-accepted?])]
            (test/is (nil? (:error attempt)))
            (test/is (fn? source-accepted?))
            (when (fn? source-accepted?)
              (test/is (= expected (source-accepted? intent revision)))
              (test/is (false? (source-accepted? intent "changed-source-revision")))))))))))

(test/deftest ^:async corrected-source-retains-reviewed-lessons-after-both-ledgers-reopen
  (await (fixture!
    (^:async fn [config dependencies options]
      (let [created (await (authoring/create! config scope actor creation dependencies))
            document (assoc scope :document (get-in created [:review :document]))
            submitted (await (review/command! config document actor
                                (command "submit-original" :submit (:review created)) dependencies))
            requested (await (review/command! config document actor
                                (assoc (command "request-revision" :request-changes (:review submitted))
                                       :notes "State who accepts the page."
                                       :lessons ["Humans make final acceptance decisions."]) dependencies))
            saved (await (authoring/save! config document actor
                             {:operation-id "save-correction" :expected-revision (get-in requested [:review :revision])
                              :content "Humans make final acceptance decisions."} dependencies))
            accepted (await (accept! config document dependencies))
            restarted (assoc dependencies :source-provider (sources/open! (:source options))
                                          :provider (reviews/open! (:review options)))
            recovered (await (review/read! config document restarted))
            expected [{:text "Humans make final acceptance decisions." :review "request-revision"
                       :revision (get-in created [:review :revision]) :actor actor
                       :acceptance-review "accept-one" :acceptance-revision (get-in saved [:review :revision])}]]
        (test/is (empty? (get-in requested [:review :lessons])))
        (test/is (empty? (get-in saved [:review :lessons])))
        (test/is (true? (get-in accepted [:review :accepted])))
        (test/is (= expected (get-in accepted [:review :lessons])))
        (test/is (= expected (:lessons recovered)))
        (test/is (= (:review accepted) recovered)))))))

(test/deftest ^:async equal-document-records-use-last-provenance-for-read-and-save
  (let [root (disk/temp-directory!)
        roots [(str root "/first") (str root "/second")]
        document {:document/id :docs/provenance :document/title "Same metadata"
                  :document/source-locale :en :document/org-id (:org-id scope)
                  :document/visibility :private :document/source {:path "source.md"}}
        document-scope (assoc scope :document (:document/id document))
        config {:contracts-dir (str (first roots) "/contracts")
                :generated-contracts-dir (str (second roots) "/contracts")}
        records (mapv (fn [source-root]
                        {:ok? true :resource/kind :document :resource/definition document
                         :resource/file-path (str source-root "/contracts/namespaces/document.edn")}) roots)]
    (try
      (doseq [source-root roots] (fs/ensure-dir! (str source-root "/contracts")))
      (doseq [ordered-records [records (vec (reverse records))]]
        (doseq [[source-root content] (map vector roots ["First root bytes." "Second root bytes."])]
          (await (files/write-text! source-root "source.md" content)))
        (let [winner (peek ordered-records)
              winning-root (revisions/resource-source-root config (:resource/file-path winner))
              losing-root (first (remove #{winning-root} roots))
              expected-content (await (files/read-text! (str winning-root "/source.md")))
              untouched-content (await (files/read-text! (str losing-root "/source.md")))
              dependencies {:source-provider (source-store/memory-store)
                            :provider (review-store/memory-store)
                            :now! (constantly "2026-09-12T12:00:00Z")}]
          (with-redefs [publications/resource-records! (fn [_] ordered-records)]
            (let [observed (await (review/observed-source! config document-scope))
                  current (await (review/read! config document-scope dependencies))]
              (test/is (= document (:document observed)) "Equal resource payloads remain lawful duplicates")
              (test/is (= winner (:record observed)))
              (test/is (= winning-root (:root observed)))
              (test/is (= expected-content (:content current)))
              (test/is (= (revisions/content-revision expected-content) (:revision current)))
              (let [saved (await (authoring/save! config document-scope actor
                                  {:operation-id "save-provenance" :expected-revision (:revision current)
                                   :content "Saved into the winning checkout."} dependencies))]
                (test/is (= "Saved into the winning checkout." (get-in saved [:review :content])))
                (test/is (= "Saved into the winning checkout."
                            (await (files/read-text! (str winning-root "/source.md")))))
                (test/is (= untouched-content (await (files/read-text! (str losing-root "/source.md"))))))))))
      (finally (fs/remove-tree! root)))))

(test/deftest ^:async creation-manifest-survives-projection-failure-and-missing-manifest-is-never-durable
  (await (fixture!
    (^:async fn [config dependencies options]
      (let [document-scope (assoc scope :document (:document (authoring-domain/creation-identity scope creation)))]
        (with-redefs [files/write-text! (fn [_ _ _] (throw (ex-info "disk full" {:status 503 :code "test_disk_full"})))]
          (test/is (= "test_disk_full"
                      (:code (await (refused #(authoring/create! config scope actor creation dependencies)))))))
        (let [events (await (source-store/source-events! (:source-provider dependencies) document-scope))
              event (first events)
              manifest (:source/manifest event)
              restarted (assoc dependencies :source-provider (sources/open! (:source options))
                                            :provider (reviews/open! (:review options)))
              repaired (await (authoring/create! config scope actor creation restarted))
              empty-options {:directory (str (get-in options [:source :directory]) "-refused")}
              empty-provider (sources/open! empty-options)]
          (test/is (= 1 (count events)))
          (test/is (= 3 (count (:resources manifest))) "The document and both publication intents are durable")
          (test/is (= (:source/document event) (first (:resources manifest))))
          (test/is (true? (:existing? repaired)))
          (test/is (= event (:event repaired)))
          (test/is (= "Original source." (get-in repaired [:review :content])))
          (doseq [malformed [(dissoc event :source/manifest)
                             (assoc event :source/manifest {})
                             (assoc event :source/manifest (dissoc manifest :resources))
                             (assoc-in event [:source/manifest :resources 1 :publication/document] :docs/unrelated)]]
            (test/is (= 400 (:status (await (refused #(source-store/admit-source! empty-provider document-scope nil malformed))))))
            (test/is (empty? (clio/history (:ledger empty-provider)))))
          (test/is (empty? (clio/history (:ledger empty-provider))))
          (test/is (empty? (await (source-store/source-events! (sources/open! empty-options) document-scope))))))))))
