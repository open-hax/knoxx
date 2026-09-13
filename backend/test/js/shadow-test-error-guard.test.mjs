import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';
import { testCountersExitCode } from '../../scripts/run-shadow-tests-ci.mjs';
import { testEnvironment, testErrorGuard } from '../../scripts/shadow-test-environment.mjs';

const summary = 'Ran 8 tests containing 46 assertions.\n0 failures, 0 errors.';
const done = `console.log(${JSON.stringify(summary)}); process.exit(0);`;

function execute(code, guarded = true) {
  const result = spawnSync(process.execPath,
    [...(guarded ? ['--require', testErrorGuard] : []), '-e', code],
    { encoding: 'utf8', timeout: 5000, env: { ...process.env, NODE_OPTIONS: '' } });
  assert.ifError(result.error);
  assert.equal(result.signal, null);
  return result;
}

test('reproduces the unguarded CLJS finally(done) false success', () => {
  const result = execute(`(async () => { try { throw Error('before-first-is'); } finally { ${done} } })();`, false);
  assert.equal(result.status, 0);
  assert.match(result.stdout, /0 failures, 0 errors/);
  assert.equal(result.stderr, '');
});

test('throw before first assertion fails even after finally prints green counters', () => {
  const result = execute(`(async () => { try { throw Error('before-first-is'); } finally { ${done} } })();`);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /FATAL unhandled rejection: Error: before-first-is/);
  assert.equal(testCountersExitCode(result.stdout + result.stderr), 1);
});

test('rejected await with finally(done) fails instead of losing the rejection', () => {
  const result = execute(`(async () => { try { await Promise.reject(Error('await-refused')); } finally { ${done} } })();`);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /await-refused/);
});

test('microtasks and immediate callbacks receive an error delivery turn', () => {
  for (const deferred of [
    `queueMicrotask(() => Promise.reject(Error('microtask-refused')));`,
    `setImmediate(() => { throw Error('callback-refused'); });`,
  ]) {
    const result = execute(`${deferred} ${done}`);
    assert.equal(result.status, 1);
    assert.match(result.stderr, /\[shadow-test-guard\] FATAL/);
  }
});

test('a successful test process exits cleanly', () => {
  const result = execute(`(async () => { try { await Promise.resolve(); } finally { ${done} } })();`);
  assert.equal(result.status, 0);
  assert.equal(result.stderr, '');
  assert.equal(testCountersExitCode(result.stdout), 0);
});

test('later success cannot erase an earlier failure exit code', () => {
  assert.equal(execute('process.exit(7); process.exit(0);').status, 7);
  assert.equal(execute('process.exitCode = 9; process.exit(0);').status, 9);
});

test('natural unhandled rejection also fails without an explicit exit', () => {
  const result = execute(`Promise.reject(Error('natural-rejection'));`);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /natural-rejection/);
});

test('counter parser refuses missing, empty, failed, and fatal summaries', () => {
  assert.equal(testCountersExitCode('0 failures, 0 errors.'), 1);
  assert.equal(testCountersExitCode('Ran 0 tests containing 0 assertions.\n0 failures, 0 errors.'), 1);
  assert.equal(testCountersExitCode('Ran 1 tests containing 0 assertions.\n0 failures, 0 errors.'), 1);
  assert.equal(testCountersExitCode('Ran 1 tests containing 1 assertions.\n1 failures, 0 errors.\n' + summary), 1);
  assert.equal(testCountersExitCode(summary + '\n[shadow-test-guard] FATAL unhandled rejection'), 1);
  assert.equal(testCountersExitCode(summary + '\n' + summary), 0);
});

test('test environment preserves existing Node options and explicit contract fixtures', () => {
  const environment = testEnvironment({ NODE_OPTIONS: '--no-warnings', CONTRACTS_DIR: 'my-fixtures', OTHER: 'retained' });
  assert.equal(environment.CONTRACTS_DIR, 'my-fixtures');
  assert.equal(environment.OTHER, 'retained');
  assert.ok(environment.NODE_OPTIONS.startsWith('--no-warnings --require '));
  const result = spawnSync(process.execPath, ['-e', `Promise.reject(Error('inherited-preload')); ${done}`],
    { encoding: 'utf8', timeout: 5000, env: environment });
  assert.ifError(result.error);
  assert.equal(result.status, 1);
  assert.match(result.stderr, /inherited-preload/);
});

test('runner detects a nested failing process even if the Shadow launcher exits zero', () => {
  const directory = mkdtempSync(join(tmpdir(), 'knoxx-shadow-runner-'));
  try {
    mkdirSync(join(directory, 'bin'));
    const fakeShadow = join(directory, 'bin', 'shadow-cljs');
    writeFileSync(fakeShadow, `#!${process.execPath}\nconst {spawnSync}=require('node:child_process');\nconst child=spawnSync(process.execPath,['-e',${JSON.stringify(`(async () => { try { throw Error('nested-await-failure'); } finally { ${done} } })();`)}],{stdio:'inherit',env:process.env});\nprocess.exit(0);\n`, { mode: 0o755 });
    const runner = fileURLToPath(new URL('../../scripts/run-shadow-tests-ci.mjs', import.meta.url));
    const result = spawnSync(process.execPath, [runner, 'test'], {
      encoding: 'utf8', timeout: 10000,
      env: { ...process.env, NODE_OPTIONS: '', PATH: `${join(directory, 'bin')}:${process.env.PATH}` },
    });
    assert.ifError(result.error);
    assert.equal(result.status, 1);
    assert.match(result.stdout, /0 failures, 0 errors/);
    assert.match(result.stderr, /nested-await-failure/);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
});
