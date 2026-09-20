import { fileURLToPath } from 'node:url';

export const testErrorGuard = fileURLToPath(new URL('./shadow-test-error-guard.cjs', import.meta.url));

/**
 * Copy an environment, preserving its Node options and adding the error preload
 * inherited by both Shadow and its test child. Default CONTRACTS_DIR only when
 * absent/null; explicit fixture paths are preserved, not validated here.
 * @returns {NodeJS.ProcessEnv} A new environment object; the input is not mutated.
 */
export function testEnvironment(environment = process.env) {
  return {
    ...environment,
    NODE_OPTIONS: [environment.NODE_OPTIONS, `--require ${JSON.stringify(testErrorGuard)}`]
      .filter(Boolean).join(' '),
    CONTRACTS_DIR: environment.CONTRACTS_DIR ?? 'test/fixtures/empty-contracts',
  };
}
