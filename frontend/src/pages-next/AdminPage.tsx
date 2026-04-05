import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  createAdminOrg,
  createOrgDataLake,
  createOrgRole,
  createOrgUser,
  getAdminBootstrap,
  getKnoxxAuthContext,
  getKnoxxAuthIdentity,
  listAdminOrgs,
  listAdminPermissions,
  listAdminTools,
  listOrgDataLakes,
  listOrgRoles,
  listOrgUsers,
  setKnoxxAuthIdentity,
  updateMembershipRoles,
  updateMembershipToolPolicies,
  updateRoleToolPolicies,
} from '../lib/nextApi';
import type {
  AdminDataLakeSummary,
  AdminMembershipSummary,
  AdminOrgSummary,
  AdminPermissionDefinition,
  AdminRoleSummary,
  AdminToolDefinition,
  AdminToolPolicy,
  AdminUserSummary,
  KnoxxAuthContext,
  KnoxxAuthIdentity,
} from '../lib/types';

type Notice = { tone: 'success' | 'error'; text: string } | null;
type ToolDraftEffect = 'inherit' | 'allow' | 'deny';

type UserFormState = {
  email: string;
  displayName: string;
  roleSlugs: string[];
};

type RoleFormState = {
  name: string;
  slug: string;
  permissionCodes: string[];
  toolIds: string[];
};

type OrgFormState = {
  name: string;
  slug: string;
  kind: string;
};

type LakeFormState = {
  name: string;
  slug: string;
  kind: string;
  workspaceRoot: string;
};

const ORG_KIND_OPTIONS = ['platform_owner', 'customer', 'internal', 'partner'];
const DATA_LAKE_KIND_OPTIONS = ['workspace_docs', 'analytics', 'notes', 'uploads'];

function classNames(...values: Array<string | false | null | undefined>): string {
  return values.filter(Boolean).join(' ');
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

function groupPermissions(permissions: AdminPermissionDefinition[]): Array<[string, AdminPermissionDefinition[]]> {
  const grouped = permissions.reduce<Record<string, AdminPermissionDefinition[]>>((acc, permission) => {
    const key = permission.resourceKind || 'misc';
    if (!acc[key]) acc[key] = [];
    acc[key].push(permission);
    return acc;
  }, {});

  return Object.entries(grouped)
    .map(([kind, items]) => [kind, [...items].sort((a, b) => a.code.localeCompare(b.code))] as [string, AdminPermissionDefinition[]])
    .sort(([a], [b]) => a.localeCompare(b));
}

function membershipForOrg(user: AdminUserSummary, orgId: string): AdminMembershipSummary | null {
  return user.memberships.find((membership) => membership.orgId === orgId) ?? null;
}

function toolDraftMap(policies: AdminToolPolicy[]): Record<string, ToolDraftEffect> {
  return policies.reduce<Record<string, ToolDraftEffect>>((acc, policy) => {
    acc[policy.toolId] = policy.effect;
    return acc;
  }, {});
}

function toolPoliciesFromDraft(draft: Record<string, ToolDraftEffect>): AdminToolPolicy[] {
  return Object.entries(draft)
    .filter(([, effect]) => effect === 'allow' || effect === 'deny')
    .map(([toolId, effect]) => ({ toolId, effect }));
}

function toggleListValue(values: string[], value: string): string[] {
  return values.includes(value)
    ? values.filter((entry) => entry !== value)
    : [...values, value];
}

function SectionCard({
  title,
  description,
  actions,
  children,
}: {
  title: string;
  description?: string;
  actions?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-slate-800 bg-slate-950/70 p-5 shadow-xl">
      <div className="mb-4 flex flex-col gap-3 border-b border-slate-800 pb-4 md:flex-row md:items-start md:justify-between">
        <div>
          <h2 className="text-lg font-semibold text-slate-100">{title}</h2>
          {description ? <p className="mt-1 text-sm text-slate-400">{description}</p> : null}
        </div>
        {actions ? <div className="shrink-0">{actions}</div> : null}
      </div>
      {children}
    </section>
  );
}

function Badge({ children, tone = 'default' }: { children: React.ReactNode; tone?: 'default' | 'success' | 'warn' | 'danger' | 'info' }) {
  const toneClass = {
    default: 'border-slate-700 bg-slate-800 text-slate-200',
    success: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200',
    warn: 'border-amber-500/30 bg-amber-500/10 text-amber-200',
    danger: 'border-rose-500/30 bg-rose-500/10 text-rose-200',
    info: 'border-cyan-500/30 bg-cyan-500/10 text-cyan-200',
  }[tone];

  return <span className={classNames('inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-medium', toneClass)}>{children}</span>;
}

export default function AdminPage() {
  const [identityForm, setIdentityForm] = useState<KnoxxAuthIdentity>(() => getKnoxxAuthIdentity());
  const [context, setContext] = useState<KnoxxAuthContext | null>(null);
  const [bootstrap, setBootstrap] = useState<any>(null);
  const [permissions, setPermissions] = useState<AdminPermissionDefinition[]>([]);
  const [tools, setTools] = useState<AdminToolDefinition[]>([]);
  const [orgs, setOrgs] = useState<AdminOrgSummary[]>([]);
  const [selectedOrgId, setSelectedOrgId] = useState('');
  const [roles, setRoles] = useState<AdminRoleSummary[]>([]);
  const [users, setUsers] = useState<AdminUserSummary[]>([]);
  const [dataLakes, setDataLakes] = useState<AdminDataLakeSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [orgLoading, setOrgLoading] = useState(false);
  const [notice, setNotice] = useState<Notice>(null);
  const [error, setError] = useState('');
  const [creatingOrg, setCreatingOrg] = useState(false);
  const [creatingUser, setCreatingUser] = useState(false);
  const [creatingRole, setCreatingRole] = useState(false);
  const [creatingLake, setCreatingLake] = useState(false);
  const [savingMembershipId, setSavingMembershipId] = useState<string | null>(null);
  const [savingRoleId, setSavingRoleId] = useState<string | null>(null);
  const [orgForm, setOrgForm] = useState<OrgFormState>({ name: '', slug: '', kind: 'customer' });
  const [userForm, setUserForm] = useState<UserFormState>({ email: '', displayName: '', roleSlugs: ['knowledge_worker'] });
  const [roleForm, setRoleForm] = useState<RoleFormState>({ name: '', slug: '', permissionCodes: [], toolIds: ['read', 'canvas'] });
  const [lakeForm, setLakeForm] = useState<LakeFormState>({ name: '', slug: '', kind: 'workspace_docs', workspaceRoot: '' });
  const [membershipRoleDrafts, setMembershipRoleDrafts] = useState<Record<string, string[]>>({});
  const [membershipToolDrafts, setMembershipToolDrafts] = useState<Record<string, Record<string, ToolDraftEffect>>>({});
  const [roleToolDrafts, setRoleToolDrafts] = useState<Record<string, Record<string, ToolDraftEffect>>>({});

  const permissionGroups = useMemo(() => groupPermissions(permissions), [permissions]);
  const selectedOrg = useMemo(
    () => orgs.find((org) => org.id === selectedOrgId) ?? (context ? {
      id: context.org.id,
      slug: context.org.slug,
      name: context.org.name,
      kind: context.org.kind || 'customer',
      isPrimary: Boolean(context.org.isPrimary),
      status: context.org.status,
    } : null),
    [context, orgs, selectedOrgId],
  );

  const hasPermission = useCallback(
    (permission: string) => Boolean(context?.isSystemAdmin || context?.permissions.includes(permission)),
    [context],
  );

  const canCreateOrgs = hasPermission('platform.org.create');
  const canReadUsers = hasPermission('org.users.read');
  const canCreateUsers = hasPermission('org.users.create');
  const canReadRoles = hasPermission('org.roles.read');
  const canCreateRoles = hasPermission('org.roles.create');
  const canUpdateMemberships = hasPermission('org.members.update');
  const canUpdateUserPolicies = hasPermission('org.user_policy.update');
  const canUpdateRolePolicies = hasPermission('org.tool_policy.update');
  const canReadDataLakes = hasPermission('org.datalakes.read');
  const canCreateDataLakes = hasPermission('org.datalakes.create');
  const looksLikeAdmin = Boolean(context?.isSystemAdmin || hasPermission('org.users.read') || hasPermission('org.roles.read'));

  const hydrateMembershipDrafts = useCallback((nextUsers: AdminUserSummary[], orgId: string) => {
    const nextRoleDrafts: Record<string, string[]> = {};
    const nextToolDrafts: Record<string, Record<string, ToolDraftEffect>> = {};

    for (const user of nextUsers) {
      const membership = membershipForOrg(user, orgId);
      if (!membership) continue;
      nextRoleDrafts[membership.id] = membership.roles.map((role) => role.slug);
      nextToolDrafts[membership.id] = toolDraftMap(membership.toolPolicies);
    }

    setMembershipRoleDrafts(nextRoleDrafts);
    setMembershipToolDrafts(nextToolDrafts);
  }, []);

  const hydrateRoleDrafts = useCallback((nextRoles: AdminRoleSummary[]) => {
    const nextDrafts: Record<string, Record<string, ToolDraftEffect>> = {};
    for (const role of nextRoles) {
      nextDrafts[role.id] = toolDraftMap(role.toolPolicies);
    }
    setRoleToolDrafts(nextDrafts);
  }, []);

  const loadOrgResources = useCallback(async (orgId: string) => {
    if (!orgId) return;
    setOrgLoading(true);
    setError('');
    try {
      const results = await Promise.all([
        canReadRoles ? listOrgRoles(orgId) : Promise.resolve({ roles: [] }),
        canReadUsers ? listOrgUsers(orgId) : Promise.resolve({ users: [] }),
        canReadDataLakes ? listOrgDataLakes(orgId) : Promise.resolve({ dataLakes: [] }),
      ]);
      const [roleResult, userResult, lakeResult] = results;
      setRoles(roleResult.roles);
      setUsers(userResult.users);
      setDataLakes(lakeResult.dataLakes);
      hydrateRoleDrafts(roleResult.roles);
      hydrateMembershipDrafts(userResult.users, orgId);
    } catch (loadError) {
      setError(errorMessage(loadError));
    } finally {
      setOrgLoading(false);
    }
  }, [canReadDataLakes, canReadRoles, canReadUsers, hydrateMembershipDrafts, hydrateRoleDrafts]);

  const loadAdminSurface = useCallback(async () => {
    setLoading(true);
    setError('');
    setNotice(null);

    try {
      const ctx = await getKnoxxAuthContext();
      setContext(ctx);
      setIdentityForm({ userEmail: ctx.user.email, orgSlug: ctx.org.slug });

      const [permissionsResult, toolsResult, bootstrapResult, orgsResult] = await Promise.allSettled([
        listAdminPermissions(),
        listAdminTools(),
        getAdminBootstrap(),
        listAdminOrgs(),
      ]);

      setPermissions(permissionsResult.status === 'fulfilled' ? permissionsResult.value.permissions : []);
      setTools(toolsResult.status === 'fulfilled' ? toolsResult.value.tools : []);
      setBootstrap(bootstrapResult.status === 'fulfilled' ? bootstrapResult.value : null);

      const scopedOrgs = orgsResult.status === 'fulfilled' && orgsResult.value.orgs.length > 0
        ? orgsResult.value.orgs
        : [{
            id: ctx.org.id,
            slug: ctx.org.slug,
            name: ctx.org.name,
            kind: ctx.org.kind || 'customer',
            isPrimary: Boolean(ctx.org.isPrimary),
            status: ctx.org.status,
          } satisfies AdminOrgSummary];

      setOrgs(scopedOrgs);
      setSelectedOrgId((current) => {
        if (current && scopedOrgs.some((org) => org.id === current)) {
          return current;
        }
        return scopedOrgs.find((org) => org.id === ctx.org.id)?.id || scopedOrgs[0]?.id || '';
      });
    } catch (loadError) {
      setError(errorMessage(loadError));
      setContext(null);
      setPermissions([]);
      setTools([]);
      setBootstrap(null);
      setOrgs([]);
      setSelectedOrgId('');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadAdminSurface();
  }, [loadAdminSurface]);

  useEffect(() => {
    if (!selectedOrgId) return;
    void loadOrgResources(selectedOrgId);
  }, [loadOrgResources, selectedOrgId]);

  useEffect(() => {
    if (!selectedOrg) return;
    setLakeForm((current) => ({
      ...current,
      workspaceRoot: current.workspaceRoot || `orgs/${selectedOrg.slug}`,
    }));
  }, [selectedOrg]);

  const handleApplyIdentity = async (event: React.FormEvent) => {
    event.preventDefault();
    const resolved = setKnoxxAuthIdentity(identityForm);
    setIdentityForm(resolved);
    setNotice({ tone: 'success', text: `Switched admin actor to ${resolved.userEmail} in ${resolved.orgSlug}.` });
    await loadAdminSurface();
  };

  const handleCreateOrg = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!canCreateOrgs) return;
    setCreatingOrg(true);
    setNotice(null);
    try {
      const result = await createAdminOrg({
        name: orgForm.name.trim(),
        slug: orgForm.slug.trim() || undefined,
        kind: orgForm.kind,
      });
      setOrgForm({ name: '', slug: '', kind: 'customer' });
      setNotice({ tone: 'success', text: `Created org ${result.org.name}.` });
      await loadAdminSurface();
      setSelectedOrgId(result.org.id);
    } catch (createError) {
      setNotice({ tone: 'error', text: errorMessage(createError) });
    } finally {
      setCreatingOrg(false);
    }
  };

  const handleCreateUser = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!selectedOrgId) return;
    setCreatingUser(true);
    setNotice(null);
    try {
      await createOrgUser(selectedOrgId, {
        email: userForm.email.trim(),
        displayName: userForm.displayName.trim() || userForm.email.trim(),
        roleSlugs: userForm.roleSlugs.length > 0 ? userForm.roleSlugs : ['knowledge_worker'],
      });
      setUserForm({ email: '', displayName: '', roleSlugs: ['knowledge_worker'] });
      setNotice({ tone: 'success', text: 'User created and membership seeded.' });
      await loadOrgResources(selectedOrgId);
    } catch (createError) {
      setNotice({ tone: 'error', text: errorMessage(createError) });
    } finally {
      setCreatingUser(false);
    }
  };

  const handleCreateRole = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!selectedOrgId) return;
    setCreatingRole(true);
    setNotice(null);
    try {
      await createOrgRole(selectedOrgId, {
        name: roleForm.name.trim(),
        slug: roleForm.slug.trim() || undefined,
        permissionCodes: roleForm.permissionCodes,
        toolPolicies: roleForm.toolIds.map((toolId) => ({ toolId, effect: 'allow' })),
      });
      setRoleForm({ name: '', slug: '', permissionCodes: [], toolIds: ['read', 'canvas'] });
      setNotice({ tone: 'success', text: 'Custom role created.' });
      await loadOrgResources(selectedOrgId);
    } catch (createError) {
      setNotice({ tone: 'error', text: errorMessage(createError) });
    } finally {
      setCreatingRole(false);
    }
  };

  const handleCreateLake = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!selectedOrgId) return;
    setCreatingLake(true);
    setNotice(null);
    try {
      await createOrgDataLake(selectedOrgId, {
        name: lakeForm.name.trim(),
        slug: lakeForm.slug.trim() || undefined,
        kind: lakeForm.kind,
        config: lakeForm.workspaceRoot.trim() ? { workspaceRoot: lakeForm.workspaceRoot.trim() } : {},
      });
      setLakeForm({ name: '', slug: '', kind: 'workspace_docs', workspaceRoot: selectedOrg ? `orgs/${selectedOrg.slug}` : '' });
      setNotice({ tone: 'success', text: 'Data lake created.' });
      await loadOrgResources(selectedOrgId);
    } catch (createError) {
      setNotice({ tone: 'error', text: errorMessage(createError) });
    } finally {
      setCreatingLake(false);
    }
  };

  const handleSaveMembershipRoles = async (membershipId: string) => {
    setSavingMembershipId(membershipId);
    setNotice(null);
    try {
      await updateMembershipRoles(membershipId, membershipRoleDrafts[membershipId] || []);
      setNotice({ tone: 'success', text: 'Membership roles updated.' });
      if (selectedOrgId) {
        await loadOrgResources(selectedOrgId);
      }
    } catch (saveError) {
      setNotice({ tone: 'error', text: errorMessage(saveError) });
    } finally {
      setSavingMembershipId(null);
    }
  };

  const handleSaveMembershipPolicies = async (membershipId: string) => {
    setSavingMembershipId(membershipId);
    setNotice(null);
    try {
      await updateMembershipToolPolicies(membershipId, toolPoliciesFromDraft(membershipToolDrafts[membershipId] || {}));
      setNotice({ tone: 'success', text: 'Membership tool overrides updated.' });
      if (selectedOrgId) {
        await loadOrgResources(selectedOrgId);
      }
    } catch (saveError) {
      setNotice({ tone: 'error', text: errorMessage(saveError) });
    } finally {
      setSavingMembershipId(null);
    }
  };

  const handleSaveRolePolicies = async (roleId: string) => {
    setSavingRoleId(roleId);
    setNotice(null);
    try {
      await updateRoleToolPolicies(roleId, toolPoliciesFromDraft(roleToolDrafts[roleId] || {}));
      setNotice({ tone: 'success', text: 'Role tool policy updated.' });
      if (selectedOrgId) {
        await loadOrgResources(selectedOrgId);
      }
    } catch (saveError) {
      setNotice({ tone: 'error', text: errorMessage(saveError) });
    } finally {
      setSavingRoleId(null);
    }
  };

  if (loading) {
    return <div className="p-8 text-sm text-slate-300">Loading Knoxx admin surface…</div>;
  }

  return (
    <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 p-6 md:p-8">
      <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-50">Admin Control Plane</h1>
          <p className="mt-2 max-w-3xl text-sm text-slate-400">
            Manage Knoxx orgs, users, roles, tool policies, and org-owned data lakes from the live RBAC control plane.
          </p>
        </div>
        <button
          type="button"
          onClick={() => void loadAdminSurface()}
          className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800"
        >
          Refresh Admin Surface
        </button>
      </header>

      {notice ? (
        <div className={classNames(
          'rounded-xl border px-4 py-3 text-sm',
          notice.tone === 'success'
            ? 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200'
            : 'border-rose-500/30 bg-rose-500/10 text-rose-200',
        )}>
          {notice.text}
        </div>
      ) : null}

      {error ? (
        <div className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
          {error}
        </div>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <SectionCard
          title="Current actor"
          description="Header-based request identity for the live admin surface."
        >
          <form className="grid gap-4 md:grid-cols-[1fr_1fr_auto]" onSubmit={handleApplyIdentity}>
            <label className="flex flex-col gap-2 text-sm text-slate-300">
              User email
              <input
                className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                value={identityForm.userEmail}
                onChange={(event) => setIdentityForm((current) => ({ ...current, userEmail: event.target.value }))}
                placeholder="system-admin@open-hax.local"
              />
            </label>
            <label className="flex flex-col gap-2 text-sm text-slate-300">
              Org slug
              <input
                className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                value={identityForm.orgSlug}
                onChange={(event) => setIdentityForm((current) => ({ ...current, orgSlug: event.target.value }))}
                placeholder="open-hax"
              />
            </label>
            <div className="flex items-end">
              <button
                type="submit"
                className="w-full rounded-lg border border-cyan-500/30 bg-cyan-500/10 px-4 py-2 text-sm font-medium text-cyan-200 hover:bg-cyan-500/20"
              >
                Apply actor
              </button>
            </div>
          </form>

          {context ? (
            <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
                <div className="text-xs uppercase tracking-wide text-slate-500">Resolved user</div>
                <div className="mt-2 text-sm font-semibold text-slate-100">{context.user.displayName}</div>
                <div className="text-sm text-slate-400">{context.user.email}</div>
              </div>
              <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
                <div className="text-xs uppercase tracking-wide text-slate-500">Resolved org</div>
                <div className="mt-2 text-sm font-semibold text-slate-100">{context.org.name}</div>
                <div className="text-sm text-slate-400">{context.org.slug}</div>
              </div>
              <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
                <div className="text-xs uppercase tracking-wide text-slate-500">Primary role</div>
                <div className="mt-2 text-sm font-semibold text-slate-100">{context.primaryRole}</div>
                <div className="mt-2 flex flex-wrap gap-2">
                  {context.isSystemAdmin ? <Badge tone="warn">system admin</Badge> : null}
                  <Badge tone="info">{context.permissions.length} permissions</Badge>
                </div>
              </div>
              <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
                <div className="text-xs uppercase tracking-wide text-slate-500">Membership</div>
                <div className="mt-2 text-sm font-semibold text-slate-100">{context.membership.id}</div>
                <div className="text-sm text-slate-400">{context.membership.status}</div>
              </div>
            </div>
          ) : null}

          {context ? (
            <div className="mt-4 flex flex-wrap gap-2">
              {context.roleSlugs.map((role) => (
                <Badge key={role} tone={role === 'system_admin' ? 'warn' : 'default'}>{role}</Badge>
              ))}
            </div>
          ) : null}
        </SectionCard>

        <SectionCard
          title="Control-plane summary"
          description="What this actor can see right now."
        >
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
              <div className="text-xs uppercase tracking-wide text-slate-500">Org scope</div>
              <div className="mt-2 text-2xl font-semibold text-slate-100">{orgs.length}</div>
              <div className="text-sm text-slate-400">Visible org entries</div>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
              <div className="text-xs uppercase tracking-wide text-slate-500">Catalog</div>
              <div className="mt-2 text-2xl font-semibold text-slate-100">{tools.length}</div>
              <div className="text-sm text-slate-400">Tool definitions</div>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
              <div className="text-xs uppercase tracking-wide text-slate-500">Permission atoms</div>
              <div className="mt-2 text-2xl font-semibold text-slate-100">{permissions.length}</div>
              <div className="text-sm text-slate-400">Available policy building blocks</div>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
              <div className="text-xs uppercase tracking-wide text-slate-500">Bootstrap</div>
              {bootstrap ? (
                <>
                  <div className="mt-2 text-sm font-semibold text-slate-100">{bootstrap.primaryOrg.name}</div>
                  <div className="text-sm text-slate-400">{bootstrap.bootstrapUser.email}</div>
                </>
              ) : (
                <div className="mt-2 text-sm text-slate-400">Platform bootstrap is only visible to platform-scoped admins.</div>
              )}
            </div>
          </div>
        </SectionCard>
      </div>

      {!looksLikeAdmin ? (
        <SectionCard
          title="Admin access required"
          description="This page is live, but the current actor is missing admin permissions."
        >
          <p className="text-sm text-slate-300">
            Switch the actor above to an org admin or system admin to manage users, roles, and data lakes.
          </p>
        </SectionCard>
      ) : (
        <>
          <div className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
            <SectionCard
              title="Organizations"
              description="Platform-wide org inventory for system admins, or the current scoped org for org admins."
              actions={
                <div className="text-xs text-slate-500">
                  {context?.isSystemAdmin ? 'Platform scope' : 'Org scope'}
                </div>
              }
            >
              <div className="space-y-3">
                {orgs.map((org) => {
                  const active = org.id === selectedOrgId;
                  return (
                    <button
                      key={org.id}
                      type="button"
                      onClick={() => setSelectedOrgId(org.id)}
                      className={classNames(
                        'w-full rounded-xl border p-4 text-left transition',
                        active
                          ? 'border-cyan-500/40 bg-cyan-500/10'
                          : 'border-slate-800 bg-slate-900/70 hover:bg-slate-900',
                      )}
                    >
                      <div className="flex items-start justify-between gap-4">
                        <div>
                          <div className="text-sm font-semibold text-slate-100">{org.name}</div>
                          <div className="text-sm text-slate-400">{org.slug}</div>
                        </div>
                        <div className="flex flex-wrap justify-end gap-2">
                          {org.isPrimary ? <Badge tone="warn">primary</Badge> : null}
                          <Badge>{org.kind}</Badge>
                        </div>
                      </div>
                      <div className="mt-3 flex flex-wrap gap-3 text-xs text-slate-400">
                        {typeof org.memberCount === 'number' ? <span>{org.memberCount} members</span> : null}
                        {typeof org.roleCount === 'number' ? <span>{org.roleCount} roles</span> : null}
                        {typeof org.dataLakeCount === 'number' ? <span>{org.dataLakeCount} lakes</span> : null}
                      </div>
                    </button>
                  );
                })}
              </div>

              {canCreateOrgs ? (
                <form className="mt-5 space-y-3 rounded-xl border border-slate-800 bg-slate-900/80 p-4" onSubmit={handleCreateOrg}>
                  <div className="text-sm font-semibold text-slate-100">Create org</div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <input
                      className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                      placeholder="Org name"
                      value={orgForm.name}
                      onChange={(event) => setOrgForm((current) => ({ ...current, name: event.target.value }))}
                    />
                    <input
                      className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                      placeholder="org-slug"
                      value={orgForm.slug}
                      onChange={(event) => setOrgForm((current) => ({ ...current, slug: event.target.value }))}
                    />
                  </div>
                  <select
                    className="w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                    value={orgForm.kind}
                    onChange={(event) => setOrgForm((current) => ({ ...current, kind: event.target.value }))}
                  >
                    {ORG_KIND_OPTIONS.map((kind) => (
                      <option key={kind} value={kind}>{kind}</option>
                    ))}
                  </select>
                  <button
                    type="submit"
                    disabled={creatingOrg || !orgForm.name.trim()}
                    className="rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-4 py-2 text-sm font-medium text-emerald-200 hover:bg-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {creatingOrg ? 'Creating…' : 'Create org'}
                  </button>
                </form>
              ) : null}
            </SectionCard>

            <SectionCard
              title={selectedOrg ? `Selected org: ${selectedOrg.name}` : 'Selected org'}
              description="Scoped management surface for memberships, roles, and data lakes."
            >
              {selectedOrg ? (
                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
                  <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
                    <div className="text-xs uppercase tracking-wide text-slate-500">Slug</div>
                    <div className="mt-2 text-sm font-semibold text-slate-100">{selectedOrg.slug}</div>
                  </div>
                  <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
                    <div className="text-xs uppercase tracking-wide text-slate-500">Kind</div>
                    <div className="mt-2 text-sm font-semibold text-slate-100">{selectedOrg.kind}</div>
                  </div>
                  <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
                    <div className="text-xs uppercase tracking-wide text-slate-500">Status</div>
                    <div className="mt-2 text-sm font-semibold text-slate-100">{selectedOrg.status}</div>
                  </div>
                  <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
                    <div className="text-xs uppercase tracking-wide text-slate-500">Current actor</div>
                    <div className="mt-2 text-sm font-semibold text-slate-100">{context?.user.email || 'Unknown'}</div>
                  </div>
                </div>
              ) : (
                <p className="text-sm text-slate-400">No org is currently selected.</p>
              )}
            </SectionCard>
          </div>

          {orgLoading ? <div className="text-sm text-slate-400">Loading selected org resources…</div> : null}

          <SectionCard
            title="Users and memberships"
            description="Create users, replace membership roles, and apply explicit per-membership tool overrides."
          >
            {selectedOrg && canCreateUsers ? (
              <form className="mb-5 grid gap-3 rounded-xl border border-slate-800 bg-slate-900/80 p-4 md:grid-cols-4" onSubmit={handleCreateUser}>
                <input
                  className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                  placeholder="user@example.com"
                  value={userForm.email}
                  onChange={(event) => setUserForm((current) => ({ ...current, email: event.target.value }))}
                />
                <input
                  className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                  placeholder="Display name"
                  value={userForm.displayName}
                  onChange={(event) => setUserForm((current) => ({ ...current, displayName: event.target.value }))}
                />
                <div className="md:col-span-2">
                  <div className="mb-2 text-xs uppercase tracking-wide text-slate-500">Initial roles</div>
                  <div className="flex flex-wrap gap-2">
                    {roles.map((role) => (
                      <label key={role.id} className="inline-flex items-center gap-2 rounded-full border border-slate-700 px-3 py-1 text-xs text-slate-200">
                        <input
                          type="checkbox"
                          checked={userForm.roleSlugs.includes(role.slug)}
                          onChange={() => setUserForm((current) => ({
                            ...current,
                            roleSlugs: toggleListValue(current.roleSlugs, role.slug),
                          }))}
                        />
                        {role.slug}
                      </label>
                    ))}
                  </div>
                </div>
                <div className="md:col-span-4 flex justify-end">
                  <button
                    type="submit"
                    disabled={creatingUser || !userForm.email.trim()}
                    className="rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-4 py-2 text-sm font-medium text-emerald-200 hover:bg-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {creatingUser ? 'Creating…' : `Create user in ${selectedOrg.name}`}
                  </button>
                </div>
              </form>
            ) : null}

            <div className="space-y-4">
              {users.length === 0 ? (
                <div className="rounded-xl border border-dashed border-slate-800 px-4 py-6 text-sm text-slate-400">
                  No users are visible in this org.
                </div>
              ) : users.map((user) => {
                const membership = selectedOrg ? membershipForOrg(user, selectedOrg.id) : null;
                if (!membership) {
                  return null;
                }
                const roleDraft = membershipRoleDrafts[membership.id] || membership.roles.map((role) => role.slug);
                const toolDraft = membershipToolDrafts[membership.id] || toolDraftMap(membership.toolPolicies);
                return (
                  <div key={user.id} className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
                    <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                      <div>
                        <div className="text-sm font-semibold text-slate-100">{user.displayName}</div>
                        <div className="text-sm text-slate-400">{user.email}</div>
                        <div className="mt-2 flex flex-wrap gap-2">
                          <Badge tone={membership.status === 'active' ? 'success' : 'danger'}>{membership.status}</Badge>
                          {membership.isDefault ? <Badge tone="info">default membership</Badge> : null}
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        {membership.roles.map((role) => (
                          <Badge key={role.id}>{role.slug}</Badge>
                        ))}
                        {membership.toolPolicies.map((policy) => (
                          <Badge key={`${membership.id}-${policy.toolId}`} tone={policy.effect === 'deny' ? 'danger' : 'warn'}>
                            {policy.toolId}:{policy.effect}
                          </Badge>
                        ))}
                      </div>
                    </div>

                    <div className="mt-4 grid gap-4 xl:grid-cols-2">
                      <div className="rounded-xl border border-slate-800 bg-slate-950/70 p-4">
                        <div className="mb-3 text-sm font-semibold text-slate-100">Membership roles</div>
                        <div className="flex flex-wrap gap-2">
                          {roles.map((role) => (
                            <label key={`${membership.id}-${role.id}`} className="inline-flex items-center gap-2 rounded-full border border-slate-700 px-3 py-1 text-xs text-slate-200">
                              <input
                                type="checkbox"
                                checked={roleDraft.includes(role.slug)}
                                onChange={() => setMembershipRoleDrafts((current) => ({
                                  ...current,
                                  [membership.id]: toggleListValue(current[membership.id] || [], role.slug),
                                }))}
                              />
                              {role.slug}
                            </label>
                          ))}
                        </div>
                        {canUpdateMemberships ? (
                          <button
                            type="button"
                            onClick={() => void handleSaveMembershipRoles(membership.id)}
                            disabled={savingMembershipId === membership.id}
                            className="mt-4 rounded-lg border border-cyan-500/30 bg-cyan-500/10 px-4 py-2 text-sm font-medium text-cyan-200 hover:bg-cyan-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            {savingMembershipId === membership.id ? 'Saving…' : 'Save membership roles'}
                          </button>
                        ) : null}
                      </div>

                      <div className="rounded-xl border border-slate-800 bg-slate-950/70 p-4">
                        <div className="mb-3 text-sm font-semibold text-slate-100">Membership tool overrides</div>
                        <div className="grid gap-3 sm:grid-cols-2">
                          {tools.map((tool) => (
                            <label key={`${membership.id}-tool-${tool.id}`} className="flex flex-col gap-2 text-xs text-slate-300">
                              <span className="font-medium text-slate-200">{tool.id}</span>
                              <select
                                className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                                value={toolDraft[tool.id] || 'inherit'}
                                onChange={(event) => setMembershipToolDrafts((current) => ({
                                  ...current,
                                  [membership.id]: {
                                    ...(current[membership.id] || {}),
                                    [tool.id]: event.target.value as ToolDraftEffect,
                                  },
                                }))}
                              >
                                <option value="inherit">inherit</option>
                                <option value="allow">allow</option>
                                <option value="deny">deny</option>
                              </select>
                            </label>
                          ))}
                        </div>
                        {canUpdateUserPolicies ? (
                          <button
                            type="button"
                            onClick={() => void handleSaveMembershipPolicies(membership.id)}
                            disabled={savingMembershipId === membership.id}
                            className="mt-4 rounded-lg border border-amber-500/30 bg-amber-500/10 px-4 py-2 text-sm font-medium text-amber-200 hover:bg-amber-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                          >
                            {savingMembershipId === membership.id ? 'Saving…' : 'Save tool overrides'}
                          </button>
                        ) : null}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </SectionCard>

          <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
            <SectionCard
              title="Roles"
              description="Seeded and custom org roles. Create roles from permission atoms, then refine tool policies."
            >
              {selectedOrg && canCreateRoles ? (
                <form className="mb-5 space-y-4 rounded-xl border border-slate-800 bg-slate-900/80 p-4" onSubmit={handleCreateRole}>
                  <div className="grid gap-3 md:grid-cols-2">
                    <input
                      className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                      placeholder="Role name"
                      value={roleForm.name}
                      onChange={(event) => setRoleForm((current) => ({ ...current, name: event.target.value }))}
                    />
                    <input
                      className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                      placeholder="role-slug"
                      value={roleForm.slug}
                      onChange={(event) => setRoleForm((current) => ({ ...current, slug: event.target.value }))}
                    />
                  </div>

                  <div className="grid gap-4 xl:grid-cols-2">
                    <div>
                      <div className="mb-2 text-xs uppercase tracking-wide text-slate-500">Permission atoms</div>
                      <div className="max-h-72 space-y-3 overflow-auto rounded-xl border border-slate-800 bg-slate-950/70 p-3">
                        {permissionGroups.map(([kind, items]) => (
                          <div key={kind}>
                            <div className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">{kind}</div>
                            <div className="space-y-2">
                              {items.map((permission) => (
                                <label key={permission.id} className="flex items-start gap-2 text-xs text-slate-300">
                                  <input
                                    type="checkbox"
                                    checked={roleForm.permissionCodes.includes(permission.code)}
                                    onChange={() => setRoleForm((current) => ({
                                      ...current,
                                      permissionCodes: toggleListValue(current.permissionCodes, permission.code),
                                    }))}
                                  />
                                  <span>
                                    <span className="block font-medium text-slate-200">{permission.code}</span>
                                    <span className="text-slate-500">{permission.description}</span>
                                  </span>
                                </label>
                              ))}
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>

                    <div>
                      <div className="mb-2 text-xs uppercase tracking-wide text-slate-500">Allowed tools</div>
                      <div className="space-y-2 rounded-xl border border-slate-800 bg-slate-950/70 p-3">
                        {tools.map((tool) => (
                          <label key={`new-role-tool-${tool.id}`} className="flex items-start gap-2 text-xs text-slate-300">
                            <input
                              type="checkbox"
                              checked={roleForm.toolIds.includes(tool.id)}
                              onChange={() => setRoleForm((current) => ({
                                ...current,
                                toolIds: toggleListValue(current.toolIds, tool.id),
                              }))}
                            />
                            <span>
                              <span className="block font-medium text-slate-200">{tool.id}</span>
                              <span className="text-slate-500">{tool.description}</span>
                            </span>
                          </label>
                        ))}
                      </div>
                    </div>
                  </div>

                  <div className="flex justify-end">
                    <button
                      type="submit"
                      disabled={creatingRole || !roleForm.name.trim()}
                      className="rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-4 py-2 text-sm font-medium text-emerald-200 hover:bg-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {creatingRole ? 'Creating…' : `Create role in ${selectedOrg.name}`}
                    </button>
                  </div>
                </form>
              ) : null}

              <div className="space-y-4">
                {roles.length === 0 ? (
                  <div className="rounded-xl border border-dashed border-slate-800 px-4 py-6 text-sm text-slate-400">No roles available in this org.</div>
                ) : roles.map((role) => {
                  const toolDraft = roleToolDrafts[role.id] || toolDraftMap(role.toolPolicies);
                  return (
                    <div key={role.id} className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
                      <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                        <div>
                          <div className="text-sm font-semibold text-slate-100">{role.name}</div>
                          <div className="text-sm text-slate-400">{role.slug}</div>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          {role.builtIn ? <Badge tone="info">built-in</Badge> : <Badge tone="success">custom</Badge>}
                          {role.systemManaged ? <Badge tone="warn">system-managed</Badge> : null}
                          <Badge>{role.permissions.length} perms</Badge>
                        </div>
                      </div>

                      <div className="mt-3 flex flex-wrap gap-2">
                        {role.toolPolicies.map((policy) => (
                          <Badge key={`${role.id}-${policy.toolId}`} tone={policy.effect === 'deny' ? 'danger' : 'warn'}>
                            {policy.toolId}:{policy.effect}
                          </Badge>
                        ))}
                      </div>

                      <details className="mt-4 rounded-xl border border-slate-800 bg-slate-950/70 p-4">
                        <summary className="cursor-pointer text-sm font-medium text-slate-200">Inspect permissions and edit tool policy</summary>
                        <div className="mt-4 grid gap-4 xl:grid-cols-2">
                          <div>
                            <div className="mb-2 text-xs uppercase tracking-wide text-slate-500">Permissions</div>
                            <div className="max-h-56 space-y-2 overflow-auto rounded-lg border border-slate-800 bg-slate-900/70 p-3 text-xs text-slate-300">
                              {role.permissions.map((permission) => (
                                <div key={`${role.id}-perm-${permission}`}>{permission}</div>
                              ))}
                            </div>
                          </div>
                          <div>
                            <div className="mb-2 text-xs uppercase tracking-wide text-slate-500">Tool policy</div>
                            <div className="grid gap-3 sm:grid-cols-2">
                              {tools.map((tool) => (
                                <label key={`${role.id}-tool-${tool.id}`} className="flex flex-col gap-2 text-xs text-slate-300">
                                  <span className="font-medium text-slate-200">{tool.id}</span>
                                  <select
                                    className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                                    value={toolDraft[tool.id] || 'inherit'}
                                    onChange={(event) => setRoleToolDrafts((current) => ({
                                      ...current,
                                      [role.id]: {
                                        ...(current[role.id] || {}),
                                        [tool.id]: event.target.value as ToolDraftEffect,
                                      },
                                    }))}
                                  >
                                    <option value="inherit">inherit</option>
                                    <option value="allow">allow</option>
                                    <option value="deny">deny</option>
                                  </select>
                                </label>
                              ))}
                            </div>
                            {canUpdateRolePolicies ? (
                              <button
                                type="button"
                                onClick={() => void handleSaveRolePolicies(role.id)}
                                disabled={savingRoleId === role.id}
                                className="mt-4 rounded-lg border border-cyan-500/30 bg-cyan-500/10 px-4 py-2 text-sm font-medium text-cyan-200 hover:bg-cyan-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                              >
                                {savingRoleId === role.id ? 'Saving…' : 'Save role tool policy'}
                              </button>
                            ) : null}
                          </div>
                        </div>
                      </details>
                    </div>
                  );
                })}
              </div>
            </SectionCard>

            <SectionCard
              title="Org data lakes"
              description="Control-plane owned data-lake inventory for the selected org."
            >
              {selectedOrg && canCreateDataLakes ? (
                <form className="mb-5 space-y-3 rounded-xl border border-slate-800 bg-slate-900/80 p-4" onSubmit={handleCreateLake}>
                  <div className="grid gap-3 md:grid-cols-2">
                    <input
                      className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                      placeholder="Lake name"
                      value={lakeForm.name}
                      onChange={(event) => setLakeForm((current) => ({ ...current, name: event.target.value }))}
                    />
                    <input
                      className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                      placeholder="lake-slug"
                      value={lakeForm.slug}
                      onChange={(event) => setLakeForm((current) => ({ ...current, slug: event.target.value }))}
                    />
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <select
                      className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                      value={lakeForm.kind}
                      onChange={(event) => setLakeForm((current) => ({ ...current, kind: event.target.value }))}
                    >
                      {DATA_LAKE_KIND_OPTIONS.map((kind) => (
                        <option key={kind} value={kind}>{kind}</option>
                      ))}
                    </select>
                    <input
                      className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                      placeholder="workspace root"
                      value={lakeForm.workspaceRoot}
                      onChange={(event) => setLakeForm((current) => ({ ...current, workspaceRoot: event.target.value }))}
                    />
                  </div>
                  <div className="flex justify-end">
                    <button
                      type="submit"
                      disabled={creatingLake || !lakeForm.name.trim()}
                      className="rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-4 py-2 text-sm font-medium text-emerald-200 hover:bg-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {creatingLake ? 'Creating…' : 'Create data lake'}
                    </button>
                  </div>
                </form>
              ) : null}

              <div className="space-y-4">
                {dataLakes.length === 0 ? (
                  <div className="rounded-xl border border-dashed border-slate-800 px-4 py-6 text-sm text-slate-400">No org data lakes registered yet.</div>
                ) : dataLakes.map((lake) => (
                  <div key={lake.id} className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
                    <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                      <div>
                        <div className="text-sm font-semibold text-slate-100">{lake.name}</div>
                        <div className="text-sm text-slate-400">{lake.slug}</div>
                      </div>
                      <div className="flex flex-wrap gap-2">
                        <Badge>{lake.kind}</Badge>
                        <Badge tone={lake.status === 'active' ? 'success' : 'danger'}>{lake.status}</Badge>
                      </div>
                    </div>
                    <pre className="mt-3 overflow-auto rounded-lg border border-slate-800 bg-slate-950/70 p-3 text-xs text-slate-300">
{JSON.stringify(lake.config, null, 2)}
                    </pre>
                  </div>
                ))}
              </div>
            </SectionCard>
          </div>

          <SectionCard
            title="Permission and tool catalog"
            description="Use this as the live reference when composing custom roles or membership overrides."
          >
            <div className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
              <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
                <div className="mb-3 text-sm font-semibold text-slate-100">Permission atoms</div>
                <div className="max-h-[28rem] space-y-4 overflow-auto pr-1">
                  {permissionGroups.map(([kind, items]) => (
                    <div key={`catalog-${kind}`}>
                      <div className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">{kind}</div>
                      <div className="space-y-2">
                        {items.map((permission) => (
                          <div key={`catalog-${permission.id}`} className="rounded-lg border border-slate-800 bg-slate-950/70 p-3">
                            <div className="text-sm font-medium text-slate-100">{permission.code}</div>
                            <div className="mt-1 text-xs text-slate-500">{permission.description}</div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
              <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
                <div className="mb-3 text-sm font-semibold text-slate-100">Tool definitions</div>
                <div className="space-y-3">
                  {tools.map((tool) => (
                    <div key={`tool-${tool.id}`} className="rounded-lg border border-slate-800 bg-slate-950/70 p-3">
                      <div className="flex items-start justify-between gap-3">
                        <div>
                          <div className="text-sm font-medium text-slate-100">{tool.id}</div>
                          <div className="mt-1 text-xs text-slate-500">{tool.description}</div>
                        </div>
                        <Badge tone={tool.riskLevel === 'high' ? 'danger' : tool.riskLevel === 'medium' ? 'warn' : 'success'}>
                          {tool.riskLevel}
                        </Badge>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </SectionCard>
        </>
      )}
    </div>
  );
}
