/** Compatibility exports for existing frontend consumers; each domain owns its types. */
export type {
  Role,
  AgentSource,
  ContentPart,
  MessageAttachment,
  ChatMessage,
  ChatTraceBlockKind,
  ChatTraceBlock,
  GroundedContextRow,
  GroundedAnswerResponse,
  SamplingSettings,
  ModelInfo,
  ProxxModelInfo,
  ServerStartPayload,
  RunSummary,
  RunEvent,
  ToolReceipt,
  ActiveAgentSummary,
  RunDetail,
  MemorySessionSummary,
  MemorySessionListResponse,
  MemorySessionRow,
  MemorySearchHit,
  ChatRequest,
  ChatStartResponse,
  WsMessage,
  LoungeMessage,
  ChatProvider,
} from "../components/chat-page/types";

export type {
  KnoxxAuthIdentity,
  AdminToolPolicy,
  AdminOrgSummary,
  AdminRoleSummary,
  AdminMembershipSummary,
  AdminActorCredentialSummary,
  AdminUserSummary,
  AdminDataLakeSummary,
  AdminPermissionDefinition,
  AdminToolDefinition,
  KnoxxAuthContext,
  AdminBootstrapContext,
} from "../components/admin-page/types";

export type {
  TranslationAdequacy,
  TranslationFluency,
  TranslationTerminology,
  TranslationRisk,
  TranslationOverall,
  TranslationStatus,
  TranslationLabel,
  TranslationSegment,
  TranslationSegmentListResponse,
  TranslationLabelPayload,
  TranslationDocumentSummary,
  TranslationDocumentDetail,
  TranslationBatchSummary,
  TranslationDocumentReviewPayload,
  TranslationManifestLanguageStats,
  TranslationManifest,
} from "./api/openplanner";

export interface FrontendConfig {
  knoxx_admin_url: string;
  knoxx_base_url: string;
  knoxx_enabled: boolean;
  proxx_enabled: boolean;
  proxx_default_model: string;
  shibboleth_ui_url: string;
  shibboleth_enabled: boolean;
  default_role: string;
  default_actor_id?: string;
  default_agent_contract?: string;
  email_enabled: boolean;

  // Voice / STT (optional)
  stt_enabled?: boolean;
  stt_base_url?: string;

  // Voice / TTS (optional)
  tts_enabled?: boolean;
  tts_provider?: string;
  tts_default_voice_id?: string;
}

export interface SttTranscribeResponse {
  text: string;
  device?: string | null;
  model_id?: string | null;
  duration_s?: number | null;
  rtf?: number | null;
}

export interface ToolDefinition {
  id: string;
  label: string;
  description: string;
  enabled: boolean;
}

export interface ToolCatalogResponse {
  role: string;
  actor_id?: string | null;
  agent_id?: string | null;
  agent_label?: string | null;
  agent_trigger_kind?: string | null;
  role_slugs?: string[];
  capability_ids?: string[];
  system_prompt?: string | null;
  actor_system_prompt?: string | null;
  agent_system_prompt?: string | null;
  task_prompt?: string | null;
  tools: ToolDefinition[];
  email_enabled: boolean;
}

export interface ActorCatalogItem {
  id: string;
  kind?: string | null;
  defaultAgent?: string | null;
  roleSlugs?: string[];
}

export interface AgentContractCatalogItem {
  id: string;
  role: string;
  model?: string | null;
  triggerKind?: string | null;
  actorId?: string | null;
}

export interface AgentContractCatalogResponse {
  actor_id?: string | null;
  default_actor_id?: string | null;
  actors?: ActorCatalogItem[];
  agents: AgentContractCatalogItem[];
  default_agent_contract?: string | null;
}

export type ContractsClass =
  | "agents" | "actors" | "roles" | "capabilities" | "policies"
  | "model_families" | "models"
  | "actions" | "pipelines" | "triggers"
  | "devel" | "ensemble" | "knoxx-session";

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

export type ActorMailboxStatus = "pending" | "delivered" | "failed" | "expired" | "superseded" | "acknowledged";
export type ActorMailboxBox = "inbox" | "outbox";

export interface ActorMailboxEntry {
  id: string;
  kind: string;
  status: ActorMailboxStatus | string;
  source: Record<string, unknown>;
  target: Record<string, unknown>;
  delivery: Record<string, unknown>;
  contentRef: Record<string, unknown>;
  metadata: Record<string, unknown>;
  preview?: string;
  lastError?: string;
  createdAt?: string;
  updatedAt?: string;
  deliveredAt?: string;
  acknowledgedAt?: string;
  expiresAt?: string;
}

export interface ActorMailboxListResponse {
  ok: boolean;
  box?: ActorMailboxBox;
  actorId?: string;
  durable?: boolean;
  durable_?: boolean;
  entries: ActorMailboxEntry[];
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

// Graph label metadata and nodes selected by a label.
export interface GraphLabelNode {
  label_id: string;
  label: string;
  emoji: string | null;
  description: string;
  color: string | null;
  tenant_id: string;
  project: string | null;
  created_by: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface GraphLabeledNode {
  node_id: string;
  event: any;
}
