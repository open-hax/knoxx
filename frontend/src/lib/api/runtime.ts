import type {
  ActorMailboxBox,
  ActorMailboxEntry,
  ActorMailboxListResponse,
  ActorMailboxStatus,
  AgentContractCatalogResponse,
  AgentSource,
  ContentPart,
  EmailSendResponse,
  RunEvent,
  ShibbolethHandoffResponse,
  SttTranscribeResponse,
  ToolBashResponse,
  ToolCatalogResponse,
  ToolEditResponse,
  ToolReadResponse,
  ToolWriteResponse,
} from "../types";
import { API_BASE, buildKnoxxAuthHeaders, request, isRuntimeRecord, runtimeBoolean, normalizeMailboxEntry, normalizeMailboxListResponse, normalizeToolCatalogResponse, normalizeConversationResponse } from "./core";

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
  const record = isRuntimeRecord(response) ? response : {};
  const entry = normalizeMailboxEntry(record.entry);
  return { ok: runtimeBoolean(record.ok) ?? true, ...(entry ? { entry } : {}) };
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
  actor_id?: string | null;
}): Promise<{ ok: boolean; conversation_id?: string | null; session_id?: string | null; run_id?: string | null; kind?: string | null }> {
  const endpoint = payload.kind === "follow_up" ? "/api/knoxx/follow-up" : "/api/knoxx/steer";
  return request<Record<string, unknown>>(endpoint, {
    method: "POST",
    body: JSON.stringify({
      message: payload.message,
      conversation_id: payload.conversation_id,
      session_id: payload.session_id,
      run_id: payload.run_id,
      actor_id: payload.actor_id,
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
  actor_id?: string | null;
  reason?: string;
}): Promise<{ ok: boolean; conversation_id?: string | null; session_id?: string | null; run_id?: string | null; error?: string | null }> {
  return request<Record<string, unknown>>("/api/knoxx/abort", {
    method: "POST",
    body: JSON.stringify({
      conversation_id: payload.conversation_id,
      session_id: payload.session_id,
      run_id: payload.run_id,
      actor_id: payload.actor_id,
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
  actor_id?: string | null;
  turns?: number;
}): Promise<{ ok: boolean; session_id?: string | null; conversation_id?: string | null; removed_count?: number; remaining_messages?: number; error?: string | null }> {
  return request<Record<string, unknown>>("/api/knoxx/session/undo", {
    method: "POST",
    body: JSON.stringify({
      session_id: payload.session_id,
      conversation_id: payload.conversation_id,
      actor_id: payload.actor_id,
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


export { listProxxModels, proxxHealth, proxxChat } from "./proxxObservability";
export {
  getAudioLibrary,
  ensureAudioDirectory,
  renameAudioFile,
  getAudioStreamUrl,
  savePlaylistAsM3U,
  getM3UDownloadUrl,
  getAudioLabels,
  getAllLabels,
  addAudioLabel,
  removeAudioLabel,
  getFilesByLabel,
  syncAudioSymlinks,
  loadM3UPlaylist,
  listPlaylists,
  getAudioAssetUrl,
  saveAudioAsset,
  scanDiscordAudio,
  scanDiscordImages,
} from "../mediaEmbeds";
export type { AudioFileEntry, AudioLibraryResponse, DiscordAudioScanResponse, DiscordImageScanResponse } from "../mediaEmbeds";
