import { describe, expect, it, vi } from "vitest";

vi.mock("./core", async (importOriginal) => ({
  ...await importOriginal<typeof import("./core")>(),
  API_BASE: "",
  buildKnoxxAuthHeaders: vi.fn(() => ({})),
  request: vi.fn(),
}));

import { acknowledgeActorMailboxEntry, getAudioLibrary, getAudioStreamUrl, getToolCatalog, knoxxChatStart, listActorMailbox, listProxxModels, proxxChat, saveAudioAsset } from "./runtime";
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


describe("runtime wire boundary compatibility", () => {
  it("filters malformed mailbox entries and preserves explicit false durability", async () => {
    vi.mocked(request).mockResolvedValueOnce({ ok: false, box: "unexpected", durable: false, durable_: true,
      entries: [null, {}, { id: "entry/one", source: { actor: "writer" }, metadata: ["legacy"], preview: "Review" }] });
    const result = await listActorMailbox("outbox", "all");
    expect(request).toHaveBeenLastCalledWith("/api/actors/mailbox?box=outbox&limit=100");
    expect(result).toMatchObject({ ok: false, box: "outbox", durable: false });
    expect(result.entries).toHaveLength(1);
    expect(result.entries[0]).toMatchObject({ id: "entry/one", kind: "actor-message", status: "pending", metadata: ["legacy"], preview: "Review" });
  });

  it("encodes acknowledgement IDs and retains a false response without an entry", async () => {
    vi.mocked(request).mockResolvedValueOnce({ ok: false, entry: null });
    await expect(acknowledgeActorMailboxEntry("changes/one")).resolves.toEqual({ ok: false });
    expect(request).toHaveBeenLastCalledWith("/api/actors/mailbox/changes%2Fone/ack", { method: "POST" });
  });

  it("keeps Proxx model order and exact chat sampling payload", async () => {
    vi.mocked(request).mockResolvedValueOnce({ models: [{ id: "z-model" }, { id: "a-model" }] });
    expect((await listProxxModels()).map(model => model.id)).toEqual(["a-model", "z-model"]);
    const payload = { model: "a-model", messages: [{ role: "user", content: "Draft" }], temperature: 0, max_tokens: 0, rag_enabled: false };
    await proxxChat(payload);
    expect(request).toHaveBeenLastCalledWith("/api/proxx/chat", { method: "POST", body: JSON.stringify(payload) });
  });

  it("retains media path encoding, zero depth and generated asset metadata", async () => {
    await getAudioLibrary({ path: "Music/review & mix", depth: 0 });
    expect(request).toHaveBeenLastCalledWith("/api/studio/audio-library?path=Music%2Freview+%26+mix&depth=0");
    expect(getAudioStreamUrl("Music/review & mix.wav")).toBe("/api/studio/stream?path=Music%2Freview+%26+mix.wav");
    await saveAudioAsset("Music/mix.wav", "waveform", "fixture-image", "image/png", 320, 80);
    expect(request).toHaveBeenLastCalledWith("/api/studio/audio-asset", { method: "POST", body: JSON.stringify({ path: "Music/mix.wav", type: "waveform", imageData: "fixture-image", mimeType: "image/png", width: 320, height: 80 }) });
  });
});
