import { useEffect, useMemo, useState, useRef, useCallback } from "react";
import { Button, Badge } from "@open-hax/uxx";
import { ChatWorkspacePane } from "../components/chat-page/ChatWorkspacePane";
import { createSidebarResizeHandlers } from "../components/chat-page/sidebar-resize";
import { useChatWorkspaceController } from "../components/chat-page/useChatWorkspaceController";
import { ContextBar } from "../components/context-bar";
import { listMemorySessions } from "../lib/api/common";
import {
  type DocumentStatus,
  STATUS_CONFIG,
} from "../components/editor/editor-types";
import type {
  BrowseResponse,
  BrowseEntry,
  PreviewResponse,
  SemanticSearchMatch,
  WorkspaceJob,
} from "../components/context-bar/types";
import type { AgentSource, MemorySessionSummary } from "../lib/types";
import styles from "./CmsPage.module.css";

const CHAT_SIDEBAR_WIDTH_KEY = "knoxx_cms_sidebar_width_px";

type GardenSummary = {
  garden_id: string;
  title: string;
  status: string;
};

type CmsDocSummary = {
  doc_id: string;
  title: string;
  source_path: string | null;
  metadata?: {
    garden_publications?: Array<{ garden_id?: string }>;
  };
};

const RECENT_SESSION_PAGE_SIZE = 10;

function mergeSessionPages(primary: MemorySessionSummary[], secondary: MemorySessionSummary[]): MemorySessionSummary[] {
  const seen = new Set<string>();
  const merged: MemorySessionSummary[] = [];
  for (const row of [...primary, ...secondary]) {
    if (!row?.session || seen.has(row.session)) continue;
    seen.add(row.session);
    merged.push(row);
  }
  return merged;
}

function CmsPage() {
  const chat = useChatWorkspaceController({ initialShowCanvas: false });

  // Editor state
  const [editorTitle, setEditorTitle] = useState("");
  const [editorBody, setEditorBody] = useState("");
  const [editorPath, setEditorPath] = useState<string | null>(null);
  const [editorStatus, setEditorStatus] = useState<DocumentStatus>("draft");
  const [isDirty, setIsDirty] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [lastSaveMessage, setLastSaveMessage] = useState<string | null>(null);
  const [gardens, setGardens] = useState<GardenSummary[]>([]);
  const [selectedGardenId, setSelectedGardenId] = useState("");
  const [cmsDocId, setCmsDocId] = useState<string | null>(null);
  const [publishedGardenIds, setPublishedGardenIds] = useState<string[]>([]);

  // ContextBar state
  const [showFiles, setShowFiles] = useState(true);
  const [sidebarWidthPx, setSidebarWidthPx] = useState(() => {
    const stored = localStorage.getItem(CHAT_SIDEBAR_WIDTH_KEY);
    return stored ? parseInt(stored, 10) : 280;
  });
  const [sidebarPaneSplitPct, setSidebarPaneSplitPct] = useState(50);
  const sidebarSplitContainerRef = useRef<HTMLDivElement | null>(null);

  // ContextBar data (CMS-specific explorer/search/sessions)
  const [browseData, setBrowseData] = useState<BrowseResponse | null>(null);
  const [loadingBrowse, setLoadingBrowse] = useState(false);
  const [entryFilter, setEntryFilter] = useState("");
  const [semanticQuery, setSemanticQuery] = useState("");
  const [semanticResults, setSemanticResults] = useState<SemanticSearchMatch[]>([]);
  const [semanticProjects, setSemanticProjects] = useState<string[]>([]);
  const [semanticSearching, setSemanticSearching] = useState(false);
  const [workspaceSourceId, setWorkspaceSourceId] = useState<string | null>(null);
  const [workspaceJob, setWorkspaceJob] = useState<WorkspaceJob | null>(null);
  const [recentSessions, setRecentSessions] = useState<MemorySessionSummary[]>([]);
  const [recentSessionsHasMore, setRecentSessionsHasMore] = useState(false);
  const [recentSessionsTotal, setRecentSessionsTotal] = useState(0);
  const [loadingRecentSessions, setLoadingRecentSessions] = useState(false);
  const [loadingMoreRecentSessions, setLoadingMoreRecentSessions] = useState(false);
  const [loadingMemorySessionId, setLoadingMemorySessionId] = useState<string | null>(null);
  const recentSessionsRef = useRef<MemorySessionSummary[]>([]);
  recentSessionsRef.current = recentSessions;

  // Filters
  const [visibilityFilter, setVisibilityFilter] = useState("all");
  const [kindFilter, setKindFilter] = useState("docs");
  const [sourceFilter, setSourceFilter] = useState("");
  const [domainFilter, setDomainFilter] = useState("");
  const [pathPrefixFilter, setPathPrefixFilter] = useState("");

  const { startSidebarPaneResize, startSidebarWidthResize } = createSidebarResizeHandlers({
    sidebarSplitContainerRef,
    sidebarWidthPx,
    setSidebarPaneSplitPct,
    setSidebarWidthPx,
  });

  useEffect(() => {
    localStorage.setItem(CHAT_SIDEBAR_WIDTH_KEY, String(sidebarWidthPx));
  }, [sidebarWidthPx]);

  // Load file browser data - default to docs/ folder for knowledge management
  useEffect(() => {
    const loadBrowseData = async () => {
      setLoadingBrowse(true);
      try {
        const params = new URLSearchParams();
        params.set("path", "docs");
        const resp = await fetch(`/api/ingestion/browse?${params}`);
        if (resp.ok) {
          setBrowseData(await resp.json());
        }
      } catch (err) {
        console.error("Failed to load browse data:", err);
      } finally {
        setLoadingBrowse(false);
      }
    };
    void loadBrowseData();
  }, []);

  useEffect(() => {
    const loadRecentSessions = async () => {
      setLoadingRecentSessions(true);
      try {
        const data = await listMemorySessions({ limit: RECENT_SESSION_PAGE_SIZE, offset: 0 });
        const nextRows = data.rows ?? [];
        recentSessionsRef.current = nextRows;
        setRecentSessions(nextRows);
        setRecentSessionsTotal(data.total ?? nextRows.length);
        setRecentSessionsHasMore(data.has_more ?? false);
      } catch {
        recentSessionsRef.current = [];
        setRecentSessions([]);
        setRecentSessionsTotal(0);
        setRecentSessionsHasMore(false);
      } finally {
        setLoadingRecentSessions(false);
      }
    };
    void loadRecentSessions();
  }, []);

  useEffect(() => {
    const loadGardens = async () => {
      try {
        const resp = await fetch("/api/openplanner/v1/gardens");
        if (!resp.ok) return;
        const body = (await resp.json()) as { gardens?: GardenSummary[] };
        const publishableGardens = (body.gardens ?? []).filter((garden) => garden.status !== "archived");
        setGardens(publishableGardens);
        if (!selectedGardenId && publishableGardens.length > 0) {
          setSelectedGardenId(publishableGardens[0].garden_id);
        }
      } catch {
        setGardens([]);
      }
    };
    void loadGardens();
  }, [selectedGardenId]);

  useEffect(() => {
    const loadWorkspaceStatus = async () => {
      try {
        const sourcesResp = await fetch("/api/ingestion/sources");
        if (!sourcesResp.ok) return;
        const sources = await sourcesResp.json();
        const source = sources.find((s: { name: string; config?: { root_path?: string } }) =>
          s.name === "devel workspace" || s.config?.root_path === "/app/workspace/devel"
        );
        setWorkspaceSourceId(source?.source_id ?? null);
        if (!source) return;

        const jobsResp = await fetch(`/api/ingestion/jobs?source_id=${encodeURIComponent(source.source_id)}&limit=10`);
        if (jobsResp.ok) {
          const jobs = await jobsResp.json();
          const active = jobs.find((j: { status: string }) => j.status === "running" || j.status === "pending");
          setWorkspaceJob(active ?? jobs[0] ?? null);
        }
      } catch (err) {
        console.error("Failed to load workspace status:", err);
      }
    };
    void loadWorkspaceStatus();
  }, []);

  const editorDirectory = editorPath?.includes("/") ? editorPath.slice(0, editorPath.lastIndexOf("/") + 1) : "";
  const isPublishedToSelectedGarden = useMemo(
    () => Boolean(selectedGardenId && publishedGardenIds.includes(selectedGardenId)),
    [publishedGardenIds, selectedGardenId],
  );

  const syncCmsDocumentByPath = useCallback(async (path: string) => {
    const normalizeSourcePath = (value: string | null | undefined) => (value ?? "").replace(/^\/+/, "");
    const params = new URLSearchParams({ path_prefix: path, limit: "20" });
    const resp = await fetch(`/api/openplanner/v1/cms/documents?${params.toString()}`);
    if (!resp.ok) {
      setCmsDocId(null);
      setPublishedGardenIds([]);
      setEditorStatus("draft");
      return null;
    }

    const body = (await resp.json()) as { documents?: CmsDocSummary[] };
    const normalizedPath = normalizeSourcePath(path);
    const match = (body.documents ?? []).find((doc) => normalizeSourcePath(doc.source_path) === normalizedPath) ?? null;
    if (!match) {
      setCmsDocId(null);
      setPublishedGardenIds([]);
      setEditorStatus("draft");
      return null;
    }

    const gardenIds = (match.metadata?.garden_publications ?? [])
      .map((publication) => publication.garden_id)
      .filter((gardenId): gardenId is string => typeof gardenId === "string" && gardenId.length > 0);
    setCmsDocId(match.doc_id);
    setPublishedGardenIds(gardenIds);
    setEditorStatus(gardenIds.includes(selectedGardenId) ? "published" : "draft");
    return match;
  }, [selectedGardenId]);

  const upsertCmsDocument = useCallback(async (path: string) => {
    const existing = await syncCmsDocumentByPath(path);
    const payload = {
      title: editorTitle.trim() || path.split("/").pop() || "Untitled",
      content: editorBody,
      source_path: path,
      visibility: existing && publishedGardenIds.length > 0 ? "public" : "internal",
    };

    const endpoint = existing
      ? `/api/openplanner/v1/cms/documents/${encodeURIComponent(existing.doc_id)}`
      : "/api/openplanner/v1/cms/documents";
    const method = existing ? "PATCH" : "POST";
    const resp = await fetch(endpoint, {
      method,
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (!resp.ok) {
      const text = await resp.text();
      if (resp.status === 503) {
        try {
          const parsed = JSON.parse(text) as { persisted?: boolean };
          if (parsed.persisted) {
            if (existing?.doc_id) {
              setCmsDocId(existing.doc_id);
              return existing.doc_id;
            }
            const refetched = await syncCmsDocumentByPath(path);
            if (refetched?.doc_id) {
              return refetched.doc_id;
            }
          }
        } catch {
          // fall through to hard error
        }
      }
      throw new Error(text);
    }
    const doc = (await resp.json()) as CmsDocSummary;
    setCmsDocId(doc.doc_id);
    return doc.doc_id;
  }, [editorBody, editorTitle, publishedGardenIds.length, syncCmsDocumentByPath]);

  const buildEditorPath = useCallback(() => {
    const fileName = editorTitle.trim().replace(/[\\/]+/g, "-");
    return `${editorDirectory}${fileName}`;
  }, [editorDirectory, editorTitle]);

  const persistEditorFile = useCallback(
    async (next: { publishState?: "published" | "draft" } = {}) => {
      const nextPath = buildEditorPath();
      if (!nextPath) return null;

      setIsSaving(true);
      setLastSaveMessage(null);
      try {
        const resp = await fetch("/api/ingestion/file", {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            path: nextPath,
            old_path: editorPath && editorPath !== nextPath ? editorPath : null,
            content: editorBody,
          }),
        });
        if (!resp.ok) {
          throw new Error(await resp.text());
        }
        const data = await resp.json();
        const savedPath = typeof data.path === "string" ? data.path : nextPath;
        setEditorPath(savedPath);
        setEditorTitle(savedPath.split("/").pop() ?? savedPath);
        await upsertCmsDocument(savedPath);
        setIsDirty(false);
        if (next.publishState === "published") {
          setEditorStatus("published");
          setLastSaveMessage("Published");
        } else if (next.publishState === "draft") {
          setEditorStatus("draft");
          setLastSaveMessage("Unpublished");
        } else {
          setLastSaveMessage("Saved");
        }
        await handleLoadDirectory(editorDirectory.slice(0, -1) || undefined);
        return savedPath;
      } finally {
        setIsSaving(false);
      }
    },
    [buildEditorPath, editorBody, editorDirectory, editorPath, upsertCmsDocument],
  );

  useEffect(() => {
    setEditorStatus(isPublishedToSelectedGarden ? "published" : "draft");
  }, [isPublishedToSelectedGarden]);

  const handleLoadDirectory = async (path?: string) => {
    setLoadingBrowse(true);
    try {
      const params = new URLSearchParams();
      if (path) params.set("path", path);
      const resp = await fetch(`/api/ingestion/browse?${params}`);
      if (resp.ok) {
        setBrowseData(await resp.json());
      }
    } finally {
      setLoadingBrowse(false);
    }
  };

  const handleSemanticSearch = async () => {
    if (!semanticQuery.trim()) return;
    setSemanticSearching(true);
    try {
      const resp = await fetch("/api/ingestion/search", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ q: semanticQuery, role: "workspace", limit: 30 }),
      });
      if (resp.ok) {
        const data = await resp.json();
        setSemanticResults(data.rows ?? []);
        setSemanticProjects(data.projects ?? []);
      }
    } finally {
      setSemanticSearching(false);
    }
  };

  // CMS behavior: select file -> open in editor + pin into shared chat runtime
  const handleOpenFile = async (entry: BrowseEntry) => {
    if (entry.type === "dir") {
      await handleLoadDirectory(entry.path);
      return;
    }

    try {
      const params = new URLSearchParams({ path: entry.path });
      const resp = await fetch(`/api/ingestion/file?${params}`);
      if (resp.ok) {
        const data: PreviewResponse = await resp.json();
        const previousEditorPath = editorPath;
        setEditorTitle(entry.name);
        setEditorBody(data.content);
        setEditorPath(entry.path);
        setEditorStatus("draft");
        setIsDirty(false);
        setLastSaveMessage(null);

        if (previousEditorPath && previousEditorPath !== entry.path) {
          chat.unpinContextItem(previousEditorPath);
        }
        chat.pinContextItem({
          id: entry.path,
          title: entry.name,
          path: entry.path,
          snippet: data.content.slice(0, 240),
          kind: "file",
        });
        await syncCmsDocumentByPath(entry.path);
      }
    } catch (err) {
      console.error("Failed to load file:", err);
    }
  };

  const handleTitleChange = useCallback((title: string) => {
    setEditorTitle(title.replace(/[\\/]+/g, "-"));
    setEditorStatus((prev) => (prev === "published" ? "draft" : prev));
    setIsDirty(true);
    setLastSaveMessage(null);
  }, []);

  const handleBodyChange = useCallback((body: string) => {
    setEditorBody(body);
    setEditorStatus((prev) => (prev === "published" ? "draft" : prev));
    setIsDirty(true);
    setLastSaveMessage(null);
  }, []);

  const handleSave = useCallback(async () => {
    if (!editorTitle.trim()) return;
    try {
      await persistEditorFile();
    } catch (err) {
      console.error("Save failed:", err);
      setLastSaveMessage("Save failed");
    }
  }, [editorTitle, persistEditorFile]);

  const handlePublishToggle = useCallback(async () => {
    if (!editorTitle.trim() || !selectedGardenId) {
      setLastSaveMessage("Select a garden");
      return;
    }

    const nextState = isPublishedToSelectedGarden ? "draft" : "published";
    try {
      const savedPath = await persistEditorFile();
      if (!savedPath) return;
      const docId = cmsDocId ?? (await upsertCmsDocument(savedPath));
      const endpoint = `/api/openplanner/v1/cms/publish/${encodeURIComponent(docId)}/${encodeURIComponent(selectedGardenId)}`;
      const resp = await fetch(endpoint, { method: isPublishedToSelectedGarden ? "DELETE" : "POST" });
      if (!resp.ok) {
        const text = await resp.text();
        let softSuccess = false;
        if (resp.status === 503) {
          try {
            const parsed = JSON.parse(text) as { persisted?: boolean; indexed?: boolean };
            softSuccess = Boolean(parsed.persisted);
            if (softSuccess) {
              setLastSaveMessage(nextState === "published" ? "Published (index pending)" : "Unpublished (index pending)");
            }
          } catch {
            softSuccess = false;
          }
        }
        if (!softSuccess) {
          throw new Error(text);
        }
      }
      await syncCmsDocumentByPath(savedPath);
      const nextGardenIds = isPublishedToSelectedGarden
        ? publishedGardenIds.filter((gardenId) => gardenId !== selectedGardenId)
        : [...new Set([...publishedGardenIds, selectedGardenId])];
      setPublishedGardenIds(nextGardenIds);
      setEditorStatus(nextGardenIds.includes(selectedGardenId) ? "published" : "draft");
      setLastSaveMessage((current) => current ?? (nextState === "published" ? "Published" : "Unpublished"));
    } catch (err) {
      console.error("Publish toggle failed:", err);
      setLastSaveMessage(nextState === "published" ? "Publish failed" : "Unpublish failed");
    }
  }, [
    cmsDocId,
    editorTitle,
    isPublishedToSelectedGarden,
    persistEditorFile,
    publishedGardenIds,
    selectedGardenId,
    syncCmsDocumentByPath,
    upsertCmsDocument,
  ]);

  const handleRefreshRecentSessions = async () => {
    setLoadingRecentSessions(true);
    try {
      const data = await listMemorySessions({ limit: RECENT_SESSION_PAGE_SIZE, offset: 0 });
      const nextRows = data.rows ?? [];
      const preservedTail = recentSessionsRef.current.filter((item) => !nextRows.some((row) => row.session === item.session));
      const merged = mergeSessionPages(nextRows, preservedTail);
      recentSessionsRef.current = merged;
      setRecentSessions(merged);
      setRecentSessionsTotal(data.total ?? merged.length);
      setRecentSessionsHasMore(data.has_more ?? false);
    } catch {
      recentSessionsRef.current = [];
      setRecentSessions([]);
      setRecentSessionsTotal(0);
      setRecentSessionsHasMore(false);
    } finally {
      setLoadingRecentSessions(false);
    }
  };

  const handleLoadMoreRecentSessions = async () => {
    if (loadingRecentSessions || loadingMoreRecentSessions || !recentSessionsHasMore) return;
    setLoadingMoreRecentSessions(true);
    try {
      const data = await listMemorySessions({
        limit: RECENT_SESSION_PAGE_SIZE,
        offset: recentSessionsRef.current.length,
      });
      const merged = mergeSessionPages(recentSessionsRef.current, data.rows ?? []);
      recentSessionsRef.current = merged;
      setRecentSessions(merged);
      setRecentSessionsTotal(data.total ?? merged.length);
      setRecentSessionsHasMore(data.has_more ?? false);
    } catch {
      // keep existing rows on incremental load failure
    } finally {
      setLoadingMoreRecentSessions(false);
    }
  };

  const handleResumeMemorySession = async (sessionId: string) => {
    setLoadingMemorySessionId(sessionId);
    try {
      await chat.resumeMemorySession(sessionId);
    } finally {
      setLoadingMemorySessionId(null);
    }
  };

  const handleOpenChatSource = async (source: AgentSource) => {
    const path = source.url;
    if (/^https?:\/\//i.test(path)) {
      await chat.openSourceInPreview(source);
      return;
    }

    await handleOpenFile({
      name: source.title || path.split("/").pop() || path,
      path,
      type: "file",
      previewable: true,
    });
  };

  const semanticMode = semanticQuery.trim().length > 0 && semanticResults.length > 0;
  const filteredEntries =
    browseData?.entries?.filter((e) =>
      entryFilter ? e.name.toLowerCase().includes(entryFilter.toLowerCase()) : true,
    ) ?? [];
  const activeEntryCount = filteredEntries.filter((e) => e.type === "file").length;
  const currentPath = browseData?.current_path ?? "";
  const currentParentPath = currentPath.includes("/") ? currentPath.split("/").slice(0, -1).join("/") : "";

  return (
    <div style={{ display: "flex", height: "calc(100vh - 120px)", gap: 0, minHeight: 0 }}>
      {showFiles ? (
        <ContextBar
          sidebarWidthPx={sidebarWidthPx}
          sidebarPaneSplitPct={sidebarPaneSplitPct}
          sidebarSplitContainerRef={sidebarSplitContainerRef}
          visibilityFilter={visibilityFilter}
          kindFilter={kindFilter}
          statsTotal={0}
          statsByVisibility={{}}
          sourceFilter={sourceFilter}
          domainFilter={domainFilter}
          pathPrefixFilter={pathPrefixFilter}
          onHide={() => setShowFiles(false)}
          onVisibilityFilterChange={setVisibilityFilter}
          onKindFilterChange={setKindFilter}
          onSourceFilterChange={setSourceFilter}
          onDomainFilterChange={setDomainFilter}
          onPathPrefixFilterChange={setPathPrefixFilter}
          onNewDocument={() => {
            setEditorTitle("untitled.md");
            setEditorBody("");
            setEditorPath(currentPath ? `${currentPath}/untitled.md` : "untitled.md");
            setEditorStatus("draft");
            setCmsDocId(null);
            setPublishedGardenIds([]);
            setIsDirty(true);
            setLastSaveMessage(null);
          }}
          onStartSidebarPaneResize={startSidebarPaneResize}
          onStartSidebarWidthResize={startSidebarWidthResize}
          currentPath={currentPath}
          currentParentPath={currentParentPath}
          browseData={browseData}
          previewData={null}
          loadingBrowse={loadingBrowse}
          loadingPreview={false}
          entryFilter={entryFilter}
          filteredEntries={filteredEntries}
          activeEntryCount={activeEntryCount}
          workspaceSourceId={workspaceSourceId}
          workspaceJob={workspaceJob}
          workspaceProgressPercent={workspaceJob ? Math.round((workspaceJob.processed_files / workspaceJob.total_files) * 100) : 0}
          onLoadDirectory={handleLoadDirectory}
          onEntryFilterChange={setEntryFilter}
          onOpenFile={handleOpenFile}
          semanticQuery={semanticQuery}
          semanticResults={semanticResults}
          semanticProjects={semanticProjects}
          semanticSearching={semanticSearching}
          semanticMode={semanticMode}
          onSemanticQueryChange={setSemanticQuery}
          onSemanticSearch={handleSemanticSearch}
          onClearSemanticSearch={() => {
            setSemanticQuery("");
            setSemanticResults([]);
            setSemanticProjects([]);
          }}
          recentSessions={recentSessions}
          recentSessionsHasMore={recentSessionsHasMore}
          recentSessionsTotal={recentSessionsTotal}
          loadingRecentSessions={loadingRecentSessions}
          loadingMoreRecentSessions={loadingMoreRecentSessions}
          loadingMemorySessionId={loadingMemorySessionId}
          onRefreshRecentSessions={handleRefreshRecentSessions}
          onLoadMoreRecentSessions={handleLoadMoreRecentSessions}
          onResumeMemorySession={handleResumeMemorySession}
          pinnedContext={chat.pinnedContext}
          onUnpinContextItem={chat.unpinContextItem}
          onPinSemanticResult={chat.pinSemanticResult}
        />
      ) : null}

      <div style={{ flex: 1, overflow: "hidden", display: "flex", flexDirection: "column", minWidth: 0 }}>
        <header className={styles.header}>
          <div className={styles.titleRow}>
            {!showFiles && (
              <Button variant="ghost" size="sm" onClick={() => setShowFiles(true)}>
                Files
              </Button>
            )}
            <div className={styles.pathEditor}>
              <span className={styles.pathPrefix}>{editorPath ? editorDirectory || "./" : ""}</span>
              <input
                type="text"
                className={styles.fileNameInput}
                value={editorTitle}
                onChange={(event) => handleTitleChange(event.target.value)}
                placeholder="Select a file from the explorer..."
                disabled={!editorPath}
              />
            </div>
            {isDirty ? <span className={styles.dirtyIndicator}>Unsaved changes</span> : null}
            {!isDirty && lastSaveMessage ? <span className={styles.savedIndicator}>{lastSaveMessage}</span> : null}
          </div>
          <div className={styles.actions}>
            {editorPath ? (
              <>
                <button className={styles.saveButton} onClick={() => void handleSave()} disabled={isSaving || !isDirty}>
                  {isSaving ? "Saving…" : "Save"}
                </button>
                <button className={styles.publishButton} onClick={() => void handlePublishToggle()} disabled={isSaving || !selectedGardenId}>
                  {isSaving ? (isPublishedToSelectedGarden ? "Unpublishing…" : "Publishing…") : isPublishedToSelectedGarden ? "Unpublish" : "Publish"}
                </button>
              </>
            ) : null}
          </div>
        </header>

        <div className={styles.metaBar}>
          <div className={styles.metaItem}>
            <Badge variant={isPublishedToSelectedGarden ? "success" : editorStatus === "review" ? "warning" : "default"}>
              {isPublishedToSelectedGarden ? "Published" : STATUS_CONFIG[editorStatus].label}
            </Badge>
          </div>
          <div className={styles.metaItem}>
            <select
              value={selectedGardenId}
              onChange={(event) => setSelectedGardenId(event.target.value)}
              className={styles.metaSelect}
              aria-label="Garden"
            >
              <option value="">Select garden…</option>
              {gardens.map((garden) => (
                <option key={garden.garden_id} value={garden.garden_id}>
                  {garden.title || garden.garden_id}{garden.status !== "active" ? ` (${garden.status})` : ""}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className={styles.editorLayout}>
          <main className={styles.bodyEditor}>
            {editorPath ? (
              <textarea
                className={styles.bodyTextarea}
                value={editorBody}
                onChange={(event) => handleBodyChange(event.target.value)}
                placeholder="Start writing..."
              />
            ) : (
              <div
                style={{
                  flex: 1,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  color: "var(--token-colors-text-muted)",
                  fontSize: 14,
                }}
              >
                <div style={{ textAlign: "center" }}>
                  <p style={{ marginBottom: 8 }}>Select a file from the explorer to edit</p>
                  <p style={{ fontSize: 12, color: "var(--token-colors-text-subtle)" }}>
                    The file explorer is the content library.
                  </p>
                </div>
              </div>
            )}
          </main>
        </div>
      </div>

      <aside
        style={{
          width: 460,
          minWidth: 380,
          borderLeft: "1px solid var(--token-colors-border-default)",
          display: "flex",
          minHeight: 0,
          overflow: "hidden",
          background: "var(--token-colors-background-surface)",
        }}
      >
        <ChatWorkspacePane
          controller={chat}
          showFiles={showFiles}
          showCanvasToggle={false}
          onShowFiles={() => setShowFiles(true)}
          onOpenHydrationSource={(source) =>
            handleOpenFile({
              name: source.title || source.path.split("/").pop() || source.path,
              path: source.path,
              type: "file",
              previewable: true,
            })
          }
          onOpenSourceInPreview={handleOpenChatSource}
        />
      </aside>
    </div>
  );
}

export default CmsPage;
