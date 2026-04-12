/**
 * Ops Event Types
 *
 * Types for the Ops Log view.
 */

export type OpsEventType = "ingestion" | "embedding" | "sync" | "policy" | "MT";

export type OpsEventStatus = "done" | "warn" | "error" | "running";

export interface OpsEvent {
  id: string;
  time: Date;
  type: OpsEventType;
  status: OpsEventStatus;
  summary: string;
  duration?: number;
  inputs?: Record<string, unknown>;
  outputs?: Record<string, unknown>;
  error?: string;
  relatedReviewItemId?: string;
}

export interface OpsEventFilter {
  types?: OpsEventType[];
  status?: OpsEventStatus[];
  dateRange?: {
    start: Date;
    end: Date;
  };
}

/**
 * Status icon mapping.
 */
export const STATUS_ICONS: Record<OpsEventStatus, string> = {
  done: "✓",
  warn: "⚠",
  error: "✗",
  running: "●",
};

/**
 * Status color mapping (CSS variable references).
 */
export const STATUS_COLORS: Record<OpsEventStatus, string> = {
  done: "var(--token-colors-accent-green)",
  warn: "var(--token-colors-accent-amber)",
  error: "var(--token-colors-accent-red)",
  running: "var(--token-colors-accent-cyan)",
};

/**
 * Type label mapping.
 */
export const TYPE_LABELS: Record<OpsEventType, string> = {
  ingestion: "Ingestion",
  embedding: "Embedding",
  sync: "Sync",
  policy: "Policy Check",
  MT: "MT Pipeline",
};
