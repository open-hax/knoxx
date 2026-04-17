import React, { useCallback, useEffect, useMemo, useState } from "react";

import {
  compileContract,
  copyContract,
  DEFAULT_CONTRACT_EDN,
  EVENT_KIND_OPTIONS,
  getContract,
  listContracts,
  MODEL_OPTIONS,
  ROLE_OPTIONS,
  saveContract,
  SOURCE_KIND_OPTIONS,
  THINKING_OPTIONS,
  TRIGGER_KIND_OPTIONS,
  validateContract,
  type AgentContract,
  type ContractCompileResult,
  type ContractListItem,
  type ContractValidationResult,
} from "../../lib/api/contracts";
import { Badge, SectionCard, classNames } from "./common";
import { EdnEditor } from "./EdnEditor";

type Notice = { tone: "success" | "error"; text: string } | null;

type ValidationWithContract = ContractValidationResult & { contract?: AgentContract | null };

function prettyJson(value: unknown): string {
  return JSON.stringify(value ?? null, null, 2);
}

function normalizeId(value: string): string {
  return value.trim();
}

function parseContractIdFromEdn(ednText: string): string | null {
  const match = ednText.match(/:contract\/id\s+"?([^"\s}]+)"?/);
  return match?.[1] ?? null;
}

function parseKeywordVector(ednText: string, key: ":always" | ":maybe"): string[] {
  const pattern = new RegExp(`:events[\\s\\S]*?${key}\\s+\\[([^\\]]*)\\]`, "m");
  const match = ednText.match(pattern);
  if (!match?.[1]) return [];
  return match[1]
    .split(/[\s,]+/g)
    .map((token) => token.trim())
    .filter(Boolean)
    .map((token) => token.startsWith(":") ? token.slice(1) : token);
}

function setKeywordVector(ednText: string, key: ":always" | ":maybe", values: string[]): string {
  const rendered = values.map((v) => (v.startsWith(":") ? v : `:${v}`)).join(" ");
  const pattern = new RegExp(`(:events[\\s\\S]*?${key}\\s+\\[)([^\\]]*)(\\])`, "m");
  if (pattern.test(ednText)) {
    return ednText.replace(pattern, `$1${rendered}$3`);
  }
  // If the key doesn't exist, do nothing (keep editor canonical).
  return ednText;
}

function extractSimpleValue(ednText: string, key: string): string | null {
  const pattern = new RegExp(`(^\\s*:${key}\\s+)([^\\n\\r]+)$`, "m");
  const match = ednText.match(pattern);
  if (!match?.[2]) return null;
  return match[2].trim();
}

function replaceSimpleValue(ednText: string, key: string, token: string): string {
  const pattern = new RegExp(`(^\\s*:${key}\\s+)([^\\n\\r]+)$`, "m");
  if (pattern.test(ednText)) {
    return ednText.replace(pattern, `$1${token}`);
  }

  // Insert after opening { on the first line
  const idx = ednText.indexOf("{");
  if (idx >= 0) {
    const insertAt = idx + 1;
    return `${ednText.slice(0, insertAt)}\n :${key} ${token}${ednText.slice(insertAt)}`;
  }
  return ednText;
}

function extractAgentValue(ednText: string, agentKey: string): string | null {
  const pattern = new RegExp(`(:agent[\\s\\S]{0,400}?:${agentKey}\\s+)([^\\s\\n\\r}]+)`, "m");
  const match = ednText.match(pattern);
  if (!match?.[2]) return null;
  return match[2].trim();
}

function replaceAgentValue(ednText: string, agentKey: string, token: string): string {
  const pattern = new RegExp(`(:agent[\\s\\S]{0,400}?:${agentKey}\\s+)([^\\s\\n\\r}]+)`, "m");
  if (pattern.test(ednText)) {
    return ednText.replace(pattern, `$1${token}`);
  }
  return ednText;
}

function SearchableSelect({
  value,
  onChange,
  options,
  disabled,
  label,
  placeholder,
}: {
  value: string;
  onChange: (value: string) => void;
  options: readonly string[];
  disabled?: boolean;
  label: string;
  placeholder?: string;
}) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const filtered = useMemo(
    () => options.filter((o) => o.toLowerCase().includes(search.toLowerCase())),
    [options, search],
  );

  return (
    <label className="space-y-1">
      <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">{label}</div>
      <div className="relative">
        <input
          value={open ? search : value}
          onChange={(e) => {
            setSearch(e.target.value);
            if (!open) setOpen(true);
          }}
          onFocus={() => {
            setOpen(true);
            setSearch("");
          }}
          onBlur={() => setTimeout(() => setOpen(false), 150)}
          disabled={disabled}
          className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
          placeholder={placeholder ?? "Select…"}
        />
        {open && filtered.length > 0 ? (
          <div className="absolute z-20 mt-1 max-h-48 w-full overflow-auto rounded-lg border border-slate-700 bg-slate-900 py-1 shadow-xl">
            {filtered.map((opt) => (
              <button
                key={opt}
                type="button"
                onMouseDown={() => {
                  onChange(opt);
                  setOpen(false);
                  setSearch("");
                }}
                className={classNames(
                  "w-full px-3 py-1.5 text-left text-sm hover:bg-sky-600/20",
                  opt === value ? "bg-sky-600/10 text-sky-200" : "text-slate-200",
                )}
              >
                {opt}
              </button>
            ))}
          </div>
        ) : null}
      </div>
    </label>
  );
}

function TagInput({
  label,
  value,
  onChange,
  suggestions,
  disabled,
}: {
  label: string;
  value: string[];
  onChange: (next: string[]) => void;
  suggestions: readonly string[];
  disabled?: boolean;
}) {
  const [draft, setDraft] = useState("");
  const filtered = useMemo(
    () =>
      draft
        ? suggestions.filter(
            (s) => s.toLowerCase().includes(draft.toLowerCase()) && !value.includes(s),
          )
        : [],
    [draft, suggestions, value],
  );

  const add = useCallback(
    (tag: string) => {
      const trimmed = tag.trim();
      if (!trimmed) return;
      if (value.includes(trimmed)) return;
      onChange([...value, trimmed]);
      setDraft("");
    },
    [onChange, value],
  );

  return (
    <label className="space-y-1">
      <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">{label}</div>
      <div className="flex flex-wrap gap-1 rounded-lg border border-slate-800 bg-slate-950/70 px-2 py-2">
        {value.map((tag) => (
          <span
            key={tag}
            className="inline-flex items-center gap-1 rounded-md border border-sky-500/30 bg-sky-500/10 px-2 py-0.5 text-xs text-sky-200"
          >
            {tag}
            <button
              type="button"
              onClick={() => onChange(value.filter((t) => t !== tag))}
              disabled={disabled}
              className="text-sky-300 hover:text-rose-300 disabled:opacity-60"
            >
              ×
            </button>
          </span>
        ))}
        <input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && draft.trim()) {
              e.preventDefault();
              add(draft);
            }
            if (e.key === "Backspace" && !draft && value.length > 0) {
              onChange(value.slice(0, -1));
            }
          }}
          disabled={disabled}
          className="min-w-[70px] flex-1 bg-transparent text-sm text-slate-100 outline-none placeholder:text-slate-600"
          placeholder="Type + Enter…"
        />
      </div>
      {filtered.length > 0 ? (
        <div className="max-h-32 overflow-auto rounded-lg border border-slate-700 bg-slate-900 py-1 text-xs">
          {filtered.slice(0, 8).map((s) => (
            <button
              key={s}
              type="button"
              onMouseDown={() => add(s)}
              className="w-full px-2 py-1 text-left text-slate-300 hover:bg-sky-600/20 hover:text-sky-200"
            >
              {s}
            </button>
          ))}
        </div>
      ) : null}
    </label>
  );
}

export function ContractsSection({ canManage }: { canManage: boolean }) {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [validating, setValidating] = useState(false);
  const [compiling, setCompiling] = useState(false);
  const [notice, setNotice] = useState<Notice>(null);
  const [error, setError] = useState("");

  const [contracts, setContracts] = useState<ContractListItem[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const [ednDraft, setEdnDraft] = useState(DEFAULT_CONTRACT_EDN);
  const [lastSavedEdn, setLastSavedEdn] = useState<string | null>(null);
  const [validation, setValidation] = useState<ValidationWithContract | null>(null);

  const [copyTarget, setCopyTarget] = useState("");
  const [showCopy, setShowCopy] = useState(false);

  const [showNormalized, setShowNormalized] = useState(false);
  const [normalizedView, setNormalizedView] = useState<unknown>(null);

  const [showSql, setShowSql] = useState(false);
  const [compiledSql, setCompiledSql] = useState<ContractCompileResult["sql"] | null>(null);

  const isDirty = lastSavedEdn == null ? ednDraft.trim().length > 0 : ednDraft !== lastSavedEdn;

  const loadList = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const result = await listContracts();
      setContracts(result.contracts);
      if (!selectedId && result.contracts.length > 0) {
        setSelectedId(result.contracts[0].id);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }, [selectedId]);

  const loadContract = useCallback(async (contractId: string) => {
    setError("");
    try {
      const result = await getContract(contractId);
      setEdnDraft(result.ednText);
      setLastSavedEdn(result.ednText);
      setValidation({ ...result.validation, contract: result.contract });
      setNormalizedView(result.contract);
      setCompiledSql(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setEdnDraft("");
      setLastSavedEdn(null);
      setValidation(null);
      setNormalizedView(null);
      setCompiledSql(null);
    }
  }, []);

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
      setNormalizedView(null);
      setCompiledSql(null);
    }
  }, [selectedId, loadContract]);

  const handleValidate = useCallback(async () => {
    setValidating(true);
    setNotice(null);
    setError("");
    try {
      const result = await validateContract(ednDraft) as ValidationWithContract;
      setValidation(result);
      if (result.contract) setNormalizedView(result.contract);

      if (result.ok) {
        setNotice({ tone: "success", text: "Validation passed." });
      } else {
        setNotice({ tone: "error", text: `Validation failed: ${result.errors.length} error(s).` });
      }
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setValidating(false);
    }
  }, [ednDraft]);

  const handleSave = useCallback(async () => {
    if (!canManage) return;

    const explicitId = selectedId ? normalizeId(selectedId) : null;
    const inferredId = parseContractIdFromEdn(ednDraft);
    const contractId = explicitId || inferredId;

    if (!contractId) {
      setNotice({ tone: "error", text: "Missing contract id. Set :contract/id in EDN (or select an existing contract)." });
      return;
    }

    setSaving(true);
    setNotice(null);
    setError("");
    try {
      const result = await saveContract(contractId, ednDraft);
      setSelectedId(contractId);
      setEdnDraft(result.ednText);
      setLastSavedEdn(result.ednText);
      setValidation({ ...result.validation, contract: result.contract });
      setNormalizedView(result.contract);
      setCompiledSql(null);

      if (result.validation.ok) {
        setNotice({ tone: "success", text: `Saved ${contractId}.` });
      } else {
        setNotice({ tone: "error", text: `Saved ${contractId}, but validation has ${result.validation.errors.length} error(s).` });
      }

      await loadList();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setSaving(false);
    }
  }, [canManage, ednDraft, loadList, selectedId]);

  const handleCopy = useCallback(async () => {
    if (!canManage || !selectedId) return;
    const nextId = normalizeId(copyTarget);
    if (!nextId) return;

    setSaving(true);
    setNotice(null);
    setError("");
    try {
      const result = await copyContract(selectedId, nextId);
      setNotice({ tone: "success", text: `Copied ${selectedId} → ${nextId}.` });
      setSelectedId(nextId);
      setEdnDraft(result.ednText);
      setLastSavedEdn(result.ednText);
      setValidation({ ...result.validation, contract: result.contract });
      setNormalizedView(result.contract);
      setCopyTarget("");
      setShowCopy(false);
      await loadList();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setSaving(false);
    }
  }, [canManage, copyTarget, loadList, selectedId]);

  const handleCompile = useCallback(async () => {
    if (!canManage || !selectedId) return;

    setCompiling(true);
    setNotice(null);
    setError("");
    try {
      const result = await compileContract(selectedId);
      if (result.ok) {
        setCompiledSql(result.sql);
        setShowSql(true);
        setNotice({ tone: "success", text: `Compiled ${selectedId}.` });
      } else {
        setNotice({ tone: "error", text: `Compile failed: ${result.errors?.map((e) => e.message).join(", ") ?? "unknown"}` });
      }
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally {
      setCompiling(false);
    }
  }, [canManage, selectedId]);

  const validationErrors = validation?.errors ?? [];

  // ── metadata form (best-effort sync from EDN) ─────────────────────────────

  const enabledToken = extractSimpleValue(ednDraft, "enabled");
  const triggerKindToken = extractSimpleValue(ednDraft, "trigger-kind") ?? ":event";
  const sourceKindToken = extractSimpleValue(ednDraft, "source-kind") ?? ":discord";
  const sourceModeToken = extractSimpleValue(ednDraft, "source-mode") ?? ":patrol";
  const cadenceToken = extractSimpleValue(ednDraft, "cadence-min") ?? "5";

  const roleToken = extractAgentValue(ednDraft, "role") ?? ":system_admin";
  const modelToken = extractAgentValue(ednDraft, "model") ?? '"glm-5"';
  const thinkingToken = extractAgentValue(ednDraft, "thinking") ?? ":off";

  const eventsAlways = useMemo(() => parseKeywordVector(ednDraft, ":always"), [ednDraft]);
  const eventsMaybe = useMemo(() => parseKeywordVector(ednDraft, ":maybe"), [ednDraft]);

  const updateEnabled = useCallback((checked: boolean) => {
    setEdnDraft((current) => replaceSimpleValue(current, "enabled", checked ? "true" : "false"));
  }, []);

  const updateTriggerKind = useCallback((value: string) => {
    setEdnDraft((current) => replaceSimpleValue(current, "trigger-kind", `:${value}`));
  }, []);

  const updateSourceKind = useCallback((value: string) => {
    setEdnDraft((current) => replaceSimpleValue(current, "source-kind", `:${value}`));
  }, []);

  const updateSourceMode = useCallback((value: string) => {
    setEdnDraft((current) => replaceSimpleValue(current, "source-mode", `:${value}`));
  }, []);

  const updateCadence = useCallback((value: string) => {
    const safe = String(Math.max(1, Number(value || 1)));
    setEdnDraft((current) => replaceSimpleValue(current, "cadence-min", safe));
  }, []);

  const updateRole = useCallback((value: string) => {
    setEdnDraft((current) => replaceAgentValue(current, "role", `:${value}`));
  }, []);

  const updateModel = useCallback((value: string) => {
    setEdnDraft((current) => replaceAgentValue(current, "model", `"${value}"`));
  }, []);

  const updateThinking = useCallback((value: string) => {
    setEdnDraft((current) => replaceAgentValue(current, "thinking", `:${value}`));
  }, []);

  const updateEventsAlways = useCallback((next: string[]) => {
    setEdnDraft((current) => setKeywordVector(current, ":always", next));
  }, []);

  const updateEventsMaybe = useCallback((next: string[]) => {
    setEdnDraft((current) => setKeywordVector(current, ":maybe", next));
  }, []);

  const kindSuggestions = useMemo(() => EVENT_KIND_OPTIONS.map((k) => (k.startsWith(":") ? k.slice(1) : k)), []);

  // ── render ───────────────────────────────────────────────────────────────

  return (
    <SectionCard
      title="Contracts"
      description="Editor-first contract IDE surface: edit canonical EDN, validate, clone, preview normalized view, and compile SQL projections."
      actions={
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => void loadList()}
            disabled={loading || saving || validating || compiling}
            className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
          >
            {loading ? "Loading…" : "Refresh"}
          </button>
          <button
            type="button"
            onClick={() => void handleValidate()}
            disabled={validating}
            className="inline-flex items-center justify-center rounded-lg border border-sky-700 bg-sky-900/50 px-3 py-2 text-sm font-medium text-sky-200 hover:bg-sky-800/50 disabled:opacity-60"
          >
            {validating ? "Validating…" : "Validate"}
          </button>
          <button
            type="button"
            onClick={() => void handleSave()}
            disabled={!canManage || saving || !isDirty}
            className="inline-flex items-center justify-center rounded-lg bg-sky-600 px-4 py-2 text-sm font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60"
          >
            {saving ? "Saving…" : "Save"}
          </button>
          <button
            type="button"
            onClick={() => setShowCopy((v) => !v)}
            disabled={!canManage || !selectedId}
            className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
          >
            Copy agent
          </button>
          <button
            type="button"
            onClick={() => void handleCompile()}
            disabled={!canManage || compiling || !selectedId}
            className="inline-flex items-center justify-center rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-3 py-2 text-sm font-medium text-emerald-200 hover:bg-emerald-500/20 disabled:opacity-60"
          >
            {compiling ? "Compiling…" : "Compile to SQL"}
          </button>
          <button
            type="button"
            onClick={() => setShowNormalized((v) => !v)}
            className="inline-flex items-center justify-center rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm font-medium text-slate-100 hover:bg-slate-800 disabled:opacity-60"
          >
            {showNormalized ? "Hide normalized" : "Show normalized"}
          </button>
        </div>
      }
    >
      <div className="space-y-4">
        {/* Copy row */}
        {showCopy ? (
          <div className="flex flex-wrap items-center gap-2 rounded-xl border border-slate-800 bg-slate-950/40 p-3">
            <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">New ID</div>
            <input
              value={copyTarget}
              onChange={(e) => setCopyTarget(e.target.value)}
              placeholder="new-contract-id"
              disabled={!canManage}
              className="flex-1 min-w-[240px] rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
            />
            <button
              type="button"
              onClick={() => void handleCopy()}
              disabled={!canManage || !copyTarget.trim()}
              className="inline-flex items-center justify-center rounded-lg bg-sky-600 px-3 py-2 text-sm font-semibold text-slate-50 hover:bg-sky-500 disabled:opacity-60"
            >
              Copy
            </button>
          </div>
        ) : null}

        <div className="grid gap-4 lg:grid-cols-[320px_1fr]">
          {/* Left column: metadata form */}
          <div className="space-y-3 rounded-xl border border-slate-800 bg-slate-950/40 p-4">
            <div className="flex items-center justify-between gap-2">
              <div className="text-sm font-semibold text-slate-100">Metadata</div>
              {selectedId ? <Badge tone="info">{selectedId}</Badge> : <Badge>new</Badge>}
            </div>

            <label className="space-y-1">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Contract</div>
              <select
                value={selectedId ?? ""}
                onChange={(e) => setSelectedId(e.target.value ? e.target.value : null)}
                className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500"
              >
                <option value="">— New contract —</option>
                {contracts.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.id}
                  </option>
                ))}
              </select>
            </label>

            <label className="space-y-1">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Enabled</div>
              <label className="inline-flex items-center gap-2 text-sm text-slate-200">
                <input
                  type="checkbox"
                  checked={enabledToken !== "false"}
                  onChange={(e) => updateEnabled(e.target.checked)}
                  disabled={!canManage}
                />
                Active
              </label>
            </label>

            <SearchableSelect
              label="Trigger kind"
              value={triggerKindToken.replace(/^:/, "")}
              onChange={updateTriggerKind}
              options={Array.from(TRIGGER_KIND_OPTIONS)}
              disabled={!canManage}
            />

            <SearchableSelect
              label="Source kind"
              value={sourceKindToken.replace(/^:/, "")}
              onChange={updateSourceKind}
              options={Array.from(SOURCE_KIND_OPTIONS)}
              disabled={!canManage}
            />

            <label className="space-y-1">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Source mode</div>
              <input
                value={sourceModeToken.replace(/^:/, "")}
                onChange={(e) => updateSourceMode(e.target.value)}
                disabled={!canManage}
                className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
              />
            </label>

            <label className="space-y-1">
              <div className="text-xs font-semibold uppercase tracking-wide text-slate-400">Cadence (min)</div>
              <input
                type="number"
                min={1}
                value={cadenceToken.replace(/[^0-9]/g, "") || "1"}
                onChange={(e) => updateCadence(e.target.value)}
                disabled={!canManage}
                className="w-full rounded-lg border border-slate-800 bg-slate-950/70 px-3 py-2 text-sm text-slate-100 outline-none focus:border-sky-500 disabled:opacity-60"
              />
            </label>

            <SearchableSelect
              label="Role"
              value={roleToken.replace(/^:/, "").replace(/"/g, "")}
              onChange={updateRole}
              options={ROLE_OPTIONS}
              disabled={!canManage}
            />

            <SearchableSelect
              label="Model"
              value={modelToken.replace(/^"|"$/g, "")}
              onChange={updateModel}
              options={MODEL_OPTIONS}
              disabled={!canManage}
            />

            <SearchableSelect
              label="Thinking"
              value={thinkingToken.replace(/^:/, "")}
              onChange={updateThinking}
              options={Array.from(THINKING_OPTIONS)}
              disabled={!canManage}
            />

            <TagInput
              label="Event kinds (always)"
              value={eventsAlways}
              onChange={updateEventsAlways}
              suggestions={kindSuggestions}
              disabled={!canManage}
            />

            <TagInput
              label="Event kinds (maybe)"
              value={eventsMaybe}
              onChange={updateEventsMaybe}
              suggestions={kindSuggestions}
              disabled={!canManage}
            />

            {validation ? (
              <div className={classNames(
                "rounded-xl border px-3 py-2 text-xs",
                validation.ok
                  ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-200"
                  : "border-rose-500/30 bg-rose-500/10 text-rose-200",
              )}>
                {validation.ok ? "✓ Valid" : `${validation.errors.length} error(s)`}
              </div>
            ) : null}

            {!canManage ? (
              <div className="rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
                Read-only. You need <code className="font-mono">platform.org.create</code> permission to edit contracts.
              </div>
            ) : null}
          </div>

          {/* Right column: editor */}
          <div className="space-y-3">
            <div className="flex items-center justify-between gap-2">
              <div className="text-sm font-semibold text-slate-100">EDN</div>
              {isDirty && canManage ? <Badge tone="warn">unsaved</Badge> : null}
            </div>

            <EdnEditor
              value={ednDraft}
              onChange={setEdnDraft}
              readOnly={!canManage}
              height="560px"
              placeholder="Enter EDN contract…"
              externalErrors={validationErrors.map((e) => ({
                message: e.path.length ? `${e.path.join(".")}: ${e.message}` : e.message,
              }))}
            />

            {showSql && compiledSql ? (
              <div className="space-y-2">
                <div className="text-sm font-semibold text-slate-100">SQL projections (preview)</div>
                <pre className="max-h-72 overflow-auto rounded-lg border border-slate-800 bg-slate-950/70 p-3 text-xs text-emerald-200">
                  {prettyJson(compiledSql)}
                </pre>
              </div>
            ) : null}

            {showNormalized ? (
              <div className="space-y-2">
                <div className="text-sm font-semibold text-slate-100">Normalized view</div>
                <pre className="max-h-72 overflow-auto rounded-lg border border-slate-800 bg-slate-950/70 p-3 text-xs text-slate-200">
                  {prettyJson(normalizedView)}
                </pre>
                <div className="text-[11px] text-slate-500">
                  (Populated from backend parse on load/save/validate; EDN remains canonical.)
                </div>
              </div>
            ) : null}

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

            {error ? (
              <div className="rounded-lg border border-rose-500/30 bg-rose-500/10 px-3 py-2 text-sm text-rose-200">
                {error}
              </div>
            ) : null}
          </div>
        </div>
      </div>
    </SectionCard>
  );
}
