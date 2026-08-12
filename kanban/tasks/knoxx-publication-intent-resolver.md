---
uuid: "knoxx-publication-intent-resolver"
title: "Resolve desired publication topology from the Knoxx resource graph"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "cms", "publication", "domain"]
created_at: "2026-08-12T00:00:00Z"
points: 5
category: tasks
---
# Resolve desired publication topology from the Knoxx resource graph

> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Provide one pure Knoxx-owned query model that turns loaded resources into the complete desired publication topology consumed by CMS, translation policy, reconciliation, and deploy verification.

The resolver must not call HTTP, Mongo, OpenPlanner, filesystem effects, or worker state. It consumes already-loaded validated resources and returns deterministic CLJS data.

## Scope

- Build document/garden/publication indexes from the resource registry.
- Resolve namespace-local refs to canonical ids before comparison.
- Expose pure queries for documents, gardens, publication intents, target locales, and intended revisions.
- Return explicit semantic blockers for malformed/incomplete desired state; leave runtime blockers to `knoxx-translation-publication-gate`.
- Add a Knoxx route/facade that serializes this projection for frontends without exposing the underlying resource-loader implementation.

## CLJS pseudocode

```clojure
(ns knoxx.backend.domain.publication)

(defn publication-index [resources]
  (reduce
   (fn [idx resource]
     (cond-> idx
       (:document/id resource)
       (assoc-in [:documents (:document/id resource)] resource)

       (:garden/id resource)
       (assoc-in [:gardens (:garden/id resource)] resource)

       (:publication/id resource)
       (update :publications conj resource)))
   {:documents {} :gardens {} :publications []}
   resources))

(defn publication-key [intent]
  [(:publication/document intent)
   (:publication/garden intent)
   (:publication/locale intent)])

(defn desired-publications [idx document-id]
  (->> (:publications idx)
       (filter #(= document-id (:publication/document %)))
       (sort-by publication-key)
       vec))

(defn document-view [idx document-id]
  {:document (get-in idx [:documents document-id])
   :publications (desired-publications idx document-id)})
```

Thin infra route:

```clojure
(defn list-publication-documents! [req reply]
  (let [resources (resource-store/resolved-resources (:context req))
        idx       (publication/publication-index resources)]
    (.send reply (publication/list-document-views idx))))
```

## Laws

- Same resource graph -> byte-equivalent canonical projection regardless of source file enumeration order.
- Resolver output contains no execution status such as worker state, publish timestamps, or adapter receipts.
- Duplicate publication keys are rejected or surfaced as deterministic conflicts; never "last one wins" by loader order.
- The domain namespace remains pure and reusable by CLI/tests without Fastify or Mongo.

## Done when

- CMS can list documents, gardens, targets, locales, and requested publication states from this projection alone.
- A pure test deletes every OpenPlanner dependency from the fixture and produces the same desired topology.
- One backend facade serves the projection without `/api/openplanner/...` in its contract.
