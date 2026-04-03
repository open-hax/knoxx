import { useEffect, useRef, useState } from "react";
import { Button, Card, Badge, Input } from "@devel/ui-react";
import ChatComposer from "../components/ChatComposer";
import ConsolePanel from "../components/ConsolePanel";
import LoungePanel from "../components/LoungePanel";
import {
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

const SESSION_ID_KEY = "knoxx_session_id";
const LOUNGE_COLLAPSED_KEY = "knoxx_lounge_collapsed";
const LOUNGE_ALIAS_KEY = "knoxx_lounge_alias";
const CHAT_PROVIDER_KEY = "knoxx_chat_provider";

function makeId(): string {
  const maybeCrypto = globalThis.crypto as Crypto | undefined;
  if (maybeCrypto && typeof maybeCrypto.randomUUID === "function") {
    return maybeCrypto.randomUUID();
  }
  return `id-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function ChatPage() {
  const [systemPrompt, setSystemPrompt] = useState("You are a careful assistant.");
  const [settings] = useState<SamplingSettings>({
    temperature: 0.7, top_p: 0.9, top_k: 40, min_p: 0.0,
    repeat_penalty: 1.1, presence_penalty: 0.0, frequency_penalty: 0.0,
    seed: null, max_tokens: 2048, stop_sequences: [],
  });
  const [sessionId, setSessionId] = useState("");
  const [loungeMessages, setLoungeMessages] = useState<LoungeMessage[]>([]);
  const [loungeCollapsed, setLoungeCollapsed] = useState(false);
  const [loungeAlias, setLoungeAlias] = useState("");
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
  const [showConsole, setShowConsole] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [showFiles, setShowFiles] = useState(true);
  const sendTimeoutRef = useRef<number | null>(null);
  const activeRunIdRef = useRef<string | null>(null);

  useEffect(() => { activeRunIdRef.current = activeRunId; }, [activeRunId]);

  useEffect(() => {
    try {
      const rawCollapsed = localStorage.getItem(LOUNGE_COLLAPSED_KEY);
      if (rawCollapsed === "true") setLoungeCollapsed(true);
      const rawAlias = localStorage.getItem(LOUNGE_ALIAS_KEY);
      if (rawAlias) setLoungeAlias(rawAlias);
      const rawProvider = localStorage.getItem(CHAT_PROVIDER_KEY);
      if (rawProvider === "proxx" || rawProvider === "knoxx-rag" || rawProvider === "knoxx-direct") {
        setChatProvider(rawProvider);
      }
    } catch { /* ignore */ }
    void listLoungeMessages().then((items) => setLoungeMessages(items.slice(-200))).catch(() => undefined);
  }, []);

  useEffect(() => { localStorage.setItem(LOUNGE_COLLAPSED_KEY, String(loungeCollapsed)); }, [loungeCollapsed]);
  useEffect(() => { localStorage.setItem(LOUNGE_ALIAS_KEY, loungeAlias); }, [loungeAlias]);
  useEffect(() => { localStorage.setItem(CHAT_PROVIDER_KEY, chatProvider); }, [chatProvider]);

  useEffect(() => {
    void getFrontendConfig().then((cfg) => { setFrontendConfig(cfg); setKnoxxConfigured(cfg.knoxx_enabled); }).catch(() => { setFrontendConfig(null); setKnoxxConfigured(false); });
  }, []);

  useEffect(() => {
    let timer: number | null = null;
    const poll = async () => {
      try {
        const status = await proxxHealth();
        setProxxReachable(Boolean(status.reachable));
        setProxxConfigured(Boolean(status.configured));
        if (status.model_count && proxxModels.length === 0) {
          void listProxxModels().then((items) => { setProxxModels(items); if (!selectedProxxModel && items.length > 0) setSelectedProxxModel(items[0].id); }).catch(() => setProxxModels([]));
        }
      } catch { setProxxReachable(false); }
    };
    void poll();
    timer = window.setInterval(poll, 5000);
    return () => { if (timer !== null) window.clearInterval(timer); };
  }, []);

  useEffect(() => {
    let timer: number | null = null;
    const poll = async () => {
      try { const status = await knoxxHealth(); setKnoxxReachable(Boolean(status.reachable)); setKnoxxConfigured(Boolean(status.configured)); }
      catch { setKnoxxReachable(false); }
    };
    void poll();
    timer = window.setInterval(poll, 5000);
    return () => { if (timer !== null) window.clearInterval(timer); };
  }, []);

  useEffect(() => {
    try {
      let sid = localStorage.getItem(SESSION_ID_KEY) || "";
      if (!sid) { sid = makeId(); localStorage.setItem(SESSION_ID_KEY, sid); }
      setSessionId(sid);
    } catch { setSessionId(makeId()); }
  }, []);

  useEffect(() => {
    const disconnect = connectStream({
      onStatus: (status) => { setWsStatus(status); if (status !== "connected") setIsSending(false); },
      onEvent: (event) => {
        const name = String(event.event ?? "");
        if (name === "run_started") { setActiveRunId(String(event.run_id ?? "")); setConsoleLines((prev) => [...prev.slice(-400), `[run] started: ${String(event.run_id ?? "")}`]); }
        if (name === "run_finished" || name === "run_completed" || name === "run_failed") setIsSending(false);
        setConsoleLines((prev) => [...prev.slice(-400), `[event] ${JSON.stringify(event)}`]);
      },
      onLounge: (message) => {
        const m = message as unknown as LoungeMessage;
        if (!m || !m.id) return;
        setLoungeMessages((prev) => { if (prev.some((p) => p.id === m.id)) return prev; return [...prev.slice(-199), m]; });
      }
    }, sessionId || undefined);
    return disconnect;
  }, [sessionId]);

  useEffect(() => {
    if (!isSending) { if (sendTimeoutRef.current !== null) { window.clearTimeout(sendTimeoutRef.current); sendTimeoutRef.current = null; } return; }
    sendTimeoutRef.current = window.setTimeout(() => { setIsSending(false); setConsoleLines((prev) => [...prev.slice(-400), "[run] timeout waiting for completion event; UI unlocked"]); }, 45000);
    return () => { if (sendTimeoutRef.current !== null) { window.clearTimeout(sendTimeoutRef.current); sendTimeoutRef.current = null; } };
  }, [isSending]);

  async function handleSend(text: string) {
    if (!sessionId) { setConsoleLines((prev) => [...prev.slice(-400), "[chat] session not ready, retry in a second"]); return; }
    const userMessage: ChatMessage = { id: makeId(), role: "user", content: text };
    setMessages((prev) => [...prev, userMessage]);
    setIsSending(true);
    try {
      if (chatProvider === "proxx") {
        if (!selectedProxxModel) throw new Error("Select a Proxx model first");
        const response = await proxxChat({ model: selectedProxxModel, system_prompt: systemPrompt, messages: [...messages, userMessage].map((m) => ({ role: m.role, content: m.content })), temperature: settings.temperature, top_p: settings.top_p, max_tokens: settings.max_tokens, stop: settings.stop_sequences, rag_enabled: ragEnabled, rag_collection: ragCollection, rag_limit: ragLimit, rag_threshold: ragThreshold });
        setMessages((prev) => [...prev, { id: makeId(), role: "assistant", content: response.answer }]);
        setIsSending(false);
        setConsoleLines((prev) => [...prev.slice(-400), `[proxx] model=${response.model ?? selectedProxxModel}${response.rag_context ? ` rag=${response.rag_context.length} chunks` : ""}`]);
        return;
      }
      if (chatProvider === "knoxx-rag" || chatProvider === "knoxx-direct") {
        const direct = chatProvider === "knoxx-direct";
        const response = await knoxxChat({ message: text, conversation_id: direct ? (knoxxDirectConversationId ?? undefined) : (knoxxConversationId ?? undefined), direct });
        if (direct) setKnoxxDirectConversationId(response.conversation_id ?? null); else setKnoxxConversationId(response.conversation_id ?? null);
        setMessages((prev) => [...prev, { id: makeId(), role: "assistant", content: response.answer }]);
        setIsSending(false);
        setConsoleLines((prev) => [...prev.slice(-400), `[knoxx] ${direct ? "direct" : "rag"} response received`]);
        return;
      }
    } catch (error) { setIsSending(false); setConsoleLines((prev) => [...prev.slice(-400), `[run] failed: ${(error as Error).message}`]); }
  }

  function handleNewChat() { setMessages([]); setActiveRunId(null); setKnoxxConversationId(null); setKnoxxDirectConversationId(null); setIsSending(false); }

  async function handleSendLounge(text: string) {
    if (!sessionId) return;
    try { await postLoungeMessage({ session_id: sessionId, alias: loungeAlias || undefined, text }); }
    catch (error) { setConsoleLines((prev) => [...prev.slice(-400), `[lounge] failed: ${(error as Error).message}`]); }
  }

  return (
    <div style={{ display: "flex", height: "calc(100vh - 96px)", gap: 0 }}>
      {/* File Explorer */}
      {showFiles && (
        <Card variant="default" padding="none" style={{ width: 256, flexShrink: 0, display: "flex", flexDirection: "column", borderRight: "1px solid #e2e8f0" }}>
          <div style={{ padding: 12, borderBottom: "1px solid #e2e8f0", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
            <h2 style={{ fontSize: 14, fontWeight: 600 }}>Files</h2>
            <Button variant="ghost" size="sm" onClick={() => setShowFiles(false)}>✕</Button>
          </div>
          <div style={{ flex: 1, overflow: "auto", padding: 8 }}>
            {["docs/", "specs/", "src/", "packages/", "services/", "orgs/"].map((dir) => (
              <div key={dir} style={{ padding: "6px 8px", fontSize: 13, cursor: "pointer", borderRadius: 4 }} onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = "#f1f5f9")} onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = "transparent")}>
                📁 {dir}
              </div>
            ))}
          </div>
        </Card>
      )}

      {/* Main Chat Area */}
      <div style={{ flex: 1, display: "flex", flexDirection: "column", minWidth: 0 }}>
        {/* Top Bar */}
        <div style={{ display: "flex", alignItems: "center", gap: 8, padding: "8px 12px", borderBottom: "1px solid #e2e8f0", flexShrink: 0 }}>
          {!showFiles && <Button variant="ghost" size="sm" onClick={() => setShowFiles(true)}>📁 Files</Button>}
          <Button variant="ghost" size="sm" onClick={() => setShowSettings(!showSettings)}>⚙ Settings</Button>
          <Button variant="ghost" size="sm" onClick={() => setShowConsole(!showConsole)}>🖥 Console</Button>
          <div style={{ flex: 1 }} />
          <select value={chatProvider} onChange={(e) => setChatProvider(e.target.value as ChatProvider)} style={{ borderRadius: 4, border: "1px solid #d1d5db", padding: "2px 6px", fontSize: 12 }}>
            <option value="proxx">Proxx</option>
            <option value="knoxx-rag">Knoxx RAG</option>
            <option value="knoxx-direct">Knoxx Direct</option>
          </select>
          {chatProvider === "proxx" && (
            <>
              <select value={selectedProxxModel} onChange={(e) => setSelectedProxxModel(e.target.value)} style={{ borderRadius: 4, border: "1px solid #d1d5db", padding: "2px 6px", fontSize: 12, maxWidth: 200 }}>
                {proxxModels.length === 0 ? <option value="">No models</option> : null}
                {proxxModels.map((model) => (<option key={model.id} value={model.id}>{model.id}</option>))}
              </select>
              <label style={{ display: "flex", alignItems: "center", gap: 2, fontSize: 12 }}>
                <input type="checkbox" checked={ragEnabled} onChange={(e) => setRagEnabled(e.target.checked)} style={{ height: 12, width: 12 }} /> RAG
              </label>
              <Badge variant={proxxReachable ? "success" : proxxConfigured ? "warning" : "error"} size="sm" dot>
                {proxxReachable ? "online" : "offline"}
              </Badge>
            </>
          )}
          {chatProvider !== "proxx" && (
            <Badge variant={knoxxReachable ? "success" : knoxxConfigured ? "warning" : "error"} size="sm" dot>
              {knoxxReachable ? "online" : "offline"}
            </Badge>
          )}
          <Button variant="ghost" size="sm" onClick={handleNewChat}>New Chat</Button>
        </div>

        {/* Settings Panel */}
        {showSettings && (
          <Card variant="default" padding="sm" style={{ margin: 8, flexShrink: 0 }}>
            <div style={{ display: "flex", gap: 12, flexWrap: "wrap" }}>
              <div style={{ flex: 2, minWidth: 200 }}>
                <label style={{ display: "block", fontSize: 11, fontWeight: 500, marginBottom: 4 }}>System Prompt</label>
                <textarea value={systemPrompt} onChange={(e) => setSystemPrompt(e.target.value)} rows={2} style={{ width: "100%", borderRadius: 4, border: "1px solid #d1d5db", padding: 6, fontSize: 13, resize: "vertical" }} />
              </div>
              <div style={{ flex: 1, minWidth: 80 }}>
                <label style={{ display: "block", fontSize: 11, fontWeight: 500, marginBottom: 4 }}>Temp</label>
                <Input type="number" value={String(settings.temperature)} onChange={() => {}} size="sm" />
              </div>
              <div style={{ flex: 1, minWidth: 80 }}>
                <label style={{ display: "block", fontSize: 11, fontWeight: 500, marginBottom: 4 }}>Max Tokens</label>
                <Input type="number" value={String(settings.max_tokens)} onChange={() => {}} size="sm" />
              </div>
              <div style={{ flex: 1, minWidth: 80 }}>
                <label style={{ display: "block", fontSize: 11, fontWeight: 500, marginBottom: 4 }}>Top P</label>
                <Input type="number" value={String(settings.top_p)} onChange={() => {}} size="sm" />
              </div>
            </div>
          </Card>
        )}

        {/* Chat Messages */}
        <div style={{ flex: 1, overflow: "auto", padding: 16 }}>
          {messages.length === 0 ? (
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", height: "100%", color: "#64748b" }}>
              <p style={{ fontSize: 18, fontWeight: 600, marginBottom: 8 }}>Chat</p>
              <p style={{ fontSize: 14 }}>Start a conversation with Proxx or Knoxx.</p>
            </div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 12, maxWidth: 768, margin: "0 auto" }}>
              {messages.map((message) => (
                <Card key={message.id} variant="outlined" padding="sm" style={{ borderColor: message.role === "user" ? "#99f6e4" : "#e2e8f0", background: message.role === "user" ? "#f0fdfa" : "#ffffff", alignSelf: message.role === "user" ? "flex-end" : "flex-start", maxWidth: "80%" }}>
                  <p style={{ fontSize: 14, whiteSpace: "pre-wrap" }}>{message.content}</p>
                </Card>
              ))}
            </div>
          )}
        </div>

        {/* Composer */}
        <div style={{ padding: 12, borderTop: "1px solid #e2e8f0", flexShrink: 0 }}>
          <ChatComposer onSend={handleSend} isSending={isSending || (chatProvider === "proxx" ? (!proxxReachable || !selectedProxxModel) : !knoxxReachable)} />
        </div>

        {/* Console */}
        {showConsole && (
          <div style={{ height: 200, borderTop: "1px solid #e2e8f0", flexShrink: 0 }}>
            <ConsolePanel lines={consoleLines} />
          </div>
        )}
      </div>

      {/* Lounge */}
      <div style={{ width: loungeCollapsed ? 32 : 256, flexShrink: 0, borderLeft: "1px solid #e2e8f0" }}>
        <LoungePanel collapsed={loungeCollapsed} onToggle={() => setLoungeCollapsed((v) => !v)} messages={loungeMessages} alias={loungeAlias} onAliasChange={setLoungeAlias} onSend={handleSendLounge} />
      </div>
    </div>
  );
}

export default ChatPage;
