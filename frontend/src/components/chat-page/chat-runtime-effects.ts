import { useEffect, type MutableRefObject } from 'react';
import { connectStream } from '../../lib/ws';
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
  useEffect(() => {
    const disconnect = connectStream(
      {
        onStatus: (status) => {
          setWsStatus(status);
          if (status !== 'connected') setIsSending(false);
        },
        onToken: (token, meta) => {
          const pendingId = pendingAssistantIdRef.current;
          if (!pendingId) return;
          const runId = meta?.runId;
          if (runId) activeRunIdRef.current = runId;
          const blockKind = meta?.kind === 'reasoning' ? 'reasoning' : 'agent_message';
          updateTraceBlocksByMessageId(pendingId, (blocks) => appendTraceTextDelta(blocks, blockKind, token));
          updateMessageById(pendingId, (message) => ({
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
          setRuntimeEvents((previous) => [...previous.slice(-79), runtimeEvent]);
          const controlTimelineMessage = controlTimelineMessageFromEvent(runtimeEvent);
          if (controlTimelineMessage) {
            appendMessageIfMissing(controlTimelineMessage);
          }
          const pendingId = pendingAssistantIdRef.current;
          if (pendingId && ['tool_start', 'tool_update', 'tool_end'].includes(String(runtimeEvent.type ?? ''))) {
            updateTraceBlocksByMessageId(pendingId, (blocks) => applyToolTraceEvent(blocks, runtimeEvent));
          }
          if (typeof runtimeEvent.run_id === 'string') {
            activeRunIdRef.current = runtimeEvent.run_id;
            if (runtimeEvent.type === 'run_started') {
              setLatestRun(null);
            }
            if (runtimeEvent.type === 'run_completed' || runtimeEvent.type === 'run_failed') {
              if (pendingId) {
                updateTraceBlocksByMessageId(
                  pendingId,
                  (blocks) => finalizeTraceBlocks(blocks, runtimeEvent.type === 'run_failed' ? 'error' : 'done'),
                );
              }
              setIsSending(false);
              void loadRunDetail(runtimeEvent.run_id);
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
      sessionId || undefined,
    );
    return disconnect;
  }, [
    activeRunIdRef,
    appendMessageIfMissing,
    loadRunDetail,
    pendingAssistantIdRef,
    sessionId,
    setConsoleLines,
    setIsSending,
    setLatestRun,
    setRuntimeEvents,
    setWsStatus,
    updateMessageById,
    updateTraceBlocksByMessageId,
  ]);

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
        void loadRunDetail(runId);
      }
    }, 2000);
    return () => window.clearInterval(interval);
  }, [activeRunIdRef, isSending, loadRunDetail]);

  useEffect(() => {
    void loadDirectory('docs');
    void refreshWorkspaceStatus();
    void refreshRecentSessions();
  }, [loadDirectory, refreshRecentSessions, refreshWorkspaceStatus]);

  useEffect(() => {
    const interval = window.setInterval(() => {
      void refreshRecentSessions();
    }, 15000);
    return () => window.clearInterval(interval);
  }, [refreshRecentSessions]);

  useEffect(() => {
    if (latestRun?.status === 'completed' || latestRun?.status === 'failed') {
      void refreshRecentSessions();
    }
  }, [latestRun?.run_id, latestRun?.status, refreshRecentSessions]);

  useEffect(() => {
    const interval = window.setInterval(() => {
      void refreshWorkspaceStatus();
    }, 3000);
    return () => window.clearInterval(interval);
  }, [refreshWorkspaceStatus]);

  useEffect(() => {
    if (semanticQuery.trim()) {
      void runSemanticSearch(semanticQuery, currentPath);
    }
  }, [currentPath, runSemanticSearch, semanticQuery]);

  useEffect(() => {
    const trimmed = semanticQuery.trim();
    if (!trimmed) {
      setSemanticResults([]);
      setSemanticProjects([]);
      return;
    }
    const timeout = window.setTimeout(() => {
      void runSemanticSearch(trimmed, currentPath);
    }, 300);
    return () => window.clearTimeout(timeout);
  }, [currentPath, runSemanticSearch, semanticQuery, setSemanticProjects, setSemanticResults]);
}
