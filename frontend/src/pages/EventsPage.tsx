import React, { useEffect, useMemo, useState } from "react";

import { DiscordSection } from "../components/admin-page/DiscordSection";
import { listAdminTools } from "../lib/api/admin";
import type { AdminToolDefinition } from "../lib/types";
import { useAuth } from "./useAuth";

export default function EventsPage() {
  const auth = useAuth();
  const [tools, setTools] = useState<AdminToolDefinition[]>([]);
  const [toolsError, setToolsError] = useState<string | null>(null);

  const canControlEvents = useMemo(
    () => Boolean(auth.isSystemAdmin || auth.permissions.includes("org.event_agents.control")),
    [auth.isSystemAdmin, auth.permissions],
  );

  const canReadToolCatalog = useMemo(
    () => Boolean(
      auth.isSystemAdmin
      || auth.permissions.includes("org.tool_policy.read")
      || auth.permissions.includes("platform.roles.manage")
      || auth.permissions.includes("org.user_policy.read"),
    ),
    [auth.isSystemAdmin, auth.permissions],
  );

  useEffect(() => {
    if (!canControlEvents || !canReadToolCatalog) {
      setTools([]);
      return;
    }

    let cancelled = false;
    void listAdminTools()
      .then((result) => {
        if (!cancelled) {
          setTools(result.tools);
          setToolsError(null);
        }
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setTools([]);
          setToolsError(error instanceof Error ? error.message : String(error));
        }
      });

    return () => {
      cancelled = true;
    };
  }, [canControlEvents, canReadToolCatalog]);

  return (
    <div data-page="events" className="h-full min-h-0 overflow-y-auto bg-slate-950 p-4 text-slate-100">
      <div className="mb-4 space-y-1">
        <h1 className="text-xl font-semibold text-slate-100">Events</h1>
        <p className="text-sm text-slate-400">
          Generic event runtime control: schedules, triggers, dispatch, and reset.
        </p>
      </div>

      {!canControlEvents ? (
        <div className="rounded-lg border border-slate-800 bg-slate-900/40 p-4 text-sm text-slate-400">
          Event runtime control access required.
        </div>
      ) : (
        <div className="space-y-4">
          {toolsError ? (
            <div className="rounded-lg border border-amber-700 bg-amber-950/30 p-3 text-xs text-amber-200">
              Tool catalog unavailable: {toolsError}
            </div>
          ) : null}
          <DiscordSection canManage={canControlEvents} tools={tools} />
        </div>
      )}
    </div>
  );
}
