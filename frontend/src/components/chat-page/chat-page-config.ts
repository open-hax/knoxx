import { useEffect } from 'react';
import { getFrontendConfig, getToolCatalog } from '../../lib/api';
import type { ToolCatalogResponse } from '../../lib/types';

type UseChatPageConfigParams = {
  defaultRole: string;
  activeRole: string;
  setActiveRole: (value: string) => void;
  setToolCatalog: (value: ToolCatalogResponse | null) => void;
  setConsoleLines: (value: string[] | ((previous: string[]) => string[])) => void;
};

export function useChatPageConfig({ defaultRole, activeRole, setActiveRole, setToolCatalog, setConsoleLines }: UseChatPageConfigParams) {
  useEffect(() => {
    void getFrontendConfig()
      .then((config) => {
        setActiveRole(config.default_role || defaultRole);
      })
      .catch(() => undefined);
  }, [defaultRole, setActiveRole]);

  useEffect(() => {
    void getToolCatalog(activeRole)
      .then(setToolCatalog)
      .catch((error) => {
        setConsoleLines((previous) => [...previous.slice(-400), `[tools] failed: ${(error as Error).message}`]);
      });
  }, [activeRole, setConsoleLines, setToolCatalog]);
}
