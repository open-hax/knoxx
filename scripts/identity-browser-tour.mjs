/** Real browser identity ceremonies. The supervisor owns isolated keys, accounts and cleanup. */
import assert from 'node:assert/strict';

async function submit(page, label, suffix, status = 200) {
  const pending = page.waitForResponse(response => response.request().method() === 'POST' &&
    new URL(response.url()).pathname.endsWith(suffix)).then(async response => ({status: response.status(), body: await response.json()}));
  await page.getByRole('button', {name: label, exact: true}).click();
  const response = await pending;
  assert.equal(response.status, status, `${suffix}: expected ${status}, got ${response.status}`);
  return response.body;
}
async function signOut(page) {
  await page.getByRole('button', {name: 'Account menu', exact: true}).click();
  await submit(page, 'Sign out', '/api/auth/logout');
  await page.getByText('Identity managed by Axxium', {exact: true}).waitFor();
}
async function account(page, baseUrl) {
  let navigated = false, pending;
  const onNavigation = frame => {if (frame === page.mainFrame()) navigated = true;};
  const onRequest = request => {
    if (navigated && request.method() === 'GET' && new URL(request.url()).pathname === '/api/auth/context') {
      pending = request.response().then(async response => ({status: response.status(), body: await response.json()})).catch(error => ({error}));
    }
  };
  page.on('framenavigated', onNavigation); page.on('request', onRequest);
  try {
    await page.goto(new URL('/account', baseUrl).href);
    await page.getByRole('heading', {name: 'Account & sign-in', exact: true}).waitFor();
    assert.ok(pending, 'The new account page must request its own verified authorization context.');
    const response = await pending;
    if (response.error) throw response.error;
    assert.equal(response.status, 200);
    return response.body;
  } finally {page.off('framenavigated', onNavigation); page.off('request', onRequest);}
}
async function passwordLogin(page, config, identifier) {
  await page.getByLabel('Username or email', {exact: true}).fill(identifier);
  await page.getByLabel('Password', {exact: true}).fill(config.password);
  await submit(page, 'Sign in with password', '/api/auth/local/login');
  return account(page, config.baseUrl);
}
async function pgpProof(page, config, enroll, status = 200) {
  await page.getByLabel(enroll ? 'Armored public key' : 'PGP key fingerprint', {exact: true}).fill(enroll ? config.pgpPublicKey : config.pgpFingerprint);
  await submit(page, 'Get signing challenge', '/pgp/challenge');
  const challenge = await page.getByLabel('Exact signing challenge', {exact: true}).inputValue();
  const signature = await config.signPgpChallenge(challenge);
  assert.ok(signature.includes('BEGIN PGP SIGNATURE'));
  await page.getByLabel('Armored detached signature', {exact: true}).fill(signature);
  return submit(page, 'Verify PGP signature', enroll ? '/pgp/enroll' : '/pgp/verify', status);
}
async function signupTour(page, config, original) {
  const signup = config.signup;
  assert.ok(signup.username && signup.email && signup.password && config.username && config.password);
  await signOut(page);
  let created = false;
  try {
    await page.getByRole('link', {name: 'Create an account', exact: true}).click();
    await page.getByRole('heading', {name: 'Create your Knoxx account', exact: true}).waitFor();
    for (const [label, value] of [['Username', signup.username], ['Email', signup.email],
      ['Display name', signup.displayName || 'Sandbox member'], ['Password', signup.password], ['Confirm password', signup.password]]) {
      await page.getByLabel(label, {exact: true}).fill(value);
    }
    const response = await submit(page, 'Create account', '/api/auth/signup'); created = true;
    const principalId = response.principal?.['principal/id'];
    assert.ok(principalId, `Signup returned no principal identity; keys=${Object.keys(response)}, principal keys=${Object.keys(response.principal || {})}`);
    const context = await account(page, config.baseUrl);
    assert.notEqual(context.user.id, original.user.id); assert.equal(context.actor.id, principalId);
    assert.equal(context.isSystemAdmin, false);
    for (const forbidden of ['platform.org.create', 'platform.roles.manage', 'org.users.create', 'org.members.update']) {
      assert.ok(!context.permissions.includes(forbidden), `Signup must not grant ${forbidden}`);
    }
    await config.shot('identity-07-signup', 'The real signup form created a distinct Axxium principal with ordinary member permissions.', []);
    await page.goto(new URL('/ops/admin/actors', config.baseUrl).href);
    await page.waitForURL(url => url.pathname === '/');
    assert.equal(await page.getByRole('button', {name: /^Create actor in /}).count(), 0);
    await config.shot('identity-08-member-admin-denied', 'An ordinary member cannot open actor administration; the guarded route returned home.', []);
    return {principalId, userId: context.user.id, signup: true, adminUiDenied: true};
  } finally {
    if (created) {
      await account(page, config.baseUrl); await signOut(page);
      assert.equal((await passwordLogin(page, config, config.username)).user.id, original.user.id);
    }
  }
}
async function revocationTour(page, config, original) {
  const pending = page.waitForResponse(response => new URL(response.url()).pathname === '/api/auth/config').then(response => response.json());
  await account(page, config.baseUrl); const registry = await pending;
  if (!registry.credentialListUrl || !registry.credentialRevokeUrl) {
    process.stdout.write('WARN Running Axxium does not advertise credential revocation.\n');
    return {available: false, verified: false};
  }
  assert.ok(config.username && config.password, 'Revocation proof requires password recovery.');
  const inventory = page.getByRole('region', {name: 'Manage sign-in credentials', exact: true});
  const pgp = inventory.locator('[data-credential-method="pgp"]'); await pgp.waitFor();
  assert.equal(await inventory.locator('[data-credential-method="passkey"]').count(), 1);
  await pgp.getByRole('button', {name: /^Revoke /}).click();
  await page.getByRole('group', {name: 'Confirm credential revocation', exact: true}).waitFor();
  await config.shot('identity-09-revocation-confirmation', 'The inventory requires explicit confirmation before removing the PGP binding; another method remains available.', []);
  const denied = page.waitForResponse(response => new URL(response.url()).pathname === '/api/auth/context' && response.status() === 401);
  const revoked = await submit(page, 'Confirm revocation', '/api/auth/credentials/revoke');
  assert.equal(revoked.ok, true); assert.equal(revoked.reauthenticate, true);
  await page.getByText('Axxium sessions ended; sign in again.', {exact: true}).waitFor(); await denied;
  await config.shot('identity-10-axxium-sessions-ended', 'Axxium revoked the binding and its sessions; a fresh context request returned 401. Independent MCP grants and external provider accounts are outside this operation.', []);
  assert.equal((await pgpProof(page, config, false, 401)).code, 'invalid-credentials');
  await page.getByText('Invalid PGP credential', {exact: true}).waitFor();
  await config.shot('identity-11-revoked-pgp-denied', 'The original private key signed a fresh challenge, but login was refused after removal of its local binding.', []);
  assert.equal((await passwordLogin(page, config, config.username)).user.id, original.user.id);
  await page.locator('[data-credential-method="passkey"]').waitFor();
  assert.equal(await page.locator('[data-credential-method="pgp"]').count(), 0);
  await config.shot('identity-12-password-recovery', 'Password login restored the same account. The revoked PGP binding is absent; the passkey remains available.', []);
  return {available: true, verified: true, revokedPgpDenied: true, passwordRecovery: true, passkeyPreserved: true};
}
export async function identityTour(page, config) {
  assert.equal(typeof config.shot, 'function'); assert.equal(typeof config.signPgpChallenge, 'function');
  assert.ok(config.pgpPublicKey?.includes('BEGIN PGP PUBLIC KEY BLOCK') && config.pgpFingerprint);
  const original = await account(page, config.baseUrl);
  const cdp = await page.context().newCDPSession(page); await cdp.send('WebAuthn.enable');
  const {authenticatorId} = await cdp.send('WebAuthn.addVirtualAuthenticator', {options: {protocol: 'ctap2', transport: 'internal',
    hasResidentKey: true, hasUserVerification: true, isUserVerified: true, automaticPresenceSimulation: true}});
  try {
    await submit(page, 'Add a passkey', '/passkey/registration-verify');
    await page.getByText('Passkey added to this account.', {exact: true}).waitFor();
    assert.equal((await cdp.send('WebAuthn.getCredentials', {authenticatorId})).credentials.length, 1);
    await config.shot('identity-01-passkey-enrolled', 'The browser created a real resident passkey and Axxium verified its enrollment.', []);
    await signOut(page); await submit(page, 'Sign in with passkey', '/passkey/authentication-verify'); await account(page, config.baseUrl);
    await config.shot('identity-02-passkey-login', 'A fresh browser assertion restored the enrolled account.', []);
    await pgpProof(page, config, true); await page.getByText('PGP key added to this account.', {exact: true}).waitFor();
    await config.shot('identity-03-pgp-enrolled', 'A detached signature proved possession; only the public key was enrolled.', []);
    await signOut(page); await pgpProof(page, config, false); await account(page, config.baseUrl);
    await config.shot('identity-04-pgp-login', 'A fresh one-time challenge authenticated the enrolled PGP identity.', []);
    for (const [identifier, step, caption] of [[config.username, 'identity-05-username-login', 'The username signs into the same verified account.'],
      [config.email, 'identity-06-email-login', 'The separate email alias signs into the exact same account.']]) {
      if (identifier && config.password) {
        await signOut(page); assert.equal((await passwordLogin(page, config, identifier)).user.id, original.user.id);
        await config.shot(step, caption, []);
      }
    }
    const signup = config.signup ? await signupTour(page, config, original) : null;
    const revocation = await revocationTour(page, config, original);
    process.stdout.write('WARN External OAuth round trips require configured clients and consenting provider accounts; this tour does not invent them.\n');
    return {passkeyEnrollment: true, passkeyLogin: true, pgpEnrollment: true, pgpLogin: true,
      usernameLogin: Boolean(config.username && config.password), emailLogin: Boolean(config.email && config.password),
      signup, revocation, externalOauthVerified: false};
  } finally {await cdp.send('WebAuthn.removeVirtualAuthenticator', {authenticatorId}); await cdp.detach();}
}
