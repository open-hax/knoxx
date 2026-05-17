type CollapsedPanelTabProps = {
  label: string;
  edge: "left" | "right" | "bottom";
  onExpand: () => void;
  title?: string;
};

export function CollapsedPanelTab({ label, edge, onExpand, title }: CollapsedPanelTabProps) {
  const vertical = edge === "left" || edge === "right";

  return (
    <button
      onClick={onExpand}
      title={title ?? `Show ${label} panel`}
      aria-label={title ?? `Show ${label} panel`}
      style={{
        ...(vertical
          ? {
              width: 28,
              flexShrink: 0,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              padding: 0,
            }
          : {
              height: 28,
              width: "100%",
              flexShrink: 0,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              padding: "0 8px",
            }),
        border: "none",
        ...(edge === "left" ? { borderRight: "1px solid var(--token-colors-border-default)" } : {}),
        ...(edge === "right" ? { borderLeft: "1px solid var(--token-colors-border-default)" } : {}),
        ...(edge === "bottom" ? { borderTop: "1px solid var(--token-colors-border-default)" } : {}),
        background: "var(--token-colors-background-surface)",
        color: "var(--token-colors-text-muted)",
        fontSize: "12px",
        cursor: "pointer",
      }}
    >
      <span style={vertical ? { writingMode: "vertical-rl", textOrientation: "mixed" } : undefined}>{label}</span>
    </button>
  );
}
