import type { UIEvent } from "react";
import { Badge, Button, Card, Markdown } from "@open-hax/uxx";
import type { MemorySearchHit, MemorySessionSummary, RunDetail, RunEvent, ToolReceipt } from "../../lib/types";
import type { HydrationSource } from "./types";
import { asMarkdownPreview, formatMaybeDate, truncateText } from "./utils";

type ChatRuntimePanelProps = {
  wsStatus: "connected" | "closed" | "error" | "connecting";
  isRecovering: boolean;
  latestRun: RunDetail | null;
  isSending: boolean;
  selectedModel: string;
  conversationId: string | null;
  activeRunId: string | null;
  hydrationSources: HydrationSource[];
  runtimeEvents: RunEvent[];
  latestToolReceipts: ToolReceipt[];
  assistantSurfaceBackground: string;
  assistantSurfaceBorder: string;
  assistantSurfaceText: string;
  onOpenHydrationSource: (source: HydrationSource) => void | Promise<void>;
  onPinHydrationSource: (source: HydrationSource) => void;
  onAppendToScratchpad: (text: string, heading?: string) => void;
};

export function ChatRuntimePanel({
  wsStatus,
  isRecovering,
  latestRun,
  isSending,
  selectedModel,
  conversationId,
  activeRunId,
  hydrationSources,
  runtimeEvents,
  latestToolReceipts,
  assistantSurfaceBackground,
  assistantSurfaceBorder,
  assistantSurfaceText,
  onOpenHydrationSource,
  onPinHydrationSource,
  onAppendToScratchpad,
}: ChatRuntimePanelProps) {
  return (
    <Card variant="outlined" padding="sm" style={{ marginBottom: 12, background: "var(--token-colors-alpha-bg-_08)" }}>
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 10, flexWrap: "wrap" }}>
        <div>
          <div style={{ fontSize: 12, fontWeight: 700 }}>Agent Runtime</div>
          <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>
            Presence, witness thread, and receipt river for the active Knoxx turn.
          </div>
        </div>
        <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
          <Badge size="sm" variant={wsStatus === "connected" ? "success" : wsStatus === "connecting" ? "warning" : "error"}>
            {wsStatus}
          </Badge>
          {isRecovering && <Badge size="sm" variant="warning">recovering</Badge>}
          <Badge size="sm" variant={latestRun?.status === "completed" ? "success" : latestRun?.status === "failed" ? "error" : isSending ? "warning" : "default"}>
            {latestRun?.status ?? (isSending ? "running" : "idle")}
          </Badge>
          <Badge size="sm" variant="info">{selectedModel || "no-model"}</Badge>
        </div>
      </div>

      <div style={{ display: "grid", gap: 12, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
        <div style={{ display: "grid", gap: 6 }}>
          <div style={{ fontSize: 11, fontWeight: 700, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Presence</div>
          <div style={{ fontSize: 11, color: "var(--token-colors-text-subtle)" }}>Conversation</div>
          <div style={{ fontFamily: "var(--token-fontFamily-mono)", fontSize: 11, borderRadius: 8, border: "1px solid var(--token-colors-border-default)", padding: "6px 8px" }}>
            {conversationId ?? "new / not started"}
          </div>
          <div style={{ fontSize: 11, color: "var(--token-colors-text-subtle)" }}>Latest run</div>
          <div style={{ fontFamily: "var(--token-fontFamily-mono)", fontSize: 11, borderRadius: 8, border: "1px solid var(--token-colors-border-default)", padding: "6px 8px" }}>
            {latestRun?.run_id ?? activeRunId ?? "waiting for first turn"}
          </div>
          <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>
            {latestRun?.ttft_ms != null ? `TTFT ${Math.round(latestRun.ttft_ms)}ms` : "No token timing yet"}
            {latestRun?.total_time_ms != null ? ` • total ${Math.round(latestRun.total_time_ms)}ms` : ""}
          </div>
        </div>

        <div style={{ display: "grid", gap: 8 }}>
          <div style={{ fontSize: 11, fontWeight: 700, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Witness Thread</div>
          {hydrationSources.length === 0 ? (
            <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", lineHeight: 1.5 }}>
              Passive hydration has not surfaced corpus witnesses for the latest run yet.
            </div>
          ) : (
            hydrationSources.slice(0, 3).map((source) => (
              <div key={source.path} style={{ border: `1px solid ${assistantSurfaceBorder}`, borderRadius: 8, padding: 8, background: assistantSurfaceBackground, color: assistantSurfaceText }}>
                <div style={{ fontSize: 11, fontWeight: 600, color: assistantSurfaceText }}>{source.title}</div>
                <div style={{ fontSize: 10, color: assistantSurfaceText, opacity: 0.84, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{source.path}</div>
                {source.section ? (
                  <div style={{ marginTop: 6 }}>
                    <Markdown content={asMarkdownPreview(truncateText(source.section, 180))} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                  </div>
                ) : null}
                <div style={{ display: "flex", gap: 6, marginTop: 8, flexWrap: "wrap" }}>
                  <Button variant="ghost" size="sm" onClick={() => void onOpenHydrationSource(source)}>Open</Button>
                  <Button variant="ghost" size="sm" onClick={() => onPinHydrationSource(source)}>Pin</Button>
                  <Button variant="ghost" size="sm" onClick={() => onAppendToScratchpad(source.section || source.path, source.title)}>Insert</Button>
                </div>
              </div>
            ))
          )}
        </div>

        <div style={{ display: "grid", gap: 8 }}>
          <div style={{ fontSize: 11, fontWeight: 700, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Receipt River</div>
          {runtimeEvents.length === 0 ? (
            <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>No live runtime events yet.</div>
          ) : (
            runtimeEvents.slice(-6).reverse().map((event, index) => (
              <div key={`${event.type ?? "event"}:${event.at ?? ""}:${index}`} style={{ borderLeft: "2px solid var(--token-colors-accent-cyan)", padding: "8px 10px", borderRadius: 8, background: assistantSurfaceBackground, color: assistantSurfaceText }}>
                <div style={{ fontSize: 11, fontWeight: 600, color: assistantSurfaceText }}>{event.type ?? "event"}{event.tool_name ? ` • ${event.tool_name}` : ""}</div>
                <div style={{ fontSize: 10, color: assistantSurfaceText, opacity: 0.84 }}>{formatMaybeDate(event.at as string | undefined) ?? event.at ?? "just now"}</div>
                {typeof event.preview === "string" && event.preview.trim().length > 0 ? (
                  <div style={{ marginTop: 6 }}>
                    <Markdown content={asMarkdownPreview(truncateText(event.preview, 300))} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                  </div>
                ) : null}
              </div>
            ))
          )}
        </div>
      </div>

      {latestToolReceipts.length ? (
        <div style={{ marginTop: 12, paddingTop: 12, borderTop: "1px solid var(--token-colors-border-default)", display: "grid", gap: 8 }}>
          <div style={{ fontSize: 11, fontWeight: 700, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Tool Receipts</div>
          {latestToolReceipts.slice(0, 4).map((receipt) => (
            <div key={receipt.id} style={{ border: `1px solid ${assistantSurfaceBorder}`, borderRadius: 8, padding: 8, background: assistantSurfaceBackground, color: assistantSurfaceText }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
                <div style={{ fontSize: 11, fontWeight: 600, color: assistantSurfaceText }}>{receipt.tool_name ?? receipt.id}</div>
                <Badge size="sm" variant={receipt.status === "completed" ? "success" : receipt.status === "failed" ? "error" : "warning"}>{receipt.status ?? "running"}</Badge>
              </div>
              {((typeof (receipt as Record<string, unknown>).input === "string" && String((receipt as Record<string, unknown>).input).trim().length > 0)
                || (typeof receipt.input_preview === "string" && receipt.input_preview.trim().length > 0)) ? (
                <div style={{ marginTop: 6 }}>
                  <div style={{ fontSize: 10, fontWeight: 600, color: assistantSurfaceText, opacity: 0.84, marginBottom: 4 }}>input</div>
                  <Markdown
                    content={asMarkdownPreview(truncateText(String((receipt as Record<string, unknown>).input ?? receipt.input_preview ?? ""), 800))}
                    theme="dark"
                    variant="compact"
                    lineNumbers={false}
                    copyButton={false}
                  />
                </div>
              ) : null}
              {((typeof (receipt as Record<string, unknown>).result === "string" && String((receipt as Record<string, unknown>).result).trim().length > 0)
                || (typeof receipt.result_preview === "string" && receipt.result_preview.trim().length > 0)) ? (
                <div style={{ marginTop: 6 }}>
                  <div style={{ fontSize: 10, fontWeight: 600, color: assistantSurfaceText, opacity: 0.84, marginBottom: 4 }}>output</div>
                  <Markdown
                    content={asMarkdownPreview(truncateText(String((receipt as Record<string, unknown>).result ?? receipt.result_preview ?? ""), 800))}
                    theme="dark"
                    variant="compact"
                    lineNumbers={false}
                    copyButton={false}
                  />
                </div>
              ) : null}
            </div>
          ))}
        </div>
      ) : null}
    </Card>
  );
}

type RecentChatSessionsProps = {
  recentSessions: MemorySessionSummary[];
  recentSessionsHasMore: boolean;
  recentSessionsTotal: number;
  loadingRecentSessions: boolean;
  loadingMoreRecentSessions: boolean;
  loadingMemorySessionId: string | null;
  conversationId: string | null;
  onRefreshRecentSessions: () => void | Promise<void>;
  onLoadMoreRecentSessions: () => void | Promise<void>;
  onResumeMemorySession: (sessionId: string) => void | Promise<void>;
};

export function RecentChatSessions({
  recentSessions, recentSessionsHasMore, recentSessionsTotal,
  loadingRecentSessions, loadingMoreRecentSessions, loadingMemorySessionId,
  conversationId, onRefreshRecentSessions, onLoadMoreRecentSessions,
  onResumeMemorySession,
}: RecentChatSessionsProps) {
  const handleRecentSessionsScroll = (event: UIEvent<HTMLDivElement>) => loadMoreSessionsOnScroll(event,
    recentSessionsHasMore, loadingMoreRecentSessions, loadingRecentSessions, onLoadMoreRecentSessions);

  return (
    <Card variant="outlined" padding="sm" style={{ minHeight: 0, display: "flex", flexDirection: "column", overflow: "hidden" }}>
                      <div style={{ height: "100%", minHeight: 0, display: "flex", flexDirection: "column", overflow: "hidden" }}>
                        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 6, flexShrink: 0 }}>
                          <div style={{ fontSize: 11, fontWeight: 600 }}>Recent Sessions</div>
                          <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
                            <Badge size="sm" variant="default">{recentSessionsTotal > 0 ? `${recentSessions.length}/${recentSessionsTotal}` : recentSessions.length}</Badge>
                            <Button variant="ghost" size="sm" loading={loadingRecentSessions} onClick={() => void onRefreshRecentSessions()}>
                              Refresh
                            </Button>
                          </div>
                        </div>
                        {recentSessions.length === 0 ? (
                          <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", lineHeight: 1.5 }}>
                            No OpenPlanner-backed Knoxx sessions yet.
                          </div>
                        ) : (
                          <div
                            role="region"
                            aria-label="Recent chat sessions"
                            onScroll={handleRecentSessionsScroll}
                            style={{
                              display: "flex",
                              flexDirection: "column",
                              gap: 8,
                              flex: 1,
                              minHeight: 0,
                              overflowY: "auto",
                              overflowX: "hidden",
                              overscrollBehavior: "contain",
                              paddingRight: 4,
                            }}
                          >
                            {recentSessions.map((item) => {
                              const isSelected = conversationId === item.session;
                              const isLive = Boolean(item.is_active);
                              const statusLabel = item.has_active_stream
                                ? "Live"
                                : item.active_status === "waiting_input"
                                  ? "Waiting"
                                  : item.active_status === "running"
                                    ? "Active"
                                    : "Idle";
                              const statusVariant = item.has_active_stream
                                ? "warning"
                                : isLive
                                  ? "info"
                                  : "default";
                              return (
                                <div
                                  key={item.session}
                                  style={{
                                    minWidth: 0,
                                    maxWidth: "100%",
                                    flexShrink: 0,
                                    overflow: "hidden",
                                    border: `1px solid ${isSelected ? "var(--token-colors-accent-cyan)" : isLive ? "var(--token-colors-accent-green)" : "var(--token-colors-border-default)"}`,
                                    borderRadius: 8,
                                    padding: 10,
                                    background: isSelected
                                      ? "var(--token-colors-alpha-blue-_15)"
                                      : isLive
                                        ? "var(--token-colors-alpha-green-_14)"
                                        : "var(--token-colors-alpha-bg-_08)",
                                  }}
                                >
                                  <div style={{ display: "grid", gap: 8, minWidth: 0 }}>
                                    <div style={{ minWidth: 0 }}>
                                      <div style={{ fontSize: 11, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                                        {item.title || item.session}
                                      </div>
                                      <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                                        {item.title ? `${item.session} • ` : ""}
                                        {formatMaybeDate(item.last_ts) ?? item.last_ts ?? "unknown time"}
                                      </div>
                                    </div>
                                    <div style={{ display: "flex", gap: 6, alignItems: "center", flexWrap: "wrap" }}>
                                      {isSelected ? <Badge size="sm" variant="info">Open</Badge> : null}
                                      <Badge size="sm" variant={statusVariant}>{statusLabel}</Badge>
                                      <Badge size="sm" variant={isSelected ? "info" : "default"}>{item.event_count ?? 0} ev</Badge>
                                      <Button variant="ghost" size="sm" loading={loadingMemorySessionId === item.session} onClick={() => void onResumeMemorySession(item.session)}>
                                        {isSelected ? "Reload" : "Resume"}
                                      </Button>
                                    </div>
                                  </div>
                                </div>
                              );
                            })}
                            {loadingMoreRecentSessions ? (
                              <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", padding: "4px 0 8px" }}>
                                Loading more sessions…
                              </div>
                            ) : recentSessionsHasMore ? (
                              <Button variant="ghost" size="sm" onClick={() => void onLoadMoreRecentSessions()}>
                                Load more
                              </Button>
                            ) : recentSessions.length > 0 ? (
                              <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", padding: "4px 0 8px" }}>
                                End of recent sessions.
                              </div>
                            ) : null}
                          </div>
                        )}
                      </div>
                    </Card>
  );
}

export function groupSessionSearchHits(sessionSearchHits: MemorySearchHit[] | undefined, recentSessions: MemorySessionSummary[] | undefined) {
  const bySession = new Map<string, { session: string; snippet: string; hitCount: number; title?: string }>();
  for (const hit of sessionSearchHits ?? []) {
    const session = typeof hit.session === "string"
      ? hit.session
      : typeof hit.metadata?.session === "string"
        ? hit.metadata.session
        : "";
    if (!session) continue;
    const snippet = typeof hit.snippet === "string"
      ? hit.snippet
      : typeof hit.text === "string"
        ? hit.text
        : typeof hit.document === "string"
          ? hit.document
          : "";
    const title = recentSessions?.find((item) => item.session === session)?.title ?? undefined;
    const existing = bySession.get(session);
    if (existing) {
      existing.hitCount += 1;
      if (!existing.snippet && snippet) existing.snippet = snippet;
    } else {
      bySession.set(session, { session, snippet, hitCount: 1, title });
    }
  }
  return [...bySession.values()];
}

export function loadMoreSessionsOnScroll(
  event: UIEvent<HTMLDivElement>, hasMore: boolean | undefined, loadingMore: boolean | undefined,
  loading: boolean | undefined, loadMore: (() => void | Promise<void>) | undefined,
) {
  if (!hasMore || loadingMore || loading || !loadMore) return;
  const target = event.currentTarget;
  const remaining = target.scrollHeight - target.scrollTop - target.clientHeight;
  if (remaining <= 120) void loadMore();
}
