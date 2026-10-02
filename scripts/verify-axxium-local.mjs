// Live local delegation proof. The first-login data lives in an isolated
// backend database and contracts directory; both are removed on exit.
import { execFileSync, spawn } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { once } from 'node:events';
import fs from 'node:fs';
import { createRequire } from 'node:module';
import net from 'node:net';
import os from 'node:os';
import path from 'node:path';
import { setTimeout as delay } from 'node:timers/promises';
import { fileURLToPath } from 'node:url';

const checkout = fs.realpathSync(path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..'));
const workspace = path.dirname(checkout);
const backendPath = path.join(checkout, 'backend');
const apps = JSON.parse(execFileSync('pm2', ['jlist'], { encoding: 'utf8' }));
const app = name => apps.find(candidate => candidate.name === name && candidate.pm2_env.status === 'online');
const backendApp = app('knoxx-axxium-local');
if (!backendApp?.pid) throw new Error('The Knoxx PM2 wrapper is not online');
const children = fs.readFileSync(`/proc/${backendApp.pid}/task/${backendApp.pid}/children`, 'utf8')
  .trim().split(/\s+/).filter(Boolean);
const servingChild = children.find(pid => {
  try {
    const args = fs.readFileSync(`/proc/${pid}/cmdline`, 'utf8').split('\0');
    return fs.realpathSync(`/proc/${pid}/cwd`) === backendPath && args.includes('dist/server.js');
  } catch {
    return false;
  }
});
if (fs.realpathSync(backendApp.pm2_env.pm_cwd) !== workspace || !servingChild ||
    fs.realpathSync(app('knoxx-axxium-ui')?.pm2_env.pm_cwd || '/') !== path.join(checkout, 'frontend') ||
    fs.realpathSync(app('axxium-local')?.pm2_env.pm_cwd || '/') !== path.join(workspace, 'axxium')) {
  throw new Error('The local PM2 processes are not serving this checkout');
}
console.log('PASS PM2 child process serves this Axxium and Knoxx checkout');

// /proc exposes the environment actually passed to the backend child. Never
// print it: it includes private service credentials.
const childEnv = Object.fromEntries(fs.readFileSync(`/proc/${servingChild}/environ`, 'utf8')
  .split('\0').filter(Boolean).map(entry => {
    const equals = entry.indexOf('=');
    return [entry.slice(0, equals), entry.slice(equals + 1)];
  }));
if (!childEnv.MONGODB_URI && !childEnv.OPENPLANNER_MONGODB_URI) {
  throw new Error('The serving backend has no MongoDB connection');
}
const requireBackend = createRequire(path.join(backendPath, 'package.json'));
const { MongoClient } = requireBackend('mongodb');

const adminEnvFile = process.env.AXXIUM_ADMIN_ENV_FILE;
if (adminEnvFile) {
  process.loadEnvFile(adminEnvFile);
} else if (!process.env.AXXIUM_ADMIN_EMAIL || !process.env.AXXIUM_ADMIN_PASSWORD) {
  const defaultEnvFile = path.join(os.homedir(), '.secrets', 'axxium', 'admin.env');
  if (fs.existsSync(defaultEnvFile)) process.loadEnvFile(defaultEnvFile);
}
const email = process.env.AXXIUM_ADMIN_EMAIL;
const password = process.env.AXXIUM_ADMIN_PASSWORD;
if (!email || !password) throw new Error('Local administrator credentials are unavailable');

const liveBase = 'http://127.0.0.1:8003';
const frontend = 'http://127.0.0.1:5176';
let base;
let backend;
let dbName;
let contractsDir;
let mongoUri;
let mongoContainer;
let cookie;
let pendingLogin;
let cleanupPromise;

async function expect(label, url, options, status) {
  const response = await fetch(url, options);
  if (response.status !== status) {
    const detail = await response.text();
    throw new Error(`${label}: expected ${status}, got ${response.status}: ${detail.slice(0, 200)}`);
  }
  console.log(`PASS ${label}`);
  return response;
}

async function availablePort() {
  const listener = net.createServer();
  await new Promise((resolve, reject) => {
    listener.once('error', reject);
    listener.listen(0, '127.0.0.1', resolve);
  });
  const port = listener.address().port;
  await new Promise(resolve => listener.close(resolve));
  return port;
}

async function startIsolatedBackend() {
  const port = await availablePort();
  const mongoPort = await availablePort();
  base = `http://127.0.0.1:${port}`;
  dbName = `knoxx_axxium_verify_${randomUUID().replaceAll('-', '')}`;
  contractsDir = fs.mkdtempSync(path.join(os.tmpdir(), 'knoxx-axxium-verify-'));
  for (const kind of ['roles', 'capabilities']) {
    fs.cpSync(path.join(checkout, 'contracts', kind), path.join(contractsDir, kind),
      { recursive: true });
  }
  mongoUri = `mongodb://127.0.0.1:${mongoPort}/?replicaSet=rs0`;
  mongoContainer = `knoxx-axxium-verify-${randomUUID()}`;
  execFileSync('docker', ['run', '--rm', '-d', '--name', mongoContainer,
    '--network', 'host', '-v', '/data/db',
    'mongo:7', '--replSet', 'rs0', '--bind_ip', '127.0.0.1', '--port', String(mongoPort)],
  { stdio: 'ignore' });
  let mongoReady = false;
  for (let attempt = 0; attempt < 60; attempt++) {
    const client = new MongoClient(`mongodb://127.0.0.1:${mongoPort}/?directConnection=true`,
      { serverSelectionTimeoutMS: 500 });
    try {
      await client.db('admin').command({ ping: 1 });
      await client.db('admin').command({ replSetInitiate: {
        _id: 'rs0', members: [{ _id: 0, host: `127.0.0.1:${mongoPort}` }],
      } });
      mongoReady = true;
      break;
    } catch { await delay(500); }
    finally { await client.close(); }
  }
  if (!mongoReady) throw new Error('Disposable MongoDB did not become ready');
  let primaryReady = false;
  for (let attempt = 0; attempt < 60; attempt++) {
    const client = new MongoClient(mongoUri, { serverSelectionTimeoutMS: 500 });
    try {
      const hello = await client.db('admin').command({ hello: 1 });
      if (hello.isWritablePrimary) { primaryReady = true; break; }
    } catch { /* Replica set is electing its primary. */ }
    finally { await client.close(); }
    await delay(500);
  }
  if (!primaryReady) throw new Error('Disposable MongoDB replica set did not elect a primary');
  backend = spawn(process.execPath, ['dist/server.js'], {
    cwd: backendPath,
    env: { ...childEnv, PORT: String(port), MONGODB_URI: mongoUri,
      OPENPLANNER_MONGODB_URI: mongoUri, MONGODB_DB: dbName,
      OPENPLANNER_MONGODB_DB: dbName, CONTRACTS_DIR: contractsDir,
      WORKSPACE_ROOT: workspace, KNOXX_BASE_URL: base,
      KNOXX_PUBLIC_BASE_URL: base, KNOXX_DISABLE_EVENT_RUNTIMES: 'true' },
    stdio: 'ignore',
  });
  for (let attempt = 0; attempt < 60; attempt++) {
    if (backend.exitCode !== null) {
      throw new Error(`The isolated Knoxx backend exited before readiness: ${backend.exitCode}`);
    }
    try {
      const response = await fetch(`${base}/api/auth/config`, { signal: AbortSignal.timeout(1000) });
      if (response.ok) {
        console.log('PASS isolated Knoxx backend started with a disposable database');
        return;
      }
    } catch { /* Backend is still starting. */ }
    await delay(500);
  }
  throw new Error('The isolated Knoxx backend did not become ready');
}

async function revokeSession() {
  if (!cookie) return;
  const logout = await fetch(`${base}/api/auth/logout`,
    { method: 'POST', headers: { cookie }, signal: AbortSignal.timeout(10000) });
  if (!logout.ok) throw new Error(`Verification session logout failed: HTTP ${logout.status}`);
  const context = await fetch(`${base}/api/auth/context`,
    { headers: { cookie }, signal: AbortSignal.timeout(10000) });
  if (context.status !== 401) {
    throw new Error(`Verification cookie still authenticates after logout: HTTP ${context.status}`);
  }
  console.log('PASS verification session revoked; captured cookie returns 401');
}

async function stopBackend() {
  if (!backend || backend.exitCode !== null || backend.signalCode !== null) return;
  const exited = once(backend, 'exit');
  backend.kill('SIGTERM');
  await Promise.race([exited, delay(5000)]);
  if (backend.exitCode === null && backend.signalCode === null) {
    backend.kill('SIGKILL');
    await exited;
  }
}

function cleanup() {
  cleanupPromise ??= (async () => {
    const errors = [];
    try { await revokeSession(); } catch (error) { errors.push(error); }
    try { await stopBackend(); } catch (error) { errors.push(error); }
    if (mongoContainer) {
      try {
        execFileSync('docker', ['stop', '--time', '5', mongoContainer], { stdio: 'ignore' });
        console.log('PASS disposable Knoxx identity database removed with its container');
      } catch (error) { errors.push(error); }
    }
    if (contractsDir) {
      try { fs.rmSync(contractsDir, { recursive: true, force: true }); }
      catch (error) { errors.push(error); }
    }
    if (errors.length) throw new AggregateError(errors, errors.map(error => error.message).join('; '));
  })();
  return cleanupPromise;
}

let interrupted = false;
for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    if (interrupted) return;
    interrupted = true;
    void (async () => {
      if (pendingLogin) await pendingLogin.catch(() => {});
      await cleanup();
      process.exit(signal === 'SIGINT' ? 130 : 143);
    })().catch(error => {
      console.error(`Verification cleanup after ${signal} failed: ${error.message}`);
      process.exit(1);
    });
  });
}

let verificationError;
try {
  const liveConfig = await (await expect('running Knoxx advertises its auth API',
    `${liveBase}/api/auth/config`, {}, 200)).json();
  if (liveConfig.identityProvider !== 'axxium') throw new Error('Running Knoxx does not use Axxium');
  await startIsolatedBackend();
  const config = await (await expect('isolated Knoxx advertises its auth API',
    `${base}/api/auth/config`, {}, 200)).json();
  if (config.identityProvider !== 'axxium' || config.localLoginUrl !== '/api/auth/local/login') {
    throw new Error('Knoxx did not select Axxium authority');
  }
  console.log('PASS Knoxx delegates password identity to Axxium');
  await expect('anonymous context is refused', `${base}/api/auth/context`, {}, 401);
  const headers = { 'content-type': 'application/json' };
  const badPassword = { method: 'POST', headers,
    body: JSON.stringify({ email, password: `${password}-wrong` }) };
  await expect('Axxium refuses a wrong password through Knoxx',
    `${base}/api/auth/local/login`, badPassword, 401);
  await expect('Knoxx signup is closed when Axxium is authoritative',
    `${base}/api/auth/signup`, badPassword, 409);
  pendingLogin = (async () => {
    const login = await expect('Axxium credentials establish a Knoxx session',
      `${base}/api/auth/local/login`, { method: 'POST', headers,
        body: JSON.stringify({ email, password }), signal: AbortSignal.timeout(10000) }, 200);
    cookie = login.headers.get('set-cookie')?.split(';')[0];
  })();
  await pendingLogin;
  if (!cookie) throw new Error('Knoxx did not create a session cookie');
  const context = await (await expect('Knoxx resolves the delegated actor context',
    `${base}/api/auth/context`, { headers: { cookie } }, 200)).json();
  if (context.user?.email !== email) throw new Error('Delegated context belongs to another user');
  console.log('PASS delegated context retains the expected user');
  await expect('frontend serves the delegated login page', frontend, {}, 200);
  await expect('frontend proxies running Knoxx auth config', `${frontend}/api/auth/config`, {}, 200);
  const axxiumConfig = await (await expect('Axxium auth configuration responds',
    'http://127.0.0.1:8788/api/auth/config', {}, 200)).json();
  if (axxiumConfig.googleEnabled !== true) throw new Error('Google sign-in is not enabled in Axxium');
  console.log('PASS Axxium advertises its registered Google sign-in');
  console.log('WARN Automated verification does not complete Google account consent');
} catch (error) {
  verificationError = error;
}

let cleanupError;
try { await cleanup(); } catch (error) { cleanupError = error; }
if (verificationError && cleanupError) {
  throw new AggregateError([verificationError, cleanupError],
    `Verification and cleanup both failed: ${verificationError.message}; ${cleanupError.message}`);
}
if (verificationError) throw verificationError;
if (cleanupError) throw cleanupError;
