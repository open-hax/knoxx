import {
  rgba,
  type EdgeStyle,
  type GraphData as RenderGraphData,
  type GraphEdge as RenderGraphEdge,
  type GraphNode as RenderGraphNode,
  type NodeStyle,
} from "@octave-commons/webgl-graph-view";
import type { GraphExportEdge, GraphExportNode, GraphExportResponse, LayoutNode, LayoutEdge, LayoutSnapshot, RenderNodePayload, RenderEdgePayload } from "../../lib/types";

export const CANONICAL_LAKES = ["devel", "web", "bluesky", "knoxx-session"] as const;

export const LAKE_COLORS: Record<string, readonly [number, number, number, number]> = {
  devel: [0.24, 0.72, 0.98, 0.96],
  web: [0.39, 0.92, 0.68, 0.96],
  bluesky: [0.31, 0.63, 0.98, 0.96],
  "knoxx-session": [0.98, 0.72, 0.34, 0.96],
  misc: [0.7, 0.74, 0.82, 0.94],
};

const NODE_TYPE_VARIANTS: Record<string, { size: number; tint: number }> = {
  docs: { size: 6.2, tint: 0.18 },
  code: { size: 5.4, tint: -0.02 },
  config: { size: 5.2, tint: 0.1 },
  data: { size: 6.0, tint: 0.24 },
  visited: { size: 5.6, tint: 0.08 },
  unvisited: { size: 4.8, tint: 0.34 },
  user: { size: 6.4, tint: -0.08 },
  post: { size: 5.1, tint: 0.22 },
  node: { size: 4.8, tint: 0 },
};

const EDGE_COLORS: Record<string, readonly [number, number, number, number]> = {
  local_markdown_link: [0.97, 0.79, 0.38, 0.24],
  external_web_link: [0.97, 0.55, 0.34, 0.28],
  code_dependency: [0.66, 0.52, 0.98, 0.24],
  visited_to_visited: [0.29, 0.92, 0.63, 0.16],
  visited_to_unvisited: [0.35, 0.82, 0.57, 0.22],
  follows_user: [0.41, 0.67, 1.0, 0.2],
  authored_post: [0.73, 0.57, 0.98, 0.24],
  shared_post: [0.98, 0.67, 0.41, 0.24],
  liked_post: [0.98, 0.41, 0.66, 0.24],
  post_links_visited_web: [0.28, 0.86, 0.94, 0.24],
  post_links_unvisited_web: [0.2, 0.78, 0.92, 0.3],
  relation: [0.74, 0.78, 0.9, 0.14],
};

function hash32(input: string): number {
  let h = 0x811c9dc5;
  for (let i = 0; i < input.length; i += 1) {
    h ^= input.charCodeAt(i);
    h = Math.imul(h, 0x01000193);
  }
  return h >>> 0;
}

function lakeKey(node: LayoutNode): string {
  const lake = node.data?.lake;
  if (typeof lake === "string" && lake.trim()) return lake.trim();
  if (node.id.includes(":")) return node.id.split(":", 1)[0] || "misc";
  return "misc";
}

function subtypeKey(node: LayoutNode): string {
  const nodeType = node.data?.node_type;
  if (typeof nodeType === "string" && nodeType.trim()) return nodeType.trim();
  if (node.kind) return node.kind;
  return "node";
}

function groupKey(node: LayoutNode): string {
  return `${lakeKey(node)}::${subtypeKey(node)}`;
}

function lakeAnchor(lake: string, index: number, total: number): { x: number; y: number } {
  const canonicalIdx = CANONICAL_LAKES.indexOf(lake as (typeof CANONICAL_LAKES)[number]);
  if (canonicalIdx >= 0) {
    return {
      x: (canonicalIdx - 1.5) * 1100,
      y: canonicalIdx === 3 ? 780 : 0,
    };
  }

  const radius = 1300;
  const angle = (Math.PI * 2 * index) / Math.max(1, total);
  return { x: Math.cos(angle) * radius, y: Math.sin(angle) * radius * 0.75 };
}

export function layoutGraph(snapshot: LayoutSnapshot): Map<string, { x: number; y: number }> {
  const groups = new Map<string, string[]>();
  const groupMeta = new Map<string, { lake: string; subtype: string }>();

  for (const node of snapshot.nodes) {
    const key = groupKey(node);
    const rows = groups.get(key) ?? [];
    rows.push(node.id);
    groups.set(key, rows);
    groupMeta.set(key, { lake: lakeKey(node), subtype: subtypeKey(node) });
  }

  const keys = [...groups.keys()].sort((a, b) => a.localeCompare(b));
  const lakes = [...new Set(keys.map((key) => groupMeta.get(key)?.lake || "misc"))].sort((a, b) => a.localeCompare(b));
  const anchors = new Map<string, { x: number; y: number }>();
  const byLake = new Map<string, string[]>();

  for (const key of keys) {
    const lake = groupMeta.get(key)?.lake || "misc";
    const rows = byLake.get(lake) ?? [];
    rows.push(key);
    byLake.set(lake, rows);
  }

  lakes.forEach((lake, lakeIndex) => {
    const base = lakeAnchor(lake, lakeIndex, lakes.length);
    const lakeGroups = (byLake.get(lake) ?? []).sort((a, b) => a.localeCompare(b));
    const count = Math.max(1, lakeGroups.length);
    lakeGroups.forEach((key, index) => {
      const band = index - (count - 1) / 2;
      anchors.set(key, {
        x: base.x,
        y: base.y + band * 300,
      });
    });
  });

  const positions = new Map<string, { x: number; y: number }>();
  const golden = Math.PI * (3 - Math.sqrt(5));

  for (const [key, ids] of groups) {
    ids.sort((a, b) => a.localeCompare(b));
    const base = anchors.get(key) ?? { x: 0, y: 0 };
    const keyPhase = ((hash32(key) % 628) / 100) * 0.85;
    const size = Math.max(1, ids.length);
    const rMax = 40 + Math.min(900, Math.sqrt(size) * 10);
    const spacing = rMax / Math.sqrt(size + 1);

    for (let i = 0; i < ids.length; i += 1) {
      const id = ids[i]!;
      const h = hash32(id);
      const angle = keyPhase + i * golden;
      const radial = spacing * Math.sqrt(i + 1);
      const jitter = (((h % 2000) / 2000) - 0.5) * spacing * 0.6;
      positions.set(id, {
        x: base.x + Math.cos(angle) * (radial + jitter),
        y: base.y + Math.sin(angle) * (radial + jitter) * 0.86,
      });
    }
  }

  return positions;
}

export function inferLake(node: GraphExportNode): string {
  return node.lake || (typeof node.data?.lake === "string" ? String(node.data.lake) : "misc");
}

export function inferNodeType(node: GraphExportNode): string {
  return node.nodeType || (typeof node.data?.node_type === "string" ? String(node.data.node_type) : "node");
}

export function inferEdgeType(edge: GraphExportEdge): string {
  return edge.edgeType || edge.kind || (typeof edge.data?.edge_type === "string" ? String(edge.data.edge_type) : "relation");
}

export function isCrossLake(edge: GraphExportEdge): boolean {
  return edge.sourceLake !== edge.targetLake;
}

function lighten(color: readonly [number, number, number, number], amount: number): [number, number, number, number] {
  const [r, g, b, a] = color;
  const mix = (value: number) => (amount >= 0 ? value + (1 - value) * amount : value * (1 + amount));
  return [mix(r), mix(g), mix(b), a];
}

export function nodeStyle(node: RenderGraphNode): NodeStyle {
  const payload = node.data as RenderNodePayload | undefined;
  const exportNode = payload?.exportNode;
  const lake = exportNode ? inferLake(exportNode) : "misc";
  const nodeType = exportNode ? inferNodeType(exportNode) : (node.kind || "node");
  const base = LAKE_COLORS[lake] || LAKE_COLORS.misc;
  const variant = NODE_TYPE_VARIANTS[nodeType] || NODE_TYPE_VARIANTS.node;
  const degreeBoost = payload ? Math.min(4.5, Math.log2(payload.degree + 1) * 0.95) : 0;

  return {
    sizePx: variant.size + degreeBoost,
    color: lighten(base, variant.tint || 0),
  };
}

export function edgeStyle(edge: RenderGraphEdge): EdgeStyle {
  const payload = edge.data as RenderEdgePayload | undefined;
  const exportEdge = payload?.exportEdge;
  const edgeType = exportEdge ? inferEdgeType(exportEdge) : (edge.kind || "relation");
  const base = EDGE_COLORS[edgeType] || EDGE_COLORS.relation;
  const alpha = exportEdge && isCrossLake(exportEdge) ? Math.min(0.34, base[3] + 0.08) : base[3];

  return {
    color: rgba(base[0], base[1], base[2], alpha),
    phase: exportEdge && isCrossLake(exportEdge) ? 1.2 : 0,
  };
}

export function shortNumber(value: number): string {
  return value.toLocaleString();
}

export function compareNodes(a: GraphExportNode, b: GraphExportNode, degree: Map<string, number>, crossLakeNodes: Set<string>): number {
  const degreeDelta = (degree.get(b.id) ?? 0) - (degree.get(a.id) ?? 0);
  if (degreeDelta !== 0) return degreeDelta;
  const crossLakeDelta = Number(crossLakeNodes.has(b.id)) - Number(crossLakeNodes.has(a.id));
  if (crossLakeDelta !== 0) return crossLakeDelta;
  const lakeDelta = inferLake(a).localeCompare(inferLake(b));
  if (lakeDelta !== 0) return lakeDelta;
  return a.label.localeCompare(b.label);
}

export function compareEdges(a: GraphExportEdge, b: GraphExportEdge, degree: Map<string, number>): number {
  const crossLakeDelta = Number(isCrossLake(b)) - Number(isCrossLake(a));
  if (crossLakeDelta !== 0) return crossLakeDelta;
  const degreeA = (degree.get(a.source) ?? 0) + (degree.get(a.target) ?? 0);
  const degreeB = (degree.get(b.source) ?? 0) + (degree.get(b.target) ?? 0);
  if (degreeB !== degreeA) return degreeB - degreeA;
  return a.id.localeCompare(b.id);
}

export function toggleLake(current: string[], lake: string): string[] {
  if (current.includes(lake)) {
    return current.filter((entry) => entry !== lake);
  }
  return [...current, lake];
}

export type { LayoutNode, LayoutEdge, RenderNodePayload, RenderEdgePayload } from "../../lib/types";

export function projectGraphExport(
  payload: GraphExportResponse | null, selectedLakes: string[], crossLakeOnly: boolean, maxNodes: number, maxEdges: number,
) {
  const nodes = payload?.nodes ?? [];
  const edges = payload?.edges ?? [];
  const selectedLakeSet = new Set(selectedLakes);

  const visibleNodes = nodes.filter((node) => selectedLakeSet.has(inferLake(node)));
  const visibleNodeIds = new Set(visibleNodes.map((node) => node.id));
  const lakeCounts = new Map<string, number>();
  const renderedLakeCounts = new Map<string, number>();

  for (const node of visibleNodes) {
    const lake = inferLake(node);
    lakeCounts.set(lake, (lakeCounts.get(lake) ?? 0) + 1);
  }

  const visibleEdges = edges.filter((edge) => {
    const sourceLakeAllowed = selectedLakeSet.has(edge.sourceLake);
    const targetLakeAllowed = selectedLakeSet.has(edge.targetLake);
    if (!sourceLakeAllowed || !targetLakeAllowed) return false;
    if (!visibleNodeIds.has(edge.source) || !visibleNodeIds.has(edge.target)) return false;
    if (crossLakeOnly && !isCrossLake(edge)) return false;
    return true;
  });

  const degree = new Map<string, number>();
  const crossLakeNodes = new Set<string>();
  for (const edge of visibleEdges) {
    degree.set(edge.source, (degree.get(edge.source) ?? 0) + 1);
    degree.set(edge.target, (degree.get(edge.target) ?? 0) + 1);
    if (isCrossLake(edge)) {
      crossLakeNodes.add(edge.source);
      crossLakeNodes.add(edge.target);
    }
  }

  const sortedNodes = [...visibleNodes].sort((a, b) => compareNodes(a, b, degree, crossLakeNodes));
  const cappedNodeIds = new Set(sortedNodes.slice(0, maxNodes).map((node) => node.id));
  const sortedEdges = visibleEdges
    .filter((edge) => cappedNodeIds.has(edge.source) && cappedNodeIds.has(edge.target))
    .sort((a, b) => compareEdges(a, b, degree));
  const finalEdges = sortedEdges.slice(0, maxEdges);

  const edgeNodeIds = new Set<string>();
  for (const edge of finalEdges) {
    edgeNodeIds.add(edge.source);
    edgeNodeIds.add(edge.target);
  }

  const finalNodes: GraphExportNode[] = [];
  const included = new Set<string>();
  for (const node of sortedNodes) {
    if (edgeNodeIds.has(node.id) && !included.has(node.id)) {
      finalNodes.push(node);
      included.add(node.id);
    }
  }
  for (const node of sortedNodes) {
    if (included.has(node.id)) continue;
    if (finalNodes.length >= maxNodes) break;
    finalNodes.push(node);
    included.add(node.id);
  }

  for (const node of finalNodes) {
    const lake = inferLake(node);
    renderedLakeCounts.set(lake, (renderedLakeCounts.get(lake) ?? 0) + 1);
  }

  const layoutNodes: LayoutNode[] = finalNodes.map((node) => ({
    id: node.id,
    kind: node.kind,
    label: node.label,
    data: {
      ...(node.data ?? {}),
      lake: inferLake(node),
      node_type: inferNodeType(node),
    },
  }));
  const layoutEdges: LayoutEdge[] = finalEdges.map((edge) => ({
    id: edge.id,
    source: edge.source,
    target: edge.target,
    kind: inferEdgeType(edge),
    data: {
      ...(edge.data ?? {}),
      edge_type: inferEdgeType(edge),
      source_lake: edge.sourceLake,
      target_lake: edge.targetLake,
    },
  }));
  const positions = layoutGraph({ nodes: layoutNodes, edges: layoutEdges });

  const renderNodes: RenderGraphNode[] = finalNodes.map((node) => {
    const position = positions.get(node.id) ?? { x: 0, y: 0 };
    return {
      id: node.id,
      x: position.x,
      y: position.y,
      kind: inferNodeType(node),
      label: node.label,
      data: {
        exportNode: node,
        degree: degree.get(node.id) ?? 0,
      } satisfies RenderNodePayload,
    };
  });

  const renderEdges: RenderGraphEdge[] = finalEdges.map((edge) => ({
    source: edge.source,
    target: edge.target,
    kind: inferEdgeType(edge),
    data: {
      exportEdge: edge,
    } satisfies RenderEdgePayload,
  }));

  const nodeMap = new Map(finalNodes.map((node) => [node.id, node]));
  const edgesByNode = new Map<string, GraphExportEdge[]>();
  for (const edge of finalEdges) {
    const sourceRows = edgesByNode.get(edge.source) ?? [];
    sourceRows.push(edge);
    edgesByNode.set(edge.source, sourceRows);
    const targetRows = edgesByNode.get(edge.target) ?? [];
    targetRows.push(edge);
    edgesByNode.set(edge.target, targetRows);
  }

  return {
    graph: {
      nodes: renderNodes,
      edges: renderEdges,
    } satisfies RenderGraphData,
    nodeMap,
    edgesByNode,
    degree,
    lakeCounts,
    renderedLakeCounts,
    rawNodeCount: nodes.length,
    rawEdgeCount: edges.length,
    filteredNodeCount: visibleNodes.length,
    filteredEdgeCount: visibleEdges.length,
  };
}
