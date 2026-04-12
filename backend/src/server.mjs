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
import { promisify } from 'node:util';
import nodemailer from 'nodemailer';
import { createPolicyDb } from './policy-db.mjs';
import { config as readConfig, registerAppRoutes, registerWsRoutes } from '../dist/app.js';

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
await app.register(fastifyMultipart);
await app.register(fastifyWebsocket);

await app.register((instance, _opts, done) => {
  registerWsRoutes(runtime, instance);
  done();
});
registerAppRoutes(runtime, app);

try {
  await app.listen({ host: config.host, port: config.port });
  app.log.info(`Knoxx backend CLJS listening on ${config.host}:${config.port}`);
} catch (error) {
  console.error('Knoxx backend CLJS failed to start', error);
  process.exit(1);
}
