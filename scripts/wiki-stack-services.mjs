import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import { createWriteStream } from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { createHash } from 'node:crypto';
import { spawn, execFileSync } from 'node:child_process';

const digest = value => createHash('sha256').update(value).digest('hex');
const pause = milliseconds => new Promise(resolve => setTimeout(resolve, milliseconds));

async function hashBuilds(repo) {
  const files = ['backend/dist/server.js', 'frontend/dist/index.html', 'frontend/dist/app.css',
    'frontend/dist/cljs/app.js', 'frontend/dist/bridge/knoxx-app-bridge.es.js',
    'frontend/dist/bridge/knoxx-frontend-bridge.es.js'];
  const hashes = {};
  for (const file of files) {
    const bytes = await fs.readFile(path.join(repo, file));
    assert(bytes.length, `Build output is empty: ${file}`);
    hashes[file] = { sha256: digest(bytes), bytes: bytes.length };
  }
  return { sourceHead: execFileSync('git', ['rev-parse', 'HEAD'], { cwd: repo, encoding: 'utf8' }).trim(), files: hashes };
}

function logStream(stream, destination, secrets) {
  let pending = '';
  const write = line => {
    for (const secret of secrets) line = line.split(secret).join('[redacted]');
    destination.write(line);
  };
  stream.setEncoding('utf8');
  stream.on('data', chunk => {
    pending += chunk;
    const boundary = pending.lastIndexOf('\n');
    if (boundary >= 0) { write(pending.slice(0, boundary + 1)); pending = pending.slice(boundary + 1); }
  });
  stream.on('end', () => { if (pending) write(pending); });
}

function managedProcess(command, args, cwd, env, logFile) {
  const output = createWriteStream(logFile, { flags: 'a', mode: 0o600 });
  const child = spawn(command, args, { cwd, env, detached: true, stdio: ['ignore', 'pipe', 'pipe'] });
  const secrets = Object.entries(env).filter(([key, value]) => /password|secret|token|api_key/i.test(key)
    && typeof value === 'string' && value.length >= 4).map(([, value]) => value);
  logStream(child.stdout, output, secrets);
  logStream(child.stderr, output, secrets);
  let failure;
  let ended = false;
  child.once('error', error => { failure = error; });
  const exited = new Promise(resolve => child.once('close', (code, signal) => {
    ended = true;
    output.end();
    resolve({ code, signal });
  }));
  return {
    child, logFile,
    check() {
      if (failure) throw failure;
      if (ended || child.exitCode !== null || child.signalCode !== null) {
        throw new Error(`Owned service exited before readiness; inspect ${logFile}`);
      }
    },
    async stop() {
      if (ended) return;
      const signal = name => { try { process.kill(-child.pid, name); } catch (error) { if (error.code !== 'ESRCH') throw error; } };
      signal('SIGTERM');
      await Promise.race([exited, pause(5000)]);
      if (!ended) { signal('SIGKILL'); await exited; }
    },
  };
}

async function ready(url, service, timeout = 60_000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    service.check();
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(2000) });
      if (response.ok) { const body = await response.text(); service.check(); return body; }
      await response.arrayBuffer();
    } catch (error) {
      if (!['TimeoutError', 'TypeError', 'AbortError'].includes(error.name)) throw error;
    }
    await pause(250);
  }
  throw new Error(`Readiness timed out at ${url}; inspect ${service.logFile}`);
}

async function prepareDirectories(repo, fixtureDirectory, outputDir) {
  for (const directory of [fixtureDirectory, outputDir, 'state', 'content', 'agent']) {
    await fs.mkdir(path.isAbsolute(directory) ? directory : path.join(fixtureDirectory, directory), { recursive: true });
  }
  const contracts = path.join(fixtureDirectory, 'contracts');
  try { await fs.access(contracts); }
  catch (error) {
    if (error.code !== 'ENOENT') throw error;
    await fs.cp(path.join(repo, 'contracts'), contracts, { recursive: true, dereference: false, errorOnExist: true, force: false });
  }
  return contracts;
}

function environment(config, contracts, generation, embedding) {
  const { fixtureDirectory, backendPort, password, username, email, publicBaseUrl, extraEnv = {} } = config;
  const state = path.join(fixtureDirectory, 'state');
  return {
    ...process.env, NODE_ENV: 'production', HOST: '127.0.0.1', PORT: String(backendPort),
    KNOXX_PUBLIC_BASE_URL: publicBaseUrl,
    KNOXX_POLICY_PROVIDER: 'edn', KNOXX_RUN_PROVIDER: 'edn', KNOXX_THREAD_PROVIDER: 'edn',
    KNOXX_CACHE_PROVIDER: 'edn', KNOXX_MCP_OAUTH_PROVIDER: 'edn',
    KNOXX_WIKI_DIRECTORY: state, KNOXX_AXXIUM_DIRECTORY: path.join(state, 'identity'),
    KNOXX_OPENPLANNER_CLIENT_MODE: 'edn', KNOXX_OPENPLANNER_DIRECTORY: path.join(state, 'openplanner'),
    KNOXX_MAILBOX_DIRECTORY: path.join(state, 'mailbox'),
    KNOXX_BOOTSTRAP_SYSTEM_ADMIN_USERNAME: username,
    KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL: email || `${username}@example.test`,
    KNOXX_BOOTSTRAP_SYSTEM_ADMIN_PASSWORD: password,
    KNOXX_CONTRACTS_DIR: contracts, CONTRACTS_DIR: contracts,
    KNOXX_PUBLICATION_CONTENT_ROOT: path.join(fixtureDirectory, 'content'),
    KNOXX_AGENT_DIR: path.join(fixtureDirectory, 'agent'), MCP_ENABLED: 'true',
    KNOXX_WIKI_MODEL_PROVIDER: 'openai-compatible', KNOXX_WIKI_MODEL_BASE_URL: generation.baseUrl,
    KNOXX_WIKI_MODEL: generation.model,
    EMBED_PROVIDER_BASE_URL: embedding.baseUrl, EMBED_PROVIDER_MODEL: embedding.model,
    EMBED_PROVIDER_DIMENSIONS: String(embedding.dimensions),
    ...extraEnv,
  };
}

/**
 * Start only this tour's model servers and built Knoxx processes. The caller
 * owns fixture contents, browser, artifact server and evidence capture. stop()
 * stops owned process groups and model handles; it never deletes any directory.
 * The returned env contains bootstrap credentials: do not serialize it.
 */
export async function startServices(options) {
  const config = { ...options };
  for (const key of ['repo', 'fixtureDirectory', 'outputDir']) assert(path.isAbsolute(config[key] || ''), `${key} must be absolute`);
  for (const key of ['frontendPort', 'backendPort']) assert(Number.isInteger(config[key]) && config[key] > 0 && config[key] < 65536, `${key} must be an explicit port`);
  assert.notEqual(config.frontendPort, config.backendPort);
  assert(typeof config.password === 'string' && config.password.length >= 12, 'An explicit bootstrap password of at least 12 characters is required');
  assert(typeof config.username === 'string' && config.username.length, 'An explicit bootstrap username is required');
  config.publicBaseUrl ||= `http://localhost:${config.frontendPort}`;
  const baseUrl = new URL(config.publicBaseUrl).origin;
  assert.equal(baseUrl, `http://localhost:${config.frontendPort}`, 'The browser tour uses localhost for its real WebAuthn relying party');
  const backendUrl = `http://127.0.0.1:${config.backendPort}`;
  const builds = await hashBuilds(config.repo);
  const contracts = await prepareDirectories(config.repo, config.fixtureDirectory, config.outputDir);
  const owned = [];
  let stopped = false;
  const stop = async () => {
    if (stopped) return;
    stopped = true;
    const errors = [];
    for (const resource of [...owned].reverse()) {
      try { await (resource.stop ? resource.stop() : resource.close()); } catch (error) { errors.push(error); }
    }
    if (errors.length) throw new AggregateError(errors, 'Owned service cleanup failed');
  };
  try {
    const devtools = path.resolve(config.repo, '../devtools');
    const { startGenerationServer } = await import(pathToFileURL(path.join(devtools, 'generation-server.mjs')));
    const { startEmbeddingServer } = await import(pathToFileURL(path.join(devtools, 'embedding-server.mjs')));
    const cacheDir = config.cacheDir || process.env.FORESIGHT_MODEL_CACHE;
    assert(path.isAbsolute(cacheDir || ''), 'Set an explicit absolute warmed model cache; startup never downloads models');
    const generation = await startGenerationServer({ port: config.modelPort || 0, cacheDir, model: 'onnx-community/Qwen2.5-0.5B-Instruct' });
    owned.push(generation);
    const embedding = await startEmbeddingServer({ port: 0, cacheDir });
    owned.push(embedding);
    const env = environment(config, contracts, generation, embedding);
    const backend = managedProcess(process.execPath, ['dist/server.js'], path.join(config.repo, 'backend'), env,
      path.join(config.outputDir, 'backend.log'));
    owned.push(backend);
    const registry = JSON.parse(await ready(`${backendUrl}/api/auth/config`, backend));
    assert.equal(registry.identityProvider, 'axxium', 'Readiness must expose the actual Axxium registry');
    assert(Array.isArray(registry.methods), 'Readiness must expose identity method availability');
    assert(registry.methods.some(method => method.id === 'password' && method.available === true),
      'The configured bootstrap password method must be available');
    const frontend = managedProcess('pnpm', ['exec', 'vite', 'preview', '--host', 'localhost', '--port', String(config.frontendPort)],
      path.join(config.repo, 'frontend'), { ...env, VITE_KNOXX_BACKEND_URL: backendUrl }, path.join(config.outputDir, 'frontend.log'));
    owned.push(frontend);
    await ready(baseUrl, frontend);
    for (const asset of ['index.html', 'app.css', 'cljs/app.js']) {
      const response = await fetch(new URL(asset, `${baseUrl}/`));
      assert(response.ok, `Built frontend asset is not served: ${asset}`);
      assert.equal(digest(Buffer.from(await response.arrayBuffer())), builds.files[`frontend/dist/${asset}`].sha256,
        `The browser must receive the preflight-hashed build: ${asset}`);
    }
    await fs.writeFile(path.join(config.outputDir, 'service-builds.json'), `${JSON.stringify({ ...builds, baseUrl, backendUrl,
      generation: { model: generation.model, baseUrl: generation.baseUrl },
      embedding: { model: embedding.model, baseUrl: embedding.baseUrl, dimensions: embedding.dimensions } }, null, 2)}\n`);
    return { baseUrl, backendUrl, modelUrl: generation.baseUrl, embeddingUrl: embedding.baseUrl, env, builds, stop };
  } catch (error) {
    try { await stop(); } catch (cleanupError) { throw new AggregateError([error, cleanupError], 'Startup and owned cleanup failed'); }
    throw error;
  }
}
