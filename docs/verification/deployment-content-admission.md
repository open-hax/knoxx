# Deployment content admission — human verification

Script: [`scripts/verify-deployment-content-admission.sh`](../../scripts/verify-deployment-content-admission.sh)

Specification: [`docs/specs/deployment-content-admission.md`](../specs/deployment-content-admission.md)

This is the live, user-reachable proof for the deployment admission boundary.
It creates one unique anchored English document and garden with Spanish and
French draft publication relations, calls the same anchor-sweep endpoint used
by Services, and proves that indexing and translation dispatch happen without
publishing. Both identities are run-scoped, so a retained review demo cannot
collide with a later verification run.

The script exits nonzero when any required assertion fails. A final green count
therefore means all checks below actually ran; an unavailable database, hidden
source, missing trigger, missing embedding, or ambiguous HTTP result is red.

## Run it

From the Knoxx repository root, with a backend running from this checkout:

```bash
KNOXX_BASE_URL=http://localhost:8000 \
KNOXX_API_KEY=<the key used by the running backend> \
KNOXX_VERIFY_PUBLICATION_CONTENT_ROOT=/absolute/path/to/publication-content \
scripts/verify-deployment-content-admission.sh
```

To add the isolated generated-post path (one post-drafter followed by its two
non-source translators), point the verifier at the same generated resource root
as the backend:

```bash
KNOXX_BASE_URL=http://localhost:8000 \
KNOXX_API_KEY=<the key used by the running backend> \
KNOXX_VERIFY_GENERATED_DRAFTS=true \
KNOXX_GENERATED_CONTRACTS_DIR=/absolute/path/to/generated-contracts \
KNOXX_VERIFY_PUBLICATION_CONTENT_ROOT=/absolute/path/to/publication-content \
scripts/verify-deployment-content-admission.sh
```

To leave a green, clearly titled local fixture available to the translation
review surfaces, use the explicit keep mode. It implies generated verification:

```bash
KNOXX_BASE_URL=http://localhost:8000 \
KNOXX_API_KEY=<the key used by the running backend> \
KNOXX_VERIFY_KEEP_REVIEW_DEMO=true \
KNOXX_GENERATED_CONTRACTS_DIR=/absolute/path/to/generated-contracts \
KNOXX_VERIFY_PUBLICATION_CONTENT_ROOT=/absolute/path/to/publication-content \
scripts/verify-deployment-content-admission.sh
```

Default behavior is still strict teardown. Keep mode retains files only after
every assertion is green and the generated translations/vectors have settled;
a red or interrupted run cleans safe owned files and never calls a failure a
demo. The final output prints the source/generated document ids, review URL,
and exact file and Mongo cleanup commands.

The defaults are:

- `KNOXX_BASE_URL=http://localhost:8000`
- `KNOXX_CONTRACTS_DIR=$PWD/contracts`
- `KNOXX_VERIFY_HTTP_TIMEOUT_SECONDS=120`
- `KNOXX_VERIFY_AGENT_WAIT_SECONDS=360`
- `KNOXX_VERIFY_GENERATED_DRAFTS=false`
- `KNOXX_VERIFY_KEEP_REVIEW_DEMO=false`
- `MONGODB_EVENTS_COLLECTION=events`
- `MONGODB_VECTOR_HOT_COLLECTION=event_chunks`

`KNOXX_VERIFY_PUBLICATION_CONTENT_ROOT` has no default. It must explicitly name
the running backend's publication content root so the verifier can authenticate
and safely name each exact `.translations/<digest>.edn` output it owns.

Only HTTPS and exact loopback HTTP are accepted. On loopback the script also
disables proxies for every request, so a proxy cannot receive the API key.

## Preconditions

- `curl`, `jq`, `sha256sum`, and `realpath` are installed.
- `KNOXX_API_KEY` resolves to a principal with
  `org.translations.manage`, `org.publications.manage`, and read access to
  publication/CMS projections.
- The running backend scans the same contract root the script writes (the
  authored root normally; the configured external generated root in keep
  mode). Step 0 proves this by creating a unique fixture and waiting until
  `GET /api/publications/documents` can see it. If that check fails, inspect the
  running process working directory and `KNOXX_CONTRACTS_DIR` rather than
  continuing against the wrong checkout.
- MongoDB and the embedded OpenPlanner indexing path are live. Event and vector
  reads are required checks, not optional diagnostics.
- For the Services local deployment, host Ollama has both `gemma4:e2b` and
  `nomic-embed-text`. The Services health verifier checks the model inventory
  and a real 768-dimensional embedding before it calls admission. This script
  independently proves that every source, index, and candidate event has an
  exact 768-dimensional `nomic-embed-text` vector in the configured OpenPlanner
  vector collection.
- Generated verification additionally requires GNU `realpath` and the backend's
  exact `KNOXX_GENERATED_CONTRACTS_DIR`. The script rejects a generated root
  inside the Knoxx checkout, derives the deterministic files before dispatch,
  and aborts if either path already exists.

## The fixture

Each run owns one directory:

```text
contracts/_verify_deployment_content_admission/
├── resources.edn
└── source-<run-id>.md
```

In keep mode the equivalent source/garden manifest and source are created under
the external generated root instead:

```text
<generated-contracts>/namespaces/knoxx-review-demo-<run-id>/resources.edn
<generated-root>/review-demos/<run-id>/source-<run-id>.md
```

They are titled “Local Translation Review Demo …” in the CMS/review projection.

`resources.edn` declares:

- one unique `:document/anchor? true` English document;
- one active garden accepting `:en`, `:es`, and `:fr`;
- one Spanish and one French publication relation;
- `:publication/state :draft` and `:translation/review :required` on both
  relations; and
- `:document/generate-drafts? false` for this fixture.

Draft generation is disabled during the baseline anchor sweep. This keeps its
replay proof limited to one source revision and two translation claims. The
Services deployment hook sends `{"anchors":true,"generateDrafts":true}`; this
script can exercise the equivalent generation semantics in a bounded, isolated
second phase by setting `KNOXX_VERIFY_GENERATED_DRAFTS=true`. That phase requests
generation for the exact fixture document once, never repeats the whole anchor
sweep, and waits for the generated post's Spanish and French candidate events
before exact-file cleanup. Across the full opt-in run, Gemma receives two source
translations, one post-drafting turn, and two generated-post translations.

## What each section proves

### 0. The backend serves the selected resource root

The script first writes only `resources.edn`, leaving the declared source
absent. It polls the publication resource projection for the unique document.
If the backend cannot see it, the run aborts before making any admission claim.
For the default fixture this also proves the active checkout. In keep mode it
proves that the active backend scans the exact external generated root where the
review demo will remain.

### 1. Anonymous admission is refused

An unauthenticated exact-document request must return 401 or 403. Admission
persists source and index events and starts translation agents, so anonymous
access would be an unbounded write surface.

### 2. Missing source fails before a durable prefix

The authenticated exact-document request names a valid anchored resource whose
source does not yet exist. It must return 409 with
`document_source_missing`. A scoped Mongo query then proves that no event for
that document was written. The entire selected set is preflighted before the
first immutable write; a broken anchor cannot leave a deployment half-admitted.

### 3. The deployment-shaped sweep indexes and dispatches

After writing nonblank source bytes, the script calls:

```http
POST /api/publications/documents/admit
Content-Type: application/json

{"anchors":true,"generateDrafts":false}
```

The response must be HTTP 200 with `ok=true`, numeric `admitted>=1`, numeric
`failed=0`, at least two indexed events, and an array of per-document results.
The script then selects its own unique result instead of assuming the fixture
was the only deployed anchor.

For that row it asserts:

- the source revision exactly equals the SHA-256 digest of the bytes on disk;
- the `docs` snapshot and `publication.document.indexed` event are both
  `recorded`;
- exactly two admissible relations reached the `agent` runner;
- both dispatches are `dispatch/accepted`, carry bound run ids, and did not
  report a missing enabled trigger; and
- neither the document row nor its dispatch summary hides a failure.

Acceptance is intentionally asynchronous: it proves that immutable claims and
local agent runs exist, not that Gemma has finished. Triggered turns enter the
process-local FIFO bounded by `KNOXX_EVENT_AGENT_CONCURRENCY` (default `1`) and
`KNOXX_EVENT_AGENT_QUEUE_LIMIT` (default `256`). A full queue records a failed
run and makes the deploy response red. The shipped local and Services
environments also set `KNOXX_EVENT_AGENT_TURN_TIMEOUT_MS=300000` to cap each
event-triggered provider turn at 300 seconds, without capping interactive chat,
so a hung request cannot hold the single-worker FIFO forever. A five-second
provider-abort grace then fail-stops Knoxx for supervisor restart and durable
replay if the provider cannot become idle; the FIFO never advances into a
still-live tool-capable turn. Durable claims and deterministic event
identity recover translation work on the next admission after a restart;
terminal failed/no-tool turns are made retriable rather than left accepted.
For translation turns, terminal settlement first recovers any durable candidate
prefix and then requests only missing splits from Ollama native `/api/chat`
using `think: false`, temperature/seed zero, and a strict one-field
`translated_text` schema. Knoxx, not the model, binds every identity coordinate
before submitting that value through the durable translation sink. Text that
imitates `save_translation(...)`, malformed JSON, extra keys, blank output,
wrong-model responses, and unfinished responses all fail closed.
Drafting uses the same terminal callback but releases only its own indexed
event when no deterministic recursive-admission completion marker and complete
immutable source/manifest pair exist. A manifest can already exist after a
failed recursive admission and is therefore not treated as completion. When
both immutable files exist without that marker, a retry re-admits those exact
files even if the later model answer differs; after completion, different bytes
remain a conflict. There is no autonomous retry timer. Steps 5 and 8 separately
wait for model completion.

### 4. Both admission events are indexed and embedded

The script queries `/api/data/mongo/query` by the two event ids returned from
admission. Exactly one `docs` row and one `publication.document.indexed` row
must exist, and both retain the fixture document id and exact source revision.

It then queries `event_chunks` by parent id. Both events must use
`nomic-embed-text`, declare exactly 768 dimensions, and carry an embedding array
of exactly 768 values. This is the end-to-end indexing assertion: a 200
admission response without the requested searchable vectors is not green.

### 5. Translation agents finish and persist events

The verifier polls the exact document and source revision for completed
`translation.segment` events. Its wait loop can recognize `meta` as a legacy
compatibility fallback, but a green event must persist every query-critical
field in `extra`. It requires both Spanish and French output, nonblank source
and translated text, at least one changed split per target locale,
`in_review` status, the `knoxx-contract-agent` producer, and `gemma4:e2b` as the
pinned execution model. It then extracts every stable candidate event id and
requires at least one exact 768-dimensional `nomic-embed-text` vector for each.
A merely accepted dispatch or an event row without its vector projection cannot
make this verifier green.

The writer enforces the same postcondition and can repair completed stable event
rows whose vector projection is absent or invalid without appending another
base event. Persisted metadata repair only backfills missing values; it refuses
to replace a conflicting non-null fact. If immutable receipt completion wins a
race with a failed event projection, settlement replays the exact stored final
pair through the same sink; it does not ask Gemma to regenerate durable bytes.

### 6. Unchanged admission is idempotent

The same anchor sweep is sent again without changing the source. The replay
must return the same source and index event ids with `existing` status. Mongo
must still contain exactly two admission event rows and the candidate event id
set must be unchanged. Once completed work is no longer missing, dispatch may
return an empty list; if it reports outcomes, every outcome must be
`dispatch/duplicate`. Any new `dispatch/accepted` is a failure.

A separate query of `knoxx_translation_dispatches` proves there is exactly one
claim per declared target locale (`es` and `fr`), not merely two arbitrary rows
for the document.

This is serialized replay idempotency. OpenPlanner does not currently own a
unique index on `events.id`, so this check does not claim that two concurrent
writers cannot race; it proves the deployment/re-admission path Knoxx actually
serializes.

### 7. Review remains pending and drafts remain unpublished

The review inventory must contain exactly the fixture's Spanish and French
publication relations, both in the fixture garden, both unapproved, and neither
in approved work state. Each row must hydrate displayable, digest-authenticated
agent bytes, and its output revision must equal the corresponding durable
translation event before the verifier records the exact content path. An
anonymous CMS PATCH must fail with 401/403. The
script then sends one authenticated idempotent CMS PATCH with
`{"state":"draft"}` and requires the response to remain unmaterialized. The CMS
GET must then report both desired states as `draft` and both observed
materializations as absent. No `published` state is ever sent.

Finally, the publication receipt endpoint either contains no receipt naming the
fixture, or explicitly reports that reconciliation is unconfigured. The script
never calls the reconciler. It cannot publish the fixture even if an
asynchronous translation finishes while the checks are running.

This draft-only verifier does not claim that review caused non-publication:
`:draft` is already structurally incapable of publishing. The companion
`scripts/verify-translation-split-review.sh` uses an isolated `:published`
intent and proves causality by reconciling before approval, requiring an exact
`translation-review-required` blocked receipt, and observing that no static
artifact was written.

### 8. Optional generated post is terminal, translated, and unpublished

When `KNOXX_VERIFY_GENERATED_DRAFTS=true`, the verifier derives the generated
document id from the canonical source/revision/locale, organization/project,
and sorted garden/locale topology before it dispatches anything. It derives the
exact manifest, source, and recursive-admission completion-marker paths from
that id. All three must be absent and outside the repository. It then admits
the exact fixture with `generateDrafts:true` and waits for `gemma4:e2b` to write
one nonblank Markdown source and manifest and for recursive admission to write
the matching topology-fingerprint marker.

The live resource projection must prove that the generated document:

- retains `derived-from` and `derived-source-revision` lineage;
- carries `generate-drafts? false`, making it terminal for further generation;
- has exactly `en`, `es`, and `fr` publication relations, all in `draft`;
- gives the source-locale relation review policy `none`;
- gives Spanish and French review policy `required`; and
- pins every relation to `open-hax.publication/static-site`.

Terminal generation does not disable translation. The script waits for Spanish
and French `translation.segment` events for the generated source revision,
requires the same Gemma metadata and exact nomic vectors as the original
document, and checks that both target review rows remain unapproved. CMS must
show all three generated relations as desired drafts with no observed
materialization, and the receipt journal must contain no generated-document
publication receipt. As with the source translations, event output revisions
must exactly equal the authenticated review rows and map to two concrete files
under the configured publication content root.

When the option is false, step 8 emits an explicit WARN rather than claiming the
post-drafter ran.

## Cleanup and durable evidence

`EXIT`, `INT`, and `TERM` all run the same teardown. By default, the owned
fixture directory, including the source, is removed on success, assertion
failure, abort, and Ctrl-C. Failure to remove it turns an otherwise green run
red. Explicit keep mode is the only exception: after a wholly green generated
run, its clearly titled source/garden fixture is retained under the external
generated root so the review UI remains testable.

In ordinary generated mode, the trap removes only the three precomputed,
run-owned files — the generated namespace manifest, Markdown source, and
recursive-admission completion marker — and
only after both generated translation agents have produced their durable
candidate events and exact vector projections. If
the post-drafter or either translator has not settled, deleting those resources
could race the internal admission, so the trap leaves them intact and prints an
exact three-file `rm -f` command for use after settlement. It never recursively
deletes the configured generated root.

Keep mode retains those three generated files together with its source/garden
fixture and the four exact source/generated agent translation entries, and
prints every deletion target. Ordinary green teardown removes only those exact
translation files after review/event identity agreement and complete vector
settlement. A partial, mismatched, failed, or interrupted run retains any known
translation entries and prints their exact paths; it never deletes the
`.translations` directory or uses a glob. A failed keep-mode run does not retain
a purported demo; other safe files are torn down using the same exact targets.

Admission events and translation claims are different: they are immutable or
may still be in use by asynchronous agent sessions. Deleting them in the trap
could corrupt a legitimate completion, so the script prints one explicit WARN
and a narrowly scoped `mongosh` command keyed by its source and optional
generated document/event ids. Run that command only after all agent sessions
settle. Runtime/session telemetry remains governed by its normal retention
policy; the cleanup command
targets only admission/index, translation evidence, review, and graph/vector
rows attributable to the fixture.

The warning is not a skipped assertion. Durable residue is an acknowledged
operational boundary; authentication, missing-source atomicity, indexing,
embedding, completed `gemma4:e2b` output events, replay identity, translation
claim deduplication, authenticated pending review, and non-publication are all required to
exit zero. In generated mode the same is true of lineage, terminal generation,
translated-locale review policy, exact files/vectors, and absent materialization.

## Reading a failure

Every assertion prints a descriptive `PASS` or `FAIL`. At the end, all failed
labels are repeated and the process exits 1. Precondition failures use `ABORT`
and exit 2. Signal interruption preserves the conventional 130/143 exit code
after teardown.

Common first checks are:

```bash
curl -fsS "$KNOXX_BASE_URL/health"
pm2 describe knoxx-backend | grep cwd
curl -fsS http://localhost:11434/api/tags | jq '.models[].name'
```

Do not repair a red run by invoking reconciliation or changing the seeded
publication states. Those operations would invalidate the property this
artifact exists to prove: admission and translation work remain strictly on
the unpublished side of the human review gate.
