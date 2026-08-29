import assert from 'node:assert/strict';
import test from 'node:test';

import {
  normalizeBuild,
  parseTestCounters,
  testCountersExitCode,
} from '../../scripts/run-shadow-tests-ci.mjs';

test('test counter parser uses the final cljs.test summary', () => {
  const output = [
    'Ran 1 tests containing 1 assertions. 1 failures, 0 errors.',
    'Ran 22 tests containing 98 assertions. 0 failures, 0 errors.',
  ].join('\n');

  assert.deepEqual(parseTestCounters(output), { failures: 0, errors: 0 });
  assert.equal(testCountersExitCode(output), 0);
});

test('test counter guard rejects failures and errors', () => {
  assert.equal(testCountersExitCode('3 failures, 0 errors.'), 1);
  assert.equal(testCountersExitCode('0 failures, 2 errors.'), 1);
});

test('test counter guard fails closed when counters are missing', () => {
  assert.equal(parseTestCounters('Build completed without a test summary'), null);
  assert.equal(testCountersExitCode('Build completed without a test summary'), 1);
});

test('only the unit and e2e shadow builds are accepted', () => {
  assert.equal(normalizeBuild(), 'test');
  assert.equal(normalizeBuild('e2e'), 'e2e');
  assert.throws(() => normalizeBuild('server'), /Unsupported shadow-cljs test build: server/);
});
