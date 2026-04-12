import { Card, Badge } from "@open-hax/uxx";
import EmptyState from "../components/EmptyState";
import { EventTable } from "../components/ops/EventTable";
import { useState, useEffect } from "react";
import type { OpsEvent } from "../components/ops/ops-types";

type WorkbenchView = "dashboard" | "content" | "review" | "memory" | "agents" | "ops";

interface WorkbenchPageProps {
  view: WorkbenchView;
}

const VIEW_CONFIG: Record<WorkbenchView, { title: string; description: string; status: "partial" | "planned" | "active"; icon: string }> = {
  dashboard: {
    title: "Dashboard",
    description: "Attention items, agent runs, and memory activity at a glance.",
    status: "planned",
    icon: "📊",
  },
  content: {
    title: "Content Editor",
    description: "Author and publish structured documents with AI assistance.",
    status: "partial",
    icon: "📝",
  },
  review: {
    title: "Review Queue",
    description: "Process pending items with correction capture that writes to memory.",
    status: "partial",
    icon: "✅",
  },
  memory: {
    title: "Memory Inspector",
    description: "Search-first graph exploration with focal expansion.",
    status: "planned",
    icon: "🧠",
  },
  agents: {
    title: "Agent Workspace",
    description: "Compose tasks, monitor runs, approve outputs, use scratchpad.",
    status: "planned",
    icon: "🤖",
  },
  ops: {
    title: "Ops Log",
    description: "Inspect ingestion, sync, embeddings, policy violations.",
    status: "active",
    icon: "⚙️",
  },
};

const STATUS_VARIANTS: Record<string, "default" | "warning" | "success"> = {
  planned: "default",
  partial: "warning",
  active: "success",
};

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

export default function WorkbenchPage({ view }: WorkbenchPageProps) {
  const config = VIEW_CONFIG[view];
  const [opsEvents, setOpsEvents] = useState<OpsEvent[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // Load ops events when view is ops
  useEffect(() => {
    if (view === "ops") {
      setIsLoading(true);
      // TODO: Fetch from API
      // For now, use mock data
      const timer = setTimeout(() => {
        setOpsEvents(MOCK_EVENTS);
        setIsLoading(false);
      }, 300);
      return () => clearTimeout(timer);
    }
  }, [view]);

  // Ops Log view - render EventTable directly
  if (view === "ops") {
    return (
      <div className="workbench-page workbench-page--full-height">
        <EventTable events={opsEvents} />
      </div>
    );
  }

  // Other views - render placeholder
  return (
    <div className="workbench-page">
      <header className="workbench-page__header">
        <h1 className="workbench-page__title">{config.title}</h1>
        <Badge variant={STATUS_VARIANTS[config.status]}>
          {config.status === "planned" ? "Planned" : config.status === "partial" ? "In Progress" : "Active"}
        </Badge>
      </header>

      <Card>
        <div className="workbench-page__content">
          <p className="workbench-page__description">{config.description}</p>
          
          <EmptyState
            title={config.status === "planned" ? "Coming Soon" : "Under Construction"}
            message={
              config.status === "planned"
                ? "This view is planned for a future release. Check the specs in packages/knoxx/specs/workbench/ for details."
                : "This view is partially implemented. Some features may be available."
            }
            icon={config.icon}
          />
        </div>
      </Card>
    </div>
  );
}
