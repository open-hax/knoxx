import type {
  ChatRequest,
  FrontendConfig,
  GroundedAnswerResponse,
  LoungeMessage,
  MemorySearchHit,
  MemorySessionListResponse,
  MemorySessionRow,
  MemorySessionSummary,
  ModelInfo,
  RunDetail,
  RunSummary,
  KnoxxAuthContext,
  TranslationLabelPayload,
  TranslationManifest,
  TranslationSegment,
  TranslationSegmentListResponse,
  TranslationStatus,
} from "../types";
import { buildKnoxxAuthHeaders, request } from "./core";

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

export async function listMemorySessions(params: { limit?: number; offset?: number } = {}): Promise<MemorySessionListResponse> {
  const query = new URLSearchParams();
  query.set("limit", String(params.limit ?? 12));
  if (typeof params.offset === "number" && params.offset > 0) {
    query.set("offset", String(params.offset));
  }
  return request<MemorySessionListResponse>(`/api/memory/sessions?${query.toString()}`);
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

export async function fetchDocumentContent(relativePath: string): Promise<{ content: string; path: string }> {
  const encoded = relativePath
    .split("/")
    .filter(Boolean)
    .map((segment) => encodeURIComponent(segment))
    .join("/");
  return request<{ content: string; path: string }>(`/api/documents/content/${encoded}`);
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

export async function listTranslationSegments(params: {
  project: string;
  status?: TranslationStatus | "all";
  target_lang?: string;
  source_lang?: string;
  domain?: string;
  limit?: number;
  offset?: number;
}): Promise<TranslationSegmentListResponse> {
  const query = new URLSearchParams({ project: params.project });
  if (params.status && params.status !== "all") query.set("status", params.status);
  if (params.target_lang) query.set("target_lang", params.target_lang);
  if (params.source_lang) query.set("source_lang", params.source_lang);
  if (params.domain) query.set("domain", params.domain);
  if (typeof params.limit === "number") query.set("limit", String(params.limit));
  if (typeof params.offset === "number") query.set("offset", String(params.offset));
  return request<TranslationSegmentListResponse>(`/api/translations/segments?${query.toString()}`);
}

export async function getTranslationSegment(segmentId: string): Promise<TranslationSegment> {
  return request<TranslationSegment>(`/api/translations/segments/${encodeURIComponent(segmentId)}`);
}

export async function submitTranslationLabel(segmentId: string, payload: TranslationLabelPayload): Promise<{ ok: boolean; label_id: string; new_status: TranslationStatus }> {
  return request<{ ok: boolean; label_id: string; new_status: TranslationStatus }>(`/api/translations/segments/${encodeURIComponent(segmentId)}/labels`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function getTranslationManifest(project: string): Promise<TranslationManifest> {
  return request<TranslationManifest>(`/api/translations/export/manifest?project=${encodeURIComponent(project)}`);
}

export async function getTranslationSftExport(params: {
  project: string;
  targetLang?: string;
  includeCorrected?: boolean;
}): Promise<string> {
  const query = new URLSearchParams({ project: params.project });
  if (params.targetLang) query.set("target_lang", params.targetLang);
  if (typeof params.includeCorrected === "boolean") {
    query.set("include_corrected", String(params.includeCorrected));
  }
  const res = await fetch(`/api/translations/export/sft?${query.toString()}`, {
    headers: buildKnoxxAuthHeaders(),
  });
  if (!res.ok) {
    throw new Error(await res.text() || `Failed to export SFT: ${res.status}`);
  }
  return res.text();
}
