import type { ModelInfo } from "../lib/types";

interface ModelSelectorProps {
  models: ModelInfo[];
  value: string;
  onChange: (modelPath: string) => void;
  serverRunning: boolean;
  onStart: () => void;
  onStop: () => void;
  onWarmup: () => void;
  ctxSize: number;
  onCtxSizeChange: (value: number) => void;
  gpuLayers: number;
  onGpuLayersChange: (value: number) => void;
  threads: number;
  onThreadsChange: (value: number) => void;
  batchSize: number;
  onBatchSizeChange: (value: number) => void;
  contextUsedTokens: number;
  contextFillPercent: number;
}

function ModelSelector(props: ModelSelectorProps) {
  const {
    models,
    value,
    onChange,
    serverRunning,
    onStart,
    onStop,
    onWarmup,
    ctxSize,
    onCtxSizeChange,
    gpuLayers,
    onGpuLayersChange,
    threads,
    onThreadsChange,
    batchSize,
    onBatchSizeChange,
    contextUsedTokens,
    contextFillPercent
  } = props;

  return (
    <section className="panel grid grid-cols-1 gap-3 md:grid-cols-6">
      <div className="md:col-span-2">
        <LabelWithTooltip label="Model" tip="GGUF file to load in llama-server." />
        <select className="input" value={value} onChange={(event) => onChange(event.target.value)}>
          {models.length === 0 ? <option value="">No models available</option> : null}
          {models.map((model) => (
            <option
              key={model.path}
              value={model.path}
              style={{ color: modelColor(model.name) }}
              title={modelTierLabel(model.name)}
            >
              {modelTierIcon(model.name)} {model.name}
            </option>
          ))}
        </select>
      </div>

      <div>
        <LabelWithTooltip label="Ctx" tip="Context window token length. Larger = more memory and latency." />
        <input className="input" type="number" value={ctxSize} onChange={(e) => onCtxSizeChange(Number(e.target.value))} />
      </div>
      <div>
        <LabelWithTooltip label="GPU Layers" tip="How many layers to offload to GPU. Higher = faster if VRAM allows." />
        <input className="input" type="number" value={gpuLayers} onChange={(e) => onGpuLayersChange(Number(e.target.value))} />
      </div>
      <div>
        <LabelWithTooltip label="Threads" tip="CPU threads used by llama-server for prompt/eval stages." />
        <input className="input" type="number" value={threads} onChange={(e) => onThreadsChange(Number(e.target.value))} />
      </div>
      <div>
        <LabelWithTooltip label="Batch" tip="Prompt processing batch size. Higher can improve throughput, but increases memory usage." />
        <input className="input" type="number" value={batchSize} onChange={(e) => onBatchSizeChange(Number(e.target.value))} />
      </div>

      <div className="md:col-span-6 flex flex-wrap items-center gap-2 pt-1">
        <button className="btn" onClick={onStart}>Start Server</button>
        <button className="btn-ghost" onClick={onStop}>Stop Server</button>
        <button className="btn-ghost" onClick={onWarmup}>Warmup</button>
        <span className={`text-xs ${serverRunning ? "text-emerald-600" : "text-slate-500"}`}>
          {serverRunning ? "inference running" : "inference stopped"}
        </span>
        <div className="min-w-[260px] flex-1 rounded-md border border-slate-200 bg-slate-50 px-2 py-1">
          <div className="relative h-8 w-full overflow-hidden rounded-md bg-slate-200">
            <div
              className={`h-full ${
                contextFillPercent < 70 ? "bg-emerald-500" : contextFillPercent < 90 ? "bg-amber-500" : "bg-rose-500"
              } transition-all`}
              style={{ width: `${Math.max(0, Math.min(100, contextFillPercent))}%` }}
            />
            <div className="absolute inset-0 flex items-center justify-between px-2 text-[11px] font-medium text-slate-800">
              <span>Context: {contextUsedTokens}/{ctxSize}</span>
              <span>{Math.max(0, Math.min(100, contextFillPercent)).toFixed(1)}%</span>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function modelTierIcon(name: string): string {
  const n = name.toLowerCase();
  if (n.includes("35b")) return "🔴";
  if (n.includes("9b")) return "🟠";
  if (n.includes("4b")) return "🟡";
  return "🟢";
}

function modelColor(name: string): string {
  const n = name.toLowerCase();
  if (n.includes("35b")) return "#b91c1c";
  if (n.includes("9b")) return "#c2410c";
  if (n.includes("4b")) return "#a16207";
  return "#166534";
}

function modelTierLabel(name: string): string {
  const n = name.toLowerCase();
  if (n.includes("35b")) return "High resource: 35B tier";
  if (n.includes("9b")) return "Moderate resource: 9B tier";
  if (n.includes("4b")) return "Balanced resource: 4B tier";
  return "Lower resource: 2B and below tier";
}

function LabelWithTooltip({ label, tip }: { label: string; tip: string }) {
  return (
    <div className="mb-1 flex items-center gap-1">
      <label className="field-label mb-0">{label}</label>
      <span className="group relative inline-flex h-4 w-4 cursor-help items-center justify-center rounded-full border border-slate-300 text-[10px] text-slate-600">
        i
        <span className="pointer-events-none absolute left-5 top-1 z-20 hidden w-64 rounded-md bg-slate-900 px-2 py-1 text-[11px] text-slate-100 shadow-lg group-hover:block">
          {tip}
        </span>
      </span>
    </div>
  );
}

export default ModelSelector;
