/**
 * Knoxx dev-mode PM2 ecosystem
 *
 * Runs all three Knoxx services on the host for source-mapped debugging:
 *   1. knoxx-shadow    — shadow-cljs watch (compiles CLJS → dist/)
 *   2. knoxx-backend   — node src/server.mjs (auto-restarts on dist/ changes)
 *   3. knoxx-frontend  — vite dev server with HMR + API proxy to backend
 *   4. knoxx-ingestion — clojure -M:run (kms-ingestion on port 3003)
 *
 * Dependencies (must be running separately):
 *   - Redis     on localhost:6379  (compose: knoxx-redis)
 *   - Postgres  on localhost:5432  (compose: knoxx-postgres, user=kms db=knoxx)
 *   - Proxx     on localhost:8790
 *   - OpenPlanner on localhost:7777
 *
 * Usage:
 *   pm2 start ecosystem.dev.cjs
 *   pm2 logs knoxx            # all knoxx-* logs
 *   pm2 stop knoxx            # stop all
 *   pm2 delete knoxx          # remove all
 */

module.exports = {
  apps: [
    // ── 1. shadow-cljs watch ──────────────────────────────────────────
    {
      name: 'knoxx-shadow',
      cwd: '/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/backend',
      script: 'pnpm',
      args: 'watch',
      watch: false,
      autorestart: true,
      max_restarts: 10,
      restart_delay: 5000,
      env: {
        NODE_ENV: 'development',
      },
    },

    // ── 2. Backend (Node + compiled CLJS) ────────────────────────────
    {
      name: 'knoxx-backend',
      cwd: '/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/backend',
      script: 'src/server.mjs',
      // Auto-restart when shadow-cljs produces new output
      watch: ['dist', 'src/server.mjs'],
      watch_delay: 800,
      ignore_watch: ['.shadow-cljs', 'node_modules', 'tmp', '.git'],
      autorestart: true,
      max_restarts: 15,
      restart_delay: 3000,
      env: {
        NODE_ENV: 'development',
        HOST: '0.0.0.0',
        PORT: '8000',
        WORKSPACE_ROOT: '/home/err/devel',
        KNOXX_SESSION_PROJECT_NAME: 'knoxx-session',
        KNOXX_COLLECTION_NAME: 'devel_docs',
        // Proxx (on host via compose port-forward)
        PROXX_BASE_URL: 'http://127.0.0.1:8790',
        PROXX_DEFAULT_MODEL: 'glm-5',
        PROXX_AUTH_TOKEN: 'change-me-openplanner-proxx-token',
        // OpenPlanner (on host via compose port-forward)
        OPENPLANNER_BASE_URL: 'http://127.0.0.1:7777',
        OPENPLANNER_API_KEY: 'change-me',
        // Redis + Postgres (compose services forwarded to host)
        REDIS_URL: 'redis://127.0.0.1:6379',
        KNOXX_POLICY_DATABASE_URL: 'postgresql://kms:kms@127.0.0.1:5432/knoxx',
        DATABASE_URL: 'postgresql://kms:kms@127.0.0.1:5432/knoxx',
        // STT (NPU service on host)
        KNOXX_STT_BASE_URL: 'http://127.0.0.1:8010',
        // Ingestion service on host
        KMS_INGESTION_URL: 'http://127.0.0.1:3003',
      },
    },

    // ── 3. Frontend (Vite dev server) ────────────────────────────────
    {
      name: 'knoxx-frontend',
      cwd: '/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/frontend',
      script: 'pnpm',
      args: 'dev',
      watch: false,
      autorestart: true,
      max_restarts: 10,
      restart_delay: 3000,
      env: {
        NODE_ENV: 'development',
        // Vite proxy target: the host backend
        VITE_KNOXX_BACKEND_URL: 'http://127.0.0.1:8000',
      },
    },

    // ── 4. Ingestion (Clojure JVM) ──────────────────────────────────
    {
      name: 'knoxx-ingestion',
      cwd: '/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/ingestion',
      script: 'clojure',
      interpreter: '/bin/bash',
      args: '-M:run',
      watch: false,
      autorestart: true,
      max_restarts: 10,
      restart_delay: 5000,
      env: {
        PORT: '3003',
        DATABASE_URL: 'postgresql://kms:kms@127.0.0.1:5432/knoxx',
        REDIS_URL: 'redis://127.0.0.1:6379',
        KNOXX_BACKEND_URL: 'http://127.0.0.1:8000',
        WORKSPACE_PATH: '/home/err/devel',
        OPENPLANNER_BASE_URL: 'http://127.0.0.1:7777',
        PROXX_BASE_URL: 'http://127.0.0.1:8790',
        PROXX_AUTH_TOKEN: 'change-me-openplanner-proxx-token',
      },
    },
  ],
};
