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

import { AdminOverviewPage } from '../components/admin-page/SummarySection';
import { AdminOrgsPage } from '../components/admin-page/OrganizationsSection';
import { UsersMembershipsSection } from '../components/admin-page/UsersMembershipsSection';
import { AdminRolesPage } from '../components/admin-page/RolesSection';
import { AdminLakesPage } from '../components/admin-page/DataLakesSection';
import { ProxxObservabilitySection } from '../components/admin-page/ProxxObservabilitySection';
import { CatalogSection } from '../components/admin-page/CatalogSection';
import { Badge } from '../components/admin-page/common';


// ── Tab definitions ──────────────────────────────────────────────────────────

const ADMIN_TABS = [
  { label: 'Overview', path: 'overview', icon: '◈' },
  { label: 'Orgs', path: 'orgs', icon: '◉' },
  { label: 'Actors', path: 'actors', icon: '◈' },
  { label: 'Roles', path: 'roles', icon: '◎' },
  { label: 'Lakes', path: 'lakes', icon: '▣' },
  { label: 'Integrations', path: 'integrations', icon: '⚡' },
  { label: 'Catalog', path: 'catalog', icon: '☰' },
] as const;

// ── Shared admin context hook ───────────────────────────────────────────────

export interface AdminCtx {
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
  const [orgDataVersion, setOrgDataVersion] = useState(0);
  const [notice, setNotice] = useState<Notice>(null);
  const [error, setError] = useState('');
  const [creatingOrg, setCreatingOrg] = useState(false);
  const [creatingUser, setCreatingUser] = useState(false);
  const [creatingRole, setCreatingRole] = useState(false);
  const [creatingLake, setCreatingLake] = useState(false);
  const [savingMembershipId, setSavingMembershipId] = useState<string | null>(null);
  const [savingRoleId, setSavingRoleId] = useState<string | null>(null);

  const [orgForm, setOrgForm] = useState<OrgFormState>({ name: '', slug: '', kind: 'customer' });
  const [userForm, setUserForm] = useState<UserFormState>({ actorId: '', axxiumPrincipalId: '', email: '', displayName: '', roleSlugs: ['basic-user'] });
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
    } finally { setLoading(false); setOrgDataVersion((current) => current + 1); }
  }, []);

  // Load org-scoped resources when selectedOrgId changes
  useEffect(() => {
    if (!selectedOrgId) return;
    let cancelled = false;
    (async () => {
      try {
        const [roleRes, userRes, lakeRes] = await Promise.allSettled([
          import('../lib/nextApi').then(m => m.listOrgRoles(selectedOrgId)).catch(() => ({ roles: [] })),
          import('../lib/nextApi').then(m => m.listOrgActors(selectedOrgId)).catch(() => ({ users: [] })),
          import('../lib/nextApi').then(m => m.listOrgDataLakes(selectedOrgId)).catch(() => ({ dataLakes: [] })),
        ]);
        if (cancelled) return;
        if (roleRes.status === 'fulfilled') { setRoles(roleRes.value.roles); setRoleToolDrafts(hydrateRoleDrafts(roleRes.value.roles)); }
        if (userRes.status === 'fulfilled') { setUsers(userRes.value.users); const d = hydrateMembershipDrafts(userRes.value.users, selectedOrgId); setMembershipRoleDrafts(d.roleDrafts); setMembershipToolDrafts(d.toolDrafts); }
        if (lakeRes.status === 'fulfilled') setDataLakes(lakeRes.value.dataLakes);
      } catch {}
    })();
    return () => { cancelled = true; };
  }, [selectedOrgId, orgDataVersion]);

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

function AdminActorsPage({ ctx }: { ctx: AdminCtx }) {
  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault(); if (!ctx.selectedOrgId) return;
    ctx.setCreatingUser(true); ctx.setNotice(null);
    try {
      const actorId = ctx.userForm.actorId.trim();
      const email = ctx.userForm.email.trim();
      const principalId = (ctx.userForm.axxiumPrincipalId || '').trim();
      const displayName = ctx.userForm.displayName.trim();
      const created = await (await import('../lib/nextApi')).createOrgActor(ctx.selectedOrgId, {
        ...(principalId ? { axxiumPrincipalId: principalId, ...(displayName ? { displayName } : {}) }
          : { actorId: actorId || undefined, email: email || undefined, displayName: displayName || actorId || email }),
        roleSlugs: ctx.userForm.roleSlugs.length > 0 ? ctx.userForm.roleSlugs : ['basic-user'],
      });
      ctx.setUserForm({ actorId: '', axxiumPrincipalId: '', email: '', displayName: '', roleSlugs: ['basic-user'] });
      await ctx.refresh();
      ctx.setNotice({ tone: 'success', text: created.user?.identityBound ? 'Actor bound to the verified Axxium principal.' : 'Actor created. Identity enrollment is required before this actor can sign in.' });
    } catch (e) { ctx.setNotice({ tone: 'error', text: errorMessage(e) }); } finally { ctx.setCreatingUser(false); }
  };
  const saveActorProfile = async (userId: string, draft: { actorId: string; displayName: string; email: string; status: string }) => {
    ctx.setNotice(null);
    try {
      const current = ctx.users.find(user => user.id === userId);
      await (await import('../lib/nextApi')).updateAdminActor(userId, {
        orgId: ctx.selectedOrgId, displayName: draft.displayName.trim(),
        ...(current?.identityBound ? {} : { actorId: draft.actorId.trim() }),
        ...(ctx.context?.isSystemAdmin && draft.status !== current?.status ? { status: draft.status } : {}),
      });
      await ctx.refresh(); ctx.setNotice({ tone: 'success', text: 'Actor profile updated.' });
    } catch (e) { ctx.setNotice({ tone: 'error', text: errorMessage(e) }); }
  };
  const saveActorCredential = async (userId: string, provider: string, draft: { kind: string; accountIdentifier: string; secretJson: Record<string, string> }) => {
    ctx.setNotice(null);
    try {
      await (await import('../lib/nextApi')).upsertAdminActorCredential(userId, provider, { orgId: ctx.selectedOrgId, kind: draft.kind, accountIdentifier: draft.accountIdentifier.trim() || undefined, secretJson: draft.secretJson });
      ctx.setNotice({ tone: 'success', text: 'Actor credential saved.' }); await ctx.refresh();
    } catch (e) { ctx.setNotice({ tone: 'error', text: errorMessage(e) }); }
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
      canUpdateGlobalStatus={Boolean(ctx.context?.isSystemAdmin)}
      canUpdateUserPolicies={ctx.hasPermission('org.user_policy.update')}
      users={ctx.users} roles={ctx.roles} tools={ctx.tools}
      userForm={ctx.userForm} setUserForm={ctx.setUserForm}
      membershipRoleDrafts={ctx.membershipRoleDrafts} setMembershipRoleDrafts={ctx.setMembershipRoleDrafts}
      membershipToolDrafts={ctx.membershipToolDrafts} setMembershipToolDrafts={ctx.setMembershipToolDrafts}
      creatingUser={ctx.creatingUser} savingMembershipId={ctx.savingMembershipId}
      onCreateUser={handleCreateUser} onSaveActorProfile={saveActorProfile} onSaveActorCredential={saveActorCredential}
      onSaveMembershipRoles={saveMemberRoles} onSaveMembershipPolicies={saveMemberPolicies}
    />
  );
}

function AdminIntegrationsPage({ ctx }: { ctx: AdminCtx }) {
  return (
    <div className="space-y-4">
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
              to={`${opsRoutes.admin}/${tab.path}`}
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
            <Route path="actors" element={<AdminActorsPage ctx={ctx} />} />
            <Route path="users" element={<Navigate to="../actors" replace />} />
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
