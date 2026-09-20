# Publication compatibility and run query recovery

The compatibility publication routes now share the Wiki publish command guard: publish capability, explicit `wiki_publish` denial, exact document ownership, and the reviewed source revision under the shared document lock. Public read visibility does not permit changing another organization's publication. A missing placement returns an opaque 404 before constructing a document scope.

Run queries return the selected provider's actual run. Missing or expired durable records never revive a stale memory record. Ordinary own-run access also requires the selected organization to match; only explicit platform administration or `agent.runs.read_all` can cross that boundary. A missing context returns 401 before accessing a provider. Event authorization runs before flushing queued events.

The focused test suite drives both actual Fastify routes through injection. Its source snapshot and final publication effects are controlled dependencies; it does not claim to reproduce the entire rendering or publication process. A real asynchronous source lock verifies that an intervening edit makes the old publication revision fail. Separate run-query tests use actual Clio files, reopen the provider, advance its clock to expiry, and corrupt the ledger to prove failures remain visible. The end-to-end browser supervisor covers real rendering and publication separately.

```sh
pnpm -C backend exec shadow-cljs --config-merge '{:ns-regexp "knoxx\\.backend\\.(publication-guard-recovery|run-queries-recovery|extern.fastify-cms-publication|infra.cms-publication-facade)-test$" :output-to "target/publication-run-proof/test.cjs"}' compile test
cd backend
node --require ./scripts/shadow-test-error-guard.cjs ./target/publication-run-proof/test.cjs
pnpm typecheck
```

The focused compilation and direct guarded execution pass 34 tests containing 173 assertions, with zero failures and errors. The test compiler reports zero warnings, and integrated `pnpm typecheck` passes with 602 files and zero warnings. Existing adapter redaction tests intentionally log their classified test errors. Those messages are expected evidence that a 500 response omits internal paths; they are not compiler warnings.

The first recovery run exposed an incorrect test double: `source-dependencies` has zero- and one-argument entry points, and replacing it with a generic constant function broke the compiler's static arity dispatch. Preserving both actual arities fixed the fixture. The seven failed assertions from that attempt remain recorded; they are not counted as a passing run.

Legacy Mongo and OpenPlanner run adapters currently lack the separate ordered-event protocol. Their existing event-memory compatibility behavior is outside this focused recovery. The EDN Clio provider implements both run and event ports, and its restart/expiry behavior is covered here.
