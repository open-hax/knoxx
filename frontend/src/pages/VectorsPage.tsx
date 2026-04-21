import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Orbit, RefreshCw } from 'lucide-react';
import { opsRoutes } from '../lib/app-routes';
import { GraphExplorer } from '../components/GraphExplorer';
import { fetchGraphExport } from '../lib/nextApi';
import type { GraphExportNode } from '../lib/types';

export default function VectorsPage() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [nodes, setNodes] = useState<GraphExportNode[]>([]);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const res = await fetchGraphExport({});
      setNodes(res.nodes || []);
    } catch (e: any) {
      setError(e.message || String(e));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  return (
    <div className="mx-auto w-full max-w-7xl p-6 md:p-8 text-slate-100">
      <header className="mb-6 flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
        <div>
          <h1 className="flex items-center gap-3 text-3xl font-bold text-slate-100">
            <Orbit className="h-8 w-8 text-cyan-300" />
            Graph Explorer
          </h1>
          <p className="mt-2 max-w-3xl text-sm text-slate-400">
            Unified graph explorer: sidebar node list + WebGL render + a debounced GraphQL editor. No iframes.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => void load()}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 hover:bg-slate-800"
          >
            <RefreshCw className="h-4 w-4" />
            Reload
          </button>
          <Link
            to={opsRoutes.graphExportDebug}
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 hover:bg-slate-800"
          >
            Raw export debug
          </Link>
        </div>
      </header>

      <section className="rounded-2xl border border-slate-800 bg-slate-900/80 p-3 shadow-2xl">
        {error && (
          <div className="mb-3 rounded bg-rose-500/10 border border-rose-500/20 px-3 py-2 text-xs text-rose-300">{error}</div>
        )}
        {loading ? (
          <div className="p-4 text-xs text-slate-500">Loading graph export…</div>
        ) : (
          <GraphExplorer nodes={nodes} />
        )}
      </section>
    </div>
  );
}
