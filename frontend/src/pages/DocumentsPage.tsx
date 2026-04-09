import React, { useState, useEffect, useRef } from 'react';
import {
  activateDatabaseProfile,
  createDatabaseProfile,
  deleteDatabaseProfile,
  fetchDocuments,
  fetchIngestionHistory,
  listDatabaseProfiles,
  updateDatabaseProfile,
  uploadDocuments,
  deleteDocument,
  ingestDocuments,
  fetchIngestionProgress,
  restartIngestion,
  makeDatabasePrivate,
  ProxyApiError,
} from '../lib/nextApi';
import { DocumentsPageView } from './documents-page/DocumentsPageView';

export default function DocumentsPage() {
  const [documents, setDocuments] = useState<any[]>([]);
  const [selectedDocs, setSelectedDocs] = useState<Set<string>>(new Set());
  const [isUploading, setIsUploading] = useState(false);
  const [isIngesting, setIsIngesting] = useState(false);
  const [progress, setProgress] = useState<any>(null);
  const [dbInfo, setDbInfo] = useState<any>(null);
  const [selectedDbId, setSelectedDbId] = useState('');
  const [newDbName, setNewDbName] = useState('');
  const [newDbUseLocalDocs, setNewDbUseLocalDocs] = useState(true);
  const [newDbForumMode, setNewDbForumMode] = useState(false);
  const [newDbPublicBaseUrl, setNewDbPublicBaseUrl] = useState('https://docs.example.com');
  const [newDbFiles, setNewDbFiles] = useState<File[]>([]);
  const [editDbName, setEditDbName] = useState('');
  const [editDbBaseUrl, setEditDbBaseUrl] = useState('');
  const [editDbUseLocalDocs, setEditDbUseLocalDocs] = useState(true);
  const [editDbForumMode, setEditDbForumMode] = useState(false);
  const [isCreatingDb, setIsCreatingDb] = useState(false);
  const [isSwitchingDb, setIsSwitchingDb] = useState(false);
  const [isSavingDbMeta, setIsSavingDbMeta] = useState(false);
  const [isDeletingDb, setIsDeletingDb] = useState(false);
  const [isPrivatizingDb, setIsPrivatizingDb] = useState(false);
  const [isRestartingIngestion, setIsRestartingIngestion] = useState(false);
  const [ingestionMessage, setIngestionMessage] = useState('');
  const [progressSamples, setProgressSamples] = useState<Array<{ ts: number; processed: number }>>([]);
  const [lastRestartAt, setLastRestartAt] = useState<number | null>(null);
  const [ingestionHistory, setIngestionHistory] = useState<any[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const selectedDb = (dbInfo?.databases || []).find((db: any) => db.id === selectedDbId);
  const selectedDbCanAccess = selectedDb ? selectedDb.canAccess !== false : true;

  const loadDocuments = async () => {
    try {
      const data = await fetchDocuments();
      setDocuments(data.documents || []);
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    loadDocuments();
    loadDatabases();
    loadIngestionHistory();
    const interval = setInterval(async () => {
      try {
        const progData = await fetchIngestionProgress();
        if (progData.active || progData.canResumeForum) {
          setIsIngesting(Boolean(progData.active));
          setProgress({ ...(progData.progress || {}), canResumeForum: Boolean(progData.canResumeForum), stale: Boolean(progData.stale) });
          setProgressSamples((prev) => {
            const now = Date.now();
            const next = [...prev, { ts: now, processed: Number(progData.progress?.processedChunks || 0) }]
              .filter((s) => now - s.ts <= 60_000)
              .slice(-120);
            return next;
          });
        } else {
          setIsIngesting(false);
          setProgress(null);
          setProgressSamples([]);
          void loadIngestionHistory();
        }
      } catch (err) {
        console.error(err);
      }
    }, 2000);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    const selected = (dbInfo?.databases || []).find((db: any) => db.id === selectedDbId);
    if (selected) {
      setEditDbName(selected.name || '');
      setEditDbBaseUrl(selected.publicDocsBaseUrl || '');
      setEditDbUseLocalDocs(Boolean(selected.useLocalDocsBaseUrl));
      setEditDbForumMode(Boolean(selected.forumMode));
    }
  }, [dbInfo, selectedDbId]);

  const loadDatabases = async () => {
    try {
      const data = await listDatabaseProfiles();
      setDbInfo(data);
      setSelectedDbId(data.activeDatabaseId);
      const active = (data.databases || []).find((db: any) => db.id === data.activeDatabaseId);
      setEditDbName(active?.name || '');
      setEditDbBaseUrl(active?.publicDocsBaseUrl || '');
      setEditDbUseLocalDocs(Boolean(active?.useLocalDocsBaseUrl));
      setEditDbForumMode(Boolean(active?.forumMode));
    } catch (error) {
      console.error(error);
    }
  };

  const loadIngestionHistory = async () => {
    try {
      const data = await fetchIngestionHistory();
      setIngestionHistory(data.items || []);
    } catch (error) {
      console.error(error);
    }
  };

  const handleCreateDatabase = async () => {
    const name = newDbName.trim();
    if (!name) return;
    setIsCreatingDb(true);
    try {
      await createDatabaseProfile({
        name,
        activate: true,
        useLocalDocsBaseUrl: newDbUseLocalDocs,
        publicDocsBaseUrl: newDbUseLocalDocs ? undefined : newDbPublicBaseUrl.trim(),
        forumMode: newDbForumMode,
      });
      if (newDbFiles.length > 0) {
        await uploadDocuments(newDbFiles, true);
      }
      setNewDbName('');
      setNewDbFiles([]);
      setSelectedDocs(new Set());
      await Promise.all([loadDatabases(), loadDocuments(), loadIngestionHistory()]);
    } catch (error) {
      console.error('Create database failed:', error);
    } finally {
      setIsCreatingDb(false);
    }
  };

  const handleActivateDatabase = async () => {
    if (isIngesting) return;
    if (!selectedDbId || selectedDbId === dbInfo?.activeDatabaseId) return;
    setIsSwitchingDb(true);
    try {
      await activateDatabaseProfile(selectedDbId);
      setSelectedDocs(new Set());
      await Promise.all([loadDatabases(), loadDocuments(), loadIngestionHistory()]);
    } catch (error) {
      console.error('Switch database failed:', error);
    } finally {
      setIsSwitchingDb(false);
    }
  };

  const handleSaveDatabaseMeta = async () => {
    if (!selectedDbId) return;
    setIsSavingDbMeta(true);
    try {
      await updateDatabaseProfile(selectedDbId, {
        name: editDbName.trim() || undefined,
        useLocalDocsBaseUrl: editDbUseLocalDocs,
        forumMode: editDbForumMode,
        publicDocsBaseUrl: editDbUseLocalDocs ? undefined : editDbBaseUrl.trim() || undefined,
      });
      await loadDatabases();
    } catch (error) {
      console.error('Update database failed:', error);
    } finally {
      setIsSavingDbMeta(false);
    }
  };

  const handleDeleteDatabase = async () => {
    if (!selectedDbId || selectedDbId === dbInfo?.activeDatabaseId) return;
    if (!confirm('Delete this lake profile? This does not delete the underlying vector index, only the Knoxx lake profile.')) return;
    setIsDeletingDb(true);
    try {
      await deleteDatabaseProfile(selectedDbId);
      await loadDatabases();
    } catch (error) {
      console.error('Delete database failed:', error);
    } finally {
      setIsDeletingDb(false);
    }
  };

  const handleMakeDatabasePrivate = async () => {
    if (!selectedDbId) return;
    if (!confirm('Make this lake profile private to your current browser session? Other sessions will no longer see it.')) return;
    setIsPrivatizingDb(true);
    try {
      await makeDatabasePrivate(selectedDbId);
      await loadDatabases();
    } catch (error) {
      console.error('Make private failed:', error);
    } finally {
      setIsPrivatizingDb(false);
    }
  };

  const elapsedSeconds = progress?.startedAt ? Math.max(1, (Date.now() - new Date(progress.startedAt).getTime()) / 1000) : 0;
  const chunksPerSec = (() => {
    if (progressSamples.length < 2) {
      return progress?.processedChunks && elapsedSeconds ? progress.processedChunks / elapsedSeconds : 0;
    }
    const first = progressSamples[0];
    const last = progressSamples[progressSamples.length - 1];
    const dt = Math.max(1, (last.ts - first.ts) / 1000);
    const dChunks = Math.max(0, last.processed - first.processed);
    return dChunks / dt;
  })();
  const remainingChunks = progress ? Math.max(0, (progress.totalChunks || 0) - (progress.processedChunks || 0)) : 0;
  const etaSeconds = chunksPerSec > 0 ? remainingChunks / chunksPerSec : 0;

  const formatEta = (seconds: number) => {
    if (!seconds || !Number.isFinite(seconds)) return 'Estimating...';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    if (mins <= 0) return `${secs}s`;
    return `${mins}m ${secs}s`;
  };

  const toggleSelectDoc = (path: string) => {
    const newSelected = new Set(selectedDocs);
    if (newSelected.has(path)) {
      newSelected.delete(path);
    } else {
      newSelected.add(path);
    }
    setSelectedDocs(newSelected);
  };

  const toggleSelectAll = () => {
    if (selectedDocs.size === documents.length) {
      setSelectedDocs(new Set());
    } else {
      setSelectedDocs(new Set(documents.map(d => d.relativePath)));
    }
  };

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>, autoIngest: boolean) => {
    if (!e.target.files || e.target.files.length === 0) return;
    setIsUploading(true);
    const filesArray = Array.from(e.target.files);
    try {
      await uploadDocuments(filesArray, autoIngest);
      await loadDocuments();
    } catch (error) {
      console.error('Upload failed:', error);
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleDelete = async (path: string) => {
    if (!confirm(`Are you sure you want to delete ${path}?`)) return;
    try {
      await deleteDocument(path);
      await loadDocuments();
      setSelectedDocs(prev => {
        const next = new Set(prev);
        next.delete(path);
        return next;
      });
    } catch (error) {
      console.error('Delete failed:', error);
    }
  };

  const handleIngestSelected = async () => {
    if (selectedDocs.size === 0) return;
    try {
      setIsIngesting(true);
      await ingestDocuments({ selectedFiles: Array.from(selectedDocs) });
    } catch (error) {
      console.error('Ingest failed:', error);
      setIsIngesting(false);
    }
  };

  const handleIngestAll = async () => {
    try {
      setIsIngesting(true);
      await ingestDocuments({ full: true });
    } catch (error) {
      console.error('Ingest failed:', error);
      setIsIngesting(false);
    }
  };

  const handleRestartIngestion = async () => {
    try {
      setIsRestartingIngestion(true);
      setIngestionMessage('');
      const before = await fetchIngestionProgress();
      if (!before.active && !before.canResumeForum) {
        setIsIngesting(false);
        setProgress(null);
        setProgressSamples([]);
        setIngestionMessage('No active ingestion run to restart. Start a new ingest instead.');
        return;
      }
      const shouldForceFresh = Boolean(before.stale && before.canResumeForum);
      const restartResult = await restartIngestion(shouldForceFresh);
      if (restartResult?.resumed === false) {
        setIsIngesting(false);
        setProgress(null);
        setProgressSamples([]);
        setIngestionMessage(String(restartResult?.message || 'No active ingestion run to restart.'));
        return;
      }
      const progData = await fetchIngestionProgress();
      setIsIngesting(Boolean(progData.active));
      setProgress(progData.progress ? { ...progData.progress, canResumeForum: Boolean(progData.canResumeForum), stale: Boolean(progData.stale) } : null);
      setProgressSamples([]);
      setLastRestartAt(Date.now());
      setIngestionMessage(
        shouldForceFresh
          ? 'Ingestion was stalled; started fresh forum ingestion from scratch.'
          : 'Ingestion restart requested. Resuming from saved progress...'
      );
    } catch (error) {
      const msg = error instanceof Error ? error.message : String(error);
      const isNoActiveRestart =
        (error instanceof ProxyApiError && error.status === 400 && error.body.includes('No active ingestion to restart')) ||
        msg.includes('No active ingestion to restart');
      if (isNoActiveRestart) {
        setIsIngesting(false);
        setProgress(null);
        setProgressSamples([]);
        setIngestionMessage('No active ingestion run to restart. Start a new ingest instead.');
      } else {
        setIngestionMessage('Restart failed. Please try again or start a fresh ingest run.');
      }
    } finally {
      setIsRestartingIngestion(false);
    }
  };

  return (
    <DocumentsPageView
      documents={documents}
      selectedDocs={selectedDocs}
      isUploading={isUploading}
      isIngesting={isIngesting}
      progress={progress}
      dbInfo={dbInfo}
      selectedDbId={selectedDbId}
      setSelectedDbId={setSelectedDbId}
      newDbName={newDbName}
      setNewDbName={setNewDbName}
      newDbUseLocalDocs={newDbUseLocalDocs}
      setNewDbUseLocalDocs={setNewDbUseLocalDocs}
      newDbForumMode={newDbForumMode}
      setNewDbForumMode={setNewDbForumMode}
      newDbPublicBaseUrl={newDbPublicBaseUrl}
      setNewDbPublicBaseUrl={setNewDbPublicBaseUrl}
      newDbFiles={newDbFiles}
      setNewDbFiles={setNewDbFiles}
      editDbName={editDbName}
      setEditDbName={setEditDbName}
      editDbBaseUrl={editDbBaseUrl}
      setEditDbBaseUrl={setEditDbBaseUrl}
      editDbUseLocalDocs={editDbUseLocalDocs}
      setEditDbUseLocalDocs={setEditDbUseLocalDocs}
      editDbForumMode={editDbForumMode}
      setEditDbForumMode={setEditDbForumMode}
      isCreatingDb={isCreatingDb}
      isSwitchingDb={isSwitchingDb}
      isSavingDbMeta={isSavingDbMeta}
      isDeletingDb={isDeletingDb}
      isPrivatizingDb={isPrivatizingDb}
      isRestartingIngestion={isRestartingIngestion}
      ingestionMessage={ingestionMessage}
      chunksPerSec={chunksPerSec}
      remainingChunks={remainingChunks}
      etaSeconds={etaSeconds}
      lastRestartAt={lastRestartAt}
      ingestionHistory={ingestionHistory}
      fileInputRef={fileInputRef}
      selectedDbCanAccess={selectedDbCanAccess}
      formatEta={formatEta}
      onUpload={handleUpload}
      onCreateDatabase={handleCreateDatabase}
      onActivateDatabase={handleActivateDatabase}
      onSaveDatabaseMeta={handleSaveDatabaseMeta}
      onDeleteDatabase={handleDeleteDatabase}
      onMakeDatabasePrivate={handleMakeDatabasePrivate}
      onIngestSelected={handleIngestSelected}
      onIngestAll={handleIngestAll}
      onRestartIngestion={handleRestartIngestion}
      onToggleSelectAll={toggleSelectAll}
      onToggleSelectDoc={toggleSelectDoc}
      onDeleteDoc={handleDelete}
    />
  );
}
