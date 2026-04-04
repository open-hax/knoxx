import { useEffect, useMemo, useRef, useState, type ChangeEvent } from "react";
import { Badge, Button, Card, Input, Markdown, Spinner } from "@open-hax/uxx";
import ChatComposer from "../components/ChatComposer";
import ConsolePanel from "../components/ConsolePanel";
import {
  getMemorySession,
  getFrontendConfig,
  listMemorySessions,
  getRun,
  getToolCatalog,
  knoxxChatStart,
  knoxxControl,
  listProxxModels,
  proxxHealth,
  sendEmailDraft,
  toolWrite,
} from "../lib/api";
import type {
  ChatMessage,
  FrontendConfig,
  GroundedContextRow,
  MemorySessionRow,
  MemorySessionSummary,
  ProxxModelInfo,
  RunDetail,
  RunEvent,
  ToolReceipt,
  ToolCatalogResponse,
} from "../lib/types";
import { connectStream } from "../lib/ws";

const SESSION_ID_KEY = "knoxx_session_id";
const SCRATCHPAD_STATE_KEY = "knoxx_scratchpad_state";
const PINNED_CONTEXT_KEY = "knoxx_pinned_context";
const CHAT_SESSION_STATE_KEY = "knoxx_chat_session_state";

const DEFAULT_ROLE = "executive";
const DEFAULT_SYNC_INTERVAL_MINUTES = 30;
const SEND_UI_GUARD_TIMEOUT_MS = 30 * 60 * 1000;
const DEFAULT_FILE_TYPES = [
  ".md", ".markdown", ".txt", ".rst", ".org", ".adoc",
  ".json", ".jsonl", ".yaml", ".yml", ".toml", ".ini", ".cfg", ".conf", ".env",
  ".xml", ".csv", ".tsv", ".html", ".htm", ".css",
  ".js", ".jsx", ".ts", ".tsx", ".py", ".rb", ".php", ".java", ".kt", ".go", ".rs",
  ".c", ".cc", ".cpp", ".h", ".hpp", ".clj", ".cljs", ".cljc", ".edn", ".sql", ".sh",
  ".bash", ".zsh", ".fish", ".tex", ".bib", ".nix", ".dockerfile", ".gradle", ".properties",
];
const DEFAULT_EXCLUDE_PATTERNS = [
  "**/.git/**",
  "**/node_modules/**",
  "**/dist/**",
  "**/coverage/**",
  "**/.next/**",
  "**/.venv/**",
  "**/venv/**",
  "**/__pycache__/**",
  "**/*.png",
  "**/*.jpg",
  "**/*.jpeg",
  "**/*.gif",
  "**/*.pdf",
  "**/*.zip",
  "**/*.tar.gz",
];
const QUICK_ROOTS = [
  { label: "docs", path: "docs" },
  { label: "specs", path: "specs" },
  { label: "notes", path: "notes" },
  { label: "packages", path: "packages" },
  { label: "services", path: "services" },
  { label: "orgs", path: "orgs" },
  { label: "data", path: "data" },
];

type BrowseEntry = {
  name: string;
  path: string;
  type: "dir" | "file";
  size?: number | null;
  previewable?: boolean;
  ingestion_status?: "ingested" | "partial" | "failed" | "not_ingested";
  ingested_count?: number;
  failed_count?: number;
  last_ingested_at?: string | null;
  last_error?: string | null;
};

type BrowseResponse = {
  workspace_root: string;
  current_path: string;
  entries: BrowseEntry[];
};

type PreviewResponse = {
  path: string;
  size: number;
  truncated: boolean;
  content: string;
};

type IngestionSource = {
  source_id: string;
  name: string;
  config?: Record<string, unknown> | null;
};

type SemanticSearchMatch = {
  id: string;
  path: string;
  project?: string;
  kind?: string;
  snippet?: string;
  distance?: number | null;
};

type SemanticSearchResponse = {
  projects: string[];
  count: number;
  rows: SemanticSearchMatch[];
};

type WorkspaceJob = {
  job_id: string;
  status: string;
  total_files: number;
  processed_files: number;
  failed_files: number;
  skipped_files: number;
  chunks_created: number;
  created_at: string;
  started_at?: string;
  completed_at?: string;
  error_message?: string | null;
};

type PinnedContextItem = {
  id: string;
  title: string;
  path: string;
  snippet?: string;
  kind: "file" | "semantic" | "message";
};

function makeId(): string {
  const maybeCrypto = globalThis.crypto as Crypto | undefined;
  if (maybeCrypto && typeof maybeCrypto.randomUUID === "function") {
    return maybeCrypto.randomUUID();
  }
  return `id-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function contextPath(row: GroundedContextRow): string {
  return row.source_path ?? row.message ?? row.source ?? row.id;
}

function isWorkspaceSource(source: IngestionSource): boolean {
  const config = source.config ?? {};
  const rootPath = config.root_path ?? config["root-path"];
  return rootPath === "/app/workspace/devel" || (source.name === "devel workspace" && rootPath == null);
}

function parentPath(path: string): string {
  const parts = path.split("/").filter(Boolean);
  if (parts.length <= 1) return "";
  return parts.slice(0, -1).join("/");
}

function seedCanvasFromMessage(message: ChatMessage): { title: string; content: string; subject: string } {
  const firstLine = message.content.split("\n").find((line) => line.trim()) ?? "Draft";
  const title = firstLine.slice(0, 80);
  return {
    title,
    subject: title,
    content: message.content,
  };
}

function slugify(value: string): string {
  return value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 64) || "draft";
}

function fileNameFromPath(path: string): string {
  const parts = path.split("/").filter(Boolean);
  return parts[parts.length - 1] || path;
}

function seedCanvasFromPreview(preview: PreviewResponse): { title: string; subject: string; content: string; path: string } {
  const title = fileNameFromPath(preview.path).replace(/\.[^.]+$/, "") || "Scratchpad";
  return {
    title,
    subject: title,
    content: preview.content,
    path: preview.path,
  };
}

function sourceUrlToPath(url: string): string {
  if (!url) return "";
  try {
    const parsed = new URL(url, window.location.origin);
    return decodeURIComponent(parsed.pathname.replace(/^\/+/, "")).split("?")[0].split("#")[0];
  } catch {
    return decodeURIComponent(url.replace(/^\/+/, "")).split("?")[0].split("#")[0];
  }
}

function jobSortTime(job: WorkspaceJob): number {
  return Date.parse(job.started_at || job.created_at || job.completed_at || "") || 0;
}

function selectWorkspaceJob(jobs: WorkspaceJob[]): WorkspaceJob | null {
  if (jobs.length === 0) return null;
  const sorted = [...jobs].sort((a, b) => jobSortTime(b) - jobSortTime(a));
  const active = sorted.find((job) => job.status === "running" || job.status === "pending");
  if (active) return active;
  const completed = sorted.find((job) => job.status === "completed");
  if (completed) return completed;
  return sorted[0] ?? null;
}

function formatMaybeDate(value?: string): string | null {
  if (!value) return null;
  const parsed = Date.parse(value);
  if (Number.isNaN(parsed)) return value;
  return new Date(parsed).toLocaleString();
}

function truncateText(value: string, max = 240): string {
  if (value.length <= max) return value;
  return `${value.slice(0, max).trimEnd()}…`;
}

function asMarkdownPreview(value: string): string {
  const trimmed = value.trim();
  if (!trimmed) return "";
  if (/^(```|#{1,6}\s|>\s|[-*+]\s|\d+\.\s)/m.test(trimmed)) {
    return value;
  }
  if (trimmed.startsWith("{") || trimmed.startsWith("[") || value.includes("\n")) {
    return `\`\`\`text\n${value}\n\`\`\``;
  }
  return value;
}

function isChatRole(value: unknown): value is "system" | "user" | "assistant" {
  return value === "system" || value === "user" || value === "assistant";
}

function parseMemoryRowExtra(row: MemorySessionRow): Record<string, unknown> | null {
  if (!row.extra) return null;
  if (typeof row.extra === "object" && !Array.isArray(row.extra)) {
    return row.extra;
  }
  if (typeof row.extra === "string") {
    try {
      const parsed = JSON.parse(row.extra) as unknown;
      return parsed && typeof parsed === "object" && !Array.isArray(parsed)
        ? (parsed as Record<string, unknown>)
        : null;
    } catch {
      return null;
    }
  }
  return null;
}

function memoryRowRunId(row: MemorySessionRow): string | null {
  const extra = parseMemoryRowExtra(row);
  const candidate = extra?.run_id ?? extra?.runId;
  return typeof candidate === "string" ? candidate : null;
}

function memoryRowsToMessages(rows: MemorySessionRow[]): ChatMessage[] {
  return rows
    .filter((row) => row.kind === "knoxx.message" && isChatRole(row.role) && typeof row.text === "string" && row.text.trim().length > 0)
    .map((row, index) => ({
      id: row.id || `${row.session ?? "memory"}:${index}`,
      role: row.role,
      content: row.text ?? "",
      model: typeof row.model === "string" ? row.model : null,
      runId: memoryRowRunId(row),
      status: row.role === "assistant" || row.role === "system" ? "done" : undefined,
    }));
}

function controlTimelineMessageFromEvent(
  event: RunEvent & { type?: string; preview?: string; run_id?: string; error?: string },
): ChatMessage | null {
  const type = String(event.type ?? "");
  const preview = typeof event.preview === "string" ? event.preview.trim() : "";
  const runId = typeof event.run_id === "string" ? event.run_id : null;
  const error = typeof event.error === "string" ? event.error.trim() : "";
  const title = {
    steer_queued: "Steer queued",
    follow_up_queued: "Follow-up queued",
    steer_failed: "Steer failed",
    follow_up_failed: "Follow-up failed",
  }[type];
  if (!title) return null;
  const id = `control:${type}:${runId ?? ""}:${preview || error}`;
  const parts = [`### ${title}`];
  if (preview) parts.push("", preview);
  if (error) parts.push("", `Error: ${error}`);
  if (runId) parts.push("", `Run: \`${runId.slice(0, 8)}\``);
  return {
    id,
    role: "system",
    content: parts.join("\n"),
    runId,
    status: type.endsWith("failed") ? "error" : "done",
  };
}

function latestRunHydrationSources(run: RunDetail | null): Array<{ title: string; path: string; section?: string }> {
  const passiveHydration = run?.resources?.passiveHydration as { results?: Array<Record<string, unknown>> } | undefined;
  if (!passiveHydration || !Array.isArray(passiveHydration.results)) return [];
  return passiveHydration.results
    .map((result) => {
      const path = typeof result.path === "string" ? result.path : "";
      if (!path) return null;
      return {
        title: typeof result.name === "string" && result.name.trim().length > 0 ? result.name : fileNameFromPath(path),
        path,
        section: typeof result.snippet === "string" ? result.snippet : undefined,
      };
    })
    .filter((value): value is { title: string; path: string; section?: string } => Boolean(value));
}

function ChatPage() {
  const [frontendConfig, setFrontendConfig] = useState<FrontendConfig | null>(null);
  const [activeRole, setActiveRole] = useState(DEFAULT_ROLE);
  const [toolCatalog, setToolCatalog] = useState<ToolCatalogResponse | null>(null);
  const [systemPrompt, setSystemPrompt] = useState("");
  const [sessionId, setSessionId] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [consoleLines, setConsoleLines] = useState<string[]>([]);
  const [isSending, setIsSending] = useState(false);
  const [showConsole, setShowConsole] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [showFiles, setShowFiles] = useState(true);
  const [showCanvas, setShowCanvas] = useState(true);
  const [wsStatus, setWsStatus] = useState<"connected" | "closed" | "error" | "connecting">("connecting");
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [latestRun, setLatestRun] = useState<RunDetail | null>(null);
  const [runtimeEvents, setRuntimeEvents] = useState<RunEvent[]>([]);
  const [liveControlText, setLiveControlText] = useState("");
  const [queueingControl, setQueueingControl] = useState<"steer" | "follow_up" | null>(null);
  const [proxxModels, setProxxModels] = useState<ProxxModelInfo[]>([]);
  const [selectedModel, setSelectedModel] = useState("");
  const [proxxReachable, setProxxReachable] = useState(false);
  const [proxxConfigured, setProxxConfigured] = useState(false);
  const [browseData, setBrowseData] = useState<BrowseResponse | null>(null);
  const [previewData, setPreviewData] = useState<PreviewResponse | null>(null);
  const [loadingBrowse, setLoadingBrowse] = useState(false);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [entryFilter, setEntryFilter] = useState("");
  const [semanticQuery, setSemanticQuery] = useState("");
  const [semanticResults, setSemanticResults] = useState<SemanticSearchMatch[]>([]);
  const [semanticProjects, setSemanticProjects] = useState<string[]>([]);
  const [semanticSearching, setSemanticSearching] = useState(false);
  const [syncingWorkspace, setSyncingWorkspace] = useState(false);
  const [workspaceSourceId, setWorkspaceSourceId] = useState<string | null>(null);
  const [workspaceJob, setWorkspaceJob] = useState<WorkspaceJob | null>(null);
  const [canvasTitle, setCanvasTitle] = useState("Untitled scratchpad");
  const [canvasSubject, setCanvasSubject] = useState("");
  const [canvasPath, setCanvasPath] = useState("notes/scratchpads/untitled-scratchpad.md");
  const [canvasRecipients, setCanvasRecipients] = useState("");
  const [canvasCc, setCanvasCc] = useState("");
  const [canvasContent, setCanvasContent] = useState("");
  const [canvasStatus, setCanvasStatus] = useState<string | null>(null);
  const [savingCanvas, setSavingCanvas] = useState(false);
  const [savingCanvasFile, setSavingCanvasFile] = useState(false);
  const [sendingCanvas, setSendingCanvas] = useState(false);
  const [pinnedContext, setPinnedContext] = useState<PinnedContextItem[]>([]);
  const [recentSessions, setRecentSessions] = useState<MemorySessionSummary[]>([]);
  const [loadingRecentSessions, setLoadingRecentSessions] = useState(false);
  const [loadingMemorySessionId, setLoadingMemorySessionId] = useState<string | null>(null);
  const sendTimeoutRef = useRef<number | null>(null);
  const pendingAssistantIdRef = useRef<string | null>(null);
  const activeRunIdRef = useRef<string | null>(null);

  const currentPath = browseData?.current_path ?? "";
  const currentParentPath = useMemo(() => parentPath(currentPath), [currentPath]);

  const filteredEntries = useMemo(() => {
    const entries = browseData?.entries ?? [];
    const query = entryFilter.trim().toLowerCase();
    if (!query) return entries;
    return entries.filter((entry) =>
      entry.name.toLowerCase().includes(query) || entry.path.toLowerCase().includes(query),
    );
  }, [browseData?.entries, entryFilter]);

  const semanticMode = semanticQuery.trim().length > 0;
  const activeEntryCount = semanticMode ? semanticResults.length : filteredEntries.length;
  const workspaceProgressPercent = workspaceJob && workspaceJob.total_files > 0
    ? Math.min(100, Math.round(((workspaceJob.processed_files + workspaceJob.failed_files) / workspaceJob.total_files) * 100))
    : 0;
  const latestToolReceipts = useMemo(() => (latestRun?.tool_receipts ?? []) as ToolReceipt[], [latestRun]);
  const hydrationSources = useMemo(() => latestRunHydrationSources(latestRun), [latestRun]);
  const liveControlEnabled = Boolean(
    isSending
      && conversationId
      && runtimeEvents.some((event) => ["run_started", "passive_hydration", "assistant_first_token", "tool_start"].includes(String(event.type ?? ""))),
  );
  const assistantSurfaceBackground = "var(--token-colors-background-surface)";
  const assistantSurfaceBorder = "var(--token-colors-border-default)";
  const assistantSurfaceText = "var(--token-colors-text-default)";

  useEffect(() => {
    void getFrontendConfig()
      .then((config) => {
        setFrontendConfig(config);
        setActiveRole(config.default_role || DEFAULT_ROLE);
      })
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    void getToolCatalog(activeRole)
      .then(setToolCatalog)
      .catch((error) => {
        setConsoleLines((prev) => [...prev.slice(-400), `[tools] failed: ${(error as Error).message}`]);
      });
  }, [activeRole]);

  useEffect(() => {
    try {
      let sid = localStorage.getItem(SESSION_ID_KEY) || "";
      if (!sid) {
        sid = makeId();
        localStorage.setItem(SESSION_ID_KEY, sid);
      }
      setSessionId(sid);
    } catch {
      setSessionId(makeId());
    }
  }, []);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(CHAT_SESSION_STATE_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw) as {
        systemPrompt?: string;
        selectedModel?: string;
        conversationId?: string | null;
        messages?: ChatMessage[];
        latestRun?: RunDetail | null;
        runtimeEvents?: RunEvent[];
        isSending?: boolean;
      };
      if (typeof parsed.systemPrompt === "string") setSystemPrompt(parsed.systemPrompt);
      if (typeof parsed.selectedModel === "string") setSelectedModel(parsed.selectedModel);
      if (typeof parsed.conversationId === "string" || parsed.conversationId === null) {
        setConversationId(parsed.conversationId ?? null);
      }
      if (Array.isArray(parsed.messages)) {
        setMessages(parsed.messages.slice(-80));
        const pending = [...parsed.messages].reverse().find((message) => message.role === "assistant" && message.status === "streaming");
        pendingAssistantIdRef.current = pending?.id ?? null;
        if (!activeRunIdRef.current && typeof pending?.runId === "string") {
          activeRunIdRef.current = pending.runId;
        }
      }
      if (parsed.latestRun && typeof parsed.latestRun === "object") {
        setLatestRun(parsed.latestRun);
        if (typeof parsed.latestRun.run_id === "string") {
          activeRunIdRef.current = parsed.latestRun.run_id;
        }
      }
      if (Array.isArray(parsed.runtimeEvents)) {
        setRuntimeEvents(parsed.runtimeEvents.slice(-80));
      }
      if (parsed.isSending) {
        setIsSending(true);
      }
    } catch {
      // ignore storage failures
    }
  }, []);

  useEffect(() => {
    try {
      localStorage.setItem(
        CHAT_SESSION_STATE_KEY,
        JSON.stringify({
          systemPrompt,
          selectedModel,
          conversationId,
          messages: messages.slice(-80),
          latestRun,
          runtimeEvents: runtimeEvents.slice(-80),
          isSending,
        }),
      );
    } catch {
      // ignore storage failures
    }
  }, [systemPrompt, selectedModel, conversationId, messages, latestRun, runtimeEvents, isSending]);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(SCRATCHPAD_STATE_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw) as {
        title?: string;
        subject?: string;
        path?: string;
        recipients?: string;
        cc?: string;
        content?: string;
      };
      if (typeof parsed.title === "string") setCanvasTitle(parsed.title);
      if (typeof parsed.subject === "string") setCanvasSubject(parsed.subject);
      if (typeof parsed.path === "string") setCanvasPath(parsed.path);
      if (typeof parsed.recipients === "string") setCanvasRecipients(parsed.recipients);
      if (typeof parsed.cc === "string") setCanvasCc(parsed.cc);
      if (typeof parsed.content === "string") setCanvasContent(parsed.content);
    } catch {
      // ignore storage failures
    }
  }, []);

  useEffect(() => {
    try {
      localStorage.setItem(
        SCRATCHPAD_STATE_KEY,
        JSON.stringify({
          title: canvasTitle,
          subject: canvasSubject,
          path: canvasPath,
          recipients: canvasRecipients,
          cc: canvasCc,
          content: canvasContent,
        }),
      );
    } catch {
      // ignore storage failures
    }
  }, [canvasTitle, canvasSubject, canvasPath, canvasRecipients, canvasCc, canvasContent]);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(PINNED_CONTEXT_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw) as PinnedContextItem[];
      if (Array.isArray(parsed)) setPinnedContext(parsed.slice(0, 24));
    } catch {
      // ignore storage failures
    }
  }, []);

  useEffect(() => {
    try {
      localStorage.setItem(PINNED_CONTEXT_KEY, JSON.stringify(pinnedContext.slice(0, 24)));
    } catch {
      // ignore storage failures
    }
  }, [pinnedContext]);

  useEffect(() => {
    let timer: number | null = null;
    const poll = async () => {
      try {
        const status = await proxxHealth();
        setProxxReachable(Boolean(status.reachable));
        setProxxConfigured(Boolean(status.configured));
        const models = await listProxxModels();
        setProxxModels(models);
        if (!selectedModel) {
          const preferred = models.find((model) => model.id === status.default_model);
          setSelectedModel(preferred?.id ?? models[0]?.id ?? "");
        }
      } catch {
        setProxxReachable(false);
      }
    };
    void poll();
    timer = window.setInterval(() => {
      void poll();
    }, 5000);
    return () => {
      if (timer !== null) window.clearInterval(timer);
    };
  }, [selectedModel]);

  useEffect(() => {
    const disconnect = connectStream(
      {
        onStatus: (status) => {
          setWsStatus(status);
          if (status !== "connected") setIsSending(false);
        },
        onToken: (token, runId) => {
          const pendingId = pendingAssistantIdRef.current;
          if (!pendingId) return;
          if (runId) activeRunIdRef.current = runId;
          updateMessageById(pendingId, (message) => ({
            ...message,
            runId: runId ?? message.runId ?? null,
            status: "streaming",
            content: `${message.content}${token}`,
          }));
        },
        onEvent: (event) => {
          const runtimeEvent = event as RunEvent & { run_id?: string; session_id?: string; type?: string; status?: string };
          setRuntimeEvents((prev) => [...prev.slice(-79), runtimeEvent]);
          const controlTimelineMessage = controlTimelineMessageFromEvent(runtimeEvent);
          if (controlTimelineMessage) {
            appendMessageIfMissing(controlTimelineMessage);
          }
          if (typeof runtimeEvent.run_id === "string") {
            activeRunIdRef.current = runtimeEvent.run_id;
            if (runtimeEvent.type === "run_started") {
              setLatestRun(null);
            }
            if (runtimeEvent.type === "run_completed" || runtimeEvent.type === "run_failed") {
              setIsSending(false);
              void loadRunDetail(runtimeEvent.run_id);
            }
          }
          const label = runtimeEvent.type ?? "event";
          const toolName = typeof runtimeEvent.tool_name === "string" ? ` ${runtimeEvent.tool_name}` : "";
          const preview = typeof runtimeEvent.preview === "string" && runtimeEvent.preview.trim().length > 0
            ? ` :: ${truncateText(runtimeEvent.preview, 120)}`
            : "";
          setConsoleLines((prev) => [...prev.slice(-400), `[agent:${label}]${toolName}${preview}`]);
        },
      },
      sessionId || undefined,
    );
    return disconnect;
  }, [sessionId]);

  useEffect(() => {
    if (!isSending) {
      if (sendTimeoutRef.current !== null) {
        window.clearTimeout(sendTimeoutRef.current);
        sendTimeoutRef.current = null;
      }
      return;
    }
    sendTimeoutRef.current = window.setTimeout(() => {
      setIsSending(false);
      setConsoleLines((prev) => [...prev.slice(-400), `[chat] still running after ${Math.round(SEND_UI_GUARD_TIMEOUT_MS / 60000)}m; UI unlocked but the backend may still be working`]);
    }, SEND_UI_GUARD_TIMEOUT_MS);
    return () => {
      if (sendTimeoutRef.current !== null) {
        window.clearTimeout(sendTimeoutRef.current);
        sendTimeoutRef.current = null;
      }
    };
  }, [isSending]);

  useEffect(() => {
    if (!isSending) {
      return;
    }
    const interval = window.setInterval(() => {
      const runId = activeRunIdRef.current;
      if (runId) {
        void loadRunDetail(runId);
      }
    }, 2000);
    return () => window.clearInterval(interval);
  }, [isSending]);

  useEffect(() => {
    void loadDirectory("docs");
    void refreshWorkspaceStatus();
    void refreshRecentSessions();
  }, []);

  useEffect(() => {
    if (latestRun?.status === "completed" || latestRun?.status === "failed") {
      void refreshRecentSessions();
    }
  }, [latestRun?.run_id, latestRun?.status]);

  useEffect(() => {
    const interval = window.setInterval(() => {
      void refreshWorkspaceStatus();
    }, 3000);
    return () => window.clearInterval(interval);
  }, []);

  useEffect(() => {
    if (semanticQuery.trim()) {
      void runSemanticSearch(semanticQuery, currentPath);
    }
  }, [currentPath]);

  useEffect(() => {
    const trimmed = semanticQuery.trim();
    if (!trimmed) {
      setSemanticResults([]);
      setSemanticProjects([]);
      return;
    }
    const timeout = window.setTimeout(() => {
      void runSemanticSearch(trimmed, currentPath);
    }, 300);
    return () => window.clearTimeout(timeout);
  }, [semanticQuery, currentPath]);

  async function loadDirectory(path = "") {
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
      setConsoleLines((prev) => [...prev.slice(-400), `[browse] failed: ${(error as Error).message}`]);
    } finally {
      setLoadingBrowse(false);
    }
  }

  async function refreshWorkspaceStatus() {
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
      setConsoleLines((prev) => [...prev.slice(-400), `[ingestion] status failed: ${(error as Error).message}`]);
    }
  }

  async function runSemanticSearch(query: string, path = currentPath) {
    const trimmed = query.trim();
    if (!trimmed) {
      setSemanticResults([]);
      setSemanticProjects([]);
      return;
    }

    setSemanticSearching(true);
    try {
      const response = await fetch("/api/ingestion/search", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          q: trimmed,
          role: "workspace",
          path,
          limit: 30,
        }),
      });
      if (!response.ok) throw new Error(`Semantic search failed: ${response.status}`);
      const data = (await response.json()) as SemanticSearchResponse;
      setSemanticResults(data.rows);
      setSemanticProjects(data.projects);
    } catch (error) {
      setConsoleLines((prev) => [...prev.slice(-400), `[semantic] failed: ${(error as Error).message}`]);
    } finally {
      setSemanticSearching(false);
    }
  }

  async function previewFile(path: string) {
    setLoadingPreview(true);
    try {
      const data = await fetchPreviewData(path);
      setPreviewData(data);
    } catch (error) {
      setConsoleLines((prev) => [...prev.slice(-400), `[preview] failed: ${(error as Error).message}`]);
    } finally {
      setLoadingPreview(false);
    }
  }

  async function ensureWorkspaceSync() {
    setSyncingWorkspace(true);
    try {
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
            name: "devel workspace",
            config: {
              root_path: "/app/workspace/devel",
              sync_interval_minutes: DEFAULT_SYNC_INTERVAL_MINUTES,
            },
            collections: ["devel-docs", "devel-code", "devel-config", "devel-data"],
            file_types: DEFAULT_FILE_TYPES,
            exclude_patterns: DEFAULT_EXCLUDE_PATTERNS,
          }),
        });
        if (!createResponse.ok) throw new Error(`Failed to create source: ${createResponse.status}`);
        const createdSource = (await createResponse.json()) as IngestionSource;
        source = createdSource;
        setConsoleLines((prev) => [...prev.slice(-400), `[ingestion] created source ${createdSource.source_id} for devel workspace`]);
      }

      if (!source) throw new Error("Failed to resolve devel workspace source");
      setWorkspaceSourceId(source.source_id);

      const jobResponse = await fetch("/api/ingestion/jobs", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ source_id: source.source_id }),
      });
      if (!jobResponse.ok) throw new Error(`Failed to start sync: ${jobResponse.status}`);
      const job = (await jobResponse.json()) as { job_id: string };
      setConsoleLines((prev) => [
        ...prev.slice(-400),
        `[ingestion] queued devel workspace sync job ${job.job_id} (interval ${DEFAULT_SYNC_INTERVAL_MINUTES}m)`,
      ]);
      void refreshWorkspaceStatus();
    } catch (error) {
      setConsoleLines((prev) => [...prev.slice(-400), `[ingestion] sync failed: ${(error as Error).message}`]);
    } finally {
      setSyncingWorkspace(false);
    }
  }

  function updateMessageById(messageId: string, updater: (message: ChatMessage) => ChatMessage) {
    setMessages((prev) => prev.map((message) => (message.id === messageId ? updater(message) : message)));
  }

  function appendMessageIfMissing(message: ChatMessage) {
    setMessages((prev) => (prev.some((entry) => entry.id === message.id) ? prev : [...prev, message]));
  }

  async function loadRunDetail(runId: string) {
    try {
      const run = await getRun(runId);
      if (activeRunIdRef.current === runId) {
        setLatestRun(run);
        const pendingId = pendingAssistantIdRef.current;
        if (pendingId) {
          updateMessageById(pendingId, (message) => ({
            ...message,
            content:
              typeof run.answer === "string" && run.answer.length > 0
                ? run.answer
                : run.status === "failed" && typeof run.error === "string" && run.error.length > 0
                  ? `Agent request failed.\n\n${run.error}`
                  : message.content,
            model: run.model ?? message.model,
            sources: Array.isArray(run.sources) ? run.sources : message.sources,
            runId,
            status: run.status === "completed" ? "done" : run.status === "failed" ? "error" : message.status,
          }));
          if (run.status === "completed" || run.status === "failed") {
            pendingAssistantIdRef.current = null;
          }
        }
      }
    } catch (error) {
      setConsoleLines((prev) => [...prev.slice(-400), `[runs] failed to load ${runId}: ${(error as Error).message}`]);
    }
  }

  async function refreshRecentSessions() {
    setLoadingRecentSessions(true);
    try {
      const sessions = await listMemorySessions(8);
      setRecentSessions(sessions);
    } catch (error) {
      setConsoleLines((prev) => [...prev.slice(-400), `[memory] failed to load recent sessions: ${(error as Error).message}`]);
    } finally {
      setLoadingRecentSessions(false);
    }
  }

  async function resumeMemorySession(sessionKey: string) {
    setLoadingMemorySessionId(sessionKey);
    try {
      const detail = await getMemorySession(sessionKey);
      const transcript = memoryRowsToMessages(detail.rows).slice(-80);
      const lastRunId = [...detail.rows].reverse().map(memoryRowRunId).find((value): value is string => Boolean(value)) ?? null;
      setMessages(transcript);
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
      setConsoleLines((prev) => [
        ...prev.slice(-400),
        `[memory] resumed ${detail.session} with ${transcript.length} transcript message${transcript.length === 1 ? "" : "s"}`,
      ]);
    } catch (error) {
      setConsoleLines((prev) => [...prev.slice(-400), `[memory] failed to resume ${sessionKey}: ${(error as Error).message}`]);
    } finally {
      setLoadingMemorySessionId(null);
    }
  }

  function pinHydrationSource(source: { title: string; path: string; section?: string }) {
    pinContextItem({
      id: source.path,
      title: source.title,
      path: source.path,
      snippet: source.section,
      kind: "semantic",
    });
  }

  async function openHydrationSource(source: { path: string }) {
    await previewFile(source.path);
  }

  async function queueLiveControl(kind: "steer" | "follow_up") {
    const trimmed = liveControlText.trim();
    if (!trimmed || !conversationId || !liveControlEnabled) {
      return;
    }

    setQueueingControl(kind);
    try {
      const response = await knoxxControl({
        kind,
        message: trimmed,
        conversation_id: conversationId,
        session_id: sessionId,
        run_id: activeRunIdRef.current,
      });
      const optimisticTimelineMessage = controlTimelineMessageFromEvent({
        type: kind === "follow_up" ? "follow_up_queued" : "steer_queued",
        preview: truncateText(trimmed, 240),
        run_id: response.run_id ?? activeRunIdRef.current ?? undefined,
      });
      if (optimisticTimelineMessage) {
        appendMessageIfMissing(optimisticTimelineMessage);
      }
      setLiveControlText("");
      setConsoleLines((prev) => [
        ...prev.slice(-400),
        `[agent:${kind}] queued for conversation=${response.conversation_id ?? conversationId} run=${response.run_id ?? activeRunIdRef.current ?? "pending"}`,
      ]);
    } catch (error) {
      const failedTimelineMessage = controlTimelineMessageFromEvent({
        type: kind === "follow_up" ? "follow_up_failed" : "steer_failed",
        preview: truncateText(trimmed, 240),
        run_id: activeRunIdRef.current ?? undefined,
        error: (error as Error).message,
      });
      if (failedTimelineMessage) {
        appendMessageIfMissing(failedTimelineMessage);
      }
      setConsoleLines((prev) => [...prev.slice(-400), `[agent:${kind}] failed: ${(error as Error).message}`]);
    } finally {
      setQueueingControl(null);
    }
  }

  async function handleSend(text: string) {
    if (!sessionId) {
      setConsoleLines((prev) => [...prev.slice(-400), "[chat] session not ready, retry in a second"]);
      return;
    }
    if (!selectedModel) {
      setConsoleLines((prev) => [...prev.slice(-400), "[chat] no model selected"]);
      return;
    }

    const userMessage: ChatMessage = { id: makeId(), role: "user", content: text };
    const assistantMessageId = makeId();
    const pendingAssistantMessage: ChatMessage = {
      id: assistantMessageId,
      role: "assistant",
      content: "",
      model: selectedModel,
      sources: [],
      status: "streaming",
    };
    const requestText = systemPrompt.trim()
      ? `${text}\n\nSession steering note:\n${systemPrompt.trim()}`
      : text;
    pendingAssistantIdRef.current = assistantMessageId;
    activeRunIdRef.current = null;
    setLatestRun(null);
    setRuntimeEvents([]);
    setMessages((prev) => [...prev, userMessage, pendingAssistantMessage]);
    setIsSending(true);

    try {
      const response = await knoxxChatStart({
        message: requestText,
        conversation_id: conversationId,
        session_id: sessionId,
        run_id: activeRunIdRef.current,
        model: selectedModel,
      });
      const runId = response.run_id ?? activeRunIdRef.current;
      if (runId) {
        activeRunIdRef.current = runId;
        void loadRunDetail(runId);
      }
      setConversationId(response.conversation_id ?? conversationId ?? null);
      updateMessageById(assistantMessageId, (message) => ({
        ...message,
        model: response.model ?? selectedModel,
        runId,
        status: "streaming",
      }));
      setConsoleLines((prev) => [
        ...prev.slice(-400),
        `[agent] queued model=${response.model ?? selectedModel} conversation=${response.conversation_id ?? conversationId ?? "new"} run=${runId ?? "pending"}`,
      ]);
    } catch (error) {
      const message = (error as Error).message;
      updateMessageById(assistantMessageId, (assistant) => ({
        ...assistant,
        content: `Agent request failed.\n\n${message}`,
        status: "error",
      }));
      pendingAssistantIdRef.current = null;
      setIsSending(false);
      setConsoleLines((prev) => [...prev.slice(-400), `[chat] failed: ${message}`]);
    }
  }

  function handleNewChat() {
    setMessages([]);
    setConversationId(null);
    setLatestRun(null);
    setRuntimeEvents([]);
    setLiveControlText("");
    activeRunIdRef.current = null;
    pendingAssistantIdRef.current = null;
    setIsSending(false);
  }

  function openMessageInCanvas(message: ChatMessage) {
    const seeded = seedCanvasFromMessage(message);
    setCanvasTitle(seeded.title);
    setCanvasSubject(seeded.subject);
    setCanvasPath(`notes/scratchpads/${slugify(seeded.title)}.md`);
    setCanvasContent(seeded.content);
    setCanvasStatus(null);
    setShowCanvas(true);
  }

  async function fetchPreviewData(path: string): Promise<PreviewResponse> {
    const params = new URLSearchParams({ path });
    const response = await fetch(`/api/ingestion/file?${params.toString()}`);
    if (!response.ok) throw new Error(`Preview failed: ${response.status}`);
    return (await response.json()) as PreviewResponse;
  }

  function pinContextItem(item: PinnedContextItem) {
    setPinnedContext((prev) => {
      const next = [item, ...prev.filter((entry) => entry.path !== item.path)];
      return next.slice(0, 24);
    });
    setCanvasStatus(`Pinned ${item.title}`);
  }

  function unpinContextItem(path: string) {
    setPinnedContext((prev) => prev.filter((entry) => entry.path !== path));
  }

  function pinPreviewContext() {
    if (!previewData) return;
    pinContextItem({
      id: previewData.path,
      title: fileNameFromPath(previewData.path),
      path: previewData.path,
      snippet: previewData.content.slice(0, 240),
      kind: "file",
    });
  }

  function pinSemanticResult(entry: SemanticSearchMatch) {
    pinContextItem({
      id: entry.id,
      title: fileNameFromPath(entry.path),
      path: entry.path,
      snippet: entry.snippet,
      kind: "semantic",
    });
  }

  function pinMessageContext(row: GroundedContextRow) {
    const path = contextPath(row);
    pinContextItem({
      id: row.id,
      title: fileNameFromPath(path),
      path,
      snippet: row.snippet ?? row.text ?? "",
      kind: "message",
    });
  }

  function pinAssistantSource(source: NonNullable<ChatMessage["sources"]>[number]) {
    const path = sourceUrlToPath(source.url);
    if (!path) return;
    pinContextItem({
      id: path,
      title: source.title || fileNameFromPath(path),
      path,
      snippet: source.section,
      kind: "message",
    });
  }

  async function openSourceInPreview(source: NonNullable<ChatMessage["sources"]>[number]) {
    const path = sourceUrlToPath(source.url);
    if (!path) return;
    await previewFile(path);
  }

  function appendToScratchpad(text: string, heading?: string) {
    setCanvasContent((prev) => {
      const prefix = prev.trim().length > 0 ? `${prev.trimEnd()}\n\n` : "";
      return `${prefix}${heading ? `## ${heading}\n\n` : ""}${text}`;
    });
    setShowCanvas(true);
  }

  async function openPreviewInCanvas() {
    if (!previewData) return;
    const seeded = seedCanvasFromPreview(previewData);
    setCanvasTitle(seeded.title);
    setCanvasSubject(seeded.subject);
    setCanvasPath(seeded.path);
    setCanvasContent(seeded.content);
    setCanvasStatus(`Loaded ${seeded.path} into scratchpad.`);
    setShowCanvas(true);
  }

  async function openPinnedInCanvas(item: PinnedContextItem) {
    try {
      const preview = await fetchPreviewData(item.path);
      setPreviewData(preview);
      const seeded = seedCanvasFromPreview(preview);
      setCanvasTitle(seeded.title);
      setCanvasSubject(seeded.subject);
      setCanvasPath(seeded.path);
      setCanvasContent(seeded.content);
      setCanvasStatus(`Loaded ${item.path} into scratchpad.`);
      setShowCanvas(true);
    } catch (error) {
      appendToScratchpad(item.snippet || item.path, item.title);
      setCanvasStatus(`Loaded pinned context excerpt for ${item.title}.`);
      setConsoleLines((prev) => [...prev.slice(-400), `[scratchpad] preview load failed for ${item.path}: ${(error as Error).message}`]);
    }
  }

  function insertPinnedIntoCanvas(item: PinnedContextItem) {
    appendToScratchpad(item.snippet || item.path, `${item.title} (${item.kind})`);
    setCanvasStatus(`Inserted pinned context from ${item.title}.`);
  }

  function clearScratchpad() {
    setCanvasTitle("Untitled scratchpad");
    setCanvasSubject("");
    setCanvasPath(`notes/scratchpads/${slugify(`scratchpad-${Date.now()}`)}.md`);
    setCanvasContent("");
    setCanvasStatus("Cleared scratchpad.");
  }

  function useLatestAssistantInCanvas() {
    const latestAssistant = [...messages].reverse().find((message) => message.role === "assistant");
    if (!latestAssistant) return;
    openMessageInCanvas(latestAssistant);
  }

  async function saveCanvasDraft() {
    setSavingCanvas(true);
    setCanvasStatus(null);
    try {
      const response = await fetch("/api/cms/documents", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: canvasTitle || "Untitled draft",
          content: canvasContent,
          domain: "outbox",
        }),
      });
      if (!response.ok) throw new Error(`Draft save failed: ${response.status}`);
      setCanvasStatus("Saved draft to CMS.");
    } catch (error) {
      setCanvasStatus(`Save failed: ${(error as Error).message}`);
    } finally {
      setSavingCanvas(false);
    }
  }

  async function saveCanvasFile() {
    setSavingCanvasFile(true);
    setCanvasStatus(null);
    try {
      const response = await toolWrite({
        role: activeRole,
        path: canvasPath,
        content: canvasContent,
        create_parents: true,
        overwrite: true,
      });
      setCanvasStatus(`Saved file to ${response.path}`);
    } catch (error) {
      setCanvasStatus(`File save failed: ${(error as Error).message}`);
    } finally {
      setSavingCanvasFile(false);
    }
  }

  async function sendCanvasEmailAction() {
    setSendingCanvas(true);
    setCanvasStatus(null);
    try {
      const to = canvasRecipients.split(",").map((value) => value.trim()).filter(Boolean);
      const cc = canvasCc.split(",").map((value) => value.trim()).filter(Boolean);
      if (to.length === 0) throw new Error("At least one recipient is required");
      const response = await sendEmailDraft({
        role: activeRole,
        to,
        cc,
        subject: canvasSubject || canvasTitle || "Untitled draft",
        markdown: canvasContent,
      });
      setCanvasStatus(`Sent to ${response.sent_to.join(", ")}`);
    } catch (error) {
      setCanvasStatus(`Email failed: ${(error as Error).message}`);
    } finally {
      setSendingCanvas(false);
    }
  }

  return (
    <div
      style={{
        display: "flex",
        height: "calc(100vh - 96px)",
        gap: 0,
        minHeight: 0,
        background:
          "radial-gradient(circle at top left, var(--token-colors-alpha-green-_14) 0%, transparent 28%), radial-gradient(circle at bottom right, var(--token-colors-alpha-orange-_12) 0%, transparent 24%), linear-gradient(180deg, var(--token-monokai-bg-default) 0%, var(--token-monokai-bg-darker) 100%)",
      }}
    >
      {showFiles ? (
        <Card
          variant="default"
          padding="none"
          style={{ width: 300, flexShrink: 0, display: "flex", flexDirection: "column", borderRight: "1px solid var(--token-colors-border-default)", minHeight: 0 }}
        >
          <div style={{ padding: 10, borderBottom: "1px solid var(--token-colors-border-default)", display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
            <div style={{ minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600 }}>Context Bar</div>
              <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                Explorer root: /{currentPath || "docs"}
              </div>
            </div>
            <div style={{ display: "flex", gap: 6 }}>
              <Button variant="ghost" size="sm" disabled={!currentPath} onClick={() => void loadDirectory(currentParentPath)}>
                Up
              </Button>
              <Button variant="ghost" size="sm" onClick={() => setShowFiles(false)}>Hide</Button>
            </div>
          </div>

          <div style={{ padding: 10, borderBottom: "1px solid var(--token-colors-border-default)", display: "grid", gap: 8 }}>
            <div style={{ display: "grid", gap: 6 }}>
              <Input value={entryFilter} onChange={(event: ChangeEvent<HTMLInputElement>) => setEntryFilter(event.target.value)} placeholder="Filter current list..." size="sm" />
              <div style={{ display: "flex", gap: 6 }}>
                <Input value={semanticQuery} onChange={(event: ChangeEvent<HTMLInputElement>) => setSemanticQuery(event.target.value)} placeholder="Semantic search in current path..." size="sm" />
                <Button variant="secondary" size="sm" loading={semanticSearching} onClick={() => void runSemanticSearch(semanticQuery)}>
                  Search
                </Button>
                {semanticMode ? (
                  <Button variant="ghost" size="sm" onClick={() => { setSemanticQuery(""); setSemanticResults([]); setSemanticProjects([]); }}>
                    Clear
                  </Button>
                ) : null}
              </div>
            </div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 6 }}>
              {QUICK_ROOTS.map((root) => (
                <Button key={root.path} size="sm" variant="ghost" onClick={() => void loadDirectory(root.path)}>
                  {root.label}
                </Button>
              ))}
            </div>
            <Card variant="outlined" padding="sm">
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 6 }}>
                <div style={{ fontSize: 11, fontWeight: 600 }}>Workspace Ingestion</div>
                <Badge size="sm" variant={workspaceJob?.status === "running" ? "warning" : workspaceJob?.status === "completed" ? "success" : workspaceJob?.status === "failed" ? "error" : "default"}>
                  {workspaceJob?.status ?? "idle"}
                </Badge>
              </div>
              <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", display: "grid", gap: 4 }}>
                <div>{workspaceSourceId ? `source ${workspaceSourceId.slice(0, 8)}` : "No workspace source yet"}</div>
                {workspaceJob ? (
                  <>
                    <div>{workspaceJob.processed_files}/{workspaceJob.total_files || 0} processed, {workspaceJob.failed_files} failed, {workspaceJob.chunks_created} chunks</div>
                    <div>
                      {workspaceJob.status === "running" || workspaceJob.status === "pending"
                        ? `Started ${formatMaybeDate(workspaceJob.started_at || workspaceJob.created_at) ?? "just now"}`
                        : workspaceJob.completed_at
                          ? `Finished ${formatMaybeDate(workspaceJob.completed_at)}`
                          : `Created ${formatMaybeDate(workspaceJob.created_at)}`}
                    </div>
                    {workspaceJob.error_message ? (
                      <div style={{ color: "var(--token-colors-accent-red)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }} title={workspaceJob.error_message}>
                        {workspaceJob.error_message}
                      </div>
                    ) : null}
                    <div style={{ height: 6, borderRadius: 999, background: "var(--token-colors-border-default)", overflow: "hidden" }}>
                      <div style={{ width: `${workspaceProgressPercent}%`, height: "100%", background: workspaceJob.status === "failed" ? "var(--token-colors-accent-red)" : workspaceJob.status === "completed" ? "var(--token-colors-accent-green)" : "var(--token-colors-accent-cyan)" }} />
                    </div>
                  </>
                ) : null}
              </div>
            </Card>
            <Button variant="secondary" size="sm" loading={syncingWorkspace} onClick={ensureWorkspaceSync}>
              Sync Devel Workspace
            </Button>
            <Card variant="outlined" padding="sm">
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 6 }}>
                <div style={{ fontSize: 11, fontWeight: 600 }}>Recent Sessions</div>
                <div style={{ display: "flex", gap: 6, alignItems: "center" }}>
                  <Badge size="sm" variant="default">{recentSessions.length}</Badge>
                  <Button variant="ghost" size="sm" loading={loadingRecentSessions} onClick={() => void refreshRecentSessions()}>
                    Refresh
                  </Button>
                </div>
              </div>
              {recentSessions.length === 0 ? (
                <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", lineHeight: 1.5 }}>
                  No OpenPlanner-backed Knoxx sessions yet.
                </div>
              ) : (
                <div style={{ display: "grid", gap: 6 }}>
                  {recentSessions.slice(0, 6).map((item) => {
                    const isActive = conversationId === item.session;
                    return (
                      <div key={item.session} style={{ border: "1px solid var(--token-colors-border-default)", borderRadius: 8, padding: 8, background: isActive ? "var(--token-colors-alpha-blue-_15)" : "var(--token-colors-alpha-bg-_08)" }}>
                        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
                          <div style={{ minWidth: 0 }}>
                            <div style={{ fontSize: 11, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{item.session}</div>
                            <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                              {formatMaybeDate(item.last_ts) ?? item.last_ts ?? "unknown time"}
                            </div>
                          </div>
                          <Badge size="sm" variant={isActive ? "info" : "default"}>{item.event_count ?? 0} ev</Badge>
                        </div>
                        <div style={{ display: "flex", gap: 6, marginTop: 8, flexWrap: "wrap" }}>
                          <Button variant="ghost" size="sm" loading={loadingMemorySessionId === item.session} onClick={() => void resumeMemorySession(item.session)}>
                            {isActive ? "Reload" : "Resume"}
                          </Button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </Card>
            <Card variant="outlined" padding="sm">
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 6 }}>
                <div style={{ fontSize: 11, fontWeight: 600 }}>Pinned Context</div>
                <Badge size="sm" variant="default">{pinnedContext.length}</Badge>
              </div>
              {pinnedContext.length === 0 ? (
                <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", lineHeight: 1.5 }}>
                  Pin a previewed file, semantic hit, or injected context snippet to keep it in your working set.
                </div>
              ) : (
                <div style={{ display: "grid", gap: 6 }}>
                  {pinnedContext.slice(0, 8).map((item) => (
                    <div key={`${item.kind}:${item.path}`} style={{ border: "1px solid var(--token-colors-border-default)", borderRadius: 8, padding: 8, background: "var(--token-colors-alpha-bg-_08)" }}>
                      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
                        <div style={{ minWidth: 0 }}>
                          <div style={{ fontSize: 11, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{item.title}</div>
                          <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{item.path}</div>
                        </div>
                        <Badge size="sm" variant="info">{item.kind}</Badge>
                      </div>
                      {item.snippet ? (
                        <div style={{ marginTop: 6, fontSize: 10, color: "var(--token-colors-text-subtle)", lineHeight: 1.5 }}>{item.snippet.slice(0, 180)}</div>
                      ) : null}
                      <div style={{ display: "flex", gap: 6, marginTop: 8, flexWrap: "wrap" }}>
                        <Button variant="ghost" size="sm" onClick={() => void openPinnedInCanvas(item)}>Open</Button>
                        <Button variant="ghost" size="sm" onClick={() => insertPinnedIntoCanvas(item)}>Insert</Button>
                        <Button variant="ghost" size="sm" onClick={() => unpinContextItem(item.path)}>Unpin</Button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </Card>
          </div>

          <div style={{ flex: 1, minHeight: 0, overflowY: "auto", borderBottom: "1px solid var(--token-colors-border-default)" }}>
            {loadingBrowse && !browseData ? (
              <div style={{ display: "flex", justifyContent: "center", padding: 24 }}>
                <Spinner size="md" />
              </div>
            ) : (
              <div>
                <div style={{ padding: "6px 8px", fontSize: 10, color: "var(--token-colors-text-muted)", borderBottom: "1px solid var(--token-colors-alpha-bg-_08)" }}>
                  {semanticMode ? "Semantic hits" : "Explorer"} •
                  {semanticMode ? `${activeEntryCount} semantic match(es)` : `${activeEntryCount} visible entr${activeEntryCount === 1 ? "y" : "ies"}`}
                  {semanticMode && semanticProjects.length ? ` across ${semanticProjects.join(", ")}` : ""}
                </div>
                {semanticMode
                  ? semanticResults.map((entry) => (
                      <button
                        key={`semantic:${entry.id}`}
                        type="button"
                        onClick={() => {
                          void previewFile(entry.path);
                        }}
                        style={{
                          width: "100%",
                          textAlign: "left",
                          padding: "8px 8px",
                          border: "none",
                          borderBottom: "1px solid var(--token-colors-alpha-bg-_08)",
                          background: previewData?.path === entry.path ? "var(--token-colors-alpha-blue-_15)" : "transparent",
                          cursor: "pointer",
                        }}
                      >
                        <div style={{ display: "flex", alignItems: "center", gap: 6, minWidth: 0 }}>
                          <span style={{ width: 6, height: 6, borderRadius: 999, background: "var(--token-colors-accent-cyan)", flexShrink: 0 }} />
                          <span style={{ fontSize: 11, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{entry.path}</span>
                        </div>
                        <div style={{ display: "flex", gap: 6, marginTop: 4, flexWrap: "wrap" }}>
                          {entry.project ? <Badge size="sm" variant="default">{entry.project}</Badge> : null}
                          {entry.kind ? <Badge size="sm" variant="default">{entry.kind}</Badge> : null}
                          {entry.distance != null ? <Badge size="sm" variant="info">{entry.distance.toFixed(3)}</Badge> : null}
                        </div>
                        {entry.snippet ? (
                          <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)", marginTop: 6, lineHeight: 1.5 }}>{entry.snippet}</div>
                        ) : null}
                        <div style={{ display: "flex", gap: 6, marginTop: 8, flexWrap: "wrap" }}>
                          <Button variant="ghost" size="sm" onClick={() => pinSemanticResult(entry)}>Pin</Button>
                          <Button variant="ghost" size="sm" onClick={() => appendToScratchpad(entry.snippet || entry.path, entry.path)}>Insert</Button>
                        </div>
                      </button>
                    ))
                  : filteredEntries.map((entry) => (
                      <button
                        key={`${entry.type}:${entry.path}`}
                        type="button"
                        onClick={() => {
                          if (entry.type === "dir") {
                            void loadDirectory(entry.path);
                          } else if (entry.previewable) {
                            void previewFile(entry.path);
                          }
                        }}
                        style={{
                          width: "100%",
                          textAlign: "left",
                          padding: "5px 8px",
                          border: "none",
                          borderBottom: "1px solid var(--token-colors-alpha-bg-_08)",
                          background: previewData?.path === entry.path ? "var(--token-colors-alpha-blue-_15)" : "transparent",
                          cursor: "pointer",
                        }}
                        title={entry.last_error ?? entry.path}
                      >
                        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 6, minWidth: 0 }}>
                          <div style={{ display: "flex", alignItems: "center", gap: 6, minWidth: 0 }}>
                            <span style={{ fontSize: 11, color: "var(--token-colors-text-subtle)", flexShrink: 0 }}>{entry.type === "dir" ? "▸" : "·"}</span>
                            <span
                              style={{
                                width: 6,
                                height: 6,
                                borderRadius: 999,
                                background:
                                  entry.ingestion_status === "failed"
                                    ? "var(--token-colors-accent-red)"
                                    :
                                  entry.ingestion_status === "ingested"
                                    ? "var(--token-colors-accent-green)"
                                    : entry.ingestion_status === "partial"
                                      ? "var(--token-colors-accent-cyan)"
                                      : "var(--token-colors-text-muted)",
                                flexShrink: 0,
                              }}
                            />
                            <span style={{ fontSize: 12, fontWeight: 500, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{entry.name}</span>
                          </div>
                          <div style={{ display: "flex", gap: 6, flexShrink: 0 }}>
                            {entry.failed_count && entry.failed_count > 0 ? (
                              <span style={{ fontSize: 10, color: "var(--token-colors-accent-red)" }}>{entry.failed_count} failed</span>
                            ) : null}
                            {entry.ingested_count && entry.ingested_count > 0 ? (
                              <span style={{ fontSize: 10, color: "var(--token-colors-text-muted)" }}>{entry.ingested_count}</span>
                            ) : null}
                          </div>
                        </div>
                        <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)", marginLeft: 19, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                          {entry.path}
                          {entry.last_ingested_at ? ` • ${entry.last_ingested_at}` : ""}
                        </div>
                        {entry.last_error ? (
                          <div style={{ fontSize: 10, color: "var(--token-colors-accent-red)", marginLeft: 19, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                            {entry.last_error}
                          </div>
                        ) : null}
                      </button>
                    ))}
              </div>
            )}
          </div>

          <div style={{ minHeight: 200, maxHeight: 280, overflowY: "auto", padding: 10, background: "var(--token-colors-alpha-bg-_08)" }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 8 }}>
              <div style={{ fontSize: 11, fontWeight: 600 }}>Preview</div>
              {previewData ? (
                <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                  <Button variant="ghost" size="sm" onClick={pinPreviewContext}>Pin</Button>
                  <Button variant="ghost" size="sm" onClick={() => void openPreviewInCanvas()}>Open in Scratchpad</Button>
                  <Button variant="ghost" size="sm" onClick={() => appendToScratchpad(previewData.content, previewData.path)}>Insert</Button>
                </div>
              ) : null}
            </div>
            {loadingPreview ? (
              <Spinner size="sm" />
            ) : previewData ? (
              <>
                <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)", marginBottom: 8 }}>{previewData.path}</div>
                <pre style={{ margin: 0, whiteSpace: "pre-wrap", fontSize: 10, lineHeight: 1.5 }}>{previewData.content}</pre>
              </>
            ) : (
              <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>Select a previewable file to inspect it.</div>
            )}
          </div>
        </Card>
      ) : null}

      <div style={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0, background: "var(--token-monokai-bg-default)" }}>
        <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "8px 12px", borderBottom: "1px solid var(--token-colors-border-default)", flexShrink: 0 }}>
          {!showFiles ? <Button variant="ghost" size="sm" onClick={() => setShowFiles(true)}>Files</Button> : null}
          <Button variant="ghost" size="sm" onClick={() => setShowSettings((value) => !value)}>Settings</Button>
          <Button variant="ghost" size="sm" onClick={() => setShowCanvas((value) => !value)}>Scratchpad</Button>
          <Button variant="ghost" size="sm" onClick={() => setShowConsole((value) => !value)}>Console</Button>
          <div style={{ flex: 1 }} />
          <select
            value={selectedModel}
            onChange={(event) => setSelectedModel(event.target.value)}
            style={{
              borderRadius: 6,
              border: "1px solid var(--token-colors-border-subtle)",
              padding: "4px 8px",
              fontSize: 12,
              maxWidth: 300,
              background: "var(--token-colors-surface-input)",
              color: "var(--token-colors-text-default)",
            }}
          >
            {proxxModels.length === 0 ? <option value="">No models available</option> : null}
            {proxxModels.map((model) => (
              <option key={model.id} value={model.id}>{model.id}</option>
            ))}
          </select>
          <Badge variant={proxxReachable ? "success" : proxxConfigured ? "warning" : "error"} size="sm" dot>
            {proxxReachable ? "online" : proxxConfigured ? "offline" : "not configured"}
          </Badge>
          <Button variant="secondary" size="sm" loading={syncingWorkspace} onClick={ensureWorkspaceSync}>Sync Devel</Button>
          <Button variant="ghost" size="sm" onClick={handleNewChat}>New Chat</Button>
        </div>

        {showSettings ? (
          <Card variant="default" padding="sm" style={{ margin: 8, flexShrink: 0 }}>
            <div style={{ display: "grid", gap: 12, gridTemplateColumns: "minmax(0,1fr) 140px 180px" }}>
              <div>
                <label style={{ display: "block", fontSize: 11, fontWeight: 500, marginBottom: 4 }}>Session Steering Note</label>
                <textarea
                  value={systemPrompt}
                  onChange={(event) => setSystemPrompt(event.target.value)}
                  rows={3}
                  style={{
                    width: "100%",
                    borderRadius: 6,
                    border: "1px solid var(--token-colors-border-subtle)",
                    padding: 8,
                    fontSize: 13,
                    resize: "vertical",
                    background: "var(--token-colors-surface-input)",
                    color: "var(--token-colors-text-default)",
                  }}
                  placeholder="Optional: steer the agent toward a specific outcome for upcoming turns..."
                />
                <div style={{ marginTop: 6, fontSize: 11, color: "var(--token-colors-text-muted)" }}>This is appended as a lightweight steering note to future turns.</div>
              </div>
              <div>
                <label style={{ display: "block", fontSize: 11, fontWeight: 500, marginBottom: 4 }}>Conversation</label>
                <div style={{ borderRadius: 6, border: "1px solid var(--token-colors-border-subtle)", padding: "8px 10px", fontSize: 11, minHeight: 34, background: "var(--token-colors-surface-input)", color: "var(--token-colors-text-default)", fontFamily: "var(--token-fontFamily-mono)" }}>
                  {conversationId ?? "new / not started"}
                </div>
                <div style={{ marginTop: 6, fontSize: 11, color: "var(--token-colors-text-muted)" }}>Multi-turn memory is preserved per conversation id.</div>
              </div>
              <div>
                <label style={{ display: "block", fontSize: 11, fontWeight: 500, marginBottom: 4 }}>Role</label>
                <select
                  value={activeRole}
                  onChange={(event) => setActiveRole(event.target.value)}
                  style={{
                    width: "100%",
                    borderRadius: 6,
                    border: "1px solid var(--token-colors-border-subtle)",
                    padding: "6px 8px",
                    fontSize: 12,
                    background: "var(--token-colors-surface-input)",
                    color: "var(--token-colors-text-default)",
                  }}
                >
                  <option value="executive">Executive</option>
                  <option value="principal_architect">Principal Architect</option>
                  <option value="junior_dev">Junior Dev</option>
                </select>
                <div style={{ marginTop: 6, display: "flex", flexWrap: "wrap", gap: 6 }}>
                  {toolCatalog?.tools.map((tool) => (
                    <Badge key={tool.id} size="sm" variant={tool.enabled ? "default" : "warning"}>{tool.label}</Badge>
                  ))}
                </div>
              </div>
            </div>
          </Card>
        ) : null}

        <div style={{ flex: 1, overflow: "auto", padding: 16 }}>
          <Card variant="outlined" padding="sm" style={{ marginBottom: 12, background: "var(--token-colors-alpha-bg-_08)" }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 10, flexWrap: "wrap" }}>
              <div>
                <div style={{ fontSize: 12, fontWeight: 700 }}>Agent Runtime</div>
                <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>Presence, witness thread, and receipt river for the active Knoxx turn.</div>
              </div>
              <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                <Badge size="sm" variant={wsStatus === "connected" ? "success" : wsStatus === "connecting" ? "warning" : "error"}>{wsStatus}</Badge>
                <Badge size="sm" variant={latestRun?.status === "completed" ? "success" : latestRun?.status === "failed" ? "error" : isSending ? "warning" : "default"}>
                  {latestRun?.status ?? (isSending ? "running" : "idle")}
                </Badge>
                <Badge size="sm" variant="info">{selectedModel || "no-model"}</Badge>
              </div>
            </div>

            <div style={{ marginBottom: 12, padding: 10, borderRadius: 10, border: "1px solid var(--token-colors-border-default)", background: "var(--token-colors-background-surface)" }}>
              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 8, flexWrap: "wrap" }}>
                <div>
                  <div style={{ fontSize: 11, fontWeight: 700, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Live Intervention</div>
                  <div style={{ fontSize: 11, color: "var(--token-colors-text-subtle)" }}>Steer the current turn or queue a follow-up while the agent is still working.</div>
                </div>
                <Badge size="sm" variant={liveControlEnabled ? "success" : "default"}>{liveControlEnabled ? "active" : "idle"}</Badge>
              </div>
              <textarea
                value={liveControlText}
                onChange={(event) => setLiveControlText(event.target.value)}
                rows={2}
                placeholder={liveControlEnabled ? "e.g. steer: focus on the corpus evidence, or follow up: turn this into a file draft" : "Start a turn to enable steer/follow-up controls"}
                disabled={!liveControlEnabled || queueingControl !== null}
                style={{
                  width: "100%",
                  borderRadius: 8,
                  border: "1px solid var(--token-colors-border-subtle)",
                  padding: 8,
                  fontSize: 12,
                  resize: "vertical",
                  background: "var(--token-colors-surface-input)",
                  color: "var(--token-colors-text-default)",
                }}
              />
              <div style={{ display: "flex", gap: 8, marginTop: 8, flexWrap: "wrap" }}>
                <Button variant="secondary" size="sm" loading={queueingControl === "steer"} disabled={!liveControlEnabled || !liveControlText.trim() || queueingControl !== null} onClick={() => void queueLiveControl("steer")}>Steer current turn</Button>
                <Button variant="ghost" size="sm" loading={queueingControl === "follow_up"} disabled={!liveControlEnabled || !liveControlText.trim() || queueingControl !== null} onClick={() => void queueLiveControl("follow_up")}>Queue follow-up</Button>
              </div>
            </div>

            <div style={{ display: "grid", gap: 12, gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))" }}>
              <div style={{ display: "grid", gap: 6 }}>
                <div style={{ fontSize: 11, fontWeight: 700, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Presence</div>
                <div style={{ fontSize: 11, color: "var(--token-colors-text-subtle)" }}>Conversation</div>
                <div style={{ fontFamily: "var(--token-fontFamily-mono)", fontSize: 11, borderRadius: 8, border: "1px solid var(--token-colors-border-default)", padding: "6px 8px" }}>
                  {conversationId ?? "new / not started"}
                </div>
                <div style={{ fontSize: 11, color: "var(--token-colors-text-subtle)" }}>Latest run</div>
                <div style={{ fontFamily: "var(--token-fontFamily-mono)", fontSize: 11, borderRadius: 8, border: "1px solid var(--token-colors-border-default)", padding: "6px 8px" }}>
                  {latestRun?.run_id ?? activeRunIdRef.current ?? "waiting for first turn"}
                </div>
                <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>
                  {latestRun?.ttft_ms != null ? `TTFT ${Math.round(latestRun.ttft_ms)}ms` : "No token timing yet"}
                  {latestRun?.total_time_ms != null ? ` • total ${Math.round(latestRun.total_time_ms)}ms` : ""}
                </div>
              </div>

              <div style={{ display: "grid", gap: 8 }}>
                <div style={{ fontSize: 11, fontWeight: 700, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Witness Thread</div>
                {hydrationSources.length === 0 ? (
                  <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)", lineHeight: 1.5 }}>
                    Passive hydration has not surfaced corpus witnesses for the latest run yet.
                  </div>
                ) : (
                  hydrationSources.slice(0, 3).map((source) => (
                    <div key={source.path} style={{ border: `1px solid ${assistantSurfaceBorder}`, borderRadius: 8, padding: 8, background: assistantSurfaceBackground, color: assistantSurfaceText }}>
                      <div style={{ fontSize: 11, fontWeight: 600, color: assistantSurfaceText }}>{source.title}</div>
                      <div style={{ fontSize: 10, color: assistantSurfaceText, opacity: 0.84, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{source.path}</div>
                      {source.section ? (
                        <div style={{ marginTop: 6 }}>
                          <Markdown content={asMarkdownPreview(truncateText(source.section, 180))} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                        </div>
                      ) : null}
                      <div style={{ display: "flex", gap: 6, marginTop: 8, flexWrap: "wrap" }}>
                        <Button variant="ghost" size="sm" onClick={() => void openHydrationSource(source)}>Open</Button>
                        <Button variant="ghost" size="sm" onClick={() => pinHydrationSource(source)}>Pin</Button>
                        <Button variant="ghost" size="sm" onClick={() => appendToScratchpad(source.section || source.path, source.title)}>Insert</Button>
                      </div>
                    </div>
                  ))
                )}
              </div>

              <div style={{ display: "grid", gap: 8 }}>
                <div style={{ fontSize: 11, fontWeight: 700, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Receipt River</div>
                {runtimeEvents.length === 0 ? (
                  <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>No live runtime events yet.</div>
                ) : (
                  runtimeEvents.slice(-6).reverse().map((event, index) => (
                    <div key={`${event.type ?? "event"}:${event.at ?? index}`} style={{ borderLeft: "2px solid var(--token-colors-accent-cyan)", padding: "8px 10px", borderRadius: 8, background: assistantSurfaceBackground, color: assistantSurfaceText }}>
                      <div style={{ fontSize: 11, fontWeight: 600, color: assistantSurfaceText }}>{event.type ?? "event"}{event.tool_name ? ` • ${event.tool_name}` : ""}</div>
                      <div style={{ fontSize: 10, color: assistantSurfaceText, opacity: 0.84 }}>{formatMaybeDate(event.at as string | undefined) ?? event.at ?? "just now"}</div>
                      {typeof event.preview === "string" && event.preview.trim().length > 0 ? (
                        <div style={{ marginTop: 6 }}>
                          <Markdown content={asMarkdownPreview(truncateText(event.preview, 300))} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                        </div>
                      ) : null}
                    </div>
                  ))
                )}
              </div>
            </div>

            {latestToolReceipts.length ? (
              <div style={{ marginTop: 12, paddingTop: 12, borderTop: "1px solid var(--token-colors-border-default)", display: "grid", gap: 8 }}>
                <div style={{ fontSize: 11, fontWeight: 700, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>Tool Receipts</div>
                {latestToolReceipts.slice(0, 4).map((receipt) => (
                  <div key={receipt.id} style={{ border: `1px solid ${assistantSurfaceBorder}`, borderRadius: 8, padding: 8, background: assistantSurfaceBackground, color: assistantSurfaceText }}>
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
                      <div style={{ fontSize: 11, fontWeight: 600, color: assistantSurfaceText }}>{receipt.tool_name ?? receipt.id}</div>
                      <Badge size="sm" variant={receipt.status === "completed" ? "success" : receipt.status === "failed" ? "error" : "warning"}>{receipt.status ?? "running"}</Badge>
                    </div>
                    {typeof receipt.input_preview === "string" && receipt.input_preview.trim().length > 0 ? (
                      <div style={{ marginTop: 6 }}>
                        <div style={{ fontSize: 10, fontWeight: 600, color: assistantSurfaceText, opacity: 0.84, marginBottom: 4 }}>input</div>
                        <Markdown content={asMarkdownPreview(truncateText(receipt.input_preview, 500))} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                      </div>
                    ) : null}
                    {typeof receipt.result_preview === "string" && receipt.result_preview.trim().length > 0 ? (
                      <div style={{ marginTop: 6 }}>
                        <div style={{ fontSize: 10, fontWeight: 600, color: assistantSurfaceText, opacity: 0.84, marginBottom: 4 }}>output</div>
                        <Markdown content={asMarkdownPreview(truncateText(receipt.result_preview, 500))} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                      </div>
                    ) : null}
                  </div>
                ))}
              </div>
            ) : null}
          </Card>

          {messages.length === 0 ? (
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", height: "100%", color: "var(--token-colors-text-muted)", gap: 8 }}>
              <div style={{ fontSize: 20, fontWeight: 600 }}>Chat</div>
              <div style={{ fontSize: 14 }}>Ask Knoxx anything about devel, your client work, or the artifact you are actively building.</div>
              <div style={{ fontSize: 13 }}>Use the context bar like an IDE explorer, pin the context that matters, and use the scratchpad as your live working surface.</div>
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 12, width: "100%" }}>
              {messages.map((message) => (
                <Card
                  key={message.id}
                  variant="outlined"
                  padding="sm"
                  style={{
                    borderColor:
                      message.role === "user"
                        ? "var(--token-colors-alpha-green-_30)"
                        : message.role === "system"
                          ? "var(--token-colors-alpha-cyan-_30)"
                          : "var(--token-colors-border-default)",
                    background:
                      message.role === "user"
                        ? "var(--token-colors-alpha-green-_08)"
                        : message.role === "system"
                          ? "var(--token-colors-alpha-cyan-_08)"
                          : "var(--token-colors-background-surface)",
                    alignSelf: message.role === "user" ? "flex-end" : "flex-start",
                    maxWidth: message.role === "user" ? "80%" : "100%",
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 6 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                      <span style={{ fontSize: 11, fontWeight: 600, textTransform: "uppercase", color: "var(--token-colors-text-muted)" }}>{message.role}</span>
                      {message.model ? <Badge size="sm" variant="info">{message.model}</Badge> : null}
                      {message.status ? <Badge size="sm" variant={message.status === "done" ? "success" : message.status === "error" ? "error" : "warning"}>{message.status}</Badge> : null}
                      {message.runId ? <Badge size="sm" variant="default">{message.runId.slice(0, 8)}</Badge> : null}
                      {message.role === "assistant" ? (
                        <Badge size="sm" variant={(message.sources?.length || message.contextRows?.length) ? "success" : "warning"}>
                          {message.sources?.length
                            ? `${message.sources.length} source(s)`
                            : message.contextRows?.length
                              ? `${message.contextRows.length} context row(s)`
                              : "No grounding metadata"}
                        </Badge>
                      ) : null}
                    </div>
                    {message.role === "assistant" ? (
                      <Button variant="ghost" size="sm" onClick={() => openMessageInCanvas(message)}>Open in Scratchpad</Button>
                    ) : null}
                  </div>
                  {message.role === "assistant" || message.role === "system" ? (
                    <Markdown content={message.content || ""} theme="dark" variant="full" />
                  ) : (
                    <div style={{ fontSize: 14, whiteSpace: "pre-wrap", lineHeight: 1.6 }}>{message.content}</div>
                  )}
                  {message.sources?.length ? (
                    <details style={{ marginTop: 12 }} open>
                      <summary style={{ cursor: "pointer", fontSize: 12, fontWeight: 600, color: "var(--token-colors-text-muted)" }}>
                        Grounding sources ({message.sources.length})
                      </summary>
                      <div style={{ display: "grid", gap: 8, marginTop: 10 }}>
                        {message.sources.map((source, idx) => {
                          const path = sourceUrlToPath(source.url);
                          return (
                            <div key={`${source.title}:${idx}`} style={{ border: `1px solid ${assistantSurfaceBorder}`, borderRadius: 8, padding: 10, background: assistantSurfaceBackground, color: assistantSurfaceText }}>
                              <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 6 }}>
                                <div style={{ minWidth: 0 }}>
                                  <div style={{ fontSize: 12, fontWeight: 600, color: "var(--token-colors-text-default)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{source.title || fileNameFromPath(path)}</div>
                                  <div style={{ fontSize: 10, color: assistantSurfaceText, opacity: 0.84, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{path || source.url}</div>
                                </div>
                                <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                                  {path ? <Button variant="ghost" size="sm" onClick={() => void openSourceInPreview(source)}>Open</Button> : null}
                                  <Button variant="ghost" size="sm" onClick={() => pinAssistantSource(source)}>Pin</Button>
                                  <Button variant="ghost" size="sm" onClick={() => appendToScratchpad(source.section || path || source.title, source.title)}>Insert</Button>
                                </div>
                              </div>
                              {source.section ? <Markdown content={asMarkdownPreview(source.section)} theme="dark" variant="compact" lineNumbers={false} copyButton={false} /> : null}
                            </div>
                          );
                        })}
                      </div>
                    </details>
                  ) : null}
                  {message.contextRows?.length ? (
                    <details style={{ marginTop: 12 }}>
                      <summary style={{ cursor: "pointer", fontSize: 12, fontWeight: 600, color: "var(--token-colors-text-muted)" }}>
                        Auto-injected context ({message.contextRows.length})
                      </summary>
                      <div style={{ display: "grid", gap: 8, marginTop: 10 }}>
                        {message.contextRows.map((row) => (
                          <div key={row.id} style={{ border: `1px solid ${assistantSurfaceBorder}`, borderRadius: 8, padding: 10, background: assistantSurfaceBackground, color: assistantSurfaceText }}>
                            <div style={{ display: "flex", alignItems: "center", gap: 6, flexWrap: "wrap", marginBottom: 6 }}>
                              <Badge size="sm" variant="default">{row.project ?? "unknown-project"}</Badge>
                              <Badge size="sm" variant="default">{row.kind ?? "unknown-kind"}</Badge>
                            </div>
                            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 4 }}>
                              <div style={{ fontSize: 12, fontWeight: 600, color: "var(--token-colors-text-default)", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{contextPath(row)}</div>
                              <Button variant="ghost" size="sm" onClick={() => pinMessageContext(row)}>Pin</Button>
                            </div>
                            <Markdown content={asMarkdownPreview(row.snippet ?? row.text ?? "")} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                          </div>
                        ))}
                      </div>
                    </details>
                  ) : null}
                </Card>
              ))}
            </div>
          )}
        </div>

        <div style={{ padding: 12, borderTop: "1px solid var(--token-colors-border-default)", flexShrink: 0 }}>
          <ChatComposer onSend={handleSend} isSending={isSending || !selectedModel} />
        </div>

        {showConsole ? (
          <div style={{ height: 220, borderTop: "1px solid var(--token-colors-border-default)", flexShrink: 0 }}>
            <ConsolePanel lines={consoleLines} />
            <div style={{ padding: "6px 12px", borderTop: "1px solid var(--token-colors-border-default)", fontSize: 11, color: "var(--token-colors-text-muted)" }}>
              WebSocket: {wsStatus}
            </div>
          </div>
        ) : null}
      </div>

      {showCanvas ? (
        <Card
          variant="default"
          padding="none"
          style={{ width: 420, flexShrink: 0, borderLeft: "1px solid var(--token-colors-border-default)", display: "flex", flexDirection: "column", minHeight: 0 }}
        >
          <div style={{ padding: 12, borderBottom: "1px solid var(--token-colors-border-default)", display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
            <div>
              <div style={{ fontSize: 14, fontWeight: 600 }}>Scratchpad</div>
              <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>Your active working surface. Draft, reshape, pin context, and save artifacts from here.</div>
            </div>
            <div style={{ display: "flex", gap: 6 }}>
              <Button variant="ghost" size="sm" onClick={useLatestAssistantInCanvas}>Use Latest</Button>
              <Button variant="ghost" size="sm" onClick={() => setShowCanvas(false)}>Hide</Button>
            </div>
          </div>

          <div style={{ padding: 12, display: "grid", gap: 10, borderBottom: "1px solid var(--token-colors-border-default)" }}>
            <Input value={canvasTitle} onChange={(event: ChangeEvent<HTMLInputElement>) => setCanvasTitle(event.target.value)} placeholder="Scratchpad title" size="sm" />
            <Input value={canvasPath} onChange={(event: ChangeEvent<HTMLInputElement>) => setCanvasPath(event.target.value)} placeholder="Workspace path for saving the scratchpad" size="sm" />
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
              <Button variant="secondary" size="sm" loading={savingCanvas} onClick={saveCanvasDraft}>Save to CMS</Button>
              <Button variant="secondary" size="sm" loading={savingCanvasFile} onClick={saveCanvasFile}>Save File</Button>
              <Button variant="ghost" size="sm" onClick={clearScratchpad}>Clear</Button>
            </div>
            <details>
              <summary style={{ cursor: "pointer", fontSize: 11, fontWeight: 600, color: "var(--token-colors-text-muted)" }}>Optional delivery actions</summary>
              <div style={{ display: "grid", gap: 10, marginTop: 10 }}>
                <Input value={canvasSubject} onChange={(event: ChangeEvent<HTMLInputElement>) => setCanvasSubject(event.target.value)} placeholder="Email subject" size="sm" />
                <Input value={canvasRecipients} onChange={(event: ChangeEvent<HTMLInputElement>) => setCanvasRecipients(event.target.value)} placeholder="To: comma-separated emails" size="sm" />
                <Input value={canvasCc} onChange={(event: ChangeEvent<HTMLInputElement>) => setCanvasCc(event.target.value)} placeholder="Cc: comma-separated emails" size="sm" />
                <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                  <Button variant="primary" size="sm" loading={sendingCanvas} disabled={!toolCatalog?.email_enabled} onClick={sendCanvasEmailAction}>Send Email</Button>
                  {!toolCatalog?.email_enabled ? <span style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>Email is optional and currently unavailable.</span> : null}
                </div>
              </div>
            </details>
            {canvasStatus ? <div style={{ fontSize: 11, color: "var(--token-colors-text-subtle)" }}>{canvasStatus}</div> : null}
          </div>

          <div style={{ flex: 1, minHeight: 0, padding: 12 }}>
            <textarea
              value={canvasContent}
              onChange={(event) => setCanvasContent(event.target.value)}
              placeholder="Work here like a ChatGPT canvas: specs, notes, plans, code sketches, prompts, drafts, implementation checklists, or excerpts from pinned context..."
              style={{
                width: "100%",
                height: "100%",
                minHeight: 280,
                borderRadius: 8,
                border: "1px solid var(--token-colors-border-subtle)",
                padding: 12,
                fontSize: 13,
                lineHeight: 1.6,
                resize: "none",
                fontFamily: "var(--token-fontFamily-mono)",
                background: "var(--token-colors-surface-input)",
                color: "var(--token-colors-text-default)",
              }}
            />
          </div>
        </Card>
      ) : null}

    </div>
  );
}

export default ChatPage;
