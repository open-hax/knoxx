import {
  asMarkdownPreview,
  normalizeToolPreview,
  structuredToMarkdown,
  toolInputSummary,
  toolOutputMarkdown,
  toolPreviewMarkdown,
} from "./chat-page/chat-page-derived";
import { Badge, Card, Markdown } from "@open-hax/uxx";
import type { CSSProperties } from "react";
import type { ChatTraceBlock, ToolReceipt, RunEvent } from "../lib/types";
import { MultimodalContent } from "./chat-page/MultimodalContent";

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
  const inputPreview = normalizeToolPreview(receipt.input_preview);
  const resultPreview = normalizeToolPreview(receipt.result_preview);
  const fullInput = (receipt as Record<string, unknown>).input;
  const fullResult = (receipt as Record<string, unknown>).result;
  const inputSummary = inputPreview ? toolInputSummary(inputPreview) : null;
  // IMPORTANT: do not truncate before JSON parsing, or we end up with invalid JSON
  // and fall back to raw JSON-like strings in the UI.
  const inputMarkdown = fullInput != null
    ? (typeof fullInput === "string" ? toolPreviewMarkdown(fullInput) : structuredToMarkdown(fullInput))
    : (inputPreview ? toolPreviewMarkdown(inputPreview) : "_(inputs unavailable)_");
  const resultMarkdown = fullResult != null
    ? (typeof fullResult === "string" ? toolOutputMarkdown(fullResult) : structuredToMarkdown(fullResult))
    : (resultPreview ? toolOutputMarkdown(resultPreview) : "");
  const contentParts = Array.isArray(receipt.contentParts) ? receipt.contentParts : [];
  const liveUpdateMarkdown = !resultMarkdown && receipt.updates && receipt.updates.length > 0
    ? toolOutputMarkdown(receipt.updates[receipt.updates.length - 1])
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

  const sectionStyle: CSSProperties = {
    border: "1px solid var(--token-colors-border-default)",
    borderRadius: 8,
    padding: 8,
    background: "var(--token-colors-background-surface)",
  };

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

      <div style={{ ...sectionStyle, marginBottom: 8 }}>
        <div style={{ fontSize: 11, fontWeight: 600, color: "var(--token-colors-text-muted)", marginBottom: 6 }}>
          Inputs
        </div>
        <div style={{ fontSize: 12, color: "var(--token-colors-text-default)", maxHeight: defaultExpanded ? 420 : 160, overflow: "auto" }}>
          <Markdown
            content={inputMarkdown}
            theme="dark"
            variant="compact"
            lineNumbers={false}
            copyButton={false}
          />
        </div>
      </div>

      {resultMarkdown ? (
        <div style={{ ...sectionStyle, marginBottom: 8 }}>
          <div style={{ fontSize: 11, fontWeight: 600, color: "var(--token-colors-text-muted)", marginBottom: 6 }}>
            Output
          </div>
          <div style={{ fontSize: 12, color: "var(--token-colors-text-default)", maxHeight: defaultExpanded ? 520 : 320, overflow: "auto" }}>
            <Markdown
              content={resultMarkdown}
              theme="dark"
              variant="compact"
              lineNumbers={false}
              copyButton={false}
            />
          </div>
        </div>
      ) : null}

      {!resultMarkdown && liveUpdateMarkdown ? (
        <div style={{ ...sectionStyle, marginBottom: 8 }}>
          <div style={{ fontSize: 11, fontWeight: 600, color: "var(--token-colors-text-muted)", marginBottom: 6 }}>
            Output (streaming)
          </div>
          <div style={{ fontSize: 12, color: "var(--token-colors-text-default)", maxHeight: defaultExpanded ? 420 : 220, overflow: "auto" }}>
            <Markdown
              content={liveUpdateMarkdown}
              theme="dark"
              variant="compact"
              lineNumbers={false}
              copyButton={false}
            />
          </div>
        </div>
      ) : null}

      {contentParts.length > 0 ? (
        <div style={{ ...sectionStyle, marginBottom: 8 }}>
          <div style={{ fontSize: 11, fontWeight: 600, color: "var(--token-colors-text-muted)", marginBottom: 6 }}>
            Attachments
          </div>
          <MultimodalContent
            parts={contentParts}
            maxPreviewWidth={defaultExpanded ? 480 : 320}
            maxPreviewHeight={defaultExpanded ? 360 : 240}
          />
        </div>
      ) : null}

      {isRunning && !resultMarkdown && !liveUpdateMarkdown ? (
        <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", marginTop: 4 }}>
          Waiting for tool output…
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
