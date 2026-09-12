import type { MouseEvent as ReactMouseEvent, Ref } from "react";
import type { ActorCatalogItem, MemorySearchHit, MemorySessionSummary } from "../../lib/types";
import type { BrowseEntry, BrowseResponse, PinnedContextItem, PreviewResponse, SemanticSearchMatch, WorkspaceJob } from "../workspace-context/types";

export type {
  BrowseEntry,
  BrowseResponse,
  HydrationSource,
  IngestionSource,
  PinnedContextItem,
  PreviewResponse,
  SemanticSearchMatch,
  SemanticSearchResponse,
  SessionStateSnapshot,
  WorkspaceJob,
} from "../workspace-context/types";

export type ContextBarProps = {
  // Layout props (required for resize)
  sidebarWidthPx: number;
  sidebarPaneSplitPct: number;
  sidebarSplitContainerRef: Ref<HTMLDivElement>;
  onHide: () => void;
  onStartSidebarPaneResize: (event: ReactMouseEvent<HTMLDivElement>) => void;
  onStartSidebarWidthResize: (event: ReactMouseEvent<HTMLDivElement>) => void;
  // Filters (shared across workplaces)
  visibilityFilter: string;
  kindFilter: string;
  statsTotal: number;
  statsByVisibility: Record<string, number>;
  onVisibilityFilterChange: (value: string) => void;
  onKindFilterChange: (value: string) => void;
  // Optional additional filters (CMS)
  sourceFilter?: string;
  domainFilter?: string;
  pathPrefixFilter?: string;
  onSourceFilterChange?: (value: string) => void;
  onDomainFilterChange?: (value: string) => void;
  onPathPrefixFilterChange?: (value: string) => void;
  // Actions
  onNewDocument?: () => void;
  onNewVisualDraft?: () => void;
  // Chat workspace props (optional)
  currentPath?: string;
  currentParentPath?: string;
  browseData?: BrowseResponse | null;
  previewData?: PreviewResponse | null;
  loadingBrowse?: boolean;
  loadingPreview?: boolean;
  entryFilter?: string;
  semanticQuery?: string;
  semanticResults?: SemanticSearchMatch[];
  semanticProjects?: string[];
  semanticSearching?: boolean;
  sessionSearchHits?: MemorySearchHit[];
  sessionSearchMode?: string;
  semanticMode?: boolean;
  filteredEntries?: BrowseEntry[];
  activeEntryCount?: number;
  workspaceSourceId?: string | null;
  workspaceJob?: WorkspaceJob | null;
  workspaceProgressPercent?: number;
  pinnedContext?: PinnedContextItem[];
  recentSessions?: MemorySessionSummary[];
  recentSessionsHasMore?: boolean;
  recentSessionsTotal?: number;
  loadingRecentSessions?: boolean;
  loadingMoreRecentSessions?: boolean;
  loadingMemorySessionId?: string | null;
  sessionId?: string;
  conversationId?: string | null;
  availableActors?: ActorCatalogItem[];
  sessionActorFilter?: string;
  excludeEtaMuSessions?: boolean;
  onLoadDirectory?: (path?: string) => void | Promise<void>;
  onEntryFilterChange?: (value: string) => void;
  onSemanticQueryChange?: (value: string) => void;
  onSemanticSearch?: () => void | Promise<void>;
  onClearSemanticSearch?: () => void;
  onSessionActorFilterChange?: (value: string) => void;
  onExcludeEtaMuSessionsChange?: (value: boolean) => void;
  onRefreshRecentSessions?: () => void | Promise<void>;
  onLoadMoreRecentSessions?: () => void | Promise<void>;
  onResumeMemorySession?: (sessionId: string) => void | Promise<void>;
  onPreviewFile?: (path: string) => void | Promise<void>;
  onOpenFile?: (entry: BrowseEntry) => void | Promise<void>;
  onPinSemanticResult?: (entry: SemanticSearchMatch) => void;
  onAppendToScratchpad?: (text: string, heading?: string) => void;
  onPinPreviewContext?: () => void;
  onOpenPreviewInCanvas?: () => void | Promise<void>;
  onOpenPinnedInCanvas?: (item: PinnedContextItem) => void | Promise<void>;
  onInsertPinnedIntoCanvas?: (item: PinnedContextItem) => void;
  onUnpinContextItem?: (path: string) => void;
};

export type ContextBarExplorerProps = {
  semanticMode: boolean;
  activeEntryCount: number;
  currentPath: string;
  currentParentPath: string;
  semanticProjects: string[];
  loadingBrowse: boolean;
  browseData: BrowseResponse | null;
  semanticResults: SemanticSearchMatch[];
  filteredEntries: BrowseEntry[];
  previewData: PreviewResponse | null;
  loadingPreview: boolean;
  onPreviewFile?: (path: string) => void | Promise<void>;
  onOpenFile?: (entry: BrowseEntry) => void | Promise<void>;
  onLoadDirectory: (path?: string) => void | Promise<void>;
  onPinSemanticResult: (entry: SemanticSearchMatch) => void;
  onAppendToScratchpad: (text: string, heading?: string) => void;
  onPinPreviewContext: () => void;
  onOpenPreviewInCanvas: () => void | Promise<void>;
};

export type ContextBarFiltersProps = Pick<ContextBarProps,
    "onEntryFilterChange" | "entryFilter" | "onSemanticQueryChange" | "semanticQuery"
  | "semanticMode" | "onClearSemanticSearch" | "semanticSearching" | "onSemanticSearch"
  | "onSessionActorFilterChange" | "sessionActorFilter" | "onExcludeEtaMuSessionsChange" | "excludeEtaMuSessions"
  | "onSourceFilterChange" | "sourceFilter" | "onDomainFilterChange" | "domainFilter"
  | "onPathPrefixFilterChange" | "pathPrefixFilter" | "visibilityFilter" | "onVisibilityFilterChange"
  | "statsByVisibility" | "kindFilter" | "onKindFilterChange" | "onNewDocument"
  | "onNewVisualDraft" | "workspaceJob"
> & {
  actorOptions: Array<{ id: string; label: string }>;
  statusColor: string;
  ingestionStatus: string | undefined;
};
