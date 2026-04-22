import { useEffect } from 'react';
import { getAgentContractsCatalog, getFrontendConfig, getToolCatalog } from '../../lib/api';
import type { AgentContractCatalogItem, ToolCatalogResponse } from '../../lib/types';

type UseChatPageConfigParams = {
  defaultRole: string;
  activeRole: string;
  activeAgentId: string;
  setActiveRole: (value: string) => void;
  setActiveAgentId: (value: string) => void;
  setAvailableAgents: (value: AgentContractCatalogItem[]) => void;
  setToolCatalog: (value: ToolCatalogResponse | null) => void;
  setConsoleLines: (value: string[] | ((previous: string[]) => string[])) => void;
};

export function useChatPageConfig({
  defaultRole,
  activeRole,
  activeAgentId,
  setActiveRole,
  setActiveAgentId,
  setAvailableAgents,
  setToolCatalog,
  setConsoleLines,
}: UseChatPageConfigParams) {
  useEffect(() => {
    void Promise.all([getFrontendConfig(), getAgentContractsCatalog().catch(() => ({ agents: [] as AgentContractCatalogItem[], default_agent_contract: null }))])
      .then(([config, catalog]) => {
        const fetchedAgents = catalog.agents ?? [];
        const defaultAgentId = config.default_agent_contract || catalog.default_agent_contract || fetchedAgents[0]?.id || "knoxx_default";
        const fallbackAgent: AgentContractCatalogItem = {
          id: defaultAgentId,
          role: config.default_role || defaultRole,
        };
        const agents = fetchedAgents.length > 0
          ? (fetchedAgents.some((agent) => agent.id === defaultAgentId)
              ? fetchedAgents
              : [fallbackAgent, ...fetchedAgents])
          : [fallbackAgent];

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
      .catch(() => undefined);
  }, [activeAgentId, defaultRole, setActiveAgentId, setActiveRole, setAvailableAgents]);

  useEffect(() => {
    void getToolCatalog(activeRole, activeAgentId || undefined)
      .then(setToolCatalog)
      .catch((error) => {
        setConsoleLines((previous) => [...previous.slice(-400), `[tools] failed: ${(error as Error).message}`]);
      });
  }, [activeAgentId, activeRole, setConsoleLines, setToolCatalog]);
}
