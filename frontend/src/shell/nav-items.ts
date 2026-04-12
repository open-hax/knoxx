/**
 * Navigation items for the Context Bar.
 *
 * Six named views accessible from the left rail:
 * - Dashboard: Landing page showing attention items, agent runs, memory activity
 * - Content Editor: Author and publish structured documents
 * - Review Queue: Process pending items with correction capture
 * - Memory Inspector: Search-first graph exploration
 * - Agent Workspace: Compose tasks, monitor runs, approve outputs
 * - Ops Log: Inspect ingestion, sync, embeddings, policy violations
 */

export interface NavItem {
  id: string;
  path: string;
  label: string;
  icon: string;
  description: string;
  chordHint: string;
}

export const NAV_ITEMS: NavItem[] = [
  {
    id: "dashboard",
    path: "/workbench/dashboard",
    label: "Dashboard",
    icon: "📊",
    description: "Review what changed, what needs attention, what agents are doing",
    chordHint: "SPC g d",
  },
  {
    id: "content",
    path: "/workbench/content",
    label: "Content",
    icon: "📝",
    description: "Author and publish structured documents with AI assistance",
    chordHint: "SPC g c",
  },
  {
    id: "review",
    path: "/workbench/review",
    label: "Review",
    icon: "✅",
    description: "Process pending items — approve, correct, reject outputs",
    chordHint: "SPC g r",
  },
  {
    id: "memory",
    path: "/workbench/memory",
    label: "Memory",
    icon: "🧠",
    description: "Search and explore the knowledge graph",
    chordHint: "SPC g m",
  },
  {
    id: "agents",
    path: "/workbench/agents",
    label: "Agents",
    icon: "🤖",
    description: "Compose tasks, monitor runs, approve results",
    chordHint: "SPC g a",
  },
  {
    id: "ops",
    path: "/workbench/ops",
    label: "Ops",
    icon: "⚙️",
    description: "Inspect ingestion, sync state, embeddings, errors",
    chordHint: "SPC g o",
  },
];

/**
 * Get the index of the active nav item for a given pathname.
 */
export function getActiveNavIndex(pathname: string): number {
  // Exact match first
  const exactMatch = NAV_ITEMS.findIndex((item) => pathname === item.path);
  if (exactMatch !== -1) return exactMatch;

  // Prefix match (but not for dashboard, which is the fallback)
  const prefixMatch = NAV_ITEMS.findIndex(
    (item) => item.id !== "dashboard" && pathname.startsWith(item.path)
  );
  if (prefixMatch !== -1) return prefixMatch;

  // Default to dashboard
  return 0;
}

/**
 * Get the next nav item index (with wrap).
 */
export function getNextNavIndex(currentIndex: number): number {
  return (currentIndex + 1) % NAV_ITEMS.length;
}

/**
 * Get the previous nav item index (with wrap).
 */
export function getPrevNavIndex(currentIndex: number): number {
  return (currentIndex - 1 + NAV_ITEMS.length) % NAV_ITEMS.length;
}
