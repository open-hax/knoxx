import React, { useEffect, useMemo, useState } from "react";

import { getKnoxxAuthContext } from "../lib/api/common";
import type { KnoxxAuthContext } from "../lib/types";
import { ContractsSection } from "../components/admin-page/ContractsSection";

export default function ContractsPage() {
  const [context, setContext] = useState<KnoxxAuthContext | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string>("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    void getKnoxxAuthContext()
      .then((ctx) => {
        if (!active) return;
        setContext(ctx);
      })
      .catch((err) => {
        if (!active) return;
        setError(err instanceof Error ? err.message : String(err));
      })
      .finally(() => {
        if (!active) return;
        setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const canManage = useMemo(() => {
    if (!context) return false;
    if (context.isSystemAdmin) return true;
    return context.permissions.includes("platform.org.create");
  }, [context]);

  if (loading) {
    return <div className="p-8 text-sm text-slate-300">Loading contracts…</div>;
  }

  if (error) {
    return (
      <div className="p-8">
        <div className="rounded-xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
          {error}
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-7xl flex-col gap-6 p-6 md:p-8">
      <ContractsSection canManage={canManage} />
    </div>
  );
}
