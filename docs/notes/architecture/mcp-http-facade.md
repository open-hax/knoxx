---
title: Knoxx MCP HTTP Facade
category: architecture
created: 2026-04-27
status: stable
tags: [mcp, oauth, http, sse, redis]
---

# Knoxx MCP HTTP Facade

Knoxx exposes an **MCP (Model Context Protocol) server over HTTP**.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/mcp` | JSON-RPC |
| `GET` | `/mcp` | SSE / session transport |
| `DELETE` | `/mcp` | Session close |
| `GET` | `/.well-known/oauth-authorization-server` | Discovery |
| `GET` | `/.well-known/oauth-protected-resource` (also `/.well-known/oauth-protected-resource/mcp` and `/mcp/.well-known/oauth-protected-resource`) | Protected-resource metadata |
| `GET` | `/api/mcp/oauth/authorize` | Consent screen (browser session) |
| `GET` | `/api/mcp/oauth/authorize/confirm` | Consent confirmation, issues code (browser session) |
| `POST` | `/api/mcp/oauth/register` | Dynamic client registration |
| `POST` | `/api/mcp/oauth/token` | PKCE code exchange |
| `GET` | `/api/mcp/tokens` | List user tokens (browser session) |
| `DELETE` | `/api/mcp/tokens/:tokenId` | Revoke token (browser session) |

## Auth Flow

1. MCP client redirects user → `GET /api/mcp/oauth/authorize?...`
2. User logs in via Knoxx GitHub OAuth (`/api/auth/login`) if needed
3. Knoxx shows consent screen with **capability dials** (tool allowlist)
4. Client exchanges code → `POST /api/mcp/oauth/token` (PKCE S256)
5. Client calls MCP endpoints with `Authorization: Bearer <access_token>`

## Notes

- Tokens stored in Redis (same `REDIS_URL` as Knoxx sessions)
  - *2026-09-30:* Redis is retired here. OAuth clients, codes, and tokens now
    live in Mongo collections `knoxx_mcp_clients`, `knoxx_mcp_codes`, and
    `knoxx_mcp_tokens` (`backend/src/cljs/knoxx/backend/infra/stores/mongo_mcp_oauth.cljs:9-11`).
    Routes are defined in `backend/src/cljs/knoxx/backend/infra/routes/mcp.cljs:268-655`.
- Delegated tools are intersected with the user's current Knoxx policy context;
  tokens cannot grant capabilities above the user's membership tier
