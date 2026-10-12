import { spawn } from 'node:child_process';
import { constants } from 'node:os';
import { testEnvironment } from './shadow-test-environment.mjs';

// test-ci is deliberately compiled without autorun. Its separate child inherits
// the same async error preload and contracts fixture as ordinary backend tests.
// Process ownership, bounded output and deadlines belong to the supervisor.
const child = spawn(process.execPath, ['target/test/test-ci.cjs'], {
  stdio: 'inherit',
  env: testEnvironment(),
});

child.on('error', (error) => {
  console.error('[shadow-test-guard] FATAL mutation test spawn:', error.message);
  process.exitCode = 2;
});
child.on('close', (code, signal) => {
  process.exitCode = signal ? 128 + (constants.signals[signal] ?? 1) : (code ?? 2);
});
