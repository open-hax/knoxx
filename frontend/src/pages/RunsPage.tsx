import { useEffect, useMemo, useState } from "react";
import { Button, Card, Badge } from "@open-hax/uxx";
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

  const statusVariant = (status: string): "success" | "error" | "warning" | "default" => {
    if (status === "completed") return "success";
    if (status === "failed") return "error";
    if (status === "running") return "warning";
    return "default";
  };

  const sortArrow = (key: SortKey) => {
    if (sortKey !== key) return "↕";
    return sortAsc ? "↑" : "↓";
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
      <div>
        <h1 style={{ fontSize: 24, fontWeight: 700 }}>Runs</h1>
        <p style={{ marginTop: 4, fontSize: 14, color: "var(--token-colors-text-muted)" }}>
          Select at least 2 for compare
        </p>
      </div>

      <Card variant="default" padding="none">
        <div style={{ overflow: "auto" }}>
          <table style={{ width: "100%", textAlign: "left", fontSize: 14, borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ borderBottom: "1px solid var(--token-colors-border-default)", color: "var(--token-colors-text-subtle)" }}>
                <th style={{ padding: 12, paddingRight: 8 }}></th>
                <th style={{ padding: 12, paddingRight: 16, cursor: "pointer" }} onClick={() => toggleSort("created_at")}>
                  Created {sortArrow("created_at")}
                </th>
                <th style={{ padding: 12, paddingRight: 16, cursor: "pointer" }} onClick={() => toggleSort("model")}>
                  Model {sortArrow("model")}
                </th>
                <th style={{ padding: 12, paddingRight: 16, cursor: "pointer" }} onClick={() => toggleSort("ttft")}>
                  TTFT ms {sortArrow("ttft")}
                </th>
                <th style={{ padding: 12, paddingRight: 16, cursor: "pointer" }} onClick={() => toggleSort("tps")}>
                  Tok/s {sortArrow("tps")}
                </th>
                <th style={{ padding: 12, paddingRight: 16 }}>Status</th>
              </tr>
            </thead>
            <tbody>
              {sortedRuns.map((run) => (
                <tr key={run.run_id} style={{ borderBottom: "1px solid var(--token-colors-alpha-bg-_08)" }}>
                  <td style={{ padding: 12, paddingRight: 8 }}>
                    <input
                      type="checkbox"
                      checked={selectedIds.includes(run.run_id)}
                      onChange={() => toggleSelection(run.run_id)}
                    />
                  </td>
                  <td style={{ padding: 12, paddingRight: 16 }}>{new Date(run.created_at).toLocaleString()}</td>
                  <td style={{ padding: 12, paddingRight: 16 }}>{run.model ?? "-"}</td>
                  <td style={{ padding: 12, paddingRight: 16 }}>{run.ttft_ms?.toFixed(1) ?? "-"}</td>
                  <td style={{ padding: 12, paddingRight: 16 }}>{run.tokens_per_s?.toFixed(2) ?? "-"}</td>
                  <td style={{ padding: 12, paddingRight: 16 }}>
                    <Badge variant={statusVariant(run.status)} size="sm">{run.status}</Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Card variant="default" padding="md">
        <h2 style={{ fontSize: 16, fontWeight: 600, marginBottom: 12 }}>Compare</h2>
        {selectedDetails.length < 2 ? (
          <p style={{ fontSize: 14, color: "var(--token-colors-text-muted)" }}>Select at least two runs.</p>
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "1fr", gap: 12 }}>
            {selectedDetails.map((run) => {
              const prompt = (run.request_messages || []).map((m) => `${m.role}: ${m.content}`).join("\n");
              return (
                <Card key={run.run_id} variant="outlined" padding="md">
                  <h3 style={{ fontSize: 14, fontWeight: 600, marginBottom: 8 }}>{run.run_id}</h3>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 8, fontSize: 14 }}>
                    <div><strong>Model:</strong> {run.model}</div>
                    <div><strong>TTFT:</strong> {run.ttft_ms?.toFixed(1) ?? "-"} ms</div>
                    <div><strong>Gen time:</strong> {run.total_time_ms?.toFixed(1) ?? "-"} ms</div>
                    <div><strong>Tok/s:</strong> {run.tokens_per_s?.toFixed(2) ?? "-"}</div>
                  </div>
                  <div style={{ marginTop: 12 }}>
                    <p style={{ fontSize: 11, fontWeight: 500, textTransform: "uppercase", color: "var(--token-colors-text-muted)", marginBottom: 4 }}>Prompt</p>
                    <pre style={{ maxHeight: 96, overflow: "auto", borderRadius: 4, background: "var(--token-colors-alpha-bg-_08)", padding: 8, fontSize: 12 }}>{prompt}</pre>
                  </div>
                  <div style={{ marginTop: 12 }}>
                    <p style={{ fontSize: 11, fontWeight: 500, textTransform: "uppercase", color: "var(--token-colors-text-muted)", marginBottom: 4 }}>Settings</p>
                    <pre style={{ maxHeight: 96, overflow: "auto", borderRadius: 4, background: "var(--token-colors-alpha-bg-_08)", padding: 8, fontSize: 12 }}>{JSON.stringify(run.settings, null, 2)}</pre>
                  </div>
                  <div style={{ marginTop: 12 }}>
                    <p style={{ fontSize: 11, fontWeight: 500, textTransform: "uppercase", color: "var(--token-colors-text-muted)", marginBottom: 4 }}>Resource summary</p>
                    <pre style={{ maxHeight: 96, overflow: "auto", borderRadius: 4, background: "var(--token-colors-alpha-bg-_08)", padding: 8, fontSize: 12 }}>{JSON.stringify(run.resources, null, 2)}</pre>
                  </div>
                </Card>
              );
            })}
          </div>
        )}
      </Card>
    </div>
  );
}

export default RunsPage;
