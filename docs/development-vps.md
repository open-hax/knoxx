# Development VPS deployment

This checkout has a source-based development instance alongside the existing
containerized Knoxx deployment. The production containers are not restarted or
modified by this setup.

## Installed host prerequisites

- Node.js 22.23.2 in `/usr/local`
- pnpm 10.15.1 through Corepack
- Clojure CLI 1.12.4.1582 in `/usr/local`
- The root, `backend`, and `frontend` pnpm lockfiles installed independently
- PM2 6.0.13 for the three long-running development processes

Java 21 was already installed on the VPS.

## Local development environment

The ignored repository-root `.env` contains the development API key and public
URL overrides. The same random credential protects `KNOXX_API_KEY`, the
OpenAI-compatible `MODEL_LAB_OPENAI_API_KEY` boundary, and the repeatable local
password login for the `pi@open-hax.local` bootstrap system administrator. It
is intentionally not committed. Load the host's shared Knoxx configuration
first, then the development overrides:

```bash
set -a
source /home/err/.env
source /home/err/spaces/knoxx/.env
set +a
```

Retrieve the development API key locally when needed:

```bash
sed -n 's/^KNOXX_API_KEY=//p' /home/err/spaces/knoxx/.env
```

The frontend login credentials are:

```text
Email: pi@open-hax.local
Password: the KNOXX_BOOTSTRAP_SYSTEM_ADMIN_PASSWORD value in .env
```

On backend startup this identity is repeatably assigned `system-admin` and its
local password credential is upserted. Updating the environment value and
restarting the development backend rotates the password.

The key is a randomly generated 256-bit hexadecimal value. Never copy it into a
tracked file.

Knoxx links `@open-hax/openplanner-sdk` from the sibling OpenPlanner checkout.
That dependency was installed at `/home/err/spaces/openplanner` and its
`openplanner-document-hydration` and `openplanner-sdk` packages were built. No
OpenPlanner source was changed.

## Processes and ports

Run the backend compiler, backend server, and frontend development server from
three shells after loading the environment above:

```bash
pnpm -C backend run watch
pnpm -C backend run start:dev
pnpm -C frontend run dev
```

The source backend listens on `0.0.0.0:8000`; the source frontend listens on
`:5173` and proxies `/api`, `/ws`, and `/health` to the backend. Caddy routes
`https://knoxx-dev.promethean.rest` to the frontend through
the `open-hax` Docker bridge gateway at `172.18.0.1:5173`.

UFW permits TCP `5173` and `8000` only from the internal `172.18.0.0/16`
Docker bridge so Caddy can reach the source frontend and direct backend routes.
Neither development port is opened to the public internet.

The frontend shadow-cljs server owns its default control port `9630`, so the
backend shadow-cljs server selects `9631`. The ignored `.env` explicitly points
the backend launcher at `http://127.0.0.1:9631` and allows four minutes for the
initial cold compile.

The processes are saved under PM2 as `knoxx-dev-shadow`,
`knoxx-dev-backend`, and `knoxx-dev-frontend`. The `pm2-err` systemd unit is
enabled so the saved process list is resurrected after a VPS reboot. Inspect
them with:

```bash
pm2 status
pm2 logs knoxx-dev-backend
```

The dev backend's saved PM2 environment reuses the deployed Knoxx container's
Atlas MongoDB URI and Proxx authentication token. It overrides the deployed
service's `openplanner` database name with `MONGODB_DB=knoxx_dev`, keeping the
development policy, sessions, runs, and OpenPlanner SDK data isolated in a new
database on the same Atlas cluster. Credentials are not written into this
checkout. Recreate the process environment from the host service configuration
if the deployed service rotates either credential.

## DNS and ingress

Cloudflare has an A record for `knoxx-dev.promethean.rest` pointing to the VPS
public address `157.245.125.134`. Caddy terminates HTTPS and forwards this
hostname to the development frontend. The Caddy configuration is owned by the
host service layer at `/srv/open-hax/services/caddy/Caddyfile`, outside this
repository.

Validate the public route with:

```bash
curl --fail --show-error \
  -H "X-API-Key: $KNOXX_API_KEY" \
  https://knoxx-dev.promethean.rest/health
```

## Verification

After dependency or backend changes, use the repository-required checks:

```bash
pnpm -C backend run typecheck
pnpm -C backend exec shadow-cljs compile test
pnpm -C frontend run typecheck
pnpm -C frontend run test
pnpm -C frontend run test:cljs
```
