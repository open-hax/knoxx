import type { EventAgentControlResponse, EventAgentJobControl, EventAgentRuntimeJob } from "../../lib/api/admin";
export type DraftControl = EventAgentControlResponse["control"];
export type JsonDrafts = Record<string, { sourceConfig: string; filters: string; toolPolicies: string }>;

import type {
  AdminMembershipSummary,
  AdminPermissionDefinition,
  AdminRoleSummary,
  AdminToolPolicy,
  AdminUserSummary,
} from '../../lib/types';
import type { ToolDraftEffect } from './types';

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
