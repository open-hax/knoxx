// Run after `shadow-cljs release route-registration-proof`, never a dev compile.
import assert from 'node:assert/strict';
import { mkdtemp } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import Fastify from 'fastify';
import { createApp } from '../dist-route-registration-proof/proof.js';
import { cleanupRouteProof } from './route-proof-cleanup.mjs';

const upstreamCalls = [];
let root;
let upstream;
let app;
let proofResult;
let exitCode = 1;
try {
  assert.equal(typeof createApp, 'function', 'release ESM export must initialize');
  root = await mkdtemp(join(tmpdir(), 'knoxx-route-registration-'));
  upstream = Fastify();
  upstream.get('/v1/graph/export', async request => {
    upstreamCalls.push(request.url);
    return { nodes: [{ id: 'route-proof' }], edges: [] };
  });
  upstream.get('/v1/sessions/:sessionId', async request => {
    upstreamCalls.push(request.url);
    return { rows: [{ content: 'fixture memory', extra: {
      org_id: 'proof-org', membership_id: 'proof-member', user_id: 'proof-user',
    } }] };
  });
  const origin = await upstream.listen({ host: '127.0.0.1', port: 0 });
  const options = {
    'workspace-root': root,
    'contracts-dir': join(root, 'empty-contracts'),
    'project-name': 'route-proof',
    'session-project-name': 'route-proof',
    'openplanner-client-mode': 'rest',
    'openplanner-base-url': origin,
    'openplanner-api-key': 'local-fixture-only',
  };
  const invalidOptions = [undefined, null, [], {},
    { ...options, 'workspace-root': 42 },
    { ...options, 'project-name': ' ' },
    { ...options, 'openplanner-client-mode': 'sdk' },
    { ...options, 'openplanner-base-url': 'not a URL' },
    { ...options, 'openplanner-base-url': 'http://127.0.0.1:99999' },
    { ...options, 'openplanner-base-url': 'http://example.com:80' },
    { ...options, unexpected: true },
    ...Object.keys(options).map(key => {
      const incomplete = { ...options };
      delete incomplete[key];
      return incomplete;
    }),
  ];
  for (const invalid of invalidOptions) {
    await assert.rejects(createApp(invalid), /Invalid route registration proof options/);
  }
  const registered = await createApp(options);
  app = registered.app;
  // One sentinel per group, including the final group: registration must finish.
  const expectedRoutes = [
    ['GET', '/health'], ['GET', '/api/admin/bootstrap'],
    ['GET', '/api/memory/sessions'], ['GET', '/api/actors/mailbox'],
    ['GET', '/api/tools/catalog'], ['GET', '/api/proxx/health'],
    ['GET', '/ws/voice/tts'],
    ['GET', '/api/admin/resources'], ['GET', '/api/cms/documents'],
    ['GET', '/api/documents'], ['GET', '/api/graph/export'],
    ['GET', '/api/workspace-media/raw'], ['GET', '/api/studio/state'],
    ['GET', '/api/ingestion/browse'], ['GET', '/api/data/health'],
    ['GET', '/api/knoxx/health'], ['GET', '/api/translations/segments'],
  ];
  for (const [method, url] of expectedRoutes) {
    assert.ok(app.hasRoute({ method, url }), `${method} ${url} registered`);
  }
  const request = (url, principal) => app.inject({
    method: 'GET', url, headers: { 'x-proof-principal': principal },
  });
  const denied = await request('/api/graph/export', 'denied');
  assert.equal(denied.statusCode, 403, denied.body);
  assert.equal(upstreamCalls.length, 0, 'permission refusal precedes the injected dependency');
  const graph = await request('/api/graph/export', 'allowed');
  assert.equal(graph.statusCode, 200, graph.body);
  assert.deepEqual(graph.json(), { nodes: [{ id: 'route-proof' }], edges: [] });
  const memory = await request('/api/memory/sessions/route-proof', 'allowed');
  assert.equal(memory.statusCode, 200, memory.body);
  assert.equal(memory.json().rows[0].content, 'fixture memory');
  assert.equal(upstreamCalls.length, 2, 'both !-suffixed injected dependencies ran');
  proofResult = { status: 'passed', optimization: 'simple', target: 'esm',
    registeredRoutes: registered.routes.length, groupSentinels: expectedRoutes.length,
    invalidOptionsRejected: invalidOptions.length,
    graphPermissionStatus: denied.statusCode, graphStatus: graph.statusCode,
    memoryStatus: memory.statusCode, upstreamCalls };
  exitCode = 0;
} catch (error) {
  console.error(error);
} finally {
  const cleanupFailures = await cleanupRouteProof({ app, upstream, root });
  for (const error of cleanupFailures) console.error('Route proof cleanup failed:', error);
  if (cleanupFailures.length) exitCode = 1;
}
if (exitCode === 0) console.log(JSON.stringify(proofResult, null, 2));
// Imported production modules own periodic timers; this isolated verifier owns
// its process and exits only after both Fastify instances and fixture cleanup.
process.exit(exitCode);
