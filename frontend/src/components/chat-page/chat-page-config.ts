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
        const agents = catalog.agents ?? [];
        setAvailableAgents(agents);

        const fallbackAgentId = config.default_agent_contract || catalog.default_agent_contract || agents[0]?.id || '';
        if (fallbackAgentId) {
          setActiveAgentId(fallbackAgentId);
        }

        const selectedAgent = agents.find((agent) => agent.id === fallbackAgentId) ?? agents[0];
        setActiveRole(selectedAgent?.role || config.default_role || defaultRole);
      })
      .catch(() => undefined);
  }, [defaultRole, setActiveAgentId, setActiveRole, setAvailableAgents]);

  useEffect(() => {
    void getToolCatalog(activeRole, activeAgentId || undefined)
      .then(setToolCatalog)
      .catch((error) => {
        setConsoleLines((previous) => [...previous.slice(-400), `[tools] failed: ${(error as Error).message}`]);
      });
  }, [activeAgentId, activeRole, setConsoleLines, setToolCatalog]);
}
