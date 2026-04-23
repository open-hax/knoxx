import Fastify from "fastify";
import fastifyCors from "@fastify/cors";
import fastifyWebsocket from "@fastify/websocket";
import fastifyMultipart from "@fastify/multipart";
import fastifyCookie from "@fastify/cookie";
import fastifyFormbody from "@fastify/formbody";

import { Type } from "@sinclair/typebox";
import * as sdk from "@mariozechner/pi-coding-agent";

import * as crypto from "node:crypto";
import * as fs from "node:fs/promises";
import * as path from "node:path";
import * as os from "node:os";
import { execFile } from "node:child_process";
import { promisify } from "node:util";
import { createRequire } from "node:module";

import nodemailer from "nodemailer";

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { isInitializeRequest } from "@modelcontextprotocol/sdk/types.js";
import { z } from "zod";

import { bootstrap } from "../dist/app.js";

// Some CLJS modules still use js/require. Provide a CJS-like require in ESM.
globalThis.require = globalThis.require || createRequire(import.meta.url);

await bootstrap({
  Fastify,
  fastifyCors,
  fastifyWebsocket,
  fastifyMultipart,
  fastifyCookie,
  fastifyFormbody,
  Type,
  sdk,
  crypto,
  fs,
  path,
  os,
  execFileAsync: promisify(execFile),
  nodemailer,
  // MCP deps
  McpServer,
  StreamableHTTPServerTransport,
  isInitializeRequest,
  // Schema/runtime helpers
  z,
});
