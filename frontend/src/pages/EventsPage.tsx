import {
  dispatchEventAgentEvent,
  getDiscordConfig,
  listAdminTools,
  getEventAgentControl,
  resetEventAgentRuntime,
  runEventAgentJob,
  startEventAgentRuntime,
  stopEventAgentRuntime,
  updateDiscordConfig,
  updateEventAgentControl,
  type EventAgentControlResponse,
  type EventAgentJobControl,
} from "../lib/api/admin";
import { jobSearchText, normalizeSearch, parseEventControl, runtimeForJob, seedJsonDrafts, type DraftControl, type JsonDrafts } from "../components/admin-page/helpers";
import React, { useCallback, useEffect, useMemo, useState } from "react";

import { DiscordSection } from "../components/admin-page/DiscordSection";
import type { AdminToolDefinition } from "../lib/types";
import { useAuth } from "./useAuth";

export default function EventsPage() {
  const auth = useAuth();
  const [tools, setTools] = useState<AdminToolDefinition[]>([]);
  const [toolsError, setToolsError] = useState<string | null>(null);

  const canControlEvents = useMemo(
    () => Boolean(auth.isSystemAdmin || auth.permissions.includes("org.event_agents.control")),
    [auth.isSystemAdmin, auth.permissions],
  );

  const canReadToolCatalog = useMemo(
    () => Boolean(
      auth.isSystemAdmin
      || auth.permissions.includes("org.tool_policy.read")
      || auth.permissions.includes("platform.roles.manage")
      || auth.permissions.includes("org.user_policy.read"),
    ),
    [auth.isSystemAdmin, auth.permissions],
  );

  useEffect(() => {
    if (!canControlEvents || !canReadToolCatalog) {
      setTools([]);
      return;
    }

    let cancelled = false;
    void listAdminTools()
      .then((result) => {
        if (!cancelled) {
          setTools(result.tools);
          setToolsError(null);
        }
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setTools([]);
          setToolsError(error instanceof Error ? error.message : String(error));
        }
      });

    return () => {
      cancelled = true;
    };
  }, [canControlEvents, canReadToolCatalog]);

  return (
    <div data-page="events" className="flex flex-col h-full min-h-0 overflow-hidden bg-slate-950 p-4 text-slate-100">
      <div className="mb-4 space-y-1 shrink-0">
        <h1 className="text-xl font-semibold text-slate-100">Events</h1>
        <p className="text-sm text-slate-400">
          Generic event runtime control: schedules, triggers, dispatch, and reset.
        </p>
      </div>

      {!canControlEvents ? (
        <div className="shrink-0 rounded-lg border border-slate-800 bg-slate-900/40 p-4 text-sm text-slate-400">
          Event runtime control access required.
        </div>
      ) : (
        <div className="flex flex-col min-h-0 flex-1 gap-4">
          {toolsError ? (
            <div className="shrink-0 rounded-lg border border-amber-700 bg-amber-950/30 p-3 text-xs text-amber-200">
              Tool catalog unavailable: {toolsError}
            </div>
          ) : null}
          <DiscordSection className="min-h-0 flex-1" canManage={canControlEvents} tools={tools} />
        </div>
      )}
    </div>
  );
}

type Notice = { tone: "success" | "error"; text: string } | null;

export type EventRuntimeOptions = {
  canManage: boolean;
  tools?: AdminToolDefinition[];
  onSelectedJobChange?: (job: EventAgentJobControl | null) => void;
};

/** Own the editable event runtime and its requests for both Events compositions. */
export function useEventRuntimeController({ canManage, tools = [], onSelectedJobChange }: EventRuntimeOptions) {
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

  const parseControlForSave = useCallback(
    () => parseEventControl(draft, jsonDrafts), [draft, jsonDrafts],
  );

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

  return {
    loading, savingToken, savingControl, runningJobId,
    dispatchingEvent, togglingRuntime, resettingRuntime, notice,
    error, status, draft, setDraft,
    draftToken, setDraftToken, eventSourceKind, setEventSourceKind,
    eventKind, setEventKind, eventPayloadDraft, setEventPayloadDraft,
    jobSearch, setJobSearch, setSelectedJobId, runtimeJobs,
    availableRoles, availableSourceKinds, availableTriggerKinds, availableToolIds,
    recentEventCount, seenDiscordChannels, filteredJobs, selectedJob,
    selectedRuntime, selectedJobJsonDraft, load, updateJob,
    updateJsonDraft, handleSaveToken, handleSaveControl, handleRunJob,
    handleDispatchEvent, handleStopRuntime, handleStartRuntime, handleResetRuntime,
  };
}

export type EventRuntimeController = ReturnType<typeof useEventRuntimeController>;
