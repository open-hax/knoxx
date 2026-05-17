import { readFileSync } from 'node:fs';
import { execSync } from 'node:child_process';
import { complete } from '/home/err/devel/orgs/open-hax/eta-mu/packages/ai/dist/index.js';

function cleanAnsi(text) {
  return text.replace(/\x1b\[[0-9;]*m/g, '');
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

function parseEdnStringField(text, key) {
  const match = text.match(new RegExp(`${key}\\s+\"([^\"]+)\"`));
  return match?.[1] ?? null;
}

function parseSystemPrompt(text) {
  const match = text.match(/:prompts\s+\{:system\s+\"([\s\S]*?)\"\}/);
  return match?.[1] ?? null;
}

function parseBooleanField(text, key) {
  const match = text.match(new RegExp(`${key}\\s+(true|false)`));
  return match ? match[1] === 'true' : null;
}

function parseCompatMap(text) {
  const match = text.match(/:model\/compat\s+\{([\s\S]*?)\}/);
  if (!match) return {};
  const raw = match[1];
  const compat = {};
  const tf = raw.match(/:thinkingFormat\s+\"([^\"]+)\"/);
  if (tf) compat.thinkingFormat = tf[1];
  const sre = raw.match(/:supportsReasoningEffort\s+(true|false)/);
  if (sre) compat.supportsReasoningEffort = sre[1] === 'true';
  return compat;
}

function modelContractPath(modelId) {
  const slug = modelId.replace(/[.:]/g, '_');
  return `contracts/models/${slug}.edn`;
}

function resolveModelFromContracts(modelId, env) {
  const modelText = readFileSync(modelContractPath(modelId), 'utf8');
  const provider = parseEdnStringField(modelText, ':model/provider') || (modelText.match(/:model\/provider\s+:([A-Za-z0-9_-]+)/)?.[1] ?? null);
  const api = parseEdnStringField(modelText, ':model/api') ?? 'openai-completions';
  const reasoning = parseBooleanField(modelText, ':model/reasoning');
  const compat = parseCompatMap(modelText);
  if (!provider) throw new Error(`Could not parse provider from model contract for ${modelId}`);

  const baseUrlMap = parseKvMap(process.env.KNOXX_PROVIDER_BASE_URLS || env.KNOXX_PROVIDER_BASE_URLS || '');
  const authTokenMap = parseKvMap(process.env.KNOXX_PROVIDER_AUTH_TOKENS || env.KNOXX_PROVIDER_AUTH_TOKENS || '');
  const baseUrl = baseUrlMap.get(provider);
  const apiKeyRef = authTokenMap.get(provider);
  const apiKey = apiKeyRef ? (process.env[apiKeyRef] || env[apiKeyRef] || apiKeyRef) : undefined;
  if (!baseUrl) throw new Error(`No base URL configured for provider ${provider}`);
  if (!apiKey) throw new Error(`No API key configured for provider ${provider}`);

  return {
    model: {
      id: modelId,
      name: modelId,
      api,
      provider,
      baseUrl: `${baseUrl.replace(/\/$/, '')}/v1`,
      reasoning: reasoning ?? false,
      compat,
      input: ['text', 'audio'],
      cost: { input: 0, output: 0, cacheRead: 0, cacheWrite: 0 },
      contextWindow: 8192,
      maxTokens: 2048,
    },
    apiKey,
  };
}

function userPromptForContract(contractId, audioPath) {
  switch (contractId) {
    case 'broadcast_studio_audio_transcriber':
      return `Audio file path: ${audioPath}. Transcribe any intelligible speech or lyrics. If there is none, say so and give a brief summary.`;
    case 'broadcast_studio_audio_describer':
      return `Audio file path: ${audioPath}. Describe it for broadcast production: content, mood, pacing, likely use, and notable sonic events.`;
    case 'broadcast_studio_audio_labeler':
      return `Audio file path: ${audioPath}. Suggest reusable graph labels grouped under Content, Mood, Function, and Production.`;
    default:
      return `Audio file path: ${audioPath}. Follow your contract.`;
  }
}

const maxTokens = Number.parseInt(process.env.ETA_MU_AUDIO_SMOKE_MAX_TOKENS || '220', 10);

async function runContract(contractPath, audioPath, env) {
  const contractText = readFileSync(contractPath, 'utf8');
  const contractId = parseEdnStringField(contractText, ':contract/id');
  const modelId = parseEdnStringField(contractText, ':model') || parseEdnStringField(contractText, ':agent {:model');
  const systemPrompt = parseSystemPrompt(contractText);
  if (!contractId || !modelId || !systemPrompt) {
    throw new Error(`Failed to parse contract metadata from ${contractPath}`);
  }

  const { model, apiKey } = resolveModelFromContracts(modelId, env);
  const audioBytes = readFileSync(audioPath);
  const ext = audioPath.split('.').pop()?.toLowerCase();
  const audioFormat = ext === 'wav' ? 'wav' : 'mp3';
  const mimeType = audioFormat === 'wav' ? 'audio/wav' : 'audio/mpeg';

  try {
    const response = await complete(model, {
      systemPrompt,
      messages: [{
        role: 'user',
        timestamp: Date.now(),
        content: [
          { type: 'text', text: userPromptForContract(contractId, audioPath) },
          { type: 'audio', mimeType, data: audioBytes.toString('base64'), format: audioFormat },
        ],
      }],
    }, {
      apiKey,
      maxTokens,
    });

    return {
      contractId,
      modelId,
      provider: model.provider,
      api: model.api,
      stopReason: response.stopReason,
      ok: response.stopReason !== 'error',
      content: response.content,
    };
  } catch (error) {
    return {
      contractId,
      modelId,
      provider: model.provider,
      api: model.api,
      ok: false,
      fatalError: error?.message || String(error),
    };
  }
}

const env = readPm2Env();
const audioPath = process.argv[2] || '/tmp/anti_espeak_broadcast_5s.wav';
const contractPaths = process.argv.slice(3).length > 0
  ? process.argv.slice(3)
  : [
      'contracts/agents/broadcast_studio_audio_transcriber.edn',
      'contracts/agents/broadcast_studio_audio_describer.edn',
      'contracts/agents/broadcast_studio_audio_labeler.edn',
    ];

const results = [];
for (const contractPath of contractPaths) {
  results.push(await runContract(contractPath, audioPath, env));
}
console.log(JSON.stringify({ audioPath, results }, null, 2));
