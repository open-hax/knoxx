import { useEffect, useState } from "react";

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

const VISIBILITY_COLORS: Record<string, string> = {
  internal: "bg-slate-100 text-slate-700 dark:bg-slate-700 dark:text-slate-300",
  review: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
  public: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  archived: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
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
    <div className="flex h-[calc(100vh-120px)] gap-4">
      {/* Sidebar */}
      <div className="w-56 flex-shrink-0 rounded-lg border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-800">
        <h2 className="mb-4 text-sm font-semibold uppercase tracking-wide text-slate-500">
          Navigation
        </h2>
        <nav className="space-y-1">
          {["all", "internal", "review", "public", "archived"].map((v) => (
            <button
              key={v}
              onClick={() => setFilter(v)}
              className={`w-full rounded-md px-3 py-2 text-left text-sm transition-colors ${
                filter === v
                  ? "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400"
                  : "text-slate-600 hover:bg-slate-100 dark:text-slate-400 dark:hover:bg-slate-700"
              }`}
            >
              {v === "all" ? "All Documents" : v.charAt(0).toUpperCase() + v.slice(1)}
              {stats?.by_visibility[v] !== undefined && (
                <span className="ml-2 text-xs text-slate-400">
                  ({stats.by_visibility[v]})
                </span>
              )}
            </button>
          ))}
        </nav>

        <hr className="my-4 border-slate-200 dark:border-slate-700" />

        <div className="space-y-2">
          <button
            onClick={handleCreateDocument}
            className="w-full rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700"
          >
            + New Document
          </button>
          <button
            onClick={() => setShowDraftPanel(!showDraftPanel)}
            className="w-full rounded-md bg-purple-600 px-3 py-2 text-sm font-medium text-white hover:bg-purple-700"
          >
            ✨ Draft with AI
          </button>
        </div>

        {stats && (
          <div className="mt-6 rounded-md bg-slate-50 p-3 dark:bg-slate-900">
            <h3 className="text-xs font-semibold uppercase text-slate-500">Stats</h3>
            <p className="mt-2 text-2xl font-bold">{stats.total}</p>
            <p className="text-xs text-slate-500">total documents</p>
          </div>
        )}
      </div>

      {/* Main content */}
      <div className="flex-1 overflow-hidden">
        <div className="flex h-full gap-4">
          {/* Document list */}
          <div className={`flex-1 overflow-auto rounded-lg border border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-800 ${showDraftPanel ? "w-1/2" : ""}`}>
            <div className="sticky top-0 z-10 border-b border-slate-200 bg-white/95 p-4 backdrop-blur dark:border-slate-700 dark:bg-slate-800/95">
              <h2 className="text-lg font-semibold">Content Library</h2>
              <p className="text-sm text-slate-500">
                {loading ? "Loading..." : `${documents.length} documents`}
              </p>
            </div>

            <div className="divide-y divide-slate-100 dark:divide-slate-700">
              {documents.map((doc) => (
                <div
                  key={doc.doc_id}
                  onClick={() => setSelectedDoc(doc)}
                  className={`cursor-pointer p-4 transition-colors hover:bg-slate-50 dark:hover:bg-slate-700 ${
                    selectedDoc?.doc_id === doc.doc_id ? "bg-blue-50 dark:bg-blue-900/20" : ""
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <h3 className="truncate font-medium">{doc.title}</h3>
                      <p className="mt-1 line-clamp-2 text-sm text-slate-500">
                        {doc.content.slice(0, 150)}...
                      </p>
                    </div>
                    <span
                      className={`flex-shrink-0 rounded-full px-2 py-1 text-xs font-medium ${
                        VISIBILITY_COLORS[doc.visibility]
                      }`}
                    >
                      {VISIBILITY_ICONS[doc.visibility]} {doc.visibility}
                    </span>
                  </div>
                  
                  <div className="mt-2 flex items-center gap-2 text-xs text-slate-400">
                    <span>{doc.source}</span>
                    <span>•</span>
                    <span>{new Date(doc.created_at).toLocaleDateString()}</span>
                    {doc.ai_drafted && (
                      <>
                        <span>•</span>
                        <span className="text-purple-500">AI-drafted</span>
                      </>
                    )}
                  </div>
                </div>
              ))}

              {documents.length === 0 && !loading && (
                <div className="p-8 text-center text-slate-500">
                  <p>No documents found.</p>
                  <p className="mt-1 text-sm">Create a new document or generate an AI draft.</p>
                </div>
              )}
            </div>
          </div>

          {/* Draft panel */}
          {showDraftPanel && (
            <div className="w-80 flex-shrink-0 overflow-auto rounded-lg border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-800">
              <h3 className="text-lg font-semibold">AI Draft Assistant</h3>
              <p className="mt-1 text-sm text-slate-500">
                Generate content from your knowledge base
              </p>

              <div className="mt-4 space-y-4">
                <div>
                  <label className="block text-sm font-medium">Topic</label>
                  <textarea
                    value={draftTopic}
                    onChange={(e) => setDraftTopic(e.target.value)}
                    placeholder="What should the document be about?"
                    className="mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700"
                    rows={3}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium">Sources</label>
                  <select className="mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm dark:border-slate-600 dark:bg-slate-700">
                    <option>devel_docs (all internal docs)</option>
                    <option>devel_specs (design docs)</option>
                  </select>
                </div>

                <button
                  onClick={handleGenerateDraft}
                  disabled={drafting || !draftTopic.trim()}
                  className="w-full rounded-md bg-purple-600 px-4 py-2 font-medium text-white hover:bg-purple-700 disabled:opacity-50"
                >
                  {drafting ? "Generating..." : "Generate Draft"}
                </button>
              </div>

              <hr className="my-4 border-slate-200 dark:border-slate-700" />

              <div className="rounded-md bg-slate-50 p-3 dark:bg-slate-900">
                <h4 className="text-sm font-medium">How it works</h4>
                <ol className="mt-2 space-y-1 text-xs text-slate-500">
                  <li>1. AI searches your knowledge base</li>
                  <li>2. Generates a draft document</li>
                  <li>3. You review and edit</li>
                  <li>4. Publish to make public</li>
                </ol>
              </div>
            </div>
          )}

          {/* Document detail panel */}
          {selectedDoc && !showDraftPanel && (
            <div className="w-96 flex-shrink-0 overflow-auto rounded-lg border border-slate-200 bg-white p-4 dark:border-slate-700 dark:bg-slate-800">
              <div className="flex items-start justify-between">
                <h3 className="text-lg font-semibold">{selectedDoc.title}</h3>
                <button
                  onClick={() => setSelectedDoc(null)}
                  className="text-slate-400 hover:text-slate-600"
                >
                  ✕
                </button>
              </div>

              <div className="mt-2 flex gap-2">
                <span
                  className={`rounded-full px-2 py-1 text-xs font-medium ${
                    VISIBILITY_COLORS[selectedDoc.visibility]
                  }`}
                >
                  {VISIBILITY_ICONS[selectedDoc.visibility]} {selectedDoc.visibility}
                </span>
                <span className="rounded-full bg-slate-100 px-2 py-1 text-xs dark:bg-slate-700">
                  {selectedDoc.source}
                </span>
              </div>

              <div className="mt-4 max-h-64 overflow-auto rounded-md bg-slate-50 p-3 text-sm dark:bg-slate-900">
                {selectedDoc.content}
              </div>

              <div className="mt-4 space-y-2">
                {selectedDoc.visibility === "internal" && (
                  <button
                    onClick={() => handlePublish(selectedDoc.doc_id)}
                    className="w-full rounded-md bg-green-600 px-4 py-2 font-medium text-white hover:bg-green-700"
                  >
                    🚀 Publish to Public
                  </button>
                )}
                
                {selectedDoc.visibility === "review" && (
                  <button
                    onClick={() => handlePublish(selectedDoc.doc_id)}
                    className="w-full rounded-md bg-green-600 px-4 py-2 font-medium text-white hover:bg-green-700"
                  >
                    ✅ Approve & Publish
                  </button>
                )}

                {selectedDoc.visibility === "public" && (
                  <button
                    onClick={() => handleArchive(selectedDoc.doc_id)}
                    className="w-full rounded-md bg-red-600 px-4 py-2 font-medium text-white hover:bg-red-700"
                  >
                    📦 Archive
                  </button>
                )}

                {selectedDoc.visibility === "archived" && (
                  <button
                    onClick={() => handlePublish(selectedDoc.doc_id)}
                    className="w-full rounded-md bg-blue-600 px-4 py-2 font-medium text-white hover:bg-blue-700"
                  >
                    🔄 Re-publish
                  </button>
                )}
              </div>

              <div className="mt-4 space-y-1 text-xs text-slate-500">
                <p>Created: {new Date(selectedDoc.created_at).toLocaleString()}</p>
                <p>By: {selectedDoc.created_by}</p>
                {selectedDoc.source_path && <p>Source: {selectedDoc.source_path}</p>}
                {selectedDoc.published_at && (
                  <p>Published: {new Date(selectedDoc.published_at).toLocaleString()}</p>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

export default CmsPage;
