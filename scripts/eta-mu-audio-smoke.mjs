import { readFileSync } from "node:fs";
import { execSync } from "node:child_process";
import process from "node:process";
import { streamSimple } from "/home/err/devel/orgs/open-hax/eta-mu/packages/ai/dist/index.js";

function cleanAnsi(text) {
  return text.replace(/\x1b\[[0-9;]*m/g, "");
}

function readPm2Env(appId = "1") {
  const raw = cleanAnsi(execSync(`pm2 env ${appId}`, { encoding: "utf8" }));
  const get = (key) => (raw.match(new RegExp(`^${key}:\\s*(.*)$`, "m")) || [])[1]?.trim();
  return {
    proxxBaseUrl: get("PROXX_BASE_URL"),
    proxxAuthToken: get("PROXX_AUTH_TOKEN"),
    proxxDefaultModel: get("PROXX_DEFAULT_MODEL"),
  };
}

const audioPath = process.argv[2] || "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/Audio/anti_espeak_broadcast.mp3";
const api = process.argv[3] || "openai-responses";
const env = readPm2Env();

if (!env.proxxBaseUrl || !env.proxxAuthToken) {
  throw new Error("Could not resolve PROXX_BASE_URL / PROXX_AUTH_TOKEN from pm2 env 1");
}

const audioBytes = readFileSync(audioPath);
const requestLog = [];
const originalFetch = globalThis.fetch;
if (typeof originalFetch !== "function") {
  throw new Error("global fetch is not available in this Node runtime");
}

globalThis.fetch = async (input, init) => {
  const url = typeof input === "string" ? input : input?.url;
  let parsedBody = null;
  if (typeof init?.body === "string") {
    try {
      parsedBody = JSON.parse(init.body);
    } catch {
      parsedBody = { rawBodyPreview: init.body.slice(0, 400) };
    }
  }
  requestLog.push({
    url,
    method: init?.method || "GET",
    body: parsedBody,
  });
  return originalFetch(input, init);
};

const model = {
  id: "gemma4:e4b",
  name: "Gemma 4 E4B",
  api,
  provider: "proxx",
  baseUrl: `${env.proxxBaseUrl.replace(/\/$/, "")}/v1`,
  reasoning: false,
  input: ["text", "audio"],
  cost: { input: 0, output: 0, cacheRead: 0, cacheWrite: 0 },
  contextWindow: 8192,
  maxTokens: 2048,
};

const context = {
  messages: [
    {
      role: "user",
      timestamp: Date.now(),
      content: [
        { type: "text", text: "Listen to the attached audio and say what is in it in one sentence." },
        {
          type: "audio",
          mimeType: "audio/mpeg",
          data: audioBytes.toString("base64"),
          format: "mp3",
        },
      ],
    },
  ],
};

try {
  const textDeltas = [];
  let doneReason = null;
  let errorMessage = null;

  const stream = streamSimple(model, context, {
    apiKey: env.proxxAuthToken,
    maxTokens: 120,
  });

  for await (const event of stream) {
    if (event.type === "text_delta") {
      textDeltas.push(event.delta);
    } else if (event.type === "error") {
      errorMessage = event.error?.errorMessage || JSON.stringify(event.error);
    } else if (event.type === "done") {
      doneReason = event.reason;
    }
  }

  console.log(JSON.stringify({
    ok: !errorMessage,
    doneReason,
    errorMessage,
    text: textDeltas.join(""),
    requestLog,
  }, null, 2));
  if (errorMessage) process.exitCode = 1;
} catch (error) {
  console.log(JSON.stringify({
    ok: false,
    fatalError: {
      message: error?.message || String(error),
      cause: error?.cause ? String(error.cause) : null,
    },
    requestLog,
  }, null, 2));
  process.exitCode = 1;
}
