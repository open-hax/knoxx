# Remaining local service providers

This audit identifies remaining Knoxx service work after the recovered Clio mailbox, source, review, run, thread, cache, policy, MCP, and local OpenPlanner changes. It does not establish that every Foresight child module is initialized or that every website surface has passed a browser walkthrough.

## Recovery provenance and verification limits

The original local documentation commit was `7ef83a9f`. Its file bytes were lost when the workspace disappeared on 2026-09-12 before the publisher captured them. This document is a reconstruction from retained findings, checked against published source at [`377892dcf4908c31b74ccc9349db04356a5d9181`](https://github.com/open-hax/knoxx/commit/377892dcf4908c31b74ccc9349db04356a5d9181); it is not a byte-exact recovery of that documentation commit.

The last guarded, focused mailbox and agent-admission gate completed **45 tests and 204 assertions, with zero failures, errors, or compiler warnings**. Compilation covered 527 files. Its retained log was `/tmp/knoxx-mailbox-agent-admission-proof.log`. That proof covered the local frozen source, including the admission checkpoint `3446b7ca` and its mixed root-owned turn/bootstrap dependencies; it must not be reassigned to a different remote tree without checking those bytes. The admission payload was captured before the outage, but it was not yet part of the published reference used for this source audit.

The subsequent full backend autodiscovery command **never started**. First the previous workspace path disappeared; then even an explicit shell in the filesystem root failed, ultimately returning `409 environment_offline: Environment is not connected`. Recent archives and publisher JSON payloads were verified present in `/tmp` before that disconnection. No new test, build, lint, browser, or model result is claimed by this remote-only documentation recovery. A full recovered backend pass and complete browser proof remain pending.

## Existing local foundation

The canonical event ledger is `eta-mu/packages/clio`. The deprecated `event-ledger` package is not a replacement for it. The recovered work adds named local providers for implemented source, review, publication evidence, translation split, run, thread, cache, policy, MCP, and mailbox operations. The local OpenPlanner adapter records accepted facts and validates actual configured embedding output. Missing or unsupported functionality remains an explicit service error.

The [local OpenPlanner recovery notes](https://github.com/open-hax/knoxx/blob/377892dcf4908c31b74ccc9349db04356a5d9181/docs/verification/local-openplanner-recovery.md) distinguish provider fixtures from actual model execution and retain the unsupported-operation list. The provider's [implementation](https://github.com/open-hax/knoxx/blob/377892dcf4908c31b74ccc9349db04356a5d9181/backend/src/cljs/knoxx/backend/infra/clients/openplanner_clio.cljs) permits startup without embedding settings, reports that configuration in health, and returns `503 openplanner_embedding_not_configured` when an operation needs vectors. Accepted events can already be durable at that point; reopening with a real configured model can repair the projection. Partial model configuration is rejected. This supports ledger and identity startup without pretending vector search is available.

## Mailbox: implemented commands, remaining route collision and browser proof

The shared [mailbox command service](https://github.com/open-hax/knoxx/blob/377892dcf4908c31b74ccc9349db04356a5d9181/backend/src/cljs/knoxx/backend/infra/actor_mailbox_commands.cljs) uses current identity context, explicit tool policy, mode-specific permission checks, durable logical message intent, claim fencing, and honest delivery receipts. HTTP and the existing actor agent tool delegate to that service. Focused proofs cover tenant isolation, restart, changed actor routing during an exact retry, acknowledgement, revocation, and failure to confirm a receipt after an external effect. An external effect and a later receipt are not one atomic transaction; an unconfirmed effect requires reconciliation before retry.

The [HTTP routes](https://github.com/open-hax/knoxx/blob/377892dcf4908c31b74ccc9349db04356a5d9181/backend/src/cljs/knoxx/backend/infra/routes/actors.cljs) include both `GET /api/actors/mailbox/:mailboxId` and the static SSE endpoint `GET /api/actors/mailbox/changes`. The [send contract](https://github.com/open-hax/knoxx/blob/377892dcf4908c31b74ccc9349db04356a5d9181/backend/src/cljs/knoxx/backend/law/mailbox_store.cljs) accepts any nonblank `:operation-id`, which becomes the message ID. Therefore the caller-supplied ID `changes` conflicts with the static endpoint's URL. This is a source-level finding; no new HTTP reproduction ran while the executor was offline.

A bounded fix should give full-message reads an unambiguous route, such as `GET /api/actors/messages/:mailboxId`, and update the human client while retaining compatible reads for existing ordinary IDs. Merely rejecting the reserved ID on new sends does not repair a previously admitted message with that ID. Preserve the existing ledger facts and exact retry identity. Add an actual Fastify regression that reads a message whose ID is `changes` through the new route and independently opens the SSE endpoint; it must verify authentication and tenant checks on both.

The new Mail UI still needs its complete supervised browser proof: compose, open the canonical full body, acknowledge, and invoke the authenticated agent tool while the human mailbox view and an unsaved draft are open. Capture live updates and fresh annotated screenshots. A successful raw HTTP request is not evidence that the human controls or agent tool were exercised.

## Invitations: keep consumption and membership in the same policy authority

The published [policy facade](https://github.com/open-hax/knoxx/blob/377892dcf4908c31b74ccc9349db04356a5d9181/backend/src/cljs/knoxx/backend/infra/db/policy.cljs) explicitly returns local-unavailable from `create-invite-for-context!` when the local provider is selected. Legacy invitation creation, listing, and redemption still call `mongo-policy-invites`. The inspected redemption code compares the supplied email, consumes the invitation, and then creates user/membership authority in a separate operation. This audit has not demonstrated the live reachability of every legacy function, so the finding is a service and authority-design gap rather than a claimed public-route exploit.

Implement finite invitation operations inside the **existing Clio policy aggregate and ledger**. Invitation consumption and membership admission must be one accepted policy transition, rather than writes to independent invitation and authority ledgers. Persist a digest of an opaque invitation token, expiry, tenant, issuer, and the exact grantable role set. Bind redemption to the current verified Axxium principal and, for an email-restricted invitation, a verified identity email. A caller-supplied email string alone must not confer authority. Check current issuer and redeemer policy at the transition boundary and define how issuer revocation affects outstanding invitations.

Verification needs simultaneous redemption, exact retry, a different principal attempting a used token, foreign tenant, expiry, excessive role grants, revocation, restart, and refused durable writes. Both human and agent entry points must call the same guarded operations. This is required provider work; it is not implemented by the present mailbox or source ledgers.

## Graph and OpenPlanner: declared protocols still need finite local implementations

The published [graph client file](https://github.com/open-hax/knoxx/blob/377892dcf4908c31b74ccc9349db04356a5d9181/backend/src/cljs/knoxx/backend/infra/clients/graph.cljs) declares `IGraphClient` with `graphql!` and `status!`. The inspected [app routes](https://github.com/open-hax/knoxx/blob/377892dcf4908c31b74ccc9349db04356a5d9181/backend/src/cljs/knoxx/backend/infra/routes/app.cljs) still send GraphQL and status requests to `http://127.0.0.1:8796` and return that address for the graph view. The local [OpenPlanner driver](https://github.com/open-hax/knoxx/blob/377892dcf4908c31b74ccc9349db04356a5d9181/backend/src/cljs/knoxx/backend/infra/clients/openplanner_clio.cljs) explicitly refuses graph memory, graph export, graph monitoring, semantic-edge construction, document statistics, record labels/reactions, and generic Mongo/HTTP forwarding with `503 openplanner_local_operation_unsupported`.

The earlier local audit also identified graph helper calls without an explicit organization option and an export path forwarding request query data. That scope finding is retained for follow-up, not presented as a newly reproduced exploit. Reinspect the restored caller and its current route guards before enabling it; all provider queries must derive tenant scope from verified context rather than trusting a query parameter.

A bounded local graph provider can project named queries from accepted Clio events and real vectors. It needs provenance, revision and deletion rules, tenant filtering, deterministic replay, and query cost limits. Implement the operations the application actually needs behind its named protocol first. Arbitrary GraphQL and Mongo dialects remain unsupported until their semantics exist; returning an empty graph would conceal the missing implementation. Include a browser view backed by real projected data when that service is wired.

## Ingestion: route through a named local provider and canonical path guards

The published [ingestion protocol](https://github.com/open-hax/knoxx/blob/377892dcf4908c31b74ccc9349db04356a5d9181/backend/src/cljs/knoxx/backend/infra/clients/ingestion.cljs) declares browse, file, source, job, job creation, and proxy operations. The inspected [app routes](https://github.com/open-hax/knoxx/blob/377892dcf4908c31b74ccc9349db04356a5d9181/backend/src/cljs/knoxx/backend/infra/routes/app.cljs) still proxy the main ingestion reads and jobs to the configured external HTTP service. A local file-write helper also exists in that file, so the gap is not accurately described as every ingestion operation being external. A complete local provider and guarded normal-route assembly have not been demonstrated.

Reuse the canonical realpath and provenance checks used by source admission. Lexical path normalization or a string-prefix check alone does not establish containment through symlinks. Admit immutable ingestion job requests before work starts, expose truthful queue/progress states, and use named local operations rather than forwarding arbitrary query dialects.

Verify a real contained file through the browser and inspect its admitted source record. Tests must reject traversal, symlink escape, foreign tenant scope, and failed durable job admission before effects. Preserve an explicit unavailable response for any operation whose local semantics remain unimplemented.

## Next execution order

1. Restore the captured source and its exact dependency pins. Run the full backend suite with the async error guard; prior focused counters do not replace this gate.
2. Fix and test the reserved mailbox URL collision, then complete the Mail browser walkthrough with the authenticated agent tool and live human view.
3. Implement invitations in the existing policy ledger, followed by finite local graph and ingestion providers, their normal bootstrap selection, and their user-facing verification paths.
4. Rebuild, run the complete relevant tests and lint, and repeat affected browser tours against one recorded source and build snapshot. Keep unsupported statuses visible until the corresponding implementation is complete.

All of these steps fit the existing ClojureScript/JVM/Node stack in the same sandbox. They are remaining implementation and verification work; this audit does not claim the entire stack is finished.
