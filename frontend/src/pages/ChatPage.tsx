import { useState } from "react";
import { CollapsedPanelTab } from "../components/CollapsedPanelTab";
import { ChatWorkspacePane } from "../components/chat-page/ChatWorkspacePane";
import { useChatWorkspaceController } from "../components/chat-page/useChatWorkspaceController";
import { ContextBar } from "../components/context-bar";

const CHAT_PAGE_ACTOR_ID = "chat_primary";
const CHAT_PAGE_SESSION_KEY = "knoxx_chat_page_session_id";
const CHAT_PAGE_SCRATCHPAD_KEY = "knoxx_chat_page_scratchpad_state";
const CHAT_PAGE_PINNED_KEY = "knoxx_chat_page_pinned_context";
const CHAT_PAGE_SESSION_STATE_KEY = "knoxx_chat_page_session_state";
const CHAT_PAGE_SIDEBAR_WIDTH_KEY = "knoxx_chat_page_sidebar_width_px";

function ChatPage() {
  const chat = useChatWorkspaceController({
    initialShowCanvas: false,
    defaultActorId: CHAT_PAGE_ACTOR_ID,
    sessionIdKey: CHAT_PAGE_SESSION_KEY,
    scratchpadStorageKey: CHAT_PAGE_SCRATCHPAD_KEY,
    pinnedContextStorageKey: CHAT_PAGE_PINNED_KEY,
    sessionStateKey: CHAT_PAGE_SESSION_STATE_KEY,
    sidebarWidthKey: CHAT_PAGE_SIDEBAR_WIDTH_KEY,
  });
  const [showFiles, setShowFiles] = useState(true);

  return (
    <div
      style={{
        display: "flex",
        flex: "1 1 0%",
        gap: 0,
        minHeight: 0,
        minWidth: 0,
        overflow: "hidden",
        background:
          "radial-gradient(circle at top left, var(--token-colors-alpha-green-_14) 0%, transparent 28%), radial-gradient(circle at bottom right, var(--token-colors-alpha-orange-_12) 0%, transparent 24%), linear-gradient(180deg, var(--token-monokai-bg-default) 0%, var(--token-monokai-bg-darker) 100%)",
      }}
    >
      {showFiles ? (
        <ContextBar
          sidebarWidthPx={chat.sidebarWidthPx}
          sidebarPaneSplitPct={chat.sidebarPaneSplitPct}
          sidebarSplitContainerRef={chat.sidebarSplitContainerRef}
          currentPath={chat.currentPath}
          currentParentPath={chat.currentParentPath}
          browseData={chat.browseData}
          previewData={chat.previewData}
          loadingBrowse={chat.loadingBrowse}
          loadingPreview={chat.loadingPreview}
          entryFilter={chat.entryFilter}
          semanticQuery={chat.semanticQuery}
          semanticResults={chat.semanticResults}
          semanticProjects={chat.semanticProjects}
          semanticSearching={chat.semanticSearching}
          sessionSearchHits={chat.sessionSearchHits}
          sessionSearchMode={chat.sessionSearchMode}
          semanticMode={chat.semanticMode}
          filteredEntries={chat.filteredEntries}
          activeEntryCount={chat.activeEntryCount}
          workspaceSourceId={chat.workspaceSourceId}
          workspaceJob={chat.workspaceJob}
          workspaceProgressPercent={chat.workspaceProgressPercent}
          pinnedContext={chat.pinnedContext}
          recentSessions={chat.recentSessions}
          recentSessionsHasMore={chat.recentSessionsHasMore}
          recentSessionsTotal={chat.recentSessionsTotal}
          loadingRecentSessions={chat.loadingRecentSessions}
          loadingMoreRecentSessions={chat.loadingMoreRecentSessions}
          loadingMemorySessionId={chat.loadingMemorySessionId}
          sessionId={chat.sessionId}
          conversationId={chat.conversationId}
          availableActors={chat.availableActors}
          sessionActorFilter={chat.sessionActorFilter}
          excludeEtaMuSessions={chat.excludeEtaMuSessions}
          visibilityFilter={chat.visibilityFilter}
          kindFilter={chat.kindFilter}
          statsTotal={chat.statsTotal}
          statsByVisibility={chat.statsByVisibility}
          onHide={() => setShowFiles(false)}
          onLoadDirectory={chat.loadDirectory}
          onEntryFilterChange={chat.setEntryFilter}
          onSemanticQueryChange={chat.setSemanticQuery}
          onSemanticSearch={() => chat.runSemanticSearch(chat.semanticQuery)}
          onClearSemanticSearch={() => {
            chat.setSemanticQuery("");
            chat.setSemanticResults([]);
            chat.setSemanticProjects([]);
            chat.setSessionSearchMode("none");
          }}
          onRefreshRecentSessions={chat.refreshRecentSessions}
          onLoadMoreRecentSessions={chat.loadMoreRecentSessions}
          onResumeMemorySession={chat.resumeMemorySession}
          onPreviewFile={chat.previewFile}
          onPinSemanticResult={chat.pinSemanticResult}
          onAppendToScratchpad={chat.appendToScratchpad}
          onPinPreviewContext={chat.pinPreviewContext}
          onOpenPreviewInCanvas={chat.openPreviewInCanvas}
          onOpenPinnedInCanvas={chat.openPinnedInCanvas}
          onInsertPinnedIntoCanvas={chat.insertPinnedIntoCanvas}
          onUnpinContextItem={chat.unpinContextItem}
          onStartSidebarPaneResize={chat.startSidebarPaneResize}
          onStartSidebarWidthResize={chat.startSidebarWidthResize}
          onVisibilityFilterChange={chat.setVisibilityFilter}
          onKindFilterChange={chat.setKindFilter}
          onSessionActorFilterChange={chat.setSessionActorFilter}
          onExcludeEtaMuSessionsChange={chat.setExcludeEtaMuSessions}
        />
      ) : (
        <CollapsedPanelTab label="Files" edge="left" onExpand={() => setShowFiles(true)} title="Show Files panel" />
      )}

      <ChatWorkspacePane controller={chat} showFiles={showFiles} onShowFiles={() => setShowFiles(true)} />
    </div>
  );
}

export default ChatPage;
