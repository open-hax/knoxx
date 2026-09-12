# Durable local actor mailbox

The mailbox uses the canonical Clio package through `IMailboxStore`. Full message
content, routes, delivery claims, acknowledgements and receipts survive a restart.
Actor mailbox code no longer queries MongoDB. Follow-up and steer delivery use the
existing thread provider and agent control service; event delivery uses the
external-event boundary and cannot manufacture internal event authority.

This source was reconstructed after scratch storage was lost on 2026-09-12.
It is not a byte-exact recovery of the earlier unpublished implementation. The
verification below was run against these reconstructed files.

## Installation and interfaces

Bootstrap installs `clio-mailbox-store/open!` with an explicit `:directory` using
`mailbox-store/install!`, then installs `mailbox-delivery/provider` using
`mailbox-delivery-registry/install!`. The optional provider `:clock!` must return a
valid timestamp. The application selects `KNOXX_MAILBOX_DIRECTORY`, normally a
`mailbox` child of its Wiki state directory. Missing providers return a classified
503; they cannot report a successful send or acknowledgement.

All routes require current Axxium identity and Knoxx membership. Tenant and actor
authority come from that context, never from request fields. Agent
`actors.send-message` and HTTP POST use the same command and explicit tool policy;
an explicit deny overrides administrator defaults.

| Interface | Operation |
| --- | --- |
| `POST /api/actors/messages` | `{operation_id,target,content,mode}`; optional target coordinates and metadata |
| `GET /api/actors/mailbox?box=inbox` | Current actor's inbox metadata; `outbox` selects its sent messages |
| `GET /api/actors/mailbox/:mailboxId` | Full immutable body for the sender, recipient or tenant administrator |
| `POST /api/actors/mailbox/:mailboxId/ack` | Recipient acknowledgement, even when the recipient is an administrator |
| `GET /api/admin/config/actors/mailbox` | Tenant operator inventory and named filters |
| `POST /api/admin/config/actors/mailbox/:mailboxId/ack` | Explicit operator acknowledgement |
| `POST /api/admin/config/actors/mailbox/retry` | Operator retry; `dispatch_events:false` leases eligible work without performing delivery |

Targets are `actor:<id>`, `session:<id>`, `conversation:<id>`, `self` or `parent`.
The last two require the caller's session lineage. Modes are `inbox-only`,
`follow-up`, `steer` and `event`; the default `message` means `follow-up`.
Follow-up/steer also require their control capability and a current thread in the
same tenant that the principal can control. A target's supplied run ID cannot
override the thread's actual run ID.

List entries expose a truncated preview, never the full canonical body. A full
read returns the `content` field. Top-level response entry keys are explicit
`id`, `orgId`, `status`, `source`, `target`, `delivery`, `contentRef`, `metadata`,
`preview`, `durable` and timestamps; qualified EDN keys cannot collide in JSON.

## Delivery guarantees and limits

The command writes full immutable content before claiming delivery. Claims have
bounded leases and attempt counts. Competing in-process attempts are serialized;
Clio's canonical append fences competing processes. A losing process may receive
a conflict and retry. Stable admission IDs reject changed message content.
Every command invocation uses a fresh claim ID: replaying a stored claim receipt
cannot authorize the same effect twice.

Current credentials and permissions are checked again immediately before the
effect, followed by the current claim token. Revocation records a failed attempt;
an acknowledgement or newer claim prevents an old callback from changing state.
Session cache signatures also include the credential digest, principal, tenant,
membership, actor, permissions and explicit tool policy, so identical tool names
cannot reuse closures belonging to an earlier login.

A successful effect followed by a failed ledger receipt returns
`mailbox_delivery_unconfirmed`. The immutable body remains available for repair.
There is still a crash window between the external effect and its receipt:
delivery is not a distributed exactly-once transaction. Operators must reconcile
an unconfirmed attempt before retrying a destination that lacks idempotence.
Expiry is a read projection; historical facts are retained.

## Fresh verification

Run from `backend` after restoring the declared toolchain and dependencies:

```sh
NODE_OPTIONS="--require $PWD/scripts/shadow-test-error-guard.cjs" \
CONTRACTS_DIR=test/fixtures/empty-contracts \
pnpm exec shadow-cljs --config-merge \
  '{:ns-regexp "knoxx\\.backend\\.(actor-mailbox-test|infra\\.(actor-mailbox-commands-test|mailbox-delivery-test|stores\\.clio-mailbox-store-test|agent\\.session-authority-test)|extern\\.actor-mailbox-test)$" :output-to "target/recovered-mailbox/test.cjs"}' \
  compile test
```

The guarded run passed **26 tests / 116 assertions**, zero failures or errors,
with **491 files / zero compiler warnings**. It includes actual Fastify injection
and actual SDK tool invocation over a fresh Clio ledger, restart reads, tenant
isolation, full-body privacy, concurrent claims, stale callbacks, expiry, receipt
failure, revocation after claim, acknowledgement before effect, and session-cache
credential/policy isolation. Each fixture removes its own temporary directory.

The first consumer run correctly failed on three stale test doubles that did not
preserve the real multi-arity CLJS service interfaces. Replacing them with matching
port arities produced the passing run; production behavior was not relaxed.
The async guard and explicit fixture catches matter: exit status alone cannot
prove that native async CLJS assertions completed.

Changed-source lint has zero errors. The sole warning is the pre-existing,
unchanged 51-line `create-session-manager!` in `infra/agent/session.cljs`; the new
mailbox and cache-authority functions have no warnings. A coordinated full backend
test and production compile remain separate required gates because other agents
are reconstructing sibling services and a browser process holds the current
server artifact.

## Human verification still required

The full-stack supervisor is `scripts/verify-wiki-stack.mjs`; it owns temporary
data, source/artifact provenance checks, actual Axxium login and browser cleanup.
This mailbox checkpoint supplies the real endpoints for that stack. It does not
yet add compose/full-message controls or live mailbox updates to the existing
administrator inventory. The HTTP/SDK proof above does **not** establish human UI
parity or a mailbox browser walkthrough. Those controls and their annotated
screenshots must be added to the existing surface before claiming this user-facing
feature complete.
