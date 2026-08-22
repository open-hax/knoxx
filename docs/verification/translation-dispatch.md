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
- MongoDB connected. If it is not, the dispatch route answers **503** and the
  script reports it as a known gap rather than a pass. That refusal is the
  designed behavior, not a bug: see *Why 503 and not a fallback* below.

## What each section proves

### 0. The running backend serves this checkout

Two publication intents are seeded on one document, and the contrast between
them is the whole fixture:

| intent | locale | source locale | translation required? |
| --- | --- | --- | --- |
| `probe-es` | `:es` | `:en` | yes — work is derivable |
| `probe-en` | `:en` | `:en` | no — nothing may ever be derived |

`probe-en` is not filler. A dispatch that queued it would be asking a worker to
translate an English document into English, and the only thing preventing that
is `publication-gate/translation-required?` comparing the two locales.

The revision is **pinned** rather than `:source/current`, so this run does not
depend on the probe file's digest being resolvable from whatever working
directory the backend was started in. The digest path — content-addressed source
revisions — has its own coverage in
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

Then the outcome:

- **`dispatch/accepted`** — the worker took the batch. The script additionally
  asserts the pinned revision appears in the recorded binding, which is the
  card's line *"a gated translation work item reaches the ingestion worker with
  a concrete revision"*.
- **`dispatch/failed`** — the worker boundary refused, almost always because
  OpenPlanner is not reachable from this backend. This is reported as a known
  gap, not as a pass and not as a silent skip. It is also a genuinely correct
  outcome: the claim was taken, the failure was recorded against it, and a later
  pass can re-dispatch. A failed dispatch left *in flight* would be the real
  defect — work that is never retried and never reported.

### 4. Asking twice does not translate twice

The second identical request must come back `dispatch/duplicate` with the same
identity, and must not create a second batch. This is the check that would catch
a dispatch that called the worker first and recorded afterwards: between those
two steps, a second pass sees no claim, enqueues again, and the second
translation cannot be withdrawn because nothing recorded that the first was
already asked for.

If the first dispatch failed (section 3), the second ask may legitimately derive
nothing or re-dispatch; the script reports that rather than asserting a duplicate
it cannot expect. A fresh `dispatch/accepted` for a claim still in flight is the
only unacceptable answer, and it fails.

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
