---
uuid: "knoxx-openplanner-publication-state-migration"
title: "Migrate existing OpenPlanner garden/publication intent into Knoxx resources"
status: incoming
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

## Scope

- Inventory current garden rows and document `garden_publications` metadata used by CMS.
- Convert semantic fields into `garden`, `document`, and `publication` resources.
- Separate operational observations (`published_at`, job/run ids, adapter timestamps) into migration receipts rather than resource data.
- Detect ambiguous/conflicting source rows and emit a report requiring explicit resolution; do not apply "last write wins".
- Make migration idempotent and restart-safe.
- Preserve stable content/document identity when source paths or titles have changed.
- After successful migration, disable any compatibility path that can write OpenPlanner publication metadata as a competing authority.

## CLJS pseudocode

```clojure
(ns knoxx.backend.domain.publication-migration)

(defn garden->resource [row]
  {:garden/id (canonical-garden-id row)
   :garden/title (:title row)
   :garden/status (if (= "archived" (:status row)) :archived :active)})

(defn publication->resource [document row]
  {:publication/id (canonical-publication-id document row)
   :publication/document (:document/id document)
   :publication/garden (canonical-garden-id row)
   :publication/locale (keyword (or (:locale row) "en"))
   :publication/revision (or (:revision row) :source/current)
   :publication/state (if (:published row) :published :withheld)
   :publication/path (:path row)
   :translation/review (if (:review-required row) :required :none)})

(defn migrate-record [resource-index source-record]
  (let [candidate (publication->resource
                   (resolve-document source-record)
                   source-record)]
    (cond
      (publication/conflicts? resource-index candidate)
      {:migration/status :conflict
       :candidate candidate
       :source source-record}

      (publication/equivalent? resource-index candidate)
      {:migration/status :noop
       :resource/id (:publication/id candidate)}

      :else
      {:migration/status :write
       :resource candidate})))
```

Migration runner sketch:

```clojure
(doseq [record (legacy/read-publication-records! ctx)]
  (let [decision (migration/migrate-record resource-index record)]
    (case (:migration/status decision)
      :write    (resources/write! ctx (:resource decision))
      :conflict (receipts/append! ctx decision)
      :noop     nil)))
```

## Laws

- Re-running migration over unchanged legacy data produces no new semantic resources.
- Unknown/ambiguous locale, path, garden, or document identity becomes a conflict receipt, not guessed contract data.
- Removing legacy operational timestamps from input cannot change generated desired-state resources.
- Once cut over, migration is retired; no bidirectional sync remains.

## Done when

- Existing publish topology can be reconstructed as validated Knoxx resources.
- Conflicts are enumerated explicitly with source evidence.
- The same migration run twice yields identical resource state and no duplicate publications.
