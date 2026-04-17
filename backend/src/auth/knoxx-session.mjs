/**
 * Knoxx Session Auth — GitHub OAuth + cookie-backed sessions
 *
 * Flow:
 *   1. GET /api/auth/login → redirect to GitHub OAuth authorize URL
 *   2. GET /api/auth/callback/github → exchange code, upsert user, mint session, set cookie, redirect
 *   3. OnRequest hook: read cookie → load session from Redis → inject x-knoxx-* headers
 *   4. GET /api/auth/context → return current user context
 *   5. POST /api/auth/logout → delete session + clear cookie
 *   6. POST /api/auth/invite/redeem → redeem an invite code for the current user
 *
 * Environment variables:
 *   KNOXX_GITHUB_OAUTH_CLIENT_ID
 *   KNOXX_GITHUB_OAUTH_CLIENT_SECRET
 *   KNOXX_SESSION_SECRET          (used to sign session tokens; auto-generated if missing)
 *   KNOXX_SESSION_TTL_SECONDS     (default 86400 = 24h)
 *   KNOXX_PUBLIC_BASE_URL         (e.g. https://knoxx.promethean.rest)
 *   REDIS_URL                     (Redis for session storage)
 *   KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL  (always whitelisted)
 */

import * as crypto from 'node:crypto';
import { createClient as createRedisClient } from 'redis';

// ---------------------------------------------------------------------------
// Session token helpers
// ---------------------------------------------------------------------------

let _sessionSecret = null;

function sessionSecret() {
  if (!_sessionSecret) {
    _sessionSecret = process.env.KNOXX_SESSION_SECRET || crypto.randomBytes(32).toString('hex');
    if (!process.env.KNOXX_SESSION_SECRET) {
      console.warn('[knoxx-session] KNOXX_SESSION_SECRET not set; using ephemeral key. Sessions will not survive restarts.');
    }
  }
  return _sessionSecret;
}

function signToken(payload) {
  const key = sessionSecret();
  const iv = crypto.randomBytes(12);
  const data = JSON.stringify(payload);
  const cipher = crypto.createCipheriv('aes-256-gcm', Buffer.from(key, 'hex').subarray(0, 32), iv);
  let encrypted = cipher.update(data, 'utf8', 'base64url');
  encrypted += cipher.final('base64url');
  const tag = cipher.getAuthTag();
  return `${iv.toString('base64url')}:${encrypted}:${tag.toString('base64url')}`;
}

function verifyToken(token) {
  try {
    const key = sessionSecret();
    const [ivB64, encrypted, tagB64] = token.split(':');
    if (!ivB64 || !encrypted || !tagB64) return null;
    const iv = Buffer.from(ivB64, 'base64url');
    const tag = Buffer.from(tagB64, 'base64url');
    const decipher = crypto.createDecipheriv('aes-256-gcm', Buffer.from(key, 'hex').subarray(0, 32), iv);
    decipher.setAuthTag(tag);
    let decrypted = decipher.update(encrypted, 'base64url', 'utf8');
    decrypted += decipher.final('utf8');
    return JSON.parse(decrypted);
  } catch {
    return null;
  }
}

// ---------------------------------------------------------------------------
// Redis session store
// ---------------------------------------------------------------------------

let _redis = null;
let _redisConnectPromise = null;

async function getRedis() {
  if (_redis?.isOpen) return _redis;
  if (_redisConnectPromise) return _redisConnectPromise;

  _redisConnectPromise = (async () => {
    const url = process.env.REDIS_URL || 'redis://127.0.0.1:6379';
    const client = createRedisClient({ url });
    client.on('error', (err) => console.error('[knoxx-session] Redis error:', err.message));
    await client.connect();
    console.log('[knoxx-session] Redis connected for session store');
    _redis = client;
    _redisConnectPromise = null;
    return client;
  })();

  return _redisConnectPromise;
}

async function storeSession(sessionId, data) {
  const redis = await getRedis();
  const ttl = parseInt(process.env.KNOXX_SESSION_TTL_SECONDS || '86400', 10);
  await redis.set(`knoxx:session:${sessionId}`, JSON.stringify(data), { EX: ttl });
}

async function loadSession(sessionId) {
  const redis = await getRedis();
  const raw = await redis.get(`knoxx:session:${sessionId}`);
  if (!raw) return null;
  try { return JSON.parse(raw); } catch { return null; }
}

async function deleteSession(sessionId) {
  const redis = await getRedis();
  await redis.del(`knoxx:session:${sessionId}`);
}

// ---------------------------------------------------------------------------
// GitHub OAuth helpers
// ---------------------------------------------------------------------------

async function exchangeGithubCode(clientId, clientSecret, code) {
  const resp = await fetch('https://github.com/login/oauth/access_token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ client_id: clientId, client_secret: clientSecret, code }),
  });
  if (!resp.ok) throw new Error(`GitHub token exchange failed: ${resp.status}`);
  const data = await resp.json();
  if (data.error) throw new Error(`GitHub OAuth error: ${data.error_description || data.error}`);
  return data.access_token;
}

async function ghJson(url, accessToken) {
  const resp = await fetch(url, {
    headers: { Authorization: `Bearer ${accessToken}`, Accept: 'application/json' },
  });
  if (!resp.ok) throw new Error(`GitHub API ${url} returned ${resp.status}`);
  return resp.json();
}

async function getGithubUserEmails(accessToken) {
  try {
    const emails = await ghJson('https://api.github.com/user/emails', accessToken);
    const primary = emails.find((e) => e.primary);
    return primary?.email ?? emails[0]?.email ?? null;
  } catch {
    return null;
  }
}

// ---------------------------------------------------------------------------
// Cookie helpers
// ---------------------------------------------------------------------------

const COOKIE_NAME = 'knoxx_session';

function isSecureOrigin(baseUrl) {
  try { return new URL(baseUrl).protocol === 'https:'; } catch { return false; }
}

function setSessionCookie(reply, token, baseUrl) {
  const secure = isSecureOrigin(baseUrl);
  const cookieOpts = {
    path: '/',
    httpOnly: true,
    secure,
    sameSite: secure ? 'Strict' : 'Lax',
    maxAge: parseInt(process.env.KNOXX_SESSION_TTL_SECONDS || '86400', 10),
  };
  reply.setCookie(COOKIE_NAME, token, cookieOpts);
}

function clearSessionCookie(reply, baseUrl) {
  const secure = isSecureOrigin(baseUrl);
  reply.clearCookie(COOKIE_NAME, {
    path: '/',
    httpOnly: true,
    secure,
    sameSite: secure ? 'Strict' : 'Lax',
  });
}

// ---------------------------------------------------------------------------
// State parameter for CSRF protection
// ---------------------------------------------------------------------------

const STATE_TTL = 600; // 10 minutes
const pendingStates = new Map();

function createState(redirect) {
  const state = crypto.randomBytes(16).toString('hex');
  pendingStates.set(state, { redirect: redirect || '/', createdAt: Date.now() });
  // Cleanup old states
  for (const [key, val] of pendingStates) {
    if (Date.now() - val.createdAt > STATE_TTL * 1000) pendingStates.delete(key);
  }
  return state;
}

function consumeState(state) {
  const entry = pendingStates.get(state);
  if (!entry) return null;
  pendingStates.delete(state);
  if (Date.now() - entry.createdAt > STATE_TTL * 1000) return null;
  return entry;
}

// ---------------------------------------------------------------------------
// Route registration
// ---------------------------------------------------------------------------

export function registerAuthRoutes(app, { policyDb, runtime }) {
  const publicBaseUrl = process.env.KNOXX_PUBLIC_BASE_URL || 'http://localhost';
  const clientId = process.env.KNOXX_GITHUB_OAUTH_CLIENT_ID || '';
  const clientSecret = process.env.KNOXX_GITHUB_OAUTH_CLIENT_SECRET || '';
  const githubEnabled = !!(clientId && clientSecret);

  // --- GET /api/auth/config ---
  // Public: tells the frontend whether GitHub OAuth is available
  app.get('/api/auth/config', async (_req, reply) => {
    return reply.send({
      githubEnabled,
      publicBaseUrl,
      loginUrl: githubEnabled ? '/api/auth/login' : null,
    });
  });

  // --- GET /api/auth/login ---
  // Redirect to GitHub OAuth
  app.get('/api/auth/login', async (req, reply) => {
    if (!githubEnabled) return reply.code(503).send({ error: 'GitHub OAuth not configured' });
    const redirect = String(req.query?.redirect || '/');
    const state = createState(redirect);
    const callbackUrl = new URL('/api/auth/callback/github', publicBaseUrl).toString();
    const authorizeUrl = new URL('https://github.com/login/oauth/authorize');
    authorizeUrl.searchParams.set('client_id', clientId);
    authorizeUrl.searchParams.set('redirect_uri', callbackUrl);
    authorizeUrl.searchParams.set('state', state);
    authorizeUrl.searchParams.set('scope', 'read:user user:email');
    return reply.redirect(authorizeUrl.toString());
  });

  // --- GET /api/auth/callback/github ---
  app.get('/api/auth/callback/github', async (req, reply) => {
    if (!githubEnabled) return reply.code(503).send({ error: 'GitHub OAuth not configured' });
    const code = String(req.query?.code || '');
    const state = String(req.query?.state || '');
    if (!code || !state) return reply.code(400).send({ error: 'Missing code or state' });

    const stateEntry = consumeState(state);
    if (!stateEntry) return reply.code(400).send({ error: 'Invalid or expired state parameter' });

    try {
      // Exchange code for access token
      const accessToken = await exchangeGithubCode(clientId, clientSecret, code);

      // Fetch GitHub user profile
      const ghUser = await ghJson('https://api.github.com/user', accessToken);
      if (!ghUser?.id) return reply.code(502).send({ error: 'GitHub user lookup failed' });

      // Get primary email
      const email = await getGithubUserEmails(accessToken);
      if (!email) return reply.code(502).send({ error: 'Could not retrieve GitHub email' });

      // Check whitelist: bootstrap admin OR existing user OR pending invite
      const whitelisted = await policyDb.isEmailWhitelisted(email).catch(() => false);
      if (!whitelisted) {
        // Not whitelisted — redirect to invite-redemption page
        const inviteUrl = new URL('/login', publicBaseUrl);
        inviteUrl.searchParams.set('error', 'not_whitelisted');
        inviteUrl.searchParams.set('email', email);
        inviteUrl.searchParams.set('github_login', ghUser.login || '');
        return reply.redirect(inviteUrl.toString());
      }

      // Upsert user + membership in policy DB
      const ctx = await policyDb.resolveRequestContext({ 'x-knoxx-user-email': email });
      const membershipId = ctx?.membership?.id;
      if (!membershipId) {
        // User exists but no active membership — try create with primary org
        const bootstrapCtx = await policyDb.getBootstrapContext();
        await policyDb.createUser({
          email,
          displayName: ghUser.name || ghUser.login || email,
          orgId: bootstrapCtx.primaryOrg.id,
          authProvider: 'github',
          externalSubject: `github:${ghUser.id}`,
          roleSlugs: ['knowledge_worker'],
        });
      }

      // Update auth_provider / external_subject on the user row
      try {
        // We use resolveRequestContext again to get the fresh membership ID
        const freshCtx = await policyDb.resolveRequestContext({ 'x-knoxx-user-email': email });
        const sessionId = crypto.randomUUID();
        const sessionData = {
          membershipId: freshCtx?.membership?.id,
          userId: freshCtx?.user?.id,
          email,
          orgSlug: freshCtx?.org?.slug,
          orgId: freshCtx?.org?.id,
          displayName: ghUser.name || ghUser.login || email,
          githubLogin: ghUser.login,
          githubId: ghUser.id,
          authProvider: 'github',
          createdAt: new Date().toISOString(),
        };
        await storeSession(sessionId, sessionData);
        const token = signToken({ sid: sessionId });
        setSessionCookie(reply, token, publicBaseUrl);
        console.log(`[knoxx-session] GitHub login: ${email} (${ghUser.login}) → session ${sessionId.slice(0, 8)}…`);
        return reply.redirect(new URL(stateEntry.redirect, publicBaseUrl).toString());
      } catch (err) {
        console.error('[knoxx-session] Session creation failed:', err.message);
        const errorUrl = new URL('/login', publicBaseUrl);
        errorUrl.searchParams.set('error', 'session_failed');
        return reply.redirect(errorUrl.toString());
      }
    } catch (err) {
      console.error('[knoxx-session] GitHub OAuth callback error:', err.message);
      const errorUrl = new URL('/login', publicBaseUrl);
      errorUrl.searchParams.set('error', 'oauth_failed');
      errorUrl.searchParams.set('message', err.message);
      return reply.redirect(errorUrl.toString());
    }
  });

  // NOTE: /api/auth/context is handled by the CLJS app_routes.cljs
  // The onRequest cookie hook injects x-knoxx-* headers so the CLJS route works transparently.

  // --- POST /api/auth/logout ---
  app.post('/api/auth/logout', async (req, reply) => {
    const cookieToken = req.cookies?.[COOKIE_NAME];
    if (cookieToken) {
      const payload = verifyToken(cookieToken);
      if (payload?.sid) await deleteSession(payload.sid).catch(() => {});
    }
    clearSessionCookie(reply, publicBaseUrl);
    return reply.send({ ok: true });
  });

  // --- POST /api/auth/invite/redeem ---
  // Redeem an invite code for the current session user
  app.post('/api/auth/invite/redeem', async (req, reply) => {
    const code = String(req.body?.code || '').trim();
    if (!code) return reply.code(400).send({ error: 'Invite code is required' });

    // Get current user email from session or headers
    let email = '';
    const cookieToken = req.cookies?.[COOKIE_NAME];
    if (cookieToken) {
      const payload = verifyToken(cookieToken);
      if (payload?.sid) {
        const sessionData = await loadSession(payload.sid);
        if (sessionData) email = sessionData.email;
      }
    }
    if (!email) email = (req.headers['x-knoxx-user-email'] || '').trim();
    if (!email) return reply.code(401).send({ error: 'Not authenticated' });

    try {
      const result = await policyDb.redeemInvite(code, email);

      // If the user has a cookie session, refresh it with the new membership
      if (cookieToken) {
        const payload = verifyToken(cookieToken);
        if (payload?.sid) {
          const freshCtx = await policyDb.resolveRequestContext({ 'x-knoxx-user-email': email });
          const sessionData = await loadSession(payload.sid);
          if (sessionData && freshCtx) {
            sessionData.membershipId = freshCtx.membership?.id;
            sessionData.orgSlug = freshCtx.org?.slug;
            sessionData.orgId = freshCtx.org?.id;
            await storeSession(payload.sid, sessionData);
          }
        }
      }

      return reply.send({ ok: true, invite: result.invite, user: result.user });
    } catch (err) {
      return reply.code(err.status || 500).send({ error: err.message || 'Invite redemption failed' });
    }
  });

  // --- POST /api/auth/invite ---
  // Create an invite (requires org.users.invite permission)
  app.post('/api/auth/invite', async (req, reply) => {
    let ctx = null;
    try {
      ctx = await resolveAuthContext(req, policyDb);
    } catch (err) {
      return reply.code(err.status || 401).send({ error: err.message || 'Unauthorized' });
    }

    const orgId = req.body?.orgId || ctx.org?.id;
    const email = req.body?.email;
    const roleSlugs = req.body?.roleSlugs || ['knowledge_worker'];

    if (!email) return reply.code(400).send({ error: 'email is required' });

    try {
      const result = await policyDb.createInvite({
        orgId,
        email,
        roleSlugs,
        inviterMembershipId: ctx.membership?.id,
      });

      // Optionally send invite email
      const sendEmail = req.body?.sendEmail !== false;
      if (sendEmail) {
        await sendInviteEmail(runtime, result.invite, email, publicBaseUrl).catch((err) => {
          console.error('[knoxx-session] Failed to send invite email:', err.message);
        });
      }

      return reply.send({ ok: true, invite: result.invite });
    } catch (err) {
      return reply.code(err.status || 500).send({ error: err.message || 'Invite creation failed' });
    }
  });

  // --- GET /api/auth/invites ---
  // List invites for the current org
  app.get('/api/auth/invites', async (req, reply) => {
    let ctx = null;
    try {
      ctx = await resolveAuthContext(req, policyDb);
    } catch (err) {
      return reply.code(err.status || 401).send({ error: err.message || 'Unauthorized' });
    }

    const orgId = req.query?.orgId || ctx.org?.id;
    const status = req.query?.status || undefined;
    try {
      const result = await policyDb.listInvites({ orgId, status });
      return reply.send(result);
    } catch (err) {
      return reply.code(500).send({ error: err.message });
    }
  });
}

// ---------------------------------------------------------------------------
// onRequest hook — inject x-knoxx-* headers from cookie session
// ---------------------------------------------------------------------------

export function createSessionHook(policyDb) {
  return async function sessionHook(req, reply) {
    // Skip auth routes (they handle their own auth)
    if (req.url.startsWith('/api/auth/')) return;

    // If headers already provide identity, skip cookie session
    const headerEmail = (req.headers['x-knoxx-user-email'] || '').trim();
    const headerMembershipId = (req.headers['x-knoxx-membership-id'] || '').trim();
    if (headerEmail || headerMembershipId) return;

    // Try cookie session
    const cookieToken = req.cookies?.[COOKIE_NAME];
    if (!cookieToken) return; // No session, let downstream handle 401

    const payload = verifyToken(cookieToken);
    if (!payload?.sid) return;

    const sessionData = await loadSession(payload.sid);
    if (!sessionData) {
      // Session expired — clear cookie
      const publicBaseUrl = process.env.KNOXX_PUBLIC_BASE_URL || 'http://localhost';
      clearSessionCookie(reply, publicBaseUrl);
      return;
    }

    // Inject headers for downstream CLJS routes
    req.headers['x-knoxx-user-email'] = sessionData.email;
    if (sessionData.orgSlug) req.headers['x-knoxx-org-slug'] = sessionData.orgSlug;
    if (sessionData.membershipId) req.headers['x-knoxx-membership-id'] = sessionData.membershipId;
  };
}

// ---------------------------------------------------------------------------
// Resolve auth context from request (headers or cookie)
// ---------------------------------------------------------------------------

export async function resolveAuthContext(req, policyDb) {
  const headerEmail = (req.headers['x-knoxx-user-email'] || '').trim();
  const headerMembershipId = (req.headers['x-knoxx-membership-id'] || '').trim();

  if (headerEmail || headerMembershipId) {
    return policyDb.resolveRequestContext(req.headers);
  }

  const cookieToken = req.cookies?.[COOKIE_NAME];
  if (!cookieToken) throw httpError(401, 'Not authenticated', 'no_session');

  const payload = verifyToken(cookieToken);
  if (!payload?.sid) throw httpError(401, 'Invalid session token', 'invalid_token');

  const sessionData = await loadSession(payload.sid);
  if (!sessionData) throw httpError(401, 'Session expired', 'session_expired');

  const headers = {
    'x-knoxx-user-email': sessionData.email,
    'x-knoxx-org-slug': sessionData.orgSlug,
  };
  if (sessionData.membershipId) headers['x-knoxx-membership-id'] = sessionData.membershipId;
  return policyDb.resolveRequestContext(headers);
}

// ---------------------------------------------------------------------------
// Email sending
// ---------------------------------------------------------------------------

async function sendInviteEmail(runtime, invite, email, publicBaseUrl) {
  const transporter = runtime?.nodemailer;
  if (!transporter) {
    console.warn('[knoxx-session] No nodemailer runtime; skipping invite email');
    return;
  }

  const gmailEmail = process.env.GMAIL_APP_EMAIL;
  const gmailPassword = process.env.GMAIL_APP_PASSWORD;
  if (!gmailEmail || !gmailPassword) {
    console.warn('[knoxx-session] GMAIL_APP_EMAIL / GMAIL_APP_PASSWORD not set; skipping invite email');
    return;
  }

  const inviteUrl = new URL('/login', publicBaseUrl);
  inviteUrl.searchParams.set('invite', invite.code);
  inviteUrl.searchParams.set('email', email);

  const transporterInstance = transporter.createTransport({
    service: 'gmail',
    auth: { user: gmailEmail, pass: gmailPassword },
  });

  await transporterInstance.sendMail({
    from: `"Knoxx" <${gmailEmail}>`,
    to: email,
    subject: 'You\'re invited to join Knoxx',
    html: `
      <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto;">
        <h2 style="color: #e2e8f0;">You're invited to Knoxx</h2>
        <p style="color: #94a3b8;">You've been invited to join a Knoxx organization.</p>
        <a href="${inviteUrl.toString()}" style="display: inline-block; background: #3b82f6; color: white; padding: 12px 24px; border-radius: 8px; text-decoration: none; margin: 16px 0;">
          Accept Invite
        </a>
        <p style="color: #64748b; font-size: 13px;">
          Or paste this link: <code style="background: #1e293b; padding: 2px 6px; border-radius: 4px;">${inviteUrl.toString()}</code>
        </p>
        <p style="color: #475569; font-size: 12px;">This invite expires ${invite.expiresAt ? new Date(invite.expiresAt).toLocaleDateString() : 'in 7 days'}.</p>
      </div>
    `,
  });

  console.log(`[knoxx-session] Invite email sent to ${email}`);
}

// ---------------------------------------------------------------------------
// Error helper (matches policy-db.mjs pattern)
// ---------------------------------------------------------------------------

function httpError(status, message, code) {
  const err = new Error(message);
  err.status = status;
  err.code = code;
  return err;
}
