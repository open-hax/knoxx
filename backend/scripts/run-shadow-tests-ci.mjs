import { spawn } from 'node:child_process';

function run() {
  const cmd = process.platform === 'win32' ? 'shadow-cljs.cmd' : 'shadow-cljs';
  const args = ['compile', 'test'];

  const child = spawn(cmd, args, {
    stdio: ['ignore', 'pipe', 'pipe'],
    env: process.env,
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

  child.on('close', (code) => {
    if (code !== 0) {
      process.exit(code);
      return;
    }

    const match = combined.match(/\b(\d+) failures?,\s*(\d+) errors?\./);
    if (match) {
      const failures = Number(match[1] || 0);
      const errors = Number(match[2] || 0);
      process.exit(failures > 0 || errors > 0 ? 1 : 0);
      return;
    }

    // If we can't parse counters, err on the side of failing CI.
    console.error('[knoxx] Could not determine CLJS test result counters from shadow-cljs output.');
    process.exit(1);
  });
}

run();
