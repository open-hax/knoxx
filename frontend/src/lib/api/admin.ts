import type {
  AdminBootstrapContext,
  AdminDataLakeSummary,
  AdminMembershipSummary,
  AdminOrgSummary,
  AdminPermissionDefinition,
  AdminRoleSummary,
  AdminToolDefinition,
  AdminToolPolicy,
  AdminUserSummary,
} from "../types";
import { request } from "./core";

export async function getAdminBootstrap(): Promise<AdminBootstrapContext> {
  return request<AdminBootstrapContext>("/api/admin/bootstrap");
}

export async function listAdminPermissions(): Promise<{ permissions: AdminPermissionDefinition[] }> {
  return request<{ permissions: AdminPermissionDefinition[] }>("/api/admin/permissions");
}

export async function listAdminTools(): Promise<{ tools: AdminToolDefinition[] }> {
  return request<{ tools: AdminToolDefinition[] }>("/api/admin/tools");
}

export async function listAdminOrgs(): Promise<{ orgs: AdminOrgSummary[] }> {
  return request<{ orgs: AdminOrgSummary[] }>("/api/admin/orgs");
}

export async function createAdminOrg(payload: { name: string; slug?: string; kind?: string }): Promise<{ org: AdminOrgSummary }> {
  return request<{ org: AdminOrgSummary }>("/api/admin/orgs", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function listOrgUsers(orgId: string): Promise<{ users: AdminUserSummary[] }> {
  return request<{ users: AdminUserSummary[] }>(`/api/admin/orgs/${encodeURIComponent(orgId)}/users`);
}

export async function createOrgUser(orgId: string, payload: {
  email: string;
  displayName: string;
  roleSlugs: string[];
  toolPolicies?: AdminToolPolicy[];
}): Promise<{ user: AdminUserSummary | null }> {
  return request<{ user: AdminUserSummary | null }>(`/api/admin/orgs/${encodeURIComponent(orgId)}/users`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function listOrgRoles(orgId: string): Promise<{ roles: AdminRoleSummary[] }> {
  return request<{ roles: AdminRoleSummary[] }>(`/api/admin/orgs/${encodeURIComponent(orgId)}/roles`);
}

export async function createOrgRole(orgId: string, payload: {
  name: string;
  slug?: string;
  permissionCodes: string[];
  toolPolicies?: AdminToolPolicy[];
}): Promise<{ role: AdminRoleSummary | null }> {
  return request<{ role: AdminRoleSummary | null }>(`/api/admin/orgs/${encodeURIComponent(orgId)}/roles`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateRoleToolPolicies(roleId: string, toolPolicies: AdminToolPolicy[]): Promise<{ role: AdminRoleSummary | null }> {
  return request<{ role: AdminRoleSummary | null }>(`/api/admin/roles/${encodeURIComponent(roleId)}/tool-policies`, {
    method: "PATCH",
    body: JSON.stringify({ toolPolicies }),
  });
}

export async function updateMembershipRoles(membershipId: string, roleSlugs: string[]): Promise<{ membership: AdminMembershipSummary | null }> {
  return request<{ membership: AdminMembershipSummary | null }>(`/api/admin/memberships/${encodeURIComponent(membershipId)}/roles`, {
    method: "PATCH",
    body: JSON.stringify({ roleSlugs, replace: true }),
  });
}

export async function updateMembershipToolPolicies(membershipId: string, toolPolicies: AdminToolPolicy[]): Promise<{ membership: AdminMembershipSummary | null }> {
  return request<{ membership: AdminMembershipSummary | null }>(`/api/admin/memberships/${encodeURIComponent(membershipId)}/tool-policies`, {
    method: "PATCH",
    body: JSON.stringify({ toolPolicies }),
  });
}

export async function listOrgDataLakes(orgId: string): Promise<{ dataLakes: AdminDataLakeSummary[] }> {
  return request<{ dataLakes: AdminDataLakeSummary[] }>(`/api/admin/orgs/${encodeURIComponent(orgId)}/data-lakes`);
}

export async function createOrgDataLake(orgId: string, payload: {
  name: string;
  slug?: string;
  kind?: string;
  config?: Record<string, unknown>;
}): Promise<{ dataLake: AdminDataLakeSummary }> {
  return request<{ dataLake: AdminDataLakeSummary }>(`/api/admin/orgs/${encodeURIComponent(orgId)}/data-lakes`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
