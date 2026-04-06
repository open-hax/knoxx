import React, { useEffect, useMemo, useState } from 'react';
import {
  Activity,
  AlertCircle,
  CheckCircle2,
  Clock3,
  Cpu,
  Database,
  HardDrive,
  Layers,
  Link as LinkIcon,
  RefreshCw,
  Server,
  Settings2,
  XCircle,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { fetchIngestionProgress, knoxxHealth } from '../lib/nextApi';
import { connectStream } from '../lib/ws';
import { Line } from 'react-chartjs-2';
import {
  CategoryScale,
  Chart as ChartJS,
  type ChartOptions,
  Filler,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler);

type AnyObj = Record<string, any>;

interface SystemSample {
  t: string;
  cpu: number;
  ram: number;
  gpu: number;
  net: number;
}

function formatBytes(input?: number): string {
  if (!input || Number.isNaN(input)) return 'N/A';
  const units = ['B', 'KB', 'MB', 'GB'];
  let value = input;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit += 1;
  }
  return `${value.toFixed(unit === 0 ? 0 : 1)} ${units[unit]}`;
}

function formatUptime(seconds?: number): string {
  if (!seconds || Number.isNaN(seconds)) return 'N/A';
  const s = Math.floor(seconds);
  const d = Math.floor(s / 86400);
  const h = Math.floor((s % 86400) / 3600);
  const m = Math.floor((s % 3600) / 60);
  if (d > 0) return `${d}d ${h}h`;
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

function formatRate(bytesPerSec?: number): string {
  if (!bytesPerSec || Number.isNaN(bytesPerSec)) return '0 B/s';
  const kb = bytesPerSec / 1024;
  if (kb < 1024) return `${kb.toFixed(1)} KB/s`;
  return `${(kb / 1024).toFixed(2)} MB/s`;
}

function normalizeHealth(payload: AnyObj | null): AnyObj {
  if (!payload) return {};
  const details = payload.details ?? payload;
  const services = details.services ?? {};
  const collection = details.collection ?? {};
  const config = details.config ?? {};
  const memory = details.memory ?? {};

  const qdrantOk = services.qdrant === 'ok' || details?.qdrant?.status === 'ok';
  const apiOk = services.api === 'ok' || payload.reachable === true;

  return {
    baseUrl: payload.base_url ?? 'N/A',
    configured: payload.configured !== false,
    reachable: payload.reachable ?? (details.status === 'healthy' || apiOk),
    timestamp: details.timestamp,
    status: details.status,
    project: details.project,
    apiOk,
    qdrantOk,
    pointsCount: collection.pointsCount ?? details?.qdrant?.details?.collections?.documents?.points_count ?? 0,
    indexedVectorsCount: collection.indexedVectorsCount ?? 0,
    vectorDim: config.vectorDim,
    retrievalTopK: config.retrievalTopK,
    retrievalMode: config.retrievalMode,
    fusion: config.hybridFusion,
    chunkTargetTokens: config.chunkTargetTokens,
    uptime: details.uptime,
    rss: memory.rss,
    heapUsed: memory.heapUsed,
    heapTotal: memory.heapTotal,
    raw: details,
  };
}

export default function DashboardPage() {
  const [health, setHealth] = useState<AnyObj | null>(null);
  const [ingestion, setIngestion] = useState<AnyObj | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>('');
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [autoRefreshSec, setAutoRefreshSec] = useState(15);
  const [paused, setPaused] = useState(false);
  const [showRaw, setShowRaw] = useState(false);
  const [currentSystem, setCurrentSystem] = useState<AnyObj | null>(null);
  const [systemSeries, setSystemSeries] = useState<SystemSample[]>([]);

  const load = async () => {
    try {
      const [data, ingest] = await Promise.all([knoxxHealth(), fetchIngestionProgress().catch(() => null)]);
      setHealth(data);
      setIngestion(ingest);
      setError('');
      setLastUpdated(new Date());
    } catch (err) {
      console.error(err);
      setError('Unable to fetch health telemetry from Knoxx.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
    if (paused) return;
    const timer = window.setInterval(() => {
      void load();
    }, autoRefreshSec * 1000);
    return () => window.clearInterval(timer);
  }, [autoRefreshSec, paused]);

  useEffect(() => {
    const disconnect = connectStream({
      onStats: (payload) => {
        const p = payload as AnyObj;
        setCurrentSystem(p);
        const gpuList = Array.isArray(p.gpu) ? p.gpu : [];
        const gpu = gpuList.length > 0 ? Number(gpuList[0]?.util_gpu || 0) : 0;
        const netBps = Number(p?.network?.total_bytes_per_sec || 0);
        const stamp = typeof p.timestamp === 'string' ? new Date(p.timestamp) : new Date();
        const item: SystemSample = {
          t: stamp.toLocaleTimeString(),
          cpu: Number(p.cpu_percent || 0),
          ram: Number(p.memory_percent || 0),
          gpu,
          net: netBps / (1024 * 1024),
        };
        setSystemSeries((prev) => [...prev.slice(-89), item]);
      },
    });
    return disconnect;
  }, []);

  const data = useMemo(() => normalizeHealth(health), [health]);
  const systemOnline = Boolean(data.reachable);
  const memoryPressure = data.heapTotal ? (Number(data.heapUsed || 0) / Number(data.heapTotal)) * 100 : 0;
  const vectorGap = Math.max(0, Number(data.pointsCount || 0) - Number(data.indexedVectorsCount || 0));
  const ingestionProgress = ingestion?.progress;
  const gpuList = Array.isArray(currentSystem?.gpu) ? currentSystem.gpu : [];

  const systemChartData = useMemo(
    () => ({
      labels: systemSeries.map((s) => s.t),
      datasets: [
        { label: 'CPU %', data: systemSeries.map((s) => s.cpu), borderColor: 'var(--token-colors-accent-blue)', backgroundColor: 'var(--token-colors-alpha-blue-_12)', fill: true, tension: 0.3, pointRadius: 0 },
        { label: 'RAM %', data: systemSeries.map((s) => s.ram), borderColor: 'var(--token-colors-accent-orange)', backgroundColor: 'var(--token-colors-alpha-orange-_12)', fill: true, tension: 0.3, pointRadius: 0 },
        { label: 'GPU %', data: systemSeries.map((s) => s.gpu), borderColor: 'var(--token-colors-accent-magenta)', backgroundColor: 'var(--token-colors-alpha-magenta-_14)', fill: true, tension: 0.3, pointRadius: 0 },
      ],
    }),
    [systemSeries]
  );

  const netChartData = useMemo(
    () => ({
      labels: systemSeries.map((s) => s.t),
      datasets: [
        { label: 'Network MB/s', data: systemSeries.map((s) => s.net), borderColor: 'var(--token-colors-accent-green)', backgroundColor: 'var(--token-colors-alpha-green-_14)', fill: true, tension: 0.3, pointRadius: 0 },
      ],
    }),
    [systemSeries]
  );

  const chartOptions = useMemo<ChartOptions<'line'>>(
    () => ({
      responsive: true,
      maintainAspectRatio: false,
      animation: false,
      plugins: { legend: { labels: { color: 'var(--token-colors-text-muted)', boxWidth: 10, boxHeight: 10 } } },
      scales: {
        x: { ticks: { color: 'var(--token-colors-text-muted)', maxTicksLimit: 6 }, grid: { color: 'var(--token-colors-alpha-bg-_25)' } },
        y: { ticks: { color: 'var(--token-colors-text-muted)' }, grid: { color: 'var(--token-colors-alpha-bg-_25)' } },
      },
    }),
    []
  );

  return (
    <div className="mx-auto w-full max-w-7xl p-6 md:p-8">
      <header className="mb-8 flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="flex items-center gap-3 text-3xl font-bold text-slate-900 dark:text-white">
            <Activity className="h-8 w-8 text-cyan-400" />
            System Overview
          </h1>
          <p className="mt-2 text-slate-400">Monitor your Knoxx stack, retrieval config, and runtime health.</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => void load()}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-800 px-3 py-2 text-sm text-slate-200 hover:bg-slate-700"
          >
            <RefreshCw className="h-4 w-4" />
            Refresh
          </button>
          <StatusPill ok={systemOnline} text={loading ? 'Checking...' : systemOnline ? 'System Online' : 'System Offline'} />
        </div>
      </header>

      <section className="mb-6 rounded-2xl border border-slate-800 bg-slate-900/80 p-4 shadow-xl">
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => void load()}
            className="rounded-md border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs text-slate-200 hover:bg-slate-700"
          >
            Refresh Now
          </button>
          <button
            onClick={() => setPaused((v) => !v)}
            className="rounded-md border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs text-slate-200 hover:bg-slate-700"
          >
            {paused ? 'Resume Auto-Refresh' : 'Pause Auto-Refresh'}
          </button>
          <label className="ml-1 flex items-center gap-2 text-xs text-slate-300">
            Interval
            <select
              value={autoRefreshSec}
              onChange={(e) => setAutoRefreshSec(Number(e.target.value))}
              className="rounded border border-slate-700 bg-slate-800 px-2 py-1 text-xs"
            >
              <option value={5}>5s</option>
              <option value={10}>10s</option>
              <option value={15}>15s</option>
              <option value={30}>30s</option>
            </select>
          </label>
          <Link to="/next/documents" className="rounded-md border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs text-slate-200 hover:bg-slate-700">Open Lakes</Link>
          <Link to="/next/vectors" className="rounded-md border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs text-slate-200 hover:bg-slate-700">Open Graph</Link>
          <Link to="/" className="rounded-md border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs text-slate-200 hover:bg-slate-700">Open Chat</Link>
          <button
            onClick={() => setShowRaw((v) => !v)}
            className="rounded-md border border-slate-700 bg-slate-800 px-3 py-1.5 text-xs text-slate-200 hover:bg-slate-700"
          >
            {showRaw ? 'Hide Raw JSON' : 'Show Raw JSON'}
          </button>
        </div>
      </section>

      {error ? (
        <div className="mb-6 rounded-xl border border-rose-500/30 bg-rose-500/10 p-4 text-rose-200">{error}</div>
      ) : null}

      {!data.configured && !loading ? (
        <div className="mb-6 flex items-start gap-3 rounded-xl border border-amber-400/30 bg-amber-400/10 p-4">
          <AlertCircle className="mt-0.5 h-5 w-5 text-amber-300" />
          <div>
            <p className="font-semibold text-amber-200">API key not configured</p>
            <p className="text-sm text-amber-100/80">Set `KNOXX_API_KEY` in Knoxx backend environment.</p>
          </div>
        </div>
      ) : null}

      <div className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard
          title="Connection"
          value={systemOnline ? 'Connected' : 'Disconnected'}
          subtitle={data.baseUrl}
          icon={<LinkIcon className="h-5 w-5 text-cyan-300" />}
          ok={systemOnline}
        />
        <StatCard
          title="Lake Index"
          value={data.qdrantOk ? 'Healthy' : 'Issue Detected'}
          subtitle={`${Number(data.pointsCount || 0).toLocaleString()} points`}
          icon={<Database className="h-5 w-5 text-emerald-300" />}
          ok={Boolean(data.qdrantOk)}
        />
        <StatCard
          title="Indexed Chunks"
          value={Number(data.indexedVectorsCount || 0).toLocaleString()}
          subtitle="active retrieval substrate"
          icon={<Layers className="h-5 w-5 text-violet-300" />}
          ok
        />
        <StatCard
          title="Uptime"
          value={formatUptime(data.uptime)}
          subtitle={data.timestamp ? `Last report: ${new Date(data.timestamp).toLocaleTimeString()}` : 'Last report: N/A'}
          icon={<Clock3 className="h-5 w-5 text-amber-300" />}
          ok
        />
        <StatCard
          title="Heap Pressure"
          value={`${memoryPressure.toFixed(1)}%`}
          subtitle={`${formatBytes(data.heapUsed)} / ${formatBytes(data.heapTotal)}`}
          icon={<HardDrive className="h-5 w-5 text-rose-300" />}
          ok={memoryPressure < 85}
        />
        <StatCard
          title="Lake Index Gap"
          value={vectorGap.toLocaleString()}
          subtitle="points minus indexed chunks"
          icon={<Layers className="h-5 w-5 text-indigo-300" />}
          ok={vectorGap === 0}
        />
        <StatCard
          title="Ingestion"
          value={ingestion?.active ? 'Running' : ingestion?.canResumeForum ? 'Paused' : 'Idle'}
          subtitle={ingestionProgress ? `${ingestionProgress.processedChunks}/${ingestionProgress.totalChunks}` : 'No active pipeline'}
          icon={<Activity className="h-5 w-5 text-cyan-300" />}
          ok={Boolean(ingestion?.active)}
        />
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-3">
        <section className="rounded-2xl border border-slate-800 bg-slate-900/80 p-5 shadow-2xl xl:col-span-3">
          <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-slate-100">
            <Activity className="h-5 w-5 text-cyan-300" />
            Complete System Telemetry
          </h2>
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-4">
            <MetricRow label="CPU Used" value={`${Number(currentSystem?.cpu_percent || 0).toFixed(1)}%`} />
            <MetricRow label="RAM Used" value={`${Number(currentSystem?.memory_percent || 0).toFixed(1)}%`} />
            <MetricRow label="Network RX" value={formatRate(Number(currentSystem?.network?.rx_bytes_per_sec || 0))} />
            <MetricRow label="Network TX" value={formatRate(Number(currentSystem?.network?.tx_bytes_per_sec || 0))} />
          </div>
          <div className="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-2">
            <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-3">
              <p className="mb-2 text-xs uppercase tracking-wide text-slate-400">CPU / RAM / GPU</p>
              <div className="h-44"><Line data={systemChartData} options={chartOptions} /></div>
            </div>
            <div className="rounded-xl border border-slate-800 bg-slate-950/60 p-3">
              <p className="mb-2 text-xs uppercase tracking-wide text-slate-400">Network Throughput</p>
              <div className="h-44"><Line data={netChartData} options={chartOptions} /></div>
            </div>
          </div>
          <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
            <RuntimeRow icon={<HardDrive className="h-4 w-4" />} label="Memory Used" value={formatBytes(Number(currentSystem?.memory_used_bytes || 0))} />
            <RuntimeRow icon={<HardDrive className="h-4 w-4" />} label="Memory Total" value={formatBytes(Number(currentSystem?.memory_total_bytes || 0))} />
            <RuntimeRow icon={<Server className="h-4 w-4" />} label="Active Runs" value={String(Number(currentSystem?.active_runs || 0))} />
            <RuntimeRow icon={<Server className="h-4 w-4" />} label="Active Clients" value={String(Number(currentSystem?.active_clients || 0))} />
            <RuntimeRow icon={<Activity className="h-4 w-4" />} label="CPU Temp (if reported)" value={currentSystem?.cpu_temp_c ? `${Number(currentSystem.cpu_temp_c).toFixed(1)} C` : 'N/A'} />
            <RuntimeRow icon={<Activity className="h-4 w-4" />} label="Stats Timestamp" value={String(currentSystem?.timestamp || 'N/A')} />
          </div>
          {gpuList.length > 0 ? (
            <div className="mt-4 rounded-xl border border-slate-800 bg-slate-950/60 p-3">
              <p className="mb-2 text-xs uppercase tracking-wide text-slate-400">GPU Details</p>
              <div className="space-y-2">
                {gpuList.map((gpu: AnyObj, idx: number) => (
                  <div key={`gpu-${idx}`} className="grid grid-cols-2 gap-2 rounded-lg border border-slate-800 bg-slate-900/70 p-2 text-xs md:grid-cols-6">
                    <span className="text-slate-200">{String(gpu.name || `GPU ${idx}`)}</span>
                    <span className="text-slate-300">util {Number(gpu.util_gpu || 0).toFixed(1)}%</span>
                    <span className="text-slate-300">mem {Number(gpu.util_mem || 0).toFixed(1)}%</span>
                    <span className="text-slate-300">vram {formatBytes(Number(gpu.mem_used_bytes || 0))}</span>
                    <span className="text-slate-300">temp {gpu.temp_c ? `${Number(gpu.temp_c).toFixed(1)} C` : 'N/A'}</span>
                    <span className="text-slate-300">power {gpu.power_w ? `${Number(gpu.power_w).toFixed(1)} W` : 'N/A'}</span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <p className="mt-3 text-xs text-slate-400">No GPU telemetry reported by the backend.</p>
          )}
        </section>

        <section className="rounded-2xl border border-slate-800 bg-slate-900/80 p-5 shadow-2xl xl:col-span-2">
          <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-slate-100">
            <Settings2 className="h-5 w-5 text-cyan-300" />
            Retrieval Coverage
          </h2>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <MetricRow label="Mode" value={data.retrievalMode ?? 'N/A'} />
            <MetricRow label="Top K" value={data.retrievalTopK ?? 'N/A'} />
            <MetricRow label="Fusion" value={data.fusion ?? 'N/A'} />
            <MetricRow label="Embedding Dimension" value={data.vectorDim ?? 'N/A'} />
            <MetricRow label="Chunk Target Tokens" value={data.chunkTargetTokens ?? 'N/A'} />
            <MetricRow label="Project" value={data.project ?? 'N/A'} />
          </div>
        </section>

        <section className="rounded-2xl border border-slate-800 bg-gradient-to-br from-slate-900 via-slate-900 to-cyan-950/40 p-5 shadow-2xl">
          <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-slate-100">
            <Server className="h-5 w-5 text-cyan-300" />
            Runtime
          </h2>
          <div className="space-y-3 text-sm">
            <RuntimeRow icon={<HardDrive className="h-4 w-4" />} label="RSS" value={formatBytes(data.rss)} />
            <RuntimeRow icon={<Cpu className="h-4 w-4" />} label="Heap Used" value={formatBytes(data.heapUsed)} />
            <RuntimeRow icon={<Cpu className="h-4 w-4" />} label="Heap Total" value={formatBytes(data.heapTotal)} />
            <RuntimeRow icon={<Activity className="h-4 w-4" />} label="API Service" value={data.apiOk ? 'ok' : 'down'} />
            <RuntimeRow icon={<Database className="h-4 w-4" />} label="Lake Index Service" value={data.qdrantOk ? 'ok' : 'down'} />
          </div>
          <p className="mt-5 border-t border-slate-700 pt-3 text-xs text-slate-400">
            {lastUpdated ? `Auto-refresh every 15s. Last refreshed ${lastUpdated.toLocaleTimeString()}.` : 'Auto-refresh every 15s.'}
          </p>
        </section>

        {showRaw ? (
        <section className="rounded-2xl border border-slate-800 bg-slate-900/80 p-5 shadow-2xl xl:col-span-3">
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-300">Raw Telemetry</h2>
          <div className="max-h-80 overflow-auto rounded-lg border border-slate-800 bg-slate-950 p-4 font-mono text-xs text-slate-300">
            <pre>{JSON.stringify(data.raw ?? {}, null, 2)}</pre>
          </div>
        </section>
        ) : null}
      </div>
    </div>
  );
}

function StatusPill({ ok, text }: { ok: boolean; text: string }) {
  return (
    <div className="inline-flex items-center gap-2 rounded-full border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-200">
      {ok ? <CheckCircle2 className="h-4 w-4 text-emerald-400" /> : <XCircle className="h-4 w-4 text-rose-400" />}
      {text}
    </div>
  );
}

function StatCard({
  title,
  value,
  subtitle,
  icon,
  ok,
}: {
  title: string;
  value: string;
  subtitle: string;
  icon: React.ReactNode;
  ok: boolean;
}) {
  return (
    <article className="rounded-2xl border border-slate-800 bg-slate-900/85 p-4 shadow-xl">
      <div className="mb-3 flex items-center justify-between">
        <p className="text-xs uppercase tracking-wide text-slate-400">{title}</p>
        <div className="rounded-lg bg-slate-800 p-2">{icon}</div>
      </div>
      <p className="text-2xl font-semibold text-slate-100">{value}</p>
      <p className="mt-1 truncate text-xs text-slate-400">{subtitle}</p>
      <div className="mt-3 h-1.5 rounded-full bg-slate-800">
        <div className={`h-1.5 rounded-full ${ok ? 'w-full bg-emerald-400' : 'w-1/3 bg-rose-400'}`} />
      </div>
    </article>
  );
}

function MetricRow({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-950/70 p-4">
      <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 text-base font-medium text-slate-200">{String(value)}</p>
    </div>
  );
}

function RuntimeRow({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="flex items-center justify-between rounded-lg border border-slate-800 bg-slate-950/50 px-3 py-2">
      <div className="flex items-center gap-2 text-slate-300">
        {icon}
        <span>{label}</span>
      </div>
      <span className="font-mono text-slate-200">{value}</span>
    </div>
  );
}
