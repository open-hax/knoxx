import { useEffect, type Dispatch, type SetStateAction } from "react";
type SetState<T> = Dispatch<SetStateAction<T>>;
import type { ChangeEvent } from "react";
import { Button, Card, Input } from "@open-hax/uxx";
import type { ToolCatalogResponse } from "../../lib/types";

type ChatScratchpadPanelProps = {
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
  toolCatalog: ToolCatalogResponse | null;
  onUseLatestAssistantInCanvas: () => void;
  onHide: () => void;
  onSaveCanvasDraft: () => void | Promise<void>;
  onSaveCanvasFile: () => void | Promise<void>;
  onClearScratchpad: () => void;
  onSendCanvasEmailAction: () => void | Promise<void>;
};

export function ChatScratchpadPanel({
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
  toolCatalog,
  onUseLatestAssistantInCanvas,
  onHide,
  onSaveCanvasDraft,
  onSaveCanvasFile,
  onClearScratchpad,
  onSendCanvasEmailAction,
}: ChatScratchpadPanelProps) {
  return (
    <Card
      variant="default"
      padding="none"
      style={{ width: 420, flexShrink: 0, borderLeft: "1px solid var(--token-colors-border-default)", display: "flex", flexDirection: "column", minHeight: 0 }}
    >
      <div style={{ padding: 12, borderBottom: "1px solid var(--token-colors-border-default)", display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 }}>
        <div>
          <div style={{ fontSize: 14, fontWeight: 600 }}>Canvas</div>
          <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>
            Your active editor surface. Draft artifacts, open files, revise content, and save publishable documents from here.
          </div>
        </div>
        <div style={{ display: "flex", gap: 6 }}>
          <Button variant="ghost" size="sm" onClick={onUseLatestAssistantInCanvas}>Use Latest</Button>
          <Button variant="ghost" size="sm" onClick={onHide}>Hide</Button>
        </div>
      </div>

      <div style={{ height: "100%", minHeight: 0, display: "flex", flexDirection: "column", overflowY: "auto" }}>
        <div style={{ padding: 12, display: "grid", gap: 10, borderBottom: "1px solid var(--token-colors-border-default)", flexShrink: 0 }}>
          <Input value={canvasTitle} onChange={(event: ChangeEvent<HTMLInputElement>) => onCanvasTitleChange(event.target.value)} placeholder="Canvas title" size="sm" />
          <Input value={canvasPath} onChange={(event: ChangeEvent<HTMLInputElement>) => onCanvasPathChange(event.target.value)} placeholder="Workspace path for the artifact file" size="sm" />
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            <Button variant="secondary" size="sm" loading={savingCanvas} onClick={() => void onSaveCanvasDraft()}>Save document</Button>
            <Button variant="secondary" size="sm" loading={savingCanvasFile} onClick={() => void onSaveCanvasFile()}>Write file</Button>
            <Button variant="ghost" size="sm" onClick={onClearScratchpad}>New canvas</Button>
          </div>
          <details>
            <summary style={{ cursor: "pointer", fontSize: 11, fontWeight: 600, color: "var(--token-colors-text-muted)" }}>Optional delivery actions</summary>
            <div style={{ display: "grid", gap: 10, marginTop: 10 }}>
              <Input value={canvasSubject} onChange={(event: ChangeEvent<HTMLInputElement>) => onCanvasSubjectChange(event.target.value)} placeholder="Email subject" size="sm" />
              <Input value={canvasRecipients} onChange={(event: ChangeEvent<HTMLInputElement>) => onCanvasRecipientsChange(event.target.value)} placeholder="To: comma-separated emails" size="sm" />
              <Input value={canvasCc} onChange={(event: ChangeEvent<HTMLInputElement>) => onCanvasCcChange(event.target.value)} placeholder="Cc: comma-separated emails" size="sm" />
              <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                <Button variant="primary" size="sm" loading={sendingCanvas} disabled={!toolCatalog?.email_enabled} onClick={() => void onSendCanvasEmailAction()}>Send Email</Button>
                {!toolCatalog?.email_enabled ? <span style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>Email is optional and currently unavailable.</span> : null}
              </div>
            </div>
          </details>
          {canvasStatus ? <div style={{ fontSize: 11, color: "var(--token-colors-text-subtle)" }}>{canvasStatus}</div> : null}
        </div>

        <div style={{ flex: 1, minHeight: 0, padding: 12, display: "flex" }}>
          <textarea
            value={canvasContent}
            onChange={(event) => onCanvasContentChange(event.target.value)}
            placeholder="Work here like a canvas editor: specs, notes, drafts, document revisions, publishable markdown, implementation plans, or excerpts from pinned context..."
            style={{
              width: "100%",
              height: "100%",
              minHeight: 280,
              borderRadius: 8,
              border: "1px solid var(--token-colors-border-subtle)",
              padding: 12,
              fontSize: 13,
              lineHeight: 1.6,
              resize: "none",
              fontFamily: "var(--token-fontFamily-mono)",
              background: "var(--token-colors-surface-input)",
              color: "var(--token-colors-text-default)",
            }}
          />
        </div>
      </div>
    </Card>
  );
}

type ScratchpadSnapshot = {
  title?: string;
  subject?: string;
  path?: string;
  recipients?: string;
  cc?: string;
  content?: string;
};

type UseScratchpadPersistenceParams = {
  storageKey: string;
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
};

export function useScratchpadPersistence({
  storageKey,
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
}: UseScratchpadPersistenceParams) {
  useEffect(() => {
    if (typeof window === "undefined") return;
    try {
      const raw = window.localStorage.getItem(storageKey);
      if (!raw) return;
      const parsed = JSON.parse(raw) as ScratchpadSnapshot;
      if (typeof parsed.title === "string") setCanvasTitle(parsed.title);
      if (typeof parsed.subject === "string") setCanvasSubject(parsed.subject);
      if (typeof parsed.path === "string") setCanvasPath(parsed.path);
      if (typeof parsed.recipients === "string") setCanvasRecipients(parsed.recipients);
      if (typeof parsed.cc === "string") setCanvasCc(parsed.cc);
      if (typeof parsed.content === "string") setCanvasContent(parsed.content);
    } catch {
      // ignore storage failures
    }
  }, [setCanvasCc, setCanvasContent, setCanvasPath, setCanvasRecipients, setCanvasSubject, setCanvasTitle, storageKey]);

  useEffect(() => {
    if (typeof window === "undefined") return;
    try {
      window.localStorage.setItem(
        storageKey,
        JSON.stringify({
          title: canvasTitle,
          subject: canvasSubject,
          path: canvasPath,
          recipients: canvasRecipients,
          cc: canvasCc,
          content: canvasContent,
        } satisfies ScratchpadSnapshot),
      );
    } catch {
      // ignore storage failures
    }
  }, [storageKey, canvasTitle, canvasSubject, canvasPath, canvasRecipients, canvasCc, canvasContent]);
}
