/**
 * Ops Log Page
 *
 * Inspect ingestion, sync state, embeddings, policy violations, errors.
 * Read-only audit surface.
 */

import { useState, useEffect } from "react";
import { Shell } from "../shell/Shell";
import { EventTable } from "../components/ops/EventTable";
import { EventDetail } from "../components/ops/EventDetail";
import {
  GardensTab,
  DependencyGardenPlaceholder,
  TruthGardenPlaceholder,
  type GardenTab,
} from "../components/ops/GardensTab";
import type { OpsEvent } from "../components/ops/ops-types";
import { EmptyState } from "../components/EmptyState";
import styles from "./OpsLogPage.module.css";

// Mock data for development
const MOCK_EVENTS: OpsEvent[] = [
  {
    id: "evt-1",
    time: new Date(Date.now() - 5 * 60 * 1000),
    type: "ingestion",
    status: "done",
    summary: "devel-docs: 14 files, 2.1MB",
    duration: 12340,
  },
  {
    id: "evt-2",
    time: new Date(Date.now() - 9 * 60 * 1000),
    type: "embedding",
    status: "done",
    summary: "14 chunks added",
    duration: 3420,
  },
  {
    id: "evt-3",
    time: new Date(Date.now() - 16 * 60 * 1000),
    type: "sync",
    status: "done",
    summary: "OpenPlanner → GraphWeaver",
    duration: 890,
  },
  {
    id: "evt-4",
    time: new Date(Date.now() - 35 * 60 * 1000),
    type: "policy",
    status: "warn",
    summary: "3 flagged segments (PII)",
    duration: 120,
  },
  {
    id: "evt-5",
    time: new Date(Date.now() - 52 * 60 * 1000),
    type: "MT",
    status: "error",
    summary: "batch7: timeout after 300s",
    error: "TimeoutError: Request exceeded 300000ms limit",
    duration: 300000,
  },
  {
    id: "evt-6",
    time: new Date(Date.now() - 2 * 60 * 60 * 1000),
    type: "ingestion",
    status: "done",
    summary: "devel-code: 89 files, 4.2MB",
    duration: 45120,
  },
];

export function OpsLogPage() {
  const [events, setEvents] = useState<OpsEvent[]>([]);
  const [selectedEvent, setSelectedEvent] = useState<OpsEvent | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<GardenTab>("events");

  useEffect(() => {
    // TODO: Fetch from API
    // For now, use mock data
    const timer = setTimeout(() => {
      setEvents(MOCK_EVENTS);
      setIsLoading(false);
    }, 500);

    return () => clearTimeout(timer);
  }, []);

  const handleSelectEvent = (event: OpsEvent) => {
    setSelectedEvent(event);
  };

  const handleCloseDetail = () => {
    setSelectedEvent(null);
  };

  const renderTabContent = () => {
    switch (activeTab) {
      case "dependency":
        return <DependencyGardenPlaceholder />;
      case "truth":
        return <TruthGardenPlaceholder />;
      case "events":
      default:
        return isLoading ? (
          <div className={styles.loading}>
            <span className={styles.loadingSpinner}>Loading...</span>
          </div>
        ) : events.length === 0 ? (
          <EmptyState
            title="No ops events"
            message="Events from ingestion, sync, embedding, and policy checks will appear here."
            actionLabel="Run ingestion"
            onAction={() => {
              // TODO: Navigate to ingestion
            }}
          />
        ) : (
          <EventTable
            events={events}
            onSelectEvent={handleSelectEvent}
            selectedEventId={selectedEvent?.id}
          />
        );
    }
  };

  return (
    <Shell>
      <div className={styles.page}>
        <header className={styles.header}>
          <h1 className={styles.title}>Ops</h1>
        </header>

        <GardensTab activeTab={activeTab} onTabChange={setActiveTab}>
          {renderTabContent()}
        </GardensTab>

        {/* Event Detail Slide-out */}
        {selectedEvent && activeTab === "events" && (
          <div className={styles.detailOverlay} onClick={handleCloseDetail}>
            <div className={styles.detailPanel} onClick={(e) => e.stopPropagation()}>
              <EventDetail event={selectedEvent} onClose={handleCloseDetail} />
            </div>
          </div>
        )}
      </div>
    </Shell>
  );
}

export default OpsLogPage;
