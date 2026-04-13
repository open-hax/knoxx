import { useEffect, useMemo, useRef, useState, type UIEvent } from "react";
import { Badge, Button, Card, Markdown } from "@open-hax/uxx";
import {
  getAgentHistorySession,
  getRun,
  listActiveAgents,
  listAgentHistorySessions,
} from "../lib/api/common";
import type {
  ActiveAgentSummary,
  MemorySessionRow,
  MemorySessionSummary,
  RunDetail,
  RunEvent,
} from "../lib/types";
import { ToolReceiptGroup } from "../components/ToolReceiptBlock";

type InspectorMode = "active" | "history";
const HISTORY_PAGE_SIZE = 40;

function mergeSessionPages(primary: MemorySessionSummary[], secondary: MemorySessionSummary[]): MemorySessionSummary[] {
  const seen = new Set<string>();
  const merged: MemorySessionSummary[] = [];
  for (const row of [...primary, ...secondary]) {
    if (!row?.session || seen.has(row.session)) continue;
    seen.add(row.session);
    merged.push(row);
  }
  return merged;
}

function formatTimestamp(value?: string | number | null): string {
  if (value == null) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString();
}

function elapsedLabel(run: ActiveAgentSummary | RunDetail | null): string {
  if (!run?.created_at) return "";
  const started = new Date(run.created_at).getTime();
  if (Number.isNaN(started)) return "";
  const end = run.total_time_ms != null ? started + run.total_time_ms : Date.now();
  const seconds = Math.max(0, Math.round((end - started) / 1000));
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return `${minutes}m ${remainder}s`;
}

function statusVariant(status?: string): "default" | "success" | "warning" | "error" | "info" {
  switch (status) {
    case "completed":
      return "success";
    case "failed":
      return "error";
    case "running":
    case "queued":
    case "waiting_input":
      return "warning";
    default:
      return "default";
  }
}

function prettyJson(value: unknown): string {
  return JSON.stringify(value ?? {}, null, 2);
}

function normalizeRowExtra(row: MemorySessionRow): Record<string, unknown> {
  const extra = row.extra;
  if (!extra) return {};
  if (typeof extra === "string") {
    try {
      return JSON.parse(extra) as Record<string, unknown>;
    } catch {
      return { raw: extra };
    }
  }
  return extra;
}

function rowTitle(row: MemorySessionRow): string {
  if (row.kind === "knoxx.tool_receipt") return `Tool receipt · ${row.message ?? row.id}`;
  if (row.kind === "graph.node") {
    const extra = normalizeRowExtra(row);
    if (typeof extra.label === "string" && extra.label) return extra.label;
  }
  if (row.kind) return row.kind;
  return row.message ?? row.id;
}

function rowBody(row: MemorySessionRow): string {
  if (typeof row.text === "string" && row.text.trim().length > 0) return row.text;
  const extra = normalizeRowExtra(row);
  if (Object.keys(extra).length > 0) return `\`\`\`json\n${JSON.stringify(extra, null, 2)}\n\`\`\``;
  return "";
}

function EventFeed({ events }: { events: RunEvent[] }) {
  if (events.length === 0) {
    return <div className="text-sm text-slate-500">No runtime events captured yet.</div>;
  }

  return (
    <div className="space-y-2">
      {events.slice().reverse().map((event, index) => (
        <div key={`${event.type ?? "event"}:${event.at ?? index}`} className="rounded-lg border border-slate-700 bg-slate-950/70 p-3">
          <div className="flex items-center justify-between gap-3">
            <div className="text-sm font-semibold text-slate-100">
              {event.type ?? "event"}
              {typeof event.tool_name === "string" && event.tool_name ? ` • ${event.tool_name}` : ""}
            </div>
            <Badge size="sm" variant={statusVariant(typeof event.status === "string" ? event.status : undefined)}>
              {typeof event.status === "string" ? event.status : "live"}
            </Badge>
          </div>
          <div className="mt-1 text-xs text-slate-400">{formatTimestamp(typeof event.at === "string" ? event.at : null)}</div>
          {typeof event.preview === "string" && event.preview.trim().length > 0 ? (
            <div className="mt-2 text-sm text-slate-200">
              <Markdown content={event.preview} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
            </div>
          ) : null}
          {typeof event.error === "string" && event.error.trim().length > 0 ? (
            <div className="mt-2 rounded border border-red-800 bg-red-950/40 p-2 text-xs text-red-200">{event.error}</div>
          ) : null}
        </div>
      ))}
    </div>
  );
}

function HistoryFeed({ rows }: { rows: MemorySessionRow[] }) {
  if (rows.length === 0) {
    return <div className="text-sm text-slate-500">No archived history rows for this session yet.</div>;
  }

  return (
    <div className="space-y-3">
      {rows.map((row) => {
        const extra = normalizeRowExtra(row);
        const receipt = typeof extra.receipt === "object" && extra.receipt !== null ? extra.receipt : null;
        return (
          <div key={row.id} className="rounded-lg border border-slate-800 bg-slate-950/70 p-3">
            <div className="flex items-start justify-between gap-3">
              <div>
                <div className="text-sm font-semibold text-slate-100">{rowTitle(row)}</div>
                <div className="mt-1 text-xs text-slate-400">{formatTimestamp(row.ts ?? null)}</div>
              </div>
              <div className="flex gap-2">
                {row.role ? <Badge size="sm" variant="info">{row.role}</Badge> : null}
                {row.kind ? <Badge size="sm" variant="default">{row.kind}</Badge> : null}
              </div>
            </div>
            {rowBody(row) ? (
              <div className="mt-3 text-sm text-slate-200">
                <Markdown content={rowBody(row)} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
              </div>
            ) : null}
            {receipt ? (
              <details className="mt-3">
                <summary className="cursor-pointer text-xs text-slate-400">receipt payload</summary>
                <pre className="mt-2 overflow-x-auto rounded bg-slate-950 p-3 text-xs text-slate-300">{prettyJson(receipt)}</pre>
              </details>
            ) : null}
          </div>
        );
      })}
    </div>
  );
}

export default function AgentsPage() {
  const [mode, setMode] = useState<InspectorMode>("active");
  const [runs, setRuns] = useState<ActiveAgentSummary[]>([]);
  const [selectedRunId, setSelectedRunId] = useState<string | null>(null);
  const [detail, setDetail] = useState<RunDetail | null>(null);
  const [historySessions, setHistorySessions] = useState<MemorySessionSummary[]>([]);
  const historySessionsRef = useRef<MemorySessionSummary[]>([]);
  historySessionsRef.current = historySessions;
  const [selectedHistorySession, setSelectedHistorySession] = useState<string | null>(null);
  const [historyRows, setHistoryRows] = useState<MemorySessionRow[]>([]);
  const [loadingList, setLoadingList] = useState(true);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [loadingMoreHistory, setLoadingMoreHistory] = useState(false);
  const [historySessionsHasMore, setHistorySessionsHasMore] = useState(false);
  const [historySessionsTotal, setHistorySessionsTotal] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [autoRefresh, setAutoRefresh] = useState(true);

  useEffect(() => {
    let cancelled = false;

    const loadRuns = async () => {
      try {
        if (!cancelled) setLoadingList(true);
        const nextRuns = await listActiveAgents(30);
        if (cancelled) return;
        setRuns(nextRuns);
        setSelectedRunId((current) => {
          if (current && nextRuns.some((run) => run.run_id === current)) return current;
          return nextRuns[0]?.run_id ?? null;
        });
        setError(null);
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : String(err));
      } finally {
        if (!cancelled) setLoadingList(false);
      }
    };

    void loadRuns();
    if (!autoRefresh) return () => {
      cancelled = true;
    };

    const timer = window.setInterval(() => {
      void loadRuns();
    }, 3000);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [autoRefresh]);

  useEffect(() => {
    let cancelled = false;

    const loadHistory = async () => {
      try {
        if (!cancelled) setLoadingHistory(true);
        const page = await listAgentHistorySessions({ limit: HISTORY_PAGE_SIZE, offset: 0 });
        if (cancelled) return;
        const nextRows = page.rows ?? [];
        const preservedTail = historySessionsRef.current.filter((item) => !nextRows.some((row) => row.session === item.session));
        const merged = mergeSessionPages(nextRows, preservedTail);
        historySessionsRef.current = merged;
        setHistorySessions(merged);
        setHistorySessionsTotal(page.total ?? merged.length);
        setHistorySessionsHasMore(page.has_more ?? false);
        setSelectedHistorySession((current) => {
          if (current && merged.some((session) => session.session === current)) return current;
          return merged[0]?.session ?? null;
        });
        setError(null);
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : String(err));
      } finally {
        if (!cancelled) setLoadingHistory(false);
      }
    };

    void loadHistory();
    if (!autoRefresh) return () => {
      cancelled = true;
    };

    const timer = window.setInterval(() => {
      void loadHistory();
    }, 10000);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [autoRefresh]);

  const handleHistoryScroll = async (event: UIEvent<HTMLDivElement>) => {
    if (mode !== "history" || loadingHistory || loadingMoreHistory || !historySessionsHasMore) return;
    const target = event.currentTarget;
    const remaining = target.scrollHeight - target.scrollTop - target.clientHeight;
    if (remaining > 120) return;
    setLoadingMoreHistory(true);
    try {
      const page = await listAgentHistorySessions({
        limit: HISTORY_PAGE_SIZE,
        offset: historySessionsRef.current.length,
      });
      const merged = mergeSessionPages(historySessionsRef.current, page.rows ?? []);
      historySessionsRef.current = merged;
      setHistorySessions(merged);
      setHistorySessionsTotal(page.total ?? merged.length);
      setHistorySessionsHasMore(page.has_more ?? false);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoadingMoreHistory(false);
    }
  };

  useEffect(() => {
    let cancelled = false;
    if (!selectedRunId) {
      setDetail(null);
      return () => {
        cancelled = true;
      };
    }

    const loadDetail = async () => {
      try {
        setLoadingDetail(true);
        const nextDetail = await getRun(selectedRunId);
        if (!cancelled) {
          setDetail(nextDetail);
          setError(null);
        }
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : String(err));
      } finally {
        if (!cancelled) setLoadingDetail(false);
      }
    };

    void loadDetail();
    if (!autoRefresh) return () => {
      cancelled = true;
    };

    const timer = window.setInterval(() => {
      void loadDetail();
    }, 3000);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [selectedRunId, autoRefresh]);

  useEffect(() => {
    let cancelled = false;
    if (!selectedHistorySession) {
      setHistoryRows([]);
      return () => {
        cancelled = true;
      };
    }

    const loadSession = async () => {
      try {
        setLoadingDetail(true);
        const result = await getAgentHistorySession(selectedHistorySession);
        if (!cancelled) {
          setHistoryRows(result.rows ?? []);
          setError(null);
        }
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : String(err));
      } finally {
        if (!cancelled) setLoadingDetail(false);
      }
    };

    void loadSession();
    if (!autoRefresh) return () => {
      cancelled = true;
    };

    const timer = window.setInterval(() => {
      void loadSession();
    }, 10000);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [selectedHistorySession, autoRefresh]);

  const selectedSummary = useMemo(
    () => runs.find((run) => run.run_id === selectedRunId) ?? null,
    [runs, selectedRunId],
  );

  const selectedHistorySummary = useMemo(
    () => historySessions.find((session) => session.session === selectedHistorySession) ?? null,
    [historySessions, selectedHistorySession],
  );

  return (
    <div className="flex h-full min-h-0 flex-col bg-slate-950 text-slate-100">
      <div className="flex items-center justify-between border-b border-slate-800 px-6 py-4">
        <div>
          <h1 className="text-2xl font-semibold">Agents</h1>
          <p className="mt-1 text-sm text-slate-400">Inspect live Knoxx runs and archived agent history with prompts, tool receipts, and event traces.</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant={mode === "active" ? "primary" : "ghost"} size="sm" onClick={() => setMode("active")}>Active</Button>
          <Button variant={mode === "history" ? "primary" : "ghost"} size="sm" onClick={() => setMode("history")}>History</Button>
          <Button variant="ghost" size="sm" onClick={() => setAutoRefresh((value) => !value)}>
            {autoRefresh ? "Pause refresh" : "Resume refresh"}
          </Button>
        </div>
      </div>

      {error ? <div className="mx-6 mt-4 rounded-lg border border-red-800 bg-red-950/40 p-3 text-sm text-red-200">{error}</div> : null}

      <div className="grid min-h-0 flex-1 grid-cols-[360px_minmax(0,1fr)] gap-4 p-6">
        <div className="min-h-0 overflow-hidden rounded-xl border border-slate-800 bg-slate-900">
          <div className="border-b border-slate-800 px-4 py-3 text-sm font-semibold text-slate-200">
            {mode === "active"
              ? `Live queue (${runs.length})`
              : `Archived sessions (${historySessionsTotal > 0 ? `${historySessions.length}/${historySessionsTotal}` : historySessions.length})`}
          </div>
          <div className="max-h-full overflow-y-auto p-3" onScroll={handleHistoryScroll}>
            {mode === "active" ? (
              <>
                {loadingList && runs.length === 0 ? <div className="text-sm text-slate-500">Loading active agents…</div> : null}
                {!loadingList && runs.length === 0 ? <div className="text-sm text-slate-500">No active runs right now.</div> : null}
                <div className="space-y-3">
                  {runs.map((run) => {
                    const isSelected = run.run_id === selectedRunId;
                    const role = typeof run.agent_spec?.role === "string" ? run.agent_spec.role : "default";
                    return (
                      <button
                        key={run.run_id}
                        type="button"
                        onClick={() => setSelectedRunId(run.run_id)}
                        className={`w-full rounded-xl border px-4 py-3 text-left transition ${
                          isSelected
                            ? "border-cyan-500 bg-cyan-950/30"
                            : "border-slate-800 bg-slate-950/50 hover:border-slate-700 hover:bg-slate-950"
                        }`}
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div>
                            <div className="text-sm font-semibold text-slate-100">{role}</div>
                            <div className="mt-1 font-mono text-xs text-slate-400">{run.run_id}</div>
                          </div>
                          <Badge size="sm" variant={statusVariant(run.status)}>{run.status}</Badge>
                        </div>
                        <div className="mt-2 flex flex-wrap gap-2 text-xs text-slate-400">
                          <span>{run.model ?? "no-model"}</span>
                          <span>•</span>
                          <span>{elapsedLabel(run)}</span>
                          <span>•</span>
                          <span>{run.has_active_stream ? "streaming" : "idle stream"}</span>
                        </div>
                        {run.latest_user_message ? (
                          <div className="mt-3 line-clamp-3 text-sm text-slate-300">{run.latest_user_message}</div>
                        ) : null}
                        <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-500">
                          <span>{run.event_count ?? 0} events</span>
                          <span>{run.tool_receipt_count ?? 0} tools</span>
                        </div>
                      </button>
                    );
                  })}
                </div>
              </>
            ) : (
              <>
                {loadingHistory && historySessions.length === 0 ? <div className="text-sm text-slate-500">Loading agent history…</div> : null}
                {!loadingHistory && historySessions.length === 0 ? <div className="text-sm text-slate-500">No archived agent history yet.</div> : null}
                <div className="space-y-3">
                  {historySessions.map((session) => {
                    const isSelected = session.session === selectedHistorySession;
                    const label = session.session.startsWith("translation-") ? "translator" : "agent";
                    return (
                      <button
                        key={session.session}
                        type="button"
                        onClick={() => setSelectedHistorySession(session.session)}
                        className={`w-full rounded-xl border px-4 py-3 text-left transition ${
                          isSelected
                            ? "border-indigo-500 bg-indigo-950/30"
                            : "border-slate-800 bg-slate-950/50 hover:border-slate-700 hover:bg-slate-950"
                        }`}
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div>
                            <div className="text-sm font-semibold text-slate-100">{label}</div>
                            <div className="mt-1 font-mono text-xs text-slate-400 break-all">{session.session}</div>
                          </div>
                          <Badge size="sm" variant="info">{session.event_count ?? 0} events</Badge>
                        </div>
                        <div className="mt-2 text-xs text-slate-400">last seen {formatTimestamp(session.last_ts ?? null)}</div>
                      </button>
                    );
                  })}
                  {loadingMoreHistory ? <div className="px-2 py-1 text-xs text-slate-500">Loading more history…</div> : null}
                </div>
              </>
            )}
          </div>
        </div>

        <div className="min-h-0 overflow-y-auto rounded-xl border border-slate-800 bg-slate-900 p-4">
          {mode === "active" ? (
            <>
              {!selectedRunId ? <div className="text-sm text-slate-500">Select an active run to inspect.</div> : null}
              {selectedRunId && loadingDetail && !detail ? <div className="text-sm text-slate-500">Loading run detail…</div> : null}
              {selectedRunId && detail ? (
                <div className="space-y-4">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <div className="text-lg font-semibold text-slate-100">{typeof selectedSummary?.agent_spec?.role === "string" ? selectedSummary.agent_spec.role : "Agent run"}</div>
                      <div className="mt-1 font-mono text-xs text-slate-400">run {detail.run_id}</div>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <Badge size="sm" variant={statusVariant(detail.status)}>{detail.status}</Badge>
                      <Badge size="sm" variant="info">{detail.model ?? "no-model"}</Badge>
                    </div>
                  </div>

                  <div className="grid gap-4 md:grid-cols-3">
                    <Card variant="outlined" padding="sm">
                      <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Presence</div>
                      <div className="mt-2 space-y-2 text-sm text-slate-200">
                        <div><span className="text-slate-500">session</span><div className="font-mono text-xs break-all">{detail.session_id ?? "—"}</div></div>
                        <div><span className="text-slate-500">conversation</span><div className="font-mono text-xs break-all">{detail.conversation_id ?? "—"}</div></div>
                        <div><span className="text-slate-500">created</span><div>{formatTimestamp(detail.created_at)}</div></div>
                        <div><span className="text-slate-500">updated</span><div>{formatTimestamp(detail.updated_at)}</div></div>
                      </div>
                    </Card>

                    <Card variant="outlined" padding="sm">
                      <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Agent spec</div>
                      <pre className="mt-2 overflow-x-auto rounded-lg bg-slate-950 p-3 text-xs text-slate-200">{prettyJson(detail.settings?.agentSpec ?? selectedSummary?.agent_spec ?? {})}</pre>
                    </Card>

                    <Card variant="outlined" padding="sm">
                      <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Resource scope</div>
                      <pre className="mt-2 overflow-x-auto rounded-lg bg-slate-950 p-3 text-xs text-slate-200">{prettyJson(detail.resources?.agentResourcePolicies ?? selectedSummary?.resource_policies ?? {})}</pre>
                    </Card>
                  </div>

                  <Card variant="outlined" padding="sm">
                    <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Prompt transcript</div>
                    <div className="mt-3 space-y-3">
                      {detail.request_messages.map((message, index) => (
                        <div key={`${message.role}:${index}`} className="rounded-lg border border-slate-800 bg-slate-950/60 p-3">
                          <div className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">{message.role}</div>
                          <Markdown content={message.content} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                        </div>
                      ))}
                    </div>
                  </Card>

                  <Card variant="outlined" padding="sm">
                    <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Tool receipts</div>
                    <div className="mt-3">
                      <ToolReceiptGroup receipts={(detail.tool_receipts ?? [])} liveEvents={detail.events ?? []} defaultExpanded />
                    </div>
                  </Card>

                  <Card variant="outlined" padding="sm">
                    <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Event feed</div>
                    <div className="mt-3">
                      <EventFeed events={detail.events ?? []} />
                    </div>
                  </Card>
                </div>
              ) : null}
            </>
          ) : (
            <>
              {!selectedHistorySession ? <div className="text-sm text-slate-500">Select a historical session to inspect.</div> : null}
              {selectedHistorySession && loadingDetail && historyRows.length === 0 ? <div className="text-sm text-slate-500">Loading historical session…</div> : null}
              {selectedHistorySession ? (
                <div className="space-y-4">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <div className="text-lg font-semibold text-slate-100">Archived agent session</div>
                      <div className="mt-1 font-mono text-xs text-slate-400 break-all">{selectedHistorySession}</div>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <Badge size="sm" variant="info">{selectedHistorySummary?.event_count ?? historyRows.length} events</Badge>
                      <Badge size="sm" variant="default">last {formatTimestamp(selectedHistorySummary?.last_ts ?? null)}</Badge>
                    </div>
                  </div>

                  <Card variant="outlined" padding="sm">
                    <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Historical trace</div>
                    <div className="mt-3">
                      <HistoryFeed rows={historyRows} />
                    </div>
                  </Card>
                </div>
              ) : null}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
