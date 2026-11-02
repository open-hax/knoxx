// Native subprocess orchestration; ledger behavior stays in the compiled CLJS adapter.
const assert = require('node:assert/strict');
const { spawn } = require('node:child_process');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const probePath = path.resolve(__dirname, '../target/openplanner-process-proof/probe.cjs');
const probe = require(probePath);

function worker(directory, label) {
  const child = spawn(process.execPath, ['-e',
    'require(process.argv[1]).write(process.argv[2],process.argv[3]).then(()=>process.exit(0),e=>{console.error(e);process.exit(1)});',
    probePath, directory, label], { stdio: ['ignore', 'pipe', 'pipe', 'ipc'] });
  let output = '';
  let readyResolve;
  let readyReject;
  const ready = new Promise((resolve, reject) => { readyResolve = resolve; readyReject = reject; });
  child.stdout.on('data', chunk => { output += chunk; });
  child.stderr.on('data', chunk => { output += chunk; });
  child.on('message', message => { if (message?.ready === true) readyResolve(); });
  const completion = new Promise((resolve, reject) => {
    child.once('error', error => { readyReject(error); reject(error); });
    child.once('close', (code, signal) => {
      if (code === 0) resolve();
      else {
        const error = new Error(`${label}: code=${code}, signal=${signal}\n${output}`);
        readyReject(error);
        reject(error);
      }
    });
  });
  // Attach handlers before waiting for both ready messages; a refused startup is fatal.
  const observed = Promise.all([ready, completion]);
  observed.catch(() => {});
  return { child, ready, completion, observed };
}

async function main() {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'knoxx-openplanner-process-'));
  const workers = [];
  let timer;
  try {
    probe.prepare(directory);
    workers.push(worker(directory, 'left'), worker(directory, 'right'));
    const limit = new Promise((_, reject) => {
      timer = setTimeout(() => reject(new Error('Native OpenPlanner writers exceeded 20 seconds')), 20_000);
    });
    await Promise.race([Promise.all(workers.map(item => item.ready)), limit]);
    for (const item of workers) item.child.send({ start: true });
    await Promise.race([Promise.all(workers.map(item => item.observed)), limit]);
    const result = await probe.verify(directory);
    assert.deepEqual(result, { ok: true, processes: 2, events: 8, vectors: 8, operations: 16 });
    console.log(JSON.stringify(result));
  } finally {
    clearTimeout(timer);
    for (const item of workers) {
      if (item.child.exitCode === null && item.child.signalCode === null) item.child.kill('SIGKILL');
    }
    await Promise.allSettled(workers.map(item => item.completion));
    fs.rmSync(directory, { recursive: true, force: true });
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
