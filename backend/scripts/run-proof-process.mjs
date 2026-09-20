import { spawn } from 'node:child_process';

/**
 * Run an owned proof process and return its combined output only on success.
 * Reject on spawn failure, timeout, nonzero exit, or the fatal async-test marker.
 * On POSIX, terminate its process group and escalate after killGraceMs so a
 * descendant cannot retain output pipes after the direct child exits.
 */
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
    /** Signal the owned process group, or the direct child on Windows. */
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
    /** Release both deadline timers after process failure or final close. */
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
