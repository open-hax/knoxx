#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { basename } from 'node:path';

const digest = (bytes) => createHash('sha256').update(bytes).digest('hex');
const inventory = 'contracts/.social-promotion-files';
const modelPath = 'contracts/models/gemma4_31b.edn';

function safePath(path) {
  return /^contracts\/(?:[.a-zA-Z0-9_-]+\/)*[.a-zA-Z0-9_-]+$/.test(path)
    && !path.split('/').some((segment) => segment === '.' || segment === '..');
}

function requireCheck(condition, evidence) {
  if (!condition) throw new Error(evidence);
  console.log(`PASS  ${evidence}`);
}

async function probe({ slot, files }) {
  const env = process.env;
  requireCheck(env.KNOXX_PUBLIC_BASE_URL === `https://${slot}.knoxx.promethean.rest`
    && env.MONGODB_URI?.startsWith('mongodb://database:27017/') && env.MONGODB_DB === 'knoxx'
    && env.CONTRACTS_DIR === '/app/contracts' && env.KNOXX_DISABLE_EVENT_RUNTIMES === 'true'
    && Boolean(env.KNOXX_API_KEY),
  'backend uses the isolated database, bundled contracts and disabled event runtimes');
  const platformCredential = Object.keys(env).some((key) => env[key]
    && /(?:discord|bluesky|bsky|atproto).*(?:token|key|secret|password)/i.test(key));
  requireCheck(!platformCredential, 'backend environment has no external platform credentials');

  const port = process.env.PORT || '8000';
  function request(path, { auth = true, body } = {}) {
    return fetch(`http://127.0.0.1:${port}${path}`, {
      headers: {
        ...(auth ? { 'x-api-key': env.KNOXX_API_KEY } : {}),
        ...(body ? { 'content-type': 'application/json' } : {}),
      },
      method: body ? 'POST' : 'GET',
      body: body ? JSON.stringify(body) : undefined,
      signal: AbortSignal.timeout(15_000),
    });
  }

  const validate = '/api/admin/contracts/validate';
  const invalid = { contractClass: 'agents', ednText: '{' };
  const refused = await request(validate, { auth: false, body: invalid });
  requireCheck([401, 403].includes(refused.status), 'anonymous contract validation is refused');
  const rejected = await request(validate, { body: invalid });
  requireCheck(rejected.ok && (await rejected.json()).ok === false,
    'authenticated validation rejects malformed EDN');
  const fileNames = Object.keys(files).sort();
  const listed = readFileSync(`/app/${inventory}`, 'utf8').trim().split('\n').map((path) => `contracts/${path}`);
  requireCheck(JSON.stringify(fileNames) === JSON.stringify([...new Set([inventory, modelPath, ...listed])].sort()),
    'the manifest covers every path listed by the installed package');
  const agentPaths = Object.keys(files).filter((path) => /^contracts\/agents\/[^/]+\.edn$/.test(path));
  for (const [path, sha] of Object.entries(files)) {
    const bytes = readFileSync(`/app/${path}`);
    requireCheck(digest(bytes) === sha, `${path} matches the reviewed source SHA-256`);
    if (!agentPaths.includes(path)) continue;
    const response = await request(validate, { body: { contractClass: 'agents', ednText: bytes.toString() } });
    requireCheck(response.ok && (await response.json()).ok === true, `${path} passes live schema validation`);
  }

  const list = await request('/api/admin/contracts?contractClass=agents');
  const ids = new Set((await list.json()).contracts?.map(({ id }) => id));
  requireCheck(list.ok && agentPaths.every((path) => ids.has(basename(path, '.edn'))),
    'all shipped social agents are visible to the live resource loader');
  const catalog = await request('/api/tools/catalog?actor=duckman&agent=duckman_social');
  const scoped = await catalog.json();
  const socialTools = ['social.request', 'social.remember', 'social.conversations'];
  requireCheck(catalog.ok && scoped.actor_id === 'duckman' && scoped.agent_id === 'duckman_social'
    && socialTools.every((id) => scoped.tools?.some((tool) => tool.id === id && tool.enabled)),
  'the Duckman contract resolves the social request and continuity tool grants');
  const otherActor = await request('/api/tools/catalog?actor=knoxx_default&agent=duckman_social');
  requireCheck([401, 403, 404].includes(otherActor.status)
    || (otherActor.ok && (await otherActor.json()).agent_id !== 'duckman_social'),
  'the Duckman contract does not resolve under a different actor');
  const eventConfig = await request('/api/admin/config/events');
  const { runtime } = await eventConfig.json();
  requireCheck(eventConfig.ok && runtime?.disabled === true && runtime.running === false,
    'the running backend reports disabled event processing');
  const scheduledAgents = ['ussyverse_social_creative', 'ussyverse_social_replies', 'open_hax_original_author',
    'duckman_original_author', 'duckman_discord_responder'];
  requireCheck(eventConfig.ok && scheduledAgents.every((id) => runtime?.triggers?.some((trigger) => trigger.agent === id))
    && runtime.triggers.filter(({ agent }) => scheduledAgents.includes(agent)).every(({ enabled }) => enabled === false),
  'the live event configuration reports all packaged social triggers as disabled');
  const model = readFileSync(`/app/${modelPath}`, 'utf8');
  requireCheck(/:model\/id\s+"gemma4:31b"/.test(model) && /:model\/default\s+true/.test(model),
    'Gemma 4 31B is declared as the portable default');
  console.log('PASS  no contracts were changed and no event or platform operation was dispatched');
}

function emit(sourceSha) {
  if (!/^[0-9a-f]{40}$/.test(sourceSha ?? '')) throw new Error('a full Knoxx source SHA is required');
  const contents = (path) => execFileSync('git', ['show', `${sourceSha}:${path}`], { maxBuffer: 8 * 1024 * 1024 });
  const listed = contents(inventory).toString().trim().split('\n').map((path) => `contracts/${path}`);
  const files = Object.fromEntries([...new Set([inventory, modelPath, ...listed])].sort().map((path) => {
    if (!safePath(path)) throw new Error(`invalid promotion path ${path}`);
    return [path, digest(contents(path))];
  }));
  console.log(JSON.stringify({ sourceSha, files }));
}

function verify(slot, container) {
  const { sourceSha, files } = JSON.parse(readFileSync(0, 'utf8'));
  if (!['testing', 'staging'].includes(slot) || !/^[0-9a-f]{40}$/.test(sourceSha ?? '')
    || !/^[a-zA-Z0-9][a-zA-Z0-9_.-]*$/.test(container ?? '')
    || !files?.[inventory] || !files[modelPath] || !Object.keys(files).some((path) => path.startsWith('contracts/agents/'))
    || !Object.entries(files).every(([path, sha]) => safePath(path) && /^[0-9a-f]{64}$/.test(sha))) {
    throw new Error('expected a valid slot, backend container and promotion manifest');
  }
  const receipt = JSON.parse(readFileSync(`/srv/open-hax/environments/${slot}/knoxx/deployment.json`, 'utf8'));
  const image = execFileSync('docker', ['inspect', '--format', '{{.Image}}', container], { encoding: 'utf8' }).trim();
  requireCheck(receipt.status === 'healthy' && receipt.environment === slot && receipt.service === 'knoxx'
    && receipt.source_sha === sourceSha && receipt.images?.KNOXX_BACKEND_IMAGE === image,
  'receiver receipt binds the inspected backend image to the supplied Knoxx revision');

  execFileSync('docker', ['exec', '-i', container, 'node', '--input-type=module', '-',
    '--probe', JSON.stringify({ slot, files })],
  { input: readFileSync(new URL(import.meta.url)), stdio: ['pipe', 'inherit', 'inherit'] });
}

try {
  const [mode, ...args] = process.argv.slice(2);
  if (mode === '--manifest' && args.length === 1) emit(args[0]);
  else if (mode === '--verify' && args.length === 2) verify(...args);
  else if (mode === '--probe' && args.length === 1) await probe(JSON.parse(args[0]));
  else throw new Error('use --manifest <full-source-sha> or --verify <testing|staging> <backend-container> with a manifest on stdin');
} catch (error) {
  console.error(`FAIL  ${error.message}`);
  process.exitCode = 1;
}
