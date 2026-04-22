import { useEffect } from 'react';
import { getAgentContractsCatalog, getFrontendConfig, getToolCatalog } from '../../lib/api';
import type { AgentContractCatalogItem, ToolCatalogResponse } from '../../lib/types';

type UseChatPageConfigParams = {
  defaultRole: string;
  defaultActorId: string;
  activeRole: string;
  activeActorId: string;
  activeAgentId: string;
  setActiveRole: (value: string) => void;
  setActiveActorId: (value: string) => void;
  setActiveAgentId: (value: string) => void;
  setAvailableAgents: (value: AgentContractCatalogItem[]) => void;
  setToolCatalog: (value: ToolCatalogResponse | null) => void;
  setConsoleLines: (value: string[] | ((previous: string[]) => string[])) => void;
};

export function useChatPageConfig({
  defaultRole,
  defaultActorId,
  activeRole,
  activeActorId,
  activeAgentId,
  setActiveRole,
  setActiveActorId,
  setActiveAgentId,
  setAvailableAgents,
  setToolCatalog,
  setConsoleLines,
}: UseChatPageConfigParams) {
  useEffect(() => {
    const requestedActorId = activeActorId || defaultActorId;
    void Promise.all([getFrontendConfig(), getAgentContractsCatalog(requestedActorId)])
      .then(([config, catalog]) => {
        const resolvedActorId = catalog.actor_id || requestedActorId || config.default_actor_id || defaultActorId;
        if (resolvedActorId && resolvedActorId !== activeActorId) {
          setActiveActorId(resolvedActorId);
        }

        const agents = catalog.agents ?? [];
        const defaultAgentId = catalog.default_agent_contract || config.default_agent_contract || agents[0]?.id || '';
        setAvailableAgents(agents);

        const nextAgentId = agents.some((agent) => agent.id === activeAgentId)
          ? activeAgentId
          : defaultAgentId;
        if (nextAgentId && nextAgentId !== activeAgentId) {
          setActiveAgentId(nextAgentId);
        }

        const selectedAgent = agents.find((agent) => agent.id === nextAgentId) ?? agents[0];
        setActiveRole(selectedAgent?.role || config.default_role || defaultRole);
      })
      .catch((error) => {
        setConsoleLines((previous) => [...previous.slice(-400), `[agents] failed: ${(error as Error).message}`]);
      });
  }, [activeActorId, activeAgentId, defaultActorId, defaultRole, setActiveActorId, setActiveAgentId, setActiveRole, setAvailableAgents, setConsoleLines]);

  useEffect(() => {
    void getToolCatalog(activeRole, activeAgentId || undefined, activeActorId || undefined)
      .then(setToolCatalog)
      .catch((error) => {
        setConsoleLines((previous) => [...previous.slice(-400), `[tools] failed: ${(error as Error).message}`]);
      });
  }, [activeActorId, activeAgentId, activeRole, setConsoleLines, setToolCatalog]);
}
