import { request } from "./core";

// ── Contract types ──────────────────────────────────────────────────────────

export interface AgentContract {
  "contract/id": string;
  "contract/kind": "agent" | "policy" | "fulfillment" | "tool-call" | "trigger";
  "contract/version"?: number;
  "contract/uses"?: string[];
  enabled?: boolean;
  "trigger-kind"?: "event" | "cron" | "manual";
  "source-kind"?: string;
  "source-mode"?: string;
  "cadence-min"?: number;
  agent?: {
    role?: string;
    model?: string;
    thinking?: string;
  };
  prompts?: {
    system?: string;
    task?: string;
    user?: string;
  };
  events?: {
    always?: string[];
    maybe?: string[];
  };
  data?: Record<string, unknown>;
  hooks?: Record<string, unknown>;
  "ui/schema"?: Record<string, unknown>;
}

export interface ContractValidationResult {
  ok: boolean;
  errors: Array<{ path: string[]; message: string }>;
  warnings: Array<{ path: string[]; message: string }>;
}

export interface ContractCompileResult {
  ok: boolean;
  contract: AgentContract;
  sql: {
    contract: Record<string, unknown>;
    "event-kinds": Array<Record<string, unknown>>;
    bindings: Array<Record<string, unknown>>;
    tools: Array<Record<string, unknown>>;
  };
  errors?: Array<{ path: string[]; message: string }>;
}

export interface ContractListItem {
  id: string;
  kind: string;
  version: number;
  enabled: boolean;
  ednHash: number;
  compiledAt: string | null;
  updatedAt: string;
}

export interface ContractListResponse {
  contracts: ContractListItem[];
}

export interface ContractGetResponse {
  contract: AgentContract;
  ednText: string;
  validation: ContractValidationResult;
}

export interface ContractSaveResponse {
  ok: boolean;
  contract: AgentContract;
  ednText: string;
  validation: ContractValidationResult;
}

// ── API functions ───────────────────────────────────────────────────────────

export async function listContracts(): Promise<ContractListResponse> {
  return request<ContractListResponse>("/api/admin/contracts");
}

export async function getContract(
  contractId: string,
): Promise<ContractGetResponse> {
  return request<ContractGetResponse>(
    `/api/admin/contracts/${encodeURIComponent(contractId)}`,
  );
}

export async function saveContract(
  contractId: string,
  ednText: string,
): Promise<ContractSaveResponse> {
  return request<ContractSaveResponse>(
    `/api/admin/contracts/${encodeURIComponent(contractId)}`,
    {
      method: "PUT",
      body: JSON.stringify({ ednText }),
    },
  );
}

export async function validateContract(
  ednText: string,
): Promise<ContractValidationResult> {
  return request<ContractValidationResult>(
    "/api/admin/contracts/validate",
    {
      method: "POST",
      body: JSON.stringify({ ednText }),
    },
  );
}

export async function compileContract(
  contractId: string,
): Promise<ContractCompileResult> {
  return request<ContractCompileResult>(
    `/api/admin/contracts/${encodeURIComponent(contractId)}/compile`,
    { method: "POST" },
  );
}

export async function copyContract(
  sourceId: string,
  newId: string,
): Promise<ContractSaveResponse> {
  return request<ContractSaveResponse>(
    `/api/admin/contracts/${encodeURIComponent(sourceId)}/copy`,
    {
      method: "POST",
      body: JSON.stringify({ newId }),
    },
  );
}

// ── Default contract template ───────────────────────────────────────────────

export const DEFAULT_CONTRACT_EDN = `{:contract/id "new-agent"
 :contract/kind :agent
 :contract/version 1
 :enabled true
 :trigger-kind :event
 :source-kind :discord
 :source-mode :patrol
 :cadence-min 5

 :agent
 {:role "system_admin"
  :model "glm-5"
  :thinking :off}

 :prompts
 {:system "Observe configured channels and detect fresh signals."
  :task   "Read recent messages and dispatch events for worthy signals."}

 :events
 {:always [:discord.mention]
  :maybe  [:discord.message :discord.reaction]}

 :data
 {:source  {:max-messages 25}
  :filters {:channels []
            :keywords []}
  :tools   []}}

 :hooks
 {:before {}
  :after  {}}}`;

// ── Event kind catalog ─────────────────────────────────────────────────────

export const EVENT_KIND_OPTIONS = [
  "discord.mention",
  "discord.message",
  "discord.message.keyword",
  "discord.message.mention",
  "discord.reaction",
  "discord.image-attachment",
  "discord.text-attachment",
  "github.issues.opened",
  "github.issues.closed",
  "github.pr.opened",
  "github.pr.merged",
  "github.push",
  "cron.tick",
  "manual.invoke",
];

export const MODEL_OPTIONS = [
  "glm-5",
  "glm-5-plus",
  "gpt-5.4",
  "gpt-5.4-mini",
  "claude-4-sonnet",
  "claude-4-opus",
  "kimi-k2.5",
];

export const ROLE_OPTIONS = [
  "system_admin",
  "knowledge_worker",
  "executive",
  "analyst",
  "editor",
];

export const TRIGGER_KIND_OPTIONS = ["event", "cron", "manual"] as const;
export const SOURCE_KIND_OPTIONS = [
  "discord",
  "github",
  "cron",
  "manual",
] as const;
export const THINKING_OPTIONS = [
  "off",
  "minimal",
  "low",
  "medium",
  "high",
  "xhigh",
] as const;
