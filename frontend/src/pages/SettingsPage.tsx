import React, { useState } from 'react';
import { getFrontendConfig } from '../lib/api';

export default function SettingsPage() {
  const [config, setConfig] = useState<any>(null);
  const [loaded, setLoaded] = useState(false);

  React.useEffect(() => {
    getFrontendConfig()
      .then((c) => { setConfig(c); setLoaded(true); })
      .catch(() => setLoaded(true));
  }, []);

  if (!loaded) return <div className="p-6 text-sm text-slate-400">Loading…</div>;

  return (
    <div className="p-6 max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-100">Settings</h1>
        <p className="mt-1 text-sm text-slate-400">Knoxx runtime configuration.</p>
      </div>

      <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-5 space-y-4">
        <h2 className="text-sm font-semibold text-slate-200 border-b border-slate-800 pb-2">Instance</h2>
        <div className="grid gap-3 sm:grid-cols-2 text-sm">
          <div><span className="text-slate-500">Environment</span><div className="font-medium text-slate-200 mt-0.5">{config?.env ?? '—'}</div></div>
          <div><span className="text-slate-500">Version</span><div className="font-medium text-slate-200 mt-0.5">{config?.version ?? '—'}</div></div>
          <div><span className="text-slate-500">GitHub OAuth</span><div className="font-medium mt-0.5">{config?.github_enabled ? <span className="text-emerald-400">Enabled</span> : <span className="text-rose-400">Disabled</span>}</div></div>
          <div><span className="text-slate-500">Auth Required</span><div className="font-medium mt-0.5">{config?.auth_required ? <span className="text-emerald-400">Yes</span> : <span className="text-slate-400">No</span>}</div></div>
        </div>
      </div>

      <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-5 space-y-4">
        <h2 className="text-sm font-semibold text-slate-200 border-b border-slate-800 pb-2">Runtime Status</h2>
        <div className="space-y-2 text-sm">
          <StatusRow label="Backend API" url="/api/config" />
          <StatusRow label="Proxx Health" url="/api/proxx/health" />
          <StatusRow label="Event Agents" url="/api/admin/config/event-agents" />
        </div>
      </div>

      <div className="rounded-xl border border-amber-500/20 bg-amber-500/5 p-4 text-xs text-amber-300">
        Legacy settings (model selection, RAG config, forum mode) have been moved to the admin control plane or environment configuration.
      </div>
    </div>
  );
}

function StatusRow({ label, url }: { label: string; url: string }) {
  const [status, setStatus] = useState<'checking' | 'ok' | 'error'>('checking');

  React.useEffect(() => {
    let cancelled = false;
    fetch(url, { credentials: 'same-origin' })
      .then((r) => { if (!cancelled) setStatus(r.ok ? 'ok' : 'error'); })
      .catch(() => { if (!cancelled) setStatus('error'); });
    return () => { cancelled = true; };
  }, [url]);

  return (
    <div className="flex items-center justify-between py-1.5">
      <span className="text-slate-400">{label}</span>
      <span className={status === 'checking' ? 'text-slate-500' : status === 'ok' ? 'text-emerald-400' : 'text-rose-400'}>
        {status === 'checking' ? '…' : status === 'ok' ? '● OK' : '✕ Unavailable'}
      </span>
    </div>
  );
}
