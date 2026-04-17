import { Badge, Button, Card, Markdown } from "@open-hax/uxx";
import { AgentTraceTimeline, ToolReceiptGroup } from "../ToolReceiptBlock";
import { MultimodalContent } from "./MultimodalContent";
import { VoiceReplyButton } from "./VoiceReplyButton";
import type {
  AgentSource,
  ChatMessage,
  ContentPart,
  GroundedContextRow,
  RunDetail,
  RunEvent,
  ToolReceipt,
} from "../../lib/types";
import { asMarkdownPreview, contextPath, fileNameFromPath, sourceUrlToPath } from "./utils";

type ChatMessageListProps = {
  messages: ChatMessage[];
  latestRun: RunDetail | null;
  latestToolReceipts: ToolReceipt[];
  liveToolReceipts: ToolReceipt[];
  liveToolEvents: RunEvent[];
  assistantSurfaceBackground: string;
  assistantSurfaceBorder: string;
  assistantSurfaceText: string;
  onSend: (text: string) => void;
  voiceReplyDisabled?: boolean;
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
  onSend,
  voiceReplyDisabled,
  onOpenMessageInCanvas,
  onOpenSourceInPreview,
  onPinAssistantSource,
  onAppendToScratchpad,
  onPinMessageContext,
}: ChatMessageListProps) {
  const latestAssistantMessageId = [...messages]
    .reverse()
    .find((message) => message.role === "assistant" && message.status === "done" && Boolean(message.content?.trim()))?.id;

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 12, width: "100%" }}>
      {messages.map((message) => {
        const rawTraceBlocks = message.traceBlocks ?? [];
        const visibleTraceBlocks = message.role === "assistant"
          && message.status === "done"
          && rawTraceBlocks.length > 0
          && rawTraceBlocks[rawTraceBlocks.length - 1]?.kind === "agent_message"
          && (rawTraceBlocks[rawTraceBlocks.length - 1]?.content ?? "").trim() === (message.content ?? "").trim()
          ? rawTraceBlocks.slice(0, -1)
          : rawTraceBlocks;
        const showAssistantFinalCard = message.role === "assistant"
          && message.status === "done"
          && rawTraceBlocks.length > 0
          && Boolean(message.content?.trim());

        const showVoiceReply = message.id === latestAssistantMessageId;

        return <Card
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
              <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap" }}>
                {showVoiceReply ? (
                  <VoiceReplyButton
                    disabled={voiceReplyDisabled}
                    onTranscript={(text) => onSend(text)}
                  />
                ) : null}
                <Button variant="ghost" size="sm" onClick={() => onOpenMessageInCanvas(message)}>Open in Scratchpad</Button>
              </div>
            ) : null}
          </div>
          {message.role === "assistant" && visibleTraceBlocks.length > 0 ? (
            <AgentTraceTimeline blocks={visibleTraceBlocks} />
          ) : null}
          {message.role === "assistant" && visibleTraceBlocks.length === 0 && message.status === "streaming" && liveToolReceipts.length > 0 && (
            <ToolReceiptGroup receipts={liveToolReceipts} liveEvents={liveToolEvents} defaultExpanded={false} />
          )}
          {message.role === "assistant" && visibleTraceBlocks.length === 0 && message.status === "done" && message.runId && latestRun?.run_id === message.runId && latestToolReceipts.length > 0 && (
            <details style={{ marginTop: 8, marginBottom: 8 }} open={false}>
              <summary style={{ cursor: "pointer", fontSize: 11, fontWeight: 600, color: "var(--token-colors-text-muted)" }}>
                Tool calls ({latestToolReceipts.length})
              </summary>
              <ToolReceiptGroup receipts={latestToolReceipts} defaultExpanded={false} />
            </details>
          )}
          {showAssistantFinalCard ? (
            <div
              style={{
                border: "1px solid var(--token-colors-border-strong, var(--token-colors-border-default))",
                background: "var(--token-colors-background-canvas)",
                borderRadius: 10,
                padding: 12,
                marginTop: 4,
              }}
            >
              <Markdown content={message.content || ""} theme="dark" variant="full" />
              {/* Multimodal content for assistant messages */}
              {message.contentParts && message.contentParts.length > 0 && (
                <MultimodalContent parts={message.contentParts} />
              )}
            </div>
          ) : message.role === "assistant" || message.role === "system" ? (
            <>
              <Markdown content={message.content || ""} theme="dark" variant="full" />
              {/* Multimodal content for assistant/system messages */}
              {message.contentParts && message.contentParts.length > 0 && (
                <MultimodalContent parts={message.contentParts} />
              )}
            </>
          ) : (
            <>
              <div style={{ fontSize: 14, whiteSpace: "pre-wrap", lineHeight: 1.6 }}>{message.content}</div>
              {/* Multimodal content for user messages */}
              {message.contentParts && message.contentParts.length > 0 && (
                <MultimodalContent parts={message.contentParts} />
              )}
              {/* Legacy attachments support */}
              {message.attachments && message.attachments.length > 0 && (
                <MultimodalContent
                  parts={message.attachments.map((a) => ({
                    type: a.type,
                    url: a.url,
                    data: a.data,
                    mimeType: a.mimeType,
                    filename: a.filename,
                    size: a.size,
                  }))}
                />
              )}
            </>
          )}
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
        </Card>;
      })}
    </div>
  );
}
