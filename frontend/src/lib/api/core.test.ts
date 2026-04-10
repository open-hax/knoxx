import { describe, expect, it } from "vitest";
import { summarizeErrorPayload, buildKnoxxAuthHeaders } from "../../lib/api/core";
import { normalizeConversationResponse } from "../../lib/api/runtime";
import { traceTextStatusVariant } from "../../components/ToolReceiptBlock";

// ─── summarizeErrorPayload (core.ts) ─────────────────────────────

describe("summarizeErrorPayload", () => {
  it("returns null for null/undefined", () => {
    expect(summarizeErrorPayload(null)).toBeNull();
    expect(summarizeErrorPayload(undefined)).toBeNull();
  });

  it("returns null for non-object primitives", () => {
    expect(summarizeErrorPayload("error string")).toBeNull();
    expect(summarizeErrorPayload(42)).toBeNull();
    expect(summarizeErrorPayload(true)).toBeNull();
  });

  it("returns null for empty object", () => {
    expect(summarizeErrorPayload({})).toBeNull();
  });

  it("extracts error field", () => {
    const result = summarizeErrorPayload({ error: "not found" });
    expect(result).toBe("not found");
  });

  it("extracts detail field", () => {
    const result = summarizeErrorPayload({ detail: "validation failed" });
    expect(result).toBe("validation failed");
  });

  it("extracts message field", () => {
    const result = summarizeErrorPayload({ message: "internal error" });
    expect(result).toBe("internal error");
  });

  it("extracts error_code as code= prefix", () => {
    const result = summarizeErrorPayload({ error_code: "RATE_LIMITED" });
    expect(result).toBe("code=RATE_LIMITED");
  });

  it("extracts model_error field", () => {
    const result = summarizeErrorPayload({ model_error: "context too long" });
    expect(result).toBe("context too long");
  });

  it("joins multiple fields with pipe separator", () => {
    const result = summarizeErrorPayload({
      error: "bad request",
      detail: "missing field",
      error_code: "INVALID",
    });
    expect(result).toBe("bad request | missing field | code=INVALID");
  });

  it("skips non-string fields", () => {
    const result = summarizeErrorPayload({
      error: "real error",
      detail: 42,
      message: true,
    });
    expect(result).toBe("real error");
  });

  it("ignores unknown fields", () => {
    const result = summarizeErrorPayload({ error: "err", extra: "ignored", nested: { a: 1 } });
    expect(result).toBe("err");
  });
});

// ─── buildKnoxxAuthHeaders (core.ts) ────────────────────────────

describe("buildKnoxxAuthHeaders", () => {
  // These tests run in node where window/localStorage don't exist;
  // getStoredAuthValue returns null, so we get env defaults or nothing.

  it("returns Headers instance", () => {
    const headers = buildKnoxxAuthHeaders();
    expect(headers).toBeInstanceOf(Headers);
  });

  it("preserves passed-in headers", () => {
    const headers = buildKnoxxAuthHeaders({ "Content-Type": "application/json" });
    expect(headers.get("Content-Type")).toBe("application/json");
  });

  it("does not overwrite existing x-knoxx-user-email header", () => {
    const headers = buildKnoxxAuthHeaders({ "x-knoxx-user-email": "preset@example.com" });
    expect(headers.get("x-knoxx-user-email")).toBe("preset@example.com");
  });

  it("does not overwrite existing x-knoxx-org-slug header", () => {
    const headers = buildKnoxxAuthHeaders({ "x-knoxx-org-slug": "preset-org" });
    expect(headers.get("x-knoxx-org-slug")).toBe("preset-org");
  });
});

// ─── normalizeConversationResponse (runtime.ts) ─────────────────

describe("normalizeConversationResponse", () => {
  it("extracts snake_case fields from standard response", () => {
    const result = normalizeConversationResponse({
      answer: "hello world",
      run_id: "run-123",
      conversation_id: "conv-456",
      session_id: "sess-789",
      model: "glm-5",
    });
    expect(result.answer).toBe("hello world");
    expect(result.run_id).toBe("run-123");
    expect(result.conversation_id).toBe("conv-456");
    expect(result.session_id).toBe("sess-789");
    expect(result.model).toBe("glm-5");
  });

  it("falls back to camelCase runId when run_id is missing", () => {
    const result = normalizeConversationResponse({
      answer: "test",
      runId: "camel-run-id",
    });
    expect(result.run_id).toBe("camel-run-id");
  });

  it("prefers snake_case over camelCase for run_id", () => {
    const result = normalizeConversationResponse({
      run_id: "snake-run",
      runId: "camel-run",
    });
    expect(result.run_id).toBe("snake-run");
  });

  it("falls back to camelCase conversationId", () => {
    const result = normalizeConversationResponse({
      answer: "x",
      conversationId: "camel-conv",
    });
    expect(result.conversation_id).toBe("camel-conv");
  });

  it("prefers snake_case over camelCase for conversation_id", () => {
    const result = normalizeConversationResponse({
      conversation_id: "snake-conv",
      conversationId: "camel-conv",
    });
    expect(result.conversation_id).toBe("snake-conv");
  });

  it("falls back to camelCase sessionId", () => {
    const result = normalizeConversationResponse({
      answer: "x",
      sessionId: "camel-sess",
    });
    expect(result.session_id).toBe("camel-sess");
  });

  it("prefers snake_case over camelCase for session_id", () => {
    const result = normalizeConversationResponse({
      session_id: "snake-sess",
      sessionId: "camel-sess",
    });
    expect(result.session_id).toBe("snake-sess");
  });

  it("defaults to empty string for non-string answer", () => {
    expect(normalizeConversationResponse({ answer: 42 }).answer).toBe("");
    expect(normalizeConversationResponse({}).answer).toBe("");
  });

  it("defaults to null for missing/non-string IDs", () => {
    const result = normalizeConversationResponse({});
    expect(result.run_id).toBeNull();
    expect(result.conversation_id).toBeNull();
    expect(result.session_id).toBeNull();
    expect(result.model).toBeNull();
  });

  it("handles completely empty response", () => {
    const result = normalizeConversationResponse({});
    expect(result).toEqual({
      answer: "",
      run_id: null,
      conversation_id: null,
      session_id: null,
      model: null,
    });
  });

  it("handles response with only answer", () => {
    const result = normalizeConversationResponse({ answer: "just text" });
    expect(result.answer).toBe("just text");
    expect(result.run_id).toBeNull();
    expect(result.model).toBeNull();
  });
});

// ─── traceTextStatusVariant (ToolReceiptBlock.tsx) ─────────────

describe("traceTextStatusVariant", () => {
  it("returns success for done status", () => {
    expect(traceTextStatusVariant("done")).toBe("success");
  });

  it("returns error for error status", () => {
    expect(traceTextStatusVariant("error")).toBe("error");
  });

  it("returns warning for streaming status", () => {
    expect(traceTextStatusVariant("streaming")).toBe("warning");
  });

  it("returns warning for undefined status", () => {
    expect(traceTextStatusVariant(undefined)).toBe("warning");
  });

  it("returns warning for any other status value", () => {
    // Exhaustiveness guard — unexpected values fall to default
    expect(traceTextStatusVariant("pending" as any)).toBe("warning");
    expect(traceTextStatusVariant("cancelled" as any)).toBe("warning");
  });
});
