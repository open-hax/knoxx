import { useCallback, useEffect, useLayoutEffect, useRef } from 'react';
import { Badge, Button, SearchableSelect } from '@open-hax/uxx';
import { CollapsedPanelTab } from '../CollapsedPanelTab';
import ChatComposer from '../ChatComposer';
import ConsolePanel from '../ConsolePanel';
import { ChatMessageList } from './ChatMessageList';
import { ChatRuntimePanel } from './ChatRuntimePanel';
import { ChatScratchpadPanel } from './ChatScratchpadPanel';
import { ChatSettingsPanel } from './ChatSettingsPanel';
import { useConversationVoiceControls } from './useAutoConversationVoice';
import type { ChatMainPaneProps } from './ChatWorkspacePane';
import { THINKING_OPTIONS } from '../../lib/api/contracts';

const EMPTY_STATE = {
  title: 'Chat',
  body: 'Ask Knoxx anything about devel, your client work, or the artifact you are actively building.',
  detail: 'Use the context bar like an IDE explorer, pin the context that matters, and use the canvas as your live working surface.',
} as const;

export function ChatMainPane({
  showFiles,
  showSettings,
  showCanvas,
  showConsole,
  onShowFiles,
  showCanvasToggle = true,
  showFilesToggle = true,
  filesLabel = 'Files',
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
  onUndoMessages,
  undoDisabled,
  systemPrompt,
  onSystemPromptChange,
  conversationId,
  activeRole,
  activeActorId,
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
  onVoiceSteer,
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
  sttEnabled = false,
  ttsEnabled = false,
  ttsDefaultVoiceId = "",
}: ChatMainPaneProps) {
  const scrollContainerRef = useRef<HTMLDivElement | null>(null);
  const scrollContentRef = useRef<HTMLDivElement | null>(null);
  const shouldAutoScrollRef = useRef(true);
  const {
    autoConversationEnabled, setAutoConversationEnabled, autoRecording,
    voiceThreshold, setVoiceThreshold, autoConversationVoice,
    autoRecorderState, audioLevelRef,
  } = useConversationVoiceControls({ messages, ttsEnabled, sttEnabled, ttsDefaultVoiceId, isSending, onSend });
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
    <div style={{ flex: 1, display: 'flex', minWidth: 0, minHeight: 0, overflow: 'hidden' }}>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0, minHeight: 0, overflow: 'hidden', background: 'var(--token-monokai-bg-default)' }}>
        {/* Row 1: global status */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 12px', borderBottom: '1px solid var(--token-colors-border-default)', flexShrink: 0 }}>
          <div style={{ flex: 1 }} />
          <Badge variant={proxxReachable ? 'success' : proxxConfigured ? 'warning' : 'error'} size="sm" dot>
            {proxxReachable ? 'online' : proxxConfigured ? 'offline' : 'not configured'}
          </Badge>
        </div>

        {/* Row 2: Settings & Console */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 12px', borderBottom: '1px solid var(--token-colors-border-default)', flexShrink: 0 }}>
          <Button variant={showSettings ? 'primary' : 'ghost'} size="sm" onClick={onToggleSettings}>Settings</Button>
          <Button variant={showConsole ? 'primary' : 'ghost'} size="sm" onClick={onToggleConsole}>Console</Button>
        </div>

        {showSettings ? (
          <ChatSettingsPanel
            systemPrompt={systemPrompt}
            onSystemPromptChange={onSystemPromptChange}
            conversationId={conversationId}
            activeRole={activeRole}
            activeActorId={activeActorId}
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
                onOpenMessageInCanvas={onOpenMessageInCanvas}
                onOpenSourceInPreview={onOpenSourceInPreview}
                onPinAssistantSource={onPinAssistantSource}
                onAppendToScratchpad={onAppendToScratchpad}
                onPinMessageContext={onPinMessageContext}
              />
            )}
          </div>
        </div>

        {/* Bottom conversation controls */}
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 8, padding: '8px 12px', borderTop: '1px solid var(--token-colors-border-default)', flexShrink: 0 }}>
          <select
            value={activeAgentId}
            onChange={(event) => onActiveAgentChange(event.target.value)}
            style={{
              minWidth: 140,
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
          <div style={{ minWidth: 160, position: 'relative', zIndex: 20 }}>
            <SearchableSelect
              options={proxxModels.map(m => m.id)}
              value={selectedModel}
              onChange={onSelectedModelChange}
              placeholder={proxxModels.length === 0 ? "No models" : "Select model"}
              disabled={proxxModels.length === 0}
              size="sm"
            />
          </div>
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
                minWidth: 90,
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
        </div>

        <div style={{ padding: 12, borderTop: '1px solid var(--token-colors-border-default)', flexShrink: 0 }}>
          <ChatComposer
            onSend={onSend}
            isSending={composerDisabled}
            voiceInputEnabled={sttEnabled}
            liveControlEnabled={liveControlEnabled}
            liveControlText={liveControlText}
            onLiveControlTextChange={onLiveControlTextChange}
            queueingControl={queueingControl}
            onQueueLiveControl={onQueueLiveControl}
            onVoiceSteer={onVoiceSteer}
            abortingTurn={abortingTurn}
            onAbortTurn={onAbortTurn}
            latestAssistantContent={messages.filter((m) => m.role === "assistant" && m.status === "done").slice(-1)[0]?.content}
            autoConversationEnabled={autoConversationEnabled}
            onToggleAutoConversation={() => setAutoConversationEnabled((v) => !v)}
            ttsEnabled={ttsEnabled}
            ttsStatus={autoConversationVoice.status}
            ttsError={autoConversationVoice.error}
            onUndoMessages={onUndoMessages}
            undoDisabled={undoDisabled}
            onNewChat={onNewChat}
            autoRecording={autoRecording}
            voiceThreshold={voiceThreshold}
            onVoiceThresholdChange={setVoiceThreshold}
            audioLevelRef={audioLevelRef}
          />
          {autoConversationEnabled && autoConversationVoice.error ? (
            <div style={{ marginTop: 8, fontSize: 12, color: 'var(--token-colors-text-muted)' }}>
              Auto voice error: {autoConversationVoice.error}
            </div>
          ) : null}
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

      {/* Right stash tab or Canvas panel */}
      {showCanvasToggle && !showCanvas ? (
        <CollapsedPanelTab label="Canvas" edge="right" onExpand={onToggleCanvas} title="Show Canvas panel" />
      ) : null}
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
    </div>
  );
}
