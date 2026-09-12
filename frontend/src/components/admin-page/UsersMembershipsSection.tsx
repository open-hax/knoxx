import { ActorIdentityEditors } from "./IdentitySection";
import { useEffect, useMemo, useState } from 'react';
import type React from 'react';
import type { AdminRoleSummary, AdminToolDefinition, AdminUserSummary } from '../../lib/types';
import { CREDENTIAL_DESCRIPTORS, credentialKey, draftProfileForUser, draftCredentialForDescriptor, actorSearchText, membershipForOrg, toggleListValue, toolDraftMap } from './helpers';
import { Badge, SectionCard } from './common';
import type { ActorProfileDraft, ActorCredentialDraft, ToolDraftEffect, UserFormState } from './types';

export function UsersMembershipsSection({
  selectedOrgId,
  selectedOrgName,
  canCreateUsers,
  canUpdateMemberships,
  canUpdateGlobalStatus = false,
  canUpdateUserPolicies,
  users,
  roles,
  tools,
  userForm,
  setUserForm,
  membershipRoleDrafts,
  setMembershipRoleDrafts,
  membershipToolDrafts,
  setMembershipToolDrafts,
  creatingUser,
  savingMembershipId,
  onCreateUser,
  onSaveActorProfile,
  onSaveActorCredential,
  onSaveMembershipRoles,
  onSaveMembershipPolicies,
}: {
  selectedOrgId: string;
  selectedOrgName: string;
  canCreateUsers: boolean;
  canUpdateMemberships: boolean;
  canUpdateGlobalStatus?: boolean;
  canUpdateUserPolicies: boolean;
  users: AdminUserSummary[];
  roles: AdminRoleSummary[];
  tools: AdminToolDefinition[];
  userForm: UserFormState;
  setUserForm: React.Dispatch<React.SetStateAction<UserFormState>>;
  membershipRoleDrafts: Record<string, string[]>;
  setMembershipRoleDrafts: React.Dispatch<React.SetStateAction<Record<string, string[]>>>;
  membershipToolDrafts: Record<string, Record<string, ToolDraftEffect>>;
  setMembershipToolDrafts: React.Dispatch<React.SetStateAction<Record<string, Record<string, ToolDraftEffect>>>>;
  creatingUser: boolean;
  savingMembershipId: string | null;
  onCreateUser: (event: React.FormEvent) => void | Promise<void>;
  onSaveActorProfile: (userId: string, draft: ActorProfileDraft) => void | Promise<void>;
  onSaveActorCredential: (userId: string, provider: string, draft: ActorCredentialDraft) => void | Promise<void>;
  onSaveMembershipRoles: (membershipId: string) => void | Promise<void>;
  onSaveMembershipPolicies: (membershipId: string) => void | Promise<void>;
}) {
  const [actorDrafts, setActorDrafts] = useState<Record<string, ActorProfileDraft>>({});
  const [credentialDrafts, setCredentialDrafts] = useState<Record<string, ActorCredentialDraft>>({});
  const [savingActorId, setSavingActorId] = useState<string | null>(null);
  const [savingCredential, setSavingCredential] = useState<string | null>(null);
  const [actorSearch, setActorSearch] = useState('');
  const [expandedActors, setExpandedActors] = useState<Record<string, boolean>>({});

  useEffect(() => {
    setActorDrafts(Object.fromEntries(users.map((user) => [user.id, draftProfileForUser(user, selectedOrgId)])));
    setCredentialDrafts(Object.fromEntries(users.flatMap((user) => CREDENTIAL_DESCRIPTORS.map((descriptor) => [
      credentialKey(user.id, descriptor.provider),
      draftCredentialForDescriptor(user, descriptor),
    ]))));
  }, [selectedOrgId, users]);

  const orgActorLabel = useMemo(() => selectedOrgName || 'selected org', [selectedOrgName]);
  const filteredUsers = useMemo(() => {
    const query = actorSearch.trim().toLowerCase();
    return users.filter((user) => {
      const membership = membershipForOrg(user, selectedOrgId);
      if (!membership) return false;
      return !query || actorSearchText(user, selectedOrgId).includes(query);
    });
  }, [actorSearch, selectedOrgId, users]);

  const toggleActorExpanded = (userId: string) => {
    setExpandedActors((current) => ({ ...current, [userId]: !current[userId] }));
  };

  const setFilteredActorsExpanded = (expanded: boolean) => {
    setExpandedActors((current) => ({
      ...current,
      ...Object.fromEntries(filteredUsers.map((user) => [user.id, expanded])),
    }));
  };

  const saveActorProfile = async (userId: string) => {
    const draft = actorDrafts[userId];
    if (!draft) return;
    setSavingActorId(userId);
    try {
      await onSaveActorProfile(userId, draft);
    } finally {
      setSavingActorId(null);
    }
  };

  const saveCredential = async (userId: string, provider: string) => {
    const key = credentialKey(userId, provider);
    const draft = credentialDrafts[key];
    if (!draft) return;
    setSavingCredential(key);
    try {
      await onSaveActorCredential(userId, provider, draft);
    } finally {
      setSavingCredential(null);
    }
  };

  return (
    <SectionCard
      title="Actors and credentials"
      description="Create machine or human actors, bind actor IDs to memberships, and store per-actor Bluesky, Twitch, Discord bot, and Discord OAuth credentials. Secrets are write-only: saved fields are shown as configured, never echoed back."
    >
      {selectedOrgId && canCreateUsers ? (
        <form className="mb-5 grid gap-3 rounded-xl border border-slate-800 bg-slate-900/80 p-4 md:grid-cols-4" onSubmit={onCreateUser}>
          <input
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            aria-label="New actor ID"
            disabled={Boolean(userForm.axxiumPrincipalId?.trim())}
            placeholder="actor id, e.g. discord_automation"
            value={userForm.actorId}
            onChange={(event) => setUserForm((current) => ({ ...current, actorId: event.target.value }))}
          />
          <input
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            aria-label="New actor contact email"
            disabled={Boolean(userForm.axxiumPrincipalId?.trim())}
            placeholder="Optional directory contact email"
            value={userForm.email}
            onChange={(event) => setUserForm((current) => ({ ...current, email: event.target.value }))}
          />
          <input
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            aria-label="New actor display name"
            placeholder="Display name"
            value={userForm.displayName}
            onChange={(event) => setUserForm((current) => ({ ...current, displayName: event.target.value }))}
          />
          <label className="text-xs text-slate-300">
            Existing Axxium principal ID (optional)
            <input aria-label="Existing Axxium principal ID (optional)"
              className="mt-2 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
              value={userForm.axxiumPrincipalId || ''}
              onChange={event => setUserForm(current => ({ ...current, axxiumPrincipalId: event.target.value }))} />
            <span className="block mt-2">A verified principal receives membership here. Leave blank to create a directory actor requiring identity enrollment.</span>
          </label>
          <div>
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
              disabled={creatingUser || (!userForm.email.trim() && !userForm.actorId.trim() && !userForm.displayName.trim() && !userForm.axxiumPrincipalId?.trim())}
              className="rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-4 py-2 text-sm font-medium text-emerald-200 hover:bg-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {creatingUser ? 'Creating…' : `Create actor in ${orgActorLabel}`}
            </button>
          </div>
        </form>
      ) : null}

      <div className="mb-4 flex flex-col gap-3 rounded-xl border border-slate-800 bg-slate-950/70 p-4 lg:flex-row lg:items-center lg:justify-between">
        <label className="flex flex-1 flex-col gap-1 text-xs font-medium uppercase tracking-wide text-slate-500">
          Search actors
          <input
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm normal-case tracking-normal text-slate-100 placeholder:text-slate-600"
            aria-label="Search actors"
            placeholder="Search actor id, email, display name, role, credential provider, tool…"
            value={actorSearch}
            onChange={(event) => setActorSearch(event.target.value)}
          />
        </label>
        <div className="flex flex-wrap items-center gap-2 text-xs text-slate-400">
          <span>{filteredUsers.length} of {users.filter((user) => membershipForOrg(user, selectedOrgId)).length} actor(s)</span>
          <button
            type="button"
            onClick={() => setFilteredActorsExpanded(true)}
            className="rounded-lg border border-slate-700 px-3 py-1.5 text-slate-200 hover:bg-slate-800"
          >
            Expand filtered
          </button>
          <button
            type="button"
            onClick={() => setFilteredActorsExpanded(false)}
            className="rounded-lg border border-slate-700 px-3 py-1.5 text-slate-200 hover:bg-slate-800"
          >
            Collapse filtered
          </button>
        </div>
      </div>

      <div className="space-y-4">
        {users.length === 0 ? (
          <div className="rounded-xl border border-dashed border-slate-800 px-4 py-6 text-sm text-slate-400">
            No actors are visible in this org.
          </div>
        ) : filteredUsers.length === 0 ? (
          <div className="rounded-xl border border-dashed border-slate-800 px-4 py-6 text-sm text-slate-400">
            No actors match “{actorSearch.trim()}”.
          </div>
        ) : filteredUsers.map((user) => {
          const membership = membershipForOrg(user, selectedOrgId);
          if (!membership) {
            return null;
          }
          const roleDraft = membershipRoleDrafts[membership.id] || membership.roles.map((role) => role.slug);
          const toolDraft = membershipToolDrafts[membership.id] || toolDraftMap(membership.toolPolicies);
          const actorDraft = actorDrafts[user.id] || draftProfileForUser(user, selectedOrgId);
          const isExpanded = Boolean(expandedActors[user.id]);
          return (
            <div key={user.id} className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
              <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                <button
                  type="button"
                  aria-expanded={isExpanded}
                  onClick={() => toggleActorExpanded(user.id)}
                  className="flex min-w-0 flex-1 items-start gap-3 text-left"
                >
                  <span className="mt-0.5 inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full border border-slate-700 text-xs text-slate-300">
                    {isExpanded ? '−' : '+'}
                  </span>
                  <span className="min-w-0">
                    <span className="block truncate text-sm font-semibold text-slate-100">{user.displayName}</span>
                    <span className="block truncate text-sm text-slate-400">{user.email}</span>
                    <span className="mt-1 block text-xs text-slate-500">{isExpanded ? 'Click to collapse' : 'Click to manage profile, credentials, roles, and tools'}</span>
                  </span>
                </button>
                <div className="min-w-0 flex-1 lg:flex-none">
                  <div className="mt-2 flex flex-wrap gap-2">
                    <Badge tone="info">actor:{membership.actorId || 'workspace_user'}</Badge>
                    <Badge tone={membership.status === 'active' ? 'success' : 'danger'}>{membership.status}</Badge>
                    {membership.isDefault ? <Badge tone="info">default membership</Badge> : null}
                  </div>
                </div>
                <div className="flex flex-wrap gap-2">
                  {membership.roles.map((role) => (
                    <Badge key={role.id}>{role.slug}</Badge>
                  ))}
                  {(user.credentials || []).map((credential) => (
                    <Badge key={credential.id} tone="warn">
                      {credential.label || credential.provider}:{credential.configuredFields.length} fields
                    </Badge>
                  ))}
                  {membership.toolPolicies.map((policy) => (
                    <Badge key={`${membership.id}-${policy.toolId}`} tone={policy.effect === 'deny' ? 'danger' : 'warn'}>
                      {policy.toolId}:{policy.effect}
                    </Badge>
                  ))}
                </div>
              </div>

              {isExpanded ? (
                <>
              <ActorIdentityEditors user={user} actorDraft={actorDraft} setActorDrafts={setActorDrafts}
                canUpdateGlobalStatus={canUpdateGlobalStatus} canUpdateMemberships={canUpdateMemberships}
                canUpdateUserPolicies={canUpdateUserPolicies} savingActorId={savingActorId}
                saveActorProfile={saveActorProfile} credentialDrafts={credentialDrafts}
                setCredentialDrafts={setCredentialDrafts} savingCredential={savingCredential}
                saveCredential={saveCredential} />

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
                      onClick={() => void onSaveMembershipRoles(membership.id)}
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
                      onClick={() => void onSaveMembershipPolicies(membership.id)}
                      disabled={savingMembershipId === membership.id}
                      className="mt-4 rounded-lg border border-amber-500/30 bg-amber-500/10 px-4 py-2 text-sm font-medium text-amber-200 hover:bg-amber-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {savingMembershipId === membership.id ? 'Saving…' : 'Save tool overrides'}
                    </button>
                  ) : null}
                </div>
              </div>
                </>
              ) : null}
            </div>
          );
        })}
      </div>
    </SectionCard>
  );
}
