(ns knoxx.backend.infra.routes.translation-dispatch-test
  "The facade's routing of one worker status report.

  The report is the only thing the worker sends, and it arrives in several
  shapes for several reasons. Which of them resolves a binding, which records a
  failure, and which does neither is decided here — so it is tested here."
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.contracts.loader :as contract-loader]
            [knoxx.backend.infra.publication-contract-content :as contract-content]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.infra.routes.translation-dispatch :as facade]
            [knoxx.backend.infra.translation-agent-content :as agent-content]
            [knoxx.backend.infra.translation-content-integrity :as content-integrity]
            [knoxx.backend.infra.routes.publications :as publications]
            [knoxx.backend.infra.translation-evidence-store :as store]
            [knoxx.backend.law.translation-dispatch :as law]))

(def ^:private at "2026-08-22T09:00:00.000Z")

(def ^:private evidence-scope
  "The tenant and project this fixture dispatches under."
  {:org-id "org-1" :project nil})

(def ^:private work
  {:document :knoxx.docs/probe
   :locale :es
   :revision "sha256-aaa111bbb222"
   :replace-stale? false})

(def ^:private context
  {:dispatch/garden "knoxx.docs/promethean"
   :dispatch/document-wire-id "knoxx.docs/probe"
   :dispatch/source-locale :en
   :dispatch/org-id "org-1"
   :dispatch/membership-id "member-1"
   :dispatch/source-digest "sha256-aaa111bbb222"})

(deftest publication-selection-does-not-fan-out-to-a-whole-document
  (let [document {:document/id :knoxx.docs/probe
                  :document/title "Probe"
                  :document/source-locale :en
                  :document/source {:path "docs/probe.md"}}
        intent (fn [publication garden]
                 {:publication/id publication
                  :publication/document :knoxx.docs/probe
                  :publication/garden garden
                  :publication/locale :es
                  :publication/revision :source/current
                  :publication/state :published
                  :publication/path "/probe"
                  :translation/review :required})
        index {:documents {:knoxx.docs/probe document}
               :publications [(intent :knoxx.docs/probe-es-a :knoxx.docs/garden-a)
                              (intent :knoxx.docs/probe-es-b :knoxx.docs/garden-b)]}
        selected (facade/selected-hydrated-intents
                  index {:publication :knoxx.docs/probe-es-b})]
    (is (= [:knoxx.docs/probe-es-b] (mapv :publication/id selected)))
    (is (= 2 (count (facade/selected-hydrated-intents
                     index {:document :knoxx.docs/probe})))
        "the older document command intentionally remains the broader action")))

(deftest invalid-facade-selections-are-refused-before-dispatch
  (let [document {:document/id :knoxx.docs/probe
                  :document/title "Probe"
                  :document/source-locale :en
                  :document/source {:path "docs/probe.md"}}
        intent {:publication/id :knoxx.publications/probe-es
                :publication/document :knoxx.docs/probe
                :publication/garden :knoxx.docs/promethean
                :publication/locale :es
                :publication/revision :source/current
                :publication/state :published
                :publication/path "/probe"
                :translation/review :required}
        index {:documents {:knoxx.docs/probe document}
               :publications [intent]}]
    (testing "unknown keys cannot silently become a corpus sweep"
      (is (thrown? js/Error
                   (facade/selected-hydrated-intents index {:locale :es}))))

    (testing "a present selector must name a qualified identity"
      (is (thrown? js/Error
                   (facade/selected-hydrated-intents index {:document nil})))
      (is (thrown? js/Error
                   (facade/selected-hydrated-intents index {:publication nil}))))

    (testing "document and publication are alternatives, not precedence rules"
      (is (thrown? js/Error
                   (facade/selected-hydrated-intents
                    index
                    {:document :knoxx.docs/probe
                     :publication :knoxx.publications/probe-es}))))

    (testing "an exact publication miss is a typed not-found error"
      (let [error (try
                    (facade/selected-hydrated-intents
                     index {:publication :knoxx.publications/missing})
                    nil
                    (catch :default err err))]
        (is (some? error))
        (is (= 404 (:status (ex-data error))))
        (is (= "translation_publication_not_found" (:code (ex-data error))))))))

(deftest ^:async exact-publication-dispatch-emits-exactly-one-agent-event
  (let [document {:document/id :knoxx.docs/probe
                  :document/title "Probe"
                  :document/source-locale :en
                  :document/source {:path "docs/probe.md"}}
        garden (fn [id]
                 {:garden/id id
                  :garden/title (name id)
                  :garden/status :active
                  :garden/locales [:en :es]})
        intent (fn [publication garden-id]
                 {:publication/id publication
                  :publication/document :knoxx.docs/probe
                  :publication/garden garden-id
                  :publication/locale :es
                  :publication/revision :source/current
                  :publication/state :published
                  :publication/path "/probe"
                  :translation/review :required})
        garden-a :knoxx.gardens/a
        garden-b :knoxx.gardens/b
        publication-a :knoxx.publications/probe-es-a
        publication-b :knoxx.publications/probe-es-b
        resource-record (fn [kind definition]
                          {:ok? true
                           :resource/kind kind
                           :resource/file-path (str "/contracts/" (name kind) ".edn")
                           :resource/definition definition})
        records [(resource-record :document document)
                 (resource-record :garden (garden garden-a))
                 (resource-record :garden (garden garden-b))
                 (resource-record :publication (intent publication-a garden-a))
                 (resource-record :publication (intent publication-b garden-b))]
        source-revision-value "sha256-aaa111bbb222"
        emitted (atom [])
        evidence-store (store/memory-store)
        deps {:evidence-store evidence-store
              :clock (constantly at)
              :digest-hex #(str "digest-" (hash %))
              :emit! (fn [event]
                       (swap! emitted conj event)
                       (js/Promise.resolve
                        {:matchedTriggers [:publication/translation-needed]}))}
        scope {:org-id "org-1"
               :membership-id "member-1"
               :project "knoxx-session"}]
    (with-redefs [publications/resource-records!
                  (fn [_] (js/Promise.resolve records))
                  source-revision/source-revisions!
                  (fn [_ _ _]
                    (js/Promise.resolve
                     {:knoxx.docs/probe source-revision-value}))
                  contract-content/source-content!
                  (fn [_ _] (js/Promise.resolve "Translate exactly this source."))]
      (let [result (await (facade/dispatch-translations!
                           {:translation-runner "agent"}
                           deps
                           scope
                           {:publication publication-b}))
            event (first @emitted)]
        (testing "the facade narrows before it derives and enqueues work"
          (is (= 1 (:considered result)))
          (is (= 1 (:admissible result)))
          (is (= [publication-b]
                 (mapv :publication/id (:dispatched result))))
          (is (= 1 (count @emitted))
              "one selected card must create one downstream agent emission"))

        (testing "the single event belongs to the exact selected relation"
          (is (= "knoxx.gardens/b"
                 (get-in event
                         [:event/payload :resource-policies :garden_id])))
          (is (not= "knoxx.gardens/a"
                    (get-in event
                            [:event/payload :resource-policies :garden_id]))))))))

(deftest ^:async a-completed-claim-with-lost-candidate-bytes-is-reopened
  (let [document {:document/id :knoxx.docs/probe
                  :document/title "Probe"
                  :document/source-locale :en
                  :document/source {:path "docs/probe.md"}}
        garden {:garden/id :knoxx.gardens/promethean
                :garden/title "Promethean"
                :garden/status :active
                :garden/locales [:en :es]}
        intent {:publication/id :knoxx.publications/probe-es
                :publication/document :knoxx.docs/probe
                :publication/garden :knoxx.gardens/promethean
                :publication/locale :es
                :publication/revision :source/current
                :publication/state :published
                :publication/path "/probe"
                :translation/review :required}
        resource-record (fn [kind definition]
                          {:ok? true
                           :resource/kind kind
                           :resource/file-path (str "/contracts/" (name kind) ".edn")
                           :resource/definition definition})
        records [(resource-record :document document)
                 (resource-record :garden garden)
                 (resource-record :publication intent)]
        revision "sha256-aaa111bbb222"
        target "Texto que ya no está en el almacén reproducible."
        evidence-store (store/memory-store)
        old-context {:dispatch/garden "knoxx.gardens/promethean"
                     :dispatch/document-wire-id "knoxx.docs/probe"
                     :dispatch/source-locale :en
                     :dispatch/org-id "org-1"
                     :dispatch/project "knoxx-session"
                     :dispatch/membership-id "member-1"
                     :dispatch/source-digest revision}
        old-record (law/dispatch-record
                    {:document :knoxx.docs/probe
                     :locale :es
                     :revision revision
                     :replace-stale? false}
                    old-context :dispatch/accepted at)
        emitted (atom [])
        deps {:evidence-store evidence-store
              :clock (constantly "2026-08-30T12:00:00.000Z")
              :digest-hex #(str "digest-" (hash %))
              :emit! (fn [event]
                       (swap! emitted conj event)
                       (js/Promise.resolve
                        {:matchedTriggers [:publication/translation-needed]}))}
        scope {:org-id "org-1"
               :membership-id "member-1"
               :project "knoxx-session"}]
    (await (store/reserve-dispatch! evidence-store old-record))
    (await (store/bind-dispatch-batch! evidence-store
                                       (:dispatch/key old-record) "old-run"))
    (await (store/resolve-dispatch! evidence-store
                                    (:dispatch/key old-record)
                                    :dispatch/completed nil))
    (await (store/record-translation!
            evidence-store
            {:receipt/type :translation/completed
             :translation/document :knoxx.docs/probe
             :translation/garden :knoxx.gardens/promethean
             :translation/source-locale :en
             :translation/locale :es
             :translation/source-revision revision
             :translation/revision "candidate-lost"
             :translation/content-digest
             (content-integrity/content-digest target)
             :translation/dispatch-key (:dispatch/key old-record)
             :translation/org-id "org-1"
             :translation/project "knoxx-session"
             :translation/at "2026-08-30T10:00:00.000Z"}))
    (with-redefs [publications/resource-records!
                  (fn [_] (js/Promise.resolve records))
                  source-revision/source-revisions!
                  (fn [_ _ _]
                    (js/Promise.resolve {:knoxx.docs/probe revision}))
                  contract-content/source-content!
                  (fn [_ _] (js/Promise.resolve "Translate this source."))
                  agent-content/content-for-receipt!
                  (fn [_ _] (js/Promise.resolve nil))]
      (let [result (await (facade/dispatch-translations!
                           {:translation-runner "agent"
                            :publication-content-root "/published"}
                           deps scope {:publication :knoxx.publications/probe-es}))
            current (await (store/dispatch-for-key!
                            evidence-store (:dispatch/key old-record)))]
        (testing "unavailable bytes derive actionable work and a fresh run"
          (is (= 1 (count @emitted)))
          (is (= :dispatch/accepted
                 (:dispatch/outcome (first (:dispatched result)))))
          (is (= :dispatch/accepted (:dispatch/outcome current)))
          (is (= :candidate-unavailable (:dispatch/recovery-reason current)))
          (is (not= "old-run" (:dispatch/batch-id current))))))))

(defn- ^:async seeded-store!
  "A store holding one in-flight claim bound to `batch-1`."
  []
  (let [evidence-store (store/memory-store)
        record (law/dispatch-record work context :dispatch/accepted at)]
    (await (store/reserve-dispatch! evidence-store record))
    (await (store/bind-dispatch-batch! evidence-store (:dispatch/key record) "batch-1"))
    {:evidence-store evidence-store
     :clock (constantly at)
     ;; Agrees with the dispatched revision, so completion is not refused for
     ;; source drift. The drift path has its own test.
     :observe-source-revision (constantly (js/Promise.resolve
                                          (:dispatch/source-digest context)))}))

(deftest failed-document-id-reads-both-shapes-the-worker-can-send
  (testing "a bare id string"
    (is (= "knoxx.docs/probe" (law/failed-document-id "knoxx.docs/probe")))
    (is (= "knoxx.docs/probe" (law/failed-document-id "  knoxx.docs/probe  "))))

  (testing "a map naming one, under any of the spellings in use"
    (is (= "a/b" (law/failed-document-id {:document_id "a/b"})))
    (is (= "a/b" (law/failed-document-id {:document_wire_id "a/b"})))
    (is (= "a/b" (law/failed-document-id {:document "a/b"})))
    (is (= "a/b" (law/failed-document-id {:id "a/b"}))))

  (testing "the most specific spelling wins when several are present"
    (is (= "specific" (law/failed-document-id {:document_id "specific"
                                               :document "general"}))))

  (testing "nothing is guessed from a shape that names no id"
    ;; Treating a nested object as an id would produce a lookup that can never
    ;; match, which reads as 'no such dispatch' rather than as a bad report.
    (is (nil? (law/failed-document-id nil)))
    (is (nil? (law/failed-document-id {})))
    (is (nil? (law/failed-document-id {:document {:nested "thing"}})))
    (is (nil? (law/failed-document-id "   ")))))

(deftest ^:async a-completed-document-resolves-its-binding
  (let [deps (await (seeded-store!))
        result (await (facade/resolve-batch-status!
                       deps {:status "partial"
                             :batch_id "batch-1"
                             :completed_document "knoxx.docs/probe"}))]
    (testing "a receipt is minted"
      (is (some? (:translation/receipt result)))
      (is (= "sha256-aaa111bbb222"
             (:translation/source-revision (:translation/receipt result)))))))

(deftest ^:async a-failed-document-is-recorded-against-its-binding
  (let [deps (await (seeded-store!))
        result (await (facade/resolve-batch-status!
                       deps {:status "partial"
                             :batch_id "batch-1"
                             :failed_document {:document_id "knoxx.docs/probe"}
                             :error "model unavailable"}))]
    (testing "the attempt is failed, carrying the worker's reason"
      (is (= :dispatch/failed (:dispatch/outcome result)))
      (is (= "model unavailable" (:dispatch/detail (:dispatch/record result)))))

    (testing "a failed attempt is not a translation"
      (is (empty? (await (store/completed-translations! (:evidence-store deps) evidence-scope)))))))

(deftest ^:async a-report-naming-no-document-is-reported-not-dropped
  (let [deps (await (seeded-store!))]
    (testing "a batch merely going processing resolves nothing, and says so"
      ;; Silence here is the difference between 'nothing to resolve' and
      ;; 'silently ignored', and an operator debugging a missing translation
      ;; needs to be able to tell them apart.
      (is (= :no-document-named
             (:reason (:translation/skipped
                       (await (facade/resolve-batch-status!
                               deps {:status "processing" :batch_id "batch-1"})))))))

    (testing "a completion with no document named resolves nothing"
      (is (= :no-document-named
             (:reason (:translation/skipped
                       (await (facade/resolve-batch-status!
                               deps {:status "complete" :batch_id "batch-1"})))))))

    (testing "nothing was recorded either way"
      (is (empty? (await (store/completed-translations! (:evidence-store deps) evidence-scope)))))))

(deftest ^:async a-report-naming-no-batch-is-refused-before-any-store-lookup
  ;; The regression: every branch of `resolve-batch-status!` joins on the batch
  ;; id, and a nil id is not a value the stores decline to match — it is the
  ;; value they match *unbound* records by. `dispatch-for-batch!` filters on
  ;; `(= batch-id (:dispatch/batch-id record))`, so a batch-level failure report
  ;; arriving without an id selected the first claim whose send never bound one
  ;; and marked it failed. That claim belongs to a different document entirely.
  (let [evidence-store (store/memory-store)
        unbound (law/dispatch-record {:document :knoxx.docs/other
                                      :locale :fr
                                      :revision "sha256-ccc333ddd444"
                                      :replace-stale? false}
                                     (assoc context
                                            :dispatch/document-wire-id "knoxx.docs/other")
                                     :dispatch/accepted
                                     at)
        deps {:evidence-store evidence-store
              :clock (constantly at)
              :observe-source-revision (constantly (js/Promise.resolve
                                                    (:dispatch/source-digest context)))}]
    (await (store/reserve-dispatch! evidence-store unbound))

    (testing "a batch-level failure with no batch id is refused, not applied"
      (is (= :batch-id-missing
             (:refusal/type (:translation/refusal
                             (await (facade/resolve-batch-status!
                                     deps {:status "failed"
                                           :error "worker died"})))))))

    (testing "a blank batch id is the same missing binding, not a blank one"
      (is (= :batch-id-missing
             (:refusal/type (:translation/refusal
                             (await (facade/resolve-batch-status!
                                     deps {:status "failed" :batch_id "   "})))))))

    (testing "a completion with no batch id is refused before the join"
      (is (= :batch-id-missing
             (:refusal/type (:translation/refusal
                             (await (facade/resolve-batch-status!
                                     deps {:status "processing"
                                           :completed_document "knoxx.docs/other"})))))))

    (testing "the unrelated unbound claim is untouched"
      ;; This is the whole point. Before the guard it read :dispatch/failed and
      ;; the next pass re-dispatched translation work that had never been sent.
      (is (= :dispatch/accepted
             (:dispatch/outcome (await (store/dispatch-for-key!
                                        evidence-store
                                        (:dispatch/key unbound)))))))))

(deftest documents-are-mapped-to-the-checkout-that-actually-declared-them
  ;; `document-source-roots` is the other half of the multi-root fix: the
  ;; per-document root only helps if something derives it from each record's own
  ;; provenance. A composite entry registering both a document and a publication
  ;; must project onto the document facet first, or its `:document/id` is gone
  ;; by the time the root is attached and the mapping is silently empty.
  (let [document-record
        (fn [root id]
          {:ok? true
           :resource/kind :document
           :resource/file-path (str root "/contracts/publications/" (name id) ".edn")
           :resource/definition {:document/id id
                                 :document/title "Probe"
                                 :document/source-locale :en
                                 :document/source {:path "docs/probe.md"}}})
        roots ["/srv/a/contracts" "/srv/b/contracts"]
        records [(document-record "/srv/a" :knoxx.docs/from-a)
                 (document-record "/srv/b" :knoxx.docs/from-b)
                 ;; A record that failed to load carries no usable provenance.
                 (assoc (document-record "/srv/b" :knoxx.docs/broken) :ok? false)
                 ;; A different kind is not a document and must not appear.
                 (assoc (document-record "/srv/a" :knoxx.docs/garden)
                        :resource/kind :garden)]]
    (with-redefs [contract-loader/contract-root-paths (constantly roots)]
      (let [mapped (facade/document-source-roots {} records)]
        (testing "each document points at its own checkout"
          (is (= "/srv/a" (get mapped :knoxx.docs/from-a)))
          (is (= "/srv/b" (get mapped :knoxx.docs/from-b))))

        (testing "records that cannot supply provenance are simply absent"
          ;; Absent, not mapped to a guess: a document with no entry falls back
          ;; to the legacy first root, which is the prior behavior.
          (is (not (contains? mapped :knoxx.docs/broken)))
          (is (not (contains? mapped :knoxx.docs/garden))))

        (testing "a record living outside every known root maps to nothing"
          (is (empty? (facade/document-source-roots
                       {} [(document-record "/elsewhere" :knoxx.docs/stray)]))))))))

(deftest tenant-receipts-do-not-leak-across-organizations
  ;; Translation is tenant-scoped: the worker keys segments by organization, so a
  ;; translation produced for org A does not exist for org B. Loading receipts
  ;; unfiltered made org B's gate report a document translated when the segments
  ;; lived only in org A's tenant.
  (let [receipt (fn [org]
                  {:receipt/type :translation/completed
                   :translation/document :knoxx.docs/probe
                   :translation/garden :knoxx.docs/promethean
                   :translation/source-locale :en
                   :translation/locale :es
                   :translation/source-revision "sha256-aaa111bbb222"
                   :translation/revision "sha256-aaa111bbb222+es@batch-1"
                   :translation/dispatch-key "key-1"
                   :translation/org-id org
                   :translation/at at})
        all [(receipt "org-a") (receipt "org-b")]]
    (testing "each tenant sees only its own evidence"
      (is (= ["org-a"] (mapv :translation/org-id (facade/tenant-receipts "org-a" all))))
      (is (= ["org-b"] (mapv :translation/org-id (facade/tenant-receipts "org-b" all)))))

    (testing "a tenant with no evidence sees none"
      (is (empty? (facade/tenant-receipts "org-c" all))))

    (testing "an unscoped receipt is not treated as global"
      ;; Admitting it into every tenant is the failure being fixed.
      (is (empty? (facade/tenant-receipts "org-a" [(dissoc (receipt "org-a")
                                                           :translation/org-id)]))))))

(deftest ^:async the-workers-real-report-vocabulary-is-accepted
  ;; Read from ingestion/src/kms_ingestion/translation/worker.clj. An earlier
  ;; version gated success on "complete"/"partial", which the worker never sends
  ;; for a per-document success — so the whole completion path was dead code.
  (let [deps (await (seeded-store!))
        result (await (facade/resolve-batch-status!
                       deps {:status "processing"
                             :batch_id "batch-1"
                             :completed_document "knoxx.docs/probe"}))]
    (testing "a per-document success arrives as status \"processing\""
      (is (some? (:translation/receipt result)))
      (is (= "sha256-aaa111bbb222"
             (:translation/source-revision (:translation/receipt result)))))))

(deftest ^:async a-batch-level-failure-naming-no-document-is-resolved
  ;; The worker's terminal failure report sends status "failed" and an error,
  ;; naming no document at all. Falling through to :no-document-named left the
  ;; claim in flight forever, so it could never be retried.
  (let [deps (await (seeded-store!))
        result (await (facade/resolve-batch-status!
                       deps {:status "failed"
                             :batch_id "batch-1"
                             :error "All documents failed"}))]
    (testing "the binding is resolved by batch id and marked failed"
      (is (= :dispatch/failed (:dispatch/outcome result)))
      (is (= "All documents failed" (:dispatch/detail (:dispatch/record result)))))))

(deftest structurally-inadmissible-intents-are-not-dispatched
  ;; `domain.publication-gate` states it decides only the evidential half of
  ;; admissibility and assumes the structural half holds upstream. Nothing
  ;; upstream of dispatch checked it, so an intent targeting an archived garden
  ;; or an unaccepted locale derived work and was enqueued on a shared worker for
  ;; content that can never be published.
  (let [intent {:publication/id :knoxx.docs/probe-es
                :publication/document :knoxx.docs/probe
                :publication/garden :knoxx.docs/garden
                :publication/locale :es
                :publication/revision "sha256-aaa111bbb222"
                :publication/state :published
                :publication/path "/probe"
                :translation/review :none
                :document/source-locale :en}
        index (fn [{:keys [status locales]}]
                {:documents {:knoxx.docs/probe {:document/id :knoxx.docs/probe}}
                 :gardens {:knoxx.docs/garden {:garden/id :knoxx.docs/garden
                                               :garden/status status
                                               :garden/locales locales}}})]
    (testing "an active garden accepting the locale is admissible"
      (is (= [intent] (facade/admissible-intents
                       (index {:status :active :locales [:es]}) [intent]))))

    (testing "an archived garden is not"
      (is (empty? (facade/admissible-intents
                   (index {:status :archived :locales [:es]}) [intent]))))

    (testing "a locale the garden does not accept is not"
      (is (empty? (facade/admissible-intents
                   (index {:status :active :locales [:fr]}) [intent]))))

    (testing "an archived intent is not, even in an active garden"
      (is (empty? (facade/admissible-intents
                   (index {:status :active :locales [:es]})
                   [(assoc intent :publication/state :archived)]))))

    (testing "a dangling garden reference is not"
      (is (empty? (facade/admissible-intents
                   {:documents {:knoxx.docs/probe {}} :gardens {}} [intent]))))))

(deftest a-changed-source-locale-invalidates-existing-evidence
  ;; The gate's key is [document target-locale revision] and cannot carry a
  ;; source locale. So changing a document's declared source locale while its
  ;; bytes stay identical left digest, document and target locale all unchanged —
  ;; and the old receipt satisfied the new intent, suppressing a retranslation
  ;; that was genuinely required.
  (let [receipt (fn [source-locale]
                  {:receipt/type :translation/completed
                   :translation/document :knoxx.docs/probe
                   :translation/garden :knoxx.docs/promethean
                   :translation/source-locale source-locale
                   :translation/locale :es
                   :translation/source-revision "sha256-aaa111bbb222"
                   :translation/revision "sha256-aaa111bbb222+es@batch-1"
                   :translation/dispatch-key "key-1"
                   :translation/org-id "org-1"
                   :translation/at at})
        documents [{:document/id :knoxx.docs/probe :document/source-locale :de}]]
    (testing "a receipt from the document's current source locale still counts"
      (is (= 1 (count (facade/current-source-locale-receipts
                       [{:document/id :knoxx.docs/probe :document/source-locale :en}]
                       [(receipt :en)])))))

    (testing "a receipt from the previous source locale no longer counts"
      (is (empty? (facade/current-source-locale-receipts documents [(receipt :en)]))))

    (testing "a receipt for a document not in scope is dropped rather than kept"
      ;; Nothing declares its current source locale, so nothing can vouch for it.
      (is (empty? (facade/current-source-locale-receipts [] [(receipt :en)]))))))
