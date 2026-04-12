import { Card, Badge } from "@open-hax/uxx";

type WorkbenchView = "dashboard" | "content" | "review" | "memory" | "agents" | "ops";

interface WorkbenchPageProps {
  view: WorkbenchView;
}

const VIEW_CONFIG: Record<WorkbenchView, { title: string; description: string; status: "partial" | "planned" | "active" }> = {
  dashboard: {
    title: "Dashboard",
    description: "Attention items, agent runs, and memory activity at a glance.",
    status: "planned",
  },
  content: {
    title: "Content Editor",
    description: "Author and publish structured documents with AI assistance.",
    status: "partial",
  },
  review: {
    title: "Review Queue",
    description: "Process pending items with correction capture that writes to memory.",
    status: "partial",
  },
  memory: {
    title: "Memory Inspector",
    description: "Search-first graph exploration with focal expansion.",
    status: "planned",
  },
  agents: {
    title: "Agent Workspace",
    description: "Compose tasks, monitor runs, approve outputs, use scratchpad.",
    status: "planned",
  },
  ops: {
    title: "Ops Log",
    description: "Inspect ingestion, sync, embeddings, policy violations.",
    status: "partial",
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
          
          <div className="workbench-page__placeholder">
            <div className="workbench-page__placeholder-icon">
              {view === "dashboard" && "📊"}
              {view === "content" && "📝"}
              {view === "review" && "✅"}
              {view === "memory" && "🧠"}
              {view === "agents" && "🤖"}
              {view === "ops" && "⚙️"}
            </div>
            <p className="workbench-page__placeholder-text">
              {config.status === "planned" 
                ? "This view is planned for a future release. Check the specs in packages/knoxx/specs/workbench/ for details."
                : "This view is partially implemented. Some features may be available."}
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}
