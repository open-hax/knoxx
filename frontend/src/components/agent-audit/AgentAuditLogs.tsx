import { useCallback, useEffect, useMemo, useRef, useState, type UIEvent } from "react";
import { Badge, Button, Card, Markdown } from "@open-hax/uxx";
import {
  getAgentHistorySession,
  listAgentHistorySessions,
  searchMemory,
} from "../../lib/api/common";
import type {
  MemorySearchHit,
  MemorySessionRow,
  MemorySessionSummary,
} from "../../lib/types";
import MultimodalContent from "../chat-page/MultimodalContent";

export type AgentAuditLogsMode = "active" | "history";

export type AgentAuditLogsProps = {
  defaultMode?: AgentAuditLogsMode;

  /** Optional, page-provided filter: only show sessions whose archived rows resolve to this actor id. */
  builtInActorId?: string;

  /** Optional, page-provided filter: only show sessions whose archived rows resolve to this agent contract id. */
  builtInContractId?: string;

  className?: string;
};

const PAGE_SIZE = 40;

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

function normalizeSearch(value: string): string {
  return value.trim().toLowerCase();
}

function formatTimestamp(value?: string | number | null): string {
  if (value == null) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString();
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

  const extra = normalizeRowExtra(row);
  if (row.kind === "graph.node" && typeof extra.label === "string" && extra.label) return extra.label;

  if (row.kind) return row.kind;
  return row.message ?? row.id;
}

function rowBodyMarkdown(row: MemorySessionRow): string {
  if (typeof row.text === "string" && row.text.trim().length > 0) return row.text;

  const extra = normalizeRowExtra(row);
  const keys = Object.keys(extra);
  if (keys.length > 0) {
    return `\`\`\`json\n${JSON.stringify(extra, null, 2)}\n\`\`\``;
  }

  return "";
}

function isContentPart(value: unknown): value is Record<string, unknown> {
  if (typeof value !== "object" || value === null) return false;
  const type = (value as Record<string, unknown>).type;
  return typeof type === "string";
}

function extractContentParts(extra: Record<string, unknown>): Array<Record<string, unknown>> {
  const direct = extra.contentParts ?? extra.content_parts;
  if (Array.isArray(direct)) {
    return direct.filter(isContentPart);
  }

  const receipt = extra.receipt;
  if (typeof receipt === "object" && receipt !== null) {
    const receiptParts = (receipt as Record<string, unknown>).contentParts ?? (receipt as Record<string, unknown>).content_parts;
    if (Array.isArray(receiptParts)) {
      return receiptParts.filter(isContentPart);
    }
  }

  return [];
}

function sessionSearchText(session: MemorySessionSummary): string {
  return [
    session.session,
    session.title ?? "",
    session.project ?? "",
    session.actor_id ?? "",
    session.contract_id ?? "",
    (session.contract_actors ?? []).join(" "),
  ]
    .join(" ")
    .toLowerCase();
}

function activeStatusLabel(session: MemorySessionSummary): string {
  if (session.is_active) return session.active_status ?? "active";
  const status = session.active_status ?? "inactive";
  return status;
}

function activeStatusTone(status: string): "default" | "success" | "warning" | "error" | "info" {
  switch (status) {
    case "running":
      return "warning";
    case "waiting_input":
      return "info";
    case "completed":
      return "success";
    case "failed":
      return "error";
    default:
      return "default";
  }
}

function SessionRow({
  session,
  active,
  onSelect,
}: {
  session: MemorySessionSummary;
  active: boolean;
  onSelect: () => void;
}) {
  const status = activeStatusLabel(session);
  const contractId = session.contract_id;
  const actorId = session.actor_id;

  return (
    <button
      type="button"
      onClick={onSelect}
      aria-pressed={active}
      className={`w-full rounded-xl border px-4 py-3 text-left transition ${
        active
          ? "border-indigo-500 bg-indigo-950/30"
          : "border-slate-800 bg-slate-950/50 hover:border-slate-700 hover:bg-slate-950"
      }`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="text-sm font-semibold text-slate-100">{session.title ?? "session"}</div>
          <div className="mt-1 font-mono text-xs text-slate-400 break-all">{session.session}</div>
        </div>
        <Badge size="sm" variant={activeStatusTone(status)}>{status}</Badge>
      </div>
      <div className="mt-2 flex flex-wrap gap-2 text-xs text-slate-500">
        <span>{session.event_count ?? 0} events</span>
        {contractId ? (
          <>
            <span>•</span>
            <span className="font-mono text-[11px] text-slate-400">contract {contractId}</span>
          </>
        ) : null}
        {actorId ? (
          <>
            <span>•</span>
            <span className="font-mono text-[11px] text-slate-400">actor {actorId}</span>
          </>
        ) : null}
        {session.last_ts ? (
          <>
            <span>•</span>
            <span>last {formatTimestamp(session.last_ts)}</span>
          </>
        ) : null}
      </div>
    </button>
  );
}

function HitList({ hits }: { hits: MemorySearchHit[] }) {
  if (hits.length === 0) {
    return <div className="text-xs text-slate-500">No semantic hits.</div>;
  }

  return (
    <div className="space-y-2">
      {hits.map((hit, idx) => (
        <div key={`${hit.session ?? "hit"}:${idx}`} className="rounded-lg border border-slate-800 bg-slate-950/60 p-3">
          <div className="text-xs text-slate-400">{hit.role ?? ""}</div>
          <div className="mt-1 text-sm text-slate-200">
            <Markdown content={hit.snippet ?? hit.text ?? ""} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
          </div>
          {typeof hit.distance === "number" ? (
            <div className="mt-2 text-[11px] text-slate-500">distance {hit.distance.toFixed(4)}</div>
          ) : null}
        </div>
      ))}
    </div>
  );
}

export default function AgentAuditLogs({
  defaultMode = "active",
  builtInActorId,
  builtInContractId,
  className,
}: AgentAuditLogsProps) {
  const [mode, setMode] = useState<AgentAuditLogsMode>(defaultMode);
  const [autoRefresh, setAutoRefresh] = useState(true);

  const [sessions, setSessions] = useState<MemorySessionSummary[]>([]);
  const sessionsRef = useRef<MemorySessionSummary[]>([]);
  sessionsRef.current = sessions;

  const [sessionsTotal, setSessionsTotal] = useState(0);
  const [sessionsHasMore, setSessionsHasMore] = useState(false);
  const [loadingSessions, setLoadingSessions] = useState(true);
  const [loadingMoreSessions, setLoadingMoreSessions] = useState(false);

  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null);
  const [rows, setRows] = useState<MemorySessionRow[]>([]);
  const [loadingRows, setLoadingRows] = useState(false);

  const [sessionsQuery, setSessionsQuery] = useState("");
  const [rowsQuery, setRowsQuery] = useState("");

  const [semanticQuery, setSemanticQuery] = useState("");
  const [semanticLoading, setSemanticLoading] = useState(false);
  const [semanticHits, setSemanticHits] = useState<MemorySearchHit[]>([]);

  const [error, setError] = useState<string | null>(null);

  const effectiveSessionsQuery = useMemo(() => normalizeSearch(sessionsQuery), [sessionsQuery]);

  const effectiveRowsQuery = useMemo(() => normalizeSearch(rowsQuery), [rowsQuery]);

  const filteredSessions = useMemo(() => {
    const query = effectiveSessionsQuery;
    const inMode = sessions.filter((session) => {
      if (mode === "active") return Boolean(session.is_active);
      return !session.is_active;
    });

    const withBuiltIns = inMode.filter((session) => {
      if (builtInActorId && session.actor_id !== builtInActorId) return false;
      if (builtInContractId && session.contract_id !== builtInContractId) return false;
      return true;
    });

    if (!query) return withBuiltIns;
    return withBuiltIns.filter((session) => sessionSearchText(session).includes(query));
  }, [sessions, effectiveSessionsQuery, mode, builtInActorId, builtInContractId]);

  const selectedSessionSummary = useMemo(
    () => sessions.find((session) => session.session === selectedSessionId) ?? null,
    [sessions, selectedSessionId],
  );

  useEffect(() => {
    const nextIds = filteredSessions.map((session) => session.session);
    setSelectedSessionId((current) => {
      if (current && nextIds.includes(current)) return current;
      return nextIds[0] ?? null;
    });
  }, [filteredSessions]);

  const loadSessions = useCallback(async (offset = 0) => {
    const isMore = offset > 0;
    try {
      if (!isMore) setLoadingSessions(true);
      else setLoadingMoreSessions(true);

      const page = await listAgentHistorySessions({ limit: PAGE_SIZE, offset });
      const nextRows = page.rows ?? [];

      if (!isMore) {
        const preservedTail = sessionsRef.current.filter((item) => !nextRows.some((row) => row.session === item.session));
        const merged = mergeSessionPages(nextRows, preservedTail);
        sessionsRef.current = merged;
        setSessions(merged);
      } else {
        const merged = mergeSessionPages(sessionsRef.current, nextRows);
        sessionsRef.current = merged;
        setSessions(merged);
      }

      setSessionsTotal(page.total ?? (sessionsRef.current.length));
      setSessionsHasMore(page.has_more ?? false);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      if (!isMore) setLoadingSessions(false);
      else setLoadingMoreSessions(false);
    }
  }, []);

  useEffect(() => {
    let cancelled = false;

    const tick = async () => {
      if (cancelled) return;
      await loadSessions(0);
    };

    void tick();
    if (!autoRefresh) return () => {
      cancelled = true;
    };

    const timer = window.setInterval(() => {
      void tick();
    }, 10000);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [autoRefresh, loadSessions]);

  const handleSessionsScroll = async (event: UIEvent<HTMLDivElement>) => {
    if (loadingSessions || loadingMoreSessions || !sessionsHasMore) return;
    const target = event.currentTarget;
    const remaining = target.scrollHeight - target.scrollTop - target.clientHeight;
    if (remaining > 120) return;
    await loadSessions(sessionsRef.current.length);
  };

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      if (!selectedSessionId) {
        setRows([]);
        setSemanticHits([]);
        return;
      }
      try {
        setLoadingRows(true);
        const result = await getAgentHistorySession(selectedSessionId);
        if (cancelled) return;
        setRows(result.rows ?? []);
        setSemanticHits([]);
        setError(null);
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : String(err));
      } finally {
        if (!cancelled) setLoadingRows(false);
      }
    };

    void load();
    if (!autoRefresh) return () => {
      cancelled = true;
    };

    const timer = window.setInterval(() => {
      void load();
    }, 10000);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [selectedSessionId, autoRefresh]);

  const filteredRows = useMemo(() => {
    if (!effectiveRowsQuery) return rows;

    const q = effectiveRowsQuery;
    return rows.filter((row) => {
      const extra = normalizeRowExtra(row);
      const content = [
        row.id,
        row.kind,
        row.role,
        row.message,
        row.text,
        JSON.stringify(extra),
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();
      return content.includes(q);
    });
  }, [rows, effectiveRowsQuery]);

  const runSemanticSearch = useCallback(async () => {
    const query = semanticQuery.trim();
    if (!selectedSessionId || !query) {
      setSemanticHits([]);
      return;
    }

    setSemanticLoading(true);
    try {
      const result = await searchMemory({ query, k: 12, sessionId: selectedSessionId });
      setSemanticHits(result.hits ?? []);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSemanticLoading(false);
    }
  }, [semanticQuery, selectedSessionId]);

  return (
    <div className={className ?? ""}>
      {error ? (
        <div className="mb-4 rounded-lg border border-red-800 bg-red-950/40 p-3 text-sm text-red-200">{error}</div>
      ) : null}

      <div className="grid min-h-0 grid-cols-[360px_minmax(0,1fr)] gap-4">
        <div className="min-h-0 overflow-hidden rounded-xl border border-slate-800 bg-slate-900">
          <div className="border-b border-slate-800 px-4 py-3">
            <div className="flex items-center justify-between gap-2">
              <div className="text-sm font-semibold text-slate-200">Sessions</div>
              <div className="text-xs text-slate-500">
                {sessionsTotal > 0 ? `${filteredSessions.length}/${sessionsTotal}` : filteredSessions.length}
              </div>
            </div>

            <div className="mt-3 grid grid-cols-2 gap-2">
              <Button variant={mode === "active" ? "primary" : "ghost"} size="sm" onClick={() => setMode("active")}>Active</Button>
              <Button variant={mode === "history" ? "primary" : "ghost"} size="sm" onClick={() => setMode("history")}>History</Button>
            </div>

            <div className="mt-2">
              <div className="w-full">
                <Button variant="ghost" size="sm" onClick={() => setAutoRefresh((value) => !value)}>
                  {autoRefresh ? "Pause refresh" : "Resume refresh"}
                </Button>
              </div>
            </div>

            <div className="mt-3">
              <input
                aria-label="Search sessions"
                value={sessionsQuery}
                onChange={(event) => setSessionsQuery(event.target.value)}
                placeholder={builtInActorId || builtInContractId ? "Search… (filters active)" : "Search…"}
                className="w-full rounded-md border border-slate-800 bg-slate-950/80 px-2.5 py-2 text-sm text-slate-100 outline-none focus:border-sky-500"
              />
            </div>
          </div>

          <div className="max-h-full overflow-y-auto p-3" onScroll={handleSessionsScroll}>
            {loadingSessions && sessions.length === 0 ? <div className="text-sm text-slate-500">Loading sessions…</div> : null}
            {!loadingSessions && filteredSessions.length === 0 ? <div className="text-sm text-slate-500">No sessions match.</div> : null}

            <div className="space-y-3">
              {filteredSessions.map((session) => (
                <SessionRow
                  key={session.session}
                  session={session}
                  active={session.session === selectedSessionId}
                  onSelect={() => setSelectedSessionId(session.session)}
                />
              ))}
              {loadingMoreSessions ? <div className="px-2 py-1 text-xs text-slate-500">Loading more…</div> : null}
            </div>
          </div>
        </div>

        <div className="min-h-0 overflow-y-auto rounded-xl border border-slate-800 bg-slate-900 p-4">
          {!selectedSessionId ? <div className="text-sm text-slate-500">Select a session to inspect.</div> : null}
          {selectedSessionId && loadingRows && rows.length === 0 ? <div className="text-sm text-slate-500">Loading session…</div> : null}

          {selectedSessionId ? (
            <div className="space-y-4">
              <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                <div className="min-w-0">
                  <div className="text-lg font-semibold text-slate-100">Agent audit logs</div>
                  <div className="mt-1 font-mono text-xs text-slate-400 break-all">{selectedSessionId}</div>
                  <div className="mt-2 text-xs text-slate-500">OpenPlanner session trace (knoxx-session).</div>
                  {selectedSessionSummary?.contract_id || selectedSessionSummary?.actor_id ? (
                    <div className="mt-2 flex flex-wrap gap-2 text-[11px] text-slate-500">
                      {selectedSessionSummary?.contract_id ? (
                        <span className="rounded bg-slate-950 px-2 py-1 font-mono text-slate-300">contract {selectedSessionSummary.contract_id}</span>
                      ) : null}
                      {selectedSessionSummary?.actor_id ? (
                        <span className="rounded bg-slate-950 px-2 py-1 font-mono text-slate-300">actor {selectedSessionSummary.actor_id}</span>
                      ) : null}
                    </div>
                  ) : null}
                </div>
                <div className="flex flex-wrap gap-2">
                  <Badge size="sm" variant="info">{filteredRows.length}/{rows.length} events</Badge>
                </div>
              </div>

              <div className="grid gap-3 md:grid-cols-2">
                <Card variant="outlined" padding="sm">
                  <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Keyword search</div>
                  <input
                    aria-label="Search events"
                    value={rowsQuery}
                    onChange={(event) => setRowsQuery(event.target.value)}
                    placeholder="Search…"
                    className="mt-2 w-full rounded-md border border-slate-800 bg-slate-950/80 px-2.5 py-2 text-sm text-slate-100 outline-none focus:border-sky-500"
                  />
                </Card>

                <Card variant="outlined" padding="sm">
                  <div className="flex items-center justify-between gap-2">
                    <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Semantic search</div>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => void runSemanticSearch()}
                      disabled={semanticLoading || !semanticQuery.trim()}
                    >
                      {semanticLoading ? "Searching…" : "Search"}
                    </Button>
                  </div>
                  <input
                    aria-label="Semantic search"
                    value={semanticQuery}
                    onChange={(event) => setSemanticQuery(event.target.value)}
                    placeholder="Ask the trace…"
                    className="mt-2 w-full rounded-md border border-slate-800 bg-slate-950/80 px-2.5 py-2 text-sm text-slate-100 outline-none focus:border-sky-500"
                    onKeyDown={(event) => {
                      if (event.key === "Enter") {
                        event.preventDefault();
                        void runSemanticSearch();
                      }
                    }}
                  />
                </Card>
              </div>

              {semanticHits.length > 0 ? (
                <Card variant="outlined" padding="sm">
                  <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Semantic hits</div>
                  <div className="mt-3">
                    <HitList hits={semanticHits} />
                  </div>
                </Card>
              ) : null}

              <Card variant="outlined" padding="sm">
                <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Session events</div>
                <div className="mt-3 space-y-3">
                  {filteredRows.length === 0 ? (
                    <div className="text-sm text-slate-500">No events match your filters.</div>
                  ) : (
                    filteredRows.map((row) => {
                      const extra = normalizeRowExtra(row);
                      const parts = extractContentParts(extra);
                      const body = rowBodyMarkdown(row);

                      return (
                        <div key={row.id} className="rounded-lg border border-slate-800 bg-slate-950/70 p-3">
                          <div className="flex items-start justify-between gap-3">
                            <div className="min-w-0">
                              <div className="text-sm font-semibold text-slate-100">{rowTitle(row)}</div>
                              <div className="mt-1 text-xs text-slate-400">{formatTimestamp(row.ts ?? null)}</div>
                            </div>
                            <div className="flex gap-2">
                              {row.role ? <Badge size="sm" variant="info">{row.role}</Badge> : null}
                              {row.kind ? <Badge size="sm" variant="default">{row.kind}</Badge> : null}
                            </div>
                          </div>

                          {body ? (
                            <div className="mt-3 text-sm text-slate-200">
                              <Markdown content={body} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                            </div>
                          ) : null}

                          {parts.length > 0 ? (
                            <div className="mt-3">
                              <MultimodalContent parts={parts as any} />
                            </div>
                          ) : null}
                        </div>
                      );
                    })
                  )}
                </div>
              </Card>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}
