/**
 * Gardens Tab Component
 *
 * Tab navigation for Events, Dependency Garden, and Truth Garden.
 * Epic 6.3: Ops Gardens Sub-Tabs
 */

import { useState, useEffect } from "react";
import type { ReactNode } from "react";
import styles from "./GardensTab.module.css";

export type GardenTab = "events" | "dependency" | "truth";

interface GardensTabProps {
  activeTab: GardenTab;
  onTabChange: (tab: GardenTab) => void;
  children: ReactNode;
}

const TAB_CONFIG: Record<GardenTab, { label: string; shortcut: string }> = {
  events: { label: "Events", shortcut: "e" },
  dependency: { label: "Dependency Garden", shortcut: "d" },
  truth: { label: "Truth Garden", shortcut: "t" },
};

export function GardensTab({ activeTab, onTabChange, children }: GardensTabProps) {
  // Persist tab state to localStorage
  useEffect(() => {
    localStorage.setItem("ops-active-tab", activeTab);
  }, [activeTab]);

  // Restore tab state on mount
  useEffect(() => {
    const saved = localStorage.getItem("ops-active-tab") as GardenTab | null;
    if (saved && ["events", "dependency", "truth"].includes(saved)) {
      onTabChange(saved);
    }
  }, []);

  const tabs = Object.entries(TAB_CONFIG) as [GardenTab, typeof TAB_CONFIG[GardenTab]][];

  return (
    <div className={styles.container}>
      <div className={styles.tabBar} role="tablist">
        {tabs.map(([tab, config]) => (
          <button
            key={tab}
            role="tab"
            aria-selected={activeTab === tab}
            className={`${styles.tab} ${activeTab === tab ? styles.tabActive : ""}`}
            onClick={() => onTabChange(tab)}
            data-shortcut={config.shortcut}
          >
            {config.label}
          </button>
        ))}
      </div>
      <div className={styles.content} role="tabpanel">
        {children}
      </div>
    </div>
  );
}

/**
 * Dependency Garden Placeholder
 *
 * Shows workspace dependency topology.
 * Full implementation exists as separate service.
 */
export function DependencyGardenPlaceholder() {
  return (
    <div className={styles.gardenPlaceholder}>
      <h2 className={styles.gardenTitle}>Dependency Garden</h2>
      <p className={styles.gardenDescription}>
        Workspace dependency topology visualization.
      </p>
      <div className={styles.gardenHint}>
        <p>Features:</p>
        <ul>
          <li>Filter by package name</li>
          <li>See dependency chain</li>
          <li>Identify circular dependencies</li>
        </ul>
        <p className={styles.gardenNote}>
          Full visualization available at{" "}
          <code>/services/devel-deps-garden/</code>
        </p>
      </div>
    </div>
  );
}

/**
 * Truth Garden Placeholder
 *
 * Shows control-plane state and truth values.
 * Full implementation exists as separate service.
 */
export function TruthGardenPlaceholder() {
  return (
    <div className={styles.gardenPlaceholder}>
      <h2 className={styles.gardenTitle}>Truth Garden</h2>
      <p className={styles.gardenDescription}>
        Control-plane state and truth values.
      </p>
      <div className={styles.gardenHint}>
        <p>Features:</p>
        <ul>
          <li>Current system state</li>
          <li>Recent state changes</li>
          <li>Audit trail</li>
        </ul>
        <p className={styles.gardenNote}>
          Full visualization available at{" "}
          <code>/services/eta-mu-truth-workbench/</code>
        </p>
      </div>
    </div>
  );
}
