(ns translation-split-review-fixture
  "Seed and remove one production-shaped split-review candidate for live checks.

  This is a verification helper, not an HTTP backdoor. It uses the production
  constructors and Mongo adapters so a reviewer can reach the UI without first
  paying for or depending on a model run. The owning shell scripts give every
  fact a run-specific identity and call `cleanup` from their signal-safe trap."
  (:require [cljs.reader :as reader]
            [clojure.string :as str]
            [knoxx.backend.domain.node.crypto :as crypto]
            [knoxx.backend.domain.node.fs :as fs]
            [knoxx.backend.domain.translation-evidence :as evidence-domain]
            [knoxx.backend.infra.publication-effects :as publication-effects]
            [knoxx.backend.infra.publication-source-revision :as source-revision]
            [knoxx.backend.infra.publication-target-static-site :as static-site]
            [knoxx.backend.infra.stores.mongo-translation-evidence :as evidence-mongo]
            [knoxx.backend.infra.stores.mongo-translation-split :as split-mongo]
            [knoxx.backend.infra.translation-agent-content :as agent-content]
            [knoxx.backend.infra.translation-content-integrity :as integrity]
            [knoxx.backend.infra.translation-evidence-store :as evidence-store]
            [knoxx.backend.infra.translation-split-store :as split-store]
            [knoxx.backend.law.translation-dispatch :as dispatch-law]
            [knoxx.backend.law.translation-source-split :as source-split]
            [knoxx.backend.law.translation-split :as split]
            ["mongodb" :refer [MongoClient]]))

(defn- env
  [name]
  (some-> (aget js/process.env name) str str/trim not-empty))

(defn- required-env
  [name]
  (or (env name)
      (throw (ex-info (str name " is required") {:environment name}))))

(defn- optional-project
  []
  (let [value (env "VERIFY_PROJECT")]
    (when-not (= "__NONE__" value) value)))

(defn- qualified-keyword!
  [label value]
  (let [decoded (keyword value)]
    (when-not (qualified-keyword? decoded)
      (throw (ex-info (str label " must be a qualified keyword wire id")
                      {:value value})))
    decoded))

(defn- locale-keyword!
  [value]
  (let [decoded (keyword value)]
    (when (qualified-keyword? decoded)
      (throw (ex-info "VERIFY_TARGET_LOCALE must be an unqualified locale"
                      {:value value})))
    decoded))

(defn- settings
  []
  {:mongo-uri (or (env "MONGODB_URI")
                  (env "OPENPLANNER_MONGODB_URI")
                  "mongodb://localhost:27017")
   :mongo-db (or (env "MONGODB_DB")
                 (env "OPENPLANNER_MONGODB_DB")
                 "openplanner")
   :run-id (required-env "VERIFY_RUN_ID")
   :org-id (required-env "VERIFY_ORG_ID")
   :project (optional-project)
   :document (qualified-keyword!
              "VERIFY_DOCUMENT_ID" (required-env "VERIFY_DOCUMENT_ID"))
   :garden (qualified-keyword!
            "VERIFY_GARDEN_ID" (required-env "VERIFY_GARDEN_ID"))
   :publication (qualified-keyword!
                 "VERIFY_PUBLICATION_ID"
                 (required-env "VERIFY_PUBLICATION_ID"))
   :publication-path (required-env "VERIFY_PUBLICATION_PATH")
   :document-title (required-env "VERIFY_DOCUMENT_TITLE")
   :target-locale (locale-keyword!
                   (or (env "VERIFY_TARGET_LOCALE") "es"))
   :source-file (required-env "VERIFY_SOURCE_FILE")
   :content-root (required-env "KNOXX_PUBLICATION_CONTENT_ROOT")})

(defn- keyword-wire
  [value]
  (str (namespace value) "/" (name value)))

(defn- dispatch-key
  [opts source-revision]
  (dispatch-law/dispatch-key
   {:org-id (:org-id opts)
    :project (:project opts)
    :garden (keyword-wire (:garden opts))
    :document (:document opts)
    :source-locale :en
    :locale (:target-locale opts)
    :revision source-revision}))

(defn- dispatch-context
  [opts source-revision]
  (cond-> {:dispatch/garden (keyword-wire (:garden opts))
           :dispatch/document-wire-id (keyword-wire (:document opts))
           :dispatch/source-locale :en
           :dispatch/source-digest source-revision
           :dispatch/org-id (:org-id opts)
           :dispatch/membership-id (str "verification-" (:run-id opts))}
    (some? (:project opts))
    (assoc :dispatch/project (:project opts))))

(defn- accepted-dispatch
  [opts source-revision admitted-at]
  (let [record
        (dispatch-law/dispatch-record
         {:document (:document opts)
          :locale (:target-locale opts)
          :revision source-revision
          :replace-stale? false}
         (dispatch-context opts source-revision)
         :dispatch/accepted
         admitted-at
         :attempt-id
         (str "verify-translation-split-review-attempt-" (:run-id opts)))]
    (when-not (= (dispatch-key opts source-revision) (:dispatch/key record))
      (throw (ex-info "verification dispatch does not match resource work" {})))
    record))

(defn- translated-part
  [index]
  (str "Traducci\u00f3n candidata " (inc index)
       " para verificar la revisi\u00f3n durable."
       (if (< index 2) "\n\n" "\n")))

(defn- manifest-for
  [opts source-text parts source-revision]
  (split/split-manifest
   crypto/sha256-hex
   {:org-id (:org-id opts)
    :project (:project opts)
    :garden (:garden opts)
    :document (:document opts)
    :source-locale :en
    :target-locale (:target-locale opts)
    :source-revision source-revision
    :source-text source-text
    :source-parts parts}))

(defn- candidate-evidence
  [manifest output-revision]
  (let [claim (split/candidate-claim crypto/sha256-hex manifest output-revision)
        candidates
        (mapv (fn [index member]
                (split/candidate-split crypto/sha256-hex member
                                       (translated-part index)))
              (range)
              (:candidate-claim/members claim))]
    {:claim claim
     :candidates candidates
     :candidate-set
     (split/complete-candidate-set crypto/sha256-hex manifest claim candidates)}))

(defn- turn-for
  [manifest claim dispatch]
  (let [execution
        (split/execution-snapshot
         crypto/sha256-hex
         {:agent-id "publication_translator"
          :model "verification-fixture"
          :thinking :medium
          :system-prompt "Verification-only production-shaped candidate."
          :tool-ids ["save_translation"]})]
    (split/translation-turn-admission
     crypto/sha256-hex
     {:dispatch-key (:dispatch/key dispatch)
      :run-id (:dispatch/batch-id dispatch)
      :admitted-at (:dispatch/at dispatch)
      :manifest manifest
      :candidate-claim claim
      :execution execution
      :memory (split/memory-snapshot {:status :empty :examples []})})))

(defn- candidate-context
  [opts source-text dispatch]
  (let [parts (source-split/source-parts source-text)
        source-revision (source-revision/content-revision source-text)]
    (when-not (= 3 (count parts))
      (throw (ex-info "the verification source must split into exactly three parts"
                      {:parts (count parts)})))
    (when-not (= source-revision (:dispatch/revision dispatch))
      (throw (ex-info "verification dispatch does not bind the source bytes" {})))
    (let [output-revision (dispatch-law/output-revision dispatch)
          manifest (manifest-for opts source-text parts source-revision)
          {:keys [claim candidates candidate-set]}
          (candidate-evidence manifest output-revision)]
      {:source-revision source-revision
       :output-revision output-revision
       :manifest manifest
       :claim claim
       :candidates candidates
       :candidate-set candidate-set
       :dispatch dispatch
       :turn (turn-for manifest claim dispatch)})))

(defn- split-lineage
  [candidate-set turn]
  {:translation/split-manifest-id (:candidate-set/manifest-id candidate-set)
   :translation/candidate-claim-id (:candidate-set/claim-id candidate-set)
   :translation/candidate-set-id (:candidate-set/id candidate-set)
   :translation/candidate-set-digest (:candidate-set/digest candidate-set)
   :translation/split-count (count (:candidate-set/members candidate-set))
   :translation/split-turn-admitted-at (:translation-turn/admitted-at turn)})

(defn- completion-receipt
  [dispatch candidate-set turn]
  (dispatch-law/translation-receipt
   dispatch
   (dispatch-law/output-revision dispatch)
   (.toISOString (js/Date.))
   (integrity/content-digest (:candidate-set/text candidate-set))
   (split-lineage candidate-set turn)))

(defn- ^:async persist-candidate-context!
  [db {:keys [turn candidates candidate-set]}]
  (let [store (split-mongo/create-store db crypto/sha256-hex)
        turn-id (:translation-turn/id turn)]
    (await (split-mongo/setup-indexes! db))
    (await (split-store/admit-turn! store turn))
    (doseq [candidate candidates]
      (await (split-store/append-candidate-split! store turn-id candidate)))
    (await (split-store/complete-candidate-set! store turn-id candidate-set))))

(defn- ^:async reserve-bound-dispatch!
  [db opts dispatch]
  (let [evidence (evidence-mongo/create-store db)
        _ (await (evidence-mongo/setup-indexes! db))
        reservation (await (evidence-store/reserve-dispatch! evidence dispatch))
        reserved (:record reservation)]
    (when-not (and (= :reserved (:reservation/status reservation))
                   (= dispatch reserved))
      (throw (ex-info "verification dispatch was not freshly reserved"
                      {:reservation reservation})))
    (let [bound (await (evidence-store/bind-dispatch-batch!
                        evidence reserved (:run-id opts)))]
      (when-not bound
        (throw (ex-info "verification dispatch could not bind its batch" {})))
      {:dispatch bound :evidence evidence})))

(defn- ^:async persist-completion!
  [evidence opts {:keys [candidate-set output-revision dispatch turn]}]
  (when-not (await (evidence-store/claim-dispatch-completion! evidence dispatch))
    (throw (ex-info "verification dispatch could not claim completion" {})))
  (await (agent-content/write!
          (:content-root opts) dispatch output-revision
          (:candidate-set/text candidate-set)))
  (await (evidence-store/record-translation!
          evidence (completion-receipt dispatch candidate-set turn)))
  (when-not (await (evidence-store/finish-dispatch-completion!
                    evidence dispatch "verification candidate set completed"))
    (throw (ex-info "verification dispatch could not finish completion" {}))))

(defn- ^:async seed!
  [db opts]
  (let [source-text (await (fs/read-file! (:source-file opts)))
        source-revision (source-revision/content-revision source-text)
        proposed (accepted-dispatch opts source-revision
                                    (.toISOString (js/Date.)))
        {:keys [dispatch evidence]}
        (await (reserve-bound-dispatch! db opts proposed))
        {:keys [turn candidate-set output-revision]
         :as context}
        (candidate-context opts source-text dispatch)]
    (await (persist-candidate-context! db context))
    (await (persist-completion! evidence opts context))
    {:action "seeded"
     :run_id (:run-id opts)
     :document (str (:document opts))
     :candidate_set_id (:candidate-set/id candidate-set)
     :dispatch_attempt_id (:dispatch/attempt-id (:dispatch context))
     :manifest_id (get-in turn [:translation-turn/manifest :split-manifest/id])
     :split_ids (mapv :candidate/split-id (:candidate-set/members candidate-set))
     :source_revision source-revision
     :translation_revision output-revision
     :split_count (count (:candidate-set/members candidate-set))}))

(defn- collection
  [db name]
  (.collection db name))

(defn- ^:async rows!
  [db collection-name query]
  (let [native (await (.toArray (.find (collection db collection-name)
                                      (clj->js query))))]
    (js->clj native :keywordize-keys true)))

(defn- receipt-revisions
  [rows]
  (->> rows
       (keep :receipt_edn)
       (keep (fn [encoded]
               (try
                 (:translation/revision (reader/read-string encoded))
                 (catch :default _ nil))))
       distinct
       vec))

(defn- ^:async remove-content!
  [content-root revisions]
  (doseq [revision revisions]
    (await (fs/unlink! (agent-content/entry-path content-root revision)))))

(defn- ^:async delete-many!
  [db collection-name query]
  (await (.deleteMany (collection db collection-name) (clj->js query))))

(defn- ^:async delete-candidate-children!
  [db turn-ids candidate-set-ids]
  (when (seq candidate-set-ids)
    (await (delete-many! db split-mongo/REVIEW_RECEIPTS_COLLECTION
                         {:candidate_set_id {:$in candidate-set-ids}})))
  (when (seq turn-ids)
    (await (delete-many! db split-mongo/CANDIDATE_SETS_COLLECTION
                         {:turn_id {:$in turn-ids}}))
    (await (delete-many! db split-mongo/CANDIDATE_SPLITS_COLLECTION
                         {:turn_id {:$in turn-ids}}))))

(defn- publication-intent
  [opts]
  {:publication/id (:publication opts)
   :publication/garden (:garden opts)
   :publication/locale (:target-locale opts)
   :publication/path (:publication-path opts)
   :document/title (:document-title opts)})

(defn- publication-target
  [opts]
  (static-site/static-site-target
   {:publication-target/id :open-hax.publication/static-site
    :publication-target/config {:content-root (:content-root opts)}}))

(defn- ^:async materialization!
  [opts]
  (if-let [observed
           (await (publication-effects/observe!
                   (publication-target opts) nil (publication-intent opts)))]
    (let [artifact-path (:route/artifact observed)]
      {:action "materialization"
       :materialized true
       :path (:materialized/path observed)
       :revision (:materialized/revision observed)
       :artifact_path artifact-path
       :content (await (fs/read-file-or-nil!
                        (fs/join (:content-root opts) artifact-path)))})
    {:action "materialization" :materialized false}))

(defn- ^:async remove-materialization!
  [opts]
  (let [target (publication-target opts)
        intent (publication-intent opts)]
    (if-let [observed (await (publication-effects/observe! target nil intent))]
      (do (await (publication-effects/remove! target nil intent observed)) true)
      false)))

(defn- ^:async release-publication-idempotency!
  "Remove this run's deterministic publish reservation, if one was created."
  [opts]
  (when-let [source-text (await (fs/read-file-or-nil! (:source-file opts)))]
    (let [key (publication-effects/publish-idempotency-key
               :open-hax.publication/static-site
               (publication-intent opts)
               (source-revision/content-revision source-text))]
      (publication-effects/release!
       (static-site/static-site-store (:content-root opts)) key)
      true)))

(defn- ^:async cleanup-context!
  [db opts]
  (let [turns (await (rows! db split-mongo/TURNS_COLLECTION
                            {:run_id (:run-id opts)}))
        turn-ids (mapv :turn_id turns)
        candidate-sets (if (seq turn-ids)
                         (await (rows! db split-mongo/CANDIDATE_SETS_COLLECTION
                                       {:turn_id {:$in turn-ids}}))
                         [])
        receipts (await (rows! db evidence-mongo/RECEIPTS_COLLECTION
                              {:document (pr-str (:document opts))}))]
    {:turns turns
     :turn-ids turn-ids
     :candidate-sets candidate-sets
     :candidate-set-ids (mapv :candidate_set_id candidate-sets)
     :revisions (->> (concat (receipt-revisions receipts)
                             (keep :candidate_revision candidate-sets))
                     distinct
                     vec)}))

(defn- ^:async delete-fixture-evidence!
  [db opts {:keys [turn-ids candidate-set-ids]}]
  (await (delete-candidate-children! db turn-ids candidate-set-ids))
  (await (delete-many! db split-mongo/TURNS_COLLECTION
                       {:run_id (:run-id opts)}))
  (await (delete-many! db evidence-mongo/RECEIPTS_COLLECTION
                       {:document (pr-str (:document opts))}))
  (await (delete-many! db evidence-mongo/APPROVALS_COLLECTION
                       {:document (pr-str (:document opts))}))
  ;; Deleting the exact run-namespaced dispatch row also clears a crash-left
  ;; completion_owner without widening cleanup to another attempt or tenant.
  (await (delete-many! db evidence-mongo/DISPATCHES_COLLECTION
                       {:document_wire_id (keyword-wire (:document opts))})))

(defn- ^:async cleanup!
  [db opts]
  (let [{:keys [turns candidate-sets revisions] :as context}
        (await (cleanup-context! db opts))
        materialization-removed? (await (remove-materialization! opts))
        idempotency-removed? (await (release-publication-idempotency! opts))]
    (await (remove-content! (:content-root opts) revisions))
    (await (delete-fixture-evidence! db opts context))
    {:action "cleaned"
     :run_id (:run-id opts)
     :turns (count turns)
     :candidate_sets (count candidate-sets)
     :content_entries (count revisions)
     :materialization_removed materialization-removed?
     :publication_idempotency_removed (boolean idempotency-removed?)}))

(defn- memory-example
  [example]
  {:source_text (:translation-memory/source-text example)
   :target_text (:translation-memory/target-text example)
   :split_id (:translation-memory/split-id example)
   :candidate_set_id (:translation-memory/candidate-set-id example)
   :review_receipt_id (:translation-memory/review-receipt-id example)})

(defn- ^:async fixture-candidate-set!
  [store run-id]
  (when-let [turn (await (split-store/turn-for-run! store run-id))]
    (await (split-store/candidate-set-for-turn!
            store (:translation-turn/id turn)))))

(defn- ^:async current-fixture-candidate-set-ids!
  [evidence opts candidate-set-id]
  (let [current
        (evidence-domain/current-receipts
         (await (evidence-store/completed-translations!
                 evidence {:org-id (:org-id opts) :project (:project opts)})))]
    ;; This is the same attempt-visible allowlist the agent runtime passes to
    ;; memory, narrowed to the verifier's exact run-owned candidate set.
    (loop [remaining (filter #(= candidate-set-id
                                 (:translation/candidate-set-id %)) current)
           allowed #{}]
      (if-let [receipt (first remaining)]
        (let [dispatch (await (evidence-store/dispatch-for-key!
                               evidence (:translation/dispatch-key receipt)))]
          (recur (next remaining)
                 (cond-> allowed
                   (and (:translation/dispatch-attempt-id receipt)
                        (evidence-store/receipt-visible-for-dispatch?
                         receipt dispatch))
                   (conj candidate-set-id))))
        allowed))))

(defn- memory-scope
  [opts current-candidate-set-ids]
  {:org-id (:org-id opts)
   :project (:project opts)
   :garden (:garden opts)
   :source-locale :en
   :target-locale (:target-locale opts)
   :current-candidate-set-ids current-candidate-set-ids
   :limit 50})

(defn- ^:async memory!
  [db opts]
  (let [store (split-mongo/create-store db crypto/sha256-hex)
        evidence (evidence-mongo/create-store db)
        candidate-set (await (fixture-candidate-set! store (:run-id opts)))
        candidate-set-id (:candidate-set/id candidate-set)
        current-candidate-set-ids
        (await (current-fixture-candidate-set-ids!
                evidence opts candidate-set-id))
        examples
        (await (split-store/applicable-memory!
                store (memory-scope opts current-candidate-set-ids)))]
    {:action "memory"
     :current_candidate_set_ids (vec (sort current-candidate-set-ids))
     :examples (mapv memory-example examples)}))

(defn- ^:async execute!
  []
  (let [action (or (first *command-line-args*) "")
        opts (settings)
        client (MongoClient. (:mongo-uri opts)
                             #js {:serverSelectionTimeoutMS 5000})]
    (try
      (await (.connect client))
      (let [db (.db client (:mongo-db opts))]
        (case action
          "seed" (await (seed! db opts))
          "cleanup" (await (cleanup! db opts))
          "memory" (await (memory! db opts))
          "materialization" (await (materialization! opts))
          (throw (ex-info "expected seed, cleanup, memory, or materialization"
                          {:action action}))))
      (finally
        (await (.close client))))))

(defn- ^:async main!
  []
  (try
    (js/console.log (.stringify js/JSON (clj->js (await (execute!)))))
    (catch :default err
      (js/console.error (or (.-stack err) (.-message err) (str err)))
      (set! (.-exitCode js/process) 1))))

(main!)
