import { Badge, Button, Card, Spinner } from "@open-hax/uxx";
import type { BrowseEntry, BrowseResponse, PreviewResponse, SemanticSearchMatch } from "./types";

type ChatWorkspaceSidebarExplorerProps = {
  semanticMode: boolean;
  activeEntryCount: number;
  currentPath: string;
  currentParentPath: string;
  semanticProjects: string[];
  loadingBrowse: boolean;
  browseData: BrowseResponse | null;
  semanticResults: SemanticSearchMatch[];
  filteredEntries: BrowseEntry[];
  previewData: PreviewResponse | null;
  loadingPreview: boolean;
  onPreviewFile: (path: string) => void | Promise<void>;
  onLoadDirectory: (path?: string) => void | Promise<void>;
  onPinSemanticResult: (entry: SemanticSearchMatch) => void;
  onAppendToScratchpad: (text: string, heading?: string) => void;
  onPinPreviewContext: () => void;
  onOpenPreviewInCanvas: () => void | Promise<void>;
};

export function ChatWorkspaceSidebarExplorer({
  semanticMode,
  activeEntryCount,
  currentPath,
  currentParentPath,
  semanticProjects,
  loadingBrowse,
  browseData,
  semanticResults,
  filteredEntries,
  previewData,
  loadingPreview,
  onPreviewFile,
  onLoadDirectory,
  onPinSemanticResult,
  onAppendToScratchpad,
  onPinPreviewContext,
  onOpenPreviewInCanvas,
}: ChatWorkspaceSidebarExplorerProps) {
  return (
    <div style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", overflow: "hidden" }}>
      <Card variant="outlined" padding="none" style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column" }}>
        <div style={{ padding: "6px 8px", fontSize: 10, color: "var(--token-colors-text-muted)", borderBottom: "1px solid var(--token-colors-alpha-bg-_08)", flexShrink: 0 }}>
          {semanticMode ? "Semantic hits" : "Explorer"} •
          {semanticMode ? `${activeEntryCount} semantic match(es)` : `${activeEntryCount} visible entr${activeEntryCount === 1 ? "y" : "ies"}`}
          {semanticMode && semanticProjects.length ? ` across ${semanticProjects.join(", ")}` : ""}
        </div>

        {!semanticMode ? (
          <div
            style={{
              padding: "6px 8px",
              borderBottom: "1px solid var(--token-colors-border-default)",
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              gap: 8,
              flexShrink: 0,
            }}
          >
            <div
              style={{
                fontSize: 10,
                color: "var(--token-colors-text-muted)",
                overflow: "hidden",
                textOverflow: "ellipsis",
                whiteSpace: "nowrap",
                minWidth: 0,
              }}
              title={`/${currentPath || ""}`}
            >
              /{currentPath || ""}
            </div>
            <Button variant="ghost" size="sm" disabled={!currentPath} onClick={() => void onLoadDirectory(currentParentPath)}>
              Up
            </Button>
          </div>
        ) : null}

        <div style={{ flex: 1, minHeight: 0, overflowY: "auto", borderBottom: "1px solid var(--token-colors-border-default)" }}>
          {loadingBrowse && !browseData ? (
            <div style={{ display: "flex", justifyContent: "center", padding: 24 }}>
              <Spinner size="md" />
            </div>
          ) : (
            <div>
              {semanticMode
                ? semanticResults.map((entry) => (
                    <button
                      key={`semantic:${entry.id}`}
                      type="button"
                      onClick={() => {
                        void onPreviewFile(entry.path);
                      }}
                      style={{
                        width: "100%",
                        textAlign: "left",
                        padding: "8px 8px",
                        border: "none",
                        borderBottom: "1px solid var(--token-colors-alpha-bg-_08)",
                        background: previewData?.path === entry.path ? "var(--token-colors-alpha-blue-_15)" : "transparent",
                        cursor: "pointer",
                      }}
                    >
                      <div style={{ display: "flex", alignItems: "center", gap: 6, minWidth: 0 }}>
                        <span style={{ width: 6, height: 6, borderRadius: 999, background: "var(--token-colors-accent-cyan)", flexShrink: 0 }} />
                        <span style={{ fontSize: 11, fontWeight: 600, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{entry.path}</span>
                      </div>
                      <div style={{ display: "flex", gap: 6, marginTop: 4, flexWrap: "wrap" }}>
                        {entry.project ? <Badge size="sm" variant="default">{entry.project}</Badge> : null}
                        {entry.kind ? <Badge size="sm" variant="default">{entry.kind}</Badge> : null}
                        {entry.distance != null ? <Badge size="sm" variant="info">{entry.distance.toFixed(3)}</Badge> : null}
                      </div>
                      {entry.snippet ? (
                        <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)", marginTop: 6, lineHeight: 1.5 }}>{entry.snippet}</div>
                      ) : null}
                      <div style={{ display: "flex", gap: 6, marginTop: 8, flexWrap: "wrap" }}>
                        <Button variant="ghost" size="sm" onClick={() => onPinSemanticResult(entry)}>Pin</Button>
                        <Button variant="ghost" size="sm" onClick={() => onAppendToScratchpad(entry.snippet || entry.path, entry.path)}>Insert</Button>
                      </div>
                    </button>
                  ))
                : filteredEntries.map((entry) => (
                    <button
                      key={`${entry.type}:${entry.path}`}
                      type="button"
                      onClick={() => {
                        if (entry.type === "dir") {
                          void onLoadDirectory(entry.path);
                        } else if (entry.previewable) {
                          void onPreviewFile(entry.path);
                        }
                      }}
                      style={{
                        width: "100%",
                        textAlign: "left",
                        padding: "5px 8px",
                        border: "none",
                        borderBottom: "1px solid var(--token-colors-alpha-bg-_08)",
                        background: previewData?.path === entry.path ? "var(--token-colors-alpha-blue-_15)" : "transparent",
                        cursor: "pointer",
                      }}
                      title={entry.last_error ?? entry.path}
                    >
                      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 6, minWidth: 0 }}>
                        <div style={{ display: "flex", alignItems: "center", gap: 6, minWidth: 0 }}>
                          <span style={{ fontSize: 11, color: "var(--token-colors-text-subtle)", flexShrink: 0 }}>{entry.type === "dir" ? "▸" : "·"}</span>
                          <span
                            style={{
                              width: 6,
                              height: 6,
                              borderRadius: 999,
                              background:
                                entry.ingestion_status === "failed"
                                  ? "var(--token-colors-accent-red)"
                                  : entry.ingestion_status === "ingested"
                                    ? "var(--token-colors-accent-green)"
                                    : entry.ingestion_status === "partial"
                                      ? "var(--token-colors-accent-cyan)"
                                      : "var(--token-colors-text-muted)",
                              flexShrink: 0,
                            }}
                          />
                          <span style={{ fontSize: 12, fontWeight: 500, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{entry.name}</span>
                        </div>
                        <div style={{ display: "flex", gap: 6, flexShrink: 0 }}>
                          {entry.failed_count && entry.failed_count > 0 ? (
                            <span style={{ fontSize: 10, color: "var(--token-colors-accent-red)" }}>{entry.failed_count} failed</span>
                          ) : null}
                          {entry.ingested_count && entry.ingested_count > 0 ? (
                            <span style={{ fontSize: 10, color: "var(--token-colors-text-muted)" }}>{entry.ingested_count}</span>
                          ) : null}
                        </div>
                      </div>
                      <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)", marginLeft: 19, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                        {entry.path}
                        {entry.last_ingested_at ? ` • ${entry.last_ingested_at}` : ""}
                      </div>
                      {entry.last_error ? (
                        <div style={{ fontSize: 10, color: "var(--token-colors-accent-red)", marginLeft: 19, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                          {entry.last_error}
                        </div>
                      ) : null}
                    </button>
                  ))}
            </div>
          )}
        </div>

        <div style={{ minHeight: 160, maxHeight: 220, overflowY: "auto", padding: 10, background: "var(--token-colors-alpha-bg-_08)", flexShrink: 0 }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 8 }}>
            <div style={{ fontSize: 11, fontWeight: 600 }}>Preview</div>
            {previewData ? (
              <div style={{ display: "flex", gap: 6, flexWrap: "wrap" }}>
                <Button variant="ghost" size="sm" onClick={onPinPreviewContext}>Pin</Button>
                <Button variant="ghost" size="sm" onClick={() => void onOpenPreviewInCanvas()}>Open in Scratchpad</Button>
                <Button variant="ghost" size="sm" onClick={() => onAppendToScratchpad(previewData.content, previewData.path)}>Insert</Button>
              </div>
            ) : null}
          </div>
          {loadingPreview ? (
            <Spinner size="sm" />
          ) : previewData ? (
            <>
              <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)", marginBottom: 8 }}>{previewData.path}</div>
              <pre style={{ margin: 0, whiteSpace: "pre-wrap", fontSize: 10, lineHeight: 1.5 }}>{previewData.content}</pre>
            </>
          ) : (
            <div style={{ fontSize: 11, color: "var(--token-colors-text-muted)" }}>Select a previewable file to inspect it.</div>
          )}
        </div>
      </Card>
    </div>
  );
}
