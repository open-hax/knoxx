import { useState, useEffect, type ReactNode } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { ChordProvider, useChord } from "./ChordProvider";
import { getDefaultChordActions } from "./chord-actions";
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
  { path: "/workbench/dashboard", label: "Dashboard", icon: "📊" },
  { path: "/workbench/content", label: "Content", icon: "📝" },
  { path: "/workbench/review", label: "Review", icon: "✅" },
  { path: "/workbench/memory", label: "Memory", icon: "🧠" },
  { path: "/workbench/agents", label: "Agents", icon: "🤖" },
  { path: "/workbench/ops", label: "Ops", icon: "⚙️" },
];

function ShellContent({ children }: ShellProps) {
  const [rightPanelOpen, setRightPanelOpen] = useState(true);
  const location = useLocation();
  const navigate = useNavigate();
  const { isActive, startChord, pressKey, cancelChord, registerAction } = useChord();

  // Register default chord actions on mount
  useEffect(() => {
    const actions = getDefaultChordActions((path) => navigate(path));
    for (const { sequence, action } of actions) {
      registerAction(sequence, action);
    }
  }, [navigate, registerAction]);

  // Global key listener for chord mode
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Ignore if focus is in an input/textarea
      const target = e.target as HTMLElement;
      if (target.tagName === "INPUT" || target.tagName === "TEXTAREA" || target.isContentEditable) {
        return;
      }

      // SPC starts chord mode
      if (e.key === " " && !isActive) {
        e.preventDefault();
        startChord();
        return;
      }

      // Forward keys to chord system when active
      if (isActive) {
        e.preventDefault();
        
        // Escape cancels
        if (e.key === "Escape") {
          cancelChord();
          return;
        }

        // Single character keys
        if (e.key.length === 1) {
          pressKey(e.key);
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isActive, startChord, pressKey, cancelChord]);

  const activeItem = NAV_ITEMS.find((item) =>
    location.pathname === item.path ||
    (item.path !== "/workbench/dashboard" && location.pathname.startsWith(item.path))
  );

  return (
    <div className={`${styles.shell} ${isActive ? styles.shellChordActive : ""}`}>
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
          <span className={styles.statusItem}>
            mode: <strong>{isActive ? "chord" : "normal"}</strong>
          </span>
        </div>
      </footer>
    </div>
  );
}

export function Shell({ children }: ShellProps) {
  return (
    <ChordProvider>
      <ShellContent>{children}</ShellContent>
    </ChordProvider>
  );
}

export default Shell;
