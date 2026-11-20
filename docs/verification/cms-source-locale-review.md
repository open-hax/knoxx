# CMS source locale review policy

Run `scripts/verify-cms-source-locale-review.sh` from the checkout under review.
It compiles that checkout's CMS resource constructor and publication gate, then
runs the existing native CMS HTTP verification against a real Fastify server on
an ephemeral loopback port and the native filesystem publication suite. All runs use the existing guarded test-counter
validator. Missing or empty test results, any failure/error, compiler warnings,
timeout, or abnormal exit fail the script. Each run is bounded to four minutes.
Its temporary output and the HTTP suite's run-scoped content are removed on
success, failure, or interruption.

Prerequisites are Node, pnpm, Clojure/Java, `timeout`, the backend's installed frozen
dependencies, and its pinned tools.deps dependencies. The domain walkthrough's
source and translated strings are complete fixture bytes, their digests are
computed by the production crypto adapter, and its receipts and approvals are
validated by the production translation evidence law. The HTTP suite creates a
native CMS document through `POST /api/cms/documents`, reads the persisted
English/Spanish resource policies, and resolves its actual immutable source path
through the production source resolver before evaluating publication admission.
It also exercises anonymous rejection, read-only writes, cross-organization
access, stale saves and history. Principals are seeded at the real auth-context
seam; this is not a password-login or deployed-image test. HTTP fixtures enter
only the suite's temporary CMS ledger and content root, then are removed.

The English intent is emitted by `domain.cms-document/manifest`, hydrated
through the publication resource graph, and evaluated with the real translation
evidence projection. Before this repair, its `:translation/review :required`
blocked publication even though there was no translation back into English to
approve. The source intent now uses `:none`, matching generated publication
drafts. The Spanish intent continues to use `:required`.

The walkthrough checks these outcomes:

- A published English intent with a concrete source digest is admissible without
  translation or review receipts and creates no self-translation work. The HTTP
  check obtains that digest from saved source bytes rather than a supplied fact.
- A withheld source is still inadmissible. Missing source revision blocks both
  locales before any translation or publication decision.
- Spanish needs a completed candidate with its target content digest and an
  approval for the exact document, garden, locale, source and output revisions.
- A different source, output, content digest, or later English edit cannot reuse
  that approval.
- At the same source revision, correcting translation output A into output B
  requires renewed whole-output approval. Before it, publication stays blocked
  and the committed A artifact is untouched. After it, B materializes with a
  new artifact path and idempotency key, and repeating B converges without a
  rewrite. Returning to a previously approved output verifies the current target
  before replaying its historical receipt; if displaced, a deterministic fresh
  reservation restores it. Repeated A→B→A→B→A cycles retain earlier completion
  records. Historical source-only routes remain readable.

The source revision remains the original-document evidence identity. The
approved translation has a separate `:artifact/content-revision`, carried into
`:route/content-revision` and `:materialized/content-revision`. The planner,
publish key and filename all bind that output revision; `:route/revision`
continues to identify the source. This prevents a corrected translation from
appearing converged merely because the original document did not change.

The concurrent filesystem checks rely on the static target's manifest lock;
they do not prove one invocation of an arbitrary external publishing adapter.
The existing `:in-flight` store state cannot distinguish a live peer from an
abandoned claim, so other targets still need their own effect deduplication or
fencing. The Mongo graph adapter similarly preserves its existing token-guarded
node rollback rather than claiming a cross-collection transaction.

Translation graph edges coexist with SDK-owned edges through a unique index on
native string aliases. Its guarded upgrade builds that protection before
retiring the old blanket `id` index and refuses incompatible named indexes or
non-string legacy identities. A native edge may fill missing SDK tuple fields
only when its alias and endpoints already agree; a foreign canonical primary
identity or tuple collision fails without rewriting the SDK edge.

This policy change does not make source content public by saving it. CMS saves
still create private documents and withheld publication intents. Publication is
an explicit desired-state request, and reconciliation remains a separate effect.
It also does not waive any declared review obligation in the publication gate.
Existing generated manifests retain their declared review fields; this change
repairs newly emitted resources and does not rewrite previously created intents.
Use a new document for the walkthrough, or review an existing resource's policy
as a separate explicit edit.

The Services deployment calls Knoxx's reusable qualification workflow. Every
qualification job explicitly checks out `open-hax/knoxx`; omitting the repository
would resolve the requested Knoxx commit inside the caller's Services repository.
`python3 scripts/test-production-checkout.py` checks all four jobs, and both the
deployment-boundary CI job and native review evidence run that guard. It verifies
repository selection, not completion of a production qualification campaign.

Native provider deltas retain literal repeated characters and paragraph breaks.
Authoritative partial and terminal arrays concatenate their distinct text or
reasoning blocks in order, so `Knox` plus `x`, repeated `ha`, and whitespace-only
blocks survive final hydration. The chat also keeps the original assistant
association across delayed GETs and retries. After a same-run completed or failed
read hydrates the final message, an overlapping nonterminal read cannot clear its
content parts or sources, change its model, or regress the latest run status.

When an authoritative partial or terminal snapshot corrects a streamed draft,
the backend updates its retained trace and sends a token packet with
`operation: "replace"`. Its `token` contains the full corrected text for that
channel across this turn; `offset` counts the UTF-16 code units of earlier
provider messages. The chat immediately replaces the answer or reasoning and
only the matching trace suffix, retaining earlier text, tools and the other
channel. Buffered tokens before the correction are applied first, and later
deltas append to the corrected text. Ordinary token packets remain literal
append deltas. Malformed replacement metadata is refused before rendering.
An explicitly supplied empty reasoning string clears the current provider
message's reasoning, including the matching trace suffix; omitted, null or
nontext reasoning preserves the observed stream. Typed reasoning content blocks
follow the same distinction in partial and terminal snapshots.
This native regression is covered by the compiled backend stream tests and the
actual WebSocket decoder/chat-hook tests; the live provider walkthrough still
requires the deployed candidate identified below.

If a reconnecting client's local trace lacks earlier text, the replacement
projection restores a missing or stale prefix from the full authoritative
snapshot, while retaining every observed tool and other channel block in order.
An exact observed prefix stays in its original blocks.
The native sink and the actual chat hook cover empty, partial and stale retained
trace fixtures, without changing how reconnects fetch run events.

For a live demonstration, first confirm that the deployed image contains this
constructor repair and that the signed-in organization has publication
read/manage and translation read/manage/review authority. The backend must have
Mongo evidence/split persistence, generated resources on its writable workspace,
a source that resolves to current bytes without CMS conflicts, and the same
publication content root mounted read-only by the website. An old image using
`/api/openplanner/v1/cms/documents` is a different CMS implementation and cannot
demonstrate this repair.

1. In `/cms`, create a uniquely named English Markdown document, save it, and
   select **Workspace Publications**. Confirm its native CMS history and generated
   English/Spanish publication relations before requesting publication.
2. Request publication for the garden. Reconcile the English relation through
   authenticated `POST /api/publications/reconcile` with its server-returned
   `publicationId`. Require a materialized/noop receipt and inspect the manifest;
   saving or changing desired state alone is not publication proof.
3. In `/translations`, select that document's Spanish relation and dispatch it.
   Wait for real persisted candidate splits. Review and correct them, then approve
   every effective split. **Approve whole output** records its exact approval and
   calls reconciliation. Require a materialized receipt rather than assuming the
   approval itself served content.
4. Reload the website to read `/published/manifest.edn` again. Open the returned
   manifest paths and inspect their exact artifacts. CMS paths come from the
   manifest; do not invent an `/es/` prefix for a generated path.
5. Correct a persisted Spanish split, including a paragraph break when relevant.
   Confirm the previous whole-output approval is no longer current and that
   reconciliation is blocked. Reapprove the complete effective output, then
   require a new materialized receipt, the same source revision, a new content
   revision and artifact path, and the exact corrected bytes. Reconcile again:
   require a noop and unchanged artifact bytes. Do not remove completion records
   or toggle publication state to make the correction appear successful.

The script does not drive a browser, perform password login, identify a deployed
image or invoke a translation model. Its publication suite writes fixture bytes
only in a temporary native content root and cleans it up. It prints the live boundaries
as WARN. Use [the existing publication browser tour](../../scripts/verify-publication-tour.sh)
and [its verification guide](publication-epic.md) for the related UI and
desired-state walkthrough, and [translation split review](translation-split-review.md)
for granular correction, whole-output approval and static-target checks. Their
fixtures do not prove real provider output or this fresh CMS source's deployment.
The live steps above still require real browser evidence through Cua in this
session, screenshots, deployed source/image verification and website artifact
inspection. A saved document, changed desired state or approved candidate alone
does not demonstrate materialized content.

## Native review repair qualification, 2026-10-10

The source phase following `0cffeaad02f3818036186dcb764e06bbc5bd6cd5`
repairs three confirmed native findings and a separately reproduced terminal
hydration failure. A completed publication replay now observes the target before
returning its old receipt: the same output is a noop, an absent target or the
plan's exact previous output permits deterministic restoration, and a different
nonempty output is refused before reserving another generation. Tests retain
freshly admitted A/B correction cycles and verify that stale completed plans do
not remove another output's artifact, manifest, cache, or receipt. This is an
observation guard; it is not atomic compare-and-swap across live peers.

Missing, null, nonstring, and tool-only assistant snapshots preserve streamed
text. Explicit empty text clears the current provider message's suffix while
preserving prior assistant text and reasoning. Both CMS verifier scripts resolve
`HEAD^{commit}` separately, require a full commit SHA, and stop before compiler,
helper, or fixture execution when Git fails.

Terminal detail hydration releases a matching run/assistant association after a
permanent failure, including exhausted bounded 404 retries. It preserves the
streamed answer and permits Undo again. Ordinary running-detail failures and
newer run/assistant associations retain their state. Existing streaming hook
tests moved into `useChatWorkspaceController.test.ts`; existing WebSocket decoder
tests moved into `hooks.test.ts`. All 25 previous test declarations remain, with
23 bodies byte-identical and the remaining two fixture refactors preserving all
11 assertions. The 283-record migration manifest is byte-identical to the
previous committed manifest; no migration or size policy was waived.

The frozen backend source passed 1,899 tests / 9,689 assertions, native HTTP
integration 23 / 118, the CMS verifier's three phases (3 / 26, 8 / 107, 19 / 206),
and shell Git-failure controls. Server compilation produced zero warnings. The
changed CLJS paths have zero lint errors or warnings; full lint retains its
inherited eight errors and 282 warnings. Frontend qualification passed 282 tests
in 44 suites with 41 existing TODOs, type checking, production compilation with
zero CLJS warnings, and the actual migration and size ratchets against base
`3409977bca4ba35e09967f9a99d50867a679a73c`. Independent bounded source reviews
found no actionable issue in either repair set.

The local evidence set `openhax-cms-20261010` retains these immutable records:

| Record | SHA-256 |
| --- | --- |
| `review-state/knoxx-native-review-repair-qualification.json` | `8e4f51a40d7a80c1c2f245f0f1bf19c103e336331d843f78b321cd58bbc1b468` |
| `review-state/knoxx-frontend-terminal-repair-final-source.json` | `a9607231479d6c8a1b4a704aac14bb2f8c1eb46a22ed1a4dfb7ec4c00cc6884b` |
| `review-state/knoxx-native-repair-independent-source-review-20261010T160958Z.json` | `4213c6f415c42b917b0beb9d8da58edc7517557f739199dd601ab0ff13210425` |
| `review-state/knoxx-frontend-terminal-hydration-independent-assessment-20261010T161250Z.json` | `6392785bbd527a5536e441f43c7082fefa56d5e21018e50044a55fd7dd735ea6` |

These source tests use the established SDK/auth fixture boundaries and linked
SDK `a09894c48760748b7fca7bf9e94f36471f599a47`. The earlier real provider and
packaged CMS demonstrations used source `0cffeaad` and operational SDK `07085d`;
their image, browser, and provider evidence does not qualify these later patches.
Fresh exact-head hosted CI, native review, and deployment evidence remain
separate obligations. The raw failed MiMo submission is discovery evidence only
and supplies no native approval.


## Atomic completed-restoration qualification, 2026-10-10

The successor to `8ab11f1ef9b6b1b5ec8e852b185c92a772b2492e` closes the
observation-to-replacement race reported in native finding
[4238323672](https://github.com/open-hax/knoxx/pull/387#discussion_r4238323672).
Completed replay carries an explicit expected materialization into restoration;
`nil` requires target absence. The static adapter compares it under the same
manifest lock used for artifact replacement, before any byte write, manifest
touch, or displaced-artifact reclamation. The memory adapter compares and
replaces in one atomic state update. A differing peer publication refuses the
stale restoration while retaining its in-flight generation and prior receipts.

An already-current canonical requested revision remains unchanged, including
valid differing media, encoding, or artifact metadata. The static adapter returns
the actual served route rather than manufacturing a replacement receipt. Native
filesystem regressions force observation, peer publication, then stale replay
for both absent and prior routes, and compare manifest, artifact, timestamps and
all old cache bytes. The separate same-canonical metadata regression failed
before the bounded correction and passes afterward. Fresh A/B correction cycles,
withdrawal, ambiguous outcomes and receipt retention remain covered.

Final source qualification uses the explicit Node 24.14.1 binary and the real
pnpm JavaScript entry point. Fourteen observed processes include pnpm, compiler
launchers, and actual compiled unit/HTTP test bundles, all on that binary. The
focused suite passes 56 tests / 507 assertions; the complete backend suite passes
1,904 / 9,756; native HTTP passes 23 / 118. Canonical server compilation covers
538 files with zero warnings. The five changed paths have zero lint diagnostics;
full lint retains the exact ordered baseline of eight errors and 282 warnings.

| Evidence in `openhax-cms-20261010` | SHA-256 |
| --- | --- |
| `review-state/knoxx-atomic-completed-restoration-node24-qualification.json` | `11769cd87a3d9766f38536d481cc6564cce6dd2fc8d7f00574ae56f95a4d981d` |
| `review-state/knoxx-atomic-restoration-independent-assessment-20261010T1659.json` | `f84a085163ae1edebac377f33c8cbffd6fb9bacd6b48dd0c33b60bc64b2e9715` |

The earlier Volta launcher phase is preserved. Its shell Node version did not
identify pnpm's actual child runtime; its intended Node 24 attribution is
superseded by the directly observed qualification above. Earlier RED controls
remain historical observations. Source tests use the established SDK fixture
`a09894c48760748b7fca7bf9e94f36471f599a47`; they do not qualify operational SDK
`07085d6557b75834ce6f50e6c54b8ca47e1c7c08`. Existing 8ab packaged images precede
this repair. Atomic conditional replacement applies to completed replay
restorations; first publication, removal, existing lock takeover assumptions,
and external filesystem edits retain their prior scope. New image/provider
evidence, hosted CI and native review remain distinct obligations.
