---
uuid: "knoxx-mcp-actor-ascription"
title: "Ascribe an actor to MCP tool calls, and choose it during authorization"
status: incoming
priority: P1
labels: ["tasks", "5sp", "has-parent", "mcp", "actors"]
created_at: "2026-08-04T00:00:00Z"
points: 5
category: tasks
---
# Ascribe an actor to MCP tool calls

> Parent epic: `knoxx-decouple-into-katamorph-contracts`

## Purpose

Discord and Bluesky tools called over MCP have **no owning actor**, so their
credential lookup fails. Actor-owned credentials are the design; the MCP surface
just never carries the actor.

## Verified as of 2026-08-04

```
agent-context/set-context!    called ONLY from infra/agent/session.cljs
                              (wrap-custom-tools-with-agent-context!) — the
                              agent-spawn path
MCP route                     calls (.execute tool "mcp" params nil nil nil)
                              directly; never wraps tools with an agent context
credentials/current-actor-id  reads ONLY agent-context :agent-spec :actor-id
                              ⇒ nil over MCP
get-credential!               throws "No current actor_id is available for
                              <provider> credentials…"
```

The actor is *knowable* and simply dropped: `knoxx_memberships` carries
`actor_id`, and `authz/ctx-actor-id` already reads `[:membership :actor-id]`. But
the MCP OAuth **code and token records store `membershipId`, `userEmail`,
`orgSlug` and no `actorId`**, and `domain.actor.credentials` never consults the
request context regardless.

Consequence: those tool groups are non-functional remotely. It fails safe — no
call runs as the wrong actor — but a user who selects `discord_send` on the
consent page gets a tool that cannot work.

## Scope

- Add actor selection to the authorization consent page
  (`mcp-authorize-client!` / `authorization-consent-html`), listing the actors
  the membership may act as. Default to the membership's own `actor_id` when
  there is exactly one.
- Carry `actorId` through the OAuth code record and into the access token
  (`mcp-authorize-confirm!` → `persist-access-token!`), alongside
  `membershipId`.
- Make credential resolution accept an actor from the request context, not only
  from an agent spawn. Prefer widening `domain.actor.credentials` over faking an
  agent context at the MCP boundary — the latter would couple the two paths.
- Reject at authorize time, not call time, when a selected tool needs an actor
  and none is available. A tool that cannot work should not be offered.

## Contract obligations

- The actor must be one the membership is permitted to act as. Do not trust an
  `actorId` echoed back from the client.
- Name the check in `law.*`; this is an authorization decision, not transport.

## Done when

- A token minted through the consent page carries an actor.
- A Discord or Bluesky tool call over MCP resolves credentials for that actor.
- Selecting an actor the membership may not act as is refused at authorize time.
- A test covers the refusal, not only the success path.
