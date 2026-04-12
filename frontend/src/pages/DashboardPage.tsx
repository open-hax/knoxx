/**
 * Dashboard Page
 *
 * Landing page showing attention items, agent runs, memory activity.
 */

import { useState, useEffect } from "react";
import { Shell } from "../shell/Shell";
import { AttentionCard } from "../components/dashboard/AttentionCard";
import { EmptyState } from "../components/EmptyState";
import type { AttentionMetric, AttentionType } from "../components/dashboard/dashboard-types";
import { ATTENTION_CONFIG } from "../components/dashboard/dashboard-types";
import styles from "./DashboardPage.module.css";

// Mock data for development
const MOCK_METRICS: AttentionMetric[] = [
  { ...ATTENTION_CONFIG.review, count: 12 },
  { ...ATTENTION_CONFIG.approval, count: 3 },
  { ...ATTENTION_CONFIG.policy, count: 2 },
];

export function DashboardPage() {
  const [metrics, setMetrics] = useState<AttentionMetric[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // TODO: Fetch from API
    // For now, use mock data
    const timer = setTimeout(() => {
      setMetrics(MOCK_METRICS);
      setIsLoading(false);
    }, 300);

    return () => clearTimeout(timer);
  }, []);

  const totalAttentionItems = metrics.reduce((sum, m) => sum + m.count, 0);

  return (
    <Shell>
      <div className={styles.page}>
        <header className={styles.header}>
          <h1 className={styles.title}>Dashboard</h1>
          <p className={styles.description}>
            Overview of attention items, agent runs, and memory activity.
          </p>
        </header>

        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Attention Items</h2>

          {isLoading ? (
            <div className={styles.loading}>
              <span className={styles.loadingSpinner}>Loading...</span>
            </div>
          ) : totalAttentionItems === 0 ? (
            <EmptyState
              title="All caught up!"
              message="No items need your attention right now. Great work!"
              actionLabel="View Ops Log"
              onAction={() => {
                window.location.href = "/workbench/ops";
              }}
            />
          ) : (
            <div className={styles.cardGrid}>
              {metrics.map((metric) => (
                <AttentionCard key={metric.type} metric={metric} />
              ))}
            </div>
          )}
        </section>

        {/* Placeholder sections for future epics */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Recent Agent Runs</h2>
          <EmptyState
            title="Coming Soon"
            message="Agent run summaries will appear here once Epic 1.2 is implemented."
            icon="🤖"
          />
        </section>

        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Memory Activity</h2>
          <EmptyState
            title="Coming Soon"
            message="Recent memory signals will appear here once Epic 1.3 is implemented."
            icon="🧠"
          />
        </section>
      </div>
    </Shell>
  );
}

export default DashboardPage;
