import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Badge, Button } from "@open-hax/uxx";
import { acknowledgeActorMailboxEntry, listActorMailbox } from "../lib/api/runtime";
import type { ActorMailboxBox, ActorMailboxEntry, ActorMailboxStatus } from "../lib/types";
import { useAuth } from "./useAuth";

const STATUSES: Array<ActorMailboxStatus | "all"> = ["all", "pending", "delivered", "failed", "acknowledged"];

function recordString(record: Record<string, unknown>, ...keys: string[]): string | null {
  for (const key of keys) {
    const value = record[key];
    if (typeof value === "string" && value.trim()) {
      return value;
    }
  }
  return null;
}

function statusTone(status: string): string {
  switch (status) {
    case "pending":
      return "bg-amber-500/15 text-amber-200 border-amber-500/30";
    case "failed":
      return "bg-red-500/15 text-red-200 border-red-500/30";
    case "delivered":
      return "bg-emerald-500/15 text-emerald-200 border-emerald-500/30";
    case "acknowledged":
      return "bg-sky-500/15 text-sky-200 border-sky-500/30";
    default:
      return "bg-slate-500/15 text-slate-200 border-slate-500/30";
  }
}

function formatDate(value?: string): string {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function findStringDeep(value: unknown, keys: string[], depth = 0): string | null {
  if (depth > 4 || value === null || typeof value !== "object") return null;
  if (Array.isArray(value)) {
    for (const item of value) {
      const found = findStringDeep(item, keys, depth + 1);
      if (found) return found;
    }
    return null;
  }

  const record = value as Record<string, unknown>;
  for (const key of keys) {
    const direct = record[key];
    if (typeof direct === "string" && direct.trim()) return direct.trim();
  }
  for (const nested of Object.values(record)) {
    const found = findStringDeep(nested, keys, depth + 1);
    if (found) return found;
  }
  return null;
}

function mailboxLinks(entry: ActorMailboxEntry): Array<{ label: string; path: string; detail: string }> {
  const runId = findStringDeep(entry.contentRef, ["run-id", "runId", "run_id", "source-run-id", "target-run-id", "sourceRunId", "targetRunId"]);
  const sessionId = findStringDeep(entry.contentRef, ["session-id", "sessionId", "session_id"])
    ?? findStringDeep(entry.target, ["session-id", "sessionId", "session_id"])
    ?? findStringDeep(entry.source, ["session-id", "sessionId", "session_id"]);
  const conversationId = findStringDeep(entry.contentRef, ["conversation-id", "conversationId", "conversation_id"])
    ?? findStringDeep(entry.target, ["conversation-id", "conversationId", "conversation_id"])
    ?? findStringDeep(entry.source, ["conversation-id", "conversationId", "conversation_id"]);
  const eventId = findStringDeep(entry.contentRef, ["event-id", "eventId", "event_id"]);

  const links: Array<{ label: string; path: string; detail: string }> = [];
  if (runId) links.push({ label: "Open run", path: `/agents?tab=audit&run=${encodeURIComponent(runId)}`, detail: runId });
  if (conversationId || sessionId) {
    const id = conversationId ?? sessionId!;
    links.push({ label: "Open session", path: `/agents?tab=audit&session=${encodeURIComponent(id)}`, detail: id });
  }
  if (eventId) links.push({ label: "Open event", path: `/events?eventId=${encodeURIComponent(eventId)}`, detail: eventId });
  return links;
}

function MailboxCard({ entry, box, onAck, acking }: {
  entry: ActorMailboxEntry;
  box: ActorMailboxBox;
  onAck: (id: string) => void;
  acking: boolean;
}) {
  const navigate = useNavigate();
  const from = recordString(entry.source, "actor-id", "actorId", "session-id", "sessionId") ?? "unknown";
  const to = recordString(entry.target, "actor-id", "actorId", "session-id", "sessionId", "conversation-id", "conversationId") ?? "unknown";
  const mode = recordString(entry.delivery, "mode") ?? "message";
  const attempts = typeof entry.delivery.attempts === "number" ? entry.delivery.attempts : null;
  const contentRef = JSON.stringify(entry.contentRef ?? {});
  const links = mailboxLinks(entry);

  return (
    <article className="rounded-xl border border-slate-800 bg-slate-950/70 p-4 shadow-lg shadow-black/20">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className={`rounded-full border px-2 py-0.5 text-xs font-semibold ${statusTone(entry.status)}`}>{entry.status}</span>
            <span className="rounded-full border border-slate-700 bg-slate-900 px-2 py-0.5 text-xs text-slate-300">{mode}</span>
            {attempts !== null ? <span className="text-xs text-slate-500">attempts {attempts}</span> : null}
          </div>
          <div className="mt-2 text-sm text-slate-300">
            <span className="text-slate-500">From</span> <span className="font-mono text-slate-100">{from}</span>
            <span className="mx-2 text-slate-600">→</span>
            <span className="text-slate-500">To</span> <span className="font-mono text-slate-100">{to}</span>
          </div>
        </div>
        {box === "inbox" && entry.status !== "acknowledged" ? (
          <Button size="sm" variant="secondary" loading={acking} onClick={() => onAck(entry.id)}>Acknowledge</Button>
        ) : null}
      </div>

      <p className="mt-4 whitespace-pre-wrap rounded-lg border border-slate-800 bg-slate-900/70 p-3 text-sm leading-6 text-slate-100">
        {entry.preview || "No preview available. Open the referenced run/event for full content."}
      </p>

      <dl className="mt-3 grid gap-2 text-xs text-slate-500 md:grid-cols-3">
        <div><dt className="uppercase tracking-wide text-slate-600">Created</dt><dd>{formatDate(entry.createdAt)}</dd></div>
        <div><dt className="uppercase tracking-wide text-slate-600">Delivered</dt><dd>{formatDate(entry.deliveredAt)}</dd></div>
        <div><dt className="uppercase tracking-wide text-slate-600">Content ref</dt><dd className="truncate font-mono" title={contentRef}>{contentRef}</dd></div>
      </dl>
      {links.length > 0 ? (
        <div className="mt-3 flex flex-wrap gap-2">
          {links.map((link) => (
            <Button key={`${link.label}:${link.detail}`} size="sm" variant="ghost" onClick={() => navigate(link.path)}>
              {link.label}
            </Button>
          ))}
        </div>
      ) : null}
      {entry.lastError ? <div className="mt-3 rounded border border-red-500/30 bg-red-500/10 p-2 text-xs text-red-200">{entry.lastError}</div> : null}
    </article>
  );
}

export default function MailPage() {
  const auth = useAuth();
  const [box, setBox] = useState<ActorMailboxBox>("inbox");
  const [status, setStatus] = useState<ActorMailboxStatus | "all">("all");
  const [entries, setEntries] = useState<ActorMailboxEntry[]>([]);
  const [actorId, setActorId] = useState<string | null>(auth.actor?.id ?? auth.membership?.actorId ?? null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [ackingId, setAckingId] = useState<string | null>(null);

  const unreadCount = useMemo(() => entries.filter((entry) => entry.status !== "acknowledged").length, [entries]);

  const loadMailbox = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await listActorMailbox(box, status);
      setEntries(response.entries);
      setActorId(response.actorId ?? auth.actor?.id ?? auth.membership?.actorId ?? null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Mailbox unavailable");
      setEntries([]);
    } finally {
      setLoading(false);
    }
  }, [auth.actor?.id, auth.membership?.actorId, box, status]);

  useEffect(() => {
    void loadMailbox();
  }, [loadMailbox]);

  const acknowledge = async (id: string) => {
    setAckingId(id);
    setError(null);
    try {
      await acknowledgeActorMailboxEntry(id);
      await loadMailbox();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to acknowledge mailbox entry");
    } finally {
      setAckingId(null);
    }
  };

  return (
    <div className="flex min-h-0 flex-1 flex-col bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 bg-slate-950/90 px-6 py-5">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-2xl font-semibold tracking-tight">Mail</h1>
              <Badge>{actorId ? `actor ${actorId}` : "actor unavailable"}</Badge>
            </div>
            <p className="mt-1 max-w-3xl text-sm text-slate-400">
              Actor mailbox for asynchronous Knoxx messages. Users are actors too, so agents can deliver work, alerts, and handoffs here without needing a live chat turn.
            </p>
          </div>
          <Button size="sm" variant="secondary" loading={loading} onClick={() => void loadMailbox()}>Refresh</Button>
        </div>

        <div className="mt-5 flex flex-wrap items-center gap-3">
          <div className="rounded-lg border border-slate-800 bg-slate-900 p-1">
            {(["inbox", "outbox"] as ActorMailboxBox[]).map((candidate) => (
              <button
                key={candidate}
                type="button"
                onClick={() => setBox(candidate)}
                className={`rounded-md px-3 py-1.5 text-sm capitalize transition ${box === candidate ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800 hover:text-slate-100"}`}
              >
                {candidate}
              </button>
            ))}
          </div>
          <label className="flex items-center gap-2 text-sm text-slate-400">
            Status
            <select
              value={status}
              onChange={(event) => setStatus(event.target.value as ActorMailboxStatus | "all")}
              className="rounded-md border border-slate-700 bg-slate-900 px-2 py-1.5 text-sm text-slate-100"
            >
              {STATUSES.map((candidate) => <option key={candidate} value={candidate}>{candidate}</option>)}
            </select>
          </label>
          <span className="text-sm text-slate-500">{box === "inbox" ? `${unreadCount} not acknowledged` : `${entries.length} sent entries`}</span>
        </div>
      </header>

      <main className="min-h-0 flex-1 overflow-y-auto p-6">
        {error ? <div className="mb-4 rounded-lg border border-red-500/30 bg-red-500/10 p-3 text-sm text-red-200">{error}</div> : null}
        {loading ? <div className="rounded-xl border border-slate-800 bg-slate-900 p-6 text-sm text-slate-400">Loading mailbox…</div> : null}
        {!loading && entries.length === 0 ? (
          <div className="rounded-xl border border-dashed border-slate-800 bg-slate-900/60 p-8 text-center text-sm text-slate-400">
            No {box} entries match this filter yet.
          </div>
        ) : null}
        <div className="grid gap-4">
          {entries.map((entry) => (
            <MailboxCard key={entry.id} entry={entry} box={box} acking={ackingId === entry.id} onAck={(id) => void acknowledge(id)} />
          ))}
        </div>
      </main>
    </div>
  );
}
