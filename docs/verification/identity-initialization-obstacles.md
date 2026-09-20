# Identity initialization and recovery evidence

The sandbox lost its working checkout during this task. Published Axxium/Clio
Git objects and exported source trees survived; the later Knoxx glue did not.
The restored Knoxx integration was reconstructed from its contracts and re-gated.
It is not claimed to be byte-identical to the lost implementation.

| Obstacle | Resolution and evidence |
| --- | --- |
| Knoxx's legacy session and GitHub OAuth flow minted authority from email | Register the Axxium plugin; require an active verified principal and explicit durable policy binding. Negative tests reject asserted email/member/API-key headers and the retired email provisioning entrypoint. |
| Missing helper during concurrent recovery caused two compiler warnings | Coordinate Mongo `resolve-bound-context!` and Fastify status restoration, then recompile the dedicated identity target. No unresolved-variable or arity warning is treated as passing. |
| Retained resource matching still fell back to email | Remove that authority path and prove even an identical email alone cannot match ownership. Stable actor IDs remain independent of email metadata. |
| Revocation could occur during awaited policy hydration | Revalidate session/principal afterward and again through a private refresh closure. Real Clio tests revoke during the await and require401. |
| Consent was a GET mutation | Move confirmation fields into a POST body; preserve displayed-actor witness checking; reject GET, missing/foreign Origin, malformed credentials and unauthenticated confirmation over real TCP. |
| Restored consent included a raw code in a closed secret-free storage record | Actual HTTP approval returned400. Remove the raw code from payload; only its hash indexes provider facts. The subsequent two-request exchange has exactly one200 winner and one400 refusal. |
| Native async tests could hide a rejection after completion | Run emitted tests with the process-error guard. The first failed consent test also triggered the guard on its subsequent invalid URL; that run remained failed. |
| Legacy MCP tests copied handler logic and could miss registration/SDK errors | Add real Fastify/TCP tests with Axxium, Clio OAuth storage and the actual MCP SDK. Controlled tools isolate grant intersection; real Wiki tool execution is a separate full-stack browser/agent proof. |
| MCP route file exceeded the configured size/style limits | Move native OAuth composition and SDK/schema helpers into named extern adapters, preserving the public API, and repeat the actual TCP suite after extraction. No linter was disabled. |

At the recovered identity/MCP checkpoint, the focused suite passed **54 tests /
201 assertions**, with zero failures/errors. Its isolated compile processed417
files with zero warnings. The scoped configured linter reported zero errors and
warnings. The integrated production server was independently compiled by the
assembly owner. These focused results do not substitute for the final combined
backend suite or full-stack browser evidence.

Reproduce from `backend/` with the repository's installed toolchain:

```sh
pnpm exec shadow-cljs compile identity-recovery-test
node --require ./scripts/shadow-test-error-guard.cjs target/identity-recovery/test.cjs
```

The tests allocate and remove their own temporary directories and bind real HTTP
servers to ephemeral localhost ports. They exercise signup, stable binding and
restart, explicit bootstrap collision/refusal, active-session/actor revocation,
Origin/mixed-credential checks, consent, atomic one-use exchange, direct Axxium
bearer access, delegated tool restriction and delegated revocation. Broader
Mongo and live external OAuth paths require their corresponding runtime/provider
configuration; those were not inferred from the local fixtures.
