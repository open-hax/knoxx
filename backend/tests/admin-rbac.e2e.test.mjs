import test from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';

const BASE_URL = (process.env.KNOXX_E2E_BASE_URL || 'http://localhost').replace(/\/$/, '');
const DEFAULT_TIMEOUT_MS = Number(process.env.KNOXX_E2E_TIMEOUT_MS || 30_000);
const DEFAULT_AUTH = {
  userEmail: process.env.KNOXX_E2E_USER_EMAIL || 'system-admin@open-hax.local',
  orgSlug: process.env.KNOXX_E2E_ORG_SLUG || 'open-hax',
};

function authHeaders(overrides = {}) {
  const headers = {
    'x-knoxx-user-email': overrides.userEmail || DEFAULT_AUTH.userEmail,
    'x-knoxx-org-slug': overrides.orgSlug || DEFAULT_AUTH.orgSlug,
  };
  if (overrides.membershipId) {
    headers['x-knoxx-membership-id'] = overrides.membershipId;
  }
  if (overrides.orgId) {
    headers['x-knoxx-org-id'] = overrides.orgId;
  }
  return headers;
}

async function sleep(ms) {
  await new Promise((resolve) => setTimeout(resolve, ms));
}

async function requestJson(path, { method = 'GET', body, expectedStatus = 200, headers = {} } = {}) {
  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: {
      ...authHeaders(),
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  const text = await response.text();
  let json = null;
  if (text) {
    try {
      json = JSON.parse(text);
    } catch {
      json = { raw: text };
    }
  }

  assert.equal(
    response.status,
    expectedStatus,
    `${method} ${path} expected ${expectedStatus} but got ${response.status}: ${text}`,
  );

  return json;
}

async function waitForApi() {
  const startedAt = Date.now();
  let lastError = null;

  while (Date.now() - startedAt < DEFAULT_TIMEOUT_MS) {
    try {
      const response = await fetch(`${BASE_URL}/health`);
      if (response.ok) {
        return;
      }
      lastError = new Error(`Health returned ${response.status}`);
    } catch (error) {
      lastError = error;
    }
    await sleep(1000);
  }

  throw new Error(`Knoxx e2e target ${BASE_URL} never became healthy: ${lastError}`);
}

function uniqueSlug(prefix) {
  return `${prefix}-${randomUUID().slice(0, 8)}`;
}

async function createOrg(overrides = {}) {
  const slug = overrides.slug || uniqueSlug('e2e-org');
  const name = overrides.name || `E2E Org ${slug}`;
  const result = await requestJson('/api/admin/orgs', {
    method: 'POST',
    expectedStatus: 201,
    body: {
      name,
      slug,
      kind: overrides.kind || 'customer',
      ...overrides,
    },
  });
  return result.org;
}

async function createRole(orgId, overrides = {}) {
  const slug = overrides.slug || uniqueSlug('e2e-role');
  const name = overrides.name || `E2E Role ${slug}`;
  const result = await requestJson(`/api/admin/orgs/${orgId}/roles`, {
    method: 'POST',
    expectedStatus: 201,
    body: {
      name,
      slug,
      permissionCodes: overrides.permissionCodes || ['agent.chat.use', 'agent.runs.read_own', 'tool.read.use'],
      toolPolicies: overrides.toolPolicies || [
        { toolId: 'read', effect: 'allow' },
        { toolId: 'canvas', effect: 'allow' },
      ],
      ...overrides,
    },
  });
  return result.role;
}

async function createUser(orgId, overrides = {}) {
  const email = overrides.email || `${uniqueSlug('user')}@example.test`;
  const result = await requestJson(`/api/admin/orgs/${orgId}/users`, {
    method: 'POST',
    expectedStatus: 201,
    body: {
      email,
      displayName: overrides.displayName || `User ${email}`,
      roleSlugs: overrides.roleSlugs || ['knowledge_worker'],
      ...overrides,
    },
  });
  return result.user;
}

async function createDataLake(orgId, orgSlug, overrides = {}) {
  const slug = overrides.slug || uniqueSlug('lake');
  const name = overrides.name || `Lake ${slug}`;
  const result = await requestJson(`/api/admin/orgs/${orgId}/data-lakes`, {
    method: 'POST',
    expectedStatus: 201,
    body: {
      name,
      slug,
      kind: overrides.kind || 'workspace_docs',
      config: overrides.config || {
        workspaceRoot: `orgs/${orgSlug}`,
      },
      ...overrides,
    },
  });
  return result.dataLake;
}

test('Knoxx admin bootstrap surfaces RBAC seed data', async () => {
  await waitForApi();

  const config = await requestJson('/api/config');
  assert.equal(config.rbac_enabled, true);

  const bootstrap = await requestJson('/api/admin/bootstrap');
  assert.equal(bootstrap.primaryOrg.slug, 'open-hax');
  assert.equal(bootstrap.primaryOrg.isPrimary, true);
  assert.match(bootstrap.bootstrapUser.email, /@/);

  const permissions = await requestJson('/api/admin/permissions');
  assert.ok(Array.isArray(permissions.permissions));
  assert.ok(permissions.permissions.length >= 20);
  assert.ok(permissions.permissions.some((entry) => entry.code === 'platform.org.create'));
  assert.ok(permissions.permissions.some((entry) => entry.code === 'tool.bash.use'));

  const tools = await requestJson('/api/admin/tools');
  assert.ok(Array.isArray(tools.tools));
  assert.ok(tools.tools.some((entry) => entry.id === 'bash' && entry.riskLevel === 'high'));
  assert.ok(tools.tools.some((entry) => entry.id === 'memory_search'));
});

test('Knoxx seeds built-in org roles for new orgs', async () => {
  await waitForApi();

  const org = await createOrg({ slug: uniqueSlug('e2e-role-org') });
  const roleList = await requestJson(`/api/admin/orgs/${org.id}/roles`);
  const roleSlugs = roleList.roles.map((role) => role.slug).sort();

  assert.deepEqual(
    roleSlugs,
    ['data_analyst', 'developer', 'knowledge_worker', 'org_admin'],
  );

  const orgs = await requestJson('/api/admin/orgs');
  assert.ok(orgs.orgs.some((entry) => entry.id === org.id && entry.slug === org.slug));
});

test('Knoxx admin APIs support custom roles, users, membership policies, and data lakes', async () => {
  await waitForApi();

  const org = await createOrg({ slug: uniqueSlug('e2e-admin-org') });

  const createdRole = { role: await createRole(org.id, { name: 'Reporter', slug: uniqueSlug('reporter') }) };

  assert.equal(createdRole.role.name, 'Reporter');
  assert.ok(createdRole.role.permissions.includes('agent.chat.use'));
  assert.ok(createdRole.role.toolPolicies.some((policy) => policy.toolId === 'read' && policy.effect === 'allow'));

  const createdUser = { user: await createUser(org.id, { displayName: 'E2E Developer', roleSlugs: ['developer'] }) };

  assert.equal(createdUser.user.memberships.length, 1);
  const membership = createdUser.user.memberships[0];
  assert.equal(membership.orgId, org.id);
  assert.ok(membership.roles.some((role) => role.slug === 'developer'));

  const updatedMembership = await requestJson(`/api/admin/memberships/${membership.id}/tool-policies`, {
    method: 'PATCH',
    body: {
      toolPolicies: [
        { toolId: 'bash', effect: 'deny' },
        { toolId: 'read', effect: 'allow' },
        { toolId: 'canvas', effect: 'allow' },
      ],
    },
  });

  assert.ok(updatedMembership.membership.toolPolicies.some((policy) => policy.toolId === 'bash' && policy.effect === 'deny'));
  assert.ok(updatedMembership.membership.toolPolicies.some((policy) => policy.toolId === 'read' && policy.effect === 'allow'));

  const dataLakeResponse = { dataLake: await createDataLake(org.id, org.slug, { name: 'E2E Knowledge Lake' }) };

  assert.equal(dataLakeResponse.dataLake.orgId, org.id);
  assert.equal(dataLakeResponse.dataLake.kind, 'workspace_docs');
  assert.equal(dataLakeResponse.dataLake.config.workspaceRoot, `orgs/${org.slug}`);

  const orgUsers = await requestJson(`/api/admin/orgs/${org.id}/users`);
  assert.ok(orgUsers.users.some((user) => user.id === createdUser.user.id));

  const memberships = await requestJson(`/api/admin/orgs/${org.id}/memberships`);
  const foundMembership = memberships.memberships.find((entry) => entry.id === membership.id);
  assert.ok(foundMembership);
  assert.ok(foundMembership.roles.some((role) => role.slug === 'developer'));
  assert.ok(foundMembership.toolPolicies.some((policy) => policy.toolId === 'bash' && policy.effect === 'deny'));

  const lakes = await requestJson(`/api/admin/orgs/${org.id}/data-lakes`);
  assert.ok(lakes.dataLakes.some((entry) => entry.id === dataLakeResponse.dataLake.id));
});

test('Knoxx org admin listings stay scoped to the selected org resources', async () => {
  await waitForApi();

  const orgA = await createOrg({ slug: uniqueSlug('e2e-scope-a') });
  const orgB = await createOrg({ slug: uniqueSlug('e2e-scope-b') });

  const roleA = await createRole(orgA.id, { name: 'Org A Reporter', slug: uniqueSlug('org-a-reporter') });
  const roleB = await createRole(orgB.id, { name: 'Org B Reporter', slug: uniqueSlug('org-b-reporter') });

  const userA = await createUser(orgA.id, { displayName: 'Scoped User A', roleSlugs: ['developer'] });
  const userB = await createUser(orgB.id, { displayName: 'Scoped User B', roleSlugs: ['data_analyst'] });

  const lakeA = await createDataLake(orgA.id, orgA.slug, { name: 'Scoped Lake A' });
  const lakeB = await createDataLake(orgB.id, orgB.slug, { name: 'Scoped Lake B' });

  const orgAUsers = await requestJson(`/api/admin/orgs/${orgA.id}/users`);
  assert.ok(orgAUsers.users.some((entry) => entry.id === userA.id));
  assert.ok(orgAUsers.users.every((entry) => entry.memberships.every((membership) => membership.orgId === orgA.id)));
  assert.ok(orgAUsers.users.every((entry) => entry.id !== userB.id));

  const orgBUsers = await requestJson(`/api/admin/orgs/${orgB.id}/users`);
  assert.ok(orgBUsers.users.some((entry) => entry.id === userB.id));
  assert.ok(orgBUsers.users.every((entry) => entry.memberships.every((membership) => membership.orgId === orgB.id)));
  assert.ok(orgBUsers.users.every((entry) => entry.id !== userA.id));

  const orgARoles = await requestJson(`/api/admin/orgs/${orgA.id}/roles`);
  assert.ok(orgARoles.roles.some((entry) => entry.id === roleA.id));
  assert.ok(orgARoles.roles.every((entry) => entry.id !== roleB.id));

  const orgBRoles = await requestJson(`/api/admin/orgs/${orgB.id}/roles`);
  assert.ok(orgBRoles.roles.some((entry) => entry.id === roleB.id));
  assert.ok(orgBRoles.roles.every((entry) => entry.id !== roleA.id));

  const orgALakes = await requestJson(`/api/admin/orgs/${orgA.id}/data-lakes`);
  assert.ok(orgALakes.dataLakes.some((entry) => entry.id === lakeA.id));
  assert.ok(orgALakes.dataLakes.every((entry) => entry.id !== lakeB.id));

  const orgBLakes = await requestJson(`/api/admin/orgs/${orgB.id}/data-lakes`);
  assert.ok(orgBLakes.dataLakes.some((entry) => entry.id === lakeB.id));
  assert.ok(orgBLakes.dataLakes.every((entry) => entry.id !== lakeA.id));
});

test('Knoxx admin APIs round-trip role policy updates and membership role replacement', async () => {
  await waitForApi();

  const org = await createOrg({ slug: uniqueSlug('e2e-role-update-org') });
  const reporterRole = await createRole(org.id, {
    name: 'Reporter',
    slug: uniqueSlug('reporter-update'),
    permissionCodes: ['agent.chat.use', 'tool.read.use'],
    toolPolicies: [
      { toolId: 'read', effect: 'allow' },
      { toolId: 'canvas', effect: 'allow' },
    ],
  });

  const patchedRole = await requestJson(`/api/admin/roles/${reporterRole.id}/tool-policies`, {
    method: 'PATCH',
    body: {
      toolPolicies: [
        { toolId: 'read', effect: 'allow' },
        { toolId: 'bash', effect: 'deny' },
      ],
    },
  });

  assert.ok(patchedRole.role.toolPolicies.some((policy) => policy.toolId === 'bash' && policy.effect === 'deny'));
  assert.ok(patchedRole.role.toolPolicies.some((policy) => policy.toolId === 'read' && policy.effect === 'allow'));
  assert.ok(patchedRole.role.toolPolicies.every((policy) => policy.toolId !== 'canvas'));

  const createdUser = await createUser(org.id, {
    displayName: 'Role Replacement User',
    roleSlugs: ['knowledge_worker'],
  });
  const membership = createdUser.memberships[0];
  assert.ok(membership.roles.some((role) => role.slug === 'knowledge_worker'));

  const updatedMembership = await requestJson(`/api/admin/memberships/${membership.id}/roles`, {
    method: 'PATCH',
    body: {
      roleSlugs: [reporterRole.slug],
      replace: true,
    },
  });

  assert.deepEqual(
    updatedMembership.membership.roles.map((role) => role.slug),
    [reporterRole.slug],
  );

  const memberships = await requestJson(`/api/admin/orgs/${org.id}/memberships`);
  const persistedMembership = memberships.memberships.find((entry) => entry.id === membership.id);
  assert.ok(persistedMembership);
  assert.deepEqual(
    persistedMembership.roles.map((role) => role.slug),
    [reporterRole.slug],
  );
});

test('Knoxx denies admin route access to a knowledge worker', async () => {
  await waitForApi();

  const org = await createOrg({ slug: uniqueSlug('e2e-deny-knowledge') });
  const user = await createUser(org.id, {
    displayName: 'Knowledge Worker Denied',
    roleSlugs: ['knowledge_worker'],
  });

  const denied = await requestJson(`/api/admin/orgs/${org.id}/users`, {
    expectedStatus: 403,
    headers: authHeaders({ userEmail: user.email, orgSlug: org.slug }),
  });

  assert.equal(denied.error_code, 'permission_denied');
});

test('Knoxx keeps org admin access scoped to their own org', async () => {
  await waitForApi();

  const orgA = await createOrg({ slug: uniqueSlug('e2e-org-admin-a') });
  const orgB = await createOrg({ slug: uniqueSlug('e2e-org-admin-b') });
  const orgAdmin = await createUser(orgA.id, {
    displayName: 'Scoped Org Admin',
    roleSlugs: ['org_admin'],
  });

  const denied = await requestJson(`/api/admin/orgs/${orgB.id}/users`, {
    expectedStatus: 403,
    headers: authHeaders({ userEmail: orgAdmin.email, orgSlug: orgA.slug }),
  });

  assert.equal(denied.error_code, 'org_scope_denied');
});

test('Knoxx membership tool-policy deny blocks bash execution', async () => {
  await waitForApi();

  const org = await createOrg({ slug: uniqueSlug('e2e-bash-deny-org') });
  const user = await createUser(org.id, {
    displayName: 'Developer With Bash Denied',
    roleSlugs: ['developer'],
  });
  const membership = user.memberships[0];

  await requestJson(`/api/admin/memberships/${membership.id}/tool-policies`, {
    method: 'PATCH',
    body: {
      toolPolicies: [
        { toolId: 'bash', effect: 'deny' },
        { toolId: 'read', effect: 'allow' },
      ],
    },
  });

  const denied = await requestJson('/api/tools/bash', {
    method: 'POST',
    expectedStatus: 403,
    headers: authHeaders({ userEmail: user.email, orgSlug: org.slug }),
    body: { command: 'echo should-not-run' },
  });

  assert.equal(denied.error_code, 'tool_denied');
});

test('Knoxx denies cross-session memory search to a knowledge worker', async () => {
  await waitForApi();

  const org = await createOrg({ slug: uniqueSlug('e2e-memory-deny-org') });
  const user = await createUser(org.id, {
    displayName: 'Knowledge Worker Memory Denied',
    roleSlugs: ['knowledge_worker'],
  });

  const denied = await requestJson('/api/memory/search', {
    method: 'POST',
    expectedStatus: 403,
    headers: authHeaders({ userEmail: user.email, orgSlug: org.slug }),
    body: { query: 'previous session token' },
  });

  assert.equal(denied.error_code, 'memory_scope_denied');
});
