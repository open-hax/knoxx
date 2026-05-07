import { Badge, Button, Card, Markdown } from "@open-hax/uxx";
import { memo, useMemo, useState } from "react";
import { AgentTraceTimeline, ToolReceiptGroup } from "../ToolReceiptBlock";
import { MultimodalContent } from "./MultimodalContent";
import { extractEmbedsFromMarkdown } from "../../lib/mediaEmbeds";
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
import { request } from "../../lib/api/core";

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

const EMPTY_TOOL_RECEIPTS: ToolReceipt[] = [];
const EMPTY_TOOL_EVENTS: RunEvent[] = [];
const REACTION_EMOJIS = ["✅", "❌", "⭐", "👀", "😂"] as const;

export function deriveAssistantPresentation(message: ChatMessage): {
  effectiveStatus: ChatMessage["status"];
  visibleTraceBlocks: NonNullable<ChatMessage["traceBlocks"]>;
  showAssistantFinalCard: boolean;
} {
  const rawTraceBlocks = message.traceBlocks ?? [];
  const allTraceBlocksDone = rawTraceBlocks.length > 0
    && rawTraceBlocks.every((block) => block.status === "done");
  const effectiveStatus = message.role === "assistant"
    && message.status === "streaming"
    && allTraceBlocksDone
    && Boolean(message.content?.trim())
    ? "done"
    : message.status;
  const showAssistantFinalCard = message.role === "assistant"
    && effectiveStatus === "done"
    && rawTraceBlocks.length > 0
    && Boolean(message.content?.trim());

  return {
    effectiveStatus,
    visibleTraceBlocks: showAssistantFinalCard ? [] : rawTraceBlocks,
    showAssistantFinalCard,
  };
}

function openPlannerRecordIdForMessage(message: ChatMessage): string {
  if (message.runId && message.role === "assistant") return `${message.runId}:assistant`;
  if (message.runId && message.role === "user") return `${message.runId}:user`;
  return message.id;
}

async function labelOpenPlannerRecord(recordId: string, emoji: string): Promise<void> {
  await request(`/api/data/op/labels/records/${encodeURIComponent(recordId)}/reaction`, {
    method: "POST",
    body: JSON.stringify({ emoji, source: "knoxx-chat-ui" }),
  });
}

type ChatMessageCardProps = {
  message: ChatMessage;
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

const ChatMessageCard = memo(function ChatMessageCard({
  message,
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
}: ChatMessageCardProps) {
  const [reactionStatus, setReactionStatus] = useState<string | null>(null);
  const handleReaction = (msg: ChatMessage, emoji: string) => {
    const recordId = openPlannerRecordIdForMessage(msg);
    setReactionStatus(`${emoji} saving…`);
    void labelOpenPlannerRecord(recordId, emoji)
      .then(() => setReactionStatus(`${emoji} saved`))
      .catch((error) => setReactionStatus(`${emoji} failed: ${(error as Error).message}`));
  };

  const renderReactionActions = (msg: ChatMessage) => (
    <div
      role="group"
      aria-label="Label message output"
      style={{ display: "flex", alignItems: "center", gap: 6, flexWrap: "wrap", marginTop: 10 }}
    >
      {REACTION_EMOJIS.map((emoji) => (
        <Button
          key={emoji}
          variant="ghost"
          size="sm"
          title={emoji === "✅" ? "good output" : emoji === "❌" ? "bad output" : "reaction label"}
          onClick={() => handleReaction(msg, emoji)}
        >
          {emoji}
        </Button>
      ))}
      {reactionStatus ? <span style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>{reactionStatus}</span> : null}
    </div>
  );

  const renderAssistantActions = (msg: ChatMessage) => (
    <div
      role="group"
      aria-label="Assistant message actions"
      style={{
        display: "flex",
        alignItems: "center",
        gap: 8,
        flexWrap: "wrap",
        marginTop: 12,
        paddingTop: 10,
        borderTop: "1px solid var(--token-colors-border-subtle, var(--token-colors-border-default))",
      }}
    >
      <Button variant="ghost" size="sm" onClick={() => onOpenMessageInCanvas(msg)}>Open in Scratchpad</Button>
      {renderReactionActions(msg)}
    </div>
  );

  const { effectiveStatus, visibleTraceBlocks, showAssistantFinalCard } = useMemo(
    () => deriveAssistantPresentation(message),
    [message],
  );

  const { embeddedMarkdown, mergedContentParts } = useMemo(() => {
    const { markdown, contentParts: embeddedParts } = extractEmbedsFromMarkdown(message.content || "");
    return {
      embeddedMarkdown: markdown,
      mergedContentParts: (message.contentParts ?? []).concat(embeddedParts),
    };
  }, [message.content, message.contentParts]);

  return (
    <Card
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
          {effectiveStatus ? <Badge size="sm" variant={effectiveStatus === "done" ? "success" : effectiveStatus === "error" ? "error" : "warning"}>{effectiveStatus}</Badge> : null}
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
      </div>

      {message.role === "assistant" && visibleTraceBlocks.length > 0 ? (
        <AgentTraceTimeline blocks={visibleTraceBlocks} />
      ) : null}

      {message.role === "assistant" && visibleTraceBlocks.length === 0 && effectiveStatus === "streaming" && liveToolReceipts.length > 0 && (
        <ToolReceiptGroup receipts={liveToolReceipts} liveEvents={liveToolEvents} defaultExpanded={false} />
      )}

      {message.role === "assistant" && visibleTraceBlocks.length === 0 && effectiveStatus === "done" && message.runId && latestRun?.run_id === message.runId && latestToolReceipts.length > 0 && (
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
          <Markdown content={embeddedMarkdown} theme="dark" variant="full" />
          {mergedContentParts.length > 0 ? <MultimodalContent parts={mergedContentParts} /> : null}
          {renderAssistantActions(message)}
        </div>
      ) : message.role === "assistant" || message.role === "system" ? (
        <>
          <Markdown content={embeddedMarkdown} theme="dark" variant="full" />
          {mergedContentParts.length > 0 ? <MultimodalContent parts={mergedContentParts} /> : null}
          {message.role === "assistant" ? renderAssistantActions(message) : null}
        </>
      ) : (
        <>
          <div style={{ fontSize: 14, whiteSpace: "pre-wrap", lineHeight: 1.6 }}>{embeddedMarkdown}</div>
          {mergedContentParts.length > 0 ? <MultimodalContent parts={mergedContentParts} /> : null}
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
              <div
                key={row.id}
                style={{
                  border: `1px solid ${assistantSurfaceBorder}`,
                  borderRadius: 8,
                  padding: 10,
                  background: assistantSurfaceBackground,
                  color: assistantSurfaceText,
                }}
              >
                <div style={{ display: "flex", alignItems: "center", gap: 6, flexWrap: "wrap", marginBottom: 6 }}>
                  <Badge size="sm" variant="default">{row.project ?? "unknown-project"}</Badge>
                  <Badge size="sm" variant="default">{row.kind ?? "unknown-kind"}</Badge>
                </div>
                <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 4 }}>
                  <div style={{ fontSize: 12, fontWeight: 600, color: "var(--token-colors-text-default)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                    {contextPath(row)}
                  </div>
                  <Button variant="ghost" size="sm" onClick={() => onPinMessageContext(row)}>Pin</Button>
                </div>
                <Markdown
                  content={asMarkdownPreview(row.snippet ?? row.text ?? "")}
                  theme="dark"
                  variant="compact"
                  lineNumbers={false}
                  copyButton={false}
                />
              </div>
            ))}
          </div>
        </details>
      ) : null}
    </Card>
  );
});

ChatMessageCard.displayName = "ChatMessageCard";

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
      {messages.map((message) => {
        const isStreamingAssistant = message.role === "assistant" && message.status === "streaming";
        return (
          <ChatMessageCard
            key={message.id}
            message={message}
            latestRun={latestRun}
            latestToolReceipts={latestToolReceipts}
            liveToolReceipts={isStreamingAssistant ? liveToolReceipts : EMPTY_TOOL_RECEIPTS}
            liveToolEvents={isStreamingAssistant ? liveToolEvents : EMPTY_TOOL_EVENTS}
            assistantSurfaceBackground={assistantSurfaceBackground}
            assistantSurfaceBorder={assistantSurfaceBorder}
            assistantSurfaceText={assistantSurfaceText}
            onOpenMessageInCanvas={onOpenMessageInCanvas}
            onOpenSourceInPreview={onOpenSourceInPreview}
            onPinAssistantSource={onPinAssistantSource}
            onAppendToScratchpad={onAppendToScratchpad}
            onPinMessageContext={onPinMessageContext}
          />
        );
      })}
    </div>
  );
}
