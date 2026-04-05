import type { Dispatch, MutableRefObject, SetStateAction } from "react";
import { getMemorySession, listMemorySessions } from "../../lib/api";
import type { ChatMessage, MemorySessionSummary, RunDetail, RunEvent } from "../../lib/types";
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
  setRecentSessions: SetState<MemorySessionSummary[]>;
  setLoadingRecentSessions: SetState<boolean>;
  setLoadingMemorySessionId: SetState<string | null>;
  setMessages: SetState<ChatMessage[]>;
  setConversationId: SetState<string | null>;
  setLatestRun: SetState<RunDetail | null>;
  setRuntimeEvents: SetState<RunEvent[]>;
  setLiveControlText: SetState<string>;
  setIsSending: SetState<boolean>;
  setConsoleLines: SetState<string[]>;
  pendingAssistantIdRef: MutableRefObject<string | null>;
  activeRunIdRef: MutableRefObject<string | null>;
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
      const sessions = await listMemorySessions(50);
      setRecentSessions(sessions);
    } catch (error) {
      appendConsoleLine(`[memory] failed to load recent sessions: ${(error as Error).message}`);
    } finally {
      setLoadingRecentSessions(false);
    }
  };

  const resumeMemorySession = async (sessionKey: string) => {
    setLoadingMemorySessionId(sessionKey);
    try {
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
    previewFile,
    refreshRecentSessions,
    refreshWorkspaceStatus,
    resumeMemorySession,
    runSemanticSearch,
    semanticQuery,
  };
}
