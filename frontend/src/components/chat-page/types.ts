export type {
  BrowseEntry,
  BrowseResponse,
  HydrationSource,
  IngestionSource,
  PinnedContextItem,
  PreviewResponse,
  SemanticSearchMatch,
  SemanticSearchResponse,
  SessionStateSnapshot,
  WorkspaceJob,
} from "../workspace-context/types";

// Chat messages, model settings and persisted run/session wire contracts.
export type Role = "system" | "user" | "assistant";

export interface AgentSource {
  title: string;
  url: string;
  section?: string;
}

// Multimodal content parts for rich messages
export interface ContentPart {
  type: "text" | "image" | "audio" | "video" | "document";
  text?: string;
  url?: string;
  data?: string; // Base64 data URL
  mimeType?: string;
  filename?: string;
  size?: number;
}

// Attachment metadata for upload tracking
export interface MessageAttachment {
  id: string;
  type: "image" | "audio" | "video" | "document";
  filename: string;
  url?: string;
  data?: string;
  mimeType: string;
  size: number;
  uploading?: boolean;
  error?: string;
}

export interface ChatMessage {
  id: string;
  role: Role;
  content: string;
  // Multimodal content parts - supports rich messages with images, audio, video, documents
  contentParts?: ContentPart[];
  // Legacy attachments field for backward compatibility
  attachments?: MessageAttachment[];
  model?: string | null;
  contextRows?: GroundedContextRow[];
  sources?: AgentSource[];
  runId?: string | null;
  status?: "streaming" | "done" | "error";
  traceBlocks?: ChatTraceBlock[];
}

export type ChatTraceBlockKind = "agent_message" | "reasoning" | "tool_call";

export interface ChatTraceBlock {
  id: string;
  kind: ChatTraceBlockKind;
  status?: "streaming" | "done" | "error";
  at?: string;
  content?: string;
  toolName?: string;
  toolCallId?: string;
  inputPreview?: string;
  outputPreview?: string;
  updates?: string[];
  isError?: boolean;
}

export interface GroundedContextRow {
  id: string;
  ts?: string;
  source?: string;
  source_path?: string;
  kind?: string;
  project?: string;
  session?: string;
  message?: string;
  snippet?: string;
  text?: string;
  tier?: string;
}

export interface GroundedAnswerResponse {
  projects: string[];
  count: number;
  rows: GroundedContextRow[];
  answer: string;
  model?: string | null;
}

export interface SamplingSettings {
  temperature: number;
  top_p: number;
  top_k: number;
  min_p: number;
  repeat_penalty: number;
  presence_penalty: number;
  frequency_penalty: number;
  seed: number | null;
  max_tokens: number;
  stop_sequences: string[];
}

export interface ModelInfo {
  id: string;
  name: string;
  path: string;
  size_bytes: number;
  modified_at: string;
  hash16mb: string;
  suggested_ctx?: number | null;
}

export interface ProxxModelInfo {
  id: string;
  name: string;
  owned_by?: string | null;
}

export interface ServerStartPayload {
  model_path: string;
  port?: number;
  ctx_size?: number;
  threads?: number;
  gpu_layers?: number;
  batch_size?: number;
  ubatch_size?: number;
  flash_attention?: boolean;
  mmap?: boolean;
  mlock?: boolean;
  multi_instance_mode?: boolean;
  extra_args?: string[];
}

export interface RunSummary {
  run_id: string;
  created_at: string;
  updated_at: string;
  status: string;
  model?: string;
  ttft_ms?: number;
  total_time_ms?: number;
  input_tokens?: number;
  output_tokens?: number;
  tokens_per_s?: number;
  error?: string;
}

export interface RunEvent {
  at?: string;
  type?: string;
  status?: string;
  tool_name?: string;
  tool_call_id?: string;
  preview?: string;
  error?: string;
  ttft_ms?: number;
  hits?: number;
  elapsed_ms?: number;
  tool_result_count?: number;
  [key: string]: unknown;
}

export interface ToolReceipt {
  id: string;
  tool_name?: string;
  status?: string;
  started_at?: string;
  ended_at?: string;
  input_preview?: string;
  result_preview?: string;
  updates?: string[];
  is_error?: boolean;
  contentParts?: ContentPart[];
  [key: string]: unknown;
}

export interface ActiveAgentSummary extends RunSummary {
  session_id?: string | null;
  conversation_id?: string | null;
  event_count?: number;
  tool_receipt_count?: number;
  has_active_stream?: boolean;
  active_turn_registered?: boolean;
  agent_spec?: Record<string, unknown> | null;
  resource_policies?: Record<string, unknown> | null;
  latest_user_message?: string | null;
  latest_event?: Record<string, unknown> | null;
}

export interface RunDetail extends RunSummary {
  session_id?: string | null;
  conversation_id?: string | null;
  answer?: string | null;
  reasoning?: string | null;
  contentParts?: ContentPart[];
  request_messages: Array<{ role: string; content: string; contentParts?: ContentPart[] }>;
  settings: Record<string, unknown>;
  resources: Record<string, unknown>;
  events?: RunEvent[];
  trace_blocks?: ChatTraceBlock[];
  tool_receipts?: ToolReceipt[];
  sources?: AgentSource[];
}

export interface MemorySessionSummary {
  project?: string;
  session: string;
  title?: string | null;
  title_model?: string | null;
  last_ts?: string;
  event_count?: number;

  // Knoxx enrichment: reconstructed from OpenPlanner rows.
  actor_id?: string;
  contract_id?: string;
  contract_actors?: string[];
  sub_agent_id?: string;
  parent_agent_id?: string;
  parent_run_id?: string;
  spawn_kind?: string;
  trigger_id?: string;
  event_type?: string;
  event_types?: string[];
  event_id?: string;
  event_scope_id?: string;
  schedule_id?: string;

  is_active?: boolean;
  active_status?: "running" | "waiting_input" | "completed" | "failed" | "inactive" | "unknown" | string;
  has_active_stream?: boolean;
  active_session_id?: string | null;
  local_only?: boolean;
}

export interface MemorySessionListResponse {
  rows: MemorySessionSummary[];
  total?: number;
  offset?: number;
  limit?: number;
  has_more?: boolean;
}

export interface MemorySessionRow {
  id: string;
  ts?: string;
  source?: string;
  kind?: string;
  project?: string;
  session?: string;
  message?: string;
  role?: Role | string;
  author?: string;
  model?: string | null;
  text?: string;
  attachments?: string;
  extra?: string | Record<string, unknown> | null;
}

export interface MemorySearchHit {
  session?: string;
  role?: string;
  text?: string;
  snippet?: string;
  document?: string;
  distance?: number | null;
  metadata?: Record<string, unknown>;
}

export interface ChatRequest {
  model?: string;
  system_prompt?: string;
  messages: Array<{ role: string; content: string }>;
  temperature?: number;
  top_p?: number;
  top_k?: number;
  min_p?: number;
  repeat_penalty?: number;
  presence_penalty?: number;
  frequency_penalty?: number;
  seed?: number | null;
  max_tokens?: number;
  stop?: string[];
  stream?: boolean;
  metadata?: Record<string, unknown>;
}

export interface ChatStartResponse {
  run_id: string;
  status: "queued" | "running";
}

export interface WsMessage {
  channel: "tokens" | "stats" | "console" | "events" | "lounge";
  timestamp: string;
  payload: Record<string, unknown>;
}

export interface LoungeMessage {
  id: string;
  timestamp: string;
  session_id: string;
  alias: string;
  text: string;
}

export type ChatProvider = "proxx" | "knoxx-rag" | "knoxx-direct";

// Browser attachment draft and input ownership.
export interface MultimodalAttachment {
  id: string;
  file: File;
  preview?: string; // Data URL for images, object URL for audio/video
  type: "image" | "audio" | "video" | "document";
  uploading?: boolean;
  error?: string;
}

export interface MultimodalInputProps {
  attachments: MultimodalAttachment[];
  onAttachmentsChange: (attachments: MultimodalAttachment[]) => void;
  maxSizeBytes?: number;
  accept?: Record<string, string[]>;
  disabled?: boolean;
  /** Hide inline attachment previews (useful when parent manages previews) */
  hidePreviews?: boolean;
}
