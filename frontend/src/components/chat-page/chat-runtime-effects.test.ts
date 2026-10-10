import { act, cleanup, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ChatMessage, RunDetail } from '../../lib/types';
import type { StreamConnection, StreamHandlers } from '../../lib/ws';

vi.mock('../../lib/ws', () => ({ connectStream: vi.fn() }));
vi.mock('../../lib/api', () => ({
  getRun: vi.fn(),
  getRunEvents: vi.fn(),
  knoxxAbort: vi.fn(),
  knoxxChatStart: vi.fn(),
  knoxxControl: vi.fn(),
  knoxxUndoSessionTurn: vi.fn(),
}));

import { getRun } from '../../lib/api';
import { connectStream } from '../../lib/ws';
import { createChatRuntimeActions } from './chat-runtime-actions';
import { useChatRuntimeEffects } from './chat-runtime-effects';

function createHarness() {
  let messages: ChatMessage[] = [{ id: 'assistant-1', role: 'assistant', content: '', status: 'streaming' }];
  const pendingAssistantIdRef = { current: 'assistant-1' as string | null };
  const activeRunIdRef = { current: 'run-1' as string | null };
  const setIsSending = vi.fn();
  const setLatestRun = vi.fn();
  const setRuntimeEvents = vi.fn();
  const setConsoleLines = vi.fn();
  const actions = createChatRuntimeActions({
    makeId: () => 'fixture-id',
    systemPrompt: '',
    activeRole: 'knowledge_worker',
    activeActorId: 'chat_primary',
    activeAgentId: 'knoxx_default',
    sessionId: 'session-1',
    setSessionId: vi.fn(),
    conversationId: 'conversation-1',
    setConversationId: vi.fn(),
    selectedModel: 'fixture-model',
    selectedThinkingLevel: 'medium',
    liveControlEnabled: false,
    liveControlText: '',
    setLiveControlText: vi.fn(),
    setMessages: (value) => {
      messages = typeof value === 'function' ? value(messages) : value;
    },
    setLatestRun,
    setRuntimeEvents,
    setIsSending,
    setConsoleLines,
    setQueueingControl: vi.fn(),
    setAbortingTurn: vi.fn(),
    pendingAssistantIdRef,
    activeRunIdRef,
    sessionIdKey: 'fixture-session',
    sessionStateKey: 'fixture-state',
  });
  let handlers: StreamHandlers = {};
  const disconnect = vi.fn();
  const connection = Object.assign(disconnect, {
    disconnect,
    setConversationId: vi.fn(),
  }) as StreamConnection;
  vi.mocked(connectStream).mockImplementation((callbacks) => {
    handlers = callbacks;
    return connection;
  });
  renderHook(() => useChatRuntimeEffects({
    sessionId: 'session-1',
    conversationId: 'conversation-1',
    isSending: true,
    latestRun: null,
    semanticQuery: '',
    currentPath: 'docs',
    sendUiGuardTimeoutMs: 60000,
    sendTimeoutRef: { current: null },
    pendingAssistantIdRef,
    activeRunIdRef,
    setWsStatus: vi.fn(),
    setIsSending,
    setLatestRun,
    setRuntimeEvents,
    setConsoleLines,
    setSemanticResults: vi.fn(),
    setSemanticProjects: vi.fn(),
    updateMessageById: actions.updateMessageById,
    updateTraceBlocksByMessageId: actions.updateTraceBlocksByMessageId,
    appendMessageIfMissing: actions.appendMessageIfMissing,
    loadRunDetail: actions.loadRunDetail,
    loadDirectory: vi.fn(),
    refreshWorkspaceStatus: vi.fn(),
    refreshRecentSessions: vi.fn(),
    runSemanticSearch: vi.fn(),
  }));
  act(() => vi.advanceTimersByTime(0));
  return { handlers, pendingAssistantIdRef, setIsSending, getMessage: () => messages[0], getMessages: () => messages };
}

describe('useChatRuntimeEffects streaming and final hydration', () => {
  let frames: FrameRequestCallback[];
  const flushFrames = () => {
    const scheduled = frames;
    frames = [];
    act(() => scheduled.forEach((callback) => callback(0)));
  };

  beforeEach(() => {
    vi.resetAllMocks();
    vi.useFakeTimers();
    frames = [];
    vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => frames.push(callback));
  });

  afterEach(() => {
    cleanup();
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it.each([1, 3, 20])('preserves literal text and reasoning bytes with %i tokens per animation frame', (batchSize) => {
    const harness = createHarness();
    const text = ['# CMS Demo', '\n', '\n', 'Knox', 'x', ' drafts.', '\n', '\n', 'ha', 'ha', '.'];
    const reasoning = ['Checking ', 'ha', 'ha', '\n', '\n', 'done.'];
    for (const [kind, chunks] of [['text', text], ['reasoning', reasoning]] as const) {
      for (let index = 0; index < chunks.length; index += batchSize) {
        act(() => chunks.slice(index, index + batchSize).forEach((token) => {
          harness.handlers.onToken?.(token, { runId: 'run-1', kind });
        }));
        flushFrames();
      }
    }

    expect(harness.getMessage().content).toBe(text.join(''));
    expect(harness.getMessage().traceBlocks?.map((block) => ({ kind: block.kind, content: block.content }))).toEqual([
      { kind: 'agent_message', content: text.join('') },
      { kind: 'reasoning', content: reasoning.join('') },
    ]);
  });

  it.each(['completed', 'failed'] as const)('keeps the assistant association until the %s run is hydrated after its terminal event', async (status) => {
    const harness = createHarness();
    let resolveRun!: (run: RunDetail) => void;
    vi.mocked(getRun).mockReturnValue(new Promise((resolve) => { resolveRun = resolve; }));
    act(() => harness.handlers.onToken?.('Knox', { runId: 'run-1', kind: 'text' }));
    flushFrames();
    act(() => {
      harness.handlers.onToken?.('x', { runId: 'run-1', kind: 'text' });
      harness.handlers.onToken?.('\n\n', { runId: 'run-1', kind: 'text' });
      harness.handlers.onEvent?.({ type: `run_${status}`, run_id: 'run-1', at: '2026-10-09T18:00:00Z' });
    });

    expect(harness.getMessage().content).toBe('Knoxx\n\n');
    expect(harness.getMessage().traceBlocks).toMatchObject([
      { kind: 'agent_message', content: 'Knoxx\n\n', status: status === 'completed' ? 'done' : 'error' },
    ]);
    expect(harness.pendingAssistantIdRef.current).toBe('assistant-1');
    expect(getRun).toHaveBeenCalledWith('run-1');
    expect(harness.setIsSending).toHaveBeenCalledWith(false);
    flushFrames();
    expect(harness.getMessage().content).toBe('Knoxx\n\n');

    await act(async () => resolveRun({
      run_id: 'run-1',
      status,
      created_at: '2026-10-09T18:00:00Z',
      updated_at: '2026-10-09T18:00:01Z',
      answer: status === 'completed' ? '# CMS Demo\n\nKnoxx drafts once.' : null,
      error: status === 'failed' ? 'Fixture failure' : undefined,
      request_messages: [],
      settings: {},
      resources: {},
    }));

    expect(harness.getMessages()).toHaveLength(1);
    expect(harness.getMessage()).toMatchObject({
      id: 'assistant-1',
      runId: 'run-1',
      content: status === 'completed' ? '# CMS Demo\n\nKnoxx drafts once.' : 'Agent request failed.\n\nFixture failure',
      status: status === 'completed' ? 'done' : 'error',
    });
    expect(harness.pendingAssistantIdRef.current).toBeNull();
  });
});
