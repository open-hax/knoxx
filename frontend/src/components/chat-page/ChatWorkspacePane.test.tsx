import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ChatWorkspacePane } from "./ChatWorkspacePane";

vi.mock("./ChatMainPane", () => ({
  ChatMainPane: () => <div data-testid="chat-main-pane" />,
}));

describe("ChatWorkspacePane layout", () => {
  it("stays shrinkable inside embedded column layouts so chat can scroll", () => {
    render(
      <div style={{ display: "flex", flexDirection: "column", height: 480 }}>
        <div>header</div>
        <ChatWorkspacePane
          controller={{
            showSettings: false,
            showCanvas: false,
            showConsole: false,
            toggleSettings: vi.fn(),
            toggleCanvas: vi.fn(),
            toggleConsole: vi.fn(),
            selectedModel: "glm-5",
            setSelectedModel: vi.fn(),
            selectedThinkingLevel: "off",
            setSelectedThinkingLevel: vi.fn(),
            proxxModels: [],
            proxxReachable: true,
            proxxConfigured: true,
            handleNewChat: vi.fn(),
            systemPrompt: "",
            setSystemPrompt: vi.fn(),
            conversationId: null,
            activeRole: "contract_librarian",
            activeActorId: "contract_librarian",
            activeAgentId: "contract_librarian",
            availableAgents: [],
            setActiveAgentId: vi.fn(),
            toolCatalog: null,
            wsStatus: "connected",
            isRecovering: false,
            latestRun: null,
            isSending: false,
            liveControlEnabled: false,
            liveControlText: "",
            setLiveControlText: vi.fn(),
            queueingControl: null,
            queueLiveControl: vi.fn(),
            abortingTurn: false,
            abortTurn: vi.fn(),
            activeRunId: null,
            hydrationSources: [],
            runtimeEvents: [],
            latestToolReceipts: [],
            liveToolReceipts: [],
            liveToolEvents: [],
            assistantSurfaceBackground: "black",
            assistantSurfaceBorder: "gray",
            assistantSurfaceText: "white",
            messages: [],
            consoleLines: [],
            handleSend: vi.fn(),
            openHydrationSource: vi.fn(),
            pinHydrationSource: vi.fn(),
            appendToScratchpad: vi.fn(),
            openMessageInCanvas: vi.fn(),
            openSourceInPreview: vi.fn(),
            pinAssistantSource: vi.fn(),
            pinMessageContext: vi.fn(),
            canvasTitle: "",
            setCanvasTitle: vi.fn(),
            canvasPath: "",
            setCanvasPath: vi.fn(),
            canvasSubject: "",
            setCanvasSubject: vi.fn(),
            canvasRecipients: "",
            setCanvasRecipients: vi.fn(),
            canvasCc: "",
            setCanvasCc: vi.fn(),
            canvasContent: "",
            setCanvasContent: vi.fn(),
            canvasStatus: null,
            savingCanvas: false,
            savingCanvasFile: false,
            sendingCanvas: false,
            useLatestAssistantInCanvas: vi.fn(),
            saveCanvasDraft: vi.fn(),
            saveCanvasFile: vi.fn(),
            clearScratchpad: vi.fn(),
            sendCanvasEmailAction: vi.fn(),
          } as never}
          showFiles={false}
          onShowFiles={vi.fn()}
          showCanvasToggle={false}
        />
      </div>,
    );

    const wrapper = screen.getByTestId("chat-main-pane").parentElement as HTMLDivElement;
    expect(wrapper.style.minHeight).toBe("0");
    expect(wrapper.style.overflow).toBe("hidden");
  });
});
