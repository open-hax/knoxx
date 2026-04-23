import test from 'node:test';
import assert from 'node:assert/strict';

import { createPolicyDb } from '../dist/app.js';

test('createPolicyDb stays promise-shaped when policy DB is disabled', async () => {
  const result = createPolicyDb({ connectionString: '' });

  assert.equal(typeof result?.then, 'function');
  assert.equal(await result, null);
});
