import { useEffect, useState } from "react";
import { Button, Card, Badge, Input } from "@devel/ui-react";

interface Document {
  doc_id: string;
  tenant_id: string;
  title: string;
  content: string;
  visibility: "internal" | "review" | "public" | "archived";
  source: "manual" | "ai-drafted" | "ingested";
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
  by_visibility: Record<string, number>;
  by_domain: Record<string, number>;
}

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
  const [documents, setDocuments] = useState<Document[]>([]);
  const [stats, setStats] = useState<StatsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<string>("all");
  const [selectedDoc, setSelectedDoc] = useState<Document | null>(null);
  const [showDraftPanel, setShowDraftPanel] = useState(false);
  const [draftTopic, setDraftTopic] = useState("");
  const [drafting, setDrafting] = useState(false);

  useEffect(() => {
    loadDocuments();
    loadStats();
  }, [filter]);

  const loadDocuments = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (filter !== "all") {
        params.set("visibility", filter);
      }
      const resp = await fetch(`/api/cms/documents?${params}`);
      if (resp.ok) {
        const data: DocumentListResponse = await resp.json();
        setDocuments(data.documents);
      }
    } catch (err) {
      console.error("Failed to load documents:", err);
    } finally {
      setLoading(false);
    }
  };

  const loadStats = async () => {
    try {
      const resp = await fetch("/api/cms/stats");
      if (resp.ok) {
        const data: StatsResponse = await resp.json();
        setStats(data);
      }
    } catch (err) {
      console.error("Failed to load stats:", err);
    }
  };

  const handlePublish = async (docId: string) => {
    try {
      const resp = await fetch(`/api/cms/publish/${docId}`, { method: "POST" });
      if (resp.ok) {
        loadDocuments();
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
        loadDocuments();
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
          tenant_id: "devel",
          topic: draftTopic,
          tone: "professional",
          audience: "general",
          source_collections: ["devel_docs"],
          max_context_chunks: 5,
        }),
      });
      
      if (resp.ok) {
        const doc: Document = await resp.json();
        setDocuments([doc, ...documents]);
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
      const resp = await fetch("/api/cms/documents", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title, content, domain: "general" }),
      });
      
      if (resp.ok) {
        const doc: Document = await resp.json();
        setDocuments([doc, ...documents]);
        loadStats();
      }
    } catch (err) {
      console.error("Create failed:", err);
    }
  };

  return (
    <div style={{ display: "flex", height: "calc(100vh - 120px)", gap: "16px" }}>
      <Card variant="default" padding="md" style={{ width: "224px", flexShrink: 0 }}>
        <h2 style={{ marginBottom: "16px", fontSize: "14px", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em", color: "#64748b" }}>
          Navigation
        </h2>
        <nav style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
          {["all", "internal", "review", "public", "archived"].map((v) => (
            <Button
              key={v}
              variant={filter === v ? "primary" : "ghost"}
              size="sm"
              fullWidth
              onClick={() => setFilter(v)}
            >
              {v === "all" ? "All Documents" : v.charAt(0).toUpperCase() + v.slice(1)}
              {stats?.by_visibility[v] !== undefined && (
                <span style={{ marginLeft: "8px", fontSize: "12px", opacity: 0.7 }}>
                  ({stats.by_visibility[v]})
                </span>
              )}
            </Button>
          ))}
        </nav>

        <hr style={{ margin: "16px 0", border: "none", borderTop: "1px solid #e2e8f0" }} />

        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          <Button variant="primary" size="sm" fullWidth onClick={handleCreateDocument}>
            + New Document
          </Button>
          <Button variant="primary" size="sm" fullWidth onClick={() => setShowDraftPanel(!showDraftPanel)}>
            ✨ Draft with AI
          </Button>
        </div>

        {stats && (
          <Card variant="outlined" padding="sm" style={{ marginTop: "24px" }}>
            <h3 style={{ fontSize: "12px", fontWeight: 600, textTransform: "uppercase", color: "#64748b" }}>Stats</h3>
            <p style={{ marginTop: "8px", fontSize: "24px", fontWeight: 700 }}>{stats.total}</p>
            <p style={{ fontSize: "12px", color: "#64748b" }}>total documents</p>
          </Card>
        )}
      </Card>

      <div style={{ flex: 1, overflow: "hidden" }}>
        <div style={{ display: "flex", height: "100%", gap: "16px" }}>
          <Card
            variant="default"
            padding="none"
            style={{ flex: 1, overflow: "auto", width: showDraftPanel ? "50%" : undefined }}
          >
            <div style={{ position: "sticky", top: 0, zIndex: 10, borderBottom: "1px solid #e2e8f0", background: "rgba(255,255,255,0.95)", backdropFilter: "blur(8px)", padding: "16px" }}>
              <h2 style={{ fontSize: "18px", fontWeight: 600 }}>Content Library</h2>
              <p style={{ fontSize: "14px", color: "#64748b" }}>
                {loading ? "Loading..." : `${documents.length} documents`}
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
                    borderBottom: "1px solid #f1f5f9",
                    background: selectedDoc?.doc_id === doc.doc_id ? "rgba(59, 130, 246, 0.1)" : "transparent",
                    transition: "background 0.15s",
                  }}
                >
                  <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: "12px" }}>
                    <div style={{ minWidth: 0, flex: 1 }}>
                      <h3 style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", fontWeight: 500 }}>{doc.title}</h3>
                      <p style={{ marginTop: "4px", overflow: "hidden", display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", fontSize: "14px", color: "#64748b" }}>
                        {doc.content.slice(0, 150)}...
                      </p>
                    </div>
                    <Badge variant={VISIBILITY_VARIANTS[doc.visibility]} size="sm" rounded>
                      {VISIBILITY_ICONS[doc.visibility]} {doc.visibility}
                    </Badge>
                  </div>
                  
                  <div style={{ marginTop: "8px", display: "flex", alignItems: "center", gap: "8px", fontSize: "12px", color: "#94a3b8" }}>
                    <span>{doc.source}</span>
                    <span>•</span>
                    <span>{new Date(doc.created_at).toLocaleDateString()}</span>
                    {doc.ai_drafted && (
                      <>
                        <span>•</span>
                        <span style={{ color: "#a855f7" }}>AI-drafted</span>
                      </>
                    )}
                  </div>
                </div>
              ))}

              {documents.length === 0 && !loading && (
                <div style={{ padding: "32px", textAlign: "center", color: "#64748b" }}>
                  <p>No documents found.</p>
                  <p style={{ marginTop: "4px", fontSize: "14px" }}>Create a new document or generate an AI draft.</p>
                </div>
              )}
            </div>
          </Card>

          {showDraftPanel && (
            <Card variant="default" padding="md" style={{ width: "320px", flexShrink: 0, overflow: "auto" }}>
              <h3 style={{ fontSize: "18px", fontWeight: 600 }}>AI Draft Assistant</h3>
              <p style={{ marginTop: "4px", fontSize: "14px", color: "#64748b" }}>
                Generate content from your knowledge base
              </p>

              <div style={{ marginTop: "16px", display: "flex", flexDirection: "column", gap: "16px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "14px", fontWeight: 500 }}>Topic</label>
                  <textarea
                    value={draftTopic}
                    onChange={(e) => setDraftTopic(e.target.value)}
                    placeholder="What should the document be about?"
                    style={{ marginTop: "4px", width: "100%", borderRadius: "6px", border: "1px solid #d1d5db", padding: "8px 12px", fontSize: "14px", resize: "vertical" }}
                    rows={3}
                  />
                </div>

                <div>
                  <label style={{ display: "block", fontSize: "14px", fontWeight: 500 }}>Sources</label>
                  <select style={{ marginTop: "4px", width: "100%", borderRadius: "6px", border: "1px solid #d1d5db", padding: "8px 12px", fontSize: "14px" }}>
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

              <hr style={{ margin: "16px 0", border: "none", borderTop: "1px solid #e2e8f0" }} />

              <Card variant="outlined" padding="sm">
                <h4 style={{ fontSize: "14px", fontWeight: 500 }}>How it works</h4>
                <ol style={{ marginTop: "8px", display: "flex", flexDirection: "column", gap: "4px", fontSize: "12px", color: "#64748b", paddingLeft: "20px" }}>
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

              <div style={{ marginTop: "16px", maxHeight: "256px", overflow: "auto", borderRadius: "6px", background: "#f8fafc", padding: "12px", fontSize: "14px" }}>
                {selectedDoc.content}
              </div>

              <div style={{ marginTop: "16px", display: "flex", flexDirection: "column", gap: "8px" }}>
                {selectedDoc.visibility === "internal" && (
                  <Button variant="primary" fullWidth onClick={() => handlePublish(selectedDoc.doc_id)}>
                    🚀 Publish to Public
                  </Button>
                )}
                
                {selectedDoc.visibility === "review" && (
                  <Button variant="primary" fullWidth onClick={() => handlePublish(selectedDoc.doc_id)}>
                    ✅ Approve & Publish
                  </Button>
                )}

                {selectedDoc.visibility === "public" && (
                  <Button variant="danger" fullWidth onClick={() => handleArchive(selectedDoc.doc_id)}>
                    📦 Archive
                  </Button>
                )}

                {selectedDoc.visibility === "archived" && (
                  <Button variant="primary" fullWidth onClick={() => handlePublish(selectedDoc.doc_id)}>
                    🔄 Re-publish
                  </Button>
                )}
              </div>

              <div style={{ marginTop: "16px", display: "flex", flexDirection: "column", gap: "4px", fontSize: "12px", color: "#64748b" }}>
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
