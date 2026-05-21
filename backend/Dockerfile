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

COPY package.json pnpm-lock.yaml ./
RUN corepack enable \
 && corepack prepare pnpm@10.14.0 --activate \
 && pnpm install --prod --frozen-lockfile

# Compiled by `shadow-cljs release server` before `docker build`.
COPY dist ./dist

# Compose/deployment should normally mount or copy real contract files and
# workspace roots. Create writable/default directories so the process can still
# boot far enough to report configuration errors instead of failing on mkdir.
RUN useradd --uid 1000 --create-home --shell /bin/bash agent || true \
 && mkdir -p /runs/knoxx-agent /tmp/knoxx-agent /app/contracts /app/workspace \
 && chown -R 1000:1000 /runs /tmp/knoxx-agent /app

USER 1000

EXPOSE 8000
CMD ["node", "dist/server.js"]
