import { ChatMainPane } from "./ChatMainPane";
import type { ChatMessage } from "../../lib/types";
import type { HydrationSource } from "./types";
import type { ChatWorkspaceController } from "./useChatWorkspaceController";

type ChatWorkspacePaneProps = {
  controller: ChatWorkspaceController;
  showFiles: boolean;
  onShowFiles: () => void;
  showCanvasToggle?: boolean;
  onOpenHydrationSource?: (source: HydrationSource) => void | Promise<void>;
  onOpenSourceInPreview?: (source: NonNullable<ChatMessage["sources"]>[number]) => void | Promise<void>;
};

export function ChatWorkspacePane({
  controller,
  showFiles,
  onShowFiles,
  showCanvasToggle = true,
  onOpenHydrationSource,
  onOpenSourceInPreview,
}: ChatWorkspacePaneProps) {
  return (
    <div style={{ flex: 1, minWidth: 0, display: "flex" }}>
      <ChatMainPane
        showFiles={showFiles}
        showSettings={controller.showSettings}
        showCanvas={controller.showCanvas}
        showCanvasToggle={showCanvasToggle}
        showConsole={controller.showConsole}
        onShowFiles={onShowFiles}
        onToggleSettings={controller.toggleSettings}
        onToggleCanvas={controller.toggleCanvas}
        onToggleConsole={controller.toggleConsole}
        selectedModel={controller.selectedModel}
        onSelectedModelChange={controller.setSelectedModel}
        proxxModels={controller.proxxModels}
        proxxReachable={controller.proxxReachable}
        proxxConfigured={controller.proxxConfigured}
        onNewChat={controller.handleNewChat}
        systemPrompt={controller.systemPrompt}
        onSystemPromptChange={controller.setSystemPrompt}
        conversationId={controller.conversationId}
        activeRole={controller.activeRole}
        onActiveRoleChange={controller.setActiveRole}
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
        activeRunId={controller.activeRunId}
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
      />
    </div>
  );
}
