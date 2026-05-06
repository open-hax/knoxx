import { readFileSync } from 'node:fs';

const audioPath = process.argv[2] || '/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/Audio/anti_espeak_broadcast.mp3';
const baseUrl = process.argv[3] || 'http://127.0.0.1:8080';
const model = process.argv[4] || 'gemma4-e4b';
const b64 = readFileSync(audioPath).toString('base64');

async function post(path, body) {
  const resp = await fetch(baseUrl + path, {
    method: 'POST',
    headers: { 'content-type': 'application/json', 'authorization': 'Bearer no-key' },
    body: JSON.stringify(body),
  });
  const text = await resp.text();
  return { status: resp.status, text };
}

const responsesBody = {
  model,
  input: [{ role: 'user', content: [
    { type: 'input_text', text: 'Listen to the attached audio and tell me what is in it in one sentence.' },
    { type: 'input_audio', input_audio: { data: b64, format: 'mp3' } },
  ] }],
  max_output_tokens: 80,
  stream: false,
};

const completionsBody = {
  model,
  messages: [{ role: 'user', content: [
    { type: 'text', text: 'Listen to the attached audio and tell me what is in it in one sentence.' },
    { type: 'input_audio', input_audio: { data: b64, format: 'mp3' } },
  ] }],
  max_completion_tokens: 80,
  stream: false,
};

const out = {
  responses: await post('/v1/responses', responsesBody),
  chatCompletions: await post('/v1/chat/completions', completionsBody),
};
console.log(JSON.stringify(out, null, 2));
