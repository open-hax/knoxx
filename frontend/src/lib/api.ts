import type {
  AgentSource,
  ChatRequest,
  FrontendConfig,
  GroundedAnswerResponse,
  LoungeMessage,
  MemorySearchHit,
  MemorySessionRow,
  MemorySessionSummary,
  ModelInfo,
  EmailSendResponse,
  ToolBashResponse,
  ToolEditResponse,
  ProxxChatResponse,
  ProxxHealth,
  ProxxModelInfo,
  RunDetail,
  RunSummary,
  ShibbolethHandoffResponse,
  ToolCatalogResponse,
  ToolReadResponse,
  ToolWriteResponse,
  AdminBootstrapContext,
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
} from "./types";

export const API_BASE =
  (import.meta.env.VITE_API_BASE as string | undefined) ?? "";

const KNOXX_USER_EMAIL_KEY = "knoxx_user_email";
const KNOXX_ORG_SLUG_KEY = "knoxx_org_slug";

function getStoredAuthValue(key: string): string | null {
  if (typeof window === "undefined") return null;
  try {
    const value = localStorage.getItem(key)?.trim();
    return value ? value : null;
  } catch {
    return null;
  }
}

export function getKnoxxAuthIdentity(): KnoxxAuthIdentity {
  return {
    userEmail: getStoredAuthValue(KNOXX_USER_EMAIL_KEY) ?? "",
    orgSlug: getStoredAuthValue(KNOXX_ORG_SLUG_KEY) ?? "",
  };
}

export function setKnoxxAuthIdentity(next: KnoxxAuthIdentity): KnoxxAuthIdentity {
  const resolved = {
    userEmail: next.userEmail.trim(),
    orgSlug: next.orgSlug.trim(),
  };

  if (typeof window !== "undefined") {
    try {
      if (resolved.userEmail) {
        localStorage.setItem(KNOXX_USER_EMAIL_KEY, resolved.userEmail);
      } else {
        localStorage.removeItem(KNOXX_USER_EMAIL_KEY);
      }
      if (resolved.orgSlug) {
        localStorage.setItem(KNOXX_ORG_SLUG_KEY, resolved.orgSlug);
      } else {
        localStorage.removeItem(KNOXX_ORG_SLUG_KEY);
      }
    } catch {
      // ignore storage failures and still return the resolved identity
    }
  }

  return resolved;
}

export function buildKnoxxAuthHeaders(headersInit?: HeadersInit): Headers {
  const headers = new Headers(headersInit || {});
  const userEmail = getStoredAuthValue(KNOXX_USER_EMAIL_KEY);
  const orgSlug = getStoredAuthValue(KNOXX_ORG_SLUG_KEY);
  if (userEmail && !headers.has("x-knoxx-user-email")) {
    headers.set("x-knoxx-user-email", userEmail);
  }
  if (orgSlug && !headers.has("x-knoxx-org-slug")) {
    headers.set("x-knoxx-org-slug", orgSlug);
  }
  return headers;
}

function summarizeErrorPayload(payload: unknown): string | null {
  if (!payload || typeof payload !== "object") return null;

  const record = payload as Record<string, unknown>;
  const parts = [
    typeof record.error === "string" ? record.error : null,
    typeof record.detail === "string" ? record.detail : null,
    typeof record.message === "string" ? record.message : null,
    typeof record.error_code === "string" ? `code=${record.error_code}` : null,
    typeof record.model_error === "string" ? record.model_error : null,
  ].filter(Boolean);

  return parts.length > 0 ? parts.join(" | ") : null;
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;

  try {
    response = await fetch(`${API_BASE}${path}`, {
      headers: {
        ...Object.fromEntries(buildKnoxxAuthHeaders({
          "Content-Type": "application/json",
          ...(init?.headers ?? {})
        }).entries())
      },
      ...init
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    throw new Error(`Request to ${path} failed before the server responded. This usually means the reverse proxy or upstream service reset the connection. ${message}`);
  }

  if (!response.ok) {
    const text = await response.text();
    let detail = text;

    try {
      detail = summarizeErrorPayload(JSON.parse(text)) ?? text;
    } catch {
      // leave detail as raw text
    }

    throw new Error(`${response.status} ${response.statusText}${detail ? ` - ${detail}` : ""}`);
  }

  return (await response.json()) as T;
}

export async function listModels(): Promise<ModelInfo[]> {
  const data = await request<{ models: ModelInfo[] }>("/api/models");
  return data.models;
}

export async function createChatRun(payload: ChatRequest) {
  return request("/api/chat", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function listRuns(limit = 100): Promise<RunSummary[]> {
  const data = await request<{ runs: RunSummary[] }>(`/api/runs?limit=${limit}`);
  return data.runs;
}

export async function getRun(runId: string): Promise<RunDetail> {
  return request<RunDetail>(`/api/runs/${runId}`);
}

export async function listMemorySessions(limit = 12): Promise<MemorySessionSummary[]> {
  const data = await request<{ rows: MemorySessionSummary[] }>(`/api/memory/sessions?limit=${limit}`);
  return data.rows;
}

export async function getMemorySession(sessionId: string): Promise<{ session: string; rows: MemorySessionRow[] }> {
  return request<{ session: string; rows: MemorySessionRow[] }>(`/api/memory/sessions/${encodeURIComponent(sessionId)}`);
}

export async function searchMemory(payload: { query: string; k?: number; sessionId?: string }): Promise<{ query: string; mode: string; hits: MemorySearchHit[] }> {
  return request<{ query: string; mode: string; hits: MemorySearchHit[] }>("/api/memory/search", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function listLoungeMessages(): Promise<LoungeMessage[]> {
  const data = await request<{ messages: LoungeMessage[] }>("/api/lounge/messages");
  return data.messages;
}

export async function postLoungeMessage(payload: {
  session_id: string;
  alias?: string;
  text: string;
}): Promise<{ ok: boolean; message: LoungeMessage }> {
  return request<{ ok: boolean; message: LoungeMessage }>("/api/lounge/messages", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function getFrontendConfig(): Promise<FrontendConfig> {
  return request<FrontendConfig>("/api/config");
}

export async function getKnoxxAuthContext(): Promise<KnoxxAuthContext> {
  return request<KnoxxAuthContext>("/api/auth/context");
}

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

export async function listProxxModels(): Promise<ProxxModelInfo[]> {
  const data = await request<{ models: ProxxModelInfo[] }>("/api/proxx/models");
  return data.models;
}

export async function proxxHealth(): Promise<ProxxHealth> {
  return request<ProxxHealth>("/api/proxx/health");
}

export async function proxxChat(payload: {
  model?: string;
  system_prompt?: string;
  messages: Array<{ role: string; content: string }>;
  temperature?: number;
  top_p?: number;
  max_tokens?: number;
  stop?: string[];
  rag_enabled?: boolean;
  rag_collection?: string;
  rag_limit?: number;
  rag_threshold?: number;
}): Promise<ProxxChatResponse> {
  return request<ProxxChatResponse>("/api/proxx/chat", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function queryAnswer(payload: {
  q: string;
  role?: string;
  projects?: string[];
  kinds?: string[];
  limit?: number;
  tenant_id?: string;
  model?: string;
  system_prompt?: string;
}): Promise<GroundedAnswerResponse> {
  return request<GroundedAnswerResponse>("/api/query/answer", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function getToolCatalog(role?: string): Promise<ToolCatalogResponse> {
  const suffix = role ? `?role=${encodeURIComponent(role)}` : "";
  return request<ToolCatalogResponse>(`/api/tools/catalog${suffix}`);
}

export async function sendEmailDraft(payload: {
  role: string;
  to: string[];
  cc?: string[];
  bcc?: string[];
  subject: string;
  markdown: string;
}): Promise<EmailSendResponse> {
  return request<EmailSendResponse>("/api/tools/email/send", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function toolRead(payload: {
  role: string;
  path: string;
  offset?: number;
  limit?: number;
}): Promise<ToolReadResponse> {
  return request<ToolReadResponse>("/api/tools/read", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function toolWrite(payload: {
  role: string;
  path: string;
  content: string;
  create_parents?: boolean;
  overwrite?: boolean;
}): Promise<ToolWriteResponse> {
  return request<ToolWriteResponse>("/api/tools/write", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function toolEdit(payload: {
  role: string;
  path: string;
  old_string: string;
  new_string: string;
  replace_all?: boolean;
}): Promise<ToolEditResponse> {
  return request<ToolEditResponse>("/api/tools/edit", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function toolBash(payload: {
  role: string;
  command: string;
  workdir?: string;
  timeout_ms?: number;
}): Promise<ToolBashResponse> {
  return request<ToolBashResponse>("/api/tools/bash", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export async function knoxxHealth(): Promise<{
  reachable: boolean;
  configured: boolean;
  base_url: string;
  status_code?: number;
}> {
  return request("/api/knoxx/health");
}

export async function knoxxChat(payload: {
  message: string;
  conversation_id?: string | null;
  session_id?: string | null;
  model?: string;
  direct?: boolean;
}): Promise<{ answer: string; run_id?: string | null; conversation_id?: string | null; session_id?: string | null; model?: string | null; sources?: AgentSource[]; compare?: unknown }> {
  const endpoint = payload.direct ? "/api/knoxx/direct" : "/api/knoxx/chat";
  return request<Record<string, unknown>>(endpoint, {
    method: "POST",
    body: JSON.stringify({
      message: payload.message,
      conversation_id: payload.conversation_id,
      session_id: payload.session_id,
      model: payload.model,
    })
  }).then((response) => ({
    answer: typeof response.answer === "string" ? response.answer : "",
    run_id:
      typeof response.run_id === "string"
        ? response.run_id
        : typeof response.runId === "string"
          ? response.runId
          : null,
    conversation_id:
      typeof response.conversation_id === "string"
        ? response.conversation_id
        : typeof response.conversationId === "string"
          ? response.conversationId
          : null,
    session_id:
      typeof response.session_id === "string"
        ? response.session_id
        : typeof response.sessionId === "string"
          ? response.sessionId
          : null,
    model: typeof response.model === "string" ? response.model : null,
    sources: Array.isArray(response.sources) ? (response.sources as AgentSource[]) : [],
    compare: response.compare,
  }));
}

export async function knoxxControl(payload: {
  kind: "steer" | "follow_up";
  message: string;
  conversation_id: string;
  session_id?: string | null;
  run_id?: string | null;
}): Promise<{ ok: boolean; conversation_id?: string | null; session_id?: string | null; run_id?: string | null; kind?: string | null }> {
  const endpoint = payload.kind === "follow_up" ? "/api/knoxx/follow-up" : "/api/knoxx/steer";
  return request<Record<string, unknown>>(endpoint, {
    method: "POST",
    body: JSON.stringify({
      message: payload.message,
      conversation_id: payload.conversation_id,
      session_id: payload.session_id,
      run_id: payload.run_id,
    })
  }).then((response) => ({
    ok: Boolean(response.ok),
    conversation_id: typeof response.conversation_id === "string" ? response.conversation_id : null,
    session_id: typeof response.session_id === "string" ? response.session_id : null,
    run_id: typeof response.run_id === "string" ? response.run_id : null,
    kind: typeof response.kind === "string" ? response.kind : null,
  }));
}

export async function knoxxChatStart(payload: {
  message: string;
  conversation_id?: string | null;
  session_id?: string | null;
  run_id?: string | null;
  model?: string;
  direct?: boolean;
}): Promise<{ ok: boolean; queued: boolean; run_id?: string | null; conversation_id?: string | null; session_id?: string | null; model?: string | null }> {
  const endpoint = payload.direct ? "/api/knoxx/direct/start" : "/api/knoxx/chat/start";
  return request<Record<string, unknown>>(endpoint, {
    method: "POST",
    body: JSON.stringify({
      message: payload.message,
      conversation_id: payload.conversation_id,
      session_id: payload.session_id,
      run_id: payload.run_id,
      model: payload.model,
    })
  }).then((response) => ({
    ok: Boolean(response.ok),
    queued: Boolean(response.queued),
    run_id: typeof response.run_id === "string" ? response.run_id : null,
    conversation_id:
      typeof response.conversation_id === "string"
        ? response.conversation_id
        : typeof response.conversationId === "string"
          ? response.conversationId
          : null,
    session_id:
      typeof response.session_id === "string"
        ? response.session_id
        : typeof response.sessionId === "string"
          ? response.sessionId
          : null,
    model: typeof response.model === "string" ? response.model : null,
  }));
}

export async function getSessionStatus(sessionId: string, conversationId?: string | null): Promise<{
  session_id: string;
  conversation_id?: string | null;
  status: "running" | "completed" | "failed" | "waiting_input" | "not_found" | "unknown";
  has_active_stream: boolean;
  can_send: boolean;
  reason?: string | null;
  model?: string | null;
  updated_at?: string | null;
}> {
  const params = new URLSearchParams({ session_id: sessionId });
  if (conversationId) params.set("conversation_id", conversationId);
  return request(`/api/knoxx/session/status?${params.toString()}`);
}

export async function handoffToShibboleth(payload: {
  model?: string;
  system_prompt?: string;
  provider?: string;
  conversation_id?: string | null;
  fake_tools_enabled?: boolean;
  items: Array<{ role: "user" | "assistant"; content: string; metadata?: Record<string, unknown> }>;
}): Promise<ShibbolethHandoffResponse> {
  return request<ShibbolethHandoffResponse>("/api/shibboleth/handoff", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}
