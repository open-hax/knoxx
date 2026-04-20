import { spawn } from 'node:child_process';

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

    const onChunk = (chunk, stream) => {
      const text = chunk.toString();
      combined += text;
      stream.write(chunk);
    };

    child.stdout.on('data', (chunk) => onChunk(chunk, process.stdout));
    child.stderr.on('data', (chunk) => onChunk(chunk, process.stderr));

    child.on('error', (err) => reject(err));

    child.on('close', (code, signal) => {
      resolve({ code, signal, combined });
    });
  });
}

function exitForTestCounters(output) {
  const match = output.match(/\b(\d+) failures?,\s*(\d+) errors?\./);
  if (match) {
    const failures = Number(match[1] || 0);
    const errors = Number(match[2] || 0);
    process.exit(failures > 0 || errors > 0 ? 1 : 0);
  }

  // If we can't parse counters, err on the side of failing CI.
  console.error('[knoxx] Could not determine CLJS test result counters from output.');
  process.exit(1);
}

async function main() {
  // 1) Compile the CI test bundle (no autorun, :optimizations :none so that
  //    V8/c8 sees individual cljs-runtime/*.js files rather than one minified
  //    bundle.  Sourcemaps remap hits back to src/cljs/knoxx/** for display.
  {
    const res = await spawnP(SHADOW_CMD, ['compile', TEST_BUILD]);

    if (res.signal) {
      console.error(`[knoxx] shadow-cljs terminated by signal ${res.signal}`);
      process.exit(res.code ?? 1);
    }

    if (res.code !== 0) {
      process.exit(res.code ?? 1);
    }
  }

  // 2) Run the bundle under c8.
  //
  //    With :optimizations :none, shadow emits individual files under
  //    target/test/cljs-runtime/.  We include only the knoxx/backend/**
  //    runtime files and exclude test namespaces (*_test*) so the numbers
  //    reflect production code only.
  //
  //    --all is intentionally omitted: it enumerates raw JS, not .cljs,
  //    and inflates 0-coverage lines for files never loaded.
  const c8Args = [
    'c8',
    '--reporter=text',
    '--reporter=json-summary',
    '--reporter=lcov',
    '--reports-dir', 'coverage',
    '--include', 'target/test/cljs-runtime/knoxx/backend/**',
    '--exclude', 'target/test/cljs-runtime/knoxx/backend/**_test*',
    'node',
    TEST_BUNDLE,
  ];

  const res = await spawnP('pnpm', ['exec', ...c8Args]);

  if (res.signal) {
    console.error(`[knoxx] coverage run terminated by signal ${res.signal}`);
    process.exit(res.code ?? 1);
  }

  if (res.code !== 0) {
    console.error(`[knoxx] c8 exited with non-zero status ${res.code}`);
    process.exit(1);
  }

  exitForTestCounters(res.combined);
}

main().catch((err) => {
  console.error('[knoxx] Backend coverage runner failed:', err);
  process.exit(1);
});
