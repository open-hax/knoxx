import { Badge, Button, Card, Spinner } from "@open-hax/uxx";
import type { BrowseEntry, BrowseResponse, PreviewResponse, SemanticSearchMatch } from "./types";

const VISIBILITY_ICONS: Record<string, string> = {
  internal: "🔒",
  review: "👀",
  public: "🌐",
  archived: "📦",
};

const VISIBILITY_COLORS: Record<string, string> = {
  internal: "var(--token-colors-text-muted)",
  review: "var(--token-colors-accent-orange)",
  public: "var(--token-colors-accent-green)",
  archived: "var(--token-colors-text-subtle)",
};

type ContextBarExplorerProps = {
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
  onPreviewFile?: (path: string) => void | Promise<void>;
  onOpenFile?: (entry: BrowseEntry) => void | Promise<void>;
  onLoadDirectory: (path?: string) => void | Promise<void>;
  onPinSemanticResult: (entry: SemanticSearchMatch) => void;
  onAppendToScratchpad: (text: string, heading?: string) => void;
  onPinPreviewContext: () => void;
  onOpenPreviewInCanvas: () => void | Promise<void>;
};

export function ContextBarExplorer({
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
  onOpenFile,
  onLoadDirectory,
  onPinSemanticResult,
  onAppendToScratchpad,
  onPinPreviewContext,
  onOpenPreviewInCanvas,
}: ContextBarExplorerProps) {
  return (
    <div style={{ flex: 1, minHeight: 0, display: "flex", flexDirection: "column", overflow: "hidden" }}>
      {/* Explorer header */}
      <div style={{ 
        padding: "4px 8px", 
        fontSize: 10, 
        color: "var(--token-colors-text-muted)", 
        borderBottom: "1px solid var(--token-colors-border-default)", 
        flexShrink: 0,
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
      }}>
        <span>
          {semanticMode ? "Semantic Results" : "Files"} • {activeEntryCount}
          {semanticMode && semanticProjects.length ? ` in ${semanticProjects.join(", ")}` : ""}
        </span>
      </div>

      {!semanticMode && (
        <div
          style={{
            padding: "4px 8px",
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
      )}

      {/* File list - IDE style */}
      <div style={{ flex: 1, minHeight: 0, overflowY: "auto" }}>
        {loadingBrowse && !browseData ? (
          <div style={{ display: "flex", justifyContent: "center", padding: 16 }}>
            <Spinner size="sm" />
          </div>
        ) : semanticMode ? (
          // Semantic results
          <div>
            {semanticResults.map((entry) => (
              <button
                key={`semantic:${entry.id}`}
                type="button"
                onClick={() => {
                  if (onOpenFile) {
                    // CMS mode: open in editor
                    void onOpenFile({
                      name: entry.path.split("/").pop() ?? entry.path,
                      path: entry.path,
                      type: "file",
                      previewable: true,
                    });
                  } else if (onPreviewFile) {
                    // Chat mode: show preview
                    void onPreviewFile(entry.path);
                  }
                }}
                style={{
                  width: "100%",
                  textAlign: "left",
                  padding: "4px 8px",
                  border: "none",
                  borderBottom: "1px solid var(--token-colors-alpha-bg-_08)",
                  background: previewData?.path === entry.path ? "var(--token-colors-alpha-blue-_15)" : "transparent",
                  cursor: "pointer",
                }}
              >
                <div style={{ display: "flex", alignItems: "center", gap: 4, minWidth: 0 }}>
                  <span style={{ fontSize: 10 }}>⚡</span>
                  <span style={{ fontSize: 11, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                    {entry.path.split("/").pop()}
                  </span>
                  {entry.distance != null && (
                    <Badge size="sm" variant="info">{(1 - entry.distance).toFixed(2)}</Badge>
                  )}
                </div>
                <div style={{ fontSize: 9, color: "var(--token-colors-text-muted)", marginLeft: 14, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                  {entry.path}
                </div>
                {entry.snippet && (
                  <div style={{ fontSize: 9, color: "var(--token-colors-text-subtle)", marginLeft: 14, marginTop: 2, lineHeight: 1.4 }}>
                    {entry.snippet.slice(0, 100)}...
                  </div>
                )}
              </button>
            ))}
          </div>
        ) : (
          // File entries - IDE style
          <div>
            {filteredEntries.map((entry) => (
              <button
                key={`${entry.type}:${entry.path}`}
                type="button"
                onClick={() => {
                  if (entry.type === "dir") {
                    void onLoadDirectory(entry.path);
                  } else if (onOpenFile) {
                    // CMS mode: open in editor
                    void onOpenFile(entry);
                  } else if (entry.previewable && onPreviewFile) {
                    // Chat mode: show preview
                    void onPreviewFile(entry.path);
                  }
                }}
                style={{
                  width: "100%",
                  textAlign: "left",
                  padding: "3px 8px",
                  border: "none",
                  borderBottom: "1px solid var(--token-colors-alpha-bg-_08)",
                  background: previewData?.path === entry.path ? "var(--token-colors-alpha-blue-_15)" : "transparent",
                  cursor: "pointer",
                }}
                title={entry.last_error ?? entry.path}
              >
                <div style={{ display: "flex", alignItems: "center", gap: 4, minWidth: 0 }}>
                  {/* Type indicator */}
                  <span style={{ fontSize: 10, color: "var(--token-colors-text-subtle)", width: 10, flexShrink: 0 }}>
                    {entry.type === "dir" ? "▸" : "·"}
                  </span>
                  
                  {/* Ingestion status dot */}
                  <span
                    style={{
                      width: 5,
                      height: 5,
                      borderRadius: 999,
                      background:
                        entry.ingestion_status === "failed"
                          ? "var(--token-colors-accent-red)"
                          : entry.ingestion_status === "ingested"
                            ? "var(--token-colors-accent-green)"
                            : entry.ingestion_status === "partial"
                              ? "var(--token-colors-accent-cyan)"
                              : "var(--token-colors-text-subtle)",
                      flexShrink: 0,
                    }}
                  />
                  
                  {/* Name */}
                  <span style={{ fontSize: 11, fontWeight: 500, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", flex: 1 }}>
                    {entry.name}
                  </span>
                  
                  {/* Visibility indicator */}
                  {entry.visibility && (
                    <span style={{ fontSize: 9 }} title={entry.visibility}>
                      {VISIBILITY_ICONS[entry.visibility] || ""}
                    </span>
                  )}
                  
                  {/* Chunk count */}
                  {entry.ingested_count && entry.ingested_count > 0 && (
                    <span style={{ fontSize: 9, color: "var(--token-colors-text-muted)", flexShrink: 0 }}>
                      {entry.ingested_count}
                    </span>
                  )}
                </div>
                
                {/* Error indicator */}
                {entry.last_error && (
                  <div style={{ fontSize: 9, color: "var(--token-colors-accent-red)", marginLeft: 19, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>
                    ⚠ {entry.last_error.slice(0, 50)}
                  </div>
                )}
              </button>
            ))}
            
            {filteredEntries.length === 0 && !loadingBrowse && (
              <div style={{ padding: 16, textAlign: "center", color: "var(--token-colors-text-muted)", fontSize: 11 }}>
                No files found
              </div>
            )}
          </div>
        )}
      </div>

      {/* Preview panel - only for chat mode (when onOpenFile is not provided) */}
      {!onOpenFile && (
        <div style={{ 
          minHeight: 80, 
          maxHeight: 120, 
          overflowY: "auto", 
          padding: 6, 
          background: "var(--token-colors-alpha-bg-_08)", 
          borderTop: "1px solid var(--token-colors-border-default)",
          flexShrink: 0 
        }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8, marginBottom: 4 }}>
            <span style={{ fontSize: 10, fontWeight: 600 }}>Preview</span>
            {previewData && (
              <div style={{ display: "flex", gap: 4 }}>
                <Button variant="ghost" size="sm" onClick={onPinPreviewContext}>Pin</Button>
                <Button variant="ghost" size="sm" onClick={() => void onOpenPreviewInCanvas()}>Open</Button>
              </div>
            )}
          </div>
          {loadingPreview ? (
            <Spinner size="sm" />
          ) : previewData ? (
            <>
              <div style={{ fontSize: 9, color: "var(--token-colors-text-muted)", marginBottom: 4 }}>{previewData.path}</div>
              <pre style={{ margin: 0, whiteSpace: "pre-wrap", fontSize: 9, lineHeight: 1.4, color: "var(--token-colors-text-subtle)", maxHeight: 60, overflow: "hidden" }}>
                {previewData.content.slice(0, 300)}{previewData.content.length > 300 ? "..." : ""}
              </pre>
            </>
          ) : (
            <div style={{ fontSize: 10, color: "var(--token-colors-text-muted)" }}>Select a file to preview</div>
          )}
        </div>
      )}
    </div>
  );
}
