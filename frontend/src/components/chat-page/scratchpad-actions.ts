import type { Dispatch, SetStateAction } from "react";
import { sendEmailDraft, toolWrite } from "../../lib/api";
import type { ChatMessage, GroundedContextRow } from "../../lib/types";
import type { PinnedContextItem, PreviewResponse, SemanticSearchMatch } from "./types";
import {
  contextPath,
  fileNameFromPath,
  seedCanvasFromMessage,
  seedCanvasFromPreview,
  slugify,
  sourceUrlToPath,
} from "./utils";

type SetState<T> = Dispatch<SetStateAction<T>>;

type ChatScratchpadActionParams = {
  activeRole: string;
  messages: ChatMessage[];
  previewData: PreviewResponse | null;
  setPreviewData: SetState<PreviewResponse | null>;
  canvasTitle: string;
  setCanvasTitle: SetState<string>;
  canvasSubject: string;
  setCanvasSubject: SetState<string>;
  canvasPath: string;
  setCanvasPath: SetState<string>;
  canvasRecipients: string;
  canvasCc: string;
  canvasContent: string;
  setCanvasContent: SetState<string>;
  setCanvasStatus: SetState<string | null>;
  setPinnedContext: SetState<PinnedContextItem[]>;
  setShowCanvas: SetState<boolean>;
  setSavingCanvas: SetState<boolean>;
  setSavingCanvasFile: SetState<boolean>;
  setSendingCanvas: SetState<boolean>;
  setConsoleLines: SetState<string[]>;
};

export function createChatScratchpadActions({
  activeRole,
  messages,
  previewData,
  setPreviewData,
  canvasTitle,
  setCanvasTitle,
  canvasSubject,
  setCanvasSubject,
  canvasPath,
  setCanvasPath,
  canvasRecipients,
  canvasCc,
  canvasContent,
  setCanvasContent,
  setCanvasStatus,
  setPinnedContext,
  setShowCanvas,
  setSavingCanvas,
  setSavingCanvasFile,
  setSendingCanvas,
  setConsoleLines,
}: ChatScratchpadActionParams) {
  const appendConsoleLine = (line: string) => {
    setConsoleLines((prev) => [...prev.slice(-400), line]);
  };

  const openMessageInCanvas = (message: ChatMessage) => {
    const seeded = seedCanvasFromMessage(message);
    setCanvasTitle(seeded.title);
    setCanvasSubject(seeded.subject);
    setCanvasPath(`notes/scratchpads/${slugify(seeded.title)}.md`);
    setCanvasContent(seeded.content);
    setCanvasStatus(null);
    setShowCanvas(true);
  };

  const fetchPreviewData = async (path: string): Promise<PreviewResponse> => {
    const params = new URLSearchParams({ path });
    const response = await fetch(`/api/ingestion/file?${params.toString()}`);
    if (!response.ok) throw new Error(`Preview failed: ${response.status}`);
    return (await response.json()) as PreviewResponse;
  };

  const pinContextItem = (item: PinnedContextItem) => {
    setPinnedContext((prev) => {
      const next = [item, ...prev.filter((entry) => entry.path !== item.path)];
      return next.slice(0, 24);
    });
    setCanvasStatus(`Pinned ${item.title}`);
  };

  const unpinContextItem = (path: string) => {
    setPinnedContext((prev) => prev.filter((entry) => entry.path !== path));
  };

  const pinPreviewContext = () => {
    if (!previewData) return;
    pinContextItem({
      id: previewData.path,
      title: fileNameFromPath(previewData.path),
      path: previewData.path,
      snippet: previewData.content.slice(0, 240),
      kind: "file",
    });
  };

  const pinSemanticResult = (entry: SemanticSearchMatch) => {
    pinContextItem({
      id: entry.id,
      title: fileNameFromPath(entry.path),
      path: entry.path,
      snippet: entry.snippet,
      kind: "semantic",
    });
  };

  const pinMessageContext = (row: GroundedContextRow) => {
    const path = contextPath(row);
    pinContextItem({
      id: row.id,
      title: fileNameFromPath(path),
      path,
      snippet: row.snippet ?? row.text ?? "",
      kind: "message",
    });
  };

  const pinAssistantSource = (source: NonNullable<ChatMessage["sources"]>[number]) => {
    const path = sourceUrlToPath(source.url);
    if (!path) return;
    pinContextItem({
      id: path,
      title: source.title || fileNameFromPath(path),
      path,
      snippet: source.section,
      kind: "message",
    });
  };

  const appendToScratchpad = (text: string, heading?: string) => {
    setCanvasContent((prev) => {
      const prefix = prev.trim().length > 0 ? `${prev.trimEnd()}\n\n` : "";
      return `${prefix}${heading ? `## ${heading}\n\n` : ""}${text}`;
    });
    setShowCanvas(true);
  };

  const previewFileInCanvas = (preview: PreviewResponse) => {
    const seeded = seedCanvasFromPreview(preview);
    setCanvasTitle(seeded.title);
    setCanvasSubject(seeded.subject);
    setCanvasPath(seeded.path);
    setCanvasContent(seeded.content);
    setCanvasStatus(`Loaded ${seeded.path} into scratchpad.`);
    setShowCanvas(true);
  };

  const openPreviewInCanvas = async () => {
    if (!previewData) return;
    previewFileInCanvas(previewData);
  };

  const openPinnedInCanvas = async (item: PinnedContextItem) => {
    try {
      const preview = await fetchPreviewData(item.path);
      setPreviewData(preview);
      const seeded = seedCanvasFromPreview(preview);
      setCanvasTitle(seeded.title);
      setCanvasSubject(seeded.subject);
      setCanvasPath(seeded.path);
      setCanvasContent(seeded.content);
      setCanvasStatus(`Loaded ${item.path} into scratchpad.`);
      setShowCanvas(true);
    } catch (error) {
      appendToScratchpad(item.snippet || item.path, item.title);
      setCanvasStatus(`Loaded pinned context excerpt for ${item.title}.`);
      appendConsoleLine(`[scratchpad] preview load failed for ${item.path}: ${(error as Error).message}`);
    }
  };

  const insertPinnedIntoCanvas = (item: PinnedContextItem) => {
    appendToScratchpad(item.snippet || item.path, `${item.title} (${item.kind})`);
    setCanvasStatus(`Inserted pinned context from ${item.title}.`);
  };

  const clearScratchpad = () => {
    setCanvasTitle("Untitled scratchpad");
    setCanvasSubject("");
    setCanvasPath(`notes/scratchpads/${slugify(`scratchpad-${Date.now()}`)}.md`);
    setCanvasContent("");
    setCanvasStatus("Cleared scratchpad.");
  };

  const useLatestAssistantInCanvas = () => {
    const latestAssistant = [...messages].reverse().find((message) => message.role === "assistant");
    if (!latestAssistant) return;
    openMessageInCanvas(latestAssistant);
  };

  const saveCanvasDraft = async () => {
    setSavingCanvas(true);
    setCanvasStatus(null);
    try {
      const response = await fetch("/api/cms/documents", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: canvasTitle || "Untitled draft",
          content: canvasContent,
          domain: "outbox",
        }),
      });
      if (!response.ok) throw new Error(`Draft save failed: ${response.status}`);
      setCanvasStatus("Saved draft to CMS.");
    } catch (error) {
      setCanvasStatus(`Save failed: ${(error as Error).message}`);
    } finally {
      setSavingCanvas(false);
    }
  };

  const saveCanvasFile = async () => {
    setSavingCanvasFile(true);
    setCanvasStatus(null);
    try {
      const response = await toolWrite({
        role: activeRole,
        path: canvasPath,
        content: canvasContent,
        create_parents: true,
        overwrite: true,
      });
      setCanvasStatus(`Saved file to ${response.path}`);
    } catch (error) {
      setCanvasStatus(`File save failed: ${(error as Error).message}`);
    } finally {
      setSavingCanvasFile(false);
    }
  };

  const sendCanvasEmailAction = async () => {
    setSendingCanvas(true);
    setCanvasStatus(null);
    try {
      const to = canvasRecipients.split(",").map((value) => value.trim()).filter(Boolean);
      const cc = canvasCc.split(",").map((value) => value.trim()).filter(Boolean);
      if (to.length === 0) throw new Error("At least one recipient is required");
      const response = await sendEmailDraft({
        role: activeRole,
        to,
        cc,
        subject: canvasSubject || canvasTitle || "Untitled draft",
        markdown: canvasContent,
      });
      setCanvasStatus(`Sent to ${response.sent_to.join(", ")}`);
    } catch (error) {
      setCanvasStatus(`Email failed: ${(error as Error).message}`);
    } finally {
      setSendingCanvas(false);
    }
  };

  const openSourceInPreview = async (source: NonNullable<ChatMessage["sources"]>[number]) => {
    const path = sourceUrlToPath(source.url);
    if (!path) return;
    const preview = await fetchPreviewData(path);
    setPreviewData(preview);
  };

  return {
    appendToScratchpad,
    clearScratchpad,
    fetchPreviewData,
    insertPinnedIntoCanvas,
    openMessageInCanvas,
    openPinnedInCanvas,
    openPreviewInCanvas,
    openSourceInPreview,
    pinAssistantSource,
    pinContextItem,
    pinMessageContext,
    pinPreviewContext,
    pinSemanticResult,
    previewFileInCanvas,
    saveCanvasDraft,
    saveCanvasFile,
    sendCanvasEmailAction,
    unpinContextItem,
    useLatestAssistantInCanvas,
  };
}
