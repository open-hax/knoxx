---
uuid: "knoxx-translation-pipeline-config-resource"
title: "Move translation pipeline configuration out of OpenPlanner"
status: accepted
priority: P1
labels: ["tasks", "5sp", "has-parent", "translations", "contracts", "openplanner"]
created_at: "2026-08-12T00:00:00Z"
points: 5
category: tasks
---
# Move translation pipeline configuration out of OpenPlanner

> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Remove the remaining translation configuration authority at `/api/openplanner/v1/translations/config`. Translation model/policy selection should resolve from Knoxx resources just like agent/model/capability configuration.

The cutover includes **every runtime consumer**, not only the review UI. The production ingestion worker currently reads OpenPlanner `/v1/translations/config` as its first-choice model authority; that worker must move to the Knoxx-owned config boundary before the OpenPlanner endpoint can be retired.

## Scope

- Represent translation pipeline configuration as Knoxx resource data using an existing suitable resource kind/facet where possible; do not invent a mutable config database.
- Resolve org/tenant-specific overrides deterministically from the resource graph.
- Replace frontend reads/writes of `/api/openplanner/v1/translations/config` with a Knoxx-owned translation config facade.
- Replace the ingestion worker's direct OpenPlanner config fetch (`ingestion/src/kms_ingestion/translation/worker.clj`) with the same Knoxx-owned resolved configuration boundary.
- Inventory repo-wide callers of both `/api/openplanner/v1/translations/config` and direct OpenPlanner `/v1/translations/config`; no worker/service may retain a hidden second authority.
- Resolve and validate referenced model ids against the Knoxx model resource catalog on both reads and writes.
- Give the config endpoint an explicit JSON wire contract with **unqualified** keys (`{:model string}`) plus a decoder onto canonical qualified domain keys. `clj->js` in the shared frontend `api/request` helper erases keyword namespaces, so a PATCH validated against `:translation/model` would leave the authoritative model unchanged while accepting an ignored extra key.
- Define worker fallback semantics explicitly: transport/config lookup failure is surfaced as configuration failure or uses a deliberately declared Knoxx fallback policy; it must not silently fall back to an unrelated env/default model that disagrees with the resource graph.
- Preserve runtime hot-reload only if it can be expressed as an explicit resource write/reload; do not keep OpenPlanner as a hidden second authority.

## CLJS pseudocode

Resource shape sketch:

```clojure
{:namespace :knoxx.translation
 :resources
 [{:policy/id :pipeline-default
   :translation/model :models/glm-5
   :translation/source-locale :en
   :translation/default-review :required}]}
```

Resolution and catalog validation:

```clojure
(defn validate-model-ref! [resource-index config]
  (let [model-id (resources/resolve-ref :knoxx.translation
                                        (:translation/model config))
        model    (get-in resource-index [:models model-id])]
    (when-not model
      (throw (ex-info "unknown translation model"
                      {:translation/model model-id})))
    (assoc config :translation/model model-id)))

(defn translation-config [resource-index {:keys [org-id]}]
  (let [global (resources/get resource-index :knoxx.translation/pipeline-default)
        org    (resources/get resource-index
                              (keyword (str "orgs." org-id) "translation-pipeline"))
        merged (-> global
                   (merge org)
                   (select-keys [:translation/model
                                 :translation/source-locale
                                 :translation/default-review]))]
    (->> merged
         (validate-model-ref! resource-index)
         (law/assert! publication/TranslationPipelineConfig))))

(defn update-translation-config! [ctx domain-patch]
  (law/assert! publication/TranslationPipelineConfigPatch domain-patch)
  (let [resource-index (resources/resolved-index ctx)
        current        (translation-config-resource ctx)
        next           (merge current domain-patch)
        validated      (->> next
                            (validate-model-ref! resource-index)
                            (law/assert! publication/TranslationPipelineConfig))]
    (resources/write! ctx validated)))
```

The endpoint owns an explicit JSON wire contract in both directions. Wire keys are
**unqualified** because the shared `knoxx.frontend.lib.api/request` helper serializes
bodies with `clj->js` (which erases keyword namespaces, so `:translation/model` leaves
as JSON `"model"`) and decodes responses with `js->clj :keywordize-keys true` (which
yields `:model`). A decoder maps those wire keys onto canonical qualified domain keys
before any merge or validation happens; without it a PATCH would add an ignored extra
key and silently retain the previous authoritative model:

```clojure
(def TranslationConfigWireJson
  [:map
   [:model :string]
   [:source-locale :string]
   [:default-review [:enum "required" "none"]]])

(def TranslationConfigPatchJson
  [:map [:model :string]])

(defn config->wire [config]
  (law/assert!
   TranslationConfigWireJson
   {:model (encode-keyword (:translation/model config))
    :source-locale (encode-keyword (:translation/source-locale config))
    :default-review (encode-keyword (:translation/default-review config))}))

(defn decode-config-patch [wire]
  (law/assert! TranslationConfigPatchJson wire)
  (let [domain {:translation/model (keyword (:model wire))}]
    (law/assert! publication/TranslationPipelineConfigPatch domain)
    domain))

(defn ^:async patch-translation-config-route! [ctx req]
  (let [request (decode-request req)
        domain  (decode-config-patch (:body request))]
    (config->wire (await (update-translation-config! ctx domain)))))
```

One Knoxx-owned read boundary is used by both UI and workers:

```clojure
(defn resolved-translation-config! [{:keys [resource-index org-id principal]}]
  (auth/require! principal :translations/config-read)
  (translation-config resource-index {:org-id org-id}))

(defn worker-config-response [ctx]
  (config->wire (resolved-translation-config! ctx)))
```

The ingestion worker may consume that boundary through an authenticated Knoxx endpoint or through an injected resolved config artifact, but it may not call OpenPlanner directly or independently reinterpret model precedence.

Frontend API:

```clojure
(defn pipeline-config []
  (api/request "/api/translations/config"))

(defn update-pipeline-config [model-id]
  (api/request "/api/translations/config"
               {:method "PATCH"
                :body {:model (encode-keyword model-id)}}))
```

The frontend sends the same unqualified wire key the backend contract declares, so the
canonical model reference is reconstructed by the endpoint decoder rather than assumed
to have survived JSON.

## Laws

- A translation config cannot resolve successfully if its model reference is absent from the Knoxx model catalog.
- Global/org merging happens before catalog validation, so an org override cannot smuggle an invalid model past a validated global default.
- Reads, writes, UI consumers, and worker consumers use the same reference normalization and validation path.
- A config PATCH is decoded from unqualified wire keys into canonical qualified domain keys before it is merged; an undecoded wire map can never reach the resource write.
- The ingestion worker cannot choose a model from OpenPlanner, environment, or a hard-coded default when that differs from the authoritative Knoxx resource configuration.
- If a fallback policy exists, it is represented in Knoxx-owned configuration/law and therefore produces the same answer for UI and worker consumers.

## Done when

- Translation review/pipeline UI no longer calls `/api/openplanner/v1/translations/config`.
- `ingestion/src/kms_ingestion/translation/worker.clj` no longer calls OpenPlanner `/v1/translations/config` and obtains the same resolved model the Knoxx config facade reports.
- Repo-wide search finds no production translation-config consumer using OpenPlanner as authority.
- Model selection is reconstructable from Knoxx resources with OpenPlanner unavailable.
- Invalid model refs fail catalog/contract validation before a worker attempts translation or a config response is served.
- A test changes the resource-selected model and proves both the config facade and ingestion worker select the same canonical model id.
- A PATCH body shaped exactly as `clj->js` emits it — JSON `{"model":"models/glm-5"}` — changes the authoritative `:translation/model`, and a test proves the resolved config after the PATCH reports the new model rather than the previous one.
- The config read response is produced by the explicit `config->wire` encoder, so model, source locale, and review policy cross the boundary as strings and decode back to the same keywords.
