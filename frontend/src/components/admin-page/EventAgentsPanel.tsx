import type { EventRuntimeController } from "../../pages/EventsPage";
import { Badge, CollapsiblePanel } from "./common";
import { joinCsv, runtimeStatusTone, splitCsv, toLocalDateTime } from "./helpers";
import React, { useEffect, useState } from "react";

// CLJS event agents panel wrapper.
//
// IMPORTANT: This wrapper intentionally does NOT auto-fallback to the legacy
// TypeScript panel. If shadow-cljs isn’t correctly exporting the panel, we want
// a loud, obvious failure so the migration stays debuggable.

type CljsComponentType = React.ComponentType<{
  canManage: boolean;
  tools: import("../../lib/types").AdminToolDefinition[];
  onSelectedJobChange?: (job: unknown) => void;
}>;

function getCljsComponent(): CljsComponentType | null {
  // CLJS namespaces use underscores instead of hyphens in JS
  const ns = (window as unknown as Record<string, unknown>).knoxx;
  if (!ns) return null;
  const frontend = (ns as Record<string, unknown>).frontend;
  if (!frontend) return null;
  const admin = (frontend as Record<string, unknown>).admin;
  if (!admin) return null;
  const panel = (admin as Record<string, unknown>).event_agents_panel;
  if (!panel) return null;
  const component = (panel as Record<string, unknown>).event_agents_panel;
  return (component as CljsComponentType) ?? null;
}

class CljsErrorBoundary extends React.Component<
  React.PropsWithChildren<{ onError: (error: Error) => void }>,
  { error: Error | null }
> {
  state: { error: Error | null } = { error: null };

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  componentDidCatch(error: Error) {
    this.props.onError(error);
  }

  render() {
    if (this.state.error) return null;
    return this.props.children;
  }
}

export function EventAgentsPanel({
  canManage,
  tools,
  onSelectedJobChange,
}: {
  canManage: boolean;
  tools: import("../../lib/types").AdminToolDefinition[];
  onSelectedJobChange?: (job: unknown) => void;
}) {
  const [Component, setComponent] = useState<CljsComponentType | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    const comp = getCljsComponent();
    if (comp) {
      setComponent(() => comp);
      return;
    }

    // Give the /cljs/app.js injector a moment to run.
    const timer = setTimeout(() => {
      const loaded = getCljsComponent();
      if (loaded) {
        setComponent(() => loaded);
        return;
      }

      setLoadError(
        "shadow-cljs EventAgentsPanel export not found on window.knoxx.frontend.admin.event_agents_panel.event_agents_panel. " +
          "This is an integration/compile problem (not a reason to silently render legacy TS).",
      );
    }, 1500);

    return () => clearTimeout(timer);
  }, []);

  if (loadError) {
    return (
      <div className="h-full rounded-lg border border-rose-500/30 bg-rose-500/10 p-4 text-sm text-rose-100">
        <div className="font-semibold">Runtime jobs (shadow-cljs) failed to load</div>
        <div className="mt-2 font-mono text-xs whitespace-pre-wrap break-words">{loadError}</div>
      </div>
    );
  }

  if (!Component) {
    return (
      <div className="flex h-full items-center justify-center text-sm text-slate-400">
        Loading runtime jobs…
      </div>
    );
  }

  return (
    <div className="h-full">
      <CljsErrorBoundary
        onError={(error) => {
          setLoadError(String(error?.message ?? error));
        }}
      >
        <Component canManage={canManage} tools={tools} onSelectedJobChange={onSelectedJobChange} />
      </CljsErrorBoundary>
    </div>
  );
}

export function SelectedEventJob({ canManage, controller }: { canManage: boolean; controller: EventRuntimeController }) {
  const {
    selectedJob, selectedRuntime, runningJobId, handleRunJob,
    savingControl, updateJob, availableTriggerKinds, availableSourceKinds,
    availableRoles, selectedJobJsonDraft, updateJsonDraft, availableToolIds,
  } = controller;
  return <>
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
  </>;
}
