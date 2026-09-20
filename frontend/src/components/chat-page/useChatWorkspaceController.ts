import type { ChatWorkspaceControllerOptions } from "../context-bar/types";
import { SESSION_ACTOR_FILTER_KEY, EXCLUDE_ETA_MU_SESSIONS_KEY, writeLastChatSettings, shouldApplyAgentModelSelection } from "./ChatSettingsPanel";
import { useEffect } from "react";
import { useChatPagePersistenceSuite } from "./chat-page-persistence-suite";
import { createChatRuntimeActions } from "./chat-runtime-actions";
import { useChatRuntimeEffects } from "./chat-runtime-effects";
import { useChatPageConfig, useChatWorkspaceState, selectChatWorkspaceState } from "./chat-page-config";
import { useChatPageDerivedState } from "./chat-page-derived";
import { makeId } from "./make-id";
import { createChatScratchpadActions } from "./scratchpad-actions";
import { canvasArtifactFromToolReceipt } from "./utils";
import { createChatWorkspaceActions } from "./workspace-actions";
import { createSidebarResizeHandlers } from "./sidebar-resize";
import {
  DEFAULT_EXCLUDE_PATTERNS,
  DEFAULT_FILE_TYPES,
  DEFAULT_SYNC_INTERVAL_MINUTES,
} from "./workspace-sync-constants";
import { useChatSessionRecovery } from "./hooks";

const SESSION_ID_KEY = "knoxx_session_id";
const SCRATCHPAD_STATE_KEY = "knoxx_scratchpad_state";
const PINNED_CONTEXT_KEY = "knoxx_pinned_context";
const CHAT_SESSION_STATE_KEY = "knoxx_chat_session_state";
const CHAT_SIDEBAR_WIDTH_KEY = "knoxx_chat_sidebar_width_px";
const DEFAULT_ROLE = "executive";
const SEND_UI_GUARD_TIMEOUT_MS = 30 * 60 * 1000;

export function useChatWorkspaceController(options: ChatWorkspaceControllerOptions = {}) {
  const {
    initialShowCanvas = true,
    initialShowConsole = false,
    initialShowSettings = false,
    initialSidebarWidthPx = 320,
    defaultRole = DEFAULT_ROLE,
    defaultActorId = "chat_primary",
    sessionIdKey = SESSION_ID_KEY,
    scratchpadStorageKey = SCRATCHPAD_STATE_KEY,
    pinnedContextStorageKey = PINNED_CONTEXT_KEY,
    sessionStateKey = CHAT_SESSION_STATE_KEY,
    sidebarWidthKey = CHAT_SIDEBAR_WIDTH_KEY,
    sendUiGuardTimeoutMs = SEND_UI_GUARD_TIMEOUT_MS,
  } = options;

  const state = useChatWorkspaceState({ defaultRole, defaultActorId, initialShowCanvas,
    initialShowConsole, initialShowSettings, initialSidebarWidthPx });

  const isRecovering = useChatSessionRecovery({
    ...selectChatWorkspaceState(state, [
      "sessionId", "pendingAssistantIdRef", "activeRunIdRef", "setConversationId", "setIsSending",
      "setLatestRun", "setConsoleLines",
    ]),
    sessionStateKey,
  });

  const {
    activeEntryCount,
    assistantSurfaceBackground,
    assistantSurfaceBorder,
    assistantSurfaceText,
    currentParentPath,
    currentPath,
    filteredEntries,
    hydrationSources,
    latestToolReceipts,
    liveControlEnabled,
    liveToolEvents,
    liveToolReceipts,
    semanticMode,
    statsByVisibility,
    statsTotal,
    workspaceProgressPercent,
  } = useChatPageDerivedState({
    ...selectChatWorkspaceState(state, [
      "browseData", "entryFilter", "visibilityFilter", "kindFilter", "semanticQuery",
      "semanticResults", "workspaceJob", "latestRun", "isSending", "runtimeEvents",
      "conversationId",
    ]),
    pendingAssistantId: state.pendingAssistantIdRef.current,
  });

  const { startSidebarPaneResize, startSidebarWidthResize } = createSidebarResizeHandlers({
    sidebarSplitContainerRef: state.sidebarSplitContainerRef,
    sidebarWidthPx: state.sidebarWidthPx,
    setSidebarPaneSplitPct: state.setSidebarPaneSplitPct,
    setSidebarWidthPx: state.setSidebarWidthPx,
  });

  useChatPagePersistenceSuite({
    ...selectChatWorkspaceState(state, [
      "sessionId", "setSessionId", "systemPrompt", "setSystemPrompt", "selectedModel",
      "setSelectedModel", "selectedThinkingLevel", "setSelectedThinkingLevel", "activeActorId", "setActiveActorId",
      "activeAgentId", "setActiveAgentId", "conversationId", "setConversationId", "messages",
      "setMessages", "latestRun", "setLatestRun", "runtimeEvents", "setRuntimeEvents",
      "isSending", "setIsSending", "sidebarWidthPx", "setSidebarWidthPx", "pendingAssistantIdRef",
      "activeRunIdRef", "canvasTitle", "setCanvasTitle", "canvasSubject", "setCanvasSubject",
      "canvasPath", "setCanvasPath", "canvasRecipients", "setCanvasRecipients", "canvasCc",
      "setCanvasCc", "canvasContent", "setCanvasContent", "pinnedContext", "setPinnedContext",
      "setProxxReachable", "setProxxConfigured", "setProxxModels",
    ]),
    makeId,
    sessionIdKey,
    sessionStateKey,
    sidebarWidthKey,
    scratchpadStorageKey,
    pinnedContextStorageKey,
  });

  const {
    appendToScratchpad,
    clearScratchpad,
    fetchPreviewData,
    insertPinnedIntoCanvas,
    openCanvasArtifact,
    openMessageInCanvas,
    openPinnedInCanvas,
    openPreviewInCanvas,
    openSourceInPreview,
    pinAssistantSource,
    pinContextItem,
    pinMessageContext,
    pinPreviewContext,
    pinSemanticResult,
    saveCanvasDraft,
    saveCanvasFile,
    sendCanvasEmailAction,
    unpinContextItem,
    useLatestAssistantInCanvas,
  } = createChatScratchpadActions({
    ...selectChatWorkspaceState(state, [
      "activeRole", "activeAgentId", "messages", "previewData", "setPreviewData",
      "canvasTitle", "setCanvasTitle", "canvasSubject", "setCanvasSubject", "canvasPath",
      "setCanvasPath", "canvasRecipients", "canvasCc", "canvasContent", "setCanvasContent",
      "setCanvasStatus", "setPinnedContext", "setShowCanvas", "setSavingCanvas", "setSavingCanvasFile",
      "setSendingCanvas", "setConsoleLines",
    ]),

  });

  const {
    appendMessageIfMissing,
    handleNewChat,
    handleSend,
    handleUndoLastTurn,
    loadRunDetail,
    queueLiveControl,
    voiceSteer,
    abortTurn,
    updateMessageById,
    updateTraceBlocksByMessageId,
  } = createChatRuntimeActions({
    ...selectChatWorkspaceState(state, [
      "systemPrompt", "activeRole", "activeActorId", "activeAgentId", "sessionId",
      "setSessionId", "conversationId", "setConversationId", "selectedModel", "selectedThinkingLevel",
      "liveControlText", "setLiveControlText", "setMessages", "setLatestRun", "setRuntimeEvents",
      "setIsSending", "setConsoleLines", "setQueueingControl", "setAbortingTurn", "pendingAssistantIdRef",
      "activeRunIdRef",
    ]),
    makeId,
    liveControlEnabled,
    sessionIdKey,
    sessionStateKey,
  });

  const {
    ensureWorkspaceSync,
    loadDirectory,
    loadMoreRecentSessions,
    previewFile,
    refreshRecentSessions,
    refreshWorkspaceStatus,
    resumeMemorySession,
    runSemanticSearch,
  } = createChatWorkspaceActions({
    ...selectChatWorkspaceState(state, [
      "browseData", "semanticQuery", "sessionActorFilter", "excludeEtaMuSessions", "setBrowseData",
      "setPreviewData", "setLoadingBrowse", "setLoadingPreview", "setSemanticResults", "setSemanticProjects",
      "setSemanticSearching", "setSessionSearchHits", "setSessionSearchMode", "setSyncingWorkspace", "setWorkspaceSourceId",
      "setWorkspaceJob", "recentSessionsRef", "remoteRecentSessionsRef", "setRecentSessions", "setRecentSessionsHasMore",
      "setRecentSessionsTotal", "setLoadingRecentSessions", "setLoadingMoreRecentSessions", "setLoadingMemorySessionId", "setMessages",
      "setSelectedModel", "setSessionId", "setConversationId", "setLatestRun", "setRuntimeEvents",
      "setLiveControlText", "setIsSending", "setConsoleLines", "pendingAssistantIdRef", "activeRunIdRef",
    ]),
    visibleAgentIds: new Set(state.availableAgents.map((agent) => agent.id)),
    currentPath,
    showFiles: true,
    makeId,
    sessionStateKey,
    fetchPreviewData,
    loadRunDetail,
    defaultSyncIntervalMinutes: DEFAULT_SYNC_INTERVAL_MINUTES,
    defaultFileTypes: DEFAULT_FILE_TYPES,
    defaultExcludePatterns: DEFAULT_EXCLUDE_PATTERNS,
  });

  useEffect(() => {
    void refreshRecentSessions();
    // refreshRecentSessions is recreated each render; session/actor/agent catalog changes are the intended triggers.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.sessionId, state.activeActorId, state.availableAgents, state.sessionActorFilter, state.excludeEtaMuSessions]);

  useEffect(() => {
    if (!state.semanticQuery.trim()) {
      state.setSessionSearchHits([]);
      state.setSessionSearchMode("none");
    }
  }, [state.semanticQuery]);

  useEffect(() => {
    if (!state.semanticQuery.trim()) return;
    void runSemanticSearch(state.semanticQuery);
    // runSemanticSearch is recreated each render; actor filter changes are the intentional retrigger here.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state.sessionActorFilter, state.excludeEtaMuSessions]);

  useEffect(() => {
    try {
      window.localStorage.setItem(SESSION_ACTOR_FILTER_KEY, state.sessionActorFilter);
      window.localStorage.setItem(EXCLUDE_ETA_MU_SESSIONS_KEY, String(state.excludeEtaMuSessions));
    } catch {
      // ignore storage failures
    }
  }, [state.excludeEtaMuSessions, state.sessionActorFilter]);

  useChatPageConfig({
    ...selectChatWorkspaceState(state, [
      "activeRole", "activeActorId", "activeAgentId", "setActiveRole", "setActiveActorId",
      "setAvailableActors", "setActiveAgentId", "setAvailableAgents", "setToolCatalog", "setConsoleLines",
      "setSttEnabled", "setTtsEnabled", "setTtsDefaultVoiceId",
    ]),
    defaultRole,
    defaultActorId,
  });

  useEffect(() => {
    if (!state.activeAgentId) return;
    const selectedAgent = state.availableAgents.find((agent) => agent.id === state.activeAgentId);
    if (!selectedAgent) return;
    const agentModel = selectedAgent.model ?? undefined;
    if (selectedAgent.role && selectedAgent.role !== state.activeRole) {
      state.setActiveRole(selectedAgent.role);
    }
    if (agentModel && shouldApplyAgentModelSelection({
      activeAgentId: state.activeAgentId,
      previousAgentId: state.lastAppliedAgentIdRef.current,
      selectedModel: state.selectedModel,
      agentModel,
    }) && agentModel !== state.selectedModel) {
      state.setSelectedModel(agentModel);
    }
    state.lastAppliedAgentIdRef.current = state.activeAgentId;
  }, [state.activeAgentId, state.activeRole, state.availableAgents, state.selectedModel, state.setActiveRole, state.setSelectedModel]);

  useChatRuntimeEffects({
    ...selectChatWorkspaceState(state, [
      "sessionId", "conversationId", "isSending", "latestRun", "semanticQuery",
      "sendTimeoutRef", "pendingAssistantIdRef", "activeRunIdRef", "setWsStatus", "setIsSending",
      "setLatestRun", "setRuntimeEvents", "setConsoleLines", "setSemanticResults", "setSemanticProjects",
    ]),
    currentPath,
    sendUiGuardTimeoutMs,
    updateMessageById,
    updateTraceBlocksByMessageId,
    appendMessageIfMissing,
    loadRunDetail,
    loadDirectory,
    refreshWorkspaceStatus,
    refreshRecentSessions,
    runSemanticSearch,
  });

  const pinHydrationSource = (source: { title: string; path: string; section?: string }) => {
    pinContextItem({
      id: source.path,
      title: source.title,
      path: source.path,
      snippet: source.section,
      kind: "semantic",
    });
  };

  const openHydrationSource = async (source: { path: string }) => {
    await previewFile(source.path);
  };

  useEffect(() => {
    const receipts = state.latestRun?.tool_receipts ?? [];
    for (const receipt of receipts) {
      if (!receipt?.id || state.appliedCanvasReceiptIdsRef.current.has(receipt.id)) continue;
      const artifact = canvasArtifactFromToolReceipt(receipt);
      if (!artifact) continue;
      state.appliedCanvasReceiptIdsRef.current.add(receipt.id);
      openCanvasArtifact({
        ...artifact,
        statusMessage: artifact.path ? `Opened ${artifact.path} in canvas.` : "Opened tool result in canvas.",
      });
    }
  }, [state.latestRun?.tool_receipts, openCanvasArtifact]);

  useEffect(() => {
    writeLastChatSettings({
      activeAgentId: state.activeAgentId,
      selectedModel: state.selectedModel,
      selectedThinkingLevel: state.selectedThinkingLevel,
    });
  }, [state.activeAgentId, state.selectedModel, state.selectedThinkingLevel]);

  return {
    ...selectChatWorkspaceState(state, [
      "activeRole", "activeActorId", "setActiveActorId", "availableActors", "activeAgentId",
      "setActiveAgentId", "availableAgents", "toolCatalog", "systemPrompt", "setSystemPrompt",
      "sessionId", "messages", "consoleLines", "isSending", "showConsole",
      "showSettings", "showCanvas", "wsStatus", "conversationId", "latestRun",
      "runtimeEvents", "liveControlText", "setLiveControlText", "queueingControl", "abortingTurn",
      "proxxModels", "selectedModel", "setSelectedModel", "selectedThinkingLevel", "setSelectedThinkingLevel",
      "proxxReachable", "proxxConfigured", "sttEnabled", "ttsEnabled", "ttsDefaultVoiceId",
      "browseData", "previewData", "loadingBrowse", "loadingPreview", "entryFilter",
      "setEntryFilter", "semanticQuery", "setSemanticQuery", "semanticResults", "setSemanticResults",
      "semanticProjects", "setSemanticProjects", "semanticSearching", "sessionSearchHits", "setSessionSearchHits",
      "sessionSearchMode", "setSessionSearchMode", "workspaceSourceId", "workspaceJob", "recentSessions",
      "recentSessionsHasMore", "recentSessionsTotal", "loadingRecentSessions", "loadingMoreRecentSessions", "loadingMemorySessionId",
      "sidebarPaneSplitPct", "sidebarWidthPx", "sidebarSplitContainerRef", "visibilityFilter", "setVisibilityFilter",
      "kindFilter", "setKindFilter", "sessionActorFilter", "setSessionActorFilter", "excludeEtaMuSessions",
      "setExcludeEtaMuSessions", "syncingWorkspace", "canvasTitle", "setCanvasTitle", "canvasSubject",
      "setCanvasSubject", "canvasPath", "setCanvasPath", "canvasRecipients", "setCanvasRecipients",
      "canvasCc", "setCanvasCc", "canvasContent", "setCanvasContent", "canvasStatus",
      "savingCanvas", "savingCanvasFile", "sendingCanvas", "pinnedContext",
    ]),
    isRecovering,
    statsTotal,
    statsByVisibility,
    activeEntryCount,
    assistantSurfaceBackground,
    assistantSurfaceBorder,
    assistantSurfaceText,
    currentParentPath,
    currentPath,
    filteredEntries,
    hydrationSources,
    latestToolReceipts,
    liveControlEnabled,
    liveToolEvents,
    liveToolReceipts,
    semanticMode,
    workspaceProgressPercent,
    startSidebarPaneResize,
    startSidebarWidthResize,
    toggleConsole: () => state.setShowConsole((value) => !value),
    toggleSettings: () => state.setShowSettings((value) => !value),
    toggleCanvas: () => state.setShowCanvas((value) => !value),
    handleNewChat,
    handleSend,
    handleUndoLastTurn,
    queueLiveControl,
    voiceSteer,
    abortTurn,
    openHydrationSource,
    pinHydrationSource,
    loadDirectory,
    loadMoreRecentSessions,
    previewFile,
    refreshRecentSessions,
    resumeMemorySession,
    runSemanticSearch,
    appendToScratchpad,
    clearScratchpad,
    insertPinnedIntoCanvas,
    openCanvasArtifact,
    openMessageInCanvas,
    openPinnedInCanvas,
    openPreviewInCanvas,
    openSourceInPreview,
    pinAssistantSource,
    pinContextItem,
    pinMessageContext,
    pinPreviewContext,
    pinSemanticResult,
    saveCanvasDraft,
    saveCanvasFile,
    sendCanvasEmailAction,
    unpinContextItem,
    useLatestAssistantInCanvas,
  };
}

export type ChatWorkspaceController = ReturnType<typeof useChatWorkspaceController>;

export type { ChatWorkspaceControllerOptions } from "../context-bar/types";
export { shouldApplyAgentModelSelection } from "./ChatSettingsPanel";
