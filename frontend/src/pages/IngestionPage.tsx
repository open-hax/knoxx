import { useCallback, useEffect, useState } from "react";
import { Badge, Button } from "@open-hax/uxx";
import { CreateSourceModal, JobProgressView, SourceDetailView } from "./ingestion-page/parts";
import type { CreateSourceForm, Job, ProgressEvent, Source, SourceAudit } from "./ingestion-page/types";

const API_BASE = "/api/ingestion";

// ── Sources sidebar ─────────────────────────────────────────────────────────

function SourcesSidebar({
  sources,
  jobs,
  loading,
  selectedSourceId,
  onSelect,
  onCreate,
}: {
  sources: Source[];
  jobs: Job[];
  loading: boolean;
  selectedSourceId: string | null;
  onSelect: (source: Source) => void;
  onCreate: () => void;
}) {
  return (
    <aside
      style={{
        width: 288,
        flexShrink: 0,
        borderRight: "1px solid var(--token-colors-border-default)",
        display: "flex",
        flexDirection: "column",
        overflow: "hidden",
      }}
    >
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          padding: 16,
          borderBottom: "1px solid var(--token-colors-border-default)",
        }}
      >
        <h2 style={{ fontSize: 16, fontWeight: 700 }}>Sources ({sources.length})</h2>
        <Button variant="primary" size="sm" onClick={onCreate}>+ Add</Button>
      </div>
      <div style={{ flex: 1, overflow: "auto", padding: 8 }}>
        {loading ? (
          <div style={{ fontSize: 14, color: "var(--token-colors-text-muted)", padding: 8 }}>Loading sources…</div>
        ) : sources.length === 0 ? (
          <div style={{ fontSize: 14, color: "var(--token-colors-text-muted)", padding: 8 }}>
            No ingestion sources configured.
          </div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
            {sources.map((source) => {
              const sourceJobs = jobs.filter((j) => j.source_id === source.source_id);
              const running = sourceJobs.some((j) => j.status === "running" || j.status === "pending");
              const isActive = source.source_id === selectedSourceId;
              return (
                <button
                  key={source.source_id}
                  type="button"
                  onClick={() => onSelect(source)}
                  style={{
                    width: "100%",
                    textAlign: "left",
                    borderRadius: 8,
                    border: "1px solid var(--token-colors-border-default)",
                    padding: 12,
                    cursor: "pointer",
                    backgroundColor: isActive
                      ? "var(--token-colors-alpha-bg-_12)"
                      : "var(--token-colors-alpha-bg-_08)",
                  }}
                >
                  <div style={{ fontSize: 14, fontWeight: 500 }}>{source.name}</div>
                  <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 4 }}>
                    <Badge size="sm">{source.driver_type}</Badge>
                    <span
                      style={{
                        display: "inline-block",
                        height: 8,
                        width: 8,
                        borderRadius: 9999,
                        backgroundColor: running
                          ? "var(--token-colors-accent-blue)"
                          : source.enabled
                            ? "var(--token-colors-accent-green)"
                            : "var(--token-colors-text-muted)",
                      }}
                    />
                    <span style={{ fontSize: 12, color: "var(--token-colors-text-muted)" }}>
                      {running ? "running" : source.enabled ? "active" : "disabled"}
                    </span>
                  </div>
                  {source.last_error ? (
                    <div
                      style={{
                        marginTop: 4,
                        fontSize: 12,
                        color: "var(--token-colors-accent-red)",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                      }}
                    >
                      ⚠ {source.last_error}
                    </div>
                  ) : null}
                </button>
              );
            })}
          </div>
        )}
      </div>
    </aside>
  );
}

// ── Page ─────────────────────────────────────────────────────────────────────

export default function IngestionPage() {
  const [sources, setSources] = useState<Source[]>([]);
  const [jobs, setJobs] = useState<Job[]>([]);
  const [selectedSource, setSelectedSource] = useState<Source | null>(null);
  const [selectedSourceAudit, setSelectedSourceAudit] = useState<SourceAudit | null>(null);
  const [showCreateSource, setShowCreateSource] = useState(false);
  const [activeJobId, setActiveJobId] = useState<string | null>(null);
  const [activeJob, setActiveJob] = useState<Job | null>(null);
  const [progressEvents, setProgressEvents] = useState<ProgressEvent[]>([]);
  const [loadingSources, setLoadingSources] = useState(true);

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
    void loadSources();
    void loadJobs();
  }, [loadSources, loadJobs]);

  useEffect(() => {
    if (!selectedSource) {
      setSelectedSourceAudit(null);
      return;
    }
    void (async () => {
      try {
        const resp = await fetch(`${API_BASE}/sources/${selectedSource.source_id}/audit`);
        setSelectedSourceAudit(resp.ok ? await resp.json() : null);
      } catch (err) {
        console.error("Failed to load source audit:", err);
        setSelectedSourceAudit(null);
      }
    })();
  }, [selectedSource]);

  useEffect(() => {
    if (!activeJobId) return;
    const ws = new WebSocket(
      `${location.protocol === "https:" ? "wss:" : "ws:"}//${location.host}${API_BASE}/ws/jobs/${activeJobId}`,
    );
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

  const handleCreateSource = async (data: CreateSourceForm) => {
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
      } else {
        alert(`Failed to create source: ${await resp.text()}`);
      }
    } catch (err) {
      console.error("Create source error:", err);
    }
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

  const handleDeleteSource = async () => {
    if (!selectedSource) return;
    if (confirm(`Delete source "${selectedSource.name}"?`)) {
      await fetch(`${API_BASE}/sources/${selectedSource.source_id}`, { method: "DELETE" });
      setSelectedSource(null);
      await loadSources();
    }
  };

  return (
    <div style={{ display: "flex", height: "100%", minHeight: 0 }}>
      <SourcesSidebar
        sources={sources}
        jobs={jobs}
        loading={loadingSources}
        selectedSourceId={selectedSource?.source_id ?? null}
        onSelect={(source) => { setSelectedSource(source); setActiveJobId(null); setActiveJob(null); }}
        onCreate={() => setShowCreateSource(true)}
      />

      {activeJobId ? (
        <JobProgressView
          jobId={activeJobId}
          job={activeJob}
          events={progressEvents}
          onCancel={() => handleCancelJob(activeJobId)}
        />
      ) : selectedSource ? (
        <SourceDetailView
          source={selectedSource}
          audit={selectedSourceAudit}
          jobs={jobs.filter((j) => j.source_id === selectedSource.source_id)}
          onStartJob={() => handleStartJob(selectedSource.source_id)}
          onDelete={handleDeleteSource}
        />
      ) : (
        <div
          style={{
            flex: 1,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "var(--token-colors-text-muted)",
            fontSize: 14,
          }}
        >
          Select a source to view details, or add a new one.
        </div>
      )}

      {showCreateSource ? (
        <CreateSourceModal onClose={() => setShowCreateSource(false)} onCreate={handleCreateSource} />
      ) : null}
    </div>
  );
}
