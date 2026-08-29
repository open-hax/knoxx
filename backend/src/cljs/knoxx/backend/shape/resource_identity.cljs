(ns knoxx.backend.shape.resource-identity
  "Canonical identity for resource kinds whose ids are namespace-qualified.

  Most resource kinds use a bare id. Document, garden, and publication do not:
  their ids are qualified keywords, so `:tenant-a/foo` and `:tenant-b/foo` are
  distinct resources. A namespace manifest writes the *local* id and declares
  the namespace once, while a standalone resource file writes the qualified id
  and declares no namespace. Both must land on the same canonical keyword.

  This lives in `shape.*` because it is a structure-only morphism over resource
  data: it knows which keys carry identity, and nothing about publication
  policy. Both the contract loader and the publication resolver consume it, so
  the rule is stated once rather than reimplemented on each side of the load
  boundary."
  (:require [clojure.string :as str]
            [katamorph.manifest :as manifest]))

;; ── Canonical identity ─────────────────────────────────────────────────────

(defn canonical-id
  "Canonical qualified identity for a resource id, or for a reference to one.

   Own ids and references share one rule so a namespace-local reference and an
   already-qualified reference to the same resource compare equal:

     already qualified  -> keep, its own namespace beats the manifest's
     bare + namespace   -> qualify under the manifest namespace
     bare, no namespace -> leave alone

   The last case matters: qualifying under `nil` would produce `(keyword nil
   \"foo\")`, which STRIPS identity instead of adding it."
  [namespace-value id]
  (cond
    (nil? id) nil
    (qualified-keyword? id) id
    (nil? namespace-value) id
    :else (manifest/qualified-id namespace-value id)))

(def identity-keys
  "Per-kind keys holding a qualified identity or a reference to one. Ordered so
   canonicalization is deterministic."
  {:document [:document/id]
   :garden [:garden/id]
   :publication [:publication/id :publication/document :publication/garden]})

(defn qualified-identity-kind?
  [kind]
  (contains? identity-keys kind))

(defn canonicalize-identity
  "Canonicalize every identity-bearing key for `kind`, using the definition's
   own declared `:namespace`.

   This must run BEFORE the definition is validated. The Malli shapes require
   qualified ids, so a manifest entry validated while its id is still
   namespace-local fails validation and is dropped by the loader — the
   resource then never reaches any projection at all."
  [kind definition]
  (if-let [keys-for-kind (get identity-keys kind)]
    (let [namespace-value (:namespace definition)]
      (reduce (fn [acc identity-key]
                (if (contains? acc identity-key)
                  (update acc identity-key #(canonical-id namespace-value %))
                  acc))
              definition
              keys-for-kind))
    definition))

;; ── Wire encoding ──────────────────────────────────────────────────────────

(defn encode-keyword
  "`:docs/probe` -> `\"docs/probe\"`, `:published` -> `\"published\"`.

   Never `(str keyword)`, which would emit the EDN leading colon."
  [value]
  (when (keyword? value)
    (if-let [namespace-part (namespace value)]
      (str namespace-part "/" (name value))
      (name value))))

(defn decode-keyword
  "Inverse of `encode-keyword`: `\"docs/probe\"` -> `:docs/probe`."
  [value]
  (cond
    (keyword? value) value
    (not (string? value)) value
    (str/includes? value "/") (let [[namespace-part name-part] (str/split value #"/" 2)]
                                (keyword namespace-part name-part))
    :else (keyword value)))

(defn encode-wire-values
  "Encode every keyword *value* in a projection so it survives JSON.

   `clj->js` renders a keyword with `name`, so `:knoxx.docs/translation-pipeline`
   would reach a client as `\"translation-pipeline\"` — different namespaces
   collapsing onto one wire id and defeating canonical identity entirely.

   Map *keys* are deliberately left alone: the wire convention for this
   codebase is unqualified JSON keys, with qualified domain keys reconstructed
   by explicit adapter mapping. Only values carry identity across the boundary.

   Written as an explicit recursion rather than `postwalk`, which visits map
   entries bottom-up and would encode the keys along with the values."
  [value]
  (cond
    (map? value) (reduce-kv (fn [acc k v] (assoc acc k (encode-wire-values v)))
                            {}
                            value)
    (or (vector? value) (set? value) (seq? value)) (mapv encode-wire-values value)
    (keyword? value) (encode-keyword value)
    :else value))
