import { Card, Badge } from "@open-hax/uxx";
import EmptyState from "../components/EmptyState";

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
    status: "partial",
    icon: "⚙️",
  },
};

const STATUS_VARIANTS: Record<string, "default" | "warning" | "success"> = {
  planned: "default",
  partial: "warning",
  active: "success",
};

export default function WorkbenchPage({ view }: WorkbenchPageProps) {
  const config = VIEW_CONFIG[view];

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
