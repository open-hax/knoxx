# Translation work dispatch — human verification

Card: `kanban/tasks/knoxx-translation-work-dispatch.md`
Script: [`scripts/verify-translation-dispatch.sh`](../../scripts/verify-translation-dispatch.sh)

This page is for a reviewer who wants to *watch* dispatch work rather than read
assertions about it. The green suite tells you the code agrees with itself; this
tells you a real Knoxx, given a real publication intent, asks the real ingestion
worker for a real translation — and refuses to ask twice.

## What dispatch is, in one paragraph

`domain.publication-gate` decides that a publication cannot happen yet because
no translation exists for the concrete revision it resolved, and it derives a
work item saying so. Until this card, that work item was a map in memory that
nothing consumed. Dispatch takes it, claims it under a stable identity, maps it
onto the ingestion worker's batch contract, and records the concrete revision
Knoxx asked about — because the worker's own contract has nowhere to put one.
When the worker later reports a document complete, that recorded binding is what
turns its answer into a translation receipt the gate can read.

## Run it

```bash
# from the repo root, with the backend running FROM THIS CHECKOUT
KNOXX_BASE_URL=http://localhost:8000 \
KNOXX_API_KEY=<the key the backend was started with> \
scripts/verify-translation-dispatch.sh
```

The script seeds its own fixture and removes it on exit — including on failure
and on Ctrl-C. It writes only inside `contracts/_verify_translation_dispatch/`
plus one probe source file, and it deletes the probe file only if this run
created it.

**Its identity is unique per run.** The document, publication ids, source file
and pinned revision all carry a run id. With a fixed identity, every run after
the first reused the previous run's *durable* dispatch claim — the second run got
`dispatch/duplicate` and verified nothing while appearing to pass.

**What it cannot tear down automatically**, printed as WARN every run: the Knoxx
dispatch record, and the OpenPlanner batch if one was created. Neither has a
delete surface, and adding one for a verification script would be the worse trade
— a route that erases translation evidence is a route that can erase *real*
evidence.

So the run **prints a scoped `mongosh` command** that removes exactly its own
records, keyed on this run's document id. It cannot touch anything else, and it
is a command an operator chooses to run rather than a deletion route that exists
forever. The batch belongs to another repository; it is claimed by the worker,
attempts one document, and terminates on its own.

### Preconditions it checks for you

- `curl` and `jq` present.
- `KNOXX_API_KEY` set.
- A backend answering `/health`.
- **That the running backend serves this checkout.** The PM2 processes on this
  machine may point at a different working copy; verifying against those
  verifies the wrong code. The script seeds its fixture and then asserts the
  backend can see it, failing loudly if not. If it fails here, check:
  ```bash
  pm2 describe knoxx-backend | grep cwd
  ```
- MongoDB connected, as a hard precondition. If it is not, the dispatch route
  answers **503**, and the script now **fails** rather than warning. That
  refusal is the designed behavior of the route — see *Why 503 and not a
  fallback* below — but it means sections 3–5 never ran, so a run that exited 0
  on it would have reported a pass for dispatch and idempotency checks that were
  skipped entirely. Start MongoDB and re-run.

## What each section proves

### 0. The running backend serves this checkout

Two publication intents are seeded on one document, and the contrast between
them is the whole fixture:

| intent | locale | source locale | translation required? |
| --- | --- | --- | --- |
| `<doc>-es` | `:es` | `:en` | yes — work is derivable |
| `<doc>-en` | `:en` | `:en` | no — nothing may ever be derived |

The garden declares `[:en :es]` as its accepted locales, which it must:
structural admissibility is now checked before dispatch, and a garden that does
not accept the target locale makes the intent inadmissible rather than merely
untranslated.

The same-locale intent is not filler. A dispatch that queued it would be asking a worker to
translate an English document into English, and the only thing preventing that
is `publication-gate/translation-required?` comparing the two locales.

The revision is **`:source/current`**, which the gate resolves to a content
digest of the probe file. That makes the run depend on the digest being
resolvable from the backend's working directory — deliberately, on two counts.
It is the path real intents take, and a pinned opaque token is now refused
outright: a pin must equal the digest Knoxx can observe, and `rev-something`
never can, so a pinned fixture would exercise only the refusal.

Path resolution itself is covered separately by
`backend/test/cljs/knoxx/backend/infra/publication_source_revision_test.cljs`.

### 1. The dispatch route refuses an unauthenticated caller

`POST /api/publications/translations/dispatch` mutates a queue on a shared
worker. A route that answers an anonymous caller lets anyone enqueue translation
work for the entire corpus, so the check is unconditional — including when the
policy database is disabled, where a nil auth context fails closed rather than
being read as permission.

### 2. A malformed request is refused, not reinterpreted

The body has exactly one field, and it is optional. That combination is what
makes an unrecognized field dangerous: silently ignored, `{"documnet": "..."}`
becomes a whole-corpus sweep the caller never asked for. The request contract is
closed, so it is a 400.

An unqualified document id is refused for the same family of reason:
`:probe` and `:knoxx.docs/probe` are different documents, and accepting the
former would sweep nothing while reporting success.

### 3. Derived work is dispatched and revision-bound

The response reports `considered` separately from `dispatched`, because an empty
dispatch list is ambiguous on its own — "nothing needed translating" and
"nothing was even looked at" read identically. You should see two considered,
one dispatched, and the dispatched one is `probe-es`.

Then the outcome. Two values, and neither means quite what its name suggests:

- **`dispatch/accepted`** — the claim is **in flight**, which is not the same as
  "the worker took the batch". It is returned both when the batch was created
  cleanly *and* whenever the outcome could not be established: several batches
  matched and none could be attributed to this dispatch, the batch listing came
  back at its 50-row cap so an absence proves nothing, or the observation call
  itself failed. In all of those the claim is deliberately left alone — a stuck
  claim is visible and fixable, a duplicate translation is neither.

  The `detail` field on the record says which case it was. When the batch really
  was created, the script additionally asserts the pinned revision appears in the
  recorded binding, which is the card's line *"a gated translation work item
  reaches the ingestion worker with a concrete revision"*.

  Observation never *binds* a batch, only rules out the "did not land"
  conclusion. A matching batch does not identify the request that created it —
  the batch record carries no dispatch id — so adopting one could attribute this
  claim to a batch that translated a different revision. The cost is on the card:
  an ambiguous send whose batch did land strands its claim, visibly, until an
  operator looks.

- **`dispatch/failed`** — the worker boundary refused **and** observation
  conclusively found no batch. Conclusive is the load-bearing word: only a
  listing shorter than the cap licenses this, because it is the only state in
  which absence is evidence. This is the one outcome that makes a claim
  retriable, so nothing else may produce it.

The asymmetry is the whole design. `accepted` is cheap to be wrong about — a
later pass observes again. `failed` is not: it invites a retry, and the batch
request has no idempotency key for a second call to collapse into. This is reported as a known
  gap, not as a pass and not as a silent skip. It is also a genuinely correct
  outcome: the claim was taken, the failure was recorded against it, and a later
  pass can re-dispatch. A failed dispatch left *in flight* would be the real
  defect — work that is never retried and never reported.

### 4. Asking twice does not translate twice — but a failure is retriable

Two different correct answers, depending on how section 3 ended.

**If the first dispatch was accepted**, the second identical request must come
back `dispatch/duplicate` and must not create a second batch. This is the check
that catches a dispatch that called the worker first and recorded afterwards:
between those two steps a second pass sees no claim, enqueues again, and the
second translation cannot be withdrawn because nothing recorded that the first
was already asked for.

**If the first dispatch failed** — meaning conclusively, per section 3 — the
second request must come back `dispatch/accepted` with a *new* batch. A failed attempt is finished but not
done — no translation came of it, so the gate still reports the translation
missing and the work genuinely still needs doing. Answering `duplicate` here
would strand that source revision permanently: every later pass would decline to
enqueue while the gate went on asking for a translation that could never arrive.
The script fails on that answer specifically.

The retry replaces the claim wholesale rather than editing its outcome, which is
what clears the failed attempt's batch id. Left behind, the old batch's
completion report would resolve the new attempt and mint a receipt for a
translation the new attempt never produced.

### 5. Dispatch alone publishes nothing and fabricates no translation

Dispatch *asks for* a translation. It must not claim one exists, and it must not
make anything public. The script checks the response carries no
`translation/completed` and no `publication/materialized`, and that the CMS view
still reports nothing materialized for the seeded document.

## Why 503 and not a fallback

With MongoDB down, the route refuses instead of falling back to the in-memory
store. That is deliberate. The in-memory store would happily accept the
dispatch — and then lose the revision binding on the next restart. The worker
would go on to translate successfully, its completion report would arrive, and
nothing would exist to join that answer to a revision. The receipt would never
be minted, and the gate would report that translation as never done, forever.
A 503 an operator can see beats a translation that silently disappears.

The script's treatment of that 503 is the opposite question, and it is a
failure. The route is right to refuse; a verification run that refused to
dispatch anything has verified nothing about dispatch, and must not exit 0.

## Who may make Knoxx believe a translation exists

Only a system-admin worker principal. `POST
/api/translations/batches/:id/status` is guarded by `org.translations.manage`,
which org admins hold too — correct for the route's original job of moving a
batch through the worker queue, and not sufficient for the evidence step added
beside it.

Without the narrower check, an org admin could dispatch work and immediately
report `completed_document` for it, producing a completed-translation receipt for
a translation that never ran — and that fabricated receipt is exactly what a
publication gate is waiting on. `next-batch-op` had already closed the same gap
for batch claiming; this closes it for evidence.

The check is deliberately narrower than the route: an org admin may still update
batch status, because that is the worker queue's own business. They simply
cannot mint evidence. A non-worker report comes back with
`translation.skipped.reason = worker-principal-required` rather than being
silently ignored, so the distinction is visible to whoever is debugging.

## Known gaps, printed every run

Both of these print as `WARN` so they stay visible without making the script
permanently red:

1. **The worker actually translating.** Whether a translation comes back depends
   on OpenPlanner and a model being reachable. That is not this card's surface;
   this card ends at handing the worker a batch and recording what it said.
2. **The completion half, end to end.** Minting a receipt needs a real batch to
   reach `complete` or `partial`, which needs (1). The join, every refusal, and
   the resulting receipt are covered by
   `backend/test/cljs/knoxx/backend/infra/translation_dispatch_test.cljs` —
   including that the same report twice cannot mint two receipts, that another
   batch's answer cannot resolve this binding, and that a failed attempt is
   never mistaken for a translation.

Closing gap 2 against live infrastructure is
`kanban/tasks/knoxx-website-publication-live-verification.md`, which is the card
that owns one recorded end-to-end run.

## No browser tour

This card adds no UI. Dispatch is an operator action over HTTP and a hook on a
route the worker calls; there is no control for a human to click, so a tour would
have nothing to screenshot. The approval surface
(`kanban/tasks/knoxx-translation-approval-surface.md`) is the card that
introduces a reviewable UI action, and the tour belongs with it.
