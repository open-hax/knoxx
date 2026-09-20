import assert from 'node:assert/strict';
import { once } from 'node:events';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { managedProcess } from './wiki-stack-services.mjs';

async function running(pid) {
  try {
    const stat = await fs.readFile(`/proc/${pid}/stat`, 'utf8');
    return stat.slice(stat.lastIndexOf(')') + 2, stat.lastIndexOf(')') + 3) !== 'Z';
  } catch (error) {
    if (error.code === 'ENOENT') return false;
    throw error;
  }
}

test('owned descendants are stopped even after their process-group leader closes', { skip: process.platform !== 'linux', timeout: 10_000 }, async () => {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), 'knoxx-owned-process-'));
  const ready = path.join(directory, 'descendant.json');
  let service;
  try {
    const descendant = `
      process.on('SIGTERM', () => {});
      require('node:fs').writeFileSync(process.argv[1], JSON.stringify({pid: process.pid}));
      setInterval(() => {}, 1000);
    `;
    const leader = `
      const fs = require('node:fs');
      const child = require('node:child_process').spawn(process.execPath,
        ['-e', ${JSON.stringify(descendant)}, ${JSON.stringify(ready)}], {stdio: 'ignore'});
      child.unref();
      const wait = setInterval(() => {
        if (fs.existsSync(${JSON.stringify(ready)})) {clearInterval(wait); process.exit(0);}
      }, 10);
    `;
    service = managedProcess(process.execPath, ['-e', leader], directory, {}, path.join(directory, 'service.log'));
    const [code] = await once(service.child, 'close', { signal: AbortSignal.timeout(5000) });
    assert.equal(code, 0);
    const { pid } = JSON.parse(await fs.readFile(ready, 'utf8'));
    assert.equal(await running(pid), true, 'The fixture leaves a real owned descendant after leader exit');
    const stoppingAt = Date.now();
    await service.stop();
    assert(Date.now() - stoppingAt >= 4900, 'The group receives its grace period before force-kill');
    for (let attempt = 0; attempt < 50 && await running(pid); attempt++) {
      await new Promise(resolve => setTimeout(resolve, 10));
    }
    assert.equal(await running(pid), false, 'Cleanup must kill the orphan even when it ignores SIGTERM');
  } finally {
    if (service?.child.pid) {
      try { process.kill(-service.child.pid, 'SIGKILL'); }
      catch (error) { if (error.code !== 'ESRCH') throw error; }
    }
    await fs.rm(directory, { recursive: true, force: true });
  }
});

test('failed process creation has no process group to signal during cleanup', async () => {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), 'knoxx-owned-process-'));
  try {
    const service = managedProcess(path.join(directory, 'missing-command'), [], directory, {}, path.join(directory, 'service.log'));
    await service.stop();
    assert.throws(() => service.check(), { code: 'ENOENT' });
  } finally { await fs.rm(directory, { recursive: true, force: true }); }
});

test('an exited leader does not shorten a descendant graceful shutdown', { skip: process.platform !== 'linux', timeout: 10_000 }, async () => {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), 'knoxx-owned-grace-'));
  const ready = path.join(directory, 'descendant.json');
  const cleaned = path.join(directory, 'cleanup-finished');
  let service;
  try {
    const descendant = `
      const fs = require('node:fs');
      process.on('SIGTERM', () => setTimeout(() => {
        fs.writeFileSync(process.argv[2], 'finished');
        process.exit(0);
      }, 100));
      fs.writeFileSync(process.argv[1], JSON.stringify({pid: process.pid}));
      setInterval(() => {}, 1000);
    `;
    const leader = `
      const fs = require('node:fs');
      const child = require('node:child_process').spawn(process.execPath,
        ['-e', ${JSON.stringify(descendant)}, ${JSON.stringify(ready)}, ${JSON.stringify(cleaned)}], {stdio: 'ignore'});
      child.unref();
      const wait = setInterval(() => {
        if (fs.existsSync(${JSON.stringify(ready)})) {clearInterval(wait); process.exit(0);}
      }, 10);
    `;
    service = managedProcess(process.execPath, ['-e', leader], directory, {}, path.join(directory, 'service.log'));
    const [code] = await once(service.child, 'close', { signal: AbortSignal.timeout(5000) });
    assert.equal(code, 0);
    const { pid } = JSON.parse(await fs.readFile(ready, 'utf8'));
    assert.equal(await running(pid), true);
    await service.stop();
    assert.equal(await fs.readFile(cleaned, 'utf8'), 'finished', 'SIGTERM cleanup must finish before escalation');
    assert.equal(await running(pid), false);
  } finally {
    if (service?.child.pid) {
      try { process.kill(-service.child.pid, 'SIGKILL'); }
      catch (error) { if (error.code !== 'ESRCH') throw error; }
    }
    await fs.rm(directory, { recursive: true, force: true });
  }
});
