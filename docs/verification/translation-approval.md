# Translation approval surface — human verification

Card: `kanban/tasks/knoxx-translation-approval-surface.md`
Script: [`scripts/verify-translation-approval.sh`](../../scripts/verify-translation-approval.sh)

An approval is the second of the two evidential questions
`domain.publication-gate` asks. The first — has this been translated — is
`knoxx-translation-work-dispatch`. This one is: did an authorized human accept
*that exact translation*.

## What an approval is, and is not

It is **not** a permission over a document or a locale. It is evidence about one
pair of concrete revisions:

| field | meaning |
| --- | --- |
| `:review/revision` | the concrete **source** revision the gate keys evidence by |
| `:review/translation-revision` | the produced output that was actually reviewed |

Both, because either alone is wrong. Keyed only by the source revision, an
approval would survive a re-translation and go on authorizing bytes nobody
looked at. Keyed only by the output, the gate could never find it — a publication
intent resolves to a source revision and has no idea what output exists.

The gate reads the first. `domain.translation-evidence/approved?` joins them, and
an approval whose output revision no longer matches the current receipt simply
stops being current. Not deleted, not an error — exactly what the gate's own
docstring already said would happen to a stale translation's approval.

### A card premise corrected

The card asks for approval "against translation identity, target locale, and
concrete **translated** revision". Taken literally that produces approvals the
gate can never match, because the gate asks with the source revision. Both are
recorded; the one the gate is keyed by is the source revision. Annotated on the
card rather than silently implemented the other way.

## Run it

```bash
KNOXX_BASE_URL=http://localhost:8000 \
KNOXX_API_KEY=<the key the backend was started with> \
scripts/verify-translation-approval.sh
```

Unique identity per run, fixture torn down on exit, failure and Ctrl-C.

## What each section proves

### 1. Review evidence cannot be manufactured anonymously

The check is unconditional, including when the policy database is disabled —
`with-request-context!` hands down a nil context there, and reading that as
permission would let an anonymous caller produce exactly the evidence a
publication gate is waiting on.

Permission is `org.translations.review`, which the `translator` role already
holds. Deliberately **not** `org.translations.manage`: approving is what a
reviewer does, and requiring queue-management authority would mean only admins
could review.

### 2. A caller cannot attribute an approval to someone else

The request contract is closed and carries no principal, timestamp, tenant or
project. The script tries to send each one and expects a 400.

The tenant and project are inherited from the **receipt** being approved, not
from the request — so a reviewer cannot file an approval into another
organization's evidence. That is a lesson from the dispatch card, which needed
two review rounds to learn that translation evidence is tenant- and
project-scoped; it is built in here from the start.

### 3. Malformed identifiers are refused, not reinterpreted

Unknown field, unqualified document, blank field, missing field, and a
selector-shaped revision.

The closed contract is checked against the **raw body**, not against the map the
adapter builds — an unknown field is never copied into that map, so validating
only the result would silently accept a typo. The dispatch adapter learned that
one the same way.

`"source/current"` is refused because this is a boundary where a revision arrives
as decoded wire input, and a selector gives a stable-looking identity to a moving
target.

### 4. Approving a translation that does not exist is refused

**409, not 404.** The request is well formed and the document exists; the system
simply is not in a state where approving means anything yet. A mismatch is 409
for the same reason — the caller is not wrong about syntax, it disagrees with
recorded fact. Every refusal carries typed evidence with both sides named, so a
reviewer can see whether their request or the recorded translation was stale.

With MongoDB down the route answers **503** rather than recording an approval
that would vanish on restart. An approval that does not survive a restart is
worse than none: the gate would admit a publication today and block it tomorrow.

## Known gaps, printed every run

1. **The successful-approval path.** Recording one requires a completed
   translation receipt in the durable store, which requires the ingestion worker
   to have actually translated something — the dispatch card's surface, and not
   reachable over HTTP from here. Covered by
   `backend/test/cljs/knoxx/backend/infra/routes/translation_review_test.cljs`:
   attribution from the receipt rather than the request, the idempotent
   double-approval, tenant and project isolation, refusal with both sides named,
   and that approval materializes nothing.
2. **Whether an approval unblocks a publication.** That is
   `knoxx-publication-reconciler-runtime`, the next card. Approval makes a plan
   admissible; it must not itself publish, and a test pins that.

## No browser tour yet

The approval action is an authorized HTTP call with no UI attached to it in this
card. The CMS surface that would let a reviewer click it is not part of this
slice, so a tour would have nothing to drive — and per AGENTS.md, a tour that
quietly avoids the hard part is worse than no tour. When the CMS action lands,
the tour belongs with it.
