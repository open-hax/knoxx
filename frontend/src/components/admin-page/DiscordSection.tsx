import React, { useCallback, useEffect, useMemo, useState } from "react";

import {
  dispatchEventAgentEvent,
  getDiscordConfig,
  getEventAgentControl,
  runEventAgentJob,
  updateDiscordConfig,
  updateEventAgentControl,
  type EventAgentControlResponse,
  type EventAgentJobControl,
  type EventAgentRuntimeJob,
} from "../../lib/api/admin";
import type { AdminToolDefinition } from "../../lib/types";
import { Badge, SectionCard } from "./common";

type Notice = { tone: "success" | "error"; text: string } | null;
type DraftControl = EventAgentControlResponse["control"];
type JsonDrafts = Record<string, { sourceConfig: string; filters: string; toolPolicies: string }>;

function splitCsv(value: string): string[] {
  return value
    .split(",")
    .map((entry) => entry.trim())
    .filter(Boolean);
}

function joinCsv(values: string[] | undefined): string {
  return (values ?? []).join(", ");
}

function prettyJson(value: unknown): string {
  return JSON.stringify(value ?? {}, null, 2);
}

function toLocalDateTime(value?: number): string {
  if (!value || !Number.isFinite(value)) return "—";
  try {
    return new Date(value).toLocaleString();
  } catch {
    return String(value);
  }
}

function runtimeForJob(runtimeJobs: EventAgentRuntimeJob[], jobId: string): EventAgentRuntimeJob | null {
  return runtimeJobs.find((job) => job.id === jobId) ?? null;
}

function seedJsonDrafts(jobs: EventAgentJobControl[]): JsonDrafts {
  return jobs.reduce<JsonDrafts>((acc, job) => {
    acc[job.id] = {
      sourceConfig: prettyJson(job.source.config ?? {}),
      filters: prettyJson(job.filters ?? {}),
      toolPolicies: prettyJson(job.agentSpec.toolPolicies ?? []),
    };
    return acc;
  }, {});
}

export function DiscordSection({ canManage, tools = [] }: { canManage: boolean; tools?: AdminToolDefinition[] }) {
  const [loading, setLoading] = useState(true);
  const [savingToken, setSavingToken] = useState(false);
  const [savingControl, setSavingControl] = useState(false);
  const [runningJobId, setRunningJobId] = useState<string | null>(null);
  const [dispatchingEvent, setDispatchingEvent] = useState(false);
  const [notice, setNotice] = useState<Notice>(null);
  const [error, setError] = useState<string>("");
  const [status, setStatus] = useState<EventAgentControlResponse | null>(null);
  const [draft, setDraft] = useState<DraftControl | null>(null);
  const [draftToken, setDraftToken] = useState("");
  const [jsonDrafts, setJsonDrafts] = useState<JsonDrafts>({});
  const [eventSourceKind, setEventSourceKind] = useState("github");
  const [eventKind, setEventKind] = useState("issues.opened");
  const [eventPayloadDraft, setEventPayloadDraft] = useState('{\n  "repository": "open-hax/openplanner",\n  "title": "Example event",\n  "content": "Investigate this issue"\n}');

  const runtimeJobs = useMemo(() => status?.runtime.jobs ?? [], [status]);
  const availableRoles = useMemo(() => status?.availableRoles ?? [], [status]);
  const availableSourceKinds = useMemo(() => status?.availableSourceKinds ?? [], [status]);
  const availableTriggerKinds = useMemo(() => status?.availableTriggerKinds ?? [], [status]);
  const availableToolIds = useMemo(() => tools.map((tool) => tool.id).sort(), [tools]);
  const discordSource = draft?.sources.discord ?? {};
  const recentEventCount = Array.isArray(status?.runtime.sources?.recentEvents)
    ? (status?.runtime.sources?.recentEvents as unknown[]).length
    : 0;
  const seenDiscordChannels = Array.isArray(status?.runtime.sources?.discord && (status.runtime.sources.discord as Record<string, unknown>).lastSeenChannels)
    ? (((status.runtime.sources?.discord as Record<string, unknown>).lastSeenChannels as unknown[])?.length ?? 0)
    : 0;

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    setNotice(null);
    try {
      const [tokenStatus, controlStatus] = await Promise.all([
        getDiscordConfig(),
        getEventAgentControl(),
      ]);
      const merged = {
        ...controlStatus,
        configured: tokenStatus.configured,
        tokenPreview: tokenStatus.tokenPreview,
      };
      setStatus(merged);
      setDraft(merged.control);
      setDraftToken("");
      setJsonDrafts(seedJsonDrafts(merged.control.jobs));
      setEventSourceKind(merged.availableSourceKinds.includes("github") ? "github" : (merged.availableSourceKinds[0] ?? "manual"));
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const updateJob = useCallback((jobId: string, patch: Partial<EventAgentJobControl>) => {
    setDraft((current) => {
      if (!current) return current;
      return {
        ...current,
        jobs: current.jobs.map((job) => (job.id === jobId ? { ...job, ...patch } : job)),
      };
    });
  }, []);

  const updateJsonDraft = useCallback((jobId: string, field: keyof JsonDrafts[string], value: string) => {
    setJsonDrafts((current) => ({
      ...current,
      [jobId]: {
        sourceConfig: current[jobId]?.sourceConfig ?? "{}",
        filters: current[jobId]?.filters ?? "{}",
        toolPolicies: current[jobId]?.toolPolicies ?? "[]",
        [field]: value,
      },
    }));
  }, []);

  const parseControlForSave = useCallback((): DraftControl => {
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
  }, [draft, jsonDrafts]);

  const handleSaveToken = useCallback(async () => {
    if (!canManage) return;
    const normalized = draftToken.trim();
    if (!normalized) {
      setError("Bot token must not be blank");
      return;
    }
    setSavingToken(true);
    setError("");
    setNotice(null);
    try {
      const updated = await updateDiscordConfig(normalized);
      setStatus((current) => (current ? { ...current, configured: updated.configured, tokenPreview: updated.tokenPreview } : current));
      setDraftToken("");
      setNotice({ tone: "success", text: `Discord bot token saved. Preview: ${updated.tokenPreview}` });
      await load();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setSavingToken(false);
    }
  }, [canManage, draftToken, load]);

  const handleSaveControl = useCallback(async () => {
    if (!canManage || !draft) return;
    setSavingControl(true);
    setError("");
    setNotice(null);
    try {
      const next = parseControlForSave();
      const updated = await updateEventAgentControl(next);
      setStatus(updated);
      setDraft(updated.control);
      setJsonDrafts(seedJsonDrafts(updated.control.jobs));
      setNotice({ tone: "success", text: "Event-agent control plane updated and runtime reloaded." });
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setSavingControl(false);
    }
  }, [canManage, draft, parseControlForSave]);

  const handleRunJob = useCallback(async (jobId: string) => {
    if (!canManage) return;
    setRunningJobId(jobId);
    setError("");
    setNotice(null);
    try {
      await runEventAgentJob(jobId);
      setNotice({ tone: "success", text: `Queued job ${jobId}.` });
      await load();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setRunningJobId(null);
    }
  }, [canManage, load]);

  const handleDispatchEvent = useCallback(async () => {
    if (!canManage) return;
    setDispatchingEvent(true);
    setError("");
    setNotice(null);
    try {
      const payload = JSON.parse(eventPayloadDraft || "{}");
      const result = await dispatchEventAgentEvent({
        sourceKind: eventSourceKind,
        eventKind,
        payload,
      });
      setNotice({ tone: "success", text: `Dispatched ${eventSourceKind}:${eventKind}. Matched jobs: ${result.matchedJobs.join(", ") || "none"}.` });
      await load();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setDispatchingEvent(false);
    }
  }, [canManage, eventKind, eventPayloadDraft, eventSourceKind, load]);

  return (
    <SectionCard
      title="Event agents"
      description="A generic event-driven agent runtime. Jobs can respond to cron ticks, Discord events, GitHub events, or arbitrary injected events with custom roles, prompts, models, and tool policies."
      actions={
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => void load()}
            disabled={loading || savingToken || savingControl}
            className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
          >
            {loading ? "Loading…" : "Refresh"}
          </button>
          <button
            type="button"
            onClick={() => void handleSaveControl()}
            disabled={!canManage || !draft || savingControl}
            className="inline-flex items-center justify-center rounded-lg bg-sky-600 px-4 py-2 text-sm font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60"
          >
            {savingControl ? "Saving…" : "Save runtime"}
          </button>
        </div>
      }
    >
      {loading || !draft || !status ? (
        <div className="text-sm text-slate-300">Loading event-agent control plane…</div>
      ) : (
        <div className="space-y-6">
          <div className="grid gap-3 md:grid-cols-4">
            <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Discord token</div>
              <div className="mt-2 flex items-center gap-2 text-sm text-slate-200">
                {status.configured ? <Badge tone="success">Configured</Badge> : <Badge tone="warn">Missing</Badge>}
                {status.tokenPreview ? <span className="font-mono text-xs text-slate-400">{status.tokenPreview}</span> : null}
              </div>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Runtime</div>
              <div className="mt-2 flex items-center gap-2 text-sm text-slate-200">
                {status.runtime.running ? <Badge tone="success">Running</Badge> : <Badge tone="warn">Stopped</Badge>}
                <span>{draft.jobs.length} jobs</span>
              </div>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Recent events</div>
              <div className="mt-2 text-2xl font-semibold text-slate-100">{recentEventCount}</div>
              <div className="mt-1 text-xs text-slate-500">Buffered normalized events</div>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Discord freshness</div>
              <div className="mt-2 text-2xl font-semibold text-slate-100">{seenDiscordChannels}</div>
              <div className="mt-1 text-xs text-slate-500">Channels with last-seen state</div>
            </div>
          </div>

          <div className="grid gap-4 xl:grid-cols-[0.8fr_1.2fr]">
            <div className="space-y-4 rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div>
                <div className="text-sm font-semibold text-slate-100">Discord adapter credentials</div>
                <div className="text-xs text-slate-500">Current generic runtime still uses the Discord bot token for Discord-sourced jobs.</div>
              </div>
              <label className="space-y-1">
                <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Discord bot token</div>
                <input
                  type="password"
                  value={draftToken}
                  onChange={(event) => setDraftToken(event.target.value)}
                  disabled={!canManage || savingToken}
                  className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                  placeholder={status.configured ? "Enter new token to replace" : "Bot token from Discord Developer Portal"}
                />
              </label>
              <button
                type="button"
                onClick={() => void handleSaveToken()}
                disabled={!canManage || savingToken || !draftToken.trim()}
                className="inline-flex items-center justify-center rounded-lg bg-sky-600 px-4 py-2 text-sm font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60"
              >
                {savingToken ? "Saving…" : "Save token"}
              </button>
            </div>

            <div className="space-y-4 rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div>
                <div className="text-sm font-semibold text-slate-100">Source defaults</div>
                <div className="text-xs text-slate-500">These defaults seed Discord jobs. Individual jobs can override them in their filter/source config JSON.</div>
              </div>
              <div className="grid gap-3 md:grid-cols-3">
                <label className="space-y-1">
                  <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Discord bot user ID</div>
                  <input
                    value={discordSource.botUserId ?? ""}
                    onChange={(event) => setDraft({
                      ...draft,
                      sources: {
                        ...draft.sources,
                        discord: {
                          ...discordSource,
                          botUserId: event.target.value,
                        },
                      },
                    })}
                    disabled={!canManage || savingControl}
                    className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                  />
                </label>
                <label className="space-y-1">
                  <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Default channels</div>
                  <input
                    value={joinCsv(discordSource.defaultChannels)}
                    onChange={(event) => setDraft({
                      ...draft,
                      sources: {
                        ...draft.sources,
                        discord: {
                          ...discordSource,
                          defaultChannels: splitCsv(event.target.value),
                        },
                      },
                    })}
                    disabled={!canManage || savingControl}
                    className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                  />
                </label>
                <label className="space-y-1">
                  <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Default keywords</div>
                  <input
                    value={joinCsv(discordSource.targetKeywords)}
                    onChange={(event) => setDraft({
                      ...draft,
                      sources: {
                        ...draft.sources,
                        discord: {
                          ...discordSource,
                          targetKeywords: splitCsv(event.target.value).map((value) => value.toLowerCase()),
                        },
                      },
                    })}
                    disabled={!canManage || savingControl}
                    className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                  />
                </label>
              </div>
            </div>
          </div>

          <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4 space-y-4">
            <div>
              <div className="text-sm font-semibold text-slate-100">Dispatch test event</div>
              <div className="text-xs text-slate-500">Use this to simulate GitHub, Discord, cron, or arbitrary source events against the matcher.</div>
            </div>
            <div className="grid gap-3 md:grid-cols-[0.8fr_1.2fr_1fr_auto] md:items-end">
              <label className="space-y-1">
                <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Source kind</div>
                <select
                  value={eventSourceKind}
                  onChange={(event) => setEventSourceKind(event.target.value)}
                  disabled={!canManage || dispatchingEvent}
                  className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                >
                  {availableSourceKinds.map((kind) => <option key={`event-kind-${kind}`} value={kind}>{kind}</option>)}
                </select>
              </label>
              <label className="space-y-1">
                <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Event kind</div>
                <input
                  value={eventKind}
                  onChange={(event) => setEventKind(event.target.value)}
                  disabled={!canManage || dispatchingEvent}
                  className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                  placeholder="issues.opened"
                />
              </label>
              <label className="space-y-1">
                <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Payload JSON</div>
                <textarea
                  value={eventPayloadDraft}
                  onChange={(event) => setEventPayloadDraft(event.target.value)}
                  disabled={!canManage || dispatchingEvent}
                  rows={5}
                  className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-xs font-mono text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                />
              </label>
              <button
                type="button"
                onClick={() => void handleDispatchEvent()}
                disabled={!canManage || dispatchingEvent}
                className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
              >
                {dispatchingEvent ? "Dispatching…" : "Dispatch"}
              </button>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <div className="text-sm font-semibold text-slate-100">Jobs</div>
              <div className="text-xs text-slate-500">Jobs are the policy layer. Adapters emit events; jobs decide roles, tools, prompts, and timing.</div>
            </div>

            {draft.jobs.map((job) => {
              const runtime = runtimeForJob(runtimeJobs, job.id);
              const jobJsonDraft = jsonDrafts[job.id] ?? { sourceConfig: "{}", filters: "{}", toolPolicies: "[]" };
              return (
                <div key={job.id} className="rounded-2xl border border-slate-800 bg-slate-950/40 p-4 space-y-4">
                  <div className="flex flex-col gap-3 border-b border-slate-800 pb-4 md:flex-row md:items-start md:justify-between">
                    <div>
                      <div className="flex flex-wrap items-center gap-2">
                        <h3 className="text-base font-semibold text-slate-100">{job.name}</h3>
                        <Badge tone={job.enabled ? "success" : "warn"}>{job.enabled ? "Enabled" : "Disabled"}</Badge>
                        <Badge tone="info">{job.source.kind}</Badge>
                        <Badge>{job.trigger.kind}</Badge>
                        {runtime?.running ? <Badge tone="success">Running now</Badge> : null}
                      </div>
                      {job.description ? <p className="mt-1 text-sm text-slate-400">{job.description}</p> : null}
                    </div>
                    <button
                      type="button"
                      onClick={() => void handleRunJob(job.id)}
                      disabled={!canManage || runningJobId === job.id}
                      className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
                    >
                      {runningJobId === job.id ? "Queueing…" : "Run now"}
                    </button>
                  </div>

                  <div className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]">
                    <div className="space-y-4">
                      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Enabled</div>
                          <label className="inline-flex items-center gap-2 rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-200">
                            <input
                              type="checkbox"
                              checked={job.enabled}
                              onChange={(event) => updateJob(job.id, { enabled: event.target.checked })}
                              disabled={!canManage || savingControl}
                            />
                            Active
                          </label>
                        </label>

                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Trigger kind</div>
                          <select
                            value={job.trigger.kind}
                            onChange={(event) => updateJob(job.id, { trigger: { ...job.trigger, kind: event.target.value } })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          >
                            {availableTriggerKinds.map((kind) => <option key={`${job.id}-trigger-${kind}`} value={kind}>{kind}</option>)}
                          </select>
                        </label>

                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Source kind</div>
                          <select
                            value={job.source.kind}
                            onChange={(event) => updateJob(job.id, { source: { ...job.source, kind: event.target.value } })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          >
                            {availableSourceKinds.map((kind) => <option key={`${job.id}-source-${kind}`} value={kind}>{kind}</option>)}
                          </select>
                        </label>

                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Source mode</div>
                          <input
                            value={job.source.mode}
                            onChange={(event) => updateJob(job.id, { source: { ...job.source, mode: event.target.value } })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          />
                        </label>
                      </div>

                      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Cadence (minutes)</div>
                          <input
                            type="number"
                            min={1}
                            max={10080}
                            value={job.trigger.cadenceMinutes}
                            onChange={(event) => updateJob(job.id, { trigger: { ...job.trigger, cadenceMinutes: Number(event.target.value || 1) } })}
                            disabled={!canManage || savingControl || job.trigger.kind !== "cron"}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          />
                        </label>
                        <label className="space-y-1 md:col-span-3">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Event kinds</div>
                          <input
                            value={joinCsv(job.trigger.eventKinds)}
                            onChange={(event) => updateJob(job.id, { trigger: { ...job.trigger, eventKinds: splitCsv(event.target.value) } })}
                            disabled={!canManage || savingControl || job.trigger.kind !== "event"}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                            placeholder="discord.message.mention, issues.opened"
                          />
                        </label>
                      </div>

                      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Role</div>
                          <select
                            value={job.agentSpec.role}
                            onChange={(event) => updateJob(job.id, { agentSpec: { ...job.agentSpec, role: event.target.value } })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          >
                            {availableRoles.map((role) => <option key={`${job.id}-role-${role}`} value={role}>{role}</option>)}
                          </select>
                        </label>
                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Model</div>
                          <input
                            value={job.agentSpec.model}
                            onChange={(event) => updateJob(job.id, { agentSpec: { ...job.agentSpec, model: event.target.value } })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          />
                        </label>
                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Thinking</div>
                          <select
                            value={job.agentSpec.thinkingLevel}
                            onChange={(event) => updateJob(job.id, { agentSpec: { ...job.agentSpec, thinkingLevel: event.target.value } })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          >
                            {["off", "minimal", "low", "medium", "high", "xhigh"].map((value) => (
                              <option key={`${job.id}-thinking-${value}`} value={value}>{value}</option>
                            ))}
                          </select>
                        </label>
                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Job description</div>
                          <input
                            value={job.description ?? ""}
                            onChange={(event) => updateJob(job.id, { description: event.target.value })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          />
                        </label>
                      </div>

                      <label className="space-y-1">
                        <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">System prompt</div>
                        <textarea
                          value={job.agentSpec.systemPrompt}
                          onChange={(event) => updateJob(job.id, { agentSpec: { ...job.agentSpec, systemPrompt: event.target.value } })}
                          disabled={!canManage || savingControl}
                          rows={4}
                          className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                        />
                      </label>

                      <label className="space-y-1">
                        <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Task prompt</div>
                        <textarea
                          value={job.agentSpec.taskPrompt}
                          onChange={(event) => updateJob(job.id, { agentSpec: { ...job.agentSpec, taskPrompt: event.target.value } })}
                          disabled={!canManage || savingControl}
                          rows={4}
                          className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                        />
                      </label>

                      <div className="grid gap-3 xl:grid-cols-3">
                        <label className="space-y-1 xl:col-span-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Source config JSON</div>
                          <textarea
                            value={jobJsonDraft.sourceConfig}
                            onChange={(event) => updateJsonDraft(job.id, "sourceConfig", event.target.value)}
                            disabled={!canManage || savingControl}
                            rows={8}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 font-mono text-xs text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          />
                        </label>
                        <label className="space-y-1 xl:col-span-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Filters JSON</div>
                          <textarea
                            value={jobJsonDraft.filters}
                            onChange={(event) => updateJsonDraft(job.id, "filters", event.target.value)}
                            disabled={!canManage || savingControl}
                            rows={8}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 font-mono text-xs text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          />
                        </label>
                        <label className="space-y-1 xl:col-span-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Tool policies JSON</div>
                          <textarea
                            value={jobJsonDraft.toolPolicies}
                            onChange={(event) => updateJsonDraft(job.id, "toolPolicies", event.target.value)}
                            disabled={!canManage || savingControl}
                            rows={8}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 font-mono text-xs text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          />
                          <div className="text-[11px] text-slate-500">Available tools: {availableToolIds.join(", ") || "(tool catalog unavailable)"}</div>
                        </label>
                      </div>
                    </div>

                    <div className="space-y-3 rounded-xl border border-slate-800 bg-slate-950/60 p-4">
                      <div className="text-sm font-semibold text-slate-100">Live runtime</div>
                      <div className="grid gap-2 text-sm text-slate-300">
                        <div className="flex items-center justify-between gap-3">
                          <span className="text-slate-500">Schedule</span>
                          <span>{runtime?.scheduleLabel ?? "—"}</span>
                        </div>
                        <div className="flex items-center justify-between gap-3">
                          <span className="text-slate-500">Runs</span>
                          <span>{runtime?.runCount ?? 0}</span>
                        </div>
                        <div className="flex items-center justify-between gap-3">
                          <span className="text-slate-500">Last status</span>
                          <span>
                            {runtime?.lastStatus === "ok" ? <Badge tone="success">ok</Badge>
                              : runtime?.lastStatus === "error" ? <Badge tone="danger">error</Badge>
                              : runtime?.lastStatus === "running" ? <Badge tone="info">running</Badge>
                              : <Badge>idle</Badge>}
                          </span>
                        </div>
                        <div className="flex items-center justify-between gap-3">
                          <span className="text-slate-500">Last started</span>
                          <span className="text-right text-xs">{toLocalDateTime(runtime?.lastStartedAt)}</span>
                        </div>
                        <div className="flex items-center justify-between gap-3">
                          <span className="text-slate-500">Last finished</span>
                          <span className="text-right text-xs">{toLocalDateTime(runtime?.lastFinishedAt)}</span>
                        </div>
                        <div className="flex items-center justify-between gap-3">
                          <span className="text-slate-500">Duration</span>
                          <span>{runtime?.lastDurationMs ? `${runtime.lastDurationMs} ms` : "—"}</span>
                        </div>
                        <div className="flex items-center justify-between gap-3">
                          <span className="text-slate-500">Next run</span>
                          <span className="text-right text-xs">{toLocalDateTime(runtime?.nextRunAt)}</span>
                        </div>
                      </div>
                      {runtime?.lastError ? (
                        <div className="rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-xs text-rose-200">
                          {runtime.lastError}
                        </div>
                      ) : null}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {!canManage ? (
            <div className="rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
              You do not have <code className="font-mono">platform.org.create</code> permission to mutate the event-agent runtime.
            </div>
          ) : null}

          {notice ? (
            <div className={notice.tone === "success"
              ? "rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-sm text-emerald-200"
              : "rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200"}
            >
              {notice.text}
            </div>
          ) : null}

          {error ? (
            <div className="rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">
              {error}
            </div>
          ) : null}
        </div>
      )}
    </SectionCard>
  );
}
