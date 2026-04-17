# Knoxx Authentication & Onboarding

## Overview

Knoxx supports **GitHub OAuth** login with **cookie-backed sessions** stored in Redis. The system includes:

- **Admin seed**: The `KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL` user is automatically created as a system admin on first boot
- **Invite system**: Admins can create invite codes that auto-provision users with org memberships
- **GitHub OAuth**: "Continue with GitHub" button on the login page
- **Cookie sessions**: Secure, HttpOnly, SameSite cookies with Redis-backed session storage

## Architecture

```
Browser → Caddy (ussy.promethean.rest, HTTPS :443, auto-TLS)
              ├── /api/* → SSH tunnel :8443 → laptop:8000 (Fastify backend)
              └── /*     → SSH tunnel :8444 → laptop:5173 (Vite dev server)

Laptop (dev machine):
  Fastify (:8000)
    ├── onRequest hook: cookie → x-knoxx-* headers
    ├── /api/auth/* routes (JS module)
    └── /api/* routes (CLJS, uses x-knoxx-* headers)
  Vite (:5173)
    └── React SPA
  nginx (:80, localhost only)
    └── Dev access fallback
```

The key design: the CLJS backend uses `x-knoxx-user-email` and `x-knoxx-org-slug` headers for auth. The `onRequest` hook reads the session cookie from Redis and injects these headers before the CLJS routes execute. This means all existing CLJS auth logic works unchanged.

## Infrastructure: SSH Tunnel

The dev laptop connects to `ussy.promethean.rest` via an autossh tunnel that persists across disconnects:

- **Local systemd service**: `~/.config/systemd/user/knoxx-tunnel.service`
- **ussy:8443** → laptop:8000 (backend API)
- **ussy:8444** → laptop:5173 (Vite frontend)
- **Caddy on ussy** routes `knoxx.promethean.rest` to these tunnel ports
- **ussy sshd** has `GatewayPorts clientspecified` to allow 0.0.0.0 binding

Tunnel management:
```bash
systemctl --user status knoxx-tunnel    # Check status
systemctl --user restart knoxx-tunnel   # Restart
journalctl --user -u knoxx-tunnel -f    # Follow logs
```

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `KNOXX_GITHUB_OAUTH_CLIENT_ID` | For GitHub login | - | GitHub OAuth App Client ID |
| `KNOXX_GITHUB_OAUTH_CLIENT_SECRET` | For GitHub login | - | GitHub OAuth App Client Secret |
| `KNOXX_PUBLIC_BASE_URL` | Yes | `http://localhost` | Public URL for callbacks and cookie domain |
| `KNOXX_SESSION_SECRET` | Recommended | auto-generated | AES-256-GCM key for session tokens |
| `KNOXX_SESSION_TTL_SECONDS` | No | `86400` | Session cookie lifetime (24h default) |
| `KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL` | Yes | `system-admin@open-hax.local` | Email of the auto-seeded system admin |
| `KNOXX_BOOTSTRAP_SYSTEM_ADMIN_NAME` | No | `Knoxx System Admin` | Display name for the bootstrap admin |
| `KNOXX_POLICY_DATABASE_URL` | Yes | - | PostgreSQL connection string |
| `REDIS_URL` | Yes | `redis://127.0.0.1:6379` | Redis for session storage |
| `GMAIL_APP_EMAIL` | For invite emails | - | Gmail address for sending invite emails |
| `GMAIL_APP_PASSWORD` | For invite emails | - | Gmail app password for SMTP |

## API Endpoints

### Public

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/auth/config` | Returns `{ githubEnabled, publicBaseUrl, loginUrl }` |
| `GET` | `/api/auth/login?redirect=/` | Redirects to GitHub OAuth authorize URL |
| `GET` | `/api/auth/callback/github` | GitHub OAuth callback (exchange code, create session, redirect) |

### Authenticated

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/auth/context` | Returns current user context (user, org, roles, permissions) |
| `POST` | `/api/auth/logout` | Deletes session and clears cookie |
| `POST` | `/api/auth/invite` | Create an invite (requires `org.users.invite` permission) |
| `POST` | `/api/auth/invite/redeem` | Redeem an invite code |
| `GET` | `/api/auth/invites` | List invites for current org |

## Invite Flow

1. Admin calls `POST /api/auth/invite` with `{ email, roleSlugs }`
2. Backend creates an invite with a unique code and sends an email (if Gmail configured)
3. Invitee visits the login page with `?invite=CODE&email=EMAIL`
4. After authenticating (GitHub), invitee redeems the code via `POST /api/auth/invite/redeem`
5. Backend auto-provisions the user with the specified org membership and roles

## Whitelist Logic

A user can log in if they pass **any** of these checks:

1. **Bootstrap admin**: Email matches `KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL`
2. **Existing user**: Email exists in the `users` table with `status = 'active'`
3. **Invite holder**: Has a pending invite (redeemed during login)

If none match, the GitHub callback redirects to `/login?error=not_whitelisted` where the user can enter an invite code.

## Setting Up GitHub OAuth

1. Go to GitHub → Settings → Developer settings → OAuth Apps → New OAuth App
2. Set **Homepage URL** to `https://knoxx.promethean.rest`
3. Set **Authorization callback URL** to `https://knoxx.promethean.rest/api/auth/callback/github`
4. Copy the **Client ID** and **Client Secret** into your `.env.cephalon-host`

## Production URL

- **DNS**: `knoxx.promethean.rest` → A record `104.130.31.129` (ussy.promethean.rest public IP)
- **TLS**: Caddy on ussy auto-provisions Let's Encrypt certificates via ACME HTTP-01 challenge
- **Caddy config**: `/home/error/devel/services/proxx/Caddyfile` on ussy (knoxx.promethean.rest block)
- **Tunnel**: `~/.config/systemd/user/knoxx-tunnel.service` (autossh, restarts automatically)
- **Local dev**: `http://localhost` via nginx (:80) for direct laptop access

## Frontend

The `AuthBoundary` component wraps the entire app:
- On mount, calls `GET /api/auth/context` (with `credentials: 'include'`)
- If 401 → shows `LoginPage` with "Continue with GitHub" and invite code input
- If authenticated → renders children with `useAuth()` hook available
- `UserMenu` component in the header shows user info + sign out button

## Caddyfile on ussy

The knoxx block in `/home/error/devel/services/proxx/Caddyfile`:

```
knoxx.promethean.rest {
  encode gzip zstd

  header {
    Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
  }

  # API and WebSocket routes → backend (port 8000 via tunnel)
  handle /api/ingestion/ws/* {
    reverse_proxy 172.18.0.1:8443
  }
  handle /api/ingestion/* {
    reverse_proxy 172.18.0.1:8443
  }
  handle /api/* {
    reverse_proxy 172.18.0.1:8443
  }
  handle /ws/* {
    reverse_proxy 172.18.0.1:8443
  }
  handle /health {
    reverse_proxy 172.18.0.1:8443
  }

  # Everything else → Vite frontend (port 5173 via tunnel)
  handle {
    reverse_proxy 172.18.0.1:8444
  }
}
```

After editing, copy into container and reload:
```bash
docker cp /home/error/devel/services/proxx/Caddyfile <container>:/etc/caddy/Caddyfile.new
docker exec <container> caddy reload --config /etc/caddy/Caddyfile.new --adapter caddyfile
```
