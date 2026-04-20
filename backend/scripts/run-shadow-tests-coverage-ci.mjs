import { spawn } from 'node:child_process';
import { readdirSync, existsSync } from 'node:fs';
import { join, relative } from 'node:path';

const SHADOW_CMD = process.platform === 'win32' ? 'shadow-cljs.cmd' : 'shadow-cljs';
const TEST_BUILD  = 'test-ci';
const TEST_BUNDLE = 'target/test/test-ci.cjs';

// Shadow emits individual cljs-runtime files here when :optimizations :none.
const CLJS_RUNTIME = '.shadow-cljs/builds/test-ci/dev/out/cljs-runtime';
const KNOXX_BACKEND_RT = join(CLJS_RUNTIME, 'knoxx', 'backend');

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

// Recursively collect all .cljs / .js files under a directory.
function collectFiles(dir) {
  if (!existsSync(dir)) return [];
  const results = [];
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name);
    if (entry.isDirectory()) results.push(...collectFiles(full));
    else results.push(full);
  }
  return results;
}

async function main() {
  // 1) Compile — :optimizations :none emits individual cljs-runtime/*.js
  {
    const res = await spawnP(SHADOW_CMD, ['compile', TEST_BUILD]);
    if (res.signal) { console.error(`[knoxx] signal ${res.signal}`); process.exit(res.code ?? 1); }
    if (res.code !== 0) process.exit(res.code ?? 1);
  }

  // 2) Enumerate production source files explicitly.
  //    Glob matching against dotfile directories is unreliable in c8/minimatch;
  //    passing explicit --include paths for each file bypasses that entirely.
  const allFiles  = collectFiles(KNOXX_BACKEND_RT);
  const prodFiles = allFiles.filter(f => !f.includes('_test') && !f.includes('-test'));

  if (prodFiles.length === 0) {
    console.error(`[knoxx] No production files found under ${KNOXX_BACKEND_RT}`);
    console.error('[knoxx] Listing CLJS_RUNTIME root:');
    if (existsSync(CLJS_RUNTIME)) {
      for (const e of readdirSync(CLJS_RUNTIME)) console.error('  ', e);
    } else {
      console.error('  (directory does not exist)');
    }
    process.exit(1);
  }

  const includeArgs = prodFiles.flatMap(f => ['--include', relative(process.cwd(), f)]);

  const c8Args = [
    'c8',
    '--reporter=text',
    '--reporter=json-summary',
    '--reporter=lcov',
    '--reports-dir', 'coverage',
    ...includeArgs,
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
