# Production ESM route registration

Run from the repository root after the frozen backend dependencies and the real
linked OpenPlanner SDK have been installed and built:

```sh
pnpm -C backend run test:routes:release
```

This command freshly releases `:route-registration-proof`, then imports its ESM
export in Node with the asynchronous error guard enabled. The target uses the
production server's `:simple` optimization and imports the actual
`knoxx.backend.infra.routes.app/register-routes!` graph. A development compile
alone is not this check. The required `knoxx (backend + frontend)` CI job runs
the production server release and this route proof before its translation
browser contract. The verification export is not reachable from the
production server target.

The fixture creates real Fastify instances with the normal app plugins, an
owned temporary workspace, and a loopback-only OpenPlanner fixture. It invokes
the complete app registration function and checks 17 route-group sentinels,
including memory, documents, and the final translation group. It then verifies:

- A graph-export request without its permission returns 403 before an upstream
  call, through the production authorization helper.
- A permitted graph-export request returns the loopback fixture's graph through
  the injected `openplanner-graph-export!` local.
- A permitted memory-session request returns the fixture's scoped row through
  the injected `fetch-openplanner-session-rows!` local.
- Both Fastify instances close and the temporary workspace is removed before
  the verifier exits. The test error guard prevents unhandled promise failures
  from being hidden by the exit.

Auth contexts are seeded at the existing request-context cache seam with a
non-admin organization/member/user fixture. This verifies permission handling
and route execution; it does not verify password login, the policy database,
all endpoint behavior, an external OpenPlanner service, or a deployed image.
Production modules have process-level timers, so the verifier owns a separate
Node process and explicitly exits after cleanup. It never starts PM2 or the
application bootstrap.

## Observed result

On the PR327 reconciliation plus these changes, using shadow-cljs 3.4.11,
ClojureScript 1.12.145, Node 24.14.1, and the real OpenPlanner SDK at
`07085d6557b75834ce6f50e6c54b8ca47e1c7c08`:

- Fresh proof release: 423 files, 349 compiled, zero compiler warnings.
- Runtime: 313 Fastify registrations, all 17 sentinels present, permission
  refusal 403, graph 200, memory 200, exactly two loopback upstream calls.
- Fresh production server release: 480 files, 401 compiled, zero compiler
  warnings. Its generated module contains none of the fixture export or auth
  seam markers.

This provides runtime counterevidence to
[the inherited `!`-local registration finding](https://github.com/open-hax/knoxx/pull/305#discussion_r3995700878)
under the current compiler. The macro and its dependency-local names are
unchanged. A future compiler or route change must rerun the release command;
the historical translation-route workaround is not evidence of a present
failure in this graph.

The route helper docstring now describes the Fastify websocket plugin without
starting a line with an npm scope. Closure interpreted the old `@fastify` text
as an unknown JSDoc tag: its warning appeared even while Shadow's summary
reported zero warnings. The fresh server release log was checked for that
warning as well as its summary.
