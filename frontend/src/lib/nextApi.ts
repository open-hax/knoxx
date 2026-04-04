export * from "./api";
import { buildKnoxxAuthHeaders } from "./api";

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

function getKnoxxSessionId(): string {
  if (typeof window === 'undefined') return '';
  let current = sessionStorage.getItem(KNOXX_SESSION_KEY);
  if (current) return current;
  current = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : `sess-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  sessionStorage.setItem(KNOXX_SESSION_KEY, current);
  return current;
}

async function sessionRequest<T>(path: string, init?: RequestInit): Promise<T> {
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

export async function fetchDocuments() {
  return sessionRequest<any>('/api/documents');
}

export async function uploadDocuments(files: File[], autoIngest: boolean = false) {
  const formData = new FormData();
  files.forEach(file => formData.append('files', file));
  formData.append('autoIngest', String(autoIngest));

  const headers = buildKnoxxAuthHeaders();
  headers.set('x-knoxx-session-id', getKnoxxSessionId());
  const res = await fetch('/api/documents/upload', {
    method: 'POST',
    headers,
    body: formData,
  });
  if (!res.ok) throw new Error('Failed to upload documents');
  return res.json();
}

export async function deleteDocument(path: string) {
  return sessionRequest<any>(`/api/documents/${encodeURIComponent(path)}`, { method: 'DELETE' });
}

export async function ingestDocuments(options: { full?: boolean, selectedFiles?: string[] } = {}) {
  return sessionRequest<any>('/api/documents/ingest', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(options),
  });
}

export async function restartIngestion(forceFresh: boolean = false) {
  return sessionRequest<any>('/api/documents/ingest/restart', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ forceFresh }),
  });
}

export async function fetchIngestionStatus() {
  return sessionRequest<any>('/api/documents/ingestion-status');
}

export async function fetchIngestionProgress() {
  return sessionRequest<any>('/api/documents/ingestion-progress');
}

export async function getSettings() {
  const res = await fetch('/api/settings', { headers: buildKnoxxAuthHeaders() });
  if (!res.ok) throw new Error('Failed to load settings');
  return res.json();
}

export async function updateSettings(settings: any) {
  const res = await fetch('/api/settings', {
    method: 'PUT',
    headers: buildKnoxxAuthHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(settings),
  });
  if (!res.ok) throw new Error('Failed to update settings');
  return res.json();
}

export async function getKnoxxStatus() {
  const res = await fetch('/api/settings/knoxx-status', { headers: buildKnoxxAuthHeaders() });
  if (!res.ok) throw new Error('Failed to load Knoxx status');
  return res.json();
}

export async function knoxxRagChat(payload: {
  message: string;
  conversationId?: string | null;
  includeCompare?: boolean;
}) {
  const res = await fetch('/api/knoxx/chat', {
    method: 'POST',
    headers: buildKnoxxAuthHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw new Error(await res.text() || 'Knoxx chat failed');
  return res.json();
}

export async function knoxxDirectChat(payload: {
  message: string;
  conversationId?: string | null;
}) {
  const res = await fetch('/api/knoxx/direct', {
    method: 'POST',
    headers: buildKnoxxAuthHeaders({ 'Content-Type': 'application/json' }),
    body: JSON.stringify(payload),
  });
  if (!res.ok) throw new Error(await res.text() || 'Knoxx direct chat failed');
  return res.json();
}

export async function fetchRetrievalStats() {
  const res = await fetch('/api/retrieval/stats', { headers: buildKnoxxAuthHeaders() });
  if (!res.ok) throw new Error('Failed to load retrieval stats');
  return res.json();
}

export async function runRetrievalDebug(payload: { message: string; topK?: number }) {
  return sessionRequest<any>('/api/chat/retrieval-debug', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function fetchDocumentContent(relativePath: string) {
  const encoded = relativePath
    .split('/')
    .filter(Boolean)
    .map((segment) => encodeURIComponent(segment))
    .join('/');
  return sessionRequest<{ content: string; path: string }>(`/api/documents/content/${encoded}`);
}

export async function listDatabaseProfiles() {
  return sessionRequest<{
    activeDatabaseId: string;
    databases: Array<{
      id: string;
      name: string;
      docsPath: string;
      qdrantCollection: string;
      publicDocsBaseUrl: string;
      useLocalDocsBaseUrl: boolean;
      forumMode: boolean;
      privateToSession?: boolean;
      ownerSessionId?: string | null;
      canAccess?: boolean;
      createdAt: string;
    }>;
    activeRuntime: {
      projectName: string;
      docsPath: string;
      qdrantCollection: string;
    };
  }>('/api/settings/databases');
}

export async function createDatabaseProfile(payload: {
  name: string;
  docsPath?: string;
  qdrantCollection?: string;
  publicDocsBaseUrl?: string;
  useLocalDocsBaseUrl?: boolean;
  forumMode?: boolean;
  privateToSession?: boolean;
  activate?: boolean;
}) {
  return sessionRequest<any>('/api/settings/databases', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function makeDatabasePrivate(id: string) {
  return sessionRequest<any>(`/api/settings/databases/${encodeURIComponent(id)}/make-private`, {
    method: 'POST',
  });
}

export async function activateDatabaseProfile(id: string) {
  return sessionRequest<any>('/api/settings/databases/activate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id }),
  });
}

export async function updateDatabaseProfile(id: string, payload: { name?: string; publicDocsBaseUrl?: string; useLocalDocsBaseUrl?: boolean; forumMode?: boolean }) {
  return sessionRequest<any>(`/api/settings/databases/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function deleteDatabaseProfile(id: string) {
  return sessionRequest<any>(`/api/settings/databases/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  });
}

export async function fetchIngestionHistory() {
  return sessionRequest<{ collection: string; items: any[] }>('/api/documents/ingestion-history');
}
