(ns knoxx.backend.domain.translation-config
  "Resolve translation pipeline configuration from the Knoxx resource graph.

  One pure resolution path serves the review UI, the config endpoint, and the
  ingestion worker, so no consumer can reinterpret model precedence for itself.
  Pure: no HTTP, no external translation backend, no environment reads.

  Ordering matters. Org overrides merge onto the global default *before* catalog
  validation, so an override cannot smuggle an unknown model past a valid global
  default — validating the global first and then merging would do exactly that."
  (:require [clojure.string :as str]
            [knoxx.backend.law.publication :as publication]
            [knoxx.backend.law.translation-config :as law]
            [knoxx.backend.shape.resource-identity :as identity]))

(def global-config-id :knoxx.translation/pipeline-default)

(def config-keys
  [:translation/model :translation/source-locale :translation/default-review])

(defn org-config-id
  "Per-org override identity. Namespaced by org so two orgs cannot collide."
  [org-id]
  (when-not (str/blank? (str org-id))
    (keyword (str "orgs." org-id) "translation-pipeline")))

;; ── Indexing ───────────────────────────────────────────────────────────────

(defn config-resource?
  "A translation config resource: a policy carrying a translation model. Reuses
   the existing policy resource kind rather than inventing a mutable config
   store."
  [resource]
  (boolean (and (:policy/id resource) (contains? resource :translation/model))))

(defn config-id
  "Canonical identity of a config resource. `:policy/id` stays namespace-local
   through manifest expansion, so it is qualified here."
  [resource]
  (identity/canonical-id (:namespace resource) (:policy/id resource)))

(defn index-resources
  "Index the translation config policies and the model catalog."
  [resources]
  (reduce
   (fn [index resource]
     (cond-> index
       (config-resource? resource)
       (assoc-in [:configs (config-id resource)] resource)

       (:model/id resource)
       (assoc-in [:models (:model/id resource)] resource)))
   {:configs {} :models {}}
   resources))

;; ── Validation ─────────────────────────────────────────────────────────────

(defn validate-model-ref!
  "Reject a config whose model is absent from the Knoxx model catalog, so an
   invalid reference fails before a worker attempts a translation or a response
   is served."
  [index config]
  (let [model (:translation/model config)]
    (when-not (get-in index [:models model])
      (throw (ex-info "unknown translation model"
                      {:translation/model model
                       :known-models (vec (sort (keys (:models index))))})))
    config))

;; ── Resolution ─────────────────────────────────────────────────────────────

(defn resolve-config
  "Global default merged with the org override, then catalog-validated."
  [index {:keys [org-id]}]
  (let [global (get-in index [:configs global-config-id])
        override (some->> (org-config-id org-id) (get (:configs index)))]
    (when-not global
      (throw (ex-info "no translation pipeline configuration"
                      {:expected global-config-id})))
    (->> (select-keys (merge global override) config-keys)
         (validate-model-ref! index)
         (publication/assert-valid! global-config-id law/TranslationPipelineConfig))))

(defn apply-patch
  "Merge a decoded domain patch onto the resolved config and revalidate. Pure —
   the caller performs the resource write."
  [index context domain-patch]
  (publication/assert-valid! :translation/patch
                             law/TranslationPipelineConfigPatch
                             domain-patch)
  (->> (merge (resolve-config index context) domain-patch)
       (validate-model-ref! index)
       (publication/assert-valid! global-config-id law/TranslationPipelineConfig)))

(defn assert-patch-target-is-effective!
  "Refuse a patch whose caller would not see its own change.

   The write target is always the manifest owning `global-config-id`, while
   `resolve-config` lets an org override win over it. So for a caller whose org
   *has* an override, rewriting the global default changes nothing they can
   observe: the response would report the new model while their effective
   configuration kept the old one — a success for a change that did not happen.

   Refusing is the conservative half of this. It does not decide who may edit the
   global default in the first place; that is a tenancy question recorded on the
   review thread, not something to settle inside a validator."
  [index {:keys [org-id]}]
  (when-let [override-id (org-config-id org-id)]
    (when (get-in index [:configs override-id])
      (throw (ex-info "translation config patch would not take effect"
                      {:expected override-id
                       :translation/scope :org-override}))))
  index)

;; ── Wire codecs ────────────────────────────────────────────────────────────

(defn config->wire
  "Encode for JSON. The model is already a catalog string; locale and review
   policy are keywords and are encoded explicitly rather than left to
   `clj->js`."
  [config]
  (publication/assert-valid!
   :translation/config-wire
   law/TranslationConfigWireJson
   {:model (:translation/model config)
    :source-locale (identity/encode-keyword (:translation/source-locale config))
    :default-review (identity/encode-keyword (:translation/default-review config))}))

(defn wire->config
  "Decode a wire config back to domain values, for consumers that read the
   endpoint rather than the resource graph — notably the ingestion worker."
  [wire]
  (publication/assert-valid! :translation/config-wire law/TranslationConfigWireJson wire)
  {:translation/model (:model wire)
   :translation/source-locale (identity/decode-keyword (:source-locale wire))
   :translation/default-review (identity/decode-keyword (:default-review wire))})

(defn decode-config-patch
  "Decode the unqualified wire key onto the canonical qualified domain key.

   `knoxx.frontend.lib.api/request` serializes with `clj->js`, which erases
   keyword namespaces, so `:translation/model` leaves the browser as JSON
   `\"model\"`. Validating the qualified key would reject every real PATCH; and
   without an explicit decode step, an undecoded wire map merged into the
   resource would add an ignored `:model` key while the authoritative
   `:translation/model` silently kept its previous value."
  [wire]
  (publication/assert-valid! :translation/patch-wire law/TranslationConfigPatchJson wire)
  (publication/assert-valid! :translation/patch
                             law/TranslationPipelineConfigPatch
                             {:translation/model (:model wire)}))

(defn config-resource
  "The global config resource carrying a patched value, ready to be written."
  [index config]
  (merge (get-in index [:configs global-config-id]) config))

(def ^:private manifest-entry-keys
  "Keys that belong in the authored manifest entry. Manifest expansion stamps
   `:namespace`, `:contract/id`, `:contract/kind` and `:resource/qualified-id`
   onto the loaded definition; writing those back would duplicate derived data
   into the source of truth."
  (into [:policy/id :contract/doc] config-keys))

(defn manifest-edn-text
  "Render the config resource back as its owning namespace manifest.

   A whole-file rewrite keeps the resource the single authority: patching in
   place cannot leave a shadow override behind, and the file stays in the same
   authored shape a human would write."
  [resource]
  (let [entry (select-keys resource manifest-entry-keys)]
    (str ";; Generated by the Knoxx translation config facade.\n"
         ";; `:translation/model` is a catalog model id, spelled exactly as\n"
         ";; contracts/models/*.edn spells it.\n"
         (pr-str {:namespace (or (:namespace resource) :knoxx.translation)
                  :resources [entry]})
         "\n")))
