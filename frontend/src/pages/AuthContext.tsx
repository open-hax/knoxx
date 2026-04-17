import { useEffect, useState, useCallback, Suspense, lazy, type ReactNode } from "react";
import { API_BASE } from "../lib/api/core";
import { AuthContextInstance } from "./auth-context-instance";

// ---------------------------------------------------------------------------
// Types (re-exported for consumers)
// ---------------------------------------------------------------------------

export interface AuthContext {
  user: {
    id: string;
    email: string;
    displayName: string;
    status: string;
  } | null;
  org: {
    id: string;
    slug: string;
    name: string;
    isPrimary: boolean;
  } | null;
  membership: {
    id: string;
    status: string;
    isDefault: boolean;
  } | null;
  roleSlugs: string[];
  permissions: string[];
  isSystemAdmin: boolean;
  authProvider: string;
  loading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
  logout: () => Promise<void>;
}

// ---------------------------------------------------------------------------
// Auth state fetcher
// ---------------------------------------------------------------------------

async function fetchAuthContext(): Promise<Record<string, unknown>> {
  const resp = await fetch(`${API_BASE}/api/auth/context`, {
    credentials: "include",
    headers: { "Content-Type": "application/json" },
  });
  if (!resp.ok) {
    const body = await resp.json().catch(() => ({ error: resp.statusText }));
    throw new Error(body.error || body.code || `${resp.status}`);
  }
  return await resp.json();
}

// ---------------------------------------------------------------------------
// AuthBoundary — the ONLY export from this file (fast-refresh compatible)
// ---------------------------------------------------------------------------

const LazyLoginPage = lazy(() =>
  import("./LoginPage").then((mod) => ({ default: mod.default }))
);

export default function AuthBoundary({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<AuthContext>({
    user: null,
    org: null,
    membership: null,
    roleSlugs: [],
    permissions: [],
    isSystemAdmin: false,
    authProvider: "",
    loading: true,
    error: null,
    refresh: async () => {},
    logout: async () => {},
  });

  const refresh = useCallback(async () => {
    setAuth((prev) => ({ ...prev, loading: true, error: null }));
    try {
      const data = await fetchAuthContext();
      const user = data.user as AuthContext["user"];
      const org = data.org as AuthContext["org"];
      const membership = data.membership as AuthContext["membership"];
      const roleSlugs = (data.roleSlugs as string[]) ?? [];
      const permissions = (data.permissions as string[]) ?? [];
      const isSystemAdmin = (data.isSystemAdmin as boolean) ?? false;
      setAuth({
        user,
        org,
        membership,
        roleSlugs,
        permissions,
        isSystemAdmin,
        authProvider: (data.authProvider as string) ?? "",
        loading: false,
        error: null,
        refresh,
        logout,
      });
    } catch (err) {
      setAuth((prev) => ({
        ...prev,
        loading: false,
        error: err instanceof Error ? err.message : "Not authenticated",
      }));
    }
  }, []);

  const logout = useCallback(async () => {
    try {
      await fetch(`${API_BASE}/api/auth/logout`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
      });
    } catch {
      // ignore
    }
    setAuth({
      user: null,
      org: null,
      membership: null,
      roleSlugs: [],
      permissions: [],
      isSystemAdmin: false,
      authProvider: "",
      loading: false,
      error: "Logged out",
      refresh,
      logout,
    });
  }, [refresh]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  if (auth.loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-slate-950 text-slate-400">
        <div className="text-center">
          <div className="mb-4 h-8 w-8 animate-spin rounded-full border-2 border-slate-600 border-t-blue-500 mx-auto" />
          <p>Loading Knoxx…</p>
        </div>
      </div>
    );
  }

  if (!auth.user) {
    return (
      <AuthContextInstance.Provider value={{ ...auth, refresh, logout }}>
        <Suspense fallback={<div className="flex h-screen items-center justify-center bg-slate-950 text-slate-400"><p>Loading…</p></div>}>
          <LazyLoginPage error={auth.error} onLoginSuccess={refresh} />
        </Suspense>
      </AuthContextInstance.Provider>
    );
  }

  return (
    <AuthContextInstance.Provider value={{ ...auth, refresh, logout }}>
      {children}
    </AuthContextInstance.Provider>
  );
}
