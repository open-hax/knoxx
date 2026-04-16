import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { CatalogSection } from '../components/admin-page/CatalogSection';
import { Badge, SectionCard, classNames } from '../components/admin-page/common';
import { DataLakesSection } from '../components/admin-page/DataLakesSection';
import {
  errorMessage,
  groupPermissions,
  hydrateMembershipDrafts,
  hydrateRoleDrafts,
  membershipForOrg,
  toggleListValue,
  toolPoliciesFromDraft,
} from '../components/admin-page/helpers';
import { IdentitySection } from '../components/admin-page/IdentitySection';
import { OrganizationsSection } from '../components/admin-page/OrganizationsSection';
import { RolesSection } from '../components/admin-page/RolesSection';
import { SelectedOrgSection } from '../components/admin-page/SelectedOrgSection';
import { SummarySection } from '../components/admin-page/SummarySection';
import { DiscordSection } from '../components/admin-page/DiscordSection';
import { ProxxObservabilitySection } from '../components/admin-page/ProxxObservabilitySection';
import { TranslationModelSection } from '../components/admin-page/TranslationModelSection';
import type { LakeFormState, Notice, OrgFormState, RoleFormState, ToolDraftEffect, UserFormState } from '../components/admin-page/types';
import { UsersMembershipsSection } from '../components/admin-page/UsersMembershipsSection';
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

const ORG_KIND_OPTIONS = ['platform_owner', 'customer', 'internal', 'partner'];
const DATA_LAKE_KIND_OPTIONS = ['workspace_docs', 'analytics', 'notes', 'uploads'];

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
  const canManageTranslations = hasPermission('org.translations.manage');
  const canViewProxxObservability = hasPermission('org.proxx.observability.read');
  const looksLikeAdmin = Boolean(context?.isSystemAdmin || hasPermission('org.users.read') || hasPermission('org.roles.read'));
  const syncMembershipDrafts = useCallback((nextUsers: AdminUserSummary[], orgId: string) => {
    const drafts = hydrateMembershipDrafts(nextUsers, orgId);
    setMembershipRoleDrafts(drafts.roleDrafts);
    setMembershipToolDrafts(drafts.toolDrafts);
  }, []);
  const syncRoleDrafts = useCallback((nextRoles: AdminRoleSummary[]) => {
    setRoleToolDrafts(hydrateRoleDrafts(nextRoles));
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
      syncRoleDrafts(roleResult.roles);
      syncMembershipDrafts(userResult.users, orgId);
    } catch (loadError) {
      setError(errorMessage(loadError));
    } finally {
      setOrgLoading(false);
    }
  }, [canReadDataLakes, canReadRoles, canReadUsers, syncMembershipDrafts, syncRoleDrafts]);
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
        <IdentitySection
          identityForm={identityForm}
          setIdentityForm={setIdentityForm}
          context={context}
          onApplyIdentity={handleApplyIdentity}
        />
        <SummarySection
          orgs={orgs}
          toolsCount={tools.length}
          permissionsCount={permissions.length}
          bootstrap={bootstrap}
        />
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
            <OrganizationsSection
              context={context}
              orgs={orgs}
              selectedOrgId={selectedOrgId}
              setSelectedOrgId={setSelectedOrgId}
              canCreateOrgs={canCreateOrgs}
              orgForm={orgForm}
              setOrgForm={setOrgForm}
              creatingOrg={creatingOrg}
              onCreateOrg={handleCreateOrg}
              orgKindOptions={ORG_KIND_OPTIONS}
            />
            <SelectedOrgSection selectedOrg={selectedOrg} context={context} />
          </div>

          {orgLoading ? <div className="text-sm text-slate-400">Loading selected org resources…</div> : null}

          <UsersMembershipsSection
            selectedOrgId={selectedOrgId}
            selectedOrgName={selectedOrg?.name || ''}
            canCreateUsers={Boolean(selectedOrg && canCreateUsers)}
            canUpdateMemberships={canUpdateMemberships}
            canUpdateUserPolicies={canUpdateUserPolicies}
            users={users}
            roles={roles}
            tools={tools}
            userForm={userForm}
            setUserForm={setUserForm}
            membershipRoleDrafts={membershipRoleDrafts}
            setMembershipRoleDrafts={setMembershipRoleDrafts}
            membershipToolDrafts={membershipToolDrafts}
            setMembershipToolDrafts={setMembershipToolDrafts}
            creatingUser={creatingUser}
            savingMembershipId={savingMembershipId}
            onCreateUser={handleCreateUser}
            onSaveMembershipRoles={handleSaveMembershipRoles}
            onSaveMembershipPolicies={handleSaveMembershipPolicies}
          />

          <div className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
            <RolesSection
              selectedOrgId={selectedOrgId}
              selectedOrgName={selectedOrg?.name || ''}
              canCreateRoles={Boolean(selectedOrg && canCreateRoles)}
              canUpdateRolePolicies={canUpdateRolePolicies}
              roles={roles}
              tools={tools}
              permissionGroups={permissionGroups}
              roleForm={roleForm}
              setRoleForm={setRoleForm}
              roleToolDrafts={roleToolDrafts}
              setRoleToolDrafts={setRoleToolDrafts}
              creatingRole={creatingRole}
              savingRoleId={savingRoleId}
              onCreateRole={handleCreateRole}
              onSaveRolePolicies={handleSaveRolePolicies}
            />
            <DataLakesSection
              selectedOrgName={selectedOrg?.name || ''}
              canCreateDataLakes={Boolean(selectedOrg && canCreateDataLakes)}
              lakeForm={lakeForm}
              setLakeForm={setLakeForm}
              creatingLake={creatingLake}
              dataLakes={dataLakes}
              dataLakeKindOptions={DATA_LAKE_KIND_OPTIONS}
              onCreateLake={handleCreateLake}
            />
          </div>

          <DiscordSection canManage={canCreateOrgs} />

          <ProxxObservabilitySection canView={canViewProxxObservability} />

          <TranslationModelSection canManage={canManageTranslations} />

          <CatalogSection
            permissionGroups={permissionGroups}
            tools={tools}
          />
        </>
      )}
    </div>
  );
}
