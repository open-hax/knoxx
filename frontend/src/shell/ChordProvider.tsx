import { createContext, useContext, useState, useCallback, type ReactNode } from "react";
import { WhichKeyPopup, type BindingEntry } from "@open-hax/uxx";

interface ChordContextValue {
  /** Current key sequence being built */
  sequence: string[];
  /** Whether chord mode is active */
  isActive: boolean;
  /** Start chord mode (usually on SPC press) */
  startChord: () => void;
  /** Add key to sequence */
  pressKey: (key: string) => void;
  /** Cancel chord mode */
  cancelChord: () => void;
  /** Execute action for current sequence */
  executeAction: () => void;
  /** Register a chord action */
  registerAction: (sequence: string[], action: ChordAction) => void;
  /** Get available bindings for current prefix */
  getAvailableBindings: () => BindingEntry[];
}

export interface ChordAction {
  description: string;
  category?: string;
  handler: () => void;
  destructive?: boolean;
}

const ChordContext = createContext<ChordContextValue | null>(null);

export function useChord() {
  const ctx = useContext(ChordContext);
  if (!ctx) {
    throw new Error("useChord must be used within ChordProvider");
  }
  return ctx;
}

interface ChordProviderProps {
  children: ReactNode;
  /** Initial action registry */
  initialActions?: Map<string, ChordAction>;
}

const KEY_NAMES: Record<string, string> = {
  " ": "SPC",
  Escape: "ESC",
  Enter: "RET",
  Backspace: "BS",
  Delete: "DEL",
  ArrowUp: "↑",
  ArrowDown: "↓",
  ArrowLeft: "←",
  ArrowRight: "→",
  Control: "C",
  Alt: "M",
  Meta: "CMD",
  Shift: "S",
};

function normalizeKey(key: string): string {
  return KEY_NAMES[key] || key.toUpperCase();
}

export function ChordProvider({ children, initialActions }: ChordProviderProps) {
  const [sequence, setSequence] = useState<string[]>([]);
  const [isActive, setIsActive] = useState(false);
  const [actions] = useState<Map<string, ChordAction>>(
    () => initialActions || new Map()
  );

  const startChord = useCallback(() => {
    setSequence(["SPC"]);
    setIsActive(true);
  }, []);

  const cancelChord = useCallback(() => {
    setSequence([]);
    setIsActive(false);
  }, []);

  const registerAction = useCallback(
    (seq: string[], action: ChordAction) => {
      const key = seq.join(" ");
      actions.set(key, action);
    },
    [actions]
  );

  const getAvailableBindings = useCallback((): BindingEntry[] => {
    const prefix = sequence.join(" ");
    const bindings: BindingEntry[] = [];

    for (const [key, action] of actions) {
      if (key.startsWith(prefix)) {
        const remaining = key.slice(prefix.length).trim();
        if (remaining) {
          const nextKey = remaining.split(" ")[0];
          bindings.push({
            key: nextKey,
            description: action.description,
            category: action.category,
            destructive: action.destructive,
          });
        }
      }
    }

    // Dedupe by key
    const seen = new Set<string>();
    return bindings.filter((b) => {
      if (seen.has(b.key)) return false;
      seen.add(b.key);
      return true;
    });
  }, [sequence, actions]);

  const pressKey = useCallback(
    (key: string) => {
      if (!isActive) return;

      const normalized = normalizeKey(key);
      
      // Escape cancels
      if (normalized === "ESC") {
        cancelChord();
        return;
      }

      // Add to sequence
      const newSequence = [...sequence, normalized];
      const keyStr = newSequence.join(" ");

      // Check if this completes an action
      const action = actions.get(keyStr);
      if (action) {
        action.handler();
        cancelChord();
        return;
      }

      // Check if this is a valid prefix
      const bindings = getAvailableBindings();
      const validNext = bindings.some((b) => b.key === normalized);
      
      if (validNext) {
        setSequence(newSequence);
      } else {
        // Invalid key, cancel
        cancelChord();
      }
    },
    [isActive, sequence, actions, getAvailableBindings, cancelChord]
  );

  const executeAction = useCallback(() => {
    const keyStr = sequence.join(" ");
    const action = actions.get(keyStr);
    if (action) {
      action.handler();
    }
    cancelChord();
  }, [sequence, actions, cancelChord]);

  const value: ChordContextValue = {
    sequence,
    isActive,
    startChord,
    pressKey,
    cancelChord,
    executeAction,
    registerAction,
    getAvailableBindings,
  };

  return (
    <ChordContext.Provider value={value}>
      {children}
      <WhichKeyPopup
        active={isActive}
        prefix={sequence}
        bindings={getAvailableBindings()}
        position="bottom"
        maxColumns={3}
        sortKey="key"
        showCategory={true}
        timeoutMs={0}
        onSelect={(binding) => {
          if (binding) {
            pressKey(binding.key);
          }
        }}
      />
    </ChordContext.Provider>
  );
}

export default ChordProvider;
