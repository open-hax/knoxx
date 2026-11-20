#!/usr/bin/env bash
set -euo pipefail
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
: "${KNOXX_WS_PLAYWRIGHT_ROOT:?Set an absolute path to the existing Playwright-core 1.58.2 runtime}"
: "${KNOXX_WS_EVIDENCE_DIR:?Set a new absolute evidence directory}"
: "${KNOXX_WS_EXPECTED_HEAD:?Bind this verification to the expected checkout commit}"
NODE_BIN="${KNOXX_WS_NODE_BIN:-node}"
SCRATCH_DIR="$(mktemp -d "${TMPDIR:-/tmp}/knoxx-ws-fixture.XXXXXX")"
OWNED_NODE_PID=""
cleanup() {
  if [[ -n "$OWNED_NODE_PID" ]]; then
    kill -TERM "$OWNED_NODE_PID" 2>/dev/null || true
    wait "$OWNED_NODE_PID" 2>/dev/null || true
  fi
  rm -rf "$SCRATCH_DIR"
}
trap cleanup EXIT INT TERM
export KNOXX_WS_REPO_ROOT="$REPO_ROOT"
cat > "$SCRATCH_DIR/verify.mjs" <<'JS'
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';
import { execFileSync } from 'node:child_process';
import crypto from 'node:crypto';

const root = process.env.KNOXX_WS_REPO_ROOT;
const evidence = process.env.KNOXX_WS_EVIDENCE_DIR;
const runtimeRoot = process.env.KNOXX_WS_PLAYWRIGHT_ROOT;
for (const value of [root, evidence, runtimeRoot]) assert(path.isAbsolute(value), 'Paths must be absolute');
assert(!fs.existsSync(evidence), 'Evidence directory must be new; prior captures are immutable');
fs.mkdirSync(evidence, { recursive: true });
const hash = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
const git = (...args) => execFileSync('git', ['-C', root, ...args], { env: { ...process.env, GIT_OPTIONAL_LOCKS: '0' }, encoding: 'utf8' }).trim();
const head = git('rev-parse', 'HEAD');
assert.equal(head, process.env.KNOXX_WS_EXPECTED_HEAD, 'Checkout head changed');
const sourcePaths = ['frontend/src/cljs/knoxx/frontend/core.cljs', 'frontend/src/cljs/knoxx/frontend/lib/ws.cljs',
  'frontend/src/cljs/knoxx/frontend/law/ws.cljc', 'frontend/src/cljs/knoxx/frontend/domain/ws.cljc',
  'frontend/src/lib/api/core.ts', 'frontend/src/components/chat-page/chat-runtime-effects.ts', 'frontend/src/components/SidebarOpsStatus.tsx'];
assert.equal(git('diff', 'HEAD', '--', 'frontend/src', 'shared/src'), '', 'Runtime source must match the bound commit');
assert.equal(git('ls-files', '--others', '--exclude-standard', '--', 'frontend/src', 'shared/src'), '', 'Untracked runtime source cannot be qualified');
assert(!fs.existsSync(path.join(root, 'frontend/src/lib/ws.ts')), 'Legacy transport must be retired');
const runtimePackage = path.join(runtimeRoot, 'package.json');
assert.equal(JSON.parse(fs.readFileSync(runtimePackage)).version, '1.58.2');
assert.equal(hash(runtimePackage), '16a25dbd7368a87af8b58a45c6216b9cec5a91a72cf704533344701997ebde22', 'Pinned runtime package identity changed');
const require = createRequire(path.join(root, 'frontend/e2e/package.json'));
const support = require(path.join(root, 'frontend/e2e/knoxx_e2e_support.cjs'));
const { chromium } = await import(pathToFileURL(path.join(runtimeRoot, 'index.mjs')).href);
const outputs = ['frontend/dist/cljs/app.js', 'frontend/dist/bridge/knoxx-app-bridge.es.js',
  'frontend/dist/bridge/knoxx-frontend-bridge.es.js', 'frontend/dist/app.css'];
// The emitted source maps bind the current originals to the optimized outputs.
const flattenMaps = map => map.sections ? map.sections.flatMap(section => flattenMaps(section.map)) : [map];
const maps = ['frontend/dist/cljs/app.js.map', 'frontend/dist/bridge/knoxx-app-bridge.es.js.map'];
const mappedSources = maps.flatMap(file => flattenMaps(JSON.parse(fs.readFileSync(path.join(root, file))))
  .flatMap(map => (map.sources || []).map((name, index) => ({ name, content: map.sourcesContent?.[index] }))));
for (const file of sourcePaths) {
  const suffix = file.replace(/^frontend\/src\/(cljs\/)?/, '');
  assert(mappedSources.some(source => source.name.endsWith(suffix)
    && source.content === fs.readFileSync(path.join(root, file), 'utf8')), `Optimized build source mismatch: ${file}`);
}
const proof = { startedAt: new Date().toISOString(), head, root, diff: git('diff', '--stat'), status: git('status', '--porcelain=v1'),
  verifier: ['scripts/verify-frontend-ws-migration.sh', 'docs/verification/frontend-ws-migration.md'].map(file => ({ file, sha256: hash(path.join(root, file)) })),
  runtime: { path: runtimeRoot, version: '1.58.2', packageSHA256: hash(runtimePackage) },
  source: sourcePaths.map(file => ({ file, sha256: hash(path.join(root, file)) })),
  build: [...outputs, ...maps].map(file => ({ file, sha256: hash(path.join(root, file)) })),
  network: [], sockets: [], controls: [], screenshots: [], production: false, providerCalls: false, cleanup: false };
let fixture, browser;
const save = () => fs.writeFileSync(path.join(evidence, 'proof.json'), JSON.stringify(proof, null, 2) + '\n');
const cleanup = async () => { if (fixture) { const owned = fixture; fixture = null; await owned.close(); proof.cleanup = true; } save(); };
for (const signal of ['SIGTERM', 'SIGINT']) process.once(signal, () => { void cleanup().finally(() => process.exit(130)); });
const wait = async (predicate, message, timeout = 15000) => {
  const until = Date.now() + timeout;
  while (Date.now() < until) { if (await predicate()) return; await new Promise(resolve => setTimeout(resolve, 25)); }
  throw new Error(message);
};
try {
  fixture = await support.createFixture();
  proof.fixtureOrigin = fixture.baseUrl;
  const baseUrl = fixture.baseUrl;
  browser = await chromium.connectOverCDP(fixture.browser.wsEndpoint());
  const context = browser.contexts()[0];
  const marker = crypto.randomUUID();
  await fixture.page.evaluate(value => { window.__knoxxOwnedFixturePage = value; }, marker);
  let page;
  for (const candidate of context.pages()) {
    if (await candidate.evaluate(value => window.__knoxxOwnedFixturePage === value, marker)) page = candidate;
  }
  assert(page, 'CDP page must be the exact Puppeteer fixture page');
  await page.setViewportSize({ width: 1440, height: 1000 });
  await page.emulateMedia({ colorScheme: 'dark' });
  const sockets = [];
  await page.routeWebSocket('**/ws/stream*', route => {
    const url = new URL(route.url());
    assert([new URL(baseUrl).host, 'fixture.invalid'].includes(url.host), 'Unexpected WebSocket destination');
    const row = { url: route.url(), openedAt: new Date().toISOString(), sent: [], packets: [], closed: false };
    proof.sockets.push(row); sockets.push({ route, row });
    route.onMessage(message => row.sent.push(String(message)));
    route.onClose((code, reason) => { row.closed = true; row.closeCode = code; row.closeReason = reason; route.close({ code: 1000 }); });
  });
  // Route is installed before navigation; no real WS server is contacted.
  await page.route('**/*', async route => {
    const request = route.request(), url = new URL(request.url());
    if (url.protocol === 'data:') return route.continue();
    proof.network.push({ url: url.href, method: request.method() });
    assert.equal(url.origin, baseUrl, 'External HTTP/provider request refused');
    assert.equal(request.method(), 'GET', 'This fixture never sends a chat/model request');
    let body;
    if (url.pathname === '/api/knoxx/session/status') body = { session_id: 'ws-fixture-session', conversation_id: 'ws-fixture-conversation', status: 'running', has_active_stream: true, can_send: false };
    if (url.pathname === '/api/runs/ws-fixture-run') body = { run_id: 'ws-fixture-run', session_id: 'ws-fixture-session', conversation_id: 'ws-fixture-conversation', status: 'running', answer: null, settings: {}, resources: {} };
    if (url.pathname === '/api/knoxx/run/ws-fixture-run/events') body = { run_id: 'ws-fixture-run', events: [], count: 0 };
    if (url.pathname === '/api/documents') body = { documents: [], files: [] };
    if (url.pathname === '/api/documents/ingestion-progress') body = { active: false, progress: null };
    if (body) return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) });
    return route.continue();
  });
  await page.addInitScript(() => {
    localStorage.setItem('knoxx_chat_page_session_id', 'ws-fixture-session');
    localStorage.setItem('knoxx_chat_page_session_state:ws-fixture-session', JSON.stringify({
      sessionId: 'ws-fixture-session', conversationId: 'ws-fixture-conversation', selectedModel: 'gemma4:31b',
      messages: [{ id: 'ws-fixture-user', role: 'user', content: 'Local WebSocket verification: repeated deltas and correction' },
        { id: 'ws-fixture-assistant', role: 'assistant', content: 'Seed ', status: 'streaming', runId: 'ws-fixture-run' }],
      runtimeEvents: [], isSending: false,
    }));
  });
  await page.goto(fixture.baseUrl, { waitUntil: 'domcontentloaded' });
  await page.waitForFunction(() => typeof window.knoxx?.frontend?.lib?.ws?.connect_stream_js === 'function');
  const send = (socket, channel, payload) => { const packet = { channel, payload }; socket.row.packets.push(packet); socket.route.send(JSON.stringify(packet)); };
  await wait(() => sockets.some(socket => new URL(socket.row.url).searchParams.get('session_id') === 'ws-fixture-session'), 'Active chat did not connect');
  const chat = sockets.find(socket => new URL(socket.row.url).searchParams.get('session_id') === 'ws-fixture-session');
  const chatContent = () => page.evaluate(() => JSON.parse(localStorage.getItem('knoxx_chat_page_session_state:ws-fixture-session')).messages.find(message => message.id === 'ws-fixture-assistant')?.content);
  for (const token of ['ha', 'ha', '\n\n', 'WS']) send(chat, 'tokens', { token, run_id: 'ws-fixture-run', kind: 'assistant_message' });
  await wait(async () => await chatContent() === 'Seed haha\n\nWS', 'Active chat did not preserve exact repeated/whitespace deltas');
  await page.getByText('Seed haha', { exact: false }).first().scrollIntoViewIfNeeded();
  proof.controls.push({ name: 'active-chat-literal-deltas', actual: await chatContent() });
  await page.screenshot({ path: path.join(evidence, 'chat-deltas.png'), fullPage: true }); proof.screenshots.push('chat-deltas.png');
  send(chat, 'tokens', { token: '🌱 corrected output', run_id: 'ws-fixture-run', kind: 'assistant_message', operation: 'replace', offset: 2 });
  await wait(async () => await chatContent() === '🌱 corrected output', 'Correction was appended or lost');
  await page.getByText('🌱 corrected output', { exact: false }).first().scrollIntoViewIfNeeded();
  proof.controls.push({ name: 'active-chat-correction', actual: await chatContent() });
  await page.screenshot({ path: path.join(evidence, 'chat-correction.png'), fullPage: true }); proof.screenshots.push('chat-correction.png');

  await page.evaluate(() => {
    window.__wsProof = { tokens: [], console: [], statuses: [] };
    window.__wsProbe = window.knoxx.frontend.lib.ws.connect_stream_js({
      onToken: (token, metadata) => window.__wsProof.tokens.push({ token, metadata, runIdUndefined: metadata.runId === undefined }),
      onConsole: line => window.__wsProof.console.push(line), onStatus: status => window.__wsProof.statuses.push(status),
    }, 'adapter-probe', 'initial', '');
  });
  await wait(() => sockets.some(socket => new URL(socket.row.url).searchParams.get('session_id') === 'adapter-probe'), 'Named adapter did not connect');
  const probe = sockets.find(socket => new URL(socket.row.url).searchParams.get('session_id') === 'adapter-probe');
  for (const token of ['ha', 'ha', '\n\n']) send(probe, 'tokens', { token });
  send(probe, 'tokens', { token: '🌱 replaced', kind: 'reasoning', operation: 'replace', offset: 2 });
  send(probe, 'tokens', { token: '', kind: 'assistant_message', operation: 'replace', offset: 0 });
  const bad = [{ offset: -1 }, { offset: .5 }, { offset: '0' }, { offset: null }, { offset: 4 }, { offset: Number.MAX_SAFE_INTEGER + 1 },
    { offset: undefined }, { kind: 'tool_call' }, { kind: undefined }, { token: 123 }, { operation: 'unknown' }, { operation: null }, { operation: undefined, offset: 0 }];
  for (const override of bad) send(probe, 'tokens', { token: 'new', kind: 'assistant_message', operation: 'replace', offset: 0, ...override });
  await wait(async () => (await page.evaluate(() => window.__wsProof.console.length)) === 13, 'Malformed correction controls did not execute');
  const decoded = await page.evaluate(() => window.__wsProof);
  assert.deepEqual(decoded.tokens.map(record => record.token), ['ha', 'ha', '\n\n', '🌱 replaced', '']);
  assert(decoded.tokens.every(record => record.runIdUndefined));
  assert(decoded.console.every(line => line === 'Malformed token packet'));
  assert.equal(decoded.tokens[3].metadata.offset, 2); assert.equal(decoded.tokens[3].metadata.kind, 'reasoning');
  proof.controls.push({ name: 'optimized-named-adapter-packet-matrix', actual: decoded });
  await page.evaluate(() => { window.__wsProbe.setConversationId(null); window.__wsProbe.setConversationId(null); });
  await wait(() => probe.row.sent.length === 1, 'Conversation clear was not sent exactly once');
  assert.deepEqual(JSON.parse(probe.row.sent[0]), { type: 'set_conversation', conversation_id: null });
  probe.row.fixtureClosedAt = new Date().toISOString(); probe.route.close({ code: 1000 });
  await wait(() => sockets.filter(socket => new URL(socket.row.url).searchParams.get('session_id') === 'adapter-probe').length === 2, 'Real close did not schedule existing reconnect');
  const rebound = sockets.filter(socket => new URL(socket.row.url).searchParams.get('session_id') === 'adapter-probe')[1];
  assert.equal(new URL(rebound.row.url).searchParams.has('conversation_id'), false);
  await page.evaluate(() => window.__wsProbe.disconnect());
  await wait(() => rebound.row.closed, 'Adapter disconnect did not close its socket');
  await new Promise(resolve => setTimeout(resolve, 1300));
  assert.equal(sockets.filter(socket => new URL(socket.row.url).searchParams.get('session_id') === 'adapter-probe').length, 2);
  proof.controls.push({ name: 'conversation-clear-reconnect-and-disposal', sent: probe.row.sent, noReconnectAfterDispose: true });

  for (const base of ['https://fixture.invalid/old?previous=1', 'http://fixture.invalid/old?previous=1']) {
    await page.evaluate(base => { window.__baseProbe = window.knoxx.frontend.lib.ws.connect_stream_js({}, 's/ 1', 'c& 2', base); }, base);
    await wait(() => sockets.some(socket => socket.row.url.startsWith(base.startsWith('https') ? 'wss://fixture.invalid/' : 'ws://fixture.invalid/')), 'Configured API base did not reach the named adapter');
    const socket = sockets.findLast(socket => new URL(socket.row.url).host === 'fixture.invalid');
    const url = new URL(socket.row.url); assert.equal(url.pathname, '/ws/stream'); assert.equal(url.searchParams.get('session_id'), 's/ 1'); assert.equal(url.searchParams.get('conversation_id'), 'c& 2'); assert.equal(url.searchParams.has('previous'), false);
    await page.evaluate(() => window.__baseProbe()); await wait(() => socket.row.closed, 'Callable cleanup did not close configured-base probe');
    proof.controls.push({ name: 'configured-api-base', url: url.href });
  }
  const timeOrigin = await page.evaluate(() => performance.timeOrigin);
  await page.getByRole('link', { name: 'Admin', exact: true }).first().click();
  await page.waitForURL('**/ops/**');
  assert.equal(await page.evaluate(() => performance.timeOrigin), timeOrigin, 'Ops transition must preserve the document and unmount through React');
  await page.getByText('System Usage', { exact: true }).waitFor();
  await wait(() => chat.row.closed, 'Active chat did not clean up on navigation');
  await wait(() => sockets.some(socket => !socket.row.closed && !new URL(socket.row.url).searchParams.has('session_id')), 'Active Ops status did not connect');
  const ops = sockets.findLast(socket => !socket.row.closed && !new URL(socket.row.url).searchParams.has('session_id'));
  send(ops, 'stats', { cpu_percent: 37.5, memory_percent: 42, gpu: [{ util_gpu: 12.5 }] });
  await page.getByText('37.5%', { exact: true }).waitFor(); await page.getByText('42.0%', { exact: true }).waitFor(); await page.getByText('12.5%', { exact: true }).waitFor();
  proof.controls.push({ name: 'active-ops-status', cpu: '37.5%', ram: '42.0%', gpu: '12.5%' });
  await page.screenshot({ path: path.join(evidence, 'ops-status.png'), fullPage: true }); proof.screenshots.push('ops-status.png');
  await page.getByRole('link', { name: 'Chat', exact: true }).first().click();
  await page.waitForURL(fixture.baseUrl + '/');
  assert.equal(await page.evaluate(() => performance.timeOrigin), timeOrigin, 'Chat transition must preserve the document and unmount through React');
  await wait(() => ops.row.closed, 'Ops status did not clean up on navigation');
  proof.controls.push({ name: 'active-chat-and-ops-navigation-cleanup', chatClosed: chat.row.closed, opsClosed: ops.row.closed });
  proof.console = fixture.consoleLines; proof.completedAt = new Date().toISOString(); proof.result = 'pass';
} catch (error) { proof.result = 'fail'; proof.error = String(error.stack || error); save(); throw error; }
finally { await cleanup(); }
console.log(JSON.stringify({ evidence, head, result: proof.result, controls: proof.controls.length, cleanup: proof.cleanup }));
JS
"$NODE_BIN" "$SCRATCH_DIR/verify.mjs" &
OWNED_NODE_PID=$!
set +e
wait "$OWNED_NODE_PID"
RESULT=$?
set -e
OWNED_NODE_PID=""
exit "$RESULT"
