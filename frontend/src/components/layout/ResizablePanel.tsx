import { useCallback, useEffect, useState } from "react";
import { CollapsedPanelTab } from "../CollapsedPanelTab";

function safeSetItem(key: string, value: string) {
  try {
    localStorage.setItem(key, value);
  } catch (err) {
    // Quota exceeded or private mode — silently degrade
    console.warn(`[ResizablePanel] localStorage setItem failed for ${key}:`, err);
  }
}

function safeGetItem(key: string): string | null {
  try {
    return localStorage.getItem(key);
  } catch (err) {
    return null;
  }
}

type ResizablePanelProps = {
  children: React.ReactNode;
  edge: "left" | "right";
  label: string;
  storageKey: string;
  defaultWidth?: number;
  minWidth?: number;
  maxWidth?: number;
  className?: string;
  header?: React.ReactNode;
};

export function ResizablePanel({
  children,
  edge,
  label,
  storageKey,
  defaultWidth = 320,
  minWidth = 240,
  maxWidth = 520,
  className = "",
  header,
}: ResizablePanelProps) {
  const [open, setOpen] = useState(() => {
    const stored = safeGetItem(`${storageKey}_open`);
    return stored !== null ? stored === "true" : true;
  });

  const [width, setWidth] = useState(() => {
    const stored = safeGetItem(`${storageKey}_width`);
    return stored ? Math.min(maxWidth, Math.max(minWidth, parseInt(stored, 10))) : defaultWidth;
  });

  useEffect(() => {
    safeSetItem(`${storageKey}_open`, String(open));
  }, [open, storageKey]);

  useEffect(() => {
    safeSetItem(`${storageKey}_width`, String(width));
  }, [width, storageKey]);

  const toggle = useCallback(() => setOpen((prev) => !prev), []);

  const startResize = useCallback(
    (event: React.MouseEvent<HTMLButtonElement>) => {
      event.preventDefault();
      const startX = event.clientX;
      const startWidth = width;
      const direction = edge === "left" ? 1 : -1;
      document.body.style.cursor = "col-resize";
      document.body.style.userSelect = "none";

      const onMove = (moveEvent: MouseEvent) => {
        const deltaX = (moveEvent.clientX - startX) * direction;
        const next = Math.min(maxWidth, Math.max(minWidth, startWidth + deltaX));
        setWidth(next);
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
    [width, edge, minWidth, maxWidth]
  );

  if (!open) {
    return (
      <CollapsedPanelTab
        label={label}
        edge={edge}
        onExpand={toggle}
        title={`Show ${label} panel`}
      />
    );
  }

  return (
    <div
      style={{
        width,
        minWidth: width,
        display: "flex",
        flexDirection: "column",
        minHeight: 0,
        overflow: "hidden",
        position: "relative",
      }}
      className={className}
    >
      {header ? (
        <div
          style={{
            flexShrink: 0,
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            padding: "6px 10px",
            borderBottom: "1px solid var(--token-colors-border-default)",
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
      <div style={{ flex: 1, minHeight: 0, overflow: "hidden", display: "flex", flexDirection: "column" }}>
        {children}
      </div>
      <button
        type="button"
        aria-label={`Resize ${label} panel`}
        onMouseDown={startResize}
        style={{
          position: "absolute",
          top: 0,
          [edge === "left" ? "right" : "left"]: 0,
          width: 4,
          height: "100%",
          cursor: "col-resize",
          zIndex: 10,
          border: "none",
          background: "transparent",
          padding: 0,
        }}
      />
    </div>
  );
}
