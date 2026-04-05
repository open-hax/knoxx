import type {
  AdminMembershipSummary,
  AdminPermissionDefinition,
  AdminRoleSummary,
  AdminToolPolicy,
  AdminUserSummary,
} from '../../lib/types';
import type { ToolDraftEffect } from './types';

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

export function groupPermissions(permissions: AdminPermissionDefinition[]): Array<[string, AdminPermissionDefinition[]]> {
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

export function membershipForOrg(user: AdminUserSummary, orgId: string): AdminMembershipSummary | null {
  return user.memberships.find((membership) => membership.orgId === orgId) ?? null;
}

export function toolDraftMap(policies: AdminToolPolicy[]): Record<string, ToolDraftEffect> {
  return policies.reduce<Record<string, ToolDraftEffect>>((acc, policy) => {
    acc[policy.toolId] = policy.effect;
    return acc;
  }, {});
}

export function toolPoliciesFromDraft(draft: Record<string, ToolDraftEffect>): AdminToolPolicy[] {
  return Object.entries(draft).flatMap(([toolId, effect]) => {
    if (effect !== 'allow' && effect !== 'deny') {
      return [];
    }

    return [{ toolId, effect } satisfies AdminToolPolicy];
  });
}

export function toggleListValue(values: string[], value: string): string[] {
  return values.includes(value)
    ? values.filter((entry) => entry !== value)
    : [...values, value];
}

export function hydrateRoleDrafts(nextRoles: AdminRoleSummary[]): Record<string, Record<string, ToolDraftEffect>> {
  const nextDrafts: Record<string, Record<string, ToolDraftEffect>> = {};
  for (const role of nextRoles) {
    nextDrafts[role.id] = toolDraftMap(role.toolPolicies);
  }
  return nextDrafts;
}

export function hydrateMembershipDrafts(nextUsers: AdminUserSummary[], orgId: string): {
  roleDrafts: Record<string, string[]>;
  toolDrafts: Record<string, Record<string, ToolDraftEffect>>;
} {
  const roleDrafts: Record<string, string[]> = {};
  const toolDrafts: Record<string, Record<string, ToolDraftEffect>> = {};

  for (const user of nextUsers) {
    const membership = membershipForOrg(user, orgId);
    if (!membership) continue;
    roleDrafts[membership.id] = membership.roles.map((role) => role.slug);
    toolDrafts[membership.id] = toolDraftMap(membership.toolPolicies);
  }

  return { roleDrafts, toolDrafts };
}
