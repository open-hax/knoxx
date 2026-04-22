import { describe, expect, it } from "vitest";

import { filterBrowseEntries, inferBrowseEntryKind, visibilityStats } from "./chat-page-derived";
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
