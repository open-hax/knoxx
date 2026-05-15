import React, { useCallback, useEffect, useMemo, useState } from "react";

import {
  dispatchEventAgentEvent,
  getDiscordConfig,
  getEventAgentControl,
  resetEventAgentRuntime,
  runEventAgentJob,
  startEventAgentRuntime,
  stopEventAgentRuntime,
  updateDiscordConfig,
  updateEventAgentControl,
  type EventAgentControlResponse,
  type EventAgentJobControl,
  type EventAgentRuntimeJob,
} from "../../lib/api/admin";
import type { AdminToolDefinition } from "../../lib/types";
import { EventAgentScheduleReview } from "./EventAgentScheduleReview";
import { Badge, SectionCard, classNames } from "./common";

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

function compactText(value: string | undefined, max = 120): string {
  const normalized = (value ?? "").replace(/\s+/g, " ").trim();
  if (!normalized) return "No description";
  if (normalized.length <= max) return normalized;
  return `${normalized.slice(0, max - 1)}…`;
}

function normalizeSearch(value: string): string {
  return value.trim().toLowerCase();
}

function jobSearchText(job: EventAgentJobControl): string {
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

function runtimeStatusTone(status: string | undefined): "default" | "success" | "warn" | "danger" | "info" {
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

function SidebarJobButton({
  job,
  runtime,
  active,
  onSelect,
}: {
  job: EventAgentJobControl;
  runtime: EventAgentRuntimeJob | null;
  active: boolean;
  onSelect: () => void;
}) {
  const meta = [job.source.kind, job.trigger.kind, job.contractSourceId ? "contract" : "custom"];
  const runtimeLabel = runtime?.running ? "running" : (runtime?.lastStatus ?? "idle");

  return (
    <button
      type="button"
      onClick={onSelect}
      className={classNames(
        "w-full rounded-lg border px-2.5 py-2 text-left transition",
        active
          ? "border-sky-500/60 bg-sky-500/10 shadow-[inset_2px_0_0_0_rgba(56,189,248,0.9)]"
          : "border-slate-800 bg-slate-950/35 hover:border-slate-700 hover:bg-slate-950/70",
      )}
      aria-pressed={active}
    >
      <div className="flex items-center justify-between gap-2">
        <div className="min-w-0 truncate text-sm font-medium text-slate-100">{job.name}</div>
        <span
          className={classNames(
            "h-2 w-2 shrink-0 rounded-full",
            job.enabled ? "bg-emerald-400" : "bg-amber-400",
          )}
          aria-hidden="true"
        />
      </div>

      <div className="mt-1 flex items-center justify-between gap-2 text-[11px] leading-4 text-slate-500">
        <span className="min-w-0 truncate">{meta.join(" · ")}</span>
        <span className="shrink-0">{runtime?.runCount ?? 0}r</span>
      </div>

      <div className="mt-1 flex items-center justify-between gap-2 text-[11px] leading-4">
        <span className="min-w-0 truncate font-mono text-slate-400">{compactText(job.id, 28)}</span>
        <span className={classNames(
          "shrink-0 uppercase tracking-wide",
          runtime?.lastStatus === "ok"
            ? "text-emerald-300"
            : runtime?.lastStatus === "error"
              ? "text-rose-300"
              : runtime?.running
                ? "text-sky-300"
                : "text-slate-500",
        )}>{runtimeLabel}</span>
      </div>
    </button>
  );
}

function CollapsiblePanel({
  title,
  description,
  defaultOpen = false,
  children,
}: {
  title: string;
  description?: string;
  defaultOpen?: boolean;
  children: React.ReactNode;
}) {
  return (
    <details
      open={defaultOpen}
      className="rounded-xl border border-slate-800 bg-slate-950/40 p-4 open:bg-slate-950/55"
    >
      <summary className="cursor-pointer list-none">
        <div className="flex items-start justify-between gap-4">
          <div>
            <div className="text-sm font-semibold text-slate-100">{title}</div>
            {description ? <div className="mt-1 text-xs text-slate-500">{description}</div> : null}
          </div>
          <span className="text-xs uppercase tracking-wide text-slate-500">toggle</span>
        </div>
      </summary>
      <div className="mt-4">{children}</div>
    </details>
  );
}

function RuntimeInfrastructureForm({
  canManage,
  draft,
  status,
  draftToken,
  setDraft,
  setDraftToken,
  savingToken,
  savingControl,
  onSaveToken,
}: {
  canManage: boolean;
  draft: DraftControl;
  status: EventAgentControlResponse;
  draftToken: string;
  setDraft: React.Dispatch<React.SetStateAction<DraftControl | null>>;
  setDraftToken: (value: string) => void;
  savingToken: boolean;
  savingControl: boolean;
  onSaveToken: () => void;
}) {
  const discordSource = draft.sources.discord ?? {};

  return (
    <div className="col-span-full rounded-xl border border-slate-800 bg-slate-950/40 p-3">
      <div className="mb-3 flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
        <div>
          <div className="text-sm font-semibold text-slate-100">Runtime infrastructure</div>
          <div className="text-xs text-slate-500">Credentials and source defaults are top-level runtime settings, not per-agent schedule fields.</div>
        </div>
        <div className="flex items-center gap-2 text-xs text-slate-300">
          {status.configured ? <Badge tone="success">Discord configured</Badge> : <Badge tone="warn">Discord missing</Badge>}
          {status.tokenPreview ? <code className="font-mono text-[11px] text-slate-400">{status.tokenPreview}</code> : null}
        </div>
      </div>

      <div className="grid gap-3 xl:grid-cols-[minmax(0,0.9fr)_minmax(0,1.4fr)]">
        <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-3">
          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Discord adapter credentials</div>
          <div className="mt-2 grid gap-2 md:grid-cols-[minmax(0,1fr)_auto] xl:grid-cols-1">
            <input
              type="password"
              value={draftToken}
              onChange={(event) => setDraftToken(event.target.value)}
              disabled={!canManage || savingToken}
              className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
              placeholder={status.configured ? "Enter new token to replace" : "Bot token from Discord Developer Portal"}
            />
            <button
              type="button"
              onClick={onSaveToken}
              disabled={!canManage || savingToken || !draftToken.trim()}
              className="inline-flex items-center justify-center rounded-lg bg-sky-600 px-4 py-2 text-sm font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60"
            >
              {savingToken ? "Saving…" : "Save token"}
            </button>
          </div>
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-3">
          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Source defaults</div>
          <div className="mt-2 grid gap-3 md:grid-cols-3">
            <label className="space-y-1">
              <div className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">Discord bot user ID</div>
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
              <div className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">Default channels</div>
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
              <div className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">Default keywords</div>
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
    </div>
  );
}

export function DiscordSection({
  canManage,
  tools = [],
  onSelectedJobChange,
  className,
}: {
  canManage: boolean;
  tools?: AdminToolDefinition[];
  onSelectedJobChange?: (job: EventAgentJobControl | null) => void;
  className?: string;
}) {
  const [loading, setLoading] = useState(true);
  const [savingToken, setSavingToken] = useState(false);
  const [savingControl, setSavingControl] = useState(false);
  const [runningJobId, setRunningJobId] = useState<string | null>(null);
  const [dispatchingEvent, setDispatchingEvent] = useState(false);
  const [togglingRuntime, setTogglingRuntime] = useState(false);
  const [resettingRuntime, setResettingRuntime] = useState(false);
  const [notice, setNotice] = useState<Notice>(null);
  const [error, setError] = useState<string>("");
  const [status, setStatus] = useState<EventAgentControlResponse | null>(null);
  const [draft, setDraft] = useState<DraftControl | null>(null);
  const [draftToken, setDraftToken] = useState("");
  const [jsonDrafts, setJsonDrafts] = useState<JsonDrafts>({});
  const [eventSourceKind, setEventSourceKind] = useState("github");
  const [eventKind, setEventKind] = useState("issues.opened");
  const [eventPayloadDraft, setEventPayloadDraft] = useState('{\n  "repository": "open-hax/openplanner",\n  "title": "Example event",\n  "content": "Investigate this issue"\n}');
  const [jobSearch, setJobSearch] = useState("");
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);

  const runtimeJobs = useMemo(() => status?.runtime.jobs ?? [], [status]);
  const availableRoles = useMemo(() => status?.availableRoles ?? [], [status]);
  const availableSourceKinds = useMemo(() => status?.availableSourceKinds ?? [], [status]);
  const availableTriggerKinds = useMemo(() => status?.availableTriggerKinds ?? [], [status]);
  const availableToolIds = useMemo(() => tools.map((tool) => tool.id).sort(), [tools]);
  const recentEventCount = Array.isArray(status?.runtime.sources?.recentEvents)
    ? (status?.runtime.sources?.recentEvents as unknown[]).length
    : 0;
  const discordRuntime = status?.runtime.sources?.discord as Record<string, unknown> | undefined;
  const seenDiscordChannels = discordRuntime && Array.isArray(discordRuntime.lastSeenChannels)
    ? (discordRuntime.lastSeenChannels as unknown[]).length
    : 0;

  const filteredJobs = useMemo(() => {
    if (!draft) return [];
    const query = normalizeSearch(jobSearch);
    if (!query) return draft.jobs;
    return draft.jobs.filter((job) => jobSearchText(job).includes(query));
  }, [draft, jobSearch]);

  const selectedJob = useMemo(() => {
    if (!filteredJobs.length) return null;
    return filteredJobs.find((job) => job.id === selectedJobId) ?? filteredJobs[0] ?? null;
  }, [filteredJobs, selectedJobId]);

  useEffect(() => {
    onSelectedJobChange?.(selectedJob);
  }, [onSelectedJobChange, selectedJob]);

  const selectedRuntime = useMemo(
    () => (selectedJob ? runtimeForJob(runtimeJobs, selectedJob.id) : null),
    [runtimeJobs, selectedJob],
  );

  const selectedJobJsonDraft = selectedJob
    ? (jsonDrafts[selectedJob.id] ?? { sourceConfig: "{}", filters: "{}", toolPolicies: "[]" })
    : null;

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

  useEffect(() => {
    const nextIds = filteredJobs.map((job) => job.id);
    setSelectedJobId((current) => {
      if (current && nextIds.includes(current)) return current;
      return nextIds[0] ?? null;
    });
  }, [filteredJobs]);

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

  const handleStopRuntime = useCallback(async () => {
    if (!canManage) return;
    setTogglingRuntime(true);
    setError("");
    setNotice(null);
    try {
      const updated = await stopEventAgentRuntime();
      setStatus(updated);
      setDraft(updated.control);
      setJsonDrafts(seedJsonDrafts(updated.control.jobs));
      setNotice({ tone: "success", text: "Event-agent runtime stopped (schedulers cleared)." });
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setTogglingRuntime(false);
    }
  }, [canManage]);

  const handleStartRuntime = useCallback(async () => {
    if (!canManage) return;
    setTogglingRuntime(true);
    setError("");
    setNotice(null);
    try {
      const updated = await startEventAgentRuntime();
      setStatus(updated);
      setDraft(updated.control);
      setJsonDrafts(seedJsonDrafts(updated.control.jobs));
      setNotice({ tone: "success", text: "Event-agent runtime started." });
      await load();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setTogglingRuntime(false);
    }
  }, [canManage, load]);

  const handleResetRuntime = useCallback(async () => {
    if (!canManage) return;
    setResettingRuntime(true);
    setError("");
    setNotice(null);
    try {
      const updated = await resetEventAgentRuntime();
      setStatus(updated);
      setDraft(updated.control);
      setJsonDrafts(seedJsonDrafts(updated.control.jobs));
      const preservedCronJobs = updated.reset.preservedCronJobCount ?? 0;
      setNotice({
        tone: "success",
        text: `Event-agent runtime reset. Cleared ${updated.reset.deletedCount} persisted state key(s). Preserved ${preservedCronJobs} cron schedule(s); runtime is stopped for review.`,
      });
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setResettingRuntime(false);
    }
  }, [canManage]);

  return (
    <SectionCard className={className}>
      {loading || !draft || !status ? (
        <div className="text-sm text-slate-300">Loading event-agent control plane…</div>
      ) : (
        <div className="flex flex-col h-full min-h-0">
          <div className="min-h-0 flex-1 overflow-hidden">
            <div className="grid h-full min-h-0 min-w-[44rem] gap-3 grid-cols-[12rem_minmax(0,1fr)] xl:grid-cols-[13rem_minmax(0,1fr)]">
            <aside className="flex flex-col overflow-hidden h-full space-y-2 rounded-xl border border-slate-800 bg-slate-950/50 p-2.5">
              <div className="flex items-center justify-between gap-2">
                <div className="text-sm font-semibold text-slate-100">Agents</div>
                <div className="text-[11px] text-slate-500">{filteredJobs.length}/{draft.jobs.length}</div>
              </div>

              <div className="grid gap-2">
                <button
                  type="button"
                  onClick={() => void load()}
                  disabled={loading || savingToken || savingControl || togglingRuntime || resettingRuntime}
                  className="inline-flex items-center justify-center rounded-md border border-slate-700 bg-slate-900 px-3 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
                >
                  {loading ? "Loading…" : "Refresh"}
                </button>
                <button
                  type="button"
                  onClick={() => void handleSaveControl()}
                  disabled={!canManage || !draft || savingControl || togglingRuntime || resettingRuntime}
                  className="inline-flex items-center justify-center rounded-md bg-sky-600 px-3 py-2 text-sm font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60"
                >
                  {savingControl ? "Saving…" : "Save runtime"}
                </button>

                {status.runtime.running ? (
                  <button
                    type="button"
                    onClick={() => void handleStopRuntime()}
                    disabled={!canManage || togglingRuntime || resettingRuntime}
                    className="inline-flex items-center justify-center rounded-md bg-rose-700 px-3 py-2 text-sm font-semibold text-slate-50 hover:bg-rose-600 disabled:opacity-60"
                    title="Stops cron scheduling + unsubscribes Discord gateway. Does not hard-cancel an in-flight LLM request."
                  >
                    {togglingRuntime ? "Stopping…" : "Stop runtime"}
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => void handleStartRuntime()}
                    disabled={!canManage || togglingRuntime || resettingRuntime}
                    className="inline-flex items-center justify-center rounded-md bg-emerald-700 px-3 py-2 text-sm font-semibold text-slate-50 hover:bg-emerald-600 disabled:opacity-60"
                  >
                    {togglingRuntime ? "Starting…" : "Start runtime"}
                  </button>
                )}

                <button
                  type="button"
                  onClick={() => void handleResetRuntime()}
                  disabled={!canManage || togglingRuntime || resettingRuntime || savingControl}
                  className="inline-flex items-center justify-center rounded-md border border-amber-700 bg-amber-950/40 px-3 py-2 text-sm font-semibold text-amber-100 hover:bg-amber-900/60 disabled:opacity-60"
                  title="Stop the runtime, clear persisted event-agent state, reload contract-derived schedules, and leave the scheduler stopped for review."
                >
                  {resettingRuntime ? "Resetting…" : "Full reset"}
                </button>
              </div>

              <div className="grid gap-2">
                <div className="rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2">
                  <div className="text-[11px] uppercase tracking-wide text-slate-500">Discord token</div>
                  <div className="mt-1 flex items-center gap-2 text-xs text-slate-200">
                    {status.configured ? <Badge tone="success">Configured</Badge> : <Badge tone="warn">Missing</Badge>}
                    {status.tokenPreview ? <span className="font-mono text-[11px] text-slate-400">{status.tokenPreview}</span> : null}
                  </div>
                </div>
                <div className="rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2">
                  <div className="text-[11px] uppercase tracking-wide text-slate-500">Runtime</div>
                  <div className="mt-1 flex items-center gap-2 text-xs text-slate-200">
                    {status.runtime.running ? <Badge tone="success">Running</Badge> : <Badge tone="warn">Stopped</Badge>}
                    <span>{draft.jobs.length} jobs</span>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div className="rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2">
                    <div className="text-[11px] uppercase tracking-wide text-slate-500">Recent events</div>
                    <div className="mt-1 text-lg font-semibold text-slate-100">{recentEventCount}</div>
                    <div className="text-[11px] text-slate-500">Buffered</div>
                  </div>
                  <div className="rounded-lg border border-slate-800 bg-slate-950/40 px-3 py-2">
                    <div className="text-[11px] uppercase tracking-wide text-slate-500">Freshness</div>
                    <div className="mt-1 text-lg font-semibold text-slate-100">{seenDiscordChannels}</div>
                    <div className="text-[11px] text-slate-500">Channels</div>
                  </div>
                </div>
              </div>

              <label className="space-y-1">
                <div className="sr-only">Search</div>
                <input
                  aria-label="Search"
                  value={jobSearch}
                  onChange={(event) => setJobSearch(event.target.value)}
                  placeholder="Search…"
                  className="w-full rounded-md border border-slate-800 bg-slate-950/80 px-2.5 py-2 text-sm text-slate-100 outline-none focus:border-sky-500"
                />
              </label>

              <div className="flex-1 min-h-0 space-y-1.5 overflow-y-auto pr-1">
                {filteredJobs.length > 0 ? filteredJobs.map((job) => (
                  <SidebarJobButton
                    key={job.id}
                    job={job}
                    runtime={runtimeForJob(runtimeJobs, job.id)}
                    active={selectedJob?.id === job.id}
                    onSelect={() => setSelectedJobId(job.id)}
                  />
                )) : (
                  <div className="rounded-xl border border-dashed border-slate-800 px-3 py-6 text-center text-sm text-slate-500">
                    No event agents match this search.
                  </div>
                )}
              </div>
            </aside>

            <div className="grid gap-3 min-w-0 h-full min-h-0 grid-rows-[auto_minmax(0,1fr)] grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]">
              <RuntimeInfrastructureForm
                canManage={canManage}
                draft={draft}
                status={status}
                draftToken={draftToken}
                setDraft={setDraft}
                setDraftToken={setDraftToken}
                savingToken={savingToken}
                savingControl={savingControl}
                onSaveToken={() => void handleSaveToken()}
              />

              <div className="flex flex-col h-full overflow-hidden rounded-xl border border-slate-800 bg-slate-950/40">
                <div className="shrink-0 border-b border-slate-800 px-3 py-2">
                  <div className="text-sm font-semibold text-slate-100">Schedule review</div>
                  <div className="text-[11px] text-slate-500">Review cadence and next-run timing.</div>
                </div>
                <div className="flex-1 min-h-0 overflow-y-auto p-2">
                  <EventAgentScheduleReview
                    jobs={draft.jobs}
                    runtimeJobs={runtimeJobs}
                    selectedJobId={selectedJob?.id ?? null}
                    onSelectJob={(jobId) => setSelectedJobId(jobId)}
                  />
                </div>
              </div>

              <div className="h-full overflow-y-auto min-w-0 space-y-3 pr-1">
                {selectedJob ? (
                  <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-3">
                    <div className="flex flex-col gap-3 border-b border-slate-800 pb-4 md:flex-row md:items-start md:justify-between">
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <h3 className="text-lg font-semibold text-slate-100">{selectedJob.name}</h3>
                          <Badge tone={selectedJob.enabled ? "success" : "warn"}>{selectedJob.enabled ? "Enabled" : "Disabled"}</Badge>
                          <span className="text-xs text-slate-500">{selectedJob.source.kind} · {selectedJob.trigger.kind} · {selectedJob.contractSourceId ? "contract" : "custom"}</span>
                          {selectedRuntime?.running ? <Badge tone="info">Running now</Badge> : null}
                        </div>
                        <p className="mt-2 text-sm text-slate-400">{selectedJob.description || "No description provided."}</p>
                        {selectedJob.contractSourceId ? (
                          <div className="mt-2 text-xs text-slate-500">
                            Contract-backed from <code className="font-mono text-slate-300">{selectedJob.contractSourceKind ?? "agent"}:{selectedJob.contractSourceId}</code>
                            {typeof selectedJob.contractHash === "number" ? (
                              <span> · hash <code className="font-mono text-slate-300">{selectedJob.contractHash}</code></span>
                            ) : null}
                          </div>
                        ) : null}
                      </div>
                      <button
                        type="button"
                        onClick={() => void handleRunJob(selectedJob.id)}
                        disabled={!canManage || runningJobId === selectedJob.id}
                        className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
                      >
                        {runningJobId === selectedJob.id ? "Queueing…" : "Run now"}
                      </button>
                    </div>

                    <div className="mt-4 space-y-4">
                    <div className="grid gap-4 xl:grid-cols-3">
                      <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-4">
                        <div className="text-sm font-semibold text-slate-100">Runtime snapshot</div>
                        <div className="mt-3 grid gap-x-4 gap-y-2 sm:grid-cols-2 text-sm">
                          <div>
                            <div className="text-[11px] uppercase tracking-wide text-slate-500">Status</div>
                            <div className="mt-1"><Badge tone={runtimeStatusTone(selectedRuntime?.lastStatus)}>{selectedRuntime?.lastStatus ?? "idle"}</Badge></div>
                          </div>
                          <div>
                            <div className="text-[11px] uppercase tracking-wide text-slate-500">Runs</div>
                            <div className="mt-1 text-lg font-semibold text-slate-100">{selectedRuntime?.runCount ?? 0}</div>
                          </div>
                          <div>
                            <div className="text-[11px] uppercase tracking-wide text-slate-500">Last finished</div>
                            <div className="mt-1 text-sm text-slate-200">{toLocalDateTime(selectedRuntime?.lastFinishedAt)}</div>
                          </div>
                          <div>
                            <div className="text-[11px] uppercase tracking-wide text-slate-500">Next run</div>
                            <div className="mt-1 text-sm text-slate-200">{toLocalDateTime(selectedRuntime?.nextRunAt)}</div>
                          </div>
                        </div>
                      </div>

                      <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-4">
                        <div className="text-sm font-semibold text-slate-100">Live runtime</div>
                        <div className="mt-3 grid gap-x-4 gap-y-2 text-sm text-slate-300">
                          <div className="flex items-center justify-between gap-3"><span className="text-slate-500">Schedule</span><span className="text-right text-slate-200">{selectedRuntime?.scheduleLabel ?? "—"}</span></div>
                          <div className="flex items-center justify-between gap-3"><span className="text-slate-500">Last started</span><span className="text-right text-xs text-slate-200">{toLocalDateTime(selectedRuntime?.lastStartedAt)}</span></div>
                          <div className="flex items-center justify-between gap-3"><span className="text-slate-500">Last finished</span><span className="text-right text-xs text-slate-200">{toLocalDateTime(selectedRuntime?.lastFinishedAt)}</span></div>
                          <div className="flex items-center justify-between gap-3"><span className="text-slate-500">Duration</span><span className="text-right text-slate-200">{selectedRuntime?.lastDurationMs ? `${selectedRuntime.lastDurationMs} ms` : "—"}</span></div>
                        </div>
                        {selectedRuntime?.lastError ? (
                          <div className="mt-3 rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-xs text-rose-200">
                            {selectedRuntime.lastError}
                          </div>
                        ) : null}
                      </div>

                      <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-4">
                        <div className="text-sm font-semibold text-slate-100">Quick reference</div>
                        <div className="mt-3 space-y-2 text-xs text-slate-400">
                          <div><span className="text-slate-500">Job id:</span> <code className="font-mono text-slate-200">{selectedJob.id}</code></div>
                          <div><span className="text-slate-500">Source mode:</span> {selectedJob.source.mode}</div>
                          <div><span className="text-slate-500">Trigger cadence:</span> {selectedJob.trigger.cadenceMinutes} min</div>
                          <div><span className="text-slate-500">Event kinds:</span> {selectedJob.trigger.eventKinds.length > 0 ? selectedJob.trigger.eventKinds.join(", ") : "none"}</div>
                        </div>
                      </div>
                    </div>

                    <div className="space-y-4">
                      <div className="grid gap-3 md:grid-cols-3">
                        <div className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Enabled</div>
                          <label className="inline-flex w-full items-center gap-2 rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-200">
                            <input
                              type="checkbox"
                              checked={selectedJob.enabled}
                              onChange={(event) => updateJob(selectedJob.id, { enabled: event.target.checked })}
                              disabled={!canManage || savingControl}
                            />
                            Active
                          </label>
                        </div>

                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Trigger kind</div>
                          <select
                            value={selectedJob.trigger.kind}
                            onChange={(event) => updateJob(selectedJob.id, { trigger: { ...selectedJob.trigger, kind: event.target.value } })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          >
                            {availableTriggerKinds.map((kind) => <option key={`${selectedJob.id}-trigger-${kind}`} value={kind}>{kind}</option>)}
                          </select>
                        </label>

                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Source kind</div>
                          <select
                            value={selectedJob.source.kind}
                            onChange={(event) => updateJob(selectedJob.id, { source: { ...selectedJob.source, kind: event.target.value } })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          >
                            {availableSourceKinds.map((kind) => <option key={`${selectedJob.id}-source-${kind}`} value={kind}>{kind}</option>)}
                          </select>
                        </label>
                      </div>

                      <div className="grid gap-3 md:grid-cols-3">
                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Source mode</div>
                          <input
                            value={selectedJob.source.mode}
                            onChange={(event) => updateJob(selectedJob.id, { source: { ...selectedJob.source, mode: event.target.value } })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          />
                        </label>

                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Cadence (minutes)</div>
                          <input
                            type="number"
                            min={1}
                            max={10080}
                            value={selectedJob.trigger.cadenceMinutes}
                            onChange={(event) => updateJob(selectedJob.id, { trigger: { ...selectedJob.trigger, cadenceMinutes: Number(event.target.value || 1) } })}
                            disabled={!canManage || savingControl || selectedJob.trigger.kind !== "cron"}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          />
                        </label>

                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Event kinds</div>
                          <input
                            value={joinCsv(selectedJob.trigger.eventKinds)}
                            onChange={(event) => updateJob(selectedJob.id, { trigger: { ...selectedJob.trigger, eventKinds: splitCsv(event.target.value) } })}
                            disabled={!canManage || savingControl || selectedJob.trigger.kind !== "event"}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                            placeholder="mention, issues.opened"
                          />
                        </label>
                      </div>

                      <div className="grid gap-3 md:grid-cols-3">
                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Role</div>
                          <select
                            value={selectedJob.agentSpec.role}
                            onChange={(event) => updateJob(selectedJob.id, { agentSpec: { ...selectedJob.agentSpec, role: event.target.value } })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          >
                            {availableRoles.map((role) => <option key={`${selectedJob.id}-role-${role}`} value={role}>{role}</option>)}
                          </select>
                        </label>
                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Model</div>
                          <input
                            value={selectedJob.agentSpec.model}
                            onChange={(event) => updateJob(selectedJob.id, { agentSpec: { ...selectedJob.agentSpec, model: event.target.value } })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          />
                        </label>
                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Thinking</div>
                          <select
                            value={selectedJob.agentSpec.thinkingLevel}
                            onChange={(event) => updateJob(selectedJob.id, { agentSpec: { ...selectedJob.agentSpec, thinkingLevel: event.target.value } })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          >
                            {["off", "minimal", "low", "medium", "high", "xhigh"].map((value) => (
                              <option key={`${selectedJob.id}-thinking-${value}`} value={value}>{value}</option>
                            ))}
                          </select>
                        </label>
                      </div>

                      <label className="space-y-1">
                        <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Job description</div>
                        <input
                          value={selectedJob.description ?? ""}
                          onChange={(event) => updateJob(selectedJob.id, { description: event.target.value })}
                          disabled={!canManage || savingControl}
                          className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                        />
                      </label>

                      <label className="space-y-1">
                        <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">System prompt</div>
                        <textarea
                          value={selectedJob.agentSpec.systemPrompt}
                          onChange={(event) => updateJob(selectedJob.id, { agentSpec: { ...selectedJob.agentSpec, systemPrompt: event.target.value } })}
                          disabled={!canManage || savingControl}
                          rows={4}
                          className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                        />
                      </label>

                      {selectedJobJsonDraft ? (
                        <CollapsiblePanel
                          title="Advanced JSON"
                          description="Source config, filters, and tool policies stay available, but hidden unless you need them."
                        >
                          <div className="grid gap-3 xl:grid-cols-3">
                            <label className="space-y-1 xl:col-span-1">
                              <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Source config JSON</div>
                              <textarea
                                value={selectedJobJsonDraft.sourceConfig}
                                onChange={(event) => updateJsonDraft(selectedJob.id, "sourceConfig", event.target.value)}
                                disabled={!canManage || savingControl}
                                rows={8}
                                className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 font-mono text-xs text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                              />
                            </label>
                            <label className="space-y-1 xl:col-span-1">
                              <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Filters JSON</div>
                              <textarea
                                value={selectedJobJsonDraft.filters}
                                onChange={(event) => updateJsonDraft(selectedJob.id, "filters", event.target.value)}
                                disabled={!canManage || savingControl}
                                rows={8}
                                className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 font-mono text-xs text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                              />
                            </label>
                            <label className="space-y-1 xl:col-span-1">
                              <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Tool policies JSON</div>
                              <textarea
                                value={selectedJobJsonDraft.toolPolicies}
                                onChange={(event) => updateJsonDraft(selectedJob.id, "toolPolicies", event.target.value)}
                                disabled={!canManage || savingControl}
                                rows={8}
                                className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 font-mono text-xs text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                              />
                              <div className="text-[11px] text-slate-500">Available tools: {availableToolIds.join(", ") || "(tool catalog unavailable)"}</div>
                            </label>
                          </div>
                        </CollapsiblePanel>
                      ) : null}
                    </div>
                  </div>
                </div>
              ) : (
                <div className="rounded-2xl border border-dashed border-slate-800 bg-slate-950/30 px-6 py-10 text-center text-sm text-slate-400">
                  Select an event agent from the sidebar to inspect it.
                </div>
              )}

              <div className="space-y-4">
                <CollapsiblePanel
                  title="Dispatch test event"
                  description="Simulate GitHub, Discord, cron, or manual events against the matcher without expanding every agent panel."
                >
                  <div className="grid gap-3 md:grid-cols-[0.8fr_1.2fr]">
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
                  </div>
                  <label className="mt-3 block space-y-1">
                    <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Payload JSON</div>
                    <textarea
                      value={eventPayloadDraft}
                      onChange={(event) => setEventPayloadDraft(event.target.value)}
                      disabled={!canManage || dispatchingEvent}
                      rows={7}
                      className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-xs font-mono text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                    />
                  </label>
                  <button
                    type="button"
                    onClick={() => void handleDispatchEvent()}
                    disabled={!canManage || dispatchingEvent}
                    className="mt-3 inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
                  >
                    {dispatchingEvent ? "Dispatching…" : "Dispatch"}
                  </button>
                </CollapsiblePanel>
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
            </div>
            </div>
          </div>
        </div>
      )}
    </SectionCard>
  );
}
