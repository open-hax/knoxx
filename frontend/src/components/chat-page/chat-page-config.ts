import { useEffect, useRef, type Dispatch, type SetStateAction } from 'react';
import { getAgentContractsCatalog, getFrontendConfig, getToolCatalog, listProxxModels, proxxHealth } from '../../lib/api';
import type { ActorCatalogItem, AgentContractCatalogItem, ProxxModelInfo, ToolCatalogResponse } from '../../lib/types';

type UseChatPageConfigParams = {
  defaultRole: string;
  defaultActorId: string;
  activeRole: string;
  activeActorId: string;
  activeAgentId: string;
  setActiveRole: (value: string) => void;
  setActiveActorId: (value: string) => void;
  setAvailableActors: (value: ActorCatalogItem[]) => void;
  setActiveAgentId: (value: string) => void;
  setAvailableAgents: (value: AgentContractCatalogItem[]) => void;
  setToolCatalog: (value: ToolCatalogResponse | null) => void;
  setConsoleLines: (value: string[] | ((previous: string[]) => string[])) => void;
  setSttEnabled: (value: boolean) => void;
  setTtsEnabled: (value: boolean) => void;
  setTtsDefaultVoiceId: (value: string) => void;
};

export function useChatPageConfig({
  defaultRole,
  defaultActorId,
  activeRole,
  activeActorId,
  activeAgentId,
  setActiveRole,
  setActiveActorId,
  setAvailableActors,
  setActiveAgentId,
  setAvailableAgents,
  setToolCatalog,
  setConsoleLines,
  setSttEnabled,
  setTtsEnabled,
  setTtsDefaultVoiceId,
}: UseChatPageConfigParams) {
  const activeAgentIdRef = useRef(activeAgentId);
  activeAgentIdRef.current = activeAgentId;

  useEffect(() => {
    const requestedActorId = activeActorId || defaultActorId;
    void Promise.all([getFrontendConfig(), getAgentContractsCatalog(requestedActorId)])
      .then(([config, catalog]) => {
        setSttEnabled(Boolean(config.stt_enabled));
        setTtsEnabled(Boolean(config.tts_enabled));
        setTtsDefaultVoiceId(config.tts_default_voice_id || '');

        const resolvedActorId = catalog.actor_id || requestedActorId || config.default_actor_id || defaultActorId;
        if (resolvedActorId && resolvedActorId !== activeActorId) {
          setActiveActorId(resolvedActorId);
        }

        const agents = catalog.agents ?? [];
        setAvailableActors(catalog.actors ?? []);
        const defaultAgentId = catalog.default_agent_contract || config.default_agent_contract || agents[0]?.id || '';
        setAvailableAgents(agents);

        // Read the CURRENT activeAgentId from the ref to avoid stale closure
        const currentAgentId = activeAgentIdRef.current;
        const nextAgentId = agents.some((agent) => agent.id === currentAgentId)
          ? currentAgentId
          : defaultAgentId;
        if (nextAgentId && nextAgentId !== currentAgentId) {
          setActiveAgentId(nextAgentId);
        }

        const selectedAgent = agents.find((agent) => agent.id === nextAgentId) ?? agents[0];
        setActiveRole(selectedAgent?.role || config.default_role || defaultRole);
      })
      .catch((error) => {
        setConsoleLines((previous) => [...previous.slice(-400), `[agents] failed: ${(error as Error).message}`]);
      });
  }, [activeActorId, activeAgentId, defaultActorId, defaultRole, setActiveActorId, setActiveAgentId, setActiveRole, setAvailableActors, setAvailableAgents, setConsoleLines, setSttEnabled, setTtsDefaultVoiceId, setTtsEnabled]);

  useEffect(() => {
    void getToolCatalog(activeRole, activeAgentId || undefined, activeActorId || undefined)
      .then(setToolCatalog)
      .catch((error) => {
        setConsoleLines((previous) => [...previous.slice(-400), `[tools] failed: ${(error as Error).message}`]);
      });
  }, [activeActorId, activeAgentId, activeRole, setConsoleLines, setToolCatalog]);
}

type SetState<T> = Dispatch<SetStateAction<T>>;

type UseProxxStatusPollingParams = {
  selectedModel: string;
  setSelectedModel: SetState<string>;
  setProxxReachable: SetState<boolean>;
  setProxxConfigured: SetState<boolean>;
  setProxxModels: SetState<ProxxModelInfo[]>;
};

export function useProxxStatusPolling({
  selectedModel,
  setSelectedModel,
  setProxxReachable,
  setProxxConfigured,
  setProxxModels,
}: UseProxxStatusPollingParams) {
  useEffect(() => {
    let timer: number | null = null;

    const poll = async () => {
      try {
        const status = await proxxHealth();
        setProxxReachable(Boolean(status.reachable));
        setProxxConfigured(Boolean(status.configured));
        const models = await listProxxModels();
        setProxxModels(models);
        if (!selectedModel) {
          const preferred = models.find((model) => model.id === status.default_model);
          setSelectedModel(preferred?.id ?? models[0]?.id ?? "");
        }
      } catch {
        setProxxReachable(false);
      }
    };

    void poll();
    timer = window.setInterval(() => {
      void poll();
    }, 5000);

    return () => {
      if (timer !== null) window.clearInterval(timer);
    };
  }, [selectedModel, setProxxConfigured, setProxxModels, setProxxReachable, setSelectedModel]);
}
