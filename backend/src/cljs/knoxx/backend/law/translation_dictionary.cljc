(ns knoxx.backend.law.translation-dictionary
  "Read-only dictionary projection over effective approved translation memory.

  This is an exact phrase dictionary, not model-inferred terminology. Its source
  is the existing review law: corrected text wins, and pending, rejected, stale,
  cross-scope candidate sets are absent. The embedded memory example preserves
  the complete immutable lineage behind every pair."
  (:require [knoxx.backend.law.publication-locale :as locale]
            [knoxx.backend.law.translation-split :as split]
            [knoxx.backend.law.translation-split-schema :as schema]))

(def DictionaryScope
  "Exact authority required to select a current approved phrase dictionary."
  [:and
   [:map {:closed true}
    [:org-id schema/NonBlankString]
    [:project {:optional true} [:maybe schema/NonBlankString]]
    [:garden :qualified-keyword]
    [:source-locale locale/Locale]
    [:target-locale locale/Locale]
    [:exclude-manifest-id {:optional true} [:maybe schema/NonBlankString]]
    [:current-candidate-set-ids [:set schema/NonBlankString]]
    [:limit {:optional true} [:int {:min 1}]]]
   [:fn {:error/message "dictionary source and target locales must differ"}
    #(not= (:source-locale %) (:target-locale %))]])

(def DictionaryEntry
  "One approved exact source/target pair and the evidence that authorizes it."
  [:and
   [:map {:closed true}
    [:translation-dictionary/id schema/NonBlankString]
    [:translation-dictionary/source-text schema/NonBlankString]
    [:translation-dictionary/target-text schema/NonBlankString]
    [:translation-dictionary/evidence schema/TranslationMemoryExample]]
   [:fn {:error/message "dictionary display bytes must equal their evidence"}
    (fn [entry]
      (let [evidence (:translation-dictionary/evidence entry)]
        (= [(:translation-dictionary/id entry)
            (:translation-dictionary/source-text entry)
            (:translation-dictionary/target-text entry)]
           [(:translation-memory/id evidence)
            (:translation-memory/source-text evidence)
            (:translation-memory/target-text evidence)])))]] )

(def DictionaryProjection
  "A bounded current dictionary plus the authority used to select it."
  [:map {:closed true}
   [:translation-dictionary/scope DictionaryScope]
   [:translation-dictionary/entries [:vector DictionaryEntry]]])

(defn checked-scope
  "Validate and default one exact dictionary selector."
  [scope]
  (schema/assert-valid! :translation-dictionary/scope
                        DictionaryScope
                        (assoc scope :limit (or (:limit scope) 100))))

(defn memory-example->entry
  "Expose one already-approved memory example as a dictionary entry."
  [example]
  (let [checked (schema/assert-valid! :translation-dictionary/memory-example
                                      schema/TranslationMemoryExample example)]
    (schema/assert-valid!
     :translation-dictionary/entry
     DictionaryEntry
     {:translation-dictionary/id (:translation-memory/id checked)
      :translation-dictionary/source-text (:translation-memory/source-text checked)
      :translation-dictionary/target-text (:translation-memory/target-text checked)
      :translation-dictionary/evidence checked})))

(defn approved-entry
  "Derive one entry directly from complete review history, or nil unless current
  effective review is approved. This is the pure entry point for callers that
  hold canonical split facts rather than the split-store memory projection."
  [digest-hex manifest candidate-set split-id review-history]
  (some-> (split/approved-memory-example
           digest-hex manifest candidate-set split-id review-history)
          memory-example->entry))

(defn- same-scope?
  [scope example]
  (and (= (:org-id scope) (:translation-memory/org-id example))
       (= (:project scope) (:translation-memory/project example))
       (= (:garden scope) (:translation-memory/garden example))
       (= (:source-locale scope) (:translation-memory/source-locale example))
       (= (:target-locale scope) (:translation-memory/target-locale example))
       (contains? (:current-candidate-set-ids scope)
                  (:translation-memory/candidate-set-id example))
       (not= (:exclude-manifest-id scope)
             (:translation-memory/manifest-id example))))

(defn projection
  "Build a deterministic dictionary from approved examples under exact scope.

  `current-candidate-set-ids` is mandatory. Durable approvals from a superseded
  candidate set remain history but cannot enter this active projection."
  [scope approved-examples]
  (let [{:keys [limit] :as checked} (checked-scope scope)
        entries (->> approved-examples
                     (map #(schema/assert-valid!
                            :translation-dictionary/memory-example
                            schema/TranslationMemoryExample %))
                     (filter #(same-scope? checked %))
                     (sort-by :translation-memory/id)
                     distinct
                     (take limit)
                     (mapv memory-example->entry))]
    (schema/assert-valid!
     :translation-dictionary/projection
     DictionaryProjection
     {:translation-dictionary/scope checked
      :translation-dictionary/entries entries})))
