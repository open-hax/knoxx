/**
 * Empty State Component
 *
 * Warm message + primary action for empty lists.
 * Used in all views that have list/card content.
 */

import { Button } from "@open-hax/uxx";

export interface EmptyStateProps {
  /** Title for the empty state */
  title: string;
  /** Description of why the list is empty */
  message: string;
  /** Label for the primary action button */
  actionLabel?: string;
  /** Handler for the primary action */
  onAction?: () => void;
  /** Optional icon (emoji or element) */
  icon?: string;
}

export function EmptyState({
  title,
  message,
  actionLabel,
  onAction,
  icon = "📭",
}: EmptyStateProps) {
  return (
    <div
      style={{
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        padding: "2rem",
        textAlign: "center",
        gap: "1rem",
      }}
    >
      <div
        style={{
          fontSize: "3rem",
          lineHeight: 1,
          opacity: 0.6,
        }}
      >
        {icon}
      </div>

      <div>
        <h3
          style={{
            margin: "0 0 0.5rem 0",
            fontSize: "1.125rem",
            fontWeight: 600,
            color: "var(--token-colors-text-default)",
          }}
        >
          {title}
        </h3>
        <p
          style={{
            margin: 0,
            fontSize: "0.875rem",
            color: "var(--token-colors-text-muted)",
            maxWidth: "24rem",
          }}
        >
          {message}
        </p>
      </div>

      {actionLabel && onAction && (
        <Button variant="primary" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </div>
  );
}

export default EmptyState;
