(ns knoxx.backend.shape.resource-manifest
  "Structure-only edits to an already-parsed resource manifest.

  A manifest file may declare many resources at once — a document, its garden,
  and the publications relating them commonly live in one file. Writing a single
  resource back therefore cannot mean `pr-str` of that resource over the whole
  file: that destroys every sibling declared beside it.

  This namespace knows the manifest's shape and nothing about publication
  policy, which is why it lives in `shape.*`. It edits ONE field of ONE entry
  and returns the whole manifest, so a caller physically cannot widen a
  field-level edit into a whole-file replacement."
  (:require [knoxx.backend.shape.resource-identity :as resource-identity]))

(defn namespace-manifest?
  "True for `{:namespace ... :resources [...]}` — the multi-resource form."
  [edn]
  (and (map? edn)
       (some? (:namespace edn))
       (sequential? (:resources edn))))

(defn- entry-matches?
  "An entry's id is written namespace-locally, so it is canonicalized under the
   manifest's namespace before comparison — the same rule the loader applies."
  [namespace-value id-key canonical-id entry]
  (and (map? entry)
       (= canonical-id
          (resource-identity/canonical-id namespace-value (get entry id-key)))))

(defn assoc-entry-field
  "Set `field` to `value` on the one entry identified by `id-key`/`canonical-id`,
   leaving every other entry and every other field byte-identical.

   Returns the manifest unchanged when no entry matches, so a caller that has
   already established the resource exists can treat an unchanged result as a
   lookup disagreement rather than silently writing nothing new."
  [edn id-key canonical-id field value]
  (if (namespace-manifest? edn)
    (let [namespace-value (:namespace edn)]
      (update edn :resources
              (fn [entries]
                (mapv (fn [entry]
                        (if (entry-matches? namespace-value id-key canonical-id entry)
                          (assoc entry field value)
                          entry))
                      entries))))
    ;; A standalone single-resource file: the whole file IS the entry.
    (if (entry-matches? (:namespace edn) id-key canonical-id edn)
      (assoc edn field value)
      edn)))

(defn contains-entry?
  [edn id-key canonical-id]
  (if (namespace-manifest? edn)
    (boolean (some (partial entry-matches? (:namespace edn) id-key canonical-id)
                   (:resources edn)))
    (entry-matches? (:namespace edn) id-key canonical-id edn)))
