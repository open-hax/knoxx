---
uuid: "knoxx-openplanner-rest-retirement"
title: "Retire OpenPlanner REST as a Knoxx CMS/translation dependency"
status: incoming
priority: P1
labels: ["tasks", "3sp", "has-parent", "cms", "translations", "openplanner", "deploy"]
created_at: "2026-08-12T00:00:00Z"
points: 3
category: tasks
---
# Retire OpenPlanner REST as a Knoxx CMS/translation dependency

> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Delete the compatibility dependency after the resource-owned publication path is live. Production Knoxx must no longer need a host OpenPlanner REST service for CMS gardens/publication state or translation pipeline config.

## Scope

- Remove CMS reads of `/api/openplanner/v1/gardens` and any legacy CMS publication routes that remain authoritative only because OpenPlanner owns the state.
- Remove translation config reads/writes of `/api/openplanner/v1/translations/config`.
- Remove or narrow proxy routes that exist only to preserve those authority paths.
- Remove `KNOXX_EXPECT_OPENPLANNER_REST` and the conditional CMS skip from deploy verification once the replacement surface is unconditional.
- Keep OpenPlanner-backed adapters only behind `IPublicationTarget` or other explicit extern/infra boundaries.
- Ensure no frontend module imports an `openplanner` API wrapper for publication semantics after cutover.

## CLJS pseudocode

Before:

```clojure
(defn load-gardens! []
  (api/request "/api/openplanner/v1/gardens"))

(defn pipeline-config []
  (-> (api/request "/api/openplanner/v1/translations/config")
      (.then :config)))
```

After:

```clojure
(defn load-gardens! []
  (-> (api/request "/api/publications/gardens")
      (.then :gardens)))

(defn pipeline-config []
  (api/request "/api/translations/config"))

(defn publication-health! []
  (api/request "/api/publications/health"))
```

Deploy probe intent:

```clojure
(def required-surfaces
  ["/api/publications/health"
   "/api/publications/documents?limit=1"
   "/api/translations/documents?limit=1"])

(defn verify-required-surfaces! [http]
  (doseq [path required-surfaces]
    (assert (= 200 (:status (http/get! path))))))
```

There should be no equivalent of:

```clojure
(when expect-openplanner-rest?
  (verify-cms!))
```

## Removal gate

Do not delete the compatibility path before:

- publication resources and resolver are live;
- CMS reads/writes the resource projection;
- translation pipeline config is resource-owned;
- existing publication state has been migrated or explicitly abandoned;
- at least one publication adapter can materialize requested state.

## Done when

- Grepping shipped frontend/backend code for the two OpenPlanner authority routes returns no callers.
- Production deploy verification requires CMS/publication surfaces unconditionally.
- `KNOXX_EXPECT_OPENPLANNER_REST` is gone.
- OpenPlanner may be absent without degrading Knoxx's ability to describe or edit desired publication state.
