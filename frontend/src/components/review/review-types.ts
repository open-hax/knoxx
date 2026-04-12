/**
 * Review Queue Types
 *
 * Types for review queue items, labels, and actions.
 */

/** Types of items in the review queue */
export type ReviewItemType = "synthesis" | "MT" | "ingestion";

/** Status of a review item */
export type ReviewItemStatus = "pending" | "approved" | "rejected" | "flagged";

/** A single item in the review queue */
export interface ReviewItem {
  id: string;
  type: ReviewItemType;
  status: ReviewItemStatus;
  title: string;
  summary: string;
  confidence: number; // 0-1
  created_at: string;
  source_count: number;
  agent_name?: string;
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

/** Mock review items for development */
export const MOCK_REVIEW_ITEMS: ReviewItem[] = [
  {
    id: "rev-1",
    type: "synthesis",
    status: "pending",
    title: "API Documentation Summary",
    summary: "Generated summary of REST API endpoints for v2 release.",
    confidence: 0.72,
    created_at: "2024-01-15T14:00:00Z",
    source_count: 12,
    agent_name: "synthesizer-v3",
  },
  {
    id: "rev-2",
    type: "MT",
    status: "pending",
    title: "German Translation: Getting Started",
    summary: "Machine translation of onboarding guide to German.",
    confidence: 0.45,
    created_at: "2024-01-15T13:30:00Z",
    source_count: 1,
    agent_name: "mt-de-v2",
  },
  {
    id: "rev-3",
    type: "ingestion",
    status: "pending",
    title: "Changelog Extraction",
    summary: "Extracted changelog entries from commit history.",
    confidence: 0.89,
    created_at: "2024-01-15T12:00:00Z",
    source_count: 48,
  },
  {
    id: "rev-4",
    type: "synthesis",
    status: "pending",
    title: "Architecture Decision Record",
    summary: "Generated ADR for database migration strategy.",
    confidence: 0.38,
    created_at: "2024-01-15T11:00:00Z",
    source_count: 8,
    agent_name: "synthesizer-v3",
  },
  {
    id: "rev-5",
    type: "MT",
    status: "pending",
    title: "Japanese Translation: API Reference",
    summary: "Machine translation of API reference to Japanese.",
    confidence: 0.61,
    created_at: "2024-01-15T10:00:00Z",
    source_count: 1,
    agent_name: "mt-ja-v2",
  },
];
