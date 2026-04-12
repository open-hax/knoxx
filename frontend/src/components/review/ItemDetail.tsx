/**
 * Review Item Detail Component
 *
 * Shows full output, confidence, label dimensions, and action buttons.
 * Epic 3.2: Review Item Detail
 */

import { useState } from "react";
import { LabelForm, type LabelValues } from "./LabelForm";
import type {
  ReviewItem,
  ReviewItemType,
  ITEM_TYPE_CONFIG,
} from "./review-types";
import styles from "./ItemDetail.module.css";

interface ItemDetailProps {
  item: ReviewItem & { id: string; type: ReviewItemType; status: string; summary: string };
  itemTypeConfig: typeof ITEM_TYPE_CONFIG;
  onApprove: () => void;
  onReject: () => void;
  onFlag: () => void;
  onNeedsEdit?: (labels: LabelValues) => void;
  onSkip?: () => void;
}

/** Label dimensions by item type */
const LABEL_DIMENSIONS: Record<ReviewItemType, string[]> = {
  synthesis: ["accuracy", "completeness", "clarity", "relevance"],
  MT: ["fluency", "adequacy", "terminology", "style"],
  ingestion: ["quality", "deduplication", "formatting", "metadata"],
};

export function ItemDetail({
  item,
  itemTypeConfig,
  onApprove,
  onReject,
  onFlag,
  onNeedsEdit,
  onSkip,
}: ItemDetailProps) {
  const [showLabelForm, setShowLabelForm] = useState(false);
  const [labels, setLabels] = useState<LabelValues>({});

  const handleNeedsEdit = () => {
    if (showLabelForm && onNeedsEdit) {
      onNeedsEdit(labels);
      setShowLabelForm(false);
    } else {
      setShowLabelForm(true);
    }
  };

  const typeConfig = itemTypeConfig[item.type];
  const labelDimensions = LABEL_DIMENSIONS[item.type];
  const isPending = item.status === "pending";

  return (
    <div className={styles.detail}>
      <header className={styles.header}>
        <h2 className={styles.title}>{item.title}</h2>
        <span
          className={styles.typeBadge}
          style={{ color: typeConfig.color }}
        >
          {typeConfig.label}
        </span>
      </header>

      <div className={styles.content}>
        <section className={styles.outputSection}>
          <h3 className={styles.sectionTitle}>Output</h3>
          <div className={styles.outputContent}>
            {item.content_preview || item.summary}
          </div>
        </section>

        <section className={styles.metaSection}>
          <h3 className={styles.sectionTitle}>Metadata</h3>
          <dl className={styles.metaGrid}>
            <div className={styles.metaRow}>
              <dt className={styles.metaLabel}>Confidence</dt>
              <dd className={styles.metaValue}>
                <span
                  className={styles.confidenceValue}
                  data-confidence={
                    item.confidence >= 0.8
                      ? "high"
                      : item.confidence >= 0.5
                        ? "medium"
                        : "low"
                  }
                >
                  {(item.confidence * 100).toFixed(0)}%
                </span>
              </dd>
            </div>
            <div className={styles.metaRow}>
              <dt className={styles.metaLabel}>Sources</dt>
              <dd className={styles.metaValue}>{item.source_count}</dd>
            </div>
            {item.agent_name && (
              <div className={styles.metaRow}>
                <dt className={styles.metaLabel}>Agent</dt>
                <dd className={styles.metaValue}>{item.agent_name}</dd>
              </div>
            )}
            <div className={styles.metaRow}>
              <dt className={styles.metaLabel}>AI Drafted</dt>
              <dd className={styles.metaValue}>
                {item.ai_drafted ? "Yes" : "No"}
              </dd>
            </div>
            <div className={styles.metaRow}>
              <dt className={styles.metaLabel}>Created</dt>
              <dd className={styles.metaValue}>
                {new Date(item.created_at).toLocaleDateString()}
              </dd>
            </div>
          </dl>
        </section>

        {showLabelForm && (
          <section className={styles.labelSection}>
            <h3 className={styles.sectionTitle}>Labels</h3>
            <LabelForm
              dimensions={labelDimensions}
              values={labels}
              onChange={setLabels}
            />
          </section>
        )}
      </div>

      <footer className={styles.actions}>
        <button
          className={styles.approveButton}
          onClick={onApprove}
          disabled={!isPending}
          title="Approve this item (SPC a)"
        >
          Approve
        </button>
        <button
          className={styles.needsEditButton}
          onClick={handleNeedsEdit}
          disabled={!isPending}
          title="Request edits with labels"
        >
          {showLabelForm ? "Submit Labels" : "Needs Edit"}
        </button>
        <button
          className={styles.rejectButton}
          onClick={onReject}
          disabled={!isPending}
          title="Reject this item (SPC r)"
        >
          Reject
        </button>
        {onSkip && (
          <button
            className={styles.skipButton}
            onClick={onSkip}
            title="Skip for now (SPC s)"
          >
            Skip
          </button>
        )}
        <button
          className={styles.flagButton}
          onClick={onFlag}
          title="Flag for further review"
        >
          Flag
        </button>
      </footer>
    </div>
  );
}
