import React, { useCallback, useEffect, useMemo, useState } from "react";

import {
  getDiscordAgentControl,
  getDiscordConfig,
  runDiscordAgentJob,
  updateDiscordAgentControl,
  updateDiscordConfig,
  type DiscordAgentControlResponse,
  type DiscordAgentJobControl,
  type DiscordAgentRuntimeJob,
} from "../../lib/api/admin";
import { Badge, SectionCard } from "./common";

type Notice = { tone: "success" | "error"; text: string } | null;
type DraftControl = DiscordAgentControlResponse["control"];

function splitCsv(value: string): string[] {
  return value
    .split(",")
    .map((entry) => entry.trim())
    .filter(Boolean);
}

function joinCsv(values: string[] | undefined): string {
  return (values ?? []).join(", ");
}

function toLocalDateTime(value?: number): string {
  if (!value || !Number.isFinite(value)) return "—";
  try {
    return new Date(value).toLocaleString();
  } catch {
    return String(value);
  }
}

function runtimeForJob(runtimeJobs: DiscordAgentRuntimeJob[], jobId: string): DiscordAgentRuntimeJob | null {
  return runtimeJobs.find((job) => job.id === jobId) ?? null;
}

export function DiscordSection({ canManage }: { canManage: boolean }) {
  const [loading, setLoading] = useState(true);
  const [savingToken, setSavingToken] = useState(false);
  const [savingControl, setSavingControl] = useState(false);
  const [runningJobId, setRunningJobId] = useState<string | null>(null);
  const [notice, setNotice] = useState<Notice>(null);
  const [error, setError] = useState<string>("");
  const [status, setStatus] = useState<DiscordAgentControlResponse | null>(null);
  const [draftToken, setDraftToken] = useState("");
  const [draft, setDraft] = useState<DraftControl | null>(null);

  const availableRoles = useMemo(() => status?.availableRoles ?? [], [status]);
  const runtimeJobs = useMemo(() => status?.runtime.jobs ?? [], [status]);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    setNotice(null);
    try {
      const [tokenStatus, controlStatus] = await Promise.all([
        getDiscordConfig(),
        getDiscordAgentControl(),
      ]);
      setStatus({
        ...controlStatus,
        configured: tokenStatus.configured,
        tokenPreview: tokenStatus.tokenPreview,
      });
      setDraft(controlStatus.control);
      setDraftToken("");
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

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
      setStatus((current) => current ? { ...current, configured: updated.configured, tokenPreview: updated.tokenPreview } : current);
      setDraftToken("");
      setNotice({ tone: "success", text: `Discord bot token saved. Preview: ${updated.tokenPreview}` });
      await load();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setSavingToken(false);
    }
  }, [canManage, draftToken, load]);

  const updateJob = useCallback((jobId: string, patch: Partial<DiscordAgentJobControl>) => {
    setDraft((current) => {
      if (!current) return current;
      return {
        ...current,
        jobs: current.jobs.map((job) => (job.id === jobId ? { ...job, ...patch } : job)),
      };
    });
  }, []);

  const handleSaveControl = useCallback(async () => {
    if (!canManage || !draft) return;
    setSavingControl(true);
    setError("");
    setNotice(null);
    try {
      const updated = await updateDiscordAgentControl(draft);
      setStatus(updated);
      setDraft(updated.control);
      setNotice({ tone: "success", text: "Discord agent control plane updated and worker reloaded." });
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setSavingControl(false);
    }
  }, [canManage, draft]);

  const handleRunJob = useCallback(async (jobId: string) => {
    if (!canManage) return;
    setRunningJobId(jobId);
    setNotice(null);
    setError("");
    try {
      await runDiscordAgentJob(jobId);
      setNotice({ tone: "success", text: `Queued Discord job ${jobId}.` });
      await load();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setRunningJobId(null);
    }
  }, [canManage, load]);

  return (
    <SectionCard
      title="Discord agents"
      description="Control Knoxx's Discord worker, schedules, targeting, prompts, roles, and runtime behavior from one dashboard."
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
            {savingControl ? "Saving…" : "Save control plane"}
          </button>
        </div>
      }
    >
      {loading || !draft || !status ? (
        <div className="text-sm text-slate-300">Loading Discord agent control plane…</div>
      ) : (
        <div className="space-y-6">
          <div className="grid gap-3 md:grid-cols-4">
            <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Bot token</div>
              <div className="mt-2 flex items-center gap-2 text-sm text-slate-200">
                {status.configured ? <Badge tone="success">Configured</Badge> : <Badge tone="warn">Missing</Badge>}
                {status.tokenPreview ? <span className="font-mono text-xs text-slate-400">{status.tokenPreview}</span> : null}
              </div>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Worker</div>
              <div className="mt-2 flex items-center gap-2 text-sm text-slate-200">
                {status.runtime.running ? <Badge tone="success">Running</Badge> : <Badge tone="warn">Stopped</Badge>}
                <span>{status.runtime.channelCount} default channels</span>
              </div>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Queued signals</div>
              <div className="mt-2 text-2xl font-semibold text-slate-100">{status.runtime.mentionQueueCount}</div>
              <div className="mt-1 text-xs text-slate-500">Pending mention / keyword hits</div>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Seen channels</div>
              <div className="mt-2 text-2xl font-semibold text-slate-100">{status.runtime.lastSeenChannels.length}</div>
              <div className="mt-1 text-xs text-slate-500">Channels with freshness state</div>
            </div>
          </div>

          <div className="grid gap-4 xl:grid-cols-[0.9fr_1.1fr]">
            <div className="space-y-4 rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div>
                <div className="text-sm font-semibold text-slate-100">Bot token</div>
                <div className="text-xs text-slate-500">Rotate credentials here without leaving the dashboard.</div>
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
                <div className="text-xs text-slate-500">Leave blank to keep the current token.</div>
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
                <div className="text-sm font-semibold text-slate-100">Global targeting</div>
                <div className="text-xs text-slate-500">Seed defaults for the whole Discord surface. Jobs can override channels and keywords individually.</div>
              </div>

              <label className="space-y-1">
                <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Bot user ID</div>
                <input
                  value={draft.botUserId}
                  onChange={(event) => setDraft({ ...draft, botUserId: event.target.value })}
                  disabled={!canManage || savingControl}
                  className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                  placeholder="Discord bot user ID for precise @mention detection"
                />
              </label>

              <label className="space-y-1">
                <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Default channels</div>
                <input
                  value={joinCsv(draft.defaultChannels)}
                  onChange={(event) => setDraft({ ...draft, defaultChannels: splitCsv(event.target.value) })}
                  disabled={!canManage || savingControl}
                  className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                  placeholder="Comma-separated Discord channel IDs"
                />
              </label>

              <label className="space-y-1">
                <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Target keywords</div>
                <input
                  value={joinCsv(draft.targetKeywords)}
                  onChange={(event) => setDraft({ ...draft, targetKeywords: splitCsv(event.target.value).map((value) => value.toLowerCase()) })}
                  disabled={!canManage || savingControl}
                  className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                  placeholder="knoxx, cephalon, urgent"
                />
              </label>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <div className="text-sm font-semibold text-slate-100">Job controls</div>
              <div className="text-xs text-slate-500">Each job has its own cadence, role, prompts, model, and targeting scope. The panel is seeded from the current backend state.</div>
            </div>

            <div className="space-y-4">
              {draft.jobs.map((job) => {
                const runtime = runtimeForJob(runtimeJobs, job.id);
                return (
                  <div key={job.id} className="rounded-2xl border border-slate-800 bg-slate-950/40 p-4">
                    <div className="flex flex-col gap-3 border-b border-slate-800 pb-4 md:flex-row md:items-start md:justify-between">
                      <div>
                        <div className="flex flex-wrap items-center gap-2">
                          <h3 className="text-base font-semibold text-slate-100">{job.name}</h3>
                          <Badge tone={job.enabled ? "success" : "warn"}>{job.enabled ? "Enabled" : "Disabled"}</Badge>
                          <Badge tone="info">{job.kind}</Badge>
                          {runtime?.running ? <Badge tone="success">Running now</Badge> : null}
                        </div>
                        <p className="mt-1 text-sm text-slate-400">{job.description}</p>
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

                    <div className="mt-4 grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
                      <div className="space-y-4">
                        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                          <label className="space-y-1">
                            <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Enabled</div>
                            <label className="inline-flex items-center gap-2 rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-200">
                              <input
                                type="checkbox"
                                checked={job.enabled}
                                onChange={(event) => updateJob(job.id, { enabled: event.target.checked })}
                                disabled={!canManage || savingControl}
                              />
                              Run this job
                            </label>
                          </label>

                          <label className="space-y-1">
                            <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Cadence (minutes)</div>
                            <input
                              type="number"
                              min={1}
                              max={10080}
                              value={job.cadenceMinutes}
                              onChange={(event) => updateJob(job.id, { cadenceMinutes: Number(event.target.value || 1) })}
                              disabled={!canManage || savingControl}
                              className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                            />
                          </label>

                          <label className="space-y-1">
                            <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Max messages</div>
                            <input
                              type="number"
                              min={1}
                              max={100}
                              value={job.maxMessages}
                              onChange={(event) => updateJob(job.id, { maxMessages: Number(event.target.value || 1) })}
                              disabled={!canManage || savingControl}
                              className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                            />
                          </label>

                          <label className="space-y-1">
                            <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Role</div>
                            <select
                              value={job.role}
                              onChange={(event) => updateJob(job.id, { role: event.target.value })}
                              disabled={!canManage || savingControl}
                              className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                            >
                              {availableRoles.map((role) => (
                                <option key={`${job.id}-${role}`} value={role}>{role}</option>
                              ))}
                            </select>
                          </label>

                          <label className="space-y-1">
                            <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Model</div>
                            <input
                              value={job.model}
                              onChange={(event) => updateJob(job.id, { model: event.target.value })}
                              disabled={!canManage || savingControl}
                              className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                              placeholder="glm-5"
                            />
                          </label>

                          <label className="space-y-1">
                            <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Thinking</div>
                            <select
                              value={job.thinkingLevel}
                              onChange={(event) => updateJob(job.id, { thinkingLevel: event.target.value })}
                              disabled={!canManage || savingControl}
                              className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                            >
                              {['off', 'minimal', 'low', 'medium', 'high', 'xhigh'].map((value) => (
                                <option key={`${job.id}-thinking-${value}`} value={value}>{value}</option>
                              ))}
                            </select>
                          </label>
                        </div>

                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Channels override</div>
                          <input
                            value={joinCsv(job.channels)}
                            onChange={(event) => updateJob(job.id, { channels: splitCsv(event.target.value) })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                            placeholder="Leave empty to inherit default channels"
                          />
                        </label>

                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Keywords override</div>
                          <input
                            value={joinCsv(job.keywords)}
                            onChange={(event) => updateJob(job.id, { keywords: splitCsv(event.target.value).map((value) => value.toLowerCase()) })}
                            disabled={!canManage || savingControl}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                            placeholder="Leave empty to inherit default keywords"
                          />
                        </label>

                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">System prompt</div>
                          <textarea
                            value={job.systemPrompt}
                            onChange={(event) => updateJob(job.id, { systemPrompt: event.target.value })}
                            disabled={!canManage || savingControl}
                            rows={4}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          />
                        </label>

                        <label className="space-y-1">
                          <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Task prompt</div>
                          <textarea
                            value={job.taskPrompt}
                            onChange={(event) => updateJob(job.id, { taskPrompt: event.target.value })}
                            disabled={!canManage || savingControl}
                            rows={4}
                            className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                          />
                        </label>
                      </div>

                      <div className="space-y-3 rounded-xl border border-slate-800 bg-slate-950/60 p-4">
                        <div className="text-sm font-semibold text-slate-100">Live runtime</div>
                        <div className="grid gap-2 text-sm text-slate-300">
                          <div className="flex items-center justify-between gap-3">
                            <span className="text-slate-500">Schedule</span>
                            <span>{runtime?.scheduleLabel ?? `Every ${job.cadenceMinutes} minutes`}</span>
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
          </div>

          {!canManage ? (
            <div className="rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
              You do not have <code className="font-mono">platform.org.create</code> permission to mutate the Discord control plane.
            </div>
          ) : null}

          {notice ? (
            <div
              className={
                notice.tone === "success"
                  ? "rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-sm text-emerald-200"
                  : "rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200"
              }
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
