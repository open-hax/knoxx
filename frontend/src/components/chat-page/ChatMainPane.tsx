import { useCallback, useEffect, useLayoutEffect, useRef } from 'react';
import { Badge, Button, SearchableSelect } from '@open-hax/uxx';
import ChatComposer from '../ChatComposer';
import ConsolePanel from '../ConsolePanel';
import { ChatMessageList } from './ChatMessageList';
import { ChatRuntimePanel } from './ChatRuntimePanel';
import { ChatScratchpadPanel } from './ChatScratchpadPanel';
import { ChatSettingsPanel } from './ChatSettingsPanel';
import type { AgentContractCatalogItem, ChatMessage, ProxxModelInfo, RunDetail, RunEvent, ToolCatalogResponse, ToolReceipt } from '../../lib/types';
import { THINKING_OPTIONS } from '../../lib/api/contracts';
import type { HydrationSource } from './types';

const EMPTY_STATE = {
  title: 'Chat',
  body: 'Ask Knoxx anything about devel, your client work, or the artifact you are actively building.',
  detail: 'Use the context bar like an IDE explorer, pin the context that matters, and use the canvas as your live working surface.',
} as const;

type ChatMainPaneProps = {
  showFiles: boolean;
  showSettings: boolean;
  showCanvas: boolean;
  showConsole: boolean;
  showCanvasToggle?: boolean;
  onShowFiles: () => void;
  onToggleSettings: () => void;
  onToggleCanvas: () => void;
  onToggleConsole: () => void;
  selectedModel: string;
  onSelectedModelChange: (value: string) => void;
  selectedThinkingLevel: string;
  onSelectedThinkingLevelChange: (value: string) => void;
  proxxModels: ProxxModelInfo[];
  proxxReachable: boolean;
  proxxConfigured: boolean;
  onNewChat: () => void;
  systemPrompt: string;
  onSystemPromptChange: (value: string) => void;
  conversationId: string | null;
  activeRole: string;
  activeAgentId: string;
  availableAgents: AgentContractCatalogItem[];
  onActiveAgentChange: (value: string) => void;
  toolCatalog: ToolCatalogResponse | null;
  wsStatus: 'connected' | 'closed' | 'error' | 'connecting';
  isRecovering: boolean;
  latestRun: RunDetail | null;
  isSending: boolean;
  liveControlEnabled: boolean;
  liveControlText: string;
  onLiveControlTextChange: (value: string) => void;
  queueingControl: 'steer' | 'follow_up' | null;
  onQueueLiveControl: (kind: 'steer' | 'follow_up') => void | Promise<void>;
  abortingTurn: boolean;
  onAbortTurn: () => void | Promise<void>;
  activeRunId: string | null;
  hydrationSources: HydrationSource[];
  runtimeEvents: RunEvent[];
  latestToolReceipts: ToolReceipt[];
  liveToolReceipts: ToolReceipt[];
  liveToolEvents: RunEvent[];
  assistantSurfaceBackground: string;
  assistantSurfaceBorder: string;
  assistantSurfaceText: string;
  messages: ChatMessage[];
  consoleLines: string[];
  onSend: (text: string) => void;
  composerDisabled: boolean;
  onOpenHydrationSource: (source: HydrationSource) => void | Promise<void>;
  onPinHydrationSource: (source: HydrationSource) => void;
  onAppendToScratchpad: (text: string, heading?: string) => void;
  onOpenMessageInCanvas: (message: ChatMessage) => void;
  onOpenSourceInPreview: (source: NonNullable<ChatMessage['sources']>[number]) => void | Promise<void>;
  onPinAssistantSource: (source: NonNullable<ChatMessage['sources']>[number]) => void;
  onPinMessageContext: (row: NonNullable<ChatMessage['contextRows']>[number]) => void;
  canvasTitle: string;
  onCanvasTitleChange: (value: string) => void;
  canvasPath: string;
  onCanvasPathChange: (value: string) => void;
  canvasSubject: string;
  onCanvasSubjectChange: (value: string) => void;
  canvasRecipients: string;
  onCanvasRecipientsChange: (value: string) => void;
  canvasCc: string;
  onCanvasCcChange: (value: string) => void;
  canvasContent: string;
  onCanvasContentChange: (value: string) => void;
  canvasStatus: string | null;
  savingCanvas: boolean;
  savingCanvasFile: boolean;
  sendingCanvas: boolean;
  onUseLatestAssistantInCanvas: () => void;
  onSaveCanvasDraft: () => void | Promise<void>;
  onSaveCanvasFile: () => void | Promise<void>;
  onClearScratchpad: () => void;
  onSendCanvasEmailAction: () => void | Promise<void>;
};

export function ChatMainPane({
  showFiles,
  showSettings,
  showCanvas,
  showConsole,
  onShowFiles,
  showCanvasToggle = true,
  onToggleSettings,
  onToggleCanvas,
  onToggleConsole,
  selectedModel,
  onSelectedModelChange,
  selectedThinkingLevel,
  onSelectedThinkingLevelChange,
  proxxModels,
  proxxReachable,
  proxxConfigured,
  onNewChat,
  systemPrompt,
  onSystemPromptChange,
  conversationId,
  activeRole,
  activeAgentId,
  availableAgents,
  onActiveAgentChange,
  toolCatalog,
  wsStatus,
  isRecovering,
  latestRun,
  isSending,
  liveControlEnabled,
  liveControlText,
  onLiveControlTextChange,
  queueingControl,
  onQueueLiveControl,
  abortingTurn,
  onAbortTurn,
  activeRunId,
  hydrationSources,
  runtimeEvents,
  latestToolReceipts,
  liveToolReceipts,
  liveToolEvents,
  assistantSurfaceBackground,
  assistantSurfaceBorder,
  assistantSurfaceText,
  messages,
  consoleLines,
  onSend,
  composerDisabled,
  onOpenHydrationSource,
  onPinHydrationSource,
  onAppendToScratchpad,
  onOpenMessageInCanvas,
  onOpenSourceInPreview,
  onPinAssistantSource,
  onPinMessageContext,
  canvasTitle,
  onCanvasTitleChange,
  canvasPath,
  onCanvasPathChange,
  canvasSubject,
  onCanvasSubjectChange,
  canvasRecipients,
  onCanvasRecipientsChange,
  canvasCc,
  onCanvasCcChange,
  canvasContent,
  onCanvasContentChange,
  canvasStatus,
  savingCanvas,
  savingCanvasFile,
  sendingCanvas,
  onUseLatestAssistantInCanvas,
  onSaveCanvasDraft,
  onSaveCanvasFile,
  onClearScratchpad,
  onSendCanvasEmailAction,
}: ChatMainPaneProps) {
  const scrollContainerRef = useRef<HTMLDivElement | null>(null);
  const scrollContentRef = useRef<HTMLDivElement | null>(null);
  const shouldAutoScrollRef = useRef(true);

  const updateAutoScrollState = useCallback((container: HTMLDivElement) => {
    const remaining = container.scrollHeight - container.scrollTop - container.clientHeight;
    shouldAutoScrollRef.current = remaining <= 96;
  }, []);

  const scrollToBottom = useCallback(() => {
    const container = scrollContainerRef.current;
    if (!container) return;
    if (typeof container.scrollTo === 'function') {
      container.scrollTo({ top: container.scrollHeight, behavior: 'auto' });
      return;
    }
    container.scrollTop = container.scrollHeight;
  }, []);

  useLayoutEffect(() => {
    if (!shouldAutoScrollRef.current) return;
    scrollToBottom();
  }, [messages, latestToolReceipts, liveToolReceipts, liveToolEvents, isSending, scrollToBottom]);

  useEffect(() => {
    const container = scrollContainerRef.current;
    const content = scrollContentRef.current;
    if (!container || !content || typeof ResizeObserver === 'undefined') {
      return;
    }

    const observer = new ResizeObserver(() => {
      if (!shouldAutoScrollRef.current) return;
      scrollToBottom();
    });

    observer.observe(content);
    return () => observer.disconnect();
  }, [scrollToBottom]);

  return (
    <>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0, background: 'var(--token-monokai-bg-default)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 12px', borderBottom: '1px solid var(--token-colors-border-default)', flexShrink: 0 }}>
          {!showFiles ? <Button variant="ghost" size="sm" onClick={onShowFiles}>Files</Button> : null}
          <Button variant="ghost" size="sm" onClick={onToggleSettings}>Settings</Button>
          {showCanvasToggle ? <Button variant="ghost" size="sm" onClick={onToggleCanvas}>Canvas</Button> : null}
          <Button variant="ghost" size="sm" onClick={onToggleConsole}>Console</Button>
          <div style={{ flex: 1 }} />
          <select
            value={activeAgentId}
            onChange={(event) => onActiveAgentChange(event.target.value)}
            style={{
              minWidth: 170,
              borderRadius: 6,
              border: '1px solid var(--token-colors-border-subtle)',
              padding: '6px 8px',
              fontSize: 12,
              background: 'var(--token-colors-surface-input)',
              color: 'var(--token-colors-text-default)',
            }}
            title="Active agent contract"
          >
            {availableAgents.length === 0 ? <option value="">No agents</option> : null}
            {availableAgents.map((agent) => (
              <option key={agent.id} value={agent.id}>
                {agent.id}
              </option>
            ))}
          </select>
          <SearchableSelect
            options={proxxModels.map(m => m.id)}
            value={selectedModel}
            onChange={onSelectedModelChange}
            placeholder={proxxModels.length === 0 ? "No models" : "Select model"}
            disabled={proxxModels.length === 0}
            size="sm"
          />
          <label
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 6,
              fontSize: 12,
              color: 'var(--token-colors-text-muted)',
            }}
            title="Thinking level"
          >
            <span>Thinking</span>
            <select
              aria-label="Thinking level"
              value={selectedThinkingLevel}
              onChange={(event) => onSelectedThinkingLevelChange(event.target.value)}
              style={{
                minWidth: 110,
                borderRadius: 6,
                border: '1px solid var(--token-colors-border-subtle)',
                padding: '6px 8px',
                fontSize: 12,
                background: 'var(--token-colors-surface-input)',
                color: 'var(--token-colors-text-default)',
              }}
            >
              {THINKING_OPTIONS.map((value) => (
                <option key={`thinking-${value}`} value={value}>{value}</option>
              ))}
            </select>
          </label>
          <Badge variant={proxxReachable ? 'success' : proxxConfigured ? 'warning' : 'error'} size="sm" dot>
            {proxxReachable ? 'online' : proxxConfigured ? 'offline' : 'not configured'}
          </Badge>
          <Button variant="ghost" size="sm" onClick={onNewChat}>New Chat</Button>
        </div>

        {showSettings ? (
          <ChatSettingsPanel
            systemPrompt={systemPrompt}
            onSystemPromptChange={onSystemPromptChange}
            conversationId={conversationId}
            activeRole={activeRole}
            activeAgentId={activeAgentId}
            availableAgents={availableAgents}
            onActiveAgentChange={onActiveAgentChange}
            toolCatalog={toolCatalog}
          />
        ) : null}

        <div
          ref={scrollContainerRef}
          data-testid="chat-scroll-region"
          onScroll={(event) => updateAutoScrollState(event.currentTarget)}
          style={{ flex: 1, overflow: 'auto', padding: 16 }}
        >
          <div ref={scrollContentRef}>
            <ChatRuntimePanel
              wsStatus={wsStatus}
              isRecovering={isRecovering}
              latestRun={latestRun}
              isSending={isSending}
              selectedModel={selectedModel}
              liveControlEnabled={liveControlEnabled}
              liveControlText={liveControlText}
              onLiveControlTextChange={onLiveControlTextChange}
              queueingControl={queueingControl}
              onQueueLiveControl={onQueueLiveControl}
              abortingTurn={abortingTurn}
              onAbortTurn={onAbortTurn}
              conversationId={conversationId}
              activeRunId={activeRunId}
              hydrationSources={hydrationSources}
              runtimeEvents={runtimeEvents}
              latestToolReceipts={latestToolReceipts}
              assistantSurfaceBackground={assistantSurfaceBackground}
              assistantSurfaceBorder={assistantSurfaceBorder}
              assistantSurfaceText={assistantSurfaceText}
              onOpenHydrationSource={onOpenHydrationSource}
              onPinHydrationSource={onPinHydrationSource}
              onAppendToScratchpad={onAppendToScratchpad}
            />

            {messages.length === 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', color: 'var(--token-colors-text-muted)', gap: 8 }}>
                <div style={{ fontSize: 20, fontWeight: 600 }}>{EMPTY_STATE.title}</div>
                <div style={{ fontSize: 14 }}>{EMPTY_STATE.body}</div>
                <div style={{ fontSize: 13 }}>{EMPTY_STATE.detail}</div>
              </div>
            ) : (
              <ChatMessageList
                messages={messages}
                latestRun={latestRun}
                latestToolReceipts={latestToolReceipts}
                liveToolReceipts={liveToolReceipts}
                liveToolEvents={liveToolEvents}
                assistantSurfaceBackground={assistantSurfaceBackground}
                assistantSurfaceBorder={assistantSurfaceBorder}
                assistantSurfaceText={assistantSurfaceText}
                onSend={onSend}
                voiceReplyDisabled={composerDisabled}
                onOpenMessageInCanvas={onOpenMessageInCanvas}
                onOpenSourceInPreview={onOpenSourceInPreview}
                onPinAssistantSource={onPinAssistantSource}
                onAppendToScratchpad={onAppendToScratchpad}
                onPinMessageContext={onPinMessageContext}
              />
            )}
          </div>
        </div>

        <div style={{ padding: 12, borderTop: '1px solid var(--token-colors-border-default)', flexShrink: 0 }}>
          <ChatComposer onSend={onSend} isSending={composerDisabled} />
        </div>

        {showConsole ? (
          <div style={{ height: 220, borderTop: '1px solid var(--token-colors-border-default)', flexShrink: 0 }}>
            <ConsolePanel lines={consoleLines} />
            <div style={{ padding: '6px 12px', borderTop: '1px solid var(--token-colors-border-default)', fontSize: 11, color: 'var(--token-colors-text-muted)' }}>
              WebSocket: {wsStatus}
            </div>
          </div>
        ) : null}
      </div>

      {showCanvasToggle && showCanvas ? (
        <ChatScratchpadPanel
          canvasTitle={canvasTitle}
          onCanvasTitleChange={onCanvasTitleChange}
          canvasPath={canvasPath}
          onCanvasPathChange={onCanvasPathChange}
          canvasSubject={canvasSubject}
          onCanvasSubjectChange={onCanvasSubjectChange}
          canvasRecipients={canvasRecipients}
          onCanvasRecipientsChange={onCanvasRecipientsChange}
          canvasCc={canvasCc}
          onCanvasCcChange={onCanvasCcChange}
          canvasContent={canvasContent}
          onCanvasContentChange={onCanvasContentChange}
          canvasStatus={canvasStatus}
          savingCanvas={savingCanvas}
          savingCanvasFile={savingCanvasFile}
          sendingCanvas={sendingCanvas}
          toolCatalog={toolCatalog}
          onUseLatestAssistantInCanvas={onUseLatestAssistantInCanvas}
          onHide={onToggleCanvas}
          onSaveCanvasDraft={onSaveCanvasDraft}
          onSaveCanvasFile={onSaveCanvasFile}
          onClearScratchpad={onClearScratchpad}
          onSendCanvasEmailAction={onSendCanvasEmailAction}
        />
      ) : null}
    </>
  );
}
