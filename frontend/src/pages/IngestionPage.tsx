import { useState, useEffect } from 'react';
import { Badge, Button, Card, Spinner } from '@open-hax/uxx';
import WorkspaceBrowserCard, { type BrowserCreateSourceForm, type BrowserCreatedSource } from '../components/WorkspaceBrowserCard';
import { CreateSourceModal, JobProgressView, SourceDetailView } from './ingestion-page/parts';
import type { CreateSourceForm, Job, ProgressEvent, Source } from './ingestion-page/types';

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
    <div style={{ display: 'flex', height: '100%', backgroundColor: 'var(--token-colors-background-surface)' }}>
      <Card variant="default" padding="none" style={{ width: 288, flexShrink: 0, display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: 16, borderBottom: '1px solid var(--token-colors-border-default)' }}>
          <h2 style={{ fontWeight: 600, fontSize: 18 }}>Data Sources</h2>
        </div>
        <div style={{ flex: 1, overflow: 'auto', padding: 8 }}>
          {sources.length === 0 ? (
            <div style={{ textAlign: 'center', color: 'var(--token-colors-text-muted)', fontSize: 14, padding: 16 }}>
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
                  style={{ cursor: 'pointer', border: selectedSource?.source_id === source.source_id ? '2px solid var(--token-colors-alpha-blue-_20)' : 'transparent' }}
                >
                  <div style={{ fontWeight: 500, fontSize: 14 }}>{source.name}</div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 4 }}>
                    <Badge variant="default" size="sm">{source.driver_type}</Badge>
                    <span style={{ width: 8, height: 8, borderRadius: '50%', backgroundColor: source.enabled ? 'var(--token-colors-accent-green)' : 'var(--token-colors-text-muted)' }} />
                  </div>
                  {source.last_error && (
                    <div style={{ fontSize: 12, color: 'var(--token-colors-accent-red)', marginTop: 4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      ⚠ {source.last_error}
                    </div>
                  )}
                </Card>
              ))}
            </div>
          )}
        </div>
        <div style={{ padding: 12, borderTop: '1px solid var(--token-colors-border-default)' }}>
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
              <p style={{ marginTop: 4, fontSize: 14, color: 'var(--token-colors-text-muted)' }}>
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
