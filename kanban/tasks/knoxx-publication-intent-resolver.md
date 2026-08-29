---
category: "tasks"
labels: ["tasks", "5sp", "has-parent", "cms", "publication", "domain"]
write-id: "1786605378024-0.kuatvftrlake4kj0hz"
points: "5"
title: "Resolve desired publication topology from the Knoxx resource graph"
priority: "P0"
status: "review"
uuid: "knoxx-publication-intent-resolver"
created_at: "2026-08-12T00:00:00Z"
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

## TDD plan

Test namespaces:

- `knoxx.backend.domain.publication-resolver-test`
  (`backend/test/cljs/knoxx/backend/domain/publication_resolver_test.cljs`)
- `knoxx.backend.extern.fastify-publication-test`
  (`backend/test/cljs/knoxx/backend/extern/fastify_publication_test.cljs`)

Pure resolver tests first:

1. `index-canonicalizes-namespace-local-refs` — a manifest using
   `:translation-pipeline` and a manifest using
   `:knoxx.docs/translation-pipeline` produce byte-equal projections.
2. `indexed-payload-identity-is-canonical` — the stored document and garden
   payloads carry canonical `:document/id` / `:garden/id`, and each payload
   validates against the qualified `Document` / `Garden` law shape.
3. `hydration-of-namespace-local-reference-validates` — hydrating a publication
   that references a namespace-local document passes `Document` validation
   (regression for the identity-vs-key split).
4. `projection-is-enumeration-order-independent` — the same manifests supplied
   in reversed load order produce identical output.
5. `byte-equivalent-duplicate-ids-collapse` — two identical document maps under
   one canonical id yield one entry and no error.
6. `conflicting-duplicate-ids-fail-deterministically` — two differing payloads
   for one canonical id fail with the same error in both enumeration orders.
7. `publication-relation-key-includes-revision` — intents identical in
   document/garden/locale but differing in revision selector both project.
8. `duplicate-active-relation-fails` — two non-archived intents for the exact
   same document × garden × locale × revision are surfaced as a conflict.
9. `archived-intent-does-not-conflict-with-replacement` — an archived intent and
   an active replacement sharing document/garden/locale coexist, and the
   archived one is still present as history.
10. `projection-excludes-runtime-state` — the projection of a fixture that also
    carries receipts contains no publish timestamp, worker state, or adapter
    receipt key.
11. `projection-has-no-openplanner-dependency` — resolver output for a fixture
    with every OpenPlanner entry deleted equals the output with them present.

Facade/adapter tests second:

12. `document-view-and-list-view-shapes` — both facade shapes validate before any
    route consumes them, and the list view is not double-wrapped.
13. `^:async publication-route-returns-projection` — an `^:async` deftest awaits
    the route handler against a decoded fake request and asserts the body; the
    test also asserts the handler source contains no `.then` chain and that
    native request handles never leave `extern.*`.

Then implement `knoxx.backend.domain.publication-resolver` and the extern
Fastify adapter until green.

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

---
Ready gate 2026-08-12: sized 5sp (<=5, eligible to implement). Walked accepted -> breakdown -> ready via the Rheos promethean FSM. Scope, laws and acceptance criteria confirmed on the card; TDD plan section names the failing tests to write first. Note: canonical-identity-in-payload and archived-vs-active conflict identity are the two highest-risk behaviours; both have dedicated first tests.
---

Pre-implementation review 2026-08-13 (CodeRabbit, not yet actioned — this card is still `ready`, not started): `PublicationListView` and `document-view` use bare `:map` entries instead of a concrete `PublicationDocumentView` schema (with nested `Document`/`Garden`/`PublicationIntent`); `document-view`'s assembled result isn't validated before `list-document-views` exposes it, so data missing the `:document/id` the CMS facade reads at `[:document :document/id]` can pass through undetected. Define and validate the concrete schema before implementing.

Pre-implementation review 2026-08-13 (Codex, not yet actioned): `index-canonical!`'s conflict exception puts the map already present in the index under `:existing` and the one being inserted under `:incoming`, but which payload lands in which key depends purely on filesystem/loader enumeration order — the same conflicting pair can throw with `:existing`/`:incoming` swapped across runs. The card requires identical, deterministic failure evidence regardless of enumeration order; sort or otherwise canonicalize the pair (e.g. by a stable key on the payloads themselves) before throwing, instead of using positional insertion order.

---
Implemented 2026-08-13. Namespaces: knoxx.backend.domain.publication-resolver (pure), knoxx.backend.infra.routes.publications (facade, CLJS data only), knoxx.backend.extern.fastify.publications (owns all Fastify interop), plus PublicationDocumentView/PublicationListView in law.publication. Routes GET /api/publications/documents and /api/publications/documents/:documentId registered in routes/app.cljs.

Both pre-implementation review findings actioned. CodeRabbit: the view shapes are concrete — PublicationDocumentView composes law Document + [:vector PublicationIntent], PublicationListView composes those plus Garden, and document-view validates before list-document-views can expose it, so a document missing the canonical :document/id the CMS reads at [:document :document/id] fails at assembly. Codex: index-canonical! no longer reports :existing/:incoming, which encoded loader enumeration order in the error itself; the pair is now one :conflicting-payloads vector sorted by an order-independent rendering, and conflicting-duplicate-ids-fail-deterministically asserts both enumeration orders yield equal ex-data. Verified that assertion bites by reverting to :existing/:incoming — it failed with the payloads swapped exactly as Codex predicted, then passed again after restoring.

Key implementation finding: katamorph's entry-definition stamps :namespace, :resource/qualified-id and :contract/id on an expanded manifest entry but leaves the entry's own :document/id namespace-LOCAL. Since law Document requires qualified-keyword?, manifest-sourced documents could not validate. That is exactly the 'local-id canonicalization' item knoxx-publication-resource-contracts recorded as blocked on this card; canonical-id now resolves it and is deliberately one rule for own ids and references alike, so a local ref and a qualified ref compare equal. An already-qualified id keeps its own namespace over the manifest's, and with no namespace in scope a bare id is left alone rather than qualified under nil (which would strip identity rather than add it).

Verification: shadow-cljs compile test 831 tests / 2476 assertions, 0 failures 0 errors (+22 tests, +66 assertions over card 1); compile server 0 warnings; clj-kondo 194 warnings / 0 errors, identical to the main baseline, none in the new files.

Post-review 2026-08-13: all five Codex findings on PR #230 actioned in one follow-up commit, none deferred. Two were integration bugs my own tests could not have caught, because they build resource maps directly and never exercise the loader: (1) namespace-resource-record validated expanded manifest definitions BEFORE canonicalization, so every manifest-sourced document/garden/publication failed the qualified-id shapes and was dropped by the loader, leaving the facade serving an empty topology — my commit message identified the local-id problem correctly but fixed it downstream of the drop; (2) dedup-contracts is first-wins on [class id], so two files declaring one canonical id with different payloads collapsed to whichever the filesystem enumerated first, making index-canonical!'s deterministic conflict detection unreachable through the real load path. Fixed by canonicalizing before validation and by adding load-all-contract-records!/load-all-resource-records!, an undeduped path used only by the publication facade; dedup-contracts is untouched for every other kind, and load-all-contracts! is now expressed in terms of the undeduped path so they cannot drift.

(3) send-json! serializes with clj->js, which renders keywords with name, so :knoxx.docs/translation-pipeline reached the CMS as "translation-pipeline" and distinct namespaces collapsed onto one wire id. Keyword values now encode as namespace/name with no EDN colon; map keys stay unqualified per this codebase's documented wire convention, and the test pins the exact key set so the convention is asserted rather than assumed. This is the encoding knoxx-cms-resource-backed-publication-ui specifies, arriving early — that card can now consume shape.resource-identity/encode-keyword, decode-keyword and encode-wire-values instead of defining its own.

(4) dangling references returned a successful but incomplete topology: a missing document silently vanished because list-document-views iterates the documents it has, and a missing garden passed through because hydration validates only the document. reference-blockers now surfaces both deterministically and the adapter maps them to 409. (5) Malli maps are open, so execution facts attached to an otherwise-valid resource survived into the projection; the projection now selects declared fields, since a schema alone cannot enforce 'resources declare desired state only'.

The canonical identity rule moved to knoxx.backend.shape.resource-identity so the loader and resolver consume one implementation rather than restating it on either side of the load boundary.

Verification after fixes: 846 tests / 2549 assertions on this branch, 0 failures 0 errors; compile server 0 warnings; clj-kondo 194 warnings / 0 errors (main baseline) after extracting index-one, assert-no-conflicts! and assert-references-resolve! to clear a function-length warning the blocker check introduced.

---