import type { Dispatch, MutableRefObject, SetStateAction } from 'react';
import type { ChatMessage, ProxxModelInfo, RunDetail, RunEvent } from '../../lib/types';
import type { PinnedContextItem } from './types';
import {
  useChatSessionPersistence,
  usePinnedContextPersistence,
  useProxxStatusPolling,
  useScratchpadPersistence,
} from './hooks';

type SetState<T> = Dispatch<SetStateAction<T>>;

type UseChatPagePersistenceSuiteParams = {
  makeId: () => string;
  sessionId: string;
  setSessionId: SetState<string>;
  systemPrompt: string;
  setSystemPrompt: SetState<string>;
  selectedModel: string;
  setSelectedModel: SetState<string>;
  conversationId: string | null;
  setConversationId: SetState<string | null>;
  messages: ChatMessage[];
  setMessages: SetState<ChatMessage[]>;
  latestRun: RunDetail | null;
  setLatestRun: SetState<RunDetail | null>;
  runtimeEvents: RunEvent[];
  setRuntimeEvents: SetState<RunEvent[]>;
  isSending: boolean;
  setIsSending: SetState<boolean>;
  sidebarWidthPx: number;
  setSidebarWidthPx: SetState<number>;
  pendingAssistantIdRef: MutableRefObject<string | null>;
  activeRunIdRef: MutableRefObject<string | null>;
  sessionIdKey: string;
  sessionStateKey: string;
  sidebarWidthKey: string;
  scratchpadStorageKey: string;
  canvasTitle: string;
  setCanvasTitle: SetState<string>;
  canvasSubject: string;
  setCanvasSubject: SetState<string>;
  canvasPath: string;
  setCanvasPath: SetState<string>;
  canvasRecipients: string;
  setCanvasRecipients: SetState<string>;
  canvasCc: string;
  setCanvasCc: SetState<string>;
  canvasContent: string;
  setCanvasContent: SetState<string>;
  pinnedContextStorageKey: string;
  pinnedContext: PinnedContextItem[];
  setPinnedContext: SetState<PinnedContextItem[]>;
  setProxxReachable: SetState<boolean>;
  setProxxConfigured: SetState<boolean>;
  setProxxModels: SetState<ProxxModelInfo[]>;
};

export function useChatPagePersistenceSuite({
  makeId,
  sessionId,
  setSessionId,
  systemPrompt,
  setSystemPrompt,
  selectedModel,
  setSelectedModel,
  conversationId,
  setConversationId,
  messages,
  setMessages,
  latestRun,
  setLatestRun,
  runtimeEvents,
  setRuntimeEvents,
  isSending,
  setIsSending,
  sidebarWidthPx,
  setSidebarWidthPx,
  pendingAssistantIdRef,
  activeRunIdRef,
  sessionIdKey,
  sessionStateKey,
  sidebarWidthKey,
  scratchpadStorageKey,
  canvasTitle,
  setCanvasTitle,
  canvasSubject,
  setCanvasSubject,
  canvasPath,
  setCanvasPath,
  canvasRecipients,
  setCanvasRecipients,
  canvasCc,
  setCanvasCc,
  canvasContent,
  setCanvasContent,
  pinnedContextStorageKey,
  pinnedContext,
  setPinnedContext,
  setProxxReachable,
  setProxxConfigured,
  setProxxModels,
}: UseChatPagePersistenceSuiteParams) {
  useChatSessionPersistence({
    makeId,
    sessionId,
    setSessionId,
    systemPrompt,
    setSystemPrompt,
    selectedModel,
    setSelectedModel,
    conversationId,
    setConversationId,
    messages,
    setMessages,
    latestRun,
    setLatestRun,
    runtimeEvents,
    setRuntimeEvents,
    isSending,
    setIsSending,
    sidebarWidthPx,
    setSidebarWidthPx,
    pendingAssistantIdRef,
    activeRunIdRef,
    sessionIdKey,
    sessionStateKey,
    sidebarWidthKey,
  });

  useScratchpadPersistence({
    storageKey: scratchpadStorageKey,
    canvasTitle,
    setCanvasTitle,
    canvasSubject,
    setCanvasSubject,
    canvasPath,
    setCanvasPath,
    canvasRecipients,
    setCanvasRecipients,
    canvasCc,
    setCanvasCc,
    canvasContent,
    setCanvasContent,
  });

  usePinnedContextPersistence({
    storageKey: pinnedContextStorageKey,
    pinnedContext,
    setPinnedContext,
  });

  useProxxStatusPolling({
    selectedModel,
    setSelectedModel,
    setProxxReachable,
    setProxxConfigured,
    setProxxModels,
  });
}
