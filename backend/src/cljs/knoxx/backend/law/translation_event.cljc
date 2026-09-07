(ns knoxx.backend.law.translation-event
  "Portable construction of durable OpenPlanner events for translation facts.

  Translation receipts and split stores remain semantic authority. These events
  are a searchable, retry-safe projection of an already durable candidate set;
  they can be rebuilt after a failed OpenPlanner write without minting new
  translation evidence."
  (:require [knoxx.backend.law.translation-evidence :as evidence]
            [knoxx.backend.law.translation-split :as split]
            [knoxx.backend.law.translation-split-schema :as schema]))

(def CandidateEvent
  "One review-pending translated split in the OpenPlanner event vocabulary."
  [:map {:closed true}
   [:schema [:= "openplanner.event.v1"]]
   [:id schema/NonBlankString]
   [:ts evidence/Instant]
   [:source [:= "mt"]]
   [:kind [:= "translation.segment"]]
   [:source_ref
    [:map {:closed true}
     [:project {:optional true} schema/NonBlankString]
     [:document_id schema/NonBlankString]
     [:segment_index [:int {:min 0}]]
     [:turn schema/NonBlankString]]]
   [:text schema/NonBlankString]
   [:meta
    [:map {:closed true}
     [:source_lang schema/NonBlankString]
     [:target_lang schema/NonBlankString]
     [:source_text schema/NonBlankString]
     [:mt_model schema/NonBlankString]
     [:status [:= "in_review"]]]]
   [:extra
    [:map {:closed true}
     [:tenant_id schema/NonBlankString]
     [:org_id schema/NonBlankString]
     [:producer [:= "knoxx-contract-agent"]]
     [:garden_id schema/NonBlankString]
     [:document_id schema/NonBlankString]
     ;; OpenPlanner persists arbitrary event data from `extra`, while only a
     ;; small generic subset of `meta` is projected onto event columns. Keep
     ;; the translation query dimensions in both places: `meta` remains the
     ;; canonical envelope vocabulary and `extra` is the durable Mongo shape.
     [:source_lang schema/NonBlankString]
     [:target_lang schema/NonBlankString]
     [:source_text schema/NonBlankString]
     [:mt_model schema/NonBlankString]
     [:status [:= "in_review"]]
     [:source_revision evidence/ConcreteRevision]
     [:candidate_revision evidence/ConcreteRevision]
     [:split_manifest_id schema/NonBlankString]
     [:candidate_claim_id schema/NonBlankString]
     [:candidate_set_id schema/NonBlankString]
     [:candidate_set_digest schema/NonBlankString]
     [:split_id schema/NonBlankString]
     [:candidate_attempt_id schema/NonBlankString]
     [:candidate_digest schema/NonBlankString]
     [:dispatch_key schema/NonBlankString]
     [:translation_content_digest schema/NonBlankString]]]])

(defn- wire-id
  "Encode a qualified resource keyword without losing its namespace."
  [value]
  (if (keyword? value)
    (if-let [ns-part (namespace value)]
      (str ns-part "/" (name value))
      (name value))
    (str value)))

(defn- completion-binding
  [receipt]
  [(:translation/org-id receipt)
   (:translation/project receipt)
   (:translation/garden receipt)
   (:translation/document receipt)
   (:translation/source-locale receipt)
   (:translation/locale receipt)
   (:translation/source-revision receipt)
   (:translation/revision receipt)
   (:translation/dispatch-key receipt)
   (:translation/split-manifest-id receipt)
   (:translation/candidate-claim-id receipt)
   (:translation/candidate-set-id receipt)
   (:translation/candidate-set-digest receipt)
   (:translation/split-count receipt)
   (:translation/split-turn-admitted-at receipt)])

(defn- turn-binding
  [turn candidate-set]
  (let [manifest (:translation-turn/manifest turn)
        claim (:translation-turn/candidate-claim turn)]
    [(:split-manifest/org-id manifest)
     (:split-manifest/project manifest)
     (:split-manifest/garden manifest)
     (:split-manifest/document manifest)
     (:split-manifest/source-locale manifest)
     (:split-manifest/target-locale manifest)
     (:split-manifest/source-revision manifest)
     (:candidate-set/revision candidate-set)
     (:translation-turn/dispatch-key turn)
     (:split-manifest/id manifest)
     (:candidate-claim/id claim)
     (:candidate-set/id candidate-set)
     (:candidate-set/digest candidate-set)
     (count (:candidate-set/members candidate-set))
     (:translation-turn/admitted-at turn)]))

(defn- assert-completion-binding!
  [receipt turn candidate-set]
  (when-not (= (completion-binding receipt) (turn-binding turn candidate-set))
    (throw (ex-info "translation candidate event does not match its completed receipt"
                    {:translation-event/receipt (completion-binding receipt)
                     :translation-event/candidate-set (turn-binding turn candidate-set)}))))

(defn- event-id
  [digest-hex candidate-set candidate]
  (str "translation-segment-"
       (digest-hex
        (pr-str [(:candidate-set/id candidate-set)
                 (:candidate/attempt-id candidate)
                 (:candidate/digest candidate)]))))

(defn- source-ref
  [turn candidate document-id]
  (let [project (get-in turn [:translation-turn/manifest
                              :split-manifest/project])]
    (cond-> {:document_id document-id
             :segment_index (:candidate/split-index candidate)
             :turn (:translation-turn/id turn)}
      (some? project) (assoc :project project))))

(defn- translation-meta
  [turn source-split]
  (let [manifest (:translation-turn/manifest turn)
        execution (:translation-turn/execution turn)]
    {:source_lang (name (:split-manifest/source-locale manifest))
     :target_lang (name (:split-manifest/target-locale manifest))
     :source_text (:split/source-text source-split)
     :mt_model (:translation-execution/model execution)
     :status "in_review"}))

(defn- event-extra
  [receipt turn candidate-set candidate translation-metadata]
  (let [manifest (:translation-turn/manifest turn)]
    (merge translation-metadata
           {:tenant_id (:split-manifest/org-id manifest)
            :org_id (:split-manifest/org-id manifest)
            :producer "knoxx-contract-agent"
            :garden_id (wire-id (:split-manifest/garden manifest))
            :document_id (wire-id (:split-manifest/document manifest))
            :source_revision (:split-manifest/source-revision manifest)
            :candidate_revision (:candidate-set/revision candidate-set)
            :split_manifest_id (:split-manifest/id manifest)
            :candidate_claim_id (:candidate-set/claim-id candidate-set)
            :candidate_set_id (:candidate-set/id candidate-set)
            :candidate_set_digest (:candidate-set/digest candidate-set)
            :split_id (:candidate/split-id candidate)
            :candidate_attempt_id (:candidate/attempt-id candidate)
            :candidate_digest (:candidate/digest candidate)
            :dispatch_key (:translation-turn/dispatch-key turn)
            :translation_content_digest (:translation/content-digest receipt)})))

(defn- candidate-event
  [digest-hex receipt turn candidate-set source-split candidate]
  (let [manifest (:translation-turn/manifest turn)
        translation-metadata (translation-meta turn source-split)
        document-id (wire-id (:split-manifest/document manifest))]
    (schema/assert-valid!
     :translation-event/candidate
     CandidateEvent
     {:schema "openplanner.event.v1"
      :id (event-id digest-hex candidate-set candidate)
      :ts (:translation/at receipt)
      :source "mt"
      :kind "translation.segment"
      :source_ref (source-ref turn candidate document-id)
      :text (:candidate/text candidate)
      :meta translation-metadata
      :extra (event-extra receipt turn candidate-set candidate
                          translation-metadata)})))

(defn candidate-events
  "Project one authenticated completed candidate set into stable split events.

  The event timestamp comes from the immutable receipt and each id comes only
  from immutable candidate lineage. Equal completion retries therefore produce
  byte-equal envelopes and can safely repair a missing OpenPlanner projection."
  [digest-hex receipt turn candidate-set]
  (let [checked-receipt (evidence/assert-receipt! receipt)
        checked-turn (split/assert-turn-integrity! digest-hex turn)
        manifest (:translation-turn/manifest checked-turn)
        checked-set (split/assert-candidate-set-integrity!
                     digest-hex manifest candidate-set)]
    (when-not (:translation/content-digest checked-receipt)
      (throw (ex-info "translation candidate events require content-bound evidence"
                      {:translation/revision (:translation/revision checked-receipt)})))
    (assert-completion-binding! checked-receipt checked-turn checked-set)
    (mapv #(candidate-event digest-hex checked-receipt checked-turn checked-set %1 %2)
          (:split-manifest/splits manifest)
          (:candidate-set/members checked-set))))
