#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { spawn } from 'node:child_process';

const frontendRoot = process.cwd();
const bridgeDir = path.join(frontendRoot, 'dist', 'bridge');

let pendingTimer = null;
let compileInFlight = false;
let compileAgain = false;

function isBridgeOutput(fileName) {
  return typeof fileName === 'string' && /knoxx-.*bridge\.es\.js$/.test(fileName);
}

function runShadowCompile(reason) {
  if (compileInFlight) {
    compileAgain = true;
    console.log(`[SHADOW_BRIDGE] compile already running; queued another pass (${reason})`);
    return;
  }

  compileInFlight = true;
  console.log(`[SHADOW_BRIDGE] bridge changed; running shadow-cljs compile app (${reason})`);

  const child = spawn('pnpm', ['exec', 'shadow-cljs', 'compile', 'app'], {
    cwd: frontendRoot,
    stdio: 'inherit',
  });

  child.on('exit', (code, signal) => {
    compileInFlight = false;
    if (code === 0) {
      console.log('[SHADOW_BRIDGE] shadow-cljs compile app completed');
    } else {
      console.error(`[SHADOW_BRIDGE] shadow-cljs compile app failed (code=${code ?? 'null'} signal=${signal ?? 'null'})`);
    }

    if (compileAgain) {
      compileAgain = false;
      runShadowCompile('queued-change');
    }
  });

  child.on('error', (err) => {
    compileInFlight = false;
    console.error(`[SHADOW_BRIDGE] failed to start shadow-cljs compile app: ${err.message}`);
  });
}

function scheduleCompile(reason) {
  if (pendingTimer) clearTimeout(pendingTimer);
  pendingTimer = setTimeout(() => {
    pendingTimer = null;
    runShadowCompile(reason);
  }, 250);
}

fs.mkdirSync(bridgeDir, { recursive: true });

fs.watch(bridgeDir, (eventType, fileName) => {
  if (isBridgeOutput(fileName)) {
    scheduleCompile(`${eventType}:${fileName}`);
  }
});

console.log(`[SHADOW_BRIDGE] watching ${path.relative(frontendRoot, bridgeDir)}/*.js; bridge changes trigger shadow-cljs compile app`);

process.on('SIGINT', () => process.exit(0));
process.on('SIGTERM', () => process.exit(0));
