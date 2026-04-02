import { useState, useEffect, useCallback } from 'react';
import WorkspaceBrowserCard, { type BrowserCreateSourceForm, type BrowserCreatedSource } from '../components/WorkspaceBrowserCard';

// Types
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

  // Load sources and jobs
  useEffect(() => {
    loadSources();
    loadJobs();
  }, []);

  const loadSources = async () => {
    try {
      const resp = await fetch(`${API_BASE}/sources`);
      if (resp.ok) {
        setSources(await resp.json());
      }
    } catch (err) {
      console.error('Failed to load sources:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadJobs = async () => {
    try {
      const resp = await fetch(`${API_BASE}/jobs?limit=20`);
      if (resp.ok) {
        setJobs(await resp.json());
      }
    } catch (err) {
      console.error('Failed to load jobs:', err);
    }
  };

  // WebSocket for job progress
  useEffect(() => {
    if (!activeJobId) return;

    const ws = new WebSocket(
      `${location.protocol === 'https:' ? 'wss:' : 'ws:'}//${location.host}${API_BASE}/ws/jobs/${activeJobId}`
    );

    ws.onmessage = (e) => {
      const event: ProgressEvent = JSON.parse(e.data);
      setProgressEvents((prev) => [...prev.slice(-99), event]);

      if (event.type === 'job_complete' || event.type === 'job_error') {
        loadJobs();
        void fetch(`${API_BASE}/jobs/${activeJobId}`)
          .then((resp) => (resp.ok ? resp.json() : null))
          .then((job) => {
            if (job) setActiveJob(job);
          })
          .catch(() => undefined);
        if (event.type === 'job_complete') {
          setTimeout(() => {
            setActiveJobId(null);
            setActiveJob(null);
          }, 3000);
        }
      }
    };

    ws.onerror = (err) => {
      console.error('WebSocket error:', err);
    };

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
        if (job.status === 'completed' || job.status === 'failed' || job.status === 'cancelled') {
          await loadJobs();
          window.clearInterval(interval);
          setTimeout(() => {
            setActiveJobId(null);
            setActiveJob(null);
          }, 3000);
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
        const err = await resp.text();
        alert(`Failed to create source: ${err}`);
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
        const err = await resp.text();
        alert(`Failed to start job: ${err}`);
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
      <div className="flex items-center justify-center h-full">
        <div className="text-gray-500">Loading...</div>
      </div>
    );
  }

  return (
    <div className="flex h-full bg-gray-50">
      {/* Sources sidebar */}
      <div className="w-72 border-r bg-white flex flex-col">
        <div className="p-4 border-b">
          <h2 className="font-semibold text-lg">Data Sources</h2>
        </div>

        <div className="flex-1 overflow-auto p-2">
          {sources.length === 0 ? (
            <div className="text-center text-gray-500 text-sm p-4">
              No sources configured. Click "Add Source" to get started.
            </div>
          ) : (
            <div className="space-y-1">
              {sources.map((source) => (
                <div
                  key={source.source_id}
                  onClick={() => setSelectedSource(source)}
                  className={`p-3 rounded-lg cursor-pointer transition-colors ${
                    selectedSource?.source_id === source.source_id
                      ? 'bg-blue-50 border border-blue-200'
                      : 'hover:bg-gray-50 border border-transparent'
                  }`}
                >
                  <div className="font-medium text-sm">{source.name}</div>
                  <div className="text-xs text-gray-500 flex items-center gap-2 mt-1">
                    <span className="px-1.5 py-0.5 bg-gray-100 rounded text-xs">
                      {source.driver_type}
                    </span>
                    <span
                      className={`w-2 h-2 rounded-full ${
                        source.enabled ? 'bg-green-500' : 'bg-gray-300'
                      }`}
                    />
                  </div>
                  {source.last_error && (
                    <div className="text-xs text-red-600 mt-1 truncate">
                      ⚠ {source.last_error}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="p-3 border-t">
          <button
            onClick={() => setShowCreateSource(true)}
            className="w-full rounded-md bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700"
          >
            + Add Source
          </button>
        </div>
      </div>

      {/* Main content */}
      <div className="flex-1 flex flex-col overflow-hidden">
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
            jobs={jobs.filter((j) => j.source_id === selectedSource.source_id)}
            onStartJob={() => handleStartJob(selectedSource.source_id)}
            onDelete={async () => {
              if (confirm(`Delete source "${selectedSource.name}"?`)) {
                await fetch(`${API_BASE}/sources/${selectedSource.source_id}`, {
                  method: 'DELETE',
                });
                setSelectedSource(null);
                await loadSources();
              }
            }}
          />
        ) : (
          <div className="flex-1 overflow-auto p-6">
            <div className="mx-auto max-w-7xl space-y-4">
              <div>
                <h1 className="text-2xl font-bold">Ingestion Workbench</h1>
                <p className="mt-1 text-sm text-gray-500">
                  Browse the workspace, preview files, and route folders into canonical lakes.
                </p>
              </div>
              <WorkspaceBrowserCard onCreateSource={handleCreateSource} onStartJob={handleStartJob} />
            </div>
          </div>
        )}
      </div>

      {/* Create source modal */}
      {showCreateSource && (
        <CreateSourceModal
          onClose={() => setShowCreateSource(false)}
          onCreate={handleCreateSource}
        />
      )}
    </div>
  );
}

// Source detail view
function SourceDetailView({
  source,
  jobs,
  onStartJob,
  onDelete,
}: {
  source: Source;
  jobs: Job[];
  onStartJob: () => void;
  onDelete: () => void;
}) {
  return (
    <div className="flex-1 overflow-auto p-6">
      <div className="max-w-3xl mx-auto">
        {/* Header */}
        <div className="flex items-start justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold">{source.name}</h1>
            <div className="text-sm text-gray-500 mt-1">
              {source.driver_type} • Created {new Date(source.created_at).toLocaleDateString()}
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={onStartJob}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
            >
              ▶ Start Sync
            </button>
            <button
              onClick={onDelete}
              className="px-4 py-2 bg-red-100 text-red-700 rounded-md hover:bg-red-200"
            >
              Delete
            </button>
          </div>
        </div>

        {/* Config */}
        <div className="bg-white rounded-lg border p-4 mb-6">
          <h3 className="font-semibold mb-3">Configuration</h3>
          <pre className="text-sm bg-gray-50 p-3 rounded overflow-auto">
            {JSON.stringify(source.config, null, 2)}
          </pre>
        </div>

        {/* Recent jobs */}
        <div className="bg-white rounded-lg border p-4">
          <h3 className="font-semibold mb-3">Recent Jobs</h3>
          {jobs.length === 0 ? (
            <div className="text-sm text-gray-500">No jobs yet</div>
          ) : (
            <div className="space-y-2">
              {jobs.slice(0, 5).map((job) => (
                <div key={job.job_id} className="flex items-center justify-between p-2 bg-gray-50 rounded">
                  <div>
                    <div className="text-sm font-medium">
                      {job.total_files > 0
                        ? `${job.processed_files}/${job.total_files} files`
                        : 'Pending...'}
                    </div>
                    <div className="text-xs text-gray-500">
                      {new Date(job.created_at).toLocaleString()}
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <span
                      className={`px-2 py-0.5 rounded text-xs ${
                        job.status === 'completed'
                          ? 'bg-green-100 text-green-700'
                          : job.status === 'failed'
                          ? 'bg-red-100 text-red-700'
                          : job.status === 'running'
                          ? 'bg-blue-100 text-blue-700'
                          : 'bg-gray-100 text-gray-700'
                      }`}
                    >
                      {job.status}
                    </span>
                    <span className="text-sm text-gray-600">
                      {job.chunks_created} chunks
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// Job progress view
function JobProgressView({
  jobId,
  job,
  events,
  onCancel,
}: {
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
    <div className="flex-1 overflow-auto p-6">
      <div className="max-w-3xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold">
            Ingestion Job
            <span className="text-gray-400 text-lg ml-2 font-mono">
              {jobId.slice(0, 8)}
            </span>
          </h1>
          {isRunning && (
            <button
              onClick={onCancel}
              className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700"
            >
              Cancel
            </button>
          )}
        </div>

        {/* Progress bar */}
        <div className="mb-6">
          <div className="flex justify-between text-sm mb-2">
            <span className="font-medium">
              {statusLabel}
            </span>
            <span className="text-gray-600">{percent.toFixed(1)}%</span>
          </div>
          <div className="h-3 bg-gray-200 rounded-full overflow-hidden">
            <div
              className={`h-full transition-all duration-300 ${
                isRunning ? 'bg-blue-600' : 'bg-green-600'
              }`}
              style={{ width: `${percent}%` }}
            />
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-4 gap-4 mb-6">
            <StatCard
              label="Total"
              value={totalFiles}
            />
            <StatCard
              label="Processed"
              value={processedFiles}
              color="text-green-600"
            />
            <StatCard
              label="Failed"
              value={failedFiles}
              color="text-red-600"
            />
            <StatCard
              label="Chunks"
              value={chunksCreated}
              color="text-blue-600"
            />
          </div>

        {job && events.length === 0 ? (
          <div className="mb-4 rounded-md border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-700">
            Live progress is being shown via polling. The activity log may remain empty for this job.
          </div>
        ) : null}

        {/* Event log */}
        <div className="bg-white rounded-lg border">
          <div className="p-3 border-b font-semibold">Activity Log</div>
          <div className="max-h-96 overflow-auto p-2">
            {events.length === 0 ? (
              <div className="text-sm text-gray-500 text-center py-4">
                Waiting for events...
              </div>
            ) : (
              <div className="space-y-1 font-mono text-sm">
                {events.map((event, i) => (
                  <div
                    key={i}
                    className={`flex gap-2 p-1 rounded ${
                      event.type === 'file_error'
                        ? 'bg-red-50 text-red-700'
                        : event.type === 'file_complete'
                        ? 'bg-green-50'
                        : ''
                    }`}
                  >
                    <span className="text-gray-400 shrink-0">
                      {new Date(event.timestamp).toLocaleTimeString()}
                    </span>
                    <span className="truncate">
                      {event.file_path ? (
                        <>
                          {event.file_path}
                          {event.file_chunks && (
                            <span className="text-green-600 ml-1">
                              ({event.file_chunks} chunks)
                            </span>
                          )}
                          {event.file_error && (
                            <span className="text-red-600 ml-1">
                              — {event.file_error}
                            </span>
                          )}
                        </>
                      ) : (
                        event.type
                      )}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function StatCard({
  label,
  value,
  color = 'text-gray-900',
}: {
  label: string;
  value: number;
  color?: string;
}) {
  return (
    <div className="bg-white rounded-lg border p-4 text-center">
      <div className={`text-2xl font-bold ${color}`}>
        {value.toLocaleString()}
      </div>
      <div className="text-sm text-gray-500">{label}</div>
    </div>
  );
}

// Create source form
interface CreateSourceForm {
  driver_type: string;
  name: string;
  config: Record<string, unknown>;
  collections: string[];
  file_types?: string[];
  include_patterns?: string[];
  exclude_patterns?: string[];
}

function CreateSourceModal({
  onClose,
  onCreate,
}: {
  onClose: () => void;
  onCreate: (data: CreateSourceForm) => void;
}) {
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
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md p-6">
        <h2 className="text-xl font-bold mb-4">Add Data Source</h2>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Driver type */}
          <div>
            <label className="block text-sm font-medium mb-1">Source Type</label>
            <select
              value={driverType}
              onChange={(e) => setDriverType(e.target.value)}
              className="w-full rounded-md border-gray-300 border p-2"
            >
              <option value="local">Local Filesystem</option>
              {/* Future drivers will be added here */}
              <option value="github" disabled>
                GitHub (coming soon)
              </option>
              <option value="google_drive" disabled>
                Google Drive (coming soon)
              </option>
            </select>
          </div>

          {/* Name */}
          <div>
            <label className="block text-sm font-medium mb-1">Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={`${driverType} source`}
              className="w-full rounded-md border-gray-300 border p-2"
            />
          </div>

          {/* Driver-specific config */}
          {driverType === 'local' && (
            <div>
              <label className="block text-sm font-medium mb-1">Root Path</label>
              <input
                type="text"
                value={rootPath}
                onChange={(e) => setRootPath(e.target.value)}
                placeholder="/path/to/your/documents"
                className="w-full rounded-md border-gray-300 border p-2"
              />
              <p className="text-xs text-gray-500 mt-1">
                The root directory to scan for documents
              </p>
            </div>
          )}

          <div>
            <label className="block text-sm font-medium mb-1">File Types</label>
            <input
              type="text"
              value={fileTypes}
              onChange={(e) => setFileTypes(e.target.value)}
              placeholder=".md,.txt,.json"
              className="w-full rounded-md border-gray-300 border p-2"
            />
            <p className="text-xs text-gray-500 mt-1">
              Comma-separated extensions. Defaults to text-like files only.
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Include Patterns</label>
            <input
              type="text"
              value={includePatterns}
              onChange={(e) => setIncludePatterns(e.target.value)}
              placeholder="docs/**/*.md,specs/**/*.md"
              className="w-full rounded-md border-gray-300 border p-2"
            />
            <p className="text-xs text-gray-500 mt-1">
              Optional glob patterns to narrow what gets scanned.
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Exclude Patterns</label>
            <input
              type="text"
              value={excludePatterns}
              onChange={(e) => setExcludePatterns(e.target.value)}
              placeholder="**/node_modules/**,**/*.png"
              className="w-full rounded-md border-gray-300 border p-2"
            />
            <p className="text-xs text-gray-500 mt-1">
              Optional glob patterns to skip binary, generated, or noisy files.
            </p>
          </div>

          {/* Collections */}
          <div>
            <label className="block text-sm font-medium mb-1">Collections</label>
            <input
              type="text"
              value={collections}
              onChange={(e) => setCollections(e.target.value)}
              placeholder="devel_docs"
              className="w-full rounded-md border-gray-300 border p-2"
            />
            <p className="text-xs text-gray-500 mt-1">
              Comma-separated list of Qdrant collections to index into
            </p>
          </div>

          {/* Actions */}
          <div className="flex gap-2 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 border rounded-md hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex-1 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
            >
              Create Source
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
