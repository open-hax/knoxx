/**
 * Attention Card
 *
 * Displays a single attention metric with count, label, and CTA.
 */

import { useNavigate } from "react-router-dom";
import type { AttentionMetric } from "./dashboard-types";
import { ATTENTION_COLORS } from "./dashboard-types";
import styles from "./AttentionCard.module.css";

interface AttentionCardProps {
  metric: AttentionMetric;
}

export function AttentionCard({ metric }: AttentionCardProps) {
  const navigate = useNavigate();
  const accentColor = ATTENTION_COLORS[metric.type];

  const handleCtaClick = () => {
    navigate(metric.ctaPath);
  };

  return (
    <article className={styles.card}>
      <header className={styles.header}>
        <h3 className={styles.label}>{metric.label}</h3>
        <span className={styles.count} style={{ color: accentColor }}>
          {metric.count}
        </span>
      </header>

      <p className={styles.description}>{metric.description}</p>

      <button
        className={styles.cta}
        onClick={handleCtaClick}
        disabled={metric.count === 0}
        style={{
          backgroundColor: metric.count > 0 ? accentColor : undefined,
        }}
      >
        {metric.ctaLabel}
      </button>
    </article>
  );
}

export default AttentionCard;
