/*
 * Knoxx backend PM2 development ecosystem example.
 *
 * This file is a template. Do not commit real secrets.
 *
 * Current backend dev shape:
 *   1. shadow-cljs watch server-dev writes dist-dev/server.js and owns hot reload.
 *   2. scripts/start-server-dev.cljs waits for the watch artifact, then imports it.
 *
 * For the full local stack, prefer the repo-root ecosystem.config.cjs. This file
 * is only a backend-local example for isolated backend development.
 */

const backendDir = '/path/to/openplanner/packages/agents/knoxx/backend';

module.exports = {
  apps: [
    {
      name: 'knoxx-shadow-watch',
      cwd: backendDir,
      script: 'pnpm',
      args: 'exec shadow-cljs --source-maps watch server-dev',
      autorestart: true,
      watch: false,
      env: {
        NODE_ENV: 'development',
      },
    },

    {
      name: 'knoxx-backend-dev',
      cwd: backendDir,
      script: 'scripts/start-server-dev.cljs',
      interpreter: 'nbb',
      watch: false,
      autorestart: true,
      wait_ready: true,
      listen_timeout: 240000,
      kill_timeout: 45000,
      shutdown_with_message: true,

      // IMPORTANT: configure these in your real host env or env loader.
      // Never paste committed secrets into this template.
      env: {
        NODE_ENV: 'development',
        HOST: '0.0.0.0',
        PORT: '8000',
        WORKSPACE_ROOT: '/path/to/workspace',
        CONTRACTS_DIR: '/path/to/openplanner/packages/agents/knoxx/contracts',
        KNOXX_AGENT_DIR: '/tmp/knoxx-agent',

        REDIS_URL: 'redis://127.0.0.1:6379',
        KNOXX_POLICY_DATABASE_URL: 'postgresql://kms:kms@127.0.0.1:5432/knoxx',
        DATABASE_URL: 'postgresql://kms:kms@127.0.0.1:5432/knoxx',

        OPENPLANNER_BASE_URL: 'http://127.0.0.1:7777',
        OPENPLANNER_API_KEY: 'change-me',
        PROXX_BASE_URL: 'http://127.0.0.1:8789',
        PROXX_AUTH_TOKEN: 'change-me-open-hax-proxy-token',
        PROXX_DEFAULT_MODEL: 'glm-5',
        KMS_INGESTION_URL: 'http://127.0.0.1:3003',

        KNOXX_API_KEY: 'change-me',
        KNOXX_API_KEY_USER_EMAIL: 'pi@open-hax.local',
      },
    },
  ],
};
