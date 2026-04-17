import React, { useCallback, useEffect, useState } from "react";

import {
  compileContract,
  copyContract,
  DEFAULT_CONTRACT_EDN,
  getContract,
  listContracts,
  saveContract,
  validateContract,
  type ContractListItem,
  type ContractValidationResult,
} from "../../lib/api/contracts";
import { Badge, SectionCard, classNames } from "./common";
import { EdnEditor } from "./EdnEditor";

type Notice = { tone: "success" | "error"; text: string } | null;

export function ContractsSection({ canManage }: { canManage: boolean }) {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [compiling, setCompiling] = useState(false);
  const [notice, setNotice] = useState<Notice>(null);
  const [error, setError] = useState("");

  // List state
  const [contracts, setContracts] = useState<ContractListItem[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  // Editor state
  const [ednDraft, setEdnDraft] = useState(DEFAULT_CONTRACT_EDN);
  const [validation, setValidation] = useState<ContractValidationResult | null>(null);
  const [lastSavedEdn, setLastSavedEdn] = useState<string | null>(null);

  // Copy dialog
  const [copyTarget, setCopyTarget] = useState<string>("");
  const [showCopy, setShowCopy] = useState(false);

  const isDirty = ednDraft !== lastSavedEdn;

  // ── Load contract list ──────────────────────────────────────────────────

  const loadList = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const result = await listContracts();
      setContracts(result.contracts);
      // Auto-select first if nothing selected
      if (!selectedId && result.contracts.length > 0) {
        setSelectedId(result.contracts[0].id);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }, [selectedId]);

  // ── Load single contract ────────────────────────────────────────────────

  const loadContract = useCallback(async (contractId: string) => {
    setError("");
    setValidation(null);
    try {
      const result = await getContract(contractId);
      setEdnDraft(result.ednText);
      setLastSavedEdn(result.ednText);
      setValidation(result.validation);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setEdnDraft("");
      setLastSavedEdn(null);
    }
  }, []);

  // ── Initial load ────────────────────────────────────────────────────────

  useEffect(() => {
    void loadList();
  }, [loadList]);

  useEffect(() => {
    if (selectedId) {
      void loadContract(selectedId);
    } else {
      setEdnDraft(DEFAULT_CONTRACT_EDN);
      setLastSavedEdn(null);
      setValidation(null);
    }
  }, [selectedId, loadContract]);

  // ── Handlers ────────────────────────────────────────────────────────────

  const handleSave = useCallback(async () => {
    if (!canManage || !selectedId) return;
    setSaving(true);
    setNotice(null);
    setError("");
    try {
      const result = await saveContract(selectedId, ednDraft);
      setEdnDraft(result.ednText);
      setLastSavedEdn(result.ednText);
      setValidation(result.validation);
      if (result.validation.ok) {
        setNotice({ tone: "success", text: `Contract ${selectedId} saved and valid.` });
      } else {
        setNotice({ tone: "error", text: `Contract saved but has ${result.validation.errors.length} error(s).` });
      }
      await loadList();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setSaving(false);
    }
  }, [canManage, selectedId, ednDraft, loadList]);

  const handleValidate = useCallback(async () => {
    setNotice(null);
    try {
      const result = await validateContract(ednDraft);
      setValidation(result);
      if (result.ok) {
        setNotice({ tone: "success", text: "EDN validation passed." });
      } else {
        setNotice({ tone: "error", text: `${result.errors.length} error(s), ${result.warnings.length} warning(s).` });
      }
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    }
  }, [ednDraft]);

  const handleCompile = useCallback(async () => {
    if (!canManage || !selectedId) return;
    setCompiling(true);
    setNotice(null);
    try {
      const result = await compileContract(selectedId);
      if (result.ok) {
        setNotice({ tone: "success", text: `Contract ${selectedId} compiled successfully.` });
      } else {
        setNotice({ tone: "error", text: `Compile failed: ${result.errors?.map((e) => e.message).join(", ") ?? "unknown"}` });
      }
      await loadList();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setCompiling(false);
    }
  }, [canManage, selectedId, loadList]);

  const handleNew = useCallback(() => {
    setSelectedId(null);
    setEdnDraft(DEFAULT_CONTRACT_EDN);
    setLastSavedEdn(null);
    setValidation(null);
    setShowCopy(false);
  }, []);

  const handleCopy = useCallback(async () => {
    if (!canManage || !selectedId || !copyTarget.trim()) return;
    setSaving(true);
    setNotice(null);
    try {
      const result = await copyContract(selectedId, copyTarget.trim());
      setNotice({ tone: "success", text: `Copied to ${copyTarget.trim()}.` });
      setSelectedId(copyTarget.trim());
      setShowCopy(false);
      setCopyTarget("");
      await loadList();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setSaving(false);
    }
  }, [canManage, selectedId, copyTarget, loadList]);

  const handleCreateFromDraft = useCallback(async () => {
    if (!canManage) return;
    // Extract contract/id from the EDN draft
    const idMatch = ednDraft.match(/:contract\/id\s+"?([^"\s}]+)"?/);
    if (!idMatch) {
      setNotice({ tone: "error", text: "Cannot find :contract/id in EDN draft." });
      return;
    }
    const newId = idMatch[1];
    setSaving(true);
    setNotice(null);
    try {
      const result = await saveContract(newId, ednDraft);
      setEdnDraft(result.ednText);
      setLastSavedEdn(result.ednText);
      setValidation(result.validation);
      setSelectedId(newId);
      setNotice({ tone: "success", text: `Contract ${newId} created.` });
      await loadList();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setSaving(false);
    }
  }, [canManage, ednDraft, loadList]);

  // ── External errors for EdnEditor ───────────────────────────────────────

  const externalErrors = validation && !validation.ok
    ? validation.errors.map((e) => ({
        line: e.path.length > 0 ? undefined : undefined,
        message: e.path.length > 0 ? `${e.path.join(".")}: ${e.message}` : e.message,
      }))
    : [];

  // ── Render ──────────────────────────────────────────────────────────────

  return (
    <SectionCard
      title="Contracts"
      description="EDN-based agent contracts. Define triggers, prompts, events, and tool bindings."
      actions={
        <div className="flex gap-2">
          <button
            type="button"
            onClick={() => void loadList()}
            disabled={loading || saving}
            className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
          >
            {loading ? "Loading…" : "Refresh"}
          </button>
          <button
            type="button"
            onClick={handleNew}
            disabled={saving}
            className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
          >
            New
          </button>
        </div>
      }
    >
      <div className="grid gap-6 xl:grid-cols-[0.4fr_0.6fr]">
        {/* ── Left: Contract list ── */}
        <div className="space-y-3">
          <div className="text-sm font-semibold text-slate-100">Contract catalog</div>

          {loading && contracts.length === 0 ? (
            <div className="text-sm text-slate-400">Loading contracts…</div>
          ) : contracts.length === 0 ? (
            <div className="rounded-xl border border-dashed border-slate-800 px-4 py-6 text-sm text-slate-400">
              No contracts yet. Click "New" to create one.
            </div>
          ) : (
            <div className="space-y-2">
              {contracts.map((c) => (
                <button
                  key={c.id}
                  type="button"
                  onClick={() => setSelectedId(c.id)}
                  className={classNames(
                    "w-full rounded-xl border p-3 text-left transition-colors",
                    selectedId === c.id
                      ? "border-sky-500/50 bg-sky-500/10"
                      : "border-slate-800 bg-slate-950/40 hover:bg-slate-900/80",
                  )}
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="truncate text-sm font-semibold text-slate-100">{c.id}</span>
                    <Badge tone={c.enabled ? "success" : "warn"}>
                      {c.enabled ? "on" : "off"}
                    </Badge>
                  </div>
                  <div className="mt-1 flex flex-wrap gap-1.5">
                    <Badge tone="info">{c.kind}</Badge>
                    <Badge>v{c.version}</Badge>
                    {c.compiledAt ? (
                      <Badge tone="success">compiled</Badge>
                    ) : (
                      <Badge tone="warn">uncompiled</Badge>
                    )}
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* ── Right: Editor ── */}
        <div className="space-y-4">
          <div className="flex items-center justify-between gap-3">
            <div className="text-sm font-semibold text-slate-100">
              {selectedId ? `Editing: ${selectedId}` : "New contract"}
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => void handleValidate()}
                disabled={saving}
                className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
              >
                Validate
              </button>
              {selectedId ? (
                <>
                  <button
                    type="button"
                    onClick={() => void handleSave()}
                    disabled={!canManage || saving || !isDirty}
                    className="inline-flex items-center justify-center rounded-lg bg-sky-600 px-3 py-1.5 text-xs font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60"
                  >
                    {saving ? "Saving…" : "Save"}
                  </button>
                  <button
                    type="button"
                    onClick={() => void handleCompile()}
                    disabled={!canManage || compiling}
                    className="inline-flex items-center justify-center rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-3 py-1.5 text-xs font-medium text-emerald-200 hover:bg-emerald-500/20 disabled:opacity-60"
                  >
                    {compiling ? "Compiling…" : "Compile"}
                  </button>
                  <button
                    type="button"
                    onClick={() => setShowCopy((v) => !v)}
                    disabled={!canManage}
                    className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-xs font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
                  >
                    Copy
                  </button>
                </>
              ) : (
                <button
                  type="button"
                  onClick={() => void handleCreateFromDraft()}
                  disabled={!canManage || saving}
                  className="inline-flex items-center justify-center rounded-lg bg-sky-600 px-3 py-1.5 text-xs font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60"
                >
                  {saving ? "Creating…" : "Create"}
                </button>
              )}
            </div>
          </div>

          {/* Copy dialog */}
          {showCopy ? (
            <div className="flex items-center gap-2 rounded-xl border border-slate-800 bg-slate-950/40 p-3">
              <input
                value={copyTarget}
                onChange={(e) => setCopyTarget(e.target.value)}
                placeholder="new-contract-id"
                className="flex-1 rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500"
              />
              <button
                type="button"
                onClick={() => void handleCopy()}
                disabled={!copyTarget.trim()}
                className="inline-flex items-center justify-center rounded-lg bg-sky-600 px-3 py-2 text-xs font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60"
              >
                Copy
              </button>
            </div>
          ) : null}

          {/* Validation status */}
          {validation ? (
            <div className={classNames(
              "rounded-xl border px-3 py-2 text-xs",
              validation.ok
                ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-200"
                : "border-rose-500/30 bg-rose-500/10 text-rose-200",
            )}>
              {validation.ok ? (
                <span>✓ Valid</span>
              ) : (
                <div className="space-y-1">
                  <div className="font-semibold">{validation.errors.length} error(s)</div>
                  {validation.errors.map((e, i) => (
                    <div key={`err-${i}`}>
                      {e.path.length > 0 ? <span className="font-mono text-slate-400">{e.path.join(".")}:</span> : null} {e.message}
                    </div>
                  ))}
                  {validation.warnings.length > 0 ? (
                    <>
                      <div className="mt-1 font-semibold text-amber-200">{validation.warnings.length} warning(s)</div>
                      {validation.warnings.map((w, i) => (
                        <div key={`warn-${i}`}>
                          {w.path.length > 0 ? <span className="font-mono text-slate-400">{w.path.join(".")}:</span> : null} {w.message}
                        </div>
                      ))}
                    </>
                  ) : null}
                </div>
              )}
            </div>
          ) : null}

          {/* EDN Editor */}
          <EdnEditor
            value={ednDraft}
            onChange={setEdnDraft}
            readOnly={!canManage}
            height="500px"
            placeholder="Enter EDN contract…"
            externalErrors={externalErrors}
          />

          {/* Dirty indicator */}
          {isDirty && canManage ? (
            <div className="text-xs text-amber-300">Unsaved changes</div>
          ) : null}

          {/* Notice */}
          {notice ? (
            <div className={classNames(
              "rounded-lg border px-3 py-2 text-sm",
              notice.tone === "success"
                ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-200"
                : "border-rose-500/30 bg-rose-500/10 text-rose-200",
            )}>
              {notice.text}
            </div>
          ) : null}

          {/* Error */}
          {error ? (
            <div className="rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">
              {error}
            </div>
          ) : null}

          {!canManage ? (
            <div className="rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
              Read-only. You need <code className="font-mono">platform.org.create</code> permission to edit contracts.
            </div>
          ) : null}
        </div>
      </div>
    </SectionCard>
  );
}
