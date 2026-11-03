import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { test } from 'node:test';
import { snapshotBuildFiles, verifyBuildSnapshot } from './wiki-build-snapshot.mjs';

async function fixture(run) {
  const repo = await fs.mkdtemp(path.join(os.tmpdir(), 'knoxx-build-snapshot-'));
  try {
    await fs.mkdir(path.join(repo, 'backend/dist/cljs-runtime'), { recursive: true });
    await fs.mkdir(path.join(repo, 'frontend/dist/assets'), { recursive: true });
    for (const [name, content] of Object.entries({
      'backend/dist/server.js': 'import "./cljs-runtime/handler.js";',
      'backend/dist/cljs-runtime/handler.js': 'export const result = 1;',
      'frontend/dist/index.html': '<script src="/assets/app.js"></script>',
      'frontend/dist/assets/app.js': 'document.title = "Wiki";',
    })) await fs.writeFile(path.join(repo, name), content);
    await run(repo);
  } finally { await fs.rm(repo, { recursive: true, force: true }); }
}

test('a nested runtime mutation is detected while the entrypoint is unchanged', () => fixture(async repo => {
  const before = await snapshotBuildFiles(repo);
  assert.equal((await verifyBuildSnapshot(repo, before)).verified, true);
  await fs.writeFile(path.join(repo, 'backend/dist/cljs-runtime/handler.js'), 'export const result = 2;');
  const after = await snapshotBuildFiles(repo);
  assert.deepEqual(after.files['backend/dist/server.js'], before.files['backend/dist/server.js']);
  assert.notEqual(after.sha256, before.sha256);
  await assert.rejects(verifyBuildSnapshot(repo, before), /Owned build files changed/);
}));

test('frontend additions, deletions and changes invalidate the complete file inventory', () => fixture(async repo => {
  const before = await snapshotBuildFiles(repo);
  const asset = path.join(repo, 'frontend/dist/assets/late.js');
  await fs.writeFile(asset, '');
  await assert.rejects(verifyBuildSnapshot(repo, before), /Owned build files changed/);
  await fs.unlink(asset);
  assert.equal((await verifyBuildSnapshot(repo, before)).verified, true);
  await fs.unlink(path.join(repo, 'frontend/dist/assets/app.js'));
  await assert.rejects(verifyBuildSnapshot(repo, before), /Owned build files changed/);
}));

test('build snapshots refuse file and directory symlinks instead of following them', () => fixture(async repo => {
  const link = path.join(repo, 'backend/dist/linked');
  for (const target of ['../dist/server.js', '../../frontend/dist']) {
    await fs.symlink(target, link);
    await assert.rejects(snapshotBuildFiles(repo), /refuses a symlink/);
    await fs.unlink(link);
  }
}));
