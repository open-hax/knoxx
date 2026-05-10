import React, { useEffect, useState } from "react";

// CLJS event agents panel wrapper.
//
// IMPORTANT: This wrapper intentionally does NOT auto-fallback to the legacy
// TypeScript panel. If shadow-cljs isn’t correctly exporting the panel, we want
// a loud, obvious failure so the migration stays debuggable.

type CljsComponentType = React.ComponentType<{
  canManage: boolean;
  tools: import("../../lib/types").AdminToolDefinition[];
  onSelectedJobChange?: (job: unknown) => void;
}>;

function getCljsComponent(): CljsComponentType | null {
  // CLJS namespaces use underscores instead of hyphens in JS
  const ns = (window as unknown as Record<string, unknown>).knoxx;
  if (!ns) return null;
  const frontend = (ns as Record<string, unknown>).frontend;
  if (!frontend) return null;
  const admin = (frontend as Record<string, unknown>).admin;
  if (!admin) return null;
  const panel = (admin as Record<string, unknown>).event_agents_panel;
  if (!panel) return null;
  const component = (panel as Record<string, unknown>).event_agents_panel;
  return (component as CljsComponentType) ?? null;
}

class CljsErrorBoundary extends React.Component<
  React.PropsWithChildren<{ onError: (error: Error) => void }>,
  { error: Error | null }
> {
  state: { error: Error | null } = { error: null };

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  componentDidCatch(error: Error) {
    this.props.onError(error);
  }

  render() {
    if (this.state.error) return null;
    return this.props.children;
  }
}

export function EventAgentsPanel({
  canManage,
  tools,
  onSelectedJobChange,
}: {
  canManage: boolean;
  tools: import("../../lib/types").AdminToolDefinition[];
  onSelectedJobChange?: (job: unknown) => void;
}) {
  const [Component, setComponent] = useState<CljsComponentType | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    const comp = getCljsComponent();
    if (comp) {
      setComponent(() => comp);
      return;
    }

    // Give the /cljs/app.js injector a moment to run.
    const timer = setTimeout(() => {
      const loaded = getCljsComponent();
      if (loaded) {
        setComponent(() => loaded);
        return;
      }

      setLoadError(
        "shadow-cljs EventAgentsPanel export not found on window.knoxx.frontend.admin.event_agents_panel.event_agents_panel. " +
          "This is an integration/compile problem (not a reason to silently render legacy TS).",
      );
    }, 1500);

    return () => clearTimeout(timer);
  }, []);

  if (loadError) {
    return (
      <div className="h-full rounded-lg border border-rose-500/30 bg-rose-500/10 p-4 text-sm text-rose-100">
        <div className="font-semibold">Runtime jobs (shadow-cljs) failed to load</div>
        <div className="mt-2 font-mono text-xs whitespace-pre-wrap break-words">{loadError}</div>
      </div>
    );
  }

  if (!Component) {
    return (
      <div className="flex h-full items-center justify-center text-sm text-slate-400">
        Loading runtime jobs…
      </div>
    );
  }

  return (
    <div className="h-full">
      <CljsErrorBoundary
        onError={(error) => {
          setLoadError(String(error?.message ?? error));
        }}
      >
        <Component canManage={canManage} tools={tools} onSelectedJobChange={onSelectedJobChange} />
      </CljsErrorBoundary>
    </div>
  );
}