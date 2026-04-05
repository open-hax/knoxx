import { useEffect, useMemo, useRef, useState, type MouseEvent as ReactMouseEvent } from "react";
import { Button } from "@open-hax/uxx";
import { ChatMainPane } from "../components/chat-page/ChatMainPane";
import { ChatWorkspaceSidebar } from "../components/chat-page/ChatWorkspaceSidebar";
import { createChatRuntimeActions } from "../components/chat-page/chat-runtime-actions";
import { useChatRuntimeEffects } from "../components/chat-page/chat-runtime-effects";
import { createChatScratchpadActions } from "../components/chat-page/scratchpad-actions";
import { createChatWorkspaceActions } from "../components/chat-page/workspace-actions";
import {
  getChatStorage,
  useChatSessionPersistence,
  useChatSessionRecovery,
  usePinnedContextPersistence,
  useProxxStatusPolling,
  useScratchpadPersistence,
} from "../components/chat-page/hooks";
import {
  getFrontendConfig,
  getToolCatalog,
} from "../lib/api";
import type {
  ChatMessage,
  ChatTraceBlock,
  FrontendConfig,
  GroundedContextRow,
  MemorySessionSummary,
  ProxxModelInfo,
  RunDetail,
  RunEvent,
  ToolReceipt,
  ToolCatalogResponse,
} from "../lib/types";
import type {
  BrowseResponse,
  IngestionSource,
  PinnedContextItem,
  PreviewResponse,
  SemanticSearchMatch,
  SemanticSearchResponse,
  WorkspaceJob,
} from "../components/chat-page/types";
import {
  latestRunHydrationSources,
  parentPath,
} from "../components/chat-page/utils";

const SESSION_ID_KEY = "knoxx_session_id";
const SCRATCHPAD_STATE_KEY = "knoxx_scratchpad_state";
const PINNED_CONTEXT_KEY = "knoxx_pinned_context";
const CHAT_SESSION_STATE_KEY = "knoxx_chat_session_state";
const CHAT_SIDEBAR_WIDTH_KEY = "knoxx_chat_sidebar_width_px";

const DEFAULT_ROLE = "executive";
const DEFAULT_SYNC_INTERVAL_MINUTES = 30;
const SEND_UI_GUARD_TIMEOUT_MS = 30 * 60 * 1000;
const DEFAULT_FILE_TYPES = [
  ".md", ".markdown", ".txt", ".rst", ".org", ".adoc",
  ".json", ".jsonl", ".yaml", ".yml", ".toml", ".ini", ".cfg", ".conf", ".env",
  ".xml", ".csv", ".tsv", ".html", ".htm", ".css",
  ".js", ".jsx", ".ts", ".tsx", ".py", ".rb", ".php", ".java", ".kt", ".go", ".rs",
  ".c", ".cc", ".cpp", ".h", ".hpp", ".clj", ".cljs", ".cljc", ".edn", ".sql", ".sh",
  ".bash", ".zsh", ".fish", ".tex", ".bib", ".nix", ".dockerfile", ".gradle", ".properties",
];
const DEFAULT_EXCLUDE_PATTERNS = [
  "**/.git/**",
  "**/node_modules/**",
  "**/dist/**",
  "**/coverage/**",
  "**/.next/**",
  "**/.venv/**",
  "**/venv/**",
  "**/__pycache__/**",
  "**/*.png",
  "**/*.jpg",
  "**/*.jpeg",
  "**/*.gif",
  "**/*.pdf",
  "**/*.zip",
  "**/*.tar.gz",
];
function makeId(): string {
  const maybeCrypto = globalThis.crypto as Crypto | undefined;
  if (maybeCrypto && typeof maybeCrypto.randomUUID === "function") {
    return maybeCrypto.randomUUID();
  }
  return `id-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function ChatPage() {
  const [frontendConfig, setFrontendConfig] = useState<FrontendConfig | null>(null);
  const [activeRole, setActiveRole] = useState(DEFAULT_ROLE);
  const [toolCatalog, setToolCatalog] = useState<ToolCatalogResponse | null>(null);
  const [systemPrompt, setSystemPrompt] = useState("");
  const [sessionId, setSessionId] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [consoleLines, setConsoleLines] = useState<string[]>([]);
  const [isSending, setIsSending] = useState(false);
  const [showConsole, setShowConsole] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [showFiles, setShowFiles] = useState(true);
  const [showCanvas, setShowCanvas] = useState(true);
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
  const [canvasTitle, setCanvasTitle] = useState("Untitled scratchpad");
  const [canvasSubject, setCanvasSubject] = useState("");
  const [canvasPath, setCanvasPath] = useState("notes/scratchpads/untitled-scratchpad.md");
  const [canvasRecipients, setCanvasRecipients] = useState("");
  const [canvasCc, setCanvasCc] = useState("");
  const [canvasContent, setCanvasContent] = useState("");
  const [canvasStatus, setCanvasStatus] = useState<string | null>(null);
  const [savingCanvas, setSavingCanvas] = useState(false);
  const [savingCanvasFile, setSavingCanvasFile] = useState(false);
  const [sendingCanvas, setSendingCanvas] = useState(false);
  const [pinnedContext, setPinnedContext] = useState<PinnedContextItem[]>([]);
  const [recentSessions, setRecentSessions] = useState<MemorySessionSummary[]>([]);
  const [loadingRecentSessions, setLoadingRecentSessions] = useState(false);
  const [loadingMemorySessionId, setLoadingMemorySessionId] = useState<string | null>(null);
  const [sidebarPaneSplitPct, setSidebarPaneSplitPct] = useState(50);
  const [sidebarWidthPx, setSidebarWidthPx] = useState(320);
  const sendTimeoutRef = useRef<number | null>(null);
  const pendingAssistantIdRef = useRef<string | null>(null);
  const activeRunIdRef = useRef<string | null>(null);
  const sidebarSplitContainerRef = useRef<HTMLDivElement | null>(null);
  const isRecovering = useChatSessionRecovery({
    sessionId,
    sessionStateKey: CHAT_SESSION_STATE_KEY,
    pendingAssistantIdRef,
    activeRunIdRef,
    setConversationId,
    setIsSending,
    setLatestRun,
    setConsoleLines,
  });

  const currentPath = browseData?.current_path ?? "";
  const currentParentPath = useMemo(() => parentPath(currentPath), [currentPath]);

  const filteredEntries = useMemo(() => {
    const entries = browseData?.entries ?? [];
    const query = entryFilter.trim().toLowerCase();
    if (!query) return entries;
    return entries.filter((entry) =>
      entry.name.toLowerCase().includes(query) || entry.path.toLowerCase().includes(query),
    );
  }, [browseData?.entries, entryFilter]);

  const semanticMode = semanticQuery.trim().length > 0;
  const activeEntryCount = semanticMode ? semanticResults.length : filteredEntries.length;
  const workspaceProgressPercent = workspaceJob && workspaceJob.total_files > 0
    ? Math.min(100, Math.round(((workspaceJob.processed_files + workspaceJob.failed_files) / workspaceJob.total_files) * 100))
    : 0;
  const latestToolReceipts = useMemo(() => (latestRun?.tool_receipts ?? []) as ToolReceipt[], [latestRun]);

  // Get live tool receipts for the current streaming message
  const liveToolReceipts = useMemo(() => {
    if (!isSending || !pendingAssistantIdRef.current) return [];
    return latestToolReceipts;
  }, [isSending, latestToolReceipts]);

  // Get runtime events for live tool updates
  const liveToolEvents = useMemo(() => {
    if (!isSending) return [];
    return runtimeEvents.filter((event) =>
      ["tool_start", "tool_update", "tool_end"].includes(String(event.type ?? ""))
    );
  }, [isSending, runtimeEvents]);

  const liveControlEnabled = Boolean(
    isSending
      && conversationId
      && runtimeEvents.some((event) => ["run_started", "passive_hydration", "assistant_first_token", "tool_start"].includes(String(event.type ?? ""))),
  );
  const hydrationSources = useMemo(() => latestRunHydrationSources(latestRun), [latestRun]);
  const assistantSurfaceBackground = "var(--token-colors-background-surface)";
  const assistantSurfaceBorder = "var(--token-colors-border-default)";
  const assistantSurfaceText = "var(--token-colors-text-default)";

  useChatSessionPersistence({
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
    sessionIdKey: SESSION_ID_KEY,
    sessionStateKey: CHAT_SESSION_STATE_KEY,
    sidebarWidthKey: CHAT_SIDEBAR_WIDTH_KEY,
  });

  useScratchpadPersistence({
    storageKey: SCRATCHPAD_STATE_KEY,
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
  });

  usePinnedContextPersistence({
    storageKey: PINNED_CONTEXT_KEY,
    pinnedContext,
    setPinnedContext,
  });

  useProxxStatusPolling({
    selectedModel,
    setSelectedModel,
    setProxxReachable,
    setProxxConfigured,
    setProxxModels,
  });

  const {
    appendToScratchpad,
    clearScratchpad,
    fetchPreviewData,
    insertPinnedIntoCanvas,
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
    sessionIdKey: SESSION_ID_KEY,
    sessionStateKey: CHAT_SESSION_STATE_KEY,
  });

  const {
    ensureWorkspaceSync,
    loadDirectory,
    previewFile,
    refreshRecentSessions,
    refreshWorkspaceStatus,
    resumeMemorySession,
    runSemanticSearch,
  } = createChatWorkspaceActions({
    currentPath,
    showFiles,
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
    setRecentSessions,
    setLoadingRecentSessions,
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

  useEffect(() => {
    void getFrontendConfig()
      .then((config) => {
        setFrontendConfig(config);
        setActiveRole(config.default_role || DEFAULT_ROLE);
      })
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    void getToolCatalog(activeRole)
      .then(setToolCatalog)
      .catch((error) => {
        setConsoleLines((prev) => [...prev.slice(-400), `[tools] failed: ${(error as Error).message}`]);
      });
  }, [activeRole]);

  useChatRuntimeEffects({
    sessionId,
    isSending,
    latestRun,
    semanticQuery,
    currentPath,
    sendUiGuardTimeoutMs: SEND_UI_GUARD_TIMEOUT_MS,
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

  function pinHydrationSource(source: { title: string; path: string; section?: string }) {
    pinContextItem({
      id: source.path,
      title: source.title,
      path: source.path,
      snippet: source.section,
      kind: "semantic",
    });
  }

  async function openHydrationSource(source: { path: string }) {
    await previewFile(source.path);
  }

  function startSidebarPaneResize(event: ReactMouseEvent<HTMLDivElement>) {
    event.preventDefault();
    const container = sidebarSplitContainerRef.current;
    if (!container) return;

    const rect = container.getBoundingClientRect();
    document.body.style.cursor = "row-resize";
    document.body.style.userSelect = "none";

    const onMove = (moveEvent: MouseEvent) => {
      const deltaY = moveEvent.clientY - rect.top;
      const nextPct = Math.min(75, Math.max(25, (deltaY / rect.height) * 100));
      setSidebarPaneSplitPct(nextPct);
    };

    const onUp = () => {
      window.removeEventListener("mousemove", onMove);
      window.removeEventListener("mouseup", onUp);
      document.body.style.cursor = "";
      document.body.style.userSelect = "";
    };

    window.addEventListener("mousemove", onMove);
    window.addEventListener("mouseup", onUp);
  }

  function startSidebarWidthResize(event: ReactMouseEvent<HTMLDivElement>) {
    event.preventDefault();

    const startX = event.clientX;
    const startWidth = sidebarWidthPx;
    document.body.style.cursor = "col-resize";
    document.body.style.userSelect = "none";

    const onMove = (moveEvent: MouseEvent) => {
      const deltaX = moveEvent.clientX - startX;
      const maxWidth = Math.min(640, Math.floor(window.innerWidth * 0.55));
      const nextWidth = Math.min(maxWidth, Math.max(260, startWidth + deltaX));
      setSidebarWidthPx(nextWidth);
    };

    const onUp = () => {
      window.removeEventListener("mousemove", onMove);
      window.removeEventListener("mouseup", onUp);
      document.body.style.cursor = "";
      document.body.style.userSelect = "";
    };

    window.addEventListener("mousemove", onMove);
    window.addEventListener("mouseup", onUp);
  }

  return (
    <div
      style={{
        display: "flex",
        height: "calc(100vh - 96px)",
        gap: 0,
        minHeight: 0,
        background:
          "radial-gradient(circle at top left, var(--token-colors-alpha-green-_14) 0%, transparent 28%), radial-gradient(circle at bottom right, var(--token-colors-alpha-orange-_12) 0%, transparent 24%), linear-gradient(180deg, var(--token-monokai-bg-default) 0%, var(--token-monokai-bg-darker) 100%)",
      }}
    >
      {showFiles ? (
        <ChatWorkspaceSidebar
          sidebarWidthPx={sidebarWidthPx}
          sidebarPaneSplitPct={sidebarPaneSplitPct}
          sidebarSplitContainerRef={sidebarSplitContainerRef}
          currentPath={currentPath}
          currentParentPath={currentParentPath}
          browseData={browseData}
          previewData={previewData}
          loadingBrowse={loadingBrowse}
          loadingPreview={loadingPreview}
          entryFilter={entryFilter}
          semanticQuery={semanticQuery}
          semanticResults={semanticResults}
          semanticProjects={semanticProjects}
          semanticSearching={semanticSearching}
          semanticMode={semanticMode}
          filteredEntries={filteredEntries}
          activeEntryCount={activeEntryCount}
          syncingWorkspace={syncingWorkspace}
          workspaceSourceId={workspaceSourceId}
          workspaceJob={workspaceJob}
          workspaceProgressPercent={workspaceProgressPercent}
          pinnedContext={pinnedContext}
          recentSessions={recentSessions}
          loadingRecentSessions={loadingRecentSessions}
          loadingMemorySessionId={loadingMemorySessionId}
          conversationId={conversationId}
          onHide={() => setShowFiles(false)}
          onLoadDirectory={loadDirectory}
          onEntryFilterChange={setEntryFilter}
          onSemanticQueryChange={setSemanticQuery}
          onSemanticSearch={() => runSemanticSearch(semanticQuery)}
          onClearSemanticSearch={() => {
            setSemanticQuery("");
            setSemanticResults([]);
            setSemanticProjects([]);
          }}
          onEnsureWorkspaceSync={ensureWorkspaceSync}
          onRefreshRecentSessions={refreshRecentSessions}
          onResumeMemorySession={resumeMemorySession}
          onPreviewFile={previewFile}
          onPinSemanticResult={pinSemanticResult}
          onAppendToScratchpad={appendToScratchpad}
          onPinPreviewContext={pinPreviewContext}
          onOpenPreviewInCanvas={openPreviewInCanvas}
          onOpenPinnedInCanvas={openPinnedInCanvas}
          onInsertPinnedIntoCanvas={insertPinnedIntoCanvas}
          onUnpinContextItem={unpinContextItem}
          onStartSidebarPaneResize={startSidebarPaneResize}
          onStartSidebarWidthResize={startSidebarWidthResize}
        />
      ) : null}

      <ChatMainPane
        showFiles={showFiles}
        showSettings={showSettings}
        showCanvas={showCanvas}
        showConsole={showConsole}
        onShowFiles={() => setShowFiles(true)}
        onToggleSettings={() => setShowSettings((value) => !value)}
        onToggleCanvas={() => setShowCanvas((value) => !value)}
        onToggleConsole={() => setShowConsole((value) => !value)}
        selectedModel={selectedModel}
        onSelectedModelChange={setSelectedModel}
        proxxModels={proxxModels}
        proxxReachable={proxxReachable}
        proxxConfigured={proxxConfigured}
        syncingWorkspace={syncingWorkspace}
        onEnsureWorkspaceSync={ensureWorkspaceSync}
        onNewChat={handleNewChat}
        systemPrompt={systemPrompt}
        onSystemPromptChange={setSystemPrompt}
        conversationId={conversationId}
        activeRole={activeRole}
        onActiveRoleChange={setActiveRole}
        toolCatalog={toolCatalog}
        wsStatus={wsStatus}
        isRecovering={isRecovering}
        latestRun={latestRun}
        isSending={isSending}
        liveControlEnabled={liveControlEnabled}
        liveControlText={liveControlText}
        onLiveControlTextChange={setLiveControlText}
        queueingControl={queueingControl}
        onQueueLiveControl={queueLiveControl}
        activeRunId={latestRun?.run_id ?? activeRunIdRef.current ?? null}
        hydrationSources={hydrationSources}
        runtimeEvents={runtimeEvents}
        latestToolReceipts={latestToolReceipts}
        liveToolReceipts={liveToolReceipts}
        liveToolEvents={liveToolEvents}
        assistantSurfaceBackground={assistantSurfaceBackground}
        assistantSurfaceBorder={assistantSurfaceBorder}
        assistantSurfaceText={assistantSurfaceText}
        messages={messages}
        consoleLines={consoleLines}
        onSend={handleSend}
        composerDisabled={isSending || isRecovering || !selectedModel}
        onOpenHydrationSource={openHydrationSource}
        onPinHydrationSource={pinHydrationSource}
        onAppendToScratchpad={appendToScratchpad}
        onOpenMessageInCanvas={openMessageInCanvas}
        onOpenSourceInPreview={openSourceInPreview}
        onPinAssistantSource={pinAssistantSource}
        onPinMessageContext={pinMessageContext}
        canvasTitle={canvasTitle}
        onCanvasTitleChange={setCanvasTitle}
        canvasPath={canvasPath}
        onCanvasPathChange={setCanvasPath}
        canvasSubject={canvasSubject}
        onCanvasSubjectChange={setCanvasSubject}
        canvasRecipients={canvasRecipients}
        onCanvasRecipientsChange={setCanvasRecipients}
        canvasCc={canvasCc}
        onCanvasCcChange={setCanvasCc}
        canvasContent={canvasContent}
        onCanvasContentChange={setCanvasContent}
        canvasStatus={canvasStatus}
        savingCanvas={savingCanvas}
        savingCanvasFile={savingCanvasFile}
        sendingCanvas={sendingCanvas}
        onUseLatestAssistantInCanvas={useLatestAssistantInCanvas}
        onSaveCanvasDraft={saveCanvasDraft}
        onSaveCanvasFile={saveCanvasFile}
        onClearScratchpad={clearScratchpad}
        onSendCanvasEmailAction={sendCanvasEmailAction}
      />

    </div>
  );
}

export default ChatPage;
