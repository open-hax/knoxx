import Fastify from 'fastify';
import fastifyCors from '@fastify/cors';
import fastifyWebsocket from '@fastify/websocket';
import fastifyMultipart from '@fastify/multipart';
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
import { createDiscordGatewayManager } from './discord-gateway.mjs';
import { runPiSessionIngest, getPiIngestStatus, listPiSessions } from './pi-session-ingester.mjs';
import { createPolicyDb } from './policy-db.mjs';
import {
  config as readConfig,
  registerAppRoutes,
  registerWsRoutes,
} from '../dist/app.js';

globalThis.require = globalThis.require || createRequire(import.meta.url);

const discordGateway = createDiscordGatewayManager({ log: console });
globalThis.knoxxDiscordGateway = discordGateway;

const bootDiscordToken = (process.env.DISCORD_BOT_TOKEN || '').trim();
if (bootDiscordToken) {
  try {
    await discordGateway.start(bootDiscordToken);
  } catch (error) {
    console.error('[discord-gateway] failed to start at boot', error);
  }
}

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
  discordGateway,
};

const config = readConfig();
const app = Fastify({ logger: true });

await app.register(fastifyCors, { origin: true });
await app.register(fastifyMultipart);
await app.register(fastifyWebsocket);

await app.register((instance, _opts, done) => {
  registerWsRoutes(runtime, instance);
  done();
});
registerAppRoutes(runtime, app);

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
    });
    if (!resp.ok) {
      const text = await resp.text().catch(() => '');
      throw new Error(`OpenPlanner ${method} ${path} returned ${resp.status}: ${text.slice(0, 200)}`);
    }
    return resp.json();
  };
}

// GET /api/admin/pi-sessions/status — ingestion state overview
app.get('/api/admin/pi-sessions/status', async (req, reply) => {
  try {
    const status = await getPiIngestStatus();
    return reply.send(status);
  } catch (err) {
    return reply.code(500).send({ ok: false, error: err.message });
  }
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

// POST /api/admin/pi-sessions/ingest — trigger ingestion
app.post('/api/admin/pi-sessions/ingest', async (req, reply) => {
  try {
    const body = req.body || {};
    const force = body.force || false;
    const limit = Math.min(body.limit || 50, 200);
    const sessionDirs = body.sessionDirs || null;
    const openplannerRequestFn = openplannerRequestFromApp(config);
    const result = await runPiSessionIngest({
      openplannerRequestFn,
      force,
      limit,
      sessionDirs,
    });
    return reply.send(result);
  } catch (err) {
    return reply.code(500).send({ ok: false, error: err.message });
  }
});

// ---------------------------------------------------------------------------
// Pi Session Ingestion Cron Scheduler
// ---------------------------------------------------------------------------

const PI_INGEST_INTERVAL_MS = parseInt(process.env.PI_INGEST_INTERVAL_MS || '900000', 10); // Default: 15 min
let piIngestTimerHandle = null;
let piIngestRunning = false;

async function scheduledPiSessionIngest() {
  if (piIngestRunning) return;
  piIngestRunning = true;
  try {
    const openplannerRequestFn = openplannerRequestFromApp(config);
    const result = await runPiSessionIngest({ openplannerRequestFn, limit: 20 });
    if (result.newSessions > 0) {
      app.log.info({ piIngest: result }, 'Pi session cron ingest completed');
    }
  } catch (err) {
    app.log.warn({ err }, 'Pi session cron ingest failed');
  } finally {
    piIngestRunning = false;
  }
}

// Only start the cron if PI_SESSIONS_ROOT exists
try {
  const stat = await fs.stat(process.env.PI_SESSIONS_ROOT || '/home/err/.pi/agent/sessions');
  if (stat.isDirectory()) {
    piIngestTimerHandle = setInterval(scheduledPiSessionIngest, PI_INGEST_INTERVAL_MS);
    // Run initial ingest after 30s (let server finish booting first)
    setTimeout(scheduledPiSessionIngest, 30000);
    app.log.info(`Pi session ingestion cron scheduled every ${PI_INGEST_INTERVAL_MS / 1000}s`);
  }
} catch {
  app.log.info('Pi sessions root not found, cron ingestion disabled');
}

try {
  await app.listen({ host: config.host, port: config.port });
  app.log.info(`Knoxx backend CLJS listening on ${config.host}:${config.port}`);
} catch (error) {
  console.error('Knoxx backend CLJS failed to start', error);
  process.exit(1);
}
