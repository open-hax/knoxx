/**
 * Review Queue Types
 *
 * Types for review queue items, labels, and actions.
 * Matches /v1/reviews API response.
 */

/** Types of items in the review queue */
export type ReviewItemType = "synthesis" | "MT" | "ingestion";

/** Status of a review item */
export type ReviewItemStatus = "pending" | "approved" | "rejected" | "flagged";

/** Source type from API */
export type ReviewSource = "manual" | "ai-drafted" | "ingestion" | "import";

/** A single item in the review queue (from API) */
export interface ReviewItem {
  doc_id: string;
  tenant_id: string;
  title: string;
  content_preview: string;
  visibility: "internal" | "review" | "public" | "archived";
  source: ReviewSource;
  ai_drafted: boolean;
  confidence: number; // 0-1
  created_at: string;
  updated_at: string;
  source_count: number;
  agent_name: string | null;
}

/** Review queue statistics */
export interface ReviewStats {
  pending: number;
  flagged: number;
  approved_today: number;
  rejected_today: number;
}

/** Batch action types */
export type BatchAction = "approve-all" | "reject-all" | "flag-for-review";

/** Item type configuration */
export const ITEM_TYPE_CONFIG: Record<ReviewItemType, { label: string; color: string }> = {
  synthesis: { label: "Synthesis", color: "var(--token-colors-accent-cyan)" },
  MT: { label: "MT Pipeline", color: "var(--token-colors-accent-purple)" },
  ingestion: { label: "Ingestion", color: "var(--token-colors-accent-green)" },
};

/** Status configuration */
export const ITEM_STATUS_CONFIG: Record<ReviewItemStatus, { label: string; color: string }> = {
  pending: { label: "Pending", color: "var(--token-colors-text-muted)" },
  approved: { label: "Approved", color: "var(--token-colors-accent-green)" },
  rejected: { label: "Rejected", color: "var(--token-colors-accent-red)" },
  flagged: { label: "Flagged", color: "var(--token-colors-accent-amber)" },
};

/** Map source to review item type for display */
export function sourceToType(source: ReviewSource, aiDrafted: boolean): ReviewItemType {
  if (aiDrafted) return "synthesis";
  if (source === "import") return "MT";
  return "ingestion";
}

/** Map visibility to status for display */
export function visibilityToStatus(
  visibility: ReviewItem["visibility"],
  flagged?: boolean
): ReviewItemStatus {
  if (flagged) return "flagged";
  if (visibility === "public") return "approved";
  if (visibility === "internal") return "rejected";
  return "pending";
}
