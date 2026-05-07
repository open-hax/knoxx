import { useCallback, useEffect, useMemo, useRef, useState, type UIEvent } from "react";
import { useLocation } from "react-router-dom";
import { Badge, Button, Card, Markdown } from "@open-hax/uxx";
import { abortAdminActiveAgent, getAgentHistorySession, getRun, listActiveAgents, listAdminActiveAgents, listMemorySessions, searchMemory } from "../../lib/api/common";
import { getAgentContractsCatalog, getRunEvents, getSessionStatus } from "../../lib/api/runtime";
import type {
  ActiveAgentSummary,
  ActorCatalogItem,
  ContentPart,
  MemorySearchHit,
  MemorySessionRow,
  MemorySessionSummary,
  RunDetail,
  RunEvent,
  ToolReceipt,
} from "../../lib/types";
import MultimodalContent from "../chat-page/MultimodalContent";
import { parseMemoryRowExtra } from "../chat-page/utils";
import { ToolReceiptBlock } from "../ToolReceiptBlock";

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
    case "aborted":
      return "error";
    default:
      return "default";
  }
}

function specString(spec: Record<string, unknown>, keys: string[]): string | null {
  for (const key of keys) {
    const value = spec[key];
    if (typeof value === "string" && value.trim()) return value.trim();
  }
  return null;
}

function activeRunSubAgentId(run: ActiveAgentSummary): string | null {
  return specString(run.agent_spec ?? {}, ["subAgentId", "sub_agent_id", "sub-agent-id"]);
}

function activeRunParentAgentId(run: ActiveAgentSummary): string | null {
  return specString(run.agent_spec ?? {}, ["parentAgentId", "parent_agent_id", "parent-agent-id"]);
}

function activeRunRole(run: ActiveAgentSummary): string | null {
  return specString(run.agent_spec ?? {}, ["role"]);
}

function activeRunContractId(run: ActiveAgentSummary): string | null {
  return specString(run.agent_spec ?? {}, ["contractId", "contract_id", "contract-id"]);
}

function activeRunToolCount(run: ActiveAgentSummary): number {
  const policies = run.agent_spec?.toolPolicies ?? run.agent_spec?.tool_policies ?? run.agent_spec?.["tool-policies"];
  return Array.isArray(policies) ? policies.length : 0;
}

function activeRunSearchText(run: ActiveAgentSummary): string {
  return [
    run.run_id,
    run.session_id ?? "",
    run.conversation_id ?? "",
    run.status,
    run.model ?? "",
    activeRunSubAgentId(run) ?? "",
    activeRunParentAgentId(run) ?? "",
    activeRunRole(run) ?? "",
    activeRunContractId(run) ?? "",
    run.latest_user_message ?? "",
    JSON.stringify(run.agent_spec ?? {}),
  ]
    .join(" ")
    .toLowerCase();
}

function activeRunTitle(run: ActiveAgentSummary): string {
  const subAgentId = activeRunSubAgentId(run);
  if (subAgentId) return `sub-agent ${subAgentId}`;
  return activeRunContractId(run) ?? activeRunRole(run) ?? run.conversation_id ?? run.session_id ?? run.run_id;
}

// ── Semantic audit items (deduplicated) ────────────────────────────

type AuditItem =
  | { kind: "user_message"; id: string; content: string; ts?: string; contentParts?: ContentPart[] }
  | { kind: "agent_turn"; id: string; content: string; ts?: string; model?: string | null; toolReceipts: ToolReceipt[]; contentParts?: ContentPart[]; status?: string }
  | { kind: "tool_call"; id: string; receipt: ToolReceipt; ts?: string }
  | { kind: "system_note"; id: string; title: string; content: string; ts?: string; variant?: "info" | "warning" | "error" | "success" };

function runDetailToAuditItems(run: RunDetail): AuditItem[] {
  const runId = run.run_id;
  const items: AuditItem[] = [];

  // User request messages
  for (let i = 0; i < run.request_messages.length; i++) {
    const msg = run.request_messages[i];
    items.push({
      kind: "user_message",
      id: `${runId}:request:${i}`,
      content: msg.content,
      contentParts: msg.contentParts,
    });
  }

  // Tool calls (deduplicated — no separate event rows for the same tool)
  if (run.tool_receipts) {
    for (const receipt of run.tool_receipts) {
      items.push({
        kind: "tool_call",
        id: `${runId}:tool:${receipt.id}`,
        receipt,
        ts: receipt.ended_at ?? receipt.started_at,
      });
    }
  }

  // Agent final answer
  if (run.answer?.trim()) {
    items.push({
      kind: "agent_turn",
      id: `${runId}:answer`,
      content: run.answer,
      model: run.model,
      contentParts: run.contentParts,
      toolReceipts: [],
      status: run.status,
      ts: run.updated_at,
    });
  }

  // Non-tool runtime events as compact system notes
  if (run.events) {
    for (const event of run.events) {
      if (event.type === "tool_start" || event.type === "tool_end" || event.type === "tool_update") continue;
      items.push({
        kind: "system_note",
        id: `${runId}:event:${event.type}:${event.at ?? ""}`,
        title: event.type ?? "event",
        content: event.preview ?? event.error ?? "",
        ts: event.at,
        variant: event.error ? "error" : event.type?.includes("failed") ? "error" : "info",
      });
    }
  }

  return items;
}

function rowsToAuditItems(rows: MemorySessionRow[]): AuditItem[] {
  const items: AuditItem[] = [];

  for (const row of rows) {
    if (row.kind === "knoxx.message") {
      const extra = parseMemoryRowExtra(row) ?? {};
      const parts = (extra.contentParts ?? extra.content_parts) as ContentPart[] | undefined;

      if (row.role === "user") {
        items.push({
          kind: "user_message",
          id: row.id,
          content: row.text ?? "",
          ts: row.ts,
          contentParts: parts,
        });
      } else if (row.role === "assistant") {
        items.push({
          kind: "agent_turn",
          id: row.id,
          content: row.text ?? "",
          ts: row.ts,
          model: row.model,
          contentParts: parts,
          toolReceipts: [],
        });
      } else {
        items.push({
          kind: "system_note",
          id: row.id,
          title: row.kind ?? "system",
          content: row.text ?? "",
          ts: row.ts,
        });
      }
    } else if (row.kind === "knoxx.tool_receipt") {
      const extra = parseMemoryRowExtra(row) ?? {};
      const receipt = extra.receipt as ToolReceipt | undefined;
      if (receipt) {
        items.push({
          kind: "tool_call",
          id: row.id,
          receipt,
          ts: row.ts,
        });
      }
    } else if (row.kind === "knoxx.run") {
      const extra = parseMemoryRowExtra(row) ?? {};
      const status = extra.status as string | undefined;
      const model = extra.model as string | undefined;
      items.push({
        kind: "system_note",
        id: row.id,
        title: "Run",
        content: `Status: ${status ?? "unknown"}${model ? ` · Model: ${model}` : ""}`,
        ts: row.ts,
        variant: status === "completed" ? "success" : status === "failed" ? "error" : "info",
      });
    }
    // Skip knoxx.runtime.* events — they are plumbing already covered by receipts
  }

  return items;
}

function eventsToAuditItems(runId: string, events: RunEvent[]): AuditItem[] {
  const items: AuditItem[] = [];

  // Group tool events by tool_call_id into synthetic receipts
  const toolGroups = new Map<string, { start?: RunEvent; end?: RunEvent; updates: RunEvent[] }>();

  for (const event of events) {
    if (event.type === "tool_start" || event.type === "tool_end" || event.type === "tool_update") {
      const id = event.tool_call_id ?? event.tool_name ?? "unknown";
      const group = toolGroups.get(id) ?? { updates: [] };
      if (event.type === "tool_start") group.start = event;
      else if (event.type === "tool_end") group.end = event;
      else group.updates.push(event);
      toolGroups.set(id, group);
    }
  }

  for (const [toolId, group] of toolGroups) {
    const receipt: ToolReceipt = {
      id: toolId,
      tool_name: group.start?.tool_name ?? group.end?.tool_name ?? toolId,
      status: group.end ? (group.end.is_error ? "failed" : "completed") : "running",
      input_preview: group.start?.preview,
      result_preview: group.end?.preview,
      is_error: typeof group.end?.is_error === "boolean" ? group.end.is_error : undefined,
      updates: group.updates.map((e) => e.preview ?? "").filter(Boolean),
    };
    items.push({
      kind: "tool_call",
      id: `${runId}:tool:${toolId}`,
      receipt,
      ts: group.end?.at ?? group.start?.at,
    });
  }

  // Non-tool events
  for (const event of events) {
    if (event.type === "tool_start" || event.type === "tool_end" || event.type === "tool_update") continue;
    items.push({
      kind: "system_note",
      id: `${runId}:event:${event.type}:${event.at ?? ""}`,
      title: event.type ?? "event",
      content: event.preview ?? event.error ?? "",
      ts: event.at,
      variant: event.error ? "error" : event.type?.includes("failed") ? "error" : "info",
    });
  }

  return items;
}

function auditItemSearchText(item: AuditItem): string {
  switch (item.kind) {
    case "user_message":
      return [item.content, item.ts].filter(Boolean).join(" ").toLowerCase();
    case "agent_turn":
      return [item.content, item.model ?? "", item.status ?? "", item.ts].filter(Boolean).join(" ").toLowerCase();
    case "tool_call":
      return [
        item.receipt.tool_name ?? "",
        item.receipt.id,
        item.receipt.input_preview ?? "",
        item.receipt.result_preview ?? "",
      ].filter(Boolean).join(" ").toLowerCase();
    case "system_note":
      return [item.title, item.content, item.ts].filter(Boolean).join(" ").toLowerCase();
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
  const subAgentId = session.sub_agent_id;

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
        {subAgentId ? (
          <>
            <span>•</span>
            <span className="font-mono text-[11px] text-sky-300">sub-agent {subAgentId}</span>
          </>
        ) : null}
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
  const location = useLocation();
  const focus = useMemo(() => {
    const query = new URLSearchParams(location.search);
    return {
      runId: query.get("run") ?? query.get("runId"),
      sessionId: query.get("session") ?? query.get("sessionId") ?? query.get("conversation") ?? query.get("conversationId"),
    };
  }, [location.search]);

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
  const [items, setItems] = useState<AuditItem[]>([]);
  const [loadingRows, setLoadingRows] = useState(false);

  const [sessionsQuery, setSessionsQuery] = useState("");
  const [rowsQuery, setRowsQuery] = useState("");

  const [semanticQuery, setSemanticQuery] = useState("");
  const [semanticLoading, setSemanticLoading] = useState(false);
  const [semanticHits, setSemanticHits] = useState<MemorySearchHit[]>([]);

  const [error, setError] = useState<string | null>(null);
  const [activeRuns, setActiveRuns] = useState<ActiveAgentSummary[]>([]);
  const [loadingActiveRuns, setLoadingActiveRuns] = useState(false);
  const [abortingRunId, setAbortingRunId] = useState<string | null>(null);

  const [availableActors, setAvailableActors] = useState<ActorCatalogItem[]>([]);
  const [sessionActorFilter, setSessionActorFilter] = useState<string>("all");

  const effectiveSessionsQuery = useMemo(() => normalizeSearch(sessionsQuery), [sessionsQuery]);

  const effectiveRowsQuery = useMemo(() => normalizeSearch(rowsQuery), [rowsQuery]);

  const filteredActiveRuns = useMemo(() => {
    if (mode !== "active") return [];
    const query = effectiveSessionsQuery;
    if (!query) return activeRuns;
    return activeRuns.filter((run) => activeRunSearchText(run).includes(query));
  }, [activeRuns, effectiveSessionsQuery, mode]);

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

    const withActorFilter = withBuiltIns.filter((session) => {
      if (sessionActorFilter === "all") return true;
      return session.actor_id === sessionActorFilter;
    });

    if (!query) return withActorFilter;
    return withActorFilter.filter((session) => sessionSearchText(session).includes(query));
  }, [sessions, effectiveSessionsQuery, mode, builtInActorId, builtInContractId, sessionActorFilter]);

  const selectedSessionSummary = useMemo(
    () => sessions.find((session) => session.session === selectedSessionId) ?? null,
    [sessions, selectedSessionId],
  );

  const selectedActiveRun = useMemo(
    () => activeRuns.find((run) => run.conversation_id === selectedSessionId || run.session_id === selectedSessionId) ?? null,
    [activeRuns, selectedSessionId],
  );

  useEffect(() => {
    const nextIds = filteredSessions.map((session) => session.session);
    setSelectedSessionId((current) => {
      if (current && (nextIds.includes(current) || focus.runId || focus.sessionId)) return current;
      return nextIds[0] ?? null;
    });
  }, [filteredSessions, focus.runId, focus.sessionId]);

  const loadActiveRuns = useCallback(async () => {
    try {
      setLoadingActiveRuns(true);
      const runs = await listAdminActiveAgents(250);
      setActiveRuns(runs);
      setError(null);
    } catch {
      // Non-operator viewers may lack org.event_agents.control. Fall back to
      // the existing scoped endpoint; operators still get the all-actors list.
      try {
        const runs = await listActiveAgents(250);
        setActiveRuns(runs);
      } catch (err) {
        setError(err instanceof Error ? err.message : String(err));
      }
    } finally {
      setLoadingActiveRuns(false);
    }
  }, []);

  const loadSessions = useCallback(async (offset = 0) => {
    const isMore = offset > 0;
    try {
      if (!isMore) setLoadingSessions(true);
      else setLoadingMoreSessions(true);

      const actorId = sessionActorFilter !== "all" ? sessionActorFilter : undefined;
      const page = await listMemorySessions({ limit: PAGE_SIZE, offset, actorId });
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
      await Promise.all([loadSessions(0), loadActiveRuns()]);
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
  }, [autoRefresh, loadActiveRuns, loadSessions]);

  useEffect(() => {
    let cancelled = false;
    const runId = focus.runId?.trim();
    const sessionId = focus.sessionId?.trim();

    if (runId) {
      setMode("history");
      setAutoRefresh(false);
      setLoadingRows(true);
      getRun(runId)
        .then((run) => {
          if (cancelled) return;
          setSelectedSessionId(run.conversation_id ?? run.session_id ?? run.run_id);
          setItems(runDetailToAuditItems(run));
          setSemanticHits([]);
          setError(null);
        })
        .catch((err: unknown) => {
          if (!cancelled) setError(err instanceof Error ? err.message : String(err));
        })
        .finally(() => {
          if (!cancelled) setLoadingRows(false);
        });
    } else if (sessionId) {
      setMode("history");
      setAutoRefresh(false);
      setSelectedSessionId(sessionId);
    }

    return () => {
      cancelled = true;
    };
  }, [focus.runId, focus.sessionId]);

  useEffect(() => {
    let cancelled = false;
    getAgentContractsCatalog()
      .then((catalog) => {
        if (cancelled) return;
        setAvailableActors(catalog.actors ?? []);
      })
      .catch(() => {
        // ignore
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleSessionsScroll = async (event: UIEvent<HTMLDivElement>) => {
    if (loadingSessions || loadingMoreSessions || !sessionsHasMore) return;
    const target = event.currentTarget;
    const remaining = target.scrollHeight - target.scrollTop - target.clientHeight;
    if (remaining > 120) return;
    await loadSessions(sessionsRef.current.length);
  };

  const handleAbortRun = useCallback(async (run: ActiveAgentSummary) => {
    const key = run.run_id || run.session_id || run.conversation_id || "active-agent";
    try {
      setAbortingRunId(key);
      await abortAdminActiveAgent({
        conversation_id: run.conversation_id,
        session_id: run.session_id,
        run_id: run.run_id,
        reason: "operator_abort_from_audit_panel",
      });
      await Promise.all([loadActiveRuns(), loadSessions(0)]);
      if (run.conversation_id && selectedSessionId === run.conversation_id) {
        setItems([]);
      }
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setAbortingRunId(null);
    }
  }, [loadActiveRuns, loadSessions, selectedSessionId]);

  useEffect(() => {
    let cancelled = false;

    const load = async () => {
      if (focus.runId) return;
      if (!selectedSessionId) {
        setItems([]);
        setSemanticHits([]);
        return;
      }
      try {
        setLoadingRows(true);
        const selected = sessionsRef.current.find((session) => session.session === selectedSessionId) ?? null;

        // Active sessions may not have been archived into OpenPlanner yet.
        // In that case, fall back to live runtime state (runs + events) so the
        // audit panel still shows *something* for in-flight turns.
        if (selected?.is_active || selectedActiveRun) {
          const activeRuns = selectedActiveRun ? [selectedActiveRun] : await listActiveAgents(150);
          if (cancelled) return;

          const matchingRun = activeRuns.find((run) => run.conversation_id === selectedSessionId || run.session_id === selectedSessionId) ?? null;
          if (matchingRun?.run_id) {
            const detail = await getRun(matchingRun.run_id);
            if (cancelled) return;
            setItems(runDetailToAuditItems(detail));
            setSemanticHits([]);
            setError(null);
            return;
          }

          // Backend restart case: @runs* is empty but Redis still has the session.
          // Fall back to Redis run events directly using run_id from session status.
          try {
            const sessionStatus = await getSessionStatus(selectedSessionId);
            if (cancelled) return;

            // If we have a run_id from Redis, fetch its events
            if (sessionStatus.run_id) {
              const eventsResult = await getRunEvents(sessionStatus.run_id);
              if (cancelled) return;
              if (eventsResult.events && eventsResult.events.length > 0) {
                setItems(eventsToAuditItems(sessionStatus.run_id!, eventsResult.events));
                setSemanticHits([]);
                setError(null);
                return;
              }
            }
          } catch {
            // Session status or events call failed - continue to OpenPlanner fallback
          }
        }

        const result = await getAgentHistorySession(selectedSessionId);
        if (cancelled) return;
        setItems(rowsToAuditItems(result.rows ?? []));
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
  }, [selectedSessionId, autoRefresh, selectedActiveRun, focus.runId]);

  const filteredItems = useMemo(() => {
    if (!effectiveRowsQuery) return items;

    const q = effectiveRowsQuery;
    return items.filter((item) => auditItemSearchText(item).includes(q));
  }, [items, effectiveRowsQuery]);

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
              <div className="grid grid-cols-2 gap-2">
                <Button variant="ghost" size="sm" onClick={() => setAutoRefresh((value) => !value)}>
                  {autoRefresh ? "Pause refresh" : "Resume refresh"}
                </Button>
                <Button variant="ghost" size="sm" onClick={() => void Promise.all([loadSessions(0), loadActiveRuns()])} disabled={loadingSessions || loadingActiveRuns}>
                  Refresh now
                </Button>
              </div>
            </div>

            {availableActors.length > 0 ? (
              <div className="mt-2">
                <select
                  aria-label="Filter by actor"
                  value={sessionActorFilter}
                  onChange={(event) => {
                    setSessionActorFilter(event.target.value);
                    void loadSessions(0);
                  }}
                  className="w-full rounded-md border border-slate-800 bg-slate-950/80 px-2.5 py-2 text-sm text-slate-100 outline-none focus:border-sky-500"
                >
                  <option value="all">All actors</option>
                  {availableActors.map((actor) => (
                    <option key={actor.id} value={actor.id}>{actor.id}</option>
                  ))}
                </select>
              </div>
            ) : null}

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
          {selectedSessionId && loadingRows && items.length === 0 ? <div className="text-sm text-slate-500">Loading session…</div> : null}

          {selectedSessionId ? (
            <div className="space-y-4">
              <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                <div className="min-w-0">
                  <div className="text-lg font-semibold text-slate-100">Agent audit logs</div>
                  <div className="mt-1 font-mono text-xs text-slate-400 break-all">{selectedSessionId}</div>
                  <div className="mt-2 text-xs text-slate-500">OpenPlanner session trace (knoxx-session).</div>
                  {selectedSessionSummary?.sub_agent_id || selectedSessionSummary?.contract_id || selectedSessionSummary?.actor_id || selectedActiveRun ? (
                    <div className="mt-2 flex flex-wrap gap-2 text-[11px] text-slate-500">
                      {selectedSessionSummary?.sub_agent_id ? (
                        <span className="rounded bg-sky-950 px-2 py-1 font-mono text-sky-200">sub-agent {selectedSessionSummary.sub_agent_id}</span>
                      ) : null}
                      {selectedActiveRun && activeRunSubAgentId(selectedActiveRun) ? (
                        <span className="rounded bg-sky-950 px-2 py-1 font-mono text-sky-200">sub-agent {activeRunSubAgentId(selectedActiveRun)}</span>
                      ) : null}
                      {selectedActiveRun?.model ? (
                        <span className="rounded bg-violet-950 px-2 py-1 font-mono text-violet-200">model {selectedActiveRun.model}</span>
                      ) : null}
                      {selectedActiveRun && activeRunToolCount(selectedActiveRun) > 0 ? (
                        <span className="rounded bg-emerald-950 px-2 py-1 font-mono text-emerald-200">{activeRunToolCount(selectedActiveRun)} tools</span>
                      ) : null}
                      {selectedSessionSummary?.contract_id ? (
                        <span className="rounded bg-slate-950 px-2 py-1 font-mono text-slate-300">contract {selectedSessionSummary.contract_id}</span>
                      ) : null}
                      {selectedSessionSummary?.actor_id ? (
                        <span className="rounded bg-slate-950 px-2 py-1 font-mono text-slate-300">actor {selectedSessionSummary.actor_id}</span>
                      ) : null}
                      {selectedActiveRun && activeRunParentAgentId(selectedActiveRun) ? (
                        <span className="rounded bg-slate-950 px-2 py-1 font-mono text-slate-300">parent {activeRunParentAgentId(selectedActiveRun)}</span>
                      ) : null}
                    </div>
                  ) : null}
                </div>
                <div className="flex flex-wrap gap-2">
                  <Badge size="sm" variant="info">{filteredItems.length}/{items.length} items</Badge>
                </div>
              </div>

              {mode === "active" ? (
                <Card variant="outlined" padding="sm">
                  <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
                    <div>
                      <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Live active agent sessions</div>
                      <div className="mt-1 text-xs text-slate-500">Admin/operator view across actors. Abort stops exactly one active conversation/session.</div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Badge size="sm" variant={filteredActiveRuns.length > 0 ? "warning" : "default"}>{filteredActiveRuns.length} running</Badge>
                      <Button variant="ghost" size="sm" onClick={() => void loadActiveRuns()} disabled={loadingActiveRuns}>
                        {loadingActiveRuns ? "Refreshing…" : "Refresh"}
                      </Button>
                    </div>
                  </div>

                  <div className="mt-3 space-y-2">
                    {filteredActiveRuns.length === 0 ? (
                      <div className="text-sm text-slate-500">No active runtime sessions match.</div>
                    ) : (
                      filteredActiveRuns.map((run) => {
                        const key = run.run_id || run.session_id || run.conversation_id || "active-agent";
                        const isSelected = Boolean(selectedSessionId && (run.conversation_id === selectedSessionId || run.session_id === selectedSessionId));
                        const isAborting = abortingRunId === key;
                        const subAgentId = activeRunSubAgentId(run);
                        const parentAgentId = activeRunParentAgentId(run);
                        const toolCount = activeRunToolCount(run);
                        return (
                          <div
                            key={key}
                            className={`rounded-lg border p-3 ${isSelected ? "border-indigo-500 bg-indigo-950/30" : "border-slate-800 bg-slate-950/70"}`}
                          >
                            <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
                              <button
                                type="button"
                                className="min-w-0 flex-1 text-left"
                                onClick={() => setSelectedSessionId(run.conversation_id ?? run.session_id ?? selectedSessionId)}
                              >
                                <div className="flex flex-wrap items-center gap-2">
                                  <span className="text-sm font-semibold text-slate-100">{activeRunTitle(run)}</span>
                                  <Badge size="sm" variant={subAgentId ? "info" : "default"}>{subAgentId ? "sub-agent" : "agent"}</Badge>
                                  <Badge size="sm" variant={activeStatusTone(run.status)}>{run.status}</Badge>
                                  {run.model ? <Badge size="sm" variant={run.model === "gemma4:e4b" ? "success" : "default"}>{run.model}</Badge> : null}
                                  {toolCount > 0 ? <Badge size="sm" variant="info">{toolCount} tools</Badge> : null}
                                  {run.has_active_stream ? <Badge size="sm" variant="info">stream</Badge> : null}
                                  {run.active_turn_registered ? <Badge size="sm" variant="warning">abortable</Badge> : null}
                                </div>
                                <div className="mt-1 break-all font-mono text-xs text-slate-400">{run.conversation_id ?? run.session_id ?? run.run_id}</div>
                                <div className="mt-2 flex flex-wrap gap-2 text-xs text-slate-500">
                                  {parentAgentId ? <span>parent <span className="font-mono text-slate-400">{parentAgentId}</span></span> : null}
                                  {run.run_id ? <span>run <span className="font-mono text-slate-400">{run.run_id}</span></span> : null}
                                  {run.session_id ? <span>session <span className="font-mono text-slate-400">{run.session_id}</span></span> : null}
                                  {run.updated_at ? <span>updated {formatTimestamp(run.updated_at)}</span> : null}
                                </div>
                                {run.latest_user_message ? <div className="mt-2 line-clamp-2 text-xs text-slate-300">{run.latest_user_message}</div> : null}
                                {run.error ? <div className="mt-2 text-xs text-red-300">{run.error}</div> : null}
                              </button>
                              <button
                                type="button"
                                onClick={() => void handleAbortRun(run)}
                                disabled={isAborting}
                                className="rounded-md border border-red-800 bg-red-950/60 px-3 py-2 text-sm font-semibold text-red-100 hover:bg-red-900 disabled:opacity-60"
                                title="Abort only this active agent conversation/session."
                              >
                                {isAborting ? "Stopping…" : "Stop this agent"}
                              </button>
                            </div>
                          </div>
                        );
                      })
                    )}
                  </div>
                </Card>
              ) : null}

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
                <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Session timeline</div>
                <div className="mt-3 space-y-3">
                  {filteredItems.length === 0 ? (
                    <div className="text-sm text-slate-500">No items match your filters.</div>
                  ) : (
                    filteredItems.map((item) => {
                      switch (item.kind) {
                        case "user_message": {
                          const extraParts = item.contentParts ? extractContentParts({ contentParts: item.contentParts }) : [];
                          return (
                            <div key={item.id} className="rounded-lg border border-green-800/30 bg-green-950/20 p-3">
                              <div className="flex items-start justify-between gap-3">
                                <div className="min-w-0">
                                  <div className="text-sm font-semibold text-slate-100">User</div>
                                  {item.ts ? <div className="mt-1 text-xs text-slate-400">{formatTimestamp(item.ts)}</div> : null}
                                </div>
                                <Badge size="sm" variant="success">user</Badge>
                              </div>
                              {item.content ? (
                                <div className="mt-3 text-sm text-slate-200">
                                  <Markdown content={item.content} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                                </div>
                              ) : null}
                              {extraParts.length > 0 ? (
                                <div className="mt-3">
                                  <MultimodalContent parts={extraParts as any} />
                                </div>
                              ) : null}
                            </div>
                          );
                        }

                        case "agent_turn": {
                          const extraParts = item.contentParts ? extractContentParts({ contentParts: item.contentParts }) : [];
                          return (
                            <div key={item.id} className="rounded-lg border border-slate-700 bg-slate-950/70 p-3">
                              <div className="flex items-start justify-between gap-3">
                                <div className="min-w-0">
                                  <div className="text-sm font-semibold text-slate-100">Agent</div>
                                  {item.ts ? <div className="mt-1 text-xs text-slate-400">{formatTimestamp(item.ts)}</div> : null}
                                </div>
                                <div className="flex gap-2">
                                  <Badge size="sm" variant="info">assistant</Badge>
                                  {item.model ? <Badge size="sm" variant="default">{item.model}</Badge> : null}
                                  {item.status ? <Badge size="sm" variant={item.status === "completed" ? "success" : item.status === "failed" ? "error" : "warning"}>{item.status}</Badge> : null}
                                </div>
                              </div>
                              {item.content ? (
                                <div className="mt-3 text-sm text-slate-200">
                                  <Markdown content={item.content} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                                </div>
                              ) : null}
                              {extraParts.length > 0 ? (
                                <div className="mt-3">
                                  <MultimodalContent parts={extraParts as any} />
                                </div>
                              ) : null}
                              {item.toolReceipts.length > 0 ? (
                                <div className="mt-3 space-y-2">
                                  {item.toolReceipts.map((receipt) => (
                                    <ToolReceiptBlock key={receipt.id} receipt={receipt} />
                                  ))}
                                </div>
                              ) : null}
                            </div>
                          );
                        }

                        case "tool_call":
                          return <ToolReceiptBlock key={item.id} receipt={item.receipt} />;

                        case "system_note":
                          return (
                            <div key={item.id} className="rounded-lg border border-slate-800 bg-slate-950/70 p-3">
                              <div className="flex items-start justify-between gap-3">
                                <div className="min-w-0">
                                  <div className="text-sm font-semibold text-slate-100">{item.title}</div>
                                  {item.ts ? <div className="mt-1 text-xs text-slate-400">{formatTimestamp(item.ts)}</div> : null}
                                </div>
                                {item.variant ? <Badge size="sm" variant={item.variant}>{item.variant}</Badge> : null}
                              </div>
                              {item.content ? (
                                <div className="mt-2 text-sm text-slate-200">
                                  <Markdown content={item.content} theme="dark" variant="compact" lineNumbers={false} copyButton={false} />
                                </div>
                              ) : null}
                            </div>
                          );
                      }
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
