---
uuid: "knoxx-deploy-actor-owning-local-credentials"
title: "Provision a production actor owning the same tool credentials as the local instance"
status: incoming
priority: P1
labels: ["tasks", "3sp", "has-parent", "deploy", "actors"]
created_at: "2026-08-04T00:00:00Z"
points: 3
category: tasks
---
# Provision a production actor owning the local instance's credentials

> Parent epic: `knoxx-decouple-into-katamorph-contracts`
> Blocks: `knoxx-mcp-actor-ascription` is useless in production without this

## Purpose

The deployed Knoxx has no actor holding Discord/Bluesky credentials, so even
once MCP ascribes an actor there is nothing for it to resolve. The local PM2
instance does have them.

## Scope

- Discover the local actor and its credentials — `knoxx_actor_credentials` and
  `knoxx_actors` in Mongo, and/or the local PM2 Knoxx instance's policy DB.
- Provision the equivalent actor on the production host through the deployment
  repo, so it is reproducible rather than hand-made.
- Decide deliberately whether production shares the local actor's credentials or
  gets its own. Sharing is faster; separate credentials mean a leak from the
  public host does not burn the local one. **Recommend separate.**

## Constraints

- `open-hax/services` is a **public** repository. Credentials go in Actions
  secrets and reach the host through the existing `env.template` → `rendered.env`
  path, which already refuses to deploy a blank value. Nothing lands in git.
- Credentials are actor-owned state in the policy DB, not process env. The
  deploy should write them to the policy DB, not export them as env vars —
  `domain.actor.credentials` deliberately refuses to read env.

## Done when

- A documented, repeatable step provisions the production actor.
- A Discord or Bluesky tool call over MCP against production succeeds end to end.
- The credential values exist only in Actions secrets and the policy DB.
