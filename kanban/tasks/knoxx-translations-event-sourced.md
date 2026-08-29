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
`law.mcp-tool-annotations` — an accurate description of today's behavior. Append-only event
history does not by itself make the hint false: this operation still advances/replaces the
mutable current projection. Retain the hint while the tool may overwrite that state; only a
separate operation that cannot change or remove any current authority can claim `false`.

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
- Keep `save_translation`'s annotation honest: it remains destructive while the operation
  advances the current projection, even though attempt history is append-only.

## Contract obligations

Settle these before migration:

| obligation | why |
|---|---|
| immutable attempt/event id | a retried save must be recognized as the same event, not appended twice |
| source artifact identity | the candidate must remain bound to the exact input it transformed |
| segment/candidate grouping key | exactly `(org_id, document_id, segment_index, target_lang)` for event grouping and every projection read/write |
| ordering rule | the store atomically assigns a unique monotonic ordinal per grouping key; wall clock never selects current authority |
| retry behavior | equal canonical events for one attempt identity return the existing event; any payload difference conflicts and preserves the original |
| projection recovery | define what happens when append succeeds but projection update fails |

An attempt identity is the composite grouping key plus its immutable attempt/event id;
source revision and candidate identity remain immutable facts on that event. Tests must
use the same document and segment index across two organizations and across two target
languages, then prove their histories, deduplication, candidates, and current projections
never merge.

Every upstream reservation for that attempt—including resolved translation configuration—uses
this same composite `AttemptIdentity`. The raw caller id is not globally unique. Reusing it for
another segment or target language creates an independent attempt; reusing the same composite
identity with changed source, candidate, request, or configuration facts conflicts. Config
admission and event admission may not disagree about that identity boundary.

The production `save_translation` MCP input schema exposes a required stable `attempt_id`
(idempotency key) created by the initiating workflow/caller and reused after timeout or lost
response. The tool handler validates it, passes it unchanged through the domain/save boundary,
and returns it with the admitted event/ordinal; it may not discard the value, mint a replacement
per invocation, substitute the transport `_tool-call-id`, or derive identity from content.
Organization scope remains server-derived and combines with this value at event admission.
Distinct attempt ids with byte-equivalent content intentionally remain distinct attempts.

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

Every accepted new attempt receives the next unique monotonic ordinal for its grouping key in
the same atomic operation that appends the event. A materialized projection stores its last
applied ordinal and advances only for a higher ordinal; delayed/out-of-order workers cannot
move it backward. Replay folds events in ordinal order and must reproduce byte-equivalent
current state. If append succeeds but projection update fails, the durable event remains
authoritative and an idempotent replay/recovery step advances the projection from its
checkpoint without appending another attempt.

Legacy migration derives a deterministic initial event id only from stable legacy row identity
and the complete grouping coordinates, never from mutable source/candidate values or revision
facts. The canonical migrated event payload contains that observed mutable snapshot. Re-running
migration returns the equal existing event; a row changed after a partial migration therefore
reuses the same derived id with a different canonical payload and stops with the normal
`:attempt-id-reused` conflict for operator reconciliation instead of appending a second initial
event. Migration writes the event before the projection, resumes from durable checkpoints, and
keeps the existing read endpoint available until every migrated grouping key can be served from
the new projection.

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
- Real-boundary MCP tests simulate a lost response after commit, retry `save_translation` with
  the same `attempt_id`, and observe the same single event/ordinal. Reusing that id with changed
  content conflicts, while two distinct ids with equal content preserve two intentional
  attempts. The schema/handler proof fails if the id is absent, regenerated, or discarded.
- Cross-organization and cross-target-language fixtures with otherwise identical document
  and segment coordinates remain isolated in both append history and current projection.
- The same raw `attempt_id` reused across two segments and two target languages produces
  independent config reservations/events, while changed reuse inside one composite grouping
  conflicts consistently at config and event admission.
- Current-state reads remain compatible from a caller's perspective while deriving from
  history.
- Concurrent distinct attempts receive unique per-key ordinals; out-of-order projection
  delivery cannot regress current state, and replay/recovery is byte-equivalent and
  idempotent after an injected append/projection split failure.
- Running legacy migration twice appends no duplicates; changed legacy authority at a reused
  stable row/grouping-derived migration identity conflicts instead of being silently replaced.
  A partial-run fixture mutates the legacy payload before restart and proves no second initial
  event can be admitted.
- An evaluation receipt can bind to a candidate that remains addressable after a newer
  translation exists.
- `save_translation` retains `destructiveHint: true` while it can replace current projection
  state; an annotation regression test prevents append-only history from being mistaken for a
  non-destructive public operation.
