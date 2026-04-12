/**
 * Queue List Component
 *
 * List of review items sorted by confidence.
 */

import { useMemo } from "react";
import type {
  ReviewItem,
  ReviewItemType,
  ITEM_TYPE_CONFIG,
  ITEM_STATUS_CONFIG,
} from "./review-types";
import styles from "./QueueList.module.css";

interface QueueListProps {
  /** Review items to display */
  items: Array<ReviewItem & { id: string; type: ReviewItemType; status: string; summary: string }>;
  /** Currently selected item ID */
  selectedId?: string;
  /** Callback when item is selected */
  onSelect: (item: ReviewItem & { id: string; type: ReviewItemType; status: string; summary: string }) => void;
  /** Item type config for display */
  itemTypeConfig: typeof ITEM_TYPE_CONFIG;
  /** Item status config for display */
  itemStatusConfig: typeof ITEM_STATUS_CONFIG;
}

export function QueueList({
  items,
  selectedId,
  onSelect,
  itemTypeConfig,
  itemStatusConfig,
}: QueueListProps) {
  // Sort by confidence (lowest first)
  const sortedItems = useMemo(
    () => [...items].sort((a, b) => a.confidence - b.confidence),
    [items]
  );

  if (items.length === 0) {
    return (
      <div className={styles.emptyState}>
        <p>No items in the review queue.</p>
      </div>
    );
  }

  return (
    <ul className={styles.list} role="listbox" aria-label="Review queue">
      {sortedItems.map((item) => {
        const typeConfig = itemTypeConfig[item.type];
        const statusConfig = itemStatusConfig[item.status];
        const isSelected = selectedId === item.id;

        return (
          <li
            key={item.id}
            className={`${styles.item} ${isSelected ? styles.itemSelected : ""}`}
            role="option"
            aria-selected={isSelected}
            onClick={() => onSelect(item)}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                onSelect(item);
              }
            }}
            tabIndex={0}
          >
            <div className={styles.itemHeader}>
              <span
                className={styles.typeBadge}
                style={{ color: typeConfig.color }}
              >
                {typeConfig.label}
              </span>
              <span className={styles.confidence}>
                {(item.confidence * 100).toFixed(0)}%
              </span>
            </div>

            <h3 className={styles.itemTitle}>{item.title}</h3>
            <p className={styles.itemSummary}>{item.summary}</p>

            <div className={styles.itemMeta}>
              <span className={styles.sourceCount}>
                {item.source_count} sources
              </span>
              {item.agent_name && (
                <span className={styles.agentName}>{item.agent_name}</span>
              )}
            </div>
          </li>
        );
      })}
    </ul>
  );
}
