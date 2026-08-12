---
uuid: "knoxx-openplanner-rest-retirement"
title: "Retire OpenPlanner REST as a Knoxx CMS/translation dependency"
status: accepted
priority: P2
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
- Remove the ingestion worker's direct OpenPlanner `/v1/translations/config` lookup; production worker model selection must resolve through the Knoxx-owned translation-config boundary before retirement.
- Remove or narrow proxy routes that exist only to preserve those authority paths.
- Remove `KNOXX_EXPECT_OPENPLANNER_REST` and the conditional CMS skip from deploy verification once the replacement surface is unconditional.
- Keep OpenPlanner-backed adapters only behind `IPublicationTarget` or other explicit extern/infra boundaries.
- Ensure no frontend module, backend route, ingestion worker, or other shipped service imports/calls an OpenPlanner config/publication authority path after cutover.
- Define one shared required-surface contract and reuse it in deploy verification and `knoxx-contract-publication-e2e`, including route method and authorization expectations.
- Replacement CLJS routes/functions use native `^:async`/`await` where asynchronous behavior is required.

## CLJS pseudocode

Legacy shapes being removed:

```clojure
(defn load-gardens-legacy! []
  (api/request "/api/openplanner/v1/gardens"))

(defn pipeline-config-legacy! []
  (api/request "/api/openplanner/v1/translations/config"))
```

Replacement shapes:

```clojure
(defn ^:async load-gardens! []
  (let [response (await (api/request "/api/publications/gardens"))]
    (:gardens response)))

(defn ^:async pipeline-config! []
  (await (api/request "/api/translations/config")))

(defn ^:async publication-health! []
  (await (api/request "/api/publications/health")))
```

One shared pure surface specification, imported by both the deploy verifier and E2E test:

```clojure
(ns knoxx.backend.law.required-surfaces)

(def required-publication-surfaces
  [{:method :get
    :path "/api/publications/health"
    :auth :public}
   {:method :get
    :path "/api/publications/documents?limit=1"
    :auth :publication-read}
   {:method :get
    :path "/api/publications/gardens"
    :auth :publication-read}
   {:method :get
    :path "/api/translations/documents?limit=1"
    :auth :translation-read}
   {:method :get
    :path "/api/translations/config"
    :auth :translation-read}])
```

The symbolic auth expectations above map to the real Knoxx authorization guards/capabilities in the verifier. The same test data must assert both an authorized success and the intended anonymous/unauthorized denial for non-public routes.

```clojure
(defn verify-required-surfaces! [http auth-harness]
  (doseq [{:keys [method path auth]} required-publication-surfaces]
    (let [authorized (http/request! method path
                                    (auth-harness/headers-for auth))]
      (assert (= 200 (:status authorized)))
      (when-not (= :public auth)
        (assert (contains? #{401 403}
                           (:status (http/request! method path {}))))))))
```

Repository-wide retirement check is broader than frontend routes:

```clojure
(def forbidden-authority-patterns
  ["/api/openplanner/v1/gardens"
   "/api/openplanner/v1/translations/config"
   "/v1/translations/config"])

(defn assert-no-openplanner-authority-callers! [source-index]
  (doseq [pattern forbidden-authority-patterns]
    (assert (empty? (source-index/shipped-callers pattern)))))
```

There should be no equivalent of:

```clojure
(when expect-openplanner-rest?
  (verify-cms!))
```

## Removal gate

Do not delete the compatibility path before:

- publication resources and resolver are live;
- the one-time OpenPlanner publication migration has converged and conflicts are resolved;
- translation pipeline config is resource-owned;
- the ingestion translation worker consumes that same Knoxx-owned config and no longer reads OpenPlanner config directly;
- translation/review gating and at least one publication adapter exist;
- CMS reads/writes the resource projection;
- repo-wide shipped-code search shows no remaining OpenPlanner authority callers;
- the shared required-surface verification passes without OpenPlanner REST.

## Done when

- Grepping shipped frontend/backend/ingestion code for OpenPlanner garden and translation-config authority routes returns no callers.
- `ingestion/src/kms_ingestion/translation/worker.clj` resolves the same Knoxx resource-selected model reported by `/api/translations/config`.
- Production deploy verification and the contract-publication E2E import the same complete required-surface list.
- All five replacement surfaces are checked for method and intended authorization behavior.
- Replacement asynchronous CLJS surfaces follow the repository's `^:async`/`await` convention.
- Production deploy verification requires CMS/publication surfaces unconditionally.
- `KNOXX_EXPECT_OPENPLANNER_REST` is gone.
- OpenPlanner may be absent without degrading Knoxx's ability to describe or edit desired publication state or changing the worker's selected translation model.
