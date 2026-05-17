import { describe, expect, it, vi } from "vitest";

vi.mock("./core", () => ({
  API_BASE: "",
  buildKnoxxAuthHeaders: vi.fn(() => ({})),
  request: vi.fn(),
}));

import { getToolCatalog, knoxxChatStart } from "./runtime";
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

describe("knoxxChatStart", () => {
  it("normalizes legacy kebab-case async chat identifiers", async () => {
    vi.mocked(request).mockResolvedValueOnce({
      ok: true,
      queued: true,
      "run-id": "run-1",
      "conversation-id": "conversation-1",
      "session-id": "session-1",
      model: "model-1",
    });

    await expect(knoxxChatStart({
      message: "hello",
      session_id: "session-1",
      model: "model-1",
    })).resolves.toMatchObject({
      ok: true,
      queued: true,
      run_id: "run-1",
      conversation_id: "conversation-1",
      session_id: "session-1",
      model: "model-1",
    });
  });
});
