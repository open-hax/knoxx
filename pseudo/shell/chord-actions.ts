import type { ChordAction } from "./ChordProvider";

/**
 * Chord action registry for the workbench.
 *
 * Chord conventions (Spacemacs-style):
 * - SPC = start chord mode
 * - First key = category (g=goto, i=insert, p=publish, t=task, a=agent)
 * - Second key = action
 * - Third key = modifier (optional)
 *
 * Navigation (g prefix):
 * - SPC g d = Dashboard
 * - SPC g c = Content Editor
 * - SPC g r = Review Queue
 * - SPC g m = Memory Inspector
 * - SPC g a = Agent Workspace
 * - SPC g o = Ops Log
 *
 * Insert/Suggestion (i prefix):
 * - SPC i a = Accept suggestion
 * - SPC i d = Dismiss suggestion
 * - SPC i r = Revise suggestion
 *
 * Publish (p prefix):
 * - SPC p = Publish menu
 * - SPC p p = Publish to review
 * - SPC p d = Demote to draft
 *
 * Review (a/r/s):
 * - SPC a = Approve item
 * - SPC r = Reject item
 * - SPC s = Skip item
 *
 * Task/Agent (t prefix):
 * - SPC t x = Stop agent run
 * - SPC t r = Retry agent run
 * - SPC t n = New task
 */

export interface ChordActionRegistry {
  register: (sequence: string[], action: ChordAction) => void;
  getActions: () => Map<string, ChordAction>;
}

export function createChordActionRegistry(): ChordActionRegistry {
  const actions = new Map<string, ChordAction>();

  return {
    register(sequence: string[], action: ChordAction) {
      const key = sequence.join(" ");
      actions.set(key, action);
    },
    getActions: () => actions,
  };
}

/**
 * Default chord actions for the workbench.
 * These are registered on ChordProvider mount.
 */
export function getDefaultChordActions(
  navigate: (path: string) => void
): Array<{ sequence: string[]; action: ChordAction }> {
  return [
    // Navigation (g = goto)
    {
      sequence: ["SPC", "g", "d"],
      action: {
        description: "Dashboard",
        category: "Navigation",
        handler: () => navigate("/workbench/dashboard"),
      },
    },
    {
      sequence: ["SPC", "g", "c"],
      action: {
        description: "Content Editor",
        category: "Navigation",
        handler: () => navigate("/workbench/content"),
      },
    },
    {
      sequence: ["SPC", "g", "r"],
      action: {
        description: "Review Queue",
        category: "Navigation",
        handler: () => navigate("/workbench/review"),
      },
    },
    {
      sequence: ["SPC", "g", "m"],
      action: {
        description: "Memory Inspector",
        category: "Navigation",
        handler: () => navigate("/workbench/memory"),
      },
    },
    {
      sequence: ["SPC", "g", "a"],
      action: {
        description: "Agent Workspace",
        category: "Navigation",
        handler: () => navigate("/workbench/agents"),
      },
    },
    {
      sequence: ["SPC", "g", "o"],
      action: {
        description: "Ops Log",
        category: "Navigation",
        handler: () => navigate("/workbench/ops"),
      },
    },

    // Review actions (context-aware)
    {
      sequence: ["SPC", "a"],
      action: {
        description: "Approve item",
        category: "Review",
        handler: () => {
          // Emit event for ReviewQueuePage to handle
          window.dispatchEvent(new CustomEvent("chord:approve"));
        },
      },
    },
    {
      sequence: ["SPC", "r"],
      action: {
        description: "Reject item",
        category: "Review",
        destructive: true,
        handler: () => {
          window.dispatchEvent(new CustomEvent("chord:reject"));
        },
      },
    },
    {
      sequence: ["SPC", "s"],
      action: {
        description: "Skip item",
        category: "Review",
        handler: () => {
          window.dispatchEvent(new CustomEvent("chord:skip"));
        },
      },
    },

    // Insert/Suggestion (i = insert)
    {
      sequence: ["SPC", "i", "a"],
      action: {
        description: "Accept suggestion",
        category: "Suggestion",
        handler: () => {
          window.dispatchEvent(new CustomEvent("chord:accept-suggestion"));
        },
      },
    },
    {
      sequence: ["SPC", "i", "d"],
      action: {
        description: "Dismiss suggestion",
        category: "Suggestion",
        handler: () => {
          window.dispatchEvent(new CustomEvent("chord:dismiss-suggestion"));
        },
      },
    },
    {
      sequence: ["SPC", "i", "r"],
      action: {
        description: "Revise suggestion",
        category: "Suggestion",
        handler: () => {
          window.dispatchEvent(new CustomEvent("chord:revise-suggestion"));
        },
      },
    },

    // Publish (p = publish)
    {
      sequence: ["SPC", "p"],
      action: {
        description: "Publish menu",
        category: "Publish",
        handler: () => {
          window.dispatchEvent(new CustomEvent("chord:publish-menu"));
        },
      },
    },
    {
      sequence: ["SPC", "p", "p"],
      action: {
        description: "Publish to review",
        category: "Publish",
        handler: () => {
          window.dispatchEvent(new CustomEvent("chord:publish-to-review"));
        },
      },
    },
    {
      sequence: ["SPC", "p", "d"],
      action: {
        description: "Demote to draft",
        category: "Publish",
        handler: () => {
          window.dispatchEvent(new CustomEvent("chord:demote-to-draft"));
        },
      },
    },

    // Task/Agent (t = task)
    {
      sequence: ["SPC", "t", "x"],
      action: {
        description: "Stop agent run",
        category: "Agent",
        destructive: true,
        handler: () => {
          window.dispatchEvent(new CustomEvent("chord:stop-agent"));
        },
      },
    },
    {
      sequence: ["SPC", "t", "r"],
      action: {
        description: "Retry agent run",
        category: "Agent",
        handler: () => {
          window.dispatchEvent(new CustomEvent("chord:retry-agent"));
        },
      },
    },
    {
      sequence: ["SPC", "t", "n"],
      action: {
        description: "New task",
        category: "Agent",
        handler: () => {
          window.dispatchEvent(new CustomEvent("chord:new-task"));
        },
      },
    },

    // Help
    {
      sequence: ["SPC", "?"],
      action: {
        description: "Show all keybindings",
        category: "Help",
        handler: () => {
          // Already showing - this is implicit
        },
      },
    },
  ];
}
