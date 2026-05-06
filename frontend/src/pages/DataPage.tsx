import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { GraphExplorer } from '../components/GraphExplorer';
import {
  fetchIngestionSources, fetchIngestionJobs, fetchSourceAudit,
  triggerIngestionJob, fetchGraphExport, fetchServiceHealth,
  fetchDataMongoCollections, fetchDataMongoList, queryDataMongo, fetchDataPgTables, buildSemanticEdges, browseFiles, fetchFileContent,
  fetchOpenPlannerProxy, postOpenPlannerProxy,
} from '../lib/nextApi';
import { buildKnoxxAuthHeaders } from '../lib/api';
import type { IngestionSource, IngestionJob, SourceAudit, ServiceHealth } from '../lib/nextApi';
import type { GraphExportNode, GraphExportEdge } from '../lib/types';

// ── Helpers ───────────────────────────────────────────────────────────────

function timeAgo(ts: string | null): string {
  if (!ts) return 'never';
  const diff = Date.now() - new Date(ts).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hours = Math.floor(mins / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

function sc(status: string): string {
  switch (status) {
    case 'completed': case 'ok': return 'text-emerald-400';
    case 'running': return 'text-blue-400';
    case 'pending': return 'text-amber-400';
    case 'failed': return 'text-rose-400';
    default: return 'text-slate-400';
  }
}

function sd(status: string): string {
  switch (status) {
    case 'completed': case 'ok': return 'bg-emerald-400';
    case 'running': return 'bg-blue-400 animate-pulse';
    case 'pending': return 'bg-amber-400';
    case 'failed': return 'bg-rose-400';
    default: return 'bg-slate-600';
  }
}

function fmt(n: number): string {
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`;
  if (n >= 1000) return `${(n / 1000).toFixed(1)}K`;
  return String(n);
}

function nodeIcon(kind: string): string {
  if (kind.includes('file') || kind === 'file') return '📄';
  if (kind.includes('url') || kind === 'url') return '🔗';
  if (kind.includes('session') || kind.includes('pi')) return '💬';
  if (kind.includes('user')) return '👤';
  return '◆';
}

function nodeColor(kind: string): string {
  if (kind.includes('file') || kind === 'file') return 'text-emerald-400';
  if (kind.includes('url') || kind === 'url') return 'text-blue-400';
  if (kind.includes('session') || kind.includes('pi')) return 'text-purple-400';
  return 'text-slate-300';
}

function edgeLabel(et: string): string {
  return et.replace(/_/g, ' ').replace(/^(.)/, c => c.toUpperCase());
}

const OPENPLANNER_JOB_TRIGGERS: Array<{ id: string; title: string; path: string; defaultBody: any; desc: string }> = [
  { id: 'semantic_edges_full', title: 'Build Semantic Edges (full)', path: 'jobs/build-semantic-edges', defaultBody: { k: 8, minSimilarity: 0.3 }, desc: 'Mongot-native kNN semantic edge builder' },
  { id: 'semantic_edges_incremental', title: 'Build Semantic Edges (incremental)', path: 'jobs/build-semantic-edges/incremental', defaultBody: {}, desc: 'Incremental semantic edge build (experimental)' },
  { id: 'strip_chunk_content', title: 'Strip Chunk Content', path: 'jobs/strip-chunk-content', defaultBody: {}, desc: 'Reduce stored chunk payloads (space/safety)' },
  { id: 'seed_layout', title: 'Seed Layout', path: 'jobs/seed-layout', defaultBody: {}, desc: 'Seed graph layout overrides for stability' },
  { id: 'backfill_embeddings', title: 'Backfill Embeddings', path: 'jobs/backfill/embeddings', defaultBody: {}, desc: 'Rebuild embeddings across events' },
  { id: 'backfill_graph_node_embeddings', title: 'Backfill Graph Node Embeddings', path: 'jobs/backfill/graph-node-embeddings', defaultBody: {}, desc: 'Populate graph_node_embeddings for graph.node events' },
  { id: 'backfill_graph_edges', title: 'Backfill Graph Edges', path: 'jobs/backfill/graph-edges', defaultBody: {}, desc: 'Reproject historical graph.edge events into graph_edges' },
  { id: 'compact_semantic', title: 'Semantic Compaction', path: 'jobs/compact/semantic', defaultBody: {}, desc: 'Semantic compaction job (async)' },
];

// ── Small Components ──────────────────────────────────────────────────────

function Badge({ children, color }: { children: React.ReactNode; color?: string }) {
  return <span className={`text-xs px-1.5 py-0.5 rounded ${color || 'bg-slate-700/50 text-slate-400'}`}>{children}</span>;
}

function HealthDot({ ok }: { ok: boolean }) {
  return <span className={`inline-block h-2.5 w-2.5 rounded-full ${ok ? 'bg-emerald-400' : 'bg-rose-400'}`} />;
}

function TabBtn({ active, onClick, icon, label }: { active: boolean; onClick: () => void; icon: string; label: string }) {
  return (
    <button onClick={onClick}
      className={`shrink-0 rounded-md px-3 py-1.5 text-xs font-medium transition whitespace-nowrap ${
        active ? 'bg-blue-500/15 text-blue-300' : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'}`}>
      <span className="mr-1">{icon}</span>{label}
    </button>
  );
}

function KV({ k, v }: { k: string; v: React.ReactNode }) {
  return (
    <div className="flex gap-2 text-xs">
      <span className="text-slate-500 shrink-0 w-28 truncate">{k}</span>
      <span className="text-slate-300 font-mono truncate">{typeof v === 'string' ? v : JSON.stringify(v)}</span>
    </div>
  );
}

// ── Tabs ──────────────────────────────────────────────────────────────────

function OverviewTab({ sources, jobs, health, graphStats, onBuildEdges }: {
  sources: IngestionSource[]; jobs: IngestionJob[]; health: ServiceHealth | null;
  graphStats: any; onBuildEdges: () => void;
}) {
  const activeJobs = jobs.filter(j => j.status === 'running' || j.status === 'pending');
  const totalDocs = graphStats?.total || 0;
  const svc = health?.services || null;

  return (
    <div className="space-y-4">
      {/* Service health */}
      <div className="grid grid-cols-3 gap-3">
        {svc && [
          { name: 'OpenPlanner', key: 'openplanner' },
          { name: 'Proxx', key: 'proxx' },
          { name: 'Ingestion', key: 'ingestion' },
        ].map(({ name, key }) => {
          const s: any = (svc as any)[key];
          if (!s) return null;
          return (
            <div key={name} className="rounded-lg border border-slate-700/50 bg-slate-800/30 px-4 py-3 flex items-center gap-3">
              <HealthDot ok={s.ok} />
              <div>
                <div className="text-sm text-slate-200">{name}</div>
                <div className="text-xs text-slate-500">{s.ok ? 'healthy' : s.error || 'down'}</div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
        {[
          { l: 'Sources', v: String(sources.length), sub: `${sources.filter(s => s.enabled).length} enabled` },
          { l: 'Active Jobs', v: String(activeJobs.length), color: activeJobs.length > 0 ? 'text-blue-400' : undefined },
          { l: 'Documents', v: fmt(totalDocs) },
          { l: 'Graph Nodes', v: graphStats?.stats ? fmt(graphStats.stats.nodes) : '…' },
          { l: 'Semantic Edges', v: graphStats?.stats ? fmt(graphStats.stats.semanticEdges) : '…', color: graphStats?.stats?.semanticEdges === 0 ? 'text-amber-400' : undefined },
        ].map(s => (
          <div key={s.l} className="rounded-lg border border-slate-700/50 bg-slate-800/40 px-4 py-3">
            <div className="text-xs text-slate-500 mb-1">{s.l}</div>
            <div className={`text-xl font-bold ${s.color || 'text-slate-100'}`}>{s.v}</div>
            {s.sub && <div className="text-xs text-slate-500 mt-0.5">{s.sub}</div>}
          </div>
        ))}
      </div>

      {/* Quick actions */}
      <div className="flex gap-2">
        <button onClick={onBuildEdges}
          className="rounded-md border border-blue-500/30 bg-blue-500/10 px-3 py-1.5 text-xs text-blue-300 hover:bg-blue-500/20 transition">
          ⚡ Rebuild Semantic Edges
        </button>
      </div>

      {/* Recent jobs */}
      <div>
        <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">Recent Jobs</h3>
        <div className="rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
          <table className="w-full text-xs">
            <thead><tr className="border-b border-slate-700/30">
              <th className="text-left px-3 py-2 text-slate-500">Status</th>
              <th className="text-left px-3 py-2 text-slate-500">Source</th>
              <th className="text-right px-3 py-2 text-slate-500">Files</th>
              <th className="text-right px-3 py-2 text-slate-500">Done</th>
              <th className="text-right px-3 py-2 text-slate-500">Fail</th>
              <th className="text-right px-3 py-2 text-slate-500">When</th>
            </tr></thead>
            <tbody>
              {jobs.slice(0, 15).map(j => {
                const src = sources.find(s => s.source_id === j.source_id);
                return (
                  <tr key={j.job_id} className="border-b border-slate-700/20 hover:bg-slate-700/10">
                    <td className="px-3 py-1.5"><div className="flex items-center gap-1.5"><span className={`h-2 w-2 rounded-full ${sd(j.status)}`} /><span className={sc(j.status)}>{j.status}</span></div></td>
                    <td className="px-3 py-1.5 text-slate-300 truncate max-w-[120px]">{src?.name || j.source_id.slice(0, 8)}</td>
                    <td className="px-3 py-1.5 text-right text-slate-400">{fmt(j.total_files)}</td>
                    <td className="px-3 py-1.5 text-right text-emerald-400">{fmt(j.processed_files)}</td>
                    <td className="px-3 py-1.5 text-right text-rose-400">{j.failed_files > 0 ? fmt(j.failed_files) : '-'}</td>
                    <td className="px-3 py-1.5 text-right text-slate-500">{timeAgo(j.created_at)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function SourcesTab({ sources, jobs, onSync }: {
  sources: IngestionSource[]; jobs: IngestionJob[]; onSync: (id: string) => void;
}) {
  const [expandedAudit, setExpandedAudit] = useState<string | null>(null);
  const [audits, setAudits] = useState<Record<string, SourceAudit>>({});

  const loadAudit = async (id: string) => {
    if (expandedAudit === id) { setExpandedAudit(null); return; }
    setExpandedAudit(id);
    if (!audits[id]) {
      try { const a = await fetchSourceAudit(id); setAudits(p => ({ ...p, [id]: a })); } catch {}
    }
  };

  return (
    <div className="space-y-2">
      {sources.map(s => {
        const sJobs = jobs.filter(j => j.source_id === s.source_id);
        const running = sJobs.find(j => j.status === 'running' || j.status === 'pending');
        const latest = sJobs.sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime())[0];
        const status = running?.status || latest?.status || 'none';
        const contractId = (s.config as any)?.contract_source_id;
        const rootPath = (s.config as any)?.root_path || (s.config as any)?.['root-path'];
        const audit = audits[s.source_id];

        return (
          <div key={s.source_id} className="rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
            <div className="flex items-center justify-between px-4 py-3">
              <div className="flex items-center gap-3 min-w-0">
                <span className={`h-2.5 w-2.5 rounded-full shrink-0 ${sd(status)}`} />
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium text-slate-200 truncate">{s.name}</span>
                    <Badge>{s.driver_type}</Badge>
                    {contractId && <Badge color="bg-emerald-500/10 text-emerald-400">{contractId}</Badge>}
                  </div>
                  <div className="flex items-center gap-3 mt-0.5">
                    <span className="text-xs text-slate-500 truncate">{rootPath || 'no path'}</span>
                    <span className={`text-xs ${sc(status)}`}>{status}{latest ? ` · ${timeAgo(latest.created_at)}` : ''}</span>
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-2 shrink-0">
                {running && <span className="text-xs text-blue-400">{fmt(running.processed_files)}/{fmt(running.total_files)}</span>}
                <button onClick={() => loadAudit(s.source_id)} className="text-xs text-slate-500 hover:text-slate-300 px-2 py-1 rounded hover:bg-slate-700/50">
                  {expandedAudit === s.source_id ? '▾' : '▸'} audit
                </button>
                <button onClick={() => onSync(s.source_id)} disabled={status === 'running'}
                  className="text-xs text-blue-400 hover:text-blue-300 disabled:text-slate-600 disabled:cursor-not-allowed px-2 py-1 rounded hover:bg-slate-700/50">
                  sync
                </button>
              </div>
            </div>
            {expandedAudit === s.source_id && audit && (
              <div className="px-4 py-3 border-t border-slate-700/30 bg-slate-900/30">
                <div className="grid grid-cols-5 gap-2 text-center text-xs">
                  <div><div className="text-sm font-medium text-slate-300">{fmt(audit.matching_files)}</div><div className="text-slate-500">matched</div></div>
                  <div><div className="text-sm font-medium text-emerald-400">{fmt(audit.state_ingested_files)}</div><div className="text-slate-500">ingested</div></div>
                  <div><div className="text-sm font-medium text-blue-400">{fmt(audit.openplanner_documents)}</div><div className="text-slate-500">in OP</div></div>
                  <div><div className="text-sm font-medium text-amber-400">{fmt(audit.state_failed_files)}</div><div className="text-slate-500">failed</div></div>
                  <div><div className={`text-sm font-medium ${audit.coverage_delta > 0 ? 'text-amber-400' : 'text-emerald-400'}`}>{audit.coverage_delta > 0 ? `+${fmt(audit.coverage_delta)}` : '✓'}</div><div className="text-slate-500">delta</div></div>
                </div>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

function FileExplorerTab() {
  const [browseData, setBrowseData] = useState<any>(null);
  const [previewData, setPreviewData] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const [currentPath, setCurrentPath] = useState('');

  const loadDir = async (path = '') => {
    setLoading(true);
    setCurrentPath(path);
    setPreviewData(null);
    try {
      const params = new URLSearchParams();
      if (path) params.set('path', path);
      const res = await fetch(`/api/ingestion/browse?${params.toString()}`);
      if (!res.ok) throw new Error(`Browse failed: ${res.status}`);
      setBrowseData(await res.json());
    } catch (e: any) {
      setBrowseData(null);
      setPreviewData({ content: `Error: ${e.message}` });
    } finally {
      setLoading(false);
    }
  };

  const previewFile = async (path: string) => {
    try {
      const res = await fetch(`/api/ingestion/file?path=${encodeURIComponent(path)}`);
      if (!res.ok) throw new Error(`Preview failed: ${res.status}`);
      setPreviewData(await res.json());
    } catch (e: any) {
      setPreviewData({ content: `Error: ${e.message}`, path });
    }
  };

  useEffect(() => { loadDir(''); }, []);

  const entries = browseData?.entries || [];
  const seen = new Set<string>();
  const dirs = entries.filter((e: any) => {
    if (e.type !== 'dir') return false;
    const key = e.path || e.name;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
  const files = entries.filter((e: any) => {
    if (e.type === 'dir') return false;
    const key = e.path || e.name;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
  const parentPath = currentPath.includes('/') ? currentPath.split('/').slice(0, -1).join('/') : '';

  return (
    <div className="flex gap-4 h-[600px]">
      {/* Directory listing */}
      <div className="w-1/2 flex flex-col rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
        {/* Path bar */}
        <div className="shrink-0 px-3 py-2 border-b border-slate-700/30 flex items-center gap-2 text-xs">
          {browseData?.workspace_root && (
            <span className="text-slate-600 truncate shrink-0 max-w-[200px]" title={browseData.workspace_root}>
              {browseData.workspace_root.split('/').pop()}/
            </span>
          )}
          <span className="text-slate-400 truncate">{currentPath || '(root)'}</span>
        </div>
        {/* Entries */}
        <div className="flex-1 overflow-y-auto">
          {loading ? (
            <div className="p-4 text-xs text-slate-500">Loading...</div>
          ) : (
            <div className="divide-y divide-slate-700/20">
              {currentPath && (
                <button onClick={() => loadDir(parentPath)}
                  className="w-full flex items-center gap-2 px-3 py-1.5 hover:bg-slate-700/20 text-left text-xs text-slate-500">
                  <span>▸</span> ..
                </button>
              )}
              {dirs.map((e: any) => (
                <button key={e.path} onClick={() => loadDir(e.path)}
                  className="w-full flex items-center gap-2 px-3 py-1.5 hover:bg-slate-700/20 text-left">
                  <span className="text-slate-400 shrink-0">▸</span>
                  <span className="text-xs text-slate-200 truncate">{e.name}</span>
                  {e.ingestion_status && e.ingestion_status !== 'not_ingested' && (
                    <span className={`text-xs shrink-0 ml-auto ${
                      e.ingestion_status === 'ingested' ? 'text-emerald-500' :
                      e.ingestion_status === 'failed' ? 'text-rose-500' : 'text-amber-500'
                    }`}>●</span>
                  )}
                </button>
              ))}
              {files.map((e: any) => (
                <button key={e.path} onClick={() => previewFile(e.path)}
                  className={`w-full flex items-center gap-2 px-3 py-1.5 hover:bg-slate-700/20 text-left ${
                    previewData?.path === e.path ? 'bg-slate-700/30' : ''
                  }`}>
                  <span className="text-slate-600 shrink-0">·</span>
                  <span className="text-xs text-slate-300 truncate">{e.name}</span>
                  {e.size != null && <span className="text-xs text-slate-600 ml-auto shrink-0">{fmt(e.size)}</span>}
                  {e.ingestion_status && e.ingestion_status !== 'not_ingested' && (
                    <span className={`text-xs shrink-0 ${
                      e.ingestion_status === 'ingested' ? 'text-emerald-500' :
                      e.ingestion_status === 'failed' ? 'text-rose-500' : 'text-amber-500'
                    }`}>●</span>
                  )}
                </button>
              ))}
              {entries.length === 0 && !loading && (
                <div className="p-4 text-xs text-slate-500 text-center">Empty directory</div>
              )}
            </div>
          )}
        </div>
        <div className="shrink-0 px-3 py-1.5 border-t border-slate-700/30 text-xs text-slate-600">
          {dirs.length} dirs, {files.length} files
        </div>
      </div>

      {/* File preview */}
      <div className="w-1/2 flex flex-col rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
        <div className="shrink-0 px-3 py-2 border-b border-slate-700/30 flex items-center justify-between">
          <span className="text-xs text-slate-500 truncate">
            {previewData?.path || 'Select a file'}
          </span>
          {previewData?.size != null && (
            <span className="text-xs text-slate-600">{fmt(previewData.size)}{previewData.truncated ? ' (truncated)' : ''}</span>
          )}
        </div>
        <div className="flex-1 overflow-auto p-3">
          {previewData?.content ? (
            <pre className="text-xs text-slate-300 font-mono whitespace-pre-wrap break-words">{previewData.content}</pre>
          ) : previewData ? (
            <div className="text-xs text-slate-600">No content</div>
          ) : (
            <div className="text-xs text-slate-600">Click a file to preview</div>
          )}
        </div>
      </div>
    </div>
  );
}

function DocumentsTab() {
  const [docs, setDocs] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [project, setProject] = useState('');
  const [kind, setKind] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (project) params.set('project', project);
      if (kind) params.set('kind', kind);
      params.set('limit', '100');
      const res = await fetch(`/api/data/op/documents?${params.toString()}`);
      if (res.ok) {
        const data = await res.json();
        setDocs({ documents: data.rows || [], total: data.total || 0 });
      } else setDocs(null);
    } catch { setDocs(null); }
    finally { setLoading(false); }
  }, [project, kind]);

  useEffect(() => { load(); }, [load]);

  return (
    <div className="space-y-3">
      <div className="flex gap-2">
        <input value={project} onChange={e => setProject(e.target.value)} placeholder="Filter by project..."
          className="bg-slate-900/50 border border-slate-700/50 rounded px-3 py-1.5 text-xs text-slate-200 placeholder-slate-600 focus:outline-none focus:border-slate-600 w-40" />
        <select value={kind} onChange={e => setKind(e.target.value)}
          className="bg-slate-900/50 border border-slate-700/50 rounded px-3 py-1.5 text-xs text-slate-200 focus:outline-none">
          <option value="">All kinds</option>
          <option value="docs">docs</option>
          <option value="code">code</option>
          <option value="config">config</option>
          <option value="data">data</option>
        </select>
        <button onClick={load} className="text-xs text-blue-400 px-3 py-1.5 rounded hover:bg-slate-700/50">Search</button>
      </div>
      {loading ? (
        <div className="text-xs text-slate-500">Loading...</div>
      ) : docs?.documents ? (
        <div className="rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
          <table className="w-full text-xs">
            <thead><tr className="border-b border-slate-700/30">
              <th className="text-left px-3 py-2 text-slate-500">Title</th>
              <th className="text-left px-3 py-2 text-slate-500">Kind</th>
              <th className="text-left px-3 py-2 text-slate-500">Project</th>
              <th className="text-left px-3 py-2 text-slate-500">Source</th>
              <th className="text-right px-3 py-2 text-slate-500">When</th>
            </tr></thead>
            <tbody>
              {docs.documents.map((d: any) => (
                <tr key={d.id} className="border-b border-slate-700/20 hover:bg-slate-700/10">
                  <td className="px-3 py-1.5 text-slate-300 truncate max-w-[200px]">{d.title}</td>
                  <td className="px-3 py-1.5"><Badge>{d.kind}</Badge></td>
                  <td className="px-3 py-1.5 text-slate-400">{d.project}</td>
                  <td className="px-3 py-1.5 text-slate-500">{d.source}</td>
                  <td className="px-3 py-1.5 text-right text-slate-500">{timeAgo(d.ts)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="text-xs text-slate-500">No documents found</div>
      )}
    </div>
  );
}

function GraphTab({ graphData, onSelectNode, selectedNodeId }: {
  graphData: { nodes: GraphExportNode[]; edges: GraphExportEdge[]; nodeIndex: Map<string, GraphExportNode>; edgesBySource: Map<string, GraphExportEdge[]>; edgesByTarget: Map<string, GraphExportEdge[]> } | null;
  onSelectNode: (id: string | null) => void;
  selectedNodeId: string | null;
}) {
  const [search, setSearch] = useState('');

  const filtered = useMemo(() => {
    if (!graphData) return [];
    if (!search) return graphData.nodes.slice(0, 200);
    const q = search.toLowerCase();
    return graphData.nodes.filter(n =>
      (n.label || '').toLowerCase().includes(q) ||
      (n.project || '').toLowerCase().includes(q) ||
      (n.nodeType || '').toLowerCase().includes(q)
    ).slice(0, 200);
  }, [graphData, search]);

  const selected = selectedNodeId && graphData ? graphData.nodeIndex.get(selectedNodeId) : null;
  const outEdges = selected && graphData ? graphData.edgesBySource.get(selected.id) || [] : [];
  const inEdges = selected && graphData ? graphData.edgesByTarget.get(selected.id) || [] : [];

  return (
    <div className="flex gap-4 h-[600px]">
      {/* Node list */}
      <div className="w-1/2 flex flex-col rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
        <div className="shrink-0 px-3 py-2 border-b border-slate-700/30">
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search nodes..."
            className="w-full bg-slate-900/50 border border-slate-700/50 rounded px-3 py-1.5 text-xs text-slate-200 placeholder-slate-600 focus:outline-none" />
        </div>
        <div className="flex-1 overflow-y-auto">
          {!graphData ? (
            <div className="p-4 text-xs text-slate-500">Loading graph...</div>
          ) : filtered.length === 0 ? (
            <div className="p-4 text-xs text-slate-500">No matches</div>
          ) : (
            <div className="divide-y divide-slate-700/20">
              {filtered.map(n => {
                const out = (graphData.edgesBySource.get(n.id) || []).length;
                const inn = (graphData.edgesByTarget.get(n.id) || []).length;
                return (
                  <button key={n.id} onClick={() => onSelectNode(n.id)}
                    className={`w-full flex items-center gap-2 px-3 py-1.5 hover:bg-slate-700/20 text-left ${selectedNodeId === n.id ? 'bg-slate-700/30' : ''}`}>
                    <span className="shrink-0">{nodeIcon(n.nodeType || n.kind)}</span>
                    <div className="min-w-0 flex-1">
                      <div className={`text-xs ${nodeColor(n.nodeType || n.kind)} truncate`}>{n.label || n.id}</div>
                      <div className="text-xs text-slate-600">{n.project} · {n.nodeType}</div>
                    </div>
                    <div className="shrink-0 text-xs text-slate-600">{out > 0 && `→${out}`} {inn > 0 && `←${inn}`}</div>
                  </button>
                );
              })}
            </div>
          )}
        </div>
        {graphData && (
          <div className="shrink-0 px-3 py-1.5 border-t border-slate-700/30 text-xs text-slate-600 flex justify-between">
            <span>{graphData.nodes.length} nodes, {graphData.edges.length} edges</span>
            <span>{search ? `${filtered.length} matches` : 'first 200'}</span>
          </div>
        )}
      </div>

      {/* Node detail */}
      <div className="w-1/2 rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
        {selected ? (
          <div className="h-full flex flex-col">
            <div className="shrink-0 px-4 py-3 border-b border-slate-700/30 flex items-start justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <span>{nodeIcon(selected.nodeType || selected.kind)}</span>
                  <span className={`text-sm font-medium ${nodeColor(selected.nodeType || selected.kind)}`}>{selected.label || selected.id}</span>
                </div>
                <div className="text-xs text-slate-500 mt-1">{selected.project} · {selected.nodeType} · {timeAgo(selected.ts || null)}</div>
              </div>
              <button onClick={() => onSelectNode(null)} className="text-xs text-slate-500 hover:text-slate-300">✕</button>
            </div>
            {/* Data */}
            {selected.data && Object.keys(selected.data).length > 0 && (
              <div className="shrink-0 px-4 py-2 border-b border-slate-700/30">
                <div className="text-xs text-slate-500 mb-1">Data</div>
                <div className="space-y-0.5 max-h-20 overflow-y-auto">
                  {Object.entries(selected.data).filter(([k]) => !['lake','node_id','node_type','entity_key','label'].includes(k)).slice(0, 8).map(([k, v]) => (
                    <KV key={k} k={k} v={typeof v === 'string' ? v : JSON.stringify(v)} />
                  ))}
                </div>
              </div>
            )}
            {/* Edges */}
            <div className="flex-1 overflow-y-auto">
              {outEdges.length > 0 && (
                <div className="px-4 py-2 border-b border-slate-700/30">
                  <div className="text-xs text-slate-500 mb-2">Outgoing ({outEdges.length})</div>
                  {outEdges.slice(0, 20).map(e => {
                    const t = graphData!.nodeIndex.get(e.target);
                    return (
                      <button key={e.id} onClick={() => onSelectNode(e.target)}
                        className="w-full flex items-center gap-2 px-2 py-1 rounded hover:bg-slate-700/30 text-left">
                        <span className="text-xs text-slate-600 w-24 truncate shrink-0">{edgeLabel(e.edgeType)}</span>
                        <span className="text-xs text-slate-400">→</span>
                        <span className={`text-xs ${nodeColor(t?.nodeType || '')} truncate`}>{t?.label || e.target.slice(0, 24)}</span>
                      </button>
                    );
                  })}
                </div>
              )}
              {inEdges.length > 0 && (
                <div className="px-4 py-2">
                  <div className="text-xs text-slate-500 mb-2">Incoming ({inEdges.length})</div>
                  {inEdges.slice(0, 20).map(e => {
                    const s = graphData!.nodeIndex.get(e.source);
                    return (
                      <button key={e.id} onClick={() => onSelectNode(e.source)}
                        className="w-full flex items-center gap-2 px-2 py-1 rounded hover:bg-slate-700/30 text-left">
                        <span className="text-xs text-slate-600 w-24 truncate shrink-0">{edgeLabel(e.edgeType)}</span>
                        <span className="text-xs text-slate-400">←</span>
                        <span className={`text-xs ${nodeColor(s?.nodeType || '')} truncate`}>{s?.label || e.source.slice(0, 24)}</span>
                      </button>
                    );
                  })}
                </div>
              )}
              {outEdges.length === 0 && inEdges.length === 0 && (
                <div className="p-4 text-xs text-slate-600">No connections</div>
              )}
            </div>
          </div>
        ) : (
          <div className="h-full flex items-center justify-center text-xs text-slate-600">Select a node to inspect</div>
        )}
      </div>
    </div>
  );
}

function ServicesTab({
  health,
  sources,
  syncing,
  onSync,
  onBuildEdges,
}: {
  health: ServiceHealth | null;
  sources: IngestionSource[];
  syncing: Record<string, boolean>;
  onSync: (id: string) => void;
  onBuildEdges: () => void;
}) {
  const [buildResult, setBuildResult] = useState<any>(null);
  const [building, setBuilding] = useState(false);

  const [openPlannerJobs, setOpenPlannerJobs] = useState<any[] | null>(null);
  const [openPlannerJobsError, setOpenPlannerJobsError] = useState('');
  const [runningJob, setRunningJob] = useState<string | null>(null);
  const [jobResults, setJobResults] = useState<Record<string, any>>({});
  const [jobBodies, setJobBodies] = useState<Record<string, string>>(() => (
    Object.fromEntries(
      OPENPLANNER_JOB_TRIGGERS.map((j) => [j.id, JSON.stringify(j.defaultBody ?? {}, null, 2)]),
    ) as Record<string, string>
  ));

  const loadOpenPlannerJobs = useCallback(async () => {
    setOpenPlannerJobsError('');
    try {
      const res = await fetchOpenPlannerProxy('jobs');
      setOpenPlannerJobs((res as any)?.jobs || []);
    } catch (e: any) {
      setOpenPlannerJobs(null);
      setOpenPlannerJobsError(e?.message || String(e));
    }
  }, []);

  useEffect(() => {
    void loadOpenPlannerJobs();
  }, [loadOpenPlannerJobs]);

  const runOpenPlannerJob = async (job: { id: string; path: string }) => {
    setRunningJob(job.id);
    try {
      const raw = (jobBodies[job.id] ?? '').trim();
      const body = raw ? JSON.parse(raw) : {};
      const res = await postOpenPlannerProxy(job.path, body);
      setJobResults((prev) => ({ ...prev, [job.id]: res }));
      void loadOpenPlannerJobs();
      onBuildEdges();
    } catch (e: any) {
      setJobResults((prev) => ({ ...prev, [job.id]: { error: e?.message || String(e) } }));
    } finally {
      setRunningJob(null);
    }
  };

  const handleBuild = async () => {
    setBuilding(true);
    try {
      const res = await buildSemanticEdges({ k: 8, minSimilarity: 0.3 });
      setBuildResult(res);
      onBuildEdges();
    } catch (e) { setBuildResult({ error: String(e) }); }
    finally { setBuilding(false); }
  };

  const svc = health?.services || {};
  const serviceRows = [
    { id: 'openplanner', name: 'OpenPlanner', desc: 'Document store, graph, embeddings' },
    { id: 'proxx', name: 'Proxx', desc: 'LLM proxy, embedding provider' },
    { id: 'ingestion', name: 'Ingestion', desc: 'File discovery, chunking, ingestion' },
    { id: 'graph-weaver', name: 'Graph Weaver', desc: 'GraphQL + focusedGraphView + WebGL vendor' },
    { id: 'eros-eris-field-app', name: 'Eros-Eris Field App', desc: 'Graph layout + semantic worker (always-on)' },
    { id: 'shuvcrawl', name: 'ShuvCrawl', desc: 'Browser crawler (web ingestion)' },
    { id: 'myrmex', name: 'Myrmex', desc: 'Crawler scheduler + ingestion into OpenPlanner' },
    { id: 'vexx', name: 'Vexx', desc: 'Optional acceleration service (NPU/GPU)' },
  ];
  return (
    <div className="space-y-4">
      <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Service Health</h3>
      <div className="space-y-2">
        {serviceRows.map((row) => {
          const s: any = (svc as any)[row.id];
          const ok = Boolean(s?.ok);
          const url = typeof s?.url === 'string' ? s.url : null;
          const detail = s?.detail;
          return (
            <div key={row.id} className="rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
              <div className="px-4 py-3 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <HealthDot ok={ok} />
                  <div>
                    <div className="text-sm text-slate-200">{row.name}</div>
                    <div className="text-xs text-slate-500">{row.desc}</div>
                    {url && <div className="text-[10px] text-slate-600 font-mono">{url}</div>}
                  </div>
                </div>
                <div className="text-xs text-slate-500">{s ? (ok ? 'healthy' : s.error || `status ${s.status}`) : 'unknown'}</div>
              </div>
              {detail != null && (
                <details className="border-t border-slate-700/30 bg-slate-900/20">
                  <summary className="cursor-pointer select-none px-4 py-2 text-xs text-slate-500 hover:text-slate-300">details</summary>
                  <pre className="px-4 pb-3 text-xs text-slate-300 font-mono whitespace-pre-wrap break-words">{JSON.stringify(detail, null, 2)}</pre>
                </details>
              )}
            </div>
          );
        })}
      </div>

      <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mt-6">Ingestion Jobs</h3>
      <div className="rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
        <table className="w-full text-xs">
          <thead className="border-b border-slate-700/30">
            <tr>
              <th className="text-left px-3 py-2 text-slate-500">Source</th>
              <th className="text-left px-3 py-2 text-slate-500">Lake</th>
              <th className="text-left px-3 py-2 text-slate-500">Enabled</th>
              <th className="text-right px-3 py-2 text-slate-500">Action</th>
            </tr>
          </thead>
          <tbody>
            {sources.map((s) => (
              <tr key={s.source_id} className="border-b border-slate-700/20 hover:bg-slate-700/10">
                <td className="px-3 py-2 text-slate-200">{s.name}</td>
                <td className="px-3 py-2 text-slate-400">{(s.config as any)?.target_lake || (s.config as any)?.['target-lake'] || '—'}</td>
                <td className="px-3 py-2 text-slate-400">{s.enabled ? 'yes' : 'no'}</td>
                <td className="px-3 py-2 text-right">
                  <button
                    onClick={() => onSync(s.source_id)}
                    disabled={!s.enabled || Boolean(syncing[s.source_id])}
                    className="rounded-md border border-blue-500/30 bg-blue-500/10 px-3 py-1.5 text-xs text-blue-300 hover:bg-blue-500/20 disabled:opacity-50"
                  >
                    {syncing[s.source_id] ? 'Running…' : 'Run'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mt-6">OpenPlanner Job Triggers</h3>
      <div className="rounded-lg border border-slate-700/50 bg-slate-800/30 px-4 py-3 space-y-3">
        {OPENPLANNER_JOB_TRIGGERS.map((job) => {
          const last = jobResults[job.id];
          const isRunning = runningJob === job.id;

          return (
            <div key={job.id} className="rounded border border-slate-700/30 bg-slate-900/20 p-3 space-y-2">
              <div className="flex items-center justify-between gap-3">
                <div>
                  <div className="text-sm text-slate-200">{job.title}</div>
                  <div className="text-xs text-slate-500">{job.desc}</div>
                  <div className="text-[10px] text-slate-600 font-mono">POST /v1/{job.path}</div>
                </div>
                <button
                  onClick={() => void runOpenPlannerJob(job)}
                  disabled={isRunning}
                  className="rounded-md border border-blue-500/30 bg-blue-500/10 px-3 py-1.5 text-xs text-blue-300 hover:bg-blue-500/20 disabled:opacity-50"
                >
                  {isRunning ? 'Running…' : 'Run'}
                </button>
              </div>
              <textarea
                value={jobBodies[job.id] ?? ''}
                onChange={(e) => setJobBodies((prev) => ({ ...prev, [job.id]: e.target.value }))}
                className="w-full bg-slate-950/40 border border-slate-700/40 rounded p-2 text-xs text-slate-200 font-mono resize-y h-20"
                spellCheck={false}
              />
              {last && (
                <div className="text-xs bg-slate-950/40 rounded p-2 font-mono whitespace-pre-wrap">
                  {last.error ? <span className="text-rose-400">{String(last.error)}</span> : JSON.stringify(last, null, 2)}
                </div>
              )}
            </div>
          );
        })}

        {/* Back-compat: keep existing semantic edges button for muscle memory */}
        <details>
          <summary className="cursor-pointer text-xs text-slate-500">Legacy trigger: Build Semantic Edges (UI wrapper)</summary>
          <div className="mt-2 flex items-center justify-between">
            <div>
              <div className="text-sm text-slate-200">Build Semantic Edges</div>
              <div className="text-xs text-slate-500">UI wrapper around /v1/jobs/build-semantic-edges</div>
            </div>
            <button onClick={handleBuild} disabled={building}
              className="rounded-md border border-blue-500/30 bg-blue-500/10 px-3 py-1.5 text-xs text-blue-300 hover:bg-blue-500/20 disabled:opacity-50">
              {building ? 'Building...' : 'Run'}
            </button>
          </div>
          {buildResult && (
            <div className="mt-2 text-xs bg-slate-900/50 rounded p-2 font-mono">
              {buildResult.error ? (
                <span className="text-rose-400">{buildResult.error}</span>
              ) : (
                <div className="space-y-0.5">
                  <div className="text-emerald-400">OK — {buildResult.nodes} nodes, {buildResult.cappedEdges || buildResult.undirectedEdges} edges (capped)</div>
                  <div className="text-slate-500">k={buildResult.k}, minSimilarity={buildResult.minSimilarity}, maxDegree={buildResult.maxDegree}</div>
                </div>
              )}
            </div>
          )}
        </details>
      </div>

      <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mt-6">OpenPlanner Job Queue</h3>
      <div className="rounded-lg border border-slate-700/50 bg-slate-800/30 px-4 py-3 space-y-3">
        <div className="flex items-center justify-between">
          <div className="text-xs text-slate-500">/v1/jobs (live queue)</div>
          <button
            onClick={() => void loadOpenPlannerJobs()}
            className="rounded-md border border-slate-700 bg-slate-800 px-3 py-1 text-xs text-slate-300 hover:bg-slate-700"
          >
            refresh
          </button>
        </div>
        {openPlannerJobsError && (
          <div className="rounded bg-rose-500/10 border border-rose-500/20 px-3 py-2 text-xs text-rose-300">{openPlannerJobsError}</div>
        )}
        {!openPlannerJobs ? (
          <div className="text-xs text-slate-600">Loading…</div>
        ) : openPlannerJobs.length === 0 ? (
          <div className="text-xs text-slate-600">No jobs in queue</div>
        ) : (
          <div className="overflow-auto rounded border border-slate-700/30">
            <table className="w-full text-xs">
              <thead className="bg-slate-900/40 text-slate-500">
                <tr>
                  <th className="text-left px-2 py-1">id</th>
                  <th className="text-left px-2 py-1">kind</th>
                  <th className="text-left px-2 py-1">status</th>
                  <th className="text-left px-2 py-1">updated</th>
                </tr>
              </thead>
              <tbody>
                {openPlannerJobs.slice(0, 50).map((j: any) => (
                  <tr key={String(j.id)} className="border-t border-slate-700/20 hover:bg-slate-700/10">
                    <td className="px-2 py-1 font-mono text-slate-300 truncate max-w-[160px]">{String(j.id)}</td>
                    <td className="px-2 py-1 text-slate-300">{String(j.kind || '')}</td>
                    <td className="px-2 py-1 text-slate-400">{String(j.status || '')}</td>
                    <td className="px-2 py-1 text-slate-600">{String(j.updated_at || j.updatedAt || '')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

function DatabaseTab() {
  const [tables, setTables] = useState<string[]>([]);
  const [selectedTable, setSelectedTable] = useState('');
  const [pgRows, setPgRows] = useState<any[]>([]);
  const [pgLoading, setPgLoading] = useState(false);
  const [mongoStats, setMongoStats] = useState<any>(null);

  const [mongoCollections, setMongoCollections] = useState<Array<{ name: string; count: number; type?: string }>>([]);
  const [selectedCollection, setSelectedCollection] = useState('');
  const [mongoRows, setMongoRows] = useState<any[]>([]);
  const [mongoSelectedRow, setMongoSelectedRow] = useState<any>(null);
  const [mongoLoading, setMongoLoading] = useState(false);
  const [mongoError, setMongoError] = useState('');
  const [mongoQueryInfo, setMongoQueryInfo] = useState('');
  const [mongoFilterText, setMongoFilterText] = useState('{}');
  const [mongoSortText, setMongoSortText] = useState('{"_id": -1}');
  const [mongoProjectionText, setMongoProjectionText] = useState('');
  const [mongoLimit, setMongoLimit] = useState(50);
  const [mongoSkip, setMongoSkip] = useState(0);

  const [rawSql, setRawSql] = useState('');
  const [pgError, setPgError] = useState('');
  const [pgQueryInfo, setPgQueryInfo] = useState('');

  useEffect(() => {
    fetchDataPgTables().then(d => setTables(d.tables || [])).catch(() => {});
    fetchDataMongoCollections().then(setMongoStats).catch(() => {});
    fetchDataMongoList().then(d => setMongoCollections(d.collections || [])).catch(() => {});
  }, []);

  const runPgQuery = async (body: any, infoLabel: string) => {
    setPgLoading(true);
    setPgError('');
    setPgRows([]);
    setPgQueryInfo('');
    try {
      const res = await fetch('/api/data/pg/query', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });
      const data = await res.json();
      if (!res.ok || data.error) {
        setPgError(data.error || `Query failed (${res.status})`);
      } else {
        setPgRows(data.rows || []);
        setPgQueryInfo(`${infoLabel} — ${data.count} rows`);
      }
    } catch (e: any) {
      setPgError(e.message);
    } finally {
      setPgLoading(false);
    }
  };

  const loadTable = (table: string) => {
    setSelectedTable(table);
    setRawSql('');
    runPgQuery({ table, limit: 100 }, table);
  };

  const runRawSql = () => {
    if (!rawSql.trim()) return;
    setSelectedTable('');
    runPgQuery({ sql: rawSql, limit: 200 }, 'raw query');
  };

  const refreshMongoCollections = useCallback(async () => {
    try {
      const data = await fetchDataMongoList();
      setMongoCollections(data.collections || []);
    } catch {
      // ignore
    }
  }, []);

  const parseMongoJson = useCallback((label: string, raw: string, defaultValue: any) => {
    const trimmed = (raw || '').trim();
    if (!trimmed) return defaultValue;
    try {
      return JSON.parse(trimmed);
    } catch (e: any) {
      throw new Error(`${label} is not valid JSON: ${e.message}`);
    }
  }, []);

  const runMongoQueryFor = useCallback(async (collection: string, opts?: { skip?: number }) => {
    if (!collection) return;
    setMongoLoading(true);
    setMongoError('');
    setMongoRows([]);
    setMongoSelectedRow(null);
    setMongoQueryInfo('');
    try {
      const filter = parseMongoJson('Filter', mongoFilterText, {});
      const sort = parseMongoJson('Sort', mongoSortText, { _id: -1 });
      const projection = parseMongoJson('Projection', mongoProjectionText, undefined);
      const res = await queryDataMongo({
        collection,
        filter,
        sort,
        projection,
        limit: mongoLimit,
        skip: opts?.skip ?? mongoSkip,
      });

      if (!res.ok || (res as any).error) {
        setMongoError((res as any).error || 'Query failed');
      } else {
        setMongoRows(res.rows || []);
        setMongoQueryInfo(`${collection} — ${res.count} rows (total ${res.total}, skip ${res.skip}, limit ${res.limit})`);
      }
    } catch (e: any) {
      setMongoError(e.message);
    } finally {
      setMongoLoading(false);
    }
  }, [mongoFilterText, mongoSortText, mongoProjectionText, mongoLimit, mongoSkip, parseMongoJson]);

  const selectCollection = (name: string) => {
    setSelectedCollection(name);
    setMongoSkip(0);
    runMongoQueryFor(name, { skip: 0 });
  };

  return (
    <div className="space-y-4">
      {/* MongoDB overview */}
      <div>
        <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">MongoDB (via OpenPlanner)</h3>
        {mongoStats ? (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <div className="rounded-lg border border-slate-700/50 bg-slate-800/40 px-4 py-3">
              <div className="text-xs text-slate-500">Documents</div>
              <div className="text-xl font-bold text-slate-100">{fmt(mongoStats.documents?.total || 0)}</div>
            </div>
            <div className="rounded-lg border border-slate-700/50 bg-slate-800/40 px-4 py-3">
              <div className="text-xs text-slate-500">Graph Nodes</div>
              <div className="text-xl font-bold text-slate-100">{fmt(mongoStats.graph?.stats?.nodes || 0)}</div>
            </div>
            <div className="rounded-lg border border-slate-700/50 bg-slate-800/40 px-4 py-3">
              <div className="text-xs text-slate-500">Embeddings</div>
              <div className="text-xl font-bold text-slate-100">{fmt(mongoStats.graph?.stats?.embeddings || 0)}</div>
            </div>
            <div className="rounded-lg border border-slate-700/50 bg-slate-800/40 px-4 py-3">
              <div className="text-xs text-slate-500">Semantic Edges</div>
              <div className="text-xl font-bold text-slate-100">{fmt(mongoStats.graph?.stats?.semanticEdges || 0)}</div>
            </div>
          </div>
        ) : <div className="text-xs text-slate-500">Loading...</div>}

        {/* Raw MongoDB collection browse/query */}
        <div className="mt-3 flex gap-4">
          <div className="w-64 shrink-0">
            <div className="flex items-center justify-between mb-2">
              <div className="text-xs text-slate-500">Collections</div>
              <button onClick={refreshMongoCollections}
                className="text-xs text-blue-400 hover:text-blue-300 px-2 py-1 rounded hover:bg-slate-700/40">
                refresh
              </button>
            </div>
            <div className="rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden max-h-[360px] overflow-y-auto">
              {mongoCollections.map(c => (
                <button key={c.name} onClick={() => selectCollection(c.name)}
                  className={`w-full px-3 py-1.5 text-xs text-left hover:bg-slate-700/20 flex items-center justify-between gap-2 ${selectedCollection === c.name ? 'bg-slate-700/30 text-blue-300' : 'text-slate-400'}`}>
                  <span className="truncate">{c.name}</span>
                  <span className="text-slate-600 shrink-0">{c.count >= 0 ? fmt(c.count) : '?'}</span>
                </button>
              ))}
              {mongoCollections.length === 0 && (
                <div className="p-3 text-xs text-slate-600">No collections (or unavailable)</div>
              )}
            </div>
          </div>

          <div className="flex-1 space-y-2">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-2">
              <div>
                <div className="text-xs text-slate-500 mb-1">Filter (JSON)</div>
                <textarea value={mongoFilterText} onChange={e => setMongoFilterText(e.target.value)}
                  onKeyDown={e => { if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) runMongoQueryFor(selectedCollection); }}
                  className="w-full bg-slate-900/50 border border-slate-700/50 rounded px-3 py-2 text-xs text-slate-200 font-mono resize-none h-20 focus:outline-none focus:border-slate-600" />
              </div>
              <div>
                <div className="text-xs text-slate-500 mb-1">Sort (JSON)</div>
                <textarea value={mongoSortText} onChange={e => setMongoSortText(e.target.value)}
                  onKeyDown={e => { if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) runMongoQueryFor(selectedCollection); }}
                  className="w-full bg-slate-900/50 border border-slate-700/50 rounded px-3 py-2 text-xs text-slate-200 font-mono resize-none h-20 focus:outline-none focus:border-slate-600" />
              </div>
              <div>
                <div className="text-xs text-slate-500 mb-1">Projection (JSON, optional)</div>
                <textarea value={mongoProjectionText} onChange={e => setMongoProjectionText(e.target.value)}
                  onKeyDown={e => { if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) runMongoQueryFor(selectedCollection); }}
                  placeholder='{ "title": 1, "kind": 1 }'
                  className="w-full bg-slate-900/50 border border-slate-700/50 rounded px-3 py-2 text-xs text-slate-200 placeholder-slate-600 font-mono resize-none h-20 focus:outline-none focus:border-slate-600" />
              </div>
            </div>

            <div className="flex items-end gap-2">
              <div>
                <div className="text-xs text-slate-500 mb-1">Skip</div>
                <input type="number" value={mongoSkip} min={0} onChange={e => setMongoSkip(Math.max(0, parseInt(e.target.value || '0', 10) || 0))}
                  className="w-24 bg-slate-900/50 border border-slate-700/50 rounded px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-slate-600" />
              </div>
              <div>
                <div className="text-xs text-slate-500 mb-1">Limit</div>
                <input type="number" value={mongoLimit} min={1} max={500} onChange={e => setMongoLimit(Math.max(1, Math.min(500, parseInt(e.target.value || '50', 10) || 50)))}
                  className="w-24 bg-slate-900/50 border border-slate-700/50 rounded px-3 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-slate-600" />
              </div>
              <button onClick={() => runMongoQueryFor(selectedCollection)} disabled={mongoLoading || !selectedCollection}
                className="rounded-md border border-blue-500/30 bg-blue-500/10 px-3 py-1.5 text-xs text-blue-300 hover:bg-blue-500/20 disabled:opacity-50">
                Run Mongo Query
              </button>
            </div>

            {mongoError && (
              <div className="rounded bg-rose-500/10 border border-rose-500/20 px-3 py-2 text-xs text-rose-300">{mongoError}</div>
            )}

            <div className="rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
              {mongoLoading ? (
                <div className="p-4 text-xs text-slate-500">Loading...</div>
              ) : mongoRows.length > 0 ? (
                <div>
                  {mongoQueryInfo && <div className="px-3 py-1.5 text-xs text-slate-500 border-b border-slate-700/30">{mongoQueryInfo}</div>}
                  <div className="overflow-auto max-h-[360px]">
                    <table className="w-full text-xs">
                      <thead><tr className="border-b border-slate-700/30">
                        {Object.keys(mongoRows[0]).slice(0, 8).map(k => (
                          <th key={k} className="text-left px-3 py-2 text-slate-500 whitespace-nowrap">{k}</th>
                        ))}
                      </tr></thead>
                      <tbody>
                        {mongoRows.map((r, i) => (
                          <tr key={i} onClick={() => setMongoSelectedRow(r)}
                            className={`border-b border-slate-700/20 hover:bg-slate-700/10 cursor-pointer ${mongoSelectedRow === r ? 'bg-slate-700/20' : ''}`}>
                            {Object.values(r).slice(0, 8).map((v: any, j) => (
                              <td key={j} className="px-3 py-1.5 text-slate-300 truncate max-w-[150px]">
                                {v === null ? <span className="text-slate-600">null</span> : typeof v === 'object' ? JSON.stringify(v).slice(0, 60) : String(v)}
                              </td>
                            ))}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  {mongoSelectedRow && (
                    <div className="border-t border-slate-700/30 bg-slate-900/40 p-3">
                      <div className="text-xs text-slate-500 mb-2">Selected document</div>
                      <pre className="text-xs text-slate-300 font-mono whitespace-pre-wrap break-words">{JSON.stringify(mongoSelectedRow, null, 2)}</pre>
                    </div>
                  )}
                </div>
              ) : (
                <div className="p-4 text-xs text-slate-500">Select a collection to browse</div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* PostgreSQL tables */}
      <div>
        <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">PostgreSQL</h3>
        {/* Raw SQL query */}
        <div className="mb-3 flex gap-2">
          <textarea value={rawSql} onChange={e => setRawSql(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) runRawSql(); }}
            placeholder="SELECT * FROM ingestion_sources WHERE enabled = true..."
            className="flex-1 bg-slate-900/50 border border-slate-700/50 rounded px-3 py-1.5 text-xs text-slate-200 placeholder-slate-600 focus:outline-none focus:border-slate-600 font-mono resize-none h-16" />
          <button onClick={runRawSql} disabled={pgLoading || !rawSql.trim()}
            className="shrink-0 rounded-md border border-blue-500/30 bg-blue-500/10 px-3 py-1.5 text-xs text-blue-300 hover:bg-blue-500/20 disabled:opacity-50 self-end">
            Run SQL
          </button>
        </div>
        {pgError && (
          <div className="mb-3 rounded bg-rose-500/10 border border-rose-500/20 px-3 py-2 text-xs text-rose-300">{pgError}</div>
        )}
        <div className="flex gap-4">
          <div className="w-48 shrink-0">
            <div className="rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
              {tables.map(t => (
                <button key={t} onClick={() => loadTable(t)}
                  className={`w-full px-3 py-1.5 text-xs text-left hover:bg-slate-700/20 ${selectedTable === t ? 'bg-slate-700/30 text-blue-300' : 'text-slate-400'}`}>
                  {t}
                </button>
              ))}
            </div>
          </div>
          <div className="flex-1 rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
            {pgLoading ? (
              <div className="p-4 text-xs text-slate-500">Loading...</div>
            ) : pgRows.length > 0 ? (
              <div>
                {pgQueryInfo && <div className="px-3 py-1.5 text-xs text-slate-500 border-b border-slate-700/30">{pgQueryInfo}</div>}
                <div className="overflow-auto max-h-[500px]">
                  <table className="w-full text-xs">
                    <thead><tr className="border-b border-slate-700/30">
                      {Object.keys(pgRows[0]).slice(0, 8).map(k => (
                        <th key={k} className="text-left px-3 py-2 text-slate-500 whitespace-nowrap">{k}</th>
                      ))}
                    </tr></thead>
                  <tbody>
                    {pgRows.map((r, i) => (
                      <tr key={i} className="border-b border-slate-700/20 hover:bg-slate-700/10">
                        {Object.values(r).slice(0, 8).map((v: any, j) => (
                          <td key={j} className="px-3 py-1.5 text-slate-300 truncate max-w-[150px]">
                            {v === null ? <span className="text-slate-600">null</span> : typeof v === 'object' ? JSON.stringify(v).slice(0, 50) : String(v)}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
                </div>
              </div>
            ) : (
              <div className="p-4 text-xs text-slate-500">Select a table or run a query</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function LabelsTab() {
  const [labels, setLabels] = useState<any[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);
  const [selectedLabel, setSelectedLabel] = useState<any>(null);
  const [labeledNodes, setLabeledNodes] = useState<any[]>([]);
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({ label: '', slug: '', description: '', emoji: '', color: '#3b82f6' });
  const [applyNodeId, setApplyNodeId] = useState('');

  const loadLabels = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetchOpenPlannerProxy(`graph/labels?search=${encodeURIComponent(search)}&limit=200`);
      setLabels(res.labels ?? []);
    } catch (e) { console.error('Failed to load labels', e); }
    finally { setLoading(false); }
  }, [search]);

  const loadLabelDetail = useCallback(async (labelId: string) => {
    try {
      const [labelData, nodesData] = await Promise.all([
        fetchOpenPlannerProxy(`graph/labels/${encodeURIComponent(labelId)}`),
        fetchOpenPlannerProxy(`graph/labels/${encodeURIComponent(labelId)}/nodes?limit=100`),
      ]);
      setSelectedLabel(labelData.label);
      setLabeledNodes(nodesData.nodes ?? []);
    } catch (e) { console.error('Failed to load label detail', e); }
  }, []);

  useEffect(() => { void loadLabels(); }, [loadLabels]);

  const handleCreate = async () => {
    try {
      await postOpenPlannerProxy('graph/labels', {
        label: createForm.label,
        slug: createForm.slug || createForm.label,
        description: createForm.description,
        emoji: createForm.emoji || undefined,
        color: createForm.color || undefined,
      });
      setShowCreate(false);
      setCreateForm({ label: '', slug: '', description: '', emoji: '', color: '#3b82f6' });
      await loadLabels();
    } catch (e: any) { alert('Failed to create label: ' + e.message); }
  };

  const handleApply = async () => {
    if (!selectedLabel || !applyNodeId.trim()) return;
    try {
      await postOpenPlannerProxy(`graph/labels/${encodeURIComponent(selectedLabel.label_id)}/apply`, { node_id: applyNodeId.trim() });
      setApplyNodeId('');
      await loadLabelDetail(selectedLabel.label_id);
    } catch (e: any) { alert('Failed to apply label: ' + e.message); }
  };

  const handleRemove = async (nodeId: string) => {
    if (!selectedLabel) return;
    try {
      await postOpenPlannerProxy(`graph/labels/${encodeURIComponent(selectedLabel.label_id)}/remove`, { node_id: nodeId });
      await loadLabelDetail(selectedLabel.label_id);
    } catch (e: any) { alert('Failed to remove label: ' + e.message); }
  };

  const handleDeleteLabel = async () => {
    if (!selectedLabel) return;
    if (!confirm(`Delete label "${selectedLabel.label}"?`)) return;
    try {
      const res = await fetch(`/api/data/op/graph/labels/${encodeURIComponent(selectedLabel.label_id)}`, {
        method: 'DELETE',
        headers: { ...buildKnoxxAuthHeaders() },
      });
      if (!res.ok) throw new Error(await res.text());
      setSelectedLabel(null);
      await loadLabels();
    } catch (e: any) { alert('Failed to delete label: ' + e.message); }
  };

  return (
    <div className="flex gap-4 h-[600px]">
      {/* Label List */}
      <div className="w-80 flex flex-col rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
        <div className="shrink-0 p-3 border-b border-slate-700/30 flex items-center justify-between">
          <input
            type="text"
            placeholder="Search labels..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="flex-1 bg-slate-900/50 border border-slate-700/50 rounded px-3 py-1.5 text-xs text-slate-200 placeholder-slate-600 focus:outline-none"
          />
          <button onClick={() => setShowCreate(!showCreate)} className="ml-2 text-xs text-blue-400 hover:text-blue-300">
            {showCreate ? 'Cancel' : '+ New'}
          </button>
        </div>
        <div className="flex-1 overflow-y-auto">
          {loading ? (
            <div className="p-4 text-xs text-slate-500">Loading...</div>
          ) : labels.length === 0 ? (
            <div className="p-4 text-xs text-slate-500">No labels found</div>
          ) : (
            <div className="divide-y divide-slate-700/20">
              {labels.map((label: any) => (
                <button
                  key={label.label_id}
                  onClick={() => { setShowCreate(false); loadLabelDetail(label.label_id); }}
                  className={`w-full flex items-start gap-3 px-3 py-2.5 hover:bg-slate-700/20 text-left ${selectedLabel?.label_id === label.label_id ? 'bg-blue-500/10' : ''}`}
                >
                  <span className="text-lg shrink-0 mt-0.5">{label.emoji || '🏷️'}</span>
                  <div className="flex-1 min-w-0">
                    <div className="text-xs font-medium text-slate-200">{label.label}</div>
                    <div className="text-[10px] text-slate-500 leading-relaxed mt-0.5 break-words">{label.description || 'No description'}</div>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Detail Panel */}
      <div className="flex-1 rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden overflow-y-auto p-4">
        {showCreate ? (
          <div className="space-y-3">
            <h3 className="text-sm font-semibold text-slate-200">Create New Label</h3>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs text-slate-500 mb-1">Label Name</label>
                <input value={createForm.label} onChange={e => setCreateForm({ ...createForm, label: e.target.value })}
                  className="w-full bg-slate-900/50 border border-slate-700/50 rounded px-3 py-1.5 text-xs text-slate-200" />
              </div>
              <div>
                <label className="block text-xs text-slate-500 mb-1">Slug (optional)</label>
                <input value={createForm.slug} onChange={e => setCreateForm({ ...createForm, slug: e.target.value })}
                  className="w-full bg-slate-900/50 border border-slate-700/50 rounded px-3 py-1.5 text-xs text-slate-200" />
              </div>
            </div>
            <div>
              <label className="block text-xs text-slate-500 mb-1">Description</label>
              <textarea value={createForm.description} onChange={e => setCreateForm({ ...createForm, description: e.target.value })}
                rows={3} className="w-full bg-slate-900/50 border border-slate-700/50 rounded px-3 py-1.5 text-xs text-slate-200 resize-none" />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs text-slate-500 mb-1">Emoji</label>
                <input value={createForm.emoji} onChange={e => setCreateForm({ ...createForm, emoji: e.target.value })}
                  className="w-full bg-slate-900/50 border border-slate-700/50 rounded px-3 py-1.5 text-xs text-slate-200" placeholder="✅" />
              </div>
              <div>
                <label className="block text-xs text-slate-500 mb-1">Color</label>
                <input type="color" value={createForm.color} onChange={e => setCreateForm({ ...createForm, color: e.target.value })}
                  className="w-full h-8 rounded border border-slate-700/50" />
              </div>
            </div>
            <button onClick={handleCreate} disabled={!createForm.label.trim()}
              className="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-600 text-white rounded text-xs font-medium">
              Create Label
            </button>
          </div>
        ) : selectedLabel ? (
          <div className="space-y-4">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3">
                <span className="text-3xl">{selectedLabel.emoji || '🏷️'}</span>
                <div>
                  <h3 className="text-sm font-bold text-slate-200">{selectedLabel.label}</h3>
                  <code className="text-[10px] text-slate-500">{selectedLabel.label_id}</code>
                </div>
              </div>
              <button onClick={handleDeleteLabel} className="text-xs text-rose-400 hover:text-rose-300">
                Delete
              </button>
            </div>

            <div className="bg-slate-900/30 rounded border border-slate-700/30 p-3">
              <div className="text-xs text-slate-500 mb-1">Description</div>
              <div className="text-xs text-slate-300 leading-relaxed whitespace-pre-wrap">{selectedLabel.description || 'No description'}</div>
            </div>

            <div className="bg-slate-900/30 rounded border border-slate-700/30 p-3">
              <div className="text-xs text-slate-500 mb-2">Apply to Node</div>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={applyNodeId}
                  onChange={(e) => setApplyNodeId(e.target.value)}
                  placeholder="node_id or event_id"
                  className="flex-1 bg-slate-900/50 border border-slate-700/50 rounded px-3 py-1.5 text-xs text-slate-200"
                />
                <button onClick={handleApply} disabled={!applyNodeId.trim()}
                  className="px-3 py-1.5 bg-green-600 hover:bg-green-700 disabled:bg-slate-600 text-white rounded text-xs">
                  Apply
                </button>
              </div>
            </div>

            <div>
              <div className="text-xs text-slate-500 mb-2">Labeled Nodes ({labeledNodes.length})</div>
              {labeledNodes.length === 0 ? (
                <div className="text-xs text-slate-600">No nodes have this label yet</div>
              ) : (
                <div className="space-y-2">
                  {labeledNodes.map((node: any) => {
                    const evt = node.event;
                    const isDiscord = evt?.source === 'discord' || evt?.kind?.startsWith('discord');
                    const title = evt?.title || evt?.data?.title || (isDiscord ? `#${evt?.extra?.channel_id || '?'}` : node.node_id);
                    const type = evt?.kind || evt?.type || evt?.event_type || 'unknown';
                    const snippet = evt?.text || evt?.message || evt?.data?.message || evt?.data?.content || evt?.data?.text || '';
                    const author = isDiscord ? (evt?.extra?.author_username || evt?.author || 'unknown') : (evt?.author || evt?.role || '');
                    const created = evt?.ts ? new Date(evt.ts).toLocaleDateString() : (evt?.created_at ? new Date(evt.created_at).toLocaleDateString() : '');
                    return (
                      <div key={node.node_id} className="p-3 bg-slate-900/20 rounded border border-slate-700/20">
                        <div className="flex items-start justify-between gap-2">
                          <div className="flex-1 min-w-0">
                            <div className="text-xs font-medium text-slate-200 truncate">{title}</div>
                            <div className="flex items-center gap-2 mt-0.5">
                              <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-700/50 text-slate-400">{type}</span>
                              {author && <span className="text-[10px] text-slate-500">@{author}</span>}
                              {created && <span className="text-[10px] text-slate-500">{created}</span>}
                            </div>
                            {snippet && (
                              <div className="text-[11px] text-slate-300 mt-1.5 leading-relaxed whitespace-pre-wrap">
                                {snippet}
                              </div>
                            )}
                          </div>
                          <button onClick={() => handleRemove(node.node_id)}
                            className="shrink-0 text-xs text-rose-400 hover:text-rose-300">
                            Remove
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        ) : (
          <div className="h-full flex items-center justify-center text-xs text-slate-500">
            Select a label to view details
          </div>
        )}
      </div>
    </div>
  );
}

// ── Main Page ─────────────────────────────────────────────────────────────

const TABS = [
  { id: 'overview', label: 'Overview', icon: '◈' },
  { id: 'sources', label: 'Sources', icon: '◉' },
  { id: 'files', label: 'Files', icon: '📁' },
  { id: 'documents', label: 'Documents', icon: '📄' },
  { id: 'graph', label: 'Graph', icon: '◇' },
  { id: 'labels', label: 'Labels', icon: '🏷️' },
  { id: 'database', label: 'Database', icon: '▣' },
  { id: 'services', label: 'Services', icon: '⚡' },
] as const;

type TabId = typeof TABS[number]['id'];

export default function DataPage() {
  const { tab: urlTab } = useParams<{ tab?: string }>();
  const navigate = useNavigate();
  const tab: TabId = TABS.find(t => t.id === urlTab)?.id ?? 'overview';

  const [sources, setSources] = useState<IngestionSource[]>([]);
  const [jobs, setJobs] = useState<IngestionJob[]>([]);
  const [health, setHealth] = useState<ServiceHealth | null>(null);
  const [graphStats, setGraphStats] = useState<any>(null);
  const [graphData, setGraphData] = useState<any>(null);
  const [syncing, setSyncing] = useState<Record<string, boolean>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadAll = useCallback(async () => {
    try {
      const [srcRes, jobRes, healthRes, mongoRes] = await Promise.allSettled([
        fetchIngestionSources(), fetchIngestionJobs(), fetchServiceHealth(),
        fetchDataMongoCollections(),
      ]);
      setSources(srcRes.status === 'fulfilled' ? srcRes.value : []);
      setJobs(jobRes.status === 'fulfilled' ? jobRes.value : []);
      setHealth(healthRes.status === 'fulfilled' ? healthRes.value : null);
      if (mongoRes.status === 'fulfilled' && mongoRes.value) {
        setGraphStats({ total: mongoRes.value.documents?.total || 0, ...mongoRes.value.documents });
      }
    } catch (e) { setError(String(e)); }
    finally { setLoading(false); }
  }, []);

  const loadGraph = useCallback(async () => {
    try {
      const res = await fetchGraphExport({});
      if (res.ok) {
        const nodeIndex = new Map<string, GraphExportNode>();
        for (const n of res.nodes) nodeIndex.set(n.id, n);
        const edgesBySource = new Map<string, GraphExportEdge[]>();
        const edgesByTarget = new Map<string, GraphExportEdge[]>();
        for (const e of res.edges) {
          if (!edgesBySource.has(e.source)) edgesBySource.set(e.source, []);
          edgesBySource.get(e.source)!.push(e);
          if (!edgesByTarget.has(e.target)) edgesByTarget.set(e.target, []);
          edgesByTarget.get(e.target)!.push(e);
        }
        setGraphData({ nodes: res.nodes, edges: res.edges, nodeIndex, edgesBySource, edgesByTarget });
      }
    } catch {}
  }, []);

  useEffect(() => { loadAll(); }, []);
  useEffect(() => { const i = setInterval(loadAll, 15000); return () => clearInterval(i); }, [loadAll]);

  // Lazy-load graph only when viewing graph tab
  useEffect(() => {
    if (tab === 'graph' && !graphData) {
      loadGraph();
    }
  }, [tab, graphData, loadGraph]);

  const handleSync = async (sourceId: string) => {
    setSyncing(p => ({ ...p, [sourceId]: true }));
    try { await triggerIngestionJob(sourceId); await loadAll(); }
    catch (e) { setError(String(e)); }
    finally { setSyncing(p => ({ ...p, [sourceId]: false })); }
  };

  const handleBuildEdges = () => { loadGraph(); loadAll(); };
  const setTab = (id: TabId) => navigate(`/data/${id}`);

  if (loading) return <div className="p-6 text-sm text-slate-400">Loading data...</div>;

  return (
    <div className="flex flex-1 flex-col overflow-hidden">
      <div className="shrink-0 border-b border-slate-700/50 bg-slate-900/80 px-6 py-3">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-base font-bold text-slate-100">Data</h1>
            <p className="text-xs text-slate-500 mt-0.5">Sources → Ingestion → OpenPlanner · Files · Documents · Graph · Database</p>
          </div>
          <button onClick={() => { loadAll(); if (tab === 'graph') loadGraph(); }}
            className="rounded-md border border-slate-700 bg-slate-800 px-3 py-1 text-xs text-slate-300 hover:bg-slate-700">Refresh All</button>
        </div>
        <nav className="mt-2 flex gap-1 overflow-x-auto">
          {TABS.map(t => <TabBtn key={t.id} active={tab === t.id} onClick={() => setTab(t.id)} icon={t.icon} label={t.label} />)}
        </nav>
      </div>

      {error && (
        <div className="shrink-0 px-6 py-2">
          <div className="rounded-lg bg-rose-500/10 px-3 py-2 text-xs text-rose-300 border border-rose-500/20">{error}</div>
        </div>
      )}

      <main className={tab === 'graph' ? 'flex-1 overflow-hidden p-0' : 'flex-1 overflow-y-auto p-6'}>
        {tab === 'overview' && <OverviewTab sources={sources} jobs={jobs} health={health} graphStats={graphStats} onBuildEdges={handleBuildEdges} />}
        {tab === 'sources' && <SourcesTab sources={sources} jobs={jobs} onSync={handleSync} />}
        {tab === 'files' && <FileExplorerTab />}
        {tab === 'documents' && <DocumentsTab />}
        {tab === 'graph' && <GraphExplorer nodes={graphData?.nodes || []} />}
        {tab === 'labels' && <LabelsTab />}
        {tab === 'database' && <DatabaseTab />}
        {tab === 'services' && (
          <ServicesTab
            health={health}
            sources={sources}
            syncing={syncing}
            onSync={handleSync}
            onBuildEdges={handleBuildEdges}
          />
        )}
      </main>
    </div>
  );
}
