# Mongo policy provider foundation

This is the independently composed provider layer after the additive identity
foundation. It extracts the Mongo policy facade into named modules and fixes
membership/profile updates, actor-coordinate projection and invitation claims.
It preserves the preceding production authentication composition: bootstrap,
configuration, auth session, authz, frontend and old auth route family stay in
place. The later identity activation is qualified separately.

## Run it

With frozen dependencies, the built sibling OpenPlanner SDK, Clojure, Volta's
Node 24.14.1, and OpenSSL available:

```sh
scripts/verify-policy-provider-foundation.sh
KNOXX_TEST_MONGOD=/absolute/path/to/mongod scripts/verify-policy-provider-foundation.sh --native
volta run --node 24.14.1 pnpm -C backend test
volta run --node 24.14.1 pnpm -C backend typecheck
```

The script identifies the checkout and commit, compiles fresh sources, refuses a
missing native proof, guards asynchronous errors, and checks the emitted test
summary. It seeds and removes its own ledgers, actor contracts, TLS authority,
Fastify apps and Mongo processes. Native login/bootstrap uses an owned one-node
replica set to exercise actual topology checks and credential transactions. It
never connects to a running Knoxx, downloads Mongo or restarts a user's service.

Without `--native`, the script prints a warning that actual Mongo and preceding
HTTP compatibility have not been qualified. Native mode requires an explicit
qualified executable and cleans only its own temporary root/processes.

## What the checks demonstrate

- Actual old admin PATCH preserves omitted roles and applies explicit empty
  removal. It refuses actor reassignment and unsupported profile patches before
  dependent writes, preserving their 409/400 status and classified error code.
- Real Mongo role changes synchronize effective actor-contract roles, including
  an empty set. Concurrent initial actor assignment has one winner; a stale
  conflicting contract cannot overwrite current user, membership or role state.
- Real invitation CAS reserves before provisioning. A known provisioner failure
  releases only its reservation, making retry possible. Concurrent or abandoned
  claims cannot provision twice. The old HTTP redemption route retains 403/409/
  503 classification. Uncertain completion requires explicit verified recovery;
  cross-store rollback and automatic claim stealing are not promised.
- The actual preceding remote login and repeated context preserve stored profile,
  membership, actor and roles. Wrong issuer/email collisions refuse; restart
  preserves session behavior. The existing inactive-user classification is409,
  while inactive membership/organization is403; the provider split does not
  silently change that contract.
- Actual preceding local bootstrap/password login/session/logout run against
  native Mongo. Wrong passwords refuse, the session secret survives reopening,
  and logout invalidates the existing cookie.
- The additive Clio provider and binding checks remain available, including
  durable command retry/reopen, current authority and scoped profile updates.

The real-route regressions were counterchecked against the omitted-role and
native-status-only caller bodies: they fail when those old bodies are restored.
The new Mongo refusal errors also exposed a shared HTTP helper that ignored
Clojure exception data; the narrow status/code helpers move here with their
first real caller.

## Deliberate boundaries

This is not a claim of verified-principal authentication activation. The
preceding runtime still owns its existing header, cookie and invitation
admission semantics. The new identity/session boundary, authenticated invitation
UI, delegated audience changes and secret-free OAuth provider are later work.
Native TLS/Mongo scaffolding is reused without importing that later composition.

Assigned actor coordinates are immutable; same or initial assignment is allowed,
and reassignment requires a separate explicit migration. Mongo rejects unsupported
profile/status updates with400 instead of reporting an ignored update as success.
See [actor-coordinate details](actor-coordinate-immutability.md).

No browser controls change in this layer. These owned HTTP/native fixtures do
not qualify an external OAuth provider, production SMTP, or a deployed account
migration. A full green suite does not erase inherited lint warnings: record
those separately against the exact candidate and owning untouched functions.
