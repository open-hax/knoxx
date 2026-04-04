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

EXPOSE 8000
CMD ["node", "src/server.mjs"]
