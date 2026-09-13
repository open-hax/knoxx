import { mergeSessionPages, sortSessions } from "../../lib/storage";
import { normalizedSessionActorFilter, excludedSessionActorIds, persistedSessionVisibleForFilter } from "./chat-page-config";
import { preferredSessionModelForResume } from "./ChatSettingsPanel";
import type { ChatWorkspaceActionParams } from "../context-bar/types";
import { getAgentHistorySession, getMemorySession, listMemorySessions, searchMemory } from "../../lib/api";
import type { ChatMessage, MemorySearchHit, MemorySessionSummary, RunDetail, RunEvent } from "../../lib/types";
import { findPersistedChatSessionByConversation, listPersistedChatSessions, readPersistedChatSessionSnapshot } from "./hooks";
import type {
  BrowseResponse,
  IngestionSource,
  PreviewResponse,
  SemanticSearchMatch,
  SemanticSearchResponse,
  WorkspaceJob,
} from "./types";
import { isWorkspaceSource, memoryRowRunId, memoryRowsToMessages, selectWorkspaceJob } from "./utils";

const RECENT_SESSION_PAGE_SIZE = 20;

export function createChatWorkspaceActions({
  visibleAgentIds,
  currentPath,
  showFiles,
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
      setSessionSearchHits([]);
      setSessionSearchMode("none");
      return;
    }

    setSemanticSearching(true);
    const actorId = normalizedSessionActorFilter(sessionActorFilter) ?? undefined;
    const excludeActorIds = excludedSessionActorIds(sessionActorFilter, excludeEtaMuSessions);
    try {
      const [fileResult, sessionResult] = await Promise.all([
        (async () => {
          const response = await fetch("/api/ingestion/search", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ q: trimmed, role: "workspace", path, limit: 30 }),
          });
          if (!response.ok) throw new Error(`Semantic search failed: ${response.status}`);
          return (await response.json()) as SemanticSearchResponse;
        })(),
        searchMemory({
          query: trimmed,
          k: 8,
          actorId,
          excludeActorIds,
        }),
      ]);
      setSemanticResults(fileResult.rows);
      setSemanticProjects(fileResult.projects);
      setSessionSearchHits(sessionResult.hits);
      setSessionSearchMode(sessionResult.mode);
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
      // Ensure we have a workspace_root (absolute path) from the ingestion service.
      // This must be used as the local driver root_path when running on the host.
      let effectiveBrowse = browseData;
      if (!effectiveBrowse) {
        const resp = await fetch("/api/ingestion/browse");
        if (resp.ok) {
          effectiveBrowse = (await resp.json()) as BrowseResponse;
          setBrowseData(effectiveBrowse);
        }
      }
      const workspaceRoot = effectiveBrowse?.workspace_root;

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
            name: "workspace",
            config: {
              root_path: workspaceRoot || "/app/workspace",
              sync_interval_minutes: defaultSyncIntervalMinutes,
              workspace_source: true,
            },
            collections: ["devel"],
            file_types: defaultFileTypes,
            exclude_patterns: defaultExcludePatterns,
          }),
        });
        if (!createResponse.ok) throw new Error(`Failed to create source: ${createResponse.status}`);
        const createdSource = (await createResponse.json()) as IngestionSource;
        source = createdSource;
        appendConsoleLine(`[ingestion] created workspace source ${createdSource.source_id} (root ${workspaceRoot || "unknown"})`);
      }

      if (!source) throw new Error("Failed to resolve workspace source");
      setWorkspaceSourceId(source.source_id);

      const jobResponse = await fetch("/api/ingestion/jobs", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ source_id: source.source_id }),
      });
      if (!jobResponse.ok) throw new Error(`Failed to start sync: ${jobResponse.status}`);
      const job = (await jobResponse.json()) as { job_id: string };
      appendConsoleLine(`[ingestion] queued workspace sync job ${job.job_id} (interval ${defaultSyncIntervalMinutes}m)`);
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
      const page = await listMemorySessions({
        limit: RECENT_SESSION_PAGE_SIZE,
        offset: 0,
        actorId: normalizedSessionActorFilter(sessionActorFilter) ?? undefined,
        excludeActorIds: excludedSessionActorIds(sessionActorFilter, excludeEtaMuSessions),
      });
      const preservedTail = remoteRecentSessionsRef.current.filter((item) => !page.rows.some((row) => row.session === item.session));
      const remoteMerged = mergeSessionPages(page.rows, preservedTail);
      remoteRecentSessionsRef.current = remoteMerged;
      const localVisible = listPersistedChatSessions(sessionStateKey)
        .filter((item) => persistedSessionVisibleForFilter(sessionStateKey, item, sessionActorFilter, excludeEtaMuSessions, visibleAgentIds));
      const merged = sortSessions(mergeSessionPages(remoteMerged, localVisible));
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
      const page = await listMemorySessions({
        limit: RECENT_SESSION_PAGE_SIZE,
        offset: remoteRecentSessionsRef.current.length,
        actorId: normalizedSessionActorFilter(sessionActorFilter) ?? undefined,
        excludeActorIds: excludedSessionActorIds(sessionActorFilter, excludeEtaMuSessions),
      });
      const remoteMerged = mergeSessionPages(remoteRecentSessionsRef.current, page.rows);
      remoteRecentSessionsRef.current = remoteMerged;
      const localVisible = listPersistedChatSessions(sessionStateKey)
        .filter((item) => persistedSessionVisibleForFilter(sessionStateKey, item, sessionActorFilter, excludeEtaMuSessions, visibleAgentIds));
      const merged = sortSessions(mergeSessionPages(remoteMerged, localVisible));
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
      const localSnapshot = readPersistedChatSessionSnapshot(sessionStateKey, resolvedSessionId);
      const persistedModel = preferredSessionModelForResume(localSnapshot, []);

      setMessages([]);
      setConversationId(sessionKey);
      setSessionId(resolvedSessionId);
      if (persistedModel) {
        setSelectedModel(persistedModel);
      }
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

      let detail = await getMemorySession(sessionKey);
      let transcript = memoryRowsToMessages(detail.rows).slice(-80);

      // Some archived/legacy sessions may not have normalized knoxx.message rows
      // in the scoped memory endpoint yet. Fall back to direct OpenPlanner history.
      if (transcript.length === 0) {
        try {
          const historyDetail = await getAgentHistorySession(sessionKey);
          const fallbackTranscript = memoryRowsToMessages(historyDetail.rows).slice(-80);
          if (fallbackTranscript.length > 0) {
            detail = historyDetail;
            transcript = fallbackTranscript;
            appendConsoleLine(`[memory] fallback loaded ${historyDetail.session} from agent history API`);
          }
        } catch {
          // keep original detail path and error semantics
        }
      }

      const resumedModel = preferredSessionModelForResume(localSnapshot, transcript);
      const lastRunId = [...detail.rows].reverse().map(memoryRowRunId).find((value): value is string => Boolean(value)) ?? null;
      setMessages(transcript);
      if (resumedModel) {
        setSelectedModel(resumedModel);
      }
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

export { preferredSessionModelForResume } from "./ChatSettingsPanel";
export { persistedSessionVisibleForActor, persistedSessionVisibleForFilter } from "./chat-page-config";
