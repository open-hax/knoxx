import { readFileSync } from 'node:fs';
import { execSync } from 'node:child_process';
import process from 'node:process';
import { complete } from '/home/err/devel/orgs/open-hax/eta-mu/packages/ai/dist/index.js';

function cleanAnsi(text) {
  return text.replace(/\x1b\[[0-9;]*m/g, '');
}

function parseEdnStringField(text, key) {
  const match = text.match(new RegExp(`${key}\\s+\"([^\"]+)\"`));
  return match?.[1] ?? null;
}

function parseEdnKeywordField(text, key) {
  const match = text.match(new RegExp(`${key}\\s+:([A-Za-z0-9_\\-/]+)`));
  return match?.[1] ?? null;
}

function parseKvMap(raw) {
  const map = new Map();
  for (const item of String(raw || '').split(',')) {
    const [left, right] = item.split('=', 2);
    const key = left?.trim();
    const value = right?.trim();
    if (key && value) map.set(key, value);
  }
  return map;
}

function readPm2Env(appId = '1') {
  const raw = cleanAnsi(execSync(`pm2 env ${appId}`, { encoding: 'utf8' }));
  const get = (key) => (raw.match(new RegExp(`^${key}:\\s*(.*)$`, 'm')) || [])[1]?.trim();
  return {
    KNOXX_PROVIDER_BASE_URLS: get('KNOXX_PROVIDER_BASE_URLS') || '',
    KNOXX_PROVIDER_AUTH_TOKENS: get('KNOXX_PROVIDER_AUTH_TOKENS') || '',
    LLAMACPP_API_KEY: get('LLAMACPP_API_KEY') || '',
  };
}

const contractPath = process.argv[2] || 'contracts/models/gemma4_e4b.edn';
const audioPath = process.argv[3] || '/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/Audio/anti_espeak_broadcast.mp3';
const contractText = readFileSync(contractPath, 'utf8');
const modelId = parseEdnStringField(contractText, ':model/id');
const api = parseEdnStringField(contractText, ':model/api');
const provider = parseEdnKeywordField(contractText, ':model/provider');
if (!modelId || !api || !provider) {
  throw new Error(`Could not parse model contract fields from ${contractPath}`);
}

const pm2env = readPm2Env();
const baseUrlMap = parseKvMap(process.env.KNOXX_PROVIDER_BASE_URLS || pm2env.KNOXX_PROVIDER_BASE_URLS || '');
const authTokenMap = parseKvMap(process.env.KNOXX_PROVIDER_AUTH_TOKENS || pm2env.KNOXX_PROVIDER_AUTH_TOKENS || '');
const baseUrl = baseUrlMap.get(provider);
const apiKeyRef = authTokenMap.get(provider);
const apiKey = apiKeyRef ? (process.env[apiKeyRef] || pm2env[apiKeyRef] || apiKeyRef) : undefined;
if (!baseUrl) {
  throw new Error(`No base URL configured for provider ${provider}`);
}
if (!apiKey) {
  throw new Error(`No API key configured for provider ${provider}`);
}

const audioBytes = readFileSync(audioPath);
const ext = audioPath.split('.').pop()?.toLowerCase();
const audioFormat = ext === 'wav' ? 'wav' : 'mp3';
const mimeType = audioFormat === 'wav' ? 'audio/wav' : 'audio/mpeg';

const model = {
  id: modelId,
  name: modelId,
  api,
  provider,
  baseUrl: `${baseUrl.replace(/\/$/, '')}/v1`,
  reasoning: false,
  input: ['text', 'audio'],
  cost: { input: 0, output: 0, cacheRead: 0, cacheWrite: 0 },
  contextWindow: 8192,
  maxTokens: 2048,
};

async function runCase(label, content) {
  try {
    const response = await complete(model, {
      messages: [{ role: 'user', timestamp: Date.now(), content }],
    }, {
      apiKey,
      maxTokens: 80,
    });
    return {
      label,
      ok: response.stopReason !== 'error',
      stopReason: response.stopReason,
      content: response.content,
    };
  } catch (error) {
    return {
      label,
      ok: false,
      fatalError: error?.message || String(error),
    };
  }
}

const textOnly = await runCase('text-only', [
  { type: 'text', text: 'Say hello in exactly three words.' },
]);
const withAudio = await runCase('with-audio', [
  { type: 'text', text: 'Listen to the attached audio and say what is in it in one sentence.' },
  { type: 'audio', mimeType, data: audioBytes.toString('base64'), format: audioFormat },
]);

console.log(JSON.stringify({
  contractPath,
  modelId,
  provider,
  api,
  baseUrl: model.baseUrl,
  audioPath,
  results: [textOnly, withAudio],
}, null, 2));
