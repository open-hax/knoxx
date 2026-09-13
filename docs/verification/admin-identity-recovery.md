# Admin identity and EDN policy recovery

The account screens, admin actor screens, and identity/admin browser helpers were reconstructed after the sandbox checkout was pruned. `auth-source-recovery.json` records their hashes and distinguishes reconstruction from exact recovery. Older screenshots and test counts do not establish the behavior of these bytes.

The admin facade now carries the authenticated Axxium request context into the selected Clio policy provider. Each public directory read and command refreshes the original session; the policy transition rechecks current directory permissions. Server context alone has no bootstrap authority. Mongo remains an explicitly selected provider, with its legacy paths retained.

A supplied `axxiumPrincipalId` is resolved on the server as an active Axxium principal. Caller-supplied principal maps are discarded. Adding another organization membership preserves the stable user ID and canonical login binding; this does not implement organization switching. A blank principal field creates a directory row that still requires identity enrollment. Bound actor IDs are immutable, and profile edits cannot update identity email, username, provider, or external subject. Directory credential responses redact secrets; service credential retrieval checks the exact active membership and current Axxium owner.

## Reproduce

From the repository root, use the installed toolchain and dependencies:

```sh
pnpm -C frontend exec vitest run src/lib/api/admin.test.ts src/components/admin-page/UsersMembershipsSection.test.tsx
pnpm -C frontend exec tsc --noEmit
pnpm -C backend exec shadow-cljs --config-merge '{:ns-regexp "knoxx\\.backend\\.(local-policy-api-recovery|admin-credential-routes|auth-session|extern.run-event-queue)-test$" :output-to "target/admin-policy-proof/test.cjs"}' compile test
node --require ./backend/scripts/shadow-test-error-guard.cjs ./backend/target/admin-policy-proof/test.cjs
pnpm -C backend typecheck
```

The selected test compilation and direct guarded execution both passed 21 tests containing 98 assertions, with zero failures and errors. The test compiler reported zero warnings; integrated `pnpm -C backend typecheck` passed with 591 files and zero warnings. The direct guarded invocation must also succeed: Shadow can finish compilation successfully after its child test process reports an uncaught rejection. Never treat the compiler exit code alone as a test result. The focused selection covers the changed facade and adjacent identity/credential boundaries; the root stack verification owns the complete backend suite.

For a live human walkthrough, run `node scripts/verify-wiki-stack.mjs`. It owns disposable services and fixtures, confirms the served checkout, executes `identity-browser-tour.mjs` and `admin-identity-browser-tour.mjs`, and writes annotated screenshots to its evidence directory. This recovery checkpoint does not yet claim a successful fresh browser run. External OAuth consent requires configured provider credentials; an unconfigured provider remains visibly unavailable.

## Obstacles observed

- The available Git objects and recovery copies lacked the historical auth/admin commits. Source was reconstructed against the current Axxium contracts, and fresh tests replaced historical evidence claims.
- The route census requires a three-form `Route` definition and separate source lines for `:path` and `:element`. Metadata carries the docstring, while explicit keyed route elements preserve the checked grammar.
- Admin routes authenticated successfully, then reduced the provider context to the legacy Mongo pool. Preserving the complete selected context and attaching the verified caller restored finite EDN reads and commands.
- The first focused Shadow invocation nested `:ns-regexp` under `:builds`, which this CLI option does not interpret as build configuration. It unintentionally ran broader stale fixtures and exposed uninitialized thread persistence. Build-level `:ns-regexp` selected the intended tests. The failed run is retained and is not counted as a pass.
- A server compile observed an MCP namespace split while its owner was still writing the new files. The owner froze that graph before the integrated build was retried; no dependency or warning checks were weakened.
