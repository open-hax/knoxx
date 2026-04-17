export const OPS_BASE_PATH = '/ops';
export const LEGACY_OPS_BASE_PATH = '/next';

function trimSlashes(value: string): string {
  return value.replace(/^\/+|\/+$/g, '');
}

export function joinPath(basePath: string, subpath = ''): string {
  const base = trimSlashes(basePath);
  const next = trimSlashes(subpath);
  if (!base && !next) return '/';
  if (!base) return `/${next}`;
  if (!next) return `/${base}`;
  return `/${base}/${next}`;
}

export const opsRoutes = {
  root: OPS_BASE_PATH,
  dashboard: joinPath(OPS_BASE_PATH, 'dashboard'),
  documents: joinPath(OPS_BASE_PATH, 'documents'),
  docsView: joinPath(OPS_BASE_PATH, 'docs/view'),
  agents: joinPath(OPS_BASE_PATH, 'agents'),
  contracts: joinPath(OPS_BASE_PATH, 'contracts'),
  vectors: joinPath(OPS_BASE_PATH, 'vectors'),
  graphExportDebug: joinPath(OPS_BASE_PATH, 'graph-export-debug'),
  settings: joinPath(OPS_BASE_PATH, 'settings'),
  admin: joinPath(OPS_BASE_PATH, 'admin'),
} as const;

export function remapLegacyOpsPath(pathname: string, search = '', hash = ''): string {
  if (pathname === LEGACY_OPS_BASE_PATH) {
    return `${OPS_BASE_PATH}${search}${hash}`;
  }

  if (pathname.startsWith(`${LEGACY_OPS_BASE_PATH}/`)) {
    return `${OPS_BASE_PATH}${pathname.slice(LEGACY_OPS_BASE_PATH.length)}${search}${hash}`;
  }

  return `${pathname}${search}${hash}`;
}
