import type React from 'react';
import type { AdminRoleSummary, AdminToolDefinition, AdminUserSummary } from '../../lib/types';
import { membershipForOrg, toggleListValue, toolDraftMap } from './helpers';
import { Badge, SectionCard } from './common';
import type { ToolDraftEffect, UserFormState } from './types';

export function UsersMembershipsSection({
  selectedOrgId,
  selectedOrgName,
  canCreateUsers,
  canUpdateMemberships,
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
  onSaveMembershipRoles,
  onSaveMembershipPolicies,
}: {
  selectedOrgId: string;
  selectedOrgName: string;
  canCreateUsers: boolean;
  canUpdateMemberships: boolean;
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
  onSaveMembershipRoles: (membershipId: string) => void | Promise<void>;
  onSaveMembershipPolicies: (membershipId: string) => void | Promise<void>;
}) {
  return (
    <SectionCard
      title="Users and memberships"
      description="Create users, replace membership roles, and apply explicit per-membership tool overrides."
    >
      {selectedOrgId && canCreateUsers ? (
        <form className="mb-5 grid gap-3 rounded-xl border border-slate-800 bg-slate-900/80 p-4 md:grid-cols-4" onSubmit={onCreateUser}>
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
              {creatingUser ? 'Creating…' : `Create user in ${selectedOrgName}`}
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
          const membership = membershipForOrg(user, selectedOrgId);
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
            </div>
          );
        })}
      </div>
    </SectionCard>
  );
}
