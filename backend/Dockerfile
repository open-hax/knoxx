# Knoxx Backend Dockerfile
# Builds the CLJS-based Knoxx backend service
#
# Build: docker build -t knoxx-knoxx-backend:latest .
# Run:   docker compose -f services/openplanner/docker-compose.yml up -d knoxx-backend
#
# Prerequisites:
#   1. CLJS must be compiled first: npx shadow-cljs release app
#   2. dist/app.js must exist before building this image

FROM node:22-bookworm-slim

ENV NODE_ENV=production
WORKDIR /app

# Install pnpm
COPY package.json pnpm-lock.yaml ./
RUN corepack enable && corepack prepare pnpm@10.14.0 --activate
RUN pnpm install --prod --no-frozen-lockfile

# Copy compiled CLJS and server bootstrap
COPY dist ./dist
COPY src/server.mjs ./src/server.mjs
COPY src/policy-db.mjs ./src/policy-db.mjs
# .env.example is optional - compose provides environment
RUN touch .env.example

# Ensure the non-root user can write to runtime directories
RUN useradd --uid 1000 --create-home --shell /bin/bash agent || true
RUN mkdir -p /runs/knoxx-agent && chown -R 1000:1000 /runs /app

USER 1000

EXPOSE 8000
CMD ["node", "src/server.mjs"]
