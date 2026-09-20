'use strict';

// Test processes only. CLJS async test wrappers can call done in finally before
// their returned promise rejects. An immediate runner process.exit(0) otherwise
// prevents Node from delivering that rejection at the end of the turn.
const { writeSync } = require('node:fs');
const installed = Symbol.for('open-hax.shadow-test-error-guard');

if (!process[installed]) {
  process[installed] = true;
  const exit = process.exit.bind(process);
  let pending = false;
  let requestedCode = 0;
  let fatal = false;

  const scheduleExit = (code = process.exitCode ?? 0) => {
    if (Number(code) !== 0) requestedCode = code;
    if (Number(process.exitCode ?? 0) !== 0) requestedCode = process.exitCode;
    if (pending) return;
    pending = true;
    // Two check phases flush the current promise queue and its immediately
    // scheduled callbacks. This is not a wait for arbitrary detached timers.
    setImmediate(() => setImmediate(() => {
      const finalCode = fatal ? 1 : (requestedCode || process.exitCode || 0);
      exit(finalCode);
    }));
  };

  const fail = (kind, error) => {
    fatal = true;
    process.exitCode = 1;
    const detail = error && error.stack ? error.stack : String(error);
    writeSync(2, `[shadow-test-guard] FATAL ${kind}: ${detail}\n`);
    scheduleExit(1);
  };

  process.on('unhandledRejection', (error) => fail('unhandled rejection', error));
  process.on('uncaughtException', (error) => fail('uncaught exception', error));
  process.exit = scheduleExit;
}
