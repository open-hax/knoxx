/**
 * Memory Signal Chip Component
 *
 * Fixed vocabulary badges for stigmergic memory signals.
 * Used in Dashboard, Memory Inspector, and Provenance panels.
 *
 * Vocabulary (from knowledge-ops-workbench-ux-v1.md):
 * - reinforcement: "Signal strengthened by successful use"
 * - contradiction: "Signal challenged by conflicting observation"
 * - correction: "Signal refined by agent or human edit"
 * - decay: "Signal weakened over time without reinforcement"
 * - spike: "Signal suddenly activated by related activity"
 * - anchor: "Signal marked as foundational/authoritative"
 * - trail: "Signal is part of an active reasoning chain"
 */

import { Badge } from "@open-hax/uxx";

export type MemorySignalType =
  | "reinforcement"
  | "contradiction"
  | "correction"
  | "decay"
  | "spike"
  | "anchor"
  | "trail";

export interface MemorySignalChipProps {
  type: MemorySignalType;
  /** Optional count to display */
  count?: number;
  /** Click handler */
  onClick?: () => void;
}

const SIGNAL_CONFIG: Record<
  MemorySignalType,
  { label: string; variant: "default" | "success" | "warning" | "error" | "info"; description: string }
> = {
  reinforcement: {
    label: "↑",
    variant: "success",
    description: "Signal strengthened by successful use",
  },
  contradiction: {
    label: "✗",
    variant: "error",
    description: "Signal challenged by conflicting observation",
  },
  correction: {
    label: "✎",
    variant: "warning",
    description: "Signal refined by agent or human edit",
  },
  decay: {
    label: "↓",
    variant: "default",
    description: "Signal weakened over time without reinforcement",
  },
  spike: {
    label: "⚡",
    variant: "info",
    description: "Signal suddenly activated by related activity",
  },
  anchor: {
    label: "⚓",
    variant: "success",
    description: "Signal marked as foundational/authoritative",
  },
  trail: {
    label: "→",
    variant: "info",
    description: "Signal is part of an active reasoning chain",
  },
};

export function MemorySignalChip({ type, count, onClick }: MemorySignalChipProps) {
  const config = SIGNAL_CONFIG[type];
  const displayText = count !== undefined ? `${config.label} ${count}` : config.label;

  return (
    <span
      title={config.description}
      onClick={onClick}
      style={{ cursor: onClick ? "pointer" : "default" }}
    >
      <Badge variant={config.variant}>{displayText}</Badge>
    </span>
  );
}

/**
 * Get all signal types with their config.
 * Useful for building signal legends or filters.
 */
export function getSignalTypes(): Array<{
  type: MemorySignalType;
  label: string;
  variant: "default" | "success" | "warning" | "error" | "info";
  description: string;
}> {
  return Object.entries(SIGNAL_CONFIG).map(([type, config]) => ({
    type: type as MemorySignalType,
    ...config,
  }));
}

export default MemorySignalChip;
