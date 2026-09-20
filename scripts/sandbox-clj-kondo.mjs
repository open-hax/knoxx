import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { chmodSync, copyFileSync, lstatSync, mkdirSync, mkdtempSync, readFileSync,
  renameSync, rmSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// The upstream v2025.07.28 release asset digest, pinned independently of downloads.
export const pin = Object.freeze({
  version: '2025.07.28', platform: 'linux', arch: 'x64',
  archive: 'clj-kondo-2025.07.28-linux-static-amd64.zip',
  sha256: 'd6449daf243516fdc703f0629cb077ddfbe1981a5e21a3d1421708f22d1b6ce7',
});
const script = fileURLToPath(import.meta.url);
const digest = bytes => createHash('sha256').update(bytes).digest('hex');

function requireHost(expected, host) {
  if (host.platform !== expected.platform || host.arch !== expected.arch) {
    throw new Error(`clj-kondo bundle requires ${expected.platform}/${expected.arch}; got ${host.platform}/${host.arch}`);
  }
}

function verifyChecksum(bytes, expected) {
  if (digest(bytes) !== expected.sha256) throw new Error('clj-kondo archive checksum mismatch');
}

export async function collect(bundle) {
  requireHost(pin, process);
  const url = `https://github.com/clj-kondo/clj-kondo/releases/download/v${pin.version}/${pin.archive}`;
  const response = await fetch(url, { signal: AbortSignal.timeout(120_000) });
  if (!response.ok) throw new Error(`clj-kondo archive download returned ${response.status}`);
  const bytes = Buffer.from(await response.arrayBuffer());
  verifyChecksum(bytes, pin);
  const toolchain = path.resolve(bundle, 'toolchain');
  mkdirSync(toolchain, { recursive: true });
  writeFileSync(path.join(toolchain, pin.archive), bytes);
  writeFileSync(path.join(toolchain, 'clj-kondo.json'), `${JSON.stringify(pin, null, 2)}\n`);
  copyFileSync(script, path.join(toolchain, path.basename(script)));
}

// The CLI always supplies the fixed release pin; explicit parameters let tests
// exercise checksum, host, archive-shape and executable-version failure paths.
export function restoreArchive(archive, destination, expected = pin, host = process) {
  requireHost(expected, host);
  verifyChecksum(readFileSync(archive), expected);
  const entries = execFileSync('unzip', ['-Z1', archive], { encoding: 'utf8' }).trim().split('\n');
  if (entries.length !== 1 || entries[0] !== 'clj-kondo') {
    throw new Error('clj-kondo archive must contain only the clj-kondo executable');
  }
  const root = path.resolve(destination);
  mkdirSync(root, { recursive: true });
  const stage = mkdtempSync(path.join(root, '.clj-kondo-'));
  try {
    execFileSync('unzip', ['-q', archive, '-d', stage]);
    const candidate = path.join(stage, 'clj-kondo');
    if (!lstatSync(candidate).isFile()) throw new Error('clj-kondo archive entry is not a regular file');
    chmodSync(candidate, 0o755);
    const version = execFileSync(candidate, ['--version'], { encoding: 'utf8', timeout: 10_000 }).trim();
    if (version !== `clj-kondo v${expected.version}`) throw new Error(`Unexpected clj-kondo version: ${version}`);
    const bin = path.join(root, `clj-kondo-${expected.version}-${expected.platform}-${expected.arch}`, 'bin');
    mkdirSync(bin, { recursive: true });
    renameSync(candidate, path.join(bin, 'clj-kondo'));
    return bin;
  } finally {
    rmSync(stage, { recursive: true, force: true });
  }
}

async function main([action, bundle, destination, ...extra]) {
  if (action === 'describe' && !bundle) {
    console.log(JSON.stringify({ ...pin, archive: `toolchain/${pin.archive}`,
      restore_script: 'toolchain/sandbox-clj-kondo.mjs' }));
  } else if (action === 'collect' && bundle && !destination) {
    await collect(bundle);
  } else if (action === 'restore' && bundle && destination && extra.length === 0) {
    console.log(restoreArchive(path.resolve(bundle, 'toolchain', pin.archive), destination));
  } else {
    throw new Error('Usage: sandbox-clj-kondo.mjs describe | collect BUNDLE | restore BUNDLE DESTINATION');
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === script) {
  main(process.argv.slice(2)).catch(error => { console.error(error.message); process.exitCode = 1; });
}
