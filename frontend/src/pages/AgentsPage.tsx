import React, { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";

import { Button } from "@open-hax/uxx";
import AgentAuditLogs from "../components/agent-audit/AgentAuditLogs";

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
  const location = useLocation();
  const navigate = useNavigate();

  const [tab, setTab] = useState<AgentsTab>(() => getTabFromLocation(location));

  useEffect(() => {
    const nextTab = getTabFromLocation(location);
    setTab(nextTab);
  }, [location]);


  return (
    <div data-page="agents" className="flex flex-col h-full min-h-0 overflow-hidden bg-slate-950 p-4 text-slate-100">
      <div className="mb-4 flex flex-col gap-3 md:flex-row md:items-center md:justify-between shrink-0">
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

      </div>

      <div className="min-h-0 flex-1 overflow-hidden">
        {tab === "control" ? (
          <div className="flex flex-col h-full min-h-0 gap-4">
            <div
              id="knoxx-event-agents-root"
              className="h-full min-h-0"
            />
          </div>
        ) : (
          <AgentAuditLogs
            defaultMode="history"
            className="h-full"
          />
        )}
      </div>
    </div>
  );
}
