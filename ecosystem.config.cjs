/**
 * Deprecated Knoxx source-tree PM2 entrypoint.
 *
 * Runtime/devops ownership moved to services/openplanner/ecosystem.host.config.cjs
 * so the Knoxx source checkout can be moved, replaced, or detached from this
 * workspace without carrying host-specific PM2 wiring.
 *
 * Use:
 *   cd services/openplanner
 *   pm2 start ecosystem.host.config.cjs
 *
 * If a compatibility shim is unavoidable, set KNOXX_HOST_ECOSYSTEM_CONFIG to an
 * absolute path and this file will delegate without assuming any workspace path.
 */

const configuredEcosystem = process.env.KNOXX_HOST_ECOSYSTEM_CONFIG;

if (!configuredEcosystem) {
  throw new Error(
    [
      'Knoxx host PM2 config moved out of the source checkout.',
      'Run from the service/devops layer instead:',
      '  cd services/openplanner',
      '  pm2 start ecosystem.host.config.cjs',
      'Or set KNOXX_HOST_ECOSYSTEM_CONFIG=/absolute/path/to/ecosystem.host.config.cjs for temporary delegation.',
    ].join('\n'),
  );
}

module.exports = require(configuredEcosystem);
