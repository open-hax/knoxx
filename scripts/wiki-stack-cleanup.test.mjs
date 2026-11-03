import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { cleanupWikiStack } from './wiki-stack-cleanup.mjs';

async function fixture(run) {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), 'knoxx-tour-cleanup-'));
  const fixtureDirectory = path.join(directory, 'fixtures');
  const outputDir = path.join(directory, 'evidence');
  try {
    await fs.mkdir(fixtureDirectory);
    await fs.writeFile(path.join(fixtureDirectory, 'owned-ledger'), 'temporary');
    await fs.mkdir(outputDir);
    await run({ fixtureDirectory, outputDir, evidence: { completed: true, failures: [] } });
  } finally { await fs.rm(directory, { recursive: true, force: true }); }
}

test('resource cleanup failure leaves a failed evidence record and still removes fixtures', () => fixture(async config => {
  let laterClosed = false;
  await cleanupWikiStack({ ...config, close: [
    async () => { throw new Error('Owned service would not stop'); },
    async () => { laterClosed = true; },
  ] });
  assert.equal(laterClosed, true);
  const result = JSON.parse(await fs.readFile(path.join(config.outputDir, 'result.json'), 'utf8'));
  assert.equal(result.completed, false, 'A cleanup failure must not retain a completed result');
  assert.deepEqual(result.failures, [{ stage: 'cleanup', message: 'Owned service would not stop' }]);
  await assert.rejects(fs.access(config.fixtureDirectory), { code: 'ENOENT' });
}));

test('failed evidence writing cannot leave the disposable identity and ledger fixtures behind', () => fixture(async config => {
  await fs.rm(config.outputDir, { recursive: true });
  await fs.writeFile(config.outputDir, 'not a directory');
  await assert.rejects(cleanupWikiStack({ ...config, close: [] }), { code: 'ENOTDIR' });
  await assert.rejects(fs.access(config.fixtureDirectory), { code: 'ENOENT' });
}));

test('successful cleanup records completion only after all resources and fixtures are removed', () => fixture(async config => {
  let closed = false;
  await cleanupWikiStack({ ...config, close: [async () => { closed = true; }] });
  const result = JSON.parse(await fs.readFile(path.join(config.outputDir, 'result.json'), 'utf8'));
  assert.equal(closed, true);
  assert.equal(result.completed, true);
  assert.deepEqual(result.failures, []);
  assert.ok(result.finishedAt);
  await assert.rejects(fs.access(config.fixtureDirectory), { code: 'ENOENT' });
}));
