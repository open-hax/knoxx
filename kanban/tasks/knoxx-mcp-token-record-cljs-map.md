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

## Live baseline and remaining gap

The issue's authentication-contract premise is stale because part of the boundary repair has
already landed:

- `infra.auth.method-config/grant->token-record` returns a keyword-keyed CLJS map;
- `law.mcp-token/TokenRecord` is the explicit shared schema; and
- `extern.mcp-token/native-record` validates before any necessary native conversion.

The named schema currently declares `{:closed false}`, so unknown keys survive validation. A row
with a misspelled optional key such as `membershipID` can therefore lose its intended membership
binding and continue into email-based policy resolution as though `membershipId` were deliberately
absent.

Do not recreate those pieces. The remaining OAuth branch still crosses the boundary differently:
`infra.stores.mongo-mcp-oauth/get-token!` serializes its CLJS token data back to a JSON string,
`infra.routes.mcp/load-token-record!` parses that string into a native object, and downstream
context resolution, actor scoping, and tool granting read the result with `aget`, `array-seq`, and
`^js` assumptions. The trusted-loopback branch is also converted to native early only to satisfy
those consumers.

The two producers therefore have a shared contract on paper but not one in-memory representation
through the route. OAuth rows bypass `TokenRecord` validation, and a misspelled field can still
silently become `nil`.

## Scope

1. Preserve `grant->token-record`, the named `TokenRecord`, and the existing extern validation
   boundary as the already-landed baseline. Tighten that named schema or its validator in place;
   introduce no replacement schema, no parallel closed schema, and no parallel adapter.
2. Make the OAuth token persistence API accept and return CLJS token maps instead of JSON strings.
   Keep Mongo driver/native encoding in the named extern boundary that owns it, and leave OAuth
   client and authorization-code representations outside this token-record slice.
3. Make `load-token-record!` validate the OAuth map with `TokenRecord`, and make
   `resolve-post-token-record!` return a validated CLJS map for both OAuth and trusted-loopback
   authentication.
4. Reject every unknown token-record key at the shared validation boundary before any policy
   lookup, actor selection, or tool registration. The exact allowed-key set is the fields declared
   by `TokenRecord`; a near-miss spelling is malformed input, not optional-field absence.
5. Convert `resolve-token-context!`, actor scoping, tool granting, token listing, and every other
   token-record consumer to keyword access and CLJS collections.
6. Remove token-record-specific `^js`, `aget`, `array-seq`, and route-local `js/JSON` assumptions.
   If an actual MCP or Mongo native API still needs a JavaScript value, convert once in its owning
   extern adapter; if `extern.mcp-token/native-record` becomes unused, retire it with its tests
   rather than preserve a dead conversion.

## Contract / invariants

- Authentication-contract and OAuth-store producers validate against the existing
  `law.mcp-token/TokenRecord` shape.
- Missing or malformed required `accessToken`, `clientId`, `userEmail`, or `tools` fields fail at
  the boundary with the canonical authentication error. Absent optional `membershipId`,
  `orgSlug`, or `actorId` fields remain valid exactly as the existing schema and
  `grant->token-record` require; when an optional field is present, a blank or wrong-typed value
  fails validation. No field typo silently narrows authority.
- Absent `membershipId` resolves only by the required `userEmail` plus `orgSlug` when supplied,
  using the canonical policy resolver. That path must yield a single active membership or fail
  closed. It is never entered because an unknown or misspelled membership key was discarded.
- A malformed or legacy OAuth token row fails closed before policy lookup, actor selection, or
  tool registration.
- Tool order and allow/deny semantics remain unchanged.
- No second token-record representation or branch-specific consumer is introduced.
- Ordinary domain/law/shape/infra namespaces receive only CLJS maps, vectors, and scalars.

## TDD / proof

1. Retain the existing tests proving `grant->token-record` is a map and the existing schema/native
   adapter refuses malformed authorization data.
2. RED-prove that the OAuth store/load branch currently returns a native object and bypasses the
   shared schema; GREEN-prove it returns the same CLJS shape as the authentication-contract branch.
3. RED-prove a `membershipID` near miss currently passes the open map. GREEN-prove it fails at the
   shared boundary and `db-policy/resolve-context!` is never called; cover every unknown key in
   both OAuth and trusted-loopback producers.
4. Exercise context resolution, actor reassignment refusal, tool intersection, token listing, and
   malformed/legacy OAuth rows through the real OAuth producer.
5. Exercise the trusted-loopback producer through the same consumers and prove both branches have
   byte-equivalent field semantics without an early native conversion, including legitimate
   absent optional identity fields and malformed present optional fields.
6. Add a focused source/namespace regression forbidding token-record raw interop and JSON
   round-tripping in ordinary infra namespaces.
7. Run focused MCP/auth/store tests, the full backend suite, server compile, and strict
   changed-surface clj-kondo with zero warnings.

## Non-goals

- Recreating the already-landed grant map, `TokenRecord` schema, or native adapter.
- Redesigning OAuth client/code storage, grant policy, or credential lifetime.
- Changing token lifetime, authorization semantics, or the MCP wire protocol.
- Converting only one producer or adding an adapter that hides two in-memory shapes.

## Done when

Both token producers and all consumers use the existing validated CLJS-map contract, OAuth token
persistence no longer JSON-round-trips the record through ordinary infra code, any necessary
JavaScript conversion exists only at an owning extern boundary, and both real MCP authentication
paths pass the same regression suite.
