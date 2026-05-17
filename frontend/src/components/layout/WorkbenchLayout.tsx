import { Component, useEffect, useMemo, useState } from "react";
import { useChatWorkspaceController } from "../chat-page/useChatWorkspaceController";
import { ChatWorkspacePane } from "../chat-page/ChatWorkspacePane";
import { Sidebar } from "./Sidebar";
import { MainArea } from "./MainArea";

class ChatErrorBoundary extends Component<
  { children: React.ReactNode },
  { hasError: boolean }
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false };
  }
  static getDerivedStateFromError() {
    return { hasError: true };
  }
  componentDidCatch(error: Error) {
    console.error("[ChatErrorBoundary] Chat panel crashed:", error);
  }
  render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: 16, fontSize: 12, color: "var(--token-colors-text-muted)" }}>
          Chat panel unavailable (storage quota exceeded). Try clearing browser data.
        </div>
      );
    }
    return this.props.children;
  }
}

export type WorkbenchLayoutProps = {
  /** Content for the left sidebar */
  leftSidebar: React.ReactNode;
  /** Content for the main area (above bottom panel) */
  mainContent: React.ReactNode;
  /** Optional bottom panel content */
  bottomPanel?: React.ReactNode;
  /** Currently selected item ID for context pinning */
  selectedId?: string | null;
  /** EDN/text of selected item for context snippet */
  selectedContext?: string;
  /** Storage key prefix for layout state */
  storagePrefix: string;
  /** Chat actor configuration */
  chatActorId?: string;
  chatRole?: string;
  /** Whether to show the chat panel */
  showChat?: boolean;
  /** Left sidebar header */
  leftHeader?: React.ReactNode;
  /** Chat sidebar header */
  chatHeader?: React.ReactNode;
  /** Left sidebar width config */
  leftDefaultWidth?: number;
  leftMinWidth?: number;
  leftMaxWidth?: number;
  /** Chat sidebar width config */
  chatDefaultWidth?: number;
  chatMinWidth?: number;
  chatMaxWidth?: number;
};

export function WorkbenchLayout({
  leftSidebar,
  mainContent,
  bottomPanel,
  selectedId,
  selectedContext,
  storagePrefix,
  chatActorId = "agent_librarian",
  chatRole = "agent_librarian",
  showChat = true,
  leftHeader,
  chatHeader,
  leftDefaultWidth = 320,
  leftMinWidth = 260,
  leftMaxWidth = 480,
  chatDefaultWidth = 420,
  chatMinWidth = 360,
  chatMaxWidth = 600,
}: WorkbenchLayoutProps) {
  const chatOptions = useMemo(() => ({
    initialShowCanvas: false,
    defaultRole: chatRole,
    defaultActorId: chatActorId,
    sessionIdKey: `${storagePrefix}_session_id`,
    scratchpadStorageKey: `${storagePrefix}_scratchpad_state`,
    pinnedContextStorageKey: `${storagePrefix}_pinned_context`,
    sessionStateKey: `${storagePrefix}_chat_session_state`,
    sidebarWidthKey: `${storagePrefix}_sidebar_width_px`,
  }), [storagePrefix, chatActorId, chatRole]);

  const chat = useChatWorkspaceController(chatOptions);

  // Pin selected item context to chat
  useEffect(() => {
    if (!selectedId || !chat) return;
    try {
      const pin = (chat as unknown as Record<string, unknown>).pinContextItem as
        | ((item: { id: string; title: string; path: string; snippet: string; kind: string }) => void)
        | undefined;
      if (pin) {
        pin({
          id: `${storagePrefix}:${selectedId}`,
          title: selectedId,
          path: `/${storagePrefix}/${selectedId}`,
          snippet: selectedContext ? selectedContext.slice(0, 120) : "",
          kind: "file",
        });
      }
    } catch (e) {
      console.warn("[WorkbenchLayout] Failed to pin context item:", e);
    }
  }, [selectedId, selectedContext, chat, storagePrefix]);

  return (
    <div style={{ display: "flex", height: "100%", minHeight: 0, overflow: "hidden", gap: 0 }}>
      <Sidebar
        edge="left"
        label="Workbench"
        storageKey={`${storagePrefix}_left`}
        defaultWidth={leftDefaultWidth}
        minWidth={leftMinWidth}
        maxWidth={leftMaxWidth}
        header={leftHeader}
      >
        {leftSidebar}
      </Sidebar>

      <MainArea bottomPanel={bottomPanel}>
        {mainContent}
      </MainArea>

      {showChat ? (
        <Sidebar
          edge="right"
          label="Chat"
          storageKey={`${storagePrefix}_chat`}
          defaultWidth={chatDefaultWidth}
          minWidth={chatMinWidth}
          maxWidth={chatMaxWidth}
          header={chatHeader}
        >
          <ChatErrorBoundary>
            <ChatWorkspacePane
              controller={chat}
              showFiles={false}
              showCanvasToggle={false}
              onShowFiles={() => {}}
            />
          </ChatErrorBoundary>
        </Sidebar>
      ) : null}
    </div>
  );
}
