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
  [key: string]: unknown;
}

export interface ActiveAgentSummary extends RunSummary {
  session_id?: string | null;
  conversation_id?: string | null;
  event_count?: number;
  tool_receipt_count?: number;
  has_active_stream?: boolean;
  agent_spec?: Record<string, unknown> | null;
  resource_policies?: Record<string, unknown> | null;
  latest_user_message?: string | null;
  latest_event?: Record<string, unknown> | null;
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
  title?: string | null;
  title_model?: string | null;
  last_ts?: string;
  event_count?: number;
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

export interface KnoxxAuthIdentity {
  userEmail: string;
  orgSlug: string;
}

export interface AdminToolPolicy {
  toolId: string;
  effect: "allow" | "deny";
  constraints?: Record<string, unknown>;
}

export interface AdminOrgSummary {
  id: string;
  slug: string;
  name: string;
  kind: string;
  isPrimary: boolean;
  status: string;
  memberCount?: number;
  roleCount?: number;
  dataLakeCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminRoleSummary {
  id: string;
  slug: string;
  name: string;
  scopeKind?: string;
  orgId?: string | null;
  builtIn?: boolean;
  systemManaged?: boolean;
  createdAt?: string;
  updatedAt?: string;
  permissions: string[];
  toolPolicies: AdminToolPolicy[];
}

export interface AdminMembershipSummary {
  id: string;
  orgId: string;
  orgName?: string;
  orgSlug?: string;
  status: string;
  isDefault?: boolean;
  createdAt?: string;
  updatedAt?: string;
  roles: Array<Pick<AdminRoleSummary, "id" | "slug" | "name" | "scopeKind" | "orgId">>;
  toolPolicies: AdminToolPolicy[];
}

export interface AdminUserSummary {
  id: string;
  email: string;
  displayName: string;
  authProvider?: string;
  externalSubject?: string | null;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  memberships: AdminMembershipSummary[];
}

export interface AdminDataLakeSummary {
  id: string;
  orgId: string;
  name: string;
  slug: string;
  kind: string;
  config: Record<string, unknown>;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface GraphExportNode {
  id: string;
  kind: string;
  label: string;
  lake: string;
  nodeType: string;
  source: string;
  project: string;
  ts?: string | null;
  data: Record<string, unknown>;
}

export interface GraphExportEdge {
  id: string;
  source: string;
  target: string;
  kind: string;
  lake: string;
  edgeType: string;
  sourceLake: string;
  targetLake: string;
  sourceEventId?: string;
  data: Record<string, unknown>;
}

export interface GraphExportResponse {
  ok: boolean;
  projects?: string[];
  nodes: GraphExportNode[];
  edges: GraphExportEdge[];
}

export interface AdminPermissionDefinition {
  id: string;
  code: string;
  resourceKind: string;
  action: string;
  description: string;
}

export interface AdminToolDefinition {
  id: string;
  label: string;
  description: string;
  riskLevel: string;
}

export type TranslationAdequacy = "excellent" | "good" | "adequate" | "poor" | "unusable";
export type TranslationFluency = "excellent" | "good" | "adequate" | "poor" | "unusable";
export type TranslationTerminology = "correct" | "minor_errors" | "major_errors";
export type TranslationRisk = "safe" | "sensitive" | "policy_violation";
export type TranslationOverall = "approve" | "needs_edit" | "reject";
export type TranslationStatus = "pending" | "in_review" | "approved" | "rejected";

export interface TranslationLabel {
  id: string;
  segment_id: string;
  labeler_id: string;
  labeler_email: string;
  adequacy: TranslationAdequacy;
  fluency: TranslationFluency;
  terminology: TranslationTerminology;
  risk: TranslationRisk;
  overall: TranslationOverall;
  corrected_text?: string | null;
  editor_notes?: string | null;
  ts: string;
}

export interface TranslationSegment {
  id: string;
  source_text: string;
  translated_text: string;
  source_lang: string;
  target_lang: string;
  status: TranslationStatus;
  confidence?: number | null;
  mt_model?: string | null;
  document_id: string;
  segment_index: number;
  domain?: string | null;
  garden_id?: string | null;
  tenant_id?: string | null;
  org_id?: string | null;
  labels?: TranslationLabel[];
  label_count?: number;
  ts?: string | null;
}

export interface TranslationSegmentListResponse {
  segments: TranslationSegment[];
  total: number;
  has_more: boolean;
}

export interface TranslationLabelPayload {
  adequacy: TranslationAdequacy;
  fluency: TranslationFluency;
  terminology: TranslationTerminology;
  risk: TranslationRisk;
  overall: TranslationOverall;
  corrected_text?: string;
  editor_notes?: string;
}

export interface TranslationDocumentSummary {
  document_id: string;
  target_lang: string;
  source_lang: string;
  garden_id: string | null;
  project: string | null;
  title: string;
  document_status: string;
  total_segments: number;
  approved: number;
  pending: number;
  rejected: number;
  in_review: number;
  overall_status: "fully_approved" | "fully_rejected" | "pending_review" | "partial_review" | "mixed";
}

export interface TranslationDocumentDetail {
  document: {
    id: string;
    title: string;
    content: string;
    source_lang: string;
    visibility: string;
    source_path: string | null;
  };
  segments: TranslationSegment[];
  summary: {
    total_segments: number;
    approved: number;
    pending: number;
    rejected: number;
    in_review: number;
    overall_status: string;
  };
}

export interface TranslationBatchSummary {
  id: string;
  batch_id: string;
  garden_id: string;
  target_lang: string;
  source_lang: string;
  project: string;
  status: "queued" | "processing" | "complete" | "partial" | "failed";
  document_ids: string[];
  completed_documents: string[];
  failed_documents: { document_id: string; error: string }[];
  agent_session_id?: string;
  agent_conversation_id?: string;
  agent_run_id?: string;
  created_at: string;
  started_at?: string;
  completed_at?: string;
  error?: string;
}

export interface TranslationDocumentReviewPayload {
  overall: "approve" | "needs_edit" | "reject";
  editor_notes?: string;
  segment_overrides?: Record<string, {
    overall: "approve" | "needs_edit" | "reject";
    corrected_text?: string;
    editor_notes?: string;
  }>;
}

export interface TranslationManifestLanguageStats {
  total_segments: number;
  approved: number;
  rejected: number;
  pending: number;
  in_review: number;
  avg_labels_per_segment: number;
  with_corrections: number;
}

export interface TranslationManifest {
  project: string;
  generated_at: string;
  languages: Record<string, TranslationManifestLanguageStats>;
  labelers: Array<{ email: string; segments_labeled: number }>;
  export_sizes: Record<string, { rows: number; bytes_estimate: number }>;
}

export interface KnoxxAuthContext {
  user: {
    id: string;
    email: string;
    displayName: string;
    status: string;
  };
  org: {
    id: string;
    slug: string;
    name: string;
    status: string;
    isPrimary?: boolean;
    kind?: string;
  };
  membership: {
    id: string;
    status: string;
    isDefault?: boolean;
    createdAt?: string;
    updatedAt?: string;
  };
  roles: AdminRoleSummary[];
  roleSlugs: string[];
  permissions: string[];
  toolPolicies: AdminToolPolicy[];
  membershipToolPolicies: AdminToolPolicy[];
  isSystemAdmin: boolean;
  primaryRole: string;
}

export interface AdminBootstrapContext {
  primaryOrg: AdminOrgSummary;
  bootstrapUser: {
    id: string;
    email: string;
    displayName: string;
    membershipId: string;
  };
}

export type ChatProvider = "proxx" | "knoxx-rag" | "knoxx-direct";
