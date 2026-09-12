import React from "react";
import type { EventAgentControlResponse } from "../../lib/api/admin";
import { useEventRuntimeController, type EventRuntimeOptions } from "../../pages/EventsPage";
import { EventAgentScheduleReview, EventRuntimeSidebar } from "./EventAgentScheduleReview";
import { SelectedEventJob } from "./EventAgentsPanel";
import { Badge, CollapsiblePanel, SectionCard } from "./common";
import { joinCsv, splitCsv, type DraftControl } from "./helpers";

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

export function DiscordSection({ className, ...options }: EventRuntimeOptions & { className?: string }) {
  const { canManage } = options;
  const controller = useEventRuntimeController(options);
  const {
    loading, savingToken, savingControl, notice,
    error, status, draft, setDraft,
    draftToken, setDraftToken, eventSourceKind, setEventSourceKind,
    eventKind, setEventKind, eventPayloadDraft, setEventPayloadDraft,
    dispatchingEvent, runtimeJobs, selectedJob, setSelectedJobId,
    availableSourceKinds, handleSaveToken, handleDispatchEvent, load,
  } = controller;
  return (
    <SectionCard className={className}>
      {loading ? (
        <div className="text-sm text-slate-300">Loading event-agent control plane…</div>
      ) : !draft || !status ? (
        <div role="alert" className="space-y-3 text-sm text-rose-200">
          <p>{error || "Event-agent control plane is unavailable."}</p>
          <button type="button" onClick={() => void load()} className="rounded border border-slate-700 px-3 py-2 text-slate-100">
            Retry runtime read
          </button>
        </div>
      ) : (
        <div className="flex flex-col h-full min-h-0">
          <div className="min-h-0 flex-1 overflow-hidden">
            <div className="grid h-full min-h-0 min-w-[44rem] gap-3 grid-cols-[12rem_minmax(0,1fr)] xl:grid-cols-[13rem_minmax(0,1fr)]">
            <EventRuntimeSidebar canManage={canManage} controller={controller} />

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
                <SelectedEventJob canManage={canManage} controller={controller} />

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
