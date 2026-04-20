import { spawn } from 'node:child_process';

const SHADOW_CMD = process.platform === 'win32' ? 'shadow-cljs.cmd' : 'shadow-cljs';
const TEST_BUILD = 'test-ci';
const TEST_BUNDLE = 'target/test/test-ci.cjs';

// Shadow-cljs emits individual cljs-runtime files under:
//   .shadow-cljs/builds/test-ci/dev/out/cljs-runtime/
// (not target/test/) when :optimizations :none is set.
const CLJS_RUNTIME = '.shadow-cljs/builds/test-ci/dev/out/cljs-runtime';

function spawnP(cmd, args, opts = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(cmd, args, {
      stdio: ['ignore', 'pipe', 'pipe'],
      env: process.env,
      ...opts,
    });

    let combined = '';
    const onChunk = (chunk, stream) => { combined += chunk.toString(); stream.write(chunk); };
    child.stdout.on('data', (chunk) => onChunk(chunk, process.stdout));
    child.stderr.on('data', (chunk) => onChunk(chunk, process.stderr));
    child.on('error', reject);
    child.on('close', (code, signal) => resolve({ code, signal, combined }));
  });
}

function exitForTestCounters(output) {
  const match = output.match(/\b(\d+) failures?,\s*(\d+) errors?\./);
  if (match) {
    const failures = Number(match[1] || 0);
    const errors   = Number(match[2] || 0);
    process.exit(failures > 0 || errors > 0 ? 1 : 0);
  }
  console.error('[knoxx] Could not determine CLJS test result counters from output.');
  process.exit(1);
}

async function main() {
  // 1) Compile with :optimizations :none so shadow emits individual
  //    cljs-runtime/*.js files that V8/c8 can instrument per-file.
  {
    const res = await spawnP(SHADOW_CMD, ['compile', TEST_BUILD]);
    if (res.signal) { console.error(`[knoxx] signal ${res.signal}`); process.exit(res.code ?? 1); }
    if (res.code !== 0) process.exit(res.code ?? 1);
  }

  // 2) Run under c8.
  //    --include  → knoxx/backend production sources only
  //    --exclude  → test namespaces + everything outside knoxx/backend
  const c8Args = [
    'c8',
    '--reporter=text',
    '--reporter=json-summary',
    '--reporter=lcov',
    '--reports-dir', 'coverage',
    '--include', `${CLJS_RUNTIME}/knoxx/backend/**`,
    '--exclude', `${CLJS_RUNTIME}/knoxx/backend/**_test*`,
    '--exclude', `${CLJS_RUNTIME}/knoxx/backend/**-test*`,
    'node', TEST_BUNDLE,
  ];

  const res = await spawnP('pnpm', ['exec', ...c8Args]);
  if (res.signal) { console.error(`[knoxx] signal ${res.signal}`); process.exit(res.code ?? 1); }
  if (res.code !== 0) { console.error(`[knoxx] c8 exit ${res.code}`); process.exit(1); }

  exitForTestCounters(res.combined);
}

main().catch((err) => {
  console.error('[knoxx] coverage runner failed:', err);
  process.exit(1);
});
