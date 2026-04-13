import { Badge, Card, Markdown } from "@open-hax/uxx";
import type { ChatTraceBlock, ToolReceipt, RunEvent } from "../lib/types";

function truncateText(value: string, max = 240): string {
  if (value.length <= max) return value;
  return `${value.slice(0, max).trimEnd()}…`;
}

function asMarkdownPreview(value: string): string {
  const trimmed = value.trim();
  if (!trimmed) return "";
  if (/^(```|#{1,6}\s|>\s|[-*+]\s|\d+\.\s)/m.test(trimmed)) {
    return value;
  }
  if (trimmed.startsWith("{") || trimmed.startsWith("[") || value.includes("\n")) {
    return `\`\`\`text\n${value}\n\`\`\``;
  }
  return value;
}

function tryParseJson(value: string): unknown | null {
  const trimmed = value.trim();
  if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) return null;
  try {
    return JSON.parse(trimmed) as unknown;
  } catch {
    return null;
  }
}

function summarizeStructuredValue(value: unknown): string | null {
  if (typeof value === "string") {
    return value.trim() ? value : null;
  }
  if (Array.isArray(value)) {
    const lines = value
      .map((item) => summarizeStructuredValue(item))
      .filter((item): item is string => Boolean(item))
      .slice(0, 8);
    return lines.length > 0 ? lines.map((line) => `- ${line}`).join("\n") : null;
  }
  if (!value || typeof value !== "object") return null;
  const record = value as Record<string, unknown>;
  const preferredKeys = [
    "content",
    "text",
    "answer",
    "message",
    "result",
    "output",
    "preview",
    "summary",
    "translated_text",
    "corrected_text",
    "snippet",
  ];
  for (const key of preferredKeys) {
    const summarized = summarizeStructuredValue(record[key]);
    if (summarized) return summarized;
  }
  for (const key of ["rows", "hits", "results", "sources", "items", "documents"]) {
    const summarized = summarizeStructuredValue(record[key]);
    if (summarized) return summarized;
  }
  return null;
}

function toolResultMarkdown(value: string): string {
  const parsed = tryParseJson(value);
  const summarized = parsed ? summarizeStructuredValue(parsed) : null;
  return asMarkdownPreview(summarized ?? value);
}

function toolInputSummary(value: string): string | null {
  const parsed = tryParseJson(value);
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    return value.trim() && !value.trim().startsWith("{") ? value : null;
  }
  const record = parsed as Record<string, unknown>;
  const parts = [record.query, record.q, record.path, record.url, record.document_id]
    .filter((item): item is string => typeof item === "string" && item.trim().length > 0)
    .slice(0, 2);
  return parts.length > 0 ? parts.join(" • ") : null;
}

export interface ToolReceiptBlockProps {
  receipt: ToolReceipt;
  isLive?: boolean;
  defaultExpanded?: boolean;
}

export function ToolReceiptBlock({ receipt, isLive, defaultExpanded = false }: ToolReceiptBlockProps) {
  const status = receipt.status ?? "running";
  const isRunning = status === "running";
  const isError = receipt.is_error || status === "failed";
  const toolName = receipt.tool_name ?? receipt.id ?? "tool";
  const inputSummary = receipt.input_preview ? toolInputSummary(receipt.input_preview) : null;
  const resultMarkdown = receipt.result_preview ? toolResultMarkdown(truncateText(receipt.result_preview, 2400)) : "";
  const liveUpdateMarkdown = !resultMarkdown && receipt.updates && receipt.updates.length > 0
    ? toolResultMarkdown(receipt.updates[receipt.updates.length - 1])
    : "";

  const statusVariant = isRunning ? "warning" : isError ? "error" : "success";
  const statusLabel = isRunning ? "running" : isError ? "failed" : "completed";

  const borderStyle = isRunning
    ? "1px solid var(--token-colors-accent-cyan)"
    : isError
      ? "1px solid var(--token-colors-accent-red)"
      : "1px solid var(--token-colors-accent-green)";

  const bgStyle = isRunning
    ? "var(--token-colors-alpha-cyan-_08)"
    : isError
      ? "var(--token-colors-alpha-red-_08)"
      : "var(--token-colors-alpha-green-_08)";

  return (
    <Card
      variant="outlined"
      padding="sm"
      style={{
        border: borderStyle,
        background: bgStyle,
        marginBottom: 8,
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 8 }}>
        <div style={{ fontSize: 12, fontWeight: 600, color: "var(--token-colors-text-default)" }}>
          {toolName}
        </div>
        <Badge size="sm" variant={statusVariant}>
          {isLive && isRunning ? "streaming..." : statusLabel}
        </Badge>
        {isRunning && (
          <span
            style={{
              width: 8,
              height: 8,
              borderRadius: "50%",
              background: "var(--token-colors-accent-cyan)",
              animation: "pulse 1.5s infinite",
            }}
          />
        )}
      </div>

      {inputSummary ? (
        <div style={{ marginBottom: 8, fontSize: 11, color: "var(--token-colors-text-muted)" }}>
          {inputSummary}
        </div>
      ) : null}

      {resultMarkdown ? (
        <div style={{ fontSize: 12, color: "var(--token-colors-text-default)", maxHeight: 320, overflow: "auto" }}>
          <Markdown
            content={resultMarkdown}
            theme="dark"
            variant="compact"
            lineNumbers={false}
            copyButton={false}
          />
        </div>
      ) : null}

      {!resultMarkdown && liveUpdateMarkdown ? (
        <div style={{ fontSize: 12, color: "var(--token-colors-text-default)", marginTop: 8, maxHeight: 220, overflow: "auto" }}>
          <Markdown
            content={liveUpdateMarkdown}
            theme="dark"
            variant="compact"
            lineNumbers={false}
            copyButton={false}
          />
        </div>
      ) : null}
    </Card>
  );
}

export interface ToolReceiptGroupProps {
  receipts: ToolReceipt[];
  liveEvents?: RunEvent[];
  defaultExpanded?: boolean;
}

export function ToolReceiptGroup({ receipts, liveEvents, defaultExpanded = false }: ToolReceiptGroupProps) {
  // Merge live events into receipts for real-time display
  const liveTools = new Map<string, { status: string; preview?: string }>();

  if (liveEvents) {
    for (const event of liveEvents) {
      if (event.type === "tool_start" && event.tool_name) {
        liveTools.set(event.tool_name, {
          status: "running",
          preview: event.preview,
        });
      } else if (event.type === "tool_end" && event.tool_name) {
        liveTools.set(event.tool_name, {
          status: event.is_error ? "failed" : "completed",
          preview: event.preview,
        });
      }
    }
  }

  // Filter to show only completed or running receipts
  const visibleReceipts = receipts.filter(
    (receipt) => receipt.status === "completed" || receipt.status === "failed" || receipt.status === "running"
  );

  if (visibleReceipts.length === 0) return null;

  return (
    <div style={{ marginBottom: 12 }}>
      {visibleReceipts.map((receipt) => (
        <ToolReceiptBlock
          key={receipt.id}
          receipt={receipt}
          isLive={receipt.status === "running"}
          defaultExpanded={defaultExpanded}
        />
      ))}
    </div>
  );
}

function traceTextStatusVariant(status?: ChatTraceBlock["status"]): "info" | "warning" | "success" | "error" {
  if (status === "done") return "success";
  if (status === "error") return "error";
  return "warning";
}

function TraceTextBlock({ block }: { block: ChatTraceBlock }) {
  const title = block.kind === "reasoning" ? "Reasoning" : "Agent message";

  return (
    <Card
      variant="outlined"
      padding="sm"
      style={{
        border:
          block.kind === "reasoning"
            ? "1px solid var(--token-colors-accent-orange)"
            : "1px solid var(--token-colors-accent-cyan)",
        background:
          block.kind === "reasoning"
            ? "var(--token-colors-alpha-orange-_12)"
            : "var(--token-colors-alpha-blue-_15)",
        marginBottom: 8,
      }}
    >
      <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 8, flexWrap: "wrap" }}>
        <div style={{ fontSize: 12, fontWeight: 600, color: "var(--token-colors-text-default)" }}>{title}</div>
        <Badge size="sm" variant={traceTextStatusVariant(block.status)}>
          {block.status ?? "streaming"}
        </Badge>
      </div>
      <Markdown
        content={block.content || ""}
        theme="dark"
        variant="compact"
        lineNumbers={false}
        copyButton={false}
      />
    </Card>
  );
}

export interface AgentTraceTimelineProps {
  blocks: ChatTraceBlock[];
}

export function AgentTraceTimeline({ blocks }: AgentTraceTimelineProps) {
  if (blocks.length === 0) return null;

  return (
    <div style={{ display: "grid", gap: 8, marginBottom: 8 }}>
      {blocks.map((block) => {
        if (block.kind === "tool_call") {
          return (
            <ToolReceiptBlock
              key={block.id}
              receipt={{
                id: block.toolCallId ?? block.id,
                tool_name: block.toolName,
                status:
                  block.status === "done"
                    ? "completed"
                    : block.status === "error"
                      ? "failed"
                      : "running",
                input_preview: block.inputPreview,
                result_preview: block.outputPreview,
                updates: block.updates,
                is_error: block.isError,
              }}
              isLive={block.status === "streaming"}
              defaultExpanded
            />
          );
        }

        return <TraceTextBlock key={block.id} block={block} />;
      })}
    </div>
  );
}

export default ToolReceiptBlock;
