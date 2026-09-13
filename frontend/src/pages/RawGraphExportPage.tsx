import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { opsRoutes } from '../lib/app-routes';
import { ExternalLink, Filter, Move, Network, RefreshCw } from 'lucide-react';
import {
  WebGLGraphView,
  rgba,
  type GraphNode as RenderGraphNode,
} from '@octave-commons/webgl-graph-view';
import { fetchGraphExport } from '../lib/nextApi';
import type { GraphExportResponse } from '../lib/types';
import {
  CANONICAL_LAKES,
  LAKE_COLORS,
  edgeStyle,
  inferLake,
  inferNodeType,
  nodeStyle,
  projectGraphExport,
  shortNumber,
  toggleLake,
} from './raw-graph-export/graph-helpers';

export default function VectorsPage() {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const viewRef = useRef<WebGLGraphView | null>(null);
  const [selectedLakes, setSelectedLakes] = useState<string[]>([...CANONICAL_LAKES]);
  const [maxNodes, setMaxNodes] = useState(1400);
  const [maxEdges, setMaxEdges] = useState(5200);
  const [crossLakeOnly, setCrossLakeOnly] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [payload, setPayload] = useState<GraphExportResponse | null>(null);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);

  const loadGraph = async (projects: string[]) => {
    if (projects.length === 0) {
      setPayload({ ok: true, projects: [], nodes: [], edges: [] });
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      const next = await fetchGraphExport({ projects });
      setPayload(next);
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadGraph(selectedLakes);
  }, [selectedLakes.join(',')]);

  useEffect(() => {
    if (!canvasRef.current) return undefined;

    const view = new WebGLGraphView(canvasRef.current, {
      background: rgba(0.03, 0.06, 0.11, 0.98),
      pulseAmplitude: 0.42,
      pulseSpeed: 1 / 420,
      denseNodeThreshold: 4000,
      denseEdgeThreshold: 16000,
      dprCap: { normal: 2.5, dense: 2.0 },
      frameIntervalMs: { normal: 16, dense: 24 },
      nodeStyle,
      edgeStyle,
      onNodeClick: (node: RenderGraphNode) => setSelectedNodeId(node.id),
    });

    viewRef.current = view;

    return () => {
      view.destroy();
      viewRef.current = null;
    };
  }, []);

  const processed = useMemo(
    () => projectGraphExport(payload, selectedLakes, crossLakeOnly, maxNodes, maxEdges),
    [payload, selectedLakes, crossLakeOnly, maxNodes, maxEdges],
  );

  useEffect(() => {
    if (!selectedNodeId) return;
    if (!processed.nodeMap.has(selectedNodeId)) {
      setSelectedNodeId(null);
    }
  }, [processed.nodeMap, selectedNodeId]);

  useEffect(() => {
    const view = viewRef.current;
    if (!view) return;
    view.setGraph(processed.graph);
    if (processed.graph.nodes.length > 0) {
      view.fitToGraph(48);
    }
  }, [processed.graph]);

  const selectedNode = selectedNodeId ? processed.nodeMap.get(selectedNodeId) ?? null : null;
  const selectedEdges = selectedNodeId ? processed.edgesByNode.get(selectedNodeId) ?? [] : [];
  const selectedNodeData = selectedNode?.data ?? {};
  const selectedNodeUrl = typeof selectedNodeData.url === 'string' ? selectedNodeData.url : '';
  const selectedNodePath = typeof selectedNodeData.path === 'string'
    ? selectedNodeData.path
    : typeof selectedNodeData.source_path === 'string'
      ? selectedNodeData.source_path
      : '';

  return (
    <div className="mx-auto w-full max-w-7xl p-6 md:p-8 text-slate-100">
      <header className="mb-6 flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="flex items-center gap-3 text-3xl font-bold text-slate-100">
            <Network className="h-8 w-8 text-cyan-300" />
            Canonical Lake Graph
          </h1>
          <p className="mt-2 max-w-3xl text-sm text-slate-400">
            WebGL projection of the current OpenPlanner graph export, filtered through Knoxx. The canonical lake model is one lake per source: <code className="rounded bg-slate-800 px-1.5 py-0.5">devel</code>, <code className="rounded bg-slate-800 px-1.5 py-0.5">web</code>, <code className="rounded bg-slate-800 px-1.5 py-0.5">bluesky</code>, and <code className="rounded bg-slate-800 px-1.5 py-0.5">knoxx-session</code>.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => void loadGraph(selectedLakes)}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 hover:bg-slate-800"
          >
            <RefreshCw className="h-4 w-4" />
            Reload export
          </button>
          <button
            type="button"
            onClick={() => viewRef.current?.fitToGraph(48)}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 hover:bg-slate-800"
          >
            <Move className="h-4 w-4" />
            Fit graph
          </button>
          <Link to={opsRoutes.documents} className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 hover:bg-slate-800">
            Open Lakes
          </Link>
        </div>
      </header>

      <section className="mb-6 rounded-2xl border border-slate-800 bg-slate-900/80 p-4 shadow-xl">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
          <div className="space-y-3">
            <div>
              <div className="mb-2 flex items-center gap-2 text-sm font-semibold text-slate-200">
                <Filter className="h-4 w-4 text-cyan-300" />
                Lake filters
              </div>
              <div className="flex flex-wrap gap-2">
                {CANONICAL_LAKES.map((lake) => {
                  const checked = selectedLakes.includes(lake);
                  const count = processed.lakeCounts.get(lake) ?? 0;
                  const color = LAKE_COLORS[lake] || LAKE_COLORS.misc;
                  return (
                    <label
                      key={lake}
                      className={`inline-flex items-center gap-2 rounded-full border px-3 py-1 text-xs ${checked ? 'border-cyan-400/40 bg-cyan-500/10 text-cyan-100' : 'border-slate-700 bg-slate-950 text-slate-300'}`}
                    >
                      <input
                        type="checkbox"
                        checked={checked}
                        onChange={() => setSelectedLakes((current) => toggleLake(current, lake))}
                        className="h-3.5 w-3.5 rounded border-slate-600 bg-slate-900 text-cyan-400"
                      />
                      <span
                        className="inline-block h-2.5 w-2.5 rounded-full"
                        style={{ backgroundColor: `rgba(${Math.round(color[0] * 255)}, ${Math.round(color[1] * 255)}, ${Math.round(color[2] * 255)}, ${color[3]})` }}
                      />
                      <span>{lake}</span>
                      <span className="text-slate-400">{shortNumber(count)}</span>
                    </label>
                  );
                })}
              </div>
            </div>
            <label className="inline-flex items-center gap-2 text-sm text-slate-300">
              <input
                type="checkbox"
                checked={crossLakeOnly}
                onChange={(event) => setCrossLakeOnly(event.target.checked)}
                className="h-4 w-4 rounded border-slate-600 bg-slate-900 text-cyan-400"
              />
              Show only cross-lake edges
            </label>
          </div>

          <div className="grid gap-3 sm:grid-cols-2 xl:min-w-[320px]">
            <label className="block text-xs text-slate-400">
              Render nodes <span className="ml-1 text-slate-200">{shortNumber(maxNodes)}</span>
              <input
                type="range"
                min={200}
                max={6000}
                step={100}
                value={maxNodes}
                onChange={(event) => setMaxNodes(Number(event.target.value))}
                className="mt-2 w-full"
              />
            </label>
            <label className="block text-xs text-slate-400">
              Render edges <span className="ml-1 text-slate-200">{shortNumber(maxEdges)}</span>
              <input
                type="range"
                min={200}
                max={24000}
                step={200}
                value={maxEdges}
                onChange={(event) => setMaxEdges(Number(event.target.value))}
                className="mt-2 w-full"
              />
            </label>
          </div>
        </div>

        <div className="mt-4 grid gap-3 md:grid-cols-4">
          <SummaryCard label="Raw export" value={`${shortNumber(processed.rawNodeCount)} nodes`} subvalue={`${shortNumber(processed.rawEdgeCount)} edges`} />
          <SummaryCard label="Filtered lakes" value={`${shortNumber(processed.filteredNodeCount)} nodes`} subvalue={`${shortNumber(processed.filteredEdgeCount)} edges`} />
          <SummaryCard label="Rendered" value={`${shortNumber(processed.graph.nodes.length)} nodes`} subvalue={`${shortNumber(processed.graph.edges.length)} edges`} />
          <SummaryCard label="Selection" value={selectedNode ? selectedNode.label : 'No node selected'} subvalue={selectedNode ? `${inferLake(selectedNode)} / ${inferNodeType(selectedNode)}` : 'Click a node'} />
        </div>
      </section>

      {error ? (
        <div className="mb-6 rounded-xl border border-rose-500/30 bg-rose-500/10 p-4 text-rose-200">
          {error}
        </div>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_360px]">
        <section className="rounded-2xl border border-slate-800 bg-slate-900/80 p-3 shadow-2xl">
          <div className="relative h-[44rem] overflow-hidden rounded-xl border border-slate-800 bg-slate-950">
            <canvas ref={canvasRef} className="h-full w-full" />
            {loading ? (
              <div className="absolute inset-0 flex items-center justify-center bg-slate-950/80 text-sm text-slate-300">
                Loading canonical graph export…
              </div>
            ) : null}
            {!loading && processed.graph.nodes.length === 0 ? (
              <div className="absolute inset-0 flex items-center justify-center bg-slate-950/70 px-6 text-center text-sm text-slate-400">
                No nodes matched the current lake and edge filters.
              </div>
            ) : null}
          </div>
        </section>

        <aside className="space-y-6">
          <section className="rounded-2xl border border-slate-800 bg-slate-900/80 p-5 shadow-xl">
            <h2 className="mb-3 text-lg font-semibold text-slate-100">Selected node</h2>
            {selectedNode ? (
              <div className="space-y-3 text-sm">
                <div>
                  <div className="text-xs uppercase tracking-wide text-slate-500">Label</div>
                  <div className="mt-1 font-medium text-slate-100">{selectedNode.label}</div>
                </div>
                <div className="grid grid-cols-2 gap-3 text-xs">
                  <InfoCell label="Lake" value={inferLake(selectedNode)} />
                  <InfoCell label="Node type" value={inferNodeType(selectedNode)} />
                  <InfoCell label="Degree" value={String(processed.degree.get(selectedNode.id) ?? 0)} />
                  <InfoCell label="Edges shown" value={String(selectedEdges.length)} />
                </div>
                <div>
                  <div className="text-xs uppercase tracking-wide text-slate-500">Node id</div>
                  <div className="mt-1 break-all rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 font-mono text-xs text-slate-300">
                    {selectedNode.id}
                  </div>
                </div>
                {selectedNodePath ? (
                  <div>
                    <div className="text-xs uppercase tracking-wide text-slate-500">Path</div>
                    <div className="mt-1 break-all rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 font-mono text-xs text-slate-300">
                      {selectedNodePath}
                    </div>
                  </div>
                ) : null}
                {selectedNodeUrl ? (
                  <div>
                    <div className="text-xs uppercase tracking-wide text-slate-500">URL</div>
                    <a
                      href={selectedNodeUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="mt-1 inline-flex break-all text-cyan-300 hover:text-cyan-200"
                    >
                      <span>{selectedNodeUrl}</span>
                      <ExternalLink className="ml-1 mt-0.5 h-3.5 w-3.5 shrink-0" />
                    </a>
                  </div>
                ) : null}
                <div>
                  <div className="text-xs uppercase tracking-wide text-slate-500">Node data</div>
                  <pre className="mt-1 max-h-80 overflow-auto rounded-lg border border-slate-800 bg-slate-950/70 p-3 text-[11px] text-slate-300">
{JSON.stringify(selectedNode.data ?? {}, null, 2)}
                  </pre>
                </div>
              </div>
            ) : (
              <p className="text-sm text-slate-400">Click a node in the canvas to inspect its lake, type, and raw export payload.</p>
            )}
          </section>

          <section className="rounded-2xl border border-slate-800 bg-slate-900/80 p-5 shadow-xl">
            <h2 className="mb-3 text-lg font-semibold text-slate-100">Rendered lake counts</h2>
            <div className="space-y-2 text-sm">
              {selectedLakes.map((lake) => (
                <div key={`rendered-${lake}`} className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-950/60 px-3 py-2">
                  <div className="flex items-center gap-2">
                    <span
                      className="inline-block h-2.5 w-2.5 rounded-full"
                      style={{ backgroundColor: `rgba(${Math.round((LAKE_COLORS[lake] || LAKE_COLORS.misc)[0] * 255)}, ${Math.round((LAKE_COLORS[lake] || LAKE_COLORS.misc)[1] * 255)}, ${Math.round((LAKE_COLORS[lake] || LAKE_COLORS.misc)[2] * 255)}, ${(LAKE_COLORS[lake] || LAKE_COLORS.misc)[3]})` }}
                    />
                    <span>{lake}</span>
                  </div>
                  <span className="text-slate-300">{shortNumber(processed.renderedLakeCounts.get(lake) ?? 0)}</span>
                </div>
              ))}
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}

function SummaryCard({ label, value, subvalue }: { label: string; value: string; subvalue: string }) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-950/60 px-4 py-3">
      <div className="text-xs uppercase tracking-wide text-slate-500">{label}</div>
      <div className="mt-1 text-sm font-semibold text-slate-100">{value}</div>
      <div className="mt-1 text-xs text-slate-400">{subvalue}</div>
    </div>
  );
}

function InfoCell({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-slate-800 bg-slate-950/60 px-3 py-2">
      <div className="text-[11px] uppercase tracking-wide text-slate-500">{label}</div>
      <div className="mt-1 text-sm text-slate-200">{value}</div>
    </div>
  );
}
