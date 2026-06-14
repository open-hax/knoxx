import React, { useEffect, useState } from "react";

// CLJS (Helix) SettingsPage loader.
//
// The real implementation lives in knoxx.frontend.pages.settings.view/settings-page
// and is exposed on window.knoxx.frontend.pages.settings.view.settings_page by the
// shadow-cljs app bundle (required from knoxx.frontend.core). This wrapper does NOT
// fall back to a legacy TS page — if the CLJS export is missing we want a loud,
// debuggable failure so the migration stays honest.

type CljsComponentType = React.ComponentType<Record<string, never>>;

function getCljsComponent(): CljsComponentType | null {
  const ns = (window as unknown as Record<string, unknown>).knoxx;
  if (!ns) return null;
  const frontend = (ns as Record<string, unknown>).frontend;
  if (!frontend) return null;
  const pages = (frontend as Record<string, unknown>).pages;
  if (!pages) return null;
  const settings = (pages as Record<string, unknown>).settings;
  if (!settings) return null;
  const view = (settings as Record<string, unknown>).view;
  if (!view) return null;
  const component = (view as Record<string, unknown>).settings_page;
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

export default function SettingsPage() {
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
        "shadow-cljs SettingsPage export not found on window.knoxx.frontend.pages.settings.view.settings_page. " +
          "This is an integration/compile problem (not a reason to silently render legacy TS).",
      );
    }, 1500);

    return () => clearTimeout(timer);
  }, []);

  if (loadError) {
    return (
      <div className="m-6 rounded-lg border border-rose-500/30 bg-rose-500/10 p-4 text-sm text-rose-100">
        <div className="font-semibold">Settings (shadow-cljs) failed to load</div>
        <div className="mt-2 font-mono text-xs whitespace-pre-wrap break-words">{loadError}</div>
      </div>
    );
  }

  if (!Component) {
    return (
      <div className="flex h-full items-center justify-center p-6 text-sm text-slate-400">
        Loading settings…
      </div>
    );
  }

  return (
    <CljsErrorBoundary onError={(error) => setLoadError(String(error?.message ?? error))}>
      <Component />
    </CljsErrorBoundary>
  );
}
