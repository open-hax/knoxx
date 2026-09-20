import { readLastChatSettings, readStoredString, readStoredBoolean, SESSION_ACTOR_FILTER_KEY, EXCLUDE_ETA_MU_SESSIONS_KEY } from "./ChatSettingsPanel";
import type { BrowseResponse, PinnedContextItem, PreviewResponse, SemanticSearchMatch, WorkspaceJob } from "../context-bar/types";
import { readPersistedChatSessionSnapshot } from "../../lib/storage";
import { useEffect, useRef, useState, type Dispatch, type SetStateAction } from 'react';
import { getAgentContractsCatalog, getFrontendConfig, getToolCatalog, listProxxModels, proxxHealth } from '../../lib/api';
import type { ActorCatalogItem, AgentContractCatalogItem, ChatMessage, MemorySearchHit, MemorySessionSummary, ProxxModelInfo, RunDetail, RunEvent, ToolCatalogResponse } from '../../lib/types';

type UseChatPageConfigParams = {
  defaultRole: string;
  defaultActorId: string;
  activeRole: string;
  activeActorId: string;
  activeAgentId: string;
  setActiveRole: (value: string) => void;
  setActiveActorId: (value: string) => void;
  setAvailableActors: (value: ActorCatalogItem[]) => void;
  setActiveAgentId: (value: string) => void;
  setAvailableAgents: (value: AgentContractCatalogItem[]) => void;
  setToolCatalog: (value: ToolCatalogResponse | null) => void;
  setConsoleLines: (value: string[] | ((previous: string[]) => string[])) => void;
  setSttEnabled: (value: boolean) => void;
  setTtsEnabled: (value: boolean) => void;
  setTtsDefaultVoiceId: (value: string) => void;
};

export function useChatPageConfig({
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
}: UseChatPageConfigParams) {
  const activeAgentIdRef = useRef(activeAgentId);
  activeAgentIdRef.current = activeAgentId;

  useEffect(() => {
    const requestedActorId = activeActorId || defaultActorId;
    void Promise.all([getFrontendConfig(), getAgentContractsCatalog(requestedActorId)])
      .then(([config, catalog]) => {
        setSttEnabled(Boolean(config.stt_enabled));
        setTtsEnabled(Boolean(config.tts_enabled));
        setTtsDefaultVoiceId(config.tts_default_voice_id || '');

        const resolvedActorId = catalog.actor_id || requestedActorId || config.default_actor_id || defaultActorId;
        if (resolvedActorId && resolvedActorId !== activeActorId) {
          setActiveActorId(resolvedActorId);
        }

        const agents = catalog.agents ?? [];
        setAvailableActors(catalog.actors ?? []);
        const defaultAgentId = catalog.default_agent_contract || config.default_agent_contract || agents[0]?.id || '';
        setAvailableAgents(agents);

        // Read the CURRENT activeAgentId from the ref to avoid stale closure
        const currentAgentId = activeAgentIdRef.current;
        const nextAgentId = agents.some((agent) => agent.id === currentAgentId)
          ? currentAgentId
          : defaultAgentId;
        if (nextAgentId && nextAgentId !== currentAgentId) {
          setActiveAgentId(nextAgentId);
        }

        const selectedAgent = agents.find((agent) => agent.id === nextAgentId) ?? agents[0];
        setActiveRole(selectedAgent?.role || config.default_role || defaultRole);
      })
      .catch((error) => {
        setConsoleLines((previous) => [...previous.slice(-400), `[agents] failed: ${(error as Error).message}`]);
      });
  }, [activeActorId, activeAgentId, defaultActorId, defaultRole, setActiveActorId, setActiveAgentId, setActiveRole, setAvailableActors, setAvailableAgents, setConsoleLines, setSttEnabled, setTtsDefaultVoiceId, setTtsEnabled]);

  useEffect(() => {
    void getToolCatalog(activeRole, activeAgentId || undefined, activeActorId || undefined)
      .then(setToolCatalog)
      .catch((error) => {
        setConsoleLines((previous) => [...previous.slice(-400), `[tools] failed: ${(error as Error).message}`]);
      });
  }, [activeActorId, activeAgentId, activeRole, setConsoleLines, setToolCatalog]);
}

type SetState<T> = Dispatch<SetStateAction<T>>;

type UseProxxStatusPollingParams = {
  selectedModel: string;
  setSelectedModel: SetState<string>;
  setProxxReachable: SetState<boolean>;
  setProxxConfigured: SetState<boolean>;
  setProxxModels: SetState<ProxxModelInfo[]>;
};

export function useProxxStatusPolling({
  selectedModel,
  setSelectedModel,
  setProxxReachable,
  setProxxConfigured,
  setProxxModels,
}: UseProxxStatusPollingParams) {
  useEffect(() => {
    let timer: number | null = null;

    const poll = async () => {
      try {
        const status = await proxxHealth();
        setProxxReachable(Boolean(status.reachable));
        setProxxConfigured(Boolean(status.configured));
        const models = await listProxxModels();
        setProxxModels(models);
        if (!selectedModel) {
          const preferred = models.find((model) => model.id === status.default_model);
          setSelectedModel(preferred?.id ?? models[0]?.id ?? "");
        }
      } catch {
        setProxxReachable(false);
      }
    };

    void poll();
    timer = window.setInterval(() => {
      void poll();
    }, 5000);

    return () => {
      if (timer !== null) window.clearInterval(timer);
    };
  }, [selectedModel, setProxxConfigured, setProxxModels, setProxxReachable, setSelectedModel]);
}

const DEFAULT_EXCLUDED_SESSION_ACTOR = "eta-mu";

export function persistedSessionVisibleForActor(
  sessionStateKey: string,
  summary: MemorySessionSummary,
  activeActorId: string,
  visibleAgentIds: ReadonlySet<string>,
): boolean {
  return persistedSessionVisibleForFilter(sessionStateKey, summary, activeActorId, false, visibleAgentIds);
}

export function normalizedSessionActorFilter(actorId: string): string | null {
  const trimmed = actorId.trim();
  return trimmed.length > 0 && trimmed !== "all" ? trimmed : null;
}

export function excludedSessionActorIds(actorFilter: string, excludeEtaMuSessions: boolean): string[] {
  if (!excludeEtaMuSessions) return [];
  return normalizedSessionActorFilter(actorFilter) === DEFAULT_EXCLUDED_SESSION_ACTOR
    ? []
    : [DEFAULT_EXCLUDED_SESSION_ACTOR];
}

export function persistedSessionVisibleForFilter(
  sessionStateKey: string,
  summary: MemorySessionSummary,
  actorFilter: string,
  excludeEtaMuSessions: boolean,
  visibleAgentIds: ReadonlySet<string>,
): boolean {
  const snapshot = summary.active_session_id
    ? readPersistedChatSessionSnapshot(sessionStateKey, summary.active_session_id)
    : null;
  const normalizedActiveActorId = normalizedSessionActorFilter(actorFilter);

  // Remote sessions carry actor_id directly from the API; local-only drafts fall back to snapshot.
  const sessionActorId = summary.actor_id
    ?? (typeof snapshot?.activeActorId === "string" && snapshot.activeActorId.trim().length > 0
      ? snapshot.activeActorId.trim()
      : "chat_primary");

  if (excludeEtaMuSessions && normalizedActiveActorId !== DEFAULT_EXCLUDED_SESSION_ACTOR && sessionActorId === DEFAULT_EXCLUDED_SESSION_ACTOR) {
    return false;
  }

  if (!normalizedActiveActorId) {
    return true;
  }

  const sessionAgentId = typeof snapshot?.activeAgentId === "string" ? snapshot.activeAgentId.trim() : "";
  if (sessionAgentId && visibleAgentIds.size > 0) {
    return visibleAgentIds.has(sessionAgentId);
  }
  return sessionActorId === normalizedActiveActorId;
}

export function useChatWorkspaceState({ defaultRole, defaultActorId, initialShowCanvas,
  initialShowConsole, initialShowSettings, initialSidebarWidthPx,
}: { defaultRole: string; defaultActorId: string; initialShowCanvas: boolean;
  initialShowConsole: boolean; initialShowSettings: boolean; initialSidebarWidthPx: number }) {
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

  return {
    activeRole, setActiveRole,
    activeActorId, setActiveActorId,
    availableActors, setAvailableActors,
    activeAgentId, setActiveAgentId,
    availableAgents, setAvailableAgents,
    toolCatalog, setToolCatalog,
    systemPrompt, setSystemPrompt,
    sessionId, setSessionId,
    messages, setMessages,
    consoleLines, setConsoleLines,
    isSending, setIsSending,
    showConsole, setShowConsole,
    showSettings, setShowSettings,
    showCanvas, setShowCanvas,
    wsStatus, setWsStatus,
    conversationId, setConversationId,
    latestRun, setLatestRun,
    runtimeEvents, setRuntimeEvents,
    liveControlText, setLiveControlText,
    queueingControl, setQueueingControl,
    abortingTurn, setAbortingTurn,
    proxxModels, setProxxModels,
    selectedModel, setSelectedModel,
    selectedThinkingLevel, setSelectedThinkingLevel,
    proxxReachable, setProxxReachable,
    proxxConfigured, setProxxConfigured,
    sttEnabled, setSttEnabled,
    ttsEnabled, setTtsEnabled,
    ttsDefaultVoiceId, setTtsDefaultVoiceId,
    browseData, setBrowseData,
    previewData, setPreviewData,
    loadingBrowse, setLoadingBrowse,
    loadingPreview, setLoadingPreview,
    entryFilter, setEntryFilter,
    semanticQuery, setSemanticQuery,
    semanticResults, setSemanticResults,
    semanticProjects, setSemanticProjects,
    semanticSearching, setSemanticSearching,
    sessionSearchHits, setSessionSearchHits,
    sessionSearchMode, setSessionSearchMode,
    syncingWorkspace, setSyncingWorkspace,
    workspaceSourceId, setWorkspaceSourceId,
    workspaceJob, setWorkspaceJob,
    canvasTitle, setCanvasTitle,
    canvasSubject, setCanvasSubject,
    canvasPath, setCanvasPath,
    canvasRecipients, setCanvasRecipients,
    canvasCc, setCanvasCc,
    canvasContent, setCanvasContent,
    canvasStatus, setCanvasStatus,
    savingCanvas, setSavingCanvas,
    savingCanvasFile, setSavingCanvasFile,
    sendingCanvas, setSendingCanvas,
    pinnedContext, setPinnedContext,
    recentSessions, setRecentSessions,
    recentSessionsRef,
    remoteRecentSessionsRef,
    recentSessionsHasMore, setRecentSessionsHasMore,
    recentSessionsTotal, setRecentSessionsTotal,
    loadingRecentSessions, setLoadingRecentSessions,
    loadingMoreRecentSessions, setLoadingMoreRecentSessions,
    loadingMemorySessionId, setLoadingMemorySessionId,
    appliedCanvasReceiptIdsRef,
    sidebarPaneSplitPct, setSidebarPaneSplitPct,
    sidebarWidthPx, setSidebarWidthPx,
    visibilityFilter, setVisibilityFilter,
    kindFilter, setKindFilter,
    sessionActorFilter, setSessionActorFilter,
    excludeEtaMuSessions, setExcludeEtaMuSessions,
    sendTimeoutRef,
    pendingAssistantIdRef,
    activeRunIdRef,
    lastAppliedAgentIdRef,
    sidebarSplitContainerRef,
  };
}

export type ChatWorkspaceState = ReturnType<typeof useChatWorkspaceState>;

/** Select only the state fields owned by a consumer contract. */
export function selectChatWorkspaceState<Key extends keyof ChatWorkspaceState>(
  state: ChatWorkspaceState, fields: readonly Key[],
): Pick<ChatWorkspaceState, Key> {
  const selected = {} as Pick<ChatWorkspaceState, Key>;
  for (const field of fields) selected[field] = state[field];
  return selected;
}
