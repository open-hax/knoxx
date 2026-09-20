import type {
  AdminBootstrapContext,
  AdminActorCredentialSummary,
  AdminDataLakeSummary,
  AdminMembershipSummary,
  AdminOrgSummary,
  AdminPermissionDefinition,
  AdminRoleSummary,
  AdminToolDefinition,
  AdminToolPolicy,
  AdminUserSummary,
} from "../types";
import {
  request,
  asRecord,
  valueAt,
  stringValue,
  optionalStringValue,
  optionalNullableStringValue,
  optionalBooleanValue,
  optionalNumberValue,
  stringArrayValue,
  recordArrayValue,
  optionalRecordValue,
} from "./core";
import type {
  GraphMonitoringStats,
  DiscordConfigStatus,
  EventAgentControlResponse,
  CreateOrgActorPayload,
  UpdateAdminActorPayload,
  ActorCredentialPayload,
  CreateOrgRolePayload,
  CreateOrgDataLakePayload,
  EventAgentEventPayload,
  EventAgentRuntimeResetResponse,
} from "../../components/admin-page/types";
export type {
  GraphMonitoringStats,
  DiscordConfigStatus,
  EventAgentToolPolicy,
  EventAgentJobControl,
  EventAgentRuntimeJob,
  EventAgentControlResponse,
} from "../../components/admin-page/types";

function inferPermissionResourceKind(code: string): string {
  return code.split(".")[0] || "misc";
}

function inferPermissionAction(code: string): string {
  const parts = code.split(".");
  return parts[parts.length - 1] || "read";
}

function normalizePermission(value: unknown): AdminPermissionDefinition {
  const record = asRecord(value);
  const code = stringValue(record, ["code"]);
  return {
    id: stringValue(record, ["id"], code),
    code,
    resourceKind: stringValue(record, ["resourceKind", "resource-kind", "resource_kind"], inferPermissionResourceKind(code)),
    action: stringValue(record, ["action"], inferPermissionAction(code)),
    description: stringValue(record, ["description"], code),
  };
}

function normalizeTool(value: unknown): AdminToolDefinition {
  const record = asRecord(value);
  const id = stringValue(record, ["id"]);
  return {
    id,
    label: stringValue(record, ["label"], id),
    description: stringValue(record, ["description"]),
    riskLevel: stringValue(record, ["riskLevel", "risk-level", "risk_level"], "standard"),
  };
}

function normalizeToolPolicy(value: unknown): AdminToolPolicy | null {
  const record = asRecord(value);
  const toolId = stringValue(record, ["toolId", "tool-id", "tool_id"]);
  const effect = valueAt(record, "effect");
  if (!toolId || (effect !== "allow" && effect !== "deny")) {
    return null;
  }

  const constraints = optionalRecordValue(record, ["constraints"]);
  return constraints ? { toolId, effect, constraints } : { toolId, effect };
}

function normalizeToolPolicies(value: unknown): AdminToolPolicy[] {
  return Array.isArray(value)
    ? value.map(normalizeToolPolicy).filter((policy): policy is AdminToolPolicy => policy !== null)
    : [];
}

function normalizeOrg(value: unknown): AdminOrgSummary {
  const record = asRecord(value);
  return {
    id: stringValue(record, ["id"]),
    slug: stringValue(record, ["slug"]),
    name: stringValue(record, ["name"]),
    kind: stringValue(record, ["kind"], "customer"),
    isPrimary: optionalBooleanValue(record, ["isPrimary", "is-primary", "is_primary"]) ?? false,
    status: stringValue(record, ["status"], "active"),
    memberCount: optionalNumberValue(record, ["memberCount", "member-count", "member_count"]),
    roleCount: optionalNumberValue(record, ["roleCount", "role-count", "role_count"]),
    dataLakeCount: optionalNumberValue(record, ["dataLakeCount", "data-lake-count", "data_lake_count"]),
    createdAt: optionalStringValue(record, ["createdAt", "created-at", "created_at"]),
    updatedAt: optionalStringValue(record, ["updatedAt", "updated-at", "updated_at"]),
  };
}

function normalizeRole(value: unknown): AdminRoleSummary {
  const record = asRecord(value);
  return {
    id: stringValue(record, ["id"]),
    slug: stringValue(record, ["slug"]),
    name: stringValue(record, ["name"]),
    scopeKind: optionalStringValue(record, ["scopeKind", "scope-kind", "scope_kind"]),
    orgId: optionalNullableStringValue(record, ["orgId", "org-id", "org_id"]),
    builtIn: optionalBooleanValue(record, ["builtIn", "built-in", "built_in"]),
    systemManaged: optionalBooleanValue(record, ["systemManaged", "system-managed", "system_managed"]),
    createdAt: optionalStringValue(record, ["createdAt", "created-at", "created_at"]),
    updatedAt: optionalStringValue(record, ["updatedAt", "updated-at", "updated_at"]),
    permissions: stringArrayValue(valueAt(record, "permissions")),
    toolPolicies: normalizeToolPolicies(valueAt(record, "toolPolicies", "tool-policies", "tool_policies")),
  };
}

function normalizeMembership(value: unknown): AdminMembershipSummary {
  const record = asRecord(value);
  return {
    id: stringValue(record, ["id"]),
    userId: optionalStringValue(record, ["userId"]),
    orgId: stringValue(record, ["orgId"]),
    actorId: optionalStringValue(record, ["actorId"]),
    orgName: optionalStringValue(record, ["orgName"]),
    orgSlug: optionalStringValue(record, ["orgSlug"]),
    status: stringValue(record, ["status"], "active"),
    isDefault: optionalBooleanValue(record, ["isDefault"]),
    createdAt: optionalStringValue(record, ["createdAt"]),
    updatedAt: optionalStringValue(record, ["updatedAt"]),
    roles: recordArrayValue(valueAt(record, "roles")).map(normalizeRole),
    toolPolicies: normalizeToolPolicies(valueAt(record, "toolPolicies")),
  };
}

function normalizeActorCredential(value: unknown): AdminActorCredentialSummary {
  const record = asRecord(value);
  const rawSecrets = optionalRecordValue(record, ["secretJson"]);
  return {
    id: stringValue(record, ["id"]),
    provider: stringValue(record, ["provider"]),
    label: optionalStringValue(record, ["label"]),
    kind: stringValue(record, ["kind"], "credential"),
    accountIdentifier: optionalNullableStringValue(record, ["accountIdentifier"]),
    status: stringValue(record, ["status"], "active"),
    configuredFields: stringArrayValue(valueAt(record, "configuredFields")),
    secretJson: rawSecrets ? Object.fromEntries(Object.entries(rawSecrets).map(([k, v]) => [k, String(v ?? "")])) : undefined,
    createdAt: optionalStringValue(record, ["createdAt"]),
    updatedAt: optionalStringValue(record, ["updatedAt"]),
  };
}

function normalizeUser(value: unknown): AdminUserSummary {
  const record = asRecord(value);
  return {
    id: stringValue(record, ["id"]),
    principalId: optionalStringValue(record, ["principalId"]),
    identityBound: valueAt(record, "identityBound") === true,
    identityEnrollmentRequired: valueAt(record, "identityEnrollmentRequired") === true,
    email: stringValue(record, ["email"]),
    displayName: stringValue(record, ["displayName"], stringValue(record, ["email"])),
    authProvider: optionalStringValue(record, ["authProvider"]),
    externalSubject: optionalNullableStringValue(record, ["externalSubject"]),
    status: stringValue(record, ["status"], "active"),
    createdAt: optionalStringValue(record, ["createdAt"]),
    updatedAt: optionalStringValue(record, ["updatedAt"]),
    credentials: recordArrayValue(valueAt(record, "credentials")).map(normalizeActorCredential),
    memberships: recordArrayValue(valueAt(record, "memberships")).map(normalizeMembership),
  };
}

function normalizeDataLake(value: unknown): AdminDataLakeSummary {
  const record = asRecord(value);
  return {
    id: stringValue(record, ["id"]),
    orgId: stringValue(record, ["orgId", "org-id", "org_id"]),
    name: stringValue(record, ["name"]),
    slug: stringValue(record, ["slug"]),
    kind: stringValue(record, ["kind"], "workspace_docs"),
    config: optionalRecordValue(record, ["config"]) ?? {},
    status: stringValue(record, ["status"], "active"),
    createdAt: optionalStringValue(record, ["createdAt", "created-at", "created_at"]),
    updatedAt: optionalStringValue(record, ["updatedAt", "updated-at", "updated_at"]),
  };
}

export async function getAdminBootstrap(): Promise<AdminBootstrapContext> {
  return request<AdminBootstrapContext>("/api/admin/bootstrap");
}

export async function listAdminPermissions(): Promise<{ permissions: AdminPermissionDefinition[] }> {
  const response = asRecord(await request<unknown>("/api/admin/permissions"));
  return { permissions: recordArrayValue(valueAt(response, "permissions")).map(normalizePermission) };
}

export async function listAdminTools(): Promise<{ tools: AdminToolDefinition[] }> {
  const response = asRecord(await request<unknown>("/api/admin/tools"));
  return { tools: recordArrayValue(valueAt(response, "tools")).map(normalizeTool) };
}

export async function listAdminOrgs(): Promise<{ orgs: AdminOrgSummary[] }> {
  const response = asRecord(await request<unknown>("/api/admin/orgs"));
  return { orgs: recordArrayValue(valueAt(response, "orgs")).map(normalizeOrg) };
}

export async function createAdminOrg(payload: { name: string; slug?: string; kind?: string }): Promise<{ org: AdminOrgSummary }> {
  const response = asRecord(await request<unknown>("/api/admin/orgs", {
    method: "POST",
    body: JSON.stringify(payload),
  }));
  return { org: normalizeOrg(valueAt(response, "org")) };
}

export async function listOrgActors(orgId: string): Promise<{ users: AdminUserSummary[] }> {
  const response = asRecord(await request<unknown>(`/api/admin/orgs/${encodeURIComponent(orgId)}/actors`));
  return { users: recordArrayValue(valueAt(response, "users")).map(normalizeUser) };
}

export async function createOrgActor(orgId: string, payload: CreateOrgActorPayload): Promise<{ user: AdminUserSummary | null }> {
  const response = asRecord(await request<unknown>(`/api/admin/orgs/${encodeURIComponent(orgId)}/actors`, {
    method: "POST",
    body: JSON.stringify(payload),
  }));
  const user = valueAt(response, "user");
  return { user: user == null ? null : normalizeUser(user) };
}

export async function updateAdminActor(userId: string, payload: UpdateAdminActorPayload): Promise<{ user: AdminUserSummary | null }> {
  // A blank optional field means this unbound row still has no actor identity.
  // Sending an empty string would ask the directory to assign an invalid ID.
  const actorId = payload.actorId?.trim() || undefined;
  const response = asRecord(await request<unknown>(`/api/admin/actors/${encodeURIComponent(userId)}`, {
    method: "PATCH",
    body: JSON.stringify({ ...payload, actorId }),
  }));
  const user = valueAt(response, "user");
  return { user: user == null ? null : normalizeUser(user) };
}

export async function upsertAdminActorCredential(userId: string, provider: string, payload: ActorCredentialPayload): Promise<{ credential: unknown }> {
  return request<{ credential: unknown }>(`/api/admin/actors/${encodeURIComponent(userId)}/credentials/${encodeURIComponent(provider)}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function listOrgRoles(orgId: string): Promise<{ roles: AdminRoleSummary[] }> {
  const response = asRecord(await request<unknown>(`/api/admin/orgs/${encodeURIComponent(orgId)}/roles`));
  return { roles: recordArrayValue(valueAt(response, "roles")).map(normalizeRole) };
}

export async function createOrgRole(orgId: string, payload: CreateOrgRolePayload): Promise<{ role: AdminRoleSummary | null }> {
  const response = asRecord(await request<unknown>(`/api/admin/orgs/${encodeURIComponent(orgId)}/roles`, {
    method: "POST",
    body: JSON.stringify(payload),
  }));
  const role = valueAt(response, "role");
  return { role: role == null ? null : normalizeRole(role) };
}

export async function updateRoleToolPolicies(roleId: string, toolPolicies: AdminToolPolicy[]): Promise<{ role: AdminRoleSummary | null }> {
  const response = asRecord(await request<unknown>(`/api/admin/roles/${encodeURIComponent(roleId)}/tool-policies`, {
    method: "PATCH",
    body: JSON.stringify({ toolPolicies }),
  }));
  const role = valueAt(response, "role");
  return { role: role == null ? null : normalizeRole(role) };
}

export async function updateMembershipRoles(membershipId: string, roleSlugs: string[]): Promise<{ membership: AdminMembershipSummary | null }> {
  const response = asRecord(await request<unknown>(`/api/admin/memberships/${encodeURIComponent(membershipId)}/roles`, {
    method: "PATCH",
    body: JSON.stringify({ roleSlugs, replace: true }),
  }));
  const membership = valueAt(response, "membership");
  return { membership: membership == null ? null : normalizeMembership(membership) };
}

export async function updateMembershipToolPolicies(membershipId: string, toolPolicies: AdminToolPolicy[]): Promise<{ membership: AdminMembershipSummary | null }> {
  const response = asRecord(await request<unknown>(`/api/admin/memberships/${encodeURIComponent(membershipId)}/tool-policies`, {
    method: "PATCH",
    body: JSON.stringify({ toolPolicies }),
  }));
  const membership = valueAt(response, "membership");
  return { membership: membership == null ? null : normalizeMembership(membership) };
}

export async function listOrgDataLakes(orgId: string): Promise<{ dataLakes: AdminDataLakeSummary[] }> {
  const response = asRecord(await request<unknown>(`/api/admin/orgs/${encodeURIComponent(orgId)}/data-lakes`));
  return { dataLakes: recordArrayValue(valueAt(response, "dataLakes", "data-lakes", "data_lakes")).map(normalizeDataLake) };
}

export async function createOrgDataLake(orgId: string, payload: CreateOrgDataLakePayload): Promise<{ dataLake: AdminDataLakeSummary }> {
  const response = asRecord(await request<unknown>(`/api/admin/orgs/${encodeURIComponent(orgId)}/data-lakes`, {
    method: "POST",
    body: JSON.stringify(payload),
  }));
  return { dataLake: normalizeDataLake(valueAt(response, "dataLake", "data-lake", "data_lake")) };
}

export async function getGraphMonitoring(): Promise<GraphMonitoringStats> {
  const res = await fetch(`${import.meta.env.VITE_OPENPLANNER_URL || "http://127.0.0.1:7777"}/v1/graph/monitoring`, {
    headers: {
      "Authorization": `Bearer ${import.meta.env.VITE_OPENPLANNER_API_KEY || "change-me"}`,
    },
  });
  if (!res.ok) {
    throw new Error(`Graph monitoring request failed: ${res.status}`);
  }
  return res.json();
}

export async function getDiscordConfig(): Promise<DiscordConfigStatus> {
  return request<DiscordConfigStatus>("/api/admin/config/discord");
}

export async function updateDiscordConfig(discordBotToken: string): Promise<DiscordConfigStatus & { ok: boolean }> {
  return request<DiscordConfigStatus & { ok: boolean }>("/api/admin/config/discord", {
    method: "PUT",
    body: JSON.stringify({ discordBotToken }),
  });
}

export async function getEventAgentControl(): Promise<EventAgentControlResponse> {
  return request<EventAgentControlResponse>("/api/admin/config/events");
}

export async function updateEventAgentControl(control: EventAgentControlResponse["control"]): Promise<EventAgentControlResponse & { ok: boolean }> {
  return request<EventAgentControlResponse & { ok: boolean }>("/api/admin/config/events", {
    method: "PUT",
    body: JSON.stringify(control),
  });
}

export async function runEventAgentJob(jobId: string): Promise<{ ok: boolean; jobId: string; result?: unknown }> {
  return request<{ ok: boolean; jobId: string; result?: unknown }>(`/api/admin/config/events/jobs/${encodeURIComponent(jobId)}/run`, {
    method: "POST",
  });
}

export async function fireTrigger(triggerId: string): Promise<{ ok: boolean; triggerId: string; result?: unknown }> {
  return request<{ ok: boolean; triggerId: string; result?: unknown }>(`/api/admin/triggers/${encodeURIComponent(triggerId)}/fire`, {
    method: "POST",
  });
}

export async function dispatchEventAgentEvent(event: EventAgentEventPayload): Promise<{ ok: boolean; matchedJobs: string[]; event: Record<string, unknown> }> {
  return request<{ ok: boolean; matchedJobs: string[]; event: Record<string, unknown> }>("/api/admin/config/events/dispatch", {
    method: "POST",
    body: JSON.stringify(event),
  });
}

export async function stopEventAgentRuntime(): Promise<EventAgentControlResponse & { ok: boolean; action: string }> {
  return request<EventAgentControlResponse & { ok: boolean; action: string }>("/api/admin/config/events/runtime/stop", {
    method: "POST",
  });
}

export async function startEventAgentRuntime(): Promise<EventAgentControlResponse & { ok: boolean; action: string }> {
  return request<EventAgentControlResponse & { ok: boolean; action: string }>("/api/admin/config/events/runtime/start", {
    method: "POST",
  });
}

export async function resetEventAgentRuntime(): Promise<EventAgentRuntimeResetResponse> {
  return request<EventAgentRuntimeResetResponse>("/api/admin/config/events/runtime/reset", {
    method: "POST",
  });
}
