import type {
  ChatRequest,
  FrontendConfig,
  GroundedAnswerResponse,
  LoungeMessage,
  MemorySearchHit,
  MemorySessionRow,
  MemorySessionSummary,
  ModelInfo,
  RunDetail,
  RunSummary,
  KnoxxAuthContext,
} from "../types";
import { request } from "./core";

export async function listModels(): Promise<ModelInfo[]> {
  const data = await request<{ models: ModelInfo[] }>("/api/models");
  return data.models;
}

export async function createChatRun(payload: ChatRequest) {
  return request("/api/chat", {
    method: "POST",
    body: JSON.stringify(payload),
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
    body: JSON.stringify(payload),
  });
}

export async function getFrontendConfig(): Promise<FrontendConfig> {
  return request<FrontendConfig>("/api/config");
}

export async function getKnoxxAuthContext(): Promise<KnoxxAuthContext> {
  return request<KnoxxAuthContext>("/api/auth/context");
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
    body: JSON.stringify(payload),
  });
}
