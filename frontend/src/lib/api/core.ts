import type { KnoxxAuthIdentity, ActorMailboxBox, ActorMailboxEntry, ActorMailboxListResponse, ToolCatalogResponse } from "../types";

const importMetaEnv = (import.meta as ImportMeta & { env?: Record<string, string | undefined> }).env;

export const API_BASE = importMetaEnv?.VITE_API_BASE ?? "";

const KNOXX_USER_EMAIL_KEY = "knoxx_user_email";
const KNOXX_ORG_SLUG_KEY = "knoxx_org_slug";
const DEFAULT_KNOXX_USER_EMAIL = importMetaEnv?.VITE_KNOXX_DEV_USER_EMAIL?.trim() ?? "";
const DEFAULT_KNOXX_ORG_SLUG = importMetaEnv?.VITE_KNOXX_DEV_ORG_SLUG?.trim() ?? "";

function getStoredAuthValue(key: string): string | null {
  if (typeof window === "undefined") return null;
  try {
    const value = localStorage.getItem(key)?.trim();
    return value ? value : null;
  } catch {
    return null;
  }
}

export function getKnoxxAuthIdentity(): KnoxxAuthIdentity {
  return {
    userEmail: getStoredAuthValue(KNOXX_USER_EMAIL_KEY) ?? DEFAULT_KNOXX_USER_EMAIL,
    orgSlug: getStoredAuthValue(KNOXX_ORG_SLUG_KEY) ?? DEFAULT_KNOXX_ORG_SLUG,
  };
}

export function setKnoxxAuthIdentity(next: KnoxxAuthIdentity): KnoxxAuthIdentity {
  const resolved = {
    userEmail: next.userEmail.trim(),
    orgSlug: next.orgSlug.trim(),
  };

  if (typeof window !== "undefined") {
    try {
      if (resolved.userEmail) {
        localStorage.setItem(KNOXX_USER_EMAIL_KEY, resolved.userEmail);
      } else {
        localStorage.removeItem(KNOXX_USER_EMAIL_KEY);
      }
      if (resolved.orgSlug) {
        localStorage.setItem(KNOXX_ORG_SLUG_KEY, resolved.orgSlug);
      } else {
        localStorage.removeItem(KNOXX_ORG_SLUG_KEY);
      }
    } catch {
      // ignore storage failures and still return the resolved identity
    }
  }

  return resolved;
}

export function buildKnoxxAuthHeaders(headersInit?: HeadersInit): Headers {
  const headers = new Headers(headersInit || {});
  const userEmail = getStoredAuthValue(KNOXX_USER_EMAIL_KEY) ?? DEFAULT_KNOXX_USER_EMAIL;
  const orgSlug = getStoredAuthValue(KNOXX_ORG_SLUG_KEY) ?? DEFAULT_KNOXX_ORG_SLUG;
  if (userEmail && !headers.has("x-knoxx-user-email")) {
    headers.set("x-knoxx-user-email", userEmail);
  }
  if (orgSlug && !headers.has("x-knoxx-org-slug")) {
    headers.set("x-knoxx-org-slug", orgSlug);
  }
  return headers;
}

function summarizeErrorPayload(payload: unknown): string | null {
  if (!payload || typeof payload !== "object") return null;

  const record = payload as Record<string, unknown>;
  const parts = [
    typeof record.error === "string" ? record.error : null,
    typeof record.detail === "string" ? record.detail : null,
    typeof record.message === "string" ? record.message : null,
    typeof record.error_code === "string" ? `code=${record.error_code}` : null,
    typeof record.model_error === "string" ? record.model_error : null,
  ].filter(Boolean);

  return parts.length > 0 ? parts.join(" | ") : null;
}

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;

  try {
    const hasBody = init?.body != null;
    response = await fetch(`${API_BASE}${path}`, {
      credentials: "include",
      headers: {
        ...Object.fromEntries(buildKnoxxAuthHeaders({
          ...(hasBody ? { "Content-Type": "application/json" } : {}),
          ...(init?.headers ?? {}),
        }).entries()),
      },
      ...init,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    throw new Error(`Request to ${path} failed before the server responded. This usually means the reverse proxy or upstream service reset the connection. ${message}`);
  }

  if (!response.ok) {
    const text = await response.text();
    let detail = text;

    try {
      detail = summarizeErrorPayload(JSON.parse(text)) ?? text;
    } catch {
      // leave detail as raw text
    }

    throw new Error(`${response.status} ${response.statusText}${detail ? ` - ${detail}` : ""}`);
  }

  return (await response.json()) as T;
}

// Shared response decoding preserves explicit nulls and alias precedence.
export type WireRecord = Record<string, unknown>;

export function asRecord(value: unknown): WireRecord {
  return value != null && typeof value === "object" && !Array.isArray(value) ? value as WireRecord : {};
}

export function valueAt(record: WireRecord, ...keys: string[]): unknown {
  for (const key of keys) {
    if (Object.prototype.hasOwnProperty.call(record, key)) {
      return record[key];
    }
  }
  return undefined;
}

export function stringValue(record: WireRecord, keys: string[], fallback = ""): string {
  const value = valueAt(record, ...keys);
  return typeof value === "string" ? value : fallback;
}

export function optionalStringValue(record: WireRecord, keys: string[]): string | undefined {
  const value = valueAt(record, ...keys);
  return typeof value === "string" ? value : undefined;
}

export function optionalNullableStringValue(record: WireRecord, keys: string[]): string | null | undefined {
  const value = valueAt(record, ...keys);
  if (value === null) return null;
  return typeof value === "string" ? value : undefined;
}

export function optionalBooleanValue(record: WireRecord, keys: string[]): boolean | undefined {
  const value = valueAt(record, ...keys);
  return typeof value === "boolean" ? value : undefined;
}

export function optionalNumberValue(record: WireRecord, keys: string[]): number | undefined {
  const value = valueAt(record, ...keys);
  return typeof value === "number" ? value : undefined;
}

export function stringArrayValue(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((entry): entry is string => typeof entry === "string") : [];
}

export function recordArrayValue(value: unknown): WireRecord[] {
  return Array.isArray(value) ? value.map(asRecord) : [];
}

export function optionalRecordValue(record: WireRecord, keys: string[]): Record<string, unknown> | undefined {
  const value = valueAt(record, ...keys);
  const normalized = asRecord(value);
  return Object.keys(normalized).length > 0 ? normalized : undefined;
}

// Session-scoped proxy requests retain their distinct error and URL contract.
const KNOXX_SESSION_KEY = 'knoxx_session_id';

export class ProxyApiError extends Error {
  status: number;
  body: string;

  constructor(status: number, body: string) {
    super(body || `Proxy request failed: ${status}`);
    this.status = status;
    this.body = body;
    this.name = 'ProxyApiError';
  }
}

export function getKnoxxSessionId(): string {
  if (typeof window === 'undefined') return '';
  let current = sessionStorage.getItem(KNOXX_SESSION_KEY);
  if (current) return current;
  current = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `sess-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  sessionStorage.setItem(KNOXX_SESSION_KEY, current);
  return current;
}

export async function sessionRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = buildKnoxxAuthHeaders(init?.headers);
  headers.set('x-knoxx-session-id', getKnoxxSessionId());
  const res = await fetch(path, {
    ...init,
    headers,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new ProxyApiError(res.status, text || `Request failed: ${res.status}`);
  }
  return res.json() as Promise<T>;
}

// Runtime wire decoders retain their established array and alias handling.
export function isRuntimeRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

function asString(value: unknown): string | undefined {
  return typeof value === "string" ? value : undefined;
}

export function runtimeBoolean(value: unknown): boolean | undefined {
  return typeof value === "boolean" ? value : undefined;
}

function runtimeRecord(value: unknown): Record<string, unknown> {
  return isRuntimeRecord(value) ? value : {};
}

export function normalizeMailboxEntry(value: unknown): ActorMailboxEntry | null {
  if (!isRuntimeRecord(value)) {
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
    source: runtimeRecord(value.source),
    target: runtimeRecord(value.target),
    delivery: runtimeRecord(value.delivery),
    contentRef: runtimeRecord(value.contentRef),
    metadata: runtimeRecord(value.metadata),
    preview: asString(value.preview),
    lastError: asString(value.lastError),
    createdAt: asString(value.createdAt),
    updatedAt: asString(value.updatedAt),
    deliveredAt: asString(value.deliveredAt),
    acknowledgedAt: asString(value.acknowledgedAt),
    expiresAt: asString(value.expiresAt),
  };
}

export function normalizeMailboxListResponse(value: unknown, fallbackBox: ActorMailboxBox): ActorMailboxListResponse {
  const record = isRuntimeRecord(value) ? value : {};
  const entries = Array.isArray(record.entries)
    ? record.entries.map(normalizeMailboxEntry).filter((entry): entry is ActorMailboxEntry => entry !== null)
    : [];

  return {
    ok: runtimeBoolean(record.ok) ?? true,
    box: (asString(record.box) === "outbox" ? "outbox" : asString(record.box) === "inbox" ? "inbox" : fallbackBox),
    actorId: asString(record.actorId),
    durable: runtimeBoolean(record.durable) ?? runtimeBoolean(record.durable_),
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
  if (!isRuntimeRecord(value)) {
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
    enabled: runtimeBoolean(value.enabled) ?? true,
  };
}

function normalizeToolDefinitions(value: unknown): ToolCatalogResponse["tools"] {
  if (Array.isArray(value)) {
    return value
      .map((entry) => normalizeToolDefinition(entry))
      .filter((entry): entry is ToolCatalogResponse["tools"][number] => entry !== null);
  }

  if (!isRuntimeRecord(value)) {
    return [];
  }

  return Object.entries(value)
    .map(([fallbackId, entry]) => normalizeToolDefinition(entry, fallbackId))
    .filter((entry): entry is ToolCatalogResponse["tools"][number] => entry !== null);
}

export function normalizeToolCatalogResponse(value: unknown): ToolCatalogResponse {
  const record = isRuntimeRecord(value) ? value : {};

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
    email_enabled: runtimeBoolean(record.email_enabled) ?? runtimeBoolean(record.emailEnabled) ?? false,
  };
}

export function normalizeConversationResponse(response: Record<string, unknown>) {
  return {
    answer: typeof response.answer === "string" ? response.answer : "",
    run_id:
      typeof response.run_id === "string"
        ? response.run_id
        : typeof response.runId === "string"
          ? response.runId
          : typeof response["run-id"] === "string"
            ? response["run-id"]
            : null,
    conversation_id:
      typeof response.conversation_id === "string"
        ? response.conversation_id
        : typeof response.conversationId === "string"
          ? response.conversationId
          : typeof response["conversation-id"] === "string"
            ? response["conversation-id"]
            : null,
    session_id:
      typeof response.session_id === "string"
        ? response.session_id
        : typeof response.sessionId === "string"
          ? response.sessionId
          : typeof response["session-id"] === "string"
            ? response["session-id"]
            : null,
    model: typeof response.model === "string" ? response.model : null,
  };
}
