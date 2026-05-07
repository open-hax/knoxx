import type {
  ActorMailboxBox,
  ActorMailboxEntry,
  ActorMailboxListResponse,
  ActorMailboxStatus,
  AgentContractCatalogResponse,
  AgentSource,
  ContentPart,
  EmailSendResponse,
  ProxxChatResponse,
  ProxxHealth,
  ProxxModelInfo,
  RunEvent,
  ShibbolethHandoffResponse,
  SttTranscribeResponse,
  ToolBashResponse,
  ToolCatalogResponse,
  ToolEditResponse,
  ToolReadResponse,
  ToolWriteResponse,
} from "../types";
import { API_BASE, buildKnoxxAuthHeaders, request } from "./core";

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function asString(value: unknown): string | undefined {
  return typeof value === "string" ? value : undefined;
}

function asBoolean(value: unknown): boolean | undefined {
  return typeof value === "boolean" ? value : undefined;
}

function asRecord(value: unknown): Record<string, unknown> {
  return isRecord(value) ? value : {};
}

function normalizeMailboxEntry(value: unknown): ActorMailboxEntry | null {
  if (!isRecord(value)) {
    return null;
  }

  const id = asString(value.id);
  if (!id) {
    return null;
  }

  return {
    id,
    kind: asString(value.kind) ?? "actor-message",
    status: asString(value.status) ?? "pending",
    source: asRecord(value.source),
    target: asRecord(value.target),
    delivery: asRecord(value.delivery),
    contentRef: asRecord(value.contentRef),
    metadata: asRecord(value.metadata),
    preview: asString(value.preview),
    lastError: asString(value.lastError),
    createdAt: asString(value.createdAt),
    updatedAt: asString(value.updatedAt),
    deliveredAt: asString(value.deliveredAt),
    acknowledgedAt: asString(value.acknowledgedAt),
    expiresAt: asString(value.expiresAt),
  };
}

function normalizeMailboxListResponse(value: unknown, fallbackBox: ActorMailboxBox): ActorMailboxListResponse {
  const record = isRecord(value) ? value : {};
  const entries = Array.isArray(record.entries)
    ? record.entries.map(normalizeMailboxEntry).filter((entry): entry is ActorMailboxEntry => entry !== null)
    : [];

  return {
    ok: asBoolean(record.ok) ?? true,
    box: (asString(record.box) === "outbox" ? "outbox" : asString(record.box) === "inbox" ? "inbox" : fallbackBox),
    actorId: asString(record.actorId),
    durable: asBoolean(record.durable) ?? asBoolean(record.durable_),
    entries,
  };
}

function normalizeStringArray(value: unknown): string[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  return value.filter((entry): entry is string => typeof entry === "string");
}

function normalizeToolDefinition(value: unknown, fallbackId?: string) {
  if (!isRecord(value)) {
    return typeof fallbackId === "string"
      ? {
          id: fallbackId,
          label: fallbackId,
          description: "",
          enabled: true,
        }
      : null;
  }

  const id = asString(value.id) ?? fallbackId;
  if (!id) {
    return null;
  }

  return {
    id,
    label: asString(value.label) ?? id,
    description: asString(value.description) ?? "",
    enabled: asBoolean(value.enabled) ?? true,
  };
}

function normalizeToolDefinitions(value: unknown): ToolCatalogResponse["tools"] {
  if (Array.isArray(value)) {
    return value
      .map((entry) => normalizeToolDefinition(entry))
      .filter((entry): entry is ToolCatalogResponse["tools"][number] => entry !== null);
  }

  if (!isRecord(value)) {
    return [];
  }

  return Object.entries(value)
    .map(([fallbackId, entry]) => normalizeToolDefinition(entry, fallbackId))
    .filter((entry): entry is ToolCatalogResponse["tools"][number] => entry !== null);
}

function normalizeToolCatalogResponse(value: unknown): ToolCatalogResponse {
  const record = isRecord(value) ? value : {};

  return {
    role: asString(record.role) ?? "",
    actor_id: asString(record.actor_id) ?? asString(record.actorId) ?? null,
    agent_id: asString(record.agent_id) ?? asString(record.agentId) ?? null,
    agent_label: asString(record.agent_label) ?? asString(record.agentLabel) ?? null,
    agent_trigger_kind: asString(record.agent_trigger_kind) ?? asString(record.agentTriggerKind) ?? null,
    role_slugs: normalizeStringArray(record.role_slugs) ?? normalizeStringArray(record.roleSlugs),
    capability_ids: normalizeStringArray(record.capability_ids) ?? normalizeStringArray(record.capabilityIds),
    system_prompt: asString(record.system_prompt) ?? asString(record.systemPrompt) ?? null,
    actor_system_prompt: asString(record.actor_system_prompt) ?? asString(record.actorSystemPrompt) ?? null,
    agent_system_prompt: asString(record.agent_system_prompt) ?? asString(record.agentSystemPrompt) ?? null,
    task_prompt: asString(record.task_prompt) ?? asString(record.taskPrompt) ?? null,
    tools: normalizeToolDefinitions(record.tools),
    email_enabled: asBoolean(record.email_enabled) ?? asBoolean(record.emailEnabled) ?? false,
  };
}

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
  return data.models.sort((a, b) => a.id.localeCompare(b.id));
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

export async function getToolCatalog(role?: string, agentContractId?: string, actorId?: string): Promise<ToolCatalogResponse> {
  const params = new URLSearchParams();
  if (role) params.set("role", role);
  if (agentContractId) params.set("agent", agentContractId);
  if (actorId) params.set("actor", actorId);
  const suffix = params.toString();
  const response = await request<unknown>(`/api/tools/catalog${suffix ? `?${suffix}` : ""}`);
  return normalizeToolCatalogResponse(response);
}

export async function getAgentContractsCatalog(actorId?: string): Promise<AgentContractCatalogResponse> {
  const params = new URLSearchParams();
  if (actorId) params.set("actor", actorId);
  return request<AgentContractCatalogResponse>(`/api/knoxx/agents/catalog${params.toString() ? `?${params.toString()}` : ""}`);
}

export async function listActorMailbox(box: ActorMailboxBox, status?: ActorMailboxStatus | "all"): Promise<ActorMailboxListResponse> {
  const params = new URLSearchParams();
  params.set("box", box);
  params.set("limit", "100");
  if (status && status !== "all") {
    params.set("status", status);
  }
  const response = await request<unknown>(`/api/actors/mailbox?${params.toString()}`);
  return normalizeMailboxListResponse(response, box);
}

export async function acknowledgeActorMailboxEntry(mailboxId: string): Promise<{ ok: boolean; entry?: ActorMailboxEntry }> {
  const response = await request<unknown>(`/api/actors/mailbox/${encodeURIComponent(mailboxId)}/ack`, { method: "POST" });
  const record = isRecord(response) ? response : {};
  const entry = normalizeMailboxEntry(record.entry);
  return { ok: asBoolean(record.ok) ?? true, ...(entry ? { entry } : {}) };
}

export async function voiceSttTranscribe(blob: Blob, filename = "audio.webm"): Promise<SttTranscribeResponse> {
  const formData = new FormData();
  formData.append("file", blob, filename);

  const response = await fetch(`${API_BASE}/api/voice/stt`, {
    method: "POST",
    headers: buildKnoxxAuthHeaders(),
    body: formData,
  });

  if (!response.ok) {
    const detail = await response.text();
    throw new Error(`${response.status} ${response.statusText}${detail ? ` - ${detail}` : ""}`);
  }

  return (await response.json()) as SttTranscribeResponse;
}

export async function voiceTtsSynthesize(payload: {
  text: string;
  voice_id?: string;
  model_id?: string;
  output_format?: string;
  postprocess_profile?: string;
  postprocess_enabled?: boolean;
  prompt_aware?: boolean;
  prompt_aware_style?: string;
  voice_settings?: Record<string, unknown>;
}): Promise<Blob> {
  const response = await fetch(`${API_BASE}/api/voice/tts`, {
    method: "POST",
    headers: {
      ...buildKnoxxAuthHeaders(),
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const detail = await response.text();
    throw new Error(`${response.status} ${response.statusText}${detail ? ` - ${detail}` : ""}`);
  }

  return await response.blob();
}

export async function sendEmailDraft(payload: {
  role: string;
  agentContractId?: string;
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
  agentContractId?: string;
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
  agentContractId?: string;
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
  agentContractId?: string;
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
  agentContractId?: string;
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
  thinkingLevel?: string;
  direct?: boolean;
  contentParts?: ContentPart[];
  agentSpec?: Record<string, unknown>;
}): Promise<{ answer: string; run_id?: string | null; conversation_id?: string | null; session_id?: string | null; model?: string | null; sources?: AgentSource[]; compare?: unknown }> {
  const endpoint = payload.direct ? "/api/knoxx/direct" : "/api/knoxx/chat";
  return request<Record<string, unknown>>(endpoint, {
    method: "POST",
    body: JSON.stringify({
      message: payload.message,
      conversation_id: payload.conversation_id,
      session_id: payload.session_id,
      model: payload.model,
      thinkingLevel: payload.thinkingLevel,
      contentParts: payload.contentParts,
      agentSpec: payload.agentSpec,
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

export async function knoxxAbort(payload: {
  conversation_id: string;
  session_id?: string | null;
  run_id?: string | null;
  reason?: string;
}): Promise<{ ok: boolean; conversation_id?: string | null; session_id?: string | null; run_id?: string | null; error?: string | null }> {
  return request<Record<string, unknown>>("/api/knoxx/abort", {
    method: "POST",
    body: JSON.stringify({
      conversation_id: payload.conversation_id,
      session_id: payload.session_id,
      run_id: payload.run_id,
      reason: payload.reason,
    }),
  }).then((response) => ({
    ok: Boolean(response.ok),
    conversation_id: typeof response.conversation_id === "string" ? response.conversation_id : null,
    session_id: typeof response.session_id === "string" ? response.session_id : null,
    run_id: typeof response.run_id === "string" ? response.run_id : null,
    error: typeof response.error === "string" ? response.error : null,
  }));
}

export async function knoxxUndoSessionTurn(payload: {
  session_id: string;
  conversation_id?: string | null;
  turns?: number;
}): Promise<{ ok: boolean; session_id?: string | null; conversation_id?: string | null; removed_count?: number; remaining_messages?: number; error?: string | null }> {
  return request<Record<string, unknown>>("/api/knoxx/session/undo", {
    method: "POST",
    body: JSON.stringify({
      session_id: payload.session_id,
      conversation_id: payload.conversation_id,
      turns: payload.turns,
    }),
  }).then((response) => ({
    ok: Boolean(response.ok),
    session_id: typeof response.session_id === "string" ? response.session_id : null,
    conversation_id: typeof response.conversation_id === "string" ? response.conversation_id : null,
    removed_count: typeof response.removed_count === "number" ? response.removed_count : undefined,
    remaining_messages: typeof response.remaining_messages === "number" ? response.remaining_messages : undefined,
    error: typeof response.error === "string" ? response.error : null,
  }));
}

export async function knoxxChatStart(payload: {
  message: string;
  conversation_id?: string | null;
  session_id?: string | null;
  run_id?: string | null;
  model?: string;
  thinkingLevel?: string;
  direct?: boolean;
  contentParts?: ContentPart[];
  agentSpec?: Record<string, unknown>;
  templateContext?: Record<string, unknown>;
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
      thinkingLevel: payload.thinkingLevel,
      contentParts: payload.contentParts,
      agentSpec: payload.agentSpec,
      templateContext: payload.templateContext,
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
  run_id?: string | null;
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

// ── Audio Library (Broadcast Studio) ────────────────────────────────

export interface AudioFileEntry {
  name: string;
  path: string;
  ext: string;
  size: number;
  modified: number;
  mime: string;
}

export interface AudioLibraryResponse {
  ok: boolean;
  root: string;
  count: number;
  files: AudioFileEntry[];
}

export async function getAudioLibrary(options?: {
  path?: string;
  depth?: number;
}): Promise<AudioLibraryResponse> {
  const params = new URLSearchParams();
  if (options?.path) params.set("path", options.path);
  if (options?.depth != null) params.set("depth", String(options.depth));
  const qs = params.toString();
  return request<AudioLibraryResponse>(
    `/api/studio/audio-library${qs ? `?${qs}` : ""}`
  );
}

export async function ensureAudioDirectory(path: string): Promise<{ ok: boolean; path: string }> {
  return request(`/api/studio/audio-library/ensure-dir`, {
    method: "POST",
    body: JSON.stringify({ path }),
  });
}

export async function renameAudioFile(from: string, to: string): Promise<{ ok: boolean; from: string; to: string }> {
  return request(`/api/studio/audio-library/rename`, {
    method: "POST",
    body: JSON.stringify({ from, to }),
  });
}

export function getAudioStreamUrl(path: string): string {
  const params = new URLSearchParams({ path });
  return `${API_BASE}/api/studio/stream?${params.toString()}`;
}

export async function savePlaylistAsM3U(name: string, items: Array<{ path: string; name: string }>): Promise<{ ok: boolean; path: string; count: number }> {
  return request("/api/studio/save-m3u", {
    method: "POST",
    body: JSON.stringify({ name, items }),
  });
}

export function getM3UDownloadUrl(): string {
  return `${API_BASE}/api/studio/download-m3u`;
}

// ── Audio Labels ──────────────────────────────────────────────────

export async function getAudioLabels(filePath: string): Promise<{ ok: boolean; path: string; labels: string[] }> {
  return request(`/api/studio/labels?path=${encodeURIComponent(filePath)}`);
}

export async function getAllLabels(): Promise<{ ok: boolean; labels: string[] }> {
  return request(`/api/studio/labels?all=true`);
}

export async function addAudioLabel(filePath: string, label: string): Promise<{ ok: boolean; path: string; labels: string[] }> {
  return request(`/api/studio/labels/add`, {
    method: "POST",
    body: JSON.stringify({ path: filePath, label }),
  });
}

export async function removeAudioLabel(filePath: string, label: string): Promise<{ ok: boolean; path: string; labels: string[] }> {
  return request(`/api/studio/labels/remove`, {
    method: "POST",
    body: JSON.stringify({ path: filePath, label }),
  });
}

export async function getFilesByLabel(label: string): Promise<{ ok: boolean; label: string; files: string[] }> {
  return request(`/api/studio/labels/by-label?label=${encodeURIComponent(label)}`);
}

export async function syncAudioSymlinks(): Promise<{ ok: boolean; symlinks: number }> {
  return request(`/api/studio/sync-symlinks`, { method: "POST" });
}

export async function loadM3UPlaylist(filePath: string): Promise<{ ok: boolean; name: string; items: Array<{ path: string; name: string }> }> {
  return request(`/api/studio/load-m3u?path=${encodeURIComponent(filePath)}`);
}

export async function listPlaylists(): Promise<{ ok: boolean; playlists: Array<{ name: string; path: string; filename: string }> }> {
  return request(`/api/studio/playlists`);
}

export async function getAudioAssetUrl(audioPath: string, assetType: "waveform" | "spectrogram"): Promise<string> {
  return `${API_BASE}/api/studio/audio-asset?path=${encodeURIComponent(audioPath)}&type=${assetType}`;
}

export async function saveAudioAsset(audioPath: string, assetType: "waveform" | "spectrogram", imageData: string, mimeType?: string, width?: number, height?: number): Promise<{ ok: boolean }> {
  return request(`/api/studio/audio-asset`, {
    method: "POST",
    body: JSON.stringify({ path: audioPath, type: assetType, imageData, mimeType, width, height }),
  });
}

export interface DiscordAudioScanResponse {
  ok: boolean;
  scanned_at: string;
  import_root: string;
  channels_scanned: number;
  messages_scanned: number;
  attachments_found: number;
  imported_count: number;
  skipped_count: number;
  failed_count: number;
  manifest_path?: string;
}

export async function scanDiscordAudio(options?: {
  channel_ids?: string[];
  since_hours?: number;
  pages_per_channel?: number;
  limit_per_page?: number;
  max_channels?: number;
  import_root?: string;
}): Promise<DiscordAudioScanResponse> {
  return request(`/api/studio/discord-audio-scan`, {
    method: "POST",
    body: JSON.stringify(options ?? {}),
  });
}

export interface DiscordImageScanResponse {
  ok: boolean;
  scanned_at: string;
  import_root: string;
  channels_scanned: number;
  messages_scanned: number;
  attachments_found: number;
  imported_count: number;
  skipped_count: number;
  failed_count: number;
  manifest_path?: string;
}

export async function scanDiscordImages(options?: {
  channel_ids?: string[];
  since_hours?: number;
  pages_per_channel?: number;
  limit_per_page?: number;
  max_channels?: number;
  import_root?: string;
}): Promise<DiscordImageScanResponse> {
  return request(`/api/studio/discord-image-scan`, {
    method: "POST",
    body: JSON.stringify(options ?? {}),
  });
}
