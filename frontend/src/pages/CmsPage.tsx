import { useEffect, useState, useRef, useCallback } from "react";
import { Button, Card, Badge, Input } from "@open-hax/uxx";
import { ContextBar } from "../components/context-bar";
import { createSidebarResizeHandlers } from "../components/chat-page/sidebar-resize";

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

const COMMON_LANGUAGES: Record<string, string> = {
  en: "English",
  es: "Spanish",
  de: "German",
  fr: "French",
  ja: "Japanese",
  ko: "Korean",
  zh: "Chinese",
  pt: "Portuguese",
  it: "Italian",
  ru: "Russian",
  ar: "Arabic",
  hi: "Hindi",
};

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

const CHAT_SIDEBAR_WIDTH_KEY = "knoxx_chat_sidebar_width_px";

function CmsPage() {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [documentTotal, setDocumentTotal] = useState(0);
  const [stats, setStats] = useState<StatsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasMore, setHasMore] = useState(true);
  const [project, setProject] = useState("devel");
  const [visibilityFilter, setVisibilityFilter] = useState<string>("all");
  const [kindFilter, setKindFilter] = useState<string>("docs");
  const [sourceFilter, setSourceFilter] = useState("");
  const [domainFilter, setDomainFilter] = useState("");
  const [pathPrefixFilter, setPathPrefixFilter] = useState("");
  const [selectedDoc, setSelectedDoc] = useState<Document | null>(null);
  const [showDraftPanel, setShowDraftPanel] = useState(false);
  const [draftTopic, setDraftTopic] = useState("");
  const [drafting, setDrafting] = useState(false);
  const [gardens, setGardens] = useState<Garden[]>([]);
  const [selectedGardenId, setSelectedGardenId] = useState<string>("");
  const [selectedLanguages, setSelectedLanguages] = useState<string[]>([]);
  const [showLanguageDropdown, setShowLanguageDropdown] = useState(false);
  const [showFiles, setShowFiles] = useState(true);
  const [sidebarWidthPx, setSidebarWidthPx] = useState(() => {
    const stored = localStorage.getItem(CHAT_SIDEBAR_WIDTH_KEY);
    return stored ? parseInt(stored, 10) : 280;
  });
  const [sidebarPaneSplitPct, setSidebarPaneSplitPct] = useState(50);
  const sidebarSplitContainerRef = useRef<HTMLDivElement | null>(null);
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

  // Keep refs in sync with state
  useEffect(() => { hasMoreRef.current = hasMore; }, [hasMore]);
  useEffect(() => { isLoadingRef.current = loading || loadingMore; }, [loading, loadingMore]);
  useEffect(() => {
    localStorage.setItem(CHAT_SIDEBAR_WIDTH_KEY, String(sidebarWidthPx));
  }, [sidebarWidthPx]);

  // Load gardens list on mount
  useEffect(() => {
    const loadGardens = async () => {
      try {
        const resp = await fetch("/api/openplanner/v1/gardens?status=active");
        if (resp.ok) {
          const data = await resp.json();
          setGardens(data.gardens ?? []);
          // Auto-select first active garden if available
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

  // Load initial documents and stats when filters change
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
        const data: StatsResponse = await resp.json();
        setStats(data);
      }
    } catch (err) {
      console.error("Failed to load stats:", err);
    }
  };

  const handlePublish = async (docId: string) => {
    if (!selectedGardenId) {
      alert("Please select a garden to publish to");
      return;
    }
    try {
      const params = new URLSearchParams();
      if (selectedLanguages.length > 0) {
        params.set("target_languages", selectedLanguages.join(","));
      }
      const resp = await fetch(`/api/cms/publish/${docId}/${selectedGardenId}?${params}`, { method: "POST" });
      if (resp.ok) {
        const data = await resp.json();
        // Show translation jobs if any
        if (data.translation_jobs?.length > 0) {
          const langs = data.translation_jobs.map((j: { target_lang: string }) => j.target_lang).join(", ");
          console.log(`Translation jobs queued for: ${langs}`);
        }
        // Reload from beginning to reflect visibility change
        offsetRef.current = 0;
        setDocuments([]);
        setHasMore(true);
        loadDocuments(true);
        loadStats();
      } else {
        const err = await resp.text();
        alert(`Failed to publish: ${err}`);
      }
    } catch (err) {
      console.error("Publish failed:", err);
    }
  };

  const handleArchive = async (docId: string) => {
    try {
      const resp = await fetch(`/api/cms/archive/${docId}`, { method: "POST" });
      if (resp.ok) {
        // Reload from beginning to reflect visibility change
        offsetRef.current = 0;
        setDocuments([]);
        setHasMore(true);
        loadDocuments(true);
        loadStats();
      }
    } catch (err) {
      console.error("Archive failed:", err);
    }
  };

  const handleGenerateDraft = async () => {
    if (!draftTopic.trim()) return;
    
    setDrafting(true);
    try {
      const resp = await fetch("/api/cms/draft", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          tenant_id: project,
          topic: draftTopic,
          tone: "professional",
          audience: "general",
          source_collections: [project],
          max_context_chunks: 5,
        }),
      });
      
      if (resp.ok) {
        const doc: Document = await resp.json();
        // Prepend new document and update offset
        setDocuments((prev) => [doc, ...prev]);
        offsetRef.current += 1;
        setDocumentTotal((prev) => prev + 1);
        setShowDraftPanel(false);
        setDraftTopic("");
      } else {
        const err = await resp.text();
        alert(`Draft generation failed: ${err}`);
      }
    } catch (err) {
      console.error("Draft generation failed:", err);
    } finally {
      setDrafting(false);
    }
  };

  const handleCreateDocument = async () => {
    const title = prompt("Document title:");
    if (!title) return;
    
    const content = prompt("Content (brief):");
    if (!content) return;
    
    try {
      const resp = await fetch(`/api/cms/documents?tenant_id=${encodeURIComponent(project)}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title, content, domain: "general" }),
      });
      
      if (resp.ok) {
        const doc: Document = await resp.json();
        // Prepend new document and update offset
        setDocuments((prev) => [doc, ...prev]);
        offsetRef.current += 1;
        setDocumentTotal((prev) => prev + 1);
        loadStats();
      }
    } catch (err) {
      console.error("Create failed:", err);
    }
  };

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
        />
      ) : null}

      <div style={{ flex: 1, overflow: "hidden" }}>
        <div style={{ display: "flex", height: "100%", gap: "16px" }}>
          <Card
            variant="default"
            padding="none"
            style={{ flex: 1, overflow: "auto", width: showDraftPanel ? "50%" : undefined }}
          >
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
                  onClick={() => setSelectedDoc(doc)}
                  style={{
                    cursor: "pointer",
                    padding: "16px",
                    borderBottom: "1px solid var(--token-colors-alpha-bg-_08)",
                    background: selectedDoc?.doc_id === doc.doc_id ? "var(--token-colors-alpha-blue-_15)" : "transparent",
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

              {/* Sentinel for infinite scroll */}
              <div ref={sentinelRef} style={{ height: 1 }} />

              {/* Loading more indicator */}
              {loadingMore && (
                <div style={{ padding: "16px", textAlign: "center", color: "var(--token-colors-text-muted)" }}>
                  Loading more...
                </div>
              )}

              {/* End of list indicator */}
              {!hasMore && documents.length > 0 && (
                <div style={{ padding: "16px", textAlign: "center", color: "var(--token-colors-text-muted)", fontSize: "14px" }}>
                  {documentTotal} documents loaded
                </div>
              )}

              {documents.length === 0 && !loading && (
                <div style={{ padding: "32px", textAlign: "center", color: "var(--token-colors-text-muted)" }}>
                  <p>No documents found.</p>
                  <p style={{ marginTop: "4px", fontSize: "14px" }}>Create a new document or generate an AI draft.</p>
                </div>
              )}
            </div>
          </Card>

          {showDraftPanel && (
            <Card variant="default" padding="md" style={{ width: "320px", flexShrink: 0, overflow: "auto" }}>
              <h3 style={{ fontSize: "18px", fontWeight: 600 }}>AI Draft Assistant</h3>
              <p style={{ marginTop: "4px", fontSize: "14px", color: "var(--token-colors-text-muted)" }}>
                Generate content from your knowledge base
              </p>

              <div style={{ marginTop: "16px", display: "flex", flexDirection: "column", gap: "16px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "14px", fontWeight: 500 }}>Topic</label>
                  <textarea
                    value={draftTopic}
                    onChange={(e) => setDraftTopic(e.target.value)}
                    placeholder="What should the document be about?"
                    style={{ marginTop: "4px", width: "100%", borderRadius: "6px", border: "1px solid var(--token-colors-border-subtle)", padding: "8px 12px", fontSize: "14px", resize: "vertical" }}
                    rows={3}
                  />
                </div>

                <div>
                  <label style={{ display: "block", fontSize: "14px", fontWeight: 500 }}>Sources</label>
                  <select style={{ marginTop: "4px", width: "100%", borderRadius: "6px", border: "1px solid var(--token-colors-border-subtle)", padding: "8px 12px", fontSize: "14px" }}>
                    <option>devel_docs (all internal docs)</option>
                    <option>devel_specs (design docs)</option>
                  </select>
                </div>

                <Button
                  variant="primary"
                  fullWidth
                  loading={drafting}
                  disabled={!draftTopic.trim()}
                  onClick={handleGenerateDraft}
                >
                  {drafting ? "Generating..." : "Generate Draft"}
                </Button>
              </div>

              <hr style={{ margin: "16px 0", border: "none", borderTop: "1px solid var(--token-colors-border-default)" }} />

              <Card variant="outlined" padding="sm">
                <h4 style={{ fontSize: "14px", fontWeight: 500 }}>How it works</h4>
                <ol style={{ marginTop: "8px", display: "flex", flexDirection: "column", gap: "4px", fontSize: "12px", color: "var(--token-colors-text-muted)", paddingLeft: "20px" }}>
                  <li>AI searches your knowledge base</li>
                  <li>Generates a draft document</li>
                  <li>You review and edit</li>
                  <li>Publish to make public</li>
                </ol>
              </Card>
            </Card>
          )}

          {selectedDoc && !showDraftPanel && (
            <Card variant="default" padding="md" style={{ width: "384px", flexShrink: 0, overflow: "auto" }}>
              <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between" }}>
                <h3 style={{ fontSize: "18px", fontWeight: 600 }}>{selectedDoc.title}</h3>
                <Button variant="ghost" size="sm" onClick={() => setSelectedDoc(null)}>
                  ✕
                </Button>
              </div>

              <div style={{ marginTop: "8px", display: "flex", gap: "8px" }}>
                <Badge variant={VISIBILITY_VARIANTS[selectedDoc.visibility]} size="sm" rounded>
                  {VISIBILITY_ICONS[selectedDoc.visibility]} {selectedDoc.visibility}
                </Badge>
                <Badge variant="default" size="sm">
                  {selectedDoc.source}
                </Badge>
              </div>

              <div style={{ marginTop: "16px", maxHeight: "256px", overflow: "auto", borderRadius: "6px", background: "var(--token-colors-alpha-bg-_08)", padding: "12px", fontSize: "14px" }}>
                {selectedDoc.content}
              </div>

              <div style={{ marginTop: "16px", display: "flex", flexDirection: "column", gap: "8px" }}>
                {(selectedDoc.visibility === "internal" || selectedDoc.visibility === "review" || selectedDoc.visibility === "archived") && (
                  <div>
                    <label style={{ display: "block", fontSize: "14px", fontWeight: 500, marginBottom: "4px" }}>
                      Publish to Garden
                    </label>
                    <select
                      value={selectedGardenId}
                      onChange={(e) => {
                        setSelectedGardenId(e.target.value);
                        // Auto-select garden's default target languages
                        const garden = gardens.find((g) => g.garden_id === e.target.value);
                        if (garden?.target_languages?.length) {
                          setSelectedLanguages(garden.target_languages);
                        } else {
                          setSelectedLanguages([]);
                        }
                      }}
                      style={{ width: "100%", padding: 8, borderRadius: 6, border: "1px solid var(--token-colors-border-default)", background: "var(--token-colors-background-canvas)", color: "var(--token-colors-text-default)" }}
                    >
                      {gardens.length === 0 && (
                        <option value="">No gardens available</option>
                      )}
                      {gardens.map((g) => (
                        <option key={g.garden_id} value={g.garden_id}>
                          {g.title} ({g.garden_id})
                        </option>
                      ))}
                    </select>
                    
                    {/* Language selection for translation */}
                    {selectedGardenId && (
                      <div style={{ marginTop: "12px" }}>
                        <div
                          onClick={() => setShowLanguageDropdown(!showLanguageDropdown)}
                          style={{
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "space-between",
                            padding: "8px 12px",
                            borderRadius: 6,
                            border: "1px solid var(--token-colors-border-default)",
                            background: "var(--token-colors-background-canvas)",
                            cursor: "pointer",
                            fontSize: "14px",
                          }}
                        >
                          <span>
                            🌐 Translate to{" "}
                            {selectedLanguages.length > 0
                              ? selectedLanguages.map((l) => COMMON_LANGUAGES[l] || l).join(", ")
                              : "No languages selected"}
                          </span>
                          <span style={{ opacity: 0.5 }}>{showLanguageDropdown ? "▲" : "▼"}</span>
                        </div>
                        
                        {showLanguageDropdown && (
                          <div
                            style={{
                              marginTop: "4px",
                              padding: "8px",
                              borderRadius: 6,
                              border: "1px solid var(--token-colors-border-default)",
                              background: "var(--token-colors-background-surface)",
                              maxHeight: "200px",
                              overflow: "auto",
                            }}
                          >
                            {Object.entries(COMMON_LANGUAGES).map(([code, name]) => (
                              <label
                                key={code}
                                style={{
                                  display: "flex",
                                  alignItems: "center",
                                  gap: "8px",
                                  padding: "4px 8px",
                                  cursor: "pointer",
                                  fontSize: "14px",
                                }}
                              >
                                <input
                                  type="checkbox"
                                  checked={selectedLanguages.includes(code)}
                                  onChange={(e) => {
                                    if (e.target.checked) {
                                      setSelectedLanguages([...selectedLanguages, code]);
                                    } else {
                                      setSelectedLanguages(selectedLanguages.filter((l) => l !== code));
                                    }
                                  }}
                                />
                                <span>{name}</span>
                                <span style={{ opacity: 0.5, fontSize: "12px" }}>({code})</span>
                              </label>
                            ))}
                          </div>
                        )}
                        
                        <p style={{ marginTop: "4px", fontSize: "12px", color: "var(--token-colors-text-muted)" }}>
                          {selectedLanguages.length > 0
                            ? `Will create ${selectedLanguages.length} translation job${selectedLanguages.length > 1 ? "s" : ""}`
                            : "Select languages to auto-translate after publishing"}
                        </p>
                      </div>
                    )}
                  </div>
                )}
                
                {selectedDoc.visibility === "internal" && (
                  <Button variant="primary" fullWidth onClick={() => handlePublish(selectedDoc.doc_id)} disabled={!selectedGardenId}>
                    🚀 Publish to Public
                  </Button>
                )}
                
                {selectedDoc.visibility === "review" && (
                  <Button variant="primary" fullWidth onClick={() => handlePublish(selectedDoc.doc_id)} disabled={!selectedGardenId}>
                    ✅ Approve & Publish
                  </Button>
                )}

                {selectedDoc.visibility === "public" && (
                  <Button variant="danger" fullWidth onClick={() => handleArchive(selectedDoc.doc_id)}>
                    📦 Archive
                  </Button>
                )}

                {selectedDoc.visibility === "archived" && (
                  <Button variant="primary" fullWidth onClick={() => handlePublish(selectedDoc.doc_id)} disabled={!selectedGardenId}>
                    🔄 Re-publish
                  </Button>
                )}
              </div>

              <div style={{ marginTop: "16px", display: "flex", flexDirection: "column", gap: "4px", fontSize: "12px", color: "var(--token-colors-text-muted)" }}>
                <p>Created: {new Date(selectedDoc.created_at).toLocaleString()}</p>
                <p>By: {selectedDoc.created_by}</p>
                {selectedDoc.source_path && <p>Source: {selectedDoc.source_path}</p>}
                {selectedDoc.published_at && (
                  <p>Published: {new Date(selectedDoc.published_at).toLocaleString()}</p>
                )}
              </div>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}

export default CmsPage;
