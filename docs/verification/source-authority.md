# Source and review authority

Run the isolated filesystem proof after installing the frozen backend dependencies:

```sh
bash scripts/verify-source-authority.sh
```

The command compiles source/review contracts, filesystem recovery workflows and
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

Source and review provider retry IDs are scoped by organization, project and
document. The same ID can name separate operations in those scopes. After
reopening and intervening work, a server-time-only retry returns the original
fact with `existing? true`; conflicting content is refused without appending.
These providers keep domain IDs distinct from ledger-global explicit Clio IDs.
Within a source scope, observations and caller create/save commands use separate
operation keys. Existing observation facts keep their stored string IDs, but a
caller may use the same string for a save without colliding with the observation.
The regression saves with `observe/<current revision>` and reopens/retries it.

Clio observers can select stream names and provider-declared scope. Scoped
listeners receive only the matching stream name; operation arguments, results
and scope facts stay inside the provider. Providers without a scope extractor
cannot notify scoped subscriptions. Legacy listeners still receive no arguments.
The proof covers unrelated streams, other organizations/projects/documents,
unsubscribe, and thrown or rejected observer failures after a successful durable
write. The separate Wiki integration owns its HTTP stream and authorization
checks; this layer supplies the tested observer contract it needs.

Acceptance facts load resource records and build their canonical index once for
the documents selected by the caller. The proof uses two selected documents and
an unrelated missing source: the selected documents share one resource load,
and the returned acceptance predicate performs no I/O. A selected missing source,
resource or review provider failure, or pending source projection still refuses
the facts. This does not make source file reads atomic across documents.

The regressions also load equal document declarations from two real checkout
roots in both orders, read the last declaration's bytes, and prove that saving
changes only that checkout. Wire locale strings must satisfy the existing
publication language-tag grammar before conversion to keywords; catalog admission
remains a separate garden obligation. A creation fact requires its manifest at
construction, admission and replay: a namespace, the exact source document and
one or more schema-valid publications referring to that document. Interrupted creation retains the document and its
publication intents through ledger reopening, while missing or malformed manifests
leave the durable ledger empty.

An already accepted create is checked against its original actor, content, title,
source locale and publication destinations before consulting any mutable garden.
The accepted manifest supplies its recovery metadata even if that garden is now
archived, no longer supports a locale, or was removed. New creates still require
current garden admission. After repair, the retry reads actual source bytes and
validates its accepted source history and review history; it does not require a
now-missing garden to reconstruct its original receipt. Ordinary publication
topology validation remains unchanged. The proof interrupts initial projection,
changes each garden condition, reopens the ledger, repairs both files and checks
that no additional source fact was appended. Changed commands and actors refuse.

Document sequencing retains each operation's result or rejection. Cleanup of a
completed predecessor cannot remove a pending successor, and a rejected write
cannot poison later repair.

An exact save retry can also repair a deleted source projection after reopening
the ledger. It restores the latest accepted bytes even when the retried command
is older, and returns that command's original receipt without appending a fact.
Recovery first validates the current declared resource, tenant ownership and
canonical provenance. Changed resource metadata, conflicting existing bytes and
symlink escapes remain refusals; a new save still requires observed source bytes.
The filesystem regression covers these cases without a server or live services.

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
