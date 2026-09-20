import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { fetchDocuments, fetchIngestionJobs, ProxyApiError, uploadDocuments } from "../lib/nextApi";
import { projectGraphExport } from "./raw-graph-export/graph-helpers";
import type { GraphExportNode, GraphExportEdge } from "../lib/types";
import { parseMongoJsonText } from "./DataPage";

describe("parseMongoJsonText", () => {
  it("returns the provided default for blank input", () => {
    expect(parseMongoJsonText("Projection", "", undefined)).toBeUndefined();
    expect(parseMongoJsonText("Filter", "   ", { status: "active" })).toEqual({ status: "active" });
  });

  it("parses valid Mongo JSON text", () => {
    expect(parseMongoJsonText("Filter", '{"status":"active"}', {})).toEqual({ status: "active" });
    expect(parseMongoJsonText("Sort", '{"_id":-1}', {})).toEqual({ _id: -1 });
  });

  it("throws a labeled error for invalid Mongo JSON before a query can be sent", () => {
    expect(() => parseMongoJsonText("Filter", '{"status":', {})).toThrow(/Filter is not valid JSON/);
  });
});


describe("session proxy transport", () => {
  let previousSession: string | null;
  beforeEach(() => {
    previousSession = sessionStorage.getItem("knoxx_session_id");
    sessionStorage.setItem("knoxx_session_id", "transport-fixture");
  });
  afterEach(() => {
    if (previousSession === null) sessionStorage.removeItem("knoxx_session_id");
    else sessionStorage.setItem("knoxx_session_id", previousSession);
    vi.unstubAllGlobals();
  });

  it("retains the same session for document and encoded ingestion requests", async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(new Response("[]")));
    vi.stubGlobal("fetch", fetchMock);
    await fetchDocuments();
    await fetchIngestionJobs("tenant/one & two");
    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual(["/api/documents", "/api/ingestion-proxy/jobs?tenant_id=tenant%2Fone%20%26%20two"]);
    for (const [, options] of fetchMock.mock.calls) {
      expect(options.headers.get("x-knoxx-session-id")).toBe("transport-fixture");
    }
  });

  it("retains multipart fields and lets the browser supply the content boundary", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response('{"ok":true}'));
    vi.stubGlobal("fetch", fetchMock);
    const file = new File(["notes"], "notes.md", { type: "text/markdown" });
    await expect(uploadDocuments([file], false)).resolves.toEqual({ ok: true });
    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe("/api/documents/upload");
    expect(options.method).toBe("POST");
    expect(options.headers.get("x-knoxx-session-id")).toBe("transport-fixture");
    expect(options.headers.has("Content-Type")).toBe(false);
    expect(options.body.getAll("files")).toEqual([file]);
    expect(options.body.get("autoIngest")).toBe("false");
  });

  it("preserves the exported error class and distinct empty-body fallback", async () => {
    vi.stubGlobal("fetch", vi.fn()
      .mockResolvedValueOnce(new Response("Denied", { status: 403 }))
      .mockResolvedValueOnce(new Response("", { status: 503 })));
    await expect(fetchDocuments()).rejects.toMatchObject({ name: "ProxyApiError", status: 403, body: "Denied" });
    try {
      await fetchIngestionJobs();
      throw new Error("request unexpectedly succeeded");
    } catch (error) {
      expect(error).toBeInstanceOf(ProxyApiError);
      expect(error).toMatchObject({ status: 503, body: "Ingestion request failed: 503" });
    }
  });
});


describe("canonical graph projection", () => {
  const node = (id: string, lake: string): GraphExportNode => ({ id, lake, kind: "docs", label: id, nodeType: "docs", source: lake, project: lake, data: {} });
  const edge = (id: string, source: string, target: string, sourceLake: string, targetLake: string): GraphExportEdge => ({ id, source, target, sourceLake, targetLake, lake: sourceLake, kind: "relation", edgeType: "relation", data: {} });
  const nodes = [node("a", "devel"), node("b", "devel"), node("c", "web"), node("d", "bluesky")];
  const edges = [edge("local", "a", "b", "devel", "devel"), edge("cross", "a", "c", "devel", "web"), edge("hidden", "a", "d", "devel", "bluesky"), edge("dangling", "a", "missing", "devel", "web")];

  it("filters inaccessible lakes and dangling edges before ranking and capping", () => {
    const out = projectGraphExport({ ok: true, nodes, edges }, ["devel", "web"], false, 2, 1);
    expect(out.rawNodeCount).toBe(4);
    expect(out.rawEdgeCount).toBe(4);
    expect(out.filteredNodeCount).toBe(3);
    expect(out.filteredEdgeCount).toBe(2);
    expect(out.graph.nodes.map(item => item.id)).toEqual(["a", "c"]);
    expect(out.graph.edges.map(item => [item.source, item.target])).toEqual([["a", "c"]]);
    expect(out.edgesByNode.get("a")?.map(item => item.id)).toEqual(["cross"]);
    expect(out.lakeCounts.get("devel")).toBe(2);
    expect(out.renderedLakeCounts.get("devel")).toBe(1);
  });

  it("keeps deterministic layout and cross-lake-only ranking", () => {
    const out = projectGraphExport({ ok: true, nodes, edges }, ["devel", "web"], true, 4, 4);
    const reordered = projectGraphExport({ ok: true, nodes: [...nodes].reverse(), edges: [...edges].reverse() }, ["devel", "web"], true, 4, 4);
    expect(out.graph).toEqual(reordered.graph);
    expect(out.filteredEdgeCount).toBe(1);
    for (const item of out.graph.nodes) {
      expect(Number.isFinite(item.x)).toBe(true);
      expect(Number.isFinite(item.y)).toBe(true);
    }
  });

  it("returns empty render indexes for missing data or zero node budget", () => {
    const empty = projectGraphExport(null, ["devel"], false, 100, 100);
    expect(empty.graph).toEqual({ nodes: [], edges: [] });
    const capped = projectGraphExport({ ok: true, nodes, edges }, ["devel", "web"], false, 0, 100);
    expect(capped.graph).toEqual({ nodes: [], edges: [] });
    expect(capped.nodeMap.size).toBe(0);
    expect(capped.edgesByNode.size).toBe(0);
  });
});
