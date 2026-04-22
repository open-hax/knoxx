import { describe, expect, it, vi } from "vitest";

vi.mock("./core", () => ({
  buildKnoxxAuthHeaders: vi.fn(() => ({})),
  request: vi.fn(),
}));

import { getRun } from "./common";
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
