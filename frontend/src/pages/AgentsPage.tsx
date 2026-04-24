import React, { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import { Button } from "@open-hax/uxx";
import { listAdminTools } from "../lib/api/admin";
import type { AdminToolDefinition } from "../lib/types";

import { DiscordSection } from "../components/admin-page/DiscordSection";
import AgentAuditLogs from "../components/agent-audit/AgentAuditLogs";
import type { EventAgentJobControl } from "../lib/api/admin";
import { useAuth } from "./useAuth";

type AgentsTab = "control" | "audit";

function getTabFromLocation(location: ReturnType<typeof useLocation>): AgentsTab {
  const query = new URLSearchParams(location.search);
  const tab = query.get("tab");
  if (tab === "audit") return "audit";
  if (tab === "control") return "control";
  return "control";
}

function setTabInLocation(location: ReturnType<typeof useLocation>, tab: AgentsTab): string {
  const query = new URLSearchParams(location.search);
  query.set("tab", tab);
  const suffix = query.toString();
  return `${location.pathname}${suffix ? `?${suffix}` : ""}${location.hash}`;
}

export default function AgentsPage() {
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const [tab, setTab] = useState<AgentsTab>(() => getTabFromLocation(location));

  const [tools, setTools] = useState<AdminToolDefinition[]>([]);
  const [toolsError, setToolsError] = useState<string | null>(null);

  const [selectedJob, setSelectedJob] = useState<EventAgentJobControl | null>(null);

  const canControlEventAgents = useMemo(
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
    const nextTab = getTabFromLocation(location);
    setTab(nextTab);
  }, [location]);

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

  const agentFilterSeed = useMemo(() => {
    if (!selectedJob) return undefined;

    // Favor stable identifiers first, then role.
    const role = selectedJob.agentSpec?.role;
    if (typeof role === "string" && role.trim().length > 0) return role;

    const name = selectedJob.name;
    if (typeof name === "string" && name.trim().length > 0) return name;

    return selectedJob.id;
  }, [selectedJob]);

  return (
    <div data-page="agents" className="h-full min-h-0 overflow-y-auto bg-slate-950 p-4 text-slate-100">
      <div className="mb-4 flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div className="flex items-center gap-2">
          <Button
            variant={tab === "control" ? "primary" : "ghost"}
            size="sm"
            onClick={() => navigate(setTabInLocation(location, "control"))}
          >
            Runtime jobs
          </Button>
          <Button
            variant={tab === "audit" ? "primary" : "ghost"}
            size="sm"
            onClick={() => navigate(setTabInLocation(location, "audit"))}
          >
            Agent Audit Logs
          </Button>
        </div>

        {agentFilterSeed ? (
          <div className="text-xs text-slate-500">audit filter: <span className="font-mono text-slate-300">{agentFilterSeed}</span></div>
        ) : null}
      </div>

      {tab === "control" ? (
        <div className="space-y-4">
          {!canControlEventAgents ? (
            <div className="rounded-lg border border-slate-800 bg-slate-900/40 p-4 text-sm text-slate-400">
              Event-agent control access required.
            </div>
          ) : (
            <>
              {toolsError ? (
                <div className="rounded-lg border border-amber-700 bg-amber-950/30 p-3 text-xs text-amber-200">
                  Tool catalog unavailable: {toolsError}
                </div>
              ) : null}
              <DiscordSection
                canManage={canControlEventAgents}
                tools={tools}
                onSelectedJobChange={(job) => setSelectedJob(job)}
              />
            </>
          )}
        </div>
      ) : (
        <AgentAuditLogs
          builtInSessionQuery={agentFilterSeed}
          className="min-h-0"
        />
      )}
    </div>
  );
}
