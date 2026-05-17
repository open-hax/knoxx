import { useEffect, useRef, useState } from "react";
import { useChatPagePersistenceSuite } from "./chat-page-persistence-suite";
import { createChatRuntimeActions } from "./chat-runtime-actions";
import { useChatRuntimeEffects } from "./chat-runtime-effects";
import { useChatPageConfig } from "./chat-page-config";
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
import type {
  ActorCatalogItem,
  AgentContractCatalogItem,
  ChatMessage,
  MemorySearchHit,
  MemorySessionSummary,
  ProxxModelInfo,
  RunDetail,
  RunEvent,
  ToolCatalogResponse,
} from "../../lib/types";
import type {
  BrowseResponse,
  PinnedContextItem,
  PreviewResponse,
  SemanticSearchMatch,
  WorkspaceJob,
} from "../context-bar/types";

const SESSION_ID_KEY = "knoxx_session_id";
const SCRATCHPAD_STATE_KEY = "knoxx_scratchpad_state";
const PINNED_CONTEXT_KEY = "knoxx_pinned_context";
const CHAT_SESSION_STATE_KEY = "knoxx_chat_session_state";
const CHAT_SIDEBAR_WIDTH_KEY = "knoxx_chat_sidebar_width_px";
const SESSION_ACTOR_FILTER_KEY = "knoxx_session_actor_filter";
const EXCLUDE_ETA_MU_SESSIONS_KEY = "knoxx_exclude_eta_mu_sessions";
const LAST_CHAT_SETTINGS_KEY = "knoxx_last_chat_settings";
const DEFAULT_ROLE = "executive";
const SEND_UI_GUARD_TIMEOUT_MS = 30 * 60 * 1000;

type LastChatSettings = {
  activeAgentId?: string;
  selectedModel?: string;
  selectedThinkingLevel?: string;
};

function readLastChatSettings(): LastChatSettings {
  try {
    const raw = window.localStorage.getItem(LAST_CHAT_SETTINGS_KEY);
    if (!raw) return {};
    return JSON.parse(raw) as LastChatSettings;
  } catch {
    return {};
  }
}

function writeLastChatSettings(settings: LastChatSettings): void {
  try {
    window.localStorage.setItem(LAST_CHAT_SETTINGS_KEY, JSON.stringify(settings));
  } catch {
    // ignore storage failures
  }
}

function readStoredString(key: string, fallback: string): string {
  try {
    const value = window.localStorage.getItem(key);
    return value && value.trim().length > 0 ? value : fallback;
  } catch {
    return fallback;
  }
}

function readStoredBoolean(key: string, fallback: boolean): boolean {
  try {
    const value = window.localStorage.getItem(key);
    if (value === "true") return true;
    if (value === "false") return false;
    return fallback;
  } catch {
    return fallback;
  }
}

export type ChatWorkspaceControllerOptions = {
  initialShowCanvas?: boolean;
  initialShowConsole?: boolean;
  initialShowSettings?: boolean;
  initialSidebarWidthPx?: number;
  defaultRole?: string;
  defaultActorId?: string;
  sessionIdKey?: string;
  scratchpadStorageKey?: string;
  pinnedContextStorageKey?: string;
  sessionStateKey?: string;
  sidebarWidthKey?: string;
  sendUiGuardTimeoutMs?: number;
};

export function shouldApplyAgentModelSelection({
  activeAgentId,
  previousAgentId,
  selectedModel,
  agentModel,
}: {
  activeAgentId: string;
  previousAgentId: string | null;
  selectedModel: string;
  agentModel?: string | null;
}): boolean {
  if (!activeAgentId || !agentModel) return false;
  // On initial mount (previousAgentId is null), only apply the agent's default
  // model if no model was already selected/persisted.
  if (previousAgentId === null) {
    return !selectedModel;
  }
  return previousAgentId !== activeAgentId;
}

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

  const [activeRole, setActiveRole] = useState(defaultRole);
  const [activeActorId, setActiveActorId] = useState(defaultActorId);
  const [availableActors, setAvailableActors] = useState<ActorCatalogItem[]>([]);
  const lastSettings = readLastChatSettings();
  const [activeAgentId, setActiveAgentId] = useState(lastSettings.activeAgentId ?? "");
  const [availableAgents, setAvailableAgents] = useState<AgentContractCatalogItem[]>([]);
  const [toolCatalog, setToolCatalog] = useState<ToolCatalogResponse | null>(null);
  const [systemPrompt, setSystemPrompt] = useState("");
  const [sessionId, setSessionId] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [consoleLines, setConsoleLines] = useState<string[]>([]);
  const [isSending, setIsSending] = useState(false);
  const [showConsole, setShowConsole] = useState(initialShowConsole);
  const [showSettings, setShowSettings] = useState(initialShowSettings);
  const [showCanvas, setShowCanvas] = useState(initialShowCanvas);
  const [wsStatus, setWsStatus] = useState<"connected" | "closed" | "error" | "connecting">("connecting");
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [latestRun, setLatestRun] = useState<RunDetail | null>(null);
  const [runtimeEvents, setRuntimeEvents] = useState<RunEvent[]>([]);
  const [liveControlText, setLiveControlText] = useState("");
  const [queueingControl, setQueueingControl] = useState<"steer" | "follow_up" | null>(null);
  const [abortingTurn, setAbortingTurn] = useState(false);
  const [proxxModels, setProxxModels] = useState<ProxxModelInfo[]>([]);
  const [selectedModel, setSelectedModel] = useState(lastSettings.selectedModel ?? "");
  const [selectedThinkingLevel, setSelectedThinkingLevel] = useState(lastSettings.selectedThinkingLevel ?? "off");
  const [proxxReachable, setProxxReachable] = useState(false);
  const [proxxConfigured, setProxxConfigured] = useState(false);
  const [sttEnabled, setSttEnabled] = useState(false);
  const [ttsEnabled, setTtsEnabled] = useState(false);
  const [ttsDefaultVoiceId, setTtsDefaultVoiceId] = useState("");
  const [browseData, setBrowseData] = useState<BrowseResponse | null>(null);
  const [previewData, setPreviewData] = useState<PreviewResponse | null>(null);
  const [loadingBrowse, setLoadingBrowse] = useState(false);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [entryFilter, setEntryFilter] = useState("");
  const [semanticQuery, setSemanticQuery] = useState("");
  const [semanticResults, setSemanticResults] = useState<SemanticSearchMatch[]>([]);
  const [semanticProjects, setSemanticProjects] = useState<string[]>([]);
  const [semanticSearching, setSemanticSearching] = useState(false);
  const [sessionSearchHits, setSessionSearchHits] = useState<MemorySearchHit[]>([]);
  const [sessionSearchMode, setSessionSearchMode] = useState("none");
  const [syncingWorkspace, setSyncingWorkspace] = useState(false);
  const [workspaceSourceId, setWorkspaceSourceId] = useState<string | null>(null);
  const [workspaceJob, setWorkspaceJob] = useState<WorkspaceJob | null>(null);
  const [canvasTitle, setCanvasTitle] = useState("Untitled canvas");
  const [canvasSubject, setCanvasSubject] = useState("");
  const [canvasPath, setCanvasPath] = useState("notes/canvas/untitled-canvas.md");
  const [canvasRecipients, setCanvasRecipients] = useState("");
  const [canvasCc, setCanvasCc] = useState("");
  const [canvasContent, setCanvasContent] = useState("");
  const [canvasStatus, setCanvasStatus] = useState<string | null>(null);
  const [savingCanvas, setSavingCanvas] = useState(false);
  const [savingCanvasFile, setSavingCanvasFile] = useState(false);
  const [sendingCanvas, setSendingCanvas] = useState(false);
  const [pinnedContext, setPinnedContext] = useState<PinnedContextItem[]>([]);
  const [recentSessions, setRecentSessions] = useState<MemorySessionSummary[]>([]);
  const recentSessionsRef = useRef<MemorySessionSummary[]>([]);
  const remoteRecentSessionsRef = useRef<MemorySessionSummary[]>([]);
  recentSessionsRef.current = recentSessions;
  const [recentSessionsHasMore, setRecentSessionsHasMore] = useState(false);
  const [recentSessionsTotal, setRecentSessionsTotal] = useState(0);
  const [loadingRecentSessions, setLoadingRecentSessions] = useState(false);
  const [loadingMoreRecentSessions, setLoadingMoreRecentSessions] = useState(false);
  const [loadingMemorySessionId, setLoadingMemorySessionId] = useState<string | null>(null);
  const appliedCanvasReceiptIdsRef = useRef<Set<string>>(new Set());
  const [sidebarPaneSplitPct, setSidebarPaneSplitPct] = useState(50);
  const [sidebarWidthPx, setSidebarWidthPx] = useState(initialSidebarWidthPx);
  const [visibilityFilter, setVisibilityFilter] = useState("all");
  const [kindFilter, setKindFilter] = useState("docs");
  const [sessionActorFilter, setSessionActorFilter] = useState(() => readStoredString(SESSION_ACTOR_FILTER_KEY, "all"));
  const [excludeEtaMuSessions, setExcludeEtaMuSessions] = useState(() => readStoredBoolean(EXCLUDE_ETA_MU_SESSIONS_KEY, true));
  const sendTimeoutRef = useRef<number | null>(null);
  const pendingAssistantIdRef = useRef<string | null>(null);
  const activeRunIdRef = useRef<string | null>(null);
  const lastAppliedAgentIdRef = useRef<string | null>(null);
  const sidebarSplitContainerRef = useRef<HTMLDivElement | null>(null);

  const isRecovering = useChatSessionRecovery({
    sessionId,
    sessionStateKey,
    pendingAssistantIdRef,
    activeRunIdRef,
    setConversationId,
    setIsSending,
    setLatestRun,
    setConsoleLines,
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
    browseData,
    entryFilter,
    visibilityFilter,
    kindFilter,
    semanticQuery,
    semanticResults,
    workspaceJob,
    latestRun,
    isSending,
    runtimeEvents,
    pendingAssistantId: pendingAssistantIdRef.current,
    conversationId,
  });

  const { startSidebarPaneResize, startSidebarWidthResize } = createSidebarResizeHandlers({
    sidebarSplitContainerRef,
    sidebarWidthPx,
    setSidebarPaneSplitPct,
    setSidebarWidthPx,
  });

  useChatPagePersistenceSuite({
    makeId,
    sessionId,
    setSessionId,
    systemPrompt,
    setSystemPrompt,
    selectedModel,
    setSelectedModel,
    selectedThinkingLevel,
    setSelectedThinkingLevel,
    activeActorId,
    setActiveActorId,
    activeAgentId,
    setActiveAgentId,
    conversationId,
    setConversationId,
    messages,
    setMessages,
    latestRun,
    setLatestRun,
    runtimeEvents,
    setRuntimeEvents,
    isSending,
    setIsSending,
    sidebarWidthPx,
    setSidebarWidthPx,
    pendingAssistantIdRef,
    activeRunIdRef,
    sessionIdKey,
    sessionStateKey,
    sidebarWidthKey,
    scratchpadStorageKey,
    canvasTitle,
    setCanvasTitle,
    canvasSubject,
    setCanvasSubject,
    canvasPath,
    setCanvasPath,
    canvasRecipients,
    setCanvasRecipients,
    canvasCc,
    setCanvasCc,
    canvasContent,
    setCanvasContent,
    pinnedContextStorageKey,
    pinnedContext,
    setPinnedContext,
    setProxxReachable,
    setProxxConfigured,
    setProxxModels,
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
    activeRole,
    activeAgentId,
    messages,
    previewData,
    setPreviewData,
    canvasTitle,
    setCanvasTitle,
    canvasSubject,
    setCanvasSubject,
    canvasPath,
    setCanvasPath,
    canvasRecipients,
    canvasCc,
    canvasContent,
    setCanvasContent,
    setCanvasStatus,
    setPinnedContext,
    setShowCanvas,
    setSavingCanvas,
    setSavingCanvasFile,
    setSendingCanvas,
    setConsoleLines,
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
    makeId,
    systemPrompt,
    activeRole,
    activeActorId,
    activeAgentId,
    sessionId,
    setSessionId,
    conversationId,
    setConversationId,
    selectedModel,
    selectedThinkingLevel,
    liveControlEnabled,
    liveControlText,
    setLiveControlText,
    setMessages,
    setLatestRun,
    setRuntimeEvents,
    setIsSending,
    setConsoleLines,
    setQueueingControl,
    setAbortingTurn,
    pendingAssistantIdRef,
    activeRunIdRef,
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
    visibleAgentIds: new Set(availableAgents.map((agent) => agent.id)),
    currentPath,
    showFiles: true,
    browseData,
    semanticQuery,
    sessionActorFilter,
    excludeEtaMuSessions,
    setBrowseData,
    setPreviewData,
    setLoadingBrowse,
    setLoadingPreview,
    setSemanticResults,
    setSemanticProjects,
    setSemanticSearching,
    setSessionSearchHits,
    setSessionSearchMode,
    setSyncingWorkspace,
    setWorkspaceSourceId,
    setWorkspaceJob,
    recentSessionsRef,
    remoteRecentSessionsRef,
    setRecentSessions,
    setRecentSessionsHasMore,
    setRecentSessionsTotal,
    setLoadingRecentSessions,
    setLoadingMoreRecentSessions,
    setLoadingMemorySessionId,
    setMessages,
    setSelectedModel,
    setSessionId,
    setConversationId,
    setLatestRun,
    setRuntimeEvents,
    setLiveControlText,
    setIsSending,
    setConsoleLines,
    pendingAssistantIdRef,
    activeRunIdRef,
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
  }, [sessionId, activeActorId, availableAgents, sessionActorFilter, excludeEtaMuSessions]);

  useEffect(() => {
    if (!semanticQuery.trim()) {
      setSessionSearchHits([]);
      setSessionSearchMode("none");
    }
  }, [semanticQuery]);

  useEffect(() => {
    if (!semanticQuery.trim()) return;
    void runSemanticSearch(semanticQuery);
    // runSemanticSearch is recreated each render; actor filter changes are the intentional retrigger here.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionActorFilter, excludeEtaMuSessions]);

  useEffect(() => {
    try {
      window.localStorage.setItem(SESSION_ACTOR_FILTER_KEY, sessionActorFilter);
      window.localStorage.setItem(EXCLUDE_ETA_MU_SESSIONS_KEY, String(excludeEtaMuSessions));
    } catch {
      // ignore storage failures
    }
  }, [excludeEtaMuSessions, sessionActorFilter]);

  useChatPageConfig({
    defaultRole,
    defaultActorId,
    activeRole,
    activeActorId,
    activeAgentId,
    setActiveRole,
    setActiveActorId,
    setAvailableActors,
    setActiveAgentId,
    setAvailableAgents,
    setToolCatalog,
    setConsoleLines,
    setSttEnabled,
    setTtsEnabled,
    setTtsDefaultVoiceId,
  });

  useEffect(() => {
    if (!activeAgentId) return;
    const selectedAgent = availableAgents.find((agent) => agent.id === activeAgentId);
    if (!selectedAgent) return;
    const agentModel = selectedAgent.model ?? undefined;
    if (selectedAgent.role && selectedAgent.role !== activeRole) {
      setActiveRole(selectedAgent.role);
    }
    if (agentModel && shouldApplyAgentModelSelection({
      activeAgentId,
      previousAgentId: lastAppliedAgentIdRef.current,
      selectedModel,
      agentModel,
    }) && agentModel !== selectedModel) {
      setSelectedModel(agentModel);
    }
    lastAppliedAgentIdRef.current = activeAgentId;
  }, [activeAgentId, activeRole, availableAgents, selectedModel, setActiveRole, setSelectedModel]);

  useChatRuntimeEffects({
    sessionId,
    conversationId,
    isSending,
    latestRun,
    semanticQuery,
    currentPath,
    sendUiGuardTimeoutMs,
    sendTimeoutRef,
    pendingAssistantIdRef,
    activeRunIdRef,
    setWsStatus,
    setIsSending,
    setLatestRun,
    setRuntimeEvents,
    setConsoleLines,
    setSemanticResults,
    setSemanticProjects,
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
    const receipts = latestRun?.tool_receipts ?? [];
    for (const receipt of receipts) {
      if (!receipt?.id || appliedCanvasReceiptIdsRef.current.has(receipt.id)) continue;
      const artifact = canvasArtifactFromToolReceipt(receipt);
      if (!artifact) continue;
      appliedCanvasReceiptIdsRef.current.add(receipt.id);
      openCanvasArtifact({
        ...artifact,
        statusMessage: artifact.path ? `Opened ${artifact.path} in canvas.` : "Opened tool result in canvas.",
      });
    }
  }, [latestRun?.tool_receipts, openCanvasArtifact]);

  useEffect(() => {
    writeLastChatSettings({
      activeAgentId,
      selectedModel,
      selectedThinkingLevel,
    });
  }, [activeAgentId, selectedModel, selectedThinkingLevel]);

  return {
    // state
    activeRole,
    activeActorId,
    setActiveActorId,
    availableActors,
    activeAgentId,
    setActiveAgentId,
    availableAgents,
    toolCatalog,
    systemPrompt,
    setSystemPrompt,
    sessionId,
    messages,
    consoleLines,
    isSending,
    showConsole,
    showSettings,
    showCanvas,
    wsStatus,
    conversationId,
    latestRun,
    runtimeEvents,
    liveControlText,
    setLiveControlText,
    queueingControl,
    abortingTurn,
    proxxModels,
    selectedModel,
    setSelectedModel,
    selectedThinkingLevel,
    setSelectedThinkingLevel,
    proxxReachable,
    proxxConfigured,
    sttEnabled,
    ttsEnabled,
    ttsDefaultVoiceId,
    isRecovering,

    // workspace/context bar state
    browseData,
    previewData,
    loadingBrowse,
    loadingPreview,
    entryFilter,
    setEntryFilter,
    semanticQuery,
    setSemanticQuery,
    semanticResults,
    setSemanticResults,
    semanticProjects,
    setSemanticProjects,
    semanticSearching,
    sessionSearchHits,
    setSessionSearchHits,
    sessionSearchMode,
    setSessionSearchMode,
    workspaceSourceId,
    workspaceJob,
    recentSessions,
    recentSessionsHasMore,
    recentSessionsTotal,
    loadingRecentSessions,
    loadingMoreRecentSessions,
    loadingMemorySessionId,
    sidebarPaneSplitPct,
    sidebarWidthPx,
    sidebarSplitContainerRef,
    visibilityFilter,
    setVisibilityFilter,
    kindFilter,
    setKindFilter,
    sessionActorFilter,
    setSessionActorFilter,
    excludeEtaMuSessions,
    setExcludeEtaMuSessions,
    statsTotal,
    statsByVisibility,
    syncingWorkspace,

    // canvas/scratchpad
    canvasTitle,
    setCanvasTitle,
    canvasSubject,
    setCanvasSubject,
    canvasPath,
    setCanvasPath,
    canvasRecipients,
    setCanvasRecipients,
    canvasCc,
    setCanvasCc,
    canvasContent,
    setCanvasContent,
    canvasStatus,
    savingCanvas,
    savingCanvasFile,
    sendingCanvas,

    // pinned context
    pinnedContext,

    // derived
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

    // layout actions
    startSidebarPaneResize,
    startSidebarWidthResize,
    toggleConsole: () => setShowConsole((value) => !value),
    toggleSettings: () => setShowSettings((value) => !value),
    toggleCanvas: () => setShowCanvas((value) => !value),

    // chat actions
    handleNewChat,
    handleSend,
    handleUndoLastTurn,
    queueLiveControl,
    voiceSteer,
    abortTurn,
    openHydrationSource,
    pinHydrationSource,

    // context/workspace actions
    loadDirectory,
    loadMoreRecentSessions,
    previewFile,
    refreshRecentSessions,
    resumeMemorySession,
    runSemanticSearch,

    // scratchpad/context actions
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
