/**
 * Knoxx PM2 ecosystem
 *
 * Runs all three Knoxx services on the host for source-mapped debugging:
 *   1. knoxx-shadow    — shadow-cljs watch with source maps (compiles CLJS → dist/)
 *   2. knoxx-backend   — node src/server.mjs with source-map stack traces
 *   3. knoxx-frontend  — vite dev server with HMR + API proxy to backend
 *   4. knoxx-ingestion — clojure -M:run (kms-ingestion on port 3003)
 *
 * Dependencies (must be running separately):
 *   - Redis     on localhost:6379  (compose: knoxx-redis)
 *   - Postgres  on localhost:5432  (compose: knoxx-postgres, user=kms db=knoxx)
 *   - Proxx     on localhost:8789
 *   - OpenPlanner on localhost:7777
 *
 * Usage:
 *   pm2 start ecosystem.dev.cjs
 *   pm2 logs knoxx            # all knoxx-* logs
 *   pm2 stop knoxx            # stop all
 *   pm2 delete knoxx          # remove all
 */

// Load host env for real API keys (avoids committing secrets)
const fs = require('fs');
const path = require('path');
const os = require('os');
const { execSync } = require('child_process');

function loadSimpleEnv(envPath) {
  try {
    const raw = fs.readFileSync(envPath, 'utf8');
    return raw.split(/\r?\n/).reduce((acc, line) => {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith('#')) return acc;
      const idx = trimmed.indexOf('=');
      if (idx < 0) return acc;
      const key = trimmed.slice(0, idx).trim();
      const value = trimmed.slice(idx + 1);
      if (!key) return acc;
      acc[key] = value;
      return acc;
    }, {});
  } catch (_err) {
    return {};
  }
}

function tryGitTopLevel(cwd) {
  try {
    return execSync('git rev-parse --show-toplevel', { cwd, stdio: ['ignore', 'pipe', 'ignore'] })
      .toString('utf8')
      .trim();
  } catch (_err) {
    return null;
  }
}

function isLocalPortBound(port) {
  try {
    execSync(`ss -ltn 'sport = :${port}' | grep -F '127.0.0.1:${port}'`, {
      stdio: ['ignore', 'ignore', 'ignore'],
    });
    return true;
  } catch (_err) {
    return false;
  }
}

const knoxxRoot = __dirname;
const backendDir = path.join(knoxxRoot, 'backend');
const frontendDir = path.join(knoxxRoot, 'frontend');
const ingestionDir = path.join(knoxxRoot, 'ingestion');
const sttNpuDir = path.join(knoxxRoot, 'voice', 'stt-npu');

// Workspace root is where Knoxx is allowed to read/write files.
// DO NOT hard-code /home/err/devel: other machines/users check out elsewhere.
const workspaceRoot =
  process.env.WORKSPACE_ROOT ||
  process.env.WORKSPACE_PATH ||
  process.env.KNOXX_WORKSPACE_ROOT ||
  tryGitTopLevel(knoxxRoot) ||
  // Fallback: assume standard repo layout: <root>/orgs/open-hax/openplanner/packages/agents/knoxx
  path.resolve(knoxxRoot, '../../../../../../..');

const defaultHostEnvPath = path.join(os.homedir(), '.knoxx', '.env.cephalon-host');
const hostEnvPath = process.env.KNOXX_HOST_ENV_PATH || defaultHostEnvPath;
const hostEnv = loadSimpleEnv(hostEnvPath);
const sttNpuPort = process.env.KNOXX_STT_PORT || hostEnv.KNOXX_STT_PORT || '8010';
// Voice STT for the local Knoxx stack is pinned to the repo-local sidecar by default.
// Only a process-level override should redirect it somewhere else.
const sttNpuExplicitBaseUrl = process.env.KNOXX_STT_BASE_URL || '';
const sttNpuBaseUrl = sttNpuExplicitBaseUrl || `http://127.0.0.1:${sttNpuPort}`;
const sttNpuModelDir = process.env.KNOXX_STT_MODEL_DIR
  || hostEnv.KNOXX_STT_MODEL_DIR
  || path.join(os.homedir(), '.knoxx', 'models', 'stt-npu');
const sttNpuPython = process.env.KNOXX_STT_PYTHON
  || hostEnv.KNOXX_STT_PYTHON
  || (fs.existsSync(path.join(sttNpuDir, '.venv', 'bin', 'python'))
    ? path.join(sttNpuDir, '.venv', 'bin', 'python')
    : 'python3');
// auto  = only launch local STT when no explicit base URL is configured
//         and the target port is currently free.
// force = always include the local PM2 sidecar.
// off   = never include the local PM2 sidecar.
const sttNpuPm2Mode = process.env.KNOXX_STT_PM2_MODE || hostEnv.KNOXX_STT_PM2_MODE || 'auto';
const sttNpuShouldRunLocal =
  fs.existsSync(path.join(sttNpuDir, 'server.py'))
  && sttNpuPm2Mode !== 'off'
  && (
    sttNpuPm2Mode === 'force'
    || (!sttNpuExplicitBaseUrl && !isLocalPortBound(sttNpuPort))
  );
const shoedelussyDir = process.env.SHOEDELUSSY_DIR || hostEnv.SHOEDELUSSY_DIR || path.join(os.homedir(), '.knoxx', 'external', 'shoedelussy');
const shoedelussyServerDir = path.join(shoedelussyDir, 'server');
const shoedelussyMcpPort = process.env.SHOEDELUSSY_MCP_PORT || hostEnv.SHOEDELUSSY_MCP_PORT || '8790';
const shoedelussyExplicitBaseUrl = process.env.SHOEDELUSSY_MCP_BASE_URL || hostEnv.SHOEDELUSSY_MCP_BASE_URL || '';
const shoedelussyMcpBaseUrl = shoedelussyExplicitBaseUrl || `http://127.0.0.1:${shoedelussyMcpPort}/mcp`;
// auto  = only launch a local Wrangler when no explicit base URL is configured
//         and the target port is currently free.
// force = always include the local PM2 sidecar.
// off   = never include the local PM2 sidecar.
const shoedelussyPm2Mode = process.env.SHOEDELUSSY_PM2_MODE || hostEnv.SHOEDELUSSY_PM2_MODE || 'auto';
const shoedelussyShouldRunLocal =
  fs.existsSync(shoedelussyServerDir)
  && shoedelussyPm2Mode !== 'off'
  && (
    shoedelussyPm2Mode === 'force'
    || (!shoedelussyExplicitBaseUrl && !isLocalPortBound(shoedelussyMcpPort))
  );

const apps = [
    // ── 1. shadow-cljs watch ──────────────────────────────────────────
    {
      name: 'knoxx-shadow',
      cwd: backendDir,
      script: 'pnpm',
      // Force source maps in the watch build so dist/*.map stays available
      // even if the underlying shadow config drifts.
      args: 'exec shadow-cljs --source-maps watch app',
      watch: false,
      autorestart: true,
      max_restarts: 10,
      restart_delay: 5000,
      env: {
        NODE_ENV: 'development',
      },
    },

    // ── 2. STT (Whisper/OpenVINO sidecar) ────────────────────────────
    ...(sttNpuShouldRunLocal
      ? [{
        name: 'knoxx-stt-npu',
        cwd: sttNpuDir,
        script: sttNpuPython,
        interpreter: 'none',
        args: 'server.py',
        watch: false,
        autorestart: true,
        max_restarts: 10,
        restart_delay: 5000,
        kill_timeout: 15000,
        env: {
          PORT: sttNpuPort,
          MODEL_DIR: sttNpuModelDir,
          WHISPER_DEVICE: process.env.WHISPER_DEVICE || hostEnv.WHISPER_DEVICE || 'NPU',
          WHISPER_MODEL_ID:
            process.env.WHISPER_MODEL_ID
            || hostEnv.WHISPER_MODEL_ID
            || 'anubhav200/openai-whisper-small-openvino-int4',
          WHISPER_NPU_COMPILER_TYPE:
            process.env.WHISPER_NPU_COMPILER_TYPE
            || hostEnv.WHISPER_NPU_COMPILER_TYPE
            || 'DRIVER',
          PYTHONUNBUFFERED: '1',
        },
      }]
      : []),

    // ── 3. Backend (Node + compiled CLJS) ────────────────────────────
    {
      name: 'knoxx-backend',
      cwd: backendDir,
      script: 'src/server.mjs',
      node_args: '--enable-source-maps',
      // Keep graceful PM2 shutdown messaging for resumable agent sessions,
      // but do not gate startup on PM2 wait_ready: Knoxx can be fully serving
      // before PM2 accepts the ready handshake, which caused duplicate launches
      // and EADDRINUSE restart loops on port 8000.
      kill_timeout: 35000,
      shutdown_with_message: true,
      // Auto-restart when shadow-cljs produces new output
      watch: ['dist', 'src/server.mjs'],
      watch_delay: 800,
      ignore_watch: ['.shadow-cljs', 'node_modules', 'tmp', '.git'],
      autorestart: true,
      max_restarts: 15,
      restart_delay: 3000,
      env: {
        NODE_ENV: 'development',
        HOST: '0.0.0.0',
        PORT: '8000',
        WORKSPACE_ROOT: workspaceRoot,
          DISCORD_BOT_TOKEN: hostEnv.DISCORD_BOT_TOKEN,
        KNOXX_SESSION_PROJECT_NAME: 'knoxx-session',
        KNOXX_COLLECTION_NAME: 'devel_docs',
        AUDD_API_TOKEN: hostEnv.AUDD_API_TOKEN || '',
        ACOUSTID_API_KEY: hostEnv.ACOUSTID_API_KEY || '',
        // Public base URL used for OAuth redirect_uri + cookie scope
        KNOXX_PUBLIC_BASE_URL: hostEnv.KNOXX_PUBLIC_BASE_URL || 'http://localhost',
        // GitHub OAuth
        KNOXX_GITHUB_OAUTH_CLIENT_ID: hostEnv.KNOXX_GITHUB_OAUTH_CLIENT_ID || '',
        KNOXX_GITHUB_OAUTH_CLIENT_SECRET: hostEnv.KNOXX_GITHUB_OAUTH_CLIENT_SECRET || '',
        // Canonical Proxx (on host via compose port-forward)
        PROXX_BASE_URL: hostEnv.PROXX_BASE_URL || 'http://127.0.0.1:8789',
          PROXX_DEFAULT_MODEL: 'gemma4:31b',
        PROXX_AUTH_TOKEN: hostEnv.PROXX_AUTH_TOKEN || hostEnv.PROXY_AUTH_TOKEN || 'change-me-open-hax-proxy-token',
        // OpenPlanner (on host via compose port-forward)
        OPENPLANNER_BASE_URL: 'http://127.0.0.1:7777',
        OPENPLANNER_API_KEY: hostEnv.OPENPLANNER_API_KEY || 'change-me',
        KNOXX_API_KEY: hostEnv.KNOXX_API_KEY || process.env.KNOXX_API_KEY || 'change-me',
        KNOXX_API_KEY_USER_EMAIL: hostEnv.KNOXX_API_KEY_USER_EMAIL || process.env.KNOXX_API_KEY_USER_EMAIL || 'pi@open-hax.local',
        SHOEDELUSSY_MCP_BASE_URL: shoedelussyMcpBaseUrl,
        SHOEDELUSSY_MCP_TOOL_NAME: hostEnv.SHOEDELUSSY_MCP_TOOL_NAME || 'shoedelussy',
        SHOEDELUSSY_MCP_SHARED_SECRET: hostEnv.SHOEDELUSSY_MCP_SHARED_SECRET || '',
        // Redis + Postgres (compose services forwarded to host)
        REDIS_URL: 'redis://127.0.0.1:6379',
        KNOXX_SHUTDOWN_GRACE_MS: '25000',
        KNOXX_SHUTDOWN_POLL_MS: '250',
        KNOXX_POLICY_DATABASE_URL: 'postgresql://kms:kms@127.0.0.1:5432/knoxx',
        DATABASE_URL: 'postgresql://kms:kms@127.0.0.1:5432/knoxx',
        // STT (NPU service on host)
        KNOXX_STT_BASE_URL: sttNpuBaseUrl,

        // TTS (ElevenLabs)
        // Accept historical/local key names from ~/.knoxx/.env.cephalon-host
        KNOXX_ELEVENLABS_API_KEY:
          hostEnv.KNOXX_ELEVENLABS_API_KEY
          || hostEnv.KNOXX_ELEVENLABS_KEY
          || hostEnv.ELEVENLABS_API_KEY
          || hostEnv.ELEVEN_LABS_API_KEY
          || hostEnv.XI_API_KEY
          || '',
        KNOXX_ELEVENLABS_VOICE_ID: hostEnv.KNOXX_ELEVENLABS_VOICE_ID || hostEnv.ELEVENLABS_VOICE_ID || '',
        KNOXX_ELEVENLABS_MODEL_ID: hostEnv.KNOXX_ELEVENLABS_MODEL_ID || hostEnv.ELEVENLABS_MODEL_ID || 'eleven_multilingual_v2',

        // Ingestion service on host
        KMS_INGESTION_URL: 'http://127.0.0.1:3003',
      },
    },

    // ── 4. Frontend (Vite dev server) ────────────────────────────────
    {
      name: 'knoxx-frontend',
      cwd: frontendDir,
      script: 'pnpm',
      args: 'dev',
      watch: false,
      autorestart: true,
      max_restarts: 10,
      restart_delay: 3000,
      env: {
        NODE_ENV: 'development',
        // Vite proxy target: the host backend
        VITE_KNOXX_BACKEND_URL: 'http://127.0.0.1:8000',
      },
    },

    // ── 5. Ingestion (Clojure JVM) ──────────────────────────────────
    {
      name: 'knoxx-ingestion',
      cwd: ingestionDir,
      script: 'clojure',
      interpreter: '/bin/bash',
      args: '-M:run',
      watch: false,
      autorestart: true,
      max_restarts: 10,
      restart_delay: 5000,
      env: {
        PORT: '3003',
        DATABASE_URL: 'postgresql://kms:kms@127.0.0.1:5432/knoxx',
        REDIS_URL: 'redis://127.0.0.1:6379',
        KNOXX_BACKEND_URL: 'http://127.0.0.1:8000',
        WORKSPACE_PATH: workspaceRoot,
        OPENPLANNER_BASE_URL: 'http://127.0.0.1:7777',
        OPENPLANNER_API_KEY: hostEnv.OPENPLANNER_API_KEY || 'change-me',
        KNOXX_API_KEY: hostEnv.KNOXX_API_KEY || process.env.KNOXX_API_KEY || 'change-me',
        PROXX_BASE_URL: hostEnv.PROXX_BASE_URL || 'http://127.0.0.1:8789',
        PROXX_AUTH_TOKEN: hostEnv.PROXX_AUTH_TOKEN || hostEnv.PROXY_AUTH_TOKEN || 'change-me-open-hax-proxy-token',
      },
    },
];

// Run Wrangler directly so PM2 manages the real parent process instead of a pnpm wrapper.
if (shoedelussyShouldRunLocal) {
  apps.splice(2, 0, {
    name: 'shoedelussy-mcp',
    cwd: shoedelussyServerDir,
    script: path.join(shoedelussyServerDir, 'node_modules', '.bin', 'wrangler'),
    interpreter: 'none',
    args: `dev --port ${shoedelussyMcpPort} --ip 127.0.0.1`,
    watch: false,
    autorestart: true,
    max_restarts: 10,
    restart_delay: 5000,
    kill_timeout: 15000,
    env: {
      NODE_ENV: 'development',
      MCP_SECRET: hostEnv.SHOEDELUSSY_MCP_SHARED_SECRET || '',
      APP_URL: hostEnv.SHOEDELUSSY_APP_URL || 'http://127.0.0.1:5173',
      OPENROUTER_API_KEY: hostEnv.SHOEDELUSSY_OPENROUTER_API_KEY || hostEnv.OPENROUTER_API_KEY || '',
      OPENROUTER_MODEL: hostEnv.SHOEDELUSSY_OPENROUTER_MODEL || hostEnv.OPENROUTER_MODEL || 'google/gemini-2.5-flash',
    },
  });
}

module.exports = {
  apps,
};
