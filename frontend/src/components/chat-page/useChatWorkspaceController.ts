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
  ChatMessage,
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
const DEFAULT_ROLE = "executive";
const SEND_UI_GUARD_TIMEOUT_MS = 30 * 60 * 1000;

export type ChatWorkspaceControllerOptions = {
  initialShowCanvas?: boolean;
  initialShowConsole?: boolean;
  initialShowSettings?: boolean;
  initialSidebarWidthPx?: number;
  defaultRole?: string;
  sessionIdKey?: string;
  scratchpadStorageKey?: string;
  pinnedContextStorageKey?: string;
  sessionStateKey?: string;
  sidebarWidthKey?: string;
  sendUiGuardTimeoutMs?: number;
};

export function useChatWorkspaceController(options: ChatWorkspaceControllerOptions = {}) {
  const {
    initialShowCanvas = true,
    initialShowConsole = false,
    initialShowSettings = false,
    initialSidebarWidthPx = 320,
    defaultRole = DEFAULT_ROLE,
    sessionIdKey = SESSION_ID_KEY,
    scratchpadStorageKey = SCRATCHPAD_STATE_KEY,
    pinnedContextStorageKey = PINNED_CONTEXT_KEY,
    sessionStateKey = CHAT_SESSION_STATE_KEY,
    sidebarWidthKey = CHAT_SIDEBAR_WIDTH_KEY,
    sendUiGuardTimeoutMs = SEND_UI_GUARD_TIMEOUT_MS,
  } = options;

  const [activeRole, setActiveRole] = useState(defaultRole);
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
  const [proxxModels, setProxxModels] = useState<ProxxModelInfo[]>([]);
  const [selectedModel, setSelectedModel] = useState("");
  const [proxxReachable, setProxxReachable] = useState(false);
  const [proxxConfigured, setProxxConfigured] = useState(false);
  const [browseData, setBrowseData] = useState<BrowseResponse | null>(null);
  const [previewData, setPreviewData] = useState<PreviewResponse | null>(null);
  const [loadingBrowse, setLoadingBrowse] = useState(false);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [entryFilter, setEntryFilter] = useState("");
  const [semanticQuery, setSemanticQuery] = useState("");
  const [semanticResults, setSemanticResults] = useState<SemanticSearchMatch[]>([]);
  const [semanticProjects, setSemanticProjects] = useState<string[]>([]);
  const [semanticSearching, setSemanticSearching] = useState(false);
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
  const [statsTotal, setStatsTotal] = useState(0);
  const [statsByVisibility, setStatsByVisibility] = useState<Record<string, number>>({});
  const sendTimeoutRef = useRef<number | null>(null);
  const pendingAssistantIdRef = useRef<string | null>(null);
  const activeRunIdRef = useRef<string | null>(null);
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
    workspaceProgressPercent,
  } = useChatPageDerivedState({
    browseData,
    entryFilter,
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
    loadRunDetail,
    queueLiveControl,
    updateMessageById,
    updateTraceBlocksByMessageId,
  } = createChatRuntimeActions({
    makeId,
    systemPrompt,
    sessionId,
    setSessionId,
    conversationId,
    setConversationId,
    selectedModel,
    liveControlEnabled,
    liveControlText,
    setLiveControlText,
    setMessages,
    setLatestRun,
    setRuntimeEvents,
    setIsSending,
    setConsoleLines,
    setQueueingControl,
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
    currentPath,
    showFiles: true,
    browseData,
    semanticQuery,
    setBrowseData,
    setPreviewData,
    setLoadingBrowse,
    setLoadingPreview,
    setSemanticResults,
    setSemanticProjects,
    setSemanticSearching,
    setSyncingWorkspace,
    setWorkspaceSourceId,
    setWorkspaceJob,
    recentSessionsRef,
    setRecentSessions,
    setRecentSessionsHasMore,
    setRecentSessionsTotal,
    setLoadingRecentSessions,
    setLoadingMoreRecentSessions,
    setLoadingMemorySessionId,
    setMessages,
    setConversationId,
    setLatestRun,
    setRuntimeEvents,
    setLiveControlText,
    setIsSending,
    setConsoleLines,
    pendingAssistantIdRef,
    activeRunIdRef,
    fetchPreviewData,
    loadRunDetail,
    defaultSyncIntervalMinutes: DEFAULT_SYNC_INTERVAL_MINUTES,
    defaultFileTypes: DEFAULT_FILE_TYPES,
    defaultExcludePatterns: DEFAULT_EXCLUDE_PATTERNS,
  });

  useChatPageConfig({
    defaultRole,
    activeRole,
    setActiveRole,
    setToolCatalog,
    setConsoleLines,
  });

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

  return {
    // pane visibility
    showConsole,
    setShowConsole,
    showSettings,
    setShowSettings,
    showCanvas,
    setShowCanvas,

    // chat runtime state
    activeRole,
    setActiveRole,
    toolCatalog,
    systemPrompt,
    setSystemPrompt,
    sessionId,
    messages,
    consoleLines,
    isSending,
    wsStatus,
    conversationId,
    latestRun,
    activeRunId: latestRun?.run_id ?? activeRunIdRef.current ?? null,
    runtimeEvents,
    liveControlText,
    setLiveControlText,
    queueingControl,
    proxxModels,
    selectedModel,
    setSelectedModel,
    proxxReachable,
    proxxConfigured,
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
    queueLiveControl,
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
