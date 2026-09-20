import { spawn } from 'node:child_process';

// A proof owns its process group so a timed-out compiler's descendants cannot
// retain output pipes or escape cleanup when the direct child ignores SIGTERM.
export function runProofProcess(command, args, {
  cwd, env = process.env, timeoutMs = 300_000, killGraceMs = 5_000,
  onOutput = text => process.stdout.write(text)
} = {}) {
  return new Promise((resolve, reject) => {
    const grouped = process.platform !== 'win32';
    const child = spawn(command, args, { cwd, env, detached: grouped, stdio: ['ignore', 'pipe', 'pipe'] });
    let output = '';
    let timedOut = false;
    let killTimer;
    function signal(name) {
      try {
        if (grouped && child.pid) process.kill(-child.pid, name);
        else child.kill(name);
      } catch (error) {
        if (error.code !== 'ESRCH') reject(error);
      }
    }
    const timer = setTimeout(() => {
      timedOut = true;
      signal('SIGTERM');
      killTimer = setTimeout(() => signal('SIGKILL'), killGraceMs);
    }, timeoutMs);
    const clearTimers = () => { clearTimeout(timer); clearTimeout(killTimer); };
    for (const stream of [child.stdout, child.stderr]) {
      stream.on('data', chunk => { const text = chunk.toString(); output += text; onOutput(text); });
    }
    child.once('error', error => { clearTimers(); reject(error); });
    child.once('close', (code, receivedSignal) => {
      clearTimers();
      if (timedOut || code !== 0 || output.includes('[shadow-test-guard] FATAL')) {
        reject(new Error(command + ' failed: code=' + code + ', signal=' + receivedSignal + ', timedOut=' + timedOut));
      } else resolve(output);
    });
  });
}
