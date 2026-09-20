(ns knoxx.backend.source-acceptance-test
  (:require [clio.extern.js.fs :as fs]
            [cljs.test :as test]
            [knoxx.backend.extern.clio-store-fixture :as disk]
            [knoxx.backend.extern.source-authoring :as files]
            [knoxx.backend.infra.clio-source-authoring-store :as sources]
            [knoxx.backend.infra.clio-source-review-store :as reviews]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.source-authoring :as authoring]
            [knoxx.backend.infra.source-review :as review]
            [knoxx.backend.infra.source-review-store :as review-store]))

(def ^:private scope {:org-id "org-a" :project "wiki"})
(def ^:private actor {:id "principal-a" :kind :human})

(defn- ^:async refused [operation]
  (try (await (operation)) nil (catch :default error (ex-data error))))

(defn- ^:async accept! [config dependencies document]
  (let [document-scope (assoc scope :document (:document document))
        command {:operation-id "submit" :action :submit :expected-head nil
                 :revision (:revision document) :source-locale (:source-locale document)}]
    (await (review/command! config document-scope actor command dependencies))
    (await (review/command! config document-scope actor
                           (assoc command :operation-id "accept" :action :accept :expected-head "submit") dependencies))))

(defn- ^:async fixture! [f]
  (let [root (disk/temp-directory!)
        contracts (str root "/contracts")
        config {:contracts-dir contracts}
        dependencies {:source-provider (sources/open! {:directory (str root "/source")})
                      :provider (reviews/open! {:directory (str root "/review")})
                      :now! (constantly "2026-09-12T12:00:00Z")}
        create {:title "Acceptance snapshot" :content "Exact source bytes." :source-locale :en
                :garden :test/garden :target-locales [:es]}]
    (try
      (fs/ensure-dir! contracts)
      (await (files/write-text! contracts "namespaces/garden.edn"
                "{:namespace :test :resources [{:garden/id :garden :garden/title \"Garden\" :garden/status :active :garden/locales [:en :es]}]}"))
      (let [first-doc (:review (await (authoring/create! config scope actor (assoc create :operation-id "one") dependencies)))
            second-doc (:review (await (authoring/create! config scope actor (assoc create :operation-id "two") dependencies)))]
        (await (accept! config dependencies first-doc))
        (await (files/write-text! contracts "namespaces/missing.edn"
                  "{:namespace :docs :resources [{:document/id :missing :document/title \"Unrelated missing source\" :document/source-locale :en :document/org-id \"org-a\" :document/visibility :private :document/source {:path \"missing.md\"}}]}"))
        (let [index (publications/publication-index (await (publications/resource-records! config)))
              selected (assoc index :documents (select-keys (:documents index) (map :document [first-doc second-doc])))]
          (test/is (= 3 (count (:documents index))))
          (await (f config dependencies selected index first-doc second-doc))))
      (finally (fs/remove-tree! root)))))

(test/deftest ^:async selected-source-acceptance-loads-one-resource-snapshot
  (await (fixture!
    (^:async fn [config dependencies selected _index first-doc second-doc]
      (let [loads (atom 0) indexes (atom 0)
            load-records! publications/resource-records!
            build-index publications/publication-index]
        (with-redefs [publications/resource-records! (^:async fn [config]
                                                     (swap! loads inc)
                                                     (await (load-records! config)))
                      publications/publication-index (fn [records] (swap! indexes inc) (build-index records))]
          (let [accepted? (:source-accepted? (await (review/acceptance-facts! config selected scope dependencies)))]
            (test/is (= 1 @loads) "Referenced documents share one actual resource load")
            (test/is (= 1 @indexes) "The canonical resource index is built once")
            (test/is (true? (accepted? {:publication/document (:document first-doc)} (:revision first-doc))))
            (test/is (false? (accepted? {:publication/document (:document second-doc)} (:revision second-doc))))
            (test/is (false? (accepted? {:publication/document (:document first-doc)} "other-revision")))
            (test/is (false? (accepted? {:publication/document :docs/missing} (:revision first-doc))))
            (test/is (= 1 @loads) "The returned predicate stays synchronous and performs no resource I/O"))))))))

(test/deftest ^:async selected-source-and-provider-failures-still-refuse-acceptance-facts
  (await (fixture!
    (^:async fn [config dependencies selected index first-doc _second-doc]
      (let [missing-only (assoc index :documents (select-keys (:documents index) [:docs/missing]))
            first-only (assoc selected :documents (select-keys (:documents selected) [(:document first-doc)]))]
        (test/is (= "source_content_not_found"
                    (:code (await (refused #(review/acceptance-facts! config missing-only scope dependencies))))))
        (with-redefs [publications/resource-records! (fn [_] (throw (ex-info "Resource unavailable" {:status 503 :code "test_resource_failure"})))]
          (test/is (= "test_resource_failure"
                      (:code (await (refused #(review/acceptance-facts! config first-only scope dependencies)))))))
        (with-redefs [review-store/read-source-review-events! (fn [_ _] (throw (ex-info "Review unavailable" {:status 503 :code "test_review_failure"})))]
          (test/is (= "test_review_failure"
                      (:code (await (refused #(review/acceptance-facts! config first-only scope dependencies)))))))
        (with-redefs [files/write-text! (fn [_ _ _] (throw (ex-info "Disk full" {:status 503 :code "test_disk_full"})))]
          (test/is (= "test_disk_full"
                      (:code (await (refused #(authoring/save! config (assoc scope :document (:document first-doc)) actor
                                                {:operation-id "pending-save" :expected-revision (:revision first-doc)
                                                 :content "Durable bytes await projection."} dependencies)))))))
        (test/is (= "source_projection_repair_required"
                    (:code (await (refused #(review/acceptance-facts! config first-only scope dependencies)))))))))))
