import assert from 'node:assert/strict';
import { access, mkdtemp, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { cleanupRouteProof } from '../../scripts/route-proof-cleanup.mjs';

test('both failed closes still remove the real run-owned directory', async () => {
  const root = await mkdtemp(join(tmpdir(), 'route-proof-cleanup-'));
  const calls = [];
  const appFailure = new Error('app close rejected');
  const upstreamFailure = new Error('upstream close threw');
  try {
    const failures = await cleanupRouteProof({
      root,
      app: { close: async () => { calls.push('app'); throw appFailure; } },
      upstream: { close: () => { calls.push('upstream'); throw upstreamFailure; } },
    });
    assert.deepEqual(calls, ['app', 'upstream']);
    assert.deepEqual(failures, [appFailure, upstreamFailure]);
    await assert.rejects(access(root), { code: 'ENOENT' });
  } finally {
    await rm(root, { recursive: true, force: true });
  }
});

test('removal failure is retained after both successful closes', async () => {
  const calls = [];
  const failure = new Error('removal failed');
  const failures = await cleanupRouteProof({
    root: '/owned-fixture',
    app: { close: async () => { calls.push('app'); } },
    upstream: { close: async () => { calls.push('upstream'); } },
  }, async (path, options) => {
    calls.push('remove');
    assert.equal(path, '/owned-fixture');
    assert.deepEqual(options, { recursive: true, force: true });
    throw failure;
  });
  assert.deepEqual(calls, ['app', 'upstream', 'remove']);
  assert.deepEqual(failures, [failure]);
});

test('partial acquisition cleans only the resources acquired', async () => {
  assert.deepEqual(await cleanupRouteProof({}), []);
  const root = await mkdtemp(join(tmpdir(), 'route-proof-partial-'));
  try {
    assert.deepEqual(await cleanupRouteProof({ root }), []);
    await assert.rejects(access(root), { code: 'ENOENT' });
  } finally {
    await rm(root, { recursive: true, force: true });
  }
});
