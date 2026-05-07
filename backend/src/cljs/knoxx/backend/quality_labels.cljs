(ns knoxx.backend.quality-labels
  "Helpers for OpenPlanner weak quality labels.

   ✅/check reactions are preferred context. ❌/cross reactions are hard
   exclusions: callers must never pass bad-labeled records through hydration or
   Discord context surfaces."
  (:require [clojure.string :as str]))

(def ^:private good-reaction-tokens
  #{"✅" "☑" "☑️" "✔" "✔️" "check" "checkmark" "quality:good" "good" "good output"})

(def ^:private bad-reaction-tokens
  #{"❌" "✖" "✖️" "✗" "✘" "❎" "x" "cross" "quality:bad" "bad" "bad output"})

(defn- normalize-token
  [value]
  (-> (str (or value ""))
      (str/replace #"\uFE0F" "")
      str/trim
      str/lower-case))

(defn- map-get-any
  [m ks]
  (some (fn [k]
          (when (contains? m k)
            (get m k)))
        ks))

(defn- seqish?
  [value]
  (and (sequential? value) (not (string? value))))

(declare quality-label-evidence)

(defn- value-evidence
  [value]
  (cond
    (nil? value) []
    (map? value) (quality-label-evidence value)
    (seqish? value) (mapcat value-evidence value)
    :else [(str value)]))

(defn- nested-label-maps
  [m]
  (remove nil?
          [(map-get-any m [:openplannerLabels :openplanner-labels :openplanner_labels])
           (get-in m [:extra :openplanner_labels])
           (get-in m [:extra :openplannerLabels])
           (get-in m [:metadata :openplanner_labels])
           (get-in m [:metadata :openplannerLabels])
           (get-in m [:metadata :extra :openplanner_labels])
           (get-in m [:metadata :extra :openplannerLabels])
           (get m :metadata)
           (get m :extra)]))

(defn- quality-label-evidence
  [m]
  (let [direct [(map-get-any m [:quality :quality_label :quality-label])
                (map-get-any m [:explicit_meaning :explicit-meaning])
                (map-get-any m [:emoji :reaction_emoji :reaction-emoji])
                (map-get-any m [:label])]
        collections [(map-get-any m [:labels])
                     (map-get-any m [:reaction_emojis :reaction-emojis])
                     (map-get-any m [:history])]
        nested (nested-label-maps m)]
    (concat (mapcat value-evidence direct)
            (mapcat value-evidence collections)
            (mapcat value-evidence nested))))

(defn- substring-candidate?
  [candidate]
  (and (not (str/blank? candidate))
       (not (re-matches #"[a-z]+" candidate))))

(defn- token-matches?
  [token candidates]
  (let [normalized (normalize-token token)]
    (boolean
     (or (contains? candidates normalized)
         (some #(and (substring-candidate? %) (str/includes? normalized %)) candidates)))))

(defn quality-label
  "Return \"good\", \"bad\", or nil for a record/message/hit.

   Bad dominates good so a crossed-out record is excluded even if it also has
   a checkmark."
  [record]
  (let [tokens (quality-label-evidence record)]
    (cond
      (some #(token-matches? % bad-reaction-tokens) tokens) "bad"
      (some #(token-matches? % good-reaction-tokens) tokens) "good"
      :else nil)))

(defn good?
  [record]
  (= "good" (quality-label record)))

(defn bad?
  [record]
  (= "bad" (quality-label record)))

(defn not-bad?
  [record]
  (not (bad? record)))

(defn drop-bad
  [records]
  (->> (or records [])
       (filter not-bad?)
       vec))

(defn good-first-then-not-bad
  "Return checkmarked records first, then unlabeled/non-bad records.
   Existing relative order within each group is preserved."
  [records]
  (let [records (drop-bad records)
        good (filter good? records)
        rest (remove good? records)]
    (vec (concat good rest))))
