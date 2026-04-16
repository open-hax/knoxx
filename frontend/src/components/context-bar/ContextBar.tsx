import type { ChangeEvent, MouseEvent as ReactMouseEvent, UIEvent, Ref } from "react";
import { Badge, Button, Card, Input } from "@open-hax/uxx";
import type { MemorySessionSummary } from "../../lib/types";
import { ContextBarExplorer } from "./ContextBarExplorer";
import type {
  BrowseEntry,
  BrowseResponse,
  PinnedContextItem,
  PreviewResponse,
  SemanticSearchMatch,
  WorkspaceJob,
} from "./types";
import { formatMaybeDate } from "./utils";

const VISIBILITY_OPTIONS = [
  { value: "all", label: "All" },
  { value: "internal", label: "🔒 Internal" },
  { value: "review", label: "👀 Review" },
  { value: "public", label: "🌐 Public" },
  { value: "archived", label: "📦 Archived" },
] as const;

const KIND_OPTIONS = [
  { value: "all", label: "All kinds" },
  { value: "docs", label: "Docs" },
  { value: "code", label: "Code" },
  { value: "config", label: "Config" },
  { value: "data", label: "Data" },
] as const;

type ContextBarProps = {
  // Layout props (required for resize)
  sidebarWidthPx: number;
  sidebarPaneSplitPct: number;
  sidebarSplitContainerRef: Ref<HTMLDivElement>;
  onHide: () => void;
  onStartSidebarPaneResize: (event: ReactMouseEvent<HTMLDivElement>) => void;
  onStartSidebarWidthResize: (event: ReactMouseEvent<HTMLDivElement>) => void;
  // Filters (shared across workplaces)
  visibilityFilter: string;
  kindFilter: string;
  statsTotal: number;
  statsByVisibility: Record<string, number>;
  onVisibilityFilterChange: (value: string) => void;
  onKindFilterChange: (value: string) => void;
  // Optional additional filters (CMS)
  sourceFilter?: string;
  domainFilter?: string;
  pathPrefixFilter?: string;
  onSourceFilterChange?: (value: string) => void;
  onDomainFilterChange?: (value: string) => void;
  onPathPrefixFilterChange?: (value: string) => void;
  // Actions
  onNewDocument?: () => void;
  // Chat workspace props (optional)
  currentPath?: string;
  currentParentPath?: string;
  browseData?: BrowseResponse | null;
  previewData?: PreviewResponse | null;
  loadingBrowse?: boolean;
  loadingPreview?: boolean;
  entryFilter?: string;
  semanticQuery?: string;
  semanticResults?: SemanticSearchMatch[];
  semanticProjects?: string[];
  semanticSearching?: boolean;
  semanticMode?: boolean;
  filteredEntries?: BrowseEntry[];
  activeEntryCount?: number;
  workspaceSourceId?: string | null;
  workspaceJob?: WorkspaceJob | null;
  workspaceProgressPercent?: number;
  pinnedContext?: PinnedContextItem[];
  recentSessions?: MemorySessionSummary[];
  recentSessionsHasMore?: boolean;
  recentSessionsTotal?: number;
  loadingRecentSessions?: boolean;
  loadingMoreRecentSessions?: boolean;
  loadingMemorySessionId?: string | null;
  sessionId?: string;
  conversationId?: string | null;
  onLoadDirectory?: (path?: string) => void | Promise<void>;
  onEntryFilterChange?: (value: string) => void;
  onSemanticQueryChange?: (value: string) => void;
  onSemanticSearch?: () => void | Promise<void>;
  onClearSemanticSearch?: () => void;
  onRefreshRecentSessions?: () => void | Promise<void>;
  onLoadMoreRecentSessions?: () => void | Promise<void>;
  onResumeMemorySession?: (sessionId: string) => void | Promise<void>;
  onPreviewFile?: (path: string) => void | Promise<void>;
  onOpenFile?: (entry: BrowseEntry) => void | Promise<void>;
  onPinSemanticResult?: (entry: SemanticSearchMatch) => void;
  onAppendToScratchpad?: (text: string, heading?: string) => void;
  onPinPreviewContext?: () => void;
  onOpenPreviewInCanvas?: () => void | Promise<void>;
  onOpenPinnedInCanvas?: (item: PinnedContextItem) => void | Promise<void>;
  onInsertPinnedIntoCanvas?: (item: PinnedContextItem) => void;
  onUnpinContextItem?: (path: string) => void;
};

export function ContextBar({
  sidebarWidthPx,
  sidebarPaneSplitPct,
  sidebarSplitContainerRef,
  onHide,
  onStartSidebarPaneResize,
  onStartSidebarWidthResize,
  // Filters
  visibilityFilter,
  kindFilter,
  statsTotal,
  statsByVisibility,
  onVisibilityFilterChange,
  onKindFilterChange,
  // Optional CMS filters
  sourceFilter,
  domainFilter,
  pathPrefixFilter,
  onSourceFilterChange,
  onDomainFilterChange,
  onPathPrefixFilterChange,
  // Actions
  onNewDocument,
  // Chat workspace props
  currentPath,
  currentParentPath,
  browseData,
  previewData,
  loadingBrowse,
  loadingPreview,
  entryFilter,
  semanticQuery,
  semanticResults,
  semanticProjects,
  semanticSearching,
  semanticMode,
  filteredEntries,
  activeEntryCount,
  workspaceSourceId,
  workspaceJob,
  workspaceProgressPercent,
  pinnedContext,
  recentSessions,
  recentSessionsHasMore,
  recentSessionsTotal,
  loadingRecentSessions,
  loadingMoreRecentSessions,
  loadingMemorySessionId,
  sessionId,
  conversationId,
  onLoadDirectory,
  onEntryFilterChange,
  onSemanticQueryChange,
  onSemanticSearch,
  onClearSemanticSearch,
  onRefreshRecentSessions,
  onLoadMoreRecentSessions,
  onResumeMemorySession,
  onPreviewFile,
  onOpenFile,
  onPinSemanticResult,
  onAppendToScratchpad,
  onPinPreviewContext,
  onOpenPreviewInCanvas,
  onOpenPinnedInCanvas,
  onInsertPinnedIntoCanvas,
  onUnpinContextItem,
}: ContextBarProps) {
  // Minimal status indicator - consolidated from both Chat and CMS
  const ingestionStatus = workspaceJob?.status;
  const statusColor = 
    ingestionStatus === "running" ? "var(--token-colors-accent-cyan)" :
    ingestionStatus === "completed" ? "var(--token-colors-accent-green)" :
    ingestionStatus === "failed" ? "var(--token-colors-accent-red)" :
    "var(--token-colors-text-muted)";

  // Determine if we're in chat mode (has sessions/files) or CMS mode
  const hasChatFeatures = recentSessions && recentSessions.length > 0 || browseData || filteredEntries && filteredEntries.length > 0;
  const hasFileExplorer = browseData || (filteredEntries && filteredEntries.length > 0);

  const handleRecentSessionsScroll = (event: UIEvent<HTMLDivElement>) => {
    if (!recentSessionsHasMore || loadingMoreRecentSessions || loadingRecentSessions || !onLoadMoreRecentSessions) return;
    const target = event.currentTarget;
    const remaining = target.scrollHeight - target.scrollTop - target.clientHeight;
    if (remaining <= 120) {
      void onLoadMoreRecentSessions();
    }
  };

  return (
    <>
      <Card
        variant="default"
        padding="none"
        style={{
          width: sidebarWidthPx,
          minWidth: 0,
          flexShrink: 0,
          display: "flex",
          flexDirection: "column",
          borderRight: "1px solid var(--token-colors-border-default)",
          minHeight: 0,
          overflow: "hidden",
        }}
      >
        {/* Header - minimal */}
        <div
          style={{
            padding: "6px 10px",
            borderBottom: "1px solid var(--token-colors-border-default)",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: 8,
          }}
        >
          <div style={{ display: "flex", alignItems: "center", gap: 8, minWidth: 0 }}>
            <span style={{ fontSize: 12, fontWeight: 600 }}>Explorer</span>
            {statsTotal > 0 && (
              <Badge size="sm" variant="default">{statsTotal}</Badge>
            )}
            {workspaceJob && ingestionStatus === "running" && (
              <Badge size="sm" variant="info">{workspaceProgressPercent}%</Badge>
            )}
          </div>
          <div style={{ display: "flex", gap: 4 }}>
            <Button variant="ghost" size="sm" onClick={onHide}>✕</Button>
          </div>
        </div>

        <div style={{ height: "100%", minHeight: 0, display: "flex", flexDirection: "column", overflow: "hidden" }}>
          <div
            style={{
              padding: 8,
              borderBottom: "1px solid var(--token-colors-border-default)",
              display: "flex",
              flexDirection: "column",
              gap: 6,
              flexShrink: 0,
            }}
          >
            {/* Search - compact (optional for file browsing) */}
            {onEntryFilterChange && (
              <Input
                value={entryFilter ?? ""}
                onChange={(event: ChangeEvent<HTMLInputElement>) => onEntryFilterChange(event.target.value)}
                placeholder="Filter..."
                size="sm"
              />
            )}
            
            {/* Semantic search - inline (optional for chat workspace) */}
            {onSemanticQueryChange && (
              <div style={{ display: "flex", gap: 4 }}>
                <div style={{ flex: 1 }}>
                  <Input
                    value={semanticQuery ?? ""}
                    onChange={(event: ChangeEvent<HTMLInputElement>) => onSemanticQueryChange(event.target.value)}
                    placeholder="Semantic search..."
                    size="sm"
                  />
                </div>
                {semanticMode ? (
                  <Button variant="ghost" size="sm" onClick={onClearSemanticSearch ?? (() => {})}>✕</Button>
                ) : (
                  <Button variant="secondary" size="sm" loading={semanticSearching} onClick={() => onSemanticSearch && void onSemanticSearch()}>
                    ⚲
                  </Button>
                )}
              </div>
            )}

            {/* CMS-specific filters (optional) */}
            {onSourceFilterChange && (
              <Input
                value={sourceFilter ?? ""}
                onChange={(event: ChangeEvent<HTMLInputElement>) => onSourceFilterChange(event.target.value)}
                placeholder="Source filter..."
                size="sm"
              />
            )}
            {onDomainFilterChange && (
              <Input
                value={domainFilter ?? ""}
                onChange={(event: ChangeEvent<HTMLInputElement>) => onDomainFilterChange(event.target.value)}
                placeholder="Domain filter..."
                size="sm"
              />
            )}
            {onPathPrefixFilterChange && (
              <Input
                value={pathPrefixFilter ?? ""}
                onChange={(event: ChangeEvent<HTMLInputElement>) => onPathPrefixFilterChange(event.target.value)}
                placeholder="Path prefix..."
                size="sm"
              />
            )}

            {/* Filters - compact inline */}
            <div style={{ display: "flex", gap: 4 }}>
              <select
                value={visibilityFilter}
                onChange={(e) => onVisibilityFilterChange(e.target.value)}
                style={{
                  flex: 1,
                  fontSize: 11,
                  padding: "4px 6px",
                  borderRadius: 4,
                  border: "1px solid var(--token-colors-border-default)",
                  background: "var(--token-colors-background-canvas)",
                  color: "var(--token-colors-text-default)",
                }}
              >
                {VISIBILITY_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}{statsByVisibility[opt.value] !== undefined ? ` (${statsByVisibility[opt.value]})` : ""}
                  </option>
                ))}
              </select>
              <select
                value={kindFilter}
                onChange={(e) => onKindFilterChange(e.target.value)}
                style={{
                  flex: 1,
                  fontSize: 11,
                  padding: "4px 6px",
                  borderRadius: 4,
                  border: "1px solid var(--token-colors-border-default)",
                  background: "var(--token-colors-background-canvas)",
                  color: "var(--token-colors-text-default)",
                }}
              >
                {KIND_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </div>

            {/* Action buttons */}
            {onNewDocument && (
              <Button variant="primary" size="sm" fullWidth onClick={onNewDocument}>
                + New Document
              </Button>
            )}

            {/* Minimal status line */}
            {workspaceJob && (
              <div style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 10, color: "var(--token-colors-text-muted)" }}>
                <span style={{ width: 6, height: 6, borderRadius: 999, background: statusColor, flexShrink: 0 }} />
                <span>
                  {ingestionStatus === "running" 
                    ? `${workspaceJob.processed_files}/${workspaceJob.total_files || 0} files`
                    : ingestionStatus === "completed"
                    ? `${workspaceJob.chunks_created} chunks indexed`
                    : ingestionStatus === "failed"
                    ? "Ingestion failed"
                    : "Ready"}
                </span>
              </div>
            )}
          </div>

          <div ref={sidebarSplitContainerRef} style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", overflow: "hidden" }}>
            {/* Sessions section - only for chat workspace */}
            {hasChatFeatures && recentSessions && (
              <div style={{ flex: `0 0 ${sidebarPaneSplitPct}%`, minHeight: 0, display: "flex", flexDirection: "column", overflow: "hidden" }}>
                {/* Recent Sessions - compact */}
                <div style={{ padding: "6px 8px", borderBottom: "1px solid var(--token-colors-border-default)", flex: 1, minHeight: 0, display: "flex", flexDirection: "column", overflow: "hidden" }}>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 6, flexShrink: 0 }}>
                    <span style={{ fontSize: 10, fontWeight: 600, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Sessions</span>
                    <div style={{ display: "flex", gap: 4, alignItems: "center" }}>
                      <Badge size="sm" variant="default">{(recentSessionsTotal ?? 0) > 0 ? `${recentSessions.length}/${recentSessionsTotal}` : recentSessions.length}</Badge>
                      {onRefreshRecentSessions && (
                        <Button variant="ghost" size="sm" loading={loadingRecentSessions} onClick={() => void onRefreshRecentSessions()}>
                          ↻
                        </Button>
                      )}
                    </div>
                  </div>
                  {recentSessions.length === 0 ? (
                    <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)" }}>No sessions</div>
                  ) : (
                    <div
                      onScroll={handleRecentSessionsScroll}
                      style={{
                        display: "flex",
                        flexDirection: "column",
                        gap: 4,
                        flex: 1,
                        minHeight: 0,
                        overflowY: "auto",
                        overflowX: "hidden",
                      }}
                    >
                      {recentSessions.map((item) => {
                        const isCurrent = (sessionId && item.active_session_id === sessionId)
                          || (conversationId && conversationId === item.session);
                        const isLive = Boolean(item.is_active);
                        const statusLabel = item.local_only && !item.event_count
                          ? "Draft"
                          : item.has_active_stream
                            ? "Live"
                            : item.active_status === "waiting_input"
                              ? "Waiting"
                              : isLive
                                ? "Active"
                                : "Idle";
                        const statusVariant = item.has_active_stream
                          ? "warning"
                          : isLive
                            ? "success"
                            : item.local_only
                              ? "default"
                              : "default";
                        return (
                          <button
                            key={item.session}
                            type="button"
                            onClick={() => onResumeMemorySession && void onResumeMemorySession(item.session)}
                            style={{
                              width: "100%",
                              textAlign: "left",
                              padding: "4px 6px",
                              border: "none",
                              borderRadius: 4,
                              background: isCurrent
                                ? "var(--token-colors-alpha-blue-_15)"
                                : isLive
                                  ? "var(--token-colors-alpha-green-_14)"
                                  : "transparent",
                              cursor: "pointer",
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "space-between",
                              gap: 4,
                            }}
                          >
                            <div style={{ minWidth: 0, flex: 1 }}>
                              <div style={{ fontSize: 11, fontWeight: 500, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                                {item.title || item.session.slice(0, 8)}
                              </div>
                            </div>
                            <div style={{ display: "flex", gap: 4, flexShrink: 0 }}>
                              {isCurrent && <Badge size="sm" variant="info">Current</Badge>}
                              <Badge size="sm" variant={statusVariant}>{statusLabel}</Badge>
                            </div>
                          </button>
                        );
                      })}
                      {loadingMoreRecentSessions && (
                        <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)", padding: 4 }}>Loading...</div>
                      )}
                    </div>
                  )}
                </div>

                {/* Pinned Context - compact */}
                {pinnedContext && pinnedContext.length > 0 && (
                  <div style={{ padding: "6px 8px", borderTop: "1px solid var(--token-colors-border-default)", maxHeight: 100, overflow: "hidden" }}>
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 4 }}>
                      <span style={{ fontSize: 10, fontWeight: 600, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Pinned</span>
                      <Badge size="sm" variant="default">{pinnedContext.length}</Badge>
                    </div>
                    <div style={{ display: "flex", flexWrap: "wrap", gap: 4 }}>
                      {pinnedContext.slice(0, 6).map((item) => (
                        <button
                          key={`${item.kind}:${item.path}`}
                          type="button"
                          onClick={() => onOpenPinnedInCanvas && void onOpenPinnedInCanvas(item)}
                          title={item.path}
                          style={{
                            padding: "2px 6px",
                            fontSize: 10,
                            borderRadius: 4,
                            border: "1px solid var(--token-colors-border-default)",
                            background: "var(--token-colors-alpha-bg-_08)",
                            cursor: "pointer",
                            maxWidth: 100,
                            overflow: "hidden",
                            textOverflow: "ellipsis",
                            whiteSpace: "nowrap",
                          }}
                        >
                          {item.title}
                        </button>
                      ))}
                      {pinnedContext.length > 6 && (
                        <span style={{ fontSize: 10, color: "var(--token-colors-text-muted)" }}>+{pinnedContext.length - 6}</span>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}

            {hasChatFeatures && <div role="separator" aria-orientation="horizontal" onMouseDown={onStartSidebarPaneResize} style={{ height: 4, cursor: "row-resize", display: "flex", alignItems: "center", justifyContent: "center", flexShrink: 0 }}>
              <div style={{ height: 2, width: 40, borderRadius: 999, background: "var(--token-colors-border-default)" }} />
            </div>}

            {/* File Explorer - only for chat workspace */}
            {hasFileExplorer && (
              <ContextBarExplorer
                semanticMode={semanticMode ?? false}
                activeEntryCount={activeEntryCount ?? 0}
                currentPath={currentPath ?? ""}
                currentParentPath={currentParentPath ?? ""}
                semanticProjects={semanticProjects ?? []}
                loadingBrowse={loadingBrowse ?? false}
                browseData={browseData ?? null}
                semanticResults={semanticResults ?? []}
                filteredEntries={filteredEntries ?? []}
                previewData={previewData ?? null}
                loadingPreview={loadingPreview ?? false}
                onPreviewFile={onPreviewFile}
                onOpenFile={onOpenFile}
                onLoadDirectory={onLoadDirectory ?? (() => {})}
                onPinSemanticResult={onPinSemanticResult ?? (() => {})}
                onAppendToScratchpad={onAppendToScratchpad ?? (() => {})}
                onPinPreviewContext={onPinPreviewContext ?? (() => {})}
                onOpenPreviewInCanvas={onOpenPreviewInCanvas ?? (() => {})}
              />
            )}
          </div>
        </div>
      </Card>
      {/* Resize handle */}
      <div
        role="separator"
        aria-orientation="vertical"
        aria-label="Resize context bar"
        onMouseDown={onStartSidebarWidthResize}
        style={{ width: 6, cursor: "col-resize", flexShrink: 0, display: "flex", alignItems: "stretch", justifyContent: "center", background: "transparent" }}
      >
        <div style={{ width: 1, background: "var(--token-colors-border-default)", margin: "4px 0" }} />
      </div>
    </>
  );
}
