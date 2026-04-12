import { useState, type ReactNode } from "react";
import { NavLink, useLocation } from "react-router-dom";
import styles from "./Shell.module.css";

interface ShellProps {
  children: ReactNode;
}

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

const NAV_ITEMS: NavItem[] = [
  { path: "/", label: "Dashboard", icon: "📊" },
  { path: "/cms", label: "Content", icon: "📝" },
  { path: "/translations", label: "Review", icon: "✅" },
  { path: "/query", label: "Memory", icon: "🧠" },
  { path: "/runs", label: "Agents", icon: "🤖" },
  { path: "/ops", label: "Ops", icon: "⚙️" },
];

export function Shell({ children }: ShellProps) {
  const [rightPanelOpen, setRightPanelOpen] = useState(true);
  const location = useLocation();

  const activeItem = NAV_ITEMS.find((item) => 
    location.pathname === item.path || 
    (item.path !== "/" && location.pathname.startsWith(item.path))
  );

  return (
    <div className={styles.shell}>
      {/* Left Context Bar */}
      <aside className={styles.contextBar}>
        <div className={styles.contextBarHeader}>
          <span className={styles.brand}>Knoxx</span>
        </div>
        <nav className={styles.contextBarNav}>
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={`${styles.navItem} ${
                activeItem?.path === item.path ? styles.navItemActive : ""
              }`}
              title={item.label}
            >
              <span className={styles.navIcon}>{item.icon}</span>
              <span className={styles.navLabel}>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className={styles.contextBarFooter}>
          <button
            className={styles.panelToggle}
            onClick={() => setRightPanelOpen(!rightPanelOpen)}
            title={rightPanelOpen ? "Hide inspection panel" : "Show inspection panel"}
          >
            <span className={styles.navIcon}>{rightPanelOpen ? "◀" : "▶"}</span>
            <span className={styles.navLabel}>Inspect</span>
          </button>
        </div>
      </aside>

      {/* Main Canvas */}
      <main className={styles.mainCanvas}>
        {children}
      </main>

      {/* Right Inspection Panel */}
      <aside className={`${styles.inspectionPanel} ${rightPanelOpen ? styles.inspectionPanelOpen : ""}`}>
        <div className={styles.inspectionPanelHeader}>
          <span className={styles.inspectionPanelTitle}>Inspection</span>
          <button
            className={styles.inspectionPanelClose}
            onClick={() => setRightPanelOpen(false)}
            title="Close panel"
          >
            ✕
          </button>
        </div>
        <div className={styles.inspectionPanelContent}>
          <p className={styles.emptyState}>
            Select an item to inspect its provenance, memory context, or agent state.
          </p>
        </div>
      </aside>

      {/* Status Bar */}
      <footer className={styles.statusBar}>
        <div className={styles.statusBarLeft}>
          <span className={styles.statusItem}>collection: <strong>devel</strong></span>
          <span className={styles.statusItem}>model: <strong>glm-5</strong></span>
        </div>
        <div className={styles.statusBarRight}>
          <span className={styles.statusItem}>tokens: <strong>—</strong></span>
          <span className={styles.statusItem}>agents: <strong>0</strong></span>
          <span className={styles.statusItem}>mode: <strong>normal</strong></span>
        </div>
      </footer>
    </div>
  );
}

export default Shell;
