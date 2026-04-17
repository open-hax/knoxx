/**
 * Status Bar Component
 *
 * Bottom status bar showing:
 * - Active collection name
 * - Current LLM provider/model
 * - Token budget
 * - Agent run count
 * - Current keyboard mode
 */

import { useStatusBarState, useKeyboardMode } from "./status-hooks";
import styles from "./Shell.module.css";

interface StatusBarProps {
  isChordActive?: boolean;
}

export function StatusBar({ isChordActive = false }: StatusBarProps) {
  const state = useStatusBarState();
  const mode = useKeyboardMode(isChordActive);

  const tokensDisplay = state.tokens.limit
    ? `${state.tokens.used.toLocaleString()} / ${state.tokens.limit.toLocaleString()}`
    : state.tokens.used > 0
      ? state.tokens.used.toLocaleString()
      : "—";

  const agentsDisplay = state.agents.active > 0
    ? `${state.agents.active} active`
    : state.agents.total > 0
      ? `${state.agents.total} total`
      : "0";

  return (
    <footer className={styles.statusBar}>
      <div className={styles.statusBarLeft}>
        <span className={styles.statusItem}>
          collection: <strong>{state.collection || "—"}</strong>
        </span>
        <span className={styles.statusItem}>
          model: <strong>{state.provider ? `${state.provider}/` : ""}{state.model || "—"}</strong>
        </span>
      </div>
      <div className={styles.statusBarRight}>
        <span className={styles.statusItem}>
          tokens: <strong>{tokensDisplay}</strong>
        </span>
        <span className={styles.statusItem}>
          agents: <strong>{agentsDisplay}</strong>
        </span>
        <span className={`${styles.statusItem} ${mode === "chord" ? styles.statusItemHighlight : ""}`}>
          mode: <strong>{mode}</strong>
        </span>
      </div>
    </footer>
  );
}

export default StatusBar;
