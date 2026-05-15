import { useCallback, useEffect, useState } from "react";
import { CollapsedPanelTab } from "../CollapsedPanelTab";

function safeSetItem(key: string, value: string) {
  try { localStorage.setItem(key, value); } catch { /* quota exceeded or private mode */ }
}

function safeGetItem(key: string): string | null {
  try { return localStorage.getItem(key); } catch { return null; }
}

export type BottomPanelProps = {
  children: React.ReactNode;
  label: string;
  storageKey: string;
  defaultHeight?: number;
  minHeight?: number;
  maxHeight?: number;
  className?: string;
  header?: React.ReactNode;
};

export function BottomPanel({
  children,
  label,
  storageKey,
  defaultHeight = 240,
  minHeight = 120,
  maxHeight = 600,
  className = "",
  header,
}: BottomPanelProps) {
  const [open, setOpen] = useState(() => {
    const stored = safeGetItem(`${storageKey}_open`);
    return stored !== null ? stored === "true" : true;
  });

  const [height, setHeight] = useState(() => {
    const stored = safeGetItem(`${storageKey}_height`);
    return stored ? Math.min(maxHeight, Math.max(minHeight, parseInt(stored, 10))) : defaultHeight;
  });

  useEffect(() => { safeSetItem(`${storageKey}_open`, String(open)); }, [open, storageKey]);
  useEffect(() => { safeSetItem(`${storageKey}_height`, String(height)); }, [height, storageKey]);

  const toggle = useCallback(() => setOpen((prev) => !prev), []);

  const startResize = useCallback(
    (event: React.MouseEvent<HTMLButtonElement>) => {
      event.preventDefault();
      const startY = event.clientY;
      const startHeight = height;
      document.body.style.cursor = "row-resize";
      document.body.style.userSelect = "none";

      const onMove = (moveEvent: MouseEvent) => {
        const deltaY = startY - moveEvent.clientY;
        const next = Math.min(maxHeight, Math.max(minHeight, startHeight + deltaY));
        setHeight(next);
      };

      const onUp = () => {
        window.removeEventListener("mousemove", onMove);
        window.removeEventListener("mouseup", onUp);
        document.body.style.cursor = "";
        document.body.style.userSelect = "";
      };

      window.addEventListener("mousemove", onMove);
      window.addEventListener("mouseup", onUp);
    },
    [height, minHeight, maxHeight]
  );

  if (!open) {
    return (
      <CollapsedPanelTab
        label={label}
        edge="bottom"
        onExpand={toggle}
        title={`Show ${label} panel`}
      />
    );
  }

  return (
    <div
      style={{
        height,
        minHeight: height,
        display: "flex",
        flexDirection: "column",
        minWidth: 0,
        overflow: "hidden",
        position: "relative",
        flexShrink: 0,
        borderTop: "1px solid var(--token-colors-border-default)",
      }}
      className={className}
    >
      <button
        type="button"
        aria-label={`Resize ${label} panel`}
        onMouseDown={startResize}
        style={{
          position: "absolute",
          top: -4,
          left: 0,
          right: 0,
          height: 8,
          cursor: "row-resize",
          zIndex: 10,
          border: "none",
          background: "transparent",
          padding: 0,
        }}
      />
      {header ? (
        <div
          style={{
            flexShrink: 0,
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: "6px 10px",
            borderBottom: "1px solid var(--token-colors-border-subtle)",
          }}
        >
          {header}
          <button
            type="button"
            onClick={toggle}
            style={{
              padding: "2px 8px",
              fontSize: "11px",
              border: "1px solid var(--token-colors-border-default)",
              borderRadius: 4,
              background: "var(--token-colors-background-surface)",
              color: "var(--token-colors-text-muted)",
              cursor: "pointer",
            }}
          >
            Collapse
          </button>
        </div>
      ) : null}
      <div style={{ flex: 1, minHeight: 0, overflow: "hidden" }}>
        {children}
      </div>
    </div>
  );
}
