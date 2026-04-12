/**
 * Ops Event Detail
 *
 * Expandable row with full trace.
 */

import type { OpsEvent } from "./ops-types";
import { STATUS_ICONS, STATUS_COLORS, TYPE_LABELS } from "./ops-types";
import styles from "./EventDetail.module.css";

interface EventDetailProps {
  event: OpsEvent;
  onClose?: () => void;
}

export function EventDetail({ event, onClose }: EventDetailProps) {
  const formatDuration = (ms?: number) => {
    if (!ms) return "—";
    if (ms < 1000) return `${ms}ms`;
    if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
    return `${(ms / 60000).toFixed(1)}m`;
  };

  const formatTime = (date: Date) => {
    const d = new Date(date);
    return d.toLocaleString("en-US", {
      weekday: "short",
      month: "short",
      day: "numeric",
      hour: "numeric",
      minute: "2-digit",
      second: "2-digit",
      hour12: true,
    });
  };

  return (
    <div className={styles.container}>
      <header className={styles.header}>
        <div className={styles.headerTop}>
          <span
            className={styles.statusIcon}
            style={{ color: STATUS_COLORS[event.status] }}
          >
            {STATUS_ICONS[event.status]}
          </span>
          <span className={styles.type}>{TYPE_LABELS[event.type]}</span>
        </div>
        <button className={styles.closeButton} onClick={onClose} title="Close">
          ✕
        </button>
      </header>

      <div className={styles.body}>
        <div className={styles.meta}>
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>Time</span>
            <span className={styles.metaValue}>{formatTime(event.time)}</span>
          </div>
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>Duration</span>
            <span className={styles.metaValue}>{formatDuration(event.duration)}</span>
          </div>
          <div className={styles.metaItem}>
            <span className={styles.metaLabel}>Status</span>
            <span className={styles.metaValue}>
              <span style={{ color: STATUS_COLORS[event.status] }}>
                {event.status}
              </span>
            </span>
          </div>
        </div>

        <div className={styles.summary}>
          <h3 className={styles.summaryTitle}>Summary</h3>
          <p className={styles.summaryText}>{event.summary}</p>
        </div>

        {event.inputs && Object.keys(event.inputs).length > 0 && (
          <div className={styles.section}>
            <h4 className={styles.sectionTitle}>Inputs</h4>
            <pre className={styles.code}>
              {JSON.stringify(event.inputs, null, 2)}
            </pre>
          </div>
        )}

        {event.outputs && Object.keys(event.outputs).length > 0 && (
          <div className={styles.section}>
            <h4 className={styles.sectionTitle}>Outputs</h4>
            <pre className={styles.code}>
              {JSON.stringify(event.outputs, null, 2)}
            </pre>
          </div>
        )}

        {event.error && (
          <div className={styles.section}>
            <h4 className={styles.sectionTitle}>Error Trace</h4>
            <pre className={styles.errorTrace}>{event.error}</pre>
          </div>
        )}

        {event.relatedReviewItemId && (
          <div className={styles.section}>
            <a
              href={`/workbench/review/${event.relatedReviewItemId}`}
              className={styles.reviewLink}
            >
              View related review item →
            </a>
          </div>
        )}
      </div>
    </div>
  );
}

export default EventDetail;
