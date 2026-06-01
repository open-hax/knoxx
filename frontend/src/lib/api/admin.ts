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
import { request } from "./core";

type WireRecord = Record<string, unknown>;

function asRecord(value: unknown): WireRecord {
  return value != null && typeof value === "object" && !Array.isArray(value) ? value as WireRecord : {};
}

function valueAt(record: WireRecord, ...keys: string[]): unknown {
  for (const key of keys) {
    if (Object.prototype.hasOwnProperty.call(record, key)) {
      return record[key];
    }
  }
  return undefined;
}

function stringValue(record: WireRecord, keys: string[], fallback = ""): string {
  const value = valueAt(record, ...keys);
  return typeof value === "string" ? value : fallback;
}

function optionalStringValue(record: WireRecord, keys: string[]): string | undefined {
  const value = valueAt(record, ...keys);
  return typeof value === "string" ? value : undefined;
}

function optionalNullableStringValue(record: WireRecord, keys: string[]): string | null | undefined {
  const value = valueAt(record, ...keys);
  if (value === null) return null;
  return typeof value === "string" ? value : undefined;
}

function optionalBooleanValue(record: WireRecord, keys: string[]): boolean | undefined {
  const value = valueAt(record, ...keys);
  return typeof value === "boolean" ? value : undefined;
}

function optionalNumberValue(record: WireRecord, keys: string[]): number | undefined {
  const value = valueAt(record, ...keys);
  return typeof value === "number" ? value : undefined;
}

function stringArrayValue(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((entry): entry is string => typeof entry === "string") : [];
}

function recordArrayValue(value: unknown): WireRecord[] {
  return Array.isArray(value) ? value.map(asRecord) : [];
}

function optionalRecordValue(record: WireRecord, keys: string[]): Record<string, unknown> | undefined {
  const value = valueAt(record, ...keys);
  const normalized = asRecord(value);
  return Object.keys(normalized).length > 0 ? normalized : undefined;
}

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
    userId: optionalStringValue(record, ["userId", "user-id", "user_id"]),
    orgId: stringValue(record, ["orgId", "org-id", "org_id"]),
    actorId: optionalStringValue(record, ["actorId", "actor-id", "actor_id"]),
    orgName: optionalStringValue(record, ["orgName", "org-name", "org_name"]),
    orgSlug: optionalStringValue(record, ["orgSlug", "org-slug", "org_slug"]),
    status: stringValue(record, ["status"], "active"),
    isDefault: optionalBooleanValue(record, ["isDefault", "is-default", "is_default"]),
    createdAt: optionalStringValue(record, ["createdAt", "created-at", "created_at"]),
    updatedAt: optionalStringValue(record, ["updatedAt", "updated-at", "updated_at"]),
    roles: recordArrayValue(valueAt(record, "roles")).map(normalizeRole),
    toolPolicies: normalizeToolPolicies(valueAt(record, "toolPolicies", "tool-policies", "tool_policies")),
  };
}

function normalizeActorCredential(value: unknown): AdminActorCredentialSummary {
  const record = asRecord(value);
  return {
    id: stringValue(record, ["id"]),
    provider: stringValue(record, ["provider"]),
    label: optionalStringValue(record, ["label"]),
    kind: stringValue(record, ["kind"], "credential"),
    accountIdentifier: optionalNullableStringValue(record, ["accountIdentifier", "account-identifier", "account_identifier"]),
    status: stringValue(record, ["status"], "active"),
    configuredFields: stringArrayValue(valueAt(record, "configuredFields", "configured-fields", "configured_fields")),
    createdAt: optionalStringValue(record, ["createdAt", "created-at", "created_at"]),
    updatedAt: optionalStringValue(record, ["updatedAt", "updated-at", "updated_at"]),
  };
}

function normalizeUser(value: unknown): AdminUserSummary {
  const record = asRecord(value);
  return {
    id: stringValue(record, ["id"]),
    email: stringValue(record, ["email"]),
    displayName: stringValue(record, ["displayName", "display-name", "display_name"], stringValue(record, ["email"])),
    authProvider: optionalStringValue(record, ["authProvider", "auth-provider", "auth_provider"]),
    externalSubject: optionalNullableStringValue(record, ["externalSubject", "external-subject", "external_subject"]),
    status: stringValue(record, ["status"], "active"),
    createdAt: optionalStringValue(record, ["createdAt", "created-at", "created_at"]),
    updatedAt: optionalStringValue(record, ["updatedAt", "updated-at", "updated_at"]),
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

export async function createOrgActor(orgId: string, payload: {
  actorId?: string;
  email?: string;
  displayName: string;
  roleSlugs: string[];
  toolPolicies?: AdminToolPolicy[];
}): Promise<{ user: AdminUserSummary | null }> {
  const response = asRecord(await request<unknown>(`/api/admin/orgs/${encodeURIComponent(orgId)}/actors`, {
    method: "POST",
    body: JSON.stringify(payload),
  }));
  const user = valueAt(response, "user");
  return { user: user == null ? null : normalizeUser(user) };
}

export async function updateAdminActor(userId: string, payload: {
  orgId: string;
  actorId?: string;
  email?: string;
  displayName?: string;
  status?: string;
  authProvider?: string;
  externalSubject?: string;
}): Promise<{ user: AdminUserSummary | null }> {
  const response = asRecord(await request<unknown>(`/api/admin/actors/${encodeURIComponent(userId)}`, {
    method: "PATCH",
    body: JSON.stringify(payload),
  }));
  const user = valueAt(response, "user");
  return { user: user == null ? null : normalizeUser(user) };
}

export async function upsertAdminActorCredential(userId: string, provider: string, payload: {
  orgId: string;
  kind: string;
  accountIdentifier?: string;
  credentials: Record<string, string>;
}): Promise<{ credential: unknown }> {
  return request<{ credential: unknown }>(`/api/admin/actors/${encodeURIComponent(userId)}/credentials/${encodeURIComponent(provider)}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function listOrgRoles(orgId: string): Promise<{ roles: AdminRoleSummary[] }> {
  const response = asRecord(await request<unknown>(`/api/admin/orgs/${encodeURIComponent(orgId)}/roles`));
  return { roles: recordArrayValue(valueAt(response, "roles")).map(normalizeRole) };
}

export async function createOrgRole(orgId: string, payload: {
  name: string;
  slug?: string;
  permissionCodes: string[];
  toolPolicies?: AdminToolPolicy[];
}): Promise<{ role: AdminRoleSummary | null }> {
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

export async function createOrgDataLake(orgId: string, payload: {
  name: string;
  slug?: string;
  kind?: string;
  config?: Record<string, unknown>;
}): Promise<{ dataLake: AdminDataLakeSummary }> {
  const response = asRecord(await request<unknown>(`/api/admin/orgs/${encodeURIComponent(orgId)}/data-lakes`, {
    method: "POST",
    body: JSON.stringify(payload),
  }));
  return { dataLake: normalizeDataLake(valueAt(response, "dataLake", "data-lake", "data_lake")) };
}

export interface GraphMonitoringStats {
  ok: boolean;
  stats: {
    nodes: number;
    edges: number;
    embeddings: number;
    layouts: number;
  };
  projectBreakdown: Array<{ project: string; count: number }>;
  recentEmbeddings: Array<{
    nodeId: string;
    model: string | null;
    dimensions: number;
    updatedAt: Date | null;
  }>;
  storageBackend: string;
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

export interface DiscordConfigStatus {
  configured: boolean;
  tokenPreview: string;
}

export interface EventAgentToolPolicy {
  toolId: string;
  effect: "allow" | "deny";
}

export interface EventAgentJobControl {
  id: string;
  name: string;
  enabled: boolean;
  description?: string;
  contractSourceId?: string;
  contractSourceKind?: string;
  contractSourceKey?: string;
  contractHash?: number;
  actorId?: string;
  trigger: {
    kind: string;
    cadenceMinutes: number;
    eventKinds: string[];
  };
  source: {
    kind: string;
    mode: string;
    config: Record<string, unknown>;
  };
  filters: Record<string, unknown>;
  agentSpec: {
    role: string;
    model: string;
    thinkingLevel: string;
    systemPrompt: string;
    taskPrompt: string;
    toolPolicies: EventAgentToolPolicy[];
  };
}

export interface EventAgentRuntimeJob {
  id: string;
  name: string;
  enabled: boolean;
  contractSourceId?: string;
  contractSourceKind?: string;
  contractSourceKey?: string;
  scheduleLabel: string;
  trigger?: {
    kind: string;
    cadenceMinutes?: number;
    eventKinds?: string[];
  };
  source?: {
    kind: string;
    mode?: string;
  };
  running?: boolean;
  runCount?: number;
  lastStartedAt?: number;
  lastFinishedAt?: number;
  lastDurationMs?: number;
  lastStatus?: string;
  lastError?: string;
  nextRunAt?: number;
}

export interface EventAgentControlResponse extends DiscordConfigStatus {
  availableRoles: string[];
  availableSourceKinds: string[];
  availableTriggerKinds: string[];
  control: {
    sources: {
      discord?: {
        botUserId?: string;
        defaultChannels?: string[];
        targetKeywords?: string[];
      };
      github?: Record<string, unknown>;
      cron?: Record<string, unknown>;
      [key: string]: unknown;
    };
    jobs: EventAgentJobControl[];
  };
  runtime: {
    running: boolean;
    configured: boolean;
    sources?: Record<string, unknown>;
    jobs: EventAgentRuntimeJob[];
  };
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

export async function dispatchEventAgentEvent(event: {
  sourceKind: string;
  eventKind: string;
  payload?: Record<string, unknown>;
}): Promise<{ ok: boolean; matchedJobs: string[]; event: Record<string, unknown> }> {
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

export async function resetEventAgentRuntime(): Promise<EventAgentControlResponse & {
  ok: boolean;
  action: string;
  reset: {
    ok: boolean;
    deletedCount: number;
    disabledCronJobCount?: number;
    preservedCronJobCount?: number;
  };
}> {
  return request<EventAgentControlResponse & {
    ok: boolean;
    action: string;
    reset: {
      ok: boolean;
      deletedCount: number;
      disabledCronJobCount?: number;
      preservedCronJobCount?: number;
    };
  }>("/api/admin/config/events/runtime/reset", {
    method: "POST",
  });
}
