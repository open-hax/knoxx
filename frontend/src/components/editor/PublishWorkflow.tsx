/**
 * Publish Workflow Component
 *
 * Draft → Review → Published workflow with explicit steps.
 * Epic 2.4: Content Editor Staged Publish Flow
 */

import { useState } from "react";
import type { DocumentStatus, DocumentVisibility } from "./editor-types";
import styles from "./PublishWorkflow.module.css";

interface PublishWorkflowProps {
  status: DocumentStatus;
  visibility: DocumentVisibility;
  canPublish: boolean;
  onPublish: () => void;
  onSetReview: () => void;
}

type Step = "draft" | "review" | "published";

const STEPS: Step[] = ["draft", "review", "published"];

const STEP_CONFIG: Record<Step, { label: string; description: string }> = {
  draft: {
    label: "Draft",
    description: "Work in progress. Not visible to others.",
  },
  review: {
    label: "Review",
    description: "Ready for review. Visible to reviewers.",
  },
  published: {
    label: "Published",
    description: "Publicly visible. Appears in collections.",
  },
};

export function PublishWorkflow({
  status,
  visibility,
  canPublish,
  onPublish,
  onSetReview,
}: PublishWorkflowProps) {
  const [showConfirm, setShowConfirm] = useState(false);

  const currentIndex = STEPS.indexOf(status);
  const currentStep = status;

  const handleAction = () => {
    if (currentStep === "draft") {
      onSetReview();
    } else if (currentStep === "review") {
      setShowConfirm(true);
    }
  };

  const handleConfirmPublish = () => {
    setShowConfirm(false);
    onPublish();
  };

  const getActionLabel = () => {
    if (currentStep === "draft") return "Submit for Review";
    if (currentStep === "review") return "Publish";
    return "";
  };

  const actionDisabled =
    currentStep === "published" || (currentStep === "review" && !canPublish);

  return (
    <div className={styles.workflow}>
      <div className={styles.steps}>
        {STEPS.map((step, index) => {
          const config = STEP_CONFIG[step];
          const isActive = index === currentIndex;
          const isPast = index < currentIndex;

          return (
            <div
              key={step}
              className={`${styles.step} ${isActive ? styles.stepActive : ""} ${
                isPast ? styles.stepPast : ""
              }`}
            >
              <div className={styles.stepIndicator}>
                {isPast ? "✓" : index + 1}
              </div>
              <div className={styles.stepInfo}>
                <span className={styles.stepLabel}>{config.label}</span>
                <span className={styles.stepDescription}>{config.description}</span>
              </div>
            </div>
          );
        })}
      </div>

      <div className={styles.actions}>
        {currentStep !== "published" && (
          <button
            className={styles.actionButton}
            onClick={handleAction}
            disabled={actionDisabled}
          >
            {getActionLabel()}
          </button>
        )}

        {currentStep === "published" && (
          <span className={styles.publishedStatus}>✓ Published and visible</span>
        )}
      </div>

      {showConfirm && (
        <div className={styles.confirmOverlay}>
          <div className={styles.confirmDialog}>
            <h3 className={styles.confirmTitle}>Confirm Publication</h3>
            <p className={styles.confirmMessage}>
              This document will become publicly visible and appear in the
              collection. Are you sure?
            </p>
            <div className={styles.confirmActions}>
              <button
                className={styles.confirmCancel}
                onClick={() => setShowConfirm(false)}
              >
                Cancel
              </button>
              <button
                className={styles.confirmPublish}
                onClick={handleConfirmPublish}
              >
                Publish
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
