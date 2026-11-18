import Fastify from 'fastify';
import { randomBytes, randomUUID, timingSafeEqual, createHash } from 'node:crypto';
import { mkdirSync, openSync, closeSync, readFileSync, writeFileSync, fsyncSync, renameSync, unlinkSync, existsSync } from 'node:fs';
import { resolve, join } from 'node:path';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';
import { z } from 'zod';

const urlPath = s => encodeURIComponent(s);
const schemas = {
  big_pi_list_content: { query: z.record(z.string()).optional() },
  big_pi_get_version: { id: z.string().min(1), version: z.string().min(1) },
  big_pi_create_content: { body: z.record(z.unknown()) },
  big_pi_create_draft: { id: z.string().min(1), body: z.record(z.unknown()).optional() },
  big_pi_patch_draft: { id: z.string().min(1), version: z.string().min(1), etag: z.string().min(1), body: z.record(z.unknown()) },
  big_pi_capabilities: {},
  big_pi_import_source: { key: z.string(), version: z.string(), field: z.string(), 'source-system': z.string(), 'document-id': z.string().optional(), 'expected-revision': z.string().optional() },
  big_pi_export_draft: { 'document-id': z.string(), revision: z.string(), 'target-locale': z.string(), field: z.string() }
};
function toolRequest(name, a) {
  switch (name) {
    case 'big_pi_list_content': return { method: 'GET', path: '/v1/content', query: a.query || {} };
    case 'big_pi_get_version': return { method: 'GET', path: `/v1/content/${urlPath(a.id)}/versions/${urlPath(a.version)}` };
    case 'big_pi_create_content': return { method: 'POST', path: '/v1/content', body: a.body };
    case 'big_pi_create_draft': return { method: 'POST', path: `/v1/content/${urlPath(a.id)}/versions`, body: a.body || {} };
    case 'big_pi_patch_draft': return { method: 'PATCH', path: `/v1/content/${urlPath(a.id)}/versions/${urlPath(a.version)}`, headers: { 'if-match': a.etag }, body: a.body };
    case 'big_pi_import_source': return { method: 'POST', path: '/big-pi/import', body: a };
    case 'big_pi_export_draft': return { method: 'POST', path: '/big-pi/export', body: a };
    case 'big_pi_capabilities': return { method: 'GET', path: '/v1/capabilities' };
  }
}
function createServer(call) {
  const server = new McpServer({ name: 'big-pi-local', version: '0.1.0' });
  for (const [name, inputSchema] of Object.entries(schemas)) {
    server.registerTool(name, { description: 'Big Pi local CMS sandbox operation', inputSchema }, async args => {
      try {
        const result = await call(name, args);
        return { content: [{ type: 'text', text: JSON.stringify(result) }], ...(result.status >= 400 ? { isError: true } : {}) };
      } catch { return { content: [{ type: 'text', text: 'Local operation failed' }], isError: true }; }
    });
  }
  return server;
}
function localAuthority(value) {
  return /^(localhost|127\.0\.0\.1|\[::1\])(?::[0-9]{1,5})?$/i.test(value);
}
function localOrigin(value) {
  if (!value) return true;
  try { const u = new URL(value); return ['http:', 'https:'].includes(u.protocol) && ['localhost', '127.0.0.1', '[::1]'].includes(u.hostname) && !u.username && !u.password; } catch { return false; }
}
function sameToken(auth, token) {
  const expected = Buffer.from(`Bearer ${token}`); const actual = Buffer.from(auth || '');
  return expected.length === actual.length && timingSafeEqual(expected, actual);
}
function queryObject(query) {
  // Fastify uses a query object with a custom prototype. Materialize a plain
  // object for CLJS, and normalize form/explode array filters of length one.
  return Object.fromEntries(Object.entries(query || {}).map(([key, value]) =>
    [key,
      ['locales', 'statuses'].includes(key) && typeof value === 'string' ? [value] : value]));
}
function stateStore(dir, initial, decode) {
  mkdirSync(dir, { recursive: true, mode: 0o700 });
  const lock = join(dir, 'writer.lock'); const fd = openSync(lock, 'wx', 0o600);
  writeFileSync(fd, `${process.pid}\n`); fsyncSync(fd);
  const snapshot = join(dir, 'state.edn');
  let value, identity;
  try {
    const identityPath = join(dir, 'source-id');
    if (existsSync(identityPath)) identity = readFileSync(identityPath, 'utf8').trim();
    else {
      identity = randomUUID();
      const identityFd = openSync(identityPath, 'wx', 0o600);
      try { writeFileSync(identityFd, `${identity}\n`); fsyncSync(identityFd); } finally { closeSync(identityFd); }
      const directoryFd = openSync(dir, 'r'); try { fsyncSync(directoryFd); } finally { closeSync(directoryFd); }
    }
    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(identity)) throw new Error('Invalid source identity');
    if (existsSync(snapshot)) {
      const text = readFileSync(snapshot, 'utf8'); const split = text.indexOf('\n');
      const digest = text.slice(0, split), body = text.slice(split + 1);
      if (split !== 64 || createHash('sha256').update(body).digest('hex') !== digest) throw new Error('Snapshot checksum mismatch');
      value = decode(body);
    } else value = initial();
  }
  catch (e) { closeSync(fd); unlinkSync(lock); throw new Error(`Cannot read state: ${e.message}`); }
  return {
    value, identity,
    save(text) {
      const tmp = join(dir, `state.${randomUUID()}.tmp`); const out = openSync(tmp, 'wx', 0o600);
      try { writeFileSync(out, `${createHash('sha256').update(text).digest('hex')}\n${text}`); fsyncSync(out); } finally { closeSync(out); }
      renameSync(tmp, snapshot); const d = openSync(dir, 'r'); try { fsyncSync(d); } finally { closeSync(d); }
    },
    close() { closeSync(fd); unlinkSync(lock); }
  };
}
export async function knoxxFetch(request) {
  const configured = process.env.BIG_PI_KNOXX_URL, token = process.env.BIG_PI_KNOXX_TOKEN;
  if (!configured || !token) throw new Error('Knoxx not configured');
  const base = new URL(configured);
  if (base.username || base.password || base.search || base.hash || base.pathname !== '/' ||
      !(base.protocol === 'https:' || (base.protocol === 'http:' && ['localhost', '127.0.0.1', '[::1]'].includes(base.hostname)))) throw new Error('Invalid Knoxx configuration');
  if (!/^\/api\/cms\/documents(?:\/[A-Za-z0-9_-]+(?:\/history)?)?$/.test(request.path)) throw new Error('Invalid Knoxx path');
  const response = await fetch(new URL(request.path, base), { method: request.method.toUpperCase(), redirect: 'error',
    headers: { ...request.headers, authorization: `Bearer ${token}`, ...(request.body === undefined ? {} : { 'content-type': 'application/json' }) },
    ...(request.body === undefined ? {} : { body: JSON.stringify(request.body) }) });
  let body = null; try { body = await response.json(); } catch { /* Invalid upstream response is rejected by coordinator. */ }
  return { status: response.status, headers: Object.fromEntries(response.headers.entries()), body };
}
export async function startHttp(initial, decode, dispatch, bridge) {
  const token = process.env.BIG_PI_TOKEN || randomBytes(32).toString('hex');
  const store = stateStore(resolve(process.env.BIG_PI_DATA_DIR || '.local-data'), initial, decode);
  const app = Fastify({ logger: false, bodyLimit: 1024 * 1024 });
  app.addContentTypeParser('application/merge-patch+json', { parseAs: 'string' }, (_req, body, done) => {
    try { done(null, JSON.parse(body)); } catch (error) { error.statusCode = 400; done(error); }
  });
  let failed = false;
  const call = async input => {
    const request = { ...input, query: queryObject(input.query) };
    if (failed || store.failed) return { status: 503, headers: {}, body: { error: 'State unavailable; restart required' } };
    if (['/big-pi/import', '/big-pi/export'].includes(request.path)) {
      if (request.method !== 'POST') return { status: 405, headers: {}, body: { error: 'Method not allowed' } };
      if (!process.env.BIG_PI_KNOXX_URL || !process.env.BIG_PI_KNOXX_TOKEN) return { status: 501, headers: {}, body: { error: 'Knoxx bridge not configured' } };
      return bridge(store, request);
    }
    try { return dispatch(store, request); } catch { failed = true; return { status: 503, headers: {}, body: { error: 'State unavailable; restart required' } }; }
  };
  app.addHook('onSend', async (_req, reply, payload) => { reply.header('cache-control', 'no-store'); reply.header('content-security-policy', "default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'"); return payload; });
  app.addHook('onRequest', async (req, reply) => {
    if (!localAuthority(req.headers.host || '') || !localOrigin(req.headers.origin)) return reply.code(403).send({ error: 'Forbidden origin or host' });
    if (req.url.split('?')[0] !== '/healthz' && !['/', '/workbench.js', '/workbench.css'].includes(req.url.split('?')[0]) && !sameToken(req.headers.authorization, token)) return reply.code(401).send({ error: 'Unauthorized' });
  });
  app.get('/healthz', async (_req, reply) => {
    if (failed || store.failed) return reply.code(503).send({ status: 'unavailable' });
    return { status: 'ok' };
  });
  app.get('/', async (_req, reply) => {
    try { return reply.type('text/html').send(readFileSync(resolve('public/index.html'))); } catch { return reply.code(404).send('Workbench unavailable'); }
  });
  for (const name of ['workbench.js', 'workbench.css']) {
    app.get(`/${name}`, async (_req, reply) => {
      try { return reply.type(name.endsWith('.js') ? 'text/javascript' : 'text/css').send(readFileSync(resolve('public', name))); }
      catch { return reply.code(404).send(); }
    });
  }
  app.all('/mcp', async (req, reply) => {
    const server = createServer((name, args) => call(toolRequest(name, args)));
    const transport = new StreamableHTTPServerTransport({ sessionIdGenerator: undefined, enableJsonResponse: true });
    await server.connect(transport); reply.raw.setHeader('cache-control', 'no-store'); reply.hijack();
    reply.raw.on('close', () => { transport.close(); server.close(); });
    await transport.handleRequest(req.raw, reply.raw, req.body);
  });
  app.all('/*', async (req, reply) => {
    const path = req.url.split('?')[0];
    const response = await call({ method: req.method, path, query: req.query || {}, headers: req.headers, body: req.body });
    for (const [k, v] of Object.entries(response.headers || {})) reply.header(k, v);
    return reply.code(response.status).send(response.body);
  });
  let closing = false;
  const close = async () => { if (closing) return; closing = true; await app.close(); store.close(); };
  process.once('SIGTERM', () => { close().then(() => process.exit(0)); });
  process.once('SIGINT', () => { close().then(() => process.exit(0)); });
  try { await app.listen({ host: '127.0.0.1', port: Number(process.env.BIG_PI_PORT || 4175) }); }
  catch (e) { await close(); throw e; }
  console.error(`Big Pi listening at http://127.0.0.1:${process.env.BIG_PI_PORT || 4175}`);
  if (!process.env.BIG_PI_TOKEN) console.error(`Generated local bearer token: ${token}`);
}
export async function startStdio() {
  const token = process.env.BIG_PI_TOKEN;
  if (!token) throw new Error('BIG_PI_TOKEN is required for stdio');
  const base = new URL(process.env.BIG_PI_BASE_URL || 'http://127.0.0.1:4175');
  if (base.protocol !== 'http:' || !['localhost', '127.0.0.1', '[::1]'].includes(base.hostname) || base.username || base.password) throw new Error('BIG_PI_BASE_URL must be loopback HTTP');
  const client = new Client({ name: 'big-pi-stdio-bridge', version: '0.1.0' });
  await client.connect(new StreamableHTTPClientTransport(new URL('/mcp', base), { requestInit: { headers: { authorization: `Bearer ${token}` } } }));
  const server = createServer(async (name, args) => {
    const result = await client.callTool({ name, arguments: args });
    const text = result.content?.find(x => x.type === 'text')?.text;
    if (result.isError) return { status: 400, error: text };
    return JSON.parse(text);
  });
  await server.connect(new StdioServerTransport());
}
