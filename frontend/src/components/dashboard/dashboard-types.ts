/**
 * Dashboard Attention Types
 *
 * Types for attention cards and dashboard metrics.
 */

export type AttentionType = "review" | "approval" | "policy";

export interface AttentionMetric {
  type: AttentionType;
  count: number;
  label: string;
  description: string;
  ctaLabel: string;
  ctaPath: string;
}

/**
 * Attention type configuration.
 */
export const ATTENTION_CONFIG: Record<AttentionType, Omit<AttentionMetric, "count">> = {
  review: {
    type: "review",
    label: "Review Queue",
    description: "Items pending review before publication",
    ctaLabel: "Review items",
    ctaPath: "/workbench/review",
  },
  approval: {
    type: "approval",
    label: "Approvals",
    description: "Items awaiting your approval",
    ctaLabel: "Approve items",
    ctaPath: "/workbench/review?filter=approval",
  },
  policy: {
    type: "policy",
    label: "Policy Violations",
    description: "Items flagged for policy review",
    ctaLabel: "Review violations",
    ctaPath: "/workbench/ops?filter=policy",
  },
};

/**
 * Get color for attention type.
 */
export const ATTENTION_COLORS: Record<AttentionType, string> = {
  review: "var(--token-colors-accent-cyan)",
  approval: "var(--token-colors-accent-amber)",
  policy: "var(--token-colors-accent-red)",
};
