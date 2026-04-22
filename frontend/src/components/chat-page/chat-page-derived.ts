import { useMemo } from 'react';
import type { RunDetail, RunEvent, ToolReceipt } from '../../lib/types';
import type { BrowseEntry, BrowseResponse, SemanticSearchMatch, WorkspaceJob } from './types';
import { latestRunHydrationSources, parentPath } from './utils';

const CODE_EXTENSIONS = new Set(["ts", "tsx", "js", "jsx", "mjs", "cjs", "py", "clj", "cljs", "cljc", "rs", "go", "java", "rb", "php"]);
const DOC_EXTENSIONS = new Set(["md", "mdx", "txt", "rst", "adoc"]);
const CONFIG_EXTENSIONS = new Set(["json", "yaml", "yml", "toml", "ini", "env", "conf"]);
const DATA_EXTENSIONS = new Set(["csv", "tsv", "sql", "xml"]);

function fileExtension(path: string): string {
  const parts = path.toLowerCase().split(".");
  return parts.length > 1 ? parts[parts.length - 1] ?? "" : "";
}

export function inferBrowseEntryKind(entry: BrowseEntry): "docs" | "code" | "config" | "data" {
  const ext = fileExtension(entry.path || entry.name);
  if (CODE_EXTENSIONS.has(ext)) return "code";
  if (CONFIG_EXTENSIONS.has(ext)) return "config";
  if (DATA_EXTENSIONS.has(ext)) return "data";
  if (DOC_EXTENSIONS.has(ext)) return "docs";
  return "docs";
}

export function filterBrowseEntries(entries: BrowseEntry[], entryFilter: string, visibilityFilter: string, kindFilter: string): BrowseEntry[] {
  const query = entryFilter.trim().toLowerCase();
  return entries.filter((entry) => {
    const matchesQuery = !query || entry.name.toLowerCase().includes(query) || entry.path.toLowerCase().includes(query);
    if (!matchesQuery) return false;
    if (entry.type === 'dir') return true;
    const matchesVisibility = visibilityFilter === 'all' || entry.visibility === visibilityFilter;
    const matchesKind = kindFilter === 'all' || inferBrowseEntryKind(entry) === kindFilter;
    return matchesVisibility && matchesKind;
  });
}

export function visibilityStats(entries: BrowseEntry[]): { total: number; byVisibility: Record<string, number> } {
  const files = entries.filter((entry) => entry.type === 'file');
  const byVisibility = files.reduce<Record<string, number>>((acc, entry) => {
    const key = entry.visibility ?? 'internal';
    acc[key] = (acc[key] ?? 0) + 1;
    return acc;
  }, {});
  return {
    total: files.length,
    byVisibility,
  };
}

type UseChatPageDerivedStateParams = {
  browseData: BrowseResponse | null;
  entryFilter: string;
  visibilityFilter: string;
  kindFilter: string;
  semanticQuery: string;
  semanticResults: SemanticSearchMatch[];
  workspaceJob: WorkspaceJob | null;
  latestRun: RunDetail | null;
  isSending: boolean;
  runtimeEvents: RunEvent[];
  pendingAssistantId: string | null;
  conversationId: string | null;
};

export function useChatPageDerivedState({
  browseData,
  entryFilter,
  visibilityFilter,
  kindFilter,
  semanticQuery,
  semanticResults,
  workspaceJob,
  latestRun,
  isSending,
  runtimeEvents,
  pendingAssistantId,
  conversationId,
}: UseChatPageDerivedStateParams) {
  const currentPath = browseData?.current_path ?? '';
  const currentParentPath = useMemo(() => parentPath(currentPath), [currentPath]);
  const stats = useMemo(() => visibilityStats(browseData?.entries ?? []), [browseData?.entries]);

  const filteredEntries = useMemo(() => {
    const entries = browseData?.entries ?? [];
    return filterBrowseEntries(entries, entryFilter, visibilityFilter, kindFilter);
  }, [browseData?.entries, entryFilter, visibilityFilter, kindFilter]);

  const semanticMode = semanticQuery.trim().length > 0;
  const activeEntryCount = semanticMode ? semanticResults.length : filteredEntries.length;
  const workspaceProgressPercent = workspaceJob && workspaceJob.total_files > 0
    ? Math.min(100, Math.round(((workspaceJob.processed_files + workspaceJob.failed_files) / workspaceJob.total_files) * 100))
    : 0;
  const latestToolReceipts = useMemo(() => (latestRun?.tool_receipts ?? []) as ToolReceipt[], [latestRun]);
  const liveToolReceipts = useMemo(() => (isSending && pendingAssistantId ? latestToolReceipts : []), [isSending, latestToolReceipts, pendingAssistantId]);
  const liveToolEvents = useMemo(() => (isSending ? runtimeEvents.filter((event) => ['tool_start', 'tool_update', 'tool_end'].includes(String(event.type ?? ''))) : []), [isSending, runtimeEvents]);
  const liveControlEnabled = Boolean(
    isSending
      && conversationId
      && runtimeEvents.some((event) => ['run_started', 'passive_hydration', 'assistant_first_token', 'tool_start'].includes(String(event.type ?? ''))),
  );
  const hydrationSources = useMemo(() => latestRunHydrationSources(latestRun), [latestRun]);

  return {
    activeEntryCount,
    assistantSurfaceBackground: 'var(--token-colors-background-surface)',
    assistantSurfaceBorder: 'var(--token-colors-border-default)',
    assistantSurfaceText: 'var(--token-colors-text-default)',
    currentParentPath,
    currentPath,
    filteredEntries,
    hydrationSources,
    latestToolReceipts,
    liveControlEnabled,
    liveToolEvents,
    liveToolReceipts,
    semanticMode,
    statsByVisibility: stats.byVisibility,
    statsTotal: stats.total,
    workspaceProgressPercent,
  };
}
