---
uuid: "knoxx-translations-event-sourced"
title: "Preserve translation attempts as append-only history instead of destructive upserts"
status: incoming
priority: P2
labels: ["tasks", "5sp", "has-parent", "translations", "transduction", "events"]
created_at: "2026-08-04T00:00:00Z"
points: 5
category: tasks
---
# Preserve translation attempts as append-only history instead of destructive upserts

> Parent epic: `knoxx-transduction-provider-pipeline`

## Purpose

A translation is a transduction attempt and must remain durable evidence. Today a
re-translation destructively replaces the previous segment value, so provenance,
comparison, evaluation, and later training data lose the candidate that actually existed
at that point in time.

Make translation attempt history append-only. The current translation presented to a
consumer is a projection over that history, not the history itself.

## Verified as of 2026-08-04

`extern/openplanner_translation_mongo/segments.cljs` `upsert-segment!` is a
`findOneAndUpdate` with `$set` and `:upsert true`, keyed on a tenant-scoped segment
identity. `create-segment!` reads the existing row, computes `modified?`, and overwrites
when the content differs.

This is why `save_translation` is declared `destructiveHint: true` in
`law.mcp-tool-annotations` — an accurate description of today's behavior, and a hint that
should become `false` when this card lands.

## Scope

- Append one immutable translation/transduction-attempt event per successful candidate
  save rather than mutating the historical record.
- Bind every attempt to the exact source segment/revision and target locale it transformed.
- Use one explicit grouping key for events and current projections:
  `{:org-id org_id :document-id document_id :segment-index segment_index
  :target-lang target_lang}`. The persisted field spellings may remain provider-native at
  the edge, but no event, deduplication key, projection read, or projection write may omit
  or reinterpret any member.
- Record candidate/provenance identity sufficient for the evaluation system to review a
  specific candidate rather than "whatever is current now".
- Derive current segment state from events as a projection.
- Keep reads fast: `/api/translations/segments` is on the deploy health gate, so a
  materialized/current projection is likely required rather than folding all events per
  request.
- Migrate existing segment rows into an initial event per segment, preserving
  `created_at`/`updated_at` as best-known historical evidence.
- Update `save_translation`'s annotation to non-destructive once it is true.

## Contract obligations

Settle these before migration:

| obligation | why |
|---|---|
| immutable attempt/event id | a retried save must be recognized as the same event, not appended twice |
| source artifact identity | the candidate must remain bound to the exact input it transformed |
| segment/candidate grouping key | exactly `(org_id, document_id, segment_index, target_lang)` for event grouping and every projection read/write |
| ordering rule | concurrent candidates must fold deterministically; wall clock is not enough |
| retry behavior | equal canonical events for one attempt identity return the existing event; any payload difference conflicts and preserves the original |
| projection recovery | define what happens when append succeeds but projection update fails |

An attempt identity is the composite grouping key plus its immutable attempt/event id;
source revision and candidate identity remain immutable facts on that event. Tests must
use the same document and segment index across two organizations and across two target
languages, then prove their histories, deduplication, candidates, and current projections
never merge.

Attempt admission is one atomic unique-insert/compare operation on that composite identity,
not a read followed by an append. A retry whose complete canonical validated event equals the
stored event returns the original event without another append. Any difference—including the
source revision, candidate identity/content, provider/model identity, parameters, provenance,
or evidence—returns
`{:error/type :translation/conflict :error/reason :attempt-id-reused
:attempt/id <attempt-id> :event/grouping-key <grouping-key>}`, leaves the original event and
projection authoritative, and appends nothing. When different events race for one attempt
identity, exactly one is stored and the other returns that conflict; equal concurrent retries
store one event and both callers observe it.

## Boundary rules

- This card owns **history of produced candidates**, not SME approval/rejection history;
  evaluation receipts belong to `knoxx-evaluation-review-system`.
- Publication consumes translation/evaluation evidence but is not the owner of either log.
- Provider invocation belongs to `knoxx-translation-transduction-boundary`; persistence is
  downstream of a successful candidate result.
- A materialized current view is disposable/rebuildable state, never the semantic authority.

## Watch out

The deploy health gate requires `/api/translations/segments?limit=1` to answer 200. Do not
land a migration that leaves that endpoint erroring.

## Done when

- Saving/retranslating the same logical segment preserves every distinct candidate attempt.
- Retrying the same attempt id with an equal canonical event is idempotent; any payload
  difference conflicts with the original event preserved. Concurrent equal and conflicting
  retries prove the unique-insert/compare operation is atomic and only one event exists.
- Cross-organization and cross-target-language fixtures with otherwise identical document
  and segment coordinates remain isolated in both append history and current projection.
- Current-state reads remain compatible from a caller's perspective while deriving from
  history.
- An evaluation receipt can bind to a candidate that remains addressable after a newer
  translation exists.
- `save_translation` is honestly non-destructive, with the MCP annotation updated.
