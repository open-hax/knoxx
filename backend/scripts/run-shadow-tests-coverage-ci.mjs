import { spawn } from 'node:child_process';
import { readdirSync, existsSync } from 'node:fs';
import { join } from 'node:path';

const SHADOW_CMD = process.platform === 'win32' ? 'shadow-cljs.cmd' : 'shadow-cljs';
const TEST_BUILD = 'test-ci';
const TEST_BUNDLE = 'target/test/test-ci.cjs';

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

// Walk a directory up to `depth` levels, printing every file path.
function walkDir(dir, depth = 4, prefix = '') {
  if (!existsSync(dir)) { console.log(`  [not found] ${dir}`); return; }
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name);
    console.log(`  ${prefix}${entry.name}`);
    if (entry.isDirectory() && depth > 0) walkDir(full, depth - 1, prefix + '  ');
  }
}

async function main() {
  // 1) Compile
  {
    const res = await spawnP(SHADOW_CMD, ['compile', TEST_BUILD]);
    if (res.signal) { console.error(`[knoxx] signal ${res.signal}`); process.exit(res.code ?? 1); }
    if (res.code !== 0) process.exit(res.code ?? 1);
  }

  // 2) Diagnostic: show what shadow emitted under target/test/
  console.log('\n[knoxx-diag] target/test/ tree:');
  walkDir('target/test', 3);

  // 3) Run c8 with NO --include/--exclude first so we see raw coverage output.
  console.log('\n[knoxx-diag] running c8 with no include filter:');
  {
    const c8Args = [
      'c8',
      '--reporter=text',
      '--reports-dir', 'coverage',
      'node', TEST_BUNDLE,
    ];
    await spawnP('pnpm', ['exec', ...c8Args]);
  }

  // 4) Now run the real scoped coverage pass.
  console.log('\n[knoxx-diag] running c8 with knoxx/backend include:');
  const c8Args = [
    'c8',
    '--reporter=text',
    '--reporter=json-summary',
    '--reporter=lcov',
    '--reports-dir', 'coverage',
    '--include', 'target/test/cljs-runtime/knoxx/backend/**',
    '--exclude', 'target/test/cljs-runtime/knoxx/backend/**_test*',
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
