import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { resolve, join } from 'node:path';
import { testEnvironment } from '../backend/scripts/shadow-test-environment.mjs';

const root = resolve(fileURLToPath(new URL('../', import.meta.url)));
const backend = join(root, 'backend');
const args = process.argv.slice(2);
if (args.length && (args.length !== 2 || args[0] !== '--mongod')) {
  throw new Error('Usage: node scripts/verify-run-event-providers.mjs [--mongod /absolute/path/to/mongod]');
}
const mongod = args[1] || process.env.KNOXX_TEST_MONGOD;
const revision = spawnSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' });
if (revision.status !== 0) throw new Error('Run this verifier in its Git checkout');
console.log('Verifying current checkout ' + revision.stdout.trim());

function run(command, commandArgs, environment) {
  return new Promise((resolveRun, rejectRun) => {
    const child = spawn(command, commandArgs, {
      cwd: backend, env: testEnvironment({ ...process.env, ...environment }),
      stdio: ['ignore', 'pipe', 'pipe']
    });
    let output = '';
    let timedOut = false;
    const timer = setTimeout(() => {
      timedOut = true;
      child.kill('SIGTERM');
    }, 300_000);
    for (const stream of [child.stdout, child.stderr]) {
      stream.on('data', chunk => { output += chunk.toString(); process.stdout.write(chunk); });
    }
    child.once('error', error => { clearTimeout(timer); rejectRun(error); });
    child.once('close', (code, signal) => {
      clearTimeout(timer);
      if (timedOut || code !== 0 || output.includes('[shadow-test-guard] FATAL')) {
        rejectRun(new Error(command + ' failed: code=' + code + ', signal=' + signal + ', timedOut=' + timedOut));
      } else resolveRun(output);
    });
  });
}

async function prove(native) {
  const environment = { KNOXX_RUN_EVENTS_NATIVE: native ? '1' : '0' };
  if (mongod) environment.KNOXX_TEST_MONGOD = mongod;
  const compiled = await run('clojure', ['-M:cljs', 'scripts/compile-run-events-proof.clj'], environment);
  if (!/Build completed\./.test(compiled) || /,\s*[1-9]\d* warnings/.test(compiled)) {
    throw new Error('Compilation did not prove a warning-free build');
  }
  const build = native ? 'run-events-native-proof' : 'run-events-proof';
  const output = await run(process.execPath, [
    '--require', './scripts/shadow-test-error-guard.cjs',
    'target/' + build + '/tests.cjs'
  ], environment);
  const summaries = [...output.matchAll(/Ran (\d+) tests containing (\d+) assertions\.\s*(\d+) failures, (\d+) errors\./g)];
  if (!summaries.length || summaries.some(([, tests, assertions, failures, errors]) =>
    Number(tests) < 1 || Number(assertions) < 1 || Number(failures) !== 0 || Number(errors) !== 0)) {
    throw new Error('A positive test/assertion summary with zero failures and errors is required');
  }
  console.log(native ? 'PASS: actual Mongo concurrent admission, process restart, retry and expiry' :
    'PASS: selected provider events, authorized durable routes and producer identity');
}

await prove(false);
if (mongod) await prove(true);
else console.warn('NOT VERIFIED: native Mongo proof requires --mongod or KNOXX_TEST_MONGOD.');
console.log('Each native fixture owns its temporary directory and process; no application database is used.');
