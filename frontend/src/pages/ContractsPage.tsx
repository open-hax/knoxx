import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useResolvedTheme, tokens } from "@open-hax/uxx";
import type { ThemePalette } from "@open-hax/uxx/tokens";

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
} from "../lib/api/contracts";
import { getEventAgentControl } from "../lib/api/admin";
import type { EventAgentControlResponse, EventAgentJobControl, EventAgentRuntimeJob } from "../lib/api/admin";
import { EdnEditor } from "../components/admin-page/EdnEditor";
import { ChatWorkspacePane } from "../components/chat-page/ChatWorkspacePane";
import { useChatWorkspaceController } from "../components/chat-page/useChatWorkspaceController";
import type { AgentSource } from "../lib/types";

// ── Chat sidebar persistence keys (namespaced to avoid CMS collisions) ───────

const CHAT_SESSION_ID_KEY = "knoxx_contracts_session_id";
const CHAT_SCRATCHPAD_KEY = "knoxx_contracts_scratchpad_state";
const CHAT_PINNED_KEY = "knoxx_contracts_pinned_context";
const CHAT_SESSION_STATE_KEY = "knoxx_contracts_chat_session_state";
const CHAT_SIDEBAR_WIDTH_KEY = "knoxx_contracts_sidebar_width_px";

// ── Helpers ──────────────────────────────────────────────────────────────────

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

// ── Inline primitives (uxx-styled) ───────────────────────────────────────────

function SearchableSelect({ value, onChange, options, disabled, label, placeholder }: {
  value: string; onChange: (value: string) => void; options: readonly string[];
  disabled?: boolean; label: string; placeholder?: string;
}) {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState("");
  const filtered = useMemo(
    () => options.filter((o) => o.toLowerCase().includes(search.toLowerCase())),
    [options, search],
  );
  const palette = useResolvedTheme().palette as ThemePalette;
  return (
    <label style={{ display: "flex", flexDirection: "column", gap: 4 }}>
      <div style={{ fontSize: tokens.fontSize.xs, fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em", color: palette.fg.muted }}>{label}</div>
      <div style={{ position: "relative" }}>
        <input
          value={open ? search : value}
          onChange={(e) => { setSearch(e.target.value); if (!open) setOpen(true); }}
          onFocus={() => { setOpen(true); setSearch(""); }}
          onBlur={() => setTimeout(() => setOpen(false), 150)}
          disabled={disabled}
          style={{
            width: "100%", borderRadius: tokens.radius.md, border: `1px solid ${palette.fg.subtle}`,
            background: palette.bg.darker, padding: "6px 10px", fontSize: tokens.fontSize.sm,
            color: palette.fg.default, outline: "none",
          }}
          placeholder={placeholder ?? "Select…"}
        />
        {open && filtered.length > 0 ? (
          <div style={{
            position: "absolute", zIndex: 20, marginTop: 4, maxHeight: 180, width: "100%",
            overflow: "auto", borderRadius: tokens.radius.md, border: `1px solid ${palette.fg.subtle}`,
            background: palette.bg.darker, padding: "4px 0",
          }}>
            {filtered.map((opt) => (
              <button
                key={opt} type="button"
                onMouseDown={() => { onChange(opt); setOpen(false); setSearch(""); }}
                style={{
                  width: "100%", padding: "6px 10px", textAlign: "left", fontSize: tokens.fontSize.sm,
                  color: opt === value ? palette.accent.cyan : palette.fg.default,
                  background: opt === value ? "rgba(102, 217, 239, 0.1)" : "transparent",
                  border: "none", cursor: "pointer",
                }}
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

function TagInput({ label, value, onChange, suggestions, disabled }: {
  label: string; value: string[]; onChange: (next: string[]) => void;
  suggestions: readonly string[]; disabled?: boolean;
}) {
  const [draft, setDraft] = useState("");
  const filtered = useMemo(
    () => draft ? suggestions.filter((s) => s.toLowerCase().includes(draft.toLowerCase()) && !value.includes(s)) : [],
    [draft, suggestions, value],
  );
  const palette = useResolvedTheme().palette as ThemePalette;
  const add = useCallback((tag: string) => {
    const trimmed = tag.trim();
    if (!trimmed || value.includes(trimmed)) return;
    onChange([...value, trimmed]);
    setDraft("");
  }, [onChange, value]);

  return (
    <label style={{ display: "flex", flexDirection: "column", gap: 4 }}>
      <div style={{ fontSize: tokens.fontSize.xs, fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em", color: palette.fg.muted }}>{label}</div>
      <div style={{ display: "flex", flexWrap: "wrap", gap: 4, borderRadius: tokens.radius.md, border: `1px solid ${palette.fg.subtle}`, background: palette.bg.darker, padding: 6 }}>
        {value.map((tag) => (
          <span key={tag} style={{ display: "inline-flex", alignItems: "center", gap: 4, borderRadius: tokens.radius.xs, border: `1px solid rgba(102, 217, 239, 0.3)`, background: "rgba(102, 217, 239, 0.1)", padding: "2px 8px", fontSize: tokens.fontSize.xs, color: palette.accent.cyan }}>
            {tag}
            <button type="button" onClick={() => onChange(value.filter((t) => t !== tag))} disabled={disabled} style={{ background: "none", border: "none", color: palette.accent.cyan, cursor: "pointer", padding: 0, fontSize: "14px", lineHeight: 1 }}>×</button>
          </span>
        ))}
        <input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => { if (e.key === "Enter" && draft.trim()) { e.preventDefault(); add(draft); } if (e.key === "Backspace" && !draft && value.length > 0) onChange(value.slice(0, -1)); }}
          disabled={disabled}
          style={{ minWidth: 70, flex: 1, background: "transparent", fontSize: tokens.fontSize.sm, color: palette.fg.default, outline: "none", border: "none" }}
          placeholder="Type + Enter…"
        />
      </div>
      {filtered.length > 0 ? (
        <div style={{ maxHeight: 120, overflow: "auto", borderRadius: tokens.radius.md, border: `1px solid ${palette.fg.subtle}`, background: palette.bg.darker, padding: "4px 0", fontSize: tokens.fontSize.xs }}>
          {filtered.slice(0, 8).map((s) => (
            <button key={s} type="button" onMouseDown={() => add(s)} style={{ width: "100%", padding: "4px 8px", textAlign: "left", color: palette.fg.default, background: "transparent", border: "none", cursor: "pointer" }}>{s}</button>
          ))}
        </div>
      ) : null}
    </label>
  );
}

// ── Agent sidebar item ───────────────────────────────────────────────────────

interface AgentSidebarEntry {
  id: string;
  label: string;
  status: "running" | "idle" | "disabled" | "error" | "unknown";
  triggerKind: string;
  sourceKind: string;
  model?: string;
  lastStatus?: string;
  isContract: boolean;       // true if from contracts list
  isRuntimeJob: boolean;     // true if from event-agent runtime
  enabled: boolean;
}

function statusColor(status: AgentSidebarEntry["status"], palette: ThemePalette): string {
  switch (status) {
    case "running": return palette.accent.green;
    case "idle": return palette.accent.cyan;
    case "disabled": return palette.fg.muted;
    case "error": return palette.accent.red;
    default: return palette.fg.soft;
  }
}

function statusDot(status: AgentSidebarEntry["status"]): string {
  switch (status) {
    case "running": return "●";
    case "idle": return "◐";
    case "disabled": return "○";
    case "error": return "✕";
    default: return "·";
  }
}

// ── Main page ────────────────────────────────────────────────────────────────

export default function ContractsPage() {
  const resolvedTheme = useResolvedTheme();
  const palette = resolvedTheme.palette as ThemePalette;
  const themeColors = resolvedTheme.colors;

  // ── Agent library state ────────────────────────────────────────────────
  const [agentEntries, setAgentEntries] = useState<AgentSidebarEntry[]>([]);
  const [loadingAgents, setLoadingAgents] = useState(true);

  // ── Contract editor state ──────────────────────────────────────────────
  const [contracts, setContracts] = useState<ContractListItem[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);

  const [ednDraft, setEdnDraft] = useState(DEFAULT_CONTRACT_EDN);
  const [lastSavedEdn, setLastSavedEdn] = useState<string | null>(null);
  const [validation, setValidation] = useState<ValidationWithContract | null>(null);

  const [notice, setNotice] = useState<Notice>(null);
  const [error, setError] = useState("");

  const [saving, setSaving] = useState(false);
  const [validating, setValidating] = useState(false);
  const [compiling, setCompiling] = useState(false);

  const [copyTarget, setCopyTarget] = useState("");
  const [showCopy, setShowCopy] = useState(false);

  const [showNormalized, setShowNormalized] = useState(false);
  const [normalizedView, setNormalizedView] = useState<unknown>(null);

  const [showSql, setShowSql] = useState(false);
  const [compiledSql, setCompiledSql] = useState<ContractCompileResult["sql"] | null>(null);

  const [showChat, setShowChat] = useState(true);

  // ── Chat workspace controller ──────────────────────────────────────────
  const chat = useChatWorkspaceController({
    initialShowCanvas: false,
    initialSidebarWidthPx: 420,
    defaultRole: "system_admin",
    sessionIdKey: CHAT_SESSION_ID_KEY,
    scratchpadStorageKey: CHAT_SCRATCHPAD_KEY,
    pinnedContextStorageKey: CHAT_PINNED_KEY,
    sessionStateKey: CHAT_SESSION_STATE_KEY,
    sidebarWidthKey: CHAT_SIDEBAR_WIDTH_KEY,
  });

  const isDirty = lastSavedEdn == null ? ednDraft.trim().length > 0 : ednDraft !== lastSavedEdn;
  const validationErrors = validation?.errors ?? [];

  // ── Load agent library (contracts + runtime jobs) ──────────────────────

  const loadAgentLibrary = useCallback(async () => {
    setLoadingAgents(true);
    try {
      const [contractsResult, agentControl] = await Promise.all([
        listContracts().catch(() => ({ contracts: [] as ContractListItem[] })),
        getEventAgentControl().catch(() => null as EventAgentControlResponse | null),
      ]);

      setContracts(contractsResult.contracts);

      // Build merged entries
      const runtimeJobs = agentControl?.runtime?.jobs ?? [];
      const controlJobs = agentControl?.control?.jobs ?? [];

      const entries: AgentSidebarEntry[] = [];

      // From stored contracts
      for (const c of contractsResult.contracts) {
        const runtimeJob = runtimeJobs.find((j) => j.id === c.id);
        const controlJob = controlJobs.find((j) => j.id === c.id);
        const isRunning = runtimeJob?.running ?? false;
        const isEnabled = controlJob?.enabled ?? c.enabled;

        entries.push({
          id: c.id,
          label: c.id,
          status: isEnabled ? (isRunning ? "running" : "idle") : "disabled",
          triggerKind: controlJob?.trigger?.kind ?? "event",
          sourceKind: controlJob?.source?.kind ?? "unknown",
          model: undefined,  // runtime job doesn't expose model
          lastStatus: runtimeJob?.lastStatus,
          isContract: true,
          isRuntimeJob: !!runtimeJob,
          enabled: isEnabled,
        });
      }

      // Runtime-only jobs (not in contracts yet)
      for (const j of runtimeJobs) {
        if (entries.some((e) => e.id === j.id)) continue;
        entries.push({
          id: j.id,
          label: j.id,
          status: j.running ? "running" : "idle",
          triggerKind: "event",
          sourceKind: "unknown",
          lastStatus: j.lastStatus,
          isContract: false,
          isRuntimeJob: true,
          enabled: true,
        });
      }

      setAgentEntries(entries);

      if (!selectedId && entries.length > 0) {
        setSelectedId(entries[0].id);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoadingAgents(false);
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
      // Pin contract as chat context
      chat.pinContextItem({
        id: `contract:${contractId}`,
        title: contractId,
        path: `/ops/contracts/${contractId}`,
        snippet: result.ednText.slice(0, 240),
        kind: "file",
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setEdnDraft("");
      setLastSavedEdn(null);
      setValidation(null);
      setNormalizedView(null);
      setCompiledSql(null);
    }
  }, []);

  useEffect(() => { void loadAgentLibrary(); }, [loadAgentLibrary]);

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

  // ── Actions ────────────────────────────────────────────────────────────

  const handleValidate = useCallback(async () => {
    setValidating(true); setNotice(null); setError("");
    try {
      const result = await validateContract(ednDraft) as ValidationWithContract;
      setValidation(result);
      if (result.contract) setNormalizedView(result.contract);
      setNotice(result.ok ? { tone: "success", text: "Validation passed." } : { tone: "error", text: `Validation failed: ${result.errors.length} error(s).` });
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally { setValidating(false); }
  }, [ednDraft]);

  const handleSave = useCallback(async () => {
    const explicitId = selectedId ? normalizeId(selectedId) : null;
    const inferredId = parseContractIdFromEdn(ednDraft);
    const contractId = explicitId || inferredId;
    if (!contractId) { setNotice({ tone: "error", text: "Missing contract id." }); return; }

    setSaving(true); setNotice(null); setError("");
    try {
      const result = await saveContract(contractId, ednDraft);
      setSelectedId(contractId);
      setEdnDraft(result.ednText);
      setLastSavedEdn(result.ednText);
      setValidation({ ...result.validation, contract: result.contract });
      setNormalizedView(result.contract);
      setCompiledSql(null);
      setNotice(result.validation.ok ? { tone: "success", text: `Saved ${contractId}.` } : { tone: "error", text: `Saved ${contractId}, but validation has ${result.validation.errors.length} error(s).` });
      await loadAgentLibrary();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally { setSaving(false); }
  }, [ednDraft, loadAgentLibrary, selectedId]);

  const handleCopy = useCallback(async () => {
    if (!selectedId) return;
    const nextId = normalizeId(copyTarget);
    if (!nextId) return;
    setSaving(true); setNotice(null); setError("");
    try {
      const result = await copyContract(selectedId, nextId);
      setNotice({ tone: "success", text: `Copied ${selectedId} → ${nextId}.` });
      setSelectedId(nextId);
      setEdnDraft(result.ednText);
      setLastSavedEdn(result.ednText);
      setValidation({ ...result.validation, contract: result.contract });
      setNormalizedView(result.contract);
      setCopyTarget(""); setShowCopy(false);
      await loadAgentLibrary();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally { setSaving(false); }
  }, [copyTarget, loadAgentLibrary, selectedId]);

  const handleCompile = useCallback(async () => {
    if (!selectedId) return;
    setCompiling(true); setNotice(null); setError("");
    try {
      const result = await compileContract(selectedId);
      if (result.ok) {
        setCompiledSql(result.sql); setShowSql(true);
        setNotice({ tone: "success", text: `Compiled ${selectedId}.` });
      } else {
        setNotice({ tone: "error", text: `Compile failed: ${result.errors?.map((e) => e.message).join(", ") ?? "unknown"}` });
      }
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally { setCompiling(false); }
  }, [selectedId]);

  // ── Metadata form sync from EDN ───────────────────────────────────────

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

  const kindSuggestions = useMemo(() => EVENT_KIND_OPTIONS.map((k) => (k.startsWith(":") ? k.slice(1) : k)), []);

  // ── Selected agent entry ───────────────────────────────────────────────

  const selectedEntry = useMemo(() => agentEntries.find((e) => e.id === selectedId) ?? null, [agentEntries, selectedId]);

  // ── Render ─────────────────────────────────────────────────────────────

  return (
    <div style={{ display: "flex", flex: "1 1 0%", minHeight: 0, background: palette.bg.default, color: palette.fg.default }}>
      {/* ── Left sidebar: Agent browser ──────────────────────────────── */}
      <div style={{
        width: 320, minWidth: 280, borderRight: `1px solid ${palette.fg.subtle}`,
        display: "flex", flexDirection: "column", background: palette.bg.darker,
      }}>
        {/* Sidebar header */}
        <div style={{ padding: "16px 16px 12px", borderBottom: `1px solid ${palette.fg.subtle}` }}>
          <div style={{ fontSize: tokens.fontSize.lg, fontWeight: 600, color: palette.fg.default }}>Contracts</div>
          <div style={{ fontSize: tokens.fontSize.xs, color: palette.fg.muted, marginTop: 4 }}>
            {agentEntries.length} agent{agentEntries.length !== 1 ? "s" : ""} · {contracts.length} contract{contracts.length !== 1 ? "s" : ""}
          </div>
        </div>

        {/* Agent list */}
        <div style={{ flex: 1, overflowY: "auto", padding: "8px 8px" }}>
          {loadingAgents ? (
            <div style={{ fontSize: tokens.fontSize.sm, color: palette.fg.muted, padding: "8px" }}>Loading agents…</div>
          ) : agentEntries.length === 0 ? (
            <div style={{ fontSize: tokens.fontSize.sm, color: palette.fg.muted, padding: "8px" }}>No agents yet. Save a contract to create one.</div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
              {agentEntries.map((entry) => {
                const isSelected = entry.id === selectedId;
                return (
                  <button
                    key={entry.id}
                    type="button"
                    onClick={() => setSelectedId(entry.id)}
                    style={{
                      width: "100%", padding: "10px 12px", textAlign: "left",
                      borderRadius: tokens.radius.md,
                      border: `1px solid ${isSelected ? palette.accent.cyan : "transparent"}`,
                      background: isSelected ? "rgba(102, 217, 239, 0.08)" : "transparent",
                      cursor: "pointer", transition: "background 0.1s, border-color 0.1s",
                    }}
                    onMouseEnter={(e) => {
                      if (!isSelected) (e.currentTarget as HTMLElement).style.background = "rgba(255,255,255,0.03)";
                    }}
                    onMouseLeave={(e) => {
                      if (!isSelected) (e.currentTarget as HTMLElement).style.background = "transparent";
                    }}
                  >
                    <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
                      <div style={{ display: "flex", alignItems: "center", gap: 6, minWidth: 0 }}>
                        <span style={{ color: statusColor(entry.status, palette), fontSize: 10 }}>{statusDot(entry.status)}</span>
                        <span style={{
                          fontSize: tokens.fontSize.sm, fontWeight: 500,
                          color: isSelected ? palette.accent.cyan : palette.fg.default,
                          overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap",
                        }}>
                          {entry.label}
                        </span>
                      </div>
                      <span style={{
                        fontSize: tokens.fontSize.xs, padding: "1px 6px",
                        borderRadius: tokens.radius.xs,
                        background: entry.enabled ? "rgba(166, 226, 46, 0.1)" : "rgba(117, 113, 94, 0.15)",
                        color: entry.enabled ? palette.accent.green : palette.fg.muted,
                      }}>
                        {entry.enabled ? "on" : "off"}
                      </span>
                    </div>
                    <div style={{ display: "flex", gap: 8, marginTop: 4, fontSize: tokens.fontSize.xs, color: palette.fg.muted }}>
                      <span>{entry.triggerKind}</span>
                      <span>·</span>
                      <span>{entry.sourceKind}</span>
                      {entry.model ? (<><span>·</span><span>{entry.model}</span></>) : null}
                    </div>
                    {entry.lastStatus ? (
                      <div style={{ marginTop: 3, fontSize: tokens.fontSize.xs, color: entry.lastStatus === "ok" ? palette.accent.green : palette.accent.orange }}>
                        last: {entry.lastStatus}
                      </div>
                    ) : null}
                  </button>
                );
              })}

              {/* New contract button */}
              <button
                type="button"
                onClick={() => { setSelectedId(null); setEdnDraft(DEFAULT_CONTRACT_EDN); setLastSavedEdn(null); }}
                style={{
                  width: "100%", padding: "10px 12px", textAlign: "center",
                  borderRadius: tokens.radius.md,
                  border: `1px dashed ${palette.fg.subtle}`,
                  background: "transparent", cursor: "pointer",
                  fontSize: tokens.fontSize.sm, color: palette.fg.muted,
                }}
              >
                + New contract
              </button>
            </div>
          )}
        </div>
      </div>

      {/* ── Main content ────────────────────────────────────────────── */}
      <div style={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0, overflow: "hidden" }}>
        {/* Header bar */}
        <div style={{
          padding: "12px 20px", borderBottom: `1px solid ${palette.fg.subtle}`,
          display: "flex", alignItems: "center", justifyContent: "space-between", gap: 16,
          background: palette.bg.darker,
        }}>
          <div style={{ display: "flex", alignItems: "center", gap: 12, minWidth: 0 }}>
            {selectedEntry ? (
              <>
                <span style={{ color: statusColor(selectedEntry.status, palette), fontSize: 14 }}>{statusDot(selectedEntry.status)}</span>
                <span style={{ fontSize: tokens.fontSize.base, fontWeight: 600, color: palette.fg.default }}>{selectedEntry.label}</span>
                <span style={{ fontSize: tokens.fontSize.xs, color: palette.fg.muted }}>
                  {selectedEntry.triggerKind} · {selectedEntry.sourceKind}
                </span>
              </>
            ) : (
              <span style={{ fontSize: tokens.fontSize.base, fontWeight: 600, color: palette.fg.default }}>New contract</span>
            )}
          </div>
          <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
            <button type="button" onClick={() => void loadAgentLibrary()} disabled={loadingAgents}
              style={{ padding: "5px 12px", borderRadius: tokens.radius.sm, border: `1px solid ${palette.fg.subtle}`, background: palette.bg.default, color: palette.fg.soft, fontSize: tokens.fontSize.xs, cursor: "pointer" }}>
              {loadingAgents ? "Loading…" : "Refresh"}
            </button>
            <button type="button" onClick={() => void handleValidate()} disabled={validating}
              style={{ padding: "5px 12px", borderRadius: tokens.radius.sm, border: `1px solid ${palette.accent.green}`, background: "rgba(166, 226, 46, 0.08)", color: palette.accent.green, fontSize: tokens.fontSize.xs, cursor: "pointer" }}>
              {validating ? "Validating…" : "✓ Validate"}
            </button>
            <button type="button" onClick={() => void handleSave()} disabled={saving || !isDirty}
              style={{ padding: "5px 12px", borderRadius: tokens.radius.sm, border: "none", background: palette.accent.cyan, color: palette.bg.default, fontSize: tokens.fontSize.xs, fontWeight: 600, cursor: "pointer", opacity: (saving || !isDirty) ? 0.5 : 1 }}>
              {saving ? "Saving…" : "Save"}
            </button>
            <button type="button" onClick={() => setShowCopy((v) => !v)} disabled={!selectedId}
              style={{ padding: "5px 12px", borderRadius: tokens.radius.sm, border: `1px solid ${palette.fg.subtle}`, background: palette.bg.default, color: palette.fg.soft, fontSize: tokens.fontSize.xs, cursor: "pointer" }}>
              Clone
            </button>
            <button type="button" onClick={() => void handleCompile()} disabled={compiling || !selectedId}
              style={{ padding: "5px 12px", borderRadius: tokens.radius.sm, border: `1px solid rgba(253, 151, 31, 0.3)`, background: "rgba(253, 151, 31, 0.08)", color: palette.accent.orange, fontSize: tokens.fontSize.xs, cursor: "pointer" }}>
              {compiling ? "Compiling…" : "Compile SQL"}
            </button>
            <button type="button" onClick={() => setShowNormalized((v) => !v)}
              style={{ padding: "5px 12px", borderRadius: tokens.radius.sm, border: `1px solid ${palette.fg.subtle}`, background: palette.bg.default, color: palette.fg.soft, fontSize: tokens.fontSize.xs, cursor: "pointer" }}>
              {showNormalized ? "Hide JSON" : "Show JSON"}
            </button>
            <button type="button" onClick={() => setShowChat((v) => !v)}
              style={{ padding: "5px 12px", borderRadius: tokens.radius.sm, border: `1px solid ${showChat ? palette.accent.cyan : palette.fg.subtle}`, background: showChat ? "rgba(102, 217, 239, 0.08)" : palette.bg.default, color: showChat ? palette.accent.cyan : palette.fg.soft, fontSize: tokens.fontSize.xs, cursor: "pointer" }}>
              {showChat ? "✕ Chat" : "💬 Chat"}
            </button>
          </div>
        </div>

        {/* Copy row */}
        {showCopy ? (
          <div style={{ padding: "8px 20px", display: "flex", alignItems: "center", gap: 8, borderBottom: `1px solid ${palette.fg.subtle}`, background: palette.bg.darker }}>
            <span style={{ fontSize: tokens.fontSize.xs, color: palette.fg.muted }}>New ID</span>
            <input
              value={copyTarget}
              onChange={(e) => setCopyTarget(e.target.value)}
              placeholder="new-contract-id"
              style={{ flex: 1, padding: "4px 10px", borderRadius: tokens.radius.sm, border: `1px solid ${palette.fg.subtle}`, background: palette.bg.default, color: palette.fg.default, fontSize: tokens.fontSize.sm, outline: "none" }}
            />
            <button type="button" onClick={() => void handleCopy()} disabled={!copyTarget.trim()}
              style={{ padding: "4px 12px", borderRadius: tokens.radius.sm, border: "none", background: palette.accent.cyan, color: palette.bg.default, fontSize: tokens.fontSize.xs, fontWeight: 600, cursor: "pointer" }}>
              Clone
            </button>
          </div>
        ) : null}

        {/* Main content area: metadata + editor */}
        <div style={{ flex: 1, display: "flex", minHeight: 0, overflow: "hidden" }}>
          {/* Metadata sidebar */}
          <div style={{
            width: 280, minWidth: 240, borderRight: `1px solid ${palette.fg.subtle}`,
            background: palette.bg.darker, padding: "16px 14px", overflowY: "auto",
            display: "flex", flexDirection: "column", gap: 12,
          }}>
            <div style={{ fontSize: tokens.fontSize.xs, fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em", color: palette.fg.muted, marginBottom: 4 }}>
              Metadata
            </div>

            {/* Enabled */}
            <label style={{ display: "flex", alignItems: "center", gap: 8, fontSize: tokens.fontSize.sm, color: palette.fg.default }}>
              <input type="checkbox" checked={enabledToken !== "false"} onChange={(e) => setEdnDraft((current) => replaceSimpleValue(current, "enabled", e.target.checked ? "true" : "false"))} />
              Enabled
            </label>

            <SearchableSelect label="Trigger" value={triggerKindToken.replace(/^:/, "")} onChange={(v) => setEdnDraft((c) => replaceSimpleValue(c, "trigger-kind", `:${v}`))} options={Array.from(TRIGGER_KIND_OPTIONS)} />
            <SearchableSelect label="Source" value={sourceKindToken.replace(/^:/, "")} onChange={(v) => setEdnDraft((c) => replaceSimpleValue(c, "source-kind", `:${v}`))} options={Array.from(SOURCE_KIND_OPTIONS)} />

            <label style={{ display: "flex", flexDirection: "column", gap: 4 }}>
              <div style={{ fontSize: tokens.fontSize.xs, fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em", color: palette.fg.muted }}>Source mode</div>
              <input
                value={sourceModeToken.replace(/^:/, "")}
                onChange={(e) => setEdnDraft((c) => replaceSimpleValue(c, "source-mode", `:${e.target.value}`))}
                style={{ width: "100%", padding: "6px 10px", borderRadius: tokens.radius.md, border: `1px solid ${palette.fg.subtle}`, background: palette.bg.default, color: palette.fg.default, fontSize: tokens.fontSize.sm, outline: "none" }}
              />
            </label>

            <label style={{ display: "flex", flexDirection: "column", gap: 4 }}>
              <div style={{ fontSize: tokens.fontSize.xs, fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em", color: palette.fg.muted }}>Cadence (min)</div>
              <input
                type="number" min={1}
                value={cadenceToken.replace(/[^0-9]/g, "") || "1"}
                onChange={(e) => setEdnDraft((c) => replaceSimpleValue(c, "cadence-min", String(Math.max(1, Number(e.target.value || 1)))))}
                style={{ width: "100%", padding: "6px 10px", borderRadius: tokens.radius.md, border: `1px solid ${palette.fg.subtle}`, background: palette.bg.default, color: palette.fg.default, fontSize: tokens.fontSize.sm, outline: "none" }}
              />
            </label>

            <SearchableSelect label="Role" value={roleToken.replace(/^:/, "").replace(/"/g, "")} onChange={(v) => setEdnDraft((c) => replaceAgentValue(c, "role", `:${v}`))} options={ROLE_OPTIONS} />
            <SearchableSelect label="Model" value={modelToken.replace(/^"|"$/g, "")} onChange={(v) => setEdnDraft((c) => replaceAgentValue(c, "model", `"${v}"`))} options={MODEL_OPTIONS} />
            <SearchableSelect label="Thinking" value={thinkingToken.replace(/^:/, "")} onChange={(v) => setEdnDraft((c) => replaceAgentValue(c, "thinking", `:${v}`))} options={Array.from(THINKING_OPTIONS)} />

            <TagInput label="Events (always)" value={eventsAlways} onChange={(next) => setEdnDraft((c) => setKeywordVector(c, ":always", next))} suggestions={kindSuggestions} />
            <TagInput label="Events (maybe)" value={eventsMaybe} onChange={(next) => setEdnDraft((c) => setKeywordVector(c, ":maybe", next))} suggestions={kindSuggestions} />

            {/* Validation badge */}
            {validation ? (
              <div style={{
                padding: "6px 10px", borderRadius: tokens.radius.md, fontSize: tokens.fontSize.xs,
                border: `1px solid ${validation.ok ? "rgba(166, 226, 46, 0.3)" : "rgba(249, 38, 114, 0.3)"}`,
                background: validation.ok ? "rgba(166, 226, 46, 0.08)" : "rgba(249, 38, 114, 0.08)",
                color: validation.ok ? palette.accent.green : palette.accent.red,
              }}>
                {validation.ok ? "✓ Valid" : `✕ ${validation.errors.length} error(s)`}
              </div>
            ) : null}

            {/* Dirty indicator */}
            {isDirty ? (
              <div style={{ fontSize: tokens.fontSize.xs, color: palette.accent.orange, padding: "4px 0" }}>
                ● unsaved changes
              </div>
            ) : null}
          </div>

          {/* Editor + results */}
          <div style={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0, overflow: "hidden" }}>
            <div style={{ flex: 1, minHeight: 0 }}>
              <EdnEditor
                value={ednDraft}
                onChange={setEdnDraft}
                height="100%"
                placeholder="Enter EDN contract…"
                externalErrors={validationErrors.map((e) => ({
                  message: e.path.length ? `${e.path.join(".")}: ${e.message}` : e.message,
                }))}
                onValidate={() => void handleValidate()}
                fileName={selectedId ?? "new-contract.edn"}
              />
            </div>

            {/* Bottom panels: SQL / normalized / notices */}
            <div style={{ flexShrink: 0 }}>
              {showSql && compiledSql ? (
                <div style={{ borderTop: `1px solid ${palette.fg.subtle}`, padding: "12px 16px", background: palette.bg.darker }}>
                  <div style={{ fontSize: tokens.fontSize.xs, fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em", color: palette.fg.muted, marginBottom: 8 }}>SQL projections</div>
                  <pre style={{ maxHeight: 200, overflow: "auto", padding: 12, borderRadius: tokens.radius.md, background: palette.bg.default, fontSize: tokens.fontSize.xs, color: palette.accent.green }}>{prettyJson(compiledSql)}</pre>
                </div>
              ) : null}

              {showNormalized ? (
                <div style={{ borderTop: `1px solid ${palette.fg.subtle}`, padding: "12px 16px", background: palette.bg.darker }}>
                  <div style={{ fontSize: tokens.fontSize.xs, fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em", color: palette.fg.muted, marginBottom: 8 }}>Normalized view</div>
                  <pre style={{ maxHeight: 200, overflow: "auto", padding: 12, borderRadius: tokens.radius.md, background: palette.bg.default, fontSize: tokens.fontSize.xs, color: palette.fg.soft }}>{prettyJson(normalizedView)}</pre>
                </div>
              ) : null}

              {notice ? (
                <div style={{
                  padding: "8px 16px", fontSize: tokens.fontSize.sm,
                  borderTop: `1px solid ${notice.tone === "success" ? "rgba(166, 226, 46, 0.3)" : "rgba(249, 38, 114, 0.3)"}`,
                  background: notice.tone === "success" ? "rgba(166, 226, 46, 0.06)" : "rgba(249, 38, 114, 0.06)",
                  color: notice.tone === "success" ? palette.accent.green : palette.accent.red,
                }}>
                  {notice.text}
                </div>
              ) : null}

              {error ? (
                <div style={{
                  padding: "8px 16px", fontSize: tokens.fontSize.sm,
                  borderTop: `1px solid rgba(249, 38, 114, 0.3)`,
                  background: "rgba(249, 38, 114, 0.06)", color: palette.accent.red,
                }}>
                  {error}
                </div>
              ) : null}
            </div>
          </div>
        </div>
      </div>

      {/* ── Right sidebar: Chat workspace ─────────────────────────────── */}
      {showChat ? (
        <aside
          style={{
            width: 460,
            minWidth: 380,
            borderLeft: `1px solid ${palette.fg.subtle}`,
            display: "flex",
            minHeight: 0,
            overflow: "hidden",
            background: palette.bg.darker,
          }}
        >
          <ChatWorkspacePane
            controller={chat}
            showFiles={false}
            showCanvasToggle={false}
            onShowFiles={() => {}}
            onOpenHydrationSource={(source) => {
              // Hydration sources from chat — could open a contract
              const contractId = source.path.split("/").pop() ?? source.path;
              if (contractId) setSelectedId(contractId);
            }}
            onOpenSourceInPreview={(source: AgentSource) => {
              const contractId = source.url.split("/").pop() ?? source.url;
              if (contractId) setSelectedId(contractId);
            }}
          />
        </aside>
      ) : null}
    </div>
  );
}
