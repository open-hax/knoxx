import { useEffect, useState, useRef, useCallback } from "react";
import { Button, Badge } from "@open-hax/uxx";
import { ChatWorkspacePane } from "../components/chat-page/ChatWorkspacePane";
import { createSidebarResizeHandlers } from "../components/chat-page/sidebar-resize";
import { useChatWorkspaceController } from "../components/chat-page/useChatWorkspaceController";
import { ContextBar } from "../components/context-bar";
import { listMemorySessions } from "../lib/api/common";
import {
  type DocumentVisibility,
  VISIBILITY_CONFIG,
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

function CmsPage() {
  const chat = useChatWorkspaceController({ initialShowCanvas: false });

  // Editor state
  const [editorTitle, setEditorTitle] = useState("");
  const [editorBody, setEditorBody] = useState("");
  const [editorPath, setEditorPath] = useState<string | null>(null);
  const [editorVisibility, setEditorVisibility] = useState<DocumentVisibility>("internal");
  const [isDirty, setIsDirty] = useState(false);

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
        const data = await listMemorySessions({ limit: 10 });
        setRecentSessions(data.rows ?? []);
        setRecentSessionsTotal(data.total ?? 0);
        setRecentSessionsHasMore(data.has_more ?? false);
      } catch (err) {
        console.error("Failed to load sessions:", err);
      } finally {
        setLoadingRecentSessions(false);
      }
    };
    void loadRecentSessions();
  }, []);

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
        setEditorTitle(entry.name);
        setEditorBody(data.content);
        setEditorPath(entry.path);
        setEditorVisibility((entry.visibility as DocumentVisibility) ?? "internal");
        setIsDirty(false);

        chat.pinContextItem({
          id: entry.path,
          title: entry.name,
          path: entry.path,
          snippet: data.content.slice(0, 240),
          kind: "file",
        });
      }
    } catch (err) {
      console.error("Failed to load file:", err);
    }
  };

  const handleTitleChange = useCallback((title: string) => {
    setEditorTitle(title);
    setIsDirty(true);
  }, []);

  const handleBodyChange = useCallback((body: string) => {
    setEditorBody(body);
    setIsDirty(true);
  }, []);

  const handleVisibilityChange = useCallback((visibility: DocumentVisibility) => {
    setEditorVisibility(visibility);
    setIsDirty(true);
  }, []);

  const handleSave = useCallback(async () => {
    if (!editorPath || !isDirty) return;

    try {
      console.log("Save:", { path: editorPath, title: editorTitle });
      setIsDirty(false);
    } catch (err) {
      console.error("Save failed:", err);
    }
  }, [editorPath, editorTitle, isDirty]);

  const handleRefreshRecentSessions = async () => {
    setLoadingRecentSessions(true);
    try {
      const data = await listMemorySessions({ limit: 10 });
      setRecentSessions(data.rows ?? []);
      setRecentSessionsTotal(data.total ?? 0);
      setRecentSessionsHasMore(data.has_more ?? false);
    } finally {
      setLoadingRecentSessions(false);
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
            setEditorTitle("Untitled");
            setEditorBody("");
            setEditorPath(null);
            setIsDirty(true);
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
          onLoadMoreRecentSessions={async () => {}}
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
            <input
              type="text"
              className={styles.titleInput}
              value={editorTitle}
              onChange={(event) => handleTitleChange(event.target.value)}
              placeholder="Select a file from the explorer..."
              disabled={!editorPath}
            />
            {editorPath ? (
              <span style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>{editorPath}</span>
            ) : null}
            {isDirty ? <span className={styles.dirtyIndicator}>Unsaved changes</span> : null}
          </div>
          <div className={styles.actions}>
            {editorPath ? (
              <>
                <button className={styles.saveButton} onClick={() => void handleSave()} disabled={!isDirty}>
                  Save
                </button>
                <button className={styles.publishButton}>Publish</button>
              </>
            ) : null}
          </div>
        </header>

        <div className={styles.metaBar}>
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>Status</span>
            <Badge variant="default">Draft</Badge>
          </div>
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>Visibility</span>
            <select
              value={editorVisibility}
              onChange={(event) => handleVisibilityChange(event.target.value as DocumentVisibility)}
              className={styles.metaSelect}
              disabled={!editorPath}
            >
              {Object.entries(VISIBILITY_CONFIG).map(([value, config]) => (
                <option key={value} value={value}>
                  {config.label}
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
