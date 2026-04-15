import React, { useCallback, useEffect, useMemo, useState } from "react";

import type { ProxxModelInfo } from "../../lib/types";
import { listProxxModels } from "../../lib/api/runtime";
import { getTranslationPipelineConfig, updateTranslationPipelineConfig } from "../../lib/api/openplanner";
import { SectionCard } from "./common";

type Notice = { tone: "success" | "error"; text: string } | null;

export function TranslationModelSection({ canManage }: { canManage: boolean }) {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<Notice>(null);
  const [error, setError] = useState<string>("");
  const [models, setModels] = useState<ProxxModelInfo[]>([]);
  const [currentModel, setCurrentModel] = useState<string>("");
  const [updatedAt, setUpdatedAt] = useState<string | null>(null);
  const [draftModel, setDraftModel] = useState<string>("");

  const modelOptions = useMemo(
    () => [...new Set(models.map((m) => m.id).filter(Boolean))].sort(),
    [models],
  );

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    setNotice(null);
    try {
      const [config, proxxModels] = await Promise.all([
        getTranslationPipelineConfig(),
        listProxxModels().catch(() => []),
      ]);
      setModels(proxxModels);
      setCurrentModel(config.model);
      setDraftModel(config.model);
      setUpdatedAt(config.updated_at);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const handleSave = useCallback(async () => {
    if (!canManage) return;
    const normalized = draftModel.trim();
    if (!normalized) {
      setError("Model is required");
      return;
    }
    setSaving(true);
    setError("");
    setNotice(null);
    try {
      const updated = await updateTranslationPipelineConfig(normalized);
      setCurrentModel(updated.model);
      setDraftModel(updated.model);
      setUpdatedAt(updated.updated_at);
      setNotice({ tone: "success", text: `Translation model updated to ${updated.model}.` });
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setSaving(false);
    }
  }, [canManage, draftModel]);

  return (
    <SectionCard
      title="Translation pipeline"
      description="Controls the model used by the translation worker when it starts Knoxx translator agent sessions."
    >
      {loading ? (
        <div className="text-sm text-slate-300">Loading translation config…</div>
      ) : (
        <div className="space-y-4">
          <div className="grid gap-3 lg:grid-cols-[1fr_auto] lg:items-end">
            <label className="space-y-1">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Translation model</div>
              <input
                list="translation-model-options"
                value={draftModel}
                onChange={(event) => setDraftModel(event.target.value)}
                disabled={!canManage || saving}
                className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
                placeholder={currentModel || "glm-5"}
              />
              <datalist id="translation-model-options">
                {modelOptions.map((id) => (
                  <option key={`translation-model-${id}`} value={id} />
                ))}
              </datalist>
              <div className="text-xs text-slate-500">
                Current: <span className="text-slate-200">{currentModel || "(unknown)"}</span>
                {updatedAt ? <span className="ml-2">(updated {updatedAt})</span> : null}
              </div>
            </label>

            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => void load()}
                disabled={saving}
                className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
              >
                Refresh
              </button>
              <button
                type="button"
                onClick={() => void handleSave()}
                disabled={!canManage || saving}
                className="inline-flex items-center justify-center rounded-lg bg-sky-600 px-4 py-2 text-sm font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60"
              >
                {saving ? "Saving…" : "Save"}
              </button>
            </div>
          </div>

          {!canManage ? (
            <div className="rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
              You do not have <code className="font-mono">org.translations.manage</code> permission.
            </div>
          ) : null}

          {notice ? (
            <div className={notice.tone === "success"
              ? "rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-sm text-emerald-200"
              : "rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200"}
            >
              {notice.text}
            </div>
          ) : null}

          {error ? (
            <div className="rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">
              {error}
            </div>
          ) : null}
        </div>
      )}
    </SectionCard>
  );
}
