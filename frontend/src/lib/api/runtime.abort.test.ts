import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";
import { knoxxAbort } from "../../lib/api/runtime";

// Mock the request function
vi.mock("../../lib/api/core", () => ({
  request: vi.fn(),
}));

import { request } from "../../lib/api/core";

describe("knoxxAbort", () => {
  beforeEach(() => {
    vi.mocked(request).mockReset();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it("calls abort endpoint with conversation_id and session_id", async () => {
    vi.mocked(request).mockResolvedValue({
      ok: true,
      aborted: true,
      conversation_id: "conv-123",
      session_id: "sess-456",
    });

    const result = await knoxxAbort({
      conversation_id: "conv-123",
      session_id: "sess-456",
    });

    expect(request).toHaveBeenCalledWith("/api/knoxx/abort", {
      method: "POST",
      body: JSON.stringify({
        conversation_id: "conv-123",
        session_id: "sess-456",
      }),
    });

    expect(result.ok).toBe(true);
    expect(result.aborted).toBe(true);
  });

  it("handles abort failure", async () => {
    vi.mocked(request).mockResolvedValue({
      ok: false,
      aborted: false,
      error: "No active session found",
    });

    const result = await knoxxAbort({
      conversation_id: "conv-123",
      session_id: "sess-456",
    });

    expect(result.ok).toBe(false);
    expect(result.aborted).toBe(false);
  });

  it("works with only conversation_id", async () => {
    vi.mocked(request).mockResolvedValue({
      ok: true,
      aborted: true,
      conversation_id: "conv-123",
    });

    const result = await knoxxAbort({
      conversation_id: "conv-123",
    });

    expect(request).toHaveBeenCalledWith("/api/knoxx/abort", {
      method: "POST",
      body: JSON.stringify({
        conversation_id: "conv-123",
      }),
    });

    expect(result.ok).toBe(true);
  });

  it("works with only session_id", async () => {
    vi.mocked(request).mockResolvedValue({
      ok: true,
      aborted: true,
      session_id: "sess-456",
    });

    const result = await knoxxAbort({
      session_id: "sess-456",
    });

    expect(request).toHaveBeenCalledWith("/api/knoxx/abort", {
      method: "POST",
      body: JSON.stringify({
        session_id: "sess-456",
      }),
    });

    expect(result.ok).toBe(true);
  });

  it("handles network errors", async () => {
    vi.mocked(request).mockRejectedValue(new Error("Network error"));

    const result = await knoxxAbort({
      conversation_id: "conv-123",
      session_id: "sess-456",
    });

    expect(result.ok).toBe(false);
    expect(result.error).toBe("Network error");
  });
});
