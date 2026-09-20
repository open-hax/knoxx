# Source and review authority

Run the isolated filesystem proof after installing the frozen backend dependencies:

```sh
bash scripts/verify-source-authority.sh
```

The command compiles only the source-review laws, source recovery workflow and
Clio application ledger tests into a distinct Shadow build. It runs the generated
artifact with the asynchronous failure guard and requires nonempty passing test
counters. The tests allocate and remove their own temporary ledgers and resource
directories; they do not start a server, connect to live services or use a policy
database. The printed checkout identifies the source being compiled.

The source facts carry tenant, document, source locale, content digest, immutable
operation identity and predecessor revision. The focused checks exercise exact
retry results, wrong-scope refusals, review acceptance and invalidation, corrupted
replay, concurrent writers, awaited admission guards, and repair after a failed
filesystem projection. Successful no-op writes with explicit operation IDs have
durable receipts without false state-change notifications. Implicit random-ID
no-ops retain their existing behavior of appending no fact.

Document sequencing retains each operation's result or rejection. Cleanup of a
completed predecessor cannot remove a pending successor, and a rejected write
cannot poison later repair.

This layer supplies independently testable ports and providers. Wiki HTTP and
agent adapters, identity, writing assistance, translation provider composition,
and the aggregate provider recovery suite remain assigned to later PRs. This
proof does not claim password login, browser behavior or a deployed application.
The former composite provider proof is deferred with its source, not treated as
a passing check in this layer.

Run the full guarded suite and server compilation as separate qualification:

```sh
pnpm -C backend exec node scripts/run-shadow-tests-ci.mjs
pnpm -C backend run typecheck
```
