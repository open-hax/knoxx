import { Badge, Button, Card, Markdown } from "@open-hax/uxx";
import { ToolReceiptGroup } from "../ToolReceiptBlock";
import type {
  AgentSource,
  ChatMessage,
  ChatTraceBlock,
  GroundedContextRow,
  RunDetail,
  RunEvent,
  ToolReceipt,
} from "../../lib/types";
import { asMarkdownPreview, contextPath, fileNameFromPath, sourceUrlToPath } from "./utils";

function extractToolCallBlocks(blocks: ChatTraceBlock[]): ChatTraceBlock[] {
  return blocks.filter((b) => b.kind === "tool_call");
}

function hasVisibleToolCalls(blocks: ChatTraceBlock[]): boolean {
  return blocks.some((b) => b.kind === "tool_call");
}

type ChatMessageListProps = {
  messages: ChatMessage[];
  latestRun: RunDetail | null;
  latestToolReceipts: ToolReceipt[];
  liveToolReceipts: ToolReceipt[];
  liveToolEvents: RunEvent[];
  assistantSurfaceBackground: string;
  assistantSurfaceBorder: string;
  assistantSurfaceText: string;
  onOpenMessageInCanvas: (message: ChatMessage) => void;
  onOpenSourceInPreview: (source: AgentSource) => void | Promise<void>;
  onPinAssistantSource: (source: AgentSource) => void;
  onAppendToScratchpad: (text: string, heading?: string) => void;
  onPinMessageContext: (row: GroundedContextRow) => void;
};

export function ChatMessageList({
  messages,
  latestRun,
  latestToolReceipts,
  liveToolReceipts,
  liveToolEvents,
  assistantSurfaceBackground,
  assistantSurfaceBorder,
  assistantSurfaceText,
  onOpenMessageInCanvas,
  onOpenSourceInPreview,
  onPinAssistantSource,
  onAppendToScratchpad,
  onPinMessageContext,
}: ChatMessageListProps) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 12, width: "100%" }}>
      {messages.map((message) => (
        <Card
          key={message.id}
          variant="outlined"
          padding="sm"
          style={{
            borderColor:
              message.role === "user"
                ? "var(--token-colors-alpha-green-_30)"
                : message.role === "system"
                  ? "var(--token-colors-alpha-cyan-_30)"
                  : "var(--token-colors-border-default)",
            background:
              message.role === "user"
                ? "var(--token-colors-alpha-green-_08)"
                : message.role === "system"
                  ? "var(--token-colors-alpha-cyan-_08)"
                  : "var(--token-colors-background-surface)",
            alignSelf: message.role === "user" ? "flex-end" : "flex-start",
            maxWidth: message.role === "user" ? "80%" : "100%",
          }}
        >
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 6 }}>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <span style={{ fontSize: 11, fontWeight: 600, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>{message.role}</span>
              {message.model ? <Badge size="sm" variant="info">{message.model}</Badge> : null}
              {message.status ? <Badge size="sm" variant={message.status === "done" ? "success" : message.status === "error" ? "error" : "warning"}>{message.status}</Badge> : null}
              {message.runId ? <Badge size="sm" variant="default">{message.runId.slice(0, 8)}</Badge> : null}
              {message.role === "assistant" ? (
                <Badge size="sm" variant={(message.sources?.length || message.contextRows?.length) ? "success" : "warning"}>
                  {message.sources?.length
                    ? `${message.sources.length} source(s)`
                    : message.contextRows?.length
                      ? `${message.contextRows.length} context row(s)`
                      : "No grounding metadata"}
                </Badge>
              ) : null}
            </div>
            {message.role === "assistant" ? (
              <Button variant="ghost" size="sm" onClick={() => onOpenMessageInCanvas(message)}>Open in Scratchpad</Button>
            ) : null}
          </div>
          {/* Always render assistant/system message content as a full dark card — unified whether live or resumed */}
          {message.role === "assistant" || message.role === "system" ? (
            <Markdown content={message.content || ""} theme="dark" variant="full" />
          ) : (
            <div style={{ fontSize: 14, whiteSpace: "pre-wrap", lineHeight: 1.6 }}>{message.content}</div>
          )}
          {/* Tool calls from traceBlocks (live session): collapsed by default, same shape as done sessions */}
          {message.role === "assistant" && hasVisibleToolCalls(message.traceBlocks ?? []) ? (
            <details style={{ marginTop: 8, marginBottom: 8 }} open={false}>
              <summary style={{ cursor: "pointer", fontSize: 11, fontWeight: 600, color: "var(--token-colors-text-muted)" }}>
                Tool calls ({extractToolCallBlocks(message.traceBlocks ?? []).length})
              </summary>
              <ToolReceiptGroup
                receipts={(message.traceBlocks ?? [])
                  .filter((b) => b.kind === "tool_call")
                  .map((b) => ({
                    id: b.toolCallId ?? b.id,
                    tool_name: b.toolName,
                    status: b.status === "done" ? "completed" : b.status === "error" ? "failed" : "running",
                    input_preview: b.inputPreview,
                    result_preview: b.outputPreview,
                    updates: b.updates,
                    is_error: b.isError,
                  }))}
                defaultExpanded={false}
              />
            </details>
          ) : null}
          {/* Live tool receipts (streaming, before traceBlocks are finalized) */}
          {message.role === "assistant" && !hasVisibleToolCalls(message.traceBlocks ?? []) && message.status === "streaming" && liveToolReceipts.length > 0 ? (
            <ToolReceiptGroup receipts={liveToolReceipts} liveEvents={liveToolEvents} defaultExpanded={false} />
          ) : null}
          {/* Completed run tool receipts (done session, no traceBlocks on message) */}
          {message.role === "assistant" && !hasVisibleToolCalls(message.traceBlocks ?? []) && message.status === "done" && message.runId && latestRun?.run_id === message.runId && latestToolReceipts.length > 0 ? (
            <details style={{ marginTop: 8, marginBottom: 8 }} open={false}>
              <summary style={{ cursor: "pointer", fontSize: 11, fontWeight: 600, color: "var(--token-colors-text-muted)" }}>
                Tool calls ({latestToolReceipts.length})
              </summary>
              <ToolReceiptGroup receipts={latestToolReceipts} defaultExpanded={false} />
            </details>
          ) : null}
          {message.sources?.length ? (
            <details style={{ marginTop: 12 }} open>
              <summary style={{ cursor: "pointer", fontSize: 12, fontWeight: 600, color: "var(--token-colors-text-muted)" }}>
                Grounding sources ({message.sources.length})
              </summary>
              <div style={{ display: "grid", gap: 8, marginTop: 10 }}>
                {message.sources.map((source, idx) => {
                  const path = sourceUrlToPath(source.url);
                  return (
                    <div key={`${source.title}:${idx}`} style={{ border: `1px solid ${assistantSurfaceBorder}`, borderRadius: 8, padding: 10, background: assistantSurfaceBackground, color: assistantSurfaceText }}>
                      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 6 }}>
                        <div style={{ minWidth: 0 }}>
                          <div style={{ fontSize: 12, fontWeight: 600, color: "var(--token-colors-text-default)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                            {source.title || fileNameFromPath(path)}
                          </div>
                          <div style={{ fontSize: 10, color: assistantSurfaceText, opacity: 0.84, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{path || source.url}</div>
                        </div>
                        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                          {path ? <Button variant="ghost" size="sm" onClick={() => void onOpenSourceInPreview(source)}>Open</Button> : null}
                          <Button variant="ghost" size="sm" onClick={() => onPinAssistantSource(source)}>Pin</Button>
                          <Button variant="ghost" size="sm" onClick={() => onAppendToScratchpad(source.section || path || source.title, source.title)}>Insert</Button>
                        </div>
                      </div>
                      {source.section ? <Markdown content={asMarkdownPreview(source.section)} theme="dark" variant="compact" lineNumbers={false} copyButton={false} /> : null}
                    </div>
                  );
                })}
              </div>
            </details>
          ) : null}
          {message.contextRows?.length ? (
            <details style={{ marginTop: 12 }}>
              <summary style={{ cursor: "pointer", fontSize: 12, fontWeight: 600, color: "var(--token-colors-text-muted)" }}>
                Auto-injected context ({message.contextRows.length})
              </summary>
              <div style={{ display: "grid", gap: 8, marginTop: 10 }}>
                {message.contextRows.map((row) => (
                  <div key={row.id} style={{ border: `1px solid ${assistantSurfaceBorder}`, borderRadius: 8, padding: 10, background: assistantSurfaceBackground, color: assistantSurfaceText }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 6, flexWrap: "wrap", marginBottom: 6 }}>
                      <Badge size="sm" variant="default">{row.project ?? "unknown-project"}</Badge>
                      <Badge size="sm" variant="default">{row.kind ?? "unknown-kind"}</Badge>
                    </div>
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 4 }}>
                      <div style={{ fontSize: 12, fontWeight: 600, color: "var(--token-colors-text-default)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{contextPath(row)}</div>
                      <Button variant="ghost" size="sm" onClick={() => onPinMessageContext(row)}>Pin</Button>
                    </div>
                    <Markdown content={asMarkdownPreview(row.snippet ?? row.text ?? "")} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                  </div>
                ))}
              </div>
            </details>
          ) : null}
        </Card>
      ))}
    </div>
  );
}
