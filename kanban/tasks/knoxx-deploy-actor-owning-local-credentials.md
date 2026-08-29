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
# Provision the `open-hax` production actor owning the local instance's credentials

> Parent epic: `knoxx-decouple-into-katamorph-contracts`
> Blocks: `knoxx-mcp-actor-ascription` is useless in production without this

## Purpose

The deployed Knoxx has no actor holding Discord/Bluesky credentials, so even
once MCP ascribes an actor there is nothing for it to resolve. The local PM2
instance does have them.

## The actor

**`open-hax`** — the DigitalOcean deployment gets an actor with that id, owning
the same credentials as the local PM2 deployment of knoxx.

Its shape is already declared upstream: `katamorph.schema/ActorContract` carries
`:actor/id`, `:actor/kind` (`:agent`), and — directly relevant here —
`:actor/accounts` with `:discord {:username :user-id}` and
`:bluesky {:handle :did}`. Model the production actor on that rather than on a
knoxx-local shape, even though knoxx cannot require katamorph yet (its
`contract-runtime` fork lacks `schema.cljs`). Matching the declared shape now
means the later cutover is a rename, not a redesign.

## Scope

- Discover the local actor and its credentials — `knoxx_actor_credentials` and
  `knoxx_actors` in Mongo, and/or the local PM2 Knoxx instance's policy DB.
- Provision the equivalent actor on the production host through the deployment
  repo, so it is reproducible rather than hand-made.
- ~~Decide deliberately whether production shares the local actor's credentials
  or gets its own.~~ **Decided 2026-08-05: shared.** The title, purpose and
  actor section all say "the same credentials", and that is what was asked for
  and what shipped — the production actor holds the local `discord_automation`
  actor's `discord_bot` and `bluesky` credentials verbatim.

  The earlier "recommend separate" line contradicted the rest of this card, so
  it is struck rather than left to be read as an open question. The risk it
  named is real and does not go away by being decided: a leak from the public
  host burns the local credentials too, because they are the same secret. That
  is now a follow-up to rotate onto production-only credentials, not a choice
  still to make — see the note below.

## Constraints

- `open-hax/services` is a **public** repository. Credentials go in Actions
  secrets and reach the host through the existing `env.template` → `rendered.env`
  path, which already refuses to deploy a blank value. Nothing lands in git.
- Credentials are actor-owned state in the policy DB, not process env. The
  deploy should write them to the policy DB, not export them as env vars —
  `domain.actor.credentials` deliberately refuses to read env.

## Follow-up: production-only credentials

Sharing was chosen for speed and is a standing risk, not a resolved one. A
separate Discord bot token and Bluesky app-password for production would mean a
compromise of the public host cannot be used against the local instance, and
would make revocation independent. Rotating is cheap once the actor exists —
both are a `PUT /api/admin/actors/:userId/credentials/:provider` away — so this
wants its own card rather than a re-litigation of this one.

## Done when

- A documented, repeatable step provisions the production actor.
- A Discord or Bluesky tool call over MCP against production succeeds end to end.
- The credential values exist only in Actions secrets and the policy DB.
- The sharing decision is recorded, with the risk it carries named rather
  than left implicit.
