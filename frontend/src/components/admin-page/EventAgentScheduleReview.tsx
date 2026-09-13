import type { EventRuntimeController } from "../../pages/EventsPage";
import { compactText } from "./helpers";
import type { EventAgentJobControl, EventAgentRuntimeJob } from "../../lib/api/admin";
import { Badge, classNames } from "./common";

type ScheduleReviewProps = {
  jobs: EventAgentJobControl[];
  runtimeJobs: EventAgentRuntimeJob[];
  onSelectJob?: (jobId: string) => void;
  selectedJobId?: string | null;
};

type ScheduleRow = {
  job: EventAgentJobControl;
  runtime: EventAgentRuntimeJob | null;
  contractKey: string;
  nextRunAt: number | null;
};

function runtimeForJob(runtimeJobs: EventAgentRuntimeJob[], jobId: string): EventAgentRuntimeJob | null {
  return runtimeJobs.find((job) => job.id === jobId) ?? null;
}

function contractKeyForJob(job: EventAgentJobControl): string {
  if (job.contractSourceKey) return job.contractSourceKey;
  if (job.contractSourceId) return `${job.contractSourceKind ?? "agent"}:${job.contractSourceId}`;
  return `custom:${job.id}`;
}

function toLocalDateTime(value?: number | null): string {
  if (!value || !Number.isFinite(value)) return "—";
  try {
    return new Date(value).toLocaleString();
  } catch {
    return String(value);
  }
}

function sortRows(left: ScheduleRow, right: ScheduleRow): number {
  const leftCron = left.job.trigger.kind === "cron" ? 0 : 1;
  const rightCron = right.job.trigger.kind === "cron" ? 0 : 1;
  if (leftCron !== rightCron) return leftCron - rightCron;

  const leftNext = left.nextRunAt ?? Number.MAX_SAFE_INTEGER;
  const rightNext = right.nextRunAt ?? Number.MAX_SAFE_INTEGER;
  if (leftNext !== rightNext) return leftNext - rightNext;

  return left.contractKey.localeCompare(right.contractKey);
}

function statusTone(runtime: EventAgentRuntimeJob | null, enabled: boolean): "default" | "success" | "warn" | "danger" | "info" {
  if (!enabled) return "warn";
  if (runtime?.running) return "info";
  if (runtime?.lastStatus === "ok") return "success";
  if (runtime?.lastStatus === "error") return "danger";
  return "default";
}

export function EventAgentScheduleReview({ jobs, runtimeJobs, onSelectJob, selectedJobId }: ScheduleReviewProps) {
  const rows = jobs
    .map((job) => {
      const runtime = runtimeForJob(runtimeJobs, job.id);
      return {
        job,
        runtime,
        contractKey: contractKeyForJob(job),
        nextRunAt: typeof runtime?.nextRunAt === "number" ? runtime.nextRunAt : null,
      } satisfies ScheduleRow;
    })
    .sort(sortRows);

  return (
    <div className="overflow-x-auto">
      <table className="min-w-full border-separate border-spacing-0 text-left text-sm">
        <thead>
          <tr className="text-xs uppercase tracking-wide text-slate-500">
            <th className="border-b border-slate-800 px-3 py-2 font-medium">Contract key</th>
            <th className="border-b border-slate-800 px-3 py-2 font-medium">Job</th>
            <th className="border-b border-slate-800 px-3 py-2 font-medium">Trigger</th>
            <th className="border-b border-slate-800 px-3 py-2 font-medium">Schedule</th>
            <th className="border-b border-slate-800 px-3 py-2 font-medium">Next run</th>
            <th className="border-b border-slate-800 px-3 py-2 font-medium">Status</th>
            <th className="border-b border-slate-800 px-3 py-2 font-medium">Runs</th>
            <th className="border-b border-slate-800 px-3 py-2 font-medium">Review</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(({ job, runtime, contractKey, nextRunAt }) => {
            const selected = selectedJobId === job.id;
            return (
              <tr key={job.id} className={classNames(selected ? "bg-sky-500/5" : "bg-transparent", "align-top")}>
                <td className="border-b border-slate-900 px-3 py-2 font-mono text-xs text-slate-300">{contractKey}</td>
                <td className="border-b border-slate-900 px-3 py-2">
                  <div className="font-medium text-slate-100">{job.name}</div>
                  <div className="mt-1 text-xs text-slate-500">{job.source.kind} · {job.source.mode}</div>
                </td>
                <td className="border-b border-slate-900 px-3 py-2">
                  <Badge tone={job.trigger.kind === "cron" ? "info" : "default"}>{job.trigger.kind}</Badge>
                </td>
                <td className="border-b border-slate-900 px-3 py-2 text-slate-300">
                  {job.trigger.kind === "cron"
                    ? `Every ${job.trigger.cadenceMinutes} min`
                    : (job.trigger.eventKinds.length > 0 ? job.trigger.eventKinds.join(", ") : "event-driven")}
                </td>
                <td className="border-b border-slate-900 px-3 py-2 text-slate-300">{toLocalDateTime(nextRunAt)}</td>
                <td className="border-b border-slate-900 px-3 py-2">
                  <Badge tone={statusTone(runtime, job.enabled)}>
                    {job.enabled
                      ? (runtime?.running ? "running" : runtime?.lastStatus ?? "ready")
                      : "disabled"}
                  </Badge>
                </td>
                <td className="border-b border-slate-900 px-3 py-2 text-slate-300">{runtime?.runCount ?? 0}</td>
                <td className="border-b border-slate-900 px-3 py-2">
                  <button
                    type="button"
                    onClick={() => onSelectJob?.(job.id)}
                    className="rounded-md border border-slate-700 bg-slate-900 px-2 py-1 text-xs text-slate-100 hover:bg-slate-800"
                  >
                    {selected ? "Selected" : "Inspect"}
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
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

export function EventRuntimeSidebar({ canManage, controller }: { canManage: boolean; controller: EventRuntimeController }) {
  const {
    loading, savingToken, savingControl, togglingRuntime,
    resettingRuntime, status, draft, jobSearch,
    setJobSearch, setSelectedJobId, runtimeJobs, recentEventCount,
    seenDiscordChannels, filteredJobs, selectedJob, load,
    handleSaveControl, handleStopRuntime, handleStartRuntime, handleResetRuntime,
  } = controller;
  if (!draft || !status) return null;
  return (
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
  );
}
