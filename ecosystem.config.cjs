/**
 * Knoxx PM2 ecosystem
 *
 * Runs all Knoxx services on the host for source-mapped debugging:
 *   1. knoxx-shadow          — shadow-cljs watch with source maps (compiles backend CLJS → dist-dev/)
 *   2. knoxx-backend         — node dist-dev/server.js (watch-produced dev output) with source-map stack traces
 *   3. knoxx-frontend        — migration mode: Vite builds to ./dist (watch) while shadow-cljs owns the dev HTTP server on port 5173
 *   4. knoxx-ingestion       — clojure -M:run (kms-ingestion on port 3003)
 *
 * Frontend migration notes (important):
 *   - Vite must NOT run its own dev server in this setup.
 *   - shadow-cljs serves ./dist and proxies /api,/ws,/health to the backend.
 *   - The TS→CLJS bridge is built by Vite in watch mode (vite.bridge.config.ts).
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
const contractsDir = path.join(knoxxRoot, 'contracts');

// Workspace root is where Knoxx is allowed to read/write files.
// DO NOT hard-code /home/err/devel: other machines/users check out elsewhere.
//
// IMPORTANT: never dump process.env in logs (it can contain secrets).
const workspaceRoot =
  process.env.WORKSPACE_ROOT
  || process.env.WORKSPACE_PATH
  || process.env.KNOXX_WORKSPACE_ROOT
  || tryGitTopLevel(knoxxRoot)
  || (() => {
    throw new Error('no workspace root defined (set WORKSPACE_ROOT/WORKSPACE_PATH/KNOXX_WORKSPACE_ROOT)');
  })();
const defaultHostEnvPath = path.join(os.homedir(), '.knoxx', '.env');
const hostEnvPath = process.env.KNOXX_HOST_ENV_PATH || defaultHostEnvPath;
const hostEnv = loadSimpleEnv(hostEnvPath);
const musicLibraryRoot = process.env.KNOXX_MUSIC_LIBRARY_ROOT
  || hostEnv.KNOXX_MUSIC_LIBRARY_ROOT
  || path.join(os.homedir(), 'Music');
const sttNpuPort = process.env.KNOXX_STT_PORT || hostEnv.KNOXX_STT_PORT || '8010';
const chromiumPath = process.env.KNOXX_CHROMIUM_PATH || hostEnv.KNOXX_CHROMIUM_PATH || '/snap/bin/chromium';
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
const shoedelussyUiDir = path.join(shoedelussyDir, 'ui');
const shoedelussyMcpPort = process.env.SHOEDELUSSY_MCP_PORT || hostEnv.SHOEDELUSSY_MCP_PORT || '8790';
const shoedelussyUiPort = process.env.SHOEDELUSSY_UI_PORT || hostEnv.SHOEDELUSSY_UI_PORT || '5175';
const shoedelussyExplicitBaseUrl = process.env.SHOEDELUSSY_MCP_BASE_URL || hostEnv.SHOEDELUSSY_MCP_BASE_URL || '';
const shoedelussyMcpBaseUrl = shoedelussyExplicitBaseUrl || `http://127.0.0.1:${shoedelussyMcpPort}/mcp`;
const shoedelussyUiBaseUrl = process.env.SHOEDELUSSY_UI_URL || hostEnv.SHOEDELUSSY_UI_URL || `http://127.0.0.1:${shoedelussyUiPort}`;
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
const shoedelussyUiShouldRunLocal =
  fs.existsSync(shoedelussyUiDir)
  && shoedelussyPm2Mode !== 'off'
  && (
    shoedelussyPm2Mode === 'force'
    || !isLocalPortBound(shoedelussyUiPort)
  );
const shoedelussyDmxDir = path.join(shoedelussyDir, 'bridges', 'dmx-mcp');
const shoedelussyDmxPort = process.env.SHOEDELUSSY_DMX_PORT || hostEnv.SHOEDELUSSY_DMX_PORT || '3334';
const shoedelussyDmxShouldRunLocal =
  fs.existsSync(shoedelussyDmxDir)
  && shoedelussyPm2Mode !== 'off'
  && (
    shoedelussyPm2Mode === 'force'
    || !isLocalPortBound(shoedelussyDmxPort)
  );

const apps = [
    // ── 1. shadow-cljs watch ──────────────────────────────────────────
    {
      name: 'knoxx-shadow',
      cwd: backendDir,
      script: 'pnpm',
      // Force source maps in the watch build so dist/*.map stays available
      // even if the underlying shadow config drifts.
      args: 'exec shadow-cljs --source-maps watch server-dev',
      watch: false,
      autorestart: true,
      max_restarts: 10,
      restart_delay: 5000,
      env: {
        NODE_ENV: 'development',
      },
    },

    // ── 2. Backend (Node + compiled CLJS) ────────────────────────────
    {
      name: 'knoxx-backend',
      cwd: backendDir,
      // Foreground launcher waits for shadow-cljs dev server + dist-dev/server.js
      // before importing the all-CLJS runtime entrypoint. This encodes the full
      // hot-reload cycle in PM2 instead of relying on an agent's ad hoc startup order.
      script: 'scripts/start-server-dev.cljs',
      interpreter: path.join(backendDir, 'node_modules', '.bin', 'nbb'),
      kill_timeout: 35000,
      listen_timeout: 60000,
      wait_ready: true,
      shutdown_with_message: true,
      // Do not let PM2 watch compiled output or source. shadow-cljs owns hot reload;
      // PM2 only owns the long-running launcher process.
      watch: false,
      // watch: ['dist'],
      // watch_delay: 800,
      // ignore_watch: ['.shadow-cljs', 'node_modules', 'tmp', '.git'],
      autorestart: true,
      max_restarts: 15,
      restart_delay: 3000,
      env: {
        NODE_ENV: 'development',
        HOST: '0.0.0.0',
        PORT: '8000',
        WORKSPACE_ROOT: workspaceRoot,
        CONTRACTS_DIR: contractsDir,
        KNOXX_MUSIC_LIBRARY_ROOT: musicLibraryRoot,
        KNOXX_EXTRA_WORKSPACE_ROOTS: musicLibraryRoot,
        KNOXX_CHROMIUM_PATH: chromiumPath,
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
        // Eta-mu native providers use this to emit body-level prompt_cache_key
        // (and long-retention fields when supported) for session-affinity caching.
        PI_CACHE_RETENTION: 'long',
        KNOXX_PROVIDER_BASE_URLS: hostEnv.KNOXX_PROVIDER_BASE_URLS || 'llamacpp=http://127.0.0.1:8082',
        KNOXX_PROVIDER_AUTH_TOKENS: hostEnv.KNOXX_PROVIDER_AUTH_TOKENS || 'llamacpp=LLAMACPP_API_KEY',
        LLAMACPP_API_KEY: hostEnv.LLAMACPP_API_KEY || 'no-key',
        // OpenPlanner (on host via compose port-forward)
        OPENPLANNER_BASE_URL: 'http://127.0.0.1:7777',
        OPENPLANNER_API_KEY: hostEnv.OPENPLANNER_API_KEY || 'change-me',
        KNOXX_API_KEY: hostEnv.KNOXX_API_KEY || process.env.KNOXX_API_KEY || 'change-me',
        KNOXX_API_KEY_USER_EMAIL: hostEnv.KNOXX_API_KEY_USER_EMAIL || process.env.KNOXX_API_KEY_USER_EMAIL || 'pi@open-hax.local',
        MCP_ENABLED: hostEnv.MCP_ENABLED || process.env.MCP_ENABLED || 'true',
        SHOEDELUSSY_MCP_BASE_URL: shoedelussyMcpBaseUrl,
        SHOEDELUSSY_MCP_TOOL_NAME: hostEnv.SHOEDELUSSY_MCP_TOOL_NAME || 'shoedelussy',
        SHOEDELUSSY_MCP_SHARED_SECRET: hostEnv.SHOEDELUSSY_MCP_SHARED_SECRET || '',
        // Redis + Postgres (compose services forwarded to host)
        REDIS_URL: 'redis://127.0.0.1:6379',
        // Automatic recovered-session resume is opt-in. Keeping this false prevents
        // ad hoc PM2 restarts or shadow-cljs hot reloads from creating duplicate
        // zombie agent jobs. Stale sessions may still be cleaned up by recovery.
        KNOXX_AGENT_AUTO_RESUME_SESSIONS: hostEnv.KNOXX_AGENT_AUTO_RESUME_SESSIONS || 'false',
        KNOXX_SHUTDOWN_GRACE_MS: '25000',
        KNOXX_SHUTDOWN_POLL_MS: '250',
        KNOXX_POLICY_DATABASE_URL: 'postgresql://kms:kms@127.0.0.1:5432/knoxx',
        DATABASE_URL: 'postgresql://kms:kms@127.0.0.1:5432/knoxx',
        // STT (NPU service on host)
        KNOXX_STT_BASE_URL: sttNpuBaseUrl,

        // Bluesky (ATProto)
        BLUESKY_IDENTIFIER: hostEnv.BLUESKY_IDENTIFIER || process.env.BLUESKY_IDENTIFIER || '',
        BLUESKY_APP_PASSWORD: hostEnv.BLUESKY_APP_PASSWORD || process.env.BLUESKY_APP_PASSWORD || '',
        BLUESKY_SERVICE_URL: hostEnv.BLUESKY_SERVICE_URL || process.env.BLUESKY_SERVICE_URL || '',
        BLUESKY_PUBLIC_API_URL: hostEnv.BLUESKY_PUBLIC_API_URL || process.env.BLUESKY_PUBLIC_API_URL || '',

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

    // ── 3. STT (Whisper/OpenVINO sidecar) ────────────────────────────
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
            || 'OpenVINO/whisper-medium.en-int8-ov',
          WHISPER_NPU_COMPILER_TYPE:
            process.env.WHISPER_NPU_COMPILER_TYPE
            || hostEnv.WHISPER_NPU_COMPILER_TYPE
            || 'DRIVER',
          PYTHONUNBUFFERED: '1',
        },
      }]
      : []),

    // ── 4. Frontend (Vite build/watch + shadow-cljs dev server) ──────
    // `pnpm dev` (frontend/package.json) runs:
    //   - vite build --watch          (writes ./dist)
    //   - vite build --watch (bridge) (writes ./dist/bridge)
    //   - shadow-cljs watch app       (serves HTTP on :5173 and writes ./dist/cljs)
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
        // Used by Vite builds (and retained for `pnpm preview`).
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
      APP_URL: hostEnv.SHOEDELUSSY_APP_URL || shoedelussyUiBaseUrl,
      OPENROUTER_API_KEY: hostEnv.SHOEDELUSSY_OPENROUTER_API_KEY || hostEnv.OPENROUTER_API_KEY || '',
      OPENROUTER_MODEL: hostEnv.SHOEDELUSSY_OPENROUTER_MODEL || hostEnv.OPENROUTER_MODEL || 'google/gemini-2.5-flash',
    },
  });
}

if (shoedelussyUiShouldRunLocal) {
  apps.splice(3, 0, {
    name: 'shoedelussy-ui',
    cwd: shoedelussyUiDir,
    script: 'pnpm',
    args: `exec vite --host 127.0.0.1 --port ${shoedelussyUiPort} --strictPort`,
    watch: false,
    autorestart: true,
    max_restarts: 10,
    restart_delay: 5000,
    env: {
      NODE_ENV: 'development',
      VITE_API_URL: '/shoe/api',
    },
  });
}

if (shoedelussyDmxShouldRunLocal) {
  apps.splice(4, 0, {
    name: 'shoedelussy-dmx',
    cwd: shoedelussyDmxDir,
    script: 'pnpm',
    args: 'start',
    watch: false,
    autorestart: true,
    max_restarts: 10,
    restart_delay: 5000,
    env: {
      NODE_ENV: 'development',
      DMX_MCP_PORT: shoedelussyDmxPort,
      DMX_BACKEND: process.env.DMX_BACKEND || hostEnv.DMX_BACKEND || 'simulator',
    },
  });
}

module.exports = {
  apps,
};
