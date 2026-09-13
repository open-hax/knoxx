import { groupSessionSearchHits, loadMoreSessionsOnScroll } from "../chat-page/ChatRuntimePanel";
import type { UIEvent } from "react";
import { Badge, Button, Card } from "@open-hax/uxx";
import { ContextBarExplorer, ContextBarFilters } from "./ContextBarExplorer";
import type { ContextBarProps } from "./types";
import { formatMaybeDate } from "./utils";

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
  onNewVisualDraft,
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
  sessionSearchHits,
  sessionSearchMode,
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
  availableActors,
  sessionActorFilter,
  excludeEtaMuSessions,
  onLoadDirectory,
  onEntryFilterChange,
  onSemanticQueryChange,
  onSemanticSearch,
  onClearSemanticSearch,
  onSessionActorFilterChange,
  onExcludeEtaMuSessionsChange,
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
  const sessionSearchActive = Boolean((semanticQuery ?? "").trim());
  const actorOptions = [
    { id: "all", label: "All actors" },
    ...((availableActors ?? []).map((actor) => ({ id: actor.id, label: actor.id }))),
  ].filter((item, index, items) => items.findIndex((candidate) => candidate.id === item.id) === index);
  const groupedSessionHits = groupSessionSearchHits(sessionSearchHits, recentSessions);

  const handleRecentSessionsScroll = (event: UIEvent<HTMLDivElement>) => loadMoreSessionsOnScroll(event,
    recentSessionsHasMore, loadingMoreRecentSessions, loadingRecentSessions, onLoadMoreRecentSessions);

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
          className="knoxx-context-heading"
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
          <ContextBarFilters
            onEntryFilterChange={onEntryFilterChange} entryFilter={entryFilter} onSemanticQueryChange={onSemanticQueryChange}
            semanticQuery={semanticQuery} semanticMode={semanticMode} onClearSemanticSearch={onClearSemanticSearch}
            semanticSearching={semanticSearching} onSemanticSearch={onSemanticSearch} onSessionActorFilterChange={onSessionActorFilterChange}
            sessionActorFilter={sessionActorFilter} onExcludeEtaMuSessionsChange={onExcludeEtaMuSessionsChange} excludeEtaMuSessions={excludeEtaMuSessions}
            onSourceFilterChange={onSourceFilterChange} sourceFilter={sourceFilter} onDomainFilterChange={onDomainFilterChange}
            domainFilter={domainFilter} onPathPrefixFilterChange={onPathPrefixFilterChange} pathPrefixFilter={pathPrefixFilter}
            visibilityFilter={visibilityFilter} onVisibilityFilterChange={onVisibilityFilterChange} statsByVisibility={statsByVisibility}
            kindFilter={kindFilter} onKindFilterChange={onKindFilterChange} onNewDocument={onNewDocument}
            onNewVisualDraft={onNewVisualDraft} workspaceJob={workspaceJob} actorOptions={actorOptions}
            statusColor={statusColor} ingestionStatus={ingestionStatus}
          />

          <div ref={sidebarSplitContainerRef} style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", overflow: "hidden" }}>
            {/* Sessions section - only for chat workspace */}
            {hasChatFeatures && recentSessions && (
              <div style={{ flex: `0 0 ${sidebarPaneSplitPct}%`, minHeight: 0, display: "flex", flexDirection: "column", overflow: "hidden" }}>
                {/* Recent Sessions - compact */}
                <div style={{ padding: "6px 8px", borderBottom: "1px solid var(--token-colors-border-default)", flex: 1, minHeight: 0, display: "flex", flexDirection: "column", overflow: "hidden" }}>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 6, flexShrink: 0 }}>
                    <span style={{ fontSize: 10, fontWeight: 600, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>
                      {sessionSearchActive ? "Session matches" : "Sessions"}
                    </span>
                    <div style={{ display: "flex", gap: 4, alignItems: "center" }}>
                      <Badge size="sm" variant="default">
                        {sessionSearchActive
                          ? groupedSessionHits.length
                          : (recentSessionsTotal ?? 0) > 0
                            ? `${recentSessions.length}/${recentSessionsTotal}`
                            : recentSessions.length}
                      </Badge>
                      {sessionSearchActive && sessionSearchMode && sessionSearchMode !== "none" ? (
                        <Badge size="sm" variant="info">{sessionSearchMode}</Badge>
                      ) : null}
                      {onRefreshRecentSessions && (
                        <Button variant="ghost" size="sm" loading={loadingRecentSessions} onClick={() => void onRefreshRecentSessions()}>
                          ↻
                        </Button>
                      )}
                    </div>
                  </div>
                  {sessionSearchActive ? (
                    groupedSessionHits.length === 0 ? (
                      <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)" }}>No session matches</div>
                    ) : (
                      <div style={{ display: "flex", flexDirection: "column", gap: 4, flex: 1, minHeight: 0, overflowY: "auto", overflowX: "hidden" }}>
                        {groupedSessionHits.map((item) => {
                          const isCurrent = (sessionId && recentSessions?.find((row) => row.session === item.session)?.active_session_id === sessionId)
                            || (conversationId && conversationId === item.session);
                          return (
                            <button
                              key={item.session}
                              type="button"
                              onClick={() => onResumeMemorySession && void onResumeMemorySession(item.session)}
                              style={{
                                width: "100%",
                                textAlign: "left",
                                padding: "6px",
                                border: "none",
                                borderRadius: 4,
                                background: isCurrent ? "var(--token-colors-alpha-blue-_15)" : "transparent",
                                cursor: "pointer",
                                display: "grid",
                                gap: 2,
                              }}
                            >
                              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 4 }}>
                                <div style={{ minWidth: 0, fontSize: 11, fontWeight: 500, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                                  {item.title || item.session.slice(0, 8)}
                                </div>
                                <div style={{ display: "flex", gap: 4, flexShrink: 0 }}>
                                  {isCurrent ? <Badge size="sm" variant="info">Current</Badge> : null}
                                  <Badge size="sm" variant="default">{item.hitCount}</Badge>
                                </div>
                              </div>
                              {item.snippet ? (
                                <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)", lineHeight: 1.35, overflow: "hidden", textOverflow: "ellipsis", display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical" as const }}>
                                  {item.snippet}
                                </div>
                              ) : null}
                            </button>
                          );
                        })}
                      </div>
                    )
                  ) : recentSessions.length === 0 ? (
                    <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)" }}>No sessions</div>
                  ) : (
                    <div
                      role="region" aria-label="Context sessions"
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
                          className="knoxx-context-explorer-path"
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
