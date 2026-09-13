import { useEffect, useMemo, useState, useRef, useCallback } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { Button, Badge } from "@open-hax/uxx";
import ReactMarkdown from "react-markdown";
import { CollapsedPanelTab } from "../components/CollapsedPanelTab";
import {
  findDocumentBySourcePath,
  listPublicationTopology,
  publicationForGarden,
  publishedGardenIdsFor,
  setPublicationState,
  type CmsListWire,
} from "../lib/api/publications";
import { ChatWorkspacePane } from "../components/chat-page/ChatWorkspacePane";
import { createSidebarResizeHandlers } from "../components/chat-page/sidebar-resize";
import { useChatWorkspaceController } from "../components/chat-page/useChatWorkspaceController";
import { ContextBar } from "../components/context-bar";
import { isWorkspaceSource } from "../components/workspace-context/utils";
import { PublicationBlocksRenderer, extractPublicationBlocks } from "../components/cms/PublicationBlocksRenderer";
import { CreateVisualDraftModal } from "../components/cms/CreateVisualDraftModal";
import type { CmsTemplateOption } from "../components/cms/CreateVisualDraftModal";
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
  IngestionSource,
} from "../components/context-bar/types";
import type { AgentSource, MemorySessionSummary } from "../lib/types";
import styles from "./CmsPage.module.css";

const CHAT_SIDEBAR_WIDTH_KEY = "knoxx_cms_sidebar_width_px";

/**
 * Opaque passthrough for the document upsert.
 *
 * The legacy garden-membership metadata field is deliberately NOT declared here.
 * Publication state is no longer read from document metadata at all — it comes
 * from the resource-backed publication topology, where `desired` is contract
 * intent and `observed` is runtime evidence.
 */
type CmsDocMetadata = Record<string, unknown>;

type CmsDocSummary = {
  doc_id: string;
  garden_id: string;
  title: string;
  content?: string;
  source_path: string | null;
  visibility?: "internal" | "review" | "public" | "archived";
  metadata?: CmsDocMetadata;
  source_paths?: string[];
  revision: string;
  revision_heads: string[];
  conflicted: boolean;
};

type CmsRevision = CmsDocSummary & {
  actor: string;
  at: string;
  parents: string[];
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

const CMS_CHAT_ACTOR_ID = "cms_chat";
const CMS_CHAT_SESSION_KEY = "knoxx_cms_chat_session_id";
const CMS_CHAT_SCRATCHPAD_KEY = "knoxx_cms_chat_scratchpad_state";
const CMS_CHAT_PINNED_KEY = "knoxx_cms_chat_pinned_context";
const CMS_CHAT_SESSION_STATE_KEY = "knoxx_cms_chat_session_state";
const CMS_CHAT_SIDEBAR_WIDTH_KEY = "knoxx_cms_chat_sidebar_width_px";

function CmsPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const chat = useChatWorkspaceController({
    initialShowCanvas: false,
    defaultActorId: CMS_CHAT_ACTOR_ID,
    sessionIdKey: CMS_CHAT_SESSION_KEY,
    scratchpadStorageKey: CMS_CHAT_SCRATCHPAD_KEY,
    pinnedContextStorageKey: CMS_CHAT_PINNED_KEY,
    sessionStateKey: CMS_CHAT_SESSION_STATE_KEY,
    sidebarWidthKey: CMS_CHAT_SIDEBAR_WIDTH_KEY,
  });

  // Editor state
  const [editorTitle, setEditorTitle] = useState("");
  const [editorBody, setEditorBody] = useState("");
  const [editorPath, setEditorPath] = useState<string | null>(null);
  const [isNewDraft, setIsNewDraft] = useState(false);
  const [editorStatus, setEditorStatus] = useState<DocumentStatus>("draft");
  const [isDirty, setIsDirty] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [lastSaveMessage, setLastSaveMessage] = useState<string | null>(null);
  const [publicationTopology, setPublicationTopology] = useState<CmsListWire | null>(null);
  const [selectedGardenId, setSelectedGardenId] = useState("");
  const [cmsDocuments, setCmsDocuments] = useState<CmsDocSummary[]>([]);
  const [loadingCmsDocuments, setLoadingCmsDocuments] = useState(false);
  const [cmsDocId, setCmsDocId] = useState<string | null>(null);
  const [cmsMetadata, setCmsMetadata] = useState<CmsDocMetadata>({});
  // This is the revision whose content the editor actually loaded. A list or
  // history refresh must never advance it behind an editor's unsaved changes.
  const [cmsRevision, setCmsRevision] = useState<string | null>(null);
  const [cmsHeads, setCmsHeads] = useState<string[]>([]);
  const [cmsConflicted, setCmsConflicted] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [history, setHistory] = useState<CmsRevision[]>([]);
  const [previewRevision, setPreviewRevision] = useState<string | null>(null);
  const [reviewedHeads, setReviewedHeads] = useState<string[]>([]);
  const historyRequestRef = useRef(0);
  const documentListRequestRef = useRef(0);
  // Loads, navigation, and edits invalidate older work against the editor.
  // Ledger writes may finish, but their responses cannot replace a newer view.
  const editorRequestRef = useRef(0);
  useEffect(() => () => {
    editorRequestRef.current += 1;
    historyRequestRef.current += 1;
  }, []);

  // Visual draft creation state
  const [showCreateDraftModal, setShowCreateDraftModal] = useState(false);
  const [cmsTemplates, setCmsTemplates] = useState<CmsTemplateOption[]>([]);
  const [creatingDraft, setCreatingDraft] = useState(false);

  // ContextBar state
  const [showFiles, setShowFiles] = useState(true);
  const [showChatPanel, setShowChatPanel] = useState(true);
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
        params.set("path", ".");
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

  const loadPublicationTopology = useCallback(async () => {
    try {
      const topology = await listPublicationTopology();
      setPublicationTopology(topology);
      const publishable = topology.gardens.filter((garden) => garden.status === "active");
      if (publishable.length > 0) {
        setSelectedGardenId((current) => current || publishable[0].id);
      }
      return topology;
    } catch {
      setPublicationTopology(null);
      return null;
    }
  }, []);

  useEffect(() => {
    void loadPublicationTopology();
  }, [loadPublicationTopology]);

  useEffect(() => {
    const loadWorkspaceStatus = async () => {
      try {
        const sourcesResp = await fetch("/api/ingestion/sources");
        if (!sourcesResp.ok) return;
        const sources = (await sourcesResp.json()) as IngestionSource[];
        const source = sources.find(isWorkspaceSource);
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

  // Load CMS templates from contract
  useEffect(() => {
    const loadTemplates = async () => {
      try {
        const resp = await fetch("/api/ingestion/file?path=contracts/cms-templates.edn");
        if (!resp.ok) return;
        const data = (await resp.json()) as { content?: string };
        const content = data.content ?? "";
        // Simple EDN parsing: extract template keys and labels
        const templates: CmsTemplateOption[] = [];
        const templateMatches = content.matchAll(/:([\w-]+)\s+\{[^}]*:label\s+"([^"]+)"/g);
        for (const match of templateMatches) {
          templates.push({ value: match[1], label: match[2] });
        }
        if (templates.length === 0) {
          // Fallback defaults if parsing fails
          templates.push(
            { value: "article-page", label: "Article" },
            { value: "studio-playlist-page", label: "Studio Playlist Page" },
            { value: "landing-page", label: "Landing Page" },
          );
        }
        setCmsTemplates(templates);
      } catch (err) {
        console.error("Failed to load CMS templates:", err);
        setCmsTemplates([
          { value: "article-page", label: "Article" },
          { value: "studio-playlist-page", label: "Studio Playlist Page" },
          { value: "landing-page", label: "Landing Page" },
        ]);
      }
    };
    void loadTemplates();
  }, []);

  const editorDirectory = editorPath?.includes("/") ? editorPath.slice(0, editorPath.lastIndexOf("/") + 1) : "";

  /**
   * The publication topology row for the document currently in the editor.
   * Derived, never stored: a second client-side authority is what let the old
   * CMS disagree with the resource graph.
   */
  const currentCmsDocument = useMemo(
    () => (editorPath ? findDocumentBySourcePath(publicationTopology, editorPath) : null),
    [publicationTopology, editorPath],
  );

  const publishedGardenIds = useMemo(
    () => publishedGardenIdsFor(currentCmsDocument),
    [currentCmsDocument],
  );

  const isPublishedToSelectedGarden = useMemo(
    () => Boolean(selectedGardenId && publishedGardenIds.includes(selectedGardenId)),
    [publishedGardenIds, selectedGardenId],
  );

  const findCmsDocumentByPath = useCallback(async (path: string) => {
    const normalize = (value: string) => value.replace(/^\/+/, "").replace(/^(?:\.\/)+/, "");
    // Snapshot paths are absolute; client-provided logical aliases are relative.
    // Search both exact forms without treating a failed request as a miss.
    for (const candidatePath of Array.from(new Set([path, normalize(path)]))) {
      const params = new URLSearchParams({ path_prefix: candidatePath, limit: "100" });
      const resp = await fetch(`/api/cms/documents?${params.toString()}`);
      if (!resp.ok) throw new Error(await resp.text());
      const body = (await resp.json()) as { documents?: CmsDocSummary[] };
      const match = (body.documents ?? []).find((doc) =>
        [doc.source_path, ...(doc.source_paths ?? [])].some((candidate) =>
          candidate != null && normalize(candidate) === normalize(path)));
      if (!match) continue;
      const documentResponse = await fetch(`/api/cms/documents/${encodeURIComponent(match.doc_id)}`);
      if (!documentResponse.ok) throw new Error(await documentResponse.text());
      return (await documentResponse.json()) as CmsDocSummary;
    }
    return null;
  }, []);

  const acceptCmsRevision = useCallback((doc: CmsDocSummary) => {
    setCmsDocId(doc.doc_id);
    setSelectedGardenId(doc.garden_id);
    setCmsMetadata(doc.metadata ?? {});
    setCmsRevision(doc.revision);
    setCmsHeads(doc.revision_heads);
    setCmsConflicted(doc.conflicted);
    setReviewedHeads([]);
  }, []);

  const clearCmsRevision = useCallback(() => {
    editorRequestRef.current += 1;
    historyRequestRef.current += 1;
    setLoadingHistory(false);
    setCmsDocId(null);
    setCmsMetadata({});
    setCmsRevision(null);
    setCmsHeads([]);
    setCmsConflicted(false);
    setShowHistory(false);
    setHistory([]);
    setPreviewRevision(null);
    setReviewedHeads([]);
  }, []);

  const applyCmsDocumentToEditor = useCallback((doc: CmsDocSummary, fallbackPath?: string) => {
    editorRequestRef.current += 1;
    historyRequestRef.current += 1;
    setLoadingHistory(false);
    const path = doc.source_path ?? fallbackPath ?? `cms/${doc.doc_id}.md`;
    setEditorTitle(doc.title);
    setEditorBody(doc.content ?? "");
    setEditorPath(path);
    setIsNewDraft(false);
    setEditorStatus(doc.visibility === "review" ? "review" : "draft");
    acceptCmsRevision(doc);
    setShowHistory(false);
    setHistory([]);
    setPreviewRevision(null);
    setIsDirty(false);
    setLastSaveMessage("Loaded CMS draft");
    chat.pinContextItem({
      id: path,
      title: doc.title,
      path,
      snippet: (doc.content ?? "").slice(0, 240),
      kind: "file",
    });
  }, [acceptCmsRevision, chat]);

  const loadCmsDocuments = useCallback(async () => {
    const requestId = ++documentListRequestRef.current;
    if (!selectedGardenId) {
      setCmsDocuments([]);
      return;
    }
    setLoadingCmsDocuments(true);
    try {
      const params = new URLSearchParams({ garden_id: selectedGardenId, limit: "100" });
      const resp = await fetch(`/api/cms/documents?${params.toString()}`);
      if (!resp.ok) return;
      const body = (await resp.json()) as { documents?: CmsDocSummary[] };
      if (documentListRequestRef.current === requestId) setCmsDocuments(body.documents ?? []);
    } catch (error) {
      console.error("Failed to load CMS documents:", error);
      if (documentListRequestRef.current === requestId) setCmsDocuments([]);
    } finally {
      if (documentListRequestRef.current === requestId) setLoadingCmsDocuments(false);
    }
  }, [selectedGardenId]);

  useEffect(() => {
    void loadCmsDocuments();
  }, [loadCmsDocuments]);

  const handleOpenCmsDocument = useCallback(async (docId: string) => {
    if (isSaving) return;
    const requestId = ++editorRequestRef.current;
    try {
      const resp = await fetch(`/api/cms/documents/${encodeURIComponent(docId)}`);
      if (!resp.ok) return;
      const document = (await resp.json()) as CmsDocSummary;
      if (editorRequestRef.current !== requestId) return;
      applyCmsDocumentToEditor(document);
    } catch (error) {
      console.error("Failed to open CMS document:", error);
    }
  }, [applyCmsDocumentToEditor, isSaving]);

  const buildEditorPath = useCallback(() => {
    const fileName = editorTitle.trim().replace(/[\\/]+/g, "-");
    return `${editorDirectory}${fileName}`;
  }, [editorDirectory, editorTitle]);

  const persistEditorFile = useCallback(async (parents?: string[]) => {
    // Imported workspace files retain their original logical identifier even
    // when their title changes before the first CMS save.
    const path = isNewDraft ? buildEditorPath() : editorPath;
    if (!path) return null;
    if (cmsDocId && !cmsRevision) {
      setLastSaveMessage("Reload this document before saving.");
      return null;
    }
    const requestId = ++editorRequestRef.current;
    setIsSaving(true);
    setLastSaveMessage(null);
    try {
      if (!cmsDocId && await findCmsDocumentByPath(path)) {
        // Discovering a document at save time does not mean its content was
        // observed. Loading it is a separate user action; never invent a parent.
        throw new Error("A CMS document already exists at this path. Open it before saving.");
      }
      if (editorRequestRef.current !== requestId) return null;
      const response = await fetch(cmsDocId
        ? `/api/cms/documents/${encodeURIComponent(cmsDocId)}`
        : "/api/cms/documents", {
        method: cmsDocId ? "PATCH" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: editorTitle.trim() || "Untitled",
          content: editorBody,
          source_path: cmsDocId ? path : path.replace(/^\/+/, "").replace(/^(?:\.\/)+/, ""),
          visibility: "review",
          metadata: cmsMetadata,
          parents: cmsDocId ? (parents ?? [cmsRevision]) : [],
        }),
      });
      if (!response.ok) throw new Error(await response.text());
      const document = (await response.json()) as CmsDocSummary;
      if (editorRequestRef.current !== requestId) return null;
      applyCmsDocumentToEditor(document);
      setLastSaveMessage(document.conflicted
        ? "Saved. Concurrent edits are preserved; review history to resolve them."
        : "Saved CMS draft");
      void loadCmsDocuments();
      void loadPublicationTopology();
      return document;
    } catch (error) {
      if (editorRequestRef.current === requestId) {
        setLastSaveMessage(error instanceof Error ? error.message : "Save failed");
      }
      throw error;
    } finally {
      setIsSaving(false);
    }
  }, [applyCmsDocumentToEditor, buildEditorPath, cmsDocId, cmsMetadata, cmsRevision,
    editorBody, editorPath, editorTitle, isNewDraft, findCmsDocumentByPath, loadCmsDocuments, loadPublicationTopology]);

  const loadHistory = useCallback(async () => {
    if (!cmsDocId) return;
    const requestId = ++historyRequestRef.current;
    setShowHistory(true);
    setLoadingHistory(true);
    try {
      const response = await fetch(`/api/cms/documents/${encodeURIComponent(cmsDocId)}/history`);
      if (!response.ok) throw new Error(await response.text());
      const result = (await response.json()) as { document: CmsDocSummary; revisions: CmsRevision[] };
      if (historyRequestRef.current !== requestId) return;
      setHistory(result.revisions);
      setCmsHeads(result.document.revision_heads);
      setCmsConflicted(result.document.conflicted);
      setPreviewRevision(cmsRevision ?? result.document.revision);
      setReviewedHeads([]);
    } catch (error) {
      if (historyRequestRef.current === requestId) setLastSaveMessage(error instanceof Error ? error.message : "Could not load history");
    } finally {
      if (historyRequestRef.current === requestId) setLoadingHistory(false);
    }
  }, [cmsDocId, cmsRevision]);

  const handleResolveRevisions = useCallback(async () => {
    if (cmsHeads.length < 2 || !cmsHeads.every((head) => reviewedHeads.includes(head))) return;
    try {
      // Only the heads shown and explicitly reviewed by this editor are joined.
      // A concurrent save after this snapshot remains a separate head.
      await persistEditorFile([...cmsHeads]);
    } catch (error) {
      console.error("Resolution save failed:", error);
    }
  }, [cmsHeads, persistEditorFile, reviewedHeads]);

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
    if (isSaving) return;
    if (entry.type === "dir") {
      await handleLoadDirectory(entry.path);
      return;
    }

    const requestId = ++editorRequestRef.current;
    try {
      const document = await findCmsDocumentByPath(entry.path);
      if (editorRequestRef.current !== requestId) return;
      if (document) {
        applyCmsDocumentToEditor(document);
        return;
      }
      const params = new URLSearchParams({ path: entry.path });
      const resp = await fetch(`/api/ingestion/file?${params}`);
      if (resp.ok) {
        const data: PreviewResponse = await resp.json();
        if (editorRequestRef.current !== requestId) return;
        const previousEditorPath = editorPath;
        setEditorTitle(entry.name);
        setEditorBody(data.content);
        setEditorPath(entry.path);
        setIsNewDraft(false);
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
        clearCmsRevision();
      }
    } catch (err) {
      console.error("Failed to load file:", err);
      if (editorRequestRef.current === requestId) {
        setLastSaveMessage(err instanceof Error ? `Could not load document: ${err.message}` : "Could not load document");
      }
    }
  };

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const docId = params.get("doc");
    const path = params.get("path");
    const requestId = ++editorRequestRef.current;
    if ((docId && cmsDocId === docId) || (!docId && path && editorPath === path)) return;
    let cancelled = false;
    void (async () => {
      try {
        if (docId) {
          const resp = await fetch(`/api/cms/documents/${encodeURIComponent(docId)}`);
          if (!resp.ok || cancelled) return;
          const document = (await resp.json()) as CmsDocSummary;
          if (cancelled || editorRequestRef.current !== requestId) return;
          applyCmsDocumentToEditor(document, path ?? undefined);
          return;
        }

        if (!path) return;
        const document = await findCmsDocumentByPath(path);
        if (cancelled || editorRequestRef.current !== requestId) return;
        if (document) {
          applyCmsDocumentToEditor(document);
          return;
        }
        const resp = await fetch(`/api/ingestion/file?${new URLSearchParams({ path })}`);
        if (!resp.ok || cancelled) return;
        const data: PreviewResponse = await resp.json();
        if (cancelled || editorRequestRef.current !== requestId) return;
        const name = path.split("/").pop() ?? path;
        setEditorTitle(name);
        setEditorBody(data.content);
        setEditorPath(path);
        setIsNewDraft(false);
        setEditorStatus("draft");
        setIsDirty(false);
        setLastSaveMessage(null);
        chat.pinContextItem({
          id: path,
          title: name,
          path,
          snippet: data.content.slice(0, 240),
          kind: "file",
        });
        clearCmsRevision();
      } catch (err) {
        console.error("Failed to load CMS document from URL:", err);
        if (!cancelled && editorRequestRef.current === requestId) {
          setLastSaveMessage(err instanceof Error ? `Could not load document: ${err.message}` : "Could not load document");
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  // Editor state changes (especially a new immutable snapshot path) are not
  // navigation. Re-running this effect for them would reload over unsaved edits.
  }, [location.search]);

  const handleTitleChange = useCallback((title: string) => {
    editorRequestRef.current += 1;
    setEditorTitle(title.replace(/[\\/]+/g, "-"));
    setEditorStatus((prev) => (prev === "published" ? "draft" : prev));
    setIsDirty(true);
    setLastSaveMessage(null);
  }, []);

  const handleBodyChange = useCallback((body: string) => {
    editorRequestRef.current += 1;
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
    }
  }, [editorTitle, persistEditorFile]);

  const handleCreateVisualDraft = useCallback(async (params: { slug: string; title: string; template: string }) => {
    setCreatingDraft(true);
    try {
      const draftPath = `cms/drafts/${params.slug}`;

      // Build default view-contract.edn content
      const contractEdn = `{:view/id "${params.slug}"
 :view/title "${params.title}"
 :view/kind :${params.template}
 :view/schema-version 1
 :view/status :draft

 :source
 {:kind :manual
  :imported-at "${new Date().toISOString()}"}

 :layout
 {:template :${params.template}
  :zones
  []}

 :blocks
 []

 :editor
 {:locked false
  :allowed-actions [:drag :drop :duplicate :hide :delete :edit-props]
  :default-panel :layout}

 :publishing
 {:last-published-at nil
  :defer-index false
  :skip-translation true}

 :settings
 {:language "en"
  :allow-comments true
  :chat-actor-id nil}}`;

      // Create view-contract.edn
      const contractResp = await fetch("/api/ingestion/file", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ path: `${draftPath}/view-contract.edn`, content: contractEdn }),
      });
      if (!contractResp.ok) throw new Error(await contractResp.text());

      // Create empty content.md
      const contentResp = await fetch("/api/ingestion/file", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ path: `${draftPath}/content.md`, content: "# " + params.title + "\n\nStart writing..." }),
      });
      if (!contentResp.ok) throw new Error(await contentResp.text());

      setShowCreateDraftModal(false);
      // Refresh browse data to show the new draft
      await handleLoadDirectory("cms/drafts");
      // Navigate to visual editor
      navigate(`/cms/editor/${encodeURIComponent(draftPath)}`);
    } catch (err) {
      console.error("Failed to create visual draft:", err);
      setLastSaveMessage(err instanceof Error ? err.message : "Failed to create draft");
    } finally {
      setCreatingDraft(false);
    }
  }, [navigate, handleLoadDirectory]);

  const handlePublishToggle = useCallback(async () => {
    if (!cmsDocId) {
      setLastSaveMessage("Save the draft to its workspace garden before publishing.");
      return;
    }
    if (!editorTitle.trim() || !selectedGardenId) {
      setLastSaveMessage("Select a garden");
      return;
    }
    const nextState = isPublishedToSelectedGarden ? "withheld" : "published";
    let requestId = ++editorRequestRef.current;
    try {
      let savedPath = editorPath;
      if (isDirty) {
        const document = await persistEditorFile();
        if (!document) return;
        requestId = editorRequestRef.current;
        savedPath = document.source_path;
        if (document.conflicted) throw new Error("Review and resolve concurrent edits before publishing.");
      }
      setIsSaving(true);
      // Saving may create or relocate this document's resource. Resolve the
      // intent from a fresh topology rather than the render that began the save.
      const topology = await listPublicationTopology();
      if (editorRequestRef.current !== requestId) return;
      setPublicationTopology(topology);
      const document = savedPath ? findDocumentBySourcePath(topology, savedPath) : null;
      const publication = publicationForGarden(document, selectedGardenId);
      if (!publication) throw new Error("No publication is configured for this document and garden.");
      await setPublicationState(publication.id, nextState);
      await loadPublicationTopology();
      if (editorRequestRef.current !== requestId) return;
      setLastSaveMessage(nextState === "published" ? "Publication requested" : "Publication withheld");
    } catch (error) {
      console.error("Publish toggle failed:", error);
      if (editorRequestRef.current === requestId) setLastSaveMessage(error instanceof Error ? error.message : "Could not update publication");
    } finally {
      setIsSaving(false);
    }
  }, [cmsDocId, editorPath, editorTitle, isDirty, isPublishedToSelectedGarden,
    loadPublicationTopology, persistEditorFile, selectedGardenId]);

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
  const blockSchemaVersion = cmsMetadata.block_schema_version === 1 ? 1 : null;
  const publicationBlocks = useMemo(
    () => (blockSchemaVersion === 1 ? extractPublicationBlocks(cmsMetadata) : []),
    [blockSchemaVersion, cmsMetadata],
  );
  const hasPublicationBlocks = publicationBlocks.length > 0;
  const publicationKind = typeof cmsMetadata.publication_kind === "string" ? cmsMetadata.publication_kind : null;

  return (
    <div style={{ display: "flex", flex: "1 1 0%", gap: 0, minHeight: 0 }}>
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
            if (isSaving) return;
            setEditorTitle("untitled.md");
            setEditorBody("");
            setEditorPath(currentPath ? `${currentPath}/untitled.md` : "untitled.md");
            setIsNewDraft(true);
            setEditorStatus("draft");
            clearCmsRevision();
            navigate("/cms");
            setIsDirty(true);
            setLastSaveMessage(null);
          }}
          onNewVisualDraft={() => setShowCreateDraftModal(true)}
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
      ) : (
        <CollapsedPanelTab label="Files" edge="left" onExpand={() => setShowFiles(true)} title="Show Files panel" />
      )}

      <div style={{ flex: 1, overflow: "hidden", display: "flex", flexDirection: "column", minWidth: 0 }}>
        <header className={styles.header}>
          <div className={styles.titleRow}>
            <div className={styles.pathEditor}>
              <span className={styles.pathPrefix}>{editorPath ? editorDirectory || "./" : ""}</span>
              <input
                type="text"
                className={styles.fileNameInput}
                value={editorTitle}
                onChange={(event) => handleTitleChange(event.target.value)}
                placeholder="Select a file from the explorer..."
                aria-label="Document title"
                disabled={!editorPath || isSaving}
              />
            </div>
            {isDirty ? <span className={styles.dirtyIndicator}>Unsaved changes</span> : null}
            {lastSaveMessage ? <span role="status" className={styles.savedIndicator}>{lastSaveMessage}</span> : null}
          </div>
          <div className={styles.actions}>
            {editorPath ? (
              <>
                <button className={styles.saveButton} onClick={() => void handleSave()} disabled={isSaving || !isDirty}>
                  {isSaving ? "Saving…" : "Save"}
                </button>
                {cmsDocId ? <button className={styles.saveButton} onClick={() => void loadHistory()} disabled={isSaving || loadingHistory}>History</button> : null}
                <button className={styles.publishButton} onClick={() => void handlePublishToggle()} disabled={isSaving || !cmsDocId || !selectedGardenId || cmsConflicted} title={!cmsDocId ? "Save to the workspace garden before publishing" : undefined}>
                  {isSaving ? (isPublishedToSelectedGarden ? "Unpublishing…" : "Publishing…") : isPublishedToSelectedGarden ? "Unpublish" : "Publish"}
                </button>
                {editorPath?.includes("view-contract.edn") ? (
                  <button
                    className={styles.visualEditorButton}
                    onClick={() => navigate(`/cms/editor/${encodeURIComponent(editorPath.replace(/\/view-contract\.edn$/, ""))}`)}
                    style={{ marginLeft: "8px", padding: "6px 12px", background: "#8b5cf6", color: "#fff", border: "none", borderRadius: "6px", fontSize: "13px", cursor: "pointer" }}
                  >
                    Visual Editor
                  </button>
                ) : null}
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
              value={editorPath && !cmsDocId ? "" : selectedGardenId}
              disabled={isSaving || Boolean(editorPath && !cmsDocId)}
              onChange={(event) => setSelectedGardenId(event.target.value)}
              className={styles.metaSelect}
              aria-label="Garden"
            >
              <option value="">{editorPath && !cmsDocId ? "Workspace garden (assigned on save)" : "Select garden…"}</option>
              {/* Publication intent may target active gardens only. */}
              {(publicationTopology?.gardens ?? [])
                .filter((garden) => garden.status === "active")
                .map((garden) => (
                <option key={garden.id} value={garden.id}>
                  {garden.title || garden.id}
                </option>
              ))}
            </select>
          </div>
        </div>

        <section className={styles.cmsDocumentStrip}>
          <div className={styles.cmsDocumentStripHeader}>
            <span>Garden CMS documents</span>
            <button type="button" className={styles.inlineButton} onClick={() => void loadCmsDocuments()} disabled={loadingCmsDocuments || !selectedGardenId}>
              {loadingCmsDocuments ? "Loading…" : "Refresh"}
            </button>
          </div>
          {!selectedGardenId ? (
            <div className={styles.cmsDocumentEmpty}>Select a garden to see its CMS drafts and publications.</div>
          ) : cmsDocuments.length === 0 ? (
            <div className={styles.cmsDocumentEmpty}>No CMS documents found for this garden yet.</div>
          ) : (
            <div className={styles.cmsDocumentList}>
              {cmsDocuments.map((doc) => (
                <button
                  key={doc.doc_id}
                  type="button"
                  className={`${styles.cmsDocumentChip} ${doc.doc_id === cmsDocId ? styles.cmsDocumentChipActive : ""}`}
                  onClick={() => void handleOpenCmsDocument(doc.doc_id)}
                  disabled={isSaving}
                  title={doc.source_path ?? doc.title}
                >
                  <span>{doc.title}</span>
                  <small>{doc.visibility ?? "internal"}</small>
                </button>
              ))}
            </div>
          )}
        </section>

        {cmsConflicted ? (
          <div role="alert" className={styles.conflictNotice}>
            Concurrent edits are preserved. Review each current version in History, edit the combined document, then save a resolution.
          </div>
        ) : null}
        {showHistory ? (
          <section aria-label="Document history" className={styles.historyPanel}>
            <div className={styles.cmsDocumentStripHeader}>
              <span>Document history</span>
              <button className={styles.inlineButton} onClick={() => setShowHistory(false)}>Close history</button>
            </div>
            {loadingHistory ? <p>Loading history…</p> : (
              <>
                <label>
                  Version
                  <select aria-label="History version" className={styles.metaSelect} value={previewRevision ?? ""} onChange={(event) => setPreviewRevision(event.target.value)}>
                    {history.map((revision, index) => (
                      <option key={revision.revision} value={revision.revision}>
                        {index + 1}. {revision.at} · {revision.actor}{cmsHeads.includes(revision.revision) ? " · Current version" : ""}
                      </option>
                    ))}
                  </select>
                </label>
                {history.filter((revision) => revision.revision === previewRevision).map((revision) => (
                  <article key={revision.revision}>
                    <h3>{revision.title}</h3>
                    <p>{revision.actor} · <time dateTime={revision.at}>{revision.at}</time></p>
                    <pre className={styles.historyContent}>{revision.content}</pre>
                    <div className={styles.historyActions}>
                      <button className={styles.inlineButton} disabled={isSaving} onClick={() => {
                        editorRequestRef.current += 1;
                        setEditorTitle(revision.title);
                        setEditorBody(revision.content ?? "");
                        setCmsMetadata(revision.metadata ?? {});
                        setCmsRevision(revision.revision);
                        setIsDirty(true);
                        setLastSaveMessage("Version copied into editor. Saving will preserve its history.");
                      }}>Use this version in editor</button>
                      {cmsHeads.includes(revision.revision) ? (
                        <button className={styles.inlineButton} disabled={reviewedHeads.includes(revision.revision)} onClick={() => setReviewedHeads((heads) => [...heads, revision.revision])}>
                          {reviewedHeads.includes(revision.revision) ? "Version reviewed" : "Mark this version reviewed"}
                        </button>
                      ) : null}
                    </div>
                  </article>
                ))}
                {cmsConflicted ? (
                  <div className={styles.historyActions}>
                    <span>{cmsHeads.filter((head) => reviewedHeads.includes(head)).length} of {cmsHeads.length} current versions reviewed</span>
                    <button className={styles.inlineButton} disabled={isSaving || cmsHeads.length < 2 || !cmsHeads.every((head) => reviewedHeads.includes(head))} onClick={() => void handleResolveRevisions()}>Save resolution from editor</button>
                  </div>
                ) : null}
              </>
            )}
          </section>
        ) : null}

        <div className={styles.editorLayout}>
          <main className={styles.bodyEditor}>
            {editorPath ? (
              <textarea
                className={styles.bodyTextarea}
                aria-label="Document content"
                disabled={isSaving}
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
          {editorPath ? (
            <aside className={styles.previewPane}>
              <div className={styles.previewHeader}>
                <span>{hasPublicationBlocks ? "Rendered output: block publication" : "Rendered output: markdown"}</span>
                {publicationKind ? <Badge variant="default">{publicationKind}</Badge> : null}
              </div>
              <div className={styles.previewBody}>
                {hasPublicationBlocks ? (
                  <PublicationBlocksRenderer
                    blocks={publicationBlocks}
                    getAudioUrl={(path) => `/api/studio/stream?path=${encodeURIComponent(path)}`}
                    maxInitialPlaylistTracks={100}
                  />
                ) : editorBody.trim() ? (
                  <div className={styles.markdownPreview}>
                    <ReactMarkdown>{editorBody}</ReactMarkdown>
                  </div>
                ) : (
                  <div className={styles.emptyPreview}>Preview appears here once the document has content.</div>
                )}
              </div>
            </aside>
          ) : null}
        </div>
      </div>

      {showChatPanel ? (
        <aside
          style={{
            width: 460,
            minWidth: 380,
            borderLeft: "1px solid var(--token-colors-border-default)",
            display: "flex",
            flexDirection: "column",
            minHeight: 0,
            overflow: "hidden",
            background: "var(--token-colors-background-surface)",
          }}
        >
          <div style={{
            padding: "8px 10px",
            borderBottom: "1px solid var(--token-colors-border-default)",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            gap: 8,
            flexShrink: 0,
          }}>
            <div style={{ fontSize: 14, fontWeight: 600 }}>CMS Chat</div>
            <Button variant="ghost" size="sm" onClick={() => setShowChatPanel(false)} title="Collapse CMS Chat panel">
              Collapse
            </Button>
          </div>
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
      ) : (
        <CollapsedPanelTab label="CMS Chat" edge="right" onExpand={() => setShowChatPanel(true)} title="Show CMS Chat panel" />
      )}

      <CreateVisualDraftModal
        open={showCreateDraftModal}
        onClose={() => setShowCreateDraftModal(false)}
        onCreate={handleCreateVisualDraft}
        templates={cmsTemplates}
        isCreating={creatingDraft}
      />
    </div>
  );
}

export default CmsPage;
