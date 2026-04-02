export * from "./api";

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

async function proxyRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers || {});
  headers.set('x-knoxx-session-id', getKnoxxSessionId());
  const res = await fetch(`/api/knoxx/proxy/${path}`, {
    ...init,
    headers,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new ProxyApiError(res.status, text || `Proxy request failed: ${res.status}`);
  }
  return res.json() as Promise<T>;
}

export async function fetchDocuments() {
  return proxyRequest<any>('documents');
}

export async function uploadDocuments(files: File[], autoIngest: boolean = false) {
  const formData = new FormData();
  files.forEach(file => formData.append('files', file));
  formData.append('autoIngest', String(autoIngest));

  const headers = new Headers();
  headers.set('x-knoxx-session-id', getKnoxxSessionId());
  const res = await fetch('/api/knoxx/proxy/documents/upload', {
    method: 'POST',
    headers,
    body: formData,
  });
  if (!res.ok) throw new Error('Failed to upload documents');
  return res.json();
}

export async function deleteDocument(path: string) {
  return proxyRequest<any>(`documents/${encodeURIComponent(path)}`, { method: 'DELETE' });
}

export async function ingestDocuments(options: { full?: boolean, selectedFiles?: string[] } = {}) {
  return proxyRequest<any>('documents/ingest', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(options),
  });
}

export async function restartIngestion(forceFresh: boolean = false) {
  return proxyRequest<any>('documents/ingest/restart', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ forceFresh }),
  });
}

export async function fetchIngestionStatus() {
  return proxyRequest<any>('documents/ingestion-status');
}

export async function fetchIngestionProgress() {
  return proxyRequest<any>('documents/ingestion-progress');
}

export async function getSettings() {
  return proxyRequest<any>('settings');
}

export async function updateSettings(settings: any) {
  return proxyRequest<any>('settings', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(settings),
  });
}

export async function getKnoxxStatus() {
  return proxyRequest<any>('settings/knoxx-status');
}

export async function knoxxRagChat(payload: {
  message: string;
  conversationId?: string | null;
  includeCompare?: boolean;
}) {
  return proxyRequest<any>('chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function knoxxDirectChat(payload: {
  message: string;
  conversationId?: string | null;
}) {
  return proxyRequest<any>('llm/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function fetchRetrievalStats() {
  return proxyRequest<any>('chat/stats');
}

export async function runRetrievalDebug(payload: { message: string; topK?: number }) {
  return proxyRequest<any>('chat/retrieval-debug', {
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
  return proxyRequest<{ content: string; path: string }>(`documents/content/${encoded}`);
}

export async function listDatabaseProfiles() {
  return proxyRequest<{
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
  }>('settings/databases');
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
  return proxyRequest<any>('settings/databases', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function makeDatabasePrivate(id: string) {
  return proxyRequest<any>(`settings/databases/${encodeURIComponent(id)}/make-private`, {
    method: 'POST',
  });
}

export async function activateDatabaseProfile(id: string) {
  return proxyRequest<any>('settings/databases/activate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ id }),
  });
}

export async function updateDatabaseProfile(id: string, payload: { name?: string; publicDocsBaseUrl?: string; useLocalDocsBaseUrl?: boolean; forumMode?: boolean }) {
  return proxyRequest<any>(`settings/databases/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function deleteDatabaseProfile(id: string) {
  return proxyRequest<any>(`settings/databases/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  });
}

export async function fetchIngestionHistory() {
  return proxyRequest<{ collection: string; items: any[] }>('documents/ingestion-history');
}
