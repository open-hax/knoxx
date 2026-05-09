/**
 * Bridge module: exports TypeScript components and APIs for shadow-cljs consumption.
 *
 * This module is built by Vite in library mode to produce a clean ESM bundle
 * that shadow-cljs can import. Keep exports minimal and only include components
 * that have been verified to work without Vite-specific transforms.
 */

export { EmptyState } from "../components/EmptyState";

// Admin API functions for event agent runtime control
export {
  getDiscordConfig,
  updateDiscordConfig,
  getEventAgentControl,
  updateEventAgentControl,
  runEventAgentJob,
  dispatchEventAgentEvent,
  stopEventAgentRuntime,
  startEventAgentRuntime,
  resetEventAgentRuntime,
} from "../lib/api/admin";