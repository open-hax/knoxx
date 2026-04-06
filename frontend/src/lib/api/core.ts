import type { KnoxxAuthIdentity } from "../types";

const importMetaEnv = (import.meta as ImportMeta & { env?: Record<string, string | undefined> }).env;

export const API_BASE = importMetaEnv?.VITE_API_BASE ?? "";

const KNOXX_USER_EMAIL_KEY = "knoxx_user_email";
const KNOXX_ORG_SLUG_KEY = "knoxx_org_slug";

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
    userEmail: getStoredAuthValue(KNOXX_USER_EMAIL_KEY) ?? "",
    orgSlug: getStoredAuthValue(KNOXX_ORG_SLUG_KEY) ?? "",
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
  const userEmail = getStoredAuthValue(KNOXX_USER_EMAIL_KEY);
  const orgSlug = getStoredAuthValue(KNOXX_ORG_SLUG_KEY);
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
    response = await fetch(`${API_BASE}${path}`, {
      headers: {
        ...Object.fromEntries(buildKnoxxAuthHeaders({
          "Content-Type": "application/json",
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
