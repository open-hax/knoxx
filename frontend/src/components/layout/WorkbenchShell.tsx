import { useCallback, useEffect, useRef, useState } from "react";
import { safeGetItem, safeSetItem } from "../../lib/storage";
import { CollapsedPanelTab } from "../CollapsedPanelTab";

export type WorkbenchShellProps = {
  children: React.ReactNode;
  className?: string;
};

export function WorkbenchShell({ children, className = "" }: WorkbenchShellProps) {
  return (
    <div
      className={className}
      style={{
        display: "flex",
        height: "100%",
        minHeight: 0,
        overflow: "hidden",
        gap: 0,
      }}
    >
      {children}
    </div>
  );
}

export type WorkbenchPanelProps = {
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

export function WorkbenchPanel({
  children,
  edge,
  label,
  storageKey,
  defaultWidth = 320,
  minWidth = 200,
  maxWidth = 600,
  className = "",
  header,
}: WorkbenchPanelProps) {
  const [open, setOpen] = useState(() => {
    const stored = safeGetItem(`${storageKey}_open`);
    return stored !== null ? stored === "true" : true;
  });

  const [width, setWidth] = useState(() => {
    const stored = safeGetItem(`${storageKey}_width`);
    return stored
      ? Math.min(maxWidth, Math.max(minWidth, parseInt(stored, 10)))
      : defaultWidth;
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
    <>
      <div
        className={className}
        style={{
          width,
          minWidth: width,
          maxWidth: width,
          display: "flex",
          flexDirection: "column",
          minHeight: 0,
          overflow: "hidden",
          flexShrink: 0,
        }}
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
              gap: 8,
            }}
          >
            <div style={{ minWidth: 0, overflow: "hidden" }}>{header}</div>
            <button
              type="button"
              onClick={toggle}
              title={`Collapse ${label}`}
              style={{
                padding: "2px 8px",
                fontSize: "11px",
                border: "1px solid var(--token-colors-border-default)",
                borderRadius: 4,
                background: "var(--token-colors-background-surface)",
                color: "var(--token-colors-text-muted)",
                cursor: "pointer",
                flexShrink: 0,
              }}
            >
              Collapse
            </button>
          </div>
        ) : null}
        <div
          style={{
            flex: 1,
            minHeight: 0,
            overflow: "hidden",
            display: "flex",
            flexDirection: "column",
          }}
        >
          {children}
        </div>
      </div>
      {/* Resize handle */}
      <button
        type="button"
        aria-label={`Resize ${label}`}
        onMouseDown={startResize}
        style={{
          width: 6,
          minWidth: 6,
          cursor: "col-resize",
          flexShrink: 0,
          display: "flex",
          alignItems: "stretch",
          justifyContent: "center",
          background: "transparent",
          border: "none",
          padding: 0,
        }}
        title={`Resize ${label}`}
      >
        <div
          style={{
            width: 1,
            background: "var(--token-colors-border-default)",
            margin: "4px 0",
            transition: "background 0.15s",
          }}
        />
      </button>
    </>
  );
}

export type WorkbenchMainProps = {
  children: React.ReactNode;
  className?: string;
};

export function WorkbenchMain({ children, className = "" }: WorkbenchMainProps) {
  return (
    <div
      className={className}
      style={{
        flex: 1,
        minWidth: 0,
        overflow: "hidden",
        display: "flex",
        flexDirection: "column",
        minHeight: 0,
      }}
    >
      {children}
    </div>
  );
}

export type WorkbenchBottomPanelProps = {
  children: React.ReactNode;
  label: string;
  storageKey: string;
  defaultHeight?: number;
  minHeight?: number;
  maxHeight?: number;
  className?: string;
  header?: React.ReactNode;
};

export function WorkbenchBottomPanel({
  children,
  label,
  storageKey,
  defaultHeight = 240,
  minHeight = 120,
  maxHeight = 600,
  className = "",
  header,
}: WorkbenchBottomPanelProps) {
  const [open, setOpen] = useState(() => {
    const stored = safeGetItem(`${storageKey}_open`);
    return stored !== null ? stored === "true" : true;
  });

  const [height, setHeight] = useState(() => {
    const stored = safeGetItem(`${storageKey}_height`);
    return stored
      ? Math.min(maxHeight, Math.max(minHeight, parseInt(stored, 10)))
      : defaultHeight;
  });

  useEffect(() => {
    safeSetItem(`${storageKey}_open`, String(open));
  }, [open, storageKey]);

  useEffect(() => {
    safeSetItem(`${storageKey}_height`, String(height));
  }, [height, storageKey]);

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
      className={className}
      style={{
        height,
        minHeight: height,
        maxHeight: height,
        display: "flex",
        flexDirection: "column",
        minWidth: 0,
        overflow: "hidden",
        flexShrink: 0,
        borderTop: "1px solid var(--token-colors-border-default)",
        position: "relative",
      }}
    >
      {/* Resize handle */}
      <button
        type="button"
        aria-label={`Resize ${label}`}
        onMouseDown={startResize}
        style={{
          position: "absolute",
          top: -3,
          left: 0,
          right: 0,
          height: 6,
          cursor: "row-resize",
          zIndex: 10,
          border: "none",
          background: "transparent",
          padding: 0,
        }}
        title={`Resize ${label}`}
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
            gap: 8,
          }}
        >
          <div>{header}</div>
          <button
            type="button"
            onClick={toggle}
            title={`Collapse ${label}`}
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
      <div style={{ flex: 1, minHeight: 0, overflow: "hidden" }}>{children}</div>
    </div>
  );
}
