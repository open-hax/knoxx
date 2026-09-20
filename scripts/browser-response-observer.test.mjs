import assert from 'node:assert/strict';
import {EventEmitter} from 'node:events';
import test from 'node:test';
import {observeBrowserResponse} from './browser-response-observer.mjs';

test('response metadata survives immediate page navigation without reading discarded body', async () => {
  const page = new EventEmitter(); let bodyReads = 0;
  const watcher = observeBrowserResponse(page, response => response.status() === 200, response => response.status());
  page.emit('response', {status: () => 200, json: () => {bodyReads++; throw new Error('Response body discarded');}});
  assert.equal(await watcher.value(), 200); assert.equal(bodyReads, 0);
  assert.equal(page.listenerCount('response'), 0); assert.equal(page.listenerCount('close'), 0);
});

test('failed command cleanup removes response waits before the page closes', async () => {
  const page = new EventEmitter(); const watcher = observeBrowserResponse(page, () => true);
  watcher.cancel(); page.emit('close');
  assert.equal(page.listenerCount('response'), 0); assert.equal(page.listenerCount('close'), 0);
  await assert.rejects(watcher.value(), /cancelled/);
});

test('body decoding failure remains a visible failure when JSON is required', async () => {
  const page = new EventEmitter();
  const watcher = observeBrowserResponse(page, () => true, response => response.json());
  page.emit('response', {json: async () => {throw new Error('Invalid response JSON');}});
  await new Promise(resolve => setImmediate(resolve));
  await assert.rejects(watcher.value(), /Invalid response JSON/);
  assert.equal(page.listenerCount('response'), 0);
});

test('unexpected page closure reports a failure and releases every observer', async () => {
  const page = new EventEmitter(); const watcher = observeBrowserResponse(page, () => true);
  page.emit('close'); await assert.rejects(watcher.value(), /page closed/);
  assert.equal(page.listenerCount('response'), 0); assert.equal(page.listenerCount('crash'), 0);
});

test('unrelated responses are ignored and matching errors are contained until observed', async () => {
  const page = new EventEmitter();
  const watcher = observeBrowserResponse(page, response => response.url === '/command', response => response.value);
  page.emit('response', {url: '/unrelated', value: 'wrong'});
  page.emit('response', {url: '/command', value: 'correct'});
  assert.equal(await watcher.value(), 'correct'); watcher.cancel();
  assert.equal(page.listenerCount('response'), 0);
});
