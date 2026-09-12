/** Run against the supervisor's disposable Clio mailbox and actual delegated agent adapter. */
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {observeBrowserResponse} from './browser-response-observer.mjs';

async function command(page, button, pathname) {
  const pending = observeBrowserResponse(page, response => response.request().method() === 'POST' &&
    new URL(response.url()).pathname === pathname, async response => ({status: response.status(), body: await response.json(),
      sent: response.request().postDataJSON()}));
  try {
    await button.click(); const result = await pending.value();
    assert.equal(result.status, 200, `${pathname}: ${JSON.stringify(result.body)}`);
    assert.equal(result.body.ok, true); return result;
  } finally {pending.cancel();}
}

export async function mailTour(page, {baseUrl, principalId, agentSend, shot}) {
  assert.equal(typeof agentSend, 'function', 'Provide a real delegated agent tool; HTTP substitution is not agent proof');
  const outsider = await page.context().browser().newContext();
  try {assert.equal((await outsider.request.get(new URL('/api/actors/mailbox', baseUrl).href)).status(), 401);}
  finally {await outsider.close();}
  await page.goto(new URL('/mail', baseUrl).href);
  await page.getByRole('heading', {name: 'Mail', exact: true}).waitFor();
  await page.getByText('Live · Durable mailbox', {exact: true}).waitFor();
  const marker = `Human handoff ${randomUUID()}`;
  const content = `${marker}\n${'Research context that must survive the preview boundary. '.repeat(8)}\nFull human message ending.`;
  await page.getByLabel('Recipient', {exact: true}).fill(`actor:${principalId}`);
  await page.getByLabel('Message', {exact: true}).fill(content);
  await page.getByLabel('Delivery', {exact: true}).selectOption('inbox-only');
  const sent = await command(page, page.getByRole('button', {name: 'Send message', exact: true}), '/api/actors/messages');
  assert.deepEqual(Object.keys(sent.sent).sort(), ['content', 'mode', 'operation_id', 'target']);
  assert.equal(sent.body.entry.status, 'delivered'); assert.equal(sent.body.entry.durable, true);
  const humanCard = page.locator('article').filter({hasText: marker}); await humanCard.waitFor();
  assert.ok(!await humanCard.textContent().then(text => text.includes('Full human message ending.')), 'List must display only its bounded preview');
  await humanCard.getByRole('button', {name: 'Read full message', exact: true}).click();
  await page.getByRole('region', {name: 'Full message', exact: true}).getByText(content, {exact: true}).waitFor();
  await shot('mail-01-human-full-content', 'The human compose command persisted a complete message; the owned reader retrieves content beyond the list preview.', []);
  const draft = 'Keep this unfinished human response while the agent sends another message.';
  await page.getByLabel('Message', {exact: true}).fill(draft);
  const agentMarker = `Agent handoff ${randomUUID()}`;
  const agentContent = `${agentMarker}\n${'Canonical agent message content. '.repeat(12)}\nFull agent message ending.`;
  await agentSend({operation_id: randomUUID(), target: `actor:${principalId}`, content: agentContent, mode: 'inbox-only'});
  const agentCard = page.locator('article').filter({hasText: agentMarker}); await agentCard.waitFor();
  assert.equal(await page.getByLabel('Message', {exact: true}).inputValue(), draft);
  await agentCard.getByRole('button', {name: 'Read full message', exact: true}).click();
  await page.getByRole('region', {name: 'Full message', exact: true}).getByText(agentContent, {exact: true}).waitFor();
  await shot('mail-02-agent-live-handoff', 'A real agent command appeared through mailbox SSE without a refresh click, while the human draft remained intact.', []);
  const incoming = await page.request.get(new URL('/api/actors/mailbox?box=inbox', baseUrl).href);
  const entry = (await incoming.json()).entries.find(row => row.preview.startsWith(agentMarker)); assert.ok(entry?.id);
  const ack = await command(page, agentCard.getByRole('button', {name: 'Acknowledge', exact: true}), `/api/actors/mailbox/${encodeURIComponent(entry.id)}/ack`);
  assert.equal(ack.body.entry.status, 'acknowledged');
  await agentCard.getByText('acknowledged', {exact: true}).waitFor();
  await shot('mail-03-human-acknowledgement', 'The same actor acknowledged the persisted agent message through its human capability control.', []);
  await page.getByLabel('Message', {exact: true}).fill('');
  return {anonymous: 401, humanSend: true, fullContent: true, agentLiveUpdate: true, draftPreserved: true, acknowledged: true};
}
