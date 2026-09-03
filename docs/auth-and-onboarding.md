# Knoxx Authentication & Onboarding

## Overview

Knoxx supports **GitHub OAuth** login with **cookie-backed sessions** stored in Redis. The system includes:

- **Admin seed**: The `KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL` user is automatically created as a system admin on every boot
- **Repeatable local admin login**: `KNOXX_BOOTSTRAP_SYSTEM_ADMIN_PASSWORD` idempotently provisions that admin's local password credential
- **Invite system**: Admins can create invite codes that auto-provision users with org memberships
- **GitHub OAuth**: "Continue with GitHub" button on the login page
- **Cookie sessions**: Secure, HttpOnly, SameSite cookies with Redis-backed session storage

## Architecture

```
Browser → Caddy on the declared DigitalOcean host
              ├── /api/* and /ws/* → knoxx-backend:8000
              └── /*               → knoxx-frontend:80

knoxx-backend
  ├── onRequest hook: cookie → x-knoxx-* headers
  ├── /api/auth/* routes
  └── /api/* routes (CLJS, uses x-knoxx-* headers)
```

The key design: the CLJS backend uses `x-knoxx-user-email` and `x-knoxx-org-slug` headers for auth. The `onRequest` hook reads the session cookie from Redis and injects these headers before the CLJS routes execute. This means all existing CLJS auth logic works unchanged.

Production host placement, pinned SSH trust, Caddy routing, image builds, and
live verification are owned by `open-hax/services`. Local development runs the
backend and frontend directly; it is not exposed by mutating production ingress
or creating a production tunnel.

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
| `KNOXX_BOOTSTRAP_SYSTEM_ADMIN_PASSWORD` | For local admin login | - | Password for the bootstrap admin; keep it in the uncommitted host environment |
| `KNOXX_BOOTSTRAP_SYSTEM_ADMIN_PREVIOUS_EMAILS` | On the first restart after changing a custom bootstrap email | - | Comma-separated prior bootstrap-admin emails whose legacy local credentials must be revoked |
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

## Repeatable local administrator

For a development instance that needs a browser-login administrator without
GitHub OAuth, define the identity and secret in the host environment:

```bash
KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL=developer@example.com
KNOXX_BOOTSTRAP_SYSTEM_ADMIN_NAME="Development Administrator"
KNOXX_BOOTSTRAP_SYSTEM_ADMIN_PASSWORD=replace-with-a-long-random-secret
```

On every backend start Knoxx ensures this user belongs to the primary org,
replaces its role assignment with `system-admin`, assigns the `system_admin`
actor, and upserts a scrypt password credential. Changing the environment
password and restarting the development backend rotates the login password.
Removing the password setting and restarting revokes the previously provisioned
local credential. When changing a custom bootstrap email, put every prior value
in `KNOXX_BOOTSTRAP_SYSTEM_ADMIN_PREVIOUS_EMAILS` for the first restart so
credentials created before Knoxx recorded bootstrap markers are also revoked.
The documented default `system-admin@open-hax.local` is included automatically.
Later rotations use the durable marker, and retaining the previous-email list is
safe and idempotent. Because local-password login is not organization-scoped,
rotation and revocation share one global bootstrap lock and retire managed
identities across former primary organizations. Retirement and replacement are
committed as one Mongo transaction, so a failed replacement leaves the prior
administrator credential active even when the primary organization changes.
Local-password login follows the sole active marked bootstrap credential to its
exact organization membership instead of independently re-reading the primary
organization. Concurrent primary-organization and credential rotations therefore
cannot strand the accepted password; duplicate active markers fail closed.

Knoxx validates Mongo topology before policy seeding and fails closed before
registering routes or binding the HTTP listener when Mongo is a standalone
server. A single-node replica set is sufficient for local development. Run the
[live rotation, revocation, and rollback proof](verification/bootstrap-credential-rotation.md)
against the exact built revision before deployment.

Ordinary `/signup` users remain `basic-user`; this bootstrap does not weaken
self-signup or infer admin rights from arbitrary email/password logins.

## Setting Up GitHub OAuth

1. Go to GitHub → Settings → Developer settings → OAuth Apps → New OAuth App
2. Set **Homepage URL** to `https://knoxx.promethean.rest`
3. Set **Authorization callback URL** to `https://knoxx.promethean.rest/api/auth/callback/github`
4. Copy the **Client ID** and **Client Secret** into your `.env.cephalon-host`

## Production URL

- **DNS**: `knoxx.promethean.rest` resolves to the host declared by
  `open-hax/services/digitalocean/hosts/production.yaml`.
- **TLS and routing**: the Services-owned Caddy definition routes the frontend,
  API, WebSocket, and health surfaces to the Knoxx Compose project.
- **Deployment**: a reviewed Services pull request carrying `deploy` at merge
  time builds and deploys Knoxx from its reviewed `main` revision.
- **Local development**: use the local Vite and Fastify endpoints; it is a
  separate runtime from production.

## Frontend

The `AuthBoundary` component wraps the entire app:
- On mount, calls `GET /api/auth/context` (with `credentials: 'include'`)
- If 401 → shows `LoginPage` with "Continue with GitHub" and invite code input
- If authenticated → renders children with `useAuth()` hook available
- `UserMenu` component in the header shows user info + sign out button

The concrete host inventory and Caddyfile intentionally live in Services so
Knoxx documentation cannot become a second deployment implementation.
