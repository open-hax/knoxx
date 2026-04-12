import { Card, Badge } from "@open-hax/uxx";
import EmptyState from "../components/EmptyState";
import { EventTable } from "../components/ops/EventTable";
import { AttentionCard } from "../components/dashboard/AttentionCard";
import { ContentEditorPage } from "./ContentEditorPage";
import { ReviewQueuePage } from "./ReviewQueuePage";
import { useState, useEffect } from "react";
import type { OpsEvent } from "../components/ops/ops-types";
import type { AttentionMetric } from "../components/dashboard/dashboard-types";
import { ATTENTION_CONFIG } from "../components/dashboard/dashboard-types";

type WorkbenchView = "dashboard" | "content" | "review" | "memory" | "agents" | "ops";

interface WorkbenchPageProps {
  view: WorkbenchView;
}

const VIEW_CONFIG: Record<WorkbenchView, { title: string; description: string; status: "partial" | "planned" | "active"; icon: string }> = {
  dashboard: {
    title: "Dashboard",
    description: "Attention items, agent runs, and memory activity at a glance.",
    status: "active",
    icon: "📊",
  },
  content: {
    title: "Content Editor",
    description: "Author and publish structured documents with AI assistance.",
    status: "active",
    icon: "📝",
  },
  review: {
    title: "Review Queue",
    description: "Process pending items with correction capture that writes to memory.",
    status: "active",
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

const MOCK_ATTENTION_METRICS: AttentionMetric[] = [
  { ...ATTENTION_CONFIG.review, count: 12 },
  { ...ATTENTION_CONFIG.approval, count: 3 },
  { ...ATTENTION_CONFIG.policy, count: 2 },
];

export default function WorkbenchPage({ view }: WorkbenchPageProps) {
  const config = VIEW_CONFIG[view];
  const [opsEvents, setOpsEvents] = useState<OpsEvent[]>([]);
  const [attentionMetrics, setAttentionMetrics] = useState<AttentionMetric[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // Load data based on view
  useEffect(() => {
    setIsLoading(true);

    const timer = setTimeout(() => {
      if (view === "ops") {
        setOpsEvents(MOCK_EVENTS);
      } else if (view === "dashboard") {
        setAttentionMetrics(MOCK_ATTENTION_METRICS);
      }
      setIsLoading(false);
    }, 200);

    return () => clearTimeout(timer);
  }, [view]);

  // Ops Log view - render EventTable directly
  if (view === "ops") {
    return (
      <div className="workbench-page workbench-page--full-height">
        <EventTable events={opsEvents} />
      </div>
    );
  }

  // Content Editor view - render ContentEditorPage
  if (view === "content") {
    return (
      <div className="workbench-page workbench-page--full-height">
        <ContentEditorPage />
      </div>
    );
  }

  // Review Queue view - render ReviewQueuePage
  if (view === "review") {
    return (
      <div className="workbench-page workbench-page--full-height">
        <ReviewQueuePage />
      </div>
    );
  }

  // Dashboard view - render attention cards
  if (view === "dashboard") {
    const totalAttentionItems = attentionMetrics.reduce((sum, m) => sum + m.count, 0);

    return (
      <div className="workbench-page workbench-page--dashboard">
        <header className="workbench-page__header">
          <h1 className="workbench-page__title">{config.title}</h1>
          <Badge variant={STATUS_VARIANTS[config.status]}>
            {config.status === "active" ? "Active" : "In Progress"}
          </Badge>
        </header>

        <section className="workbench-page__section">
          <h2 className="workbench-page__section-title">Attention Items</h2>

          {isLoading ? (
            <div className="workbench-page__loading">
              <span>Loading...</span>
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
            <div className="workbench-page__card-grid">
              {attentionMetrics.map((metric) => (
                <AttentionCard key={metric.type} metric={metric} />
              ))}
            </div>
          )}
        </section>

        {/* Placeholder sections for future epics */}
        <section className="workbench-page__section">
          <h2 className="workbench-page__section-title">Recent Agent Runs</h2>
          <EmptyState
            title="Coming Soon"
            message="Agent run summaries will appear here once Epic 1.2 is implemented."
            icon="🤖"
          />
        </section>

        <section className="workbench-page__section">
          <h2 className="workbench-page__section-title">Memory Activity</h2>
          <EmptyState
            title="Coming Soon"
            message="Recent memory signals will appear here once Epic 1.3 is implemented."
            icon="🧠"
          />
        </section>
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
