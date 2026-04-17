module.exports = {
  apps: [
    {
      name: 'knoxx',
      script: 'npx',
      args: 'shadow-cljs watch app',
      cwd: '/home/err/devel/orgs/open-hax/openplanner/packages/knoxx/backend',
      watch: false,
      autorestart: true,
      max_restarts: 10,
      restart_delay: 5000,
      env: {
        NODE_ENV: 'development',
      },
    },
  ],
};
