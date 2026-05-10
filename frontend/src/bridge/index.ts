/**
 * Bridge module: exports TypeScript components and APIs for shadow-cljs consumption.
 *
 * This module is built by Vite in library mode to produce a clean ESM bundle
 * that shadow-cljs can import. Keep exports minimal and only include components
 * that have been verified to work without Vite-specific transforms.
 */

export { EmptyState } from "../components/EmptyState";

// APIs for shadow-cljs consumption
export {
  // Auth context (permissions / isSystemAdmin)
  getKnoxxAuthContext,
} from "../lib/api/common";

export {
  // Tool catalog
  listAdminTools,

  // Event agent runtime control
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