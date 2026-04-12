import { useState, useEffect, useRef, type ReactNode } from "react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import { ChordProvider, useChord } from "./ChordProvider";
import { getDefaultChordActions } from "./chord-actions";
import {
  NAV_ITEMS,
  getActiveNavIndex,
  getNextNavIndex,
  getPrevNavIndex,
} from "./nav-items";
import styles from "./Shell.module.css";

interface ShellProps {
  children: ReactNode;
}

function ShellContent({ children }: ShellProps) {
  const [rightPanelOpen, setRightPanelOpen] = useState(true);
  const [focusedNavIndex, setFocusedNavIndex] = useState<number | null>(null);
  const [isCollapsed, setIsCollapsed] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();
  const navItemRefs = useRef<(HTMLAnchorElement | null)[]>([]);
  const { isActive, startChord, pressKey, cancelChord, registerAction } = useChord();

  const activeIndex = getActiveNavIndex(location.pathname);

  // Register default chord actions on mount
  useEffect(() => {
    const actions = getDefaultChordActions((path) => navigate(path));
    for (const { sequence, action } of actions) {
      registerAction(sequence, action);
    }
  }, [navigate, registerAction]);

  // Detect collapsed mode based on viewport
  useEffect(() => {
    const checkCollapsed = () => {
      setIsCollapsed(window.innerWidth < 1024);
    };
    checkCollapsed();
    window.addEventListener("resize", checkCollapsed);
    return () => window.removeEventListener("resize", checkCollapsed);
  }, []);

  // Global key listener for chord mode and nav keyboard navigation
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
        return;
      }

      // Nav keyboard navigation (when not in chord mode)
      if (focusedNavIndex !== null) {
        if (e.key === "ArrowDown" || e.key === "j") {
          e.preventDefault();
          const nextIndex = getNextNavIndex(focusedNavIndex);
          setFocusedNavIndex(nextIndex);
          navItemRefs.current[nextIndex]?.focus();
        } else if (e.key === "ArrowUp" || e.key === "k") {
          e.preventDefault();
          const prevIndex = getPrevNavIndex(focusedNavIndex);
          setFocusedNavIndex(prevIndex);
          navItemRefs.current[prevIndex]?.focus();
        } else if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          navigate(NAV_ITEMS[focusedNavIndex].path);
          setFocusedNavIndex(null);
        } else if (e.key === "Escape") {
          setFocusedNavIndex(null);
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isActive, startChord, pressKey, cancelChord, focusedNavIndex, navigate]);

  // Handle nav item focus
  const handleNavFocus = (index: number) => {
    setFocusedNavIndex(index);
  };

  const handleNavBlur = () => {
    // Delay blur to allow clicking nav items
    setTimeout(() => setFocusedNavIndex(null), 100);
  };

  return (
    <div className={`${styles.shell} ${isActive ? styles.shellChordActive : ""}`}>
      {/* Left Context Bar */}
      <aside className={styles.contextBar}>
        <div className={styles.contextBarHeader}>
          <span className={styles.brand}>Knoxx</span>
        </div>
        <nav className={styles.contextBarNav} role="navigation" aria-label="Main navigation">
          {NAV_ITEMS.map((item, index) => {
            const isActiveItem = activeIndex === index;
            const isFocused = focusedNavIndex === index;
            
            return (
              <NavLink
                key={item.id}
                ref={(el) => { navItemRefs.current[index] = el; }}
                to={item.path}
                className={`${styles.navItem} ${
                  isActiveItem ? styles.navItemActive : ""
                } ${isFocused ? styles.navItemFocused : ""}`}
                title={isCollapsed ? `${item.label}: ${item.chordHint}` : item.chordHint}
                onFocus={() => handleNavFocus(index)}
                onBlur={handleNavBlur}
                aria-current={isActiveItem ? "page" : undefined}
              >
                <span className={styles.navIcon} aria-hidden="true">{item.icon}</span>
                <span className={styles.navLabel}>{item.label}</span>
                {!isCollapsed && (
                  <span className={styles.navChord} aria-label={`Keyboard shortcut: ${item.chordHint}`}>
                    {item.chordHint.replace("SPC ", "␣ ")}
                  </span>
                )}
              </NavLink>
            );
          })}
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
