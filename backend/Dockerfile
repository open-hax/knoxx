FROM node:22-bookworm-slim

ENV NODE_ENV=production
WORKDIR /app

COPY package.json pnpm-lock.yaml ./
RUN corepack enable && corepack prepare pnpm@10.14.0 --activate
RUN pnpm install --prod --no-frozen-lockfile

COPY dist ./dist
COPY src/server.mjs ./src/server.mjs
COPY src/policy-db.mjs ./src/policy-db.mjs
COPY .env.example ./.env.example

# Ensure the non-root user can write to runtime directories
RUN useradd --uid 1000 --create-home --shell /bin/bash agent || true
RUN mkdir -p /runs/knoxx-agent && chown -R 1000:1000 /runs /app

USER 1000

EXPOSE 8000
CMD ["node", "src/server.mjs"]
