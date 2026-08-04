---
uuid: "knoxx-translations-event-sourced"
title: "Make translations append-only / event sourced instead of an overwriting upsert"
status: incoming
priority: P2
labels: ["tasks", "5sp", "has-parent", "translations"]
created_at: "2026-08-04T00:00:00Z"
points: 5
category: tasks
---
# Make translations append-only / event sourced

> Parent epic: `knoxx-decouple-into-katamorph-contracts`

## Purpose

Translations should be event sourced. They are currently a destructive upsert, so
a re-translation silently replaces the previous one and the history is gone.

## Verified as of 2026-08-04

`extern/openplanner_translation_mongo/segments.cljs` `upsert-segment!` is a
`findOneAndUpdate` with `$set` and `:upsert true`, keyed on a tenant-scoped
segment identity. `create-segment!` reads the existing row, computes
`modified?`, and overwrites when the content differs.

This is why `save_translation` is declared `destructiveHint: true` in
`law.mcp-tool-annotations` — an accurate description of today's behaviour, and a
hint that should become `false` when this card lands.

## Scope

- Append a translation **event** per save rather than mutating a segment row.
- Derive the current segment state from its events — projection, not truth.
- Keep reads fast: `/api/translations/segments` is on the deploy health gate, so
  a projection/materialised view is likely required rather than folding events
  per request.
- Migrate existing segment rows into an initial event per segment, preserving
  `created_at`/`updated_at` as best known.
- Update `save_translation`'s annotation to non-destructive once it is true, and
  drop the destructive justification from `law.mcp-tool-annotations`.

## Contract obligations

- The event shape belongs in `law.*` with a Malli schema, and the projection
  should assert against it. This is a good candidate for the first genuinely
  Katamorph-shaped store in Knoxx.

## Watch out

- The deploy health gate requires `/api/translations/segments?limit=1` to answer
  200. Do not land a migration that leaves that endpoint erroring, or every
  deployment fails — see the Knoxx verify.sh translation probe.

## Done when

- Saving the same segment twice preserves both versions.
- The current-state read is unchanged from a caller's perspective.
- `save_translation` is honestly non-destructive, with the annotation updated.
