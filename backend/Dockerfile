# Knoxx Backend Dockerfile
# Builds the current shadow-cljs/Fastify backend runtime.
#
# Build prerequisite from backend/:
#   pnpm run build          # writes dist/server.js
#
# Build image:
#   docker build -t knoxx-backend:latest .
#
# Runtime entrypoint:
#   node dist/server.js

FROM node:22-bookworm-slim

ENV NODE_ENV=production
WORKDIR /app

COPY package.json ./
RUN apt-get update \
 && apt-get install -y --no-install-recommends docker.io \
 && rm -rf /var/lib/apt/lists/* \
 && corepack enable \
 && corepack prepare pnpm@10.14.0 --activate \
 && pnpm install --prod --no-frozen-lockfile --ignore-scripts

# Compiled by `shadow-cljs release server` before `docker build`.
COPY dist ./dist
COPY docker ./docker

# Compose/deployment MUST mount the real contracts tree at /app/contracts. It
# cannot be COPYed here: the build context is ./backend, so the repository-level
# contracts/ directory is outside it. Since the translation worker resolves its
# model solely through the config resource in that tree — the TRANSLATION_MODEL
# fallback was removed — an empty mount point means every translation batch
# fails. docker-compose.yml mounts it read-only; any other deployment path must
# do the equivalent.
#
# The directories below are still created so the process boots far enough to
# report a configuration error instead of failing on mkdir.
RUN useradd --uid 1000 --create-home --shell /bin/bash agent || true \
 && mkdir -p /runs/knoxx-agent /tmp/knoxx-agent /app/contracts /app/workspace \
 && chown -R 1000:1000 /runs /tmp/knoxx-agent /app

USER 1000

EXPOSE 8000
CMD ["node", "dist/server.js"]
