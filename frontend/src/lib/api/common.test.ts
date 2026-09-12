import { afterEach, describe, expect, it, vi } from "vitest";

vi.mock("./core", () => ({
  buildKnoxxAuthHeaders: vi.fn(() => ({})),
  request: vi.fn(),
}));

import { getRun, getTranslationDocument, getTranslationSftExport, listTranslationSegments, reviewTranslationDocument } from "./common";
import { request } from "./core";

describe("getRun", () => {
  it("hydrates assistant content_parts into frontend contentParts", async () => {
    vi.mocked(request).mockResolvedValueOnce({
      run_id: "run-1",
      status: "completed",
      answer: "Here is the workspace media.",
      content_parts: [
        {
          type: "output_image",
          image_url: { url: "/api/multimodal/files/image-1" },
          mime_type: "image/png",
          filename: "plot.png",
          size: 42,
        },
        {
          type: "output_audio",
          data: "QUFBQQ==",
          mimeType: "audio/wav",
          filename: "clip.wav",
        },
      ],
      request_messages: [
        {
          role: "user",
          content: "show me the waveform",
          "content-parts": [
            {
              type: "image",
              url: "/api/multimodal/files/input-1",
              mimeType: "image/png",
            },
          ],
        },
      ],
      tool_receipts: [
        {
          id: "tool-1",
          tool_name: "workspace_media.attach",
          status: "completed",
          content_parts: [
            {
              type: "audio",
              data: "QUFBQQ==",
              mimeType: "audio/wav",
              filename: "receipt.wav",
            },
          ],
        },
      ],
      settings: {},
      resources: {},
    });

    const run = await getRun("run-1");

    expect(run.contentParts).toEqual([
      {
        type: "image",
        url: "/api/multimodal/files/image-1",
        data: undefined,
        mimeType: "image/png",
        filename: "plot.png",
        size: 42,
      },
      {
        type: "audio",
        url: undefined,
        data: "data:audio/wav;base64,QUFBQQ==",
        mimeType: "audio/wav",
        filename: "clip.wav",
        size: undefined,
      },
      {
        type: "audio",
        url: undefined,
        data: "data:audio/wav;base64,QUFBQQ==",
        mimeType: "audio/wav",
        filename: "receipt.wav",
        size: undefined,
      },
    ]);

    expect(run.request_messages[0]?.contentParts).toEqual([
      {
        type: "image",
        url: "/api/multimodal/files/input-1",
        data: undefined,
        mimeType: "image/png",
        filename: undefined,
        size: undefined,
      },
    ]);

    expect(run.tool_receipts?.[0]?.contentParts).toEqual([
      {
        type: "audio",
        url: undefined,
        data: "data:audio/wav;base64,QUFBQQ==",
        mimeType: "audio/wav",
        filename: "receipt.wav",
        size: undefined,
      },
    ]);
  });
});


afterEach(() => { vi.unstubAllGlobals(); });

describe("translation API compatibility exports", () => {
  it("encodes selectors without treating zero pagination or all status as missing", async () => {
    await listTranslationSegments({ project: "wiki & notes", status: "all", target_lang: "pt-BR", limit: 0, offset: 0 });
    expect(request).toHaveBeenLastCalledWith("/api/translations/segments?project=wiki+%26+notes&target_lang=pt-BR&limit=0&offset=0");
    await getTranslationDocument("doc/one", "zh/Hant");
    expect(request).toHaveBeenLastCalledWith("/api/translations/documents/doc%2Fone/zh%2FHant");
    const payload = { overall: "approve" as const, segment_overrides: { "s/1": { overall: "needs_edit" as const, corrected_text: "Better" } } };
    await reviewTranslationDocument("doc/one", "zh/Hant", payload);
    expect(request).toHaveBeenLastCalledWith("/api/translations/documents/doc%2Fone/zh%2FHant/review", { method: "POST", body: JSON.stringify(payload) });
  });

  it("keeps the text export response and explicit false option", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response("training row\n"));
    vi.stubGlobal("fetch", fetchMock);
    await expect(getTranslationSftExport({ project: "wiki", includeCorrected: false })).resolves.toBe("training row\n");
    expect(fetchMock).toHaveBeenCalledWith("/api/translations/export/sft?project=wiki&include_corrected=false", { headers: {} });
  });

  it("retains export failure bodies and status fallback", async () => {
    vi.stubGlobal("fetch", vi.fn()
      .mockResolvedValueOnce(new Response("Access denied", { status: 403 }))
      .mockResolvedValueOnce(new Response("", { status: 503 })));
    await expect(getTranslationSftExport({ project: "wiki" })).rejects.toThrow("Access denied");
    await expect(getTranslationSftExport({ project: "wiki" })).rejects.toThrow("Failed to export SFT: 503");
  });
});
