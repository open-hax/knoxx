/**
 * Review Queue Page
 *
 * Process pending items with correction capture.
 */

import { useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { QueueList } from "../components/review/QueueList";
import { EmptyState } from "../components/EmptyState";
import {
  type ReviewItem,
  type BatchAction,
  ITEM_TYPE_CONFIG,
  ITEM_STATUS_CONFIG,
  MOCK_REVIEW_ITEMS,
} from "../components/review/review-types";
import styles from "./ReviewQueuePage.module.css";

export function ReviewQueuePage() {
  const navigate = useNavigate();
  const [items, setItems] = useState<ReviewItem[]>(MOCK_REVIEW_ITEMS);
  const [selectedItem, setSelectedItem] = useState<ReviewItem | null>(null);
  const [batchAction, setBatchAction] = useState<BatchAction | null>(null);

  const handleSelect = useCallback((item: ReviewItem) => {
    setSelectedItem(item);
  }, []);

  const handleBatchAction = useCallback((action: BatchAction) => {
    setBatchAction(action);

    if (action === "approve-all") {
      setItems((prev) =>
        prev.map((item) =>
          item.status === "pending" ? { ...item, status: "approved" } : item
        )
      );
    } else if (action === "reject-all") {
      setItems((prev) =>
        prev.map((item) =>
          item.status === "pending" ? { ...item, status: "rejected" } : item
        )
      );
    } else if (action === "flag-for-review") {
      setItems((prev) =>
        prev.map((item) =>
          item.status === "pending" ? { ...item, status: "flagged" } : item
        )
      );
    }

    // Clear selection after batch action
    setSelectedItem(null);
    setBatchAction(null);
  }, []);

  const pendingCount = items.filter((i) => i.status === "pending").length;

  // Show empty state if no items
  if (items.length === 0) {
    return (
      <div className={styles.page}>
        <EmptyState
          title="Review queue is empty"
          message="All items have been processed. Great work!"
          primaryAction={{
            label: "View Dashboard",
            onClick: () => navigate("/workbench/dashboard"),
          }}
        />
      </div>
    );
  }

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <div className={styles.titleRow}>
          <h1 className={styles.title}>Review Queue</h1>
          <span className={styles.count}>{pendingCount} pending</span>
        </div>

        <div className={styles.actions}>
          <select
            className={styles.batchSelect}
            value={batchAction || ""}
            onChange={(e) => {
              const value = e.target.value as BatchAction;
              if (value) {
                handleBatchAction(value);
              }
            }}
            disabled={pendingCount === 0}
          >
            <option value="">Batch actions...</option>
            <option value="approve-all">Approve all pending</option>
            <option value="reject-all">Reject all pending</option>
            <option value="flag-for-review">Flag for review</option>
          </select>
        </div>
      </header>

      <div className={styles.content}>
        <div className={styles.listPanel}>
          <QueueList
            items={items}
            selectedId={selectedItem?.id}
            onSelect={handleSelect}
            itemTypeConfig={ITEM_TYPE_CONFIG}
            itemStatusConfig={ITEM_STATUS_CONFIG}
          />
        </div>

        <div className={styles.detailPanel}>
          {selectedItem ? (
            <div className={styles.detail}>
              <h2 className={styles.detailTitle}>{selectedItem.title}</h2>
              <p className={styles.detailSummary}>{selectedItem.summary}</p>

              <div className={styles.detailMeta}>
                <div className={styles.metaItem}>
                  <span className={styles.metaLabel}>Type</span>
                  <span
                    className={styles.metaValue}
                    style={{ color: ITEM_TYPE_CONFIG[selectedItem.type].color }}
                  >
                    {ITEM_TYPE_CONFIG[selectedItem.type].label}
                  </span>
                </div>
                <div className={styles.metaItem}>
                  <span className={styles.metaLabel}>Confidence</span>
                  <span className={styles.metaValue}>
                    {(selectedItem.confidence * 100).toFixed(0)}%
                  </span>
                </div>
                <div className={styles.metaItem}>
                  <span className={styles.metaLabel}>Sources</span>
                  <span className={styles.metaValue}>{selectedItem.source_count}</span>
                </div>
                {selectedItem.agent_name && (
                  <div className={styles.metaItem}>
                    <span className={styles.metaLabel}>Agent</span>
                    <span className={styles.metaValue}>{selectedItem.agent_name}</span>
                  </div>
                )}
              </div>

              <div className={styles.detailActions}>
                <button
                  className={styles.approveButton}
                  onClick={() => {
                    setItems((prev) =>
                      prev.map((i) =>
                        i.id === selectedItem.id ? { ...i, status: "approved" } : i
                      )
                    );
                    setSelectedItem(null);
                  }}
                >
                  Approve
                </button>
                <button
                  className={styles.rejectButton}
                  onClick={() => {
                    setItems((prev) =>
                      prev.map((i) =>
                        i.id === selectedItem.id ? { ...i, status: "rejected" } : i
                      )
                    );
                    setSelectedItem(null);
                  }}
                >
                  Reject
                </button>
                <button
                  className={styles.flagButton}
                  onClick={() => {
                    setItems((prev) =>
                      prev.map((i) =>
                        i.id === selectedItem.id ? { ...i, status: "flagged" } : i
                      )
                    );
                    setSelectedItem(null);
                  }}
                >
                  Flag
                </button>
              </div>
            </div>
          ) : (
            <EmptyState
              title="Select an item"
              message="Choose an item from the queue to review its details."
              icon="📋"
            />
          )}
        </div>
      </div>
    </div>
  );
}
