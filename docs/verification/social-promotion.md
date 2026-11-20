# Verify a Knoxx social release

Start with the active local contract mount. Track the files selected in
`contracts/.social-promotion-files` and the runtime code that implements their
tools. Use an isolated local Knoxx instance with a separate Mongo database and
workspace for prototype turns. Give it synthetic events and a recording
transport. The packaged contracts leave automatic social sources, triggers,
and schedules disabled. Keep real platform credentials and deployment-specific
channel, account, and workspace bindings in the operator's environment.
The shipped actors declare `:social/bindings-pending true` so manual turns and
direct MCP writes also refuse until those bindings have been installed.

Run the focused social tests against synthetic Discord and Bluesky requests.
The tests cover effect classification, actor scope, action admission, receipt
capture, ambiguous delivery, and thread ancestry without contacting either
platform. Run the full backend suite and build before submitting the candidate.

```sh
pnpm -C backend run typecheck
pnpm -C backend run test
pnpm -C backend run build
```

## Staging

Use the Services-managed environment promotion and record the exact admitted
Knoxx source SHA, controller revision, and OpenPlanner SDK revision. A testing
lease can qualify an open PR. The staging slot takes the merged Knoxx commit.
Keep event runtimes disabled and platform credentials absent in both slots.

From the reviewed source checkout, create the manifest for the actual deployed
commit. Transfer it and the verifier to a temporary directory on the staging
host using the usual audited operator connection. The manifest contains only
the source SHA, relative contract paths, and digests.

```sh
node scripts/verify-social-promotion.mjs --manifest "$KNOXX_SHA" > social-manifest.json
```

On the slot host, use the exact running backend container from the receiver's
deployment. Remove the temporary directory after the check.

```sh
node verify-social-promotion.mjs --verify staging "$STAGING_BACKEND" < social-manifest.json
```

The verifier binds the container image ID to the healthy slot receipt. It checks
every manifest file, submits a malformed and then valid contract to the live
validator, and confirms the deployed actor receives the expected social tool
grants. It checks that a different actor cannot select that contract. The slot
key stays inside the backend container. This check generates no agent turns or
platform events. The recording-transport tests provide action and receipt
evidence. A real inference check requires the stage's own provider route.

## Production

Use the same source revision and SDK pin. Services delivers production's
contract mount, so compare its package against the staged manifest before the
protected production workflow runs. Verify the serving human login, model
mapping, embedding route, accepted contracts, and disabled event runtimes after
deployment. The published resources use `gemma4:31b` as the portable default.
Preserve explicit model choices and keep the live embedding model tied to its
existing vector store.

Production installs the portable package with automatic social consumption
parked. Apply deployment-owned identities and routes before any activation.
Check all-actor activity, drain the existing consumer, and verify exactly one
active consumer for each bot identity at cutover. An execution worker is
required for affected actors' shell and browser tools. Native subscription
enrollment belongs to the host that owns the subscription.

Set `KNOXX_SOCIAL_KNOWLEDGE_MONGODB_URI` and
`KNOXX_SOCIAL_KNOWLEDGE_MONGODB_DB` to the actor knowledge deployment's separate
Mongo database. That database must have a distinct name from Knoxx and the
generic OpenPlanner store. Use an environment-owned credential scoped to that
database. The knowledge SDK uses the configured embedding provider and checks
the stored vector projection before acknowledging a write. Verify storage and
actor visibility with synthetic records before rendering live bindings.
