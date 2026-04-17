/**
 * Knoxx (dev) PM2 ecosystem example
 *
 * NOTE:
 * - This file is a *template*. Do not commit real secrets.
 * - shadow-cljs watch provides an nREPL (default port 4500; see shadow-cljs.edn).
 */

module.exports = {
  apps: [
    {
      name: 'knoxx-cephalon-dev',
      cwd: '/home/err/devel/orgs/open-hax/openplanner/packages/knoxx/backend',
      script: 'src/server.mjs',
      // Restart when compiled CLJS output changes.
      watch: ['dist', 'src/server.mjs'],
      watch_delay: 500,
      ignore_watch: ['.shadow-cljs', 'node_modules', 'tmp', '.git'],

      // IMPORTANT: configure these in your real host env (or an env loader).
      env: {
        NODE_ENV: 'development',
        HOST: '0.0.0.0',
        PORT: '8000',
        WORKSPACE_ROOT: '/home/err/devel',
        KNOXX_AGENT_DIR: '/tmp/knoxx-agent',

        // Dependencies
        REDIS_URL: 'redis://localhost:6379',
        OPENPLANNER_BASE_URL: 'http://localhost:7777',
        OPENPLANNER_API_KEY: 'change-me',
        PROXX_BASE_URL: 'http://localhost:8790',
        PROXX_AUTH_TOKEN: 'change-me',
      },
    },

    {
      name: 'knoxx-shadow-watch',
      cwd: '/home/err/devel/orgs/open-hax/openplanner/packages/knoxx/backend',
      script: 'pnpm',
      args: 'watch',
      autorestart: true,
      watch: false,
      env: {
        NODE_ENV: 'development',
      },
    },
  ],
};
