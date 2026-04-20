import { spawn } from 'node:child_process';
import { readdirSync, existsSync } from 'node:fs';
import { join, relative } from 'node:path';

const SHADOW_CMD = process.platform === 'win32' ? 'shadow-cljs.cmd' : 'shadow-cljs';
const TEST_BUILD  = 'test-ci';
const TEST_BUNDLE = 'target/test/test-ci.cjs';

// Shadow emits a flat cljs-runtime/ dir with dot-namespaced filenames, e.g.:
//   knoxx.backend.authz.js
//   knoxx.backend.app_shapes.js
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
  // 1) Compile — :optimizations :none emits individual flat .js files.
  {
    const res = await spawnP(SHADOW_CMD, ['compile', TEST_BUILD]);
    if (res.signal) { console.error(`[knoxx] signal ${res.signal}`); process.exit(res.code ?? 1); }
    if (res.code !== 0) process.exit(res.code ?? 1);
  }

  // 2) Collect production knoxx.backend.*.js files from the flat runtime dir.
  //    Filenames look like: knoxx.backend.authz.js, knoxx.backend.app_shapes.js
  //    Exclude test files:  knoxx.backend.authz_test.js etc.
  if (!existsSync(CLJS_RUNTIME)) {
    console.error(`[knoxx] cljs-runtime dir not found: ${CLJS_RUNTIME}`);
    process.exit(1);
  }

  const prodFiles = readdirSync(CLJS_RUNTIME)
    .filter(name =>
      name.startsWith('knoxx.backend.') &&
      name.endsWith('.js') &&
      !name.endsWith('.js.map') &&
      !name.includes('_test.') &&
      !name.includes('-test.'))
    .map(name => relative(process.cwd(), join(CLJS_RUNTIME, name)));

  if (prodFiles.length === 0) {
    console.error('[knoxx] No knoxx.backend production files found after filtering.');
    process.exit(1);
  }

  console.log(`[knoxx] Coverage includes ${prodFiles.length} files:`);
  prodFiles.forEach(f => console.log('  ', f));

  // 3) Run under c8 with explicit --include per file.
  const c8Args = [
    'c8',
    '--reporter=text',
    '--reporter=json-summary',
    '--reporter=lcov',
    '--reports-dir', 'coverage',
    ...prodFiles.flatMap(f => ['--include', f]),
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
