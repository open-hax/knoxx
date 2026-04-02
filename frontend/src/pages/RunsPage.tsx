import { useEffect, useMemo, useState } from "react";
import { getRun, listRuns } from "../lib/api";
import type { RunDetail, RunSummary } from "../lib/types";

type SortKey = "created_at" | "model" | "ttft" | "tps";

function RunsPage() {
  const [runs, setRuns] = useState<RunSummary[]>([]);
  const [details, setDetails] = useState<Record<string, RunDetail>>({});
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [sortKey, setSortKey] = useState<SortKey>("created_at");
  const [sortAsc, setSortAsc] = useState(false);

  useEffect(() => {
    void listRuns().then(setRuns);
  }, []);

  useEffect(() => {
    selectedIds.forEach((id) => {
      if (!details[id]) {
        void getRun(id).then((detail) => {
          setDetails((prev) => ({ ...prev, [id]: detail }));
        });
      }
    });
  }, [selectedIds, details]);

  const sortedRuns = useMemo(() => {
    const copy = [...runs];
    copy.sort((a, b) => {
      const dir = sortAsc ? 1 : -1;
      if (sortKey === "created_at") return (new Date(a.created_at).getTime() - new Date(b.created_at).getTime()) * dir;
      if (sortKey === "model") return String(a.model ?? "").localeCompare(String(b.model ?? "")) * dir;
      if (sortKey === "ttft") return ((a.ttft_ms ?? 0) - (b.ttft_ms ?? 0)) * dir;
      return ((a.tokens_per_s ?? 0) - (b.tokens_per_s ?? 0)) * dir;
    });
    return copy;
  }, [runs, sortAsc, sortKey]);

  function toggleSelection(runId: string) {
    setSelectedIds((prev) =>
      prev.includes(runId) ? prev.filter((id) => id !== runId) : [...prev, runId]
    );
  }

  function toggleSort(key: SortKey) {
    if (sortKey === key) {
      setSortAsc((prev) => !prev);
      return;
    }
    setSortKey(key);
    setSortAsc(false);
  }

  const selectedDetails = selectedIds.map((id) => details[id]).filter(Boolean);

  return (
    <div className="space-y-4">
      <section className="panel">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="panel-title">Runs</h2>
          <p className="text-xs text-slate-500">Select at least 2 for compare</p>
        </div>

        <div className="overflow-auto">
          <table className="min-w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-slate-600">
                <th className="py-2 pr-2"></th>
                <th className="cursor-pointer py-2 pr-4" onClick={() => toggleSort("created_at")}>Created</th>
                <th className="cursor-pointer py-2 pr-4" onClick={() => toggleSort("model")}>Model</th>
                <th className="cursor-pointer py-2 pr-4" onClick={() => toggleSort("ttft")}>TTFT ms</th>
                <th className="cursor-pointer py-2 pr-4" onClick={() => toggleSort("tps")}>Tok/s</th>
                <th className="py-2 pr-4">Status</th>
              </tr>
            </thead>
            <tbody>
              {sortedRuns.map((run) => (
                <tr key={run.run_id} className="border-b border-slate-100 hover:bg-slate-50">
                  <td className="py-2 pr-2">
                    <input
                      type="checkbox"
                      checked={selectedIds.includes(run.run_id)}
                      onChange={() => toggleSelection(run.run_id)}
                    />
                  </td>
                  <td className="py-2 pr-4">{new Date(run.created_at).toLocaleString()}</td>
                  <td className="py-2 pr-4">{run.model ?? "-"}</td>
                  <td className="py-2 pr-4">{run.ttft_ms?.toFixed(1) ?? "-"}</td>
                  <td className="py-2 pr-4">{run.tokens_per_s?.toFixed(2) ?? "-"}</td>
                  <td className="py-2 pr-4">{run.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="panel">
        <h2 className="panel-title mb-3">Compare</h2>
        {selectedDetails.length < 2 ? (
          <p className="text-sm text-slate-500">Select at least two runs.</p>
        ) : (
          <div className="grid grid-cols-1 gap-3 xl:grid-cols-2">
            {selectedDetails.map((run) => {
              const prompt = (run.request_messages || []).map((m) => `${m.role}: ${m.content}`).join("\n");
              return (
                <article key={run.run_id} className="rounded-md border border-slate-200 bg-slate-50 p-3 text-sm">
                  <h3 className="mb-2 font-semibold">{run.run_id}</h3>
                  <p><strong>Model:</strong> {run.model}</p>
                  <p><strong>TTFT:</strong> {run.ttft_ms?.toFixed(1) ?? "-"} ms</p>
                  <p><strong>Gen time:</strong> {run.total_time_ms?.toFixed(1) ?? "-"} ms</p>
                  <p><strong>Tok/s:</strong> {run.tokens_per_s?.toFixed(2) ?? "-"}</p>
                  <div className="mt-2">
                    <p className="mb-1 text-xs uppercase text-slate-500">Prompt</p>
                    <pre className="max-h-24 overflow-auto rounded bg-white p-2 text-xs">{prompt}</pre>
                  </div>
                  <div className="mt-2">
                    <p className="mb-1 text-xs uppercase text-slate-500">Settings</p>
                    <pre className="max-h-24 overflow-auto rounded bg-white p-2 text-xs">{JSON.stringify(run.settings, null, 2)}</pre>
                  </div>
                  <div className="mt-2">
                    <p className="mb-1 text-xs uppercase text-slate-500">Resource summary</p>
                    <pre className="max-h-24 overflow-auto rounded bg-white p-2 text-xs">{JSON.stringify(run.resources, null, 2)}</pre>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}

export default RunsPage;
