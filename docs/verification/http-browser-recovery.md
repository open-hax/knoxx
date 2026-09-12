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
