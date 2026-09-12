import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useResolvedTheme, tokens } from "@open-hax/uxx";
import {
  copyContract, DEFAULT_CONTRACT_EDN, getContract, listContracts, mergeContractEntries,
  saveContract, validateContract, type AgentContract, type ContractListItem,
  type ContractSidebarEntry, type ContractValidationResult,
} from "../lib/api/contracts";
import { getEventAgentControl, type EventAgentControlResponse } from "../lib/api/admin";
import { useChatWorkspaceController } from "../components/chat-page/useChatWorkspaceController";
import type { ContractsClass } from "../lib/types";

const CHAT_SESSION_ID_KEY = "knoxx_contracts_session_id";
const CHAT_SCRATCHPAD_KEY = "knoxx_contracts_scratchpad_state";
const CHAT_PINNED_KEY = "knoxx_contracts_pinned_context";
const CHAT_SESSION_STATE_KEY = "knoxx_contracts_chat_session_state";
const CHAT_SIDEBAR_WIDTH_KEY = "knoxx_contracts_sidebar_width_px";
const AUTO_FOCUS_CONTRACT_KEY = "knoxx_contracts_auto_focus_contract";

const LEFT_PANEL_OPEN_KEY = "knoxx_contracts_left_panel_open";

const CHAT_DOCK_BREAKPOINT_PX = 1100;

function useNarrowLayout(breakpointPx: number): boolean {
  const [isNarrow, setIsNarrow] = useState(() => {
    if (typeof window === "undefined") return false;
    return window.innerWidth <= breakpointPx;
  });

  useEffect(() => {
    if (typeof window === "undefined") return;
    const media = window.matchMedia(`(max-width: ${breakpointPx}px)`);
    const onChange = (evt: MediaQueryListEvent) => setIsNarrow(evt.matches);

    setIsNarrow(media.matches);

    // Safari fallback
    if (typeof media.addEventListener === "function") {
      media.addEventListener("change", onChange);
      return () => media.removeEventListener("change", onChange);
    }

    media.addListener(onChange);
    return () => media.removeListener(onChange);
  }, [breakpointPx]);

  return isNarrow;
}

// ── Helpers ──────────────────────────────────────────────────────────────────

type Notice = { tone: "success" | "error"; text: string } | null;
type ValidationWithContract = ContractValidationResult & { contract?: AgentContract | null };

function normalizeId(value: string): string {
  return value.trim();
}

function parseContractIdFromEdn(ednText: string): string | null {
  const match = ednText.match(/:contract\/id\s+"?([^"\s}]+)"?/);
  return match?.[1] ?? null;
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

function ednTokenToInputValue(token: string | null): string {
  if (!token) return "";
  return token.trim().replace(/^:/, "").replace(/^"|"$/g, "");
}

function stringToken(value: string): string {
  return JSON.stringify(value);
}

function keywordToken(value: string): string {
  const normalized = value.trim().replace(/^:/, "");
  return normalized ? `:${normalized}` : ":agent";
}

export function useContractsController() {
  const resolvedTheme = useResolvedTheme();

  // ── Agent library state ────────────────────────────────────────────────
  const [agentEntries, setAgentEntries] = useState<ContractSidebarEntry[]>([]);
  const [loadingAgents, setLoadingAgents] = useState(true);

  // ── Contract editor state ──────────────────────────────────────────────
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [selectedContractClass, setSelectedContractClass] = useState<ContractsClass>("agents");

  const [ednDraft, setEdnDraft] = useState(DEFAULT_CONTRACT_EDN);
  const [lastSavedEdn, setLastSavedEdn] = useState<string | null>(null);
  const [validation, setValidation] = useState<ValidationWithContract | null>(null);

  const [notice, setNotice] = useState<Notice>(null);
  const [error, setError] = useState("");

  const [saving, setSaving] = useState(false);
  const [validating, setValidating] = useState(false);

  const [copyTarget, setCopyTarget] = useState("");
  const [showCopy, setShowCopy] = useState(false);

  const [showNormalized, setShowNormalized] = useState(false);
  const [normalizedView, setNormalizedView] = useState<unknown>(null);

  const [showChat, setShowChat] = useState(true);

  const isNarrow = useNarrowLayout(CHAT_DOCK_BREAKPOINT_PX);

  const [showLeftPanel, setShowLeftPanel] = useState(() => {
    const stored = localStorage.getItem(LEFT_PANEL_OPEN_KEY);
    return stored !== null ? stored === "true" : true;
  });

  const toggleLeftPanel = useCallback(() => {
    setShowLeftPanel((prev) => {
      const next = !prev;
      localStorage.setItem(LEFT_PANEL_OPEN_KEY, String(next));
      return next;
    });
  }, []);

  // ── Auto-focus toggle: when ON, contract agent interactions switch the editor focus ──
  const [autoFocusContract, setAutoFocusContract] = useState(() => {
    const stored = localStorage.getItem(AUTO_FOCUS_CONTRACT_KEY);
    return stored !== null ? stored === "true" : true; // default ON
  });
  const toggleAutoFocus = useCallback(() => {
    setAutoFocusContract((prev) => {
      const next = !prev;
      localStorage.setItem(AUTO_FOCUS_CONTRACT_KEY, String(next));
      return next;
    });
  }, []);

  // ── Chat workspace controller (contract librarian persona) ───────────
  const chat = useChatWorkspaceController({
    initialShowCanvas: false,
    initialSidebarWidthPx: 420,
    defaultRole: "contract_librarian",
    defaultActorId: "contract_librarian",
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

      const dedupedEntries = mergeContractEntries(contractsResult.contracts, agentControl);
      setAgentEntries(dedupedEntries);

      if (!selectedId && dedupedEntries.length > 0) {
        setSelectedId(dedupedEntries[0].id);
        setSelectedContractClass(dedupedEntries[0].contractClass);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoadingAgents(false);
    }
  }, [selectedId]);

  const loadContract = useCallback(async (contractId: string, contractClass: ContractsClass) => {
    setError("");
    try {
      const result = await getContract(contractId, contractClass);
      setEdnDraft(result.ednText);
      setLastSavedEdn(result.ednText);
      setValidation({ ...result.validation, contract: result.contract });
      setNormalizedView(result.contract);
      // Pin contract as chat context
      chat.pinContextItem({
        id: `contract:${contractId}`,
        title: contractId,
        path: `/ops/contracts/${contractClass}/${contractId}`,
        snippet: result.ednText.slice(0, 240),
        kind: "file",
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setEdnDraft("");
      setLastSavedEdn(null);
      setValidation(null);
      setNormalizedView(null);
    }
  }, []);

  useEffect(() => { void loadAgentLibrary(); }, [loadAgentLibrary]);

  useEffect(() => {
    if (selectedId) {
      void loadContract(selectedId, selectedContractClass);
    } else {
      setEdnDraft(DEFAULT_CONTRACT_EDN);
      setLastSavedEdn(null);
      setValidation(null);
      setNormalizedView(null);
    }
  }, [selectedId, selectedContractClass, loadContract]);

  // ── Left panel search + folder state ────────────────────────────────
  const [searchQuery, setSearchQuery] = useState("");
  const [collapsedFolders, setCollapsedFolders] = useState<Set<string>>(new Set());
  const toggleFolder = useCallback((folder: string) => {
    setCollapsedFolders((prev) => {
      const next = new Set(prev);
      if (next.has(folder)) next.delete(folder); else next.add(folder);
      return next;
    });
  }, []);
  const isSearching = searchQuery.trim().length > 0;

  const filteredContracts = useMemo(() => {
    const q = searchQuery.toLowerCase();
    const groups = new Map<string, ContractSidebarEntry[]>();
    for (const entry of agentEntries) {
      if (q && !entry.id.toLowerCase().includes(q)) continue;
      const folder = entry.contractClass;
      if (!groups.has(folder)) groups.set(folder, []);
      groups.get(folder)!.push(entry);
    }
    return Array.from(groups.entries()).sort(([a], [b]) => a.localeCompare(b));
  }, [agentEntries, searchQuery]);

  // ── Actions ────────────────────────────────────────────────────────────

  const handleValidate = useCallback(async () => {
    setValidating(true); setNotice(null); setError("");
    try {
      const result = await validateContract(ednDraft, selectedContractClass) as ValidationWithContract;
      setValidation(result);
      if (result.contract) setNormalizedView(result.contract);
      setNotice(result.ok ? { tone: "success", text: "Validation passed." } : { tone: "error", text: `Validation failed: ${result.errors.length} error(s).` });
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally { setValidating(false); }
  }, [ednDraft, selectedContractClass]);

  const handleSave = useCallback(async () => {
    const explicitId = selectedId ? normalizeId(selectedId) : null;
    const inferredId = parseContractIdFromEdn(ednDraft);
    const contractId = inferredId || explicitId;
    if (!contractId) { setNotice({ tone: "error", text: "Missing contract id." }); return; }

    setSaving(true); setNotice(null); setError("");
    try {
      const result = await saveContract(contractId, ednDraft, selectedContractClass);
      setSelectedId(contractId);
      setSelectedContractClass(selectedContractClass);
      setEdnDraft(result.ednText);
      setLastSavedEdn(result.ednText);
      setValidation({ ...result.validation, contract: result.contract });
      setNormalizedView(result.contract);
      setNotice(result.validation.ok ? { tone: "success", text: `Saved ${contractId}.` } : { tone: "error", text: `Saved ${contractId}, but validation has ${result.validation.errors.length} error(s).` });
      await loadAgentLibrary();
    } catch (err) {
      setNotice({ tone: "error", text: err instanceof Error ? err.message : String(err) });
    } finally { setSaving(false); }
  }, [ednDraft, loadAgentLibrary, selectedContractClass, selectedId]);

  const handleCopy = useCallback(async () => {
    if (!selectedId) return;
    const nextId = normalizeId(copyTarget);
    if (!nextId) return;
    setSaving(true); setNotice(null); setError("");
    try {
      const result = await copyContract(selectedId, nextId, selectedContractClass);
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
  }, [copyTarget, loadAgentLibrary, selectedContractClass, selectedId]);

  // ── Metadata form sync from EDN ───────────────────────────────────────

  const contractIdValue = ednTokenToInputValue(extractSimpleValue(ednDraft, "contract/id")) || selectedId || "new-agent";
  const contractKindValue = ednTokenToInputValue(extractSimpleValue(ednDraft, "contract/kind")) || selectedContractClass.slice(0, -1) || "agent";
  const contractVersionValue = ednTokenToInputValue(extractSimpleValue(ednDraft, "contract/version")) || "1";
  const enabledToken = extractSimpleValue(ednDraft, "enabled");
  // ── Selected agent entry ───────────────────────────────────────────────

  const selectedEntry = useMemo(() => agentEntries.find((e) => e.id === selectedId && e.contractClass === selectedContractClass) ?? null, [agentEntries, selectedContractClass, selectedId]);

  const state = {
    palette: resolvedTheme.palette, tokens, "agent-entries": agentEntries,
    "loading-agents": loadingAgents, "selected-id": selectedId, "selected-class": selectedContractClass,
    "edn-draft": ednDraft, "validation-errors": validationErrors, notice, error, saving, validating,
    "copy-target": copyTarget, "show-copy": showCopy, "show-normalized": showNormalized,
    "normalized-json": JSON.stringify(normalizedView ?? null, null, 2), "show-chat": showChat,
    "is-narrow": isNarrow, "show-left-panel": showLeftPanel, "auto-focus": autoFocusContract,
    "is-dirty": isDirty, "search-query": searchQuery, "collapsed-folders": Array.from(collapsedFolders),
    "is-searching": isSearching, "filtered-contracts": filteredContracts, "selected-entry": selectedEntry,
    "contract-id": contractIdValue, "contract-kind": contractKindValue,
    "contract-version": contractVersionValue, "enabled": enabledToken !== "false",
  };
  const actions = {
    "toggle-left-panel": toggleLeftPanel, "toggle-auto-focus": toggleAutoFocus,
    "refresh": loadAgentLibrary, "validate": handleValidate, "save": handleSave, "copy": handleCopy,
    "set-draft": setEdnDraft, "set-search": setSearchQuery, "toggle-folder": toggleFolder,
    "set-copy-target": setCopyTarget, "toggle-copy": () => setShowCopy((value) => !value),
    "toggle-normalized": () => setShowNormalized((value) => !value),
    "toggle-chat": () => setShowChat((value) => !value),
    "select-contract": (id: string, contractClass: ContractsClass) => {
      setSelectedId(id); setSelectedContractClass(contractClass);
      if (isNarrow) setShowLeftPanel(false);
    },
    "new-contract": () => {
      setSelectedId(null); setSelectedContractClass("agents"); setEdnDraft(DEFAULT_CONTRACT_EDN);
      setLastSavedEdn(null); if (isNarrow) setShowLeftPanel(false);
    },
    "update-field": (field: "id" | "kind" | "version" | "enabled", value: string | boolean) => {
      const key = field === "enabled" ? field : `contract/${field}`;
      const token = field === "id" ? stringToken(String(value)) : field === "kind" ? keywordToken(String(value))
        : field === "version" ? String(Math.max(1, Number(value || 1))) : value ? "true" : "false";
      setEdnDraft((current) => replaceSimpleValue(current, key, token));
    },
    "open-source": (path: string) => {
      if (autoFocusContract) {
        const id = path.split("/").pop() ?? path;
        if (id) { setSelectedId(id); setSelectedContractClass("agents"); }
      }
    },
  };
  return { state, actions, chat };
}

// The existing app bridge owns the React/controller instance; shadow-cljs supplies
// the visible view. No hidden legacy metadata panel is mounted as a fallback.
export default function ContractsPage({ render }: {
  render: (controller: ReturnType<typeof useContractsController>) => React.ReactNode;
}) {
  return render(useContractsController());
}
