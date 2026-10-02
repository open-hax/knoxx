// Live local delegation proof. It uses the existing private local administrator
// and logs out the session it creates. No repository fixtures are needed.
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import { createRequire } from 'node:module';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseEnv } from 'node:util';

const checkout = fs.realpathSync(path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..'));
const workspace = path.dirname(checkout);
const apps = JSON.parse(execFileSync('pm2', ['jlist'], { encoding: 'utf8' }));
const app = name => apps.find(candidate => candidate.name === name && candidate.pm2_env.status === 'online');
const backendApp = app('knoxx-axxium-local');
const backendPath = path.join(checkout, 'backend');
if (!backendApp?.pid) throw new Error('The Knoxx PM2 wrapper is not online');
const children = fs.readFileSync(`/proc/${backendApp.pid}/task/${backendApp.pid}/children`, 'utf8')
  .trim().split(/\s+/).filter(Boolean);
const servesBackend = children.some(pid => {
  try {
    const args = fs.readFileSync(`/proc/${pid}/cmdline`, 'utf8').split('\0');
    return fs.realpathSync(`/proc/${pid}/cwd`) === backendPath && args.includes('dist/server.js');
  } catch {
    return false;
  }
});
if (fs.realpathSync(backendApp?.pm2_env.pm_cwd || '/') !== workspace ||
    !servesBackend ||
    fs.realpathSync(app('knoxx-axxium-ui')?.pm2_env.pm_cwd || '/') !== path.join(checkout, 'frontend') ||
    fs.realpathSync(app('axxium-local')?.pm2_env.pm_cwd || '/') !== path.join(workspace, 'axxium')) {
  throw new Error('The local PM2 processes are not serving this checkout');
}
console.log('PASS PM2 child process serves this Axxium and Knoxx checkout');

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
const base = 'http://127.0.0.1:8003';
const frontend = 'http://127.0.0.1:5176';
let cookie;
let cleanupPromise;
let pendingLogin;

async function requireExistingIdentity() {
  const localEnv = parseEnv(fs.readFileSync(path.join(os.homedir(), '.secrets', 'knoxx', 'local.env'), 'utf8'));
  const uri = backendApp.pm2_env.MONGODB_URI || localEnv.MONGODB_URI ||
    backendApp.pm2_env.OPENPLANNER_MONGODB_URI || localEnv.OPENPLANNER_MONGODB_URI;
  const dbName = backendApp.pm2_env.MONGODB_DB || localEnv.MONGODB_DB ||
    backendApp.pm2_env.OPENPLANNER_MONGODB_DB || localEnv.OPENPLANNER_MONGODB_DB || 'openplanner';
  if (!uri) throw new Error('Cannot verify the backend identity store before signing in');
  const requireBackend = createRequire(path.join(backendPath, 'package.json'));
  const { MongoClient } = requireBackend('mongodb');
  const client = new MongoClient(uri, { serverSelectionTimeoutMS: 5000 });
  try {
    const db = client.db(dbName);
    const user = await db.collection('knoxx_users').findOne({ email: email.toLowerCase(), auth_provider: 'axxium' });
    const membership = user && await db.collection('knoxx_memberships')
      .findOne({ user_id: user.user_id, status: 'active' });
    if (!membership) {
      throw new Error('The verification actor has no existing Knoxx membership; provision it separately');
    }
    console.log('PASS verification actor and membership existed before this run');
  } finally {
    await client.close();
  }
}

async function cleanupSession() {
  if (!cookie) return;
  cleanupPromise ??= (async () => {
    const logout = await fetch(`${base}/api/auth/logout`, { method: 'POST', headers: { cookie } });
    if (!logout.ok) throw new Error(`Verification session logout failed: HTTP ${logout.status}`);
    const context = await fetch(`${base}/api/auth/context`, { headers: { cookie } });
    if (context.status !== 401) {
      throw new Error(`Verification cookie still authenticates after logout: HTTP ${context.status}`);
    }
    console.log('PASS verification session revoked; captured cookie returns 401');
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
      await cleanupSession();
      process.exit(signal === 'SIGINT' ? 130 : 143);
    })().catch(error => {
        console.error(`Session cleanup after ${signal} failed: ${error.message}`);
        process.exit(1);
      });
  });
}

async function expect(label, url, options, status) {
  const response = await fetch(url, options);
  if (response.status !== status) {
    const detail = await response.text();
    throw new Error(`${label}: expected ${status}, got ${response.status}: ${detail.slice(0, 200)}`);
  }
  console.log(`PASS ${label}`);
  return response;
}

let verificationError;
try {
  await requireExistingIdentity();
  const config = await (await expect('Knoxx advertises its configured auth API',
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
    return login;
  })();
  await pendingLogin;
  if (!cookie) throw new Error('Knoxx did not create a session cookie');
  const context = await (await expect('Knoxx resolves the delegated actor context',
    `${base}/api/auth/context`, { headers: { cookie } }, 200)).json();
  if (context.user?.email !== email) throw new Error('Delegated context belongs to another user');
  console.log('PASS delegated context retains the expected user');
  await expect('frontend serves the delegated login page', frontend, {}, 200);
  await expect('frontend proxies Knoxx auth config', `${frontend}/api/auth/config`, {}, 200);
  const axxiumConfig = await (await expect('Axxium auth configuration responds',
    'http://127.0.0.1:8788/api/auth/config', {}, 200)).json();
  if (axxiumConfig.googleEnabled !== true) throw new Error('Google sign-in is not enabled in Axxium');
  console.log('PASS Axxium advertises its registered Google sign-in');
  console.log('WARN Automated verification does not complete Google account consent');
} catch (error) {
  verificationError = error;
}

let logoutError;
if (cookie) {
  try {
    await cleanupSession();
  } catch (error) {
    logoutError = error;
  }
}
if (verificationError && logoutError) {
  throw new AggregateError([verificationError, logoutError],
    `Verification and session cleanup both failed: ${verificationError.message}; ${logoutError.message}`);
}
if (verificationError) throw verificationError;
if (logoutError) throw logoutError;
