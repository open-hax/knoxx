import React, { useEffect, useMemo, useState } from 'react';
import { listAdminTools } from '../lib/api/admin';
import { DiscordSection } from '../components/admin-page/DiscordSection';
import type { AdminToolDefinition } from '../lib/types';
import { useAuth } from './useAuth';

export default function EventAgentsPage() {
  const auth = useAuth();
  const [tools, setTools] = useState<AdminToolDefinition[]>([]);
  const [toolsError, setToolsError] = useState<string | null>(null);

  const canControlEventAgents = useMemo(
    () => Boolean(auth.isSystemAdmin || auth.permissions.includes('org.event_agents.control')),
    [auth.isSystemAdmin, auth.permissions],
  );

  const canReadToolCatalog = useMemo(
    () => Boolean(
      auth.isSystemAdmin
      || auth.permissions.includes('org.tool_policy.read')
      || auth.permissions.includes('platform.roles.manage')
      || auth.permissions.includes('org.user_policy.read'),
    ),
    [auth.isSystemAdmin, auth.permissions],
  );

  useEffect(() => {
    if (!canControlEventAgents || !canReadToolCatalog) {
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
  }, [canControlEventAgents, canReadToolCatalog]);

  if (!canControlEventAgents) {
    return (
      <div className="p-6 text-sm text-slate-400">
        Event-agent control access required.
      </div>
    );
  }

  return (
    <div className="space-y-4 p-4">
      <div>
        <h1 className="text-xl font-bold text-slate-100">Event Agents</h1>
        <p className="mt-1 text-sm text-slate-400">
          Manage the event-agent control plane, Discord bridge token, dispatch testing, and runtime jobs.
        </p>
        {toolsError ? (
          <p className="mt-2 text-xs text-amber-300">
            Tool catalog unavailable: {toolsError}
          </p>
        ) : null}
      </div>
      <DiscordSection canManage={canControlEventAgents} tools={tools} />
    </div>
  );
}
