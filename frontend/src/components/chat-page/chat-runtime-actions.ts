import type { Dispatch, MutableRefObject, SetStateAction } from 'react';
import { getRun, knoxxAbort, knoxxChatStart, knoxxControl } from '../../lib/api';
import type { ChatMessage, ChatTraceBlock, ContentPart, RunDetail, RunEvent } from '../../lib/types';
import { getChatStorage, initializePersistedChatSession } from './hooks';
import { controlTimelineMessageFromEvent, truncateText } from './utils';

type SetState<T> = Dispatch<SetStateAction<T>>;

type CreateChatRuntimeActionsParams = {
  makeId: () => string;
  systemPrompt: string;
  sessionId: string;
  setSessionId: SetState<string>;
  conversationId: string | null;
  setConversationId: SetState<string | null>;
  selectedModel: string;
  liveControlEnabled: boolean;
  liveControlText: string;
  setLiveControlText: SetState<string>;
  setMessages: SetState<ChatMessage[]>;
  setLatestRun: SetState<RunDetail | null>;
  setRuntimeEvents: SetState<RunEvent[]>;
  setIsSending: SetState<boolean>;
  setConsoleLines: SetState<string[]>;
  setQueueingControl: SetState<'steer' | 'follow_up' | null>;
  setAbortingTurn: SetState<boolean>;
  pendingAssistantIdRef: MutableRefObject<string | null>;
  activeRunIdRef: MutableRefObject<string | null>;
  sessionIdKey: string;
  sessionStateKey: string;
};

export function createChatRuntimeActions({
  makeId,
  systemPrompt,
  sessionId,
  setSessionId,
  conversationId,
  setConversationId,
  selectedModel,
  liveControlEnabled,
  liveControlText,
  setLiveControlText,
  setMessages,
  setLatestRun,
  setRuntimeEvents,
  setIsSending,
  setConsoleLines,
  setQueueingControl,
  setAbortingTurn,
  pendingAssistantIdRef,
  activeRunIdRef,
  sessionIdKey,
  sessionStateKey,
}: CreateChatRuntimeActionsParams) {
  const appendConsoleLine = (line: string) => {
    setConsoleLines((prev) => [...prev.slice(-400), line]);
  };

  const updateMessageById = (messageId: string, updater: (message: ChatMessage) => ChatMessage) => {
    setMessages((prev) => prev.map((message) => (message.id === messageId ? updater(message) : message)));
  };

  const updateTraceBlocksByMessageId = (
    messageId: string,
    updater: (blocks: ChatTraceBlock[]) => ChatTraceBlock[],
  ) => {
    updateMessageById(messageId, (message) => ({
      ...message,
      traceBlocks: updater([...(message.traceBlocks ?? [])]),
    }));
  };

  const appendMessageIfMissing = (message: ChatMessage) => {
    setMessages((prev) => (prev.some((entry) => entry.id === message.id) ? prev : [...prev, message]));
  };

  const loadRunDetail = async (runId: string) => {
    try {
      const run = await getRun(runId);
      if (activeRunIdRef.current === runId) {
        setLatestRun(run);
        const pendingId = pendingAssistantIdRef.current;
        if (pendingId) {
          updateMessageById(pendingId, (message) => ({
            ...message,
            content:
              typeof run.answer === 'string' && run.answer.length > 0
                ? run.answer
                : run.status === 'failed' && typeof run.error === 'string' && run.error.length > 0
                  ? `Agent request failed.\n\n${run.error}`
                  : message.content,
            model: run.model ?? message.model,
            sources: Array.isArray(run.sources) ? run.sources : message.sources,
            runId,
            status: run.status === 'completed' ? 'done' : run.status === 'failed' ? 'error' : message.status,
          }));
          if (run.status === 'completed' || run.status === 'failed') {
            pendingAssistantIdRef.current = null;
          }
        }
      }
    } catch (error) {
      appendConsoleLine(`[runs] failed to load ${runId}: ${(error as Error).message}`);
    }
  };

  const queueLiveControl = async (kind: 'steer' | 'follow_up') => {
    const trimmed = liveControlText.trim();
    if (!trimmed || !conversationId || !liveControlEnabled) {
      return;
    }

    setQueueingControl(kind);
    try {
      const response = await knoxxControl({
        kind,
        message: trimmed,
        conversation_id: conversationId,
        session_id: sessionId,
        run_id: activeRunIdRef.current,
      });
      const optimisticTimelineMessage = controlTimelineMessageFromEvent({
        type: kind === 'follow_up' ? 'follow_up_queued' : 'steer_queued',
        preview: truncateText(trimmed, 240),
        run_id: response.run_id ?? activeRunIdRef.current ?? undefined,
      });
      if (optimisticTimelineMessage) {
        appendMessageIfMissing(optimisticTimelineMessage);
      }
      setLiveControlText('');
      appendConsoleLine(
        `[agent:${kind}] queued for conversation=${response.conversation_id ?? conversationId} run=${response.run_id ?? activeRunIdRef.current ?? 'pending'}`,
      );
    } catch (error) {
      const failedTimelineMessage = controlTimelineMessageFromEvent({
        type: kind === 'follow_up' ? 'follow_up_failed' : 'steer_failed',
        preview: truncateText(trimmed, 240),
        run_id: activeRunIdRef.current ?? undefined,
        error: (error as Error).message,
      });
      if (failedTimelineMessage) {
        appendMessageIfMissing(failedTimelineMessage);
      }
      appendConsoleLine(`[agent:${kind}] failed: ${(error as Error).message}`);
    } finally {
      setQueueingControl(null);
    }
  };

  const abortTurn = async () => {
    if (!conversationId) {
      appendConsoleLine('[abort] missing conversation id');
      return;
    }

    setAbortingTurn(true);
    try {
      const response = await knoxxAbort({
        conversation_id: conversationId,
        session_id: sessionId,
        run_id: activeRunIdRef.current,
        reason: 'aborted_by_user',
      });
      appendConsoleLine(`[abort] ${response.ok ? 'requested' : 'failed'}${response.error ? `: ${response.error}` : ''}`);
    } catch (error) {
      appendConsoleLine(`[abort] failed: ${(error as Error).message}`);
    } finally {
      setAbortingTurn(false);
    }
  };

  const handleSend = async (text: string, contentParts?: ContentPart[]) => {
    if (!sessionId) {
      appendConsoleLine('[chat] session not ready, retry in a second');
      return;
    }
    if (!selectedModel) {
      appendConsoleLine('[chat] no model selected');
      return;
    }

    const userMessage: ChatMessage = { 
      id: makeId(), 
      role: 'user', 
      content: text,
      contentParts,
    };
    const assistantMessageId = makeId();
    const pendingAssistantMessage: ChatMessage = {
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      model: selectedModel,
      sources: [],
      traceBlocks: [],
      status: 'streaming',
    };
    const requestText = systemPrompt.trim()
      ? `${text}\n\nSession steering note:\n${systemPrompt.trim()}`
      : text;
    pendingAssistantIdRef.current = assistantMessageId;
    activeRunIdRef.current = null;
    setLatestRun(null);
    setRuntimeEvents([]);
    setMessages((prev) => [...prev, userMessage, pendingAssistantMessage]);
    setIsSending(true);

    try {
      const response = await knoxxChatStart({
        message: requestText,
        conversation_id: conversationId,
        session_id: sessionId,
        run_id: activeRunIdRef.current,
        model: selectedModel,
        contentParts,
      });
      const runId = response.run_id ?? activeRunIdRef.current;
      if (runId) {
        activeRunIdRef.current = runId;
        void loadRunDetail(runId);
      }
      setConversationId(response.conversation_id ?? conversationId ?? null);
      updateMessageById(assistantMessageId, (message) => ({
        ...message,
        model: response.model ?? selectedModel,
        runId,
        status: 'streaming',
      }));
      appendConsoleLine(
        `[agent] queued model=${response.model ?? selectedModel} conversation=${response.conversation_id ?? conversationId ?? 'new'} run=${runId ?? 'pending'}`,
      );
    } catch (error) {
      const message = (error as Error).message;
      const isAlreadyProcessing = message.includes('409') || message.includes('agent_already_processing') || message.includes('already processing');
      updateMessageById(assistantMessageId, (assistant) => ({
        ...assistant,
        content: isAlreadyProcessing
          ? `Agent is already processing a turn. Use steer or follow-up to queue your message, or wait for the current turn to finish.\n\n${message}`
          : `Agent request failed.\n\n${message}`,
        status: isAlreadyProcessing ? 'streaming' : 'error',
      }));
      if (!isAlreadyProcessing) {
        pendingAssistantIdRef.current = null;
      }
      setIsSending(false);
      appendConsoleLine(`[chat] failed: ${message}`);
    }
  };

  const handleNewChat = () => {
    const nextSessionId = makeId();
    const nextConversationId = makeId();
    try {
      const store = getChatStorage();
      store?.setItem(sessionIdKey, nextSessionId);
      store?.removeItem(sessionStateKey);
      initializePersistedChatSession(sessionStateKey, nextSessionId, nextConversationId, {
        selectedModel,
        systemPrompt,
      });
    } catch {
      // ignore storage failures
    }
    setSessionId(nextSessionId);
    setMessages([]);
    setConversationId(nextConversationId);
    setLatestRun(null);
    setRuntimeEvents([]);
    setLiveControlText('');
    activeRunIdRef.current = null;
    pendingAssistantIdRef.current = null;
    setIsSending(false);
  };

  return {
    appendMessageIfMissing,
    handleNewChat,
    handleSend,
    loadRunDetail,
    queueLiveControl,
    abortTurn,
    updateMessageById,
    updateTraceBlocksByMessageId,
  };
}
