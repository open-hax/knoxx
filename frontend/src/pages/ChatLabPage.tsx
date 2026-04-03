import { useEffect, useMemo, useRef, useState } from "react";
import { Button, Card, Badge, Input } from "@devel/ui-react";
import ChatComposer from "../components/ChatComposer";
import ConsolePanel from "../components/ConsolePanel";
import LoungePanel from "../components/LoungePanel";
import {
  createChatRun,
  getFrontendConfig,
  listLoungeMessages,
  listProxxModels,
  postLoungeMessage,
  proxxChat,
  proxxHealth,
  knoxxChat,
  knoxxHealth,
} from "../lib/api";
import type { ChatMessage, ChatProvider, FrontendConfig, LoungeMessage, ProxxModelInfo, SamplingSettings } from "../lib/types";
import { connectStream } from "../lib/ws";

const PROMPT_HISTORY_KEY = "knoxx_prompt_history";
const CONSOLE_HEIGHT_KEY = "knoxx_console_height_pct";
const SESSION_ID_KEY = "knoxx_session_id";
const LOUNGE_COLLAPSED_KEY = "knoxx_lounge_collapsed";
const LOUNGE_ALIAS_KEY = "knoxx_lounge_alias";
const LAYOUT_MODE_KEY = "knoxx_layout_mode";
const CHAT_PROVIDER_KEY = "knoxx_chat_provider";

type LayoutMode = "default" | "chat-right" | "console-right";

function makeId(): string {
  const maybeCrypto = globalThis.crypto as Crypto | undefined;
  if (maybeCrypto && typeof maybeCrypto.randomUUID === "function") {
    return maybeCrypto.randomUUID();
  }
  return `id-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
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
  const [systemPrompt, setSystemPrompt] = useState("You are a careful assistant.");
  const [settings, setSettings] = useState<SamplingSettings>({
    temperature: 0.7,
    top_p: 0.9,
    top_k: 40,
    min_p: 0.0,
    repeat_penalty: 1.1,
    presence_penalty: 0.0,
    frequency_penalty: 0.0,
    seed: null,
    max_tokens: 2048,
    stop_sequences: [],
  });
  const [promptHistory, setPromptHistory] = useState<string[]>([]);
  const [consoleHeightPct, setConsoleHeightPct] = useState(35);
  const [sessionId, setSessionId] = useState("");
  const [loungeMessages, setLoungeMessages] = useState<LoungeMessage[]>([]);
  const [loungeCollapsed, setLoungeCollapsed] = useState(false);
  const [loungeAlias, setLoungeAlias] = useState("");
  const [layoutMode, setLayoutMode] = useState<LayoutMode>("default");
  const [chatProvider, setChatProvider] = useState<ChatProvider>("proxx");
  const [knoxxConversationId, setKnoxxConversationId] = useState<string | null>(null);
  const [knoxxDirectConversationId, setKnoxxDirectConversationId] = useState<string | null>(null);
  const [frontendConfig, setFrontendConfig] = useState<FrontendConfig | null>(null);
  const [knoxxReachable, setKnoxxReachable] = useState(false);
  const [knoxxConfigured, setKnoxxConfigured] = useState(false);
  const [proxxModels, setProxxModels] = useState<ProxxModelInfo[]>([]);
  const [selectedProxxModel, setSelectedProxxModel] = useState("");
  const [proxxReachable, setProxxReachable] = useState(false);
  const [proxxConfigured, setProxxConfigured] = useState(false);
  const [ragEnabled, setRagEnabled] = useState(true);
  const [ragCollection, setRagCollection] = useState("devel_docs");
  const [ragLimit, setRagLimit] = useState(5);
  const [ragThreshold, setRagThreshold] = useState(0.6);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [consoleLines, setConsoleLines] = useState<string[]>([]);
  const [activeRunId, setActiveRunId] = useState<string | null>(null);
  const [isSending, setIsSending] = useState(false);
  const [wsStatus, setWsStatus] = useState<"connected" | "closed" | "error" | "connecting">("connecting");
  const sendTimeoutRef = useRef<number | null>(null);
  const activeRunIdRef = useRef<string | null>(null);

  useEffect(() => {
    activeRunIdRef.current = activeRunId;
  }, [activeRunId]);

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
      if (rawProvider === "proxx" || rawProvider === "knoxx-rag" || rawProvider === "knoxx-direct") {
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
        if (status.model_count && proxxModels.length === 0) {
          void listProxxModels().then((items) => {
            setProxxModels(items);
            if (!selectedProxxModel && items.length > 0) {
              setSelectedProxxModel(items[0].id);
            }
          }).catch(() => setProxxModels([]));
        }
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
  }, []);

  useEffect(() => {
    const disconnect = connectStream({
      onStatus: (status) => {
        setWsStatus(status);
        if (status !== "connected") {
          setIsSending(false);
        }
      },
      onToken: (_token, runId) => {
        const currentRun = activeRunIdRef.current;
        if (currentRun && runId && runId !== currentRun) {
          return;
        }
      },
      onEvent: (event) => {
        const name = String(event.event ?? "");
        if (name === "run_started") {
          setActiveRunId(String(event.run_id ?? ""));
          setConsoleLines((prev) => [...prev.slice(-400), `[run] started: ${String(event.run_id ?? "")}`]);
        }
        if (name === "run_finished") {
          setIsSending(false);
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

  const chatRight = layoutMode === "chat-right";
  const consoleRight = layoutMode === "console-right";
  const middleHeightPct = 100 - consoleHeightPct;

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
        system_prompt: systemPrompt,
        messages: [...messages, userMessage].map((m) => ({ role: m.role, content: m.content })),
        temperature: settings.temperature,
        top_p: settings.top_p,
        max_tokens: settings.max_tokens,
        stop: settings.stop_sequences,
        metadata: { session_id: sessionId }
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
    setSystemPrompt(prompt);
    setConsoleLines((prev) => [...prev.slice(-400), "[prompt] loaded from history"]);
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
    <div style={{ display: "flex", flexDirection: "column", height: "calc(100vh - 96px)", gap: 16 }}>
      <Card variant="default" padding="sm">
        <div style={{ display: "flex", alignItems: "center", gap: 12, flexWrap: "wrap" }}>
          <span style={{ fontSize: 12, color: "#64748b" }}>Provider</span>
          <select
            value={chatProvider}
            onChange={(e) => setChatProvider(e.target.value as ChatProvider)}
            style={{ borderRadius: 6, border: "1px solid #d1d5db", padding: "4px 8px", fontSize: 12 }}
          >
            <option value="proxx">Proxx</option>
            <option value="knoxx-rag">Knoxx RAG</option>
            <option value="knoxx-direct">Knoxx Direct</option>
          </select>
          {chatProvider === "proxx" ? (
            <>
              <span style={{ fontSize: 12, color: "#64748b" }}>Model</span>
              <select
                value={selectedProxxModel}
                onChange={(e) => setSelectedProxxModel(e.target.value)}
                style={{ borderRadius: 6, border: "1px solid #d1d5db", padding: "4px 8px", fontSize: 12, maxWidth: 352 }}
              >
                {proxxModels.length === 0 ? <option value="">No Proxx models</option> : null}
                {proxxModels.map((model) => (
                  <option key={model.id} value={model.id}>{model.id}</option>
                ))}
              </select>
              <label style={{ display: "flex", alignItems: "center", gap: 4, fontSize: 12 }}>
                <input
                  type="checkbox"
                  checked={ragEnabled}
                  onChange={(e) => setRagEnabled(e.target.checked)}
                  style={{ height: 12, width: 12 }}
                />
                <span>RAG</span>
              </label>
              {ragEnabled ? (
                <>
                  <select
                    value={ragCollection}
                    onChange={(e) => setRagCollection(e.target.value)}
                    style={{ borderRadius: 6, border: "1px solid #d1d5db", padding: "4px 8px", fontSize: 12, width: 112 }}
                  >
                    <option value="devel_docs">devel_docs</option>
                    <option value="test_docs">test_docs</option>
                  </select>
                </>
              ) : null}
              <Badge variant={proxxReachable ? "success" : proxxConfigured ? "warning" : "error"} size="sm" dot>
                Proxx {proxxReachable ? "online" : proxxConfigured ? "offline" : "not configured"}
              </Badge>
            </>
          ) : (
            <Badge variant={knoxxReachable ? "success" : knoxxConfigured ? "warning" : "error"} size="sm" dot>
              Knoxx {knoxxReachable ? "online" : knoxxConfigured ? "offline" : "not configured"}
            </Badge>
          )}
          <span style={{ fontSize: 12, color: "#64748b" }}>Layout</span>
          <select
            value={layoutMode}
            onChange={(e) => setLayoutMode(e.target.value as LayoutMode)}
            style={{ borderRadius: 6, border: "1px solid #d1d5db", padding: "4px 8px", fontSize: 12 }}
          >
            <option value="default">Default</option>
            <option value="chat-right">Chat Right</option>
            <option value="console-right">Console Right</option>
          </select>
        </div>
      </Card>

      <div
        style={{ display: "grid", gridTemplateColumns: "1fr", minHeight: 0, gap: 16, flex: `0 0 ${middleHeightPct}%` }}
      >
        <div style={{ minWidth: 0, gridColumn: chatRight && !consoleRight ? "1" : undefined, order: chatRight && !consoleRight ? 1 : undefined }}>
          <Card variant="default" padding="md" style={{ display: "flex", flexDirection: "column", height: "100%" }}>
            <div style={{ marginBottom: 12 }}>
              <label style={{ display: "block", fontSize: 12, fontWeight: 500, marginBottom: 4 }}>System Prompt</label>
              <textarea
                value={systemPrompt}
                onChange={(e) => setSystemPrompt(e.target.value)}
                rows={3}
                style={{ width: "100%", borderRadius: 6, border: "1px solid #d1d5db", padding: 8, fontSize: 14, resize: "vertical" }}
              />
              <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
                <Button variant="secondary" size="sm" onClick={handleSavePromptVersion}>Save Prompt</Button>
                {promptHistory.length > 0 && (
                  <select
                    onChange={(e) => { if (e.target.value) handleLoadPromptVersion(e.target.value); e.target.value = ""; }}
                    style={{ borderRadius: 6, border: "1px solid #d1d5db", padding: "2px 8px", fontSize: 12 }}
                    defaultValue=""
                  >
                    <option value="" disabled>Load from history...</option>
                    {promptHistory.map((p, i) => (
                      <option key={i} value={p}>{p.slice(0, 60)}{p.length > 60 ? "..." : ""}</option>
                    ))}
                  </select>
                )}
              </div>
            </div>
            <div style={{ display: "flex", gap: 8, marginTop: 12 }}>
              <div style={{ flex: 1 }}>
                <label style={{ display: "block", fontSize: 12, fontWeight: 500, marginBottom: 4 }}>Temperature</label>
                <Input type="number" value={String(settings.temperature)} onChange={(e) => setSettings({ ...settings, temperature: Number(e.target.value) || 0.7 })} size="sm" />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: "block", fontSize: 12, fontWeight: 500, marginBottom: 4 }}>Max Tokens</label>
                <Input type="number" value={String(settings.max_tokens)} onChange={(e) => setSettings({ ...settings, max_tokens: Number(e.target.value) || 2048 })} size="sm" />
              </div>
              <div style={{ flex: 1 }}>
                <label style={{ display: "block", fontSize: 12, fontWeight: 500, marginBottom: 4 }}>Top P</label>
                <Input type="number" value={String(settings.top_p)} onChange={(e) => setSettings({ ...settings, top_p: Number(e.target.value) || 0.9 })} size="sm" />
              </div>
            </div>
          </Card>
        </div>

        <div style={{ minWidth: 0, order: chatRight && !consoleRight ? 3 : undefined }}>
          <Card variant="default" padding="md" style={{ display: "flex", flexDirection: "column", height: "100%" }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 8 }}>
              <h2 style={{ fontSize: 16, fontWeight: 600 }}>Transcript</h2>
              <Button variant="ghost" size="sm" onClick={handleNewChat}>New Chat</Button>
            </div>
            <p style={{ fontSize: 12, color: "#64748b", marginBottom: 8 }}>
              Mode: {chatProvider === "proxx" ? "Proxx" : chatProvider === "knoxx-rag" ? "Knoxx RAG" : "Knoxx Direct"}
            </p>
            <div style={{ minHeight: 288, flex: 1, overflow: "auto", borderRadius: 6, background: "#f8fafc", padding: 12 }}>
              {messages.length === 0 ? (
                <p style={{ fontSize: 14, color: "#64748b" }}>No conversation yet.</p>
              ) : (
                <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
                  {messages.map((message) => (
                    <Card
                      key={message.id}
                      variant="outlined"
                      padding="sm"
                      style={{
                        borderColor: message.role === "user" ? "#99f6e4" : "#e2e8f0",
                        background: message.role === "user" ? "#f0fdfa" : "#ffffff",
                      }}
                    >
                      <p style={{ fontSize: 11, fontWeight: 500, textTransform: "uppercase", letterSpacing: "0.05em", color: "#64748b", marginBottom: 4 }}>{message.role}</p>
                      <p style={{ fontSize: 14, whiteSpace: "pre-wrap" }}>{message.content}</p>
                    </Card>
                  ))}
                </div>
              )}
            </div>
            <ChatComposer
              onSend={handleSend}
              isSending={isSending || (chatProvider === "proxx" ? (!proxxReachable || !selectedProxxModel) : !knoxxReachable)}
            />
            {chatProvider === "proxx" && !proxxConfigured && <p style={{ marginTop: 8, fontSize: 12, color: "#b45309" }}>Proxx is not configured in backend env (set PROXX_AUTH_TOKEN).</p>}
            {chatProvider === "proxx" && proxxConfigured && !proxxReachable && <p style={{ marginTop: 8, fontSize: 12, color: "#b45309" }}>Proxx is unreachable at configured base URL.</p>}
            {(chatProvider === "knoxx-rag" || chatProvider === "knoxx-direct") && !knoxxConfigured && <p style={{ marginTop: 8, fontSize: 12, color: "#b45309" }}>Knoxx is not configured in backend env (set KNOXX_API_KEY).</p>}
            {(chatProvider === "knoxx-rag" || chatProvider === "knoxx-direct") && knoxxConfigured && !knoxxReachable && <p style={{ marginTop: 8, fontSize: 12, color: "#b45309" }}>Knoxx is unreachable at configured base URL.</p>}
            {frontendConfig?.knoxx_admin_url ? (
              <a style={{ marginTop: 8, display: "inline-block", fontSize: 12, color: "#0d9488", textDecoration: "underline" }} href={resolveExternalUrl(frontendConfig.knoxx_admin_url)} target="_blank" rel="noreferrer">
                Open Knoxx Admin
              </a>
            ) : null}
          </Card>
        </div>

        <div style={{ minWidth: 0, order: chatRight && !consoleRight ? 2 : undefined }}>
          {consoleRight ? (
            <ConsolePanel lines={consoleLines} />
          ) : (
            <Card variant="default" padding="md">
              <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 8 }}>WebSocket</h3>
              <Badge variant={wsStatus === "connected" ? "success" : wsStatus === "error" ? "error" : "warning"} size="sm" dot>
                {wsStatus}
              </Badge>
            </Card>
          )}
        </div>
      </div>

      {consoleRight ? (
        <div style={{ minWidth: 0, flex: "0 0 20%" }}>
          <Card variant="default" padding="md">
            <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 8 }}>WebSocket</h3>
            <Badge variant={wsStatus === "connected" ? "success" : wsStatus === "error" ? "error" : "warning"} size="sm" dot>
              {wsStatus}
            </Badge>
          </Card>
        </div>
      ) : null}

      <div style={{ minWidth: 0, flex: loungeCollapsed ? "0 0 auto" : "0 0 24%" }}>
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
          <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "0 4px", marginTop: -4 }}>
            <span style={{ fontSize: 11, color: "#64748b" }}>Console size</span>
            <input
              type="range"
              min={20}
              max={70}
              step={1}
              value={consoleHeightPct}
              onChange={(e) => setConsoleHeightPct(Number(e.target.value))}
              style={{ width: 224 }}
            />
            <span style={{ fontSize: 11, color: "#475569" }}>{consoleHeightPct}%</span>
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
