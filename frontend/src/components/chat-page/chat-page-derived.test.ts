import { describe, expect, it } from "vitest";

import { filterBrowseEntries, inferBrowseEntryKind, normalizeToolPreview, structuredToMarkdown, toolInputSummary, toolOutputMarkdown, toolPreviewMarkdown, visibilityStats } from "./chat-page-derived";
import type { BrowseEntry } from "./types";

describe("inferBrowseEntryKind", () => {
  it("infers kinds from common extensions", () => {
    expect(inferBrowseEntryKind({ name: "foo.ts", path: "src/foo.ts", type: "file" })).toBe("code");
    expect(inferBrowseEntryKind({ name: "guide.md", path: "docs/guide.md", type: "file" })).toBe("docs");
    expect(inferBrowseEntryKind({ name: "config.json", path: "config/config.json", type: "file" })).toBe("config");
    expect(inferBrowseEntryKind({ name: "table.csv", path: "data/table.csv", type: "file" })).toBe("data");
  });
});

describe("filterBrowseEntries", () => {
  const entries: BrowseEntry[] = [
    { name: "docs", path: "docs", type: "dir" },
    { name: "guide.md", path: "docs/guide.md", type: "file", visibility: "public" },
    { name: "server.ts", path: "src/server.ts", type: "file", visibility: "internal" },
  ];

  it("filters files by visibility and kind while keeping directories navigable", () => {
    const filtered = filterBrowseEntries(entries, "", "public", "docs");
    expect(filtered.map((entry) => entry.path)).toEqual(["docs", "docs/guide.md"]);
  });

  it("still applies the text filter to directories", () => {
    const filtered = filterBrowseEntries(entries, "server", "all", "all");
    expect(filtered.map((entry) => entry.path)).toEqual(["src/server.ts"]);
  });
});

describe("visibilityStats", () => {
  it("counts file visibility buckets", () => {
    const stats = visibilityStats([
      { name: "docs", path: "docs", type: "dir" },
      { name: "a.md", path: "docs/a.md", type: "file", visibility: "public" },
      { name: "b.ts", path: "src/b.ts", type: "file", visibility: "internal" },
      { name: "c.ts", path: "src/c.ts", type: "file" },
    ]);

    expect(stats.total).toBe(3);
    expect(stats.byVisibility).toEqual({ public: 1, internal: 2 });
  });
});

describe("Tool receipt presentation", () => {
  it("preserves missing sentinels and displays human tool output separately from input metadata", () => {
    expect(normalizeToolPreview(" null ")).toBeNull();
    expect(normalizeToolPreview(" UNDEFINED ")).toBeNull();
    expect(normalizeToolPreview(" useful result ")).toBe("useful result");
    expect(toolInputSummary(JSON.stringify({query: "ledger", path: "docs/ledger.edn"}))).toBe("ledger • docs/ledger.edn");
    const output = toolOutputMarkdown(JSON.stringify({content: [{type: "text", text: "First"}, {type: "text", text: "Second"}]}));
    expect(output).toContain("First\n\nSecond");
    expect(output).not.toContain('"content"');
  });

  it("uses fences longer than embedded backticks and bounds structured breadth and depth", () => {
    expect(toolPreviewMarkdown(JSON.stringify({body: "```\ntext"}))).toContain("````text\n```\ntext\n````");
    expect(structuredToMarkdown(Array.from({length: 25}, (_, i) => i))).toContain("1 more item(s)");
    expect(structuredToMarkdown(Object.fromEntries(Array.from({length: 33}, (_, i) => [String(i), i])))).toContain("1 more key(s)");
    expect(structuredToMarkdown({a: {b: {c: {d: {e: {f: "bounded"}}}}}})).toContain("max depth");
  });

  it("caps raw output while exposing truncation to the reader", () => {
    const output = toolOutputMarkdown("x".repeat(12001));
    expect(output).toBe("x".repeat(12000) + "…\n\n_(truncated)_");
  });
});
