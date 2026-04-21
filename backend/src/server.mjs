import Fastify from 'fastify';
import fastifyCors from '@fastify/cors';
import fastifyWebsocket from '@fastify/websocket';
import fastifyMultipart from '@fastify/multipart';
import fastifyCookie from '@fastify/cookie';
import fastifyFormbody from '@fastify/formbody';
import { Type } from '@sinclair/typebox';
import * as sdk from '@mariozechner/pi-coding-agent';
import * as crypto from 'node:crypto';
import * as fs from 'node:fs/promises';
import * as path from 'node:path';
import * as os from 'node:os';
import { execFile } from 'node:child_process';
import { createRequire } from 'node:module';
import { promisify } from 'node:util';
import nodemailer from 'nodemailer';
import { createClient } from 'redis';
import { z } from 'zod';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js';
import { isInitializeRequest } from '@modelcontextprotocol/sdk/types.js';
import {
  config as readConfig,
  registerAppRoutes,
  registerWsRoutes,
  createDiscordGatewayManager,
  createPolicyDb,
  registerAuthRoutes,
  createSessionHook,
  resolveAuthContext,
  createKnoxxCustomTools,
  getPiIngestStatus,
  listPiSessions,
} from '../dist/app.js';

globalThis.require = globalThis.require || createRequire(import.meta.url);

const discordGateway = createDiscordGatewayManager({ log: console });

const policyDb = await createPolicyDb({
  connectionString: process.env.KNOXX_POLICY_DATABASE_URL || process.env.DATABASE_URL || '',
  primaryOrgSlug: process.env.KNOXX_PRIMARY_ORG_SLUG || 'open-hax',
  primaryOrgName: process.env.KNOXX_PRIMARY_ORG_NAME || 'Open Hax',
  primaryOrgKind: process.env.KNOXX_PRIMARY_ORG_KIND || 'platform_owner',
  bootstrapSystemAdminEmail: process.env.KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL || 'system-admin@open-hax.local',
  bootstrapSystemAdminName: process.env.KNOXX_BOOTSTRAP_SYSTEM_ADMIN_NAME || 'Knoxx System Admin',
  // Comma/space separated list of emails to auto-create as active users in
  // the primary org during DB bootstrap. These users become "whitelisted" for
  // GitHub OAuth login because resolveRequestContext will succeed for them.
  bootstrapAllowlistEmails: process.env.KNOXX_BOOTSTRAP_ALLOWLIST_EMAILS || '',
  // Optional: role slugs to grant to allowlisted emails (defaults to
  // knowledge_worker). Example: "knowledge_worker,system_admin".
  bootstrapAllowlistRoleSlugs: process.env.KNOXX_BOOTSTRAP_ALLOWLIST_ROLE_SLUGS || '',
});

const runtime = {
  Fastify,
  fastifyCors,
  fastifyWebsocket,
  fastifyMultipart,
  Type,
  sdk,
  crypto,
  fs,
  path,
  os,
  execFileAsync: promisify(execFile),
  policyDb,
  nodemailer,
};

const config = readConfig();
const app = Fastify({ logger: true });

// Allow POST/PUT/PATCH with Content-Type: application/json but empty body.
// Fastify's default JSON parser throws FST_ERR_CTP_EMPTY_JSON_BODY in this case,
// but some routes (e.g. /api/admin/config/event-agents/jobs/:jobId/run) are
// POST-without-body by design, and the browser may still send the header.
app.addContentTypeParser('application/json', { parseAs: 'string' }, (req, body, done) => {
  try {
    done(null, body === '' ? {} : JSON.parse(body));
  } catch (err) {
    done(err);
  }
});

await app.register(fastifyCors, { origin: true });
await app.register(fastifyCookie);
await app.register(fastifyFormbody);
await app.register(fastifyMultipart);
await app.register(fastifyWebsocket);

await app.register((instance, _opts, done) => {
  registerWsRoutes(runtime, instance);
  done();
});
// Session cookie hook: injects x-knoxx-* headers from cookie session before CLJS routes.
// Default is OFF because cookie-backed auth context resolution now lives in CLJS
// (see knoxx.backend.authz/resolve-request-context!). Enable only if you need
// legacy header-injection behavior for non-authz code paths.
if (process.env.KNOXX_ENABLE_SESSION_HOOK === '1') {
  app.addHook('onRequest', createSessionHook(policyDb));
}

// GitHub OAuth + cookie session auth routes
registerAuthRoutes(app, { policyDb, runtime });

// registerAppRoutes may perform async bootstrap (Redis init, session recovery, etc.).
await registerAppRoutes(runtime, app, config);

// ---------------------------------------------------------------------------
// Knoxx MCP (Model Context Protocol) HTTP facade
// ---------------------------------------------------------------------------

const MCP_ENV = z
  .object({
    KNOXX_PUBLIC_BASE_URL: z.string().optional(),
    KNOXX_MCP_TOKEN_TTL_SECONDS: z.coerce.number().int().positive().optional(),
    KNOXX_MCP_CODE_TTL_SECONDS: z.coerce.number().int().positive().optional(),
    REDIS_URL: z.string().optional(),
  })
  .parse(process.env);

const publicBaseUrl = new URL(MCP_ENV.KNOXX_PUBLIC_BASE_URL || process.env.RENDER_EXTERNAL_URL || 'http://localhost');

const mcpRedis = {
  client: null,
  connectPromise: null,
};

async function getRedis() {
  if (mcpRedis.client && mcpRedis.client.isOpen) return mcpRedis.client;
  if (mcpRedis.connectPromise) return mcpRedis.connectPromise;
  const url = MCP_ENV.REDIS_URL || process.env.REDIS_URL || 'redis://127.0.0.1:6379';
  const client = createClient({ url });
  client.on('error', (err) => app.log.error({ err }, '[knoxx-mcp] redis error'));
  mcpRedis.connectPromise = client
    .connect()
    .then(() => {
      mcpRedis.client = client;
      mcpRedis.connectPromise = null;
      app.log.info(`[knoxx-mcp] redis connected (${url})`);
      return client;
    })
    .catch((err) => {
      mcpRedis.connectPromise = null;
      throw err;
    });
  return mcpRedis.connectPromise;
}

const MCP_CODE_TTL_SECONDS = MCP_ENV.KNOXX_MCP_CODE_TTL_SECONDS ?? 300;
const MCP_TOKEN_TTL_SECONDS = MCP_ENV.KNOXX_MCP_TOKEN_TTL_SECONDS ?? 60 * 60 * 24 * 30;

function base64url(buf) {
  return Buffer.from(buf).toString('base64url');
}

function pkceChallenge(verifier) {
  return base64url(crypto.createHash('sha256').update(verifier).digest());
}

function parseBearerToken(req) {
  const raw = String(req.headers?.authorization || '');
  const m = raw.match(/^Bearer\s+(.+)$/i);
  return m ? m[1].trim() : null;
}

async function requireBrowserAuthContext(req, reply) {
  try {
    // resolveAuthContext comes from CLJS and understands Knoxx cookie sessions.
    return await resolveAuthContext(req, policyDb);
  } catch (err) {
    const currentUrl = new URL(req.raw.url || '/api/mcp/oauth/authorize', publicBaseUrl);
    const loginUrl = new URL('/api/auth/login', publicBaseUrl);
    loginUrl.searchParams.set('redirect', currentUrl.pathname + currentUrl.search);
    // Fastify v5 signature: reply.redirect(url, [statusCode])
    reply.redirect(loginUrl.toString(), 302);
    return null;
  }
}

function toolCheckboxHtml(tools, selected) {
  const safe = (s) => String(s).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;');
  return tools
    .map((t) => {
      const name = String(t.name || '');
      const label = String(t.label || t.name || t.description || name);
      const desc = String(t.description || '');
      const checked = selected.has(name) ? 'checked' : '';
      return `
        <label style="display:block; margin: 6px 0;">
          <input type="checkbox" name="tool" value="${safe(name)}" ${checked} />
          <span style="font-weight:600;">${safe(label)}</span>
          <span style="color:#666;">(${safe(name)})</span>
          <div style="color:#444; margin-left: 22px;">${safe(desc)}</div>
        </label>
      `;
    })
    .join('\n');
}

function normalizeToolSelection(raw) {
  if (!raw) return [];
  if (Array.isArray(raw)) return raw.map(String);
  return [String(raw)];
}

app.get('/.well-known/oauth-authorization-server', async (_req, reply) => {
  const issuer = new URL(publicBaseUrl);
  // Allow the running service to be reverse-proxied without hardcoding port.
  reply.send({
    issuer: issuer.toString().replace(/\/$/, ''),
    authorization_endpoint: new URL('/api/mcp/oauth/authorize', issuer).toString(),
    token_endpoint: new URL('/api/mcp/oauth/token', issuer).toString(),
    registration_endpoint: new URL('/api/mcp/oauth/register', issuer).toString(),
    response_types_supported: ['code'],
    grant_types_supported: ['authorization_code'],
    code_challenge_methods_supported: ['S256'],
    token_endpoint_auth_methods_supported: ['none'],
  });
});

async function getRegisteredClient(clientId) {
  if (!clientId) return null;
  const redis = await getRedis();
  const raw = await redis.get(`knoxx:mcp:client:${clientId}`);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

function redirectUriAllowed(client, redirectUri) {
  if (!client) return true;
  const uris = Array.isArray(client.redirect_uris) ? client.redirect_uris : [];
  return uris.includes(redirectUri);
}

// OAuth Dynamic Client Registration (minimal).
// Many MCP clients will try to auto-register; we support public clients (no secret).
app.post('/api/mcp/oauth/register', async (req, reply) => {
  const body = req.body || {};
  const redirectUris = Array.isArray(body.redirect_uris) ? body.redirect_uris.map(String) : [];
  if (redirectUris.length === 0) {
    reply.code(400).send({ error: 'invalid_client_metadata', detail: 'redirect_uris is required' });
    return;
  }

  const clientId = crypto.randomUUID();
  const client = {
    client_id: clientId,
    client_name: body.client_name ? String(body.client_name) : 'mcp-client',
    redirect_uris: redirectUris,
    token_endpoint_auth_method: 'none',
    grant_types: ['authorization_code'],
    response_types: ['code'],
    created_at: new Date().toISOString(),
  };

  const redis = await getRedis();
  await redis.set(`knoxx:mcp:client:${clientId}`, JSON.stringify(client));
  reply.code(201).send(client);
});

// OAuth authorize endpoint with a consent screen.
// This uses the existing Knoxx GitHub OAuth session cookie for user identity,
// then issues a short-lived authorization code tied to a chosen tool allowlist.
app.get('/api/mcp/oauth/authorize', async (req, reply) => {
  const q = req.query || {};
  const clientId = String(q.client_id || '');
  const redirectUri = String(q.redirect_uri || '');
  const state = q.state ? String(q.state) : '';
  const codeChallenge = String(q.code_challenge || '');
  const codeChallengeMethod = String(q.code_challenge_method || 'S256');
  const requestedScope = String(q.scope || '');

  if (!clientId || !redirectUri || !codeChallenge || codeChallengeMethod !== 'S256') {
    reply.code(400).send({ error: 'invalid_request', detail: 'Missing required OAuth parameters (client_id, redirect_uri, code_challenge, S256)' });
    return;
  }

  const registeredClient = await getRegisteredClient(clientId);
  if (registeredClient && !redirectUriAllowed(registeredClient, redirectUri)) {
    reply.code(400).send({ error: 'invalid_request', detail: 'redirect_uri not allowed for registered client' });
    return;
  }

  const ctx = await requireBrowserAuthContext(req, reply);
  if (!ctx) return;

  const tools = createKnoxxCustomTools(runtime, config, ctx) || [];
  const requested = new Set(requestedScope.split(/\s+/).map((s) => s.trim()).filter(Boolean));
  const selected = new Set();
  for (const t of tools) {
    const name = String(t?.name || '');
    if (!name) continue;
    if (requested.has('all') || requested.has(name)) selected.add(name);
  }
  if (selected.size === 0) {
    // Sensible default: read-only-ish memory + search tools if available.
    ['semantic_query', 'semantic_read', 'memory_search', 'memory_session', 'graph_query', 'websearch', 'read'].forEach((id) => selected.add(id));
  }

  const confirmUrl = new URL('/api/mcp/oauth/authorize/confirm', publicBaseUrl);
  confirmUrl.searchParams.set('client_id', clientId);
  confirmUrl.searchParams.set('redirect_uri', redirectUri);
  if (state) confirmUrl.searchParams.set('state', state);
  confirmUrl.searchParams.set('code_challenge', codeChallenge);
  confirmUrl.searchParams.set('code_challenge_method', 'S256');
  // We re-emit requested scope for display/debug, but actual allowlist comes from checkboxes.
  if (requestedScope) confirmUrl.searchParams.set('scope', requestedScope);

  const html = `<!doctype html>
  <html>
    <head>
      <meta charset="utf-8" />
      <title>Authorize MCP Client</title>
      <style>
        body { font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial; margin: 24px; }
        .box { max-width: 920px; }
        .meta { color: #555; margin-bottom: 12px; }
        .tools { border: 1px solid #ddd; border-radius: 8px; padding: 12px 16px; }
        .actions { margin-top: 18px; display: flex; gap: 12px; }
        button { padding: 8px 14px; border-radius: 8px; border: 1px solid #333; background: #111; color: #fff; cursor: pointer; }
        a { color: #0b67d0; }
      </style>
    </head>
    <body>
      <div class="box">
        <h1>Authorize MCP Client</h1>
        <div class="meta">
          <div><strong>Client:</strong> ${clientId}</div>
          <div><strong>Redirect URI:</strong> ${redirectUri}</div>
          <div><strong>User:</strong> ${String(ctx?.user?.email || ctx?.userEmail || '')}</div>
          <div><strong>Org:</strong> ${String(ctx?.org?.slug || ctx?.orgSlug || '')}</div>
        </div>

        <form method="GET" action="${confirmUrl.pathname}">
          <input type="hidden" name="client_id" value="${clientId}" />
          <input type="hidden" name="redirect_uri" value="${redirectUri}" />
          <input type="hidden" name="state" value="${state}" />
          <input type="hidden" name="code_challenge" value="${codeChallenge}" />
          <input type="hidden" name="code_challenge_method" value="S256" />
          <input type="hidden" name="scope" value="${requestedScope}" />

          <h2>Capabilities</h2>
          <p>Select exactly which Knoxx tools this client can call. You can always revoke tokens later.</p>
          <div class="tools">
            ${toolCheckboxHtml(tools, selected)}
          </div>

          <div class="actions">
            <button type="submit">Authorize</button>
            <a href="/">Cancel</a>
          </div>
        </form>
      </div>
    </body>
  </html>`;

  reply.header('content-type', 'text/html; charset=utf-8').send(html);
});

app.get('/api/mcp/oauth/authorize/confirm', async (req, reply) => {
  const q = req.query || {};
  const clientId = String(q.client_id || '');
  const redirectUri = String(q.redirect_uri || '');
  const state = q.state ? String(q.state) : '';
  const codeChallenge = String(q.code_challenge || '');
  const codeChallengeMethod = String(q.code_challenge_method || 'S256');
  const selectedTools = normalizeToolSelection(q.tool);

  if (!clientId || !redirectUri || !codeChallenge || codeChallengeMethod !== 'S256') {
    reply.code(400).send({ error: 'invalid_request', detail: 'Missing required OAuth parameters' });
    return;
  }

  const registeredClient = await getRegisteredClient(clientId);
  if (registeredClient && !redirectUriAllowed(registeredClient, redirectUri)) {
    reply.code(400).send({ error: 'invalid_request', detail: 'redirect_uri not allowed for registered client' });
    return;
  }

  const ctx = await requireBrowserAuthContext(req, reply);
  if (!ctx) return;

  const allTools = createKnoxxCustomTools(runtime, config, ctx) || [];
  const available = new Set(allTools.map((t) => String(t?.name || '')).filter(Boolean));
  const requested = [...new Set(selectedTools.map((t) => String(t).trim()).filter(Boolean))].filter((t) => available.has(t));
  if (requested.length === 0) {
    reply.code(400).send({ error: 'invalid_scope', detail: 'No valid tools selected' });
    return;
  }

  const membershipId = String(ctx?.membership?.id || ctx?.membershipId || '');
  const userEmail = String(ctx?.user?.email || ctx?.userEmail || '');
  const orgSlug = String(ctx?.org?.slug || ctx?.orgSlug || '');

  const code = crypto.randomUUID();
  const redis = await getRedis();
  const codeKey = `knoxx:mcp:code:${code}`;
  const codeValue = {
    code,
    clientId,
    redirectUri,
    codeChallenge,
    codeChallengeMethod: 'S256',
    tools: requested,
    membershipId,
    userEmail,
    orgSlug,
    createdAt: new Date().toISOString(),
  };
  await redis.set(codeKey, JSON.stringify(codeValue), { EX: MCP_CODE_TTL_SECONDS });

  const redirect = new URL(redirectUri);
  redirect.searchParams.set('code', code);
  if (state) redirect.searchParams.set('state', state);
  // Fastify v5 signature: reply.redirect(url, [statusCode])
  reply.redirect(redirect.toString(), 302);
});

// OAuth token exchange endpoint (authorization_code + PKCE S256).
app.post('/api/mcp/oauth/token', async (req, reply) => {
  const body = req.body || {};
  const grantType = String(body.grant_type || body.grantType || '');
  const code = String(body.code || '');
  const codeVerifier = String(body.code_verifier || body.codeVerifier || '');
  const clientId = String(body.client_id || body.clientId || '');
  const redirectUri = String(body.redirect_uri || body.redirectUri || '');

  if (grantType !== 'authorization_code' || !code || !codeVerifier || !clientId || !redirectUri) {
    reply.code(400).send({ error: 'invalid_request' });
    return;
  }

  const redis = await getRedis();

  const registeredClient = await getRegisteredClient(clientId);
  if (registeredClient && !redirectUriAllowed(registeredClient, redirectUri)) {
    reply.code(400).send({ error: 'invalid_grant', detail: 'redirect_uri not allowed for registered client' });
    return;
  }

  const codeKey = `knoxx:mcp:code:${code}`;
  const raw = await redis.get(codeKey);
  if (!raw) {
    reply.code(400).send({ error: 'invalid_grant', detail: 'Unknown or expired code' });
    return;
  }

  const record = JSON.parse(raw);
  if (record.clientId !== clientId || record.redirectUri !== redirectUri) {
    reply.code(400).send({ error: 'invalid_grant', detail: 'Client/redirect mismatch' });
    return;
  }

  const expected = String(record.codeChallenge || '');
  const actual = pkceChallenge(codeVerifier);
  if (!expected || expected !== actual) {
    reply.code(400).send({ error: 'invalid_grant', detail: 'PKCE verification failed' });
    return;
  }

  await redis.del(codeKey);

  const accessToken = crypto.randomUUID();
  const tokenKey = `knoxx:mcp:token:${accessToken}`;
  const tokenValue = {
    accessToken,
    clientId,
    membershipId: record.membershipId,
    userEmail: record.userEmail,
    orgSlug: record.orgSlug,
    tools: record.tools,
    createdAt: new Date().toISOString(),
    expiresAt: new Date(Date.now() + MCP_TOKEN_TTL_SECONDS * 1000).toISOString(),
  };
  await redis.set(tokenKey, JSON.stringify(tokenValue), { EX: MCP_TOKEN_TTL_SECONDS });

  if (record.membershipId) {
    await redis.sAdd(`knoxx:mcp:user:${record.membershipId}:tokens`, accessToken);
  }

  reply.send({
    access_token: accessToken,
    token_type: 'Bearer',
    scope: Array.isArray(record.tools) ? record.tools.join(' ') : '',
    expires_in: MCP_TOKEN_TTL_SECONDS,
  });
});

app.get('/api/mcp/tokens', async (req, reply) => {
  const ctx = await requireBrowserAuthContext(req, reply);
  if (!ctx) return;
  const membershipId = String(ctx?.membership?.id || ctx?.membershipId || '');
  if (!membershipId) {
    reply.code(400).send({ error: 'missing_membership' });
    return;
  }
  const redis = await getRedis();
  const tokenIds = await redis.sMembers(`knoxx:mcp:user:${membershipId}:tokens`);
  const records = [];
  for (const tokenId of tokenIds) {
    const raw = await redis.get(`knoxx:mcp:token:${tokenId}`);
    if (raw) records.push(JSON.parse(raw));
  }
  reply.send({ ok: true, tokens: records });
});

app.delete('/api/mcp/tokens/:tokenId', async (req, reply) => {
  const ctx = await requireBrowserAuthContext(req, reply);
  if (!ctx) return;
  const membershipId = String(ctx?.membership?.id || ctx?.membershipId || '');
  const tokenId = String(req.params?.tokenId || '');
  if (!membershipId || !tokenId) {
    reply.code(400).send({ error: 'invalid_request' });
    return;
  }
  const redis = await getRedis();
  await redis.del(`knoxx:mcp:token:${tokenId}`);
  await redis.sRem(`knoxx:mcp:user:${membershipId}:tokens`, tokenId);
  reply.send({ ok: true });
});

async function loadMcpToken(req) {
  const token = parseBearerToken(req);
  if (!token) return null;
  const redis = await getRedis();
  const raw = await redis.get(`knoxx:mcp:token:${token}`);
  if (!raw) return null;
  return JSON.parse(raw);
}

function resolveSessionId(req) {
  const headerSessionId = req.headers?.['mcp-session-id'];
  if (typeof headerSessionId === 'string' && headerSessionId.length > 0) return headerSessionId;
  const querySessionId = req.query?.sessionId;
  if (typeof querySessionId === 'string' && querySessionId.length > 0) return querySessionId;
  return undefined;
}

const mcpSessions = new Map();

async function handleMcpPost(req, reply) {
  const sessionId = resolveSessionId(req);
  const existing = sessionId ? mcpSessions.get(sessionId) : null;
  if (existing) {
    const token = parseBearerToken(req);
    if (!token || token !== existing.tokenId) {
      reply.code(401).send('Unauthorized');
      return;
    }
    await existing.transport.handleRequest(req.raw, reply.raw, req.body);
    return;
  }

  if (!isInitializeRequest(req.body)) {
    reply.code(400).send({
      jsonrpc: '2.0',
      error: { code: -32000, message: 'Bad Request: Server not initialized' },
      id: null,
    });
    return;
  }

  const tokenRecord = await loadMcpToken(req);
  if (!tokenRecord) {
    reply.code(401).send('Unauthorized');
    return;
  }

  // Resolve fresh Knoxx policy context from the token principal to ensure delegated tools
  // can never exceed the caller's current membership/role policy.
  const headersLike = {};
  if (tokenRecord.membershipId) headersLike['x-knoxx-membership-id'] = tokenRecord.membershipId;
  if (tokenRecord.userEmail) headersLike['x-knoxx-user-email'] = tokenRecord.userEmail;
  if (tokenRecord.orgSlug) headersLike['x-knoxx-org-slug'] = tokenRecord.orgSlug;
  const ctx = await policyDb.resolveRequestContext(headersLike);

  const allTools = createKnoxxCustomTools(runtime, config, ctx) || [];
  const allow = new Set((tokenRecord.tools || []).map(String));
  const effectiveTools = allTools.filter((t) => allow.has(String(t?.name || '')));

  const server = new McpServer({ name: 'knoxx', version: '0.1.0' });
  for (const tool of effectiveTools) {
    const toolName = String(tool?.name || '').trim();
    if (!toolName) continue;
    server.registerTool(
      toolName,
      {
        description: String(tool?.description || tool?.label || toolName),
        inputSchema: z.record(z.any()),
      },
      async (params) => {
        // Pass params as-is to the CLJS tool execute function.
        // tool.execute returns MCP-compatible {content: [...], ...} objects.
        return await tool.execute('mcp', params, null, null, null);
      },
    );
  }

  const transport = new StreamableHTTPServerTransport({
    sessionIdGenerator: () => crypto.randomUUID(),
    onsessioninitialized: async (sid) => {
      mcpSessions.set(sid, { transport, tokenId: tokenRecord.accessToken });
    },
  });

  transport.onclose = () => {
    if (transport.sessionId) mcpSessions.delete(transport.sessionId);
  };

  await server.connect(transport);
  await transport.handleRequest(req.raw, reply.raw, req.body);
}

async function handleMcpSession(req, reply) {
  const sessionId = resolveSessionId(req);
  if (!sessionId) {
    reply.code(400).send('Missing mcp-session-id');
    return;
  }
  const existing = mcpSessions.get(sessionId);
  if (!existing) {
    reply.code(404).send(`Invalid mcp-session-id: ${sessionId}`);
    return;
  }
  const token = parseBearerToken(req);
  if (!token || token !== existing.tokenId) {
    reply.code(401).send('Unauthorized');
    return;
  }
  await existing.transport.handleRequest(req.raw, reply.raw);
}

app.post('/mcp', handleMcpPost);
app.get('/mcp', handleMcpSession);
app.delete('/mcp', handleMcpSession);

// ---------------------------------------------------------------------------
// Pi Session Ingestion Routes
// ---------------------------------------------------------------------------

function openplannerRequestFromApp(appConfig) {
  return async function openplannerRequest(method, path, body) {
    const baseUrl = appConfig.openplannerBaseUrl || process.env.OPENPLANNER_BASE_URL || 'http://openplanner:7777';
    const url = `${baseUrl}${path}`;
    const headers = { 'Content-Type': 'application/json' };
    const apiKey = appConfig.openplannerApiKey || process.env.OPENPLANNER_API_KEY;
    if (apiKey) headers['Authorization'] = `Bearer ${apiKey}`;
    const resp = await fetch(url, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
      signal: AbortSignal.timeout(120_000), // 2 min hard timeout
    });
    if (!resp.ok) {
      const text = await resp.text().catch(() => '');
      throw new Error(`OpenPlanner ${method} ${path} returned ${resp.status}: ${text.slice(0, 200)}`);
    }
    return resp.json();
  };
}

// GET /api/admin/pi-sessions/status — ingestion state overview
//
// NOTE: There are *two* ingestion mechanisms:
// - legacy JS ingester state: ~/.knoxx/pi-ingest-state/ingested-sessions.json
// - current kms-ingestion service (pi-sessions driver)
//
// This endpoint reports both so the admin UI can show accurate progress.
app.get('/api/admin/pi-sessions/status', async (req, reply) => {
  const kmsBase = process.env.KMS_INGESTION_URL || 'http://localhost:3003';
  const kmsHeaders = { 'x-knoxx-user-email': 'system-admin@open-hax.local', 'x-knoxx-org-slug': 'open-hax' };

  const legacy = await getPiIngestStatus().catch((err) => ({ ok: false, error: err.message }));

  let kms = { ok: false, error: 'kms-ingestion unavailable' };
  try {
    const sources = await fetch(`${kmsBase}/api/ingestion/sources?tenant_id=knoxx-session`, { headers: kmsHeaders })
      .then((r) => r.json())
      .catch(() => []);
    const piSource = sources.find((s) => s.driver_type === 'pi-sessions');
    if (!piSource) {
      kms = { ok: false, error: 'pi-sessions source not found', sources: Array.isArray(sources) ? sources : [] };
    } else {
      const jobs = await fetch(`${kmsBase}/api/ingestion/jobs?tenant_id=knoxx-session&source_id=${piSource.source_id}`, { headers: kmsHeaders })
        .then((r) => r.json())
        .catch(() => []);
      kms = { ok: true, source: piSource, jobs };
    }
  } catch (err) {
    kms = { ok: false, error: err.message };
  }

  return reply.send({ ok: true, legacy, kms_ingestion: kms });
});

// GET /api/admin/pi-sessions — list available pi sessions
app.get('/api/admin/pi-sessions', async (req, reply) => {
  try {
    const limit = Math.min(parseInt(req.query?.limit || '50', 10), 200);
    const offset = parseInt(req.query?.offset || '0', 10);
    const workspace = req.query?.workspace || null;
    const result = await listPiSessions({ limit, offset, workspace });
    return reply.send(result);
  } catch (err) {
    return reply.code(500).send({ ok: false, error: err.message });
  }
});

// POST /api/admin/pi-sessions/ingest — proxy to ingestion service
app.post('/api/admin/pi-sessions/ingest', async (req, reply) => {
  // Ingestion is now handled by the kms-ingestion service (pi-sessions driver).
  // This endpoint delegates to it.
  try {
    const ingestUrl = `${process.env.KMS_INGESTION_URL || 'http://localhost:3003'}/api/ingestion/jobs`;
    const sources = await fetch(`${process.env.KMS_INGESTION_URL || 'http://localhost:3003'}/api/ingestion/sources?tenant_id=knoxx-session`, {
      headers: { 'x-knoxx-user-email': 'system-admin@open-hax.local', 'x-knoxx-org-slug': 'open-hax' },
    }).then(r => r.json()).catch(() => []);
    const piSource = sources.find(s => s.driver_type === 'pi-sessions');
    if (!piSource) {
      return reply.code(404).send({ ok: false, error: 'pi-sessions source not found in ingestion service' });
    }
    const result = await fetch(ingestUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'x-knoxx-user-email': 'system-admin@open-hax.local', 'x-knoxx-org-slug': 'open-hax' },
      body: JSON.stringify({ source_id: piSource.source_id, full_scan: req.body?.force || false }),
    }).then(r => r.json());
    return reply.send({ ok: true, job: result });
  } catch (err) {
    return reply.code(500).send({ ok: false, error: err.message });
  }
});

// ---------------------------------------------------------------------------
// Ingestion Service Proxy
// ---------------------------------------------------------------------------
// The kms-ingestion service runs on port 3003. Caddy routes /api/ingestion/*
// to this backend (port 8000), so we proxy those requests through.
const INGESTION_BASE = process.env.KMS_INGESTION_URL || 'http://localhost:3003';

app.all('/api/ingestion/*', async (req, reply) => {
  const subPath = req.params['*'];
  const targetUrl = `${INGESTION_BASE}/api/ingestion/${subPath}`;
  const headers = { ...req.headers };
  delete headers['host'];
  delete headers['connection'];
  delete headers['content-length'];
  try {
    const resp = await fetch(targetUrl, {
      method: req.method,
      headers,
      body: ['GET', 'HEAD'].includes(req.method) ? undefined : JSON.stringify(req.body),
      signal: AbortSignal.timeout(60_000),
    });
    const contentType = resp.headers.get('content-type') || 'application/json';
    const body = contentType.includes('application/json') ? await resp.json() : await resp.text();
    return reply.code(resp.status).header('content-type', contentType).send(body);
  } catch (err) {
    return reply.code(502).send({ ok: false, error: `Ingestion proxy error: ${err.message}` });
  }
});

// ---------------------------------------------------------------------------
// OpenPlanner Proxy
// ---------------------------------------------------------------------------
// Frontend calls /api/openplanner/v1/* but OpenPlanner serves /v1/*.
// Proxy: strip /api/openplanner prefix, add Authorization header.
const OPENPLANNER_BASE = process.env.OPENPLANNER_BASE_URL || 'http://localhost:7777';
const OPENPLANNER_KEY = process.env.OPENPLANNER_API_KEY || 'change-me';

app.all('/api/openplanner/*', async (req, reply) => {
  const subPath = req.params['*']; // e.g. "v1/gardens"
  const targetUrl = `${OPENPLANNER_BASE}/${subPath}`;
  const fwdHeaders = {
    'content-type': 'application/json',
    'authorization': `Bearer ${OPENPLANNER_KEY}`,
    'x-knoxx-user-email': req.headers['x-knoxx-user-email'] || '',
    'x-knoxx-org-slug': req.headers['x-knoxx-org-slug'] || '',
  };
  try {
    const resp = await fetch(targetUrl, {
      method: req.method,
      headers: fwdHeaders,
      body: ['GET', 'HEAD'].includes(req.method) ? undefined : JSON.stringify(req.body),
      signal: AbortSignal.timeout(60_000),
    });
    const contentType = resp.headers.get('content-type') || 'application/json';
    const body = contentType.includes('application/json') ? await resp.json() : await resp.text();
    return reply.code(resp.status).header('content-type', contentType).send(body);
  } catch (err) {
    return reply.code(502).send({ ok: false, error: `OpenPlanner proxy error: ${err.message}` });
  }
});

try {
  await app.listen({ host: config.host, port: config.port });
  app.log.info(`Knoxx backend CLJS listening on ${config.host}:${config.port}`);
} catch (error) {
  console.error('Knoxx backend CLJS failed to start', error);
  process.exit(1);
}
