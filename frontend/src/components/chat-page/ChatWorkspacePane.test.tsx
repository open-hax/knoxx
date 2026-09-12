import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import ChatComposer from "../ChatComposer";
import { ContextBar } from "../context-bar/ContextBar";
import type { ContextBarProps } from "../context-bar/types";
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


describe("chat composer controls", () => {
  it("sends a trimmed draft, clears it and retains disabled undo", async () => {
    const onSend = vi.fn();
    render(<ChatComposer onSend={onSend} isSending={false} multimodalEnabled={false} undoDisabled />);
    expect(screen.getByTitle("Undo last turn")).toBeDisabled();
    expect(screen.getByTitle("Send")).toBeDisabled();
    const input = screen.getByRole("textbox");
    fireEvent.change(input, { target: { value: "  useful draft  " } });
    fireEvent.click(screen.getByTitle("Send"));
    await waitFor(() => expect(onSend).toHaveBeenCalledWith("useful draft", undefined));
    expect(input).toHaveValue("");
  });

  it("routes live steering and follow-up controls without sending a normal message", () => {
    const onSend = vi.fn();
    const onQueueLiveControl = vi.fn();
    const onAbortTurn = vi.fn();
    render(<ChatComposer onSend={onSend} isSending liveControlEnabled liveControlText="Revise the title" onQueueLiveControl={onQueueLiveControl} onAbortTurn={onAbortTurn} />);
    fireEvent.click(screen.getByTitle("Steer (Enter)"));
    fireEvent.click(screen.getByTitle("Queue follow-up (Ctrl+Enter)"));
    fireEvent.click(screen.getByTitle("Abort turn"));
    expect(onQueueLiveControl.mock.calls).toEqual([["steer"], ["follow_up"]]);
    expect(onAbortTurn).toHaveBeenCalledOnce();
    expect(onSend).not.toHaveBeenCalled();
  });

  it("shows the active voice toggle and changes the recording threshold", () => {
    const onVoiceThresholdChange = vi.fn();
    const onToggleAutoConversation = vi.fn();
    render(<ChatComposer onSend={vi.fn()} isSending={false} multimodalEnabled={false} ttsEnabled autoConversationEnabled autoRecording audioLevelRef={{ current: 0.02 }} voiceThreshold={0.015} onVoiceThresholdChange={onVoiceThresholdChange} onToggleAutoConversation={onToggleAutoConversation} />);
    expect(screen.getByTitle("Auto voice on")).toHaveClass("knoxx-chat-glyph-active");
    fireEvent.click(screen.getByTitle("Auto voice on"));
    expect(onToggleAutoConversation).toHaveBeenCalledOnce();
    fireEvent.change(screen.getByRole("slider"), { target: { value: "0.023" } });
    expect(onVoiceThresholdChange).toHaveBeenCalledWith(0.023);
  });
});


function contextProps(overrides: Partial<ContextBarProps> = {}): ContextBarProps {
  return { sidebarWidthPx: 320, sidebarPaneSplitPct: 50, sidebarSplitContainerRef: {current: null},
    onHide: vi.fn(), onStartSidebarPaneResize: vi.fn(), onStartSidebarWidthResize: vi.fn(),
    visibilityFilter: "all", kindFilter: "docs", statsTotal: 2, statsByVisibility: {internal: 2},
    onVisibilityFilterChange: vi.fn(), onKindFilterChange: vi.fn(), ...overrides };
}

describe("context explorer controls", () => {
  it("keeps CMS filters and creation actions independently controlled", () => {
    const props = contextProps({onNewDocument: vi.fn(), onNewVisualDraft: vi.fn()});
    render(<ContextBar {...props} />);
    fireEvent.change(screen.getByRole("combobox", {name: "Visibility filter"}), {target: {value: "internal"}});
    fireEvent.change(screen.getByRole("combobox", {name: "Content kind filter"}), {target: {value: "code"}});
    fireEvent.click(screen.getByRole("button", {name: "+ New Document"}));
    fireEvent.click(screen.getByRole("button", {name: "+ New Visual Draft"}));
    expect(props.onVisibilityFilterChange).toHaveBeenCalledWith("internal");
    expect(props.onKindFilterChange).toHaveBeenCalledWith("code");
    expect(props.onNewDocument).toHaveBeenCalledOnce();
    expect(props.onNewVisualDraft).toHaveBeenCalledOnce();
  });

  it("keeps actor exclusion and grouped semantic session matches", () => {
    const props = contextProps({semanticQuery: "draft", availableActors: [{id: "writer"}, {id: "writer"}],
      sessionActorFilter: "all", onSessionActorFilterChange: vi.fn(), excludeEtaMuSessions: true,
      onExcludeEtaMuSessionsChange: vi.fn(), onResumeMemorySession: vi.fn(), conversationId: "conversation-one",
      recentSessions: [{session: "conversation-one", title: "Review session", event_count: 2}],
      sessionSearchHits: [{session: "conversation-one", snippet: "First draft"}, {metadata: {session: "conversation-one"}, text: "Second draft"}]});
    render(<ContextBar {...props} />);
    const actors = screen.getByRole("combobox", {name: "Session actor filter"});
    expect(actors.querySelectorAll("option")).toHaveLength(2);
    fireEvent.change(actors, {target: {value: "writer"}});
    fireEvent.click(screen.getByTitle("Show eta-mu sessions"));
    fireEvent.click(screen.getByRole("button", {name: /Review session/}));
    expect(props.onSessionActorFilterChange).toHaveBeenCalledWith("writer");
    expect(props.onExcludeEtaMuSessionsChange).toHaveBeenCalledWith(false);
    expect(props.onResumeMemorySession).toHaveBeenCalledWith("conversation-one");
    expect(screen.getByText("First draft")).toBeInTheDocument();
    expect(screen.getByText("Current")).toBeInTheDocument();
  });

  it("opens files in CMS mode and previews them in chat mode", () => {
    const props = contextProps({filteredEntries: [{name: "notes.md", path: "docs/notes.md", type: "file", previewable: true}],
      onOpenFile: vi.fn(), onPreviewFile: vi.fn()});
    const mounted = render(<ContextBar {...props} />);
    fireEvent.click(screen.getByText("notes.md"));
    expect(props.onOpenFile).toHaveBeenCalledWith(props.filteredEntries![0]);
    expect(props.onPreviewFile).not.toHaveBeenCalled();
    mounted.rerender(<ContextBar {...props} onOpenFile={undefined} />);
    fireEvent.click(screen.getByText("notes.md"));
    expect(props.onPreviewFile).toHaveBeenCalledWith("docs/notes.md");
  });

  it("loads more sessions near the end and preserves the loading guard", () => {
    const props = contextProps({recentSessions: [{session: "conversation-one", title: "Review session", event_count: 2}],
      recentSessionsHasMore: true, onLoadMoreRecentSessions: vi.fn()});
    const mounted = render(<ContextBar {...props} />);
    const region = screen.getByRole("region", {name: "Context sessions"});
    Object.defineProperties(region, {scrollHeight: {value: 500}, clientHeight: {value: 200}, scrollTop: {value: 190}});
    fireEvent.scroll(region);
    expect(props.onLoadMoreRecentSessions).toHaveBeenCalledOnce();
    mounted.rerender(<ContextBar {...props} loadingMoreRecentSessions />);
    fireEvent.scroll(region);
    expect(props.onLoadMoreRecentSessions).toHaveBeenCalledOnce();
  });
});
