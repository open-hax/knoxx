import Fastify from 'fastify';
import fastifyCors from '@fastify/cors';
import fastifyWebsocket from '@fastify/websocket';
import fastifyMultipart from '@fastify/multipart';
import fastifyCookie from '@fastify/cookie';
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
import {
  config as readConfig,
  registerAppRoutes,
  registerWsRoutes,
  createDiscordGatewayManager,
} from '../dist/app.js';
import { createPolicyDb } from './policy-db.mjs';
import { getPiIngestStatus, listPiSessions } from './pi-session-ingester.mjs';
import { registerAuthRoutes, createSessionHook } from './auth/knoxx-session.mjs';

globalThis.require = globalThis.require || createRequire(import.meta.url);

const discordGateway = createDiscordGatewayManager({ log: console });

const policyDb = await createPolicyDb({
  connectionString: process.env.KNOXX_POLICY_DATABASE_URL || process.env.DATABASE_URL || '',
  primaryOrgSlug: process.env.KNOXX_PRIMARY_ORG_SLUG || 'open-hax',
  primaryOrgName: process.env.KNOXX_PRIMARY_ORG_NAME || 'Open Hax',
  primaryOrgKind: process.env.KNOXX_PRIMARY_ORG_KIND || 'platform_owner',
  bootstrapSystemAdminEmail: process.env.KNOXX_BOOTSTRAP_SYSTEM_ADMIN_EMAIL || 'system-admin@open-hax.local',
  bootstrapSystemAdminName: process.env.KNOXX_BOOTSTRAP_SYSTEM_ADMIN_NAME || 'Knoxx System Admin',
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

await app.register(fastifyCors, { origin: true });
await app.register(fastifyCookie);
await app.register(fastifyMultipart);
await app.register(fastifyWebsocket);

await app.register((instance, _opts, done) => {
  registerWsRoutes(runtime, instance);
  done();
});
// Session cookie hook: injects x-knoxx-* headers from cookie session before CLJS routes
app.addHook('onRequest', createSessionHook(policyDb));

// GitHub OAuth + cookie session auth routes
registerAuthRoutes(app, { policyDb, runtime });

// registerAppRoutes may perform async bootstrap (Redis init, session recovery, etc.).
await registerAppRoutes(runtime, app, config);

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

try {
  await app.listen({ host: config.host, port: config.port });
  app.log.info(`Knoxx backend CLJS listening on ${config.host}:${config.port}`);
} catch (error) {
  console.error('Knoxx backend CLJS failed to start', error);
  process.exit(1);
}
