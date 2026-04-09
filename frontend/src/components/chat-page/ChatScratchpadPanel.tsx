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
          <div style={{ fontSize: 14, fontWeight: 600 }}>Scratchpad</div>
          <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>
            Your active working surface. Draft, reshape, pin context, and save artifacts from here.
          </div>
        </div>
        <div style={{ display: "flex", gap: 6 }}>
          <Button variant="ghost" size="sm" onClick={onUseLatestAssistantInCanvas}>Use Latest</Button>
          <Button variant="ghost" size="sm" onClick={onHide}>Hide</Button>
        </div>
      </div>

      <div style={{ height: "100%", minHeight: 0, display: "flex", flexDirection: "column", overflowY: "auto" }}>
        <div style={{ padding: 12, display: "grid", gap: 10, borderBottom: "1px solid var(--token-colors-border-default)", flexShrink: 0 }}>
          <Input value={canvasTitle} onChange={(event: ChangeEvent<HTMLInputElement>) => onCanvasTitleChange(event.target.value)} placeholder="Scratchpad title" size="sm" />
          <Input value={canvasPath} onChange={(event: ChangeEvent<HTMLInputElement>) => onCanvasPathChange(event.target.value)} placeholder="Workspace path for saving the scratchpad" size="sm" />
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
            <Button variant="secondary" size="sm" loading={savingCanvas} onClick={() => void onSaveCanvasDraft()}>Save to CMS</Button>
            <Button variant="secondary" size="sm" loading={savingCanvasFile} onClick={() => void onSaveCanvasFile()}>Save File</Button>
            <Button variant="ghost" size="sm" onClick={onClearScratchpad}>Clear</Button>
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
            placeholder="Work here like a ChatGPT canvas: specs, notes, plans, code sketches, prompts, drafts, implementation checklists, or excerpts from pinned context..."
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
