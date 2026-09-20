import assert from 'node:assert/strict';
import { test } from 'node:test';
import { runProofProcess } from '../../scripts/run-proof-process.mjs';

const quiet = { onOutput() {} };
test('proof deadline kills a process that ignores SIGTERM', { timeout: 5000 }, async () => {
  let observed = '';
  await assert.rejects(runProofProcess(process.execPath, ['-e', `
    process.on('SIGTERM', () => process.stdout.write('ignored'));
    process.stdout.write('ready');
    setInterval(() => {}, 1000);
  `], { timeoutMs: 500, killGraceMs: 50, onOutput: text => { observed += text; } }), /timedOut=true/);
  assert.match(observed, /ready/);
  if (process.platform !== 'win32') assert.match(observed, /ignored/);
});

test('proof timeout also closes pipes inherited by a descendant', {
  timeout: 5000, skip: process.platform === 'win32'
}, async () => {
  await assert.rejects(runProofProcess(process.execPath, ['-e', `
    const { spawn } = require('node:child_process');
    spawn(process.execPath, ['-e', "process.on('SIGTERM', () => {}); setInterval(() => {}, 1000)"], { stdio: 'inherit' });
    process.on('SIGTERM', () => {});
    setInterval(() => {}, 1000);
  `], { ...quiet, timeoutMs: 500, killGraceMs: 50 }), /timedOut=true/);
});

test('proof process returns output and surfaces spawn or guarded failures', async () => {
  assert.equal(await runProofProcess(process.execPath, ['-e', "process.stdout.write('passed')"], quiet), 'passed');
  await assert.rejects(runProofProcess('/definitely-missing-knoxx-proof', [], quiet), /ENOENT/);
  await assert.rejects(runProofProcess(process.execPath, ['-e', "console.log('[shadow-test-guard] FATAL')"], quiet), /failed/);
});
