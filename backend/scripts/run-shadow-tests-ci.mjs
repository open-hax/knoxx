import { spawn } from 'node:child_process';
import { pathToFileURL } from 'node:url';

const supportedBuilds = new Set(['test', 'e2e']);

export function normalizeBuild(rawBuild) {
  const build = rawBuild ?? 'test';
  if (!supportedBuilds.has(build)) {
    throw new Error(`Unsupported shadow-cljs test build: ${build}`);
  }
  return build;
}

export function parseTestCounters(output) {
  const matches = [...String(output).matchAll(/\b(\d+) failures?,\s*(\d+) errors?\./g)];
  const match = matches.at(-1);
  if (!match) return null;
  return {
    failures: Number(match[1]),
    errors: Number(match[2]),
  };
}

export function testCountersExitCode(output) {
  const counters = parseTestCounters(output);
  if (!counters) return 1;
  return counters.failures > 0 || counters.errors > 0 ? 1 : 0;
}

export function run(rawBuild) {
  const build = normalizeBuild(rawBuild);
  const cmd = process.platform === 'win32' ? 'shadow-cljs.cmd' : 'shadow-cljs';
  const args = ['compile', build];

  const child = spawn(cmd, args, {
    stdio: ['ignore', 'pipe', 'pipe'],
    env: {
      ...process.env,
      // Hermetic tests: never let default contract-root resolution reach the
      // live, operator-owned contracts/ folder. Tests that need contracts must
      // pass an explicit :contracts-dir pointing at a test/fixtures snapshot.
      CONTRACTS_DIR: process.env.CONTRACTS_DIR ?? 'test/fixtures/empty-contracts',
    },
  });

  let combined = '';

  const onChunk = (chunk, stream) => {
    const text = chunk.toString();
    combined += text;
    stream.write(chunk);
  };

  child.stdout.on('data', (chunk) => onChunk(chunk, process.stdout));
  child.stderr.on('data', (chunk) => onChunk(chunk, process.stderr));

  child.on('error', (err) => {
    console.error('[knoxx] Failed to spawn shadow-cljs:', err);
    process.exit(1);
  });

  child.on('close', (code, signal) => {
    if (signal) {
      console.error(`[knoxx] shadow-cljs terminated by signal ${signal}`);
      process.exit(code ?? 1);
      return;
    }

    if (code !== 0) {
      process.exit(code ?? 1);
      return;
    }

    const counters = parseTestCounters(combined);
    if (counters) {
      process.exit(testCountersExitCode(combined));
      return;
    }

    // If we can't parse counters, err on the side of failing CI.
    console.error('[knoxx] Could not determine CLJS test result counters from shadow-cljs output.');
    process.exit(1);
  });
}

const invokedDirectly = process.argv[1]
  && import.meta.url === pathToFileURL(process.argv[1]).href;

if (invokedDirectly) {
  try {
    run(process.argv[2]);
  } catch (err) {
    console.error(`[knoxx] ${err.message}`);
    process.exit(1);
  }
}
