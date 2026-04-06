import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { ExternalLink, Orbit, RefreshCw } from 'lucide-react';
import { opsRoutes } from '../lib/app-routes';

function resolveGraphWeaverUrl(): string {
  if (typeof window === 'undefined') {
    return 'http://127.0.0.1:8796/';
  }

  const protocol = window.location.protocol || 'http:';
  const hostname = window.location.hostname || '127.0.0.1';
  return `${protocol}//${hostname}:8796/`;
}

export default function VectorsPage() {
  const graphWeaverUrl = useMemo(() => resolveGraphWeaverUrl(), []);
  const [refreshKey, setRefreshKey] = useState(0);

  return (
    <div className="mx-auto w-full max-w-7xl p-6 md:p-8 text-slate-100">
      <header className="mb-6 flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="flex items-center gap-3 text-3xl font-bold text-slate-100">
            <Orbit className="h-8 w-8 text-cyan-300" />
            Semantic Graph Weaver
          </h1>
          <p className="mt-2 max-w-3xl text-sm text-slate-400">
            This page now embeds the live Graph Weaver surface rather than an approximate Knoxx-side reimplementation. That means you get the actual
            semantic-gravity layout written by <code className="rounded bg-slate-800 px-1.5 py-0.5">eros-eris-field-app</code> into Graph Weaver node positions.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => setRefreshKey((value) => value + 1)}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 hover:bg-slate-800"
          >
            <RefreshCw className="h-4 w-4" />
            Reload embed
          </button>
          <a
            href={graphWeaverUrl}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 hover:bg-slate-800"
          >
            <ExternalLink className="h-4 w-4" />
            Open Graph Weaver directly
          </a>
          <Link
            to={opsRoutes.graphExportDebug}
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 hover:bg-slate-800"
          >
            Raw export debug
          </Link>
        </div>
      </header>

      <section className="mb-6 rounded-2xl border border-cyan-500/20 bg-cyan-500/5 p-4 text-sm text-cyan-100 shadow-xl">
        <div className="font-semibold text-cyan-200">Why this differs from the previous Knoxx graph page</div>
        <ul className="mt-2 list-disc space-y-1 pl-5 text-cyan-100/90">
          <li>The previous page rendered a fresh client-side layout over raw graph export data.</li>
          <li>Graph Weaver uses the live graph workbench state, including persisted node positions.</li>
          <li>
            Those positions are continuously shaped by the semantic layout worker, which applies attraction/repulsion from embeddings and writes the resulting
            coordinates back into Graph Weaver.
          </li>
        </ul>
      </section>

      <section className="rounded-2xl border border-slate-800 bg-slate-900/80 p-3 shadow-2xl">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2 px-2 pt-2 text-xs text-slate-400">
          <span>Embedded source: {graphWeaverUrl}</span>
          <span>If the iframe fails, use the direct-open button above.</span>
        </div>
        <div className="overflow-hidden rounded-xl border border-slate-800 bg-slate-950">
          <iframe
            key={refreshKey}
            title="Graph Weaver"
            src={graphWeaverUrl}
            className="h-[76vh] min-h-[720px] w-full bg-slate-950"
          />
        </div>
      </section>
    </div>
  );
}
