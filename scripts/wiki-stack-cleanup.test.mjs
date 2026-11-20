import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { spawnSync } from 'node:child_process';
import { cleanupWikiStack, finishWikiStack, reserveEvidenceDirectory } from './wiki-stack-cleanup.mjs';

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

for (const mode of ['boot-only', 'full']) {
  test(`${mode} cannot report completion without source/build provenance`, () => fixture(async config => {
    config.evidence.mode = mode;
    assert.equal(await finishWikiStack({ ...config, close: [] }), 1);
    const result = JSON.parse(await fs.readFile(path.join(config.outputDir, 'result.json'), 'utf8'));
    assert.equal(result.completed, false);
    assert.equal(result.buildProvenance, 'unverified');
    assert.ok(result.failures.some(failure => failure.stage === 'build-provenance'));
    await assert.rejects(fs.access(config.fixtureDirectory), { code: 'ENOENT' });
  }));
}

test('stable build A cannot become completed evidence for observed source B', () => fixture(async config => {
  const { snapshotBuildFiles, verifyBuildSnapshot } = await import('./wiki-build-snapshot.mjs');
  const repo = path.join(path.dirname(config.fixtureDirectory), 'checkout');
  for (const root of ['backend/dist', 'frontend/dist']) await fs.mkdir(path.join(repo, root), { recursive: true });
  await fs.writeFile(path.join(repo, 'backend/dist/server.js'), 'build A');
  await fs.writeFile(path.join(repo, 'frontend/dist/index.html'), 'build A');
  const snapshot = await snapshotBuildFiles(repo);
  config.evidence.observedSourceHead = 'source B';
  config.evidence.failures.push({ stage: 'earlier-check', message: 'retained' });
  assert.equal((await verifyBuildSnapshot(repo, snapshot)).verified, true, 'unchanged A bytes are stable');
  assert.equal(await finishWikiStack({ ...config, close: [] }), 1);
  const result = JSON.parse(await fs.readFile(path.join(config.outputDir, 'result.json'), 'utf8'));
  assert.equal(result.completed, false);
  assert.deepEqual(result.failures.map(failure => failure.stage), ['earlier-check', 'build-provenance']);
}));

for (const mode of ['boot-only', 'full']) {
  test(`${mode} finalizer exits nonzero and removes owned fixtures`, () => fixture(async config => {
    const moduleUrl = new URL('./wiki-stack-cleanup.mjs', import.meta.url).href;
    const child = spawnSync(process.execPath, ['--input-type=module', '-e',
      `import {finishWikiStack} from ${JSON.stringify(moduleUrl)}; const [fixtureDirectory, outputDir, mode] = process.argv.slice(1); process.exitCode = await finishWikiStack({fixtureDirectory, outputDir, evidence:{mode, completed:true, failures:[]}, close:[]});`,
      config.fixtureDirectory, config.outputDir, mode], { encoding: 'utf8' });
    assert.equal(child.status, 1, child.stderr);
    const result = JSON.parse(await fs.readFile(path.join(config.outputDir, 'result.json'), 'utf8'));
    assert.equal(result.completed, false);
    assert.equal(result.buildProvenance, 'unverified');
    await assert.rejects(fs.access(config.fixtureDirectory), { code: 'ENOENT' });
  }));
}

test('evidence directory reservation refuses prior results without replacing them', () => fixture(async config => {
  const prior = path.join(config.outputDir, 'result.json');
  await fs.writeFile(prior, '{"completed":true}');
  await assert.rejects(reserveEvidenceDirectory(config.outputDir), { code: 'EEXIST' });
  assert.equal(await fs.readFile(prior, 'utf8'), '{"completed":true}');
  const fresh = path.join(path.dirname(config.outputDir), 'fresh', 'run');
  await reserveEvidenceDirectory(fresh);
  assert.deepEqual(await fs.readdir(fresh), []);
}));
