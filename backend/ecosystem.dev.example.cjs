/**
 * Knoxx (dev) PM2 ecosystem example
 *
 * NOTE:
 * - This file is a *template*. Do not commit real secrets.
 * - shadow-cljs watch provides an nREPL (default port 4500; see shadow-cljs.edn).
 * - Dev runtime uses :server-dev / dist-dev; :server / dist is reserved for verification.
 */

module.exports = {
  apps: [
    {
      name: 'knoxx-cephalon-dev',
      cwd: '/path/to/openplanner/packages/agents/knoxx/backend',
      script: 'dist-dev/server.js',
      node_args: '--enable-source-maps',
      // shadow-cljs owns hot reload; PM2 should only restart on hard crashes.
      watch: false,

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
        PROXX_BASE_URL: 'http://localhost:8789',
        PROXX_AUTH_TOKEN: 'change-me-open-hax-proxy-token',
      },
    },

    {
      name: 'knoxx-shadow-watch',
      cwd: '/path/to/openplanner/packages/agents/knoxx/backend',
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
