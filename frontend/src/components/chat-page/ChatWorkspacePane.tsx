import { ChatMainPane } from "./ChatMainPane";
import type { AgentContractCatalogItem, ChatMessage, ProxxModelInfo, RunDetail, RunEvent, ToolCatalogResponse, ToolReceipt } from "../../lib/types";
import type { HydrationSource } from "./types";
import type { ChatWorkspaceController } from "./useChatWorkspaceController";

type ChatWorkspacePaneProps = {
  controller: ChatWorkspaceController;
  showFiles: boolean;
  onShowFiles: () => void;
  showFilesToggle?: boolean;
  filesLabel?: string;
  showCanvasToggle?: boolean;
  onOpenHydrationSource?: (source: HydrationSource) => void | Promise<void>;
  onOpenSourceInPreview?: (source: NonNullable<ChatMessage["sources"]>[number]) => void | Promise<void>;
};

export function ChatWorkspacePane({
  controller,
  showFiles,
  onShowFiles,
  showFilesToggle = true,
  filesLabel = "Files",
  showCanvasToggle = true,
  onOpenHydrationSource,
  onOpenSourceInPreview,
}: ChatWorkspacePaneProps) {
  return (
    <div style={{ flex: 1, width: "100%", height: "100%", minWidth: 0, minHeight: 0, display: "flex", overflow: "hidden" }}>
      <ChatMainPane
        showFiles={showFiles}
        showSettings={controller.showSettings}
        showCanvas={controller.showCanvas}
        showCanvasToggle={showCanvasToggle}
        showConsole={controller.showConsole}
        onShowFiles={onShowFiles}
        showFilesToggle={showFilesToggle}
        filesLabel={filesLabel}
        onToggleSettings={controller.toggleSettings}
        onToggleCanvas={controller.toggleCanvas}
        onToggleConsole={controller.toggleConsole}
        selectedModel={controller.selectedModel}
        onSelectedModelChange={controller.setSelectedModel}
        selectedThinkingLevel={controller.selectedThinkingLevel}
        onSelectedThinkingLevelChange={controller.setSelectedThinkingLevel}
        proxxModels={controller.proxxModels}
        proxxReachable={controller.proxxReachable}
        proxxConfigured={controller.proxxConfigured}
        onNewChat={controller.handleNewChat}
        onUndoMessages={controller.handleUndoLastTurn}
        undoDisabled={controller.isSending || controller.isRecovering || !controller.messages.some((message) => message.role === "user")}
        systemPrompt={controller.systemPrompt}
        onSystemPromptChange={controller.setSystemPrompt}
        conversationId={controller.conversationId}
        activeRole={controller.activeRole}
        activeActorId={controller.activeActorId}
        activeAgentId={controller.activeAgentId}
        availableAgents={controller.availableAgents}
        onActiveAgentChange={controller.setActiveAgentId}
        toolCatalog={controller.toolCatalog}
        wsStatus={controller.wsStatus}
        isRecovering={controller.isRecovering}
        latestRun={controller.latestRun}
        isSending={controller.isSending}
        liveControlEnabled={controller.liveControlEnabled}
        liveControlText={controller.liveControlText}
        onLiveControlTextChange={controller.setLiveControlText}
        queueingControl={controller.queueingControl}
        onQueueLiveControl={controller.queueLiveControl}
        onVoiceSteer={controller.voiceSteer}
        abortingTurn={controller.abortingTurn}
        onAbortTurn={controller.abortTurn}
        activeRunId={controller.latestRun?.run_id ?? null}
        hydrationSources={controller.hydrationSources}
        runtimeEvents={controller.runtimeEvents}
        latestToolReceipts={controller.latestToolReceipts}
        liveToolReceipts={controller.liveToolReceipts}
        liveToolEvents={controller.liveToolEvents}
        assistantSurfaceBackground={controller.assistantSurfaceBackground}
        assistantSurfaceBorder={controller.assistantSurfaceBorder}
        assistantSurfaceText={controller.assistantSurfaceText}
        messages={controller.messages}
        consoleLines={controller.consoleLines}
        onSend={controller.handleSend}
        composerDisabled={controller.isSending || controller.isRecovering || !controller.selectedModel}
        onOpenHydrationSource={onOpenHydrationSource ?? controller.openHydrationSource}
        onPinHydrationSource={controller.pinHydrationSource}
        onAppendToScratchpad={controller.appendToScratchpad}
        onOpenMessageInCanvas={controller.openMessageInCanvas}
        onOpenSourceInPreview={onOpenSourceInPreview ?? controller.openSourceInPreview}
        onPinAssistantSource={controller.pinAssistantSource}
        onPinMessageContext={controller.pinMessageContext}
        canvasTitle={controller.canvasTitle}
        onCanvasTitleChange={controller.setCanvasTitle}
        canvasPath={controller.canvasPath}
        onCanvasPathChange={controller.setCanvasPath}
        canvasSubject={controller.canvasSubject}
        onCanvasSubjectChange={controller.setCanvasSubject}
        canvasRecipients={controller.canvasRecipients}
        onCanvasRecipientsChange={controller.setCanvasRecipients}
        canvasCc={controller.canvasCc}
        onCanvasCcChange={controller.setCanvasCc}
        canvasContent={controller.canvasContent}
        onCanvasContentChange={controller.setCanvasContent}
        canvasStatus={controller.canvasStatus}
        savingCanvas={controller.savingCanvas}
        savingCanvasFile={controller.savingCanvasFile}
        sendingCanvas={controller.sendingCanvas}
        onUseLatestAssistantInCanvas={controller.useLatestAssistantInCanvas}
        onSaveCanvasDraft={controller.saveCanvasDraft}
        onSaveCanvasFile={controller.saveCanvasFile}
        onClearScratchpad={controller.clearScratchpad}
        onSendCanvasEmailAction={controller.sendCanvasEmailAction}
        sttEnabled={controller.sttEnabled}
        ttsEnabled={controller.ttsEnabled}
        ttsDefaultVoiceId={controller.ttsDefaultVoiceId}
      />
    </div>
  );
}

// The workspace owns the main-pane presentation contract passed to its child.
export type ChatMainPaneProps = {
  showFiles: boolean;
  showSettings: boolean;
  showCanvas: boolean;
  showConsole: boolean;
  showCanvasToggle?: boolean;
  onShowFiles: () => void;
  showFilesToggle?: boolean;
  filesLabel?: string;
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
  onUndoMessages: () => void | Promise<void>;
  undoDisabled: boolean;
  systemPrompt: string;
  onSystemPromptChange: (value: string) => void;
  conversationId: string | null;
  activeRole: string;
  activeActorId: string;
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
  onVoiceSteer: (text: string) => void | Promise<void>;
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
  sttEnabled?: boolean;
  ttsEnabled?: boolean;
  ttsDefaultVoiceId?: string;
};
