import { fireEvent, render, screen } from "@testing-library/react";
import { beforeAll, describe, expect, it, vi } from "vitest";

import { ChatMainPane } from "./ChatMainPane";
import type { ChatMessage, ProxxModelInfo, ToolCatalogResponse } from "../../lib/types";

vi.mock("../ChatComposer", () => ({
  default: ({ onSend, isSending }: { onSend: (value: string) => void; isSending: boolean }) => (
    <button disabled={isSending} onClick={() => onSend("hello")}>send</button>
  ),
}));

vi.mock("../ConsolePanel", () => ({
  default: ({ lines }: { lines: string[] }) => <div data-testid="console-panel">{lines.join("\n")}</div>,
}));

vi.mock("./ChatRuntimePanel", () => ({
  ChatRuntimePanel: () => <div data-testid="runtime-panel">runtime</div>,
}));

vi.mock("./ChatScratchpadPanel", () => ({
  ChatScratchpadPanel: () => <div data-testid="scratchpad-panel">scratchpad</div>,
}));

vi.mock("./ChatSettingsPanel", () => ({
  ChatSettingsPanel: () => <div data-testid="settings-panel">settings</div>,
}));

vi.mock("./ChatMessageList", () => ({
  ChatMessageList: ({ messages }: { messages: ChatMessage[] }) => (
    <div data-testid="message-list">
      {messages.map((message) => <div key={message.id}>{message.content}</div>)}
    </div>
  ),
}));

class ResizeObserverMock {
  public observe() {}
  public disconnect() {}
  public unobserve() {}
}

beforeAll(() => {
  vi.stubGlobal("ResizeObserver", ResizeObserverMock);
});

const noop = () => {};
const noopAsync = async () => {};

const baseModels: ProxxModelInfo[] = [{ id: "gemma4:31b", name: "gemma4:31b" }];
const baseToolCatalog: ToolCatalogResponse | null = null;

function makeMessage(id: string, content: string): ChatMessage {
  return {
    id,
    role: "assistant",
    content,
    status: "streaming",
  };
}

function makeProps(messages: ChatMessage[]) {
  return {
    showFiles: true,
    showSettings: false,
    showCanvas: false,
    showConsole: false,
    onShowFiles: noop,
    onToggleSettings: noop,
    onToggleCanvas: noop,
    onToggleConsole: noop,
    selectedModel: "gemma4:31b",
    onSelectedModelChange: noop,
    proxxModels: baseModels,
    proxxReachable: true,
    proxxConfigured: true,
    onNewChat: noop,
    systemPrompt: "",
    onSystemPromptChange: noop,
    conversationId: null,
    activeRole: "developer",
    onActiveRoleChange: noop,
    toolCatalog: baseToolCatalog,
    wsStatus: "connected" as const,
    isRecovering: false,
    latestRun: null,
    isSending: false,
    liveControlEnabled: false,
    liveControlText: "",
    onLiveControlTextChange: noop,
    queueingControl: null,
    onQueueLiveControl: noopAsync,
    activeRunId: null,
    hydrationSources: [],
    runtimeEvents: [],
    latestToolReceipts: [],
    liveToolReceipts: [],
    liveToolEvents: [],
    assistantSurfaceBackground: "black",
    assistantSurfaceBorder: "gray",
    assistantSurfaceText: "white",
    messages,
    consoleLines: [],
    onSend: noop,
    composerDisabled: false,
    onOpenHydrationSource: noopAsync,
    onPinHydrationSource: noop,
    onAppendToScratchpad: noop,
    onOpenMessageInCanvas: noop,
    onOpenSourceInPreview: noopAsync,
    onPinAssistantSource: noop,
    onPinMessageContext: noop,
    canvasTitle: "",
    onCanvasTitleChange: noop,
    canvasPath: "",
    onCanvasPathChange: noop,
    canvasSubject: "",
    onCanvasSubjectChange: noop,
    canvasRecipients: "",
    onCanvasRecipientsChange: noop,
    canvasCc: "",
    onCanvasCcChange: noop,
    canvasContent: "",
    onCanvasContentChange: noop,
    canvasStatus: null,
    savingCanvas: false,
    savingCanvasFile: false,
    sendingCanvas: false,
    onUseLatestAssistantInCanvas: noop,
    onSaveCanvasDraft: noopAsync,
    onSaveCanvasFile: noopAsync,
    onClearScratchpad: noop,
    onSendCanvasEmailAction: noopAsync,
  };
}

function renderPane(messages: ChatMessage[]) {
  return render(<ChatMainPane {...makeProps(messages)} />);
}

describe("ChatMainPane auto-scroll", () => {
  it("keeps following the transcript when the user is near the bottom", () => {
    const firstMessages = [makeMessage("m1", "hello")];
    const secondMessages = [makeMessage("m1", "hello there"), makeMessage("m2", "world")];
    const { rerender } = renderPane(firstMessages);
    const scrollRegion = screen.getByTestId("chat-scroll-region") as HTMLDivElement;
    const scrollTo = vi.fn();

    Object.defineProperty(scrollRegion, "clientHeight", { configurable: true, value: 200 });
    Object.defineProperty(scrollRegion, "scrollHeight", { configurable: true, writable: true, value: 400 });
    Object.defineProperty(scrollRegion, "scrollTop", { configurable: true, writable: true, value: 180 });
    Object.defineProperty(scrollRegion, "scrollTo", { configurable: true, value: scrollTo });

    fireEvent.scroll(scrollRegion);

    Object.defineProperty(scrollRegion, "scrollHeight", { configurable: true, writable: true, value: 640 });
    rerender(<ChatMainPane {...makeProps(secondMessages)} />);

    expect(scrollTo).toHaveBeenCalledWith({ top: 640, behavior: "auto" });
  });

  it("stops auto-scrolling once the user scrolls away from the bottom", () => {
    const firstMessages = [makeMessage("m1", "hello")];
    const secondMessages = [makeMessage("m1", "hello there"), makeMessage("m2", "world")];
    const { rerender } = renderPane(firstMessages);
    const scrollRegion = screen.getByTestId("chat-scroll-region") as HTMLDivElement;
    const scrollTo = vi.fn();

    Object.defineProperty(scrollRegion, "clientHeight", { configurable: true, value: 200 });
    Object.defineProperty(scrollRegion, "scrollHeight", { configurable: true, writable: true, value: 400 });
    Object.defineProperty(scrollRegion, "scrollTop", { configurable: true, writable: true, value: 0 });
    Object.defineProperty(scrollRegion, "scrollTo", { configurable: true, value: scrollTo });

    fireEvent.scroll(scrollRegion);
    scrollTo.mockClear();

    Object.defineProperty(scrollRegion, "scrollHeight", { configurable: true, writable: true, value: 640 });
    rerender(<ChatMainPane {...makeProps(secondMessages)} />);

    expect(scrollTo).not.toHaveBeenCalled();
  });
});
