---
uuid: "knoxx-translation-pipeline-config-resource"
title: "Move translation pipeline configuration out of OpenPlanner"
status: accepted
priority: P1
labels: ["tasks", "3sp", "has-parent", "translations", "contracts", "openplanner"]
created_at: "2026-08-12T00:00:00Z"
points: 3
category: tasks
---
# Move translation pipeline configuration out of OpenPlanner

> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Remove the remaining translation configuration authority at `/api/openplanner/v1/translations/config`. Translation model/policy selection should resolve from Knoxx resources just like agent/model/capability configuration.

## Scope

- Represent translation pipeline configuration as Knoxx resource data using an existing suitable resource kind/facet where possible; do not invent a mutable config database.
- Resolve org/tenant-specific overrides deterministically from the resource graph.
- Replace frontend reads/writes of `/api/openplanner/v1/translations/config` with a Knoxx-owned translation config facade.
- Resolve and validate referenced model ids against the Knoxx model resource catalog on both reads and writes.
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

(defn update-translation-config! [ctx patch]
  (let [resource-index (resources/resolved-index ctx)
        current        (translation-config-resource ctx)
        next           (merge current patch)
        validated      (->> next
                            (validate-model-ref! resource-index)
                            (law/assert! publication/TranslationPipelineConfig))]
    (resources/write! ctx validated)))
```

Frontend API:

```clojure
(defn pipeline-config []
  (api/request "/api/translations/config"))

(defn update-pipeline-config [model-id]
  (api/request "/api/translations/config"
               {:method "PATCH"
                :body {:translation/model model-id}}))
```

## Laws

- A translation config cannot resolve successfully if its model reference is absent from the Knoxx model catalog.
- Global/org merging happens before catalog validation, so an org override cannot smuggle an invalid model past a validated global default.
- Reads and writes use the same reference normalization and validation path.

## Done when

- Translation review/pipeline UI no longer calls `/api/openplanner/v1/translations/config`.
- Model selection is reconstructable from Knoxx resources with OpenPlanner unavailable.
- Invalid model refs fail catalog/contract validation before a worker attempts translation or a config response is served.
