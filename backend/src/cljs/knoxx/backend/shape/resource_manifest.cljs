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

(defn matching-entry-count
  [edn id-key canonical-id]
  (if (namespace-manifest? edn)
    (count (filterv (partial entry-matches? (:namespace edn) id-key canonical-id)
                    (:resources edn)))
    (if (entry-matches? (:namespace edn) id-key canonical-id edn) 1 0)))

(defn contains-entry?
  [edn id-key canonical-id]
  (pos? (matching-entry-count edn id-key canonical-id)))

(defn assoc-entry-field
  "Set `field` to `value` on the EXACTLY ONE entry identified by
   `id-key`/`canonical-id`, leaving every other entry and every other field
   byte-identical.

   Throws on zero matches and on more than one. The multiple-match case is not
   hypothetical: duplicate canonical publication ids are currently not rejected
   by the resolver (`knoxx-publication-duplicate-identity`), so a file can
   legitimately hold two entries claiming the same id today. A `mapv` that
   edits every match would then mutate declarations the caller never named,
   from a request that addressed one resource. Refusing is the only answer that
   cannot silently do more than it was asked."
  [edn id-key canonical-id field value]
  (let [matches (matching-entry-count edn id-key canonical-id)]
    (when (zero? matches)
      (throw (ex-info "no manifest entry matches the requested resource"
                      {:resource/id canonical-id :id-key id-key})))
    (when (> matches 1)
      (throw (ex-info "refusing to write: more than one manifest entry claims this id"
                      {:resource/id canonical-id :id-key id-key :matches matches})))
    (if (namespace-manifest? edn)
      (let [namespace-value (:namespace edn)]
        (update edn :resources
                (fn [entries]
                  (mapv (fn [entry]
                          (if (entry-matches? namespace-value id-key canonical-id entry)
                            (assoc entry field value)
                            entry))
                        entries))))
      (assoc edn field value))))

(defn unchanged-except?
  "True when `next-edn` differs from `edn` in nothing but `field` on the entries
   matching `canonical-id`.

   The writer's whole contract is that it touches one field. Asserting that
   against the value about to be persisted — rather than trusting the transform
   — is what turns 'this function should not widen' into something the write
   path checks before it reaches the filesystem."
  [edn next-edn id-key canonical-id field]
  (let [strip (fn [m]
                (if (entry-matches? (:namespace m) id-key canonical-id m)
                  (dissoc m field)
                  m))]
    (if (namespace-manifest? edn)
      (let [ns-value (:namespace edn)
            strip-ns (fn [entries]
                       (mapv (fn [entry]
                               (if (entry-matches? ns-value id-key canonical-id entry)
                                 (dissoc entry field)
                                 entry))
                             entries))]
        (and (namespace-manifest? next-edn)
             (= (dissoc edn :resources) (dissoc next-edn :resources))
             (= (strip-ns (:resources edn)) (strip-ns (:resources next-edn)))))
      (= (strip edn) (strip next-edn)))))
