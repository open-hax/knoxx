import type { ChangeEvent } from "react";
import { Badge, Button, Card, Input, Spinner } from "@open-hax/uxx";
import type { ContextBarExplorerProps, ContextBarFiltersProps } from "./types";

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
      <div className="knoxx-context-explorer-heading">
        <span>
          {semanticMode ? "Semantic Results" : "Files"} • {activeEntryCount}
          {semanticMode && semanticProjects.length ? ` in ${semanticProjects.join(", ")}` : ""}
        </span>
      </div>

      {!semanticMode && (
        <div
          className="knoxx-context-explorer-navigation"
        >
          <div
            className="knoxx-context-explorer-path"
            title={`/${currentPath || ""}`}
          >
            /{currentPath || ""}
          </div>
          <Button variant="ghost" size="sm" onClick={() => void onLoadDirectory(currentParentPath)}>
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
                className="knoxx-context-semantic-entry"
                style={{ background: previewData?.path === entry.path ? "var(--token-colors-alpha-blue-_15)" : "transparent" }}
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
                className="knoxx-context-file-entry"
                style={{ background: previewData?.path === entry.path ? "var(--token-colors-alpha-blue-_15)" : "transparent" }}
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
        <div className="knoxx-context-preview">
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

const VISIBILITY_OPTIONS = [
  { value: "all", label: "All" },
  { value: "internal", label: "🔒 Internal" },
  { value: "review", label: "👀 Review" },
  { value: "public", label: "🌐 Public" },
  { value: "archived", label: "📦 Archived" },
] as const;

const KIND_OPTIONS = [
  { value: "all", label: "All kinds" },
  { value: "docs", label: "Docs" },
  { value: "code", label: "Code" },
  { value: "config", label: "Config" },
  { value: "data", label: "Data" },
] as const;

export function ContextBarFilters({
  onEntryFilterChange, entryFilter, onSemanticQueryChange, semanticQuery,
  semanticMode, onClearSemanticSearch, semanticSearching, onSemanticSearch,
  onSessionActorFilterChange, sessionActorFilter, onExcludeEtaMuSessionsChange, excludeEtaMuSessions,
  onSourceFilterChange, sourceFilter, onDomainFilterChange, domainFilter,
  onPathPrefixFilterChange, pathPrefixFilter, visibilityFilter, onVisibilityFilterChange,
  statsByVisibility, kindFilter, onKindFilterChange, onNewDocument,
  onNewVisualDraft, workspaceJob, actorOptions, statusColor,
  ingestionStatus,
}: ContextBarFiltersProps) {
  return (
  <div
    className="knoxx-context-filters"
  >
    {/* Search - compact (optional for file browsing) */}
    {onEntryFilterChange && (
      <Input
        value={entryFilter ?? ""}
        onChange={(event: ChangeEvent<HTMLInputElement>) => onEntryFilterChange(event.target.value)}
        placeholder="Filter..."
        size="sm"
      />
    )}

    {/* Semantic search - inline (optional for chat workspace) */}
    {onSemanticQueryChange && (
      <div style={{ display: "flex", gap: 4 }}>
        <div style={{ flex: 1 }}>
          <Input
            value={semanticQuery ?? ""}
            onChange={(event: ChangeEvent<HTMLInputElement>) => onSemanticQueryChange(event.target.value)}
            placeholder="Semantic search..."
            size="sm"
          />
        </div>
        {semanticMode ? (
          <Button variant="ghost" size="sm" onClick={onClearSemanticSearch ?? (() => {})}>✕</Button>
        ) : (
          <Button variant="secondary" size="sm" loading={semanticSearching} onClick={() => onSemanticSearch && void onSemanticSearch()}>
            ⚲
          </Button>
        )}
      </div>
    )}

    {onSessionActorFilterChange && (
      <div style={{ display: "flex", gap: 4, alignItems: "center" }}>
        <select
          aria-label="Session actor filter"
              value={sessionActorFilter ?? "all"}
          onChange={(event) => onSessionActorFilterChange(event.target.value)}
          className="knoxx-context-select"
        >
          {actorOptions.map((actor) => (
            <option key={actor.id} value={actor.id}>{actor.label}</option>
          ))}
        </select>
        {onExcludeEtaMuSessionsChange ? (
          <Button
            variant={excludeEtaMuSessions ? "secondary" : "ghost"}
            size="sm"
            onClick={() => onExcludeEtaMuSessionsChange(!excludeEtaMuSessions)}
            title={excludeEtaMuSessions ? "Show eta-mu sessions" : "Hide eta-mu sessions"}
          >
            {excludeEtaMuSessions ? "eta-mu off" : "eta-mu on"}
          </Button>
        ) : null}
      </div>
    )}

    {/* CMS-specific filters (optional) */}
    {onSourceFilterChange && (
      <Input
        value={sourceFilter ?? ""}
        onChange={(event: ChangeEvent<HTMLInputElement>) => onSourceFilterChange(event.target.value)}
        placeholder="Source filter..."
        size="sm"
      />
    )}
    {onDomainFilterChange && (
      <Input
        value={domainFilter ?? ""}
        onChange={(event: ChangeEvent<HTMLInputElement>) => onDomainFilterChange(event.target.value)}
        placeholder="Domain filter..."
        size="sm"
      />
    )}
    {onPathPrefixFilterChange && (
      <Input
        value={pathPrefixFilter ?? ""}
        onChange={(event: ChangeEvent<HTMLInputElement>) => onPathPrefixFilterChange(event.target.value)}
        placeholder="Path prefix..."
        size="sm"
      />
    )}

    {/* Filters - compact inline */}
    <div style={{ display: "flex", gap: 4 }}>
      <select
        aria-label="Visibility filter"
            value={visibilityFilter}
        onChange={(e) => onVisibilityFilterChange(e.target.value)}
        className="knoxx-context-select"
      >
        {VISIBILITY_OPTIONS.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}{statsByVisibility[opt.value] !== undefined ? ` (${statsByVisibility[opt.value]})` : ""}
          </option>
        ))}
      </select>
      <select
        aria-label="Content kind filter"
            value={kindFilter}
        onChange={(e) => onKindFilterChange(e.target.value)}
        className="knoxx-context-select"
      >
        {KIND_OPTIONS.map((opt) => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
    </div>

    {/* Action buttons */}
    {onNewDocument && (
      <Button variant="primary" size="sm" fullWidth onClick={onNewDocument}>
        + New Document
      </Button>
    )}
    {onNewVisualDraft && (
      <Button variant="secondary" size="sm" fullWidth onClick={onNewVisualDraft}>
        + New Visual Draft
      </Button>
    )}

    {/* Minimal status line */}
    {workspaceJob && (
      <div style={{ display: "flex", alignItems: "center", gap: 6, fontSize: 10, color: "var(--token-colors-text-muted)" }}>
        <span style={{ width: 6, height: 6, borderRadius: 999, background: statusColor, flexShrink: 0 }} />
        <span>
          {ingestionStatus === "running"
            ? `${workspaceJob.processed_files}/${workspaceJob.total_files || 0} files`
            : ingestionStatus === "completed"
            ? `${workspaceJob.chunks_created} chunks indexed`
            : ingestionStatus === "failed"
            ? "Ingestion failed"
            : "Ready"}
        </span>
      </div>
    )}
  </div>

  );
}
