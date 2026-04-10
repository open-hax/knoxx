import { describe, expect, it } from "vitest";
import {
  CANONICAL_LAKES,
  LAKE_COLORS,
  inferLake,
  inferNodeType,
  inferEdgeType,
  isCrossLake,
  shortNumber,
  toggleLake,
  compareNodes,
  compareEdges,
  layoutGraph,
} from "./graph-helpers";
import type { GraphExportNode, GraphExportEdge } from "../../lib/types";

describe("CANONICAL_LAKES", () => {
  it("contains expected lakes", () => {
    expect(CANONICAL_LAKES).toContain("devel");
    expect(CANONICAL_LAKES).toContain("web");
    expect(CANONICAL_LAKES).toContain("bluesky");
    expect(CANONICAL_LAKES).toContain("knoxx-session");
  });
});

describe("LAKE_COLORS", () => {
  it("has colors for canonical lakes", () => {
    for (const lake of CANONICAL_LAKES) {
      expect(LAKE_COLORS[lake]).toBeDefined();
      expect(LAKE_COLORS[lake]).toHaveLength(4);
    }
  });

  it("has misc color for unknown lakes", () => {
    expect(LAKE_COLORS.misc).toBeDefined();
  });
});

describe("inferLake", () => {
  it("returns lake property if present", () => {
    const node = { id: "n1", lake: "devel" } as GraphExportNode;
    expect(inferLake(node)).toBe("devel");
  });

  it("returns data.lake if lake property is absent", () => {
    const node = { id: "n1", data: { lake: "web" } } as GraphExportNode;
    expect(inferLake(node)).toBe("web");
  });

  it("returns misc if no lake info", () => {
    const node = { id: "n1" } as GraphExportNode;
    expect(inferLake(node)).toBe("misc");
  });
});

describe("inferNodeType", () => {
  it("returns nodeType property if present", () => {
    const node = { id: "n1", nodeType: "docs" } as GraphExportNode;
    expect(inferNodeType(node)).toBe("docs");
  });

  it("returns data.node_type if nodeType property is absent", () => {
    const node = { id: "n1", data: { node_type: "code" } } as GraphExportNode;
    expect(inferNodeType(node)).toBe("code");
  });

  it("returns node if no type info", () => {
    const node = { id: "n1" } as GraphExportNode;
    expect(inferNodeType(node)).toBe("node");
  });
});

describe("inferEdgeType", () => {
  it("returns edgeType property if present", () => {
    const edge = { id: "e1", edgeType: "local_markdown_link" } as GraphExportEdge;
    expect(inferEdgeType(edge)).toBe("local_markdown_link");
  });

  it("returns kind if edgeType is absent", () => {
    const edge = { id: "e1", kind: "code_dependency" } as GraphExportEdge;
    expect(inferEdgeType(edge)).toBe("code_dependency");
  });

  it("returns data.edge_type as fallback", () => {
    const edge = { id: "e1", data: { edge_type: "relation" } } as GraphExportEdge;
    expect(inferEdgeType(edge)).toBe("relation");
  });

  it("returns relation if no type info", () => {
    const edge = { id: "e1" } as GraphExportEdge;
    expect(inferEdgeType(edge)).toBe("relation");
  });
});

describe("isCrossLake", () => {
  it("returns true when source and target lakes differ", () => {
    const edge = { sourceLake: "devel", targetLake: "web" } as GraphExportEdge;
    expect(isCrossLake(edge)).toBe(true);
  });

  it("returns false when source and target lakes are same", () => {
    const edge = { sourceLake: "devel", targetLake: "devel" } as GraphExportEdge;
    expect(isCrossLake(edge)).toBe(false);
  });

  it("returns false when both are undefined", () => {
    const edge = {} as GraphExportEdge;
    expect(isCrossLake(edge)).toBe(false);
  });
});

describe("shortNumber", () => {
  it("formats numbers with locale separators", () => {
    // en-US locale uses commas
    expect(shortNumber(1000)).toBe("1,000");
    expect(shortNumber(1234567)).toBe("1,234,567");
  });

  it("handles small numbers", () => {
    expect(shortNumber(0)).toBe("0");
    expect(shortNumber(123)).toBe("123");
  });
});

describe("toggleLake", () => {
  it("removes lake if present", () => {
    expect(toggleLake(["devel", "web"], "devel")).toEqual(["web"]);
  });

  it("adds lake if not present", () => {
    expect(toggleLake(["devel"], "web")).toEqual(["devel", "web"]);
  });

  it("handles empty list", () => {
    expect(toggleLake([], "devel")).toEqual(["devel"]);
  });
});

describe("compareNodes", () => {
  const degree = new Map([["a", 10], ["b", 5], ["c", 10]]);
  const crossLakeNodes = new Set(["a"]);

  it("sorts by degree descending", () => {
    const a = { id: "a", label: "A" } as GraphExportNode;
    const b = { id: "b", label: "B" } as GraphExportNode;
    expect(compareNodes(a, b, degree, crossLakeNodes)).toBeLessThan(0); // a has higher degree
  });

  it("sorts by cross-lake status when degree is equal", () => {
    const a = { id: "a", label: "A" } as GraphExportNode;
    const c = { id: "c", label: "C" } as GraphExportNode;
    expect(compareNodes(a, c, degree, crossLakeNodes)).toBeLessThan(0); // a is cross-lake
  });

  it("sorts by lake when degree and cross-lake are equal", () => {
    const c = { id: "c", label: "C", lake: "alpha" } as GraphExportNode;
    const d = { id: "d", label: "D", lake: "beta" } as GraphExportNode;
    const degree2 = new Map([["c", 5], ["d", 5]]);
    expect(compareNodes(c, d, degree2, new Set())).toBeLessThan(0); // alpha < beta
  });

  it("sorts by label as final tiebreaker", () => {
    const c = { id: "c", label: "Alpha" } as GraphExportNode;
    const d = { id: "d", label: "Beta" } as GraphExportNode;
    const degree2 = new Map([["c", 5], ["d", 5]]);
    expect(compareNodes(c, d, degree2, new Set())).toBeLessThan(0);
  });
});

describe("compareEdges", () => {
  const degree = new Map([["n1", 10], ["n2", 5], ["n3", 3]]);

  it("sorts cross-lake edges first", () => {
    const a = { id: "e1", source: "n1", target: "n2", sourceLake: "devel", targetLake: "devel" } as GraphExportEdge;
    const b = { id: "e2", source: "n1", target: "n3", sourceLake: "devel", targetLake: "web" } as GraphExportEdge;
    expect(compareEdges(a, b, degree)).toBeGreaterThan(0); // b is cross-lake, comes first
  });

  it("sorts by combined degree when cross-lake is equal", () => {
    const a = { id: "e1", source: "n1", target: "n2", sourceLake: "devel", targetLake: "devel" } as GraphExportEdge;
    const b = { id: "e2", source: "n2", target: "n3", sourceLake: "devel", targetLake: "devel" } as GraphExportEdge;
    // a: degree 10 + 5 = 15, b: degree 5 + 3 = 8
    expect(compareEdges(a, b, degree)).toBeLessThan(0); // a has higher degree
  });

  it("sorts by id as final tiebreaker", () => {
    const a = { id: "e1", source: "n1", target: "n1", sourceLake: "devel", targetLake: "devel" } as GraphExportEdge;
    const b = { id: "e2", source: "n1", target: "n1", sourceLake: "devel", targetLake: "devel" } as GraphExportEdge;
    expect(compareEdges(a, b, degree)).toBeLessThan(0); // e1 < e2
  });
});

describe("layoutGraph", () => {
  it("returns empty map for empty snapshot", () => {
    const positions = layoutGraph({ nodes: [], edges: [] });
    expect(positions.size).toBe(0);
  });

  it("returns positions for nodes", () => {
    const nodes = [
      { id: "n1", kind: "docs", label: "Node 1", data: { lake: "devel" } },
      { id: "n2", kind: "code", label: "Node 2", data: { lake: "devel" } },
    ];
    const positions = layoutGraph({ nodes, edges: [] });
    expect(positions.size).toBe(2);
    expect(positions.get("n1")).toBeDefined();
    expect(positions.get("n2")).toBeDefined();
  });

  it("places nodes in same lake together", () => {
    const nodes = [
      { id: "n1", kind: "docs", label: "Node 1", data: { lake: "devel" } },
      { id: "n2", kind: "docs", label: "Node 2", data: { lake: "web" } },
    ];
    const positions = layoutGraph({ nodes, edges: [] });
    const p1 = positions.get("n1")!;
    const p2 = positions.get("n2")!;
    // devel and web should be at different anchor positions
    expect(Math.abs(p1.x - p2.x)).toBeGreaterThan(500);
  });
});
