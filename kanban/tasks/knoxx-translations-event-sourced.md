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
> Coupled identity contracts: `knoxx-ingestion-scoped-service-identity-handoff` (#287) and
> `knoxx-versioned-resolved-translation-config` (#275)

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
- Name one complete immutable `SegmentCoordinate` for event grouping, current projections, #287
  effect fences, and #275 attempt admission:
  `{:org-id org_id :project project :garden-id garden_id :document-id document_id
  :source-lang source_lang :source-revision source_revision
  :source-span {:start source_start :end source_end :slice-digest source_slice_digest}
  :segment-index segment_index :target-lang target_lang}`. The authoritative source span/slice
  identity exact-matches canonical source bytes at that revision. The persisted field spellings may
  remain provider-native at the edge, but no grouping, projection read, or projection write may omit
  or reinterpret organization, project, garden, document, source language, source revision,
  authoritative source span/slice, segment index, or target language.
- Define `AttemptIdentity` as `(SegmentCoordinate, stable attempt/event id)`. The event unique index
  uses `AttemptIdentity`, while the current projection unique index and per-coordinate ordinal use
  `SegmentCoordinate`. Multiple intentional retranslations therefore share one segment coordinate
  but remain distinct events under different stable attempt ids; the highest admitted ordinal for
  that `SegmentCoordinate` alone advances its current projection.
- Every pre-admitted turn member adds a stable effect id to its `AttemptIdentity`, forming
  `AttemptEffectIdentity = (AttemptIdentity, stable effect id)`. A #287 delegated attempt
  additionally binds the admitting manifest id/digest and Knoxx-owned authority epoch as immutable
  canonical facts, and its exact-once ledger claims `AttemptEffectIdentity`, never
  `SegmentCoordinate` globally. Equal retries compare manifest, epoch, effect identity, and canonical
  payload; substitution conflicts without another event or projection write.
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
| --- | --- |
| immutable attempt/event id | a retried save must be recognized as the same event, not appended twice |
| source artifact identity | the candidate must remain bound to the exact input it transformed |
| segment/candidate grouping key | `SegmentCoordinate` is the complete immutable coordinate `(org_id, project, garden_id, document_id, source_lang, source_revision, authoritative source span/slice identity+digest, segment_index, target_lang)` for event grouping and every projection read/write |
| event uniqueness | the event unique index is `AttemptIdentity = (SegmentCoordinate, stable attempt/event id)` so intentional retranslations do not alias |
| projection uniqueness | the current projection unique index and ordinal stream use `SegmentCoordinate`; the legacy short key is migrated, never retained as an aliasing authority |
| ordering rule | the store atomically assigns a unique monotonic ordinal per grouping key; wall clock never selects current authority |
| retry behavior | equal canonical events for one attempt identity return the existing event; any payload difference conflicts and preserves the original |
| projection recovery | define what happens when append succeeds but projection update fails |

An `AttemptIdentity` is the complete immutable `SegmentCoordinate` plus its immutable attempt/event
id. Every pre-admitted member adds its immutable stable effect id to form `AttemptEffectIdentity`.
For #287 work the canonical attempt additionally exact-matches manifest id/digest and authority
epoch on every retry. Candidate identity remains an immutable fact on that event. Tests must reuse
the same document and segment
index across organizations, projects, gardens, source languages, source revisions, authoritative
source spans/slices, and target languages, then prove histories, deduplication, candidates, unique
indexes, and current projections never merge.

Every upstream admission record for that attempt—including resolved translation configuration—uses
this same composite `AttemptIdentity`. The raw caller id is not globally unique. Reusing it for
another complete immutable segment coordinate creates an independent attempt; reusing the same
composite identity with changed source, candidate, request, manifest, epoch, or configuration facts
conflicts. Config
admission and event admission may not disagree about that identity boundary.

The upstream config owner does not persist a bare reservation: it atomically installs the
complete identity/request/source facts with one attested config artifact before provider
invocation. Event admission later names that exact installed artifact in the canonical event.
A crash before config install leaves no reservation; a crash after install reuses it. This
config record and the later produced-candidate event are distinct lifecycle facts, but neither
may partially install or reinterpret the shared identity.

The initiating translation operation owns an immutable `TranslationTurnClaim` before any bound
translation provider/model session can produce a candidate or receive `save_translation`. After
carrier/source-manifest verification, a typed sender-constrained `ProposalSelectionCapability`
authorizes only source-manifest read plus one config model-selection observation. Before a preceding
unbound `SegmentationProposal` session, server preflight uses that capability to resolve an immutable
`ProposalModelSelection` from Knoxx config authority and pins its config resource/version plus
catalog model id. The session receives that selected model and manifest-bound canonical source
bytes, but no config credential or payload; it may propose logical boundaries but cannot call
`save_translation`, mint attempt/effect identity, or persist candidate/history. The server validates
each proposal against the admitted source revision, derives the complete coordinates and stable
attempt/effect ids, and exact-matches later config admission to the proposal selection. Selection
drift discards the proposal and restarts preflight. Only an equal selection may atomically install
the claim and launch a separate bound translation turn.
That claim binds a non-empty, canonically ordered collection of
provider-neutral `TranslationAttemptClaim` members keyed by complete canonical
`AttemptEffectIdentity`, not by `SegmentCoordinate`, and validates a second unique index over the
embedded `AttemptIdentity`. Multiple members may share the same `SegmentCoordinate` only when their
stable attempt ids make their complete `AttemptIdentity` values distinct; a repeated canonical
`AttemptIdentity` with a different `effect_id` is invalid. Distinct stable attempt/effect pairs then
produce distinct `AttemptEffectIdentity` values. Each member owns its stable `attempt_id`, stable
`effect_id`, exact grouping/source/request facts, and matching `AttemptConfigAdmission`. A duplicate
complete canonical `AttemptEffectIdentity`, repeated canonical `AttemptIdentity`, or non-canonical
encoding is invalid, while a repeated `SegmentCoordinate` alone is not a duplicate. The turn claim
also binds one immutable
`TranslationTurnExecutionSnapshot` and canonical
`provider-session-config-digest` covering the exact provider/model identity, config/policy
resource revisions, and normalized session parameters used by the one model session. Member
source and request facts may differ, but every member config admission names that same snapshot
and execution digest. All members name one server-derived effective organization, and the durable
turn admission identity is that effective organization plus `turn_id`; the raw turn id is unique
only within that organization. The member-set, snapshot, execution, and final claim digests are
record facts rather than uniqueness keys, so changed membership cannot evade conflict by creating
a digest-keyed slot. Identity binding is one-way: the snapshot names a stable `turn_id` plus the
canonical member-set digest, and the final turn-claim digest names the snapshot digest; neither
digest is defined in terms of itself. If preflight derives different execution digests, it returns
the canonical `:translation/turn-partition-required` plan and persists no claim, snapshot, member
admission, receipt, or session. The plan cannot authorize work, and config admission never derives
child turn ids or automatically installs a partition. Each group may proceed only through a later
explicit initiation with its own stable turn id and a fresh complete admission. The authenticated
session/tool context pins the turn-claim identity/digest, execution snapshot, and complete member
map, and the session starts only after every member has a complete matching config admission.
The claim, snapshot, and ordered member-admission map become visible together through one atomic
`TranslationTurnConfigAdmission`; there is no independently committed member admission that a
later operation must authorize from an old snapshot.

Publication dispatch accepts a workflow-stable idempotency key or mints a server-stable value;
its durable dispatch claim owns the turn claim and stores the stable per-member attempt ids. Its
retry identity includes the publication-dispatch claim variant, durable dispatch-claim identity,
and workflow idempotency key. Ordinary authenticated chat/tool translation uses an interactive
turn claim created by the same pre-turn initiator and requires no publication dispatch claim. Its
retry identity includes the ordinary-chat claim variant and interactive translation-start claim
identity. A same-organization/turn mismatch in the claim variant or those variant-specific
initiator facts conflicts rather than returning a claim of the wrong type. If an interactive
request cannot yet name the complete set of grouping/source/request facts, the unbound turn does
not receive `save_translation`; an explicit translation-start action gathers them and launches a
bound follow-up turn instead. A claimed member collection cannot be expanded, removed, or reinterpreted
after the model turn starts; translating another segment requires a newly admitted follow-up turn.
No already-running unbound turn may generate a candidate and synthesize admission at save time.
Non-agent workflows establish the same per-attempt admission before their provider call. Recovery
uses the server-derived organization/turn slot and compares any installed record against the
caller's stable initiator/member facts before a new config observation or reauthorization. An equal
installed turn returns unchanged without new reauthorization. Empty and mismatched outcomes remain
internal and pass the same current slot authorization over the canonical organization/turn
coordinate before disclosure. Denial returns one slot-existence-neutral result and performs no
config observation; an authorized mismatch returns a redacted conflict, while only an authorized
empty installed-slot outcome may perform the fresh authorized config observation and complete
atomic install. A crash before that admission leaves no persisted turn claim, execution snapshot,
member admission,
or session, so recovery of that empty slot observes afresh. A crash after commit returns the one
complete stored turn on retry even if a postcommit policy denial linearizes before that retry; the
installed allow decision is not retroactively rewritten and the retry starts no second session.
The observation receipt is historical evidence for that one operation and is never reused to
install a member later. The server-derived candidate execution digest is excluded from retry equality:
callers with the same stable organization/turn, authenticated initiator, claim variant,
variant-specific initiator facts, and member/source/request facts return the installed whole-turn
winner even when their unattached observations straddle a config change. A different execution
configuration uses a new turn id rather than replacing the installed digest.

The production `save_translation` MCP input schema exposes the required stable `attempt_id` and the
required stable `effect_id` on every call so the session echoes both pre-existing member values after
timeout or lost response. The tool handler selects the exact member by the authenticated turn pin
plus its full pre-admitted `AttemptEffectIdentity` and complete immutable segment coordinate. It
compares both echoes with that member and its installed config admission, then passes the full `AttemptEffectIdentity`,
manifest id/digest when delegated, and authority epoch when delegated
unchanged through the domain/save boundary to canonical event admission and, for delegated
ingestion, the Knoxx authority-store transaction and outbox adapter to the existing OpenPlanner
segment sink. Manifest and epoch come only from authenticated server-owned admission/envelope state,
never model or caller input. The handler returns the bound
identity with the admitted event/ordinal. The tool may be called repeatedly for different admitted
members in either order; saving one member never consumes or authorizes another. Missing or
different attempt/effect input, an unclaimed
composite, or a claim-set change is rejected and appends no candidate. The handler may not discard
either value, mint an attempt or effect identity per invocation, substitute the transport
`_tool-call-id`, or derive identity from content. Organization scope remains server-derived and
combines with these values at event admission. Every saved member event carries its member artifact
plus the same turn execution snapshot/digest as the authenticated provider session; a mixed or
substituted digest conflicts and appends nothing. Distinct attempt ids with byte-equivalent content
intentionally remain distinct attempts.

Until the existing OpenPlanner sink keys rows by the complete `SegmentCoordinate`, the Knoxx
delegated adapter also reserves its actual narrower
`LegacyProjectionKey = (org_id, document_id, segment_index, target_lang)`. That reservation binds
exactly one complete coordinate. A different project, garden, source language/revision, or source
span that aliases the same legacy key conflicts before a new ledger claim or remote invocation;
retranslations of the one bound complete coordinate remain serialized by attempt ordinal.

### Canonical attempt event

Define one versioned `CanonicalAttemptEvent` before storage. Its compared/digested fields are:

- the complete `AttemptEffectIdentity`, including the complete `AttemptIdentity`, stable attempt id,
  and stable effect id;
- for delegated ingestion, carrier type, exact manifest id/digest, and authority epoch;
- authenticated actor and effective/delegated organization evidence;
- immutable source artifact identity, revision, media/locale facts, and digest;
- candidate artifact identity, canonical content/media facts, and digest;
- the exact turn-claim and execution-snapshot identities/digests;
- the exact resolved-config artifact identity/attestation;
- provider, model, provider-config/policy versions, and normalized request parameters; and
- provenance plus raw-result evidence digest.

One canonical encoder/version supplies admission equality, the evidence digest, persistence,
and replay. Store-assigned ordinal/record id, accepted/updated timestamps, transport tool-call
id, retry count, latency, logs, worker/process identity, and projection checkpoint/envelope
metadata are explicitly excluded. Those generated facts may surround the stored payload but a
retry never recomputes them for equality. Any change to a canonical field, including attempt/effect,
delegated manifest, or epoch facts, conflicts; changes only to excluded execution-envelope facts
return the original admitted event.

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

Legacy migration first persists a `LegacyMigrationIdentity` derived only from the immutable legacy
store/collection namespace and persisted legacy-row primary key. It is independent of
`SegmentCoordinate`, candidate values, and every coordinate inferred during migration. The event
store installs a unique index on `LegacyMigrationIdentity`; the deterministic initial attempt/event
id is derived from that identity in a fixed migration namespace, and the canonical migrated event
retains both identities. Migration first attempts to resolve project, garden, source language,
source revision, and authoritative source span/slice from historical source authority. When that
history proves the facts, the migrated event uses the normal verified `SegmentCoordinate`. When a
legacy row's persisted `source_text` cannot be attributed to one historical revision/span, migration
instead uses the explicit tagged `LegacyUnknownSourceCoordinate`, containing
`LegacyMigrationIdentity`, the digest of the row's persisted source text, recorded locale/project/
garden fields, and reason `:historical-source-unavailable`. It never substitutes the current source
revision or fabricates a span. This migration-only variant is a distinct grouping key, remains
servable in the new projection, and is ineligible for provenance-sensitive evaluation, training,
publication, or a new append. A later verified retranslation creates a normal
`SegmentCoordinate`/attempt while preserving the unknown-provenance initial event as separate
history. Ambiguous competing historical matches still stop for operator reconciliation. The
canonical migrated event payload contains whichever tagged source snapshot was actually observed.

Migration admission atomically unique-inserts or compares by `LegacyMigrationIdentity` before
normal `AttemptIdentity` admission. An equal retry returns the stored initial event. If any derived
coordinate or payload differs from the stored migrated event, retry reports the existing
`:attempt-id-reused` conflict with the persisted migration identity for operator reconciliation,
without deriving or appending a second initial event. Migration writes the event before the
projection, resumes from durable checkpoints, and keeps the existing read endpoint available until
every migrated grouping key can be served from the new projection.

Partial migration uses one server-owned, compare-and-swap `MigrationAuthority` marker per
`LegacyAuthorityKey = (immutable legacy store/collection namespace, persisted legacy-row primary
key)`, not per new grouping key. For an existing row this is the same persisted row identity used
by `LegacyMigrationIdentity`; an absent-row bootstrap deterministically reserves the potential
legacy primary-key slot. The marker stores `:legacy` or `:events` plus the one bound complete tagged
coordinate. While `:legacy`, reads use the legacy row. Migration writes and verifies the
deterministic initial event and projection first, then atomically advances the marker; before that
CAS the event/projection is staged evidence, not read authority. Once `:events`, reads use the
projection only and never fall back to or dual-write the legacy row. A distinct complete coordinate
that resolves to an already bound `LegacyAuthorityKey` returns a typed migration-coordinate
conflict before coordinate-specific admission, cutover, or writes.

Marker absence is a bootstrap pre-state, not a third authority value. Every first read,
migration, or save derives and acquires the same legacy-authority migration/admission fence before
any coordinate grouping-key fence, then tests marker absence
and legacy-row existence in one transactionally consistent decision. When a legacy row exists,
one compare-and-swap claims `:legacy`, binds its `LegacyMigrationIdentity` to exactly one complete
tagged coordinate, and gives that migration path cutover authority. When no legacy row exists, a
first save uses one atomic store transaction to compare both facts as still absent, bind the
`LegacyAuthorityKey` to its complete coordinate, append its canonical event with the next ordinal,
install its projection, and set the marker to `:events`; no marker-only or event-only intermediate
state is visible. A read or migration that
finds no row may establish an empty `:events` authority, after which a save uses normal event
admission.

Concurrent first save and migration retry the whole fenced decision after a failed compare. A
migration that claims an existing legacy row completes that deterministic initial event before
the save is appended; a first save that wins the verified no-legacy transaction leaves migration
to observe `:events` and do nothing. A crash before either atomic bootstrap changes nothing; a
crash after it exposes the complete chosen authority and, for a saving caller, its admitted event
and projection. Thus no candidate waits on a marker that nobody owns, disappears between legacy
discovery and cutover, or is written to both authorities.

`save_translation` acquires the same legacy-authority migration/admission fence before its
coordinate grouping-key fence. For a legacy key it
completes/reuses the initial event and projection, advances the marker, and only then appends
the new canonical attempt; it never writes both authorities. A crash before marker advancement
leaves legacy authoritative and retryable; a crash after advancement leaves events
authoritative and replay repairs any projection checkpoint. Only distinct legacy-authority keys
may cut over independently.

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
  the same stable `attempt_id` and `effect_id`, and observe the same single event/ordinal. Reusing
  either id with changed canonical content conflicts, while two distinct attempt/effect identities
  with equal content preserve two intentional attempts. The schema/handler proof fails if either id
  is absent, regenerated, discarded, or replaced by a transport/tool-call identity.
- Cross-organization and cross-target-language fixtures with otherwise identical document
  and segment coordinates remain isolated in both append history and current projection. Extend the
  matrix across project, garden, source language, source revision, and authoritative source
  span/slice; each dimension remains isolated in event and current-projection unique indexes.
- Two different stable `attempt_id` values on the same `SegmentCoordinate` produce two distinct
  intentional retranslation events and consecutive per-coordinate ordinals; the current projection
  advances to the highest ordinal without rejecting the second attempt. The same raw `attempt_id`
  reused across two complete immutable segment coordinates produces
  independent complete config admissions/events, while changed reuse inside one composite grouping
  conflicts consistently at config and event admission. A delegated attempt with a substituted
  manifest or epoch also conflicts before append/projection mutation.
- On the same `SegmentCoordinate`, two members with different stable `attempt_id` values and
  different stable `effect_id` values in the same turn save independently under their distinct
  `AttemptEffectIdentity` values. Duplicate full member identities are rejected, but repeated
  coordinates are not. Missing or substituted attempt/effect, manifest, or epoch facts append no
  candidate and do not advance the projection. A claim containing two members with the same
  `AttemptIdentity` but different stable `effect_id` values is rejected before persistence,
  config observation, provider work, or event admission.
- Turn-config crash/race fixtures prove no bare reservation or partial member can strand a later
  event: pre-install retry must resolve/authorize anew, post-install retry reuses the exact complete
  turn map, and each canonical event accepts only its embedded artifact identity/attestation.
- The real agent-dispatch boundary durably pins the attempt id and complete config admission
  before session/provider start, then the real tool schema/handler accepts only that same id.
  Missing/mismatched echoes, session creation before admission, or save-time id generation fail
  the proof and append no candidate. A dispatch-claim/admission split failure retries the pinned
  id and starts exactly one session only after admission completes.
- The real ordinary-chat boundary proves the same law without a publication dispatch: a complete
  interactive translation request installs and pins its interactive turn claim and every member
  attempt/config admission atomically before the model turn, exposes `save_translation`, and
  survives a lost-response retry. One bound-turn fixture pre-admits two segment members, saves
  both in either order, and proves each call uses its own composite identity, config artifact,
  event, and ordinal. Omitting the second member rejects its save without consuming the first;
  adding or replacing a member after turn start also fails and appends nothing. An incomplete
  target withholds the tool and starts no translation turn; the handler neither demands a
  publication claim nor fabricates an admission after candidate generation.
- A deterministic barrier prepares a complete turn from config V1, advances current
  provider/model config to V2 before atomic admission, and allows only one of two outcomes: the
  already-authorized V1 operation commits every member under V1, or the whole operation aborts and
  a fresh observation admits every member under V2. Injected failures after staging each member
  expose no claim, snapshot, admission, or session. Substituting a V2 member into a V1 map or
  changing normalized session parameters fails the whole commit. Claiming two legitimately
  different execution digests returns the canonical partition-required plan with no turn state or
  session; only later explicit requests with distinct stable turn ids can admit the groups as
  independent turns.
- Current-state reads remain compatible from a caller's perspective while deriving from
  history.
- During partial migration, per-key authority routes reads/writes to exactly one source; crash
  fixtures on both sides of cutover prove no stale fallback, missing save, or divergent
  dual-write.
- A previously unseen grouping key with no marker or legacy row atomically establishes
  `:events` together with its first saved event/projection. Two concurrent first saves and a
  first save racing legacy discovery prove one bootstrap authority, normal attempt-id conflict
  or ordering law for the candidates, no indefinite block, and no lost or dual-written save.
- Concurrent distinct attempts receive unique per-key ordinals; out-of-order projection
  delivery cannot regress current state, and replay/recovery is byte-equivalent and
  idempotent after an injected append/projection split failure.
- Running legacy migration twice appends no duplicates; changed legacy authority at a reused
  `LegacyMigrationIdentity` conflicts instead of being silently replaced. Partial-run fixtures
  mutate the legacy payload and separately change project, garden, source revision, or source span
  before restart; every retry resolves the same persisted migration slot, conflicts against its
  stored coordinate/payload snapshot, and proves one initial event remains without a second event or
  projection advance.
- Legacy rows with no historical revision/span fixture migrate once under
  `LegacyUnknownSourceCoordinate`, remain readable after `:events` cutover, and expose the persisted
  source-text digest plus `:historical-source-unavailable` without naming the current revision.
  Provenance-sensitive evaluation/training/publication and appending a new attempt to that unknown
  grouping key fail closed; a later verified retranslation uses a separate normal coordinate.
- Migration fixtures address one legacy row concurrently through two complete coordinates that
  differ by project, garden, source locale, source revision, or source span. Both resolve the same
  `LegacyAuthorityKey`; exactly one coordinate binding wins, and the other receives the typed
  migration-coordinate conflict before a second marker, cutover, event, projection, or write.
- An evaluation receipt can bind to a candidate that remains addressable after a newer
  translation exists.
- `save_translation` retains `destructiveHint: true` while it can replace current projection
  state; an annotation regression test prevents append-only history from being mistaken for a
  non-destructive public operation.
