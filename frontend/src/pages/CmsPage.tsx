import { useEffect, useState, useRef, useCallback } from "react";
import { Button, Badge } from "@open-hax/uxx";
import { ContextBar } from "../components/context-bar";
import { createSidebarResizeHandlers } from "../components/chat-page/sidebar-resize";
import { listMemorySessions } from "../lib/api/common";
import {
  type DocumentVisibility,
  VISIBILITY_CONFIG,
} from "../components/editor/editor-types";
import type {
  BrowseResponse,
  BrowseEntry,
  PinnedContextItem,
  PreviewResponse,
  SemanticSearchMatch,
  WorkspaceJob,
} from "../components/context-bar/types";
import type { MemorySessionSummary } from "../lib/types";
import styles from "./CmsPage.module.css";

interface Garden {
  garden_id: string;
  title: string;
  description: string | null;
  status: "draft" | "active" | "archived";
  default_language: string;
  target_languages: string[];
  auto_translate: boolean;
}

const CHAT_SIDEBAR_WIDTH_KEY = "knoxx_cms_sidebar_width_px";

function CmsPage() {
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

  // ContextBar data (file explorer, semantic search, sessions)
  const [browseData, setBrowseData] = useState<BrowseResponse | null>(null);
  const [loadingBrowse, setLoadingBrowse] = useState(false);
  const [entryFilter, setEntryFilter] = useState("");
  const [semanticQuery, setSemanticQuery] = useState("");
  const [semanticResults, setSemanticResults] = useState<SemanticSearchMatch[]>([]);
  const [semanticProjects, setSemanticProjects] = useState<string[]>([]);
  const [semanticSearching, setSemanticSearching] = useState(false);
  const [workspaceSourceId, setWorkspaceSourceId] = useState<string | null>(null);
  const [workspaceJob, setWorkspaceJob] = useState<WorkspaceJob | null>(null);
  const [pinnedContext, setPinnedContext] = useState<PinnedContextItem[]>([]);
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

  // Gardens (for publishing)
  const [gardens, setGardens] = useState<Garden[]>([]);
  const [selectedGardenId, setSelectedGardenId] = useState<string>("");

  // Chat panel - always visible in Editor workflow
  const [showChat, setShowChat] = useState(true);
  const [rightPanelWidthPx, setRightPanelWidthPx] = useState(360);

  const { startSidebarPaneResize, startSidebarWidthResize } = createSidebarResizeHandlers({
    sidebarSplitContainerRef,
    sidebarWidthPx,
    setSidebarPaneSplitPct,
    setSidebarWidthPx,
  });

  // Persist sidebar width
  useEffect(() => {
    localStorage.setItem(CHAT_SIDEBAR_WIDTH_KEY, String(sidebarWidthPx));
  }, [sidebarWidthPx]);

  // Load gardens on mount
  useEffect(() => {
    const loadGardens = async () => {
      try {
        const resp = await fetch("/api/openplanner/v1/gardens?status=active");
        if (resp.ok) {
          const data = await resp.json();
          setGardens(data.gardens ?? []);
          if (data.gardens?.length > 0 && !selectedGardenId) {
            setSelectedGardenId(data.gardens[0].garden_id);
          }
        }
      } catch (err) {
        console.error("Failed to load gardens:", err);
      }
    };
    loadGardens();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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
    loadBrowseData();
  }, []);

  // Load recent sessions
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
    loadRecentSessions();
  }, []);

  // Load workspace status
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
    loadWorkspaceStatus();
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

  // KEY DIFFERENCE: Selecting a file opens it in editor AND pins to chat context
  const handleOpenFile = async (entry: BrowseEntry) => {
    if (entry.type === "dir") {
      await handleLoadDirectory(entry.path);
      return;
    }

    // Load file content
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
      }
    } catch (err) {
      console.error("Failed to load file:", err);
    }

    // Auto-pin to chat context (key CMS behavior)
    const pinnedItem: PinnedContextItem = {
      id: entry.path,
      title: entry.name,
      path: entry.path,
      kind: "file",
    };
    setPinnedContext((prev) => {
      if (prev.some((p) => p.path === entry.path)) return prev;
      return [pinnedItem, ...prev.slice(0, 23)];
    });
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
      // Save back to ingestion/storage
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
      setShowChat(true);
      console.log("Resume session:", sessionId);
    } finally {
      setLoadingMemorySessionId(null);
    }
  };

  // Derived state
  const semanticMode = semanticQuery.trim().length > 0 && semanticResults.length > 0;
  const filteredEntries = browseData?.entries?.filter((e) =>
    entryFilter ? e.name.toLowerCase().includes(entryFilter.toLowerCase()) : true
  ) ?? [];
  const activeEntryCount = filteredEntries.filter((e) => e.type === "file").length;
  const currentPath = browseData?.current_path ?? "";
  const currentParentPath = currentPath.includes("/") ? currentPath.split("/").slice(0, -1).join("/") : "";

  return (
    <div style={{ display: "flex", height: "calc(100vh - 120px)", gap: 0 }}>
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
          // File explorer
          currentPath={currentPath}
          currentParentPath={currentParentPath}
          browseData={browseData}
          previewData={null} // CMS doesn't use preview panel
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
          onOpenFile={handleOpenFile} // CMS uses onOpenFile instead of onPreviewFile
          // Semantic search
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
          }}
          // Sessions
          recentSessions={recentSessions}
          recentSessionsHasMore={recentSessionsHasMore}
          recentSessionsTotal={recentSessionsTotal}
          loadingRecentSessions={loadingRecentSessions}
          loadingMoreRecentSessions={loadingMoreRecentSessions}
          loadingMemorySessionId={loadingMemorySessionId}
          onRefreshRecentSessions={handleRefreshRecentSessions}
          onLoadMoreRecentSessions={async () => {}}
          onResumeMemorySession={handleResumeMemorySession}
          // Pinned context
          pinnedContext={pinnedContext}
          onUnpinContextItem={(path) => setPinnedContext((prev) => prev.filter((p) => p.path !== path))}
          onPinSemanticResult={(entry) => setPinnedContext((prev) => [...prev, {
            id: entry.id,
            title: entry.path.split("/").pop() ?? entry.path,
            path: entry.path,
            snippet: entry.snippet,
            kind: "semantic",
          }])}
        />
      ) : null}

      {/* Main editor area - always visible */}
      <div style={{ flex: 1, overflow: "hidden", display: "flex", flexDirection: "column" }}>
        {/* Editor header */}
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
              onChange={(e) => handleTitleChange(e.target.value)}
              placeholder="Select a file from the explorer..."
              disabled={!editorPath}
            />
            {editorPath && (
              <span style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>
                {editorPath}
              </span>
            )}
            {isDirty && <span className={styles.dirtyIndicator}>Unsaved changes</span>}
          </div>
          <div className={styles.actions}>
            {editorPath && (
              <>
                <button
                  className={styles.saveButton}
                  onClick={handleSave}
                  disabled={!isDirty}
                >
                  Save
                </button>
                <button className={styles.publishButton}>
                  Publish
                </button>
              </>
            )}
          </div>
        </header>

        {/* Editor layout */}
        <div className={styles.editorLayout}>
          <main className={styles.bodyEditor}>
            {editorPath ? (
              <textarea
                className={styles.bodyTextarea}
                value={editorBody}
                onChange={(e) => handleBodyChange(e.target.value)}
                placeholder="Start writing..."
              />
            ) : (
              <div style={{
                flex: 1,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "var(--token-colors-text-muted)",
                fontSize: 14,
              }}>
                <div style={{ textAlign: "center" }}>
                  <p style={{ marginBottom: 8 }}>Select a file from the explorer to edit</p>
                  <p style={{ fontSize: 12, color: "var(--token-colors-text-subtle)" }}>
                    Files in docs/ folder are available
                  </p>
                </div>
              </div>
            )}
          </main>

          <aside className={styles.fieldsSidebar}>
            {/* Status */}
            <div className={styles.fieldGroup}>
              <label className={styles.fieldLabel}>Status</label>
              <div className={styles.fieldValue}>
                <Badge variant="default">Draft</Badge>
              </div>
            </div>

            {/* Visibility */}
            <div className={styles.fieldGroup}>
              <label className={styles.fieldLabel}>Visibility</label>
              <select
                value={editorVisibility}
                onChange={(e) => handleVisibilityChange(e.target.value as DocumentVisibility)}
                className={styles.fieldSelect}
                disabled={!editorPath}
              >
                {Object.entries(VISIBILITY_CONFIG).map(([value, config]) => (
                  <option key={value} value={value}>
                    {config.label}
                  </option>
                ))}
              </select>
            </div>

            {/* Pinned context */}
            {pinnedContext.length > 0 && (
              <div className={styles.fieldGroup}>
                <label className={styles.fieldLabel}>Pinned Context</label>
                <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                  {pinnedContext.slice(0, 5).map((item) => (
                    <div
                      key={item.id}
                      style={{
                        fontSize: 11,
                        padding: "4px 8px",
                        background: "var(--token-colors-alpha-bg-_08)",
                        borderRadius: 4,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "space-between",
                      }}
                    >
                      <span style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                        {item.title}
                      </span>
                      <button
                        onClick={() => setPinnedContext((prev) => prev.filter((p) => p.path !== item.path))}
                        style={{
                          border: "none",
                          background: "transparent",
                          color: "var(--token-colors-text-muted)",
                          cursor: "pointer",
                          fontSize: 10,
                        }}
                      >
                        ✕
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Chat toggle */}
            <div className={styles.fieldGroup}>
              <Button
                variant="ghost"
                size="sm"
                fullWidth
                onClick={() => setShowChat(!showChat)}
              >
                {showChat ? "Hide Chat" : "Show Chat"}
              </Button>
            </div>
          </aside>
        </div>
      </div>

      {/* Right panel: Chat (secondary) - always visible in Editor workflow */}
      {showChat && (
        <div
          style={{
            width: rightPanelWidthPx,
            borderLeft: "1px solid var(--token-colors-border-default)",
            display: "flex",
            flexDirection: "column",
            background: "var(--token-colors-background-surface)",
          }}
        >
          <div
            style={{
              padding: "12px 16px",
              borderBottom: "1px solid var(--token-colors-border-default)",
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
            }}
          >
            <span style={{ fontWeight: 600, fontSize: 14 }}>Chat</span>
            <Button variant="ghost" size="sm" onClick={() => setShowChat(false)}>
              ✕
            </Button>
          </div>
          <div style={{ flex: 1, overflow: "auto", padding: 16 }}>
            {editorPath ? (
              <div style={{ marginBottom: 12, padding: 12, borderRadius: 8, background: "var(--token-colors-alpha-bg-_08)" }}>
                <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 4 }}>Editing</div>
                <div style={{ fontSize: 12, color: "var(--token-colors-text-muted)" }}>
                  {editorTitle}
                </div>
                <div style={{ fontSize: 10, color: "var(--token-colors-text-subtle)", marginTop: 4 }}>
                  {editorPath}
                </div>
              </div>
            ) : (
              <p style={{ fontSize: 14, color: "var(--token-colors-text-muted)" }}>
                Select a file to start editing. The file will be pinned to this chat automatically.
              </p>
            )}
            {pinnedContext.length > 0 && (
              <div style={{ marginTop: 12 }}>
                <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 8 }}>Pinned Context</div>
                {pinnedContext.slice(0, 5).map((item) => (
                  <div
                    key={item.id}
                    style={{
                      fontSize: 11,
                      padding: "6px 8px",
                      background: "var(--token-colors-alpha-blue-_08)",
                      borderRadius: 4,
                      marginBottom: 4,
                    }}
                  >
                    {item.title}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default CmsPage;
