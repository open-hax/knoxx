export type Role = "system" | "user" | "assistant";

export interface AgentSource {
  title: string;
  url: string;
  section?: string;
}

export interface ChatMessage {
  id: string;
  role: Role;
  content: string;
  model?: string | null;
  contextRows?: GroundedContextRow[];
  sources?: AgentSource[];
  runId?: string | null;
  status?: "streaming" | "done" | "error";
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
  [key: string]: unknown;
}

export interface RunDetail extends RunSummary {
  session_id?: string | null;
  conversation_id?: string | null;
  answer?: string | null;
  request_messages: Array<{ role: string; content: string }>;
  settings: Record<string, unknown>;
  resources: Record<string, unknown>;
  events?: RunEvent[];
  tool_receipts?: ToolReceipt[];
  sources?: AgentSource[];
}

export interface MemorySessionSummary {
  project?: string;
  session: string;
  last_ts?: string;
  event_count?: number;
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

export interface FrontendConfig {
  knoxx_admin_url: string;
  knoxx_base_url: string;
  knoxx_enabled: boolean;
  proxx_enabled: boolean;
  proxx_default_model: string;
  shibboleth_ui_url: string;
  shibboleth_enabled: boolean;
  default_role: string;
  email_enabled: boolean;
}

export interface ToolDefinition {
  id: string;
  label: string;
  description: string;
  enabled: boolean;
}

export interface ToolCatalogResponse {
  role: string;
  tools: ToolDefinition[];
  email_enabled: boolean;
}

export interface EmailSendResponse {
  ok: boolean;
  role: string;
  sent_to: string[];
  subject: string;
}

export interface ToolReadResponse {
  ok: boolean;
  role: string;
  path: string;
  content: string;
  truncated: boolean;
}

export interface ToolWriteResponse {
  ok: boolean;
  role: string;
  path: string;
  bytes_written: number;
}

export interface ToolEditResponse {
  ok: boolean;
  role: string;
  path: string;
  replacements: number;
}

export interface ToolBashResponse {
  ok: boolean;
  role: string;
  command: string;
  exit_code: number;
  stdout: string;
  stderr: string;
}

export interface ProxxHealth {
  reachable: boolean;
  configured: boolean;
  base_url: string;
  status_code?: number;
  model_count?: number;
  default_model?: string | null;
}

export interface ProxxChatResponse {
  answer: string;
  model?: string | null;
  rag_context?: Array<{
    score: number;
    text: string;
    source: string;
  }> | null;
}

export interface ShibbolethHandoffResponse {
  ok: boolean;
  session_id: string;
  ui_url: string;
  imported_item_count: number;
}

export type ChatProvider = "proxx" | "knoxx-rag" | "knoxx-direct";
