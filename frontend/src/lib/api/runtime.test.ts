import { describe, expect, it, vi } from "vitest";

vi.mock("./core", () => ({
  API_BASE: "",
  buildKnoxxAuthHeaders: vi.fn(() => ({})),
  request: vi.fn(),
}));

import { getToolCatalog } from "./runtime";
import { request } from "./core";

describe("getToolCatalog", () => {
  it("normalizes object-shaped tool catalogs into a tools array", async () => {
    vi.mocked(request).mockResolvedValueOnce({
      role: "knowledge_worker",
      capability_ids: ["memory.search"],
      tools: {
        read: {
          label: "Read",
          description: "Read a workspace file",
          enabled: true,
        },
        bash: {
          id: "bash",
          label: "Bash",
          description: "Run shell commands",
          enabled: false,
        },
      },
      email_enabled: true,
    });

    const catalog = await getToolCatalog("knowledge_worker", "knoxx_default", "chat_primary");

    expect(request).toHaveBeenCalledWith("/api/tools/catalog?role=knowledge_worker&agent=knoxx_default&actor=chat_primary");
    expect(catalog.tools).toEqual([
      {
        id: "read",
        label: "Read",
        description: "Read a workspace file",
        enabled: true,
      },
      {
        id: "bash",
        label: "Bash",
        description: "Run shell commands",
        enabled: false,
      },
    ]);
    expect(catalog.email_enabled).toBe(true);
    expect(catalog.capability_ids).toEqual(["memory.search"]);
  });
});