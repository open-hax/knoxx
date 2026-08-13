---
category: "tasks"
labels: ["tasks", "5sp", "has-parent", "publication", "migration", "openplanner"]
write-id: "1786565793371-0.95x6g3ahwhph3fyeud"
points: "5"
title: "Migrate existing OpenPlanner garden/publication intent into Knoxx resources"
priority: "P0"
status: "ready"
uuid: "knoxx-openplanner-publication-state-migration"
created_at: "2026-08-12T00:00:00Z"
---

# Migrate existing OpenPlanner garden/publication intent into Knoxx resources

> Parent epic: `knoxx-contract-owned-publication-pipeline`

## Purpose

Perform the one-time authority transfer from existing OpenPlanner garden/publication metadata to Knoxx resources without silently losing or inventing desired state.

This is not an ongoing synchronization system. After cutover, resources are authoritative and OpenPlanner is an adapter/projection only.

This migration and explicit conflict resolution land **before** the CMS resource-authority cutover.

## Scope

- Inventory current garden rows and document `garden_publications` metadata used by CMS.
- Convert semantic fields into `garden`, `document`, and `publication` resources.
- Validate every legacy semantic field before constructing resources: garden status, source/target locale, path, garden identity, document identity, revision selector/value, requested publication state, and review policy. Missing, malformed, or unrecognized values become conflicts rather than defaults.
- Reuse `knoxx.backend.law.publication/valid-publication-path?` for path validation so migrated data and directly authored resource data obey the exact same route law.
- Normalize each legacy representation into a **named, validated source shape** before decoding. `CmsDocMetadata.garden_publications` entries carry only `{garden_id}` and membership in that array is the legacy published fact, so a decoder that demands an explicit `:published` boolean would turn every existing association into a conflict and leave the resource topology empty.
- Decode per declared shape, and state for each shape which semantic fields it carries explicitly and which the shape itself defines. An undeclared shape is a conflict; it is never decoded by guessing.
- In particular, do **not** infer `:source/current` from a merely missing revision on a shape that is supposed to carry one, and do **not** coerce truthy/falsy values into publication state. Where a shape defines the selector or the state (legacy membership defines both), that definition is documented on the shape rather than derived from absence.
- Review policy is not represented by the membership shape at all: it must be supplied as an explicit declared migration policy for the run, and a run without one yields conflicts rather than defaulted review semantics.
- Separate operational observations (`published_at`, job/run ids, adapter timestamps) into migration receipts rather than resource data.
- Detect ambiguous/conflicting source rows and emit a report requiring explicit resolution; do not apply "last write wins".
- Make migration idempotent and restart-safe both for resource writes and conflict receipts.
- Update the in-memory migration index after each successful write so later legacy rows are checked against state produced earlier in the same run.
- Preserve stable content/document identity when source paths or titles have changed.
- After successful migration, disable any compatibility path that can write OpenPlanner publication metadata as a competing authority.

## CLJS pseudocode

```clojure
(ns knoxx.backend.domain.publication-migration)

(defn conflict [reason field value row]
  {:migration/status :conflict
   :reason reason
   :field field
   :value value
   :source row})

(defn decode-garden-status [row]
  (case (:status row)
    "active"   {:ok :active}
    "archived" {:ok :archived}
    (conflict :unknown-garden-status :status (:status row) row)))

(defn decode-locale [field row]
  (let [value  (get row field)
        locale (cond
                 (keyword? value) value
                 (and (string? value) (seq value)) (keyword value))]
    (if (and locale (law/valid? publication/Locale locale))
      {:ok locale}
      (conflict :unknown-locale field value row))))
```

Decoders are **shape-aware**. The legacy source does not have one uniform publication
row: `CmsDocMetadata.garden_publications` entries are shaped only as `{garden_id}`
(`frontend/src/pages/CmsPage.tsx:38-40`), and the CMS treats membership in that array as
the published fact (lines 299-305). A decoder that demanded an explicit `:published`
boolean would therefore classify every existing association as a conflict and the
migration could never populate the topology cutover depends on.

Normalization runs first and turns each legacy representation into a named, validated
source shape. Membership is decoded as publication because that array *is* the legacy
representation of publication — not because a missing field is being treated as truthy:

```clojure
(def GardenMembershipEntry
  [:map [:garden_id publication/NonBlankString]])

(defn normalize-membership-entry [document entry]
  (if (law/valid? GardenMembershipEntry entry)
    {:ok {:source/shape :garden-membership
          :source/collection :cms-doc-garden-publications
          :source/id [(:legacy/doc-id document) (:garden_id entry)]
          :garden-id (:garden_id entry)
          :path (:source_path document)
          :locale (:document/source-locale document)}}
    (conflict :unresolvable-garden-membership :garden_id (:garden_id entry) entry)))

(defn normalize-publication-rows [document]
  (mapv #(normalize-membership-entry document %)
        (get-in document [:metadata :garden_publications] [])))
```

A source shape that is not declared is a conflict; it is never decoded by guessing.
Each declared shape names exactly which semantic fields it carries explicitly and which
ones the shape itself defines:

```clojure
(defn decode-source-shape [row]
  (case (:source/shape row)
    :garden-membership {:ok :garden-membership}
    :explicit-publication-row {:ok :explicit-publication-row}
    (conflict :unknown-publication-source-shape
              :source/shape (:source/shape row) row)))

(defn decode-revision [row]
  (case (:source/shape row)
    ;; Legacy membership republished whatever the live document held; that selector is
    ;; what the shape means, not an inference drawn from an absent field.
    :garden-membership {:ok :source/current}

    :explicit-publication-row
    (let [value (:revision row)]
      (cond
        (= :source/current value) {:ok :source/current}
        (and (string? value) (seq value)) {:ok value}
        :else (conflict :invalid-publication-revision :revision value row)))))

(defn decode-publication-state [row]
  (case (:source/shape row)
    ;; Presence in `garden_publications` is the legacy published fact.
    :garden-membership {:ok :published}

    :explicit-publication-row
    (if (and (contains? row :published)
             (boolean? (:published row)))
      {:ok (if (:published row) :published :withheld)}
      (conflict :invalid-published-state :published (:published row) row))))
```

Review policy is the one semantic field the membership shape genuinely does not
represent, so it is neither guessed nor defaulted: the migration run must be given an
explicit review policy, and a run without one produces conflicts instead of resources.

```clojure
(defn decode-review-policy [migration-policy row]
  (case (:source/shape row)
    :garden-membership
    (if-let [declared (:migration/membership-review migration-policy)]
      (if (contains? #{:required :none} declared)
        {:ok declared}
        (conflict :invalid-review-policy
                  :migration/membership-review declared row))
      (conflict :undeclared-membership-review-policy
                :migration/membership-review nil row))

    :explicit-publication-row
    (if (and (contains? row :review-required)
             (boolean? (:review-required row)))
      {:ok (if (:review-required row) :required :none)}
      (conflict :invalid-review-policy
                :review-required
                (:review-required row)
                row))))
```

Garden and publication decisions consume the normalized rows:

```clojure
(defn garden->decision [row]
  (let [status (decode-garden-status row)]
    (if (:migration/status status)
      status
      {:migration/status :candidate
       :resource {:garden/id (canonical-garden-id row)
                  :garden/title (:title row)
                  :garden/status (:ok status)}})))

(defn publication->decision [migration-policy document row]
  (let [shape    (decode-source-shape row)
        locale   (decode-locale :locale row)
        revision (decode-revision row)
        state    (decode-publication-state row)
        review   (decode-review-policy migration-policy row)
        invalid  (some #(when (:migration/status %) %)
                       [shape locale revision state review])]
    (cond
      invalid
      invalid

      (not (publication/valid-publication-path? (:path row)))
      (conflict :invalid-publication-path :path (:path row) row)

      :else
      {:migration/status :candidate
       :resource
       {:publication/id (canonical-publication-id document row)
        :publication/document (:document/id document)
        :publication/garden (canonical-garden-id row)
        :publication/locale (:ok locale)
        :publication/revision (:ok revision)
        :publication/state (:ok state)
        :publication/path (:path row)
        :translation/review (:ok review)}})))

(defn migrate-record [migration-policy resource-index source-record]
  (let [candidate-decision
        (publication->decision migration-policy
                               (resolve-document source-record)
                               source-record)]
    (if (= :conflict (:migration/status candidate-decision))
      candidate-decision
      (let [candidate (:resource candidate-decision)]
        (cond
          (publication/conflicts? resource-index candidate)
          {:migration/status :conflict
           :reason :publication-conflict
           :candidate candidate
           :source source-record}

          (publication/equivalent? resource-index candidate)
          {:migration/status :noop
           :resource/id (:publication/id candidate)}

          :else
          {:migration/status :write
           :resource candidate})))))
```

Stable receipt identity and stateful migration fold. The fold is effectful — the
filesystem-backed resource writer and the receipt appender both return Promises — so it
is an `^:async` function that awaits the legacy read, every write, and every receipt
append before recurring. Without the awaits `written` would be a Promise rather than the
saved resource, `index-resource` would index that Promise, and the promised in-run index
update and restart-safe sequencing would both be lost:

```clojure
(defn migration-receipt-key [source-record]
  [:openplanner/publication-migration
   (:source/collection source-record)
   (:source/id source-record)])

(defn ^:async migrate-publication-records! [ctx migration-policy resource-index]
  (let [records (await (legacy/read-publication-records! ctx))]
    (loop [idx     resource-index
           records (seq records)]
      (if-let [record (first records)]
        (let [decision (migration/migrate-record migration-policy idx record)]
          (case (:migration/status decision)
            :write
            (let [written (await (resources/write! ctx (:resource decision)))]
              (recur (publication/index-resource idx written) (rest records)))

            :conflict
            (do (await (receipts/append-once! ctx
                                              (migration-receipt-key record)
                                              decision))
                (recur idx (rest records)))

            :noop
            (recur idx (rest records))))
        idx))))
```

`loop`/`recur` stays lawful here because every `await` resolves before the recursion
point; the recursion is over already-settled state, not over pending Promises.

Garden migration uses the same decision/receipt discipline. Document migration must likewise refuse to invent required `:document/source-locale`; missing/ambiguous source locale is a conflict that must be resolved before CMS cutover.

## Laws

- Re-running migration over unchanged legacy data produces no new semantic resources.
- Unknown/ambiguous source locale, target locale, status, path, garden, document identity, revision, publication state, or review policy becomes a conflict receipt, not guessed contract data.
- Migration path validation is exactly the authoritative `publication/valid-publication-path?` law used for directly authored publication resources.
- Every legacy row is normalized into a declared source shape before any semantic decoding; an unrecognized shape is a conflict.
- A shape that is declared to carry an explicit revision or publication state must carry a valid one; absence is a conflict, and truthiness/falsiness is never a decoder.
- Where a declared shape defines the selector or the state — legacy garden membership defines `:source/current` and `:published` — that definition is the shape's documented meaning, and it applies only to rows normalized into that shape.
- The migration fold awaits the legacy read, every resource write, and every receipt append; an unresolved Promise never enters the in-run index.
- Conflict receipts have stable source-record keys; reruns do not duplicate them.
- Successful writes immediately enter the migration index before the next source record is classified.
- Removing legacy operational timestamps from input cannot change generated desired-state resources.
- Once cut over, migration is retired; no bidirectional sync remains.

## TDD plan

Test namespace: `knoxx.backend.domain.publication-migration-test`
(`backend/test/cljs/knoxx/backend/domain/publication_migration_test.cljs`).

Normalization first — these are the tests the review thread demanded:

1. `membership-entry-normalizes-to-published-candidate` — a legacy document whose
   `metadata.garden_publications` is `[{:garden_id "garden-a"}]` normalizes to a
   `:garden-membership` row and migrates to a validated publication resource
   with `:publication/state :published` and
   `:publication/revision :source/current`. It must NOT be a conflict.
2. `membership-entry-without-garden-id-conflicts` — entries with a missing,
   blank, or non-string `garden_id` become
   `:unresolvable-garden-membership` conflicts.
3. `undeclared-source-shape-conflicts` — a row with no `:source/shape` yields
   `:unknown-publication-source-shape`; nothing is guessed.
4. `membership-review-policy-must-be-declared` — a run whose migration policy
   omits `:migration/membership-review` yields
   `:undeclared-membership-review-policy` conflicts; supplying `:required`
   produces `:translation/review :required` on the resource.
5. `explicit-row-still-requires-explicit-fields` — on
   `:explicit-publication-row`, a missing revision, a non-boolean `:published`,
   and a truthy-but-not-boolean `:published` each conflict.
6. `migration-path-law-is-shared` — the same malformed path table used by
   `knoxx.backend.law.publication-test` is rejected by migration through
   `publication/valid-publication-path?`, asserted by calling the same predicate
   var rather than a copy.
7. `missing-source-locale-conflicts` — a document with no resolvable source
   locale conflicts instead of defaulting.

Fold behaviour second — the async/await regression:

8. `^:async fold-awaits-promise-returning-writer` — a fake writer that returns
   `(js/Promise.resolve resource)` and a fake receipt appender that returns a
   Promise. Assert the index after the run contains saved resource maps, not
   Promise objects (`(is (map? (get-in idx [...])))`).
9. `^:async second-row-reconciles-against-in-run-index` — two legacy rows
   mapping to the same publication identity: the second is classified against
   the first row's written state, producing `:noop` or `:conflict`, never two
   blind writes.
10. `^:async rerun-is-idempotent` — running the fold twice over unchanged legacy
    data yields identical resource state, no duplicate publications, and no
    duplicate conflict receipts (stable receipt keys).
11. `operational-fields-are-receipts-only` — deleting `published_at` and job ids
    from the input does not change any generated resource.

Then implement `knoxx.backend.domain.publication-migration` and the `^:async`
fold until green.

## Done when

- Existing publish topology can be reconstructed as validated Knoxx resources before the CMS authority cutover.
- Conflicts are enumerated explicitly with source evidence and no defaulted semantic values.
- A fixture of the real legacy shape — a document whose `metadata.garden_publications` is `[{garden_id "garden-a"}]` — migrates into a validated published publication resource, not a conflict.
- A membership entry with a missing or blank `garden_id` is a conflict, and a run with no declared membership review policy yields conflicts rather than defaulted review semantics.
- Fixtures prove that on shapes declared to carry them, missing/invalid revision and non-boolean/missing publish state produce conflicts rather than resources.
- A fixture with a Promise-returning resource writer proves the fold indexes saved resources rather than Promises: a second legacy row for the same publication identity is reconciled against the first row's written state within a single run.
- Direct-resource and migration fixtures reject the same malformed publication paths through the same shared predicate.
- Two source rows mapping to the same publication are reconciled against the updated in-run index rather than both being blindly written.
- The same migration run twice yields identical resource state and no duplicate publications or conflict receipts.

---
Ready gate 2026-08-12: sized 5sp (<=5, eligible to implement). Walked accepted -> breakdown -> ready via the Rheos promethean FSM. Scope, laws and acceptance criteria confirmed on the card; TDD plan section names the failing tests to write first. Risk: the run needs an explicitly declared membership review policy before it can produce resources; that is now a conflict rather than a default.
***

Pre-implementation review 2026-08-13 (CodeRabbit, not yet actioned — this card is still `ready`, not started): `GardenMembershipEntry`'s `normalize-membership-entry` references `publication/NonBlankString`, which `knoxx.backend.law.publication` does not declare — define/export it there or point at an existing contract before implementing. Also, `migrate-publication-records!` is sketched inside a `domain.*` namespace but performs I/O (reads legacy records, writes resources, appends receipts); keep `migrate-record` and decision logic in `domain.*` and move the effectful fold itself to `infra.*`/orchestration.
***