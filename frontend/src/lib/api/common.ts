import type {
  ChatRequest,
  ContentPart,
  FrontendConfig,
  GroundedAnswerResponse,
  LoungeMessage,
  MemorySearchHit,
  MemorySessionListResponse,
  MemorySessionRow,
  MemorySessionSummary,
  ModelInfo,
  ActiveAgentSummary,
  RunDetail,
  RunSummary,
  KnoxxAuthContext,
  TranslationLabelPayload,
  TranslationManifest,
  TranslationSegment,
  TranslationSegmentListResponse,
  TranslationStatus,
  TranslationDocumentSummary,
  TranslationDocumentDetail,
  TranslationDocumentReviewPayload,
  TranslationBatchSummary,
} from "../types";
import { buildKnoxxAuthHeaders, request } from "./core";

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function asString(value: unknown): string | undefined {
  return typeof value === "string" ? value : undefined;
}

function asNumber(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value) ? value : undefined;
}

function pickKey(record: Record<string, unknown>, keys: string[]): unknown {
  for (const key of keys) {
    if (record[key] !== undefined) {
      return record[key];
    }
  }
  return undefined;
}

function normalizeContentPart(part: unknown): ContentPart | null {
  if (typeof part === "string") {
    return { type: "text", text: part };
  }

  if (!isRecord(part)) {
    return null;
  }

  const rawType = asString(pickKey(part, ["type", "partType", "part_type", "part-type"]))?.toLowerCase();
  const filename = asString(pickKey(part, ["filename", "fileName", "file_name", "file-name", "name"]));
  const size = asNumber(pickKey(part, ["size", "bytes", "byteSize", "byte_size", "byte-size"]));

  const imageUrlValue = pickKey(part, ["image_url", "imageUrl"]);
  const videoUrlValue = pickKey(part, ["video_url", "videoUrl"]);
  const audioUrlValue = pickKey(part, ["audio_url", "audioUrl"]);
  const source = isRecord(part.source) ? part.source : null;
  const inputAudio = isRecord(part.input_audio) ? part.input_audio : null;
  const outputAudio = isRecord(part.output_audio) ? part.output_audio : null;

  const url = asString(pickKey(part, ["url", "file_url", "fileUrl"]))
    ?? (isRecord(imageUrlValue) ? asString(imageUrlValue.url) : asString(imageUrlValue))
    ?? (isRecord(videoUrlValue) ? asString(videoUrlValue.url) : asString(videoUrlValue))
    ?? (isRecord(audioUrlValue) ? asString(audioUrlValue.url) : asString(audioUrlValue))
    ?? (source && typeof source.type === "string" && source.type.toLowerCase() === "url" ? asString(source.url) : undefined);

  const rawData = asString(pickKey(part, ["data", "b64_json", "result"]))
    ?? asString(inputAudio?.data)
    ?? asString(outputAudio?.data)
    ?? (source && typeof source.type === "string" && source.type.toLowerCase() === "base64" ? asString(source.data) : undefined);

  const mimeType = asString(pickKey(part, ["mimeType", "mime_type", "mime-type", "mediaType", "media_type", "media-type"]))
    ?? asString(source?.media_type)
    ?? asString(source?.mime_type)
    ?? (typeof inputAudio?.format === "string" ? `audio/${inputAudio.format}` : undefined)
    ?? (typeof outputAudio?.format === "string" ? `audio/${outputAudio.format}` : undefined);

  const type = (() => {
    if (!rawType && typeof mimeType === "string") {
      if (mimeType.startsWith("image/")) return "image" as const;
      if (mimeType.startsWith("audio/")) return "audio" as const;
      if (mimeType.startsWith("video/")) return "video" as const;
      return "document" as const;
    }

    switch (rawType) {
      case "text":
      case "input_text":
      case "output_text":
      case "refusal":
      case "summary_text":
        return "text" as const;
      case "image":
      case "image_url":
      case "input_image":
      case "output_image":
        return "image" as const;
      case "audio":
      case "audio_url":
      case "input_audio":
      case "output_audio":
        return "audio" as const;
      case "video":
      case "video_url":
      case "input_video":
      case "output_video":
        return "video" as const;
      case "document":
      case "file":
      case "input_file":
      case "output_file":
        return "document" as const;
      default:
        return null;
    }
  })();

  if (type === "text") {
    const text = asString(pickKey(part, ["text", "refusal", "content"]));
    return text !== undefined ? { type, text, filename, size } : null;
  }

  if (!type) {
    return null;
  }

  const data = rawData
    ? rawData.startsWith("data:")
      ? rawData
      : mimeType
        ? `data:${mimeType};base64,${rawData}`
        : rawData
    : undefined;

  if (!url && !data) {
    return null;
  }

  return {
    type,
    url,
    data,
    mimeType,
    filename,
    size,
  };
}

function normalizeContentParts(value: unknown): ContentPart[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  const parts = value
    .map((part) => normalizeContentPart(part))
    .filter((part): part is ContentPart => part !== null)
    .filter((part) => part.type !== "text");

  return parts;
}

function mergeContentParts(...groups: Array<ContentPart[] | undefined>): ContentPart[] | undefined {
  const merged: ContentPart[] = [];
  const seen = new Set<string>();

  for (const group of groups) {
    if (!Array.isArray(group)) {
      continue;
    }

    for (const part of group) {
      const key = JSON.stringify([
        part.type,
        part.url ?? null,
        part.data ?? null,
        part.mimeType ?? null,
        part.filename ?? null,
        part.size ?? null,
      ]);

      if (seen.has(key)) {
        continue;
      }

      seen.add(key);
      merged.push(part);
    }
  }

  return merged.length > 0 ? merged : undefined;
}

function normalizeRequestMessages(value: unknown): RunDetail["request_messages"] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .filter(isRecord)
    .map((message) => ({
      role: asString(message.role) ?? "user",
      content: asString(message.content) ?? "",
      contentParts: normalizeContentParts(
        pickKey(message, ["contentParts", "content_parts", "content-parts"]),
      ),
    }));
}

function normalizeToolReceipt(value: unknown): import("../types").ToolReceipt | null {
  if (!isRecord(value)) {
    return null;
  }

  return {
    ...value,
    id: asString(value.id) ?? "tool-receipt",
    contentParts: normalizeContentParts(
      pickKey(value, ["contentParts", "content_parts", "content-parts"]),
    ),
  };
}

function normalizeRunDetail(run: RunDetail): RunDetail {
  const toolReceipts = Array.isArray(run.tool_receipts)
    ? run.tool_receipts
        .map((receipt) => normalizeToolReceipt(receipt))
        .filter((receipt): receipt is import("../types").ToolReceipt => receipt !== null)
    : [];

  const replyAttachmentParts = toolReceipts
    .filter((receipt) => receipt.tool_name === "workspace_media.attach")
    .flatMap((receipt) => receipt.contentParts ?? []);

  return {
    ...run,
    answer: typeof run.answer === "string" ? run.answer : null,
    contentParts: mergeContentParts(
      normalizeContentParts(
        pickKey(run as unknown as Record<string, unknown>, ["contentParts", "content_parts", "content-parts"]),
      ),
      replyAttachmentParts,
    ),
    request_messages: normalizeRequestMessages(run.request_messages),
    events: Array.isArray(run.events) ? run.events : [],
    tool_receipts: toolReceipts,
    sources: Array.isArray(run.sources) ? run.sources : [],
  };
}

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
  const run = await request<RunDetail>(`/api/runs/${runId}`);
  return normalizeRunDetail(run);
}

export async function listActiveAgents(limit = 25): Promise<ActiveAgentSummary[]> {
  const data = await request<{ runs: ActiveAgentSummary[] }>(`/api/knoxx/agents/active?limit=${limit}`);
  return data.runs;
}

export async function listAdminActiveAgents(limit = 200): Promise<ActiveAgentSummary[]> {
  const data = await request<{ runs: ActiveAgentSummary[] }>(`/api/admin/agents/active?limit=${limit}`);
  return data.runs;
}

export async function abortAdminActiveAgent(payload: {
  conversation_id?: string | null;
  conversationId?: string | null;
  session_id?: string | null;
  sessionId?: string | null;
  run_id?: string | null;
  runId?: string | null;
  reason?: string;
}): Promise<{ ok: boolean; conversation_id?: string; session_id?: string; run_id?: string; error?: string; marked_aborted?: boolean }> {
  return request<{ ok: boolean; conversation_id?: string; session_id?: string; run_id?: string; error?: string; marked_aborted?: boolean }>("/api/admin/agents/abort", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function listMemorySessions(params: { limit?: number; offset?: number; actorId?: string; excludeActorIds?: string[] } = {}): Promise<MemorySessionListResponse> {
  const query = new URLSearchParams();
  query.set("limit", String(params.limit ?? 12));
  if (typeof params.offset === "number" && params.offset > 0) {
    query.set("offset", String(params.offset));
  }
  if (params.actorId) {
    query.set("actorId", params.actorId);
  }
  if (params.excludeActorIds && params.excludeActorIds.length > 0) {
    query.set("excludeActorIds", params.excludeActorIds.join(","));
  }
  return request<MemorySessionListResponse>(`/api/memory/sessions?${query.toString()}`);
}

export async function getMemorySession(sessionId: string): Promise<{ session: string; rows: MemorySessionRow[] }> {
  return request<{ session: string; rows: MemorySessionRow[] }>(`/api/memory/sessions/${encodeURIComponent(sessionId)}`);
}

export async function listAgentHistorySessions(params: { limit?: number; offset?: number } = {}): Promise<MemorySessionListResponse> {
  const query = new URLSearchParams();
  query.set("project", "knoxx-session");
  query.set("limit", String(params.limit ?? 50));
  if (typeof params.offset === "number" && params.offset > 0) {
    query.set("offset", String(params.offset));
  }
  return request<MemorySessionListResponse>(`/api/openplanner/v1/sessions?${query.toString()}`);
}

export async function getAgentHistorySession(sessionId: string): Promise<{ session: string; rows: MemorySessionRow[] }> {
  return request<{ session: string; rows: MemorySessionRow[] }>(`/api/openplanner/v1/sessions/${encodeURIComponent(sessionId)}?project=knoxx-session&mode=full`);
}

export async function searchMemory(payload: { query: string; k?: number; sessionId?: string; actorId?: string; excludeActorIds?: string[] }): Promise<{ query: string; mode: string; hits: MemorySearchHit[] }> {
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

export async function listTranslationDocuments(params: {
  project: string;
  target_lang?: string;
  source_lang?: string;
  garden_id?: string;
}): Promise<{ documents: TranslationDocumentSummary[]; total: number }> {
  const query = new URLSearchParams({ project: params.project });
  if (params.target_lang) query.set("target_lang", params.target_lang);
  if (params.source_lang) query.set("source_lang", params.source_lang);
  if (params.garden_id) query.set("garden_id", params.garden_id);
  return request<{ documents: TranslationDocumentSummary[]; total: number }>(`/api/translations/documents?${query.toString()}`);
}

export async function getTranslationDocument(documentId: string, targetLang: string): Promise<TranslationDocumentDetail> {
  return request<TranslationDocumentDetail>(`/api/translations/documents/${encodeURIComponent(documentId)}/${encodeURIComponent(targetLang)}`);
}

export async function reviewTranslationDocument(
  documentId: string,
  targetLang: string,
  payload: TranslationDocumentReviewPayload,
): Promise<{ ok: boolean; segments_reviewed: number; overall: string; overrides_applied: number }> {
  return request<{ ok: boolean; segments_reviewed: number; overall: string; overrides_applied: number }>(
    `/api/translations/documents/${encodeURIComponent(documentId)}/${encodeURIComponent(targetLang)}/review`,
    { method: "POST", body: JSON.stringify(payload) },
  );
}

export async function listTranslationBatches(params?: {
  status?: string;
  garden_id?: string;
  target_lang?: string;
}): Promise<{ batches: TranslationBatchSummary[] }> {
  const query = new URLSearchParams();
  if (params?.status) query.set("status", params.status);
  if (params?.garden_id) query.set("garden_id", params.garden_id);
  if (params?.target_lang) query.set("target_lang", params.target_lang);
  const qs = query.toString();
  return request<{ batches: TranslationBatchSummary[] }>(`/api/translations/batches${qs ? `?${qs}` : ""}`);
}

export async function createTranslationBatch(payload: {
  garden_id: string;
  target_lang: string;
  document_ids: string[];
  source_lang?: string;
  project?: string;
}): Promise<{ ok: boolean; batch_id: string; status: string; document_ids: string[] }> {
  return request<{ ok: boolean; batch_id: string; status: string; document_ids: string[] }>("/api/translations/batches", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
