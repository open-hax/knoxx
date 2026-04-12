/**
 * Review Queue Page
 *
 * Process pending items with correction capture.
 * Connected to /v1/reviews API.
 */

import { useState, useCallback, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { QueueList } from "../components/review/QueueList";
import { EmptyState } from "../components/EmptyState";
import { useReviewQueue, getItemStatus } from "../components/review/useReviewQueue";
import {
  type ReviewItem,
  type ReviewItemStatus,
  type BatchAction,
  type ReviewItemType,
  ITEM_TYPE_CONFIG,
  ITEM_STATUS_CONFIG,
  sourceToType,
} from "../components/review/review-types";
import styles from "./ReviewQueuePage.module.css";

/** Extended item with computed display fields */
interface DisplayItem extends ReviewItem {
  id: string;
  type: ReviewItemType;
  status: ReviewItemStatus;
  summary: string;
}

function toDisplayItem(item: ReviewItem): DisplayItem {
  return {
    ...item,
    id: item.doc_id,
    type: sourceToType(item.source, item.ai_drafted),
    status: getItemStatus(item),
    summary: item.content_preview,
  };
}

export function ReviewQueuePage() {
  const navigate = useNavigate();
  const { items: apiItems, stats, loading, error, approve, reject, flag, batchAction, refresh } = useReviewQueue();
  const [selectedItem, setSelectedItem] = useState<DisplayItem | null>(null);

  // Convert API items to display items
  const items = useMemo(() => apiItems.map(toDisplayItem), [apiItems]);

  const handleSelect = useCallback((item: DisplayItem) => {
    setSelectedItem(item);
  }, []);

  const handleBatchAction = useCallback(async (action: BatchAction) => {
    const pendingIds = items
      .filter((i) => i.status === "pending")
      .map((i) => i.doc_id);

    if (pendingIds.length === 0) return;

    try {
      if (action === "approve-all") {
        await batchAction("approve", pendingIds);
      } else if (action === "reject-all") {
        await batchAction("reject", pendingIds);
      } else if (action === "flag-for-review") {
        await batchAction("flag", pendingIds);
      }
      setSelectedItem(null);
    } catch (err) {
      console.error("Batch action failed:", err);
    }
  }, [items, batchAction]);

  const handleApprove = useCallback(async () => {
    if (!selectedItem) return;
    try {
      await approve(selectedItem.doc_id);
      setSelectedItem(null);
    } catch (err) {
      console.error("Approve failed:", err);
    }
  }, [selectedItem, approve]);

  const handleReject = useCallback(async () => {
    if (!selectedItem) return;
    try {
      await reject(selectedItem.doc_id);
      setSelectedItem(null);
    } catch (err) {
      console.error("Reject failed:", err);
    }
  }, [selectedItem, reject]);

  const handleFlag = useCallback(async () => {
    if (!selectedItem) return;
    try {
      await flag(selectedItem.doc_id);
      setSelectedItem(null);
    } catch (err) {
      console.error("Flag failed:", err);
    }
  }, [selectedItem, flag]);

  const pendingCount = items.filter((i) => i.status === "pending").length;

  // Loading state
  if (loading && items.length === 0) {
    return (
      <div className={styles.page}>
        <div className={styles.loading}>
          <p>Loading review queue...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className={styles.page}>
        <div className={styles.error}>
          <p>Failed to load review queue: {error}</p>
          <button onClick={refresh} className={styles.retryButton}>
            Retry
          </button>
        </div>
      </div>
    );
  }

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
          {stats && (
            <span className={styles.stats}>
              {stats.approved_today} approved today
            </span>
          )}
        </div>

        <div className={styles.actions}>
          <select
            className={styles.batchSelect}
            onChange={(e) => {
              const value = e.target.value as BatchAction;
              if (value) {
                handleBatchAction(value);
                e.target.value = "";
              }
            }}
            disabled={pendingCount === 0}
            defaultValue=""
          >
            <option value="" disabled>
              Batch actions...
            </option>
            <option value="approve-all">Approve all pending ({pendingCount})</option>
            <option value="reject-all">Reject all pending ({pendingCount})</option>
            <option value="flag-for-review">Flag for review ({pendingCount})</option>
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
                <div className={styles.metaItem}>
                  <span className={styles.metaLabel}>AI Drafted</span>
                  <span className={styles.metaValue}>
                    {selectedItem.ai_drafted ? "Yes" : "No"}
                  </span>
                </div>
              </div>

              <div className={styles.detailActions}>
                <button
                  className={styles.approveButton}
                  onClick={handleApprove}
                  disabled={selectedItem.status !== "pending"}
                >
                  Approve
                </button>
                <button
                  className={styles.rejectButton}
                  onClick={handleReject}
                  disabled={selectedItem.status !== "pending"}
                >
                  Reject
                </button>
                <button
                  className={styles.flagButton}
                  onClick={handleFlag}
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
