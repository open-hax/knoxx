import { useEffect, useRef, type MutableRefObject } from 'react';
import { connectStream, type StreamConnection } from '../../lib/ws';
import { getRunEvents } from '../../lib/api';
import type { ChatMessage, RunDetail, RunEvent } from '../../lib/types';
import type { SemanticSearchMatch } from './types';
import {
  appendTraceTextDelta,
  applyToolTraceEvent,
  controlTimelineMessageFromEvent,
  finalizeTraceBlocks,
  truncateText,
} from './utils';

type UseChatRuntimeEffectsParams = {
  sessionId: string;
  conversationId: string | null;
  isSending: boolean;
  latestRun: RunDetail | null;
  semanticQuery: string;
  currentPath: string;
  sendUiGuardTimeoutMs: number;
  sendTimeoutRef: MutableRefObject<number | null>;
  pendingAssistantIdRef: MutableRefObject<string | null>;
  activeRunIdRef: MutableRefObject<string | null>;
  setWsStatus: (status: 'connected' | 'closed' | 'error' | 'connecting') => void;
  setIsSending: (value: boolean | ((previous: boolean) => boolean)) => void;
  setLatestRun: (value: RunDetail | null) => void;
  setRuntimeEvents: (value: RunEvent[] | ((previous: RunEvent[]) => RunEvent[])) => void;
  setConsoleLines: (value: string[] | ((previous: string[]) => string[])) => void;
  setSemanticResults: (value: SemanticSearchMatch[] | ((previous: SemanticSearchMatch[]) => SemanticSearchMatch[])) => void;
  setSemanticProjects: (value: string[] | ((previous: string[]) => string[])) => void;
  updateMessageById: (messageId: string, updater: (message: ChatMessage) => ChatMessage) => void;
  updateTraceBlocksByMessageId: (messageId: string, updater: (blocks: import('../../lib/types').ChatTraceBlock[]) => import('../../lib/types').ChatTraceBlock[]) => void;
  appendMessageIfMissing: (message: ChatMessage) => void;
  loadRunDetail: (runId: string) => void | Promise<void>;
  loadDirectory: (path?: string) => void | Promise<void>;
  refreshWorkspaceStatus: () => void | Promise<void>;
  refreshRecentSessions: () => void | Promise<void>;
  runSemanticSearch: (query: string, path?: string) => void | Promise<void>;
};

export function useChatRuntimeEffects({
  sessionId,
  conversationId,
  isSending,
  latestRun,
  semanticQuery,
  currentPath,
  sendUiGuardTimeoutMs,
  sendTimeoutRef,
  pendingAssistantIdRef,
  activeRunIdRef,
  setWsStatus,
  setIsSending,
  setLatestRun,
  setRuntimeEvents,
  setConsoleLines,
  setSemanticResults,
  setSemanticProjects,
  updateMessageById,
  updateTraceBlocksByMessageId,
  appendMessageIfMissing,
  loadRunDetail,
  loadDirectory,
  refreshWorkspaceStatus,
  refreshRecentSessions,
  runSemanticSearch,
}: UseChatRuntimeEffectsParams) {
  const streamRef = useRef<StreamConnection | null>(null);
  const lastEventTimestampRef = useRef<string | null>(null);
  const conversationIdRef = useRef(conversationId);
  const callbacksRef = useRef({
    appendMessageIfMissing,
    loadDirectory,
    loadRunDetail,
    refreshRecentSessions,
    refreshWorkspaceStatus,
    runSemanticSearch,
    updateMessageById,
    updateTraceBlocksByMessageId,
  });

  callbacksRef.current = {
    appendMessageIfMissing,
    loadDirectory,
    loadRunDetail,
    refreshRecentSessions,
    refreshWorkspaceStatus,
    runSemanticSearch,
    updateMessageById,
    updateTraceBlocksByMessageId,
  };
  conversationIdRef.current = conversationId;

  useEffect(() => {
    if (!sessionId) {
      return;
    }
    let cancelled = false;
    let stream: StreamConnection | null = null;
    const connectTimer = window.setTimeout(() => {
      if (cancelled) {
        return;
      }
      stream = connectStream(
        {
          onStatus: (status) => {
            setWsStatus(status);
            if (status === 'connected') {
              // WS reconnected: catch up on missed events for active run
              const activeRunId = activeRunIdRef.current;
              if (activeRunId && isSending) {
                const since = lastEventTimestampRef.current;
                void getRunEvents(activeRunId, since).then((result) => {
                  if (result.count > 0) {
                    setRuntimeEvents((previous) => {
                      const existingIds = new Set(previous.map((e: RunEvent) => `${e.type}:${e.at}`));
                      const newEvents = result.events.filter((e: RunEvent) => !existingIds.has(`${e.type}:${e.at}`));
                      return [...previous.slice(-79), ...newEvents];
                    });
                    setConsoleLines((previous) => [...previous.slice(-400), `[ws] caught up ${result.count} events since reconnect`]);
                    // Update last event timestamp
                    const lastEvent = result.events[result.events.length - 1];
                    if (lastEvent?.at) {
                      lastEventTimestampRef.current = String(lastEvent.at);
                    }
                  }
                }).catch(() => {
                  // Catch-up failed silently — WS will still stream new events
                });
              }
            }
            if (status !== 'connected') setIsSending(false);
          },
          onToken: (token, meta) => {
            const pendingId = pendingAssistantIdRef.current;
            if (!pendingId) return;
            const runId = meta?.runId;
            if (runId) activeRunIdRef.current = runId;
            const blockKind = meta?.kind === 'reasoning' ? 'reasoning' : 'agent_message';
            callbacksRef.current.updateTraceBlocksByMessageId(pendingId, (blocks) => appendTraceTextDelta(blocks, blockKind, token));
            callbacksRef.current.updateMessageById(pendingId, (message) => ({
              ...message,
              runId: runId ?? message.runId ?? null,
              status: 'streaming',
              content: blockKind === 'agent_message' ? `${message.content}${token}` : message.content,
            }));
          },
          onEvent: (event) => {
            const runtimeEvent = event as RunEvent & {
              run_id?: string;
              session_id?: string;
              type?: string;
              status?: string;
              tool_name?: string;
              tool_call_id?: string;
              preview?: string;
              is_error?: boolean;
            };
            // Track latest event timestamp for WS reconnect catch-up
            if (typeof runtimeEvent.at === 'string') {
              lastEventTimestampRef.current = runtimeEvent.at;
            }
            setRuntimeEvents((previous) => [...previous.slice(-79), runtimeEvent]);
            const controlTimelineMessage = controlTimelineMessageFromEvent(runtimeEvent);
            if (controlTimelineMessage) {
              callbacksRef.current.appendMessageIfMissing(controlTimelineMessage);
            }
            const pendingId = pendingAssistantIdRef.current;
            if (pendingId && ['tool_start', 'tool_update', 'tool_end'].includes(String(runtimeEvent.type ?? ''))) {
              callbacksRef.current.updateTraceBlocksByMessageId(pendingId, (blocks) => applyToolTraceEvent(blocks, runtimeEvent));
            }
            if (typeof runtimeEvent.run_id === 'string') {
              activeRunIdRef.current = runtimeEvent.run_id;
              if (runtimeEvent.type === 'run_started') {
                setLatestRun(null);
              }
              if (runtimeEvent.type === 'run_completed' || runtimeEvent.type === 'run_failed') {
                if (pendingId) {
                  callbacksRef.current.updateTraceBlocksByMessageId(
                    pendingId,
                    (blocks) => finalizeTraceBlocks(blocks, runtimeEvent.type === 'run_failed' ? 'error' : 'done'),
                  );
                }
                setIsSending(false);
                void callbacksRef.current.loadRunDetail(runtimeEvent.run_id);
              }
            }
            const label = runtimeEvent.type ?? 'event';
            const toolName = typeof runtimeEvent.tool_name === 'string' ? ` ${runtimeEvent.tool_name}` : '';
            const preview = typeof runtimeEvent.preview === 'string' && runtimeEvent.preview.trim().length > 0
              ? ` :: ${truncateText(runtimeEvent.preview, 120)}`
              : '';
            setConsoleLines((previous) => [...previous.slice(-400), `[agent:${label}]${toolName}${preview}`]);
          },
        },
        sessionId,
        conversationIdRef.current,
      );
      streamRef.current = stream;
    }, 0);
    return () => {
      cancelled = true;
      window.clearTimeout(connectTimer);
      stream?.disconnect();
      streamRef.current = null;
    };
  }, [
    activeRunIdRef,
    pendingAssistantIdRef,
    sessionId,
    setConsoleLines,
    setIsSending,
    setLatestRun,
    setRuntimeEvents,
    setWsStatus,
  ]);

  // Update conversation_id on the websocket when it changes
  useEffect(() => {
    if (streamRef.current) {
      streamRef.current.setConversationId(conversationId);
    }
  }, [conversationId]);

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
      setConsoleLines((previous) => [...previous.slice(-400), `[chat] still running after ${Math.round(sendUiGuardTimeoutMs / 60000)}m; UI unlocked but the backend may still be working`]);
    }, sendUiGuardTimeoutMs);
    return () => {
      if (sendTimeoutRef.current !== null) {
        window.clearTimeout(sendTimeoutRef.current);
        sendTimeoutRef.current = null;
      }
    };
  }, [isSending, sendTimeoutRef, sendUiGuardTimeoutMs, setConsoleLines, setIsSending]);

  useEffect(() => {
    if (!isSending) {
      return;
    }
    const interval = window.setInterval(() => {
      const runId = activeRunIdRef.current;
      if (runId) {
        void callbacksRef.current.loadRunDetail(runId);
      }
    }, 4000);
    return () => window.clearInterval(interval);
  }, [isSending, activeRunIdRef]);

  useEffect(() => {
    void callbacksRef.current.loadDirectory('docs');
    void callbacksRef.current.refreshWorkspaceStatus();
    void callbacksRef.current.refreshRecentSessions();
  }, []);

  useEffect(() => {
    const interval = window.setInterval(() => {
      void callbacksRef.current.refreshRecentSessions();
    }, 30000);
    return () => window.clearInterval(interval);
  }, []);

  useEffect(() => {
    if (!latestRun?.run_id) {
      return;
    }
    if (latestRun.status === 'completed' || latestRun.status === 'failed') {
      void callbacksRef.current.refreshRecentSessions();
    }
  }, [latestRun?.run_id, latestRun?.status]);

  useEffect(() => {
    const interval = window.setInterval(() => {
      void callbacksRef.current.refreshWorkspaceStatus();
    }, 10000);
    return () => window.clearInterval(interval);
  }, []);

  useEffect(() => {
    if (semanticQuery.trim()) {
      void callbacksRef.current.runSemanticSearch(semanticQuery, currentPath);
    }
  }, [currentPath, semanticQuery]);

  useEffect(() => {
    const trimmed = semanticQuery.trim();
    if (!trimmed) {
      setSemanticResults([]);
      setSemanticProjects([]);
      return;
    }
    const timeout = window.setTimeout(() => {
      void callbacksRef.current.runSemanticSearch(trimmed, currentPath);
    }, 300);
    return () => window.clearTimeout(timeout);
  }, [currentPath, semanticQuery, setSemanticResults, setSemanticProjects]);
}
