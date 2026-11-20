import fs from 'node:fs/promises';
import path from 'node:path';

/** Reserve a new evidence directory; never reuse an earlier run's results. */
export async function reserveEvidenceDirectory(outputDir) {
  await fs.mkdir(path.dirname(outputDir), { recursive: true });
  await fs.mkdir(outputDir, { mode: 0o700 });
}

/** Keep diagnostic results incomplete until an owned source-to-build producer exists. */
export function markBuildProvenanceUnverified(evidence) {
  evidence.buildProvenance = 'unverified';
  evidence.completed = false;
  evidence.failures.push({ stage: 'build-provenance',
    message: 'Build-byte stability does not prove the observed checkout produced the artifacts' });
}

/** Finalize owned resources, disposable fixtures and the run's evidence. */
export async function cleanupWikiStack({ close, evidence, fixtureDirectory, outputDir }) {
  for (const stop of close) {
    try { await stop(); }
    catch (error) { evidence.failures.push({ stage: 'cleanup', message: error.message }); }
  }
  // Evidence storage can fail independently; it must never prevent fixture removal.
  try { await fs.rm(fixtureDirectory, { recursive: true, force: true }); }
  catch (error) { evidence.failures.push({ stage: 'fixture-cleanup', message: error.message }); }
  if (evidence.failures.length) evidence.completed = false;
  evidence.finishedAt = new Date().toISOString();
  await fs.writeFile(path.join(outputDir, 'result.json'), `${JSON.stringify(evidence, null, 2)}\n`);
}

/** Finalize a diagnostic run without claiming unproven source/build provenance. */
export async function finishWikiStack(options) {
  markBuildProvenanceUnverified(options.evidence);
  await cleanupWikiStack(options);
  return 1;
}
