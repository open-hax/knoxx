// Live local delegation proof. It uses the existing private local administrator
// and logs out the session it creates. No repository fixtures are needed.
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const checkout = fs.realpathSync(path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..'));
const workspace = path.dirname(checkout);
const apps = JSON.parse(execFileSync('pm2', ['jlist'], { encoding: 'utf8' }));
const app = name => apps.find(candidate => candidate.name === name && candidate.pm2_env.status === 'online');
if (fs.realpathSync(app('knoxx-axxium-local')?.pm2_env.pm_cwd || '/') !== workspace ||
    fs.realpathSync(app('knoxx-axxium-ui')?.pm2_env.pm_cwd || '/') !== path.join(checkout, 'frontend') ||
    fs.realpathSync(app('axxium-local')?.pm2_env.pm_cwd || '/') !== path.join(workspace, 'axxium')) {
  throw new Error('The local PM2 processes are not serving this checkout');
}
console.log('PASS PM2 serves this Axxium and Knoxx checkout');

process.loadEnvFile('/home/err/.secrets/axxium/admin.env');
const email = process.env.AXXIUM_ADMIN_EMAIL;
const password = process.env.AXXIUM_ADMIN_PASSWORD;
if (!email || !password) throw new Error('Local administrator credentials are unavailable');
const base = 'http://127.0.0.1:8003';
const frontend = 'http://127.0.0.1:5176';
let cookie;

async function expect(label, url, options, status) {
  const response = await fetch(url, options);
  if (response.status !== status) {
    const detail = await response.text();
    throw new Error(`${label}: expected ${status}, got ${response.status}: ${detail.slice(0, 200)}`);
  }
  console.log(`PASS ${label}`);
  return response;
}

try {
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
  const login = await expect('Axxium credentials establish a Knoxx session',
    `${base}/api/auth/local/login`, { method: 'POST', headers,
      body: JSON.stringify({ email, password }) }, 200);
  cookie = login.headers.get('set-cookie')?.split(';')[0];
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
} finally {
  if (cookie) {
    await fetch(`${base}/api/auth/logout`, { method: 'POST', headers: { cookie } });
    console.log('Cleaned verification session');
  }
}
