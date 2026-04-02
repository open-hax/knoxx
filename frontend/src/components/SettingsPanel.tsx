import { useState } from "react";
import type { SamplingSettings } from "../lib/types";

export const QUICK_PRESETS: Record<string, SamplingSettings> = {
  "Deterministic/Eval": {
    temperature: 0,
    top_p: 1,
    top_k: 1,
    min_p: 0,
    repeat_penalty: 1,
    presence_penalty: 0,
    frequency_penalty: 0,
    seed: 42,
    max_tokens: 256,
    stop_sequences: []
  },
  Balanced: {
    temperature: 0.6,
    top_p: 0.9,
    top_k: 40,
    min_p: 0.05,
    repeat_penalty: 1.05,
    presence_penalty: 0,
    frequency_penalty: 0,
    seed: null,
    max_tokens: 512,
    stop_sequences: []
  },
  Creative: {
    temperature: 1,
    top_p: 0.95,
    top_k: 80,
    min_p: 0,
    repeat_penalty: 1,
    presence_penalty: 0,
    frequency_penalty: 0,
    seed: null,
    max_tokens: 700,
    stop_sequences: []
  },
  "RAG-friendly": {
    temperature: 0.2,
    top_p: 0.85,
    top_k: 30,
    min_p: 0.05,
    repeat_penalty: 1.12,
    presence_penalty: 0,
    frequency_penalty: 0,
    seed: null,
    max_tokens: 400,
    stop_sequences: ["\n\nSources:"]
  }
};

interface SettingsPanelProps {
  systemPrompt: string;
  onSystemPromptChange: (value: string) => void;
  settings: SamplingSettings;
  onChange: (settings: SamplingSettings) => void;
  reasoningEnabled: boolean;
  reasoningBudget: number;
  onReasoningEnabledChange: (v: boolean) => void;
  onReasoningBudgetChange: (v: number) => void;
  flashAttention: boolean;
  mmap: boolean;
  mlock: boolean;
  ubatchSize: number;
  serverPort: number;
  extraArgs: string;
  onFlashAttentionChange: (v: boolean) => void;
  onMmapChange: (v: boolean) => void;
  onMlockChange: (v: boolean) => void;
  onUbatchSizeChange: (v: number) => void;
  onServerPortChange: (v: number) => void;
  onExtraArgsChange: (v: string) => void;
  promptHistory: string[];
  onSavePromptVersion: () => void;
  onLoadPromptVersion: (prompt: string) => void;
  customPresets: string[];
  onSavePreset: (name: string) => void;
  onLoadPreset: (name: string) => void;
  onDeletePreset: (name: string) => void;
}

function SettingsPanel({
  systemPrompt,
  onSystemPromptChange,
  settings,
  onChange,
  reasoningEnabled,
  reasoningBudget,
  onReasoningEnabledChange,
  onReasoningBudgetChange,
  flashAttention,
  mmap,
  mlock,
  ubatchSize,
  serverPort,
  extraArgs,
  onFlashAttentionChange,
  onMmapChange,
  onMlockChange,
  onUbatchSizeChange,
  onServerPortChange,
  onExtraArgsChange,
  promptHistory,
  onSavePromptVersion,
  onLoadPromptVersion,
  customPresets,
  onSavePreset,
  onLoadPreset,
  onDeletePreset
}: SettingsPanelProps) {
  const unsupported = getUnsupportedWarnings(settings);
  const [presetName, setPresetName] = useState("");

  return (
    <section className="panel h-full min-h-0 overflow-auto space-y-4">
      <div>
        <h2 className="panel-title">Settings</h2>
        <p className="mt-1 text-xs text-slate-500">
          Sampling + system prompt apply on next message. Runtime controls apply on start/restart.
        </p>
      </div>

      <div>
        <LabelWithTooltip
          label="Quick Presets"
          tip="Built-in sampler presets for common testing modes."
        />
        <div className="grid grid-cols-2 gap-2">
          {Object.entries(QUICK_PRESETS).map(([name, preset]) => (
            <button key={name} className="btn-ghost" onClick={() => onChange({ ...preset })}>
              {name}
            </button>
          ))}
        </div>
      </div>

      <div className="rounded-md border border-slate-200 bg-slate-50 p-2">
        <LabelWithTooltip
          label="System Prompt"
          tip="Instruction injected before user messages. Affects model behavior and style for subsequent requests."
        />
        <textarea
          rows={6}
          className="input"
          value={systemPrompt}
          onChange={(event) => onSystemPromptChange(event.target.value)}
          placeholder="You are a careful assistant..."
        />
        <div className="mt-2 flex flex-wrap gap-2">
          <button className="btn-ghost" onClick={onSavePromptVersion}>Save Prompt Version</button>
        </div>
        {promptHistory.length > 0 ? (
          <div className="mt-2 space-y-1">
            <p className="text-xs text-slate-500">Prompt history</p>
            <div className="max-h-28 overflow-auto space-y-1">
              {promptHistory.map((prompt, idx) => (
                <button
                  key={`${idx}-${prompt.slice(0, 20)}`}
                  className="w-full rounded border border-slate-200 bg-white px-2 py-1 text-left text-xs text-slate-700 hover:bg-slate-50"
                  onClick={() => onLoadPromptVersion(prompt)}
                  title={prompt}
                >
                  {prompt.slice(0, 80)}{prompt.length > 80 ? "..." : ""}
                </button>
              ))}
            </div>
          </div>
        ) : null}
      </div>

      <div className="rounded-md border border-slate-200 bg-slate-50 p-2">
        <LabelWithTooltip
          label="Custom Presets"
          tip="Save/load your own sampler + runtime combinations for repeatable experiments."
        />
        <div className="flex gap-2">
          <input
            className="input"
            value={presetName}
            onChange={(e) => setPresetName(e.target.value)}
            placeholder="Preset name"
          />
          <button
            className="btn-ghost"
            onClick={() => {
              onSavePreset(presetName);
              setPresetName("");
            }}
          >
            Save
          </button>
        </div>
        {customPresets.length > 0 ? (
          <div className="mt-2 max-h-28 space-y-1 overflow-auto">
            {customPresets.map((name) => (
              <div key={name} className="flex items-center gap-2 rounded border border-slate-200 bg-white px-2 py-1">
                <span className="flex-1 truncate text-xs text-slate-700">{name}</span>
                <button className="btn-ghost" onClick={() => onLoadPreset(name)}>Load</button>
                <button className="btn-ghost" onClick={() => onDeletePreset(name)}>Delete</button>
              </div>
            ))}
          </div>
        ) : null}
      </div>

      <details className="rounded-md border border-slate-200 bg-slate-50 p-2" open>
        <summary className="cursor-pointer text-sm font-semibold text-slate-700">Sampling Controls</summary>
        <div className="mt-3 space-y-3">
          <SettingNumber label="Temperature" tip="Higher = more creative/random; lower = more deterministic." step={0.05} min={0} max={2} value={settings.temperature} onValue={(value) => onChange({ ...settings, temperature: value })} />
          <SettingNumber label="Top P" tip="Nucleus sampling cutoff. Lower values reduce diversity." step={0.01} min={0} max={1} value={settings.top_p} onValue={(value) => onChange({ ...settings, top_p: value })} />
          <SettingNumber label="Top K" tip="Limits token choices to top-K candidates each step." step={1} min={0} max={500} value={settings.top_k} onValue={(value) => onChange({ ...settings, top_k: value })} />
          <SettingNumber label="Min P" tip="Drops tokens below a minimum probability threshold." step={0.01} min={0} max={1} value={settings.min_p} onValue={(value) => onChange({ ...settings, min_p: value })} />
          <SettingNumber label="Repeat Penalty" tip="Penalizes repeated tokens/phrases. >1 reduces repetition." step={0.01} min={0.5} max={2} value={settings.repeat_penalty} onValue={(value) => onChange({ ...settings, repeat_penalty: value })} />
          <SettingNumber label="Presence Penalty" tip="Encourages introducing new topics; discourages repeated concepts." step={0.05} min={-2} max={2} value={settings.presence_penalty} onValue={(value) => onChange({ ...settings, presence_penalty: value })} />
          <SettingNumber label="Frequency Penalty" tip="Penalizes token frequency to reduce verbosity/repetition." step={0.05} min={-2} max={2} value={settings.frequency_penalty} onValue={(value) => onChange({ ...settings, frequency_penalty: value })} />
          <SettingNumber label="Seed" tip="Fixed seed = reproducible runs. -1/null = random seed." step={1} min={-1} max={999999} value={settings.seed ?? -1} onValue={(value) => onChange({ ...settings, seed: value < 0 ? null : value })} />
          <SettingNumber label="Max Tokens" tip="Maximum generated output length per request." step={1} min={1} max={4096} value={settings.max_tokens} onValue={(value) => onChange({ ...settings, max_tokens: value })} />

          <div>
            <LabelWithTooltip
              label="Stop Sequences"
              tip="Generation stops when one of these strings appears."
            />
            <input
              className="input"
              value={settings.stop_sequences.join(", ")}
              onChange={(event) =>
                onChange({
                  ...settings,
                  stop_sequences: event.target.value
                    .split(",")
                    .map((v) => v.trim())
                    .filter(Boolean)
                })
              }
              placeholder="###, END"
            />
          </div>
        </div>
      </details>

      <details className="rounded-md border border-slate-200 bg-slate-50 p-2">
        <summary className="cursor-pointer text-sm font-semibold text-slate-700">Advanced Lab Controls</summary>
        <div className="mt-3 space-y-3">
          <div className="rounded-md border border-slate-200 bg-white p-3">
            <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-500">Reasoning</p>
            <label className="mb-2 flex items-center gap-2 text-sm text-slate-700">
              <input type="checkbox" checked={reasoningEnabled} onChange={(e) => onReasoningEnabledChange(e.target.checked)} />
              <span title="Enable chain-of-thought style reasoning output for models that support it.">Enable reasoning</span>
            </label>
            <SettingNumber
              label="Reasoning Budget"
              tip="-1 = unlimited; 0 = disabled. Controls thinking token budget where supported."
              step={1}
              min={-1}
              max={1024}
              value={reasoningBudget}
              onValue={onReasoningBudgetChange}
            />
          </div>

          <div className="rounded-md border border-slate-200 bg-white p-3 space-y-2">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Server Runtime (Start/Restart)</p>
            <label className="flex items-center gap-2 text-sm text-slate-700" title="Enables Flash Attention kernels for faster attention on compatible GPUs.">
              <input type="checkbox" checked={flashAttention} onChange={(e) => onFlashAttentionChange(e.target.checked)} /> Flash attention
            </label>
            <label className="flex items-center gap-2 text-sm text-slate-700" title="Memory-map model file from disk to speed startup and reduce RAM pressure.">
              <input type="checkbox" checked={mmap} onChange={(e) => onMmapChange(e.target.checked)} /> mmap
            </label>
            <label className="flex items-center gap-2 text-sm text-slate-700" title="Lock model memory pages to avoid swapping; may require privileges.">
              <input type="checkbox" checked={mlock} onChange={(e) => onMlockChange(e.target.checked)} /> mlock
            </label>
            <div>
              <LabelWithTooltip label="Server Port" tip="Port where llama-server listens (backend proxies to this)." />
              <input className="input" type="number" value={serverPort} onChange={(e) => onServerPortChange(Number(e.target.value))} />
            </div>
            <div>
              <LabelWithTooltip label="uBatch Size" tip="Micro-batch size. Lower values reduce VRAM spikes; higher may improve throughput." />
              <input className="input" type="number" value={ubatchSize} onChange={(e) => onUbatchSizeChange(Number(e.target.value))} />
            </div>
            <div>
              <LabelWithTooltip label="Extra llama-server args" tip="Pass additional raw CLI args for advanced tuning." />
              <input className="input" value={extraArgs} onChange={(e) => onExtraArgsChange(e.target.value)} placeholder="--cache-ram 0 --some-flag value" />
            </div>
          </div>
        </div>
      </details>

      {unsupported.length > 0 ? (
        <p className="text-xs text-slate-500">
          Unsupported on this model: {unsupported.join(", ")}. Values may be ignored by backend.
        </p>
      ) : null}
    </section>
  );
}

interface SettingNumberProps {
  label: string;
  tip: string;
  min: number;
  max: number;
  step: number;
  value: number;
  onValue: (value: number) => void;
}

function SettingNumber({ label, tip, min, max, step, value, onValue }: SettingNumberProps) {
  return (
    <div>
      <LabelWithTooltip label={label} tip={tip} />
      <div className="flex items-center gap-2">
        <input
          className="w-full"
          type="range"
          min={min}
          max={max}
          step={step}
          value={value}
          onChange={(event) => onValue(Number(event.target.value))}
        />
        <input
          className="input w-24"
          type="number"
          min={min}
          max={max}
          step={step}
          value={value}
          onChange={(event) => onValue(Number(event.target.value))}
        />
      </div>
    </div>
  );
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

function getUnsupportedWarnings(settings: SamplingSettings): string[] {
  const warnings: string[] = [];
  if (settings.top_k !== 40) warnings.push("top_k support depends on llama-server version");
  if (settings.min_p > 0) warnings.push("min_p may be ignored on older builds");
  return warnings;
}

export default SettingsPanel;
