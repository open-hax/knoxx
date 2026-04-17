const path = require('path');

module.exports = {
  apps: [
    {
      name: 'knoxx',
      script: 'npx',
      args: 'shadow-cljs watch app',
      cwd: __dirname,
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
