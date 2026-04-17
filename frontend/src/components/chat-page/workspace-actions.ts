import type { Dispatch, MutableRefObject, SetStateAction } from "react";
import { getMemorySession, listMemorySessions } from "../../lib/api";
import type { ChatMessage, MemorySessionSummary, RunDetail, RunEvent } from "../../lib/types";
import { findPersistedChatSessionByConversation, listPersistedChatSessions } from "./hooks";
import type {
  BrowseResponse,
  IngestionSource,
  PreviewResponse,
  SemanticSearchMatch,
  SemanticSearchResponse,
  WorkspaceJob,
} from "./types";
import { isWorkspaceSource, memoryRowRunId, memoryRowsToMessages, selectWorkspaceJob } from "./utils";

type SetState<T> = Dispatch<SetStateAction<T>>;

const RECENT_SESSION_PAGE_SIZE = 20;

function mergeSessionPages(primary: MemorySessionSummary[], secondary: MemorySessionSummary[]): MemorySessionSummary[] {
  const statusScore = (item: MemorySessionSummary): number => {
    if (item.has_active_stream) return 50;
    const status = typeof item.active_status === "string" ? item.active_status : "";
    if (status === "running") return 40;
    if (status === "queued") return 35;
    if (status === "waiting_input") return 30;
    if (status === "failed") return 20;
    if (status === "completed") return 10;
    if (item.is_active) return 5;
    return 0;
  };

  const parseTs = (value?: string | null): number => {
    if (!value) return 0;
    const parsed = Date.parse(value);
    return Number.isFinite(parsed) ? parsed : 0;
  };

  const mergeEntry = (left: MemorySessionSummary, right: MemorySessionSummary): MemorySessionSummary => {
    const leftScore = statusScore(left);
    const rightScore = statusScore(right);
    const live = leftScore >= rightScore ? left : right;
    const lastTs = parseTs(left.last_ts ?? null) >= parseTs(right.last_ts ?? null) ? left.last_ts : right.last_ts;

    return {
      session: left.session,
      title: left.title || right.title,
      title_model: left.title_model ?? right.title_model ?? null,
      last_ts: lastTs,
      event_count: Math.max(left.event_count ?? 0, right.event_count ?? 0),

      // Liveness should always prefer the most-active view (local snapshot or Redis-enriched remote row).
      is_active: Boolean(live.is_active),
      active_status: live.active_status ?? left.active_status ?? right.active_status,
      has_active_stream: Boolean(live.has_active_stream),

      // Prefer the active session id from whichever side is live; otherwise keep any known id.
      active_session_id: live.active_session_id ?? left.active_session_id ?? right.active_session_id ?? null,

      // Only truly local-only if both sides are local-only.
      local_only: Boolean(left.local_only) && Boolean(right.local_only),
    };
  };

  const byId = new Map<string, MemorySessionSummary>();
  for (const item of primary) {
    byId.set(item.session, item);
  }
  for (const item of secondary) {
    const existing = byId.get(item.session);
    byId.set(item.session, existing ? mergeEntry(existing, item) : item);
  }
  return [...byId.values()];
}

function sortSessions(items: MemorySessionSummary[]): MemorySessionSummary[] {
  return [...items].sort((left, right) => {
    const leftTime = Date.parse(left.last_ts ?? "") || 0;
    const rightTime = Date.parse(right.last_ts ?? "") || 0;
    if (rightTime !== leftTime) {
      return rightTime - leftTime;
    }
    if (left.is_active !== right.is_active) {
      return left.is_active ? -1 : 1;
    }
    return (left.title ?? left.session).localeCompare(right.title ?? right.session);
  });
}

type ChatWorkspaceActionParams = {
  currentPath: string;
  showFiles: boolean;
  browseData: BrowseResponse | null;
  semanticQuery: string;
  setBrowseData: SetState<BrowseResponse | null>;
  setPreviewData: SetState<PreviewResponse | null>;
  setLoadingBrowse: SetState<boolean>;
  setLoadingPreview: SetState<boolean>;
  setSemanticResults: SetState<SemanticSearchMatch[]>;
  setSemanticProjects: SetState<string[]>;
  setSemanticSearching: SetState<boolean>;
  setSyncingWorkspace: SetState<boolean>;
  setWorkspaceSourceId: SetState<string | null>;
  setWorkspaceJob: SetState<WorkspaceJob | null>;
  recentSessionsRef: MutableRefObject<MemorySessionSummary[]>;
  remoteRecentSessionsRef: MutableRefObject<MemorySessionSummary[]>;
  setRecentSessions: SetState<MemorySessionSummary[]>;
  setRecentSessionsHasMore: SetState<boolean>;
  setRecentSessionsTotal: SetState<number>;
  setLoadingRecentSessions: SetState<boolean>;
  setLoadingMoreRecentSessions: SetState<boolean>;
  setLoadingMemorySessionId: SetState<string | null>;
  setMessages: SetState<ChatMessage[]>;
  setSessionId: SetState<string>;
  setConversationId: SetState<string | null>;
  setLatestRun: SetState<RunDetail | null>;
  setRuntimeEvents: SetState<RunEvent[]>;
  setLiveControlText: SetState<string>;
  setIsSending: SetState<boolean>;
  setConsoleLines: SetState<string[]>;
  pendingAssistantIdRef: MutableRefObject<string | null>;
  activeRunIdRef: MutableRefObject<string | null>;
  makeId: () => string;
  sessionStateKey: string;
  fetchPreviewData: (path: string) => Promise<PreviewResponse>;
  loadRunDetail: (runId: string) => void | Promise<void>;
  defaultSyncIntervalMinutes: number;
  defaultFileTypes: string[];
  defaultExcludePatterns: string[];
};

export function createChatWorkspaceActions({
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
  recentSessionsRef,
  remoteRecentSessionsRef,
  setRecentSessions,
  setRecentSessionsHasMore,
  setRecentSessionsTotal,
  setLoadingRecentSessions,
  setLoadingMoreRecentSessions,
  setLoadingMemorySessionId,
  setMessages,
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
  defaultSyncIntervalMinutes,
  defaultFileTypes,
  defaultExcludePatterns,
}: ChatWorkspaceActionParams) {
  const appendConsoleLine = (line: string) => {
    setConsoleLines((prev) => [...prev.slice(-400), line]);
  };

  const loadDirectory = async (path = "") => {
    setLoadingBrowse(true);
    try {
      const params = new URLSearchParams();
      if (path) params.set("path", path);
      const response = await fetch(`/api/ingestion/browse?${params.toString()}`);
      if (!response.ok) throw new Error(`Browse failed: ${response.status}`);
      const data = (await response.json()) as BrowseResponse;
      setBrowseData(data);
      setPreviewData(null);
    } catch (error) {
      appendConsoleLine(`[browse] failed: ${(error as Error).message}`);
    } finally {
      setLoadingBrowse(false);
    }
  };

  const refreshWorkspaceStatus = async () => {
    try {
      const sourcesResponse = await fetch("/api/ingestion/sources");
      if (!sourcesResponse.ok) return;
      const sources = (await sourcesResponse.json()) as IngestionSource[];
      const source = sources.find(isWorkspaceSource) ?? null;
      setWorkspaceSourceId(source?.source_id ?? null);
      if (!source) {
        setWorkspaceJob(null);
        return;
      }

      const jobsResponse = await fetch(`/api/ingestion/jobs?source_id=${encodeURIComponent(source.source_id)}&limit=10`);
      if (!jobsResponse.ok) return;
      const jobs = (await jobsResponse.json()) as WorkspaceJob[];
      setWorkspaceJob(selectWorkspaceJob(jobs));
      if (showFiles && browseData) {
        void loadDirectory(currentPath);
      }
    } catch (error) {
      appendConsoleLine(`[ingestion] status failed: ${(error as Error).message}`);
    }
  };

  const runSemanticSearch = async (query: string, path = currentPath) => {
    const trimmed = query.trim();
    if (!trimmed) {
      setSemanticResults([]);
      setSemanticProjects([]);
      return;
    }

    setSemanticSearching(true);
    try {
      const response = await fetch("/api/ingestion/search", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ q: trimmed, role: "workspace", path, limit: 30 }),
      });
      if (!response.ok) throw new Error(`Semantic search failed: ${response.status}`);
      const data = (await response.json()) as SemanticSearchResponse;
      setSemanticResults(data.rows);
      setSemanticProjects(data.projects);
    } catch (error) {
      appendConsoleLine(`[semantic] failed: ${(error as Error).message}`);
    } finally {
      setSemanticSearching(false);
    }
  };

  const previewFile = async (path: string) => {
    setLoadingPreview(true);
    try {
      const data = await fetchPreviewData(path);
      setPreviewData(data);
    } catch (error) {
      appendConsoleLine(`[preview] failed: ${(error as Error).message}`);
    } finally {
      setLoadingPreview(false);
    }
  };

  const ensureWorkspaceSync = async () => {
    setSyncingWorkspace(true);
    try {
      const sourcesResponse = await fetch("/api/ingestion/sources");
      if (!sourcesResponse.ok) throw new Error(`Failed to list sources: ${sourcesResponse.status}`);
      const sources = (await sourcesResponse.json()) as IngestionSource[];
      let source = sources.find(isWorkspaceSource);

      if (!source) {
        const createResponse = await fetch("/api/ingestion/sources", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            driver_type: "local",
            name: "devel workspace",
            config: {
              root_path: "/app/workspace/devel",
              sync_interval_minutes: defaultSyncIntervalMinutes,
            },
            collections: ["devel"],
            file_types: defaultFileTypes,
            exclude_patterns: defaultExcludePatterns,
          }),
        });
        if (!createResponse.ok) throw new Error(`Failed to create source: ${createResponse.status}`);
        const createdSource = (await createResponse.json()) as IngestionSource;
        source = createdSource;
        appendConsoleLine(`[ingestion] created source ${createdSource.source_id} for devel workspace`);
      }

      if (!source) throw new Error("Failed to resolve devel workspace source");
      setWorkspaceSourceId(source.source_id);

      const jobResponse = await fetch("/api/ingestion/jobs", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ source_id: source.source_id }),
      });
      if (!jobResponse.ok) throw new Error(`Failed to start sync: ${jobResponse.status}`);
      const job = (await jobResponse.json()) as { job_id: string };
      appendConsoleLine(`[ingestion] queued devel workspace sync job ${job.job_id} (interval ${defaultSyncIntervalMinutes}m)`);
      void refreshWorkspaceStatus();
    } catch (error) {
      appendConsoleLine(`[ingestion] sync failed: ${(error as Error).message}`);
    } finally {
      setSyncingWorkspace(false);
    }
  };

  const refreshRecentSessions = async () => {
    setLoadingRecentSessions(true);
    try {
      const page = await listMemorySessions({ limit: RECENT_SESSION_PAGE_SIZE, offset: 0 });
      const preservedTail = remoteRecentSessionsRef.current.filter((item) => !page.rows.some((row) => row.session === item.session));
      const remoteMerged = mergeSessionPages(page.rows, preservedTail);
      remoteRecentSessionsRef.current = remoteMerged;
      const merged = sortSessions(mergeSessionPages(remoteMerged, listPersistedChatSessions(sessionStateKey)));
      recentSessionsRef.current = merged;
      setRecentSessions(merged);
      const remoteTotal = typeof page.total === "number" ? page.total : remoteMerged.length;
      setRecentSessionsTotal(Math.max(remoteTotal, merged.length));
      setRecentSessionsHasMore(
        typeof page.total === "number"
          ? remoteMerged.length < page.total
          : Boolean(page.has_more ?? page.rows.length >= RECENT_SESSION_PAGE_SIZE),
      );
    } catch (error) {
      appendConsoleLine(`[memory] failed to load recent sessions: ${(error as Error).message}`);
    } finally {
      setLoadingRecentSessions(false);
    }
  };

  const loadMoreRecentSessions = async () => {
    setLoadingMoreRecentSessions(true);
    try {
      const page = await listMemorySessions({ limit: RECENT_SESSION_PAGE_SIZE, offset: remoteRecentSessionsRef.current.length });
      const remoteMerged = mergeSessionPages(remoteRecentSessionsRef.current, page.rows);
      remoteRecentSessionsRef.current = remoteMerged;
      const merged = sortSessions(mergeSessionPages(remoteMerged, listPersistedChatSessions(sessionStateKey)));
      recentSessionsRef.current = merged;
      setRecentSessions(merged);
      const remoteTotal = typeof page.total === "number" ? page.total : remoteMerged.length;
      setRecentSessionsTotal(Math.max(remoteTotal, merged.length));
      setRecentSessionsHasMore(
        typeof page.total === "number"
          ? remoteMerged.length < page.total
          : Boolean(page.has_more ?? page.rows.length >= RECENT_SESSION_PAGE_SIZE),
      );
    } catch (error) {
      appendConsoleLine(`[memory] failed to load more sessions: ${(error as Error).message}`);
    } finally {
      setLoadingMoreRecentSessions(false);
    }
  };

  const resumeMemorySession = async (sessionKey: string) => {
    setLoadingMemorySessionId(sessionKey);
    try {
      const localSession = findPersistedChatSessionByConversation(sessionStateKey, sessionKey);
      const remoteSession = recentSessionsRef.current.find((entry) => entry.session === sessionKey) ?? null;
      const resolvedSessionId = localSession?.active_session_id ?? remoteSession?.active_session_id ?? makeId();

      setMessages([]);
      setConversationId(sessionKey);
      setSessionId(resolvedSessionId);
      setLatestRun(null);
      setRuntimeEvents([]);
      setLiveControlText("");
      setIsSending(false);
      pendingAssistantIdRef.current = null;
      activeRunIdRef.current = null;

      if (localSession?.local_only) {
        appendConsoleLine(`[memory] resumed local draft ${sessionKey}`);
        return;
      }

      const detail = await getMemorySession(sessionKey);
      const transcript = memoryRowsToMessages(detail.rows).slice(-80);
      const lastRunId = [...detail.rows].reverse().map(memoryRowRunId).find((value): value is string => Boolean(value)) ?? null;
      setMessages(transcript);
      setConversationId(detail.session);
      setLatestRun(null);
      setRuntimeEvents([]);
      setLiveControlText("");
      setIsSending(false);
      pendingAssistantIdRef.current = null;
      activeRunIdRef.current = lastRunId;
      if (lastRunId) {
        void loadRunDetail(lastRunId);
      }
      appendConsoleLine(`[memory] resumed ${detail.session} with ${transcript.length} transcript message${transcript.length === 1 ? "" : "s"}`);
    } catch (error) {
      appendConsoleLine(`[memory] failed to resume ${sessionKey}: ${(error as Error).message}`);
    } finally {
      setLoadingMemorySessionId(null);
    }
  };

  return {
    ensureWorkspaceSync,
    loadDirectory,
    loadMoreRecentSessions,
    previewFile,
    refreshRecentSessions,
    refreshWorkspaceStatus,
    resumeMemorySession,
    runSemanticSearch,
    semanticQuery,
  };
}
