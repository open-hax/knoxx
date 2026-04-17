import React, { useCallback, useEffect, useMemo, useState } from "react";

import {
  getProxxProviderModelAnalytics,
  getProxxUsageOverview,
  listProxxRequestLogs,
  type ProxxProviderModelAnalytics,
  type ProxxProviderModelAnalyticsRow,
  type ProxxRequestLogEntry,
  type ProxxUsageOverview,
  type ProxxUsageWindow,
} from "../../lib/api/proxxObservability";
import { Badge, SectionCard, classNames } from "./common";

type Notice = { tone: "success" | "error"; text: string } | null;

function formatCount(value: number | null | undefined): string {
  if (typeof value !== "number" || !Number.isFinite(value)) return "0";
  return Intl.NumberFormat().format(Math.round(value));
}

function formatUsd(value: number | null | undefined): string {
  if (typeof value !== "number" || !Number.isFinite(value)) return "$0";
  return `$${value.toFixed(4)}`;
}

function formatPercent(value: number | null | undefined): string {
  if (typeof value !== "number" || !Number.isFinite(value)) return "0%";
  return `${value.toFixed(2)}%`;
}

function toneForStatus(status: number): "success" | "warn" | "danger" | "default" {
  if (status >= 500) return "danger";
  if (status >= 400) return "warn";
  if (status >= 200 && status < 300) return "success";
  return "default";
}

function renderStatusBadge(status: number) {
  const tone = toneForStatus(status);
  return <Badge tone={tone === "default" ? "default" : tone}>{status}</Badge>;
}

function toLocalTimestamp(ms: number): string {
  try {
    return new Date(ms).toLocaleString();
  } catch {
    return String(ms);
  }
}

function topRows(rows: readonly ProxxProviderModelAnalyticsRow[], limit: number): ProxxProviderModelAnalyticsRow[] {
  return [...rows]
    .sort((a, b) => (b.requestCount ?? 0) - (a.requestCount ?? 0))
    .slice(0, limit);
}

export function ProxxObservabilitySection({ canView }: { canView: boolean }) {
  const [window, setWindow] = useState<ProxxUsageWindow>("daily");
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState<Notice>(null);
  const [error, setError] = useState<string>("");

  const [overview, setOverview] = useState<ProxxUsageOverview | null>(null);
  const [analytics, setAnalytics] = useState<ProxxProviderModelAnalytics | null>(null);
  const [logs, setLogs] = useState<ProxxRequestLogEntry[]>([]);

  const [modelFilter, setModelFilter] = useState<string>("");

  const filteredLogs = useMemo(() => {
    const normalized = modelFilter.trim().toLowerCase();
    if (!normalized) return logs;
    return logs.filter((entry) => (entry.model ?? "").toLowerCase().includes(normalized));
  }, [logs, modelFilter]);

  const load = useCallback(async () => {
    if (!canView) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError("");
    setNotice(null);

    try {
      const [overviewResp, analyticsResp, logsResp] = await Promise.all([
        getProxxUsageOverview(window),
        getProxxProviderModelAnalytics(window).catch(() => null),
        listProxxRequestLogs({ limit: 200 }).catch(() => ({ entries: [] })),
      ]);

      setOverview(overviewResp);
      setAnalytics(analyticsResp);
      setLogs(logsResp.entries ?? []);

      setNotice({ tone: "success", text: `Loaded Proxx observability (${window}).` });
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setOverview(null);
      setAnalytics(null);
      setLogs([]);
    } finally {
      setLoading(false);
    }
  }, [canView, window]);

  useEffect(() => {
    void load();
  }, [load]);

  const summary = overview?.summary;
  const routing = summary?.routingRequests24h;

  const topProviderModels = useMemo(() => {
    if (!analytics) return [];
    return topRows(analytics.providerModels ?? [], 20);
  }, [analytics]);

  return (
    <SectionCard
      title="Proxx observability"
      description="Usage analytics + recent request logs for the Proxx instance Knoxx is configured to use."
      actions={
        <div className="flex flex-col gap-2 md:flex-row md:items-center">
          <select
            value={window}
            onChange={(event) => setWindow(event.target.value as ProxxUsageWindow)}
            disabled={!canView || loading}
            className="rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 disabled:opacity-60"
          >
            <option value="daily">Daily (24h)</option>
            <option value="weekly">Weekly (7d)</option>
            <option value="monthly">Monthly (30d)</option>
          </select>
          <button
            type="button"
            onClick={() => void load()}
            disabled={!canView || loading}
            className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
          >
            {loading ? "Loading…" : "Refresh"}
          </button>
        </div>
      }
    >
      {!canView ? (
        <div className="rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
          You do not have <code className="font-mono">org.proxx.observability.read</code> permission.
        </div>
      ) : null}

      {notice ? (
        <div
          className={notice.tone === "success"
            ? "mb-4 rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-sm text-emerald-200"
            : "mb-4 rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200"}
        >
          {notice.text}
        </div>
      ) : null}

      {error ? (
        <div className="mb-4 rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">
          {error}
        </div>
      ) : null}

      {canView ? (
        <div className="space-y-6">
          <div className="grid gap-3 md:grid-cols-3">
            <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Requests (24h)</div>
              <div className="mt-2 text-2xl font-semibold text-slate-100">{formatCount(summary?.requests24h)}</div>
              <div className="mt-1 text-xs text-slate-500">Errors: {formatPercent(summary?.errorRate24h)}</div>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Tokens (24h)</div>
              <div className="mt-2 text-2xl font-semibold text-slate-100">{formatCount(summary?.tokens24h)}</div>
              <div className="mt-1 text-xs text-slate-500">Cache hit rate: {formatPercent(summary?.cacheHitRate24h)}</div>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Cost (24h)</div>
              <div className="mt-2 text-2xl font-semibold text-slate-100">{formatUsd(summary?.costUsd24h)}</div>
              <div className="mt-1 text-xs text-slate-500">Active accounts: {formatCount(summary?.activeAccounts)}</div>
            </div>
          </div>

          <div className="grid gap-3 md:grid-cols-2">
            <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Top model</div>
              <div className="mt-2 text-lg font-semibold text-slate-100">{summary?.topModel ?? "(none)"}</div>
              <div className="mt-1 text-xs text-slate-500">Top provider: {summary?.topProvider ?? "(none)"}</div>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-950/40 p-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">Routing (24h)</div>
              {routing ? (
                <div className="mt-2 space-y-1 text-sm text-slate-200">
                  <div>Local: {formatCount(routing.local)} · Federated: {formatCount(routing.federated)} · Bridge: {formatCount(routing.bridge)}</div>
                  <div className="text-xs text-slate-500">Peers: {formatCount(routing.distinctPeers)} · Top peer: {routing.topPeer ?? "(none)"}</div>
                </div>
              ) : (
                <div className="mt-2 text-sm text-slate-500">No routing data.</div>
              )}
            </div>
          </div>

          <div className="space-y-2">
            <div className="flex flex-col gap-2 md:flex-row md:items-center md:justify-between">
              <div>
                <div className="text-sm font-semibold text-slate-100">Recent request logs</div>
                <div className="text-xs text-slate-500">Showing up to 200 newest entries (filters are applied client-side for model).</div>
              </div>
              <label className="flex items-center gap-2 text-sm text-slate-200">
                <span className="text-xs text-slate-500">Model filter</span>
                <input
                  value={modelFilter}
                  onChange={(event) => setModelFilter(event.target.value)}
                  className="w-60 rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500"
                  placeholder="e.g. gemma4"
                />
              </label>
            </div>

            <div className="overflow-auto rounded-xl border border-slate-800">
              <table className="min-w-full divide-y divide-slate-800 text-sm">
                <thead className="bg-slate-950/70 text-xs uppercase tracking-wide text-slate-400">
                  <tr>
                    <th className="px-3 py-2 text-left">Time</th>
                    <th className="px-3 py-2 text-left">Status</th>
                    <th className="px-3 py-2 text-left">Model</th>
                    <th className="px-3 py-2 text-left">Provider</th>
                    <th className="px-3 py-2 text-right">Latency</th>
                    <th className="px-3 py-2 text-right">Tokens</th>
                    <th className="px-3 py-2 text-right">Cost</th>
                    <th className="px-3 py-2 text-left">Error</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800 bg-slate-950/40">
                  {filteredLogs.length === 0 ? (
                    <tr>
                      <td className="px-3 py-3 text-slate-500" colSpan={8}>
                        No request logs.
                      </td>
                    </tr>
                  ) : (
                    filteredLogs.map((entry) => (
                      <tr key={entry.id} className="hover:bg-slate-900/40">
                        <td className="whitespace-nowrap px-3 py-2 text-slate-200">{toLocalTimestamp(entry.timestamp)}</td>
                        <td className="whitespace-nowrap px-3 py-2">{renderStatusBadge(entry.status)}</td>
                        <td className="max-w-[22rem] truncate px-3 py-2 font-mono text-xs text-slate-200" title={entry.model}>
                          {entry.model}
                        </td>
                        <td className="px-3 py-2 text-xs text-slate-200">
                          <div className="font-mono">{entry.providerId}</div>
                          <div className="font-mono text-slate-500">{entry.accountId}</div>
                        </td>
                        <td className="whitespace-nowrap px-3 py-2 text-right text-slate-200">{formatCount(entry.latencyMs)} ms</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right text-slate-200">{formatCount(entry.totalTokens ?? 0)}</td>
                        <td className="whitespace-nowrap px-3 py-2 text-right text-slate-200">{formatUsd(entry.costUsd ?? 0)}</td>
                        <td className="max-w-[24rem] px-3 py-2 text-xs text-slate-300">
                          {entry.error || entry.upstreamErrorMessage || entry.upstreamErrorCode ? (
                            <div className={classNames("truncate", entry.status >= 400 ? "text-rose-200" : "text-slate-300")}>
                              {entry.error || entry.upstreamErrorMessage || entry.upstreamErrorCode}
                            </div>
                          ) : (
                            <span className="text-slate-600">—</span>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {topProviderModels.length > 0 ? (
            <div className="space-y-2">
              <div>
                <div className="text-sm font-semibold text-slate-100">Top provider/model pairs</div>
                <div className="text-xs text-slate-500">From Proxx analytics (top 20 by request count).</div>
              </div>
              <div className="overflow-auto rounded-xl border border-slate-800">
                <table className="min-w-full divide-y divide-slate-800 text-sm">
                  <thead className="bg-slate-950/70 text-xs uppercase tracking-wide text-slate-400">
                    <tr>
                      <th className="px-3 py-2 text-left">Provider</th>
                      <th className="px-3 py-2 text-left">Model</th>
                      <th className="px-3 py-2 text-right">Requests</th>
                      <th className="px-3 py-2 text-right">Error rate</th>
                      <th className="px-3 py-2 text-right">Tokens</th>
                      <th className="px-3 py-2 text-right">Cost</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800 bg-slate-950/40">
                    {topProviderModels.map((row) => (
                      <tr key={`${row.providerId ?? ""}:${row.model ?? ""}`} className="hover:bg-slate-900/40">
                        <td className="px-3 py-2 font-mono text-xs text-slate-200">{row.providerId ?? "—"}</td>
                        <td className="px-3 py-2 font-mono text-xs text-slate-200">{row.model ?? "—"}</td>
                        <td className="px-3 py-2 text-right text-slate-200">{formatCount(row.requestCount)}</td>
                        <td className="px-3 py-2 text-right text-slate-200">{formatPercent(row.errorRate)}</td>
                        <td className="px-3 py-2 text-right text-slate-200">{formatCount(row.totalTokens)}</td>
                        <td className="px-3 py-2 text-right text-slate-200">{formatUsd(row.costUsd)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ) : null}
        </div>
      ) : null}
    </SectionCard>
  );
}
