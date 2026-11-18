import { test } from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { spawn } from 'node:child_process';
import { mkdtemp, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js';

const pause = ms => new Promise(r => setTimeout(r, ms));
test('HTTP and SDK transports share authenticated durable draft state', async () => {
  const dir = await mkdtemp(join(tmpdir(), 'big-pi-'));
  const port = 23000 + Math.floor(Math.random() * 10000), token = 'local-test-token';
  const base = `http://127.0.0.1:${port}`;
  const env = { ...process.env, BIG_PI_PORT: String(port), BIG_PI_TOKEN: token, BIG_PI_DATA_DIR: dir };
  let child, client, stdio, sourceId;
  const start = async () => {
    child = spawn(resolve('node_modules/.bin/nbb'), ['-cp', 'src', '-m', 'big-pi.optimizely.main'], { env, stdio: 'ignore' });
    for (let i = 0; i < 100; i++) { try { if ((await fetch(`${base}/healthz`)).ok) return; } catch {} await pause(100); }
    assert.fail('server did not start');
  };
  const stop = async () => { if (child && child.exitCode === null) { child.kill('SIGTERM'); await new Promise(r => child.once('exit', r)); } };
  const request = (path, options = {}) => fetch(base + path, { ...options, headers: { authorization: `Bearer ${token}`, ...options.headers } });
  try {
    await start();
    assert.equal((await fetch(`${base}/v1/content`)).status, 401);
    assert.equal((await fetch(`${base}/mcp`, { method: 'POST' })).status, 401);
    assert.equal((await request('/v1/content', { headers: { origin: 'https://evil.example' } })).status, 403);
    assert.equal(await new Promise((resolve, reject) => { http.get(`${base}/v1/content`, { headers: { host: 'evil.example', authorization: `Bearer ${token}` } }, r => { r.resume(); resolve(r.statusCode); }).on('error', reject); }), 403);
    assert.equal((await request('/v1/unknown')).status, 501);
    assert.equal((await request('/v1/content', { method: 'POST', headers: { 'content-type': 'application/json' }, body: '{' })).status, 400);
    let res = await request('/v1/content', { method: 'POST', headers: { 'content-type': 'application/json', prefer: 'return=representation' }, body: JSON.stringify({ key: 'sample', contentType: 'Article', initialVersion: { displayName: 'Hello', locale: 'en', properties: { title: 'Original' } } }) });
    assert.equal(res.status, 201); let etag = res.headers.get('etag');
    sourceId = res.headers.get('x-big-pi-source-id');
    assert.match(sourceId || '', /^[0-9a-f-]{36}$/, 'source store has a durable identity');
    assert.equal((await res.json()).status, 'draft');
    res = await request('/v1/content/sample/versions/1', { method: 'PATCH', headers: { 'content-type': 'application/merge-patch+json', 'if-match': '"stale"' }, body: '{"displayName":"Bad"}' });
    assert.equal(res.status, 412);
    client = new Client({ name: 'integration', version: '1' });
    await client.connect(new StreamableHTTPClientTransport(new URL(`${base}/mcp`), { requestInit: { headers: { authorization: `Bearer ${token}` } } }));
    const listed = await client.listTools();
    assert.ok(listed.tools.some(t => t.name === 'big_pi_patch_draft'));
    assert.ok(listed.tools.find(t => t.name === 'big_pi_import_source').inputSchema.properties['expected-revision']);
    const read = await client.callTool({ name: 'big_pi_get_version', arguments: { id: 'sample', version: '1' } });
    assert.equal(JSON.parse(read.content[0].text).body.displayName, 'Hello');
    const patch = await client.callTool({ name: 'big_pi_patch_draft', arguments: { id: 'sample', version: '1', etag, body: { displayName: 'Edited' } } });
    assert.equal(patch.isError, undefined);
    const invalid = await client.callTool({ name: 'big_pi_patch_draft', arguments: { id: 'sample', version: '1', etag, body: { status: 'published' } } });
    assert.equal(invalid.isError, true);
    stdio = new Client({ name: 'stdio-test', version: '1' });
    await stdio.connect(new StdioClientTransport({ command: resolve('node_modules/.bin/nbb'), args: ['-cp', 'src', '-m', 'big-pi.optimizely.main', '--stdio'], env: { ...env, BIG_PI_BASE_URL: base }, stderr: 'pipe' }));
    assert.ok((await stdio.listTools()).tools.some(t => t.name === 'big_pi_list_content'));
    assert.equal(JSON.parse((await stdio.callTool({ name: 'big_pi_get_version', arguments: { id: 'sample', version: '1' } })).content[0].text).body.displayName, 'Edited');
    await stdio.close(); stdio = null; await client.close(); client = null;
    const competing = spawn(resolve('node_modules/.bin/nbb'), ['-cp', 'src', '-m', 'big-pi.optimizely.main'], { env: { ...env, BIG_PI_PORT: String(port + 1) }, stdio: 'ignore' });
    assert.notEqual(await new Promise(r => competing.once('exit', r)), 0, 'second writer must fail');
    await stop(); await start();
    res = await request('/v1/content/sample/versions/1');
    assert.equal(res.headers.get('x-big-pi-source-id'), sourceId, 'identity survives restart');
    assert.equal((await res.json()).displayName, 'Edited');
    await stop();
    await writeFile(join(dir, 'state.edn'), '{:generation 0}');
    const corrupt = spawn(resolve('node_modules/.bin/nbb'), ['-cp', 'src', '-m', 'big-pi.optimizely.main'], { env, stdio: 'ignore' });
    assert.notEqual(await new Promise(r => corrupt.once('exit', r)), 0, 'corrupt state must fail startup');
  } finally { await stdio?.close(); await client?.close(); await stop(); await rm(dir, { recursive: true, force: true }); }
});

test('configured bridge imports an exact source and exports only a draft to the local emulator', async () => {
  const calls = [], revisions = []; let doc, client;
  const fixture = http.createServer(async (req, res) => {
    calls.push([req.method, req.url, req.headers.authorization]);
    let raw = ''; for await (const chunk of req) raw += chunk;
    res.setHeader('content-type', 'application/json');
    if (req.method === 'POST' && req.url === '/api/cms/documents') {
      doc = { ...JSON.parse(raw), doc_id: 'doc1', revision: 'rev1', revision_heads: ['rev1'], conflicted: false }; revisions.push(doc);
      res.end(JSON.stringify(doc));
    } else if (req.method === 'PATCH' && req.url === '/api/cms/documents/doc1') {
      const body = JSON.parse(raw);
      if (JSON.stringify(body.parents) !== JSON.stringify([doc.revision])) { res.statusCode = 409; res.end('{}'); return; }
      const revision = `rev${revisions.length + 1}`;
      doc = { ...body, doc_id: 'doc1', revision, revision_heads: [revision], conflicted: false }; revisions.push(doc);
      res.end(JSON.stringify(doc));
    } else if (req.method === 'GET' && req.url === '/api/cms/documents/doc1/history') {
      res.end(JSON.stringify({ document: doc, revisions }));
    } else { res.statusCode = 404; res.end('{}'); }
  });
  await new Promise(r => fixture.listen(0, '127.0.0.1', r));
  const dir = await mkdtemp(join(tmpdir(), 'big-pi-bridge-'));
  const port = 33000 + Math.floor(Math.random() * 10000), base = `http://127.0.0.1:${port}`;
  const env = { ...process.env, BIG_PI_PORT: String(port), BIG_PI_TOKEN: 'local', BIG_PI_DATA_DIR: dir, BIG_PI_KNOXX_URL: `http://127.0.0.1:${fixture.address().port}`, BIG_PI_KNOXX_TOKEN: 'fixture-token' };
  const child = spawn(resolve('node_modules/.bin/nbb'), ['-cp', 'src', '-m', 'big-pi.optimizely.main'], { env, stdio: 'ignore' });
  const post = (path, body) => fetch(base + path, { method: 'POST', headers: { authorization: 'Bearer local', 'content-type': 'application/json', prefer: 'return=representation' }, body: JSON.stringify(body) });
  try {
    for (let i = 0; i < 100; i++) { try { if ((await fetch(`${base}/healthz`)).ok) break; } catch {} await pause(100); }
    const created = await post('/v1/content', { key: 'source', contentType: 'Article', initialVersion: { displayName: 'Source', locale: 'en', properties: { title: 'Hello' } } });
    assert.equal(created.status, 201);
    const sourceId = created.headers.get('x-big-pi-source-id');
    let response = await post('/big-pi/import', { key: 'source', version: '1', field: 'title', 'source-system': 'fixture' });
    assert.equal(response.status, 200, await response.clone().text());
    const imported = await response.json();
    assert.equal(imported.document.doc_id, 'doc1');
    assert.equal(imported.document.metadata.optimizely['source-id'], sourceId);
    client = new Client({ name: 'bridge-update', version: '1' });
    await client.connect(new StreamableHTTPClientTransport(new URL(`${base}/mcp`), { requestInit: { headers: { authorization: 'Bearer local' } } }));
    const input = { key: 'source', version: '1', field: 'title', 'source-system': 'fixture', 'document-id': 'doc1' };
    const stale = await client.callTool({ name: 'big_pi_import_source', arguments: { ...input, 'expected-revision': 'old' } });
    assert.equal(stale.isError, true);
    assert.equal(JSON.parse(stale.content[0].text).status, 409);
    assert.equal(doc.revision, 'rev1');
    const updated = await client.callTool({ name: 'big_pi_import_source', arguments: { ...input, 'expected-revision': 'rev1' } });
    assert.equal(updated.isError, undefined);
    assert.equal(JSON.parse(updated.content[0].text).body.document.revision, 'rev2');
    const translation = await fetch(`${env.BIG_PI_KNOXX_URL}/api/cms/documents/doc1`, {
      method: 'PATCH', headers: { authorization: 'Bearer fixture-token', 'content-type': 'application/json' },
      body: JSON.stringify({ title: doc.title, content: 'Hallo', visibility: 'internal', metadata: { ...doc.metadata, annotation: 'Internal reviewer note' }, parents: ['rev2'] }),
    });
    assert.equal(translation.status, 200);
    response = await post('/big-pi/export', { 'document-id': 'doc1', revision: 'rev3', 'target-locale': 'de', field: 'title' });
    assert.equal(response.status, 200, await response.clone().text());
    const result = await response.json(); assert.equal(result.vendor.status, 'draft'); assert.equal(result.vendor.locale, 'de');
    assert.equal(result.vendor.properties.title, 'Hallo');
    assert.equal(JSON.stringify(result.vendor).includes('Internal reviewer note'), false);
    assert.equal(calls.filter(([method]) => method === 'PATCH').length, 2);
    assert.ok(calls.every(([, , auth]) => auth === 'Bearer fixture-token'));
  } finally {
    await client?.close();
    if (child.exitCode === null) { child.kill('SIGTERM'); await new Promise(r => child.once('exit', r)); }
    await new Promise(r => fixture.close(r)); await rm(dir, { recursive: true, force: true });
  }
});
