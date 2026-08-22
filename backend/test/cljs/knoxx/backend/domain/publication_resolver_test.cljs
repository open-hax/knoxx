(ns knoxx.backend.domain.publication-resolver-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [malli.core :as m]
            [knoxx.backend.domain.publication-resolver :as resolver]
            [knoxx.backend.law.publication :as law]))

;; ── Fixtures ───────────────────────────────────────────────────────────────
;;
;; Two shapes of the same topology. `local-*` mirrors a namespace manifest,
;; whose entries carry a bare id plus the manifest's :namespace. `qualified-*`
;; mirrors the same manifest written with fully-qualified ids. Both must
;; project identically.

(def local-document
  {:namespace :knoxx.docs
   :document/id :translation-pipeline
   :document/title "Translation Pipeline"
   :document/source-locale :en
   :document/source {:path "docs/translation-pipeline.md"}})

(def qualified-document
  (assoc local-document :document/id :knoxx.docs/translation-pipeline))

(def local-garden
  {:namespace :knoxx.docs
   :garden/id :promethean
   :garden/title "Promethean"
   :garden/status :active
   :garden/locales [:en :es]})

(def qualified-garden
  (assoc local-garden :garden/id :knoxx.docs/promethean))

(def local-intent
  {:namespace :knoxx.docs
   :publication/id :translation-pipeline-es
   :publication/document :translation-pipeline
   :publication/garden :promethean
   :publication/locale :es
   :publication/revision :source/current
   :publication/state :published
   :publication/path "/translation-pipeline"
   :translation/review :required})

(def qualified-intent
  (assoc local-intent
         :publication/id :knoxx.docs/translation-pipeline-es
         :publication/document :knoxx.docs/translation-pipeline
         :publication/garden :knoxx.docs/promethean))

(def local-resources [local-document local-garden local-intent])
(def qualified-resources [qualified-document qualified-garden qualified-intent])

(defn- all-keys
  "Every map key appearing anywhere in a nested structure."
  [value]
  (let [found (atom #{})]
    (walk/postwalk
     (fn [node]
       (when (map? node) (swap! found into (keys node)))
       node)
     value)
    @found))

;; ── 1 canonicalization ─────────────────────────────────────────────────────

(deftest index-canonicalizes-namespace-local-refs
  (is (= (resolver/publication-index local-resources)
         (resolver/publication-index qualified-resources))
      "a manifest written with local ids and one written with qualified ids
       project to the same canonical index")
  (testing "canonical-id is idempotent on already-qualified ids"
    (is (= :knoxx.docs/translation-pipeline
           (resolver/canonical-id :knoxx.docs :translation-pipeline)
           (resolver/canonical-id :knoxx.docs :knoxx.docs/translation-pipeline)))
    (testing "a qualified id keeps its own namespace over the manifest's"
      (is (= :gardens/promethean
             (resolver/canonical-id :knoxx.docs :gardens/promethean))))
    (testing "no namespace in scope leaves a bare id alone rather than nil-qualifying it"
      (is (= :translation-pipeline
             (resolver/canonical-id nil :translation-pipeline))))))

;; ── 2/3 canonical identity inside payloads ────────────────────────────────

(deftest indexed-payload-identity-is-canonical
  (let [index (resolver/publication-index local-resources)
        document (get-in index [:documents :knoxx.docs/translation-pipeline])
        garden (get-in index [:gardens :knoxx.docs/promethean])]
    (testing "the payload carries the same canonical id used as its index key"
      (is (= :knoxx.docs/translation-pipeline (:document/id document)))
      (is (= :knoxx.docs/promethean (:garden/id garden))))
    (testing "so the stored payload satisfies the qualified law shape"
      (is (true? (m/validate law/Document document)))
      (is (true? (m/validate law/Garden garden))))))

(deftest hydration-of-namespace-local-reference-validates
  (let [index (resolver/publication-index local-resources)
        [intent] (resolver/desired-publications index :knoxx.docs/translation-pipeline)]
    (is (= :en (:document/source-locale intent)))
    (is (true? (m/validate law/PublicationIntent intent)))))

;; ── 4 enumeration order ────────────────────────────────────────────────────

(deftest projection-is-enumeration-order-independent
  (is (= (resolver/publication-index local-resources)
         (resolver/publication-index (reverse local-resources))
         (resolver/publication-index (shuffle local-resources)))))

;; ── 5/6 duplicate canonical ids ────────────────────────────────────────────

(deftest byte-equivalent-duplicate-ids-collapse
  (let [index (resolver/publication-index [local-document qualified-document
                                           local-garden local-intent])]
    (is (= 1 (count (:documents index))))
    (is (= :knoxx.docs/translation-pipeline
           (:document/id (get-in index [:documents :knoxx.docs/translation-pipeline]))))))

(deftest conflicting-duplicate-ids-fail-deterministically
  (let [other-title (assoc local-document :document/title "Something Else")
        resources [local-document other-title local-garden]
        capture (fn [ordered]
                  (try
                    (resolver/publication-index ordered)
                    ::no-throw
                    (catch :default err (ex-data err))))
        forward (capture resources)
        backward (capture (reverse resources))]
    (is (not= ::no-throw forward))
    (testing "the same conflict yields identical evidence in both orders"
      (is (= forward backward))
      (is (= :documents (:resource/kind forward)))
      (is (= :knoxx.docs/translation-pipeline (:resource/id forward)))
      (is (= 2 (count (:conflicting-payloads forward)))))))

;; ── 7/8/9 relation identity ────────────────────────────────────────────────

(deftest publication-relation-key-includes-revision
  (let [pinned (assoc local-intent
                      :publication/id :translation-pipeline-es-pinned
                      :publication/revision "abc123")
        index (resolver/publication-index [local-document local-garden
                                           local-intent pinned])]
    (is (= 2 (count (:publications index))))
    (is (= #{:source/current "abc123"}
           (set (map :publication/revision (:publications index)))))))

(deftest duplicate-active-relation-fails
  (let [twin (assoc local-intent :publication/id :translation-pipeline-es-twin)]
    (is (thrown-with-msg?
         js/Error #"conflicting publication intents"
         (resolver/publication-index [local-document local-garden
                                      local-intent twin])))))

(deftest archived-intent-does-not-conflict-with-replacement
  (let [archived (assoc local-intent
                        :publication/id :translation-pipeline-es-archived
                        :publication/state :archived)
        index (resolver/publication-index [local-document local-garden
                                           local-intent archived])]
    (testing "the archived intent coexists rather than conflicting"
      (is (= 2 (count (:publications index)))))
    (testing "and is still present as history"
      (is (= #{:published :archived}
             (set (map :publication/state (:publications index))))))
    (testing "while only the active one counts as a live target"
      (is (= [:es] (resolver/target-locales index))))))

;; ── 10/11 purity ───────────────────────────────────────────────────────────

;; ── Reference blockers (Codex P1 on #230) ─────────────────────────────────

(deftest dangling-document-reference-is-a-blocker-not-an-omission
  (testing "an intent pointing at a missing document used to vanish silently,
            because list-document-views iterates the documents it has"
    (let [orphan (assoc local-intent
                        :publication/id :orphan
                        :publication/document :missing)
          err (try (resolver/publication-index [local-garden orphan])
                   nil
                   (catch :default e e))]
      (is (some? err))
      (is (= [{:publication/id :knoxx.docs/orphan
               :blocker :unresolved-document
               :reference :knoxx.docs/missing}]
             (:blockers (ex-data err)))))))

(deftest dangling-garden-reference-is-a-blocker
  (testing "hydration validates only the document, so a missing garden passed through"
    (let [orphan (assoc local-intent
                        :publication/id :orphan
                        :publication/garden :missing)
          err (try (resolver/publication-index [local-document orphan])
                   nil
                   (catch :default e e))]
      (is (some? err))
      (is (= [{:publication/id :knoxx.docs/orphan
               :blocker :unresolved-garden
               :reference :knoxx.docs/missing}]
             (:blockers (ex-data err)))))))

(deftest reference-blockers-are-order-independent
  (let [orphan (assoc local-intent :publication/id :orphan
                      :publication/document :missing-doc
                      :publication/garden :missing-garden)
        blockers (fn [ordered]
                   (:blockers (ex-data (try (resolver/publication-index ordered)
                                            nil
                                            (catch :default e e)))))]
    (is (= 2 (count (blockers [orphan]))))
    (is (= (blockers [orphan]) (blockers (reverse [orphan]))))
    (testing "both failure modes are named, not just the first"
      (is (= #{:unresolved-document :unresolved-garden}
             (set (map :blocker (blockers [orphan]))))))))

;; ── 10/11 purity ───────────────────────────────────────────────────────────

(deftest runtime-keys-attached-to-a-projected-payload-are-stripped
  (testing "Malli maps are open, so a schema alone cannot keep execution facts
            out of the projection — only selecting declared fields can"
    (let [dirty-document (assoc local-document
                                :receipt/published-at "2026-08-13T00:00:00Z"
                                :worker/state :succeeded
                                :publish_job_id "job-42")
          dirty-garden (assoc local-garden :receipt/synced-at "2026-08-13")
          dirty-intent (assoc local-intent :publication/published-at "2026-08-13")
          index (resolver/publication-index [dirty-document dirty-garden dirty-intent])
          view (resolver/list-document-views index)
          keys-present (all-keys view)]
      (testing "the payload still validates"
        (is (true? (m/validate law/PublicationListView view))))
      (doseq [leaked [:receipt/published-at :worker/state :publish_job_id
                      :receipt/synced-at :publication/published-at]]
        (is (not (contains? keys-present leaked))
            (str leaked " leaked into the projection")))
      (testing "and the declared fields all survived"
        (is (= :knoxx.docs/translation-pipeline
               (get-in view [:documents 0 :document :document/id])))
        (is (= :active (get-in view [:gardens 0 :garden/status])))))))

(deftest projection-excludes-runtime-state
  (let [receipt {:receipt/id :knoxx.docs/translation-pipeline-es-receipt
                 :receipt/published-at "2026-08-13T00:00:00Z"
                 :receipt/worker-state :succeeded}
        index (resolver/publication-index (conj local-resources receipt))
        view (resolver/list-document-views index)
        key-namespaces (into #{} (keep namespace) (all-keys view))]
    (is (not (contains? key-namespaces "receipt")))
    (is (empty? (filter #(str/includes? (str %) "published-at") (all-keys view))))
    (is (empty? (filter #(str/includes? (str %) "worker") (all-keys view))))))

(deftest projection-has-no-openplanner-dependency
  (let [legacy-marker (str "open" "planner")
        legacy-resource {(keyword legacy-marker "garden-id") "garden-a"
                         (keyword legacy-marker "sync") true}
        with-legacy (resolver/publication-index (conj local-resources legacy-resource))
        without-legacy (resolver/publication-index local-resources)]
    (is (= (resolver/list-document-views without-legacy)
           (resolver/list-document-views with-legacy))
        "deleting every legacy-backend resource leaves the desired topology
         unchanged")
    (testing "and the projection never mentions the legacy backend"
      (is (not (str/includes? (str/lower-case (pr-str (resolver/list-document-views with-legacy)))
                              legacy-marker))))))

;; ── 12 facade shapes ───────────────────────────────────────────────────────

(deftest document-view-and-list-view-shapes
  (let [index (resolver/publication-index local-resources)
        document-view (resolver/document-view index :knoxx.docs/translation-pipeline)
        list-view (resolver/list-document-views index)]
    (is (true? (m/validate law/PublicationDocumentView document-view)))
    (is (true? (m/validate law/PublicationListView list-view)))
    (testing "the list view is not double-wrapped"
      (is (= #{:documents :gardens} (set (keys list-view))))
      (is (= #{:document :publications} (set (keys (first (:documents list-view))))))
      (is (= :knoxx.docs/translation-pipeline
             (get-in list-view [:documents 0 :document :document/id]))))
    (testing "a document view whose document lost its canonical id is rejected"
      (let [broken (assoc-in index [:documents :knoxx.docs/translation-pipeline
                                    :document/id]
                             :translation-pipeline)]
        (is (thrown? js/Error
                     (resolver/document-view broken :knoxx.docs/translation-pipeline)))))
    (testing "an unknown document is an explicit failure, not an empty view"
      (is (thrown-with-msg? js/Error #"unknown document"
                            (resolver/document-view index :knoxx.docs/nope))))))

(deftest queries-expose-locales-and-revisions
  (let [pinned (assoc local-intent
                      :publication/id :translation-pipeline-fr
                      :publication/locale :fr
                      :publication/revision "abc123")
        index (resolver/publication-index [local-document local-garden
                                           local-intent pinned])]
    (is (= [:es :fr] (resolver/target-locales index)))
    (is (= ["abc123" :source/current] (resolver/intended-revisions index)))))

;; ── canonical publication identity (CodeRabbit on #230) ────────────────────

(def duplicate-id-intent
  "The same canonical publication id claiming a *different* relation. Only the
   locale differs, so the relation key differs too and the duplicate-relation
   check cannot see the collision."
  (assoc qualified-intent :publication/locale :fr))

(deftest one-publication-id-cannot-stand-for-two-relations
  (testing "the relation check keys on document x garden x locale x revision, so
            two intents sharing one id across different relations slip past it"
    (is (empty? (resolver/publication-conflicts [qualified-intent duplicate-id-intent]))))
  (testing "the identity check is what refuses them"
    (is (thrown-with-msg?
         js/Error #"conflicting canonical resource identity"
         (resolver/publication-index
          [qualified-document qualified-garden qualified-intent duplicate-id-intent]))))
  (testing "a byte-equal duplicate collapses rather than conflicting, exactly as
            it does for documents and gardens"
    (is (empty? (resolver/publication-identity-conflicts
                 [qualified-intent qualified-intent]))))
  (testing "the conflict names the id and carries both payloads"
    (let [[conflict] (resolver/publication-identity-conflicts
                      [qualified-intent duplicate-id-intent])]
      (is (= :knoxx.docs/translation-pipeline-es (:resource/id conflict)))
      (is (= :publications (:resource/kind conflict)))
      (is (= 2 (count (:conflicting-payloads conflict))))))
  (testing "and the pair is ordered independently of the order it arrived in"
    (is (= (resolver/publication-identity-conflicts [qualified-intent duplicate-id-intent])
           (resolver/publication-identity-conflicts [duplicate-id-intent qualified-intent])))))
