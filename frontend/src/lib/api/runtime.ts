import type {
  AgentSource,
  EmailSendResponse,
  ProxxChatResponse,
  ProxxHealth,
  ProxxModelInfo,
  RunEvent,
  ShibbolethHandoffResponse,
  ToolBashResponse,
  ToolCatalogResponse,
  ToolEditResponse,
  ToolReadResponse,
  ToolWriteResponse,
} from "../types";
import { request } from "./core";

function normalizeConversationResponse(response: Record<string, unknown>) {
  return {
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
  };
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
    body: JSON.stringify(payload),
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
    body: JSON.stringify(payload),
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
    body: JSON.stringify(payload),
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
    body: JSON.stringify(payload),
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
    body: JSON.stringify(payload),
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
    body: JSON.stringify(payload),
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
    }),
  }).then((response) => ({
    ...normalizeConversationResponse(response),
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
    }),
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
    }),
  }).then((response) => ({
    ok: Boolean(response.ok),
    queued: Boolean(response.queued),
    ...normalizeConversationResponse(response),
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

export async function getRunEvents(runId: string, since?: string | null): Promise<{
  run_id: string;
  events: RunEvent[];
  count: number;
}> {
  const params = new URLSearchParams();
  if (since) params.set("since", since);
  const qs = params.toString();
  return request(`/api/knoxx/run/${encodeURIComponent(runId)}/events${qs ? `?${qs}` : ""}`);
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
    body: JSON.stringify(payload),
  });
}
