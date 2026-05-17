'use strict';

const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');
const { URL } = require('node:url');
const puppeteer = require('puppeteer-core');

const FRONTEND_ROOT = path.resolve(__dirname, '..');
const DIST_ROOT = path.join(FRONTEND_ROOT, 'dist');
const INDEX_PATH = path.join(FRONTEND_ROOT, 'index.html');

function json(res, status, body) {
  const payload = JSON.stringify(body);
  res.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'content-length': Buffer.byteLength(payload),
  });
  res.end(payload);
}

function text(res, status, body, contentType = 'text/plain; charset=utf-8') {
  res.writeHead(status, {
    'content-type': contentType,
    'content-length': Buffer.byteLength(body),
  });
  res.end(body);
}

function authContext() {
  return {
    actor: { id: 'system_admin' },
    user: {
      id: 'user-system-admin',
      email: 'system-admin@open-hax.local',
      displayName: 'Knoxx System Admin',
      status: 'active',
    },
    org: { id: 'org-open-hax', slug: 'open-hax', name: 'Open Hax', isPrimary: true },
    membership: { id: 'membership-system-admin', actorId: 'system_admin', status: 'active', isDefault: true },
    roleSlugs: ['system-admin'],
    permissions: [
      'platform.org.create',
      'org.event_agents.control',
      'agent.chat.use',
      'agent.memory.read',
      'agent.runs.read_all',
    ],
    isSystemAdmin: true,
    authProvider: 'e2e-header',
  };
}

const agents = [
  { id: 'knoxx_default', contractClass: 'agents', kind: 'agent', enabled: true, path: 'contracts/agents/knoxx_default.edn' },
  { id: 'fork_tales_creative_director', contractClass: 'agents', kind: 'agent', enabled: true, path: 'contracts/agents/fork_tales_creative_director.edn' },
];

const roles = [
  { id: 'knowledge_worker', contractClass: 'roles', kind: 'role', enabled: true },
  { id: 'system-admin', contractClass: 'roles', kind: 'role', enabled: true },
];

const triggers = [
  {
    id: 'fork_tales_creative_director_cron',
    contractClass: 'triggers',
    kind: 'trigger',
    enabled: true,
    trigger: {
      kind: 'cron',
      target: 'fork_tales_creative_director',
      schedule: '*/30 * * * *',
      source: { kind: 'cron' },
      filters: null,
      context: {},
    },
  },
  {
    id: 'daily_synthesis',
    contractClass: 'triggers',
    kind: 'trigger',
    enabled: true,
    trigger: {
      kind: 'cron',
      target: 'synthesis_pipeline',
      schedule: '0 9 * * *',
      source: { kind: 'cron' },
      filters: null,
      context: {},
    },
  },
];

const pipelines = [
  {
    id: 'synthesis_pipeline',
    contractClass: 'pipelines',
    kind: 'pipeline',
    enabled: true,
    pipeline: { steps: [{ id: 'write', contract: 'knoxx_default' }] },
  },
];

const memorySessions = [
  {
    project: 'knoxx-session',
    session: 'fork-run-history',
    title: 'Fork history',
    last_ts: '2026-05-14T23:00:00.000Z',
    event_count: 3,
    contract_id: 'fork_tales_creative_director',
    actor_id: 'fork_tales_creative_director',
    is_active: false,
    active_status: 'completed',
    has_active_stream: false,
    active_session_id: 'fork-session-history',
  },
  {
    project: 'knoxx-session',
    session: 'knoxx-default-history',
    title: 'Default history',
    last_ts: '2026-05-14T22:00:00.000Z',
    event_count: 1,
    contract_id: 'knoxx_default',
    actor_id: 'knoxx_default',
    is_active: false,
    active_status: 'completed',
    has_active_stream: false,
    active_session_id: 'default-session-history',
  },
];

const activeRuns = [
  {
    run_id: 'fork-active-run-id',
    session_id: 'fork-active-session-id',
    conversation_id: 'fork-run-active',
    status: 'running',
    model: 'gemma4:31b',
    created_at: '2026-05-14T23:10:00.000Z',
    updated_at: '2026-05-14T23:12:00.000Z',
    has_active_stream: true,
    active_turn_registered: true,
    latest_user_message: 'Advance Fork Tales lore and compose the next song.',
    agent_spec: {
      contractId: 'fork_tales_creative_director',
      subAgentId: 'fork_tales_creative_director',
      role: 'knowledge_worker',
    },
  },
];

function initialServerState() {
  return {
    savedAgentContracts: [],
    validationRequests: [],
    saveRequests: [],
  };
}

function contractIdFromEdnText(ednText, fallback = 'e2e_contract_agent') {
  const match = String(ednText || '').match(/:contract\/id\s+"([^"]+)"/);
  return match ? match[1] : fallback;
}

function contractFromEdnText(ednText, fallbackId = 'e2e_contract_agent') {
  const id = contractIdFromEdnText(ednText, fallbackId);
  return {
    'contract/id': id,
    'contract/kind': 'agent',
    'contract/version': 1,
    enabled: true,
    agent: { roles: ['knowledge_worker'], model: 'gemma4:31b', thinking: 'off' },
    prompts: { system: '', task: '' },
  };
}

function contractListItems(state, kind) {
  if (kind === 'agents') return [...agents, ...state.savedAgentContracts];
  if (kind === 'roles') return roles;
  if (kind === 'triggers') return triggers;
  if (kind === 'pipelines') return pipelines;
  return [...agents, ...state.savedAgentContracts, ...roles, ...triggers, ...pipelines];
}

function readRequestBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    req.on('data', (chunk) => chunks.push(chunk));
    req.on('error', reject);
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
  });
}

async function readJsonBody(req) {
  const body = await readRequestBody(req);
  if (!body.trim()) return {};
  return JSON.parse(body);
}

function contractCatalog() {
  return {
    actor_id: 'chat_primary',
    default_agent_contract: 'knoxx_default',
    default_role: 'knowledge_worker',
    actors: [
      { id: 'chat_primary', kind: 'agent', defaultAgent: 'knoxx_default', roleSlugs: ['knowledge_worker'] },
      { id: 'system_admin', kind: 'user', defaultAgent: 'knoxx_default', roleSlugs: ['system-admin'] },
    ],
    agents: [
      { id: 'knoxx_default', role: 'knowledge_worker', model: 'gemma4:31b', title: 'Knoxx Default' },
      { id: 'fork_tales_creative_director', role: 'knowledge_worker', model: 'gemma4:31b', title: 'Fork Tales Creative Director' },
    ],
  };
}

function toolCatalog() {
  return { role: 'knowledge_worker', agent: 'knoxx_default', actor: 'chat_primary', tools: [], catalog: [] };
}

function frontendConfig() {
  return {
    default_role: 'knowledge_worker',
    default_actor_id: 'chat_primary',
    default_agent_contract: 'knoxx_default',
    stt_enabled: false,
    tts_enabled: false,
    tts_default_voice_id: '',
  };
}

function proxxHealth() {
  return { reachable: true, configured: true, default_model: 'gemma4:31b' };
}

function proxxModels() {
  return { models: [{ id: 'gemma4:31b', name: 'Gemma 4 31B', provider: 'e2e', available: true }] };
}

function eventControl() {
  return {
    configured: true,
    tokenPreview: 'mock-token',
    availableRoles: ['knowledge_worker', 'system-admin'],
    availableSourceKinds: ['manual', 'cron', 'discord'],
    availableTriggerKinds: ['manual', 'cron', 'event'],
    control: { sources: {}, jobs: [] },
    runtime: { running: true, configured: true, sources: {}, jobs: [] },
  };
}

function rowsForSession(sessionId) {
  if (sessionId === 'fork-run-history') {
    return {
      session: sessionId,
      rows: [
        {
          id: 'row-user-1',
          ts: '2026-05-14T23:00:00.000Z',
          kind: 'knoxx.message',
          role: 'user',
          text: 'Compose the Fork Tales song from the audit session.',
          model: null,
        },
        {
          id: 'row-assistant-1',
          ts: '2026-05-14T23:01:00.000Z',
          kind: 'knoxx.message',
          role: 'assistant',
          text: 'Fork Tales audit transcript loaded into chat UI.',
          model: 'gemma4:31b',
        },
      ],
    };
  }
  return { session: sessionId, rows: [] };
}

function contentTypeFor(filePath) {
  if (filePath.endsWith('.html')) return 'text/html; charset=utf-8';
  if (filePath.endsWith('.js') || filePath.endsWith('.mjs')) return 'text/javascript; charset=utf-8';
  if (filePath.endsWith('.css')) return 'text/css; charset=utf-8';
  if (filePath.endsWith('.json')) return 'application/json; charset=utf-8';
  if (filePath.endsWith('.svg')) return 'image/svg+xml';
  return 'application/octet-stream';
}

function safeStaticPath(pathname) {
  const clean = decodeURIComponent(pathname).replace(/^\/+/, '');
  const distCandidate = path.resolve(DIST_ROOT, clean);
  if (distCandidate.startsWith(DIST_ROOT) && fs.existsSync(distCandidate) && fs.statSync(distCandidate).isFile()) {
    return distCandidate;
  }
  const rootCandidate = path.resolve(FRONTEND_ROOT, clean);
  if (rootCandidate.startsWith(FRONTEND_ROOT) && fs.existsSync(rootCandidate) && fs.statSync(rootCandidate).isFile()) {
    return rootCandidate;
  }
  return null;
}

async function handleApi(req, res, pathname, url, state) {
  if (pathname === '/api/auth/config') return json(res, 200, { githubEnabled: false, publicBaseUrl: '', loginUrl: null });
  if (pathname === '/api/auth/context') return json(res, 200, authContext());
  if (pathname === '/api/config') return json(res, 200, frontendConfig());
  if (pathname === '/api/proxx/health') return json(res, 200, proxxHealth());
  if (pathname === '/api/proxx/models') return json(res, 200, proxxModels());
  if (pathname === '/api/knoxx/agents/catalog') return json(res, 200, contractCatalog());
  if (pathname === '/api/tools/catalog') return json(res, 200, toolCatalog());
  if (pathname === '/api/admin/config/events') return json(res, 200, eventControl());
  if (pathname === '/api/admin/config/discord') return json(res, 200, { configured: true, tokenPreview: 'mock-token' });
  if (pathname === '/api/admin/agents/active') return json(res, 200, { runs: activeRuns, count: activeRuns.length });
  if (pathname === '/api/knoxx/agents/active') return json(res, 200, { runs: activeRuns, count: activeRuns.length });
  if (pathname === '/api/memory/sessions') return json(res, 200, { ok: true, rows: memorySessions, total: memorySessions.length, offset: 0, limit: Number(url.searchParams.get('limit') || 12), has_more: false });
  if (pathname.startsWith('/api/memory/sessions/')) {
    const sessionId = decodeURIComponent(pathname.slice('/api/memory/sessions/'.length));
    return json(res, 200, rowsForSession(sessionId));
  }
  if (pathname === '/api/admin/contracts') {
    const kind = url.searchParams.get('kind');
    return json(res, 200, { contracts: contractListItems(state, kind) });
  }
  if (pathname === '/api/admin/contracts/validate') {
    const body = await readJsonBody(req);
    state.validationRequests.push(body);
    return json(res, 200, {
      ok: true,
      errors: [],
      warnings: [],
      contract: contractFromEdnText(body.ednText),
    });
  }
  if (pathname.startsWith('/api/admin/contracts/')) {
    const id = decodeURIComponent(pathname.split('/').pop() || '');
    if (req.method === 'PUT') {
      const body = await readJsonBody(req);
      state.saveRequests.push({ id, body });
      const contract = contractFromEdnText(body.ednText, id);
      const item = { id, contractClass: body.kind || 'agents', kind: 'agent', enabled: true, path: `contracts/agents/${id}.edn` };
      state.savedAgentContracts = [item, ...state.savedAgentContracts.filter((existing) => existing.id !== id)];
      return json(res, 200, {
        ok: true,
        contractClass: body.kind || 'agents',
        contract,
        ednText: body.ednText,
        validation: { ok: true, errors: [], warnings: [] },
      });
    }
    return json(res, 200, {
      contractClass: url.searchParams.get('kind') || 'agents',
      contract: { 'contract/id': id, 'contract/kind': 'agent', enabled: true, agent: { roles: ['knowledge_worker'], model: 'gemma4:31b' } },
      ednText: `{:contract/id "${id}" :contract/kind :agent :enabled true}`,
      validation: { ok: true, errors: [], warnings: [] },
    });
  }
  if (pathname === '/api/knoxx/session/status') return json(res, 200, { status: 'idle', messages: [] });
  return json(res, 404, { error: 'No e2e mock for route', pathname });
}

function createServer(state) {
  const server = http.createServer((req, res) => {
    const url = new URL(req.url || '/', 'http://127.0.0.1');
    const pathname = url.pathname;
    if (pathname.startsWith('/api/')) {
      handleApi(req, res, pathname, url, state).catch((error) => {
        json(res, 500, { error: error.message || String(error) });
      });
      return;
    }
    if (pathname === '/app.css' && !fs.existsSync(path.join(FRONTEND_ROOT, 'app.css')) && !fs.existsSync(path.join(DIST_ROOT, 'app.css'))) {
      return text(res, 200, '', 'text/css; charset=utf-8');
    }
    const filePath = pathname === '/' ? INDEX_PATH : safeStaticPath(pathname);
    if (filePath) {
      res.writeHead(200, { 'content-type': contentTypeFor(filePath) });
      fs.createReadStream(filePath).pipe(res);
      return;
    }
    // SPA fallback.
    res.writeHead(200, { 'content-type': 'text/html; charset=utf-8' });
    fs.createReadStream(INDEX_PATH).pipe(res);
  });

  server.on('upgrade', (_req, socket) => socket.destroy());
  return server;
}

function listen(server) {
  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => {
      server.off('error', reject);
      const address = server.address();
      resolve(`http://127.0.0.1:${address.port}`);
    });
  });
}

function chromeExecutablePath() {
  const candidates = [
    process.env.PUPPETEER_EXECUTABLE_PATH,
    process.env.CHROME_PATH,
    '/usr/bin/google-chrome-stable',
    '/usr/bin/google-chrome',
    '/snap/bin/chromium',
    '/usr/bin/chromium',
    '/usr/bin/chromium-browser',
  ].filter(Boolean);
  const found = candidates.find((candidate) => fs.existsSync(candidate));
  if (!found) throw new Error('No Chromium/Chrome executable found. Set PUPPETEER_EXECUTABLE_PATH.');
  return found;
}

async function createFixture() {
  const state = initialServerState();
  const server = createServer(state);
  const baseUrl = await listen(server);
  const browser = await puppeteer.launch({
    executablePath: chromeExecutablePath(),
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-dev-shm-usage'],
  });
  const page = await browser.newPage();
  page.setDefaultTimeout(25_000);
  await page.setViewport({ width: 1440, height: 1000 });
  const consoleLines = [];
  page.on('console', (msg) => consoleLines.push(`${msg.type()}: ${msg.text()}`));
  page.on('pageerror', (err) => consoleLines.push(`pageerror: ${err.message}`));
  return {
    baseUrl,
    browser,
    page,
    state,
    consoleLines,
    async close() {
      await browser.close().catch(() => undefined);
      await new Promise((resolve) => server.close(resolve));
    },
  };
}

async function waitForText(page, text) {
  await page.waitForFunction((needle) => {
    const haystack = (document.body && document.body.innerText || '').toLowerCase();
    return haystack.includes(String(needle).toLowerCase());
  }, {}, text);
}

async function waitForSelectValue(page, value) {
  await page.waitForFunction((expected) => Array.from(document.querySelectorAll('select')).some((select) => select.value === expected), {}, value);
}

async function clickByText(page, textValue) {
  await page.evaluate((needle) => {
    const candidates = Array.from(document.querySelectorAll('button,a,[role="button"]'));
    const exact = candidates.find((el) => (el.textContent || '').trim() === needle);
    const partial = candidates.find((el) => (el.textContent || '').includes(needle));
    const el = exact || partial;
    if (!el) throw new Error(`Clickable text not found: ${needle}`);
    el.click();
  }, textValue);
}

async function setInputByLabel(page, labelText, value) {
  await page.evaluate(({ labelText: needle, value: nextValue }) => {
    const label = Array.from(document.querySelectorAll('label'))
      .find((candidate) => (candidate.textContent || '').toLowerCase().includes(String(needle).toLowerCase()));
    const input = label && label.querySelector('input,textarea,select');
    if (!input) throw new Error(`Input for label not found: ${needle}`);
    const proto = input instanceof HTMLTextAreaElement
      ? HTMLTextAreaElement.prototype
      : input instanceof HTMLSelectElement
        ? HTMLSelectElement.prototype
        : HTMLInputElement.prototype;
    const descriptor = Object.getOwnPropertyDescriptor(proto, 'value');
    descriptor.set.call(input, nextValue);
    input.dispatchEvent(new Event('input', { bubbles: true }));
    input.dispatchEvent(new Event('change', { bubbles: true }));
  }, { labelText, value });
}

async function bodyText(page) {
  return page.evaluate(() => document.body ? document.body.innerText : '');
}

function includesText(haystack, needle) {
  return String(haystack).toLowerCase().includes(String(needle).toLowerCase());
}

async function withFixture(run) {
  const fixture = await createFixture();
  try {
    return await run(fixture);
  } catch (error) {
    error.message = `${error.message}\nConsole:\n${fixture.consoleLines.slice(-30).join('\n')}`;
    throw error;
  } finally {
    await fixture.close();
  }
}

function waitForNodeCondition(predicate, timeoutMs = 25_000) {
  const started = Date.now();
  return new Promise((resolve, reject) => {
    const tick = () => {
      try {
        if (predicate()) {
          resolve(true);
          return;
        }
      } catch (error) {
        reject(error);
        return;
      }
      if (Date.now() - started > timeoutMs) {
        reject(new Error('Timed out waiting for node-side e2e condition'));
        return;
      }
      setTimeout(tick, 25);
    };
    tick();
  });
}

async function runAuditBridgeSmoke() {
  return withFixture(async ({ page, baseUrl }) => {
    await page.goto(`${baseUrl}/agents?tab=audit`, { waitUntil: 'domcontentloaded' });
    await waitForText(page, 'Agent Audit');
    await waitForText(page, 'Audit sessions');
    await waitForText(page, 'fork_tales_creative_director');
    await clickByText(page, 'fork_tales_creative_director');
    await waitForText(page, 'fork_tales_creative_director_cron');
    await waitForText(page, 'fork-run-active');
    await waitForText(page, 'Fork history');
    await waitForSelectValue(page, 'fork_tales_creative_director');
    const textValue = await bodyText(page);
    return {
      text: textValue,
      hasAuditSessions: includesText(textValue, 'Audit sessions'),
      hasUnifiedActive: includesText(textValue, 'fork-run-active'),
      hasUnifiedHistory: includesText(textValue, 'Fork history'),
      hasTrigger: includesText(textValue, 'fork_tales_creative_director_cron'),
    };
  });
}

async function runAuditSessionResume() {
  return withFixture(async ({ page, baseUrl }) => {
    await page.goto(`${baseUrl}/agents?tab=audit`, { waitUntil: 'domcontentloaded' });
    await waitForText(page, 'fork_tales_creative_director');
    await clickByText(page, 'fork_tales_creative_director');
    await waitForText(page, 'Fork history');
    await clickByText(page, 'Fork history');
    await waitForText(page, 'Compose the Fork Tales song from the audit session.');
    await waitForText(page, 'Fork Tales audit transcript loaded into chat UI.');
    const textValue = await bodyText(page);
    return {
      text: textValue,
      resumedUserMessage: includesText(textValue, 'Compose the Fork Tales song from the audit session.'),
      resumedAssistantMessage: includesText(textValue, 'Fork Tales audit transcript loaded into chat UI.'),
    };
  });
}

async function runContractsEditorSaveValidate() {
  return withFixture(async ({ page, baseUrl, state }) => {
    await page.goto(`${baseUrl}/agents?tab=contracts`, { waitUntil: 'domcontentloaded' });
    await waitForText(page, 'Agents');
    await waitForText(page, 'Contract identity');
    await waitForText(page, 'Full contract EDN');
    await setInputByLabel(page, 'Contract id', 'e2e_contract_agent');
    await waitForText(page, 'e2e_contract_agent');
    await clickByText(page, 'Check');
    await waitForText(page, 'Validation passed.');
    await clickByText(page, 'Save');
    await waitForNodeCondition(() => state.saveRequests.length > 0);
    const textValue = await bodyText(page);
    const saved = state.saveRequests.at(-1);
    const validated = state.validationRequests.at(-1);
    return {
      text: textValue,
      validationPosted: Boolean(validated && includesText(validated.ednText, ':contract/id "e2e_contract_agent"')),
      savePosted: Boolean(saved && saved.id === 'e2e_contract_agent' && includesText(saved.body.ednText, ':contract/id "e2e_contract_agent"')),
      editedIdVisible: includesText(textValue, 'e2e_contract_agent'),
    };
  });
}

module.exports = {
  createFixture,
  runAuditBridgeSmoke,
  runAuditSessionResume,
  runContractsEditorSaveValidate,
};
