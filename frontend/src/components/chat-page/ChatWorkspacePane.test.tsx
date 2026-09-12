import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ChatWorkspacePane } from "./ChatWorkspacePane";
import { RecentChatSessions } from "./ChatRuntimePanel";
import { MultimodalInput } from "./MultimodalInput";
import { createAttachmentPreview, getAttachmentType } from "./MultimodalContent";

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
    expect(wrapper.style.width).toBe("100%");
    expect(wrapper.style.height).toBe("100%");
    expect(wrapper.style.minHeight).toBe("0");
    expect(wrapper.style.overflow).toBe("hidden");
  });
});

function recentSessionProps() {
  return {
    recentSessions: [{session: "session-1", title: "Research notes", is_active: true, has_active_stream: true, event_count: 3}],
    recentSessionsHasMore: true, recentSessionsTotal: 7,
    loadingRecentSessions: false, loadingMoreRecentSessions: false,
    loadingMemorySessionId: null, conversationId: "session-1",
    onRefreshRecentSessions: vi.fn(), onLoadMoreRecentSessions: vi.fn(), onResumeMemorySession: vi.fn(),
  };
}

describe("Recent chat sessions", () => {
  it("shows live selected session state and resumes the exact session", () => {
    const props = recentSessionProps();
    render(<RecentChatSessions {...props} />);
    expect(screen.getByText("1/7")).toBeInTheDocument();
    expect(screen.getByText("Live")).toBeInTheDocument();
    expect(screen.getByText("Open")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", {name: "Reload"}));
    expect(props.onResumeMemorySession).toHaveBeenCalledWith("session-1");
    fireEvent.click(screen.getByRole("button", {name: "Refresh"}));
    expect(props.onRefreshRecentSessions).toHaveBeenCalledTimes(1);
  });

  it("loads another page near the scroll boundary and guards busy or exhausted lists", () => {
    const props = recentSessionProps();
    const view = render(<RecentChatSessions {...props} />);
    const list = screen.getByRole("region", {name: "Recent chat sessions"});
    Object.defineProperties(list, {scrollHeight: {value: 300}, clientHeight: {value: 100}, scrollTop: {value: 80}});
    fireEvent.scroll(list);
    expect(props.onLoadMoreRecentSessions).toHaveBeenCalledTimes(1);
    view.rerender(<RecentChatSessions {...props} loadingMoreRecentSessions />);
    fireEvent.scroll(list);
    expect(props.onLoadMoreRecentSessions).toHaveBeenCalledTimes(1);
    view.rerender(<RecentChatSessions {...props} recentSessionsHasMore={false} />);
    fireEvent.scroll(list);
    expect(props.onLoadMoreRecentSessions).toHaveBeenCalledTimes(1);
    expect(screen.getByText("End of recent sessions.")).toBeInTheDocument();
  });

  it("keeps an empty history refreshable without offering pagination", () => {
    const props = recentSessionProps();
    render(<RecentChatSessions {...props} recentSessions={[]} recentSessionsTotal={0} />);
    expect(screen.getByText("No OpenPlanner-backed Knoxx sessions yet.")).toBeInTheDocument();
    expect(screen.queryByRole("button", {name: "Load more"})).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", {name: "Refresh"}));
    expect(props.onRefreshRecentSessions).toHaveBeenCalledTimes(1);
  });
});

describe("Workspace media input", () => {
  it("keeps attachment wire metadata while accepting a browser file", async () => {
    const changed = vi.fn();
    const file = new File(["# Research"], "notes.md", {type: "text/markdown"});
    const view = render(<MultimodalInput attachments={[]} onAttachmentsChange={changed} />);
    fireEvent.change(view.container.querySelector('input[type="file"]')!, {target: {files: [file]}});
    await waitFor(() => expect(changed).toHaveBeenCalledTimes(1));
    expect(changed.mock.calls[0][0]).toEqual([
      {id: expect.any(String), file, preview: undefined, type: "document"},
    ]);
    expect(view.container.querySelector('input[type="file"]')).toHaveValue("");
  });

  it("retains rejected oversized files as visible error drafts without creating previews", async () => {
    const changed = vi.fn();
    const file = new File(["too large"], "large.png", {type: "image/png"});
    const view = render(<MultimodalInput attachments={[]} onAttachmentsChange={changed} maxSizeBytes={1} />);
    fireEvent.change(view.container.querySelector('input[type="file"]')!, {target: {files: [file]}});
    await waitFor(() => expect(changed).toHaveBeenCalledTimes(1));
    const [draft] = changed.mock.calls[0][0];
    expect(draft.type).toBe("image");
    expect(draft.error).toContain("File too large");
    expect(draft.preview).toBeUndefined();
    view.rerender(<MultimodalInput attachments={[draft]} onAttachmentsChange={changed} maxSizeBytes={1} />);
    expect(screen.getByText(/File too large/)).toBeInTheDocument();
  });

  it("reads image previews and classifies audio, video and document content", async () => {
    const file = new File(["image content"], "image.png", {type: "image/png"});
    expect(await createAttachmentPreview(file, "image")).toMatch(/^data:image\/png;base64,/);
    expect(getAttachmentType(new File([], "audio", {type: "audio/ogg"}))).toBe("audio");
    expect(getAttachmentType(new File([], "video", {type: "video/webm"}))).toBe("video");
    expect(getAttachmentType(new File([], "document", {type: "application/pdf"}))).toBe("document");
  });
});
