import type { RunDetail } from "../../lib/types";

export type BrowseEntry = {
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
  visibility?: "internal" | "review" | "public" | "archived";
};

export type BrowseResponse = {
  workspace_root: string;
  current_path: string;
  entries: BrowseEntry[];
};

export type PreviewResponse = {
  path: string;
  size: number;
  truncated: boolean;
  content: string;
};

export type IngestionSource = {
  source_id: string;
  name: string;
  config?: Record<string, unknown> | null;
};

export type SemanticSearchMatch = {
  id: string;
  path: string;
  project?: string;
  kind?: string;
  snippet?: string;
  distance?: number | null;
};

export type SemanticSearchResponse = {
  projects: string[];
  count: number;
  rows: SemanticSearchMatch[];
};

export type WorkspaceJob = {
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

export type PinnedContextItem = {
  id: string;
  title: string;
  path: string;
  snippet?: string;
  kind: "file" | "semantic" | "message";
};

export type HydrationSource = { title: string; path: string; section?: string };

export type SessionStateSnapshot = {
  sessionId?: string;
  systemPrompt?: string;
  selectedModel?: string;
  conversationId?: string | null;
  messages?: RunDetail["request_messages"] | unknown;
  latestRun?: RunDetail | null;
};
