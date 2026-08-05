---
uuid: "knoxx-mcp-consent-server-side-state"
title: "Bind MCP authorization consent to server-side state, not to query parameters"
status: incoming
priority: P0
labels: ["tasks", "5sp", "has-parent", "mcp", "security", "oauth"]
created_at: "2026-08-05T00:00:00Z"
points: 5
category: tasks
---
# Bind MCP consent to server-side state

> Parent epic: `knoxx-decouple-into-katamorph-contracts`
> Found by Codex reviewing `knoxx#219`, while checking whether the actor field
> added there actually binds anything. It does not, and the reason generalises.

## The hole

`GET /api/mcp/oauth/authorize/confirm` mints an authorization code using only:

- the browser's session cookie, and
- query parameters.

There is **no server-side record of what the consent page displayed**. Every
input that decides what the token can do arrives in the URL:

```text
client_id  redirect_uri  code_challenge  scope  tool (repeated)  actor_id
```

The consent page is therefore decorative from a security standpoint. A crafted
link, opened by a user with a live Knoxx session, mints a code for a client the
attacker registered — dynamic client registration is open — with whatever tool
set the attacker chose, and the user sees only a redirect.

`law/consent-actor-unchanged?` does not close this. Its own docstring now says
so: it is a race guard for an admin reassigning the membership's actor while a
page sits open. A hostile caller simply sends the matching `actor_id`.

## Why it is P0 rather than P2

- The code exchange needs PKCE, but the attacker generates the verifier, so PKCE
  costs them nothing here. It protects the code in transit, not the consent.
- `redirect_uri` must belong to a registered client — and registration is open
  and unauthenticated, so the attacker registers their own.
- The minted token is indistinguishable from a legitimate one, so nothing
  downstream can detect it. With `knoxx#219` landed, that token can carry an
  actor and spend real Discord and Bluesky credentials.

## Scope

- On `GET /api/mcp/oauth/authorize`, persist a **consent record** under an
  unguessable single-use nonce, with a short TTL: client id, redirect uri, code
  challenge, membership id, the actor displayed, and the tools *offered*.
- The page carries the nonce. Nothing else it sends is trusted.
- On confirm, look the nonce up, require that it belongs to the confirming
  membership, and take client id, redirect uri, code challenge and actor **from
  the record**. Only the tool selection comes from the form, and it must be a
  subset of the tools the record offered.
- Consume the nonce on use, atomically, the same way `consume-code!` claims a
  code — so a replayed confirmation cannot mint a second code.
- Reject a confirm with no nonce. Do not fall back to query parameters; a
  fallback is the hole.

## Contract obligations

- The consent record shape belongs in `law.mcp-oauth`, beside the code and token
  contracts, with membership id and nonce non-blank for the same reason
  `RevocationRequest` requires them.
- Name the subset rule in `law.*`: "the tools authorized are a subset of the
  tools offered" is an authorization decision, not transport.

## Watch out

- A consent nonce and an authorization code must not be interchangeable. If they
  share a collection, a nonce presented at `/token` must be refused — do not let
  one record type satisfy the other's lookup.
- `mcp-authorize-confirm!` is already at the 60-line function ceiling; this needs
  the extraction done, not squeezed in.

## Done when

- A confirm without a valid nonce is refused.
- A confirm whose tool set exceeds what the record offered is refused, with a
  test for the refusal rather than only the success path.
- A replayed confirm mints no second code.
- No decision on the token's authority is read from a query parameter.
