import { useCallback, useState } from "react";

export type SidebarSectionProps = {
  children: React.ReactNode;
  title: string;
  badge?: React.ReactNode;
  defaultOpen?: boolean;
  className?: string;
  headerActions?: React.ReactNode;
};

export function SidebarSection({
  children,
  title,
  badge,
  defaultOpen = true,
  className = "",
  headerActions,
}: SidebarSectionProps) {
  const [open, setOpen] = useState(defaultOpen);
  const toggle = useCallback(() => setOpen((prev) => !prev), []);

  return (
    <div
      className={className}
      style={{
        display: "flex",
        flexDirection: "column",
        minHeight: open ? 60 : undefined,
        flex: open ? 1 : undefined,
        overflow: "hidden",
        borderBottom: "1px solid var(--token-colors-border-subtle)",
      }}
    >
      <button
        type="button"
        onClick={toggle}
        style={{
          flexShrink: 0,
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: "6px 10px",
          background: "transparent",
          border: "none",
          cursor: "pointer",
          fontSize: "11px",
          fontWeight: 600,
          textTransform: "uppercase",
          letterSpacing: "0.05em",
          color: "var(--token-colors-text-muted)",
        }}
      >
        <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
          {title}
          {badge}
        </span>
        <span style={{ display: "flex", alignItems: "center", gap: 6 }}>
          {headerActions}
          <span style={{ fontSize: 10, opacity: 0.6 }}>{open ? "▼" : "▶"}</span>
        </span>
      </button>
      {open ? (
        <div style={{ flex: 1, minHeight: 0, overflow: "auto" }}>
          {children}
        </div>
      ) : null}
    </div>
  );
}
