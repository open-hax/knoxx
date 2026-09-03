# Deployment content admission

Status: complete — implemented and verified locally 2026-09-02

## Intent

A Services deployment admits every explicitly anchored publication document
into Knoxx. Admission indexes the exact source revision, records durable events,
starts translation work for every declared target locale, and may request a new
derived post draft. Translation and drafting are production work; publication
is a separate, human-gated decision.

## Authority and boundaries

- Knoxx owns document, translation, dictionary, review, and publication laws.
- Services owns deployment topology, environment selection, and the one-shot
  post-health activation hook.
- EDN resources are desired state. Content digests, agent outputs, reviews, and
  deployment observations are events or immutable receipts, never rewritten
  into the authored resource as operational status.
- An OpenPlanner document snapshot is an indexed event identified by document,
  tenant/project, and source digest. Replaying an unchanged deployment reuses
  the same identity.
- Existing translation receipts and split review history remain semantic
  authority. Generic events make that history searchable and replayable; they
  do not weaken the receipt joins.

## Resource contract

An anchored document is a normal publication `Document` carrying:

```clojure
{:document/id :example.documents/source
 :document/title "Source"
 :document/source-locale :en
 :document/visibility :public
 :document/source {:path "publication-source/source.md"}
 :document/anchor? true
 :document/generate-drafts? true}
```

Target locales are not guessed. They are the locales of declared publication
relations for that document. A generated post is itself a document with draft
publication relations, and therefore re-enters the same admission/translation
path. Generated documents carry `:document/generate-drafts? false`: they are
terminal for post generation, but their non-source publication relations are
still admitted automatically for translation. Admission never infers public
visibility from a missing owner: authored shared anchors declare
`:document/visibility :public`, while generated drafts declare their owning
`:document/org-id` and `:document/visibility :private`. A legacy document with
neither a matching owner nor explicit public visibility is omitted from sweeps
and indistinguishable from a missing document in exact admission.

The same server-derived organization predicate scopes manual translation
dispatch, review inventory and receipt synthesis, publication/CMS projections
and state changes, and production reconciliation. Each surface filters both a
hidden document and its publication relations before reading source bytes;
exact hidden identifiers remain ordinary 404s. The authenticated generic
OpenPlanner compatibility proxy refuses vector search until its backend can
persist and enforce organization metadata, rather than response-filtering an
already leaked or truncated top-k result.

## Admission transaction

For one immutable resource snapshot and authenticated tenant/project scope:

1. Select every `:document/anchor? true` document, or one exact requested
   document.
2. Resolve the source path from the resource file's provenance and read it
   once. Missing or blank content is a hard failure.
3. Compute the source revision and append/index a content-addressed `docs`
   event. Await indexing where the embedded SDK can provide read-your-writes.
4. Append a stable `publication.document.indexed` event and notify the local
   trigger runtime with the same identity.
5. Derive translation work from the publication relations and dispatch it via
   the existing claim/turn machinery. A deploy may accept asynchronous agent
   work, but it must not silently accept a failed dispatch.
6. When drafting is enabled, start one revision-pinned post-drafting session.
   The resulting post is stored as an unpublished draft and admitted through
   this same path.

Steps are retry-safe. Durable facts precede process-local notifications. A
partial failure is returned with the exact document and phase; a deployment
with any admission failure is red.

`dispatch/accepted` means the durable translation claim and local agent run
were accepted; it does not mean the model has completed or a candidate event
already exists. Triggered agent turns enter a process-local FIFO bounded by
`KNOXX_EVENT_AGENT_CONCURRENCY` (default `1`) and
`KNOXX_EVENT_AGENT_QUEUE_LIMIT` (default `256`). A slot is held until the full
turn settles. The Services and local deployment contracts set
`KNOXX_EVENT_AGENT_TURN_TIMEOUT_MS=300000` (a canonical integer in Node's
`1..2147483647` millisecond timer range), so one stalled provider turn cannot
hold that slot forever. A timeout waits five seconds for provider abort; if the
provider cannot become idle, Knoxx fail-stops for PM2/Compose restart and
durable replay rather than releasing the FIFO into overlapping tool execution.
Queue overflow is a failed run and therefore makes admission red;
accepted queue/run metadata remains observable. This bound applies to
triggered turns, not interactive chat. The FIFO itself is deliberately
process-local. Recovery comes from durable work state: an accepted translation
claim replays its deterministic event after restart, while a terminal turn that
did not complete its exact claim becomes retriable on the next admission. A
post-drafter turn that leaves no deterministic recursive-admission completion
marker releases only its own indexed event for same-process retry. Generated
source and manifest bytes are materialization, not completion: the marker is
written only after recursive admission returns `ok=true`, numeric `failed=0`,
and numeric `admitted>0`. If admission fails after those create-only bytes are
written, the next retry reuses them and retries admission without an overwrite.
A later nondeterministic model answer is ignored while those valid unfinished
bytes are re-admitted; after completion, differing-byte replay still conflicts.
Knoxx does not run an autonomous retry timer; deployment, resource
entry, or explicit re-admission supplies the next reconciliation pass.

One-tool production agents declare `:tools/choice :required-first`. Contract
resolution rejects unsupported choices and rejects `:required-first` when no
tool survives role/capability and allowlist resolution. At the eta-mu boundary,
only the provider request whose current LLM transcript ends in the initiating
`user` message receives an exact named function choice when one tool survives,
or generic `toolChoice: "required"` when multiple tools survive. The request
after a `toolResult` retains the provider's original options so the agent may
finish. Ollama initiating requests are additionally pinned to temperature and
seed zero, but this is a reliability hint rather than authority: a
`required-first` turn with no durable tool receipt is recorded as failed even
when the model returned nonblank prose.

Ollama's OpenAI-compatible endpoint does not reliably enforce `tool_choice`.
When a publication translation turn settles without its exact candidate set,
Knoxx therefore calls native `/api/chat` only for the missing admitted splits,
with thinking disabled and a strict schema containing only
`translated_text`. Model output supplies that one untrusted string; Knoxx
retains the run, document, locale, split, attempt, and source-text coordinates
and submits the result through the existing `save_translation` sink. Call-shaped
prose is never parsed or executed. A retry reuses durable candidate prefixes,
and exact final-pair replay repairs a receipt whose event/vector projection
failed after immutable completion.

## Translation events and dictionary

- Every completed candidate split is additionally appended as a stable
  `translation.segment` event with immutable turn, manifest, candidate-set,
  locale, source revision, and output revision coordinates. Query-critical
  `source_lang`, `target_lang`, `source_text`, `mt_model`, and `status` values
  live in the persisted event `extra` projection (and may also be present in a
  compatible `meta` envelope). Repair may backfill only missing/nil duplicated
  metadata; a conflicting non-nil fact is a hard failure.
- Candidate completion includes a read-after-write vector postcondition. The
  stable candidate event id must resolve to at least one hot-vector chunk, and
  every chunk must contain finite numeric values whose array length and stored
  `embedding_dimensions` equal `EMBED_PROVIDER_DIMENSIONS`. A completed event
  whose projection is missing or invalid is re-indexed from the existing
  immutable event (without reinserting it), then verified before the tool
  reports success. Receipt-backed completed work invokes this repair seam on
  replay instead of silently skipping it.
- Every review is appended as an immutable split-review receipt. Reviewer
  corrections are preserved as review facts rather than overwriting candidate
  text; candidate translations are the searchable OpenPlanner event projection.
- A translation dictionary is a projection of currently effective approved
  split pairs. Corrected text wins over raw candidate text. Pending, rejected,
  stale, superseded, cross-tenant, and cross-locale pairs never enter the active
  dictionary.
- The translator receives a pinned dictionary/example snapshot before model
  execution. Later reviews cannot change an in-flight turn.

## Draft and publication law

- Generated draft identity is the SHA-256 fingerprint of canonical policy:
  source document, source revision, source locale, organization, optional
  project, and sorted unique garden/locale topology. The source locale is
  included in each generated garden. Reordering or duplicate coordinates reuse
  one identity; a topology or scope change creates a new immutable draft id.
- `:publication/state :draft` requests translation but never reconciliation or
  materialization.
- Moving a draft to `:published` is an explicit human desired-state change.
- A generated source-locale relation carries `:translation/review :none`.
  Every non-source generated relation carries
  `:translation/review :required`, and every generated relation is pinned to
  `:open-hax.publication/static-site`.
- Even after the state changes to `:published`, missing, stale, partial,
  rejected, or unapproved translation evidence blocks the publication effect.
- Admission, generation, translation completion, or dictionary accumulation
  can never invoke the publication reconciler.

## Deployment contract

After the Knoxx health gate succeeds, Services calls exactly once:

```http
POST /api/publications/documents/admit
{"anchors": true, "generateDrafts": true}
```

The hook is authenticated from inside the Knoxx container, is time-bounded,
requires HTTP 200, and fails when the response reports any failed document or
translation dispatch. It waits for durable admission and agent-run acceptance,
not model completion or human review. The bounded triggered-agent FIFO protects
local Ollama from an unbounded anchor fan-out after that response returns.

## Acceptance checks

- [x] A missing anchored source makes admission and deployment fail visibly.
- [x] An unchanged redeploy produces the same event identities and no duplicate
      translation claim.
- [x] A changed source digest creates one new indexed revision and translation
      attempt per declared target locale.
- [x] A resource PUT for a document invokes the exact-document admission seam.
- [x] Candidate translations are durable, indexed events while reviews remain
      immutable receipt authority.
- [x] Translation and post-drafting turns must call their sole save tool before
      they may finish, while the post-tool-result provider turn remains free to
      return its final response.
- [x] Only effective approved/corrected pairs appear in the dictionary.
- [x] Generated posts remain drafts, are translated to all declared locales,
      are terminal for further generation, target the static site, and produce
      no public materialization. Their source locale requires no translation
      review; every translated locale does.
- [x] A materialized draft whose recursive admission fails remains incomplete;
      retry reuses its persisted bytes even when the model proposes different
      bytes, retries admission, and writes the deterministic completion marker
      only after coherent success. Completed replays retain immutable conflict
      detection, and a marker alone is not completion when either persisted
      file is absent.
- [x] Draft identity is stable under garden/locale reordering and duplication,
      while source locale, topology, organization, or project changes create a
      new immutable identity.
- [x] Publishing remains impossible until an explicit state change and all
      revision-bound review evidence pass the existing gate.
- [x] The Services hook is Knoxx-only, one-shot, injection-safe, bounded, and
      covered by an offline test.
- [x] The deployment verifier self-seeds a small anchor, exercises failure and
      replay paths, authenticates exact pending-review output bytes, and cleans
      only the run-owned content files after settlement. The split-review
      verifier causally proves that an unapproved publication-intended output
      is blocked and writes no public artifact.

## Checkpoints

1. Resource law and admission/index endpoint.
2. Durable translation events and approved dictionary projection.
3. Draft state, post-drafter contract, and draft admission tool.
4. Services post-deploy activation hook.
5. Focused tests, full Knoxx gates, verification script, independent review,
   and execution receipt.
