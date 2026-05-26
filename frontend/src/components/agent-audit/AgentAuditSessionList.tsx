import { useCallback, useEffect, useMemo, useRef, useState, type UIEvent } from "react";
import { Badge, Button } from "@open-hax/uxx";
import { listActiveAgents, listAdminActiveAgents, listMemorySessions } from "../../lib/api/common";
import type { ActiveAgentSummary, MemorySessionSummary } from "../../lib/types";
import type { ChatWorkspaceController } from "../chat-page/useChatWorkspaceController";

type AuditSessionSource = "active" | "history";

export type AuditSessionSummary = MemorySessionSummary & {
  auditSource: AuditSessionSource;
  run_id?: string | null;
  model?: string | null;
  latest_user_message?: string | null;
};

export type AgentAuditSessionListProps = {
  controller: Pick<ChatWorkspaceController,
    | "conversationId"
    | "sessionId"
    | "loadingMemorySessionId"
    | "resumeMemorySession"
  >;
  builtInContractId?: string | null;
  className?: string;
};

const AUDIT_SESSION_PAGE_SIZE = 20;
const ACTIVE_RUN_LIMIT = 25;

function normalizeSearch(value: string): string {
  return value.trim().toLowerCase();
}

function formatMaybeDate(value?: string | number | null): string | null {
  if (value == null) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString();
}

function specString(spec: Record<string, unknown> | undefined, keys: string[]): string | null {
  if (!spec) return null;
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

function activeRunActorId(run: ActiveAgentSummary): string | null {
  return specString(run.agent_spec ?? {}, ["actorId", "actor_id", "actor-id"]);
}

function activeRunTriggerId(run: ActiveAgentSummary): string | null {
  return specString(run.agent_spec ?? {}, ["triggerId", "trigger_id", "trigger-id"]);
}

function activeRunEventType(run: ActiveAgentSummary): string | null {
  return specString(run.agent_spec ?? {}, ["eventType", "event_type", "event-type", "triggerEventType", "trigger_event_type", "trigger-event-type"]);
}

function activeRunEventId(run: ActiveAgentSummary): string | null {
  return specString(run.agent_spec ?? {}, ["eventId", "event_id", "event-id"]);
}

function activeRunEventScopeId(run: ActiveAgentSummary): string | null {
  return specString(run.agent_spec ?? {}, ["eventScopeId", "event_scope_id", "event-scope-id"]);
}

function activeRunScheduleId(run: ActiveAgentSummary): string | null {
  return specString(run.agent_spec ?? {}, ["scheduleId", "schedule_id", "schedule-id"]);
}

function activeRunEventTypes(run: ActiveAgentSummary): string[] | undefined {
  const values = run.agent_spec?.eventTypes ?? run.agent_spec?.event_types ?? run.agent_spec?.["event-types"];
  if (!Array.isArray(values)) return undefined;
  const normalized = values.filter((value): value is string => typeof value === "string" && value.trim().length > 0);
  return normalized.length > 0 ? normalized : undefined;
}

function activeRunTitle(run: ActiveAgentSummary): string {
  const subAgentId = activeRunSubAgentId(run);
  if (subAgentId) return `sub-agent ${subAgentId}`;
  return activeRunContractId(run) ?? activeRunRole(run) ?? run.conversation_id ?? run.session_id ?? run.run_id;
}

function auditSessionTimestamp(session: AuditSessionSummary): number {
  const parsed = Date.parse(session.last_ts ?? "");
  return Number.isFinite(parsed) ? parsed : 0;
}

function auditSessionActivityScore(session: AuditSessionSummary): number {
  if (session.has_active_stream) return 50;
  const status = session.active_status ?? "";
  if (status === "running") return 45;
  if (status === "queued") return 40;
  if (status === "waiting_input") return 35;
  if (session.is_active) return 30;
  if (status === "failed") return 20;
  return 0;
}

export function auditSessionMatchesContract(session: MemorySessionSummary, contractId?: string | null): boolean {
  const target = contractId?.trim();
  if (!target || target === "new-agent") return true;
  return session.contract_id === target
    || session.sub_agent_id === target
    || session.parent_agent_id === target
    || Boolean(session.contract_actors?.includes(target));
}

export function activeRunMatchesContract(run: ActiveAgentSummary, contractId?: string | null): boolean {
  const target = contractId?.trim();
  if (!target || target === "new-agent") return true;
  return activeRunContractId(run) === target
    || activeRunSubAgentId(run) === target
    || activeRunParentAgentId(run) === target
    || activeRunRole(run) === target;
}

export function activeRunToAuditSession(run: ActiveAgentSummary): AuditSessionSummary | null {
  const session = run.conversation_id ?? run.session_id ?? run.run_id;
  if (!session) return null;
  const contractId = activeRunContractId(run);
  const subAgentId = activeRunSubAgentId(run);
  const parentAgentId = activeRunParentAgentId(run);
  const actorId = activeRunActorId(run);
  return {
    auditSource: "active",
    project: "knoxx-session",
    session,
    title: activeRunTitle(run),
    last_ts: run.updated_at ?? run.created_at,
    event_count: 0,
    actor_id: actorId ?? subAgentId ?? parentAgentId ?? activeRunRole(run) ?? undefined,
    contract_id: contractId ?? subAgentId ?? undefined,
    sub_agent_id: subAgentId ?? undefined,
    parent_agent_id: parentAgentId ?? undefined,
    is_active: true,
    active_status: run.status,
    has_active_stream: run.has_active_stream,
    active_session_id: run.session_id ?? null,
    run_id: run.run_id,
    model: run.model,
    latest_user_message: run.latest_user_message,
    trigger_id: activeRunTriggerId(run) ?? undefined,
    event_type: activeRunEventType(run) ?? undefined,
    event_types: activeRunEventTypes(run),
    event_id: activeRunEventId(run) ?? undefined,
    event_scope_id: activeRunEventScopeId(run) ?? undefined,
    schedule_id: activeRunScheduleId(run) ?? undefined,
  };
}

function mergeAuditSession(left: AuditSessionSummary, right: AuditSessionSummary): AuditSessionSummary {
  const leftScore = auditSessionActivityScore(left);
  const rightScore = auditSessionActivityScore(right);
  const live = rightScore >= leftScore ? right : left;
  const history = left.auditSource === "history" ? left : right.auditSource === "history" ? right : left;

  return {
    ...history,
    ...live,
    title: live.title ?? history.title,
    last_ts: auditSessionTimestamp(live) >= auditSessionTimestamp(history) ? live.last_ts : history.last_ts,
    event_count: Math.max(left.event_count ?? 0, right.event_count ?? 0),
    contract_id: live.contract_id ?? history.contract_id,
    contract_actors: live.contract_actors ?? history.contract_actors,
    actor_id: live.actor_id ?? history.actor_id,
    auditSource: live.auditSource === "active" || history.auditSource === "active" ? "active" : "history",
    is_active: Boolean(left.is_active || right.is_active),
    has_active_stream: Boolean(left.has_active_stream || right.has_active_stream),
    active_session_id: live.active_session_id ?? history.active_session_id ?? null,
    trigger_id: live.trigger_id ?? history.trigger_id,
    event_type: live.event_type ?? history.event_type,
    event_types: live.event_types ?? history.event_types,
    event_id: live.event_id ?? history.event_id,
    event_scope_id: live.event_scope_id ?? history.event_scope_id,
    schedule_id: live.schedule_id ?? history.schedule_id,
  };
}

export function mergeAuditSessions(memorySessions: MemorySessionSummary[], activeRuns: ActiveAgentSummary[], contractId?: string | null): AuditSessionSummary[] {
  const byId = new Map<string, AuditSessionSummary>();

  for (const session of memorySessions) {
    if (!auditSessionMatchesContract(session, contractId)) continue;
    byId.set(session.session, { ...session, auditSource: session.is_active ? "active" : "history" });
  }

  for (const run of activeRuns) {
    if (!activeRunMatchesContract(run, contractId)) continue;
    const auditSession = activeRunToAuditSession(run);
    if (!auditSession) continue;
    const existing = byId.get(auditSession.session);
    byId.set(auditSession.session, existing ? mergeAuditSession(existing, auditSession) : auditSession);
  }

  return [...byId.values()].sort((left, right) => {
    const leftScore = auditSessionActivityScore(left);
    const rightScore = auditSessionActivityScore(right);
    if (rightScore !== leftScore) return rightScore - leftScore;
    const timeDiff = auditSessionTimestamp(right) - auditSessionTimestamp(left);
    if (timeDiff !== 0) return timeDiff;
    return (left.title ?? left.session).localeCompare(right.title ?? right.session);
  });
}

export function auditSessionStatus(session: AuditSessionSummary): { label: string; variant: "default" | "success" | "warning" | "error" | "info" } {
  if (session.has_active_stream) return { label: "Live", variant: "warning" };
  if (session.active_status === "running") return { label: "Active", variant: "success" };
  if (session.active_status === "waiting_input") return { label: "Waiting", variant: "info" };
  if (session.active_status === "failed") return { label: "Failed", variant: "error" };
  if (session.is_active) return { label: "Active", variant: "success" };
  return { label: "History", variant: "default" };
}

function auditSessionSearchText(session: AuditSessionSummary): string {
  return [
    session.session,
    session.title ?? "",
    session.actor_id ?? "",
    session.contract_id ?? "",
    session.sub_agent_id ?? "",
    session.parent_agent_id ?? "",
    session.model ?? "",
    session.latest_user_message ?? "",
    session.trigger_id ?? "",
    session.event_type ?? "",
    session.event_id ?? "",
    session.event_scope_id ?? "",
    session.schedule_id ?? "",
    (session.event_types ?? []).join(" "),
    (session.contract_actors ?? []).join(" "),
  ].join(" ").toLowerCase();
}

async function listOperatorActiveAgents(): Promise<ActiveAgentSummary[]> {
  try {
    return await listAdminActiveAgents(ACTIVE_RUN_LIMIT);
  } catch {
    return await listActiveAgents(ACTIVE_RUN_LIMIT);
  }
}

function mergeAuditSessionPages(current: AuditSessionSummary[], next: AuditSessionSummary[]): AuditSessionSummary[] {
  const byId = new Map<string, AuditSessionSummary>();

  for (const session of current) {
    byId.set(session.session, session);
  }

  for (const session of next) {
    const existing = byId.get(session.session);
    byId.set(session.session, existing ? mergeAuditSession(existing, session) : session);
  }

  return [...byId.values()].sort((left, right) => {
    const leftScore = auditSessionActivityScore(left);
    const rightScore = auditSessionActivityScore(right);
    if (rightScore !== leftScore) return rightScore - leftScore;
    const timeDiff = auditSessionTimestamp(right) - auditSessionTimestamp(left);
    if (timeDiff !== 0) return timeDiff;
    return (left.title ?? left.session).localeCompare(right.title ?? right.session);
  });
}

export function AgentAuditSessionList({ controller, builtInContractId, className }: AgentAuditSessionListProps) {
  const [sessions, setSessions] = useState<AuditSessionSummary[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [hasLoaded, setHasLoaded] = useState(false);
  const [hasMore, setHasMore] = useState(false);
  const [nextOffset, setNextOffset] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const loadSeqRef = useRef(0);

  const loadSessions = useCallback(async (offset = 0) => {
    const isMore = offset > 0;
    const loadSeq = loadSeqRef.current + 1;
    loadSeqRef.current = loadSeq;
    const contractId = builtInContractId;
    if (isMore) setLoadingMore(true);
    else setLoading(true);
    try {
      const trimmedContractId = contractId?.trim();
      const scopedContractId = trimmedContractId && trimmedContractId !== "new-agent" ? trimmedContractId : undefined;
      const [memoryPage, activeRuns] = await Promise.all([
        listMemorySessions({
          limit: AUDIT_SESSION_PAGE_SIZE,
          offset,
          contractId: scopedContractId,
        }),
        listOperatorActiveAgents(),
      ]);
      if (loadSeqRef.current !== loadSeq) return;
      const nextRows = memoryPage.rows ?? [];
      const nextSessions = mergeAuditSessions(nextRows, activeRuns, contractId);
      setSessions((current) => (isMore ? mergeAuditSessionPages(current, nextSessions) : nextSessions));
      setNextOffset(offset + nextRows.length);
      setHasMore(Boolean(memoryPage.has_more));
      setHasLoaded(true);
      setError(null);
    } catch (err) {
      if (loadSeqRef.current !== loadSeq) return;
      setError(err instanceof Error ? err.message : String(err));
      setHasLoaded(true);
    } finally {
      if (loadSeqRef.current === loadSeq) {
        if (isMore) setLoadingMore(false);
        else setLoading(false);
      }
    }
  }, [builtInContractId]);

  useEffect(() => {
    setSessions([]);
    setNextOffset(0);
    setHasMore(false);
    setHasLoaded(false);
    void loadSessions(0);
    const timer = window.setInterval(() => void loadSessions(0), 60000);
    return () => window.clearInterval(timer);
  }, [loadSessions]);

  const filteredSessions = useMemo(() => {
    const q = normalizeSearch(query);
    if (!q) return sessions;
    return sessions.filter((session) => auditSessionSearchText(session).includes(q));
  }, [sessions, query]);

  const handleScroll = (event: UIEvent<HTMLDivElement>) => {
    const target = event.currentTarget;
    const remaining = target.scrollHeight - target.scrollTop - target.clientHeight;
    if (remaining < 120 && hasMore && !loading && !loadingMore) {
      void loadSessions(nextOffset);
    }
  };

  return (
    <div className={`flex min-h-0 flex-1 flex-col overflow-hidden ${className ?? ""}`}>
      <div className="shrink-0 border-b border-slate-900/80 px-2 py-2">
        <div className="flex items-center justify-between gap-2">
          <div className="min-w-0">
            <div className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">Sessions</div>
            <div className="truncate font-mono text-[10px] text-slate-600">{builtInContractId || "all agents"}</div>
          </div>
          <div className="flex shrink-0 items-center gap-1">
            <Badge size="sm" variant="default">{filteredSessions.length}</Badge>
            <Button variant="ghost" size="sm" loading={loading} onClick={() => void loadSessions(0)}>↻</Button>
          </div>
        </div>
        <input
          aria-label="Search audit sessions"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search sessions…"
          className="mt-2 w-full rounded-md border border-slate-800 bg-slate-950/70 px-2 py-1.5 text-xs text-slate-100 outline-none focus:border-sky-500"
        />
        {error ? <div className="mt-2 rounded border border-rose-500/30 bg-rose-500/10 px-2 py-1 text-[10px] text-rose-200">{error}</div> : null}
      </div>

      <div aria-label="Audit sessions list" className="min-h-0 flex-1 space-y-1 overflow-y-auto p-2" onScroll={handleScroll}>
        {loading && !hasLoaded ? <div className="p-2 text-xs text-slate-500">Loading sessions…</div> : null}
        {hasLoaded && filteredSessions.length === 0 ? <div className="p-2 text-xs text-slate-500">No sessions match this agent.</div> : null}
        {filteredSessions.map((session) => {
          const isCurrent = (controller.conversationId && controller.conversationId === session.session)
            || (controller.sessionId && controller.sessionId === session.active_session_id);
          const status = auditSessionStatus(session);
          const isActive = session.auditSource === "active" || Boolean(session.is_active);
          const loadingThis = controller.loadingMemorySessionId === session.session;
          return (
            <button
              key={session.session}
              type="button"
              onClick={() => void controller.resumeMemorySession(session.session)}
              aria-pressed={Boolean(isCurrent)}
              className={`w-full rounded-lg border px-2 py-2 text-left transition ${
                isCurrent
                  ? "border-sky-500/60 bg-sky-500/10"
                  : isActive
                    ? "border-emerald-500/35 bg-emerald-500/10 hover:border-emerald-400/50"
                    : "border-slate-800 bg-slate-950/35 hover:border-slate-700 hover:bg-slate-950/70"
              }`}
            >
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <div className="truncate text-xs font-semibold text-slate-100">{session.title || session.session}</div>
                  <div className="mt-0.5 truncate font-mono text-[10px] text-slate-500">{session.session}</div>
                </div>
                <div className="flex shrink-0 flex-col items-end gap-1">
                  {isCurrent ? <Badge size="sm" variant="info">Open</Badge> : null}
                  <Badge size="sm" variant={status.variant}>{loadingThis ? "Loading" : status.label}</Badge>
                </div>
              </div>
              <div className="mt-1 flex flex-wrap gap-x-2 gap-y-1 text-[10px] text-slate-500">
                {session.event_count != null ? <span>{session.event_count} ev</span> : null}
                {session.model ? <span className="font-mono text-violet-300">{session.model}</span> : null}
                {session.contract_id ? <span className="font-mono text-slate-400">{session.contract_id}</span> : null}
                {session.actor_id ? <span className="font-mono text-slate-400">{session.actor_id}</span> : null}
                {session.trigger_id ? <span className="font-mono text-amber-300">trigger {session.trigger_id}</span> : null}
                {session.event_type ? <span className="font-mono text-cyan-300">event {session.event_type}</span> : null}
                {session.schedule_id ? <span className="font-mono text-slate-400">schedule {session.schedule_id}</span> : null}
                {session.last_ts ? <span>{formatMaybeDate(session.last_ts)}</span> : null}
              </div>
              {session.latest_user_message ? <div className="mt-1 line-clamp-2 text-[10px] leading-4 text-slate-400">{session.latest_user_message}</div> : null}
            </button>
          );
        })}
        {loadingMore ? <div className="p-2 text-xs text-slate-500">Loading more sessions…</div> : null}
      </div>
    </div>
  );
}

export default AgentAuditSessionList;
