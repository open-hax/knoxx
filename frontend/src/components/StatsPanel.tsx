interface StatsPanelProps {
  stats: Record<string, unknown>;
  status: string;
  history: {
    cpu: number[];
    ram: number[];
    gpu: number[];
    rssGb: number[];
  };
  contextTokens: number;
  kvEstimateGb: string;
}

function fmtBytes(v?: unknown): string {
  if (typeof v !== "number") return "-";
  const gb = v / (1024 ** 3);
  return `${gb.toFixed(2)} GB`;
}

function sparkPath(values: number[], width = 180, height = 36, max = 100): string {
  if (values.length === 0) return "";
  const step = values.length > 1 ? width / (values.length - 1) : width;
  return values
    .map((v, i) => {
      const x = i * step;
      const y = height - (Math.max(0, Math.min(v, max)) / max) * height;
      return `${i === 0 ? "M" : "L"}${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(" ");
}

function Sparkline({ values, color, max = 100 }: { values: number[]; color: string; max?: number }) {
  const path = sparkPath(values, 180, 36, max);
  return (
    <svg viewBox="0 0 180 36" className="h-10 w-full rounded bg-slate-50">
      {path ? <path d={path} fill="none" stroke={color} strokeWidth="2" /> : null}
    </svg>
  );
}

function StatsPanel({ stats, status, history, contextTokens, kvEstimateGb }: StatsPanelProps) {
  const gpu = Array.isArray(stats.gpu) ? stats.gpu : [];
  const llama = (stats.llama as Record<string, unknown> | undefined) ?? {};
  const multi = (stats.multi_user as Record<string, unknown> | undefined) ?? {};

  return (
    <section className="panel h-full min-h-0 overflow-auto">
      <div className="mb-3 flex items-center justify-between">
        <h2 className="panel-title">Live Stats</h2>
        <span className="text-xs text-slate-500">WS: {status}</span>
      </div>

      <div className="space-y-2 text-sm">
        <StatRow label="CPU" value={`${Number(stats.cpu_percent ?? 0).toFixed(1)} %`} />
        <StatRow label="RAM" value={`${Number(stats.memory_percent ?? 0).toFixed(1)} %`} />
        <StatRow label="System Used" value={fmtBytes(stats.memory_used_bytes)} />
        <StatRow label="llama RSS" value={fmtBytes(llama.rss_bytes)} />
        <StatRow label="Active Users" value={String(multi.active_clients ?? "1")} />
        <StatRow label="Active Runs" value={String(multi.active_runs ?? "0")} />
      </div>

      <div className="mt-4">
        <p className="mb-1 text-xs uppercase tracking-wide text-slate-500">Recent Changes (60s)</p>
        <div className="space-y-2">
          <MetricGraph label="CPU %" values={history.cpu} color="#0f766e" max={100} />
          <MetricGraph label="RAM %" values={history.ram} color="#2563eb" max={100} />
          <MetricGraph label="GPU %" values={history.gpu} color="#7c3aed" max={100} />
          <MetricGraph label="RSS GB" values={history.rssGb} color="#ea580c" max={Math.max(1, ...history.rssGb, 1)} />
        </div>
      </div>

      <div className="mt-4">
        <p className="mb-1 text-xs uppercase tracking-wide text-slate-500">GPU</p>
        {gpu.length === 0 ? (
          <p className="text-xs text-slate-500">GPU stats unavailable (pynvml missing or no NVIDIA device).</p>
        ) : (
          <div className="space-y-2">
            {gpu.map((d, idx) => {
              const dev = d as Record<string, unknown>;
              return (
                <div key={idx} className="rounded-md border border-slate-200 bg-slate-50 p-2 text-xs">
                  <p className="font-semibold">{String(dev.name ?? `GPU ${idx}`)}</p>
                  <p>Util: {String(dev.util_gpu ?? "-")} %</p>
                  <p>
                    VRAM: {fmtBytes(dev.memory_used_bytes)} / {fmtBytes(dev.memory_total_bytes)}
                  </p>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <div className="mt-4 rounded-md border border-slate-200 bg-slate-50 p-2 text-xs text-slate-700">
        <p><strong>Context:</strong> {contextTokens} tokens</p>
        <p><strong>Estimated KV:</strong> ~{kvEstimateGb} GB</p>
        <p><strong>RAM avail/user:</strong> {fmtBytes(multi.memory_available_per_client_bytes)}</p>
        <p><strong>VRAM avail/user:</strong> {fmtBytes(multi.gpu_memory_available_per_client_bytes)}</p>
        <p className="mt-1 text-slate-500">Server args (ctx/batch/gpu layers/threads) apply on server start. Sampling/system prompt apply on next message.</p>
      </div>
    </section>
  );
}

function StatRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between rounded-md bg-slate-50 px-2 py-1">
      <span className="text-slate-600">{label}</span>
      <span className="font-medium text-slate-900">{value}</span>
    </div>
  );
}

function MetricGraph({ label, values, color, max }: { label: string; values: number[]; color: string; max: number }) {
  const latest = values.length ? values[values.length - 1] : 0;
  return (
    <div>
      <div className="mb-1 flex items-center justify-between text-xs text-slate-600">
        <span>{label}</span>
        <span>{latest.toFixed(2)}</span>
      </div>
      <Sparkline values={values} color={color} max={max} />
    </div>
  );
}

export default StatsPanel;
