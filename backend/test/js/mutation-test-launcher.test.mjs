import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdtempSync, mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import test from 'node:test';

const launcher = fileURLToPath(new URL('../../scripts/run-mutation-tests.mjs', import.meta.url));

function fixture(source, environment = process.env) {
  const directory = mkdtempSync(join(tmpdir(), 'knoxx-mutation-launcher-'));
  try {
    mkdirSync(join(directory, 'target/test'), { recursive: true });
    writeFileSync(join(directory, 'target/test/test-ci.cjs'), source);
    return spawnSync(process.execPath, [launcher], { cwd: directory, env: environment, encoding: 'utf8', timeout: 5000 });
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
}

test('actual guarded child rejects rejection hidden by immediate green exit', () => {
  const result = fixture("console.log('Ran 1 test containing 1 assertion.\\n0 failures, 0 errors.'); Promise.reject(new Error('fixture rejection')); process.exit(0);");
  assert.equal(result.status, 1);
  assert.match(result.stderr, /\[shadow-test-guard\] FATAL/);
});

test('ordinary green and completed red preserve their normal exit contracts', () => {
  const green = fixture("console.log('Ran 1 test containing 1 assertion.\\n0 failures, 0 errors.'); process.exit(0);");
  const red = fixture("console.log('Ran 1 test containing 1 assertion.\\n1 failure, 0 errors.'); process.exit(1);");
  assert.equal(green.status, 0);
  assert.equal(red.status, 1);
  assert.doesNotMatch(red.stderr, /FATAL/);
});

test('child receives the existing default contracts fixture environment', () => {
  const environment = { ...process.env };
  delete environment.CONTRACTS_DIR;
  const result = fixture("if (process.env.CONTRACTS_DIR !== 'test/fixtures/empty-contracts') process.exit(2); process.exit(0);", environment);
  assert.equal(result.status, 0);
});

test('child preserves an explicit caller contracts fixture environment', () => {
  const result = fixture("if (process.env.CONTRACTS_DIR !== 'test/fixtures/caller-contracts') process.exit(2); process.exit(0);", {
    ...process.env,
    CONTRACTS_DIR: 'test/fixtures/caller-contracts',
  });
  assert.equal(result.status, 0);
});
