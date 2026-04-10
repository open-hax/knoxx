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
          Tool: {toolName}
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

      {receipt.input_preview && (
        <details open={defaultExpanded || isRunning} style={{ marginBottom: 8 }}>
          <summary style={{ cursor: "pointer", fontSize: 11, fontWeight: 600, color: "var(--token-colors-text-muted)", marginBottom: 6 }}>
            Input
          </summary>
          <div style={{ fontSize: 11, color: "var(--token-colors-text-subtle)", maxHeight: 200, overflow: "auto" }}>
            <Markdown
              content={asMarkdownPreview(truncateText(receipt.input_preview, 500))}
              theme="dark"
              variant="compact"
              lineNumbers={false}
              copyButton={false}
            />
          </div>
        </details>
      )}

      {receipt.result_preview && !isRunning && (
        <details open={defaultExpanded} style={{ marginBottom: 8 }}>
          <summary style={{ cursor: "pointer", fontSize: 11, fontWeight: 600, color: "var(--token-colors-text-muted)", marginBottom: 6 }}>
            Output
          </summary>
          <div style={{ fontSize: 11, color: "var(--token-colors-text-subtle)", maxHeight: 300, overflow: "auto" }}>
            <Markdown
              content={asMarkdownPreview(truncateText(receipt.result_preview, 800))}
              theme="dark"
              variant="compact"
              lineNumbers={false}
              copyButton={false}
            />
          </div>
        </details>
      )}

      {receipt.updates && receipt.updates.length > 0 && (
        <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)", marginTop: 8 }}>
          <div style={{ fontWeight: 600, marginBottom: 4 }}>Live updates:</div>
          {receipt.updates.slice(-3).map((update, idx) => (
            <div key={idx} style={{ opacity: 0.7, marginBottom: 2 }}>
              {truncateText(update, 120)}
            </div>
          ))}
        </div>
      )}
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

export function traceTextStatusVariant(status?: ChatTraceBlock["status"]): "info" | "warning" | "success" | "error" {
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
