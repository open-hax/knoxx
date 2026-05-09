import React, { useEffect, useRef, useState } from "react";

// Lazy-loaded CLJS event agents panel wrapper
// The shadow-cljs build must be loaded before this component renders

type CljsComponentType = React.ComponentType<{
  canManage: boolean;
  tools: Array<{ id: string; name?: string }>;
  onSelectedJobChange?: (job: unknown) => void;
}>;

function getCljsComponent(): CljsComponentType | null {
  const ns = (window as Record<string, unknown>).knoxx;
  if (!ns) return null;
  const frontend = (ns as Record<string, unknown>).frontend;
  if (!frontend) return null;
  const admin = (frontend as Record<string, unknown>).admin;
  if (!admin) return null;
  const panel = (admin as Record<string, unknown>)["event-agents-panel"];
  if (!panel) return null;
  const component = (panel as Record<string, unknown>)["event-agents-panel"];
  return (component as CljsComponentType) ?? null;
}

export function EventAgentsPanel({
  canManage,
  tools,
  onSelectedJobChange,
}: {
  canManage: boolean;
  tools: Array<{ id: string; name?: string }>;
  onSelectedJobChange?: (job: unknown) => void;
}) {
  const [Component, setComponent] = useState<CljsComponentType | null>(null);
  const [error, setError] = useState<string | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Try to get the CLJS component immediately
    const comp = getCljsComponent();
    if (comp) {
      setComponent(() => comp);
      return;
    }

    // If not available, the CLJS module may still be loading
    // Wait a bit and check again
    const timer = setTimeout(() => {
      const loaded = getCljsComponent();
      if (loaded) {
        setComponent(() => loaded);
      } else {
        setError("CLJS module not available. Make sure /cljs/app.js is loaded.");
      }
    }, 2000);

    return () => clearTimeout(timer);
  }, []);

  if (error) {
    return (
      <div className="flex h-full items-center justify-center text-sm text-rose-400">
        Error loading event agents panel: {error}
      </div>
    );
  }

  if (!Component) {
    return (
      <div className="flex h-full items-center justify-center text-sm text-slate-400">
        Loading event agents panel…
      </div>
    );
  }

  return (
    <div ref={containerRef} className="h-full">
      <Component
        canManage={canManage}
        tools={tools}
        onSelectedJobChange={onSelectedJobChange}
      />
    </div>
  );
}