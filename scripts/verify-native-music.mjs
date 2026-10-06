import assert from 'node:assert/strict';
import { createHash, randomUUID } from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { testCountersExitCode } from '../backend/scripts/run-shadow-tests-ci.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const backend = path.join(root, 'backend');
const [nodeMajor, nodeMinor] = process.versions.node.split('.').map(Number);
assert.ok(nodeMajor > 22 || (nodeMajor === 22 && nodeMinor >= 19), 'backend requires Node >=22.19.0');
const require = createRequire(path.join(backend, 'package.json'));
const pass = (message) => console.log(`PASS ${message}`);
function command(program, args, options = {}) {
  const result = spawnSync(program, args, { cwd: backend, encoding: 'utf8', timeout: 600000, ...options });
  if (result.error || result.status !== 0) throw new Error(`${program} failed (${result.status ?? result.error?.code})`);
  return result.stdout;
}
const revision = command('git', ['rev-parse', 'HEAD']).trim();

function sourceProof() {
  const dirty = Boolean(command('git', ['status', '--porcelain']).trim());
  console.log(`SOURCE revision=${revision} dirty=${dirty} checkout=${root}`);
  const cli = require.resolve('shadow-cljs/cli/runner.js');
  const result = spawnSync(process.execPath, [cli, '--force-spawn', '--config-merge',
    '{:ns-regexp "^knoxx\\\\.backend\\\\.extern\\\\.native-music-test$" :output-to "target/native-music-test/test.cjs"}',
    'compile', 'test'], {
    cwd: backend, encoding: 'utf8', timeout: 600000, maxBuffer: 8 * 1024 * 1024,
    env: { ...process.env, CONTRACTS_DIR: 'test/fixtures/empty-contracts',
      NODE_OPTIONS: `${process.env.NODE_OPTIONS ?? ''} --require ${path.join(backend, 'scripts/shadow-test-error-guard.cjs')}` },
  });
  const output = `${result.stdout ?? ''}${result.stderr ?? ''}`;
  process.stdout.write(output);
  assert.equal(result.status, 0, 'Shadow compile/test process must exit zero');
  assert.equal(testCountersExitCode(output), 0, 'a nonempty, zero-failure test summary is required');
  assert.match(output, /Testing knoxx.backend.extern.native-music-test/, 'native regression namespace must execute');
  pass('real execFile object, decoded metadata, RIFF/WAVE and nonzero PCM verified; fixture WAV removed');
  console.log('WARN source proof does not verify a deployed server. Use --live-container after deployment.');
}

// The same bounded script hashes only deployed JS and the existing synthesis engine.
const manifestScript = `
  const fs = require('node:fs'), path = require('node:path'), crypto = require('node:crypto');
  const root = process.cwd(), files = [];
  function scan(dir) { for (const entry of fs.readdirSync(dir, {withFileTypes:true})) {
    const file = path.join(dir, entry.name);
    if (entry.isDirectory()) scan(file); else if (entry.isFile() && file.endsWith('.js')) files.push(file);
  }}
  scan(path.join(root, 'dist')); files.push(path.join(root, 'scripts/synthesize-music.mjs'));
  console.log(JSON.stringify(files.sort().map(file => ({path:path.relative(root,file),
    sha256:crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex'),
    modified:fs.statSync(file).mtimeMs}))));
`;

function liveIdentity(container, url) {
  assert.equal(command('git', ['status', '--porcelain', '--untracked-files=no']).trim(), '', 'commit changes before live proof');
  const format = '{"running":{{json .State.Running}},"started":{{json .State.StartedAt}},"revision":{{json (index .Config.Labels "org.opencontainers.image.revision")}},"cwd":{{json .Config.WorkingDir}},"cmd":{{json .Config.Cmd}},"ports":{{json .NetworkSettings.Ports}},"mounts":{{json .Mounts}}}';
  const info = JSON.parse(command('docker', ['inspect', '--format', format, container]));
  assert.equal(info.running, true, 'container must be running');
  assert.equal(info.revision, revision, 'server image revision label must equal checkout HEAD');
  assert.equal(info.cwd, '/app');
  assert.deepEqual(info.cmd, ['node', 'dist/server.js']);
  assert.equal(url.hostname, '127.0.0.1', 'live proof requires the local Docker published port');
  assert.equal(url.protocol, 'http:');
  assert.equal(`${url.username}${url.password}${url.search}${url.hash}`, '', 'use a plain loopback URL');
  assert.ok(process.env.NODE_USE_ENV_PROXY !== '1' && !process.execArgv.includes('--use-env-proxy'), 'credential proof requires direct loopback transport');
  assert.equal(url.pathname, '/');
  assert.ok(info.ports['8000/tcp']?.some(port => port.HostPort === url.port && ['127.0.0.1', '0.0.0.0'].includes(port.HostIp)), 'URL must select this container');
  for (const mount of info.mounts) {
    assert.ok(!['/app/dist', '/app/scripts'].some(target => target === mount.Destination || target.startsWith(`${mount.Destination}/`) || mount.Destination.startsWith(`${target}/`)), 'runtime code must come from the labelled image');
  }
  const local = JSON.parse(command(process.execPath, ['-e', manifestScript]));
  const deployed = JSON.parse(command('docker', ['exec', '-w', '/app', container, 'node', '-e', manifestScript]));
  assert.ok(local.some(file => file.path === 'dist/server.js'), 'build the production server first');
  assert.deepEqual(deployed.map(({ path, sha256 }) => ({ path, sha256 })), local.map(({ path, sha256 }) => ({ path, sha256 })), 'deployed code/engine hashes must match local build');
  assert.ok(deployed.every(file => file.modified <= Date.parse(info.started)), 'runtime code must predate server startup');
  pass(`server revision=${revision}; published port, compiled JS and native engine hashes match`);
}

async function liveProof(container, url) {
  liveIdentity(container, url);
  assert.ok(process.env.KNOXX_MCP_TOKEN, 'supply KNOXX_MCP_TOKEN through the existing approved credential mechanism');
  const endpoint = new URL('/mcp', url);
  const unauthenticated = await fetch(endpoint, { method: 'POST', headers: { 'content-type': 'application/json', accept: 'application/json, text/event-stream' }, body: JSON.stringify({ jsonrpc: '2.0', id: 1, method: 'tools/list' }), signal: AbortSignal.timeout(15000) });
  assert.equal(unauthenticated.status, 401, 'unauthenticated MCP must be refused');
  pass('unauthenticated music surface refused with HTTP 401');
  const { Client } = require('@modelcontextprotocol/sdk/client/index.js');
  const { StreamableHTTPClientTransport } = require('@modelcontextprotocol/sdk/client/streamableHttp.js');
  const client = new Client({ name: 'knoxx-native-music-proof', version: '1' });
  const directory = `/app/workspace/Music/generated/.verify-native-music-${randomUUID()}`;
  const outputPath = `${directory.slice('/app/workspace/'.length)}/proof.wav`;
  const cleanup = () => command('docker', ['exec', container, 'node', '-e', 'require("node:fs").rmSync(process.argv[1],{recursive:true,force:true})', directory]);
  for (const signal of ['SIGINT', 'SIGTERM']) process.once(signal, () => { try { cleanup(); } finally { process.exit(130); } });
  try {
    await client.connect(new StreamableHTTPClientTransport(endpoint, { requestInit: { headers: { Authorization: `Bearer ${process.env.KNOXX_MCP_TOKEN}` } } }));
    const tools = await client.listTools();
    const tool = tools.tools.find(tool => ['music_generate', 'music.generate'].includes(tool.name));
    assert.ok(tool, 'authenticated actor must have music.generate');
    const result = await client.callTool({ name: tool.name, arguments: { spec_json: { bpm: 120, duration: 0.5, tracks: [{ instrument: 'synth', waveform: 'sine', notes: [{ note: 'A4', time: 0, duration: 0.25 }] }] }, output_path: outputPath } }, undefined, { timeout: 150000 });
    assert.ok(!result.isError, 'native tool call must succeed');
    const metadata = result.details ?? result.structuredContent;
    assert.equal(metadata?.ok, true, 'tool must return decoded native metadata');
    assert.equal(metadata.workspacePath ?? metadata['workspace-path'], outputPath);
    assert.equal(metadata.sampleRate, 44100);
    assert.equal(metadata.channels, 2);
    const wav = spawnSync('docker', ['exec', container, 'cat', `${directory}/proof.wav`], { maxBuffer: 1024 * 1024 });
    assert.equal(wav.status, 0, 'read actual generated WAV');
    assert.equal(wav.stdout.toString('ascii', 0, 4), 'RIFF');
    assert.equal(wav.stdout.toString('ascii', 8, 12), 'WAVE');
    assert.equal(wav.stdout.readUInt32LE(24), metadata.sampleRate);
    assert.equal(wav.stdout.readUInt16LE(22), metadata.channels);
    assert.equal(wav.stdout.length, 44 + metadata.samples * metadata.channels * 2);
    assert.ok(wav.stdout.subarray(44).some(byte => byte !== 0), 'PCM must contain sound');
    pass(`authenticated ${tool.name}: ${metadata.samples} frames, ${wav.stdout.length} WAV bytes, sha256=${createHash('sha256').update(wav.stdout).digest('hex')}`);
  } finally { try { await client.close(); } finally { cleanup(); } }
  pass('dedicated live WAV directory removed');
}

try {
  const args = process.argv.slice(2);
  if (!args.length || (args.length === 1 && args[0] === '--source')) sourceProof();
  else if (args.length === 3 && args[0] === '--live-container') await liveProof(args[1], new URL(args[2]));
  else throw new Error('Usage: scripts/verify-native-music.sh [--source | --live-container NAME http://127.0.0.1:PORT]');
} catch (error) { console.error(`FAIL ${error.message}`); process.exitCode = 1; }
