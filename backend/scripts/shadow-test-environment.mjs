import { fileURLToPath } from 'node:url';

export const testErrorGuard = fileURLToPath(new URL('./shadow-test-error-guard.cjs', import.meta.url));

// NODE_OPTIONS reaches both the Shadow CLI and the test process it launches.
// An explicit node invocation can equivalently use --require testErrorGuard.
export function testEnvironment(environment = process.env) {
  return {
    ...environment,
    NODE_OPTIONS: [environment.NODE_OPTIONS, `--require ${JSON.stringify(testErrorGuard)}`]
      .filter(Boolean).join(' '),
    CONTRACTS_DIR: environment.CONTRACTS_DIR ?? 'test/fixtures/empty-contracts',
  };
}
