---
uuid: "knoxx-openplanner-publication-state-migration"
title: "Migrate existing OpenPlanner garden/publication intent into Knoxx resources"
status: accepted
priority: P1
labels: ["tasks", "5sp", "has-parent", "publication", "migration", "openplanner"]
created_at: "2026-08-12T00:00:00Z"
points: 5
category: tasks
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
- Validate legacy garden status, source/target locale, path, garden identity, document identity, and revision information before constructing resources; missing or unrecognized semantic values become conflicts rather than defaults.
- Separate operational observations (`published_at`, job/run ids, adapter timestamps) into migration receipts rather than resource data.
- Detect ambiguous/conflicting source rows and emit a report requiring explicit resolution; do not apply "last write wins".
- Make migration idempotent and restart-safe both for resource writes and conflict receipts.
- Update the in-memory migration index after each successful write so later legacy rows are checked against state produced earlier in the same run.
- Preserve stable content/document identity when source paths or titles have changed.
- After successful migration, disable any compatibility path that can write OpenPlanner publication metadata as a competing authority.

## CLJS pseudocode

```clojure
(ns knoxx.backend.domain.publication-migration)

(defn decode-garden-status [row]
  (case (:status row)
    "active"   {:ok :active}
    "archived" {:ok :archived}
    {:conflict :unknown-garden-status
     :value (:status row)}))

(defn decode-locale [field row]
  (let [value (get row field)
        locale (when (and (string? value) (seq value)) (keyword value))]
    (if (and locale (law/valid? publication/Locale locale))
      {:ok locale}
      {:conflict :unknown-locale
       :field field
       :value value})))

(defn garden->decision [row]
  (let [status (decode-garden-status row)]
    (if-let [reason (:conflict status)]
      {:migration/status :conflict
       :reason reason
       :source row}
      {:migration/status :candidate
       :resource {:garden/id (canonical-garden-id row)
                  :garden/title (:title row)
                  :garden/status (:ok status)}})))

(defn publication->decision [document row]
  (let [locale (decode-locale :locale row)]
    (cond
      (:conflict locale)
      {:migration/status :conflict
       :reason (:conflict locale)
       :source row}

      (not (valid-publication-path? (:path row)))
      {:migration/status :conflict
       :reason :invalid-publication-path
       :source row}

      :else
      {:migration/status :candidate
       :resource
       {:publication/id (canonical-publication-id document row)
        :publication/document (:document/id document)
        :publication/garden (canonical-garden-id row)
        :publication/locale (:ok locale)
        :publication/revision (or (validated-revision row)
                                  :source/current)
        :publication/state (if (:published row) :published :withheld)
        :publication/path (:path row)
        :translation/review (if (:review-required row) :required :none)}})))

(defn migrate-record [resource-index source-record]
  (let [candidate-decision
        (publication->decision (resolve-document source-record) source-record)]
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

Stable receipt identity and stateful migration fold:

```clojure
(defn migration-receipt-key [source-record]
  [:openplanner/publication-migration
   (:source/collection source-record)
   (:source/id source-record)])

(loop [idx resource-index
       records (legacy/read-publication-records! ctx)]
  (when-let [record (first records)]
    (let [decision (migration/migrate-record idx record)]
      (case (:migration/status decision)
        :write
        (let [written (resources/write! ctx (:resource decision))]
          (recur (publication/index-resource idx written) (rest records)))

        :conflict
        (do (receipts/append-once! ctx
                                   (migration-receipt-key record)
                                   decision)
            (recur idx (rest records)))

        :noop
        (recur idx (rest records))))))
```

Garden migration uses the same decision/receipt discipline. Document migration must likewise refuse to invent required `:document/source-locale`; missing/ambiguous source locale is a conflict that must be resolved before CMS cutover.

## Laws

- Re-running migration over unchanged legacy data produces no new semantic resources.
- Unknown/ambiguous source locale, target locale, status, path, garden, or document identity becomes a conflict receipt, not guessed contract data.
- Conflict receipts have stable source-record keys; reruns do not duplicate them.
- Successful writes immediately enter the migration index before the next source record is classified.
- Removing legacy operational timestamps from input cannot change generated desired-state resources.
- Once cut over, migration is retired; no bidirectional sync remains.

## Done when

- Existing publish topology can be reconstructed as validated Knoxx resources before the CMS authority cutover.
- Conflicts are enumerated explicitly with source evidence and no defaulted semantic values.
- Two source rows mapping to the same publication are reconciled against the updated in-run index rather than both being blindly written.
- The same migration run twice yields identical resource state and no duplicate publications or conflict receipts.
