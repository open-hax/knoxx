/** Actual Admin UI commands; the isolated supervisor owns all created identity/directory data. */
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {observeBrowserResponse} from './browser-response-observer.mjs';
async function command(page, button, method, pathname, status) {
  const pending = observeBrowserResponse(page, response => response.request().method() === method && new URL(response.url()).pathname === pathname,
    async response => ({status: response.status(), body: await response.json(), sent: response.request().postDataJSON()}));
  try {
    await button.click(); const response = await pending.value();
    assert.equal(response.status, status, `${method} ${pathname}: ${response.status}`);
    return response;
  } finally { pending.cancel(); }
}
function noIdentityEdits(payload) {
  for (const field of ['email', 'authProvider', 'externalSubject', 'axxiumPrincipalId']) {
    assert.ok(!Object.hasOwn(payload, field), `Profile PATCH must not edit ${field}`);
  }
}
export async function adminIdentityTour(page, config) {
  assert.equal(typeof config.shot, 'function'); assert.ok(config.principalId && config.principalEmail);
  const anonymous = await page.context().browser().newContext();
  try {
    const visitor = await anonymous.newPage();
    const denied = observeBrowserResponse(visitor, response => new URL(response.url()).pathname === '/api/auth/context');
    try {
      await visitor.goto(new URL('/ops/admin/actors', config.baseUrl).href);
      assert.equal((await denied.value()).status(), 401);
    } finally { denied.cancel(); }
    await visitor.getByText('Identity managed by Axxium', {exact: true}).waitFor();
  } finally {await anonymous.close();}
  const suffix = randomUUID().slice(0, 8), orgName = `Sandbox editorial team ${suffix}`;
  await page.goto(new URL('/ops/admin/orgs', config.baseUrl).href);
  await page.getByPlaceholder('Org name', {exact: true}).fill(orgName);
  await page.getByPlaceholder('org-slug', {exact: true}).fill(`sandbox-editorial-${suffix}`);
  const org = await command(page, page.getByRole('button', {name: 'Create org', exact: true}), 'POST', '/api/admin/orgs', 201);
  const orgId = org.body.org.id; assert.ok(orgId);
  const orgButton = page.getByRole('button').filter({hasText: orgName}); await orgButton.waitFor(); await orgButton.click();
  const actors = page.getByRole('link', {name: 'Actors', exact: false});
  assert.equal(new URL(await actors.getAttribute('href'), config.baseUrl).pathname, '/ops/admin/actors');
  await actors.click(); await page.waitForURL(url => url.pathname === '/ops/admin/actors');
  const create = page.getByRole('button', {name: `Create actor in ${orgName}`, exact: true}); await create.waitFor();
  const name = `Unbound research actor ${suffix}`;
  await page.getByLabel('New actor display name', {exact: true}).fill(name);
  const unbound = await command(page, create, 'POST', `/api/admin/orgs/${orgId}/actors`, 201);
  assert.equal(unbound.body.user.identityBound, false); assert.equal(unbound.body.user.identityEnrollmentRequired, true);
  assert.ok(!Object.hasOwn(unbound.sent, 'axxiumPrincipalId'));
  await page.getByLabel('Search actors', {exact: true}).fill(name);
  await page.getByRole('button').filter({hasText: name}).click();
  await page.getByText('Identity enrollment required', {exact: true}).waitFor();
  assert.equal(await page.getByLabel('Directory contact email', {exact: true}).inputValue(), '');
  await config.shot('admin-01-unbound-directory', 'A named directory row without a principal creates no login; its enrollment requirement is visible.', []);
  await page.getByLabel('Display name', {exact: true}).fill(`${name} revised`);
  const saved = await command(page, page.getByRole('button', {name: 'Save actor profile', exact: true}), 'PATCH', `/api/admin/actors/${unbound.body.user.id}`, 200);
  noIdentityEdits(saved.sent); assert.equal(saved.body.user.displayName, `${name} revised`);
  await page.getByLabel('Search actors', {exact: true}).fill('');
  await page.getByLabel('Existing Axxium principal ID (optional)', {exact: true}).fill(config.principalId);
  assert.equal(await page.getByLabel('New actor ID', {exact: true}).isDisabled(), true);
  assert.equal(await page.getByLabel('New actor contact email', {exact: true}).isDisabled(), true);
  const bound = await command(page, create, 'POST', `/api/admin/orgs/${orgId}/actors`, 201);
  assert.equal(bound.body.user.identityBound, true); assert.equal(bound.body.user.identityEnrollmentRequired, false);
  assert.equal(bound.body.user.principalId, config.principalId); assert.equal(bound.body.user.email, config.principalEmail);
  assert.ok(!Object.hasOwn(bound.sent, 'actorId') && !Object.hasOwn(bound.sent, 'email'));
  await page.getByLabel('Search actors', {exact: true}).fill(config.principalEmail);
  await page.getByRole('button').filter({hasText: config.principalEmail}).click();
  assert.equal(await page.getByLabel('Actor ID', {exact: true}).inputValue(), config.principalId);
  assert.equal(await page.getByLabel('Actor ID', {exact: true}).getAttribute('readonly'), '');
  assert.equal(await page.getByLabel('Identity email (Axxium)', {exact: true}).getAttribute('readonly'), '');
  await config.shot('admin-02-verified-principal-binding', 'Axxium verified the existing principal; this organization receives a membership while identity fields remain read-only.', []);
  await page.getByLabel('Display name', {exact: true}).fill(`Editorial member ${suffix}`);
  const updated = await command(page, page.getByRole('button', {name: 'Save actor profile', exact: true}), 'PATCH', `/api/admin/actors/${bound.body.user.id}`, 200);
  noIdentityEdits(updated.sent); assert.ok(!Object.hasOwn(updated.sent, 'actorId') && !Object.hasOwn(updated.sent, 'status'));
  assert.equal(updated.body.user.principalId, config.principalId);
  await page.getByText('Actor profile updated.', {exact: true}).waitFor();
  await config.shot('admin-03-profile-preserves-identity', 'Profile editing preserves the principal, actor ID, email and unchanged global status.', []);
  process.stdout.write('WARN Admin organization selection grants scoped membership; it does not switch the login organization.\n');
  return {unauthenticatedContext: 401, orgId, unboundUserId: unbound.body.user.id, boundUserId: bound.body.user.id,
    principalId: config.principalId, profilePreservedIdentity: true};
}
