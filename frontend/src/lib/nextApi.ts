export * from "./api";
import { buildKnoxxAuthHeaders } from "./api";
import type { GraphExportResponse } from './types';

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

export async function knoxxSessionStatus(sessionId: string, conversationId?: string | null) {
  const params = new URLSearchParams({ session_id: sessionId });
  if (conversationId) {
    params.set('conversation_id', conversationId);
  }
  const res = await fetch(`/api/knoxx/session/status?${params.toString()}`, {
    headers: buildKnoxxAuthHeaders(),
  });
  if (!res.ok) throw new Error('Failed to check session status');
  return res.json() as Promise<{
    session_id: string;
    conversation_id: string;
    status: string;
    has_active_stream: boolean;
    can_send: boolean;
    reason?: string;
    model?: string;
    updated_at?: string;
  }>;
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

export async function fetchGraphExport(params: {
  projects?: string[];
  nodeTypes?: string[];
  edgeTypes?: string[];
} = {}) {
  const query = new URLSearchParams();

  if (params.projects && params.projects.length > 0) {
    query.set('projects', params.projects.join(','));
  }

  if (params.nodeTypes && params.nodeTypes.length > 0) {
    query.set('nodeTypes', params.nodeTypes.join(','));
  }

  if (params.edgeTypes && params.edgeTypes.length > 0) {
    query.set('edgeTypes', params.edgeTypes.join(','));
  }

  return sessionRequest<GraphExportResponse>(`/api/graph/export${query.size > 0 ? `?${query.toString()}` : ''}`);
}

// ── Ingestion proxy API ──────────────────────────────────────────────────

export interface IngestionSource {
  source_id: string;
  tenant_id: string;
  driver_type: string;
  name: string;
  config: Record<string, unknown>;
  state: Record<string, unknown>;
  collections?: string[];
  file_types?: string[];
  include_patterns?: string[];
  exclude_patterns?: string[];
  last_scan_at: string | null;
  last_error: string | null;
  enabled: boolean;
  created_at: string;
  updated_at: string;
}

export interface IngestionJob {
  job_id: string;
  source_id: string;
  tenant_id: string;
  status: 'pending' | 'running' | 'completed' | 'failed' | 'cancelled';
  total_files: number;
  processed_files: number;
  failed_files: number;
  skipped_files: number;
  chunks_created: number;
  started_at: string | null;
  completed_at: string | null;
  error_message: string | null;
  created_at: string;
}

export interface SourceAudit {
  source_id: string;
  tenant_id: string;
  driver_type: string;
  root_path: string | null;
  collections: string[];
  file_types: string[];
  matching_files: number;
  new_files: number;
  changed_files: number;
  unchanged_files: number;
  state_ingested_files: number;
  state_failed_files: number;
  openplanner_documents: number;
  coverage_delta: number;
}

async function ingestionRequest<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = buildKnoxxAuthHeaders(init?.headers);
  headers.set('x-knoxx-session-id', getKnoxxSessionId());
  const res = await fetch(`/api/ingestion-proxy/${path}`, { ...init, headers });
  if (!res.ok) {
    const text = await res.text();
    throw new ProxyApiError(res.status, text || `Ingestion request failed: ${res.status}`);
  }
  return res.json() as Promise<T>;
}

export async function fetchIngestionSources(): Promise<IngestionSource[]> {
  return ingestionRequest<IngestionSource[]>('sources');
}

export async function fetchIngestionJobs(tenantId?: string): Promise<IngestionJob[]> {
  const qs = tenantId ? `?tenant_id=${encodeURIComponent(tenantId)}` : '';
  return ingestionRequest<IngestionJob[]>(`jobs${qs}`);
}

export async function fetchSourceAudit(sourceId: string): Promise<SourceAudit> {
  return ingestionRequest<SourceAudit>(`sources/${encodeURIComponent(sourceId)}/audit`);
}

export async function triggerIngestionJob(sourceId: string): Promise<IngestionJob> {
  return ingestionRequest<IngestionJob>('jobs', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ source_id: sourceId }),
  });
}

export async function cancelIngestionJob(jobId: string): Promise<void> {
  await ingestionRequest<void>(`jobs/${encodeURIComponent(jobId)}/cancel`, { method: 'POST' });
}

// ── Data Explorer API ────────────────────────────────────────────────────

export interface ServiceHealth {
  ok: boolean;
  services: Record<string, { ok: boolean; status?: number; error?: string; url?: string }>;
}

export async function fetchServiceHealth(): Promise<ServiceHealth> {
  return sessionRequest<ServiceHealth>('/api/data/health');
}

export async function fetchDataMongoCollections(): Promise<{ ok: boolean; documents: any; graph: any }> {
  return sessionRequest('/api/data/mongo/collections');
}

export interface MongoCollectionInfo {
  name: string;
  count: number;
  type?: string;
}

export async function fetchDataMongoList(): Promise<{ ok: boolean; collections: MongoCollectionInfo[] }> {
  return sessionRequest('/api/data/mongo/list');
}

export async function queryDataMongo(payload: {
  collection: string;
  filter?: Record<string, unknown>;
  sort?: Record<string, 1 | -1>;
  projection?: Record<string, number>;
  limit?: number;
  skip?: number;
}): Promise<{ ok: boolean; collection: string; count: number; total: number; skip: number; limit: number; rows: any[]; error?: string }> {
  return sessionRequest('/api/data/mongo/query', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
}

export async function fetchDataPgTables(): Promise<{ ok: boolean; tables: string[] }> {
  return sessionRequest('/api/data/pg/tables');
}

export async function fetchOpenPlannerProxy(path: string): Promise<any> {
  // Knoxx backend exposes an OpenPlanner /v1/* proxy under /api/data/op/*
  return sessionRequest(`/api/data/op/${path}`);
}

export async function postOpenPlannerProxy(path: string, body: any): Promise<any> {
  return sessionRequest(`/api/data/op/${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

export async function buildSemanticEdges(opts: { k?: number; minSimilarity?: number } = {}): Promise<any> {
  return sessionRequest('/api/data/jobs/build-semantic-edges', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(opts),
  });
}

export async function buildSemanticEdgesIncremental(body: any): Promise<any> {
  return sessionRequest('/api/data/jobs/build-semantic-edges/incremental', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

export async function browseFiles(path: string): Promise<any> {
  return sessionRequest(`/api/data/browse?path=${encodeURIComponent(path)}`);
}

export async function fetchFileContent(path: string): Promise<{ content: string; path: string }> {
  return sessionRequest(`/api/data/file?path=${encodeURIComponent(path)}`);
}

// ── GraphQL / Graph-weaver API ────────────────────────────────────────────

export async function graphqlQuery<T = any>(query: string, variables?: Record<string, any>): Promise<T> {
  const res = await fetch('/api/data/graphql', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...buildKnoxxAuthHeaders() },
    body: JSON.stringify({ query, variables }),
  });
  if (!res.ok) throw new Error(`GraphQL request failed: ${res.status}`);
  const data = await res.json();
  if (data.errors) throw new Error(data.errors.map((e: any) => e.message).join('; '));
  return data.data as T;
}

export async function fetchGraphWeaverStatus(): Promise<any> {
  return sessionRequest('/api/data/graph/status');
}

export async function fetchGraphViewUrl(): Promise<{ url: string }> {
  return sessionRequest('/api/data/graph/view-url');
}
