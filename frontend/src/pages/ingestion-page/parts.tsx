import { useState } from "react";
import { Button, Card, Badge, Input, Spinner } from "@open-hax/uxx";
import type { BrowserCreateSourceForm, BrowserCreatedSource } from "../../components/WorkspaceBrowserCard";
import type { CreateSourceForm, Job, ProgressEvent, Source, SourceAudit } from "./types";

export function SourceDetailView({ source, audit, jobs, onStartJob, onDelete }: {
  source: Source;
  audit: SourceAudit | null;
  jobs: Job[];
  onStartJob: () => void;
  onDelete: () => void;
}) {
  return (
    <div style={{ flex: 1, overflow: "auto", padding: 24 }}>
      <div style={{ maxWidth: 768, margin: "0 auto" }}>
        <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: 24 }}>
          <div>
            <h1 style={{ fontSize: 24, fontWeight: 700 }}>{source.name}</h1>
            <div style={{ fontSize: 14, color: "var(--token-colors-text-muted)", marginTop: 4 }}>
              {source.driver_type} • Created {new Date(source.created_at).toLocaleDateString()}
            </div>
          </div>
          <div style={{ display: "flex", gap: 8 }}>
            <Button variant="primary" onClick={onStartJob}>▶ Start Sync</Button>
            <Button variant="danger" onClick={onDelete}>Delete</Button>
          </div>
        </div>

        <Card variant="default" padding="md" style={{ marginBottom: 24 }}>
          <h3 style={{ fontWeight: 600, marginBottom: 12 }}>Coverage Audit</h3>
          {audit ? (
            <>
              <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 16, marginBottom: 16 }}>
                <StatCard label="Matching Files" value={audit.matching_files} />
                <StatCard label="In Lake" value={audit.openplanner_documents} color="var(--token-colors-accent-green)" />
                <StatCard label="Pending Sync" value={audit.new_files + audit.changed_files} color="var(--token-colors-accent-blue)" />
                <StatCard label="Failed State" value={audit.state_failed_files} color="var(--token-colors-accent-red)" />
              </div>
              <div style={{ fontSize: 14, color: "var(--token-colors-text-muted)", display: "grid", gap: 6, marginBottom: 12 }}>
                <div><strong>Root:</strong> {audit.root_path || "(none)"}</div>
                <div><strong>Lakes:</strong> {audit.collections.join(", ") || "devel"}</div>
                <div><strong>Include:</strong> {audit.include_patterns.join(", ") || "(all matching text-like files)"}</div>
                <div><strong>Exclude:</strong> {audit.exclude_patterns.join(", ") || "(none)"}</div>
                <div><strong>File types:</strong> {audit.file_types.join(", ") || "(driver default)"}</div>
                <div><strong>State audit:</strong> {audit.state_ingested_files} tracked as ingested, {audit.unchanged_files} unchanged, {audit.changed_files} changed, {audit.new_files} new, {audit.skipped_files} skipped</div>
                <div><strong>Lake delta:</strong> {audit.coverage_delta > 0 ? `${audit.coverage_delta} matching files not yet represented in OpenPlanner` : audit.coverage_delta < 0 ? `${Math.abs(audit.coverage_delta)} more OpenPlanner docs than current matching files` : "source scan and OpenPlanner doc counts are aligned"}</div>
              </div>
            </>
          ) : (
            <div style={{ fontSize: 14, color: "var(--token-colors-text-muted)" }}>Loading coverage audit…</div>
          )}
        </Card>

        <Card variant="default" padding="md" style={{ marginBottom: 24 }}>
          <h3 style={{ fontWeight: 600, marginBottom: 12 }}>Configuration</h3>
          <pre style={{ fontSize: 14, backgroundColor: "var(--token-colors-alpha-bg-_08)", padding: 12, borderRadius: 6, overflow: "auto" }}>
            {JSON.stringify(source.config, null, 2)}
          </pre>
        </Card>

        <Card variant="default" padding="md">
          <h3 style={{ fontWeight: 600, marginBottom: 12 }}>Recent Jobs</h3>
          {jobs.length === 0 ? (
            <div style={{ fontSize: 14, color: "var(--token-colors-text-muted)" }}>No jobs yet</div>
          ) : (
            <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
              {jobs.slice(0, 5).map((job) => (
                <div key={job.job_id} style={{ display: "flex", alignItems: "center", justifyContent: "space-between", padding: 8, backgroundColor: "var(--token-colors-alpha-bg-_08)", borderRadius: 6 }}>
                  <div>
                    <div style={{ fontSize: 14, fontWeight: 500 }}>
                      {job.total_files > 0 ? `${job.processed_files}/${job.total_files} files` : "Pending..."}
                    </div>
                    <div style={{ fontSize: 12, color: "var(--token-colors-text-muted)" }}>
                      {new Date(job.created_at).toLocaleString()}
                    </div>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    <Badge variant={job.status === "completed" ? "success" : job.status === "failed" ? "error" : job.status === "running" ? "info" : "default"} size="sm">
                      {job.status}
                    </Badge>
                    <span style={{ fontSize: 14, color: "var(--token-colors-text-subtle)" }}>{job.chunks_created} chunks</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}

function StatCard({ label, value, color = "var(--token-colors-text-default)" }: { label: string; value: number; color?: string }) {
  return (
    <Card variant="default" padding="md" style={{ textAlign: "center" }}>
      <div style={{ fontSize: 24, fontWeight: 700, color }}>{value.toLocaleString()}</div>
      <div style={{ fontSize: 14, color: "var(--token-colors-text-muted)" }}>{label}</div>
    </Card>
  );
}

export function JobProgressView({ jobId, job, events, onCancel }: {
  jobId: string;
  job: Job | null;
  events: ProgressEvent[];
  onCancel: () => void;
}) {
  const latestProgress = events.filter((e) => e.type === "progress").slice(-1)[0];
  const terminalEvent = [...events].reverse().find((e) => e.type === "job_complete" || e.type === "job_error");
  const isRunning = job ? ["pending", "running"].includes(job.status) : !terminalEvent;

  const totalFiles = latestProgress?.total_files ?? job?.total_files ?? 0;
  const processedFiles = latestProgress?.processed_files ?? job?.processed_files ?? 0;
  const failedFiles = latestProgress?.failed_files ?? job?.failed_files ?? 0;
  const chunksCreated = latestProgress?.chunks_created ?? job?.chunks_created ?? 0;
  const percent = totalFiles > 0 ? ((processedFiles + failedFiles) / totalFiles) * 100 : latestProgress?.percent_complete ?? 0;
  const statusLabel = latestProgress?.status ?? job?.status ?? "Starting...";

  return (
    <div style={{ flex: 1, overflow: "auto", padding: 24 }}>
      <div style={{ maxWidth: 768, margin: "0 auto" }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 24 }}>
          <h1 style={{ fontSize: 24, fontWeight: 700 }}>
            Ingestion Job
            <span style={{ fontSize: 18, color: "var(--token-colors-text-muted)", marginLeft: 8, fontFamily: "monospace" }}>
              {jobId.slice(0, 8)}
            </span>
          </h1>
          {isRunning && <Button variant="danger" onClick={onCancel}>Cancel</Button>}
        </div>

        <div style={{ marginBottom: 24 }}>
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: 14, marginBottom: 8 }}>
            <span style={{ fontWeight: 500 }}>{statusLabel}</span>
            <span style={{ color: "var(--token-colors-text-subtle)" }}>{percent.toFixed(1)}%</span>
          </div>
          <div style={{ height: 12, backgroundColor: "var(--token-colors-border-default)", borderRadius: 9999, overflow: "hidden" }}>
            <div style={{ height: "100%", width: `${percent}%`, transition: "width 0.3s", backgroundColor: isRunning ? "var(--token-colors-accent-blue)" : "var(--token-colors-accent-green)" }} />
          </div>
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 16, marginBottom: 24 }}>
          <StatCard label="Total" value={totalFiles} />
          <StatCard label="Processed" value={processedFiles} color="var(--token-colors-accent-green)" />
          <StatCard label="Failed" value={failedFiles} color="var(--token-colors-accent-red)" />
          <StatCard label="Chunks" value={chunksCreated} color="var(--token-colors-accent-blue)" />
        </div>

        {job && events.length === 0 ? (
          <Card variant="outlined" padding="sm" style={{ marginBottom: 16 }}>
            <p style={{ fontSize: 14, color: "var(--token-colors-accent-blue)" }}>
              Live progress is being shown via polling. The activity log may remain empty for this job.
            </p>
          </Card>
        ) : null}

        <Card variant="default" padding="none">
          <div style={{ padding: 12, borderBottom: "1px solid var(--token-colors-border-default)", fontWeight: 600 }}>Activity Log</div>
          <div style={{ maxHeight: 384, overflow: "auto", padding: 8 }}>
            {events.length === 0 ? (
              <div style={{ fontSize: 14, color: "var(--token-colors-text-muted)", textAlign: "center", padding: 16 }}>Waiting for events...</div>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: 4, fontFamily: "monospace", fontSize: 14 }}>
                {events.map((event, i) => (
                  <div key={i} style={{ display: "flex", gap: 8, padding: 4, borderRadius: 4, backgroundColor: event.type === "file_error" ? "var(--token-colors-alpha-red-_12)" : event.type === "file_complete" ? "var(--token-colors-alpha-green-_08)" : "transparent", color: event.type === "file_error" ? "var(--token-colors-accent-red)" : "inherit" }}>
                    <span style={{ color: "var(--token-colors-text-muted)", flexShrink: 0, width: 80 }}>{new Date(event.timestamp).toLocaleTimeString()}</span>
                    <span style={{ overflow: "hidden", textOverflow: "ellipsis" }}>
                      {event.file_path ? (
                        <>
                          {event.file_path}
                          {event.file_chunks && <span style={{ color: "var(--token-colors-accent-green)", marginLeft: 4 }}>({event.file_chunks} chunks)</span>}
                          {event.file_error && <span style={{ color: "var(--token-colors-accent-red)", marginLeft: 4 }}>— {event.file_error}</span>}
                        </>
                      ) : event.type}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
}

export function CreateSourceModal({ onClose, onCreate }: { onClose: () => void; onCreate: (data: CreateSourceForm) => void }) {
  const [driverType, setDriverType] = useState("local");
  const [name, setName] = useState("");
  const [rootPath, setRootPath] = useState("/app/workspace/docs");
  const [collections, setCollections] = useState("devel");
  const [fileTypes, setFileTypes] = useState(".md,.txt,.clj,.cljs,.cljc,.edn,.json,.yml,.yaml,.ts,.tsx,.js,.html,.css");
  const [includePatterns, setIncludePatterns] = useState("");
  const [excludePatterns, setExcludePatterns] = useState("**/node_modules/**,**/.git/**,**/dist/**,**/coverage/**,**/*.png,**/*.jpg,**/*.jpeg,**/*.gif,**/*.pdf,**/*.zip,**/*.tar.gz");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onCreate({
      driver_type: driverType,
      name: name || `${driverType} source`,
      config: driverType === "local" ? { root_path: rootPath } : {},
      collections: collections.split(",").map((c) => c.trim()).filter(Boolean),
      file_types: fileTypes.split(",").map((v) => v.trim()).filter(Boolean),
      include_patterns: includePatterns.split(",").map((v) => v.trim()).filter(Boolean),
      exclude_patterns: excludePatterns.split(",").map((v) => v.trim()).filter(Boolean),
    });
  };

  return (
    <div style={{ position: "fixed", inset: 0, backgroundColor: "var(--token-colors-background-overlay)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 50 }}>
      <Card variant="elevated" padding="lg" style={{ width: "100%", maxWidth: 448 }}>
        <h2 style={{ fontSize: 20, fontWeight: 700, marginBottom: 16 }}>Add Data Source</h2>
        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          <div>
            <label style={{ display: "block", fontSize: 14, fontWeight: 500, marginBottom: 4 }}>Source Type</label>
            <select value={driverType} onChange={(e) => setDriverType(e.target.value)} style={{ width: "100%", padding: 8, borderRadius: 6, border: "1px solid var(--token-colors-border-subtle)" }}>
              <option value="local">Local Filesystem</option>
              <option value="github" disabled>GitHub (coming soon)</option>
              <option value="google_drive" disabled>Google Drive (coming soon)</option>
            </select>
          </div>
          <div>
            <label style={{ display: "block", fontSize: 14, fontWeight: 500, marginBottom: 4 }}>Name</label>
            <Input value={name} onChange={(e: React.ChangeEvent<HTMLInputElement>) => setName(e.target.value)} placeholder={`${driverType} source`} />
          </div>
          {driverType === "local" ? (
            <div>
              <label style={{ display: "block", fontSize: 14, fontWeight: 500, marginBottom: 4 }}>Root Path</label>
              <Input value={rootPath} onChange={(e: React.ChangeEvent<HTMLInputElement>) => setRootPath(e.target.value)} placeholder="/path/to/your/documents" />
              <p style={{ fontSize: 12, color: "var(--token-colors-text-muted)", marginTop: 4 }}>The root directory to scan for documents</p>
            </div>
          ) : null}
          <div>
            <label style={{ display: "block", fontSize: 14, fontWeight: 500, marginBottom: 4 }}>File Types</label>
            <Input value={fileTypes} onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFileTypes(e.target.value)} placeholder=".md,.txt,.json" />
            <p style={{ fontSize: 12, color: "var(--token-colors-text-muted)", marginTop: 4 }}>Comma-separated extensions. Defaults to text-like files only.</p>
          </div>
          <div>
            <label style={{ display: "block", fontSize: 14, fontWeight: 500, marginBottom: 4 }}>Include Patterns</label>
            <Input value={includePatterns} onChange={(e: React.ChangeEvent<HTMLInputElement>) => setIncludePatterns(e.target.value)} placeholder="docs/**/*.md,specs/**/*.md" />
          </div>
          <div>
            <label style={{ display: "block", fontSize: 14, fontWeight: 500, marginBottom: 4 }}>Exclude Patterns</label>
            <Input value={excludePatterns} onChange={(e: React.ChangeEvent<HTMLInputElement>) => setExcludePatterns(e.target.value)} placeholder="**/node_modules/**,**/*.png" />
          </div>
          <div>
            <label style={{ display: "block", fontSize: 14, fontWeight: 500, marginBottom: 4 }}>Lakes</label>
            <Input value={collections} onChange={(e: React.ChangeEvent<HTMLInputElement>) => setCollections(e.target.value)} placeholder="devel" />
            <p style={{ fontSize: 12, color: "var(--token-colors-text-muted)", marginTop: 4 }}>Comma-separated list of lake keys to ingest into</p>
          </div>
          <div style={{ display: "flex", gap: 8, paddingTop: 16 }}>
            <Button variant="secondary" fullWidth onClick={onClose}>Cancel</Button>
            <Button variant="primary" fullWidth type="submit">Create Source</Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
