import fs from 'node:fs/promises';
import path from 'node:path';

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
