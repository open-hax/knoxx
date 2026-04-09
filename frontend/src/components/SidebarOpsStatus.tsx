import { useEffect, useMemo, useState } from 'react';
import { fetchIngestionProgress } from '../lib/nextApi';
import { connectStream } from '../lib/ws';

interface StatSample {
  t: number;
  cpu: number;
  ram: number;
  gpu: number;
}

function sparklinePath(values: number[], width: number, height: number): string {
  if (values.length === 0) return '';
  const max = Math.max(1, ...values);
  const min = Math.min(0, ...values);
  const range = Math.max(1, max - min);
  return values
    .map((v, i) => {
      const x = (i / Math.max(1, values.length - 1)) * width;
      const y = height - ((v - min) / range) * height;
      return `${i === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`;
    })
    .join(' ');
}

export default function SidebarOpsStatus() {
  const [samples, setSamples] = useState<StatSample[]>([]);
  const [ingestion, setIngestion] = useState<any>(null);

  useEffect(() => {
    const disconnect = connectStream({
      onStats: (payload) => {
        const p = payload as any;
        const gpu = Array.isArray(p.gpu) && p.gpu.length > 0 ? Number(p.gpu[0]?.util_gpu || 0) : 0;
        const next: StatSample = {
          t: Date.now(),
          cpu: Number(p.cpu_percent || 0),
          ram: Number(p.memory_percent || 0),
          gpu,
        };
        setSamples((prev) => [...prev, next].slice(-50));
      },
    });
    return disconnect;
  }, []);

  useEffect(() => {
    const poll = async () => {
      try {
        const data = await fetchIngestionProgress();
        setIngestion(data);
      } catch {
        setIngestion(null);
      }
    };
    void poll();
    const timer = window.setInterval(() => {
      void poll();
    }, 2500);
    return () => window.clearInterval(timer);
  }, []);

  const latest = samples[samples.length - 1];

  const cpuPath = useMemo(() => sparklinePath(samples.map((s) => s.cpu), 220, 56), [samples]);
  const ramPath = useMemo(() => sparklinePath(samples.map((s) => s.ram), 220, 56), [samples]);
  const gpuPath = useMemo(() => sparklinePath(samples.map((s) => s.gpu), 220, 56), [samples]);

  return (
    <div className="border-t border-slate-200 dark:border-slate-700/60 p-3 space-y-3 overflow-y-auto">
      <div>
        <p className="text-[11px] uppercase tracking-wide text-slate-500 dark:text-slate-400">System Usage</p>
        <div className="mt-2 space-y-1 text-xs text-slate-300">
          <MetricRow label="CPU" value={`${(latest?.cpu ?? 0).toFixed(1)}%`} color="text-cyan-300" />
          <MetricRow label="RAM" value={`${(latest?.ram ?? 0).toFixed(1)}%`} color="text-amber-300" />
          <MetricRow label="GPU" value={`${(latest?.gpu ?? 0).toFixed(1)}%`} color="text-violet-300" />
        </div>
        <svg viewBox="0 0 220 56" className="mt-2 h-16 w-full rounded bg-slate-900/70">
          <path d={cpuPath} fill="none" stroke="var(--token-colors-accent-blue)" strokeWidth="2" />
          <path d={ramPath} fill="none" stroke="var(--token-colors-accent-orange)" strokeWidth="1.8" />
          <path d={gpuPath} fill="none" stroke="var(--token-colors-accent-magenta)" strokeWidth="1.8" />
        </svg>
      </div>

      <div>
        <p className="text-[11px] uppercase tracking-wide text-slate-500 dark:text-slate-400">Ingestion</p>
        {ingestion?.active && ingestion?.progress ? (
          <>
            <p className="mt-1 text-xs text-slate-200">
              {ingestion.progress.processedChunks} / {ingestion.progress.totalChunks} ({Number(ingestion.progress.percentPrecise ?? ingestion.progress.percent ?? 0).toFixed(2)}%)
            </p>
            <div className="mt-1 h-1.5 w-full rounded bg-slate-800">
              <div
                className="h-1.5 rounded bg-cyan-400"
                style={{ width: `${Math.max(0, Math.min(100, Number(ingestion.progress.percentPrecise ?? ingestion.progress.percent ?? 0)))}%` }}
              />
            </div>
            <p className="mt-1 truncate text-[11px] text-slate-400">{ingestion.progress.currentFile || 'Working...'}</p>
          </>
        ) : ingestion?.canResumeForum && ingestion?.progress ? (
          <p className="mt-1 text-xs text-amber-300">Paused at {Number(ingestion.progress.percentPrecise ?? ingestion.progress.percent ?? 0).toFixed(2)}% (resumable)</p>
        ) : (
          <p className="mt-1 text-xs text-slate-400">No active ingestion</p>
        )}
      </div>
    </div>
  );
}

function MetricRow({ label, value, color }: { label: string; value: string; color: string }) {
  return (
    <div className="flex items-center justify-between rounded border border-slate-800 bg-slate-900/60 px-2 py-1">
      <span className="text-slate-400">{label}</span>
      <span className={color}>{value}</span>
    </div>
  );
}
