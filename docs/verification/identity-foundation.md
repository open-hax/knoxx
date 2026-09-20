# Identity and policy foundation

[Task 373](https://github.com/open-hax/knoxx/issues/373) introduces durable identity
bindings and a finite Clio policy provider. Production bootstrap, authentication,
sessions, HTTP routes, the existing policy facade and frontend remain unchanged
from the prerequisite run/event provider layer. No new provider is selected by
application startup in this change.

The 14 foundation files come from integrated identity candidate
`416bacb019e432c642f45494dc0c6d013a1af05d`. The separate runtime adoption remains
in PR #335. The provider has explicit trusted observation and named command/read
ports; a principal map passed to those trusted ports is not itself an HTTP
credential. Public local-policy API calls require a refreshed principal and
recheck current directory authority.

Run the standalone proof from this checkout:

```bash
./scripts/verify-identity-foundation.sh
```

It requires the repository's pinned Clojure dependencies, installed backend
packages, Clojure CLI, Node 24 or newer, and ripgrep on `PATH`. It refuses an
older Node before compiling or creating fixture data; no version manager is
required. Qualification used Node 24.14.1. The
script prints its checkout and Git head, warns about local modifications,
compiles a fresh proof artifact and executes it through the asynchronous-error
guard. It never talks to a running Knoxx, an external identity service or Mongo.
No credentials from the environment are used.

Each test seeds real Clio files in a private temporary directory and removes
it in `finally`. The script also owns the temporary parent and removes it on
exit or interruption. The local API proof opens the pinned Axxium EDN provider,
creates a test administrator and actual password-backed session, then revokes
that session. It does not use the later Knoxx identity-bootstrap, auth/session,
authz or HTTP composition to manufacture a passing context.

The proof covers:

- initialization, explicit principal/binding observation and authorized reads;
- accepted commands, first-result retries with a changed timestamp, conflicting
  retries, and complete directory state after reopening the ledger;
- malformed admission and rejected reads leaving canonical history unchanged;
- role revocation surviving initialization, observation and provider reopening, including refusal of a formerly accepted command retry;
- session logout, missing authentication and delegated credentials refusing the
  next local API call; current directory revocation also overrides cached grants;
- canonical binding replay beside additive memberships, same-owner selection,
  concurrent conflicting writers, exact retries, foreign ownership, ambiguous
  organizations and invalid selectors;
- scoped updates preserving the other tenant's membership and shared profile.

The human-readable PASS lines print only after the compiled test suite has
nonzero test/assertion counts, zero failures/errors and zero compiler warnings.
The proof emits a persistent WARN for the runtime workflows it does not cover.
There is no browser tour because this layer adds no user-facing UI or HTTP route.

Foundation implementation files are additive. The proof and tests do not prove
that old remote/login behavior is healthy, nor that switching the application to
these providers is safe. Later provider extraction and runtime activation must
qualify their exact heads separately, including Mongo, real HTTP/session
compatibility and browser invitations. The full guarded backend suite and
server typecheck remain required alongside this focused proof.
