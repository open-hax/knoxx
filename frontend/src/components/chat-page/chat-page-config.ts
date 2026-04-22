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
    void Promise.all([getFrontendConfig(), getAgentContractsCatalog()])
      .then(([config, catalog]) => {
        const agents = catalog.agents ?? [];
        const defaultAgentId = config.default_agent_contract || catalog.default_agent_contract || agents[0]?.id || '';

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
  }, [activeAgentId, defaultRole, setActiveAgentId, setActiveRole, setAvailableAgents, setConsoleLines]);

  useEffect(() => {
    void getToolCatalog(activeRole, activeAgentId || undefined)
      .then(setToolCatalog)
      .catch((error) => {
        setConsoleLines((previous) => [...previous.slice(-400), `[tools] failed: ${(error as Error).message}`]);
      });
  }, [activeAgentId, activeRole, setConsoleLines, setToolCatalog]);
}
