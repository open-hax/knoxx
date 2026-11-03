/** Real Contracts UI verification. The stack supervisor owns disposable fixtures and cleanup.
 * The librarian panel is exercised here; model generation belongs to the Wiki writing tour.
 */
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {observeBrowserResponse} from './browser-response-observer.mjs';

const root = '/api/admin/contracts';
const editorSelector = '.cm-content[contenteditable="true"]';
const endpoint = (baseUrl, path) => new URL(path, baseUrl).href;
const contractPath = id => `${root}/${encodeURIComponent(id)}`;
const readPath = (id, kind) => `${contractPath(id)}?kind=${encodeURIComponent(kind)}`;

function contractEdn(id, kind, version = 1) {
  const trigger = kind === 'triggers' ? '\n :trigger/kind :event\n :trigger/events []' : '';
  return `{\n :contract/id "${id}"\n :contract/kind :${kind === 'triggers' ? 'trigger' : 'agent'}\n :contract/version ${version}\n :enabled false${trigger}\n}\n`;
}

async function jsonResponse(response, expected, description) {
  const text = await response.text();
  assert.equal(response.status(), expected, `${description}: HTTP ${response.status()} ${text}`);
  return JSON.parse(text);
}

async function command(page, run, {method, path, status = 200, kind}) {
  const pending = observeBrowserResponse(page, response => {
    const url = new URL(response.url());
    return response.request().method() === method && url.pathname === path
      && (method !== 'GET' || kind == null || url.searchParams.get('kind') === kind);
  }, async response => ({body: await jsonResponse(response, status, `${method} ${path}`),
    sent: method === 'GET' ? null : response.request().postDataJSON()}));
  try {
    await run();
    return await pending.value();
  } finally {pending.cancel();}
}

async function selectContract(page, id, kind) {
  await page.getByPlaceholder('Search contracts...', {exact: true}).fill(id);
  const entry = page.getByRole('button').filter({hasText: id}).filter({hasText: kind});
  const response = await command(page, () => entry.click(), {method: 'GET', path: contractPath(id), kind});
  assert.equal(response.body.contractClass, kind);
  await page.locator(editorSelector).getByText(`:contract/id "${id}"`, {exact: false}).waitFor();
  assert.equal(await page.getByLabel('Contract identity', {exact: true}).inputValue(), id);
  assert.equal(await page.getByLabel('Kind', {exact: true}).inputValue(), kind === 'triggers' ? 'trigger' : 'agent');
  return response.body;
}

async function readContract(page, baseUrl, id, kind) {
  return jsonResponse(await page.request.get(endpoint(baseUrl, readPath(id, kind))), 200, `Read persisted ${kind}/${id}`);
}

async function editAndValidate(page, ednText, kind, valid) {
  await page.locator(editorSelector).fill(ednText);
  const response = await command(page, () => page.getByRole('button', {name: '✓ Validate', exact: true}).click(),
    {method: 'POST', path: `${root}/validate`});
  assert.deepEqual(response.sent, {ednText, kind});
  assert.equal(response.body.ok, valid);
  await page.getByText(valid ? 'Validation passed.' : /^Validation failed: \d+ error\(s\)\.$/, {exact: true}).waitFor();
  return response.body;
}

async function cloneContract(page, baseUrl, sourceId, cloneId, kind) {
  await page.getByRole('button', {name: 'Clone', exact: true}).click();
  const input = page.getByPlaceholder('new-contract-id', {exact: true});
  await input.fill(cloneId);
  const response = await command(page, () => input.locator('..').getByRole('button', {name: 'Clone', exact: true}).click(),
    {method: 'POST', path: `${contractPath(sourceId)}/copy`});
  assert.deepEqual(response.sent, {newId: cloneId, kind});
  assert.equal(response.body.contractClass, kind);
  await page.getByText(`Copied ${sourceId} → ${cloneId}.`, {exact: true}).waitFor();
  await page.locator(editorSelector).getByText(`:contract/id "${cloneId}"`, {exact: false}).waitFor();
  const persisted = await readContract(page, baseUrl, cloneId, kind);
  assert.equal(persisted.contract['contract/id'], cloneId);
  assert.equal(persisted.contract['contract/version'], 2);
  return persisted;
}

async function responsiveControls(page, shot) {
  const viewport = page.viewportSize();
  const focus = page.getByLabel('Auto-focus', {exact: true});
  const initialFocus = await focus.isChecked();
  await focus.setChecked(!initialFocus);
  assert.equal(await focus.isChecked(), !initialFocus);
  await focus.setChecked(initialFocus);
  await page.getByRole('button', {name: '✕ Chat', exact: true}).click();
  await page.getByText('📋 Contract Librarian', {exact: true}).waitFor({state: 'hidden'});
  await page.getByRole('button', {name: '💬 Chat', exact: true}).click();
  await page.getByText('📋 Contract Librarian', {exact: true}).waitFor();
  try {
    await page.setViewportSize({width: 800, height: 1000});
    const overlay = page.getByRole('button', {name: 'Close contracts panel', exact: true});
    await overlay.waitFor();
    // The library occupies the left of the overlay; click its uncovered right edge.
    const bounds = await overlay.boundingBox();
    assert.ok(bounds && bounds.width > 350, 'Narrow contract overlay must have an exposed dismissal area');
    await overlay.click({position: {x: bounds.width - 12, y: 12}});
    await overlay.waitFor({state: 'hidden'});
    await page.getByRole('button', {name: '☰ Left', exact: true}).click();
    await overlay.waitFor();
    await shot('contracts-05-responsive-library',
      'The narrow library opens above the editor and dismisses through its real overlay; the librarian remains in its bottom dock.',
      ['[aria-label="Close contracts panel"]', '[placeholder="Search contracts..."]']);
    await page.getByRole('button', {name: 'Hide', exact: true}).click();
    await overlay.waitFor({state: 'hidden'});
  } finally {
    if (viewport) await page.setViewportSize(viewport);
  }
  await page.getByRole('button', {name: '☰ Left', exact: true}).click();
  await page.getByPlaceholder('Search contracts...', {exact: true}).waitFor();
}

/** Requires an authenticated admin page served by the disposable verify-wiki-stack services. */
export async function contractsTour(page, {baseUrl, shot, verifyCheckout}) {
  assert.equal(typeof shot, 'function', 'The supervisor must record annotated screenshots');
  assert.equal(typeof verifyCheckout, 'function', 'The supervisor must verify the actual running checkout');
  await verifyCheckout();
  const anonymous = await page.context().browser().newContext();
  try {
    const response = await anonymous.request.get(endpoint(baseUrl, root));
    assert.equal(response.status(), 401, 'Anonymous callers cannot read the contract library');
  } finally {await anonymous.close();}
  console.log('PASS Anonymous contract-library access is refused with HTTP 401.');

  const suffix = randomUUID();
  const agentId = `browser-agent-${suffix}`, triggerId = `browser-trigger-${suffix}`, cloneId = `browser-clone-${suffix}`;
  for (const [id, kind] of [[agentId, 'agents'], [triggerId, 'triggers']]) {
    const seeded = await jsonResponse(await page.request.put(endpoint(baseUrl, contractPath(id)),
      {data: {ednText: contractEdn(id, kind), kind}, headers: {Origin: new URL(baseUrl).origin}}),
      200, `Seed disabled ${kind}/${id}`);
    assert.equal(seeded.validation.ok, true);
    assert.equal(seeded.contract.enabled, false, 'Fixtures must not activate agent or trigger execution');
  }

  const library = await command(page, () => page.goto(endpoint(baseUrl, '/contracts')),
    {method: 'GET', path: root});
  assert.ok(library.body.contracts.some(entry => entry.id === agentId && entry.contractClass === 'agents'));
  assert.ok(library.body.contracts.some(entry => entry.id === triggerId && entry.contractClass === 'triggers'));
  await page.getByRole('button', {name: 'Refresh', exact: true}).waitFor();
  if (!await page.getByPlaceholder('Search contracts...', {exact: true}).isVisible()) {
    await page.getByRole('button', {name: '☰ Left', exact: true}).click();
  }
  await selectContract(page, triggerId, 'triggers');
  assert.equal(await page.getByLabel('Enabled', {exact: true}).isChecked(), false);
  await shot('contracts-01-class-selection',
    'The authenticated library selects a stored trigger using kind=triggers; identity, class and disabled state reflect the real response.',
    ['[placeholder="Search contracts..."]', 'select']);
  console.log('PASS Stored agent and trigger fixtures appear in the real library; trigger selection preserves its class.');

  await selectContract(page, agentId, 'agents');
  const revised = contractEdn(agentId, 'agents', 2);
  await editAndValidate(page, revised, 'agents', true);
  const saved = await command(page, () => page.getByRole('button', {name: 'Save', exact: true}).click(),
    {method: 'PUT', path: contractPath(agentId)});
  assert.deepEqual(saved.sent, {ednText: revised, kind: 'agents'});
  assert.equal(saved.body.validation.ok, true);
  await page.getByText(`Saved ${agentId}.`, {exact: true}).waitFor();
  assert.equal((await readContract(page, baseUrl, agentId, 'agents')).ednText, revised);
  await page.getByRole('button', {name: 'Show JSON', exact: true}).click();
  await page.getByText('Normalized view', {exact: true}).waitFor();
  await shot('contracts-02-validated-save',
    'Real EDN validation and saving succeeded; a fresh server read confirms revision 2 and the normalized view is visible.',
    [editorSelector]);
  await page.getByRole('button', {name: 'Hide JSON', exact: true}).click();
  await cloneContract(page, baseUrl, agentId, cloneId, 'agents');
  await shot('contracts-03-cloned-contract',
    'The Clone control created a separate stored identity in the selected agent class, retaining revision 2.', [editorSelector]);
  console.log('PASS UI validation, save and clone sent the expected class and EDN; fresh reads confirm persisted identities and content.');

  const persisted = await readContract(page, baseUrl, cloneId, 'agents');
  const invalid = contractEdn(cloneId, 'agents', 2).replace(':enabled false', ':enabled "invalid"');
  await editAndValidate(page, invalid, 'agents', false);
  const refused = await command(page, () => page.getByRole('button', {name: 'Save', exact: true}).click(),
    {method: 'PUT', path: contractPath(cloneId), status: 400});
  assert.deepEqual(refused.sent, {ednText: invalid, kind: 'agents'});
  assert.equal(refused.body.detail, 'Contract EDN failed validation');
  await page.getByText(/400 .*Contract EDN failed validation/).waitFor();
  assert.equal((await readContract(page, baseUrl, cloneId, 'agents')).ednText, persisted.ednText);
  assert.ok((await page.locator(editorSelector).innerText()).includes(':enabled "invalid"'), 'Failed saving must preserve the draft');
  assert.equal(await page.getByRole('button', {name: 'Save', exact: true}).isEnabled(), true);
  await shot('contracts-04-rejected-save',
    'The server rejected an invalid enabled value with HTTP 400. The draft remains editable and a fresh read confirms stored EDN was not changed.',
    [editorSelector]);
  await editAndValidate(page, persisted.ednText, 'agents', true);
  console.log('PASS Real invalid EDN fails validation and saving; the server preserves stored bytes while the UI retains the editable draft.');

  await responsiveControls(page, shot);
  await verifyCheckout();
  console.log('PASS Native Contracts overlay, librarian visibility and auto-focus controls work through the actual browser.');
  console.log('WARN Contracts librarian generation is not exercised here; the Wiki tour separately verifies a real writing-model response.');
  return {anonymousStatus: 401, agentId, triggerId, cloneId, selectedClasses: ['triggers', 'agents'],
    validateStatus: 200, saveStatus: 200, cloneStatus: 200, rejectedSaveStatus: 400,
    rejectedSavePreservedStoredEdn: true, rejectedSavePreservedDraft: true,
    responsiveOverlay: true, librarianControls: true, librarianGeneration: false};
}
