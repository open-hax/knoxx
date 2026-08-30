---
uuid: knoxx-mcp-token-record-cljs-map
title: Make the MCP token record a CLJS map across producers and consumers
status: incoming
priority: P2
points: 3
labels: tasks, 3sp, mcp, auth, boundaries, security
created_at: 2026-08-30T01:19:13.727Z
category: tasks
---

# Make the MCP token record a CLJS map across producers and consumers

> GitHub issue: [#258](https://github.com/open-hax/knoxx/issues/258)

## Problem

`infra.auth.method-config/grant->token-record` creates a JavaScript object from an ordinary
`infra.*` namespace. The OAuth path in `infra.routes.mcp/load-token-record!` is a second producer,
and downstream MCP authentication, actor scoping, and tool granting consume both branches with
`aget`, `array-seq`, and `^js` hints. Converting one producer alone would leave the two branches
with incompatible shapes and make the boundary less safe.

This violates the repository rule that raw JavaScript interop is born, decoded, encoded,
sequenced, or mutated only in `knoxx.backend.extern.*`. It also makes a misspelled token field
silently become `nil`.

## Scope

1. Define one explicit token-record contract/schema with keyword keys.
2. Make both `grant->token-record` and `load-token-record!` return the same validated CLJS map.
3. Convert `resolve-post-token-record!`, `resolve-token-context!`, actor scoping, and tool granting
   to consume that map without raw-object inspection.
4. Remove obsolete `^js`, `aget`, and `array-seq` assumptions from the ordinary infra route.
5. If the MCP transport requires a JavaScript object, perform the final conversion only in the
   named extern adapter that owns that transport.

## Contract / invariants

- Authentication-contract and OAuth-store producers validate against one shape.
- Missing or malformed membership, actor, organization, email, or tool fields fail at the
  boundary with the canonical authentication error; no field typo silently narrows authority.
- Tool order and allow/deny semantics remain unchanged.
- No second token-record representation or branch-specific consumer is introduced.
- Ordinary domain/law/shape/infra namespaces receive only CLJS maps, vectors, and scalars.

## TDD / proof

1. Capture both producer outputs and every consumer-visible field before the refactor.
2. Make the shared shape test fail independently for a malformed auth-contract grant and OAuth
   store row.
3. Exercise authentication, actor scoping, and tool granting through both producer branches.
4. Add a source/namespace regression forbidding token-record raw interop outside its extern
   adapter.
5. Run focused MCP/auth tests, the full backend suite, server compile, and strict changed-surface
   clj-kondo with zero warnings.

## Non-goals

- Redesigning OAuth storage or grant policy.
- Changing token lifetime, authorization semantics, or the MCP wire protocol.
- Converting only one producer or adding an adapter that hides two in-memory shapes.

## Done when

Both token producers and all consumers use one validated CLJS map, any necessary JavaScript
conversion exists only at the owning extern boundary, and both real MCP authentication paths pass
the same regression suite.
