import { useEffect, useMemo, useRef, useState } from "react";
import ChatComposer from "../components/ChatComposer";
import ConsolePanel from "../components/ConsolePanel";
import LoungePanel from "../components/LoungePanel";
import ModelSelector from "../components/ModelSelector";
import SettingsPanel, { QUICK_PRESETS } from "../components/SettingsPanel";
import StatsPanel from "../components/StatsPanel";
import {
  createChatRun,
  getFrontendConfig,
  listLoungeMessages,
  listModels,
  listProxxModels,
  postLoungeMessage,
  proxxChat,
  proxxHealth,
  knoxxChat,
  knoxxHealth,
  serverHealth,
  serverStatus,
  startServer,
  stopServer,
  warmupServer
} from "../lib/api";
import type { ChatMessage, ChatProvider, FrontendConfig, LoungeMessage, ModelInfo, ProxxModelInfo, SamplingSettings } from "../lib/types";
import { connectStream } from "../lib/ws";

const DEFAULT_SETTINGS: SamplingSettings = { ...QUICK_PRESETS.Balanced };
const PROMPT_HISTORY_KEY = "llm_lab_prompt_history";
const PRESETS_KEY = "llm_lab_custom_presets";
const CONSOLE_HEIGHT_KEY = "llm_lab_console_height_pct";
const SESSION_ID_KEY = "llm_lab_session_id";
const LOUNGE_COLLAPSED_KEY = "llm_lab_lounge_collapsed";
const LOUNGE_ALIAS_KEY = "llm_lab_lounge_alias";
const LAYOUT_MODE_KEY = "llm_lab_layout_mode";
const CHAT_PROVIDER_KEY = "llm_lab_chat_provider";

type LayoutMode = "default" | "chat-right" | "console-right";

interface CustomPreset {
  name: string;
  systemPrompt: string;
  settings: SamplingSettings;
  runtime: {
    ctxSize: number;
    gpuLayers: number;
    threads: number;
    batchSize: number;
    ubatchSize: number;
    flashAttention: boolean;
    mmap: boolean;
    mlock: boolean;
    serverPort: number;
    extraArgsText: string;
    reasoningEnabled: boolean;
    reasoningBudget: number;
  };
}

function makeId(): string {
  const maybeCrypto = globalThis.crypto as Crypto | undefined;
  if (maybeCrypto && typeof maybeCrypto.randomUUID === "function") {
    return maybeCrypto.randomUUID();
  }
  return `id-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function parseExtraArgs(text: string): string[] {
  return text
    .split(" ")
    .map((v) => v.trim())
    .filter(Boolean);
}

function resolveExternalUrl(rawUrl: string): string {
  try {
    const parsed = new URL(rawUrl);
    const localHosts = new Set(["localhost", "127.0.0.1", "::1"]);
    if (!localHosts.has(parsed.hostname)) {
      return rawUrl;
    }
    const next = new URL(rawUrl);
    next.hostname = window.location.hostname;
    next.protocol = window.location.protocol;
    return next.toString();
  } catch {
    return rawUrl;
  }
}

function ChatLabPage() {
  const [models, setModels] = useState<ModelInfo[]>([]);
  const [selectedModelPath, setSelectedModelPath] = useState("");
  const [systemPrompt, setSystemPrompt] = useState("You are a careful assistant.");
  const [settings, setSettings] = useState<SamplingSettings>(DEFAULT_SETTINGS);
  const [ctxSize, setCtxSize] = useState(8192);
  const [gpuLayers, setGpuLayers] = useState(99);
  const [threads, setThreads] = useState(8);
  const [batchSize, setBatchSize] = useState(512);
  const [ubatchSize, setUbatchSize] = useState(512);
  const [flashAttention, setFlashAttention] = useState(true);
  const [mmap, setMmap] = useState(true);
  const [mlock, setMlock] = useState(false);
  const [serverPort, setServerPort] = useState(8081);
  const [extraArgsText, setExtraArgsText] = useState("");
  const [reasoningEnabled, setReasoningEnabled] = useState(true);
  const [reasoningBudget, setReasoningBudget] = useState(-1);
  const [promptHistory, setPromptHistory] = useState<string[]>([]);
  const [customPresets, setCustomPresets] = useState<CustomPreset[]>([]);
  const [lastAppliedRuntimeSig, setLastAppliedRuntimeSig] = useState("");
  const [lastRunInputTokens, setLastRunInputTokens] = useState(0);
  const [consoleHeightPct, setConsoleHeightPct] = useState(35);
  const [sessionId, setSessionId] = useState("");
  const [loungeMessages, setLoungeMessages] = useState<LoungeMessage[]>([]);
  const [loungeCollapsed, setLoungeCollapsed] = useState(false);
  const [loungeAlias, setLoungeAlias] = useState("");
  const [layoutMode, setLayoutMode] = useState<LayoutMode>("default");
  const [chatProvider, setChatProvider] = useState<ChatProvider>("local");
  const [knoxxConversationId, setKnoxxConversationId] = useState<string | null>(null);
  const [knoxxDirectConversationId, setKnoxxDirectConversationId] = useState<string | null>(null);
  const [frontendConfig, setFrontendConfig] = useState<FrontendConfig | null>(null);
  const [knoxxReachable, setKnoxxReachable] = useState(false);
  const [knoxxConfigured, setKnoxxConfigured] = useState(false);
  // Proxx state
  const [proxxModels, setProxxModels] = useState<ProxxModelInfo[]>([]);
  const [selectedProxxModel, setSelectedProxxModel] = useState("");
  const [proxxReachable, setProxxReachable] = useState(false);
  const [proxxConfigured, setProxxConfigured] = useState(false);
  // RAG state
  const [ragEnabled, setRagEnabled] = useState(true);
  const [ragCollection, setRagCollection] = useState("devel_docs");
  const [ragLimit, setRagLimit] = useState(5);
  const [ragThreshold, setRagThreshold] = useState(0.6);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [stats, setStats] = useState<Record<string, unknown>>({});
  const [statsHistory, setStatsHistory] = useState<{
    cpu: number[];
    ram: number[];
    gpu: number[];
    rssGb: number[];
  }>({ cpu: [], ram: [], gpu: [], rssGb: [] });
  const [consoleLines, setConsoleLines] = useState<string[]>([]);
  const [serverRunning, setServerRunning] = useState(false);
  const [serverHealthy, setServerHealthy] = useState(false);
  const [activeRunId, setActiveRunId] = useState<string | null>(null);
  const [isSending, setIsSending] = useState(false);
  const [wsStatus, setWsStatus] = useState<"connected" | "closed" | "error" | "connecting">("connecting");
  const sendTimeoutRef = useRef<number | null>(null);
  const activeRunIdRef = useRef<string | null>(null);

  useEffect(() => {
    activeRunIdRef.current = activeRunId;
  }, [activeRunId]);

  useEffect(() => {
    void listModels().then((items) => {
      setModels(items);
      if (!selectedModelPath && items.length > 0) {
        setSelectedModelPath(items[0].path);
        if (items[0].suggested_ctx) {
          setCtxSize(items[0].suggested_ctx);
        }
      }
    });
    void serverStatus()
      .then((s) => setServerRunning(Boolean(s.running)))
      .catch(() => setServerRunning(false));
  }, [selectedModelPath]);

  useEffect(() => {
    void listProxxModels().then((items) => {
      setProxxModels(items);
      if (!selectedProxxModel && items.length > 0) {
        setSelectedProxxModel(items[0].id);
      }
    }).catch(() => setProxxModels([]));
  }, []);

  useEffect(() => {
    let timer: number | null = null;

    const poll = async () => {
      try {
        const h = await serverHealth();
        setServerRunning(Boolean(h.running));
        setServerHealthy(Boolean(h.running && h.healthy));
      } catch {
        setServerHealthy(false);
      }
    };

    void poll();
    timer = window.setInterval(() => {
      void poll();
    }, 2000);

    return () => {
      if (timer !== null) {
        window.clearInterval(timer);
      }
    };
  }, []);

  useEffect(() => {
    try {
      const rawCollapsed = localStorage.getItem(LOUNGE_COLLAPSED_KEY);
      if (rawCollapsed === "true") setLoungeCollapsed(true);
      const rawAlias = localStorage.getItem(LOUNGE_ALIAS_KEY);
      if (rawAlias) setLoungeAlias(rawAlias);
      const rawLayout = localStorage.getItem(LAYOUT_MODE_KEY);
      if (rawLayout === "default" || rawLayout === "chat-right" || rawLayout === "console-right") {
        setLayoutMode(rawLayout);
      }
      const rawProvider = localStorage.getItem(CHAT_PROVIDER_KEY);
      if (rawProvider === "local" || rawProvider === "knoxx-rag" || rawProvider === "knoxx-direct") {
        setChatProvider(rawProvider);
      }
    } catch {
      // ignore
    }

    void listLoungeMessages()
      .then((items) => setLoungeMessages(items.slice(-200)))
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    localStorage.setItem(LOUNGE_COLLAPSED_KEY, String(loungeCollapsed));
  }, [loungeCollapsed]);

  useEffect(() => {
    localStorage.setItem(LOUNGE_ALIAS_KEY, loungeAlias);
  }, [loungeAlias]);

  useEffect(() => {
    localStorage.setItem(LAYOUT_MODE_KEY, layoutMode);
  }, [layoutMode]);

  useEffect(() => {
    localStorage.setItem(CHAT_PROVIDER_KEY, chatProvider);
  }, [chatProvider]);

  useEffect(() => {
    void getFrontendConfig()
      .then((cfg) => {
        setFrontendConfig(cfg);
        setKnoxxConfigured(cfg.knoxx_enabled);
      })
      .catch(() => {
        setFrontendConfig(null);
        setKnoxxConfigured(false);
      });
  }, []);

  useEffect(() => {
    let timer: number | null = null;
    const poll = async () => {
      try {
        const status = await proxxHealth();
        setProxxReachable(Boolean(status.reachable));
        setProxxConfigured(Boolean(status.configured));
      } catch {
        setProxxReachable(false);
      }
    };
    void poll();
    timer = window.setInterval(poll, 5000);
    return () => {
      if (timer !== null) {
        window.clearInterval(timer);
      }
    };
  }, []);

  useEffect(() => {
    let timer: number | null = null;
    const poll = async () => {
      try {
        const status = await knoxxHealth();
        setKnoxxReachable(Boolean(status.reachable));
        setKnoxxConfigured(Boolean(status.configured));
      } catch {
        setKnoxxReachable(false);
      }
    };
    void poll();
    timer = window.setInterval(() => {
      void poll();
    }, 5000);

    return () => {
      if (timer !== null) {
        window.clearInterval(timer);
      }
    };
  }, []);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(CONSOLE_HEIGHT_KEY);
      if (!raw) return;
      const parsed = Number(raw);
      if (!Number.isNaN(parsed) && parsed >= 20 && parsed <= 70) {
        setConsoleHeightPct(parsed);
      }
    } catch {
      // ignore
    }
  }, []);

  useEffect(() => {
    localStorage.setItem(CONSOLE_HEIGHT_KEY, String(consoleHeightPct));
  }, [consoleHeightPct]);

  useEffect(() => {
    try {
      let sid = localStorage.getItem(SESSION_ID_KEY) || "";
      if (!sid) {
        sid = makeId();
        localStorage.setItem(SESSION_ID_KEY, sid);
      }
      setSessionId(sid);
    } catch {
      setSessionId(makeId());
    }
  }, []);

  useEffect(() => {
    try {
      const rawHistory = localStorage.getItem(PROMPT_HISTORY_KEY);
      if (rawHistory) {
        const parsed = JSON.parse(rawHistory) as string[];
        setPromptHistory(Array.isArray(parsed) ? parsed.slice(0, 30) : []);
      }
    } catch {
      setPromptHistory([]);
    }

    try {
      const rawPresets = localStorage.getItem(PRESETS_KEY);
      if (rawPresets) {
        const parsed = JSON.parse(rawPresets) as CustomPreset[];
        setCustomPresets(Array.isArray(parsed) ? parsed : []);
      }
    } catch {
      setCustomPresets([]);
    }
  }, []);

  useEffect(() => {
    const disconnect = connectStream({
      onStatus: (status) => {
        setWsStatus(status);
        if (status !== "connected") {
          setIsSending(false);
        }
      },
      onStats: (snapshot) => {
        setStats(snapshot);
        const cpu = Number(snapshot.cpu_percent ?? 0);
        const ram = Number(snapshot.memory_percent ?? 0);
        const gpuList = Array.isArray(snapshot.gpu) ? (snapshot.gpu as Array<Record<string, unknown>>) : [];
        const gpu = gpuList.length > 0 ? Number(gpuList[0].util_gpu ?? 0) : 0;
        const llama = (snapshot.llama as Record<string, unknown> | undefined) ?? {};
        const rssGb = Number(llama.rss_bytes ?? 0) / (1024 ** 3);

        setStatsHistory((prev) => ({
          cpu: [...prev.cpu.slice(-59), cpu],
          ram: [...prev.ram.slice(-59), ram],
          gpu: [...prev.gpu.slice(-59), gpu],
          rssGb: [...prev.rssGb.slice(-59), rssGb]
        }));
      },
      onConsole: (line) => setConsoleLines((prev) => [...prev.slice(-400), line]),
      onToken: (token, runId) => {
        const currentRun = activeRunIdRef.current;
        if (currentRun && runId && runId !== currentRun) {
          return;
        }
        setMessages((prev) => {
          if (prev.length === 0 || prev[prev.length - 1].role !== "assistant") {
            return [...prev, { id: makeId(), role: "assistant", content: token }];
          }
          const copy = [...prev];
          copy[copy.length - 1] = {
            ...copy[copy.length - 1],
            content: copy[copy.length - 1].content + token
          };
          return copy;
        });
      },
      onEvent: (event) => {
        const name = String(event.event ?? "");
        if (name === "run_started") {
          setActiveRunId(String(event.run_id ?? ""));
          setConsoleLines((prev) => [...prev.slice(-400), `[run] started: ${String(event.run_id ?? "")}`]);
        }
        if (name === "run_finished") {
          setIsSending(false);
          const input = Number(event.input_tokens ?? NaN);
          if (!Number.isNaN(input) && input >= 0) {
            setLastRunInputTokens(input);
          }
        }
        if (name === "run_completed" || name === "run_failed") {
          setIsSending(false);
        }
        setConsoleLines((prev) => [...prev.slice(-400), `[event] ${JSON.stringify(event)}`]);
      },
      onLounge: (message) => {
        const m = message as unknown as LoungeMessage;
        if (!m || !m.id) return;
        setLoungeMessages((prev) => {
          if (prev.some((p) => p.id === m.id)) return prev;
          return [...prev.slice(-199), m];
        });
      }
    }, sessionId || undefined);
    return disconnect;
  }, [sessionId]);

  useEffect(() => {
    if (!isSending) {
      if (sendTimeoutRef.current !== null) {
        window.clearTimeout(sendTimeoutRef.current);
        sendTimeoutRef.current = null;
      }
      return;
    }

    sendTimeoutRef.current = window.setTimeout(() => {
      setIsSending(false);
      setConsoleLines((prev) => [
        ...prev.slice(-400),
        "[run] timeout waiting for completion event; UI unlocked"
      ]);
    }, 45000);

    return () => {
      if (sendTimeoutRef.current !== null) {
        window.clearTimeout(sendTimeoutRef.current);
        sendTimeoutRef.current = null;
      }
    };
  }, [isSending]);

  const selected = useMemo(
    () => models.find((m) => m.path === selectedModelPath),
    [models, selectedModelPath]
  );

  const kvEstimateGb = useMemo(() => {
    const modelGb = selected ? selected.size_bytes / (1024 ** 3) : 4;
    return ((ctxSize / 8192) * (modelGb * 0.35)).toFixed(2);
  }, [ctxSize, selected]);

  const estimatedConversationTokens = useMemo(() => {
    const text = [systemPrompt, ...messages.map((m) => `${m.role}: ${m.content}`)].join("\n");
    return Math.max(1, Math.round(text.length / 4));
  }, [messages, systemPrompt]);

  const contextUsedTokens = Math.max(lastRunInputTokens, estimatedConversationTokens);
  const contextFillPercent = (contextUsedTokens / Math.max(1, ctxSize)) * 100;

  const runtimeSignature = useMemo(
    () =>
      JSON.stringify({
        model: selectedModelPath,
        ctxSize,
        gpuLayers,
        threads,
        batchSize,
        ubatchSize,
        flashAttention,
        mmap,
        mlock,
        serverPort,
        extraArgsText,
        reasoningEnabled,
        reasoningBudget
      }),
    [
      selectedModelPath,
      ctxSize,
      gpuLayers,
      threads,
      batchSize,
      ubatchSize,
      flashAttention,
      mmap,
      mlock,
      serverPort,
      extraArgsText,
      reasoningEnabled,
      reasoningBudget
    ]
  );

  const runtimeDirty = lastAppliedRuntimeSig !== "" && runtimeSignature !== lastAppliedRuntimeSig;
  const middleHeightPct = 100 - consoleHeightPct;
  const chatRight = layoutMode === "chat-right";
  const consoleRight = layoutMode === "console-right";

  async function handleServerStart() {
    if (!selectedModelPath) return;
    try {
      const extraArgs = parseExtraArgs(extraArgsText);

      extraArgs.push("--reasoning-format", reasoningEnabled ? "deepseek" : "none");
      extraArgs.push("--reasoning-budget", String(reasoningEnabled ? reasoningBudget : 0));

      await startServer({
        model_path: selectedModelPath,
        port: serverPort,
        ctx_size: ctxSize,
        gpu_layers: gpuLayers,
        threads,
        batch_size: batchSize,
        ubatch_size: ubatchSize,
        flash_attention: flashAttention,
        mmap,
        mlock,
        extra_args: extraArgs
      });
      setServerRunning(true);
      setServerHealthy(false);
      setLastAppliedRuntimeSig(runtimeSignature);
      setConsoleLines((prev) => [...prev.slice(-400), "[server] started"]);
    } catch (error) {
      setConsoleLines((prev) => [...prev.slice(-400), `[server] start failed: ${(error as Error).message}`]);
    }
  }

  async function handleServerStop() {
    try {
      await stopServer();
      setServerRunning(false);
      setServerHealthy(false);
      setConsoleLines((prev) => [...prev.slice(-400), "[server] stopped"]);
    } catch (error) {
      setConsoleLines((prev) => [...prev.slice(-400), `[server] stop failed: ${(error as Error).message}`]);
    }
  }

  async function handleWarmup() {
    try {
      const resp = await warmupServer("Reply with one short sentence.");
      setConsoleLines((prev) => [...prev.slice(-400), `[warmup] ${resp.latency_ms.toFixed(1)} ms`]);
    } catch (error) {
      setConsoleLines((prev) => [...prev.slice(-400), `[warmup] failed: ${(error as Error).message}`]);
    }
  }

  async function handleSend(text: string) {
    if (!sessionId) {
      setConsoleLines((prev) => [...prev.slice(-400), "[chat] session not ready, retry in a second"]);
      return;
    }
    const userMessage: ChatMessage = { id: makeId(), role: "user", content: text };
    setMessages((prev) => [...prev, userMessage]);
    setIsSending(true);

    try {
      if (chatProvider === "proxx") {
        if (!selectedProxxModel) {
          throw new Error("Select a Proxx model first");
        }
        const response = await proxxChat({
          model: selectedProxxModel,
          system_prompt: systemPrompt,
          messages: [...messages, userMessage].map((m) => ({ role: m.role, content: m.content })),
          temperature: settings.temperature,
          top_p: settings.top_p,
          max_tokens: settings.max_tokens,
          stop: settings.stop_sequences,
          rag_enabled: ragEnabled,
          rag_collection: ragCollection,
          rag_limit: ragLimit,
          rag_threshold: ragThreshold,
        });
        setMessages((prev) => [...prev, { id: makeId(), role: "assistant", content: response.answer }]);
        setIsSending(false);
        setConsoleLines((prev) => [...prev.slice(-400), `[proxx] model=${response.model ?? selectedProxxModel}${response.rag_context ? ` rag=${response.rag_context.length} chunks` : ""}`]);
        return;
      }

      if (chatProvider === "knoxx-rag" || chatProvider === "knoxx-direct") {
        const direct = chatProvider === "knoxx-direct";
        const response = await knoxxChat({
          message: text,
          conversation_id: direct ? (knoxxDirectConversationId ?? undefined) : (knoxxConversationId ?? undefined),
          direct
        });
        if (direct) {
          setKnoxxDirectConversationId(response.conversation_id ?? null);
        } else {
          setKnoxxConversationId(response.conversation_id ?? null);
        }
        setMessages((prev) => [...prev, { id: makeId(), role: "assistant", content: response.answer }]);
        setIsSending(false);
        setConsoleLines((prev) => [...prev.slice(-400), `[knoxx] ${direct ? "direct" : "rag"} response received`]);
        return;
      }

      const run = await createChatRun({
        model: selected?.name,
        system_prompt: systemPrompt,
        messages: [...messages, userMessage].map((m) => ({ role: m.role, content: m.content })),
        temperature: settings.temperature,
        top_p: settings.top_p,
        top_k: settings.top_k,
        min_p: settings.min_p,
        repeat_penalty: settings.repeat_penalty,
        presence_penalty: settings.presence_penalty,
        frequency_penalty: settings.frequency_penalty,
        seed: settings.seed,
        max_tokens: settings.max_tokens,
        stop: settings.stop_sequences,
        metadata: {
          session_id: sessionId,
          model_path: selectedModelPath,
          ctx_size: ctxSize,
          kv_estimate_gb: kvEstimateGb
        }
      });
      setActiveRunId(run.run_id);
      setConsoleLines((prev) => [...prev.slice(-400), `[run] queued: ${run.run_id}`]);
    } catch (error) {
      setIsSending(false);
      setConsoleLines((prev) => [...prev.slice(-400), `[run] failed: ${(error as Error).message}`]);
    }
  }

  function handleNewChat() {
    setMessages([]);
    setActiveRunId(null);
    setKnoxxConversationId(null);
    setKnoxxDirectConversationId(null);
    setIsSending(false);
    setConsoleLines((prev) => [...prev.slice(-400), "[chat] new chat started"]);
  }

  function handleSavePromptVersion() {
    const next = [systemPrompt, ...promptHistory.filter((p) => p !== systemPrompt)].slice(0, 30);
    setPromptHistory(next);
    localStorage.setItem(PROMPT_HISTORY_KEY, JSON.stringify(next));
    setConsoleLines((prev) => [...prev.slice(-400), "[prompt] saved to history"]);
  }

  function handleLoadPromptVersion(prompt: string) {
    onSystemPromptChangeSafe(prompt);
    setConsoleLines((prev) => [...prev.slice(-400), "[prompt] loaded from history"]);
  }

  function onSystemPromptChangeSafe(prompt: string) {
    setSystemPrompt(prompt);
  }

  function handleSavePreset(name: string) {
    const trimmed = name.trim();
    if (!trimmed) {
      return;
    }
    const preset: CustomPreset = {
      name: trimmed,
      systemPrompt,
      settings,
      runtime: {
        ctxSize,
        gpuLayers,
        threads,
        batchSize,
        ubatchSize,
        flashAttention,
        mmap,
        mlock,
        serverPort,
        extraArgsText,
        reasoningEnabled,
        reasoningBudget
      }
    };
    const next = [preset, ...customPresets.filter((p) => p.name !== trimmed)].slice(0, 50);
    setCustomPresets(next);
    localStorage.setItem(PRESETS_KEY, JSON.stringify(next));
    setConsoleLines((prev) => [...prev.slice(-400), `[preset] saved: ${trimmed}`]);
  }

  function handleLoadPreset(name: string) {
    const preset = customPresets.find((p) => p.name === name);
    if (!preset) return;
    setSystemPrompt(preset.systemPrompt);
    setSettings(preset.settings);
    setCtxSize(preset.runtime.ctxSize);
    setGpuLayers(preset.runtime.gpuLayers);
    setThreads(preset.runtime.threads);
    setBatchSize(preset.runtime.batchSize);
    setUbatchSize(preset.runtime.ubatchSize);
    setFlashAttention(preset.runtime.flashAttention);
    setMmap(preset.runtime.mmap);
    setMlock(preset.runtime.mlock);
    setServerPort(preset.runtime.serverPort);
    setExtraArgsText(preset.runtime.extraArgsText);
    setReasoningEnabled(preset.runtime.reasoningEnabled);
    setReasoningBudget(preset.runtime.reasoningBudget);
    setConsoleLines((prev) => [...prev.slice(-400), `[preset] loaded: ${name}`]);
  }

  function handleDeletePreset(name: string) {
    const next = customPresets.filter((p) => p.name !== name);
    setCustomPresets(next);
    localStorage.setItem(PRESETS_KEY, JSON.stringify(next));
    setConsoleLines((prev) => [...prev.slice(-400), `[preset] deleted: ${name}`]);
  }

  async function handleSendLounge(text: string) {
    if (!sessionId) return;
    try {
      await postLoungeMessage({
        session_id: sessionId,
        alias: loungeAlias || undefined,
        text,
      });
    } catch (error) {
      setConsoleLines((prev) => [...prev.slice(-400), `[lounge] failed: ${(error as Error).message}`]);
    }
  }

  return (
    <div className="flex h-[calc(100vh-96px)] flex-col gap-4">
      <div>
        <ModelSelector
          models={models}
          value={selectedModelPath}
          onChange={setSelectedModelPath}
          serverRunning={serverRunning}
          onStart={handleServerStart}
          onStop={handleServerStop}
          onWarmup={handleWarmup}
          ctxSize={ctxSize}
          onCtxSizeChange={setCtxSize}
          gpuLayers={gpuLayers}
          onGpuLayersChange={setGpuLayers}
          threads={threads}
          onThreadsChange={setThreads}
          batchSize={batchSize}
          onBatchSizeChange={setBatchSize}
          contextUsedTokens={contextUsedTokens}
          contextFillPercent={contextFillPercent}
        />
        <div className="mt-2 flex items-center gap-2 text-xs">
          <span className={runtimeDirty ? "text-amber-700" : "text-emerald-700"}>
            {runtimeDirty ? "Runtime settings changed (not applied)" : "Runtime settings in sync"}
          </span>
          {runtimeDirty ? <button className="btn-ghost" onClick={handleServerStart}>Restart With New Runtime</button> : null}
          <div className="ml-auto flex items-center gap-2">
            <span className="text-slate-500">Provider</span>
            <select
              className="input h-8 py-0 text-xs"
              value={chatProvider}
              onChange={(e) => setChatProvider(e.target.value as ChatProvider)}
            >
              <option value="proxx">Proxx</option>
              <option value="knoxx-rag">Knoxx RAG</option>
              <option value="knoxx-direct">Knoxx Direct</option>
            </select>
            {chatProvider === "proxx" ? (
              <>
                <span className="text-slate-500">Model</span>
                <select
                  className="input h-8 max-w-[22rem] py-0 text-xs"
                  value={selectedProxxModel}
                  onChange={(e) => setSelectedProxxModel(e.target.value)}
                >
                  {proxxModels.length === 0 ? <option value="">No Proxx models</option> : null}
                  {proxxModels.map((model) => (
                    <option key={model.id} value={model.id}>{model.id}</option>
                  ))}
                </select>
                <label className="flex items-center gap-1 text-xs">
                  <input
                    type="checkbox"
                    checked={ragEnabled}
                    onChange={(e) => setRagEnabled(e.target.checked)}
                    className="h-3 w-3"
                  />
                  <span>RAG</span>
                </label>
                {ragEnabled ? (
                  <>
                    <select
                      className="input h-8 w-28 py-0 text-xs"
                      value={ragCollection}
                      onChange={(e) => setRagCollection(e.target.value)}
                    >
                      <option value="devel_docs">devel_docs</option>
                      <option value="test_docs">test_docs</option>
                    </select>
                    <input
                      type="number"
                      min={1}
                      max={20}
                      value={ragLimit}
                      onChange={(e) => setRagLimit(parseInt(e.target.value) || 5)}
                      className="input h-8 w-12 py-0 text-xs"
                      title="Max RAG results"
                    />
                  </>
                ) : null}
                <span className={proxxReachable ? "text-emerald-700" : "text-amber-700"}>
                  Proxx {proxxReachable ? "online" : proxxConfigured ? "offline" : "not configured"}
                </span>
              </>
            ) : (
              <span className={knoxxReachable ? "text-emerald-700" : "text-amber-700"}>
                Knoxx {knoxxReachable ? "online" : knoxxConfigured ? "offline" : "not configured"}
              </span>
            )}
            <span className="text-slate-500">Layout</span>
            <select
              className="input h-8 py-0 text-xs"
              value={layoutMode}
              onChange={(e) => setLayoutMode(e.target.value as LayoutMode)}
            >
              <option value="default">Default</option>
              <option value="chat-right">Chat Right</option>
              <option value="console-right">Console Right</option>
            </select>
          </div>
        </div>
      </div>

      <div
        className="grid min-h-0 grid-cols-1 gap-4 lg:grid-cols-12"
        style={{ flex: `0 0 ${middleHeightPct}%` }}
      >
      <div className={`min-h-0 lg:col-span-3 ${chatRight && !consoleRight ? "lg:order-1" : ""}`}>
        <SettingsPanel
          systemPrompt={systemPrompt}
          onSystemPromptChange={onSystemPromptChangeSafe}
          settings={settings}
          onChange={setSettings}
          reasoningEnabled={reasoningEnabled}
          reasoningBudget={reasoningBudget}
          onReasoningEnabledChange={setReasoningEnabled}
          onReasoningBudgetChange={setReasoningBudget}
          flashAttention={flashAttention}
          mmap={mmap}
          mlock={mlock}
          ubatchSize={ubatchSize}
          serverPort={serverPort}
          extraArgs={extraArgsText}
          onFlashAttentionChange={setFlashAttention}
          onMmapChange={setMmap}
          onMlockChange={setMlock}
          onUbatchSizeChange={setUbatchSize}
          onServerPortChange={setServerPort}
          onExtraArgsChange={setExtraArgsText}
          promptHistory={promptHistory}
          onSavePromptVersion={handleSavePromptVersion}
          onLoadPromptVersion={handleLoadPromptVersion}
          customPresets={customPresets.map((p) => p.name)}
          onSavePreset={handleSavePreset}
          onLoadPreset={handleLoadPreset}
          onDeletePreset={handleDeletePreset}
        />
      </div>

      <div className={`min-h-0 lg:col-span-6 ${chatRight && !consoleRight ? "lg:order-3" : ""}`}>
        <section className="panel flex h-full flex-col">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="panel-title">Transcript</h2>
            <button className="btn-ghost" onClick={handleNewChat}>New Chat</button>
          </div>
          <p className="mb-2 text-xs text-slate-500">
            Mode: {chatProvider === "proxx" ? "Proxx" : chatProvider === "knoxx-rag" ? "Knoxx RAG" : "Knoxx Direct"}
          </p>
          <div className="min-h-72 flex-1 space-y-3 overflow-auto rounded-md bg-slate-50 p-3">
            {messages.length === 0 ? (
              <p className="text-sm text-slate-500">No conversation yet.</p>
            ) : (
              messages.map((message) => (
                <article
                  key={message.id}
                  className={`rounded-md border px-3 py-2 text-sm ${
                    message.role === "user" ? "border-teal-200 bg-teal-50" : "border-slate-200 bg-white"
                  }`}
                >
                  <p className="mb-1 text-xs uppercase tracking-wide text-slate-500">{message.role}</p>
                  <p className="whitespace-pre-wrap">{message.content}</p>
                </article>
              ))
            )}
          </div>
          <ChatComposer
            onSend={handleSend}
            isSending={isSending || (chatProvider === "proxx" ? (!proxxReachable || !selectedProxxModel) : !knoxxReachable)}
          />
          {chatProvider === "proxx" && !proxxConfigured ? <p className="mt-2 text-xs text-amber-700">Proxx is not configured in backend env (set PROXX_AUTH_TOKEN).</p> : null}
          {chatProvider === "proxx" && proxxConfigured && !proxxReachable ? <p className="mt-2 text-xs text-amber-700">Proxx is unreachable at configured base URL.</p> : null}
          {(chatProvider === "knoxx-rag" || chatProvider === "knoxx-direct") && !knoxxConfigured ? <p className="mt-2 text-xs text-amber-700">Knoxx is not configured in backend env (set KNOXX_API_KEY).</p> : null}
          {(chatProvider === "knoxx-rag" || chatProvider === "knoxx-direct") && knoxxConfigured && !knoxxReachable ? <p className="mt-2 text-xs text-amber-700">Knoxx is unreachable at configured base URL.</p> : null}
          {frontendConfig?.knoxx_admin_url ? (
            <a className="mt-2 inline-block text-xs text-teal-700 hover:underline" href={resolveExternalUrl(frontendConfig.knoxx_admin_url)} target="_blank" rel="noreferrer">
              Open Knoxx Admin
            </a>
          ) : null}
        </section>
      </div>

      <div className={`min-h-0 lg:col-span-3 ${chatRight && !consoleRight ? "lg:order-2" : ""}`}>
        {consoleRight ? (
          <ConsolePanel lines={consoleLines} />
        ) : (
          <StatsPanel
            stats={stats}
            status={wsStatus}
            history={statsHistory}
            contextTokens={ctxSize}
            kvEstimateGb={kvEstimateGb}
          />
        )}
      </div>
      </div>

      {consoleRight ? (
        <div className="min-h-0" style={{ flex: "0 0 20%" }}>
          <StatsPanel
            stats={stats}
            status={wsStatus}
            history={statsHistory}
            contextTokens={ctxSize}
            kvEstimateGb={kvEstimateGb}
          />
        </div>
      ) : null}

      <div className="min-h-0" style={{ flex: loungeCollapsed ? "0 0 auto" : "0 0 24%" }}>
        <LoungePanel
          collapsed={loungeCollapsed}
          onToggle={() => setLoungeCollapsed((v) => !v)}
          messages={loungeMessages}
          alias={loungeAlias}
          onAliasChange={setLoungeAlias}
          onSend={handleSendLounge}
        />
      </div>

      {!consoleRight ? (
        <>
          <div className="-mt-1 flex items-center gap-2 px-1">
            <span className="text-[11px] text-slate-500">Console size</span>
            <input
              className="w-56"
              type="range"
              min={20}
              max={70}
              step={1}
              value={consoleHeightPct}
              onChange={(e) => setConsoleHeightPct(Number(e.target.value))}
            />
            <span className="text-[11px] text-slate-600">{consoleHeightPct}%</span>
          </div>

          <div style={{ flex: `0 0 ${consoleHeightPct}%` }}>
            <ConsolePanel lines={consoleLines} />
          </div>
        </>
      ) : null}
    </div>
  );
}

export default ChatLabPage;
