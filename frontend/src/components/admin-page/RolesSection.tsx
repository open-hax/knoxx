import type React from 'react';
import type { AdminPermissionDefinition, AdminRoleSummary, AdminToolDefinition } from '../../lib/types';
import { toggleListValue, toolDraftMap } from './helpers';
import { Badge, SectionCard } from './common';
import type { RoleFormState, ToolDraftEffect } from './types';

export function RolesSection({
  selectedOrgId,
  selectedOrgName,
  canCreateRoles,
  canUpdateRolePolicies,
  roles,
  tools,
  permissionGroups,
  roleForm,
  setRoleForm,
  roleToolDrafts,
  setRoleToolDrafts,
  creatingRole,
  savingRoleId,
  onCreateRole,
  onSaveRolePolicies,
}: {
  selectedOrgId: string;
  selectedOrgName: string;
  canCreateRoles: boolean;
  canUpdateRolePolicies: boolean;
  roles: AdminRoleSummary[];
  tools: AdminToolDefinition[];
  permissionGroups: Array<[string, AdminPermissionDefinition[]]>;
  roleForm: RoleFormState;
  setRoleForm: React.Dispatch<React.SetStateAction<RoleFormState>>;
  roleToolDrafts: Record<string, Record<string, ToolDraftEffect>>;
  setRoleToolDrafts: React.Dispatch<React.SetStateAction<Record<string, Record<string, ToolDraftEffect>>>>;
  creatingRole: boolean;
  savingRoleId: string | null;
  onCreateRole: (event: React.FormEvent) => void | Promise<void>;
  onSaveRolePolicies: (roleId: string) => void | Promise<void>;
}) {
  return (
    <SectionCard
      title="Roles"
      description="Seeded and custom org roles. Create roles from permission atoms, then refine tool policies."
    >
      {selectedOrgId && canCreateRoles ? (
        <form className="mb-5 space-y-4 rounded-xl border border-slate-800 bg-slate-900/80 p-4" onSubmit={onCreateRole}>
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
              {creatingRole ? 'Creating…' : `Create role in ${selectedOrgName}`}
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
                        onClick={() => void onSaveRolePolicies(role.id)}
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
  );
}
