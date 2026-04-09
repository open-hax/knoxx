import type { ChangeEvent, Dispatch, RefObject, SetStateAction } from "react";

type DocumentsPageViewProps = {
  documents: any[];
  selectedDocs: Set<string>;
  isUploading: boolean;
  isIngesting: boolean;
  progress: any;
  dbInfo: any;
  selectedDbId: string;
  setSelectedDbId: Dispatch<SetStateAction<string>>;
  newDbName: string;
  setNewDbName: Dispatch<SetStateAction<string>>;
  newDbUseLocalDocs: boolean;
  setNewDbUseLocalDocs: Dispatch<SetStateAction<boolean>>;
  newDbForumMode: boolean;
  setNewDbForumMode: Dispatch<SetStateAction<boolean>>;
  newDbPublicBaseUrl: string;
  setNewDbPublicBaseUrl: Dispatch<SetStateAction<string>>;
  newDbFiles: File[];
  setNewDbFiles: Dispatch<SetStateAction<File[]>>;
  editDbName: string;
  setEditDbName: Dispatch<SetStateAction<string>>;
  editDbBaseUrl: string;
  setEditDbBaseUrl: Dispatch<SetStateAction<string>>;
  editDbUseLocalDocs: boolean;
  setEditDbUseLocalDocs: Dispatch<SetStateAction<boolean>>;
  editDbForumMode: boolean;
  setEditDbForumMode: Dispatch<SetStateAction<boolean>>;
  isCreatingDb: boolean;
  isSwitchingDb: boolean;
  isSavingDbMeta: boolean;
  isDeletingDb: boolean;
  isPrivatizingDb: boolean;
  isRestartingIngestion: boolean;
  ingestionMessage: string;
  chunksPerSec: number;
  remainingChunks: number;
  etaSeconds: number;
  lastRestartAt: number | null;
  ingestionHistory: any[];
  fileInputRef: RefObject<HTMLInputElement>;
  selectedDbCanAccess: boolean;
  formatEta: (seconds: number) => string;
  onUpload: (event: ChangeEvent<HTMLInputElement>, autoIngest: boolean) => Promise<void>;
  onCreateDatabase: () => Promise<void>;
  onActivateDatabase: () => Promise<void>;
  onSaveDatabaseMeta: () => Promise<void>;
  onDeleteDatabase: () => Promise<void>;
  onMakeDatabasePrivate: () => Promise<void>;
  onIngestSelected: () => Promise<void>;
  onIngestAll: () => Promise<void>;
  onRestartIngestion: () => Promise<void>;
  onToggleSelectAll: () => void;
  onToggleSelectDoc: (path: string) => void;
  onDeleteDoc: (path: string) => Promise<void>;
};

export function DocumentsPageView({
  documents,
  selectedDocs,
  isUploading,
  isIngesting,
  progress,
  dbInfo,
  selectedDbId,
  setSelectedDbId,
  newDbName,
  setNewDbName,
  newDbUseLocalDocs,
  setNewDbUseLocalDocs,
  newDbForumMode,
  setNewDbForumMode,
  newDbPublicBaseUrl,
  setNewDbPublicBaseUrl,
  newDbFiles,
  setNewDbFiles,
  editDbName,
  setEditDbName,
  editDbBaseUrl,
  setEditDbBaseUrl,
  editDbUseLocalDocs,
  setEditDbUseLocalDocs,
  editDbForumMode,
  setEditDbForumMode,
  isCreatingDb,
  isSwitchingDb,
  isSavingDbMeta,
  isDeletingDb,
  isPrivatizingDb,
  isRestartingIngestion,
  ingestionMessage,
  chunksPerSec,
  remainingChunks,
  etaSeconds,
  lastRestartAt,
  ingestionHistory,
  fileInputRef,
  selectedDbCanAccess,
  formatEta,
  onUpload,
  onCreateDatabase,
  onActivateDatabase,
  onSaveDatabaseMeta,
  onDeleteDatabase,
  onMakeDatabasePrivate,
  onIngestSelected,
  onIngestAll,
  onRestartIngestion,
  onToggleSelectAll,
  onToggleSelectDoc,
  onDeleteDoc,
}: DocumentsPageViewProps) {
  return (
    <div className="p-8 max-w-5xl mx-auto space-y-6 text-slate-100">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold">Data Lakes</h1>
          <p className="mt-1 text-sm text-slate-400">
            Knoxx now treats lakes as the primary document boundary. This page manages the active runtime lake profile that ingestion and retrieval are using right now.
          </p>
        </div>
        <div className="flex gap-4">
          <input type="file" multiple ref={fileInputRef} className="hidden" onChange={(e) => void onUpload(e, true)} />
          <button onClick={() => fileInputRef.current?.click()} disabled={isUploading} className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
            {isUploading ? "Uploading..." : "Upload & Auto-Ingest"}
          </button>
        </div>
      </div>

      <div className="rounded-md border border-slate-700 bg-slate-900 p-4">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-300">Upload Files to Active Lake</h2>
        <p className="text-xs text-slate-400 mt-1">Upload files or zip archives. They will be placed in the active lake docs path and auto-ingested.</p>
        <div className="mt-3 flex items-center gap-3">
          <input type="file" multiple onChange={(e) => void onUpload(e, true)} className="block w-full text-xs text-slate-300 file:mr-3 file:rounded file:border-0 file:bg-slate-700 file:px-3 file:py-1.5 file:text-slate-100" />
        </div>
      </div>

      <div className="rounded-md border border-slate-700 bg-slate-900 p-4 space-y-3">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-300">Lake Runtime Profiles</h2>
        <p className="text-xs text-slate-400">Org-owned data lakes are the control-plane truth. This runtime surface lets you activate and tune the lake profile currently mounted for document ingest, preview, and retrieval.</p>
        <div className="grid gap-3 md:grid-cols-3">
          <div className="md:col-span-2">
            <label className="text-xs text-slate-400">Active Lake Profile</label>
            <div className="mt-1 flex gap-2">
              <select className="field-input" value={selectedDbId} onChange={(e) => setSelectedDbId(e.target.value)}>
                {(dbInfo?.databases || []).map((db: any) => (
                  <option key={db.id} value={db.id}>
                    {db.name} · index {db.qdrantCollection}{db.privateToSession ? (db.canAccess === false ? " [private: other session]" : " [private]") : ""}
                  </option>
                ))}
              </select>
              <button onClick={() => void onActivateDatabase()} disabled={isIngesting || isSwitchingDb || !selectedDbId || selectedDbId === dbInfo?.activeDatabaseId || !selectedDbCanAccess} className="px-3 py-2 rounded bg-cyan-600 text-white hover:bg-cyan-500 disabled:opacity-50">
                {isSwitchingDb ? "Activating..." : "Activate"}
              </button>
            </div>
            <p className="text-xs text-slate-400 mt-1">Mounted docs path: {dbInfo?.activeRuntime?.docsPath || "N/A"}</p>
            {isIngesting ? <p className="text-xs text-amber-300 mt-1">Lake switching is disabled while ingestion is active.</p> : null}
            {!selectedDbCanAccess ? <p className="text-xs text-rose-300 mt-1">This lake profile is private to another session. You can view it but cannot activate or edit it.</p> : null}
          </div>
          <div>
            <label className="text-xs text-slate-400">Create New Lake Profile</label>
            <div className="mt-1 flex gap-2">
              <input value={newDbName} onChange={(e) => setNewDbName(e.target.value)} className="field-input" placeholder="e.g. Engine Manuals" />
              <button onClick={() => void onCreateDatabase()} disabled={isCreatingDb || !newDbName.trim()} className="px-3 py-2 rounded bg-emerald-600 text-white hover:bg-emerald-500 disabled:opacity-50">
                {isCreatingDb ? "Creating..." : newDbFiles.length > 0 ? "Create + Upload" : "Create"}
              </button>
            </div>
            <div className="mt-2 space-y-2">
              <label className="flex items-center gap-2 text-xs text-slate-300"><input type="checkbox" checked={newDbForumMode} onChange={(e) => setNewDbForumMode(e.target.checked)} />Forum mode for this lake</label>
              <label className="flex items-center gap-2 text-xs text-slate-300"><input type="checkbox" checked={newDbUseLocalDocs} onChange={(e) => setNewDbUseLocalDocs(e.target.checked)} />Use local docs viewer links</label>
              {!newDbUseLocalDocs ? <input className="field-input" placeholder="https://docs.example.com" value={newDbPublicBaseUrl} onChange={(e) => setNewDbPublicBaseUrl(e.target.value)} /> : null}
              <input type="file" multiple onChange={(e) => setNewDbFiles(Array.from(e.target.files || []))} className="block w-full text-xs text-slate-300 file:mr-3 file:rounded file:border-0 file:bg-slate-700 file:px-3 file:py-1.5 file:text-slate-100" />
              <p className="text-[11px] text-slate-400">Optional bootstrap upload (.zip or files) after creating the lake profile.</p>
            </div>
          </div>
        </div>

        <div className="grid gap-3 md:grid-cols-3">
          <div>
            <label className="text-xs text-slate-400">Display Name</label>
            <input className="field-input mt-1" value={editDbName} onChange={(e) => setEditDbName(e.target.value)} />
          </div>
          <div className="md:col-span-2">
            <label className="text-xs text-slate-400">Public Docs Base URL</label>
            <input className="field-input mt-1" value={editDbBaseUrl} onChange={(e) => setEditDbBaseUrl(e.target.value)} disabled={editDbUseLocalDocs} />
          </div>
        </div>
        <div className="flex flex-wrap gap-4 text-xs text-slate-300">
          <label className="flex items-center gap-2"><input type="checkbox" checked={editDbForumMode} onChange={(e) => setEditDbForumMode(e.target.checked)} />Forum mode</label>
          <label className="flex items-center gap-2"><input type="checkbox" checked={editDbUseLocalDocs} onChange={(e) => setEditDbUseLocalDocs(e.target.checked)} />Use local docs viewer links</label>
        </div>
        <div className="flex gap-2">
          <button onClick={() => void onSaveDatabaseMeta()} disabled={isSavingDbMeta || !selectedDbId || !selectedDbCanAccess} className="px-3 py-2 rounded bg-indigo-600 text-white hover:bg-indigo-500 disabled:opacity-50">{isSavingDbMeta ? "Saving..." : "Save Lake Profile"}</button>
          <button onClick={() => void onDeleteDatabase()} disabled={isDeletingDb || !selectedDbId || selectedDbId === dbInfo?.activeDatabaseId || isIngesting || !selectedDbCanAccess} className="px-3 py-2 rounded bg-rose-700 text-white hover:bg-rose-600 disabled:opacity-50">{isDeletingDb ? "Deleting..." : "Delete Lake Profile"}</button>
          <button onClick={() => void onMakeDatabasePrivate()} disabled={isPrivatizingDb || !selectedDbId || !selectedDbCanAccess} className="px-3 py-2 rounded bg-amber-700 text-white hover:bg-amber-600 disabled:opacity-50">{isPrivatizingDb ? "Applying..." : "Make Session-Private"}</button>
        </div>
      </div>

      {isIngesting && progress ? (
        <div className="bg-cyan-500/10 border border-cyan-500/30 p-4 rounded-md">
          <h3 className="font-semibold text-cyan-200">Ingestion in Progress</h3>
          <div className="w-full bg-slate-800 rounded-full h-2.5 mt-2"><div className="bg-cyan-400 h-2.5 rounded-full" style={{ width: `${progress.percent || 0}%` }} /></div>
          <p className="text-sm mt-2 text-cyan-100">{progress.processedChunks} / {progress.totalChunks} chunks processed ({Number(progress.percentPrecise ?? progress.percent ?? 0).toFixed(2)}%)</p>
          <p className="text-xs text-cyan-200/90 mt-1">Throughput: {chunksPerSec.toFixed(2)} chunks/s | Remaining: {remainingChunks} chunks | ETA: {formatEta(etaSeconds)}</p>
          {lastRestartAt ? <p className="text-xs text-cyan-200/80">Last restart requested at {new Date(lastRestartAt).toLocaleTimeString()}</p> : null}
          <p className="text-xs text-cyan-200/80 truncate">{progress.currentFile}</p>
          {progress.stale ? <p className="text-xs text-amber-300 mt-1">Progress appears stalled. Use restart resume.</p> : null}
        </div>
      ) : null}
      {!isIngesting && progress?.canResumeForum ? (
        <div className="bg-amber-500/10 border border-amber-500/30 p-4 rounded-md">
          <h3 className="font-semibold text-amber-200">Resumable Forum Ingestion Found</h3>
          <p className="text-xs text-amber-100 mt-1">Last checkpoint: {progress.currentFile || "N/A"} ({Number(progress.percentPrecise ?? progress.percent ?? 0).toFixed(2)}%).</p>
          <p className="text-xs text-amber-200/80 mt-1">Press restart to resume from checkpoint.</p>
        </div>
      ) : null}

      <div className="flex items-center gap-4 bg-slate-900 p-4 rounded-md border border-slate-700">
        <button onClick={() => void onIngestSelected()} disabled={selectedDocs.size === 0 || isIngesting} className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50">Ingest Selected ({selectedDocs.size})</button>
        <button onClick={() => void onIngestAll()} disabled={isIngesting} className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700 disabled:opacity-50">Ingest All Missing</button>
        <button onClick={() => void onRestartIngestion()} disabled={(!isIngesting && !progress?.canResumeForum) || isRestartingIngestion} className="px-4 py-2 bg-amber-600 text-white rounded hover:bg-amber-500 disabled:opacity-50">{isRestartingIngestion ? "Restarting..." : progress?.stale ? "Restart Ingestion (Force Fresh)" : "Restart Ingestion (Resume)"}</button>
      </div>

      {ingestionMessage ? <div className="rounded-md border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">{ingestionMessage}</div> : null}

      <div className="overflow-x-auto border border-slate-700 rounded-md bg-slate-900">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-800 border-b border-slate-700">
              <th className="p-3 w-12 text-center"><input type="checkbox" checked={documents.length > 0 && selectedDocs.size === documents.length} onChange={onToggleSelectAll} /></th>
              <th className="p-3 font-semibold">Name</th>
              <th className="p-3 font-semibold">Size</th>
              <th className="p-3 font-semibold">Status</th>
              <th className="p-3 font-semibold text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {documents.length === 0 ? (
              <tr><td colSpan={5} className="p-8 text-center text-slate-400">No documents found.</td></tr>
            ) : (
              documents.map((doc) => (
                <tr key={doc.relativePath} className="border-b border-slate-800 hover:bg-slate-800/60">
                  <td className="p-3 text-center"><input type="checkbox" checked={selectedDocs.has(doc.relativePath)} onChange={() => onToggleSelectDoc(doc.relativePath)} /></td>
                  <td className="p-3"><div className="font-medium">{doc.name}</div><div className="text-xs text-slate-400">{doc.relativePath}</div></td>
                  <td className="p-3 text-sm text-slate-300">{(doc.size / 1024).toFixed(1)} KB</td>
                  <td className="p-3">{doc.indexed ? <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">Indexed ({doc.chunkCount} chunks)</span> : <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-amber-500/20 text-amber-300 border border-amber-500/30">Pending</span>}</td>
                  <td className="p-3 text-right"><button onClick={() => void onDeleteDoc(doc.relativePath)} className="text-red-600 hover:text-red-800 text-sm font-medium">Delete</button></td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="rounded-md border border-slate-700 bg-slate-900 p-4">
        <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-300 mb-3">Ingestion History (Current Lake)</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="border-b border-slate-700 text-slate-300">
                <th className="p-2">Completed</th>
                <th className="p-2">Mode</th>
                <th className="p-2">Chunks Upserted</th>
                <th className="p-2">Files Updated</th>
                <th className="p-2">Duration</th>
                <th className="p-2">Errors</th>
              </tr>
            </thead>
            <tbody>
              {ingestionHistory.length === 0 ? (
                <tr><td colSpan={6} className="p-3 text-slate-400">No ingestion runs yet for this lake.</td></tr>
              ) : (
                ingestionHistory.map((item: any) => (
                  <tr key={item.id} className="border-b border-slate-800">
                    <td className="p-2 text-slate-200">{new Date(item.completedAt).toLocaleString()}</td>
                    <td className="p-2 text-slate-300">{item.mode}</td>
                    <td className="p-2 text-slate-300">{item.chunksUpserted ?? item.processedChunks ?? 0}</td>
                    <td className="p-2 text-slate-300">{item.filesUpdated ?? 0}</td>
                    <td className="p-2 text-slate-300">{item.durationSeconds ?? 0}s</td>
                    <td className="p-2 text-slate-300">{item.errors ?? 0}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
