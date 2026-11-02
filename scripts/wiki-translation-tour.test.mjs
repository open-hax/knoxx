import assert from 'node:assert/strict';
import test from 'node:test';
import { translationAction } from './wiki-translation-tour.mjs';

const ready = { revision: 'r2', reviewable: true, work_state: 'ready', split_review: {
  candidate_set_id: 'new', manifest_id: 'manifest', splits: [{ candidate_text: 'hola' }],
} };
test('automatic work is consumed or awaited before dispatch', () => {
  assert.equal(translationAction(ready, 'r2', 'old'), 'ready');
  assert.equal(translationAction({...ready, work_state: 'in_flight', reviewable: false}, 'r2', 'old'), 'wait');
  assert.equal(translationAction({...ready, revision: 'r1'}, 'r2', 'old'), 'wait');
  assert.equal(translationAction({...ready, reviewable: false, work_state: 'failed', allowed_actions: ['retry']}, 'r2', 'old'), 'dispatch');
  assert.equal(translationAction(ready, 'r2', 'new'), 'wait');
});
