# Knoxx Authentication & Onboarding

## Overview

Knoxx supports **GitHub OAuth** login with **cookie-backed sessions** stored in Redis. The system includes:

- **Admin seed**: The `KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL` user is automatically created as a system admin on first boot
- **Invite system**: Admins can create invite codes that auto-provision users with org memberships
- **GitHub OAuth**: "Continue with GitHub" button on the login page
- **Cookie sessions**: Secure, HttpOnly, SameSite cookies with Redis-backed session storage

## Architecture

```
Browser → nginx (HTTPS :443) → Fastify (:8000)
                                  ├── onRequest hook: cookie → x-knoxx-* headers
                                  ├── /api/auth/* routes (JS module)
                                  └── /api/* routes (CLJS, uses x-knoxx-* headers)
```

The key design: the CLJS backend uses `x-knoxx-user-email` and `x-knoxx-org-slug` headers for auth. The `onRequest` hook reads the session cookie from Redis and injects these headers before the CLJS routes execute. This means all existing CLJS auth logic works unchanged.

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

- **DNS**: `knoxx.promethean.rest` → A record `100.77.244.9` (Tailscale IP)
- **TLS**: Let's Encrypt via certbot + Cloudflare DNS challenge
- **nginx**: `/etc/nginx/sites-available/knoxx-https` (HTTPS :443 → backend:8000 + Vite:5173)
- **Cert renewal**: Automatic via `certbot renew` systemd timer

## Frontend

The `AuthBoundary` component wraps the entire app:
- On mount, calls `GET /api/auth/context` (with `credentials: 'include'`)
- If 401 → shows `LoginPage` with "Continue with GitHub" and invite code input
- If authenticated → renders children with `useAuth()` hook available
- `UserMenu` component in the header shows user info + sign out button
