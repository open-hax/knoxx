#!/usr/bin/env bash
set -euo pipefail

: "${PROMETHEAN_SSH_HOST:?PROMETHEAN_SSH_HOST is required}"
: "${PROMETHEAN_SSH_USER:=error}"
: "${PROMETHEAN_SSH_KEY_PATH:?PROMETHEAN_SSH_KEY_PATH is required}"
: "${KNOXX_REMOTE_SOURCE_PATH:?KNOXX_REMOTE_SOURCE_PATH is required}"
: "${OPENPLANNER_SERVICE_PATH:?OPENPLANNER_SERVICE_PATH is required}"
: "${KNOXX_COMPOSE_PROJECT:?KNOXX_COMPOSE_PROJECT is required}"
: "${KNOXX_BACKEND_PORT:?KNOXX_BACKEND_PORT is required}"
: "${KNOXX_PUBLIC_BASE_URL:?KNOXX_PUBLIC_BASE_URL is required}"
: "${DEPLOY_ENV:?DEPLOY_ENV is required}"

rsync -az --delete \
  --exclude '.git' \
  --exclude 'node_modules' \
  --exclude '.shadow-cljs' \
  --exclude 'target' \
  --exclude 'coverage' \
  -e "ssh -i ${PROMETHEAN_SSH_KEY_PATH}" \
  ./ "${PROMETHEAN_SSH_USER}@${PROMETHEAN_SSH_HOST}:${KNOXX_REMOTE_SOURCE_PATH}/"

ssh -i "${PROMETHEAN_SSH_KEY_PATH}" "${PROMETHEAN_SSH_USER}@${PROMETHEAN_SSH_HOST}" \
  DEPLOY_ENV="$DEPLOY_ENV" \
  KNOXX_BACKEND_PORT="$KNOXX_BACKEND_PORT" \
  KNOXX_COMPOSE_PROJECT="$KNOXX_COMPOSE_PROJECT" \
  KNOXX_PUBLIC_BASE_URL="$KNOXX_PUBLIC_BASE_URL" \
  KNOXX_REMOTE_SOURCE_PATH="$KNOXX_REMOTE_SOURCE_PATH" \
  OPENPLANNER_SERVICE_PATH="$OPENPLANNER_SERVICE_PATH" \
  'bash -s' <<'REMOTE'
set -euo pipefail
cd "$OPENPLANNER_SERVICE_PATH"
ENV_FILE=".env.vps"
if [ "$DEPLOY_ENV" = "staging" ]; then
  ENV_FILE=".env.staging"
  if [ ! -f "$ENV_FILE" ]; then cp .env.vps "$ENV_FILE"; fi
fi
prod_token=$(docker exec proxx-production-federation-proxx-a1-1 printenv PROXY_AUTH_TOKEN)
python3 - "$prod_token" "$ENV_FILE" <<'PYREMOTE'
from pathlib import Path
import secrets, sys
prod_token, env_file = sys.argv[1], sys.argv[2]
p=Path(env_file)
vals={}
lines=p.read_text().splitlines() if p.exists() else []
for line in lines:
    if '=' in line and not line.lstrip().startswith('#'):
        k,v=line.split('=',1); vals[k]=v
updates={
    'PROXX_BASE_URL':'https://proxx.promethean.rest',
    'PROXX_AUTH_TOKEN':prod_token,
    'PROXX_DEFAULT_MODEL':'gpt-5.5',
    'KNOXX_API_KEY_USER_EMAIL': vals.get('KNOXX_API_KEY_USER_EMAIL') or vals.get('KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL') or 'foamy125@gmail.com',
}
if not vals.get('KNOXX_API_KEY'):
    updates['KNOXX_API_KEY']='knoxx-dev-'+secrets.token_urlsafe(32)
out=[]; seen=set()
for line in lines:
    if '=' in line and not line.lstrip().startswith('#'):
        k=line.split('=',1)[0]
        if k in updates:
            out.append(f'{k}={updates[k]}'); seen.add(k); continue
    out.append(line)
for k,v in updates.items():
    if k not in seen: out.append(f'{k}={v}')
p.write_text('\n'.join(out)+'\n')
PYREMOTE
unset prod_token
cat > "deploy.knoxx.${DEPLOY_ENV}.override.yml" <<YAML
services:
  knoxx-postgres:
    ports: !reset []
  knoxx-redis:
    ports: !reset []
  backend:
    build:
      context: ${KNOXX_REMOTE_SOURCE_PATH}/backend
      dockerfile: Dockerfile
    env_file:
      - path: ${KNOXX_REMOTE_SOURCE_PATH}/.env
        required: false
    environment:
      PROXX_BASE_URL: \${PROXX_BASE_URL:-https://proxx.promethean.rest}
      PROXX_AUTH_TOKEN: \${PROXX_AUTH_TOKEN:?PROXX_AUTH_TOKEN is required}
      PROXX_DEFAULT_MODEL: \${PROXX_DEFAULT_MODEL:-gpt-5.5}
      KNOXX_API_KEY: \${KNOXX_API_KEY:?KNOXX_API_KEY is required}
      KNOXX_API_KEY_USER_EMAIL: \${KNOXX_API_KEY_USER_EMAIL:-foamy125@gmail.com}
      KNOXX_PUBLIC_BASE_URL: ${KNOXX_PUBLIC_BASE_URL}
    ports: !reset
      - "127.0.0.1:${KNOXX_BACKEND_PORT}:8000"
    volumes:
      - knoxx-workspace:/app/workspace
      - knoxx-runs:/runs/knoxx-agent
      - /var/run/docker.sock:/var/run/docker.sock
      - \${KNOXX_SANDBOX_ROOT_DIR:-/home/error/devel/services/openplanner/runtime/knoxx-sandboxes}:\${KNOXX_SANDBOX_ROOT_DIR:-/home/error/devel/services/openplanner/runtime/knoxx-sandboxes}
      - ${KNOXX_REMOTE_SOURCE_PATH}/contracts:/app/contracts:ro
      - ./cloud/github-app-key.pem:/run/secrets/github-app-key.pem:ro
  frontend:
    build:
      context: ${KNOXX_REMOTE_SOURCE_PATH}/frontend
      dockerfile: Dockerfile
  knoxx-ingestion:
    build:
      context: ${KNOXX_REMOTE_SOURCE_PATH}/ingestion
      dockerfile: Dockerfile
YAML
docker compose --env-file "$ENV_FILE" --project-name "$KNOXX_COMPOSE_PROJECT" -f docker-compose.knoxx.yml -f "deploy.knoxx.${DEPLOY_ENV}.override.yml" up -d --build backend
for _ in $(seq 1 30); do
  status=$(docker inspect -f '{{.State.Health.Status}}' "${KNOXX_COMPOSE_PROJECT}-backend-1" 2>/dev/null || docker inspect -f '{{.State.Health.Status}}' knoxx-backend-1 2>/dev/null || echo none)
  [ "$status" = healthy ] && break
  sleep 6
done
key=$(grep -E '^KNOXX_API_KEY=' "$ENV_FILE" | tail -1 | cut -d= -f2-)
curl -fsS -H "x-api-key: ${key}" "http://127.0.0.1:${KNOXX_BACKEND_PORT}/api/knoxx/health" >/tmp/knoxx-health.json
REMOTE
