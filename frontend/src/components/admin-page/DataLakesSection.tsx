import React, { useCallback, useEffect, useState } from "react";
import { Badge, SectionCard } from "./common";
import type { LakeFormState } from "./types";
import type { AdminDataLakeSummary } from "../../lib/types";
import WorkspaceBrowserCard, { type BrowserCreateSourceForm, type BrowserCreatedSource } from "../WorkspaceBrowserCard";
import { CreateSourceModal, JobProgressView, SourceDetailView } from "../../pages/ingestion-page/parts";
import type { CreateSourceForm, Job, ProgressEvent, Source, SourceAudit } from "../../pages/ingestion-page/types";

const API_BASE = "/api/ingestion";

export function DataLakesSection({
  selectedOrgName,
  canCreateDataLakes,
  lakeForm,
  setLakeForm,
  creatingLake,
  dataLakes,
  dataLakeKindOptions,
  onCreateLake,
}: {
  selectedOrgName: string;
  canCreateDataLakes: boolean;
  lakeForm: LakeFormState;
  setLakeForm: React.Dispatch<React.SetStateAction<LakeFormState>>;
  creatingLake: boolean;
  dataLakes: AdminDataLakeSummary[];
  dataLakeKindOptions: string[];
  onCreateLake: (event: React.FormEvent) => void | Promise<void>;
}) {
  // ── Ingestion source state ──────────────────────────────────────────────
  const [sources, setSources] = useState<Source[]>([]);
  const [jobs, setJobs] = useState<Job[]>([]);
  const [selectedSource, setSelectedSource] = useState<Source | null>(null);
  const [selectedSourceAudit, setSelectedSourceAudit] = useState<SourceAudit | null>(null);
  const [showCreateSource, setShowCreateSource] = useState(false);
  const [activeJobId, setActiveJobId] = useState<string | null>(null);
  const [activeJob, setActiveJob] = useState<Job | null>(null);
  const [progressEvents, setProgressEvents] = useState<ProgressEvent[]>([]);
  const [loadingSources, setLoadingSources] = useState(true);
  const [showIngestion, setShowIngestion] = useState(false);

  const loadSources = useCallback(async () => {
    try {
      const resp = await fetch(`${API_BASE}/sources`);
      if (resp.ok) setSources(await resp.json());
    } catch (err) {
      console.error("Failed to load sources:", err);
    } finally {
      setLoadingSources(false);
    }
  }, []);

  const loadJobs = useCallback(async () => {
    try {
      const resp = await fetch(`${API_BASE}/jobs?limit=20`);
      if (resp.ok) setJobs(await resp.json());
    } catch (err) {
      console.error("Failed to load jobs:", err);
    }
  }, []);

  useEffect(() => {
    if (showIngestion) {
      void loadSources();
      void loadJobs();
    }
  }, [showIngestion, loadSources, loadJobs]);

  useEffect(() => {
    if (!selectedSource) {
      setSelectedSourceAudit(null);
      return;
    }
    void (async () => {
      try {
        const resp = await fetch(`${API_BASE}/sources/${selectedSource.source_id}/audit`);
        if (resp.ok) {
          setSelectedSourceAudit(await resp.json());
        } else {
          setSelectedSourceAudit(null);
        }
      } catch (err) {
        console.error("Failed to load source audit:", err);
        setSelectedSourceAudit(null);
      }
    })();
  }, [selectedSource]);

  useEffect(() => {
    if (!activeJobId) return;
    const ws = new WebSocket(`${location.protocol === "https:" ? "wss:" : "ws:"}//${location.host}${API_BASE}/ws/jobs/${activeJobId}`);
    ws.onmessage = (e) => {
      const event: ProgressEvent = JSON.parse(e.data);
      setProgressEvents((prev) => [...prev.slice(-99), event]);
      if (event.type === "job_complete" || event.type === "job_error") {
        void loadJobs();
        void fetch(`${API_BASE}/jobs/${activeJobId}`)
          .then((resp) => (resp.ok ? resp.json() : null))
          .then((job) => { if (job) setActiveJob(job); })
          .catch(() => undefined);
        if (event.type === "job_complete") {
          setTimeout(() => { setActiveJobId(null); setActiveJob(null); }, 3000);
        }
      }
    };
    ws.onerror = () => console.error("WebSocket error");
    return () => ws.close();
  }, [activeJobId, loadJobs]);

  const handleCreateSource = async (data: CreateSourceForm | BrowserCreateSourceForm): Promise<BrowserCreatedSource | null> => {
    try {
      const resp = await fetch(`${API_BASE}/sources`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
      });
      if (resp.ok) {
        const created: Source = await resp.json();
        await loadSources();
        setSelectedSource(created);
        setShowCreateSource(false);
        return { source_id: created.source_id, name: created.name };
      } else {
        alert(`Failed to create source: ${await resp.text()}`);
      }
    } catch (err) {
      console.error("Create source error:", err);
    }
    return null;
  };

  const handleStartJob = async (sourceId: string) => {
    try {
      const resp = await fetch(`${API_BASE}/jobs`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ source_id: sourceId }),
      });
      if (resp.ok) {
        const job: Job = await resp.json();
        setActiveJobId(job.job_id);
        setActiveJob(job);
        setProgressEvents([]);
        await loadJobs();
      } else {
        alert(`Failed to start job: ${await resp.text()}`);
      }
    } catch (err) {
      console.error("Start job error:", err);
    }
  };

  const handleCancelJob = async (jobId: string) => {
    try {
      await fetch(`${API_BASE}/jobs/${jobId}/cancel`, { method: "POST" });
      setActiveJobId(null);
      setActiveJob(null);
      await loadJobs();
    } catch (err) {
      console.error("Cancel job error:", err);
    }
  };

  return (
    <SectionCard
      title="Data lakes & ingestion"
      description="Control-plane owned data-lake inventory and ingestion source management for the selected org."
    >
      {/* ── Lake inventory ──────────────────────────────────────────────── */}
      {canCreateDataLakes ? (
        <form className="mb-5 space-y-3 rounded-xl border border-slate-800 bg-slate-900/80 p-4" onSubmit={onCreateLake}>
          <div className="grid gap-3 md:grid-cols-2">
            <input
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
              placeholder="Lake name"
              value={lakeForm.name}
              onChange={(event) => setLakeForm((current) => ({ ...current, name: event.target.value }))}
            />
            <input
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
              placeholder="lake-slug"
              value={lakeForm.slug}
              onChange={(event) => setLakeForm((current) => ({ ...current, slug: event.target.value }))}
            />
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            <select
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
              value={lakeForm.kind}
              onChange={(event) => setLakeForm((current) => ({ ...current, kind: event.target.value }))}
            >
              {dataLakeKindOptions.map((kind) => (
                <option key={kind} value={kind}>{kind}</option>
              ))}
            </select>
            <input
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
              placeholder="workspace root"
              value={lakeForm.workspaceRoot}
              onChange={(event) => setLakeForm((current) => ({ ...current, workspaceRoot: event.target.value }))}
            />
          </div>
          <div className="flex justify-end">
            <button
              type="submit"
              disabled={creatingLake || !lakeForm.name.trim()}
              className="rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-4 py-2 text-sm font-medium text-emerald-200 hover:bg-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {creatingLake ? "Creating…" : `Create data lake${selectedOrgName ? ` for ${selectedOrgName}` : ""}`}
            </button>
          </div>
        </form>
      ) : null}

      <div className="space-y-4">
        {dataLakes.length === 0 ? (
          <div className="rounded-xl border border-dashed border-slate-800 px-4 py-6 text-sm text-slate-400">No org data lakes registered yet.</div>
        ) : dataLakes.map((lake) => (
          <div key={lake.id} className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
              <div>
                <div className="text-sm font-semibold text-slate-100">{lake.name}</div>
                <div className="text-sm text-slate-400">{lake.slug}</div>
              </div>
              <div className="flex flex-wrap gap-2">
                <Badge>{lake.kind}</Badge>
                <Badge tone={lake.status === "active" ? "success" : "danger"}>{lake.status}</Badge>
              </div>
            </div>
            <pre className="mt-3 overflow-auto rounded-lg border border-slate-800 bg-slate-950/70 p-3 text-xs text-slate-300">
{JSON.stringify(lake.config, null, 2)}
            </pre>
          </div>
        ))}
      </div>

      {/* ── Ingestion sources toggle ────────────────────────────────────── */}
      <div className="mt-6 border-t border-slate-800 pt-4">
        <button
          type="button"
          onClick={() => setShowIngestion((v) => !v)}
          className="flex w-full items-center justify-between rounded-lg border border-slate-700 bg-slate-900/60 px-4 py-2.5 text-sm font-medium text-slate-200 hover:bg-slate-800 transition"
        >
          <span>Ingestion Sources ({sources.length})</span>
          <span className="text-xs text-slate-500">{showIngestion ? "▲ collapse" : "▼ expand"}</span>
        </button>
      </div>

      {/* ── Ingestion sources panel ─────────────────────────────────────── */}
      {showIngestion ? (
        <div className="mt-4 space-y-4">
          {activeJobId ? (
            <div className="rounded-xl border border-slate-800 bg-slate-950/50 p-4">
              <JobProgressView jobId={activeJobId} job={activeJob} events={progressEvents} onCancel={() => handleCancelJob(activeJobId)} />
            </div>
          ) : selectedSource ? (
            <div className="rounded-xl border border-slate-800 bg-slate-950/50 p-4">
              <SourceDetailView
                source={selectedSource}
                audit={selectedSourceAudit}
                jobs={jobs.filter((j) => j.source_id === selectedSource.source_id)}
                onStartJob={() => handleStartJob(selectedSource.source_id)}
                onDelete={async () => {
                  if (confirm(`Delete source "${selectedSource.name}"?`)) {
                    await fetch(`${API_BASE}/sources/${selectedSource.source_id}`, { method: "DELETE" });
                    setSelectedSource(null);
                    await loadSources();
                  }
                }}
              />
            </div>
          ) : null}

          {/* Source list */}
          {loadingSources ? (
            <div className="text-sm text-slate-400 px-2">Loading sources…</div>
          ) : sources.length === 0 ? (
            <div className="text-sm text-slate-400 px-2">No ingestion sources configured.</div>
          ) : (
            <div className="space-y-2">
              {sources.map((source) => (
                <button
                  key={source.source_id}
                  type="button"
                  onClick={() => setSelectedSource(source)}
                  className="w-full rounded-lg border border-slate-800 bg-slate-900/80 p-3 text-left hover:bg-slate-800 transition"
                >
                  <div className="text-sm font-medium text-slate-100">{source.name}</div>
                  <div className="flex items-center gap-2 mt-1">
                    <Badge>{source.driver_type}</Badge>
                    <span
                      className="inline-block h-2 w-2 rounded-full"
                      style={{ backgroundColor: source.enabled ? "var(--token-colors-accent-green)" : "var(--token-colors-text-muted)" }}
                    />
                    <span className="text-xs text-slate-500">{source.enabled ? "active" : "disabled"}</span>
                  </div>
                  {source.last_error ? (
                    <div className="mt-1 text-xs text-rose-400 truncate">⚠ {source.last_error}</div>
                  ) : null}
                </button>
              ))}
            </div>
          )}

          {/* Workspace browser */}
          {!selectedSource && !activeJobId ? (
            <WorkspaceBrowserCard onCreateSource={handleCreateSource} onStartJob={handleStartJob} />
          ) : null}

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={() => setShowCreateSource(true)}
              className="rounded-lg border border-sky-500/30 bg-sky-500/10 px-4 py-2 text-sm font-medium text-sky-200 hover:bg-sky-500/20 transition"
            >
              + Add Source
            </button>
          </div>
        </div>
      ) : null}

      {/* ── Create source modal ─────────────────────────────────────────── */}
      {showCreateSource ? (
        <CreateSourceModal onClose={() => setShowCreateSource(false)} onCreate={handleCreateSource} />
      ) : null}
    </SectionCard>
  );
}
