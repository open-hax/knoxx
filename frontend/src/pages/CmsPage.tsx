import { useEffect, useMemo, useState } from "react";
import { Badge, Button, Card, Input } from "@open-hax/uxx";

interface OpenPlannerDocument {
  id: string;
  title: string;
  content: string;
  project: string;
  kind: "docs" | "code" | "config" | "data";
  visibility: "internal" | "review" | "public" | "archived";
  source?: string;
  sourcePath?: string;
  domain?: string;
  language?: string;
  createdBy?: string;
  publishedBy?: string | null;
  publishedAt?: string | null;
  aiDrafted?: boolean;
  aiModel?: string | null;
  metadata?: Record<string, unknown>;
  ts: string;
}

interface CmsDocument {
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

interface OpenPlannerDocumentListResponse {
  rows: OpenPlannerDocument[];
}

interface Garden {
  garden_id: string;
  title: string;
  purpose: string;
}

interface GardensResponse {
  gardens: Garden[];
}

const PROJECT = "devel";
const KIND = "docs";

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

function toCmsDocument(doc: OpenPlannerDocument): CmsDocument {
  return {
    doc_id: doc.id,
    tenant_id: doc.project || PROJECT,
    title: doc.title,
    content: doc.content,
    visibility: doc.visibility,
    source: doc.source || "manual",
    source_path: doc.sourcePath || null,
    domain: doc.domain || "general",
    language: doc.language || "en",
    created_by: doc.createdBy || "unknown",
    published_by: doc.publishedBy || null,
    published_at: doc.publishedAt || null,
    ai_drafted: Boolean(doc.aiDrafted),
    ai_model: doc.aiModel || null,
    metadata: doc.metadata || {},
    created_at: doc.ts,
    updated_at: doc.ts,
  };
}

function markdownSeed(title: string) {
  const safeTitle = title.trim() || "Untitled document";
  return `# ${safeTitle}\n\n`;
}

function getGardenId(doc: CmsDocument | null) {
  const value = doc?.metadata?.garden_id;
  return typeof value === "string" ? value : "";
}

export default function CmsPage() {
  const [allDocuments, setAllDocuments] = useState<CmsDocument[]>([]);
  const [gardens, setGardens] = useState<Garden[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [filter, setFilter] = useState<string>("all");
  const [selectedDocId, setSelectedDocId] = useState<string | null>(null);
  const [showDraftPanel, setShowDraftPanel] = useState(false);
  const [draftTopic, setDraftTopic] = useState("");
  const [draftGardenId, setDraftGardenId] = useState("");
  const [editorTitle, setEditorTitle] = useState("");
  const [editorContent, setEditorContent] = useState("");
  const [editorGardenId, setEditorGardenId] = useState("");
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const stats = useMemo(() => {
    const by_visibility = allDocuments.reduce<Record<string, number>>((acc, doc) => {
      acc[doc.visibility] = (acc[doc.visibility] || 0) + 1;
      return acc;
    }, {});
    const by_domain = allDocuments.reduce<Record<string, number>>((acc, doc) => {
      acc[doc.domain] = (acc[doc.domain] || 0) + 1;
      return acc;
    }, {});
    return { total: allDocuments.length, by_visibility, by_domain };
  }, [allDocuments]);

  const documents = useMemo(() => {
    if (filter === "all") return allDocuments;
    return allDocuments.filter((doc) => doc.visibility === filter);
  }, [allDocuments, filter]);

  const selectedDoc = useMemo(
    () => documents.find((doc) => doc.doc_id === selectedDocId) ?? allDocuments.find((doc) => doc.doc_id === selectedDocId) ?? null,
    [allDocuments, documents, selectedDocId],
  );

  useEffect(() => {
    void Promise.all([loadDocuments(), loadGardens()]);
  }, []);

  useEffect(() => {
    if (!selectedDocId && documents[0]) {
      setSelectedDocId(documents[0].doc_id);
    }
  }, [documents, selectedDocId]);

  useEffect(() => {
    if (!selectedDoc) {
      setEditorTitle("");
      setEditorContent("");
      setEditorGardenId("");
      return;
    }
    setEditorTitle(selectedDoc.title);
    setEditorContent(selectedDoc.content);
    setEditorGardenId(getGardenId(selectedDoc));
  }, [selectedDoc]);

  async function loadDocuments() {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ project: PROJECT, kind: KIND, limit: "200" });
      const resp = await fetch(`/api/openplanner/v1/documents?${params.toString()}`);
      if (!resp.ok) throw new Error(`Failed to load documents: ${resp.status}`);
      const data: OpenPlannerDocumentListResponse = await resp.json();
      const nextDocs = (data.rows || []).map(toCmsDocument);
      setAllDocuments(nextDocs);
      setSelectedDocId((current) => current && nextDocs.some((doc) => doc.doc_id === current) ? current : nextDocs[0]?.doc_id ?? null);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setAllDocuments([]);
      setSelectedDocId(null);
    } finally {
      setLoading(false);
    }
  }

  async function loadGardens() {
    try {
      const resp = await fetch("/api/openplanner/v1/gardens");
      if (!resp.ok) throw new Error(`Failed to load gardens: ${resp.status}`);
      const data: GardensResponse = await resp.json();
      setGardens(data.gardens || []);
    } catch (err) {
      console.error("Failed to load gardens:", err);
      setGardens([]);
    }
  }

  async function createDocument(title: string, content: string, gardenId: string) {
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      const body = {
        document: {
          id: window.crypto.randomUUID(),
          title,
          content,
          project: PROJECT,
          kind: KIND,
          visibility: "internal",
          source: "manual",
          domain: "general",
          language: "en",
          createdBy: "knoxx-cms",
          metadata: {
            format: "markdown",
            ...(gardenId ? { garden_id: gardenId } : {}),
          },
        },
      };
      const resp = await fetch("/api/openplanner/v1/documents", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });
      if (!resp.ok) throw new Error(await resp.text());
      const data = await resp.json();
      const created = toCmsDocument(data.document as OpenPlannerDocument);
      setAllDocuments((current) => [created, ...current]);
      setSelectedDocId(created.doc_id);
      setShowDraftPanel(false);
      setDraftTopic("");
      setDraftGardenId("");
      setNotice(`Created markdown draft “${created.title}”.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  async function handleCreateDocument() {
    await createDocument("Untitled document", markdownSeed("Untitled document"), "");
  }

  async function handleCreateDraftFromTopic() {
    const title = draftTopic.trim();
    if (!title) return;
    await createDocument(title, markdownSeed(title), draftGardenId);
  }

  async function handleSaveSelected() {
    if (!selectedDoc) return;
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      const nextMetadata = {
        ...(selectedDoc.metadata || {}),
        format: "markdown",
        ...(editorGardenId ? { garden_id: editorGardenId } : { garden_id: null }),
      };
      const resp = await fetch(`/api/openplanner/v1/documents/${encodeURIComponent(selectedDoc.doc_id)}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: editorTitle,
          content: editorContent,
          metadata: nextMetadata,
          domain: selectedDoc.domain,
          language: selectedDoc.language,
        }),
      });
      if (!resp.ok) throw new Error(await resp.text());
      const data = await resp.json();
      const updated = toCmsDocument(data.document as OpenPlannerDocument);
      setAllDocuments((current) => current.map((doc) => (doc.doc_id === updated.doc_id ? updated : doc)));
      setNotice(`Saved “${updated.title}”.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  async function handlePublish(doc: CmsDocument) {
    const gardenId = selectedDoc?.doc_id === doc.doc_id ? editorGardenId : getGardenId(doc);
    if (!gardenId) {
      setError("Choose a garden target before publishing.");
      return;
    }

    if (selectedDoc?.doc_id === doc.doc_id) {
      await handleSaveSelected();
    }

    setPublishing(true);
    setError(null);
    setNotice(null);
    try {
      const resp = await fetch(`/api/openplanner/v1/documents/${encodeURIComponent(doc.doc_id)}/publish`, {
        method: "POST",
      });
      if (!resp.ok) throw new Error(await resp.text());
      await loadDocuments();
      setNotice(`Published “${doc.title}” to ${gardenId}.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setPublishing(false);
    }
  }

  async function handleArchive(doc: CmsDocument) {
    setPublishing(true);
    setError(null);
    setNotice(null);
    try {
      const resp = await fetch(`/api/openplanner/v1/documents/${encodeURIComponent(doc.doc_id)}/archive`, {
        method: "POST",
      });
      if (!resp.ok) throw new Error(await resp.text());
      await loadDocuments();
      setNotice(`Archived “${doc.title}”.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setPublishing(false);
    }
  }

  const publishButtonLabel = selectedDoc && editorGardenId
    ? `Publish to ${editorGardenId}`
    : "Publish to selected garden";

  return (
    <div style={{ display: "flex", height: "calc(100vh - 120px)", gap: "16px" }}>
      <Card variant="default" padding="md" style={{ width: "248px", flexShrink: 0 }}>
        <h2 style={{ marginBottom: "16px", fontSize: "14px", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em", color: "var(--token-colors-text-muted)" }}>
          CMS
        </h2>
        <p style={{ marginBottom: "16px", fontSize: "13px", color: "var(--token-colors-text-muted)" }}>
          Native CMS view over OpenPlanner documents in the canonical <code>devel</code> lake with <code>kind=docs</code>. No Python compatibility layer.
        </p>

        <nav style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
          {["all", "internal", "review", "public", "archived"].map((value) => (
            <Button
              key={value}
              variant={filter === value ? "primary" : "ghost"}
              size="sm"
              fullWidth
              onClick={() => setFilter(value)}
            >
              {value === "all" ? "All Documents" : value.charAt(0).toUpperCase() + value.slice(1)}
              {stats.by_visibility[value] !== undefined ? (
                <span style={{ marginLeft: "8px", fontSize: "12px", opacity: 0.7 }}>
                  ({stats.by_visibility[value]})
                </span>
              ) : null}
            </Button>
          ))}
        </nav>

        <hr style={{ margin: "16px 0", border: "none", borderTop: "1px solid var(--token-colors-border-default)" }} />

        <div style={{ display: "flex", flexDirection: "column", gap: "8px" }}>
          <Button variant="primary" size="sm" fullWidth onClick={() => void handleCreateDocument()} disabled={saving}>
            + New Markdown Doc
          </Button>
          <Button variant="secondary" size="sm" fullWidth onClick={() => setShowDraftPanel((current) => !current)}>
            {showDraftPanel ? "Hide Draft Panel" : "+ Topic Draft"}
          </Button>
          <Button variant="ghost" size="sm" fullWidth onClick={() => void loadDocuments()} disabled={loading}>
            Refresh Corpus
          </Button>
        </div>

        <Card variant="outlined" padding="sm" style={{ marginTop: "24px" }}>
          <h3 style={{ fontSize: "12px", fontWeight: 600, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Stats</h3>
          <p style={{ marginTop: "8px", fontSize: "24px", fontWeight: 700 }}>{stats.total}</p>
          <p style={{ fontSize: "12px", color: "var(--token-colors-text-muted)" }}>documents in devel/docs</p>
        </Card>
      </Card>

      <div style={{ flex: 1, overflow: "hidden" }}>
        {notice ? <div style={{ marginBottom: "12px", borderRadius: "8px", border: "1px solid rgba(16,185,129,.35)", background: "rgba(16,185,129,.08)", padding: "12px", color: "#86efac" }}>{notice}</div> : null}
        {error ? <div style={{ marginBottom: "12px", borderRadius: "8px", border: "1px solid rgba(244,63,94,.35)", background: "rgba(244,63,94,.08)", padding: "12px", color: "#fda4af" }}>{error}</div> : null}

        <div style={{ display: "flex", height: "100%", gap: "16px" }}>
          <Card variant="default" padding="none" style={{ flex: 1, overflow: "auto" }}>
            <div style={{ position: "sticky", top: 0, zIndex: 10, borderBottom: "1px solid var(--token-colors-border-default)", background: "var(--token-colors-background-surface)", backdropFilter: "blur(8px)", padding: "16px" }}>
              <h2 style={{ fontSize: "18px", fontWeight: 600 }}>Content Library</h2>
              <p style={{ fontSize: "14px", color: "var(--token-colors-text-muted)" }}>
                {loading ? "Loading markdown corpus..." : `${documents.length} documents`}
              </p>
            </div>

            <div style={{ display: "flex", flexDirection: "column" }}>
              {documents.map((doc) => (
                <div
                  key={doc.doc_id}
                  onClick={() => setSelectedDocId(doc.doc_id)}
                  style={{
                    cursor: "pointer",
                    padding: "16px",
                    borderBottom: "1px solid var(--token-colors-alpha-bg-_08)",
                    background: selectedDocId === doc.doc_id ? "var(--token-colors-alpha-blue-_15)" : "transparent",
                    transition: "background 0.15s",
                  }}
                >
                  <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: "12px" }}>
                    <div style={{ minWidth: 0, flex: 1 }}>
                      <h3 style={{ overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", fontWeight: 500 }}>{doc.title}</h3>
                      <p style={{ marginTop: "4px", overflow: "hidden", display: "-webkit-box", WebkitLineClamp: 2, WebkitBoxOrient: "vertical", fontSize: "14px", color: "var(--token-colors-text-muted)" }}>
                        {doc.content.slice(0, 180)}
                      </p>
                    </div>
                    <Badge variant={VISIBILITY_VARIANTS[doc.visibility]} size="sm" rounded>
                      {VISIBILITY_ICONS[doc.visibility]} {doc.visibility}
                    </Badge>
                  </div>

                  <div style={{ marginTop: "8px", display: "flex", alignItems: "center", gap: "8px", flexWrap: "wrap", fontSize: "12px", color: "var(--token-colors-text-muted)" }}>
                    <span>{doc.source}</span>
                    <span>•</span>
                    <span>{new Date(doc.created_at).toLocaleDateString()}</span>
                    {getGardenId(doc) ? (
                      <>
                        <span>•</span>
                        <span>garden: {getGardenId(doc)}</span>
                      </>
                    ) : null}
                    {doc.source_path ? (
                      <>
                        <span>•</span>
                        <span style={{ maxWidth: "240px", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{doc.source_path}</span>
                      </>
                    ) : null}
                  </div>
                </div>
              ))}

              {documents.length === 0 && !loading ? (
                <div style={{ padding: "32px", textAlign: "center", color: "var(--token-colors-text-muted)" }}>
                  <p>No documents found in the canonical devel/docs lake.</p>
                  <p style={{ marginTop: "4px", fontSize: "14px" }}>Create a markdown doc or run ingestion into project=devel, kind=docs.</p>
                </div>
              ) : null}
            </div>
          </Card>

          {showDraftPanel ? (
            <Card variant="default" padding="md" style={{ width: "360px", flexShrink: 0, overflow: "auto" }}>
              <h3 style={{ fontSize: "18px", fontWeight: 600 }}>Topic Draft</h3>
              <p style={{ marginTop: "4px", fontSize: "14px", color: "var(--token-colors-text-muted)" }}>
                Python AI draft flow is retired from this surface. Create a native markdown draft directly in OpenPlanner.
              </p>

              <div style={{ marginTop: "16px", display: "flex", flexDirection: "column", gap: "16px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "14px", fontWeight: 500 }}>Topic / title</label>
                  <Input value={draftTopic} onChange={(event) => setDraftTopic(event.target.value)} placeholder="What should this document be about?" />
                </div>

                <div>
                  <label style={{ display: "block", fontSize: "14px", fontWeight: 500 }}>Garden target</label>
                  <select value={draftGardenId} onChange={(event) => setDraftGardenId(event.target.value)} style={{ marginTop: "4px", width: "100%", borderRadius: "6px", border: "1px solid var(--token-colors-border-subtle)", padding: "8px 12px", fontSize: "14px" }}>
                    <option value="">Unassigned</option>
                    {gardens.map((garden) => (
                      <option key={garden.garden_id} value={garden.garden_id}>{garden.title} ({garden.garden_id})</option>
                    ))}
                  </select>
                  <p style={{ marginTop: "6px", fontSize: "12px", color: "var(--token-colors-text-muted)" }}>
                    Current garden catalog is still operator-facing; this metadata is a bridge until garden publishing becomes first-class.
                  </p>
                </div>

                <Button variant="primary" fullWidth loading={saving} disabled={!draftTopic.trim()} onClick={() => void handleCreateDraftFromTopic()}>
                  Create Markdown Draft
                </Button>
              </div>
            </Card>
          ) : selectedDoc ? (
            <Card variant="default" padding="md" style={{ width: "460px", flexShrink: 0, overflow: "auto" }}>
              <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", gap: "12px" }}>
                <div style={{ flex: 1 }}>
                  <Input value={editorTitle} onChange={(event) => setEditorTitle(event.target.value)} placeholder="Document title" />
                </div>
                <Button variant="ghost" size="sm" onClick={() => setSelectedDocId(null)}>✕</Button>
              </div>

              <div style={{ marginTop: "12px", display: "flex", gap: "8px", flexWrap: "wrap" }}>
                <Badge variant={VISIBILITY_VARIANTS[selectedDoc.visibility]} size="sm" rounded>
                  {VISIBILITY_ICONS[selectedDoc.visibility]} {selectedDoc.visibility}
                </Badge>
                <Badge variant="default" size="sm">{selectedDoc.source}</Badge>
                <Badge variant="default" size="sm">{selectedDoc.language}</Badge>
              </div>

              <div style={{ marginTop: "16px" }}>
                <label style={{ display: "block", marginBottom: "6px", fontSize: "13px", fontWeight: 600 }}>Garden target</label>
                <select value={editorGardenId} onChange={(event) => setEditorGardenId(event.target.value)} style={{ width: "100%", borderRadius: "6px", border: "1px solid var(--token-colors-border-subtle)", padding: "8px 12px", fontSize: "14px" }}>
                  <option value="">Unassigned</option>
                  {gardens.map((garden) => (
                    <option key={garden.garden_id} value={garden.garden_id}>{garden.title} ({garden.garden_id})</option>
                  ))}
                </select>
              </div>

              <div style={{ marginTop: "16px" }}>
                <label style={{ display: "block", marginBottom: "6px", fontSize: "13px", fontWeight: 600 }}>Markdown</label>
                <textarea
                  value={editorContent}
                  onChange={(event) => setEditorContent(event.target.value)}
                  rows={20}
                  style={{ width: "100%", borderRadius: "8px", border: "1px solid var(--token-colors-border-subtle)", padding: "12px", fontSize: "14px", fontFamily: "monospace", resize: "vertical", background: "var(--token-colors-background-surface)" }}
                />
              </div>

              <div style={{ marginTop: "16px", display: "flex", flexDirection: "column", gap: "8px" }}>
                <Button variant="secondary" fullWidth disabled={saving || !selectedDoc} onClick={() => void handleSaveSelected()}>
                  {saving ? "Saving..." : "Save Markdown"}
                </Button>

                {(selectedDoc.visibility === "internal" || selectedDoc.visibility === "review" || selectedDoc.visibility === "archived") ? (
                  <Button variant="primary" fullWidth disabled={publishing} onClick={() => void handlePublish(selectedDoc)}>
                    {publishing ? "Publishing..." : publishButtonLabel}
                  </Button>
                ) : null}

                {selectedDoc.visibility === "public" ? (
                  <Button variant="danger" fullWidth disabled={publishing} onClick={() => void handleArchive(selectedDoc)}>
                    {publishing ? "Archiving..." : "Archive"}
                  </Button>
                ) : null}
              </div>

              <div style={{ marginTop: "16px", display: "flex", flexDirection: "column", gap: "4px", fontSize: "12px", color: "var(--token-colors-text-muted)" }}>
                <p>Created: {new Date(selectedDoc.created_at).toLocaleString()}</p>
                <p>By: {selectedDoc.created_by}</p>
                {selectedDoc.source_path ? <p>Source: {selectedDoc.source_path}</p> : null}
                {selectedDoc.published_at ? <p>Published: {new Date(selectedDoc.published_at).toLocaleString()}</p> : null}
              </div>
            </Card>
          ) : null}
        </div>
      </div>
    </div>
  );
}
