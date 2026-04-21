import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  WebGLGraphView,
  rgba,
  type EdgeStyle,
  type GraphData as RenderGraphData,
  type GraphEdge as RenderGraphEdge,
  type GraphNode as RenderGraphNode,
  type NodeStyle,
} from '@octave-commons/webgl-graph-view';
import { graphqlQuery } from '../lib/nextApi';
import type { GraphExportNode } from '../lib/types';

type GraphViewNode = {
  id: string;
  kind: string;
  label: string;
  x: number;
  y: number;
  layer: string;
  external: boolean;
  loadedByDefault: boolean;
  dataJson: string | null;
};

type GraphViewEdge = {
  source: string;
  target: string;
  kind: string;
  layer: string;
  dataJson: string | null;
};

type GraphView = {
  nodes: GraphViewNode[];
  edges: GraphViewEdge[];
  meta: { totalNodes: number; totalEdges: number; sampledNodes: boolean; sampledEdges: boolean };
};

function safeJsonParse(value: string | null): Record<string, unknown> | null {
  if (!value) return null;
  try {
    const parsed = JSON.parse(value) as unknown;
    if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) return parsed as Record<string, unknown>;
    return { value: parsed } as Record<string, unknown>;
  } catch {
    return null;
  }
}

function lakeFromId(id: string): string {
  if (id.includes(':')) return id.split(':', 1)[0] || 'misc';
  return 'misc';
}

const LAKE_COLORS: Record<string, readonly [number, number, number, number]> = {
  devel: [0.24, 0.72, 0.98, 0.96],
  web: [0.39, 0.92, 0.68, 0.96],
  bluesky: [0.31, 0.63, 0.98, 0.96],
  'knoxx-session': [0.98, 0.72, 0.34, 0.96],
  user: [0.92, 0.58, 0.98, 0.94],
  misc: [0.7, 0.74, 0.82, 0.94],
};

function nodeStyle(node: RenderGraphNode): NodeStyle {
  const payload = (node.data ?? {}) as { selected?: boolean; degree?: number; lake?: string };
  const selected = Boolean(payload.selected);
  const degree = typeof payload.degree === 'number' ? payload.degree : 0;
  const lake = typeof payload.lake === 'string' ? payload.lake : lakeFromId(node.id);
  const base = LAKE_COLORS[lake] || LAKE_COLORS.misc;
  const boost = Math.min(4.2, Math.log2(degree + 1) * 0.9);
  return {
    sizePx: (selected ? 9.2 : 6.0) + boost,
    color: selected ? rgba(base[0] + 0.12, base[1] + 0.12, base[2] + 0.12, 0.98) : base,
  };
}

function edgeStyle(edge: RenderGraphEdge): EdgeStyle {
  const payload = (edge.data ?? {}) as { selected?: boolean };
  const selected = Boolean(payload.selected);
  return {
    color: selected ? rgba(0.82, 0.88, 0.98, 0.28) : rgba(0.75, 0.8, 0.92, 0.14),
    phase: selected ? 0.9 : 0,
  };
}

function summarizeNodeLabel(node: { label?: string; id: string }): string {
  const label = (node.label || '').trim();
  if (label) return label;
  return node.id;
}

function buildFocusedGraphQuery(args: { rootId: string; distance: number; maxNodes: number; maxEdges: number }): string {
  const rootId = JSON.stringify(args.rootId);
  const distance = Math.max(0, Math.floor(args.distance));
  const maxNodes = Math.max(1, Math.floor(args.maxNodes));
  const maxEdges = Math.max(1, Math.floor(args.maxEdges));
  return `{
  focusedGraphView(rootId: ${rootId}, distance: ${distance}, maxNodes: ${maxNodes}, maxEdges: ${maxEdges}) {
    nodes { id kind label x y external loadedByDefault layer dataJson }
    edges { source target kind layer dataJson }
    meta { totalNodes totalEdges sampledNodes sampledEdges }
  }
}`;
}

export function GraphExplorer(props: {
  nodes: GraphExportNode[];
  defaultDistance?: number;
}) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const viewRef = useRef<WebGLGraphView | null>(null);

  const nodeIndex = useMemo(() => new Map(props.nodes.map((n) => [n.id, n] as const)), [props.nodes]);

  const [search, setSearch] = useState('');
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [distance, setDistance] = useState(props.defaultDistance ?? 2);
  const [maxNodes, setMaxNodes] = useState(220);
  const [maxEdges, setMaxEdges] = useState(520);

  const [graphqlText, setGraphqlText] = useState('');
  const [graphqlResultText, setGraphqlResultText] = useState('');
  const [graphqlError, setGraphqlError] = useState('');
  const [graphView, setGraphView] = useState<GraphView | null>(null);

  const filteredNodes = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return props.nodes.slice(0, 250);
    return props.nodes
      .filter((n) => (n.label || '').toLowerCase().includes(q) || (n.project || '').toLowerCase().includes(q) || (n.nodeType || '').toLowerCase().includes(q) || n.id.toLowerCase().includes(q))
      .slice(0, 250);
  }, [props.nodes, search]);

  const renderedGraph = useMemo(() => {
    if (!graphView) return null;

    const degree = new Map<string, number>();
    for (const e of graphView.edges) {
      degree.set(e.source, (degree.get(e.source) ?? 0) + 1);
      degree.set(e.target, (degree.get(e.target) ?? 0) + 1);
    }

    const nodes: RenderGraphNode[] = graphView.nodes.map((n) => {
      const parsedData = safeJsonParse(n.dataJson);
      const lake = typeof parsedData?.lake === 'string' ? parsedData.lake : lakeFromId(n.id);
      return {
        id: n.id,
        x: n.x,
        y: n.y,
        kind: n.kind,
        label: n.label,
        data: {
          lake,
          selected: selectedNodeId === n.id,
          degree: degree.get(n.id) ?? 0,
          layer: n.layer,
          data: parsedData,
        },
      };
    });

    const selectedSet = new Set<string>();
    if (selectedNodeId) selectedSet.add(selectedNodeId);

    const edges: RenderGraphEdge[] = graphView.edges.map((e) => ({
      source: e.source,
      target: e.target,
      kind: e.kind,
      data: {
        selected: selectedSet.has(e.source) || selectedSet.has(e.target),
        layer: e.layer,
        data: safeJsonParse(e.dataJson),
      },
    }));

    return { nodes, edges } satisfies RenderGraphData;
  }, [graphView, selectedNodeId]);

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

  useEffect(() => {
    const view = viewRef.current;
    if (!view || !renderedGraph) return;
    view.setGraph(renderedGraph);
    if (renderedGraph.nodes.length > 0) {
      view.fitToGraph(48);
    }
  }, [renderedGraph]);

  // When a node is selected or controls change, regenerate the canonical query.
  useEffect(() => {
    if (!selectedNodeId) return;
    setGraphqlText(buildFocusedGraphQuery({ rootId: selectedNodeId, distance, maxNodes, maxEdges }));
  }, [selectedNodeId, distance, maxNodes, maxEdges]);

  const runGraphql = useCallback(async (query: string) => {
    const trimmed = (query || '').trim();
    if (!trimmed) return;
    setGraphqlError('');
    try {
      const data = await graphqlQuery<any>(trimmed);
      setGraphqlResultText(JSON.stringify(data, null, 2));

      const view: GraphView | null =
        (data as any)?.focusedGraphView ??
        (data as any)?.graphView ??
        null;
      if (view && Array.isArray(view.nodes) && Array.isArray(view.edges)) {
        setGraphView(view);
      }
    } catch (e: any) {
      setGraphqlError(e.message || String(e));
    }
  }, []);

  // Debounced execution: typing in the GraphQL box updates the rendered graph.
  useEffect(() => {
    const t = setTimeout(() => {
      void runGraphql(graphqlText);
    }, 450);
    return () => clearTimeout(t);
  }, [graphqlText, runGraphql]);

  const selectedNode = selectedNodeId ? nodeIndex.get(selectedNodeId) ?? null : null;

  return (
    <div className="flex gap-4 h-[720px]">
      {/* Sidebar node list */}
      <div className="w-80 shrink-0 flex flex-col rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
        <div className="shrink-0 p-3 border-b border-slate-700/30 space-y-2">
          <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search nodes…"
            className="w-full bg-slate-900/50 border border-slate-700/50 rounded px-3 py-1.5 text-xs text-slate-200 placeholder-slate-600 focus:outline-none" />
          <div className="flex gap-2 items-end">
            <div className="flex-1">
              <div className="text-[10px] text-slate-500 mb-1">Distance</div>
              <input type="number" min={0} max={12} value={distance}
                onChange={e => setDistance(Math.max(0, Math.min(12, parseInt(e.target.value || '0', 10) || 0)))}
                className="w-full bg-slate-900/50 border border-slate-700/50 rounded px-2 py-1 text-xs text-slate-200" />
            </div>
            <div className="flex-1">
              <div className="text-[10px] text-slate-500 mb-1">Max nodes</div>
              <input type="number" min={10} max={2000} value={maxNodes}
                onChange={e => setMaxNodes(Math.max(10, Math.min(2000, parseInt(e.target.value || '200', 10) || 200)))}
                className="w-full bg-slate-900/50 border border-slate-700/50 rounded px-2 py-1 text-xs text-slate-200" />
            </div>
            <div className="flex-1">
              <div className="text-[10px] text-slate-500 mb-1">Max edges</div>
              <input type="number" min={10} max={8000} value={maxEdges}
                onChange={e => setMaxEdges(Math.max(10, Math.min(8000, parseInt(e.target.value || '500', 10) || 500)))}
                className="w-full bg-slate-900/50 border border-slate-700/50 rounded px-2 py-1 text-xs text-slate-200" />
            </div>
          </div>
          {selectedNode && (
            <div className="pt-1 text-xs text-slate-400">
              <div className="text-slate-500">Selected</div>
              <div className="text-slate-200 truncate" title={selectedNode.id}>{summarizeNodeLabel(selectedNode)}</div>
              <div className="text-slate-600">{selectedNode.project} · {selectedNode.nodeType}</div>
            </div>
          )}
        </div>

        <div className="flex-1 overflow-y-auto">
          {filteredNodes.map((n) => (
            <button key={n.id} onClick={() => setSelectedNodeId(n.id)}
              className={`w-full px-3 py-2 text-left border-b border-slate-700/20 hover:bg-slate-700/10 ${selectedNodeId === n.id ? 'bg-slate-700/25' : ''}`}>
              <div className="text-xs text-slate-200 truncate">{summarizeNodeLabel(n)}</div>
              <div className="text-[10px] text-slate-600 truncate">{n.project} · {n.nodeType}</div>
            </button>
          ))}
          {filteredNodes.length === 0 && (
            <div className="p-4 text-xs text-slate-600">No matches</div>
          )}
        </div>
      </div>

      {/* Graph canvas */}
      <div className="flex-1 flex flex-col rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
        <div className="shrink-0 px-3 py-2 border-b border-slate-700/30 flex items-center justify-between">
          <div className="text-xs text-slate-500">Rendered subgraph</div>
          <button onClick={() => viewRef.current?.fitToGraph(48)}
            className="text-xs text-blue-400 hover:text-blue-300 px-2 py-1 rounded hover:bg-slate-700/40">
            fit
          </button>
        </div>
        <div className="flex-1">
          <canvas ref={canvasRef} className="w-full h-full" />
        </div>
      </div>

      {/* GraphQL pane */}
      <div className="w-[520px] shrink-0 flex flex-col rounded-lg border border-slate-700/50 bg-slate-800/30 overflow-hidden">
        <div className="shrink-0 px-3 py-2 border-b border-slate-700/30 flex items-center justify-between">
          <div className="text-xs text-slate-500">GraphQL (debounced → updates graph)</div>
          <button
            onClick={() => void runGraphql(graphqlText)}
            className="text-xs text-blue-400 hover:text-blue-300 px-2 py-1 rounded hover:bg-slate-700/40"
          >
            run
          </button>
        </div>

        <div className="p-3 border-b border-slate-700/30">
          <textarea value={graphqlText} onChange={e => setGraphqlText(e.target.value)}
            placeholder={'{ focusedGraphView(rootId: "...", distance: 2) { nodes { id } edges { source target } } }'}
            className="w-full bg-slate-900/50 border border-slate-700/50 rounded p-3 text-xs text-slate-200 font-mono resize-none focus:outline-none focus:border-slate-600 h-56"
            spellCheck={false}
          />
          {graphqlError && (
            <div className="mt-2 rounded bg-rose-500/10 border border-rose-500/20 px-3 py-2 text-xs text-rose-300">{graphqlError}</div>
          )}
        </div>

        <div className="flex-1 overflow-auto p-3">
          {graphqlResultText ? (
            <pre className="text-xs text-slate-300 font-mono whitespace-pre-wrap">{graphqlResultText}</pre>
          ) : (
            <div className="text-xs text-slate-600">Query results appear here</div>
          )}
        </div>
      </div>
    </div>
  );
}
