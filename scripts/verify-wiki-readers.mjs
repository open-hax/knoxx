import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

// These are reader-boundary checks. Successful cross-turn memory is proven
// only by the full tour against the model's actual canonical Clio turns.
const repo = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const { readMemory, readManifest } = await import(pathToFileURL(path.join(repo, 'backend/dist-verification/wiki.js')));
const directory = await fs.mkdtemp(path.join(os.tmpdir(), 'knoxx-wiki-readers-'));
const expectation = {
  document: 'wiki/example', publication: 'publication/example-es', garden: 'garden/research', locale: 'es',
  firstCandidateSetId: 'candidate/first', secondCandidateSetId: 'candidate/second',
  firstSourceRevision: 'sha256-first', secondSourceRevision: 'sha256-second',
  acceptedReviewId: 'review/approved', acceptedSplitId: 'split/1',
  sourceText: 'People decide.', acceptedCorrection: 'Las personas deciden.',
};
try {
  const absent = path.join(directory, 'absent');
  assert.deepEqual(readManifest(absent), []);
  await assert.rejects(() => fs.access(absent), { code: 'ENOENT' });
  await assert.rejects(() => readMemory(absent, expectation), /Required translation split ledger is missing/);
  await assert.rejects(() => fs.access(absent), { code: 'ENOENT' });
  await assert.rejects(() => readMemory(absent, { ...expectation, acceptedCorrection: '' }), /Missing required canonical memory expectation/);
  await assert.rejects(() => readMemory(absent, { ...expectation, secondCandidateSetId: expectation.firstCandidateSetId }), /distinct candidates/);
  await assert.rejects(() => readMemory(absent, { ...expectation, secondSourceRevision: expectation.firstSourceRevision }), /distinct source revisions/);
  const partial = path.join(directory, 'partial');
  await fs.mkdir(partial);
  await fs.writeFile(path.join(partial, 'events.edn'), '');
  await assert.rejects(() => readMemory(partial, expectation), /Required translation split schemas are missing/);
  assert.deepEqual(await fs.readdir(partial), ['events.edn'], 'Reading missing schemas must not initialize a replacement provider');
  const manifestPath = path.join(directory, 'manifest.edn');
  await fs.writeFile(manifestPath, '{:manifest/version 1');
  assert.throws(() => readManifest(directory), /EOF|reading/);
  await fs.writeFile(manifestPath, '{:manifest/version 99 :manifest/routes []}');
  assert.throws(() => readManifest(directory), /manifest contract violation/);
  const valid = `{:manifest/version 1 :manifest/generated-at "2026-09-12T00:00:00.000Z"
    :manifest/routes [{:route/path "/wiki/example/es" :route/locale :es
      :route/artifact "artifacts/wiki/example/es/revision.html" :route/media-type "text/html"
      :route/encoding "utf-8" :route/revision "sha256-published" :publication/id :publication/example-es}]}`;
  await fs.writeFile(manifestPath, valid);
  assert.deepEqual(readManifest(directory), [{ path: '/wiki/example/es', artifact: 'artifacts/wiki/example/es/revision.html',
    mediaType: 'text/html', revision: 'sha256-published', publication: 'publication/example-es' }]);
  await fs.writeFile(manifestPath, valid.replace('artifacts/wiki/example/es/revision.html', '../outside.html'));
  assert.throws(() => readManifest(directory), /manifest contract violation/);
  console.log('PASS canonical reader boundaries: absence stays absent; corrupt/traversing manifests refuse; qualified identity survives; incomplete memory evidence cannot initialize or pass.');
} finally {
  await fs.rm(directory, { recursive: true, force: true });
}
