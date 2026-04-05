import type { ChangeEvent, MouseEvent as ReactMouseEvent, Ref } from "react";
import { Badge, Button, Card, Input } from "@open-hax/uxx";
import type { MemorySessionSummary } from "../../lib/types";
import { ChatWorkspaceSidebarExplorer } from "./ChatWorkspaceSidebarExplorer";
import type {
  BrowseEntry,
  BrowseResponse,
  PinnedContextItem,
  PreviewResponse,
  SemanticSearchMatch,
  WorkspaceJob,
} from "./types";
import { formatMaybeDate } from "./utils";

const QUICK_ROOTS = [
  { label: "docs", path: "docs" },
  { label: "specs", path: "specs" },
  { label: "notes", path: "notes" },
  { label: "packages", path: "packages" },
  { label: "services", path: "services" },
  { label: "orgs", path: "orgs" },
  { label: "data", path: "data" },
];

type ChatWorkspaceSidebarProps = {
  sidebarWidthPx: number;
  sidebarPaneSplitPct: number;
  sidebarSplitContainerRef: Ref<HTMLDivElement>;
  currentPath: string;
  currentParentPath: string;
  browseData: BrowseResponse | null;
  previewData: PreviewResponse | null;
  loadingBrowse: boolean;
  loadingPreview: boolean;
  entryFilter: string;
  semanticQuery: string;
  semanticResults: SemanticSearchMatch[];
  semanticProjects: string[];
  semanticSearching: boolean;
  semanticMode: boolean;
  filteredEntries: BrowseEntry[];
  activeEntryCount: number;
  syncingWorkspace: boolean;
  workspaceSourceId: string | null;
  workspaceJob: WorkspaceJob | null;
  workspaceProgressPercent: number;
  pinnedContext: PinnedContextItem[];
  recentSessions: MemorySessionSummary[];
  loadingRecentSessions: boolean;
  loadingMemorySessionId: string | null;
  conversationId: string | null;
  onHide: () => void;
  onLoadDirectory: (path?: string) => void | Promise<void>;
  onEntryFilterChange: (value: string) => void;
  onSemanticQueryChange: (value: string) => void;
  onSemanticSearch: () => void | Promise<void>;
  onClearSemanticSearch: () => void;
  onEnsureWorkspaceSync: () => void | Promise<void>;
  onRefreshRecentSessions: () => void | Promise<void>;
  onResumeMemorySession: (sessionId: string) => void | Promise<void>;
  onPreviewFile: (path: string) => void | Promise<void>;
  onPinSemanticResult: (entry: SemanticSearchMatch) => void;
  onAppendToScratchpad: (text: string, heading?: string) => void;
  onPinPreviewContext: () => void;
  onOpenPreviewInCanvas: () => void | Promise<void>;
  onOpenPinnedInCanvas: (item: PinnedContextItem) => void | Promise<void>;
  onInsertPinnedIntoCanvas: (item: PinnedContextItem) => void;
  onUnpinContextItem: (path: string) => void;
  onStartSidebarPaneResize: (event: ReactMouseEvent<HTMLDivElement>) => void;
  onStartSidebarWidthResize: (event: ReactMouseEvent<HTMLDivElement>) => void;
};

export function ChatWorkspaceSidebar({
  sidebarWidthPx,
  sidebarPaneSplitPct,
  sidebarSplitContainerRef,
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
  syncingWorkspace,
  workspaceSourceId,
  workspaceJob,
  workspaceProgressPercent,
  pinnedContext,
  recentSessions,
  loadingRecentSessions,
  loadingMemorySessionId,
  conversationId,
  onHide,
  onLoadDirectory,
  onEntryFilterChange,
  onSemanticQueryChange,
  onSemanticSearch,
  onClearSemanticSearch,
  onEnsureWorkspaceSync,
  onRefreshRecentSessions,
  onResumeMemorySession,
  onPreviewFile,
  onPinSemanticResult,
  onAppendToScratchpad,
  onPinPreviewContext,
  onOpenPreviewInCanvas,
  onOpenPinnedInCanvas,
  onInsertPinnedIntoCanvas,
  onUnpinContextItem,
  onStartSidebarPaneResize,
  onStartSidebarWidthResize,
}: ChatWorkspaceSidebarProps) {
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
        <div
          style={{
            padding: 10,
            borderBottom: "1px solid var(--token-colors-border-default)",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: 8,
          }}
        >
          <div style={{ minWidth: 0 }}>
            <div style={{ fontSize: 13, fontWeight: 600 }}>Context Bar</div>
            <div
              style={{
                fontSize: 11,
                color: "var(--token-colors-text-muted)",
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
              }}
            >
              Explorer root: /{currentPath || "docs"}
            </div>
          </div>
          <div style={{ display: "flex", gap: 6 }}>
            <Button variant="ghost" size="sm" disabled={!currentPath} onClick={() => void onLoadDirectory(currentParentPath)}>
              Up
            </Button>
            <Button variant="ghost" size="sm" onClick={onHide}>Hide</Button>
          </div>
        </div>

        <div style={{ height: "100%", minHeight: 0, display: "flex", flexDirection: "column", overflow: "hidden" }}>
          <div
            style={{
              padding: 10,
              borderBottom: "1px solid var(--token-colors-border-default)",
              display: "flex",
              flexDirection: "column",
              gap: 8,
              flex: 1,
              minHeight: 0,
            }}
          >
            <div style={{ display: "grid", gap: 6 }}>
              <Input
                value={entryFilter}
                onChange={(event: ChangeEvent<HTMLInputElement>) => onEntryFilterChange(event.target.value)}
                placeholder="Filter current list..."
                size="sm"
              />
              <div style={{ display: "flex", gap: 6 }}>
                <Input
                  value={semanticQuery}
                  onChange={(event: ChangeEvent<HTMLInputElement>) => onSemanticQueryChange(event.target.value)}
                  placeholder="Semantic search in current path..."
                  size="sm"
                />
                <Button variant="secondary" size="sm" loading={semanticSearching} onClick={() => void onSemanticSearch()}>
                  Search
                </Button>
                {semanticMode ? (
                  <Button variant="ghost" size="sm" onClick={onClearSemanticSearch}>
                    Clear
                  </Button>
                ) : null}
              </div>
            </div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
              {QUICK_ROOTS.map((root) => (
                <Button key={root.path} size="sm" variant="ghost" onClick={() => void onLoadDirectory(root.path)}>
                  {root.label}
                </Button>
              ))}
            </div>
            <Card variant="outlined" padding="sm">
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 6 }}>
                <div style={{ fontSize: 11, fontWeight: 600 }}>Workspace Ingestion</div>
                <Badge
                  size="sm"
                  variant={
                    workspaceJob?.status === "running"
                      ? "warning"
                      : workspaceJob?.status === "completed"
                        ? "success"
                        : workspaceJob?.status === "failed"
                          ? "error"
                          : "default"
                  }
                >
                  {workspaceJob?.status ?? "idle"}
                </Badge>
              </div>
              <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", display: "grid", gap: 4 }}>
                <div>{workspaceSourceId ? `source ${workspaceSourceId.slice(0, 8)}` : "No workspace source yet"}</div>
                {workspaceJob ? (
                  <>
                    <div>
                      {workspaceJob.processed_files}/{workspaceJob.total_files || 0} processed, {workspaceJob.failed_files} failed, {workspaceJob.chunks_created} chunks
                    </div>
                    <div>
                      {workspaceJob.status === "running" || workspaceJob.status === "pending"
                        ? `Started ${formatMaybeDate(workspaceJob.started_at || workspaceJob.created_at) ?? "just now"}`
                        : workspaceJob.completed_at
                          ? `Finished ${formatMaybeDate(workspaceJob.completed_at)}`
                          : `Created ${formatMaybeDate(workspaceJob.created_at)}`}
                    </div>
                    {workspaceJob.error_message ? (
                      <div
                        style={{
                          color: "var(--token-colors-accent-red)",
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          whiteSpace: "nowrap",
                        }}
                        title={workspaceJob.error_message}
                      >
                        {workspaceJob.error_message}
                      </div>
                    ) : null}
                    <div style={{ height: 6, borderRadius: 999, background: "var(--token-colors-border-default)", overflow: "hidden" }}>
                      <div
                        style={{
                          width: `${workspaceProgressPercent}%`,
                          height: "100%",
                          background:
                            workspaceJob.status === "failed"
                              ? "var(--token-colors-accent-red)"
                              : workspaceJob.status === "completed"
                                ? "var(--token-colors-accent-green)"
                                : "var(--token-colors-accent-cyan)",
                        }}
                      />
                    </div>
                  </>
                ) : null}
              </div>
            </Card>
            <Button variant="secondary" size="sm" loading={syncingWorkspace} onClick={() => void onEnsureWorkspaceSync()}>
              Sync Devel Workspace
            </Button>

            <div ref={sidebarSplitContainerRef} style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", gap: 8, overflow: "hidden" }}>
              <div
                style={{
                  flex: `0 0 ${sidebarPaneSplitPct}%`,
                  minHeight: 0,
                  display: "grid",
                  gap: 8,
                  gridTemplateRows: pinnedContext.length > 0 ? "minmax(0, 1fr) minmax(0, 1fr)" : "minmax(0, 1fr)",
                }}
              >
                <Card variant="outlined" padding="sm" style={{ minHeight: 0, display: "flex", flexDirection: "column" }}>
                  <div style={{ height: "100%", minHeight: 0, display: "flex", flexDirection: "column" }}>
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 6, flexShrink: 0 }}>
                      <div style={{ fontSize: 11, fontWeight: 600 }}>Recent Sessions</div>
                      <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
                        <Badge size="sm" variant="default">{recentSessions.length}</Badge>
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
                      <div style={{ display: "grid", gap: 8, flex: 1, minHeight: 0, overflowY: "auto", overflowX: "hidden", paddingRight: 4 }}>
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
                      </div>
                    )}
                  </div>
                </Card>

                {pinnedContext.length > 0 ? (
                  <Card variant="outlined" padding="sm" style={{ minHeight: 0, display: "flex", flexDirection: "column" }}>
                    <div style={{ height: "100%", minHeight: 0, display: "flex", flexDirection: "column" }}>
                      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 6, flexShrink: 0 }}>
                        <div style={{ fontSize: 11, fontWeight: 600 }}>Pinned Context</div>
                        <Badge size="sm" variant="default">{pinnedContext.length}</Badge>
                      </div>
                      <div style={{ display: "grid", gap: 6, flex: 1, minHeight: 0, overflowY: "auto", paddingRight: 4 }}>
                        {pinnedContext.slice(0, 12).map((item) => (
                          <div key={`${item.kind}:${item.path}`} style={{ border: "1px solid var(--token-colors-border-default)", borderRadius: 8, padding: 8, background: "var(--token-colors-alpha-bg-_08)" }}>
                            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
                              <div style={{ minWidth: 0 }}>
                                <div style={{ fontSize: 11, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{item.title}</div>
                                <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{item.path}</div>
                              </div>
                              <Badge size="sm" variant="info">{item.kind}</Badge>
                            </div>
                            {item.snippet ? (
                              <div style={{ marginTop: 6, fontSize: 10, color: "var(--token-colors-text-subtle)", lineHeight: 1.5 }}>{item.snippet.slice(0, 180)}</div>
                            ) : null}
                            <div style={{ display: "flex", gap: 6, marginTop: 8, flexWrap: "wrap" }}>
                              <Button variant="ghost" size="sm" onClick={() => void onOpenPinnedInCanvas(item)}>Open</Button>
                              <Button variant="ghost" size="sm" onClick={() => onInsertPinnedIntoCanvas(item)}>Insert</Button>
                              <Button variant="ghost" size="sm" onClick={() => onUnpinContextItem(item.path)}>Unpin</Button>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </Card>
                ) : null}
              </div>

              <div role="separator" aria-orientation="horizontal" onMouseDown={onStartSidebarPaneResize} className="group flex h-4 cursor-row-resize items-center justify-center">
                <div style={{ height: 6, width: 56, borderRadius: 999, border: "1px solid var(--token-colors-border-default)", background: "var(--token-colors-alpha-bg-_16)" }} />
              </div>

              <ChatWorkspaceSidebarExplorer
                semanticMode={semanticMode}
                activeEntryCount={activeEntryCount}
                semanticProjects={semanticProjects}
                loadingBrowse={loadingBrowse}
                browseData={browseData}
                semanticResults={semanticResults}
                filteredEntries={filteredEntries}
                previewData={previewData}
                loadingPreview={loadingPreview}
                onPreviewFile={onPreviewFile}
                onLoadDirectory={onLoadDirectory}
                onPinSemanticResult={onPinSemanticResult}
                onAppendToScratchpad={onAppendToScratchpad}
                onPinPreviewContext={onPinPreviewContext}
                onOpenPreviewInCanvas={onOpenPreviewInCanvas}
              />
              </div>
            </div>
        </div>
      </Card>
      <div
        role="separator"
        aria-orientation="vertical"
        aria-label="Resize context bar"
        onMouseDown={onStartSidebarWidthResize}
        style={{ width: 8, cursor: "col-resize", flexShrink: 0, display: "flex", alignItems: "stretch", justifyContent: "center", background: "transparent" }}
      >
        <div style={{ width: 2, borderRadius: 999, background: "var(--token-colors-alpha-bg-_16)", margin: "8px 0" }} />
      </div>
    </>
  );
}
