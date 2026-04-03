import { useState, useEffect } from 'react';
import { Button, Card, Badge, Input, Spinner } from '@devel/ui-react';
import WorkspaceBrowserCard, { type BrowserCreateSourceForm, type BrowserCreatedSource } from '../components/WorkspaceBrowserCard';

interface Source {
  source_id: string;
  tenant_id: string;
  driver_type: string;
  name: string;
  config: Record<string, unknown>;
  state: Record<string, unknown>;
  last_scan_at: string | null;
  last_error: string | null;
  enabled: boolean;
  created_at: string;
  updated_at: string;
}

interface Job {
  job_id: string;
  source_id: string;
  tenant_id: string;
  status: 'pending' | 'running' | 'completed' | 'failed' | 'cancelled';
  total_files: number;
  processed_files: number;
  failed_files: number;
  skipped_files: number;
  chunks_created: number;
  started_at: string | null;
  completed_at: string | null;
  error_message: string | null;
  created_at: string;
}

interface ProgressEvent {
  type: string;
  job_id: string;
  timestamp: string;
  total_files?: number;
  processed_files?: number;
  failed_files?: number;
  chunks_created?: number;
  percent_complete?: number;
  file_id?: string;
  file_path?: string;
  file_status?: 'success' | 'failed' | 'skipped';
  file_chunks?: number;
  file_error?: string;
  status?: string;
  error_message?: string;
  duration_seconds?: number;
}

const API_BASE = '/api/ingestion';

export default function IngestionPage() {
  const [sources, setSources] = useState<Source[]>([]);
  const [jobs, setJobs] = useState<Job[]>([]);
  const [selectedSource, setSelectedSource] = useState<Source | null>(null);
  const [showCreateSource, setShowCreateSource] = useState(false);
  const [activeJobId, setActiveJobId] = useState<string | null>(null);
  const [activeJob, setActiveJob] = useState<Job | null>(null);
  const [progressEvents, setProgressEvents] = useState<ProgressEvent[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadSources();
    loadJobs();
  }, []);

  const loadSources = async () => {
    try {
      const resp = await fetch(`${API_BASE}/sources`);
      if (resp.ok) setSources(await resp.json());
    } catch (err) {
      console.error('Failed to load sources:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadJobs = async () => {
    try {
      const resp = await fetch(`${API_BASE}/jobs?limit=20`);
      if (resp.ok) setJobs(await resp.json());
    } catch (err) {
      console.error('Failed to load jobs:', err);
    }
  };

  useEffect(() => {
    if (!activeJobId) return;
    const ws = new WebSocket(`${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}${API_BASE}/ws/jobs/${activeJobId}`);
    ws.onmessage = (e) => {
      const event: ProgressEvent = JSON.parse(e.data);
      setProgressEvents((prev) => [...prev.slice(-99), event]);
      if (event.type === 'job_complete' || event.type === 'job_error') {
        loadJobs();
        void fetch(`${API_BASE}/jobs/${activeJobId}`)
          .then((resp) => (resp.ok ? resp.json() : null))
          .then((job) => { if (job) setActiveJob(job); })
          .catch(() => undefined);
        if (event.type === 'job_complete') {
          setTimeout(() => { setActiveJobId(null); setActiveJob(null); }, 3000);
        }
      }
    };
    ws.onerror = () => console.error('WebSocket error');
    return () => ws.close();
  }, [activeJobId]);

  useEffect(() => {
    if (!activeJobId) return;
    const interval = window.setInterval(async () => {
      try {
        const resp = await fetch(`${API_BASE}/jobs/${activeJobId}`);
        if (!resp.ok) return;
        const job: Job = await resp.json();
        setActiveJob(job);
        if (['completed', 'failed', 'cancelled'].includes(job.status)) {
          await loadJobs();
          window.clearInterval(interval);
          setTimeout(() => { setActiveJobId(null); setActiveJob(null); }, 3000);
        }
      } catch (err) {
        console.error('Polling job state failed:', err);
      }
    }, 1000);
    return () => window.clearInterval(interval);
  }, [activeJobId]);

  const handleCreateSource = async (data: CreateSourceForm | BrowserCreateSourceForm): Promise<BrowserCreatedSource | null> => {
    try {
      const resp = await fetch(`${API_BASE}/sources`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
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
      console.error('Create source error:', err);
    }
    return null;
  };

  const handleStartJob = async (sourceId: string) => {
    try {
      const resp = await fetch(`${API_BASE}/jobs`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
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
      console.error('Start job error:', err);
    }
  };

  const handleCancelJob = async (jobId: string) => {
    try {
      await fetch(`${API_BASE}/jobs/${jobId}/cancel`, { method: 'POST' });
      setActiveJobId(null);
      setActiveJob(null);
      await loadJobs();
    } catch (err) {
      console.error('Cancel job error:', err);
    }
  };

  if (loading) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100%' }}>
        <Spinner size="lg" />
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', height: '100%', backgroundColor: '#f9fafb' }}>
      <Card variant="default" padding="none" style={{ width: 288, flexShrink: 0, display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: 16, borderBottom: '1px solid #e5e7eb' }}>
          <h2 style={{ fontWeight: 600, fontSize: 18 }}>Data Sources</h2>
        </div>
        <div style={{ flex: 1, overflow: 'auto', padding: 8 }}>
          {sources.length === 0 ? (
            <div style={{ textAlign: 'center', color: '#6b7280', fontSize: 14, padding: 16 }}>
              No sources configured. Click "Add Source" to get started.
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
              {sources.map((source) => (
                <Card
                  key={source.source_id}
                  onClick={() => setSelectedSource(source)}
                  variant="default"
                  padding="sm"
                  interactive
                  style={{ cursor: 'pointer', border: selectedSource?.source_id === source.source_id ? '2px solid #dbeafe' : 'transparent' }}
                >
                  <div style={{ fontWeight: 500, fontSize: 14 }}>{source.name}</div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
                    <Badge variant="default" size="sm">{source.driver_type}</Badge>
                    <span style={{ width: 8, height: 8, borderRadius: '50%', backgroundColor: source.enabled ? '#22c55e' : '#9ca3af' }} />
                  </div>
                  {source.last_error && (
                    <div style={{ fontSize: 12, color: '#dc2626', marginTop: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      ⚠ {source.last_error}
                    </div>
                  )}
                </Card>
              ))}
            </div>
          )}
        </div>
        <div style={{ padding: 12, borderTop: '1px solid #e5e7eb' }}>
          <Button variant="primary" fullWidth onClick={() => setShowCreateSource(true)}>+ Add Source</Button>
        </div>
      </Card>

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        {activeJobId ? (
          <JobProgressView jobId={activeJobId} job={activeJob} events={progressEvents} onCancel={() => handleCancelJob(activeJobId)} />
        ) : selectedSource ? (
          <SourceDetailView
            source={selectedSource}
            jobs={jobs.filter((j) => j.source_id === selectedSource.source_id)}
            onStartJob={() => handleStartJob(selectedSource.source_id)}
            onDelete={async () => {
              if (confirm(`Delete source "${selectedSource.name}"?`)) {
                await fetch(`${API_BASE}/sources/${selectedSource.source_id}`, { method: 'DELETE' });
                setSelectedSource(null);
                await loadSources();
              }
            }}
          />
        ) : (
          <div style={{ flex: 1, overflow: 'auto', padding: 24 }}>
            <div style={{ maxWidth: 896, margin: '0 auto' }}>
              <h1 style={{ fontSize: 24, fontWeight: 700 }}>Ingestion Workbench</h1>
              <p style={{ marginTop: 4, fontSize: 14, color: '#6b7280' }}>
                Browse the workspace, preview files, and route folders into canonical lakes.
              </p>
              <WorkspaceBrowserCard onCreateSource={handleCreateSource} onStartJob={handleStartJob} />
            </div>
          </div>
        )}
      </div>

      {showCreateSource && (
        <CreateSourceModal onClose={() => setShowCreateSource(false)} onCreate={handleCreateSource} />
      )}
    </div>
  );
}

function SourceDetailView({ source, jobs, onStartJob, onDelete }: {
  source: Source;
  jobs: Job[];
  onStartJob: () => void;
  onDelete: () => void;
}) {
  return (
    <div style={{ flex: 1, overflow: 'auto', padding: 24 }}>
      <div style={{ maxWidth: 768, margin: '0 auto' }}>
        <div style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 24 }}>
          <div>
            <h1 style={{ fontSize: 24, fontWeight: 700 }}>{source.name}</h1>
            <div style={{ fontSize: 14, color: '#6b7280', marginTop: 4 }}>
              {source.driver_type} • Created {new Date(source.created_at).toLocaleDateString()}
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <Button variant="primary" onClick={onStartJob}>▶ Start Sync</Button>
            <Button variant="danger" onClick={onDelete}>Delete</Button>
          </div>
        </div>

        <Card variant="default" padding="md" style={{ marginBottom: 24 }}>
          <h3 style={{ fontWeight: 600, marginBottom: 12 }}>Configuration</h3>
          <pre style={{ fontSize: 14, backgroundColor: '#f8fafc', padding: 12, borderRadius: 6, overflow: 'auto' }}>
            {JSON.stringify(source.config, null, 2)}
          </pre>
        </Card>

        <Card variant="default" padding="md">
          <h3 style={{ fontWeight: 600, marginBottom: 12 }}>Recent Jobs</h3>
          {jobs.length === 0 ? (
            <div style={{ fontSize: 14, color: '#6b7280' }}>No jobs yet</div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {jobs.slice(0, 5).map((job) => (
                <div key={job.job_id} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: 8, backgroundColor: '#f8fafc', borderRadius: 6 }}>
                  <div>
                    <div style={{ fontSize: 14, fontWeight: 500 }}>
                      {job.total_files > 0 ? `${job.processed_files}/${job.total_files} files` : 'Pending...'}
                    </div>
                    <div style={{ fontSize: 12, color: '#6b7280' }}>
                      {new Date(job.created_at).toLocaleString()}
                    </div>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <Badge variant={job.status === 'completed' ? 'success' : job.status === 'failed' ? 'error' : job.status === 'running' ? 'info' : 'default'} size="sm">
                      {job.status}
                    </Badge>
                    <span style={{ fontSize: 14, color: '#4b5563' }}>{job.chunks_created} chunks</span>
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

function JobProgressView({ jobId, job, events, onCancel }: {
  jobId: string;
  job: Job | null;
  events: ProgressEvent[];
  onCancel: () => void;
}) {
  const latestProgress = events.filter((e) => e.type === 'progress').slice(-1)[0];
  const terminalEvent = [...events].reverse().find((e) => e.type === 'job_complete' || e.type === 'job_error');
  const isRunning = job ? ['pending', 'running'].includes(job.status) : !terminalEvent;

  const totalFiles = latestProgress?.total_files ?? job?.total_files ?? 0;
  const processedFiles = latestProgress?.processed_files ?? job?.processed_files ?? 0;
  const failedFiles = latestProgress?.failed_files ?? job?.failed_files ?? 0;
  const chunksCreated = latestProgress?.chunks_created ?? job?.chunks_created ?? 0;
  const percent = totalFiles > 0 ? ((processedFiles + failedFiles) / totalFiles) * 100 : latestProgress?.percent_complete ?? 0;
  const statusLabel = latestProgress?.status ?? job?.status ?? 'Starting...';

  return (
    <div style={{ flex: 1, overflow: 'auto', padding: 24 }}>
      <div style={{ maxWidth: 768, margin: '0 auto' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 }}>
          <h1 style={{ fontSize: 24, fontWeight: 700 }}>
            Ingestion Job
            <span style={{ fontSize: 18, color: '#9ca3af', marginLeft: 8, fontFamily: 'monospace' }}>
              {jobId.slice(0, 8)}
            </span>
          </h1>
          {isRunning && <Button variant="danger" onClick={onCancel}>Cancel</Button>}
        </div>

        <div style={{ marginBottom: 24 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 14, marginBottom: 8 }}>
            <span style={{ fontWeight: 500 }}>{statusLabel}</span>
            <span style={{ color: '#4b5563' }}>{percent.toFixed(1)}%</span>
          </div>
          <div style={{ height: 12, backgroundColor: '#e5e7eb', borderRadius: 9999, overflow: 'hidden' }}>
            <div style={{ height: '100%', width: `${percent}%`, transition: 'width 0.3s', backgroundColor: isRunning ? '#2563eb' : '#16a34a' }} />
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, marginBottom: 24 }}>
          <StatCard label="Total" value={totalFiles} />
          <StatCard label="Processed" value={processedFiles} color="#16a34a" />
          <StatCard label="Failed" value={failedFiles} color="#dc2626" />
          <StatCard label="Chunks" value={chunksCreated} color="#2563eb" />
        </div>

        {job && events.length === 0 ? (
          <Card variant="outlined" padding="sm" style={{ marginBottom: 16 }}>
            <p style={{ fontSize: 14, color: '#1e40af' }}>
              Live progress is being shown via polling. The activity log may remain empty for this job.
            </p>
          </Card>
        ) : null}

        <Card variant="default" padding="none">
          <div style={{ padding: 12, borderBottom: '1px solid #e5e7eb', fontWeight: 600 }}>Activity Log</div>
          <div style={{ maxHeight: 384, overflow: 'auto', padding: 8 }}>
            {events.length === 0 ? (
              <div style={{ fontSize: 14, color: '#6b7280', textAlign: 'center', padding: 16 }}>Waiting for events...</div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4, fontFamily: 'monospace', fontSize: 14 }}>
                {events.map((event, i) => (
                  <div key={i} style={{ display: 'flex', gap: 8, padding: 4, borderRadius: 4, backgroundColor: event.type === 'file_error' ? '#fef2f2' : event.type === 'file_complete' ? '#f0fdf4' : 'transparent', color: event.type === 'file_error' ? '#dc2626' : 'inherit' }}>
                    <span style={{ color: '#9ca3af', flexShrink: 0, width: 80 }}>{new Date(event.timestamp).toLocaleTimeString()}</span>
                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {event.file_path ? (
                        <>
                          {event.file_path}
                          {event.file_chunks && <span style={{ color: '#16a34a', marginLeft: 4 }}>({event.file_chunks} chunks)</span>}
                          {event.file_error && <span style={{ color: '#dc2626', marginLeft: 4 }}>— {event.file_error}</span>}
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

function StatCard({ label, value, color = '#111827' }: { label: string; value: number; color?: string }) {
  return (
    <Card variant="default" padding="md" style={{ textAlign: 'center' }}>
      <div style={{ fontSize: 24, fontWeight: 700, color }}>{value.toLocaleString()}</div>
      <div style={{ fontSize: 14, color: '#6b7280' }}>{label}</div>
    </Card>
  );
}

interface CreateSourceForm {
  driver_type: string;
  name: string;
  config: Record<string, unknown>;
  collections: string[];
  file_types?: string[];
  include_patterns?: string[];
  exclude_patterns?: string[];
}

function CreateSourceModal({ onClose, onCreate }: { onClose: () => void; onCreate: (data: CreateSourceForm) => void }) {
  const [driverType, setDriverType] = useState('local');
  const [name, setName] = useState('');
  const [rootPath, setRootPath] = useState('/app/workspace/docs');
  const [collections, setCollections] = useState('devel_docs');
  const [fileTypes, setFileTypes] = useState('.md,.txt,.clj,.cljs,.cljc,.edn,.json,.yml,.yaml,.ts,.tsx,.js,.html,.css');
  const [includePatterns, setIncludePatterns] = useState('');
  const [excludePatterns, setExcludePatterns] = useState('**/node_modules/**,**/.git/**,**/dist/**,**/coverage/**,**/*.png,**/*.jpg,**/*.jpeg,**/*.gif,**/*.pdf,**/*.zip,**/*.tar.gz');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onCreate({
      driver_type: driverType,
      name: name || `${driverType} source`,
      config: driverType === 'local' ? { root_path: rootPath } : {},
      collections: collections.split(',').map((c) => c.trim()).filter(Boolean),
      file_types: fileTypes.split(',').map((v) => v.trim()).filter(Boolean),
      include_patterns: includePatterns.split(',').map((v) => v.trim()).filter(Boolean),
      exclude_patterns: excludePatterns.split(',').map((v) => v.trim()).filter(Boolean),
    });
  };

  return (
    <div style={{ position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50 }}>
      <Card variant="elevated" padding="lg" style={{ width: '100%', maxWidth: 448 }}>
        <h2 style={{ fontSize: 20, fontWeight: 700, marginBottom: 16 }}>Add Data Source</h2>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div>
            <label style={{ display: 'block', fontSize: 14, fontWeight: 500, marginBottom: 4 }}>Source Type</label>
            <select value={driverType} onChange={(e) => setDriverType(e.target.value)} style={{ width: '100%', padding: 8, borderRadius: 6, border: '1px solid #d1d5db' }}>
              <option value="local">Local Filesystem</option>
              <option value="github" disabled>GitHub (coming soon)</option>
              <option value="google_drive" disabled>Google Drive (coming soon)</option>
            </select>
          </div>
          <div>
            <label style={{ display: 'block', fontSize: 14, fontWeight: 500, marginBottom: 4 }}>Name</label>
            <Input value={name} onChange={(e) => setName(e.target.value)} placeholder={`${driverType} source`} />
          </div>
          {driverType === 'local' && (
            <div>
              <label style={{ display: 'block', fontSize: 14, fontWeight: 500, marginBottom: 4 }}>Root Path</label>
              <Input value={rootPath} onChange={(e) => setRootPath(e.target.value)} placeholder="/path/to/your/documents" />
              <p style={{ fontSize: 12, color: '#6b7280', marginTop: 4 }}>The root directory to scan for documents</p>
            </div>
          )}
          <div>
            <label style={{ display: 'block', fontSize: 14, fontWeight: 500, marginBottom: 4 }}>File Types</label>
            <Input value={fileTypes} onChange={(e) => setFileTypes(e.target.value)} placeholder=".md,.txt,.json" />
            <p style={{ fontSize: 12, color: '#6b7280', marginTop: 4 }}>Comma-separated extensions. Defaults to text-like files only.</p>
          </div>
          <div>
            <label style={{ display: 'block', fontSize: 14, fontWeight: 500, marginBottom: 4 }}>Include Patterns</label>
            <Input value={includePatterns} onChange={(e) => setIncludePatterns(e.target.value)} placeholder="docs/**/*.md,specs/**/*.md" />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: 14, fontWeight: 500, marginBottom: 4 }}>Exclude Patterns</label>
            <Input value={excludePatterns} onChange={(e) => setExcludePatterns(e.target.value)} placeholder="**/node_modules/**,**/*.png" />
          </div>
          <div>
            <label style={{ display: 'block', fontSize: 14, fontWeight: 500, marginBottom: 4 }}>Collections</label>
            <Input value={collections} onChange={(e) => setCollections(e.target.value)} placeholder="devel_docs" />
            <p style={{ fontSize: 12, color: '#6b7280', marginTop: 4 }}>Comma-separated list of collections to index into</p>
          </div>
          <div style={{ display: 'flex', gap: 8, paddingTop: 16 }}>
            <Button variant="secondary" fullWidth onClick={onClose}>Cancel</Button>
            <Button variant="primary" fullWidth type="submit">Create Source</Button>
          </div>
        </form>
      </Card>
    </div>
  );
}
