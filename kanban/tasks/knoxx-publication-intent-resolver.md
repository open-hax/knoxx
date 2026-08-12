---
uuid: "knoxx-publication-intent-resolver"
title: "Resolve desired publication topology from the Knoxx resource graph"
status: accepted
priority: P0
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
- Canonicalize resource identities and namespace-local references before indexing, filtering, conflict checks, key construction, **and before storing document/garden payloads in the index**.
- Ensure indexed document/garden payload identity fields contain the same canonical IDs used as their index keys, so downstream Malli validation never sees a namespace-local ID where a qualified identity is required.
- Detect duplicate canonical document/garden identities before insertion. Byte-equivalent duplicates may collapse; conflicting payloads for the same canonical id fail deterministically instead of using loader enumeration order.
- Treat publication relation identity as document × garden × locale × revision selector. Different revisions may coexist; only conflicting **non-archived** intents for the same relation key fail projection.
- Preserve archived publication intents as historical desired-state records without letting them conflict with an active replacement merely because document/garden/locale match.
- Expose pure queries for documents, gardens, publication intents, target locales, and intended revisions.
- Return explicit semantic blockers for malformed/incomplete desired state; leave runtime blockers to `knoxx-translation-publication-gate`.
- Define both document and list facade shapes before routes consume them.
- Add a Knoxx route/facade that serializes this projection for frontends without exposing the underlying resource-loader implementation.
- Keep raw Fastify request/reply interop inside `knoxx.backend.extern.*`; infra/domain receive and return CLJS data only.
- New asynchronous CLJS adapter code uses native `^:async` + `await`, not Promise `.then` chains.

## CLJS pseudocode

```clojure
(ns knoxx.backend.domain.publication)

(defn canonical-document-id [resource]
  (resources/canonical-id (:resource/namespace resource)
                          (:document/id resource)))

(defn canonical-garden-id [resource]
  (resources/canonical-id (:resource/namespace resource)
                          (:garden/id resource)))

(defn canonicalize-document [resource]
  (assoc resource :document/id (canonical-document-id resource)))

(defn canonicalize-garden [resource]
  (assoc resource :garden/id (canonical-garden-id resource)))

(defn canonicalize-intent [resource]
  (let [ns (:resource/namespace resource)]
    (-> resource
        (update :publication/id #(resources/canonical-id ns %))
        (update :publication/document #(resources/resolve-ref ns %))
        (update :publication/garden #(resources/resolve-ref ns %)))))

(defn publication-key [intent]
  [(:publication/document intent)
   (:publication/garden intent)
   (:publication/locale intent)
   (:publication/revision intent)])

(defn publication-sort-key [intent]
  [(:publication/document intent)
   (:publication/garden intent)
   (:publication/locale intent)
   (pr-str (:publication/revision intent))
   (:publication/id intent)])

(defn active-publication-intent? [intent]
  (not= :archived (:publication/state intent)))

(defn publication-conflicts [publications]
  (->> publications
       (filter active-publication-intent?)
       (group-by publication-key)
       (keep (fn [[k intents]]
               (when (> (count intents) 1)
                 {:publication/key k
                  :intents (vec (sort-by :publication/id intents))})))
       (sort-by #(mapv pr-str (:publication/key %)))
       vec))

(defn index-canonical! [idx kind id resource]
  (let [path     [kind id]
        existing (get-in idx path)]
    (cond
      (nil? existing)
      (assoc-in idx path resource)

      (= existing resource)
      idx

      :else
      (throw
       (ex-info "conflicting canonical resource identity"
                {:resource/kind kind
                 :resource/id id
                 :existing existing
                 :incoming resource})))))

(defn publication-index [resources]
  (let [idx
        (reduce
         (fn [idx resource]
           (let [idx (if (:document/id resource)
                       (let [document (canonicalize-document resource)]
                         (index-canonical! idx
                                           :documents
                                           (:document/id document)
                                           document))
                       idx)
                 idx (if (:garden/id resource)
                       (let [garden (canonicalize-garden resource)]
                         (index-canonical! idx
                                           :gardens
                                           (:garden/id garden)
                                           garden))
                       idx)]
             (if (:publication/id resource)
               (update idx :publications conj (canonicalize-intent resource))
               idx)))
         {:documents {} :gardens {} :publications []}
         resources)
        conflicts (publication-conflicts (:publications idx))]
    (when (seq conflicts)
      (throw (ex-info "conflicting publication intents"
                      {:conflicts conflicts})))
    (update idx :publications #(vec (sort-by publication-sort-key %)))))

(defn desired-publications [idx document-id]
  (let [canonical-id (resources/resolve-query-id document-id)]
    (->> (:publications idx)
         (filter #(= canonical-id (:publication/document %)))
         (mapv #(law.publication/hydrate-publication-intent idx %)))))

(defn document-view [idx document-id]
  {:document (get-in idx [:documents (resources/resolve-query-id document-id)])
   :publications (desired-publications idx document-id)})

(def PublicationListView
  [:map
   [:documents [:vector :map]]
   [:gardens [:vector :map]]])

(defn list-document-views [idx]
  (let [view {:documents (->> (keys (:documents idx))
                              sort
                              (mapv #(document-view idx %)))
              :gardens (->> (:gardens idx) vals (sort-by :garden/id) vec)}]
    (law/assert! PublicationListView view)
    view))
```

Infra handler returns CLJS data; it never touches Fastify handles:

```clojure
(ns knoxx.backend.infra.routes.publications)

(defn list-publication-documents! [{:keys [context]}]
  (let [resources (resource-store/resolved-resources context)
        idx       (publication/publication-index resources)]
    (publication/list-document-views idx)))
```

The owning extern adapter alone decodes/encodes Fastify and uses Knoxx's native async style:

```clojure
(ns knoxx.backend.extern.fastify.publications)

(defn register-list-route! [app handler]
  (.get app "/api/publications/documents"
        (fn ^:async [req reply]
          (let [request  (decode-request req)
                response (await (handler request))]
            (send-json! reply response)))))
```

If the infra handler is synchronous, `await` still safely accepts its resolved value; the extern shape therefore remains compatible if the handler later becomes effectful without introducing a `.then` chain.

## Laws

- Same resource graph -> byte-equivalent canonical projection regardless of source file enumeration order.
- Namespace-local and already-qualified references resolve to the same canonical identity before comparison.
- Every document/garden stored in the resolver index has a canonical identity in both its map key and its own `:document/id` / `:garden/id` field.
- Two differing document or garden payloads cannot silently occupy the same canonical id; projection rejects the conflict rather than picking the last enumerated manifest.
- Publication conflict identity includes revision selector. Different revisions may coexist, and archived historical intents do not invalidate a non-archived replacement at another/current revision.
- Two non-archived intents for the same document/garden/locale/revision relation are surfaced deterministically before projection.
- Resolver output contains no execution status such as worker state, publish timestamps, or adapter receipts.
- The domain namespace remains pure and reusable by CLI/tests without Fastify or Mongo.
- Native request/reply handles are born and die in the extern adapter.
- New async extern functions use `^:async`/`await`; Promise chaining is not part of the prescribed implementation shape.

## Done when

- CMS can list documents, gardens, targets, locales, and requested publication states from this projection alone.
- A pure test deletes every OpenPlanner dependency from the fixture and produces the same desired topology.
- Namespace-local reference fixtures produce the same canonical projection as fully-qualified fixtures, including canonical IDs inside the returned document/garden payloads.
- Hydrating a publication that references a namespace-local document validates against the qualified `Document` law shape.
- Two manifests with the same canonical document/garden id and different payloads fail deterministically regardless of enumeration order; byte-equivalent duplicates do not change projection.
- Different revision selectors for the same document/garden/locale are not rejected merely for sharing those first three dimensions; duplicate active intent for the exact revision relation does fail.
- Archived historical intent can coexist with a replacement without becoming an active relation conflict.
- One backend facade serves the projection without `/api/openplanner/...` in its contract and without raw Fastify interop outside `extern.*`.
- The Fastify adapter test exercises the route with native `^:async`/`await` and no `.then` chain.
