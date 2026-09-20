import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import { execFileSync, spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { collect, pin, restoreArchive } from '../../../scripts/sandbox-clj-kondo.mjs';

function fixture(t, { version = pin.version, extraEntry = false } = {}) {
  const directory = mkdtempSync(path.join(tmpdir(), 'knoxx-sandbox-kondo-'));
  t.after(() => rmSync(directory, { recursive: true, force: true }));
  const source = path.join(directory, 'source');
  mkdirSync(source);
  writeFileSync(path.join(source, 'clj-kondo'), `#!/bin/sh\nprintf 'clj-kondo v${version}\\n'\n`);
  const files = ['clj-kondo'];
  if (extraEntry) { writeFileSync(path.join(source, 'unexpected'), 'extra'); files.push('unexpected'); }
  const archive = path.join(directory, 'fixture.zip');
  execFileSync('zip', ['-q', archive, ...files], { cwd: source });
  const sha256 = createHash('sha256').update(readFileSync(archive)).digest('hex');
  return { archive, destination: path.join(directory, 'tools'), expected: { ...pin, sha256 } };
}

test('restore installs a verified executable in a stable PATH directory and can repeat', t => {
  const { archive, destination, expected } = fixture(t);
  const bin = restoreArchive(archive, destination, expected);
  assert.equal(execFileSync('clj-kondo', ['--version'], { env: { PATH: bin }, encoding: 'utf8' }).trim(),
    `clj-kondo v${pin.version}`);
  assert.equal(restoreArchive(archive, destination, expected), bin);
  assert(!readdirSync(destination).some(name => name.startsWith('.clj-kondo-')));
});

test('restore refuses a corrupt archive before creating an installation', t => {
  const { archive, destination, expected } = fixture(t);
  writeFileSync(archive, 'corrupt bytes');
  assert.throws(() => restoreArchive(archive, destination, expected), /checksum mismatch/);
  assert(!existsSync(destination));
});

test('restore refuses a different host architecture or operating system', t => {
  const { archive, destination, expected } = fixture(t);
  for (const host of [{ platform: 'linux', arch: 'arm64' }, { platform: 'darwin', arch: 'x64' }]) {
    assert.throws(() => restoreArchive(archive, destination, expected, host), /bundle requires linux\/x64/);
  }
  assert(!existsSync(destination));
});

test('restore rejects unexpected archive entries before extraction', t => {
  const { archive, destination, expected } = fixture(t, { extraEntry: true });
  assert.throws(() => restoreArchive(archive, destination, expected), /must contain only/);
  assert(!existsSync(destination));
});

test('restore rejects a wrong executable version and cleans its staging directory', t => {
  const { archive, destination, expected } = fixture(t, { version: '0.0.0' });
  assert.throws(() => restoreArchive(archive, destination, expected), /Unexpected clj-kondo version/);
  assert.deepEqual(readdirSync(destination), []);
});

test('CLI fails rather than falling back to an installed clj-kondo when the archive is missing', t => {
  const { destination } = fixture(t);
  const script = new URL('../../../scripts/sandbox-clj-kondo.mjs', import.meta.url);
  const result = spawnSync(process.execPath, [script.pathname, 'restore', destination, destination],
    { encoding: 'utf8' });
  assert.equal(result.status, 1);
  assert.match(result.stderr, /ENOENT/);
  assert.equal(result.stdout, '');
});

test('collection rejects download failures and does not write a success artifact', async t => {
  const { destination } = fixture(t);
  t.mock.method(globalThis, 'fetch', async () => new Response('unavailable', { status: 503 }));
  await assert.rejects(collect(destination), /download returned 503/);
  assert(!existsSync(destination));
});

test('collection checks the pinned digest before persisting downloaded bytes', async t => {
  const { destination } = fixture(t);
  t.mock.method(globalThis, 'fetch', async () => new Response('tampered archive'));
  await assert.rejects(collect(destination), /checksum mismatch/);
  assert(!existsSync(destination));
});
