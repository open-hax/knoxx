import { useMemo } from 'react';
import type { RunDetail, RunEvent, ToolReceipt } from '../../lib/types';
import type { BrowseResponse, SemanticSearchMatch, WorkspaceJob } from './types';
import { latestRunHydrationSources, parentPath } from './utils';

type UseChatPageDerivedStateParams = {
  browseData: BrowseResponse | null;
  entryFilter: string;
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

  const filteredEntries = useMemo(() => {
    const entries = browseData?.entries ?? [];
    const query = entryFilter.trim().toLowerCase();
    if (!query) return entries;
    return entries.filter((entry) => entry.name.toLowerCase().includes(query) || entry.path.toLowerCase().includes(query));
  }, [browseData?.entries, entryFilter]);

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
    workspaceProgressPercent,
  };
}
