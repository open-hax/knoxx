import pg from 'pg';

const { Pool } = pg;

const PERMISSIONS = [
  ['platform.org.create', 'platform', 'create', 'Create orgs across the Knoxx platform'],
  ['platform.org.read', 'platform', 'read', 'Read orgs across the Knoxx platform'],
  ['platform.org.update', 'platform', 'update', 'Update orgs across the Knoxx platform'],
  ['platform.org.delete', 'platform', 'delete', 'Delete orgs across the Knoxx platform'],
  ['platform.roles.manage', 'platform_roles', 'manage', 'Manage platform-scoped roles'],
  ['platform.audit.read', 'platform_audit', 'read', 'Read platform-wide audit events'],
  ['org.settings.read', 'org_settings', 'read', 'Read org settings'],
  ['org.settings.update', 'org_settings', 'update', 'Update org settings'],
  ['org.members.read', 'org_members', 'read', 'Read org memberships'],
  ['org.members.create', 'org_members', 'create', 'Create org memberships'],
  ['org.members.update', 'org_members', 'update', 'Update org memberships'],
  ['org.members.delete', 'org_members', 'delete', 'Delete org memberships'],
  ['org.users.invite', 'org_users', 'invite', 'Invite users into an org'],
  ['org.users.create', 'org_users', 'create', 'Create users inside an org'],
  ['org.users.read', 'org_users', 'read', 'Read users inside an org'],
  ['org.users.update', 'org_users', 'update', 'Update users inside an org'],
  ['org.users.disable', 'org_users', 'disable', 'Disable users inside an org'],
  ['org.roles.read', 'org_roles', 'read', 'Read org roles'],
  ['org.roles.create', 'org_roles', 'create', 'Create org roles'],
  ['org.roles.update', 'org_roles', 'update', 'Update org roles'],
  ['org.roles.delete', 'org_roles', 'delete', 'Delete org roles'],
  ['org.tool_policy.read', 'org_tool_policy', 'read', 'Read org tool policies'],
  ['org.tool_policy.update', 'org_tool_policy', 'update', 'Update org tool policies'],
  ['org.user_policy.read', 'org_user_policy', 'read', 'Read per-user policy overrides'],
  ['org.user_policy.update', 'org_user_policy', 'update', 'Update per-user policy overrides'],
  ['org.datalakes.read', 'org_datalakes', 'read', 'Read org data lakes'],
  ['org.datalakes.create', 'org_datalakes', 'create', 'Create org data lakes'],
  ['org.datalakes.update', 'org_datalakes', 'update', 'Update org data lakes'],
  ['org.datalakes.delete', 'org_datalakes', 'delete', 'Delete org data lakes'],
  ['datalake.read', 'datalake', 'read', 'Read a data lake'],
  ['datalake.query', 'datalake', 'query', 'Query a data lake'],
  ['datalake.write', 'datalake', 'write', 'Write to a data lake'],
  ['datalake.ingest', 'datalake', 'ingest', 'Ingest into a data lake'],
  ['datalake.admin', 'datalake', 'admin', 'Administer a data lake'],
  ['agent.chat.use', 'agent_chat', 'use', 'Use the Knoxx agent chat runtime'],
  ['agent.memory.read', 'agent_memory', 'read', 'Read prior Knoxx memory'],
  ['agent.memory.cross_session', 'agent_memory', 'cross_session', 'Search prior Knoxx sessions'],
  ['agent.runs.read_own', 'agent_runs', 'read_own', 'Read own Knoxx runs'],
  ['agent.runs.read_org', 'agent_runs', 'read_org', 'Read org Knoxx runs'],
  ['agent.runs.read_all', 'agent_runs', 'read_all', 'Read all Knoxx runs'],
  ['agent.controls.steer', 'agent_controls', 'steer', 'Steer a live Knoxx run'],
  ['agent.controls.follow_up', 'agent_controls', 'follow_up', 'Queue follow-up on a Knoxx run'],
  ['tool.read.use', 'tool', 'read', 'Use read tool'],
  ['tool.write.use', 'tool', 'write', 'Use write tool'],
  ['tool.edit.use', 'tool', 'edit', 'Use edit tool'],
  ['tool.bash.use', 'tool', 'bash', 'Use bash tool'],
  ['tool.email.send', 'tool', 'email_send', 'Send email from Knoxx'],
  ['tool.discord.publish', 'tool', 'discord_publish', 'Publish to Discord from Knoxx'],
  ['tool.discord.send', 'tool', 'discord_send', 'Send Discord messages and replies from Knoxx'],
  ['tool.discord.read', 'tool', 'discord_read', 'Read Discord channel messages from Knoxx'],
  ['tool.discord.channel.messages', 'tool', 'discord_channel_messages', 'Fetch Discord channel messages with cursors from Knoxx'],
  ['tool.discord.channel.scroll', 'tool', 'discord_channel_scroll', 'Scroll older Discord channel messages from Knoxx'],
  ['tool.discord.dm.messages', 'tool', 'discord_dm_messages', 'Fetch Discord DM messages from Knoxx'],
  ['tool.discord.search', 'tool', 'discord_search', 'Search Discord messages from Knoxx'],
  ['tool.discord.guilds', 'tool', 'discord_guilds', 'List Discord guilds from Knoxx'],
  ['tool.discord.channels', 'tool', 'discord_channels', 'List Discord channels from Knoxx'],
  ['tool.discord.list.servers', 'tool', 'discord_list_servers', 'List all Discord servers from Knoxx'],
  ['tool.discord.list.channels', 'tool', 'discord_list_channels', 'List Discord channels across guilds from Knoxx'],
  ['tool.event_agents.status', 'tool', 'event_agents_status', 'Inspect event-agent runtime state from Knoxx'],
  ['tool.event_agents.dispatch', 'tool', 'event_agents_dispatch', 'Dispatch event-agent events from Knoxx'],
  ['tool.event_agents.run_job', 'tool', 'event_agents_run_job', 'Trigger event-agent jobs from Knoxx'],
  ['tool.event_agents.upsert_job', 'tool', 'event_agents_upsert_job', 'Create/update event-agent jobs from Knoxx'],
  ['tool.schedule_event_agent', 'tool', 'schedule_event_agent', 'Schedule event-agent jobs from Knoxx'],
  ['tool.bluesky.publish', 'tool', 'bluesky_publish', 'Publish to Bluesky from Knoxx'],
  ['tool.semantic_query.use', 'tool', 'semantic_query', 'Use semantic query tool'],
  ['tool.memory_search.use', 'tool', 'memory_search', 'Use memory search tool'],
  ['tool.memory_session.use', 'tool', 'memory_session', 'Use memory session tool'],
  ['tool.websearch.use', 'tool', 'websearch', 'Use websearch tool'],
  ['tool.graph_query.use', 'tool', 'graph_query', 'Use graph query tool'],
  ['org.translations.read', 'org_translations', 'read', 'Read translation segments'],
  ['org.translations.review', 'org_translations', 'review', 'Review and label translations'],
  ['org.translations.export', 'org_translations', 'export', 'Export translation training data'],
  ['org.translations.manage', 'org_translations', 'manage', 'Manage translation pipeline config'],
  ['org.proxx.observability.read', 'org_proxx_observability', 'read', 'Read Proxx analytics and request logs'],
];

const TOOL_DEFINITIONS = [
  ['read', 'Read', 'Read files and retrieved context', 'low'],
  ['write', 'Write', 'Create new markdown drafts and artifacts', 'medium'],
  ['edit', 'Edit', 'Revise existing documents and drafts', 'medium'],
  ['bash', 'Shell', 'Run controlled shell commands', 'high'],
  ['canvas', 'Canvas', 'Open long-form markdown drafting canvas', 'low'],
  ['email.send', 'Email', 'Send drafts through configured email account', 'medium'],
  ['discord.publish', 'Discord Publish', 'Publish updates to Discord', 'medium'],
  ['discord.send', 'Discord Send', 'Send Discord messages and threaded replies', 'medium'],
  ['discord.read', 'Discord Read', 'Read messages from Discord channels', 'low'],
  ['discord.channel.messages', 'Discord Channel Messages', 'Fetch messages from a Discord channel with before/after/around cursors', 'low'],
  ['discord.channel.scroll', 'Discord Channel Scroll', 'Scroll older messages in a Discord channel', 'low'],
  ['discord.dm.messages', 'Discord DM Messages', 'Fetch messages from a Discord DM channel', 'low'],
  ['discord.search', 'Discord Search', 'Search messages in Discord channels', 'low'],
  ['discord.guilds', 'Discord Guilds', 'List Discord servers the bot is in', 'low'],
  ['discord.channels', 'Discord Channels', 'List channels in a Discord server', 'low'],
  ['discord.list.servers', 'Discord List Servers', 'List all Discord servers the bot can access', 'low'],
  ['discord.list.channels', 'Discord List Channels', 'List channels across one or all Discord servers', 'low'],
  ['event_agents.status', 'Event Agent Status', 'Inspect scheduled event-agent runtime state and configuration', 'low'],
  ['event_agents.dispatch', 'Event Agent Dispatch', 'Dispatch a structured event into the event-agent runtime', 'medium'],
  ['event_agents.run_job', 'Event Agent Run Job', 'Trigger a configured event-agent job immediately', 'medium'],
  ['event_agents.upsert_job', 'Event Agent Upsert Job', 'Create or update a scheduled event-agent job', 'high'],
  ['schedule_event_agent', 'Schedule Event Agent', 'Create or update a scheduled event-agent job with prompts, tools, triggers, and source config', 'high'],
  ['bluesky.publish', 'Bluesky', 'Publish updates to Bluesky', 'medium'],
  ['semantic_query', 'Semantic Query', 'Query semantic context in the active corpus', 'low'],
  ['memory_search', 'Memory Search', 'Search prior Knoxx sessions in OpenPlanner', 'low'],
  ['memory_session', 'Memory Session', 'Load a specific Knoxx session from OpenPlanner', 'low'],
  ['websearch', 'Web Search', 'Search the live web through Proxx websearch', 'low'],
  ['graph_query', 'Graph Query', 'Query the canonical knowledge graph', 'low'],
];

const PLATFORM_ROLE_SEEDS = [
  {
    slug: 'system_admin',
    name: 'System Admin',
    permissions: PERMISSIONS.map(([code]) => code),
    toolPolicies: TOOL_DEFINITIONS.map(([toolId]) => ({ toolId, effect: 'allow' })),
  },
];

const ORG_ROLE_SEEDS = [
  {
    slug: 'org_admin',
    name: 'Org Admin',
    permissions: [
      'org.settings.read',
      'org.settings.update',
      'org.members.read',
      'org.members.create',
      'org.members.update',
      'org.members.delete',
      'org.users.invite',
      'org.users.create',
      'org.users.read',
      'org.users.update',
      'org.users.disable',
      'org.roles.read',
      'org.roles.create',
      'org.roles.update',
      'org.roles.delete',
      'org.tool_policy.read',
      'org.tool_policy.update',
      'org.user_policy.read',
      'org.user_policy.update',
      'org.datalakes.read',
      'org.datalakes.create',
      'org.datalakes.update',
      'org.datalakes.delete',
      'datalake.read',
      'datalake.query',
      'datalake.write',
      'datalake.ingest',
      'datalake.admin',
      'agent.chat.use',
      'agent.memory.read',
      'agent.memory.cross_session',
      'agent.runs.read_org',
      'agent.controls.steer',
      'agent.controls.follow_up',
      'tool.read.use',
      'tool.write.use',
      'tool.edit.use',
      'tool.bash.use',
      'tool.email.send',
      'tool.discord.publish',
      'tool.discord.send',
      'tool.discord.read',
      'tool.discord.channel.messages',
      'tool.discord.channel.scroll',
      'tool.discord.dm.messages',
      'tool.discord.search',
      'tool.discord.guilds',
      'tool.discord.channels',
      'tool.discord.list.servers',
      'tool.discord.list.channels',
      'tool.event_agents.status',
      'tool.event_agents.dispatch',
      'tool.event_agents.run_job',
      'tool.event_agents.upsert_job',
      'tool.schedule_event_agent',
      'tool.bluesky.publish',
      'tool.semantic_query.use',
      'tool.memory_search.use',
      'tool.memory_session.use',
      'tool.websearch.use',
      'tool.graph_query.use',
      'org.translations.read',
      'org.translations.review',
      'org.translations.export',
      'org.translations.manage',
      'org.proxx.observability.read',
    ],
    toolPolicies: TOOL_DEFINITIONS.map(([toolId]) => ({ toolId, effect: 'allow' })),
  },
  {
    slug: 'knowledge_worker',
    name: 'Knowledge Worker',
    permissions: [
      'org.datalakes.read',
      'datalake.read',
      'datalake.query',
      'agent.chat.use',
      'agent.memory.read',
      'agent.runs.read_own',
      'agent.controls.steer',
      'agent.controls.follow_up',
      'tool.read.use',
      'tool.semantic_query.use',
      'tool.memory_search.use',
      'tool.memory_session.use',
    ],
    toolPolicies: [
      { toolId: 'read', effect: 'allow' },
      { toolId: 'canvas', effect: 'allow' },
      { toolId: 'semantic_query', effect: 'allow' },
      { toolId: 'memory_search', effect: 'allow' },
      { toolId: 'memory_session', effect: 'allow' },
    ],
  },
  {
    slug: 'data_analyst',
    name: 'Data Analyst',
    permissions: [
      'org.datalakes.read',
      'datalake.read',
      'datalake.query',
      'agent.chat.use',
      'agent.memory.read',
      'agent.memory.cross_session',
      'agent.runs.read_own',
      'tool.read.use',
      'tool.write.use',
      'tool.edit.use',
      'tool.semantic_query.use',
      'tool.memory_search.use',
      'tool.memory_session.use',
    ],
    toolPolicies: [
      { toolId: 'read', effect: 'allow' },
      { toolId: 'write', effect: 'allow' },
      { toolId: 'edit', effect: 'allow' },
      { toolId: 'canvas', effect: 'allow' },
      { toolId: 'semantic_query', effect: 'allow' },
      { toolId: 'memory_search', effect: 'allow' },
      { toolId: 'memory_session', effect: 'allow' },
    ],
  },
  {
    slug: 'developer',
    name: 'Developer',
    permissions: [
      'org.datalakes.read',
      'datalake.read',
      'datalake.query',
      'datalake.write',
      'datalake.ingest',
      'agent.chat.use',
      'agent.memory.read',
      'agent.memory.cross_session',
      'agent.runs.read_own',
      'tool.read.use',
      'tool.write.use',
      'tool.edit.use',
      'tool.bash.use',
      'tool.semantic_query.use',
      'tool.memory_search.use',
      'tool.memory_session.use',
    ],
    toolPolicies: [
      { toolId: 'read', effect: 'allow' },
      { toolId: 'write', effect: 'allow' },
      { toolId: 'edit', effect: 'allow' },
      { toolId: 'bash', effect: 'allow' },
      { toolId: 'canvas', effect: 'allow' },
      { toolId: 'semantic_query', effect: 'allow' },
      { toolId: 'memory_search', effect: 'allow' },
      { toolId: 'memory_session', effect: 'allow' },
    ],
  },
  {
    slug: 'translator',
    name: 'Translator',
    permissions: [
      'org.datalakes.read',
      'datalake.read',
      'agent.chat.use',
      'org.translations.read',
      'org.translations.review',
    ],
    toolPolicies: [
      { toolId: 'read', effect: 'allow' },
      { toolId: 'canvas', effect: 'allow' },
      { toolId: 'semantic_query', effect: 'allow' },
    ],
  },
];

function slugify(value, fallback = 'item') {
  const slug = String(value ?? '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return slug || fallback;
}

function unique(values) {
  return [...new Set(values.filter(Boolean))];
}

function normalizeToolPolicy(policy) {
  if (typeof policy === 'string') {
    return { toolId: policy, effect: 'allow', constraints: {} };
  }
  const toolId = policy?.toolId ?? policy?.tool_id ?? policy?.id;
  if (!toolId) {
    throw new Error('toolId is required for tool policy');
  }
  return {
    toolId: String(toolId),
    effect: policy?.effect === 'deny' ? 'deny' : 'allow',
    constraints: policy?.constraints ?? policy?.constraints_json ?? {},
  };
}

function normalizeLakeConfig(config) {
  if (!config || typeof config !== 'object' || Array.isArray(config)) {
    return {};
  }
  return config;
}

function httpError(statusCode, message, code = 'policy_error') {
  const error = new Error(message);
  error.statusCode = statusCode;
  error.code = code;
  return error;
}

function headerValue(headersLike, name) {
  if (!headersLike) return '';
  if (typeof headersLike.get === 'function') {
    return String(headersLike.get(name) ?? headersLike.get(name.toLowerCase()) ?? '').trim();
  }
  const direct = headersLike[name] ?? headersLike[name.toLowerCase()];
  return String(direct ?? '').trim();
}

function mergeToolPolicies(rolePolicies = [], membershipPolicies = []) {
  const merged = new Map();

  for (const policy of rolePolicies) {
    const normalized = normalizeToolPolicy(policy);
    const existing = merged.get(normalized.toolId);
    if (!existing || normalized.effect === 'deny' || existing.effect !== 'deny') {
      merged.set(normalized.toolId, normalized);
    }
  }

  for (const policy of membershipPolicies) {
    const normalized = normalizeToolPolicy(policy);
    merged.set(normalized.toolId, normalized);
  }

  return [...merged.values()].sort((a, b) => a.toolId.localeCompare(b.toolId));
}

function rolePriority(slug) {
  const priorities = {
    system_admin: 100,
    org_admin: 90,
    developer: 80,
    data_analyst: 70,
    knowledge_worker: 60,
  };
  return priorities[slug] ?? 0;
}

async function loadDetailedRoles(pool, roleIds) {
  if (!Array.isArray(roleIds) || roleIds.length === 0) return [];
  const { rows } = await pool.query('SELECT * FROM roles WHERE id = ANY($1::uuid[]) ORDER BY name ASC', [roleIds]);
  return hydrateRoleMaps(pool, rows);
}

async function findRequestMembershipRow(pool, headersLike) {
  const membershipId = headerValue(headersLike, 'x-knoxx-membership-id');
  const userEmail = headerValue(headersLike, 'x-knoxx-user-email').toLowerCase();
  const orgId = headerValue(headersLike, 'x-knoxx-org-id');
  const orgSlug = headerValue(headersLike, 'x-knoxx-org-slug').toLowerCase();

  if (!membershipId && !userEmail) {
    throw httpError(
      401,
      'Knoxx request context is missing x-knoxx-user-email or x-knoxx-membership-id',
      'request_context_missing',
    );
  }

  if (membershipId) {
    const { rows } = await pool.query(
      `SELECT m.*, u.email, u.display_name, u.status AS user_status,
              o.slug AS org_slug, o.name AS org_name, o.status AS org_status, o.is_primary, o.kind AS org_kind
         FROM memberships m
         JOIN users u ON u.id = m.user_id
         JOIN orgs o ON o.id = m.org_id
        WHERE m.id = $1::uuid`,
      [membershipId],
    );
    return rows[0] ?? null;
  }

  if (userEmail && (orgId || orgSlug)) {
    const { rows } = await pool.query(
      `SELECT m.*, u.email, u.display_name, u.status AS user_status,
              o.slug AS org_slug, o.name AS org_name, o.status AS org_status, o.is_primary, o.kind AS org_kind
         FROM memberships m
         JOIN users u ON u.id = m.user_id
         JOIN orgs o ON o.id = m.org_id
        WHERE lower(u.email) = $1
          AND (($2 <> '' AND o.id = $2::uuid) OR ($3 <> '' AND lower(o.slug) = $3))
        ORDER BY m.is_default DESC, m.created_at ASC
        LIMIT 1`,
      [userEmail, orgId, orgSlug],
    );
    return rows[0] ?? null;
  }

  const { rows } = await pool.query(
    `SELECT m.*, u.email, u.display_name, u.status AS user_status,
            o.slug AS org_slug, o.name AS org_name, o.status AS org_status, o.is_primary, o.kind AS org_kind
       FROM memberships m
       JOIN users u ON u.id = m.user_id
       JOIN orgs o ON o.id = m.org_id
      WHERE lower(u.email) = $1
      ORDER BY m.is_default DESC, o.is_primary DESC, m.created_at ASC
      LIMIT 1`,
    [userEmail],
  );
  return rows[0] ?? null;
}

async function buildRequestContext(pool, membershipRow) {
  if (!membershipRow) {
    throw httpError(401, 'Knoxx request context did not resolve to a membership', 'request_context_unresolved');
  }
  if (membershipRow.user_status !== 'active') {
    throw httpError(403, 'Knoxx user is not active', 'user_inactive');
  }
  if (membershipRow.status !== 'active') {
    throw httpError(403, 'Knoxx membership is not active', 'membership_inactive');
  }
  if (membershipRow.org_status !== 'active') {
    throw httpError(403, 'Knoxx org is not active', 'org_inactive');
  }

  const memberships = await hydrateMemberships(pool, [membershipRow]);
  const membership = memberships[0];
  const detailedRoles = await loadDetailedRoles(pool, membership.roles.map((role) => role.id));
  const permissions = unique(detailedRoles.flatMap((role) => role.permissions ?? [])).sort();
  const effectiveToolPolicies = mergeToolPolicies(
    detailedRoles.flatMap((role) => role.toolPolicies ?? []),
    membership.toolPolicies ?? [],
  );
  const roleSlugs = detailedRoles.map((role) => role.slug).sort((a, b) => rolePriority(b) - rolePriority(a) || a.localeCompare(b));

  return {
    user: {
      id: membershipRow.user_id,
      email: membershipRow.email,
      displayName: membershipRow.display_name,
      status: membershipRow.user_status,
    },
    org: {
      id: membershipRow.org_id,
      slug: membershipRow.org_slug,
      name: membershipRow.org_name,
      status: membershipRow.org_status,
      isPrimary: membershipRow.is_primary,
      kind: membershipRow.org_kind,
    },
    membership: {
      id: membership.id,
      status: membership.status,
      isDefault: membership.isDefault,
      createdAt: membership.createdAt,
      updatedAt: membership.updatedAt,
    },
    roles: detailedRoles,
    roleSlugs,
    permissions,
    toolPolicies: effectiveToolPolicies,
    membershipToolPolicies: membership.toolPolicies ?? [],
    isSystemAdmin: roleSlugs.includes('system_admin'),
  };
}

function toolAllowed(context, toolId) {
  const policy = (context?.toolPolicies ?? []).find((entry) => entry.toolId === toolId);
  return policy?.effect === 'allow';
}

async function ensureSchema(pool) {
  await pool.query(`
    CREATE EXTENSION IF NOT EXISTS pgcrypto;

    CREATE TABLE IF NOT EXISTS orgs (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      slug TEXT NOT NULL UNIQUE,
      name TEXT NOT NULL,
      kind TEXT NOT NULL DEFAULT 'customer',
      is_primary BOOLEAN NOT NULL DEFAULT FALSE,
      status TEXT NOT NULL DEFAULT 'active',
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS users (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      email TEXT NOT NULL UNIQUE,
      display_name TEXT NOT NULL,
      auth_provider TEXT NOT NULL DEFAULT 'bootstrap',
      external_subject TEXT,
      status TEXT NOT NULL DEFAULT 'active',
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS memberships (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
      status TEXT NOT NULL DEFAULT 'active',
      is_default BOOLEAN NOT NULL DEFAULT FALSE,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      UNIQUE (user_id, org_id)
    );

    CREATE TABLE IF NOT EXISTS roles (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      org_id UUID REFERENCES orgs(id) ON DELETE CASCADE,
      name TEXT NOT NULL,
      slug TEXT NOT NULL,
      scope_kind TEXT NOT NULL DEFAULT 'org',
      built_in BOOLEAN NOT NULL DEFAULT FALSE,
      system_managed BOOLEAN NOT NULL DEFAULT FALSE,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      CHECK (scope_kind IN ('platform', 'org'))
    );

    CREATE UNIQUE INDEX IF NOT EXISTS roles_platform_slug_uniq
      ON roles (slug)
      WHERE org_id IS NULL;

    CREATE UNIQUE INDEX IF NOT EXISTS roles_org_slug_uniq
      ON roles (org_id, slug)
      WHERE org_id IS NOT NULL;

    CREATE TABLE IF NOT EXISTS permissions (
      id BIGSERIAL PRIMARY KEY,
      code TEXT NOT NULL UNIQUE,
      resource_kind TEXT NOT NULL,
      action TEXT NOT NULL,
      description TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS role_permissions (
      role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
      permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
      effect TEXT NOT NULL DEFAULT 'allow',
      PRIMARY KEY (role_id, permission_id),
      CHECK (effect IN ('allow', 'deny'))
    );

    CREATE TABLE IF NOT EXISTS membership_roles (
      membership_id UUID NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,
      role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
      PRIMARY KEY (membership_id, role_id)
    );

    CREATE TABLE IF NOT EXISTS tool_definitions (
      id TEXT PRIMARY KEY,
      label TEXT NOT NULL,
      description TEXT NOT NULL,
      risk_level TEXT NOT NULL DEFAULT 'medium'
    );

    CREATE TABLE IF NOT EXISTS role_tool_policies (
      role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
      tool_id TEXT NOT NULL REFERENCES tool_definitions(id) ON DELETE CASCADE,
      effect TEXT NOT NULL DEFAULT 'allow',
      constraints_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      PRIMARY KEY (role_id, tool_id),
      CHECK (effect IN ('allow', 'deny'))
    );

    CREATE TABLE IF NOT EXISTS user_tool_policies (
      membership_id UUID NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,
      tool_id TEXT NOT NULL REFERENCES tool_definitions(id) ON DELETE CASCADE,
      effect TEXT NOT NULL DEFAULT 'allow',
      constraints_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      PRIMARY KEY (membership_id, tool_id),
      CHECK (effect IN ('allow', 'deny'))
    );

    CREATE TABLE IF NOT EXISTS data_lakes (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
      name TEXT NOT NULL,
      slug TEXT NOT NULL,
      kind TEXT NOT NULL DEFAULT 'workspace_docs',
      config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
      status TEXT NOT NULL DEFAULT 'active',
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      UNIQUE (org_id, slug)
    );

    CREATE TABLE IF NOT EXISTS audit_events (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
      actor_membership_id UUID REFERENCES memberships(id) ON DELETE SET NULL,
      org_id UUID REFERENCES orgs(id) ON DELETE SET NULL,
      action TEXT NOT NULL,
      resource_kind TEXT NOT NULL,
      resource_id TEXT,
      before_json JSONB,
      after_json JSONB,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE TABLE IF NOT EXISTS invites (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      code TEXT NOT NULL UNIQUE,
      org_id UUID NOT NULL REFERENCES orgs(id) ON DELETE CASCADE,
      inviter_membership_id UUID REFERENCES memberships(id) ON DELETE SET NULL,
      email TEXT NOT NULL,
      role_slugs TEXT[] NOT NULL DEFAULT '{"knowledge_worker"}',
      status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','accepted','revoked','expired')),
      max_uses INT NOT NULL DEFAULT 1,
      use_count INT NOT NULL DEFAULT 0,
      expires_at TIMESTAMPTZ,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

    CREATE INDEX IF NOT EXISTS idx_invites_code ON invites (code);
    CREATE INDEX IF NOT EXISTS idx_invites_email ON invites (email);
    CREATE INDEX IF NOT EXISTS idx_invites_org_id ON invites (org_id);
  `);
}

async function insertPermissionSeeds(pool) {
  for (const [code, resourceKind, action, description] of PERMISSIONS) {
    await pool.query(
      `INSERT INTO permissions (code, resource_kind, action, description)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (code) DO UPDATE
       SET resource_kind = EXCLUDED.resource_kind,
           action = EXCLUDED.action,
           description = EXCLUDED.description`,
      [code, resourceKind, action, description],
    );
  }
}

async function insertToolSeeds(pool) {
  for (const [id, label, description, riskLevel] of TOOL_DEFINITIONS) {
    await pool.query(
      `INSERT INTO tool_definitions (id, label, description, risk_level)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (id) DO UPDATE
       SET label = EXCLUDED.label,
           description = EXCLUDED.description,
           risk_level = EXCLUDED.risk_level`,
      [id, label, description, riskLevel],
    );
  }
}

async function ensurePrimaryOrg(pool, options) {
  const slug = slugify(options.primaryOrgSlug ?? 'open-hax', 'open-hax');
  const name = String(options.primaryOrgName ?? 'Open Hax');
  const kind = String(options.primaryOrgKind ?? 'platform_owner');
  const { rows } = await pool.query(
    `INSERT INTO orgs (slug, name, kind, is_primary, status)
     VALUES ($1, $2, $3, TRUE, 'active')
     ON CONFLICT (slug) DO UPDATE
     SET name = EXCLUDED.name,
         kind = EXCLUDED.kind,
         is_primary = TRUE,
         updated_at = NOW()
     RETURNING *`,
    [slug, name, kind],
  );
  await pool.query(`UPDATE orgs SET is_primary = CASE WHEN slug = $1 THEN TRUE ELSE FALSE END`, [slug]);
  return rows[0];
}

async function findRole(pool, { orgId = null, slug }) {
  const { rows } = await pool.query(
    `SELECT *
       FROM roles
      WHERE slug = $1
        AND (($2::uuid IS NULL AND org_id IS NULL) OR org_id = $2::uuid)
      LIMIT 1`,
    [slug, orgId],
  );
  return rows[0] ?? null;
}

async function ensureRole(pool, { orgId = null, name, slug, scopeKind, builtIn = false, systemManaged = false }) {
  const existing = await findRole(pool, { orgId, slug });
  if (existing) {
    const { rows } = await pool.query(
      `UPDATE roles
          SET name = $2,
              scope_kind = $3,
              built_in = $4,
              system_managed = $5,
              updated_at = NOW()
        WHERE id = $1
      RETURNING *`,
      [existing.id, name, scopeKind, builtIn, systemManaged],
    );
    return rows[0];
  }
  const { rows } = await pool.query(
    `INSERT INTO roles (org_id, name, slug, scope_kind, built_in, system_managed)
     VALUES ($1, $2, $3, $4, $5, $6)
     RETURNING *`,
    [orgId, name, slug, scopeKind, builtIn, systemManaged],
  );
  return rows[0];
}

async function setRolePermissions(pool, roleId, permissionCodes) {
  const codes = unique(permissionCodes);
  await pool.query('DELETE FROM role_permissions WHERE role_id = $1', [roleId]);
  if (codes.length === 0) return;
  const { rows } = await pool.query(
    'SELECT id, code FROM permissions WHERE code = ANY($1::text[])',
    [codes],
  );
  const found = new Set(rows.map((row) => row.code));
  const missing = codes.filter((code) => !found.has(code));
  if (missing.length > 0) {
    throw new Error(`Unknown permission codes: ${missing.join(', ')}`);
  }
  for (const row of rows) {
    await pool.query(
      `INSERT INTO role_permissions (role_id, permission_id, effect)
       VALUES ($1, $2, 'allow')
       ON CONFLICT (role_id, permission_id) DO UPDATE SET effect = EXCLUDED.effect`,
      [roleId, row.id],
    );
  }
}

async function setRoleToolPolicies(pool, roleId, toolPolicies) {
  const normalized = toolPolicies.map(normalizeToolPolicy);
  await pool.query('DELETE FROM role_tool_policies WHERE role_id = $1', [roleId]);
  for (const policy of normalized) {
    await pool.query(
      `INSERT INTO role_tool_policies (role_id, tool_id, effect, constraints_json)
       VALUES ($1, $2, $3, $4::jsonb)
       ON CONFLICT (role_id, tool_id) DO UPDATE
       SET effect = EXCLUDED.effect,
           constraints_json = EXCLUDED.constraints_json`,
      [roleId, policy.toolId, policy.effect, JSON.stringify(policy.constraints ?? {})],
    );
  }
}

async function setMembershipToolPolicies(pool, membershipId, toolPolicies) {
  const normalized = toolPolicies.map(normalizeToolPolicy);
  await pool.query('DELETE FROM user_tool_policies WHERE membership_id = $1', [membershipId]);
  for (const policy of normalized) {
    await pool.query(
      `INSERT INTO user_tool_policies (membership_id, tool_id, effect, constraints_json)
       VALUES ($1, $2, $3, $4::jsonb)
       ON CONFLICT (membership_id, tool_id) DO UPDATE
       SET effect = EXCLUDED.effect,
           constraints_json = EXCLUDED.constraints_json`,
      [membershipId, policy.toolId, policy.effect, JSON.stringify(policy.constraints ?? {})],
    );
  }
}

async function ensureBuiltinOrgRoles(pool, org) {
  for (const seed of ORG_ROLE_SEEDS) {
    const role = await ensureRole(pool, {
      orgId: org.id,
      name: seed.name,
      slug: seed.slug,
      scopeKind: 'org',
      builtIn: true,
      systemManaged: true,
    });
    await setRolePermissions(pool, role.id, seed.permissions);
    await setRoleToolPolicies(pool, role.id, seed.toolPolicies);
  }
}

async function ensureBuiltinPlatformRoles(pool) {
  for (const seed of PLATFORM_ROLE_SEEDS) {
    const role = await ensureRole(pool, {
      orgId: null,
      name: seed.name,
      slug: seed.slug,
      scopeKind: 'platform',
      builtIn: true,
      systemManaged: true,
    });
    await setRolePermissions(pool, role.id, seed.permissions);
    await setRoleToolPolicies(pool, role.id, seed.toolPolicies);
  }
}

async function resolveRoleIds(pool, { orgId, roleIds = [], roleSlugs = [] }) {
  const resolvedIds = new Set(roleIds.map(String));
  for (const value of roleSlugs.filter(Boolean)) {
    const rawSlug = String(value).trim();
    const normalizedSlug = slugify(rawSlug);
    const { rows } = await pool.query(
      `SELECT id
         FROM roles
        WHERE (slug = $1 OR slug = $2)
          AND (org_id = $3::uuid OR org_id IS NULL)
        ORDER BY CASE WHEN org_id IS NULL THEN 1 ELSE 0 END, created_at ASC
        LIMIT 1`,
      [rawSlug, normalizedSlug, orgId],
    );
    if (!rows[0]) {
      throw new Error(`Role not found for slug '${rawSlug}'`);
    }
    resolvedIds.add(rows[0].id);
  }
  return [...resolvedIds];
}

async function setMembershipRoles(pool, membershipId, { orgId, roleIds = [], roleSlugs = [], replace = true }) {
  const resolvedRoleIds = await resolveRoleIds(pool, { orgId, roleIds, roleSlugs });
  if (replace) {
    await pool.query('DELETE FROM membership_roles WHERE membership_id = $1', [membershipId]);
  }
  for (const roleId of resolvedRoleIds) {
    await pool.query(
      `INSERT INTO membership_roles (membership_id, role_id)
       VALUES ($1, $2)
       ON CONFLICT (membership_id, role_id) DO NOTHING`,
      [membershipId, roleId],
    );
  }
  return resolvedRoleIds;
}

async function ensureBootstrapUser(pool, primaryOrg, options) {
  const email = String(options.bootstrapSystemAdminEmail ?? 'system-admin@open-hax.local').toLowerCase();
  const displayName = String(options.bootstrapSystemAdminName ?? 'Knoxx System Admin');
  const userResp = await pool.query(
    `INSERT INTO users (email, display_name, auth_provider, status)
     VALUES ($1, $2, 'bootstrap', 'active')
     ON CONFLICT (email) DO UPDATE
     SET display_name = EXCLUDED.display_name,
         updated_at = NOW()
     RETURNING *`,
    [email, displayName],
  );
  const user = userResp.rows[0];
  const membershipResp = await pool.query(
    `INSERT INTO memberships (user_id, org_id, status, is_default)
     VALUES ($1, $2, 'active', TRUE)
     ON CONFLICT (user_id, org_id) DO UPDATE
     SET is_default = TRUE,
         updated_at = NOW()
     RETURNING *`,
    [user.id, primaryOrg.id],
  );
  const membership = membershipResp.rows[0];
  const systemAdmin = await findRole(pool, { slug: 'system_admin', orgId: null });
  if (systemAdmin) {
    await pool.query(
      `INSERT INTO membership_roles (membership_id, role_id)
       VALUES ($1, $2)
       ON CONFLICT (membership_id, role_id) DO NOTHING`,
      [membership.id, systemAdmin.id],
    );
  }
  return { user, membership };
}

async function appendAudit(pool, { actorUserId = null, actorMembershipId = null, orgId = null, action, resourceKind, resourceId = null, before = null, after = null }) {
  await pool.query(
    `INSERT INTO audit_events (actor_user_id, actor_membership_id, org_id, action, resource_kind, resource_id, before_json, after_json)
     VALUES ($1, $2, $3, $4, $5, $6, $7::jsonb, $8::jsonb)`,
    [
      actorUserId,
      actorMembershipId,
      orgId,
      action,
      resourceKind,
      resourceId,
      before ? JSON.stringify(before) : null,
      after ? JSON.stringify(after) : null,
    ],
  );
}

async function hydrateRoleMaps(pool, roles) {
  if (roles.length === 0) return [];
  const roleIds = roles.map((role) => role.id);
  const [permissionResp, toolResp] = await Promise.all([
    pool.query(
      `SELECT rp.role_id, p.code
         FROM role_permissions rp
         JOIN permissions p ON p.id = rp.permission_id
        WHERE rp.role_id = ANY($1::uuid[])
        ORDER BY p.code ASC`,
      [roleIds],
    ),
    pool.query(
      `SELECT rtp.role_id, rtp.tool_id, rtp.effect, rtp.constraints_json
         FROM role_tool_policies rtp
        WHERE rtp.role_id = ANY($1::uuid[])
        ORDER BY rtp.tool_id ASC`,
      [roleIds],
    ),
  ]);

  const permissionMap = new Map();
  for (const row of permissionResp.rows) {
    if (!permissionMap.has(row.role_id)) permissionMap.set(row.role_id, []);
    permissionMap.get(row.role_id).push(row.code);
  }

  const toolMap = new Map();
  for (const row of toolResp.rows) {
    if (!toolMap.has(row.role_id)) toolMap.set(row.role_id, []);
    toolMap.get(row.role_id).push({
      toolId: row.tool_id,
      effect: row.effect,
      constraints: row.constraints_json ?? {},
    });
  }

  return roles.map((role) => ({
    id: role.id,
    orgId: role.org_id,
    name: role.name,
    slug: role.slug,
    scopeKind: role.scope_kind,
    builtIn: role.built_in,
    systemManaged: role.system_managed,
    createdAt: role.created_at,
    updatedAt: role.updated_at,
    permissions: permissionMap.get(role.id) ?? [],
    toolPolicies: toolMap.get(role.id) ?? [],
  }));
}

async function hydrateMemberships(pool, memberships) {
  if (memberships.length === 0) return [];
  const membershipIds = memberships.map((membership) => membership.id);
  const [roleResp, toolResp] = await Promise.all([
    pool.query(
      `SELECT mr.membership_id, r.id AS role_id, r.slug, r.name, r.scope_kind, r.org_id
         FROM membership_roles mr
         JOIN roles r ON r.id = mr.role_id
        WHERE mr.membership_id = ANY($1::uuid[])
        ORDER BY r.name ASC`,
      [membershipIds],
    ),
    pool.query(
      `SELECT membership_id, tool_id, effect, constraints_json
         FROM user_tool_policies
        WHERE membership_id = ANY($1::uuid[])
        ORDER BY tool_id ASC`,
      [membershipIds],
    ),
  ]);

  const rolesByMembership = new Map();
  for (const row of roleResp.rows) {
    if (!rolesByMembership.has(row.membership_id)) rolesByMembership.set(row.membership_id, []);
    rolesByMembership.get(row.membership_id).push({
      id: row.role_id,
      slug: row.slug,
      name: row.name,
      scopeKind: row.scope_kind,
      orgId: row.org_id,
    });
  }

  const toolsByMembership = new Map();
  for (const row of toolResp.rows) {
    if (!toolsByMembership.has(row.membership_id)) toolsByMembership.set(row.membership_id, []);
    toolsByMembership.get(row.membership_id).push({
      toolId: row.tool_id,
      effect: row.effect,
      constraints: row.constraints_json ?? {},
    });
  }

  return memberships.map((membership) => ({
    id: membership.id,
    orgId: membership.org_id,
    orgName: membership.org_name,
    orgSlug: membership.org_slug,
    status: membership.status,
    isDefault: membership.is_default,
    createdAt: membership.created_at,
    updatedAt: membership.updated_at,
    roles: rolesByMembership.get(membership.id) ?? [],
    toolPolicies: toolsByMembership.get(membership.id) ?? [],
  }));
}

export async function createPolicyDb(options = {}) {
  const connectionString = options.connectionString || '';
  if (!connectionString) {
    return null;
  }

  const pool = new Pool({ connectionString });
  await ensureSchema(pool);
  await insertPermissionSeeds(pool);
  await insertToolSeeds(pool);
  const primaryOrg = await ensurePrimaryOrg(pool, options);
  await ensureBuiltinPlatformRoles(pool);
  await ensureBuiltinOrgRoles(pool, primaryOrg);
  const bootstrap = await ensureBootstrapUser(pool, primaryOrg, options);

  return {
    async close() {
      await pool.end();
    },

    async resolveRequestContext(headersLike = {}) {
      const membershipRow = await findRequestMembershipRow(pool, headersLike);
      return buildRequestContext(pool, membershipRow);
    },

    async evaluateToolAccess(headersLike = {}, toolId) {
      const context = await this.resolveRequestContext(headersLike);
      return {
        context,
        toolId,
        allowed: toolAllowed(context, toolId),
      };
    },

    async listPermissions() {
      const { rows } = await pool.query('SELECT id, code, resource_kind, action, description FROM permissions ORDER BY code ASC');
      return {
        permissions: rows.map((row) => ({
          id: row.id,
          code: row.code,
          resourceKind: row.resource_kind,
          action: row.action,
          description: row.description,
        })),
      };
    },

    async listTools() {
      const { rows } = await pool.query(
        'SELECT id, label, description, risk_level FROM tool_definitions ORDER BY id ASC',
      );
      return {
        tools: rows.map((row) => ({
          id: row.id,
          label: row.label,
          description: row.description,
          riskLevel: row.risk_level,
        })),
      };
    },

    async getBootstrapContext() {
      return {
        primaryOrg: {
          id: primaryOrg.id,
          slug: primaryOrg.slug,
          name: primaryOrg.name,
          kind: primaryOrg.kind,
          isPrimary: primaryOrg.is_primary,
          status: primaryOrg.status,
        },
        bootstrapUser: {
          id: bootstrap.user.id,
          email: bootstrap.user.email,
          displayName: bootstrap.user.display_name,
          membershipId: bootstrap.membership.id,
        },
      };
    },

    async listOrgs() {
      const { rows } = await pool.query(`
        SELECT o.*,
               COUNT(DISTINCT m.id) AS member_count,
               COUNT(DISTINCT r.id) FILTER (WHERE r.org_id = o.id) AS role_count,
               COUNT(DISTINCT d.id) AS data_lake_count
          FROM orgs o
          LEFT JOIN memberships m ON m.org_id = o.id
          LEFT JOIN roles r ON r.org_id = o.id
          LEFT JOIN data_lakes d ON d.org_id = o.id
         GROUP BY o.id
         ORDER BY o.is_primary DESC, o.name ASC
      `);
      return {
        orgs: rows.map((row) => ({
          id: row.id,
          slug: row.slug,
          name: row.name,
          kind: row.kind,
          isPrimary: row.is_primary,
          status: row.status,
          memberCount: Number(row.member_count ?? 0),
          roleCount: Number(row.role_count ?? 0),
          dataLakeCount: Number(row.data_lake_count ?? 0),
          createdAt: row.created_at,
          updatedAt: row.updated_at,
        })),
      };
    },

    async createOrg(payload = {}) {
      const name = String(payload.name ?? '').trim();
      if (!name) throw new Error('name is required');
      const slug = slugify(payload.slug ?? name, 'org');
      const kind = String(payload.kind ?? 'customer');
      const status = String(payload.status ?? 'active');
      const { rows } = await pool.query(
        `INSERT INTO orgs (slug, name, kind, is_primary, status)
         VALUES ($1, $2, $3, FALSE, $4)
         RETURNING *`,
        [slug, name, kind, status],
      );
      const org = rows[0];
      await ensureBuiltinOrgRoles(pool, org);
      await appendAudit(pool, {
        actorUserId: bootstrap.user.id,
        actorMembershipId: bootstrap.membership.id,
        orgId: org.id,
        action: 'org.create',
        resourceKind: 'org',
        resourceId: org.id,
        after: org,
      });
      return {
        org: {
          id: org.id,
          slug: org.slug,
          name: org.name,
          kind: org.kind,
          isPrimary: org.is_primary,
          status: org.status,
          createdAt: org.created_at,
          updatedAt: org.updated_at,
        },
      };
    },

    async listRoles({ orgId } = {}) {
      const params = [];
      let where = '';
      if (orgId) {
        params.push(orgId);
        where = 'WHERE org_id = $1';
      }
      const { rows } = await pool.query(
        `SELECT * FROM roles ${where} ORDER BY built_in DESC, name ASC`,
        params,
      );
      return { roles: await hydrateRoleMaps(pool, rows) };
    },

    async getRole(roleId) {
      const { rows } = await pool.query('SELECT * FROM roles WHERE id = $1::uuid', [roleId]);
      const hydrated = await hydrateRoleMaps(pool, rows);
      return { role: hydrated[0] ?? null };
    },

    async createRole(payload = {}) {
      const orgId = String(payload.orgId ?? '').trim();
      const name = String(payload.name ?? '').trim();
      if (!orgId) throw new Error('orgId is required');
      if (!name) throw new Error('name is required');
      const slug = slugify(payload.slug ?? name, 'role');
      const role = await ensureRole(pool, {
        orgId,
        name,
        slug,
        scopeKind: 'org',
        builtIn: false,
        systemManaged: false,
      });
      await setRolePermissions(pool, role.id, payload.permissionCodes ?? payload.permissions ?? []);
      await setRoleToolPolicies(pool, role.id, payload.toolPolicies ?? []);
      await appendAudit(pool, {
        actorUserId: bootstrap.user.id,
        actorMembershipId: bootstrap.membership.id,
        orgId,
        action: 'role.create',
        resourceKind: 'role',
        resourceId: role.id,
      });
      const hydrated = await hydrateRoleMaps(pool, [role]);
      return { role: hydrated[0] ?? null };
    },

    async setRoleToolPolicies(roleId, payload = {}) {
      const roleResp = await pool.query('SELECT * FROM roles WHERE id = $1', [roleId]);
      const role = roleResp.rows[0];
      if (!role) throw new Error('role not found');
      await setRoleToolPolicies(pool, roleId, payload.toolPolicies ?? []);
      await appendAudit(pool, {
        actorUserId: bootstrap.user.id,
        actorMembershipId: bootstrap.membership.id,
        orgId: role.org_id,
        action: 'role.tool_policy.update',
        resourceKind: 'role',
        resourceId: roleId,
      });
      const hydrated = await hydrateRoleMaps(pool, [role]);
      return { role: hydrated[0] ?? null };
    },

    async listUsers({ orgId = null } = {}) {
      const userResp = orgId
        ? await pool.query(
            `SELECT DISTINCT u.*
               FROM users u
               JOIN memberships m ON m.user_id = u.id
              WHERE m.org_id = $1::uuid
              ORDER BY u.display_name ASC, u.email ASC`,
            [orgId],
          )
        : await pool.query('SELECT * FROM users ORDER BY display_name ASC, email ASC');
      const users = userResp.rows;
      if (users.length === 0) return { users: [] };
      const userIds = users.map((user) => user.id);
      const membershipResp = orgId
        ? await pool.query(
            `SELECT m.*, o.name AS org_name, o.slug AS org_slug
               FROM memberships m
               JOIN orgs o ON o.id = m.org_id
              WHERE m.user_id = ANY($1::uuid[])
                AND m.org_id = $2::uuid
              ORDER BY m.created_at ASC`,
            [userIds, orgId],
          )
        : await pool.query(
            `SELECT m.*, o.name AS org_name, o.slug AS org_slug
               FROM memberships m
               JOIN orgs o ON o.id = m.org_id
              WHERE m.user_id = ANY($1::uuid[])
              ORDER BY m.created_at ASC`,
            [userIds],
          );
      const memberships = await hydrateMemberships(pool, membershipResp.rows);
      const membershipMap = new Map();
      for (const membership of memberships) {
        if (!membershipMap.has(membership.id)) membershipMap.set(membership.id, membership);
      }
      const groupedMemberships = new Map();
      for (const row of membershipResp.rows) {
        if (!groupedMemberships.has(row.user_id)) groupedMemberships.set(row.user_id, []);
        groupedMemberships.get(row.user_id).push(membershipMap.get(row.id));
      }
      return {
        users: users.map((user) => ({
          id: user.id,
          email: user.email,
          displayName: user.display_name,
          authProvider: user.auth_provider,
          externalSubject: user.external_subject,
          status: user.status,
          createdAt: user.created_at,
          updatedAt: user.updated_at,
          memberships: groupedMemberships.get(user.id) ?? [],
        })),
      };
    },

    async createUser(payload = {}) {
      const email = String(payload.email ?? '').trim().toLowerCase();
      const displayName = String(payload.displayName ?? payload.display_name ?? email).trim();
      const orgId = String(payload.orgId ?? payload.org_id ?? '').trim();
      if (!email) throw new Error('email is required');
      if (!orgId) throw new Error('orgId is required');
      const userResp = await pool.query(
        `INSERT INTO users (email, display_name, auth_provider, external_subject, status)
         VALUES ($1, $2, $3, $4, $5)
         ON CONFLICT (email) DO UPDATE
         SET display_name = EXCLUDED.display_name,
             auth_provider = EXCLUDED.auth_provider,
             external_subject = EXCLUDED.external_subject,
             status = EXCLUDED.status,
             updated_at = NOW()
         RETURNING *`,
        [
          email,
          displayName || email,
          String(payload.authProvider ?? payload.auth_provider ?? 'local'),
          payload.externalSubject ?? payload.external_subject ?? null,
          String(payload.status ?? 'active'),
        ],
      );
      const user = userResp.rows[0];
      const membershipResp = await pool.query(
        `INSERT INTO memberships (user_id, org_id, status, is_default)
         VALUES ($1, $2, $3, $4)
         ON CONFLICT (user_id, org_id) DO UPDATE
         SET status = EXCLUDED.status,
             is_default = EXCLUDED.is_default,
             updated_at = NOW()
         RETURNING *`,
        [user.id, orgId, String(payload.membershipStatus ?? payload.membership_status ?? 'active'), payload.isDefault !== false],
      );
      const membership = membershipResp.rows[0];
      await setMembershipRoles(pool, membership.id, {
        orgId,
        roleIds: payload.roleIds ?? payload.role_ids ?? [],
        roleSlugs: payload.roleSlugs ?? payload.role_slugs ?? ['knowledge_worker'],
        replace: true,
      });
      if (Array.isArray(payload.toolPolicies)) {
        await setMembershipToolPolicies(pool, membership.id, payload.toolPolicies);
      }
      await appendAudit(pool, {
        actorUserId: bootstrap.user.id,
        actorMembershipId: bootstrap.membership.id,
        orgId,
        action: 'user.create_or_update',
        resourceKind: 'user',
        resourceId: user.id,
      });
      const result = await this.listUsers({ orgId });
      const hydrated = result.users.find((entry) => entry.id === user.id) ?? null;
      return { user: hydrated };
    },

    async listMemberships({ orgId }) {
      if (!orgId) throw new Error('orgId is required');
      const { rows } = await pool.query(
        `SELECT m.*, o.name AS org_name, o.slug AS org_slug
           FROM memberships m
           JOIN orgs o ON o.id = m.org_id
          WHERE m.org_id = $1::uuid
          ORDER BY m.created_at ASC`,
        [orgId],
      );
      return { memberships: await hydrateMemberships(pool, rows) };
    },

    async getMembership(membershipId) {
      const { rows } = await pool.query(
        `SELECT m.*, o.name AS org_name, o.slug AS org_slug
           FROM memberships m
           JOIN orgs o ON o.id = m.org_id
          WHERE m.id = $1::uuid`,
        [membershipId],
      );
      const memberships = await hydrateMemberships(pool, rows);
      return { membership: memberships[0] ?? null };
    },

    async setMembershipRoles(membershipId, payload = {}) {
      const membershipResp = await pool.query('SELECT * FROM memberships WHERE id = $1', [membershipId]);
      const membership = membershipResp.rows[0];
      if (!membership) throw new Error('membership not found');
      await setMembershipRoles(pool, membershipId, {
        orgId: membership.org_id,
        roleIds: payload.roleIds ?? payload.role_ids ?? [],
        roleSlugs: payload.roleSlugs ?? payload.role_slugs ?? [],
        replace: payload.replace !== false,
      });
      await appendAudit(pool, {
        actorUserId: bootstrap.user.id,
        actorMembershipId: bootstrap.membership.id,
        orgId: membership.org_id,
        action: 'membership.roles.update',
        resourceKind: 'membership',
        resourceId: membershipId,
      });
      const result = await this.listMemberships({ orgId: membership.org_id });
      return { membership: result.memberships.find((entry) => entry.id === membershipId) ?? null };
    },

    async setMembershipToolPolicies(membershipId, payload = {}) {
      const membershipResp = await pool.query('SELECT * FROM memberships WHERE id = $1', [membershipId]);
      const membership = membershipResp.rows[0];
      if (!membership) throw new Error('membership not found');
      await setMembershipToolPolicies(pool, membershipId, payload.toolPolicies ?? []);
      await appendAudit(pool, {
        actorUserId: bootstrap.user.id,
        actorMembershipId: bootstrap.membership.id,
        orgId: membership.org_id,
        action: 'membership.tool_policy.update',
        resourceKind: 'membership',
        resourceId: membershipId,
      });
      const result = await this.listMemberships({ orgId: membership.org_id });
      return { membership: result.memberships.find((entry) => entry.id === membershipId) ?? null };
    },

    async listDataLakes({ orgId }) {
      if (!orgId) throw new Error('orgId is required');
      const { rows } = await pool.query(
        `SELECT *
           FROM data_lakes
          WHERE org_id = $1::uuid
          ORDER BY name ASC`,
        [orgId],
      );
      return {
        dataLakes: rows.map((row) => ({
          id: row.id,
          orgId: row.org_id,
          name: row.name,
          slug: row.slug,
          kind: row.kind,
          config: row.config_json ?? {},
          status: row.status,
          createdAt: row.created_at,
          updatedAt: row.updated_at,
        })),
      };
    },

    async createDataLake(payload = {}) {
      const orgId = String(payload.orgId ?? payload.org_id ?? '').trim();
      const name = String(payload.name ?? '').trim();
      if (!orgId) throw new Error('orgId is required');
      if (!name) throw new Error('name is required');
      const slug = slugify(payload.slug ?? name, 'lake');
      const kind = String(payload.kind ?? 'workspace_docs');
      const status = String(payload.status ?? 'active');
      const configJson = normalizeLakeConfig(payload.config ?? payload.config_json ?? {});
      const { rows } = await pool.query(
        `INSERT INTO data_lakes (org_id, name, slug, kind, config_json, status)
         VALUES ($1, $2, $3, $4, $5::jsonb, $6)
         RETURNING *`,
        [orgId, name, slug, kind, JSON.stringify(configJson), status],
      );
      const lake = rows[0];
      await appendAudit(pool, {
        actorUserId: bootstrap.user.id,
        actorMembershipId: bootstrap.membership.id,
        orgId,
        action: 'data_lake.create',
        resourceKind: 'data_lake',
        resourceId: lake.id,
      });
      return {
        dataLake: {
          id: lake.id,
          orgId: lake.org_id,
          name: lake.name,
          slug: lake.slug,
          kind: lake.kind,
          config: lake.config_json ?? {},
          status: lake.status,
          createdAt: lake.created_at,
          updatedAt: lake.updated_at,
        },
      };
    },

    // -----------------------------------------------------------------
    // Invite system
    // -----------------------------------------------------------------

    async createInvite(payload = {}) {
      const orgId = String(payload.orgId ?? payload.org_id ?? '').trim();
      const email = String(payload.email ?? '').trim().toLowerCase();
      if (!orgId) throw new Error('orgId is required');
      if (!email) throw new Error('email is required');
      const code = payload.code ?? crypto.randomUUID().replace(/-/g, '');
      const roleSlugs = payload.roleSlugs ?? payload.role_slugs ?? ['knowledge_worker'];
      const maxUses = payload.maxUses ?? payload.max_uses ?? 1;
      const expiresAt = payload.expiresAt ?? payload.expires_at ?? new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(); // 7 days default
      const { rows } = await pool.query(
        `INSERT INTO invites (code, org_id, inviter_membership_id, email, role_slugs, max_uses, expires_at)
         VALUES ($1, $2, $3, $4, $5, $6, $7)
         RETURNING *`,
        [code, orgId, payload.inviterMembershipId ?? payload.inviter_membership_id ?? null, email, roleSlugs, maxUses, expiresAt],
      );
      const inv = rows[0];
      await appendAudit(pool, {
        actorUserId: bootstrap.user.id,
        actorMembershipId: bootstrap.membership.id,
        orgId,
        action: 'invite.create',
        resourceKind: 'invite',
        resourceId: inv.id,
      });
      return { invite: hydrateInvite(inv) };
    },

    async listInvites({ orgId, status } = {}) {
      if (!orgId) throw new Error('orgId is required');
      let sql = `SELECT * FROM invites WHERE org_id = $1`;
      const params = [orgId];
      if (status) {
        sql += ` AND status = $2`;
        params.push(status);
      }
      sql += ` ORDER BY created_at DESC`;
      const { rows } = await pool.query(sql, params);
      return { invites: rows.map(hydrateInvite) };
    },

    async getInviteByCode(code) {
      const { rows } = await pool.query(`SELECT * FROM invites WHERE code = $1`, [code]);
      return { invite: rows[0] ? hydrateInvite(rows[0]) : null };
    },

    async redeemInvite(code, userEmail) {
      const email = String(userEmail).trim().toLowerCase();
      const { rows } = await pool.query(`SELECT * FROM invites WHERE code = $1`, [code]);
      const inv = rows[0];
      if (!inv) throw httpError(404, 'Invite not found', 'invite_not_found');
      if (inv.status !== 'pending') throw httpError(410, `Invite is ${inv.status}`, 'invite_' + inv.status);
      if (inv.expires_at && new Date(inv.expires_at) < new Date()) {
        await pool.query(`UPDATE invites SET status = 'expired', updated_at = NOW() WHERE id = $1`, [inv.id]);
        throw httpError(410, 'Invite has expired', 'invite_expired');
      }
      if (inv.use_count >= inv.max_uses) throw httpError(410, 'Invite has no remaining uses', 'invite_exhausted');
      // Auto-provision the user with the invite's org + role_slugs
      const created = await this.createUser({
        email,
        orgId: inv.org_id,
        roleSlugs: inv.role_slugs,
        membershipStatus: 'active',
      });
      await pool.query(
        `UPDATE invites SET use_count = use_count + 1, status = CASE WHEN use_count + 1 >= max_uses THEN 'accepted' ELSE status END, updated_at = NOW() WHERE id = $1`,
        [inv.id],
      );
      await appendAudit(pool, {
        actorUserId: created.user.id,
        orgId: inv.org_id,
        action: 'invite.redeem',
        resourceKind: 'invite',
        resourceId: inv.id,
      });
      return { invite: hydrateInvite(inv), user: created.user };
    },

    async revokeInvite(inviteId) {
      const { rows } = await pool.query(
        `UPDATE invites SET status = 'revoked', updated_at = NOW() WHERE id = $1 RETURNING *`,
        [inviteId],
      );
      if (!rows[0]) throw httpError(404, 'Invite not found', 'invite_not_found');
      return { invite: hydrateInvite(rows[0]) };
    },

    async isEmailWhitelisted(email) {
      const lc = String(email).trim().toLowerCase();
      // Bootstrap system admin is always whitelisted
      if (lc === String(bootstrap.user.email).toLowerCase()) return true;
      // Check if user already exists in the policy DB
      const { rows } = await pool.query(`SELECT id FROM users WHERE email = $1 AND status = 'active'`, [lc]);
      return rows.length > 0;
    },
  };
}

function hydrateInvite(row) {
  return {
    id: row.id,
    code: row.code,
    orgId: row.org_id,
    inviterMembershipId: row.inviter_membership_id,
    email: row.email,
    roleSlugs: row.role_slugs,
    status: row.status,
    maxUses: row.max_uses,
    useCount: row.use_count,
    expiresAt: row.expires_at,
    createdAt: row.created_at,
    updatedAt: row.updated_at,
  };
}
