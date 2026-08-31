# Resource translation split review

Script: [`scripts/verify-translation-split-review.sh`](../../scripts/verify-translation-split-review.sh)

Browser tour: [`scripts/verify-translation-split-review-tour.sh`](../../scripts/verify-translation-split-review-tour.sh)

This is the human verification artifact for the resource-CMS translation review
workflow. It makes the failure that motivated the work visible: eighteen desired
resource relations must produce eighteen rows even when only one completed
translation exists. That one candidate must expose its real persisted splits,
the historical granular review card, explicit reject/edit/approve actions, and
revision-bound whole-output approval.

## What the verifier owns

Each run creates a unique resource namespace with eighteen English documents
and eighteen Spanish publication intents. One source has three paragraphs. A
small ClojureScript helper uses Knoxx's production split constructors, digest
function, Mongo stores, dispatch law, and agent-content writer to admit a
three-split candidate for that source. The completion receipt is bound to a
fresh dispatch attempt: the helper reserves the exact resource-derived key,
binds a batch, derives the candidate revision from that bound dispatch, claims
completion, mints the receipt through the production constructor, and only then
finishes the dispatch. It does not hand-author opaque split ids or add a fixture
endpoint to the service.

All identities include the run id. The signal-safe cleanup trap removes:

- the dedicated contract/source directory;
- the exact dispatch/completion owner, turn, candidate splits, candidate set,
  split reviews, completed receipts, and whole-output approvals created for the
  run;
- every immutable `.translations` entry referenced by those receipts; and
- any static-site route, artifact, and deterministic publication idempotency
  reservation created for the run.

Cleanup failure turns an otherwise green run red. The script never broad-matches
production evidence. The reconciliation route's receipt journal is intentionally
process-local diagnostics; a successful materialization entry remains visible
there until the backend restarts, but no route, bytes, idempotency claim, review,
or translation evidence remains authoritative after teardown.

## Live prerequisites

The scripts intentionally fail before seeding if the environment cannot support
meaningful evidence.

1. Start the backend from the checkout under review. Its `CONTRACTS_DIR` must be
   this checkout's contracts directory. The verifier writes a unique resource
   and refuses to continue unless that exact resource appears over HTTP. This
   catches the common case where PM2 is serving another working copy.
2. The backend must have Mongo translation evidence and split stores enabled.
   Export the same `MONGODB_URI`/`MONGODB_DB` (or
   `OPENPLANNER_MONGODB_URI`/`OPENPLANNER_MONGODB_DB`) to the verifier.
3. Set `KNOXX_PUBLICATION_CONTENT_ROOT` to the exact filesystem root mounted in
   the backend. The helper writes and later removes immutable candidate content
   there, and the walkthrough inspects the static-site manifest and artifact
   written by production reconciliation. A backend without this production
   target fails the walkthrough instead of treating approval as publication.
4. Set `VERIFY_ORG_ID` to the organization id resolved for the API key/browser
   identity. This is not guessed: review evidence is tenant-scoped, and seeding
   under a guessed tenant would only prove that the join correctly returns
   nothing.
5. Install repository dependencies. The helper runs with
   `backend/node_modules/.bin/nbb` and derives the pinned Malli/Katamorph
   classpath from `backend/deps.edn`; `clojure`, Java, and `unzip` must be
   available.
6. The API key or browser identity needs publication read/manage permission as
   well as access to the translation review routes. Reconciliation is a real
   public-content mutation, although every fixture path and resource id is
   unique to the run and the cleanup removes it.

Credential transport fails closed. `KNOXX_BASE_URL` and
`KNOXX_FRONTEND_URL` may use plain HTTP only for the exact hosts `localhost`,
`127.0.0.1`, or `[::1]`; every other host requires HTTPS. URL userinfo is
always refused. Curl ignores curlrc files with `-q`, and loopback checks bypass
all proxies. The browser tour likewise launches its loopback session without
HTTP proxy variables, so an API key or local-password session cannot be routed
off-host by ambient client configuration.

For the HTTP walkthrough:

```bash
KNOXX_API_KEY=... \
VERIFY_ORG_ID=... \
MONGODB_URI=mongodb://localhost:27017 \
MONGODB_DB=openplanner \
KNOXX_PUBLICATION_CONTENT_ROOT=/absolute/path/to/publication-content \
scripts/verify-translation-split-review.sh
```

The script reads the project coordinate from the live review response before it
seeds evidence, so it follows the backend's configured
`KNOXX_SESSION_PROJECT_NAME` instead of assuming the historical default.

## What the HTTP walkthrough proves

### Checkout and cardinality

Before any receipt is seeded, all eighteen fixture relations appear as missing,
dispatchable rows. After one candidate is seeded, the response still has
eighteen rows: one exact-byte hydrated agent candidate and seventeen missing
rows. The candidate carries its server-owned candidate-set id and three ordered
split ids. This is the check that fails when receipt inventory and resource
inventory become separate translation systems again.

### Authorization and failure modes

The verifier calls the inventory, single-split review, document review,
whole-output approval, and reconciliation surfaces without credentials and
requires 401/403. It also requires closed request contracts to refuse:

- caller-supplied principal/tenant fields;
- a missing `split_id` on the granular route;
- `corrected_text` on the document fast path;
- the obsolete `risk: "unsafe"` spelling (the closed values are `safe`,
  `sensitive`, and `policy_violation`); and
- a syntactically valid candidate set that was never persisted.

Whole-output approval is attempted while splits are incomplete and must return
409. It is attempted again after a later rejection and must still return 409.

### Granular review and memory

The first split is submitted with explicit adequacy, fluency, terminology,
risk, correction A, and editor notes. A reload must return every value. An
exact double-submit must return `existing`, not append a second semantic fact.
The reviewer then approves correction B. A reload must expose two attributed,
newest-first immutable labels—B followed by A—while the editable card hydrates
from B. This catches the failure mode where only the latest form values survive
and the historical label machinery silently disappears.

After the corrected split is approved, the helper queries
`ITranslationSplitStore.applicable-memory!` through the production Mongo
adapter. It derives the fixture set's allowlist from current completed receipts,
point-checks their exact dispatch attempt, and passes
`current-candidate-set-ids` just as runtime memory admission does. Correction
B—not A or the raw model candidate—must be the target in a future
translation-memory example.

### Reject/edit/approve and whole-output gating

The walkthrough records an individual rejection, then drives `Needs Edit`,
`Reject All`, and `Approve All` through the document endpoint. The server, not
the client, enumerates the persisted set. A correction is illegal on that wire,
but document review must preserve the already accepted per-split correction;
the verifier checks both the refreshed card and the future-memory projection
after `Approve All`.

Only the fully approved current projection can receive whole-output approval.
An exact replay is idempotent. A later split rejection must make that approval
stale immediately; the inventory must report `approved: false` and refuse a new
whole approval until the set is repaired.

Approval is not counted as publication. After approval, the verifier calls the
production reconciliation route, requires a materialized receipt, reads the
committed static-site route and artifact through the production adapter, and
requires the HTML to contain correction B but not superseded correction A.

## Browser tour and screenshots

The browser tour needs the same backend/Mongo/content variables, plus a running
frontend and either a scoped API key or a real local password session. CI uses
an ephemeral API key so no human password enters workflow state:

```bash
KNOXX_FRONTEND_URL=http://localhost:5173 \
KNOXX_API_KEY=... \
KNOXX_USER_EMAIL=reviewer@example.test \
KNOXX_ORG_SLUG=open-hax \
VERIFY_ORG_ID=... \
MONGODB_URI=mongodb://localhost:27017 \
MONGODB_DB=openplanner \
KNOXX_PUBLICATION_CONTENT_ROOT=/absolute/path/to/publication-content \
scripts/verify-translation-split-review-tour.sh
```

For a manual password-session run, replace `KNOXX_API_KEY` with
`KNOXX_DEV_EMAIL` and `KNOXX_DEV_PASSWORD`. The tour refuses to treat
local-storage identity hints as authentication. It establishes either an API
key header context or a real session cookie, verifies the frontend proxy can
see the run-scoped fixture, selects dark mode with reduced motion, and then
captures:

1. an exact DOM count of eighteen resource rows, with top and bottom captures
   that include row 18 in the internally scrolling inventory;
2. the selected three-split candidate;
3. adequacy/fluency/terminology/risk, correction, notes, and four split actions;
4. submitted in-review state;
5. the two immutable A→B labels rendered in the `Existing labels` card;
6. `Skip` advancing without a review mutation;
7. an individual rejection;
8. `Needs Edit`, `Reject All`, and `Approve All` states;
9. whole-output approval plus inspection of correction B in the committed
   static artifact; and
10. a later rejection revoking that approval.

It finishes with an unauthenticated same-origin request. API-key headers are
cleared first and `credentials: "omit"` prevents a real session cookie from
making that check authenticated. Screenshots are written to a run-specific directory below
`docs/verification/screenshots/`, which is gitignored; a failed capture cannot
be mistaken for an older run's evidence.

Pull-request CI runs `scripts/verify-translation-browser-ci.sh`. The wrapper
boots an isolated backend and frontend, derives the organization identity from
the authenticated context, runs this same tour, and uploads its screenshots.
It requires a disposable Mongo database supplied by the workflow.

## Honest boundary

The fixture does **not** ask a model to translate. That is deliberate: provider
availability and output quality would make a review verifier nondeterministic,
and a failed provider would prevent a reviewer from ever reaching the controls
this artifact exists to inspect. Both scripts print this as `WARN`, not as a
silent omission.

Both scripts are live-service checks, not hermetic tests. They require a backend
and frontend built from this checkout, reachable Mongo, real authorization, and
the backend's mounted publication content root. If any of those are absent the
scripts stop; a static or partial run must not be reported as end-to-end proof.

Run [`scripts/verify-translation-dispatch.sh`](../../scripts/verify-translation-dispatch.sh)
separately to inspect live agent dispatch/provider availability. Together the
two artifacts distinguish “could the producer create work?” from “can a human
review, correct, reject, approve, and reuse the produced work?”
