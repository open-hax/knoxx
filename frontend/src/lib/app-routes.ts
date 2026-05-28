export const OPS_BASE_PATH = '/ops';
export const LEGACY_OPS_BASE_PATH = '/next';
export const AGENTS_ROUTE = '/agents';
export const EVENTS_ROUTE = '/events';
/** Legacy route kept for redirects. */
export const LEGACY_EVENT_AGENTS_ROUTE = '/event-agents';
// Back-compat name: this was previously the event-agent control surface.
export const EVENT_AGENTS_ROUTE = EVENTS_ROUTE;
export const BASIC_USER_ROLES = ['basic_user', 'basic-user'] as const;
export const BASIC_USER_ROLE = BASIC_USER_ROLES[0];

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
  documents: joinPath(OPS_BASE_PATH, 'documents'),
  docsView: joinPath(OPS_BASE_PATH, 'docs/view'),
  agents: joinPath(OPS_BASE_PATH, 'agents'),
  studio: joinPath(OPS_BASE_PATH, 'studio'),
  vectors: joinPath(OPS_BASE_PATH, 'vectors'),
  labels: joinPath(OPS_BASE_PATH, 'labels'),
  graphExportDebug: joinPath(OPS_BASE_PATH, 'graph-export-debug'),
  settings: joinPath(OPS_BASE_PATH, 'settings'),
  admin: joinPath(OPS_BASE_PATH, 'admin'),
} as const;

export function isBasicUserRole(roleSlugs: string[] = []): boolean {
  return roleSlugs.some((role) => (BASIC_USER_ROLES as readonly string[]).includes(role));
}

export function canAccessPath(pathname: string, roleSlugs: string[] = []): boolean {
  if (!isBasicUserRole(roleSlugs)) {
    return true;
  }

  return pathname === '/'
    || pathname === ''
    || pathname === '/login'
    || pathname === '/signup';
}

export function remapLegacyOpsPath(pathname: string, search = '', hash = ''): string {
  if (pathname === LEGACY_OPS_BASE_PATH) {
    return `${OPS_BASE_PATH}${search}${hash}`;
  }

  if (pathname.startsWith(`${LEGACY_OPS_BASE_PATH}/`)) {
    return `${OPS_BASE_PATH}${pathname.slice(LEGACY_OPS_BASE_PATH.length)}${search}${hash}`;
  }

  return `${pathname}${search}${hash}`;
}
