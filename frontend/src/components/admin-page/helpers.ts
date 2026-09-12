import type { EventAgentControlResponse, EventAgentJobControl, EventAgentRuntimeJob } from "../../lib/api/admin";
export type DraftControl = EventAgentControlResponse["control"];
export type JsonDrafts = Record<string, { sourceConfig: string; filters: string; toolPolicies: string }>;

import type {
  AdminActorCredentialSummary,
  AdminMembershipSummary,
  AdminPermissionDefinition,
  AdminRoleSummary,
  AdminToolPolicy,
  AdminUserSummary,
} from '../../lib/types';
import type { ActorProfileDraft, ActorCredentialDraft, CredentialDescriptor, ToolDraftEffect } from './types';

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error);
}

export function groupPermissions(permissions: AdminPermissionDefinition[]): Array<[string, AdminPermissionDefinition[]]> {
  const grouped = permissions.reduce<Record<string, AdminPermissionDefinition[]>>((acc, permission) => {
    const key = permission.resourceKind || 'misc';
    if (!acc[key]) acc[key] = [];
    acc[key].push(permission);
    return acc;
  }, {});

  return Object.entries(grouped)
    .map(([kind, items]) => [kind, [...items].sort((a, b) => a.code.localeCompare(b.code))] as [string, AdminPermissionDefinition[]])
    .sort(([a], [b]) => a.localeCompare(b));
}

export function membershipForOrg(user: AdminUserSummary, orgId: string): AdminMembershipSummary | null {
  return user.memberships.find((membership) => membership.orgId === orgId) ?? null;
}

export function toolDraftMap(policies: AdminToolPolicy[]): Record<string, ToolDraftEffect> {
  return policies.reduce<Record<string, ToolDraftEffect>>((acc, policy) => {
    acc[policy.toolId] = policy.effect;
    return acc;
  }, {});
}

export function toolPoliciesFromDraft(draft: Record<string, ToolDraftEffect>): AdminToolPolicy[] {
  return Object.entries(draft).flatMap(([toolId, effect]) => {
    if (effect !== 'allow' && effect !== 'deny') {
      return [];
    }

    return [{ toolId, effect } satisfies AdminToolPolicy];
  });
}

export function toggleListValue(values: string[], value: string): string[] {
  return values.includes(value)
    ? values.filter((entry) => entry !== value)
    : [...values, value];
}

export function hydrateRoleDrafts(nextRoles: AdminRoleSummary[]): Record<string, Record<string, ToolDraftEffect>> {
  const nextDrafts: Record<string, Record<string, ToolDraftEffect>> = {};
  for (const role of nextRoles) {
    nextDrafts[role.id] = toolDraftMap(role.toolPolicies);
  }
  return nextDrafts;
}

export function hydrateMembershipDrafts(nextUsers: AdminUserSummary[], orgId: string): {
  roleDrafts: Record<string, string[]>;
  toolDrafts: Record<string, Record<string, ToolDraftEffect>>;
} {
  const roleDrafts: Record<string, string[]> = {};
  const toolDrafts: Record<string, Record<string, ToolDraftEffect>> = {};

  for (const user of nextUsers) {
    const membership = membershipForOrg(user, orgId);
    if (!membership) continue;
    roleDrafts[membership.id] = membership.roles.map((role) => role.slug);
    toolDrafts[membership.id] = toolDraftMap(membership.toolPolicies);
  }

  return { roleDrafts, toolDrafts };
}

export function splitCsv(value: string): string[] {
  return value
    .split(",")
    .map((entry) => entry.trim())
    .filter(Boolean);
}

export function joinCsv(values: string[] | undefined): string {
  return (values ?? []).join(", ");
}

export function prettyJson(value: unknown): string {
  return JSON.stringify(value ?? {}, null, 2);
}

export function toLocalDateTime(value?: number): string {
  if (!value || !Number.isFinite(value)) return "—";
  try {
    return new Date(value).toLocaleString();
  } catch {
    return String(value);
  }
}

export function runtimeForJob(runtimeJobs: EventAgentRuntimeJob[], jobId: string): EventAgentRuntimeJob | null {
  return runtimeJobs.find((job) => job.id === jobId) ?? null;
}

export function seedJsonDrafts(jobs: EventAgentJobControl[]): JsonDrafts {
  return jobs.reduce<JsonDrafts>((acc, job) => {
    acc[job.id] = {
      sourceConfig: prettyJson(job.source.config ?? {}),
      filters: prettyJson(job.filters ?? {}),
      toolPolicies: prettyJson(job.agentSpec.toolPolicies ?? []),
    };
    return acc;
  }, {});
}

export function compactText(value: string | undefined, max = 120): string {
  const normalized = (value ?? "").replace(/\s+/g, " ").trim();
  if (!normalized) return "No description";
  if (normalized.length <= max) return normalized;
  return `${normalized.slice(0, max - 1)}…`;
}

export function normalizeSearch(value: string): string {
  return value.trim().toLowerCase();
}

export function jobSearchText(job: EventAgentJobControl): string {
  return [
    job.id,
    job.name,
    job.description,
    job.source.kind,
    job.source.mode,
    job.trigger.kind,
    job.trigger.eventKinds.join(" "),
    job.agentSpec.role,
    job.agentSpec.model,
    job.contractSourceId,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();
}

export function runtimeStatusTone(status: string | undefined): "default" | "success" | "warn" | "danger" | "info" {
  switch (status) {
    case "ok":
      return "success";
    case "error":
      return "danger";
    case "running":
      return "info";
    default:
      return "default";
  }
}

export function parseEventControl(draft: DraftControl | null, jsonDrafts: JsonDrafts): DraftControl {
  if (!draft) throw new Error("No draft control loaded");
  return {
    ...draft,
    jobs: draft.jobs.map((job) => {
      const drafts = jsonDrafts[job.id] ?? {
        sourceConfig: prettyJson(job.source.config ?? {}),
        filters: prettyJson(job.filters ?? {}),
        toolPolicies: prettyJson(job.agentSpec.toolPolicies ?? []),
      };
      let sourceConfig: Record<string, unknown>;
      let filters: Record<string, unknown>;
      let toolPolicies: EventAgentJobControl["agentSpec"]["toolPolicies"];
      try {
        sourceConfig = JSON.parse(drafts.sourceConfig || "{}");
      } catch (err) {
        throw new Error(`Invalid source config JSON for job ${job.name}: ${err instanceof Error ? err.message : String(err)}`);
      }
      try {
        filters = JSON.parse(drafts.filters || "{}");
      } catch (err) {
        throw new Error(`Invalid filters JSON for job ${job.name}: ${err instanceof Error ? err.message : String(err)}`);
      }
      try {
        toolPolicies = JSON.parse(drafts.toolPolicies || "[]");
      } catch (err) {
        throw new Error(`Invalid tool policy JSON for job ${job.name}: ${err instanceof Error ? err.message : String(err)}`);
      }
      return {
        ...job,
        source: {
          ...job.source,
          config: sourceConfig,
        },
        filters,
        agentSpec: {
          ...job.agentSpec,
          toolPolicies,
        },
      };
    }),
  };
}

export const CREDENTIAL_DESCRIPTORS: CredentialDescriptor[] = [
  {
    provider: 'bluesky',
    label: 'Bluesky',
    kind: 'app-password',
    accountPlaceholder: 'handle.bsky.social',
    fields: [
      { key: 'identifier', label: 'Identifier / handle', placeholder: 'handle.bsky.social' },
      { key: 'appPassword', label: 'App password', secret: true, placeholder: 'xxxx-xxxx-xxxx-xxxx' },
    ],
  },
  {
    provider: 'twitch',
    label: 'Twitch',
    kind: 'oauth-login',
    accountPlaceholder: 'twitch username',
    fields: [
      { key: 'username', label: 'Username', placeholder: 'channel_or_login' },
      { key: 'oauthToken', label: 'OAuth token', secret: true, placeholder: 'oauth:… or raw token' },
    ],
  },
  {
    provider: 'discord_bot',
    label: 'Discord bot',
    kind: 'bot-token',
    accountPlaceholder: 'bot application/client id',
    fields: [
      { key: 'botToken', label: 'Bot token', secret: true, placeholder: 'Bot token' },
      { key: 'applicationId', label: 'Application ID', placeholder: 'Discord application id' },
      { key: 'publicKey', label: 'Public key', placeholder: 'Optional interactions public key' },
    ],
  },
  {
    provider: 'discord_oauth',
    label: 'Discord OAuth login',
    kind: 'oauth-login',
    accountPlaceholder: 'discord user id or username',
    fields: [
      { key: 'clientId', label: 'Client ID', placeholder: 'OAuth client id' },
      { key: 'clientSecret', label: 'Client secret', secret: true, placeholder: 'OAuth client secret' },
      { key: 'accessToken', label: 'Access token', secret: true, placeholder: 'Optional current access token' },
      { key: 'refreshToken', label: 'Refresh token', secret: true, placeholder: 'Optional refresh token' },
    ],
  },
];

export function credentialForProvider(credentials: AdminActorCredentialSummary[] | undefined, provider: string): AdminActorCredentialSummary | null {
  return credentials?.find((credential) => credential.provider === provider) ?? null;
}

export function credentialKey(userId: string, provider: string): string {
  return `${userId}:${provider}`;
}

export function draftProfileForUser(user: AdminUserSummary, selectedOrgId: string): ActorProfileDraft {
  const membership = membershipForOrg(user, selectedOrgId);
  return {
    actorId: membership?.actorId ?? '',
    displayName: user.displayName ?? '',
    email: user.email ?? '',
    status: user.status ?? 'active',
  };
}

export function draftCredentialForDescriptor(
  user: AdminUserSummary,
  descriptor: CredentialDescriptor,
): ActorCredentialDraft {
  const current = credentialForProvider(user.credentials, descriptor.provider);
  return {
    kind: current?.kind || descriptor.kind,
    accountIdentifier: current?.accountIdentifier || '',
    secretJson: descriptor.fields.reduce<Record<string, string>>((acc, field) => {
      acc[field.key] = current?.secretJson?.[field.key] || '';
      return acc;
    }, {}),
  };
}

export function configuredFieldLabel(current: AdminActorCredentialSummary | null, fieldKey: string): string {
  return current?.configuredFields.includes(fieldKey) ? 'configured' : 'not set';
}

export function actorSearchText(user: AdminUserSummary, selectedOrgId: string): string {
  const membership = membershipForOrg(user, selectedOrgId);
  return [
    user.displayName,
    user.email,
    user.authProvider,
    user.externalSubject,
    user.status,
    membership?.actorId,
    membership?.status,
    ...(membership?.roles.map((role) => `${role.slug} ${role.name}`) ?? []),
    ...(membership?.toolPolicies.map((policy) => `${policy.toolId} ${policy.effect}`) ?? []),
    ...(user.credentials?.flatMap((credential) => [
      credential.provider,
      credential.label,
      credential.kind,
      credential.accountIdentifier,
      ...credential.configuredFields,
    ]) ?? []),
  ].filter(Boolean).join(' ').toLowerCase();
}
