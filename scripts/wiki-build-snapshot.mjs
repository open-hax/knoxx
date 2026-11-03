import assert from 'node:assert/strict';
import { constants } from 'node:fs';
import fs from 'node:fs/promises';
import path from 'node:path';
import { createHash } from 'node:crypto';

const roots = Object.freeze(['backend/dist', 'frontend/dist']);
const digest = bytes => createHash('sha256').update(bytes).digest('hex');

async function readTree(repo, relative, files) {
  const absolute = path.join(repo, relative);
  const stat = await fs.lstat(absolute);
  assert(!stat.isSymbolicLink(), `Build snapshot refuses a symlink: ${relative}`);
  if (stat.isDirectory()) {
    for (const name of (await fs.readdir(absolute)).sort()) {
      await readTree(repo, `${relative}/${name}`, files);
    }
  } else {
    assert(stat.isFile(), `Build snapshot requires regular files: ${relative}`);
    const handle = await fs.open(absolute, constants.O_RDONLY | constants.O_NOFOLLOW);
    try {
      const before = await handle.stat({ bigint: true });
      const bytes = await handle.readFile();
      const after = await handle.stat({ bigint: true });
      assert(before.ino === after.ino && before.size === after.size && before.mtimeNs === after.mtimeNs,
        `Build output changed while hashing: ${relative}`);
      files[relative] = Object.freeze({ sha256: digest(bytes), bytes: bytes.length });
    } finally { await handle.close(); }
  }
}

/** Snapshot every regular file under both owned build trees, including nested modules and maps. */
export async function snapshotBuildFiles(repo) {
  assert(path.isAbsolute(repo), 'Build snapshot repository must be absolute');
  const files = {};
  for (const root of roots) {
    assert((await fs.lstat(path.join(repo, root))).isDirectory(), `Missing regular build directory: ${root}`);
    await readTree(repo, root, files);
  }
  const rows = Object.keys(files).sort().map(file => [file, files[file].bytes, files[file].sha256]);
  return Object.freeze({ roots, sha256: digest(JSON.stringify(rows)), files: Object.freeze(files) });
}

/** Refuse changed bytes, added/deleted files or symlinks after a supervised browser run. */
export async function verifyBuildSnapshot(repo, expected) {
  const current = await snapshotBuildFiles(repo);
  assert.deepEqual(current.roots, expected.roots, 'Build snapshot roots changed');
  assert.deepEqual(current.files, expected.files, 'Owned build files changed during verification');
  assert.equal(current.sha256, expected.sha256, 'Owned build tree digest changed');
  return { verified: true, sha256: current.sha256, files: Object.keys(current.files).length };
}
