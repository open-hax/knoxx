import { useEffect, useState, useRef, useCallback } from "react";
import { Button, Badge } from "@open-hax/uxx";
import { ContextBar } from "../components/context-bar";
import { createSidebarResizeHandlers } from "../components/chat-page/sidebar-resize";
import { listMemorySessions } from "../lib/api/common";
import {
  type EditorDocument,
  type DocumentStatus,
  type DocumentVisibility,
  STATUS_CONFIG,
  VISIBILITY_CONFIG,
  MOCK_COLLECTIONS,
} from "../components/editor/editor-types";
import type {
  BrowseResponse,
  PinnedContextItem,
  PreviewResponse,
  SemanticSearchMatch,
  WorkspaceJob,
} from "../components/context-bar/types";
import type { MemorySessionSummary } from "../lib/types";
import styles from "./CmsPage.module.css";

const PAGE_SIZE = 20;

interface Document {
  doc_id: string;
  tenant_id: string;
  title: string;
  content: string;
  visibility: "internal" | "review" | "public" | "archived";
  source: string;
  source_path: string | null;
  domain: string;
  language: string;
  created_by: string;
  published_by: string | null;
  published_at: string | null;
  ai_drafted: boolean;
  ai_model: string | null;
  metadata: Record<string, unknown>;
  created_at: string;
  updated_at: string;
}

interface DocumentListResponse {
  documents: Document[];
  total: number;
  by_visibility: Record<string, number>;
}

interface StatsResponse {
  total: number;
  project_total: number;
  by_visibility: Record<string, number>;
  by_domain: Record<string, number>;
  by_kind: Record<string, number>;
  by_source: Record<string, number>;
}

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

const VISIBILITY_VARIANTS: Record<string, "default" | "warning" | "success" | "error"> = {
  internal: "default",
  review: "warning",
  public: "success",
  archived: "error",
};

const VISIBILITY_ICONS: Record<string, string> = {
  internal: "🔒",
  review: "👀",
  public: "🌐",
  archived: "📦",
};

function CmsPage() {
  // Document state
  const [documents, setDocuments] = useState<Document[]>([]);
  const [documentTotal, setDocumentTotal] = useState(0);
  const [selectedDoc, setSelectedDoc] = useState<Document | null>(null);
  const [editorDoc, setEditorDoc] = useState<EditorDocument | null>(null);
  const [isDirty, setIsDirty] = useState(false);

  // Stats
  const [stats, setStats] = useState<StatsResponse | null>(null);

  // Loading states
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);

  // Filters
  const [project, setProject] = useState("devel");
  const [visibilityFilter, setVisibilityFilter] = useState("all");
  const [kindFilter, setKindFilter] = useState("docs");
  const [sourceFilter, setSourceFilter] = useState("");
  const [domainFilter, setDomainFilter] = useState("");
  const [pathPrefixFilter, setPathPrefixFilter] = useState("");

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
  const [previewData, setPreviewData] = useState<PreviewResponse | null>(null);
  const [loadingBrowse, setLoadingBrowse] = useState(false);
  const [loadingPreview, setLoadingPreview] = useState(false);
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

  // Gardens (for publishing)
  const [gardens, setGardens] = useState<Garden[]>([]);
  const [selectedGardenId, setSelectedGardenId] = useState<string>("");

  // Right panel (chat secondary)
  const [showChat, setShowChat] = useState(false);
  const [rightPanelWidthPx, setRightPanelWidthPx] = useState(360);

  // Infinite scroll
  const sentinelRef = useRef<HTMLDivElement>(null);
  const offsetRef = useRef(0);
  const isLoadingRef = useRef(false);
  const hasMoreRef = useRef(true);

  const { startSidebarPaneResize, startSidebarWidthResize } = createSidebarResizeHandlers({
    sidebarSplitContainerRef,
    sidebarWidthPx,
    setSidebarPaneSplitPct,
    setSidebarWidthPx,
  });

  // Keep refs in sync
  useEffect(() => { hasMoreRef.current = hasMore; }, [hasMore]);
  useEffect(() => { isLoadingRef.current = loading || loadingMore; }, [loading, loadingMore]);
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

  // Load documents when filters change
  useEffect(() => {
    offsetRef.current = 0;
    setDocuments([]);
    setHasMore(true);
    hasMoreRef.current = true;
    loadDocuments(true);
    loadStats();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [project, visibilityFilter, kindFilter, sourceFilter, domainFilter, pathPrefixFilter]);

  // Infinite scroll observer
  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (entry.isIntersecting && hasMoreRef.current && !isLoadingRef.current) {
          loadDocuments(false);
        }
      },
      { rootMargin: "200px" }
    );

    observer.observe(sentinel);
    return () => observer.disconnect();
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

  const loadDocuments = async (reset: boolean) => {
    if (reset) {
      setLoading(true);
    } else {
      setLoadingMore(true);
    }

    try {
      const params = new URLSearchParams();
      params.set("tenant_id", project);
      params.set("kind", kindFilter);
      params.set("limit", String(PAGE_SIZE));
      params.set("offset", String(reset ? 0 : offsetRef.current));
      if (visibilityFilter !== "all") params.set("visibility", visibilityFilter);
      if (sourceFilter.trim()) params.set("source", sourceFilter.trim());
      if (domainFilter.trim()) params.set("domain", domainFilter.trim());
      if (pathPrefixFilter.trim()) params.set("source_path_prefix", pathPrefixFilter.trim());

      const resp = await fetch(`/api/cms/documents?${params}`);
      if (resp.ok) {
        const data: DocumentListResponse = await resp.json();
        if (reset) {
          setDocuments(data.documents);
          offsetRef.current = data.documents.length;
        } else {
          setDocuments((prev) => [...prev, ...data.documents]);
          offsetRef.current += data.documents.length;
        }
        setDocumentTotal(data.total);
        const newHasMore = offsetRef.current < data.total;
        setHasMore(newHasMore);
        hasMoreRef.current = newHasMore;
      }
    } catch (err) {
      console.error("Failed to load documents:", err);
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  };

  const loadStats = async () => {
    try {
      const params = new URLSearchParams();
      params.set("tenant_id", project);
      params.set("kind", kindFilter);
      if (visibilityFilter !== "all") params.set("visibility", visibilityFilter);
      if (sourceFilter.trim()) params.set("source", sourceFilter.trim());
      if (domainFilter.trim()) params.set("domain", domainFilter.trim());
      if (pathPrefixFilter.trim()) params.set("source_path_prefix", pathPrefixFilter.trim());
      const resp = await fetch(`/api/cms/stats?${params}`);
      if (resp.ok) {
        setStats(await resp.json());
      }
    } catch (err) {
      console.error("Failed to load stats:", err);
    }
  };

  // Convert Document to EditorDocument for editing
  const handleSelectDocument = (doc: Document) => {
    setSelectedDoc(doc);
    setEditorDoc({
      id: doc.doc_id,
      title: doc.title,
      body: doc.content,
      collection_id: doc.source,
      visibility: doc.visibility === "archived" ? "internal" : doc.visibility as DocumentVisibility,
      status: doc.visibility === "public" ? "published" : doc.visibility === "review" ? "review" : "draft",
      created_at: doc.created_at,
      updated_at: doc.updated_at,
    });
    setIsDirty(false);
  };

  const handleTitleChange = useCallback((title: string) => {
    setEditorDoc((prev) => prev ? { ...prev, title } : null);
    setIsDirty(true);
  }, []);

  const handleBodyChange = useCallback((body: string) => {
    setEditorDoc((prev) => prev ? { ...prev, body } : null);
    setIsDirty(true);
  }, []);

  const handleVisibilityChange = useCallback((visibility: DocumentVisibility) => {
    setEditorDoc((prev) => prev ? { ...prev, visibility } : null);
    setIsDirty(true);
  }, []);

  const handleSetReview = useCallback(() => {
    setEditorDoc((prev) => prev ? { ...prev, status: "review" } : null);
    setIsDirty(true);
  }, []);

  const handlePublish = useCallback(async () => {
    if (!selectedDoc || !selectedGardenId) return;

    try {
      const resp = await fetch(`/api/cms/publish/${selectedDoc.doc_id}/${selectedGardenId}`, { method: "POST" });
      if (resp.ok) {
        setEditorDoc((prev) => prev ? {
          ...prev,
          status: "published",
          visibility: "public",
        } : null);
        setIsDirty(false);
        loadDocuments(true);
        loadStats();
      }
    } catch (err) {
      console.error("Publish failed:", err);
    }
  }, [selectedDoc, selectedGardenId]);

  const handleSave = useCallback(async () => {
    if (!editorDoc) return;

    try {
      const resp = await fetch(`/api/cms/documents/${editorDoc.id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: editorDoc.title,
          content: editorDoc.body,
          visibility: editorDoc.visibility,
        }),
      });

      if (resp.ok) {
        setIsDirty(false);
        loadDocuments(true);
      }
    } catch (err) {
      console.error("Save failed:", err);
    }
  }, [editorDoc]);

  const handleCreateDocument = async () => {
    const title = prompt("Document title:");
    if (!title) return;

    try {
      const resp = await fetch(`/api/cms/documents?tenant_id=${encodeURIComponent(project)}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title, content: "", domain: "general" }),
      });

      if (resp.ok) {
        const doc: Document = await resp.json();
        handleSelectDocument(doc);
        loadStats();
      }
    } catch (err) {
      console.error("Create failed:", err);
    }
  };

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

  const handlePreviewFile = async (path: string) => {
    setLoadingPreview(true);
    try {
      const params = new URLSearchParams({ path });
      const resp = await fetch(`/api/ingestion/file?${params}`);
      if (resp.ok) {
        setPreviewData(await resp.json());
      }
    } finally {
      setLoadingPreview(false);
    }
  };

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
      // In CMS context, resuming a session could open the chat panel
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
          statsTotal={stats?.total ?? 0}
          statsByVisibility={stats?.by_visibility ?? {}}
          sourceFilter={sourceFilter}
          domainFilter={domainFilter}
          pathPrefixFilter={pathPrefixFilter}
          onHide={() => setShowFiles(false)}
          onVisibilityFilterChange={setVisibilityFilter}
          onKindFilterChange={setKindFilter}
          onSourceFilterChange={setSourceFilter}
          onDomainFilterChange={setDomainFilter}
          onPathPrefixFilterChange={setPathPrefixFilter}
          onNewDocument={handleCreateDocument}
          onStartSidebarPaneResize={startSidebarPaneResize}
          onStartSidebarWidthResize={startSidebarWidthResize}
          // File explorer
          currentPath={currentPath}
          currentParentPath={currentParentPath}
          browseData={browseData}
          previewData={previewData}
          loadingBrowse={loadingBrowse}
          loadingPreview={loadingPreview}
          entryFilter={entryFilter}
          filteredEntries={filteredEntries}
          activeEntryCount={activeEntryCount}
          workspaceSourceId={workspaceSourceId}
          workspaceJob={workspaceJob}
          workspaceProgressPercent={workspaceJob ? Math.round((workspaceJob.processed_files / workspaceJob.total_files) * 100) : 0}
          onLoadDirectory={handleLoadDirectory}
          onEntryFilterChange={setEntryFilter}
          onPreviewFile={handlePreviewFile}
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

      {/* Main editor area */}
      <div style={{ flex: 1, overflow: "hidden", display: "flex", flexDirection: "column" }}>
        {editorDoc ? (
          <>
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
                  value={editorDoc.title}
                  onChange={(e) => handleTitleChange(e.target.value)}
                  placeholder="Untitled document"
                />
                {isDirty && <span className={styles.dirtyIndicator}>Unsaved changes</span>}
              </div>
              <div className={styles.actions}>
                <button
                  className={styles.saveButton}
                  onClick={handleSave}
                  disabled={!isDirty}
                >
                  Save
                </button>
                {editorDoc.status === "draft" && (
                  <button className={styles.publishButton} onClick={handleSetReview}>
                    Submit for Review
                  </button>
                )}
                {editorDoc.status === "review" && (
                  <button className={styles.publishButton} onClick={handlePublish} disabled={!selectedGardenId}>
                    Publish
                  </button>
                )}
              </div>
            </header>

            {/* Editor layout */}
            <div className={styles.editorLayout}>
              <main className={styles.bodyEditor}>
                <textarea
                  className={styles.bodyTextarea}
                  value={editorDoc.body}
                  onChange={(e) => handleBodyChange(e.target.value)}
                  placeholder="Start writing..."
                />
              </main>

              <aside className={styles.fieldsSidebar}>
                {/* Status */}
                <div className={styles.fieldGroup}>
                  <label className={styles.fieldLabel}>Status</label>
                  <div className={styles.fieldValue}>
                    <Badge variant={editorDoc.status === "published" ? "success" : editorDoc.status === "review" ? "warning" : "default"}>
                      {STATUS_CONFIG[editorDoc.status].label}
                    </Badge>
                  </div>
                </div>

                {/* Visibility */}
                <div className={styles.fieldGroup}>
                  <label className={styles.fieldLabel}>Visibility</label>
                  <select
                    value={editorDoc.visibility}
                    onChange={(e) => handleVisibilityChange(e.target.value as DocumentVisibility)}
                    className={styles.fieldSelect}
                  >
                    {Object.entries(VISIBILITY_CONFIG).map(([value, config]) => (
                      <option key={value} value={value}>
                        {config.label}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Collection */}
                <div className={styles.fieldGroup}>
                  <label className={styles.fieldLabel}>Collection</label>
                  <select
                    value={editorDoc.collection_id}
                    onChange={(e) => setEditorDoc((prev) => prev ? { ...prev, collection_id: e.target.value } : null)}
                    className={styles.fieldSelect}
                  >
                    {MOCK_COLLECTIONS.map((col) => (
                      <option key={col.id} value={col.id}>
                        {col.name}
                      </option>
                    ))}
                  </select>
                </div>

                {/* Garden for publishing */}
                {(editorDoc.status === "review" || editorDoc.status === "published") && (
                  <div className={styles.fieldGroup}>
                    <label className={styles.fieldLabel}>Publish to Garden</label>
                    <select
                      value={selectedGardenId}
                      onChange={(e) => setSelectedGardenId(e.target.value)}
                      className={styles.fieldSelect}
                    >
                      {gardens.map((g) => (
                        <option key={g.garden_id} value={g.garden_id}>
                          {g.title}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {/* Metadata */}
                <div className={styles.fieldGroup}>
                  <label className={styles.fieldLabel}>Metadata</label>
                  <div className={styles.metadata}>
                    <div>Created: {new Date(editorDoc.created_at).toLocaleDateString()}</div>
                    <div>Updated: {new Date(editorDoc.updated_at).toLocaleDateString()}</div>
                    {selectedDoc?.ai_drafted && (
                      <div style={{ color: "var(--token-colors-accent-magenta)" }}>AI-drafted</div>
                    )}
                  </div>
                </div>

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
          </>
        ) : (
          /* No document selected - show document list */
          <div style={{ flex: 1, overflow: "auto" }}>
            <div style={{ position: "sticky", top: 0, zIndex: 10, borderBottom: "1px solid var(--token-colors-border-default)", background: "var(--token-colors-background-surface)", backdropFilter: "blur(8px)", padding: "16px" }}>
              <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                {!showFiles && <Button variant="ghost" size="sm" onClick={() => setShowFiles(true)}>Files</Button>}
                <h2 style={{ fontSize: "18px", fontWeight: 600 }}>Content Library</h2>
              </div>
              <p style={{ fontSize: "14px", color: "var(--token-colors-text-muted)" }}>
                {loading ? "Loading..." : `${documentTotal} matching records in ${project}`}
              </p>
            </div>

            <div style={{ display: "flex", flexDirection: "column" }}>
              {documents.map((doc) => (
                <div
                  key={doc.doc_id}
                  onClick={() => handleSelectDocument(doc)}
                  style={{
                    cursor: "pointer",
                    padding: "16px",
                    borderBottom: "1px solid var(--token-colors-alpha-bg-_08)",
                    transition: "background 0.15s",
                  }}
                >
                  <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: "12px" }}>
                    <div style={{ minWidth: 0, flex: 1 }}>
                      <h3 style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", fontWeight: 500 }}>{doc.title}</h3>
                      <p style={{ marginTop: "4px", overflow: "hidden", display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", fontSize: "14px", color: "var(--token-colors-text-muted)" }}>
                        {doc.content.slice(0, 150)}...
                      </p>
                    </div>
                    <Badge variant={VISIBILITY_VARIANTS[doc.visibility]} size="sm" rounded>
                      {VISIBILITY_ICONS[doc.visibility]} {doc.visibility}
                    </Badge>
                  </div>

                  <div style={{ marginTop: "8px", display: "flex", alignItems: "center", gap: "8px", fontSize: "12px", color: "var(--token-colors-text-muted)" }}>
                    <span>{doc.source}</span>
                    <span>•</span>
                    <span>{new Date(doc.created_at).toLocaleDateString()}</span>
                    {doc.ai_drafted && (
                      <>
                        <span>•</span>
                        <span style={{ color: "var(--token-colors-accent-magenta)" }}>AI-drafted</span>
                      </>
                    )}
                  </div>
                </div>
              ))}

              <div ref={sentinelRef} style={{ height: 1 }} />

              {loadingMore && (
                <div style={{ padding: "16px", textAlign: "center", color: "var(--token-colors-text-muted)" }}>
                  Loading more...
                </div>
              )}

              {!hasMore && documents.length > 0 && (
                <div style={{ padding: "16px", textAlign: "center", color: "var(--token-colors-text-muted)", fontSize: "14px" }}>
                  {documentTotal} documents loaded
                </div>
              )}

              {documents.length === 0 && !loading && (
                <div style={{ padding: "32px", textAlign: "center", color: "var(--token-colors-text-muted)" }}>
                  <p>No documents found.</p>
                  <div style={{ marginTop: 12 }}>
                    <Button variant="primary" size="sm" onClick={handleCreateDocument}>
                      Create Document
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Right panel: Chat (secondary) */}
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
            <p style={{ fontSize: 14, color: "var(--token-colors-text-muted)" }}>
              Chat interface will be here. Can discuss the current document with AI assistant.
            </p>
            {editorDoc && (
              <div style={{ marginTop: 12, padding: 12, borderRadius: 8, background: "var(--token-colors-alpha-bg-_08)" }}>
                <div style={{ fontSize: 12, fontWeight: 600, marginBottom: 4 }}>Pinned Context</div>
                <div style={{ fontSize: 12, color: "var(--token-colors-text-muted)" }}>
                  {editorDoc.title}
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default CmsPage;
