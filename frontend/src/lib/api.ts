import type {
  ChatRequest,
  FrontendConfig,
  LoungeMessage,
  ModelInfo,
  ProxxChatResponse,
  ProxxHealth,
  ProxxModelInfo,
  RunDetail,
  RunSummary,
  ShibbolethHandoffResponse,
} from "./types";

export const API_BASE =
  (import.meta.env.VITE_API_BASE as string | undefined) ?? "";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {})
    },
    ...init
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`${response.status} ${response.statusText}${text ? ` - ${text}` : ""}`);
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
  conversation_id?: string;
  direct?: boolean;
}): Promise<{ answer: string; conversation_id?: string }> {
  const endpoint = payload.direct ? "/api/knoxx/direct" : "/api/knoxx/chat";
  return request(endpoint, {
    method: "POST",
    body: JSON.stringify({
      message: payload.message,
      conversation_id: payload.conversation_id
    })
  });
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
