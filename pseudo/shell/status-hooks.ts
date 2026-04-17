/**
 * Status Bar Hooks
 *
 * Hooks for subscribing to status bar state:
 * - Collection context (from tenant)
 * - Model/provider
 * - Token budget
 * - Agent run count
 * - Keyboard mode
 */

import { useState, useEffect, useCallback } from "react";

/**
 * Status bar state.
 */
export interface StatusBarState {
  collection: string | null;
  model: string | null;
  provider: string | null;
  tokens: {
    used: number;
    limit: number | null;
  };
  agents: {
    active: number;
    total: number;
  };
  mode: "normal" | "chord" | "insert";
}

const DEFAULT_STATE: StatusBarState = {
  collection: null,
  model: null,
  provider: null,
  tokens: {
    used: 0,
    limit: null,
  },
  agents: {
    active: 0,
    total: 0,
  },
  mode: "normal",
};

/**
 * Hook for status bar state.
 *
 * In the future, this will subscribe to:
 * - Tenant context from API
 * - Model/provider from settings
 * - Token budget from agent runs
 * - Agent run count from agent service
 *
 * For now, returns default state with optional overrides.
 */
export function useStatusBarState(): StatusBarState {
  const [state, setState] = useState<StatusBarState>(DEFAULT_STATE);

  // Listen for collection changes (from tenant context)
  useEffect(() => {
    // TODO: Fetch from tenant context API
    // For now, use a default or check URL params
    const params = new URLSearchParams(window.location.search);
    const collection = params.get("collection") || "devel";
    
    setState((prev) => ({ ...prev, collection }));
  }, []);

  // Listen for model/provider changes
  useEffect(() => {
    // TODO: Fetch from settings API
    // For now, use localStorage or default
    const savedModel = localStorage.getItem("knoxx:model");
    const savedProvider = localStorage.getItem("knoxx:provider");
    
    setState((prev) => ({
      ...prev,
      model: savedModel || "glm-5",
      provider: savedProvider || "proxx",
    }));
  }, []);

  return state;
}

/**
 * Hook for keyboard mode.
 *
 * Tracks whether chord mode is active.
 */
export function useKeyboardMode(isChordActive: boolean): "normal" | "chord" {
  return isChordActive ? "chord" : "normal";
}

/**
 * Hook for agent run count.
 *
 * In the future, this will subscribe to agent service.
 */
export function useAgentRunCount(): { active: number; total: number } {
  const [count, setCount] = useState({ active: 0, total: 0 });

  // TODO: Subscribe to agent service
  // For now, check for agent runs in localStorage (demo)
  useEffect(() => {
    const saved = localStorage.getItem("knoxx:agentRuns");
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        setCount(parsed);
      } catch {
        // Ignore parse errors
      }
    }
  }, []);

  return count;
}

/**
 * Hook for token budget.
 *
 * In the future, this will track token usage from agent runs.
 */
export function useTokenBudget(): { used: number; limit: number | null } {
  const [budget, setBudget] = useState({ used: 0, limit: null as number | null });

  // TODO: Subscribe to token tracking service
  // For now, return defaults
  useEffect(() => {
    // Placeholder for token tracking
    setBudget({ used: 0, limit: null });
  }, []);

  return budget;
}

/**
 * Hook for collection context.
 *
 * In the future, this will fetch from tenant context API.
 */
export function useCollectionContext(): string | null {
  const [collection, setCollection] = useState<string | null>(null);

  useEffect(() => {
    // TODO: Fetch from tenant context API
    // For now, use URL param or default
    const params = new URLSearchParams(window.location.search);
    const coll = params.get("collection") || "devel";
    setCollection(coll);
  }, []);

  return collection;
}

/**
 * Hook for model selection.
 *
 * Returns current model and a setter.
 */
export function useModel(): [string | null, (model: string) => void] {
  const [model, setModelState] = useState<string | null>(null);

  useEffect(() => {
    const saved = localStorage.getItem("knoxx:model") || "glm-5";
    setModelState(saved);
  }, []);

  const setModel = useCallback((newModel: string) => {
    localStorage.setItem("knoxx:model", newModel);
    setModelState(newModel);
  }, []);

  return [model, setModel];
}
