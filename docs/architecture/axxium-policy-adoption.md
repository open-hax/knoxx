# Axxium identity and Knoxx policy

Axxium owns credentials, login ceremonies, explicit account linking, active
principals and sessions. Knoxx registers Axxium's public CLJS plugin and binds
its verified immutable principal ID to an independent policy membership.
Email and username are display/lookup metadata inside the identity owner;
neither supplied headers nor matching email addresses establish Knoxx authority.

`identity-bootstrap/create-context!` accepts the application configuration and
the defined CLJS policy options map. `assert-config!` runs before application
storage opens. Policy selection is explicitly `:edn` or `:mongo`; unknown values
refuse. Axxium identity remains the local Clio provider in this composition.
`KNOXX_AXXIUM_DIRECTORY` (or `AXXIUM_EDN_DIRECTORY`) selects its durable directory;
otherwise it is `<wiki-directory>/identity`. Bindings and local policy use sibling
`-bindings` and `-policy` directories. Preserve all three directories and Axxium's
private encrypted vault/key when restarting or restoring.

Local signup creates a private workspace from the verified principal ID. A
server-owned role catalog supplies its policy. Bootstrap requires explicit
configured credentials, uses a stable configured Axxium principal ID, and must
pass Axxium's collision/restart checks. Application APIs never mint an identity
from an asserted email or promote a signup because its email resembles an admin.

The browser session cookie is `axxium_session`. `identity/resolve-request!`
decodes credentials through Axxium, rejects malformed or mixed credentials,
checks Origin for cookie-authenticated mutations and resolves current policy.
It rechecks the original session/principal after awaited policy hydration. A
private refresh closure, rather than a serializable token field, supports
`identity/current-context!` for long-lived Wiki views and SSE updates. Mongo
policy hydration additionally rereads binding, joined rows and role composition;
this is bounded revalidation, not a cross-store transactional snapshot.

MCP accepts a direct active Axxium session as `Authorization: Bearer <token>` or
an explicitly consented delegated bearer. A Node agent using a browser user's
session must omit the browser cookie from the same request. Delegated records
carry stable Axxium principal/entity and Knoxx membership IDs; email-only legacy
tokens require new authorization. Current actor/directory policy and the exact
tool grant intersect on each MCP request. Revoking an Axxium login method ends
Axxium sessions; independently issued MCP grants require their own revocation.

Consent is a protected POST. Its displayed actor is a witness checked against
current policy, never a requested identity. Code verification precedes an atomic
Clio consume-and-issue transition. Only credential hashes and closed grant
metadata enter the ledger. Token inventory exposes digest IDs for membership-
scoped revocation. The former email-based loopback grant is explicitly disabled;
development and deployment probes use verified Axxium authority.

Native OAuth response composition and MCP SDK/schema/lifetime handling live in
`extern.mcp-oauth` and `extern.mcp-sdk`; the public route registration and
`typebox->zod-*` compatibility functions remain in `infra.routes.mcp`.

Provider login configuration and browser credential-management behavior belong
to Axxium's package documentation. Local issuer and crypto tests do not establish
that an unconfigured live third-party OAuth consent flow has succeeded.
