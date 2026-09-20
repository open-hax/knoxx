import { rm } from 'node:fs/promises';

/**
 * Close every resource acquired by the isolated route verifier, in order.
 * Each failure is returned without skipping subsequent cleanup or replacing
 * the verifier's original failure. Missing, not-yet-acquired resources are skipped.
 * @param {{app?: {close: Function}, upstream?: {close: Function}, root?: string}} resources
 * @param {Function} remove Directory remover, injectable for cleanup-failure tests.
 * @returns {Promise<unknown[]>} All cleanup failures, in attempted order.
 */
export async function cleanupRouteProof({ app, upstream, root }, remove = rm) {
  const failures = [];
  const operations = [
    ...(app ? [() => app.close()] : []),
    ...(upstream ? [() => upstream.close()] : []),
    ...(root ? [() => remove(root, { recursive: true, force: true })] : []),
  ];
  for (const operation of operations) {
    try {
      await operation();
    } catch (error) {
      failures.push(error);
    }
  }
  return failures;
}
