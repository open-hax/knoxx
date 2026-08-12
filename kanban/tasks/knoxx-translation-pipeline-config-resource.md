---
uuid: "knoxx-translation-pipeline-config-resource"
title: "Move translation pipeline configuration out of OpenPlanner"
status: incoming
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
- Validate referenced model ids against the Knoxx model resource catalog.
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

Resolution:

```clojure
(defn translation-config [resource-index {:keys [org-id]}]
  (let [global (resources/get resource-index :knoxx.translation/pipeline-default)
        org    (resources/get resource-index
                              (keyword (str "orgs." org-id) "translation-pipeline"))]
    (-> global
        (merge org)
        (select-keys [:translation/model
                      :translation/source-locale
                      :translation/default-review]))))

(defn update-translation-config! [ctx patch]
  (let [current (translation-config-resource ctx)
        next    (merge current patch)]
    (law/assert! publication/TranslationPipelineConfig next)
    (resources/write! ctx next)))
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

## Done when

- Translation review/pipeline UI no longer calls `/api/openplanner/v1/translations/config`.
- Model selection is reconstructable from Knoxx resources with OpenPlanner unavailable.
- Invalid model refs fail contract validation before a worker attempts translation.
