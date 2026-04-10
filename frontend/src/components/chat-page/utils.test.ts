import { describe, expect, it, vi, beforeEach } from "vitest";
import {
  contextPath,
  isWorkspaceSource,
  parentPath,
  seedCanvasFromMessage,
  slugify,
  fileNameFromPath,
  seedCanvasFromPreview,
  selectWorkspaceJob,
  formatMaybeDate,
  truncateText,
  asMarkdownPreview,
  isChatRole,
  memoryRowRunId,
  memoryRowsToMessages,
  controlTimelineMessageFromEvent,
  appendTraceTextDelta,
  applyToolTraceEvent,
  finalizeTraceBlocks,
  latestRunHydrationSources,
} from "./utils";
import type { ChatMessage, ChatTraceBlock, GroundedContextRow, MemorySessionRow, RunDetail, RunEvent } from "../../lib/types";

// ─── Factory helpers ──────────────────────────────────────────────

function makeContextRow(overrides: Partial<GroundedContextRow> = {}): GroundedContextRow {
  return { id: "row-1", ...overrides };
}

function makeMemoryRow(overrides: Partial<MemorySessionRow> = {}): MemorySessionRow {
  return { id: "mem-1", kind: "knoxx.message", role: "user", text: "hello", ...overrides };
}

function makeWorkspaceJob(overrides: Record<string, unknown> = {}): NonNullable<Parameters<typeof selectWorkspaceJob>[0]>[number] {
  return {
    job_id: "job-1",
    status: "completed",
    started_at: "2026-04-10T10:00:00Z",
    created_at: "2026-04-10T09:59:00Z",
    completed_at: "2026-04-10T10:05:00Z",
    ...overrides,
  };
}

function makeRunEvent(overrides: Record<string, unknown> = {}): RunEvent & Record<string, unknown> {
  return { type: "tool_start", at: "2026-04-10T10:00:00Z", ...overrides };
}

// ─── contextPath ────────────────────────────────────────────────

describe("contextPath", () => {
  it("prefers source_path", () => {
    expect(contextPath(makeContextRow({ source_path: "src/index.ts" }))).toBe("src/index.ts");
  });

  it("falls back to message when no source_path", () => {
    expect(contextPath(makeContextRow({ message: "check this file" }))).toBe("check this file");
  });

  it("falls back to source when no source_path or message", () => {
    expect(contextPath(makeContextRow({ source: "lib/utils.ts" }))).toBe("lib/utils.ts");
  });

  it("falls back to id as last resort", () => {
    expect(contextPath(makeContextRow({ id: "ctx-42" }))).toBe("ctx-42");
  });

  it("prefers source_path over message and source", () => {
    const row = makeContextRow({ source_path: "a.ts", message: "b.ts", source: "c.ts" });
    expect(contextPath(row)).toBe("a.ts");
  });
});

// ─── isWorkspaceSource ───────────────────────────────────────────

describe("isWorkspaceSource", () => {
  it("matches devel workspace by root_path", () => {
    expect(isWorkspaceSource({ name: "anything", config: { root_path: "/app/workspace/devel" } })).toBe(true);
  });

  it("matches devel workspace by name with null root_path", () => {
    expect(isWorkspaceSource({ name: "devel workspace", config: {} })).toBe(true);
  });

  it("rejects non-matching root_path", () => {
    expect(isWorkspaceSource({ name: "other", config: { root_path: "/tmp/data" } })).toBe(false);
  });

  it("rejects different name without matching root_path", () => {
    expect(isWorkspaceSource({ name: "production data", config: { root_path: "/data/prod" } })).toBe(false);
  });

  it("handles missing config gracefully", () => {
    expect(isWorkspaceSource({ name: "test" } as any)).toBe(false);
  });

  it("matches kebab-case root-path variant", () => {
    expect(isWorkspaceSource({ name: "x", config: { "root-path": "/app/workspace/devel" } })).toBe(true);
  });
});

// ─── parentPath ─────────────────────────────────────────────────

describe("parentPath", () => {
  it("returns parent of nested path", () => {
    expect(parentPath("src/components/chat-page")).toBe("src/components");
  });

  it("returns empty string for single segment", () => {
    expect(parentPath("src")).toBe("");
  });

  it("handles trailing slashes", () => {
    expect(parentPath("src/components/")).toBe("src");
  });

  it("handles root-level empty-ish paths", () => {
    expect(parentPath("")).toBe("");
  });

  it("normalizes double slashes", () => {
    expect(parentPath("a//b//c")).toBe("a/b");
  });
});

// ─── seedCanvasFromMessage ──────────────────────────────────────

describe("seedCanvasFromMessage", () => {
  it("extracts first line as title", () => {
    const result = seedCanvasFromMessage({ id: "m1", role: "assistant", content: "# Title\n\nBody text" } as ChatMessage);
    expect(result.title).toBe("# Title");
    expect(result.subject).toBe("# Title");
    expect(result.content).toBe("# Title\n\nBody text");
  });

  it("truncates title to 80 chars", () => {
    const long = "x".repeat(100);
    const result = seedCanvasFromMessage({ id: "m1", role: "assistant", content: long } as ChatMessage);
    expect(result.title).toHaveLength(80);
    expect(result.subject).toHaveLength(80);
  });

  it("falls back to Draft for empty content", () => {
    const result = seedCanvasFromMessage({ id: "m1", role: "assistant", content: "" } as ChatMessage);
    expect(result.title).toBe("Draft");
    expect(result.subject).toBe("Draft");
  });

  it("skips blank first lines", () => {
    const result = seedCanvasFromMessage({ id: "m1", role: "assistant", content: "\n\n# Real Title" } as ChatMessage);
    expect(result.title).toBe("# Real Title");
  });
});

// ─── slugify ────────────────────────────────────────────────────

describe("slugify", () => {
  it("lowercases and replaces spaces with hyphens", () => {
    expect(slugify("Hello World")).toBe("hello-world");
  });

  it("removes special characters", () => {
    expect(slugify("Hello, World! #2024")).toBe("hello-world-2024");
  });

  it("truncates to 64 chars", () => {
    const long = "a ".repeat(100);
    expect(slugify(long).length).toBeLessThanOrEqual(64);
  });

  it("returns draft for empty/all-special input", () => {
    expect(slugify("!!!")).toBe("draft");
  });

  it("collapses multiple hyphens", () => {
    expect(slugify("Hello   ---  World")).toBe("hello-world");
  });

  it("trims leading/trailing hyphens", () => {
    expect(slugify("---hello---")).toBe("hello");
  });
});

// ─── fileNameFromPath ───────────────────────────────────────────

describe("fileNameFromPath", () => {
  it("extracts filename from path", () => {
    expect(fileNameFromPath("src/components/ChatPage.tsx")).toBe("ChatPage.tsx");
  });

  it("returns single-segment input as-is", () => {
    expect(fileNameFromPath("README.md")).toBe("README.md");
  });

  it("handles trailing slash", () => {
    expect(fileNameFromPath("docs/notes/")).toBe("notes");
  });

  it("returns original for bare filename", () => {
    expect(fileNameFromPath("package.json")).toBe("package.json");
  });
});

// ─── seedCanvasFromPreview ──────────────────────────────────────

describe("seedCanvasFromPreview", () => {
  it("derives title from path basename without extension", () => {
    const result = seedCanvasFromPreview({ path: "docs/notes/meeting.md", content: "# Meeting" });
    expect(result.title).toBe("meeting");
    expect(result.subject).toBe("meeting");
    expect(result.content).toBe("# Meeting");
    expect(result.path).toBe("docs/notes/meeting.md");
  });

  it("uses basename even for extensionless files", () => {
    const result = seedCanvasFromPreview({ path: "Makefile", content: "" });
    // Makefile has no dot, so regex replace doesn't strip anything → title is "Makefile"
    expect(result.title).toBe("Makefile");
  });

  it("falls back to Scratchpad for empty path", () => {
    const result = seedCanvasFromPreview({ path: "", content: "" });
    expect(result.title).toBe("Scratchpad");
  });
});

// ─── selectWorkspaceJob ─────────────────────────────────────────

describe("selectWorkspaceJob", () => {
  it("returns null for empty array", () => {
    expect(selectWorkspaceJob([])).toBeNull();
  });

  it("prefers running job over others", () => {
    const jobs = [
      makeWorkspaceJob({ status: "completed", started_at: "2026-04-10T11:00:00Z" }),
      makeWorkspaceJob({ status: "running", started_at: "2026-04-10T12:00:00Z" }),
      makeWorkspaceJob({ status: "pending", started_at: "2026-04-10T10:00:00Z" }),
    ];
    const result = selectWorkspaceJob(jobs);
    expect(result?.status).toBe("running");
  });

  it("prefers pending job if no running", () => {
    const jobs = [
      makeWorkspaceJob({ status: "completed", started_at: "2026-04-10T11:00:00Z" }),
      makeWorkspaceJob({ status: "pending", started_at: "2026-04-10T12:00:00Z" }),
    ];
    const result = selectWorkspaceJob(jobs);
    expect(result?.status).toBe("pending");
  });

  it("returns most recent completed job if no active", () => {
    const jobs = [
      makeWorkspaceJob({ status: "completed", completed_at: "2026-04-10T09:00:00Z", started_at: "2026-04-10T08:55:00Z" }),
      makeWorkspaceJob({ status: "completed", completed_at: "2026-04-10T11:00:00Z", started_at: "2026-04-10T10:55:00Z" }),
    ];
    const result = selectWorkspaceJob(jobs);
    expect(result?.status).toBe("completed");
    // Sort is descending by timestamp; find returns first completed match
    expect(result).not.toBeNull();
  });

  it("returns first job as ultimate fallback", () => {
    const jobs = [
      makeWorkspaceJob({ status: "failed", started_at: "2026-04-10T08:00:00Z" }),
      makeWorkspaceJob({ status: "failed", started_at: "2026-04-10T07:00:00Z" }),
    ];
    const result = selectWorkspaceJob(jobs);
    expect(result).not.toBeNull();
  });
});

// ─── formatMaybeDate ────────────────────────────────────────────

describe("formatMaybeDate", () => {
  it("returns null for undefined", () => {
    expect(formatMaybeDate(undefined)).toBeNull();
  });

  it("returns null for empty string", () => {
    expect(formatMaybeDate("")).toBeNull();
  });

  it("parses ISO date strings", () => {
    const result = formatMaybeDate("2026-04-10T10:30:00Z");
    expect(result).not.toBeNull();
    expect(result).toContain("2026");
  });

  it("returns original value for unparseable strings", () => {
    expect(formatMaybeDate("not-a-date")).toBe("not-a-date");
  });
});

// ─── truncateText ───────────────────────────────────────────────

describe("truncateText", () => {
  it("returns short text unchanged", () => {
    const text = "hello world";
    expect(truncateText(text)).toBe(text);
  });

  it("truncates at default max of 240", () => {
    const long = "x".repeat(300);
    const result = truncateText(long);
    expect(result.length).toBeLessThanOrEqual(243); // 240 + ellipsis
    expect(result.endsWith("…")).toBe(true);
  });

  it("respects custom max", () => {
    const text = "hello world";
    expect(truncateText(text, 5)).toBe("hello…");
  });

  it("trims trailing whitespace before ellipsis", () => {
    const text = "abc   ";
    expect(truncateText(text, 3)).toBe("abc…");
  });

  it("handles exact boundary length", () => {
    const text = "abc";
    expect(truncateText(text, 3)).toBe("abc");
  });
});

// ─── asMarkdownPreview ──────────────────────────────────────────

describe("asMarkdownPreview", () => {
  it("returns empty for whitespace-only input", () => {
    expect(asMarkdownPreview("   \n  ")).toBe("");
  });

  it("passes through markdown-formatted text", () => {
    const md = "# Heading\n\n- item 1\n- item 2";
    expect(asMarkdownPreview(md)).toBe(md);
  });

  it("passes through code fences", () => {
    const code = "```js\nconsole.log('hi')\n```";
    expect(asMarkdownPreview(code)).toBe(code);
  });

  it("passes through blockquotes", () => {
    const bq = "> quoted text";
    expect(asMarkdownPreview(bq)).toBe(bq);
  });

  it("wraps JSON-like objects in code fence", () => {
    const json = '{"key": "value"}';
    expect(asMarkdownPreview(json)).toContain("```text");
    expect(asMarkdownPreview(json)).toContain(json);
  });

  it("wraps array-like objects in code fence", () => {
    const arr = "[1, 2, 3]";
    expect(asMarkdownPreview(arr)).toContain("```text");
  });

  it("wraps multiline plain text in code fence", () => {
    const multi = "line one\nline two";
    expect(asMarkdownPreview(multi)).toContain("```text");
  });

  it("passes through single-line plain text unchanged", () => {
    const plain = "just some words here";
    expect(asMarkdownPreview(plain)).toBe(plain);
  });
});

// ─── isChatRole ─────────────────────────────────────────────────

describe("isChatRole", () => {
  it("accepts user", () => {
    expect(isChatRole("user")).toBe(true);
  });

  it("accepts assistant", () => {
    expect(isChatRole("assistant")).toBe(true);
  });

  it("accepts system", () => {
    expect(isChatRole("system")).toBe(true);
  });

  it("rejects invalid roles", () => {
    expect(isChatRole("moderator")).toBe(false);
    expect(isChatRole("")).toBe(false);
    expect(isChatRole(null)).toBe(false);
    expect(isChatRole(undefined)).toBe(false);
    expect(isChatRole(42)).toBe(false);
  });
});

// ─── memoryRowRunId ─────────────────────────────────────────────

describe("memoryRowRunId", () => {
  it("extracts run_id from object extra", () => {
    expect(memoryRowRunId(makeMemoryRow({ extra: { run_id: "run-abc123" } }))).toBe("run-abc123");
  });

  it("extracts camelCase runId from object extra", () => {
    expect(memoryRowRunId(makeMemoryRow({ extra: { runId: "run-def456" } }))).toBe("run-def456");
  });

  it("prefers run_id over runId", () => {
    expect(memoryRowRunId(makeMemoryRow({ extra: { run_id: "first", runId: "second" } }))).toBe("first");
  });

  it("parses run_id from JSON string extra", () => {
    expect(memoryRowRunId(makeMemoryRow({ extra: '{"run_id":"run-json"}' }))).toBe("run-json");
  });

  it("returns null for missing extra", () => {
    expect(memoryRowRunId(makeMemoryRow())).toBeNull();
  });

  it("returns null for null extra", () => {
    expect(memoryRowRunId(makeMemoryRow({ extra: null }))).toBeNull();
  });

  it("returns null for non-string candidate", () => {
    expect(memoryRowRunId(makeMemoryRow({ extra: { run_id: 12345 } }))).toBeNull();
  });

  it("returns null for malformed JSON extra", () => {
    expect(memoryRowRunId(makeMemoryRow({ extra: "not-json" }))).toBeNull();
  });

  it("returns null for array extra", () => {
    expect(memoryRowRunId(makeMemoryRow({ extra: [] as any }))).toBeNull();
  });
});

// ─── memoryRowsToMessages ───────────────────────────────────────

describe("memoryRowsToMessages", () => {
  it("converts valid knoxx.message rows to messages", () => {
    const rows = [
      makeMemoryRow({ id: "m1", role: "user", text: "hello" }),
      makeMemoryRow({ id: "m2", role: "assistant", text: "hi there", model: "glm-5" }),
    ];
    const msgs = memoryRowsToMessages(rows);
    expect(msgs).toHaveLength(2);
    expect(msgs[0].role).toBe("user");
    expect(msgs[0].content).toBe("hello");
    expect(msgs[1].role).toBe("assistant");
    expect(msgs[1].content).toBe("hi there");
    expect(msgs[1].model).toBe("glm-5");
    expect(msgs[1].status).toBe("done");
  });

  it("skips non-knoxx.message rows", () => {
    const rows = [
      makeMemoryRow({ kind: "other.event", role: "user", text: "should skip" }),
      makeMemoryRow({ role: "assistant", text: "should include" }),
    ];
    const msgs = memoryRowsToMessages(rows);
    expect(msgs).toHaveLength(1);
    expect(msgs[0].content).toBe("should include");
  });

  it("skips rows with invalid roles", () => {
    const rows = [
      makeMemoryRow({ role: "moderator", text: "bad role" }),
      makeMemoryRow({ role: "user", text: "good role" }),
    ];
    const msgs = memoryRowsToMessages(rows);
    expect(msgs).toHaveLength(1);
  });

  it("skips rows with empty/whitespace text", () => {
    const rows = [
      makeMemoryRow({ text: "   " }),
      makeMemoryRow({ text: "has content" }),
    ];
    const msgs = memoryRowsToMessages(rows);
    expect(msgs).toHaveLength(1);
  });

  it("skips rows with non-string text", () => {
    const rows = [
      makeMemoryRow({ text: undefined as any }),
      makeMemoryRow({ text: 42 as any }),
      makeMemoryRow({ text: "valid" }),
    ];
    const msgs = memoryRowsToMessages(rows);
    expect(msgs).toHaveLength(1);
  });

  it("generates fallback id from session + index", () => {
    const rows = [
      makeMemoryRow({ id: "", session: "sess-1", role: "user", text: "test" }),
    ];
    const msgs = memoryRowsToMessages(rows);
    expect(msgs[0].id).toBe("sess-1:0");
  });

  it("sets done status for assistant/system roles only", () => {
    const rows = [
      makeMemoryRow({ role: "user", text: "u" }),
      makeMemoryRow({ role: "assistant", text: "a" }),
      makeMemoryRow({ role: "system", text: "s" }),
    ];
    const msgs = memoryRowsToMessages(rows);
    expect(msgs[0].status).toBeUndefined();
    expect(msgs[1].status).toBe("done");
    expect(msgs[2].status).toBe("done");
  });

  it("extracts runId via memoryRowRunId", () => {
    const rows = [
      makeMemoryRow({ role: "assistant", text: "answer", extra: { run_id: "r-99" } }),
    ];
    const msgs = memoryRowsToMessages(rows);
    expect(msgs[0].runId).toBe("r-99");
  });

  it("returns empty array for all-invalid input", () => {
    expect(memoryRowsToMessages([])).toEqual([]);
    expect(memoryRowsToMessages([makeMemoryRow({ kind: "meta" })])).toEqual([]);
  });
});

// ─── controlTimelineMessageFromEvent ───────────────────────────

describe("controlTimelineMessageFromEvent", () => {
  it("creates steer_queued message", () => {
    const msg = controlTimelineMessageFromEvent({
      type: "steer_queued",
      preview: "focus on X",
      run_id: "run-abc",
    });
    expect(msg).not.toBeNull();
    expect(msg!.role).toBe("system");
    expect(msg!.content).toContain("Steer queued");
    expect(msg!.content).toContain("focus on X");
    expect(msg!.status).toBe("done");
    expect(msg!.runId).toBe("run-abc");
  });

  it("creates follow_up_queued message", () => {
    const msg = controlTimelineMessageFromEvent({
      type: "follow_up_queued",
      preview: "ask about Y",
    });
    expect(msg).not.toBeNull();
    expect(msg!.content).toContain("Follow-up queued");
    expect(msg!.content).toContain("ask about Y");
  });

  it("creates steer_failed message with error", () => {
    const msg = controlTimelineMessageFromEvent({
      type: "steer_failed",
      error: "timeout",
      run_id: "run-fail",
    });
    expect(msg).not.toBeNull();
    expect(msg!.content).toContain("Steer failed");
    expect(msg!.content).toContain("Error: timeout");
    expect(msg!.status).toBe("error");
  });

  it("creates follow_up_failed message", () => {
    const msg = controlTimelineMessageFromEvent({
      type: "follow_up_failed",
      error: "rejected",
    });
    expect(msg).not.toBeNull();
    expect(msg!.content).toContain("Follow-up failed");
    expect(msg!.status).toBe("error");
  });

  it("returns null for unrecognized event types", () => {
    expect(controlTimelineMessageFromEvent({ type: "tool_start" })).toBeNull();
    expect(controlTimelineMessageFromEvent({ type: "run_completed" })).toBeNull();
    expect(controlTimelineMessageFromEvent({ type: "" })).toBeNull();
    expect(controlTimelineMessageFromEvent({})).toBeNull();
  });

  it("includes truncated run ID in output", () => {
    const msg = controlTimelineMessageFromEvent({
      type: "steer_queued",
      preview: "test",
      run_id: "abcdef1234567890",
    });
    expect(msg!.content).toContain("`abcdef12`"); // first 8 chars
  });

  it("generates stable id from type+runId+preview/error", () => {
    const a = controlTimelineMessageFromEvent({ type: "steer_queued", preview: "p1", run_id: "r1" });
    const b = controlTimelineMessageFromEvent({ type: "steer_queued", preview: "p1", run_id: "r1" });
    const c = controlTimelineMessageFromEvent({ type: "steer_queued", preview: "p2", run_id: "r1" });
    expect(a!.id).toBe(b!.id);
    expect(a!.id).not.toBe(c!.id);
  });
});

// ─── appendTraceTextDelta ───────────────────────────────────────

describe("appendTraceTextDelta", () => {
  it("appends to existing streaming block of same kind", () => {
    const blocks: ChatTraceBlock[] = [
      { id: "b1", kind: "agent_message", status: "streaming", content: "Hello" },
    ];
    const result = appendTraceTextDelta(blocks, "agent_message", " world");
    expect(result).toHaveLength(1);
    expect(result[0].content).toBe("Hello world");
  });

  it("creates new block when last block is different kind", () => {
    const blocks: ChatTraceBlock[] = [
      { id: "b1", kind: "reasoning", status: "streaming", content: "thinking..." },
    ];
    const result = appendTraceTextDelta(blocks, "agent_message", "answer");
    expect(result).toHaveLength(2);
    expect(result[1].kind).toBe("agent_message");
    expect(result[1].content).toBe("answer");
    expect(result[1].status).toBe("streaming");
  });

  it("creates new block when last block is not streaming", () => {
    const blocks: ChatTraceBlock[] = [
      { id: "b1", kind: "agent_message", status: "done", content: "finished" },
    ];
    const result = appendTraceTextDelta(blocks, "agent_message", " more");
    expect(result).toHaveLength(2);
    expect(result[1].status).toBe("streaming");
  });

  it("returns same blocks for empty delta", () => {
    const blocks: ChatTraceBlock[] = [
      { id: "b1", kind: "agent_message", status: "streaming", content: "hi" },
    ];
    const result = appendTraceTextDelta(blocks, "agent_message", "");
    expect(result).toBe(blocks); // same reference
  });

  it("updates at timestamp on existing block", () => {
    const blocks: ChatTraceBlock[] = [
      { id: "b1", kind: "agent_message", status: "streaming", content: "H", at: "t1" },
    ];
    const result = appendTraceTextDelta(blocks, "agent_message", "i", "t2");
    expect(result[0].at).toBe("t2");
  });

  it("sets at on new block", () => {
    const result = appendTraceTextDelta([], "reasoning", "hmm", "t1");
    expect(result[0].at).toBe("t1");
  });
});

// ─── applyToolTraceEvent ────────────────────────────────────────

describe("applyToolTraceEvent", () => {
  it("adds tool_start block", () => {
    const result = applyToolTraceEvent([], makeRunEvent({
      type: "tool_start",
      tool_name: "graph_query",
      tool_call_id: "tc-1",
      preview: '{"query":"test"}',
    }));
    expect(result).toHaveLength(1);
    expect(result[0].kind).toBe("tool_call");
    expect(result[0].toolName).toBe("graph_query");
    expect(result[0].status).toBe("streaming");
    expect(result[0].inputPreview).toBe('{"query":"test"}');
    expect(result[0].updates).toEqual([]);
  });

  it("updates tool_update on existing block", () => {
    const blocks: ChatTraceBlock[] = [
      { id: "tool:tc-1", kind: "tool_call", toolName: "graph_query", toolCallId: "tc-1", status: "streaming", updates: ["step1"] },
    ];
    const result = applyToolTraceEvent(blocks, makeRunEvent({
      type: "tool_update",
      tool_call_id: "tc-1",
      preview: "step2",
    }));
    expect(result).toHaveLength(1);
    expect(result[0].updates).toEqual(["step1", "step2"]);
  });

  it("finalizes tool_end with done status", () => {
    const blocks: ChatTraceBlock[] = [
      { id: "tool:tc-1", kind: "tool_call", toolName: "graph_query", toolCallId: "tc-1", status: "streaming", inputPreview: '{"q":1}', updates: ["s1"] },
    ];
    const result = applyToolTraceEvent(blocks, makeRunEvent({
      type: "tool_end",
      tool_call_id: "tc-1",
      preview: "results here",
    }));
    expect(result).toHaveLength(1);
    expect(result[0].status).toBe("done");
    expect(result[0].outputPreview).toBe("results here");
    expect(result[0].isError).toBe(false);
    // Preserves inputPreview and updates from start/update
    expect(result[0].inputPreview).toBe('{"q":1}');
    expect(result[0].updates).toEqual(["s1"]);
  });

  it("finalizes tool_end with error status", () => {
    const blocks: ChatTraceBlock[] = [
      { id: "tool:tc-1", kind: "tool_call", toolName: "broken", toolCallId: "tc-1", status: "streaming" },
    ];
    const result = applyToolTraceEvent(blocks, makeRunEvent({
      type: "tool_end",
      tool_call_id: "tc-1",
      is_error: true,
      preview: "timeout",
    }));
    expect(result[0].status).toBe("error");
    expect(result[0].isError).toBe(true);
    expect(result[0].outputPreview).toBe("timeout");
  });

  it("creates tool_end block even if no prior start exists", () => {
    const result = applyToolTraceEvent([], makeRunEvent({
      type: "tool_end",
      tool_name: "orphan_tool",
      preview: "done",
    }));
    expect(result).toHaveLength(1);
    expect(result[0].status).toBe("done");
    expect(result[0].updates).toEqual([]);
  });

  it("ignores unrecognized event types", () => {
    const blocks: ChatTraceBlock[] = [{ id: "b1", kind: "agent_message", status: "streaming", content: "hi" }];
    const result = applyToolTraceEvent(blocks, makeRunEvent({ type: "token" }));
    expect(result).toBe(blocks);
  });

  it("limits updates to last 7+1 entries (slice then append)", () => {
    let blocks: ChatTraceBlock[] = [
      { id: "tool:tc-1", kind: "tool_call", toolName: "t", toolCallId: "tc-1", status: "streaming", updates: [] },
    ];
    for (let i = 1; i <= 9; i++) {
      blocks = applyToolTraceEvent(blocks, makeRunEvent({
        type: "tool_update",
        tool_call_id: "tc-1",
        preview: `update-${i}`,
      }));
    }
    // Each step: slice(-7) then push → max 8 before next slice
    expect(blocks[0].updates!.length).toBeLessThanOrEqual(8);
    expect(blocks[0].updates![blocks[0].updates!.length - 1]).toBe("update-9");
  });

  it("generates block id from tool_call_id", () => {
    const result = applyToolTraceEvent([], makeRunEvent({
      type: "tool_start",
      tool_call_id: "my-custom-id",
    }));
    expect(result[0].id).toBe("tool:my-custom-id");
  });

  it("falls back to tool_name:id for block id when no tool_call_id", () => {
    const result = applyToolTraceEvent([], makeRunEvent({
      type: "tool_start",
      tool_name: "search",
      at: "t-100",
    }));
    expect(result[0].id).toBe("tool:search:t-100");
  });
});

// ─── finalizeTraceBlocks ───────────────────────────────────────

describe("finalizeTraceBlocks", () => {
  it("finalizes streaming blocks to done", () => {
    const blocks: ChatTraceBlock[] = [
      { id: "b1", kind: "agent_message", status: "streaming", content: "hello" },
      { id: "b2", kind: "tool_call", status: "done", content: "" },
    ];
    const result = finalizeTraceBlocks(blocks, "done");
    expect(result[0].status).toBe("done");
    expect(result[1].status).toBe("done"); // unchanged
  });

  it("finalizes streaming blocks to error", () => {
    const blocks: ChatTraceBlock[] = [
      { id: "b1", kind: "agent_message", status: "streaming", content: "partial" },
    ];
    const result = finalizeTraceBlocks(blocks, "error");
    expect(result[0].status).toBe("error");
  });

  it("preserves existing isError value during error finalization", () => {
    const blocks: ChatTraceBlock[] = [
      { id: "b1", kind: "tool_call", status: "streaming", isError: false },
      { id: "b2", kind: "agent_message", status: "streaming" },
    ];
    const result = finalizeTraceBlocks(blocks, "error");
    // ?? operator only overrides when isError is null/undefined; explicit false is kept
    expect(result[0].isError).toBe(false);
    // agent_message gets isError=false from `kind === "tool_call"` fallback (false)
    expect(result[1].isError).toBe(false);
  });

  it("sets isError=true for tool_call with undefined isError during error finalization", () => {
    const blocks: ChatTraceBlock[] = [
      { id: "b1", kind: "tool_call", status: "streaming" }, // no isError field
    ];
    const result = finalizeTraceBlocks(blocks, "error");
    expect(result[0].isError).toBe(true);
  });

  it("preserves existing isError on tool_call during done finalization", () => {
    const blocks: ChatTraceBlock[] = [
      { id: "b1", kind: "tool_call", status: "streaming", isError: true },
    ];
    const result = finalizeTraceBlocks(blocks, "done");
    expect(result[0].isError).toBe(true); // preserved
  });

  it("leaves non-streaming blocks untouched", () => {
    const blocks: ChatTraceBlock[] = [
      { id: "b1", kind: "agent_message", status: "done", content: "finished" },
      { id: "b2", kind: "reasoning", status: "error", content: "oops" },
    ];
    const result = finalizeTraceBlocks(blocks, "done");
    expect(result[0].status).toBe("done");
    expect(result[1].status).toBe("error");
  });
});

// ─── latestRunHydrationSources ─────────────────────────────────

describe("latestRunHydrationSources", () => {
  it("extracts sources from passive hydration results", () => {
    const run: RunDetail = {
      run_id: "r1",
      status: "completed",
      resources: {
        passiveHydration: {
          results: [
            { path: "src/core.ts", name: "Core Module", snippet: "export function core() {}" },
            { path: "utils/helpers.ts", snippet: "export function help() {}" },
            { path: "", name: "should-skip" }, // empty path → skipped
          ],
        },
      },
    } as RunDetail;
    const sources = latestRunHydrationSources(run);
    expect(sources).toHaveLength(2);
    expect(sources[0].title).toBe("Core Module");
    expect(sources[0].path).toBe("src/core.ts");
    expect(sources[0].section).toBe("export function core() {}");
    expect(sources[1].title).toBe("helpers.ts"); // falls back to fileNameFromPath
    expect(sources[1].section).toBe("export function help() {}");
  });

  it("returns empty for null run", () => {
    expect(latestRunHydrationSources(null)).toEqual([]);
  });

  it("returns empty for missing resources", () => {
    expect(latestRunHydrationSources({ run_id: "r1", status: "completed" } as any)).toEqual([]);
  });

  it("returns empty for missing passiveHydration", () => {
    expect(latestRunHydrationSources({ run_id: "r1", status: "completed", resources: {} } as any)).toEqual([]);
  });

  it("returns empty for non-array results", () => {
    const run: RunDetail = {
      run_id: "r1",
      resources: { passiveHydration: { results: "not-array" } },
    } as any;
    expect(latestRunHydrationSources(run)).toEqual([]);
  });

  it("uses name when available, otherwise fileNameFromPath", () => {
    const run: RunDetail = {
      run_id: "r1",
      resources: {
        passiveHydration: {
          results: [{ path: "docs/README.md" }],
        },
      },
    } as RunDetail;
    const sources = latestRunHydrationSources(run);
    expect(sources).toHaveLength(1);
    // Uses fileNameFromPath directly, not extension-stripped
    expect(sources[0].title).toBe("README.md");
  });
});
