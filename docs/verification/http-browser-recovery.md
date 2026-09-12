# Browser transport recovery — 12 September 2026

This checkpoint restores the actual Fastify and MCP boundaries exposed by the
earlier browser failures. It is not a claim that the complete stack or PR is ready.

| Obstacle | Resolution and proof |
| --- | --- |
| A rejected session became HTTP 500 instead of its owned 401/403/503 response | A named Fastify adapter now creates native errors from a closed portable failure contract. The callback hook awaits resolution without returning a second completion promise. |
| Falsy thrown values could look like successful session resolution | The adapter carries an explicit success flag. Actual Fastify injection refuses nil/false failures; 503 response details remain opaque. |
| A generated qualified Wiki document ID exceeded Fastify's default parameter limit | The server explicitly accepts parameters up to 1,024 characters. The regression accepts a real 139-character document identity and refuses 1,025 characters. |
| Every delegated MCP invocation used the same occurrence identity | The native SDK boundary assigns a distinct random call identity while preserving the caller's stable operation ID and input object. |
| Gate evidence orchestration exceeded the repository's size limits | One named evidence adapter owns the scoped receipt/approval/source-acceptance snapshot. The public facade and ordering remain intact; dispatch and worker-report steps are bounded functions. |
| The recovered browser supervisor omitted Mail and only rechecked one build file | The tour now invokes the actual Mail MCP tool, checks its operation-ID schema, and verifies the complete recursive frontend/backend build snapshot before and after the workflow. |

The Fastify regression first failed with 9 assertions. The fixed native gate
passes **5 tests / 34 assertions**. The build snapshot regression passes **3 tests**
covering nested mutations, additions/deletions and symlink refusal. The changed
backend boundary/evidence files pass scoped lint with zero errors and warnings;
the repository JS-boundary check also passes.

Fresh production compilation passed: server **623 files / 0 warnings** and the
Wiki verification library. This diagnostic build uses the explicit local Clio
and Axxium source checkout `e0cdf3585b15101c83ef01b7bc2901652b3b54dd` in
`eta-identity`, which includes foundation `f22199ee` and the verified Axxium
89-test change. It does not establish that the older published dependency pins
have been promoted. Final gates must run again on the reviewed dependency pins.

The full backend suite remains a blocking gate. Its first run exposed stale
publication fixtures and a Mongo thread metadata readback defect; the focused
repair passed 65 tests / 396 assertions. A later full run reached stale message
source fixtures missing tenant scope and provider setup. Those failures are being
repaired without weakening admission contracts or the fatal async-error guard.

Run the complete browser workflow with `node scripts/verify-wiki-stack.mjs` after
building the frontend, backend server and `wiki-verification` target, and setting
the explicit warmed `FORESIGHT_MODEL_CACHE` and Chromium executable. The supervisor
creates disposable users, ledgers and content, owns all service processes, and
removes its fixtures when it exits. Its `result.json` and annotated screenshots
are authoritative for that run only. Historical screenshots do not prove this
reconstructed source works.

## Fresh browser run 07

The real browser completed password, username/email, passkey and PGP login,
signup, session revocation/recovery, Admin organization and identity binding,
and authenticated MCP initialization. It then stopped at a disabled Mail send
button: the capability projection used explicit permissions while the command
also authorized the system-administrator role. A new regression reproduced
three failures while the actual command successfully delivered the message.
Both paths now use the same permission predicate; explicit tool denial still
wins. The complete Mail command regression passes **12 tests / 54 assertions**,
and scoped lint remains zero errors and warnings.

The supplemental `agent-browser` CLI could not bind its daemon's Unix socket
(`Operation not permitted`), including a debug retry with the installed Chromium
path and `--no-sandbox`. The application tour uses Playwright's existing Chromium
pipe connection successfully inside this sandbox. No external browser environment
is required. Run 07 screenshots and its exact failed result were saved together;
they do not establish Mail, translation or publication completion.

## Fresh browser run 08 and admission regressions

Run 08 completed the identity and Admin ceremonies again, then the complete
Mail interaction: human composition and full-body retrieval, a real delegated
MCP delivery visible through SSE, preservation of an unfinished human response,
and acknowledgement. The Wiki created and saved a page, obtained and applied a
real Qwen writing suggestion, submitted it for review, recorded requested changes
and a lesson, edited and resubmitted. First source acceptance returned HTTP 502.
Translation and publication were therefore not reached.

The admission writer still queried Mongo directly despite selecting Clio. Its
new actual Clio/reopen regression failed all 11 assertions. The writer now calls
the existing `event-by-id!` provider boundary; retries repair projections without
duplicating the durable source fact. The test also retains that fact across an
embedding failure and repairs the index after reopening. Its small vector fixture
checks persistence semantics; real model quality remains a separate browser gate.

A real Fastify stream regression reproduced one implicit second send after
`reply.hijack()`. Returning native `undefined` from the owned SSE adapter keeps
Fastify from sending the private stream state. Existing authorization, scoped
invalidation and revocation checks still run.

CodeRabbit also found that source-review retry identity discarded the caller's
expected head. Reusing `accept-1` with a changed or absent predecessor reproduced
two failures. Only server-recorded time is now excluded from identity comparison;
an unchanged retry retains the first receipt even after the source changes.

The first combined run passed its assertions but the fatal async guard caught two
old Mongo adapter fixtures depending on another test namespace's registration.
Those tests now select their adapter explicitly. The guarded rerun passes
**41 tests / 233 assertions**, with **547 compiler inputs / 0 warnings**. Scoped
lint still flags two pre-existing oversized admission source/test files; the
size gate remains enabled while those responsibilities are extracted. The prior
full backend checkpoint passed **1,786 tests / 8,083 assertions**; a new full run
including these fixes and the policy cleanup is required and running.

That full run completed with **1,797 tests / 8,124 assertions**, no test failures,
no fatal async guard events, and process exit zero. Production release 03 compiled
the server from **649 inputs / 0 warnings** and the Wiki verification library from
**107 inputs / 0 warnings**. Run 09 repeated the identity, Admin and full Mail
ceremonies. Source event persistence now succeeded, but translation dispatch
refused an incomplete context: the Wiki review scope omitted the authenticated
membership. This is a separate HTTP 500 failure, not a completed publishing tour.

## Admission responsibilities and test isolation

The admission facade is now 393 lines. Immutable event persistence and duplicate
classification live in `infra.publication-event-writer`; draft-turn ownership and
retained settlement live in `infra.routes.document-draft-dispatch`; queue execution
and its recovery barrier live in `infra.routes.document-admission-queue`. Public
facade entrypoints remain intact. Independent review caught a reload hazard in
moving the original `defonce` tail; it remains in the facade and is explicitly
passed to the extracted queue, preserving already pending work across reloads.
The queue still observes every prior admission before releasing its barrier, and
a rejected operation cannot poison subsequent work.

Shared resource builders, source preflight tests, draft settlement tests and Mongo
adapter compatibility tests now have explicit namespaces. The exact same guarded
suite still passes **41 tests / 233 assertions**, and all affected source and test
namespaces pass strict lint with **zero errors and warnings**. No tests were
dropped and no size threshold was changed. These tests cover concurrent admission,
queued barriers, retained failed settlement, retry repair, and the actual Clio
and Fastify boundaries; they do not substitute for the next production tour.

## Verified membership at the acceptance handoff

The closed review identity intentionally contains organization, project and
document, while dispatch additionally requires the acting membership. Adding a
membership to the review ledger key would change its identity. Acceptance now
builds a separate admission context from the verified context, retaining that
membership and refusing its absence before the durable review command runs.
Neither the document body nor the agent tool can supply this authority.

The actual Wiki command calling the real dispatch-context contract reproduced
**21 failures / 24 assertions**. Initial test attempts exposed their own malformed
actor fixture and a single-arity replacement for a multi-arity runtime function;
those were corrected before claiming the failure reproduced the browser defect.
The final regression exercises all three existing verified membership shapes,
unchanged review identity, and nil/blank membership refusal before mutation.
Combined with the admission, Clio, Mongo, SSE and retry checks, the fixed guarded
suite passes **43 tests / 257 assertions**, compiler **554 inputs / 0 warnings**,
and strict scoped lint **0 errors / 0 warnings**. The next browser run remains
required. Partial Wiki screenshots now enter the supervisor's evidence inventory
immediately, so a later failure cannot omit already captured steps from its index.
