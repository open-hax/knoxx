export type Notice = { tone: 'success' | 'error'; text: string } | null;
export type ToolDraftEffect = 'inherit' | 'allow' | 'deny';

export type UserFormState = {
  axxiumPrincipalId?: string;
  actorId: string;
  email: string;
  displayName: string;
  roleSlugs: string[];
};

export type RoleFormState = {
  name: string;
  slug: string;
  permissionCodes: string[];
  toolIds: string[];
};

export type OrgFormState = {
  name: string;
  slug: string;
  kind: string;
};

export type LakeFormState = {
  name: string;
  slug: string;
  kind: string;
  workspaceRoot: string;
};

// Administration identity, organization and permission wire contracts.
export interface KnoxxAuthIdentity {
  userEmail: string;
  orgSlug: string;
}

export interface AdminToolPolicy {
  toolId: string;
  effect: "allow" | "deny";
  constraints?: Record<string, unknown>;
}

export interface AdminOrgSummary {
  id: string;
  slug: string;
  name: string;
  kind: string;
  isPrimary: boolean;
  status: string;
  memberCount?: number;
  roleCount?: number;
  dataLakeCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminRoleSummary {
  id: string;
  slug: string;
  name: string;
  scopeKind?: string;
  orgId?: string | null;
  builtIn?: boolean;
  systemManaged?: boolean;
  createdAt?: string;
  updatedAt?: string;
  permissions: string[];
  toolPolicies: AdminToolPolicy[];
}

export interface AdminMembershipSummary {
  id: string;
  userId?: string;
  orgId: string;
  actorId?: string;
  orgName?: string;
  orgSlug?: string;
  status: string;
  isDefault?: boolean;
  createdAt?: string;
  updatedAt?: string;
  roles: Array<Pick<AdminRoleSummary, "id" | "slug" | "name" | "scopeKind" | "orgId">>;
  toolPolicies: AdminToolPolicy[];
}

export interface AdminActorCredentialSummary {
  id: string;
  provider: string;
  label?: string;
  kind: string;
  accountIdentifier?: string | null;
  status: string;
  configuredFields: string[];
  secretJson?: Record<string, string>;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminUserSummary {
  principalId?: string;
  identityBound?: boolean;
  identityEnrollmentRequired?: boolean;
  id: string;
  email: string;
  displayName: string;
  authProvider?: string;
  externalSubject?: string | null;
  status: string;
  createdAt?: string;
  updatedAt?: string;
  credentials?: AdminActorCredentialSummary[];
  memberships: AdminMembershipSummary[];
}

export interface AdminDataLakeSummary {
  id: string;
  orgId: string;
  name: string;
  slug: string;
  kind: string;
  config: Record<string, unknown>;
  status: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AdminPermissionDefinition {
  id: string;
  code: string;
  resourceKind: string;
  action: string;
  description: string;
}

export interface AdminToolDefinition {
  id: string;
  label: string;
  description: string;
  riskLevel: string;
}

export interface KnoxxAuthContext {
  actor?: {
    id: string;
  };
  user: {
    id: string;
    email: string;
    username?: string;
    displayName: string;
    status: string;
  };
  org: {
    id: string;
    slug: string;
    name: string;
    status: string;
    isPrimary?: boolean;
    kind?: string;
  };
  membership: {
    id: string;
    actorId?: string;
    status: string;
    isDefault?: boolean;
    createdAt?: string;
    updatedAt?: string;
  };
  roles: AdminRoleSummary[];
  roleSlugs: string[];
  permissions: string[];
  toolPolicies: AdminToolPolicy[];
  membershipToolPolicies: AdminToolPolicy[];
  isSystemAdmin: boolean;
  primaryRole: string;
}

export interface AdminBootstrapContext {
  primaryOrg: AdminOrgSummary;
  bootstrapUser: {
    id: string;
    email: string;
    displayName: string;
    membershipId: string;
  };
}

// Administration API request and runtime response contracts.
export interface GraphMonitoringStats {
  ok: boolean;
  stats: {
    nodes: number;
    edges: number;
    embeddings: number;
    layouts: number;
  };
  projectBreakdown: Array<{ project: string; count: number }>;
  recentEmbeddings: Array<{
    nodeId: string;
    model: string | null;
    dimensions: number;
    updatedAt: Date | null;
  }>;
  storageBackend: string;
}

export interface DiscordConfigStatus {
  configured: boolean;
  tokenPreview: string;
}

export interface EventAgentToolPolicy {
  toolId: string;
  effect: "allow" | "deny";
}

export interface EventAgentJobControl {
  id: string;
  name: string;
  enabled: boolean;
  description?: string;
  contractSourceId?: string;
  contractSourceKind?: string;
  contractSourceKey?: string;
  contractHash?: number;
  actorId?: string;
  trigger: {
    kind: string;
    cadenceMinutes: number;
    eventKinds: string[];
  };
  source: {
    kind: string;
    mode: string;
    config: Record<string, unknown>;
  };
  filters: Record<string, unknown>;
  agentSpec: {
    role: string;
    model: string;
    thinkingLevel: string;
    systemPrompt: string;
    taskPrompt: string;
    toolPolicies: EventAgentToolPolicy[];
  };
}

export interface EventAgentRuntimeJob {
  id: string;
  name: string;
  enabled: boolean;
  contractSourceId?: string;
  contractSourceKind?: string;
  contractSourceKey?: string;
  scheduleLabel: string;
  trigger?: {
    kind: string;
    cadenceMinutes?: number;
    eventKinds?: string[];
  };
  source?: {
    kind: string;
    mode?: string;
  };
  running?: boolean;
  runCount?: number;
  lastStartedAt?: number;
  lastFinishedAt?: number;
  lastDurationMs?: number;
  lastStatus?: string;
  lastError?: string;
  nextRunAt?: number;
}

export interface EventAgentControlResponse extends DiscordConfigStatus {
  availableRoles: string[];
  availableSourceKinds: string[];
  availableTriggerKinds: string[];
  control: {
    sources: {
      discord?: {
        botUserId?: string;
        defaultChannels?: string[];
        targetKeywords?: string[];
      };
      github?: Record<string, unknown>;
      cron?: Record<string, unknown>;
      [key: string]: unknown;
    };
    jobs: EventAgentJobControl[];
  };
  runtime: {
    running: boolean;
    configured: boolean;
    sources?: Record<string, unknown>;
    jobs: EventAgentRuntimeJob[];
  };
}

export interface CreateOrgActorPayload {
  axxiumPrincipalId?: string;
  actorId?: string;
  email?: string;
  displayName?: string;
  roleSlugs: string[];
  toolPolicies?: AdminToolPolicy[];
}

export interface UpdateAdminActorPayload {
  orgId: string;
  actorId?: string;
  email?: string;
  displayName?: string;
  status?: string;
  authProvider?: string;
  externalSubject?: string;
}

export interface ActorCredentialPayload {
  orgId: string;
  kind: string;
  accountIdentifier?: string;
  secretJson: Record<string, string>;
}

export interface CreateOrgRolePayload {
  name: string;
  slug?: string;
  permissionCodes: string[];
  toolPolicies?: AdminToolPolicy[];
}

export interface CreateOrgDataLakePayload {
  name: string;
  slug?: string;
  kind?: string;
  config?: Record<string, unknown>;
}

export interface EventAgentEventPayload {
  sourceKind: string;
  eventKind: string;
  payload?: Record<string, unknown>;
}

export type EventAgentRuntimeResetResponse = EventAgentControlResponse & {
  ok: boolean;
  action: string;
  reset: {
    ok: boolean;
    deletedCount: number;
    disabledCronJobCount?: number;
    preservedCronJobCount?: number;
  };
};
