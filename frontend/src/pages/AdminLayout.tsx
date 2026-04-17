import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Routes, Route, NavLink, Navigate, useLocation } from 'react-router-dom';
import { opsRoutes } from '../lib/app-routes';
import { errorMessage, groupPermissions, hydrateMembershipDrafts, hydrateRoleDrafts } from '../components/admin-page/helpers';
import type { Notice, OrgFormState, RoleFormState, ToolDraftEffect, UserFormState, LakeFormState } from '../components/admin-page/types';
import {
  getAdminBootstrap,
  getKnoxxAuthContext,
  getKnoxxAuthIdentity,
  listAdminPermissions,
  listAdminTools,
  listAdminOrgs,
  setKnoxxAuthIdentity,
  createAdminOrg,
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

import { IdentitySection } from '../components/admin-page/IdentitySection';
import { SummarySection } from '../components/admin-page/SummarySection';
import { OrganizationsSection } from '../components/admin-page/OrganizationsSection';
import { SelectedOrgSection } from '../components/admin-page/SelectedOrgSection';
import { UsersMembershipsSection } from '../components/admin-page/UsersMembershipsSection';
import { RolesSection } from '../components/admin-page/RolesSection';
import { DataLakesSection } from '../components/admin-page/DataLakesSection';
import { DiscordSection } from '../components/admin-page/DiscordSection';
import { ProxxObservabilitySection } from '../components/admin-page/ProxxObservabilitySection';
import { CatalogSection } from '../components/admin-page/CatalogSection';
import { Badge } from '../components/admin-page/common';

const ORG_KIND_OPTIONS = ['platform_owner', 'customer', 'internal', 'partner'];
const DATA_LAKE_KIND_OPTIONS = ['workspace_docs', 'analytics', 'notes', 'uploads'];

// ── Tab definitions ──────────────────────────────────────────────────────────

const ADMIN_TABS = [
  { label: 'Overview', path: 'overview', icon: '◈' },
  { label: 'Orgs', path: 'orgs', icon: '◉' },
  { label: 'Users', path: 'users', icon: '◈' },
  { label: 'Roles', path: 'roles', icon: '◎' },
  { label: 'Lakes', path: 'lakes', icon: '▣' },
  { label: 'Integrations', path: 'integrations', icon: '⚡' },
  { label: 'Catalog', path: 'catalog', icon: '☰' },
] as const;

// ── Shared admin context hook ───────────────────────────────────────────────

interface AdminCtx {
  identityForm: KnoxxAuthIdentity;
  setIdentityForm: React.Dispatch<React.SetStateAction<KnoxxAuthIdentity>>;
  context: KnoxxAuthContext | null;
  bootstrap: any;
  permissions: AdminPermissionDefinition[];
  tools: AdminToolDefinition[];
  orgs: AdminOrgSummary[];
  selectedOrgId: string;
  setSelectedOrgId: React.Dispatch<React.SetStateAction<string>>;
  selectedOrg: AdminOrgSummary | null;
  roles: AdminRoleSummary[];
  users: AdminUserSummary[];
  dataLakes: AdminDataLakeSummary[];
  permissionGroups: Array<[string, AdminPermissionDefinition[]]>;
  membershipRoleDrafts: Record<string, string[]>;
  setMembershipRoleDrafts: React.Dispatch<React.SetStateAction<Record<string, string[]>>>;
  membershipToolDrafts: Record<string, Record<string, ToolDraftEffect>>;
  setMembershipToolDrafts: React.Dispatch<React.SetStateAction<Record<string, Record<string, ToolDraftEffect>>>>;
  roleToolDrafts: Record<string, Record<string, ToolDraftEffect>>;
  setRoleToolDrafts: React.Dispatch<React.SetStateAction<Record<string, Record<string, ToolDraftEffect>>>>;
  loading: boolean;
  notice: Notice | null;
  error: string;
  setNotice: (n: Notice | null) => void;
  setError: (e: string) => void;
  refresh: () => void;
  hasPermission: (p: string) => boolean;
  // Form state
  orgForm: OrgFormState; setOrgForm: React.Dispatch<React.SetStateAction<OrgFormState>>;
  userForm: UserFormState; setUserForm: React.Dispatch<React.SetStateAction<UserFormState>>;
  roleForm: RoleFormState; setRoleForm: React.Dispatch<React.SetStateAction<RoleFormState>>;
  lakeForm: LakeFormState; setLakeForm: React.Dispatch<React.SetStateAction<LakeFormState>>;
  // Mutation flags
  creatingOrg: boolean; setCreatingOrg: React.Dispatch<React.SetStateAction<boolean>>;
  creatingUser: boolean; setCreatingUser: React.Dispatch<React.SetStateAction<boolean>>;
  creatingRole: boolean; setCreatingRole: React.Dispatch<React.SetStateAction<boolean>>;
  creatingLake: boolean; setCreatingLake: React.Dispatch<React.SetStateAction<boolean>>;
  savingMembershipId: string | null; setSavingMembershipId: React.Dispatch<React.SetStateAction<string | null>>;
  savingRoleId: string | null; setSavingRoleId: React.Dispatch<React.SetStateAction<string | null>>;
}

function useAdminContext(): AdminCtx {
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

  const loadAdminSurface = useCallback(async () => {
    setLoading(true); setError(''); setNotice(null);
    try {
      const ctx = await getKnoxxAuthContext();
      setContext(ctx);
      setIdentityForm({ userEmail: ctx.user.email, orgSlug: ctx.org.slug });

      const [permissionsResult, toolsResult, bootstrapResult, orgsResult] = await Promise.allSettled([
        listAdminPermissions(), listAdminTools(), getAdminBootstrap(), listAdminOrgs(),
      ]);
      setPermissions(permissionsResult.status === 'fulfilled' ? permissionsResult.value.permissions : []);
      setTools(toolsResult.status === 'fulfilled' ? toolsResult.value.tools : []);
      setBootstrap(bootstrapResult.status === 'fulfilled' ? bootstrapResult.value : null);

      const scopedOrgs = orgsResult.status === 'fulfilled' && orgsResult.value.orgs.length > 0
        ? orgsResult.value.orgs
        : [{ id: ctx.org.id, slug: ctx.org.slug, name: ctx.org.name, kind: ctx.org.kind || 'customer', isPrimary: Boolean(context?.org.isPrimary), status: ctx.org.status }];
      setOrgs(scopedOrgs);
      setSelectedOrgId((current) =>
        current && scopedOrgs.some((o) => o.id === current) ? current
          : scopedOrgs.find((o) => o.id === ctx.org.id)?.id || scopedOrgs[0]?.id || ''
      );
    } catch (e) {
      setError(errorMessage(e));
      setContext(null); setPermissions([]); setTools([]); setBootstrap(null); setOrgs([]); setSelectedOrgId('');
    } finally { setLoading(false); }
  }, []);

  // Load org-scoped resources when selectedOrgId changes
  useEffect(() => {
    if (!selectedOrgId) return;
    let cancelled = false;
    (async () => {
      try {
        const [roleRes, userRes, lakeRes] = await Promise.allSettled([
          import('../lib/nextApi').then(m => m.listOrgRoles(selectedOrgId)).catch(() => ({ roles: [] })),
          import('../lib/nextApi').then(m => m.listOrgUsers(selectedOrgId)).catch(() => ({ users: [] })),
          import('../lib/nextApi').then(m => m.listOrgDataLakes(selectedOrgId)).catch(() => ({ dataLakes: [] })),
        ]);
        if (cancelled) return;
        if (roleRes.status === 'fulfilled') { setRoles(roleRes.value.roles); setRoleToolDrafts(hydrateRoleDrafts(roleRes.value.roles)); }
        if (userRes.status === 'fulfilled') { setUsers(userRes.value.users); const d = hydrateMembershipDrafts(userRes.value.users, selectedOrgId); setMembershipRoleDrafts(d.roleDrafts); setMembershipToolDrafts(d.toolDrafts); }
        if (lakeRes.status === 'fulfilled') setDataLakes(lakeRes.value.dataLakes);
      } catch {}
    })();
    return () => { cancelled = true; };
  }, [selectedOrgId]);

  useEffect(() => { void loadAdminSurface(); }, [loadAdminSurface]);

  useEffect(() => {
    if (selectedOrg) setLakeForm((c) => ({ ...c, workspaceRoot: c.workspaceRoot || `orgs/${selectedOrg.slug}` }));
  }, [selectedOrg]);

  return {
    identityForm, setIdentityForm, context, bootstrap, permissions, tools, orgs, selectedOrgId, setSelectedOrgId,
    selectedOrg, roles, users, dataLakes, permissionGroups,
    membershipRoleDrafts, setMembershipRoleDrafts, membershipToolDrafts, setMembershipToolDrafts,
    roleToolDrafts, setRoleToolDrafts, loading, notice, setNotice, error, setError, refresh: loadAdminSurface, hasPermission,
    orgForm, setOrgForm, userForm, setUserForm, roleForm, setRoleForm, lakeForm, setLakeForm,
    creatingOrg, setCreatingOrg, creatingUser, setCreatingUser, creatingRole, setCreatingRole, creatingLake, setCreatingLake,
    savingMembershipId, setSavingMembershipId, savingRoleId, setSavingRoleId,
  };
}

// ── Sub-page components (compact) ───────────────────────────────────────────

function AdminOverviewPage({ ctx }: { ctx: AdminCtx }) {
  const handleApplyIdentity = async (e: React.FormEvent) => {
    e.preventDefault();
    const resolved = setKnoxxAuthIdentity(ctx.identityForm);
    ctx.setIdentityForm(resolved);
    ctx.setNotice({ tone: 'success', text: `Switched to ${resolved.userEmail} in ${resolved.orgSlug}.` });
    await ctx.refresh();
  };

  return (
    <div className="space-y-4">
      <div className="grid gap-4 lg:grid-cols-2">
        <IdentitySection identityForm={ctx.identityForm} setIdentityForm={ctx.setIdentityForm} context={ctx.context} onApplyIdentity={handleApplyIdentity} />
        <SummarySection orgs={ctx.orgs} toolsCount={ctx.tools.length} permissionsCount={ctx.permissions.length} bootstrap={ctx.bootstrap} />
      </div>
    </div>
  );
}

function AdminOrgsPage({ ctx }: { ctx: AdminCtx }) {
  const handleCreateOrg = async (e: React.FormEvent) => {
    e.preventDefault(); if (!ctx.hasPermission('platform.org.create')) return;
    ctx.setCreatingOrg(true); ctx.setNotice(null);
    try {
      const r = await createAdminOrg({ name: ctx.orgForm.name.trim(), slug: ctx.orgForm.slug.trim() || undefined, kind: ctx.orgForm.kind });
      ctx.setOrgForm({ name: '', slug: '', kind: 'customer' });
      ctx.setNotice({ tone: 'success', text: `Created ${r.org.name}.` }); await ctx.refresh(); ctx.setSelectedOrgId(r.org.id);
    } catch (e) { ctx.setNotice({ tone: 'error', text: errorMessage(e) }); } finally { ctx.setCreatingOrg(false); }
  };

  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_1fr]">
      <OrganizationsSection context={ctx.context} orgs={ctx.orgs} selectedOrgId={ctx.selectedOrgId} setSelectedOrgId={ctx.setSelectedOrgId}
        canCreateOrgs={ctx.hasPermission('platform.org.create')} orgForm={ctx.orgForm} setOrgForm={ctx.setOrgForm}
        creatingOrg={ctx.creatingOrg} onCreateOrg={handleCreateOrg} orgKindOptions={ORG_KIND_OPTIONS} />
      <SelectedOrgSection selectedOrg={ctx.selectedOrg} context={ctx.context} />
    </div>
  );
}

function AdminUsersPage({ ctx }: { ctx: AdminCtx }) {
  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault(); if (!ctx.selectedOrgId) return;
    ctx.setCreatingUser(true); ctx.setNotice(null);
    try {
      await (await import('../lib/nextApi')).createOrgUser(ctx.selectedOrgId, { email: ctx.userForm.email.trim(), displayName: ctx.userForm.displayName.trim() || ctx.userForm.email.trim(), roleSlugs: ctx.userForm.roleSlugs.length > 0 ? ctx.userForm.roleSlugs : ['knowledge_worker'] });
      ctx.setUserForm({ email: '', displayName: '', roleSlugs: ['knowledge_worker'] });
      ctx.setNotice({ tone: 'success', text: 'User created.' }); await ctx.refresh();
    } catch (e) { ctx.setNotice({ tone: 'error', text: errorMessage(e) }); } finally { ctx.setCreatingUser(false); }
  };
  const saveMemberRoles = async (id: string) => {
    ctx.setSavingMembershipId(id); ctx.setNotice(null);
    try { await (await import('../lib/nextApi')).updateMembershipRoles(id, ctx.membershipRoleDrafts[id] || []); ctx.setNotice({ tone: 'success', text: 'Roles updated.' }); await ctx.refresh(); }
    catch (e) { ctx.setNotice({ tone: 'error', text: errorMessage(e) }); } finally { ctx.setSavingMembershipId(null); }
  };
  const saveMemberPolicies = async (id: string) => {
    ctx.setSavingMembershipId(id); ctx.setNotice(null);
    try { await (await import('../lib/nextApi')).updateMembershipToolPolicies(id, (await import('../components/admin-page/helpers')).toolPoliciesFromDraft(ctx.membershipToolDrafts[id] || {})); ctx.setNotice({ tone: 'success', text: 'Policies updated.' }); await ctx.refresh(); }
    catch (e) { ctx.setNotice({ tone: 'error', text: errorMessage(e) }); } finally { ctx.setSavingMembershipId(null); }
  };

  return (
    <UsersMembershipsSection
      selectedOrgId={ctx.selectedOrgId} selectedOrgName={ctx.selectedOrg?.name || ''}
      canCreateUsers={Boolean(ctx.selectedOrg && ctx.hasPermission('org.users.create'))}
      canUpdateMemberships={ctx.hasPermission('org.members.update')}
      canUpdateUserPolicies={ctx.hasPermission('org.user_policy.update')}
      users={ctx.users} roles={ctx.roles} tools={ctx.tools}
      userForm={ctx.userForm} setUserForm={ctx.setUserForm}
      membershipRoleDrafts={ctx.membershipRoleDrafts} setMembershipRoleDrafts={ctx.setMembershipRoleDrafts}
      membershipToolDrafts={ctx.membershipToolDrafts} setMembershipToolDrafts={ctx.setMembershipToolDrafts}
      creatingUser={ctx.creatingUser} savingMembershipId={ctx.savingMembershipId}
      onCreateUser={handleCreateUser} onSaveMembershipRoles={saveMemberRoles} onSaveMembershipPolicies={saveMemberPolicies}
    />
  );
}

function AdminRolesPage({ ctx }: { ctx: AdminCtx }) {
  const handleCreateRole = async (e: React.FormEvent) => {
    e.preventDefault(); if (!ctx.selectedOrgId) return;
    ctx.setCreatingRole(true); ctx.setNotice(null);
    try {
      await (await import('../lib/nextApi')).createOrgRole(ctx.selectedOrgId, { name: ctx.roleForm.name.trim(), slug: ctx.roleForm.slug.trim() || undefined, permissionCodes: ctx.roleForm.permissionCodes, toolPolicies: ctx.roleForm.toolIds.map((t) => ({ toolId: t, effect: 'allow' })) });
      ctx.setRoleForm({ name: '', slug: '', permissionCodes: [], toolIds: ['read', 'canvas'] });
      ctx.setNotice({ tone: 'success', text: 'Role created.' }); await ctx.refresh();
    } catch (e) { ctx.setNotice({ tone: 'error', text: errorMessage(e) }); } finally { ctx.setCreatingRole(false); }
  };
  const saveRolePolicies = async (id: string) => {
    ctx.setSavingRoleId(id); ctx.setNotice(null);
    try { await (await import('../lib/nextApi')).updateRoleToolPolicies(id, (await import('../components/admin-page/helpers')).toolPoliciesFromDraft(ctx.roleToolDrafts[id] || {})); ctx.setNotice({ tone: 'success', text: 'Policy updated.' }); await ctx.refresh(); }
    catch (e) { ctx.setNotice({ tone: 'error', text: errorMessage(e) }); } finally { ctx.setSavingRoleId(null); }
  };

  return (
    <RolesSection
      selectedOrgId={ctx.selectedOrgId} selectedOrgName={ctx.selectedOrg?.name || ''}
      canCreateRoles={Boolean(ctx.selectedOrg && ctx.hasPermission('org.roles.create'))}
      canUpdateRolePolicies={ctx.hasPermission('org.tool_policy.update')}
      roles={ctx.roles} tools={ctx.tools} permissionGroups={ctx.permissionGroups}
      roleForm={ctx.roleForm} setRoleForm={ctx.setRoleForm}
      roleToolDrafts={ctx.roleToolDrafts} setRoleToolDrafts={ctx.setRoleToolDrafts}
      creatingRole={ctx.creatingRole} savingRoleId={ctx.savingRoleId}
      onCreateRole={handleCreateRole} onSaveRolePolicies={saveRolePolicies}
    />
  );
}

function AdminLakesPage({ ctx }: { ctx: AdminCtx }) {
  const handleCreateLake = async (e: React.FormEvent) => {
    e.preventDefault(); if (!ctx.selectedOrgId) return;
    ctx.setCreatingLake(true); ctx.setNotice(null);
    try {
      await (await import('../lib/nextApi')).createOrgDataLake(ctx.selectedOrgId, { name: ctx.lakeForm.name.trim(), slug: ctx.lakeForm.slug.trim() || undefined, kind: ctx.lakeForm.kind, config: ctx.lakeForm.workspaceRoot.trim() ? { workspaceRoot: ctx.lakeForm.workspaceRoot.trim() } : {} });
      ctx.setLakeForm({ name: '', slug: '', kind: 'workspace_docs', workspaceRoot: ctx.selectedOrg ? `orgs/${ctx.selectedOrg.slug}` : '' });
      ctx.setNotice({ tone: 'success', text: 'Lake created.' }); await ctx.refresh();
    } catch (e) { ctx.setNotice({ tone: 'error', text: errorMessage(e) }); } finally { ctx.setCreatingLake(false); }
  };

  return (
    <DataLakesSection
      selectedOrgName={ctx.selectedOrg?.name || ''} canCreateDataLakes={Boolean(ctx.selectedOrg && ctx.hasPermission('org.datalakes.create'))}
      lakeForm={ctx.lakeForm} setLakeForm={ctx.setLakeForm} creatingLake={ctx.creatingLake}
      dataLakes={ctx.dataLakes} dataLakeKindOptions={DATA_LAKE_KIND_OPTIONS} onCreateLake={handleCreateLake}
    />
  );
}

function AdminIntegrationsPage({ ctx }: { ctx: AdminCtx }) {
  return (
    <div className="space-y-4">
      <DiscordSection canManage={ctx.hasPermission('platform.org.create')} tools={ctx.tools} />
      <ProxxObservabilitySection canView={ctx.hasPermission('org.proxx.observability.read')} />
    </div>
  );
}

function AdminCatalogPage({ ctx }: { ctx: AdminCtx }) {
  return <CatalogSection permissionGroups={ctx.permissionGroups} tools={ctx.tools} />;
}

// ── Main layout ─────────────────────────────────────────────────────────────

export default function AdminLayout() {
  const ctx = useAdminContext();
  const location = useLocation();

  if (ctx.loading) return <div className="p-6 text-sm text-slate-400">Loading admin…</div>;

  const looksLikeAdmin = Boolean(ctx.context?.isSystemAdmin || ctx.hasPermission('org.users.read') || ctx.hasPermission('org.roles.read'));

  return (
    <div className="admin-layout flex flex-1 flex-col overflow-hidden">
      {/* Compact header */}
      <div className="shrink-0 border-b border-slate-700/50 bg-slate-900/80 px-4 py-2">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <h1 className="text-base font-bold text-slate-100">Admin</h1>
            {ctx.context ? (
              <span className="text-xs text-slate-400">
                <Badge>{ctx.context.org.name}</Badge> · {ctx.context.user.email}
              </span>
            ) : null}
          </div>
          <button onClick={() => void ctx.refresh()} className="rounded-md border border-slate-700 bg-slate-800 px-3 py-1 text-xs text-slate-300 hover:bg-slate-700 transition">
            Refresh
          </button>
        </div>

        {/* Tab bar */}
        <nav className="mt-2 flex gap-1 overflow-x-auto">
          {ADMIN_TABS.map((tab) => (
            <NavLink
              key={tab.path}
              to={tab.path}
              end={tab.path === 'overview'}
              className={({ isActive }) =>
                `shrink-0 rounded-md px-3 py-1.5 text-xs font-medium transition ${
                  isActive
                    ? 'bg-blue-500/15 text-blue-300'
                    : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                }`
              }
            >
              <span className="mr-1">{tab.icon}</span>{tab.label}
            </NavLink>
          ))}
        </nav>
      </div>

      {/* Notices */}
      {(ctx.notice || ctx.error) ? (
        <div className="shrink-0 px-4 py-2">
          {ctx.notice ? (
            <div className={`rounded-lg px-3 py-2 text-xs ${ctx.notice.tone === 'success' ? 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20' : 'bg-rose-500/10 text-rose-300 border border-rose-500/20'}`}>
              {ctx.notice.text}
            </div>
          ) : null}
          {ctx.error ? (
            <div className="rounded-lg bg-rose-500/10 px-3 py-2 text-xs text-rose-300 border border-rose-500/20">{ctx.error}</div>
          ) : null}
        </div>
      ) : null}

      {/* Content area */}
      <main className="flex-1 overflow-y-auto p-4">
        {!looksLikeAdmin ? (
          <div className="rounded-xl border border-dashed border-slate-700 p-8 text-center text-sm text-slate-500">
            Admin access required. Switch actor in Overview.
          </div>
        ) : (
          <Routes>
            <Route index element={<AdminOverviewPage ctx={ctx} />} />
            <Route path="overview" element={<AdminOverviewPage ctx={ctx} />} />
            <Route path="orgs" element={<AdminOrgsPage ctx={ctx} />} />
            <Route path="users" element={<AdminUsersPage ctx={ctx} />} />
            <Route path="roles" element={<AdminRolesPage ctx={ctx} />} />
            <Route path="lakes" element={<AdminLakesPage ctx={ctx} />} />
            <Route path="integrations" element={<AdminIntegrationsPage ctx={ctx} />} />
            <Route path="catalog" element={<AdminCatalogPage ctx={ctx} />} />
            <Route path="*" element={<Navigate to={opsRoutes.admin} replace />} />
          </Routes>
        )}
      </main>
    </div>
  );
}
