/** Same-workspace, real-process browser verification. Every account and ledger is disposable. */
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import http from 'node:http';
import {createRequire} from 'node:module';
import {fileURLToPath, pathToFileURL} from 'node:url';
import {randomBytes, randomUUID} from 'node:crypto';
import {startServices} from './wiki-stack-services.mjs';
import {annotatedScreenshot, tour} from './wiki-browser-tour.mjs';
import {identityTour} from './identity-browser-tour.mjs';
import {adminIdentityTour} from './admin-identity-browser-tour.mjs';
import {mailTour} from './mail-browser-tour.mjs';
import {translationTour, verifySourceUnaccepted} from './wiki-translation-tour.mjs';

const repo = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const require = createRequire(path.join(repo, 'backend/package.json'));
const {chromium} = require(path.join(process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES, 'playwright-core'));
const outputDir = path.resolve(process.env.WIKI_EVIDENCE_DIR || path.join(repo, 'docs/verification/screenshots', `wiki-${Date.now()}`));
const fixtureDirectory = await fs.mkdtemp(path.join(os.tmpdir(), 'knoxx-wiki-stack-'));
const username = 'wiki-owner', email = 'owner@wiki.test', password = randomBytes(24).toString('base64url');
const bootOnly = process.argv.includes('--boot-only');
const frontendPort = parseInt(process.env.WIKI_FRONTEND_PORT || '8311',10);
const backendPort = parseInt(process.env.WIKI_BACKEND_PORT || '8312',10);
const evidence = {startedAt: new Date().toISOString(), mode: bootOnly ? 'boot-only' : 'full', checks: [], screenshots: [], failures: []};
let services, browser, artifactServer, client, page;
await fs.mkdir(outputDir, {recursive:true});
await fs.writeFile(path.join(outputDir, 'fixture-owner.json'), `${JSON.stringify({fixtureDirectory, owner:'verify-wiki-stack', pid:process.pid})}\n`, {mode:0o600});
const check = (description, details = {}) => { evidence.checks.push({description, ...details}); console.log(`PASS ${description}`); };

async function seedContracts() {
  const contracts = path.join(fixtureDirectory, 'contracts');
  await fs.cp(path.join(repo, 'contracts'), contracts, {recursive:true, dereference:false});
  await fs.writeFile(path.join(contracts, 'namespaces/sandbox-wiki.edn'), `{:namespace :sandbox.wiki
 :resources [{:garden/id :sandbox.wiki/research :garden/title "Sandbox research wiki"
              :garden/status :active :garden/locales [:en :es]}]}\n`);
  const model = await fs.readFile(path.join(repo, 'backend/test/fixtures/translation-local-model-contracts/models/qwen_05.edn'));
  await fs.mkdir(path.join(contracts, 'models'), {recursive:true});
  await fs.writeFile(path.join(contracts, 'models/sandbox_qwen.edn'), model);
  const agentFile = path.join(contracts, 'agents/publication_translator.edn');
  const source = await fs.readFile(agentFile, 'utf8');
  assert(source.includes(':model "gemma4:31b"'), 'Fixture must identify the existing model assignment before replacing it');
  await fs.writeFile(agentFile, source.replace(':model "gemma4:31b"', ':model "onnx-community/Qwen2.5-0.5B-Instruct"').replace(':thinking :medium', ':thinking :off'));
}

async function artifactService(readManifest) {
  const root = await fs.realpath(path.join(fixtureDirectory, 'content'));
  const server = http.createServer(async (request, response) => {
    try {
      if (!['GET', 'HEAD'].includes(request.method)) { response.writeHead(405).end(); return; }
      const url = new URL(request.url, 'http://localhost');
      const routes = await readManifest(root);
      const matches = routes.filter(route => route.path === url.pathname);
      if (matches.length !== 1) { response.writeHead(404).end('Not published'); return; }
      const route = matches[0];
      const file = await fs.realpath(path.resolve(root, route.artifact));
      const relative = path.relative(root, file);
      assert(relative && !relative.startsWith('..') && !path.isAbsolute(relative), 'Manifest artifact must remain inside its content root');
      const bytes = await fs.readFile(file);
      response.writeHead(200, {'Content-Type': `${route.mediaType}; charset=utf-8`, 'Content-Length': bytes.length,
        'X-Content-Type-Options':'nosniff', 'Cache-Control':'no-store'});
      response.end(request.method === 'HEAD' ? undefined : bytes);
    } catch (error) {
      evidence.failures.push({stage:'artifact-serving', message:error.message});
      response.writeHead(error.code === 'ENOENT' ? 404 : 500).end('Artifact unavailable');
    }
  });
  await new Promise((resolve, reject) => { server.once('error', reject); server.listen(0, '127.0.0.1', resolve); });
  return {server, baseUrl:`http://127.0.0.1:${server.address().port}`};
}

async function mcpClient(page) {
  const cookies = await page.context().cookies();
  const token = cookies.find(cookie => cookie.name === 'axxium_session')?.value;
  assert(token, 'A real Axxium session must authorize the delegated agent');
  const {Client} = await import(pathToFileURL(require.resolve('@modelcontextprotocol/sdk/client/index.js')));
  const {StreamableHTTPClientTransport} = await import(pathToFileURL(require.resolve('@modelcontextprotocol/sdk/client/streamableHttp.js')));
  const connected = new Client({name:'foresight-browser-verification', version:'1.0.0'});
  await connected.connect(new StreamableHTTPClientTransport(new URL('/mcp', services.baseUrl),
    {requestInit:{headers:{Authorization:`Bearer ${token}`}}}));
  const tools = await connected.listTools();
  for (const name of ['wiki_read', 'wiki_save', 'wiki_review', 'actors_send-message']) assert(tools.tools.some(tool => tool.name === name), `${name} is absent from the actual MCP catalog`);
  const mailboxTool = tools.tools.find(tool => tool.name === 'actors_send-message');
  assert.equal(mailboxTool.inputSchema.properties?.operation_id?.type, 'string',
    'The delegated Mail command must expose its stable caller operation identity');
  check('Real MCP initialization exposes the same Wiki and Mail commands as the authorized human UI', {principalMode:'human-delegated Axxium session'});
  return connected;
}

async function invokeWiki(name, args) {
  const result = await client.callTool({name, arguments:args});
  assert(!result.isError, `MCP ${name} refused: ${JSON.stringify(result.content)}`);
  return result.structuredContent || result.details || JSON.parse(result.content.find(part => part.type === 'text').text);
}

async function agentCommand(command) {
  const snapshot = await invokeWiki('wiki_read', {document:command.document});
  if (command.kind === 'source') return invokeWiki('wiki_save', {document:command.document,
    operation_id:randomUUID(), expected_revision:snapshot.revision, content:command.content});
  return invokeWiki('wiki_review', {document:command.document, operation_id:randomUUID(), revision:snapshot.revision,
    source_locale:snapshot.source_locale, expected_head:snapshot.head, action:command.action, notes:command.notes});
}

async function login(page) {
  await page.goto(new URL('/cms', services.baseUrl).href);
  await page.getByLabel('Username or email', {exact:true}).fill(username);
  await page.getByLabel('Password', {exact:true}).fill(password);
  const waiting = page.waitForResponse(response => new URL(response.url()).pathname === '/api/auth/local/login' && response.request().method() === 'POST');
  await page.getByRole('button', {name:'Sign in with password', exact:true}).click();
  const response = await waiting;
  assert.equal(response.status(), 200, `Real password login failed (${response.status()})`);
  await page.getByRole('heading', {name:'Team wiki', exact:true}).waitFor();
  const inventory = await page.request.get('/api/publications/documents');
  assert.equal(inventory.status(), 200, `Authenticated inventory failed: ${await inventory.text()}`);
  await page.waitForFunction(() => [...document.querySelectorAll('button')]
    .some(button => button.textContent.trim() === 'New page' && !button.disabled));
  check('Axxium password login reaches the rendered Wiki');
}

let cleanupPromise;
async function cleanup() {
  if (cleanupPromise) return cleanupPromise;
  cleanupPromise = (async () => {
    for (const close of [async()=>client?.close(), async()=>browser?.close(),
      async()=>artifactServer && new Promise(resolve=>artifactServer.close(resolve)), async()=>services?.stop()]) {
      try {await close();} catch(error) {evidence.failures.push({stage:'cleanup',message:error.message});process.exitCode=1;}
    }
    evidence.finishedAt=new Date().toISOString();
    await fs.writeFile(path.join(outputDir,'result.json'),`${JSON.stringify(evidence,null,2)}\n`);
    await fs.rm(fixtureDirectory,{recursive:true,force:true});
    console.log(`Evidence: ${outputDir}`);
  })();
  return cleanupPromise;
}
const interrupt = async signal => {
  evidence.completed=false; evidence.failures.push({stage:'signal',message:signal});
  try {await cleanup();} finally {process.exit(signal === 'SIGINT' ? 130 : 143);}
};
const onInterrupt = () => void interrupt('SIGINT');
const onTerminate = () => void interrupt('SIGTERM');
const onFatal = error => {
  evidence.completed=false;
  evidence.failures.push({stage:'unhandled-async-error',message:error?.message || String(error)});
  process.exitCode=1;
  void cleanup().then(()=>process.exit(1),cleanupError=>{
    console.error(`FAIL cleanup after fatal error: ${cleanupError.message}`); process.exit(1);
  });
};
process.once('SIGINT', onInterrupt);
process.once('SIGTERM', onTerminate);
process.once('unhandledRejection', onFatal);
try {
  await seedContracts();
  services = await startServices({repo, fixtureDirectory, outputDir, username, email, password,
    frontendPort, backendPort,
    extraEnv:{KNOXX_DISABLE_REQUEST_LOGGING:'true', KNOXX_WORKSPACE_ROOT:fixtureDirectory, WORKSPACE_ROOT:fixtureDirectory,
      KNOXX_BASE_URL:`http://127.0.0.1:${backendPort}`, PROXX_BASE_URL:'', OPENPLANNER_MCP_BASE_URL:'',
      MONGODB_URI:'mongodb://127.0.0.1:1/?serverSelectionTimeoutMS=1000'}});
  check('Real model services and compiled backend/frontend are ready', {builds:services.builds});
  const unauthorized = await fetch(new URL('/api/publications/documents', services.baseUrl));
  assert.equal(unauthorized.status,401); check('Anonymous Wiki inventory is refused with 401');
  browser = await chromium.launch({executablePath:process.env.FORESIGHT_BROWSER_EXECUTABLE, headless:true, args:['--no-sandbox']});
  const context = await browser.newContext({baseURL:services.baseUrl, viewport:{width:1440,height:1000}});
  page = await context.newPage();
  page.setDefaultTimeout(30_000);
  page.on('pageerror', error => evidence.failures.push({stage:'browser', message:error.message}));
  const shot = async (name, caption, selectors=[]) => {
    const record = await annotatedScreenshot(page,outputDir,name,caption,selectors); evidence.screenshots.push(record); return record;
  };
  await login(page);
  await shot('00-live-wiki', 'This is the current compiled Knoxx, authenticated by Axxium and backed by canonical Clio ledgers.');
  if (!bootOnly) {
    const openpgp = await import(pathToFileURL(require.resolve('openpgp')));
    const keys = await openpgp.generateKey({type:'ecc', curve:'ed25519Legacy', userIDs:[{name:'Disposable Wiki Owner', email}], format:'armored'});
    const privateKey = await openpgp.readPrivateKey({armoredKey:keys.privateKey});
    const publicKey = await openpgp.readKey({armoredKey:keys.publicKey});
    evidence.identity = await identityTour(page, {baseUrl:services.baseUrl, username, email, password, shot,
      pgpPublicKey:keys.publicKey, pgpFingerprint:publicKey.getFingerprint(),
      signPgpChallenge:async text => openpgp.sign({message:await openpgp.createMessage({text}), signingKeys:privateKey, detached:true, format:'armored'}),
      signup:{username:'research-member',email:'research-member@wiki.test',password:randomBytes(24).toString('base64url')}});
    check('Real passkey, PGP, signup and revocation browser ceremonies completed');
    const verifiedContext = await (await page.request.get('/api/auth/context')).json();
    evidence.admin = await adminIdentityTour(page, {baseUrl:services.baseUrl, shot,
      principalId:verifiedContext.actor.id, principalEmail:email});
    client = await mcpClient(page);
    evidence.mail = await mailTour(page, {baseUrl:services.baseUrl, principalId:verifiedContext.actor.id, shot,
      agentSend:async args => {
        const result = await client.callTool({name:'actors_send-message', arguments:args});
        assert(!result.isError, `MCP Mail command refused: ${JSON.stringify(result.content)}`);
        return result;
      }});
    check('Human and delegated-agent Mail commands persist full content and update the open view without losing its draft');
    const verification = await import(pathToFileURL(path.join(repo,'backend/dist-verification/wiki.js')));
    const artifacts = await artifactService(verification.readManifest); artifactServer = artifacts.server;
    const unlisted = await fetch(new URL('/manifest.edn', artifacts.baseUrl)); assert.equal(unlisted.status,404);
    evidence.wiki = await tour(page, {baseUrl:services.baseUrl, outputDir, garden:'sandbox.wiki/research', agentCommand,
      recordScreenshot:record => evidence.screenshots.push(record),
      verifyCheckout:services.verifyBuilds, beforeSourceAcceptance:verifySourceUnaccepted,
      translationTour:(currentPage, config) => translationTour(currentPage, {...config, artifactBaseUrl:artifacts.baseUrl,
        verifyMemory:input => verification.readMemory(path.join(fixtureDirectory,'state/translation-splits'), input)})});
    check('Two complete publication cycles reused accepted corrections from distinct canonical translation turns');
  }
  evidence.verifiedBuilds = await services.verifyBuilds();
  assert.equal(evidence.failures.length,0, 'Browser and artifact failures remain');
  evidence.completed = true;
} catch (error) {
  evidence.completed = false; evidence.failures.push({stage:'supervisor',message:error.message, stack:error.stack});
  if (page && !page.isClosed()) {
    try {
      await page.screenshot({path:path.join(outputDir,'failure.png'),fullPage:true,
        mask:[page.locator('input[type="password"]'),page.locator('textarea')]});
      const markup = await page.evaluate(() => {
        const root = document.documentElement.cloneNode(true);
        root.querySelectorAll('script').forEach(node => node.remove());
        root.querySelectorAll('input').forEach(node => node.removeAttribute('value'));
        root.querySelectorAll('textarea').forEach(node => {node.textContent='[redacted]';});
        return root.outerHTML;
      });
      await fs.writeFile(path.join(outputDir,'failure.html'),markup,{mode:0o600});
    } catch (captureError) {
      evidence.failures.push({stage:'failure-capture',message:captureError.message});
    }
  }
  console.error(`FAIL ${error.message}`); process.exitCode=1;
} finally {
  await cleanup();
  process.removeListener('SIGINT',onInterrupt);
  process.removeListener('SIGTERM',onTerminate);
  process.removeListener('unhandledRejection',onFatal);
}
