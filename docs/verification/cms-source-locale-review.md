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
