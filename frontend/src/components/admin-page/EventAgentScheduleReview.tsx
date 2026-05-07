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
