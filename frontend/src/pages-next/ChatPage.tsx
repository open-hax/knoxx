import { FormEvent, MouseEvent as ReactMouseEvent, ReactNode, useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { Bot, Gauge, MessageSquare, Settings2, SlidersHorizontal, Sparkles, User } from 'lucide-react';
import { Line } from 'react-chartjs-2';
import {
  CategoryScale,
  Chart as ChartJS,
  type ChartOptions,
  Filler,
  Legend,
  LineElement,
  LinearScale,
  PointElement,
  Tooltip,
} from 'chart.js';
import {
  fetchRetrievalStats,
  getFrontendConfig,
  getSettings,
  handoffToShibboleth,
  listLoungeMessages,
  knoxxDirectChat,
  knoxxHealth,
  knoxxRagChat,
  postLoungeMessage,
  updateSettings,
} from '../lib/nextApi';
import { connectStream } from '../lib/ws';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend, Filler);

type ChatMode = 'rag' | 'direct';

interface CompareHit {
  id: string | number;
  score: number;
  title: string;
  section: string;
  url: string;
}

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  sources?: Array<{ title: string; url: string; section?: string }>;
  compare?: {
    denseOnly: CompareHit[];
    hybrid?: CompareHit[];
    debug?: Record<string, unknown>;
  };
}

interface StoredChatState {
  messages?: ChatMessage[];
  conversationId?: string | null;
  draft?: string;
}

interface SystemSample {
  t: string;
  cpu: number;
  ram: number;
  gpu: number;
  net: number;
}

const PREFS_KEY = 'llm_lab_next_chat_prefs';
const LOUNGE_ALIAS_KEY = 'llm_lab_next_lounge_alias';
const SESSION_ID_KEY = 'llm_lab_next_session_id';
const SPLIT_KEY = 'llm_lab_next_chat_split_pct';
const CHAT_STATE_KEY = 'llm_lab_next_chat_state';

function makeId(): string {
  if (globalThis.crypto && typeof globalThis.crypto.randomUUID === 'function') {
    return globalThis.crypto.randomUUID();
  }
  return `id-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function sourceUrlToViewerPath(url: string): string {
  if (!url || url === '#') return '';
  const normalize = (value: string): string => {
    const clean = decodeURIComponent(value.replace(/^\/+/, '').split('#')[0]).split('?')[0];
    if (!clean) return '';
    if (/^thread_\d+$/i.test(clean)) {
      return `${clean}.json`;
    }
    const forumThreadMatch = clean.match(/^forum\/thread\/(\d+)$/i);
    if (forumThreadMatch) {
      return `thread_${forumThreadMatch[1]}.json`;
    }
    const forumPostMatch = clean.match(/^forum\/(\d+)\/(\d+)$/i);
    if (forumPostMatch) {
      return `thread_${forumPostMatch[1]}.json`;
    }
    const forumIdMatch = clean.match(/^forum\/(\d+)$/i);
    if (forumIdMatch) {
      return `thread_${forumIdMatch[1]}.json`;
    }
    const threadIdMatch = clean.match(/(?:^|\/)thread\/(\d+)$/i);
    if (threadIdMatch) {
      return `thread_${threadIdMatch[1]}.json`;
    }
    return clean;
  };

  try {
    const parsed = new URL(url);
    return normalize(parsed.pathname);
  } catch {
    return normalize(url);
  }
}

function loadStoredChatState(): StoredChatState {
  try {
    const raw = localStorage.getItem(CHAT_STATE_KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw) as StoredChatState;
    return parsed && typeof parsed === 'object' ? parsed : {};
  } catch {
    return {};
  }
}

function saveStoredChatState(next: StoredChatState): void {
  try {
    localStorage.setItem(CHAT_STATE_KEY, JSON.stringify(next));
  } catch {
    // ignore local storage errors
  }
}

export default function ChatPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [mode, setMode] = useState<ChatMode>('rag');
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [includeCompare, setIncludeCompare] = useState(true);
  const [stats, setStats] = useState<any>(null);
  const [settings, setSettings] = useState<any>(null);
  const [saveState, setSaveState] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle');
  const [health, setHealth] = useState<any>(null);
  const [loungeMessages, setLoungeMessages] = useState<any[]>([]);
  const [loungeInput, setLoungeInput] = useState('');
  const [loungeAlias, setLoungeAlias] = useState('');
  const [sessionId, setSessionId] = useState('');
  const [wsStatus, setWsStatus] = useState<'connected' | 'closed' | 'error'>('closed');
  const [transcriptSplitPct, setTranscriptSplitPct] = useState(68);
  const [currentSystem, setCurrentSystem] = useState<any>(null);
  const [systemSeries, setSystemSeries] = useState<SystemSample[]>([]);
  const [chatHydrated, setChatHydrated] = useState(false);
  const [frontendConfig, setFrontendConfig] = useState<any>(null);
  const [shibbolethBusy, setShibbolethBusy] = useState(false);
  const [handoffMessage, setHandoffMessage] = useState('');
  const splitContainerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    try {
      const saved = localStorage.getItem(PREFS_KEY);
      if (saved) {
        const prefs = JSON.parse(saved) as {
          mode?: ChatMode;
          includeCompare?: boolean;
          retrieval?: Record<string, unknown>;
        };
        if (prefs.mode === 'rag' || prefs.mode === 'direct') {
          setMode(prefs.mode);
        }
        if (typeof prefs.includeCompare === 'boolean') {
          setIncludeCompare(prefs.includeCompare);
        }
      }

      const alias = localStorage.getItem(LOUNGE_ALIAS_KEY);
      if (alias) setLoungeAlias(alias);

      const splitRaw = localStorage.getItem(SPLIT_KEY);
      if (splitRaw) {
        const parsed = Number(splitRaw);
        if (!Number.isNaN(parsed) && parsed >= 35 && parsed <= 85) {
          setTranscriptSplitPct(parsed);
        }
      }

      const chatStateRaw = localStorage.getItem(CHAT_STATE_KEY);
      if (chatStateRaw) {
        const chatState = JSON.parse(chatStateRaw) as {
          messages?: ChatMessage[];
          conversationId?: string | null;
          draft?: string;
        };
        if (Array.isArray(chatState.messages)) {
          setMessages(chatState.messages.slice(-80));
        }
        if (typeof chatState.conversationId === 'string' || chatState.conversationId === null) {
          setConversationId(chatState.conversationId ?? null);
        }
        if (typeof chatState.draft === 'string') {
          setInput(chatState.draft);
        }
      }

      let sid = localStorage.getItem(SESSION_ID_KEY) ?? '';
      if (!sid) {
        sid = makeId();
        localStorage.setItem(SESSION_ID_KEY, sid);
      }
      setSessionId(sid);
    } catch {
      setSessionId(makeId());
    } finally {
      setChatHydrated(true);
    }
  }, []);

  useEffect(() => {
    if (!chatHydrated) return;
    try {
      localStorage.setItem(
        CHAT_STATE_KEY,
        JSON.stringify({
          messages: messages.slice(-80),
          conversationId,
          draft: input,
        })
      );
    } catch {
      // ignore local storage errors
    }
  }, [chatHydrated, messages, conversationId, input]);

  useEffect(() => {
    try {
      localStorage.setItem(SPLIT_KEY, String(transcriptSplitPct));
    } catch {
      // ignore local storage errors
    }
  }, [transcriptSplitPct]);

  useEffect(() => {
    void getSettings()
      .then((serverSettings) => {
        try {
          const saved = localStorage.getItem(PREFS_KEY);
          if (saved) {
            const prefs = JSON.parse(saved) as { retrieval?: Record<string, unknown> };
            if (prefs.retrieval) {
              setSettings({ ...serverSettings, ...prefs.retrieval });
              return;
            }
          }
        } catch {
          // ignore local storage errors
        }
        setSettings(serverSettings);
      })
      .catch(() => undefined);
    void knoxxHealth().then(setHealth).catch(() => undefined);
    void getFrontendConfig().then(setFrontendConfig).catch(() => undefined);
  }, []);

  useEffect(() => {
    if (!settings) return;
    try {
      localStorage.setItem(
        PREFS_KEY,
        JSON.stringify({
          mode,
          includeCompare,
          retrieval: {
            retrievalMode: settings.retrievalMode,
            retrievalTopK: settings.retrievalTopK,
            hybridTopKDense: settings.hybridTopKDense,
            hybridTopKSparse: settings.hybridTopKSparse,
            hybridTopKFinal: settings.hybridTopKFinal,
            hybridFusion: settings.hybridFusion,
            hybridRrfK: settings.hybridRrfK,
          },
        })
      );
    } catch {
      // ignore local storage errors
    }
  }, [mode, includeCompare, settings]);

  useEffect(() => {
    try {
      localStorage.setItem(LOUNGE_ALIAS_KEY, loungeAlias);
    } catch {
      // ignore local storage errors
    }
  }, [loungeAlias]);

  useEffect(() => {
    const poll = async () => {
      try {
        const s = await fetchRetrievalStats();
        setStats(s?.retrieval ?? null);
      } catch {
        setStats(null);
      }
    };
    void poll();
    const timer = window.setInterval(() => {
      void poll();
    }, 5000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!sessionId) return;
    const disconnect = connectStream(
      {
        onStatus: (status) => setWsStatus(status),
        onStats: (payload) => {
          const sample = payload as any;
          setCurrentSystem(sample);
          const gpuList = Array.isArray(sample.gpu) ? sample.gpu : [];
          const gpuUtil = gpuList.length > 0 ? Number(gpuList[0].util_gpu ?? 0) : 0;
          const netBps = Number(sample?.network?.total_bytes_per_sec ?? 0);
          const stamp = typeof sample.timestamp === 'string' ? new Date(sample.timestamp) : new Date();
          const item: SystemSample = {
            t: stamp.toLocaleTimeString(),
            cpu: Number(sample.cpu_percent ?? 0),
            ram: Number(sample.memory_percent ?? 0),
            gpu: gpuUtil,
            net: netBps / (1024 * 1024),
          };
          setSystemSeries((prev) => [...prev.slice(-59), item]);
        },
        onLounge: (message) => {
          const next = message as any;
          if (!next?.id) return;
          setLoungeMessages((prev) => {
            if (prev.some((m) => m.id === next.id)) return prev;
            return [...prev.slice(-99), next];
          });
        },
      },
      sessionId
    );
    return disconnect;
  }, [sessionId]);

  useEffect(() => {
    const poll = async () => {
      try {
        const items = await listLoungeMessages();
        setLoungeMessages(items.slice(-100));
      } catch {
        // ignore
      }
    };
    void poll();
    const timer = window.setInterval(() => {
      if (wsStatus !== 'connected') {
        void poll();
      }
    }, 4000);
    return () => window.clearInterval(timer);
  }, [wsStatus]);

  const startResize = (event: ReactMouseEvent<HTMLDivElement>) => {
    event.preventDefault();
    const container = splitContainerRef.current;
    if (!container) return;

    const rect = container.getBoundingClientRect();
    document.body.style.cursor = 'row-resize';
    document.body.style.userSelect = 'none';
    const onMove = (moveEvent: MouseEvent) => {
      const deltaY = moveEvent.clientY - rect.top;
      const nextPct = Math.min(85, Math.max(35, (deltaY / rect.height) * 100));
      setTranscriptSplitPct(nextPct);
    };
    const onUp = () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseup', onUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);
  };

  const knoxxStateText = useMemo(() => {
    if (!health) return 'Checking';
    if (!health.configured) return 'Not configured';
    if (!health.reachable) return 'Offline';
    return 'Online';
  }, [health]);

  const cpuNow = Number(currentSystem?.cpu_percent ?? 0);
  const ramNow = Number(currentSystem?.memory_percent ?? 0);
  const gpuNow = Number(currentSystem?.gpu?.[0]?.util_gpu ?? 0);
  const netNowBytes = Number(currentSystem?.network?.total_bytes_per_sec ?? 0);
  const netNowMbps = netNowBytes / (1024 * 1024);

  const systemChartData = useMemo(() => {
    return {
      labels: systemSeries.map((s) => s.t),
      datasets: [
        {
          label: 'CPU %',
          data: systemSeries.map((s) => s.cpu),
          borderColor: '#22d3ee',
          backgroundColor: 'rgba(34, 211, 238, 0.14)',
          fill: true,
          tension: 0.32,
          pointRadius: 0,
        },
        {
          label: 'RAM %',
          data: systemSeries.map((s) => s.ram),
          borderColor: '#f59e0b',
          backgroundColor: 'rgba(245, 158, 11, 0.12)',
          fill: true,
          tension: 0.32,
          pointRadius: 0,
        },
        {
          label: 'GPU %',
          data: systemSeries.map((s) => s.gpu),
          borderColor: '#a78bfa',
          backgroundColor: 'rgba(167, 139, 250, 0.12)',
          fill: true,
          tension: 0.32,
          pointRadius: 0,
        },
      ],
    };
  }, [systemSeries]);

  const networkChartData = useMemo(() => {
    return {
      labels: systemSeries.map((s) => s.t),
      datasets: [
        {
          label: 'Network MB/s',
          data: systemSeries.map((s) => s.net),
          borderColor: '#10b981',
          backgroundColor: 'rgba(16, 185, 129, 0.12)',
          fill: true,
          tension: 0.32,
          pointRadius: 0,
        },
      ],
    };
  }, [systemSeries]);

  const chartOptions = useMemo<ChartOptions<'line'>>(
    () => ({
      responsive: true,
      maintainAspectRatio: false,
      animation: false,
      plugins: {
        legend: {
          labels: { color: '#cbd5e1', boxWidth: 10, boxHeight: 10 },
        },
      },
      scales: {
        x: {
          ticks: { color: '#64748b', maxTicksLimit: 6 },
          grid: { color: 'rgba(100,116,139,0.15)' },
        },
        y: {
          ticks: { color: '#94a3b8' },
          grid: { color: 'rgba(100,116,139,0.15)' },
        },
      },
    }),
    []
  );

  const handleSend = async (event?: FormEvent) => {
    if (event) event.preventDefault();
    const trimmed = input.trim();
    if (!trimmed || isSending) return;

    const userMessage: ChatMessage = { role: 'user', content: trimmed };
    const optimisticMessages = [...messages, userMessage].slice(-80);
    setInput('');
    setMessages((prev) => [...prev, userMessage]);
    saveStoredChatState({
      messages: optimisticMessages,
      conversationId,
      draft: '',
    });
    setIsSending(true);

    try {
      if (mode === 'direct') {
        const response = await knoxxDirectChat({ message: trimmed, conversationId });
        setConversationId(response.conversationId ?? null);
        const assistantMessage: ChatMessage = { role: 'assistant', content: String(response.answer ?? '') };
        setMessages((prev) => [...prev, assistantMessage]);
        const stored = loadStoredChatState();
        saveStoredChatState({
          messages: [...(stored.messages || []), assistantMessage].slice(-80),
          conversationId: response.conversationId ?? null,
          draft: '',
        });
      } else {
        const response = await knoxxRagChat({
          message: trimmed,
          conversationId,
          includeCompare,
        });
        setConversationId(response.conversationId ?? null);
        const assistantMessage: ChatMessage = {
          role: 'assistant',
          content: String(response.answer ?? ''),
          sources: Array.isArray(response.sources) ? response.sources : [],
          compare: response.compare,
        };
        setMessages((prev) => [
          ...prev,
          assistantMessage,
        ]);
        const stored = loadStoredChatState();
        saveStoredChatState({
          messages: [...(stored.messages || []), assistantMessage].slice(-80),
          conversationId: response.conversationId ?? null,
          draft: '',
        });
      }
    } catch (error) {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: `Request failed: ${(error as Error).message}` },
      ]);
    } finally {
      setIsSending(false);
    }
  };

  const saveRetrievalSettings = async () => {
    if (!settings) return;
    setSaveState('saving');
    try {
      await updateSettings({
        retrievalMode: settings.retrievalMode,
        retrievalTopK: Number(settings.retrievalTopK),
        hybridTopKDense: Number(settings.hybridTopKDense),
        hybridTopKSparse: Number(settings.hybridTopKSparse),
        hybridTopKFinal: Number(settings.hybridTopKFinal),
        hybridFusion: settings.hybridFusion,
        hybridRrfK: Number(settings.hybridRrfK),
      });
      setSaveState('saved');
      window.setTimeout(() => setSaveState('idle'), 1300);
    } catch {
      setSaveState('error');
    }
  };

  const sendLoungeMessage = async (event: FormEvent) => {
    event.preventDefault();
    const trimmed = loungeInput.trim();
    if (!trimmed || !sessionId) return;
    try {
      await postLoungeMessage({
        session_id: sessionId,
        alias: loungeAlias || undefined,
        text: trimmed,
      });
      setLoungeInput('');
      const items = await listLoungeMessages();
      setLoungeMessages(items.slice(-100));
    } catch {
      // ignore
    }
  };

  const handoffToLabeling = async () => {
    if (messages.length === 0 || shibbolethBusy) return;

    try {
      setShibbolethBusy(true);
      setHandoffMessage('');
      const response = await handoffToShibboleth({
        model: mode === 'direct' ? 'knoxx-direct' : 'knoxx-rag',
        system_prompt: 'Imported from Knoxx Next chat for Shibboleth labeling.',
        provider: mode,
        conversation_id: conversationId,
        fake_tools_enabled: false,
        items: messages
          .filter((msg) => msg.role === 'user' || msg.role === 'assistant')
          .map((msg) => ({
            role: msg.role,
            content: msg.content,
            metadata: {
              ...(msg.sources ? { sources: msg.sources } : {}),
              ...(msg.compare ? { compare: msg.compare } : {}),
            },
          })),
      });
      if (response.ui_url) {
        window.open(response.ui_url, '_blank', 'noopener,noreferrer');
      }
      setHandoffMessage(`Opened ${response.session_id} in Shibboleth.`);
    } catch (error) {
      setHandoffMessage(`Shibboleth handoff failed: ${(error as Error).message}`);
    } finally {
      setShibbolethBusy(false);
    }
  };

  return (
    <div className="grid h-full min-h-0 grid-cols-1 gap-4 p-4 md:p-6 xl:grid-cols-12">
      <aside className="xl:col-span-4 min-h-0 rounded-2xl border border-slate-800 bg-slate-900/85 p-4 text-slate-100 shadow-xl">
        <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold uppercase tracking-wide text-slate-300">
          <Settings2 className="h-4 w-4 text-cyan-300" />
          Chat Controls
        </h2>

        <div className="space-y-4 overflow-auto pr-1">
          <section className="rounded-xl border border-slate-800 bg-slate-950/60 p-3">
            <p className="mb-2 text-xs uppercase tracking-wide text-slate-400">Provider</p>
            <div className="grid grid-cols-2 gap-2">
              <button
                className={`rounded-md px-3 py-2 text-sm ${mode === 'rag' ? 'bg-cyan-500/20 text-cyan-200' : 'bg-slate-800 text-slate-300 hover:bg-slate-700'}`}
                onClick={() => setMode('rag')}
              >
                RAG Chat
              </button>
              <button
                className={`rounded-md px-3 py-2 text-sm ${mode === 'direct' ? 'bg-cyan-500/20 text-cyan-200' : 'bg-slate-800 text-slate-300 hover:bg-slate-700'}`}
                onClick={() => setMode('direct')}
              >
                Direct LLM
              </button>
            </div>
            <p className="mt-2 text-xs text-slate-400">Knoxx status: {knoxxStateText}</p>
          </section>

          <section className="rounded-xl border border-slate-800 bg-slate-950/60 p-3">
            <p className="mb-2 flex items-center gap-2 text-xs uppercase tracking-wide text-slate-400">
              <SlidersHorizontal className="h-4 w-4" />
              Retrieval Settings
            </p>
            <div className="space-y-3">
              <Field label="Retrieval Mode">
                <select
                  className="field-input"
                  value={settings?.retrievalMode ?? 'dense'}
                  onChange={(e) => setSettings((prev: any) => ({ ...prev, retrievalMode: e.target.value }))}
                >
                  <option value="dense">dense</option>
                  <option value="hybrid">hybrid</option>
                  <option value="hybrid_rerank">hybrid_rerank</option>
                </select>
              </Field>
              <Field label="Retrieval Top K">
                <input
                  className="field-input"
                  type="number"
                  value={settings?.retrievalTopK ?? 6}
                  onChange={(e) => setSettings((prev: any) => ({ ...prev, retrievalTopK: Number(e.target.value) }))}
                />
              </Field>
              <div className="grid grid-cols-3 gap-2">
                <Field label="Dense">
                  <input
                    className="field-input"
                    type="number"
                    value={settings?.hybridTopKDense ?? 30}
                    onChange={(e) => setSettings((prev: any) => ({ ...prev, hybridTopKDense: Number(e.target.value) }))}
                  />
                </Field>
                <Field label="Sparse">
                  <input
                    className="field-input"
                    type="number"
                    value={settings?.hybridTopKSparse ?? 50}
                    onChange={(e) => setSettings((prev: any) => ({ ...prev, hybridTopKSparse: Number(e.target.value) }))}
                  />
                </Field>
                <Field label="Final">
                  <input
                    className="field-input"
                    type="number"
                    value={settings?.hybridTopKFinal ?? 12}
                    onChange={(e) => setSettings((prev: any) => ({ ...prev, hybridTopKFinal: Number(e.target.value) }))}
                  />
                </Field>
              </div>
              <Field label="Fusion">
                <select
                  className="field-input"
                  value={settings?.hybridFusion ?? 'rrf'}
                  onChange={(e) => setSettings((prev: any) => ({ ...prev, hybridFusion: e.target.value }))}
                >
                  <option value="rrf">rrf</option>
                  <option value="relative_score">relative_score</option>
                </select>
              </Field>
              <Field label="RRF K">
                <input
                  className="field-input"
                  type="number"
                  value={settings?.hybridRrfK ?? 60}
                  onChange={(e) => setSettings((prev: any) => ({ ...prev, hybridRrfK: Number(e.target.value) }))}
                />
              </Field>
              <button
                className="w-full rounded-md bg-cyan-500 px-3 py-2 text-sm font-medium text-slate-950 hover:bg-cyan-400 disabled:opacity-60"
                onClick={saveRetrievalSettings}
                disabled={saveState === 'saving' || !settings}
              >
                {saveState === 'saving' ? 'Saving...' : 'Save Retrieval Config'}
              </button>
              {saveState === 'saved' ? <p className="text-xs text-emerald-300">Saved. New requests use updated settings.</p> : null}
              {saveState === 'error' ? <p className="text-xs text-rose-300">Failed to save settings.</p> : null}
            </div>
          </section>

          <section className="rounded-xl border border-slate-800 bg-slate-950/60 p-3">
            <label className="flex items-center gap-2 text-sm text-slate-300">
              <input
                type="checkbox"
                checked={includeCompare}
                onChange={(e) => setIncludeCompare(e.target.checked)}
                className="h-4 w-4 rounded border-slate-600 bg-slate-900 text-cyan-400"
              />
              Compare dense vs hybrid retrieval
            </label>
            <p className="mt-1 text-xs text-slate-400">Shows top-hit diffs in assistant responses.</p>
          </section>

          <section className="rounded-xl border border-slate-800 bg-slate-950/60 p-3">
            <h3 className="mb-2 flex items-center gap-2 text-sm font-semibold text-slate-200">
              <Gauge className="h-4 w-4 text-cyan-300" />
              Retrieval Stats
            </h3>
            <div className="grid grid-cols-3 gap-2 text-center">
              <Stat title="avg" value={`${stats?.avgRetrievalMs ?? 0}ms`} />
              <Stat title="p95" value={`${stats?.p95RetrievalMs ?? 0}ms`} />
              <Stat title="samples" value={String(stats?.recentSamples ?? 0)} />
            </div>
            <div className="mt-2 grid grid-cols-3 gap-2 text-xs">
              <MiniCount label="dense" value={stats?.modeCounts?.dense ?? 0} />
              <MiniCount label="hybrid" value={stats?.modeCounts?.hybrid ?? 0} />
              <MiniCount label="rerank" value={stats?.modeCounts?.hybrid_rerank ?? 0} />
            </div>
          </section>

          <section className="rounded-xl border border-slate-800 bg-slate-950/60 p-3">
            <h3 className="mb-2 text-sm font-semibold text-slate-200">System Info</h3>
            <div className="space-y-1 text-xs text-slate-300">
              <InfoRow label="Knoxx" value={knoxxStateText} />
              <InfoRow label="Base URL" value={health?.base_url ?? 'N/A'} mono />
              <InfoRow label="Service Status" value={health?.details?.status ?? 'N/A'} />
              <InfoRow label="Project" value={health?.details?.project ?? 'N/A'} />
              <InfoRow label="Retrieval Mode" value={settings?.retrievalMode ?? 'N/A'} />
              <InfoRow label="Conversation ID" value={conversationId ?? 'None'} mono />
              <InfoRow label="WS" value={wsStatus} />
              <InfoRow
                label="Collection"
                value={String(health?.details?.collection?.name ?? 'N/A')}
                mono
              />
              <InfoRow
                label="Points"
                value={String(health?.details?.collection?.pointsCount ?? health?.details?.collection?.indexedVectorsCount ?? 'N/A')}
              />
              <InfoRow label="CPU Used" value={`${cpuNow.toFixed(1)}%`} />
              <InfoRow label="RAM Used" value={`${ramNow.toFixed(1)}%`} />
              <InfoRow label="GPU Used" value={`${gpuNow.toFixed(1)}%`} />
              <InfoRow label="Network" value={`${netNowMbps.toFixed(2)} MB/s`} />
            </div>
            <div className="mt-3 rounded-lg border border-slate-800 bg-slate-900/70 p-2">
              <p className="mb-1 text-[11px] uppercase tracking-wide text-slate-400">CPU / RAM / GPU</p>
              <div className="h-40">
                <Line data={systemChartData} options={chartOptions} />
              </div>
            </div>
            <div className="mt-2 rounded-lg border border-slate-800 bg-slate-900/70 p-2">
              <p className="mb-1 text-[11px] uppercase tracking-wide text-slate-400">Network Throughput</p>
              <div className="h-24">
                <Line data={networkChartData} options={chartOptions} />
              </div>
            </div>
          </section>
        </div>
      </aside>

      <main className="xl:col-span-8 min-h-0 rounded-2xl border border-slate-800 bg-slate-900/85 p-4 shadow-xl">
        <div className="mb-3 flex items-center justify-between">
          <div>
            <h2 className="flex items-center gap-2 text-sm font-semibold uppercase tracking-wide text-slate-300">
              <MessageSquare className="h-4 w-4 text-cyan-300" />
              Chat Transcript
            </h2>
            {handoffMessage ? <p className="mt-1 text-xs text-cyan-300">{handoffMessage}</p> : null}
          </div>
          <div className="flex items-center gap-2">
            {frontendConfig?.shibboleth_ui_url ? (
              <a
                className="rounded-md border border-slate-700 bg-slate-800 px-2 py-1 text-xs text-slate-200 hover:bg-slate-700"
                href={frontendConfig.shibboleth_ui_url}
                target="_blank"
                rel="noreferrer"
              >
                Open Shibboleth
              </a>
            ) : null}
            <button
              className="rounded-md border border-cyan-500/50 bg-cyan-500/10 px-2 py-1 text-xs text-cyan-200 hover:bg-cyan-500/20 disabled:opacity-60"
              onClick={handoffToLabeling}
              disabled={shibbolethBusy || messages.length === 0 || !frontendConfig?.shibboleth_enabled}
            >
              {shibbolethBusy ? 'Opening…' : 'Label in Shibboleth'}
            </button>
            <button
              className="rounded-md border border-slate-700 bg-slate-800 px-2 py-1 text-xs text-slate-200 hover:bg-slate-700"
              onClick={() => {
                setMessages([]);
                setConversationId(null);
                setHandoffMessage('');
              }}
            >
              New Chat
            </button>
          </div>
        </div>

        <div ref={splitContainerRef} className="mb-3 flex h-[68vh] min-h-[500px] flex-col">
          <section
            className="flex min-h-0 flex-col rounded-xl border border-slate-800 bg-slate-950/60 p-3"
            style={{ flex: `0 0 ${transcriptSplitPct}%` }}
          >
            <div className="mb-2 flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-400">Transcript</h3>
              <span className="text-[11px] text-slate-500">{Math.round(transcriptSplitPct)}%</span>
            </div>
            <div className="min-h-0 flex-1 space-y-3 overflow-auto rounded-lg border border-slate-800 bg-slate-900/40 p-2">
              {messages.length === 0 ? (
                <div className="flex h-full min-h-[220px] items-center justify-center">
                  <div className="text-center text-slate-400">
                    <Sparkles className="mx-auto mb-2 h-8 w-8 text-cyan-300" />
                    Ask a question to start a RAG session.
                  </div>
                </div>
              ) : (
                messages.map((msg, idx) => (
                  <article
                    key={`${idx}-${msg.role}`}
                    className={`rounded-xl border px-3 py-2 ${
                      msg.role === 'user'
                        ? 'ml-auto max-w-[85%] border-cyan-500/30 bg-cyan-500/10'
                        : 'mr-auto max-w-[92%] border-slate-700 bg-slate-900'
                    }`}
                  >
                    <p className="mb-1 flex items-center gap-1 text-xs uppercase tracking-wide text-slate-400">
                      {msg.role === 'user' ? <User className="h-3.5 w-3.5" /> : <Bot className="h-3.5 w-3.5" />}
                      {msg.role}
                    </p>
                    <p className="whitespace-pre-wrap text-sm text-slate-100">{msg.content}</p>

                    {msg.sources && msg.sources.length > 0 ? (
                      <div className="mt-3 rounded-md border border-slate-800 bg-slate-950/80 p-2">
                        <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-400">Sources</p>
                        <div className="space-y-1">
                          {msg.sources.slice(0, 6).map((source, sourceIdx) => {
                            const relativePath = sourceUrlToViewerPath(String(source.url || ''));
                            return relativePath ? (
                              <Link
                                key={`${source.title}-${sourceIdx}`}
                                to={`/next/docs/view?path=${encodeURIComponent(relativePath)}`}
                                className="block rounded border border-slate-800 bg-slate-900 px-2 py-1 text-xs text-slate-200 hover:border-cyan-400/60"
                              >
                                <p className="truncate font-medium">{source.title}</p>
                                <p className="truncate text-slate-400">{source.section || relativePath}</p>
                              </Link>
                            ) : (
                              <div
                                key={`${source.title}-${sourceIdx}`}
                                className="block rounded border border-slate-800 bg-slate-900 px-2 py-1 text-xs text-slate-500"
                              >
                                <p className="truncate font-medium">{source.title}</p>
                                <p className="truncate">Unavailable source URL</p>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    ) : null}

                    {msg.compare ? (
                      <div className="mt-3 grid grid-cols-1 gap-2 md:grid-cols-2">
                        <CompareList title="Dense" hits={msg.compare.denseOnly} />
                        <CompareList title="Hybrid" hits={msg.compare.hybrid ?? []} />
                      </div>
                    ) : null}
                  </article>
                ))
              )}
            </div>

            <form className="mt-2 flex gap-2" onSubmit={handleSend}>
              <textarea
                rows={3}
                className="field-input min-h-20 flex-1 resize-y"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="Ask about docs, retrieval quality, or compare hybrid behavior..."
              />
              <button
                type="submit"
                disabled={isSending || !input.trim()}
                className="h-fit rounded-md bg-cyan-500 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-cyan-400 disabled:opacity-60"
              >
                {isSending ? 'Sending...' : 'Send'}
              </button>
            </form>
          </section>

          <div
            role="separator"
            aria-orientation="horizontal"
            onMouseDown={startResize}
            onDoubleClick={() => setTranscriptSplitPct(68)}
            className="group my-1 flex h-4 cursor-row-resize items-center justify-center"
          >
            <div className="h-2 w-28 rounded-full border border-slate-600 bg-slate-700 transition group-hover:bg-cyan-500/70" />
          </div>

          <section
            className="flex min-h-0 flex-col rounded-xl border border-slate-800 bg-slate-950/60 p-3"
            style={{ flex: `1 1 ${100 - transcriptSplitPct}%` }}
          >
            <div className="mb-2 flex items-center justify-between">
              <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-400">Inter-user Chat</h3>
              <div className="flex items-center gap-2">
                <span className={`text-[11px] ${wsStatus === 'connected' ? 'text-emerald-300' : 'text-amber-300'}`}>
                  {wsStatus === 'connected' ? 'Realtime' : 'Polling'}
                </span>
                <input
                  className="field-input h-8 max-w-[180px] py-1 text-xs"
                  placeholder="Alias"
                  value={loungeAlias}
                  onChange={(e) => setLoungeAlias(e.target.value)}
                />
              </div>
            </div>
            <div className="mb-2 min-h-0 flex-1 space-y-2 overflow-auto rounded-lg border border-slate-800 bg-slate-900/40 p-2 text-xs">
              {loungeMessages.length === 0 ? (
                <p className="text-slate-500">No messages yet.</p>
              ) : (
                loungeMessages.map((item) => (
                  <article key={item.id} className="rounded-xl border border-slate-700 bg-slate-900 px-3 py-2">
                    <p className="mb-1 text-[11px] uppercase tracking-wide text-cyan-300">{item.alias || 'anon'}</p>
                    <p className="text-sm text-slate-100">{item.text}</p>
                  </article>
                ))
              )}
            </div>
            <form className="flex gap-2" onSubmit={sendLoungeMessage}>
              <input
                className="field-input"
                placeholder="Say something to other operators..."
                value={loungeInput}
                onChange={(e) => setLoungeInput(e.target.value)}
              />
              <button
                type="submit"
                className="rounded-md bg-slate-700 px-3 py-2 text-xs font-semibold text-slate-100 hover:bg-slate-600"
                disabled={!loungeInput.trim()}
              >
                Post
              </button>
            </form>
          </section>
        </div>
      </main>
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block text-xs text-slate-400">
      <span className="mb-1 block uppercase tracking-wide">{label}</span>
      {children}
    </label>
  );
}

function Stat({ title, value }: { title: string; value: string }) {
  return (
    <div className="rounded-md border border-slate-800 bg-slate-900 p-2">
      <p className="text-[11px] uppercase tracking-wide text-slate-500">{title}</p>
      <p className="text-sm font-semibold text-slate-200">{value}</p>
    </div>
  );
}

function MiniCount({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-md border border-slate-800 bg-slate-900 px-2 py-1 text-slate-300">
      {label}: {value}
    </div>
  );
}

function InfoRow({ label, value, mono }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="flex items-start justify-between gap-2 rounded-md border border-slate-800 bg-slate-900/70 px-2 py-1.5">
      <span className="text-slate-400">{label}</span>
      <span className={`max-w-[60%] break-all text-right ${mono ? 'font-mono text-[11px]' : ''}`}>{value}</span>
    </div>
  );
}

function CompareList({ title, hits }: { title: string; hits: CompareHit[] }) {
  return (
    <section className="rounded-md border border-slate-700 bg-slate-950/80 p-2">
      <h4 className="mb-1 text-xs font-semibold uppercase tracking-wide text-slate-400">{title} Top Hits</h4>
      {hits.length === 0 ? (
        <p className="text-xs text-slate-500">No hits.</p>
      ) : (
        <div className="space-y-1">
          {hits.slice(0, 3).map((hit) => (
            (() => {
              const relativePath = sourceUrlToViewerPath(hit.url);
              if (!relativePath) {
                return (
                  <div
                    key={`${title}-${hit.id}`}
                    className="block rounded border border-slate-800 bg-slate-900 px-2 py-1 text-xs text-slate-500"
                  >
                    <p className="truncate font-medium">{hit.title}</p>
                    <p className="truncate">Unavailable source URL</p>
                  </div>
                );
              }
              return (
                <Link
                  key={`${title}-${hit.id}`}
                  to={`/next/docs/view?path=${encodeURIComponent(relativePath)}`}
                  className="block rounded border border-slate-800 bg-slate-900 px-2 py-1 text-xs text-slate-200 hover:border-cyan-400/60"
                >
                  <p className="truncate font-medium">{hit.title}</p>
                  <p className="truncate text-slate-400">{hit.section || relativePath}</p>
                </Link>
              );
            })()
          ))}
        </div>
      )}
    </section>
  );
}
