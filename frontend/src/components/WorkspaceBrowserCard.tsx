import { useCallback, useEffect, useMemo, useState, type ChangeEvent } from 'react';
import { Button, Card, Input, Spinner } from '@open-hax/uxx';

type BrowserEntry = {
  name: string;
  path: string;
  type: 'dir' | 'file';
  size?: number | null;
  previewable?: boolean;
};

type BrowseResponse = {
  workspace_root: string;
  current_path: string;
  entries: BrowserEntry[];
};

type PreviewResponse = {
  path: string;
  size: number;
  truncated: boolean;
  content: string;
};

export type BrowserCreateSourceForm = {
  driver_type: string;
  name: string;
  config: Record<string, unknown>;
  collections: string[];
  file_types?: string[];
  include_patterns?: string[];
  exclude_patterns?: string[];
};

export type BrowserCreatedSource = {
  source_id: string;
  name?: string;
};

interface WorkspaceBrowserCardProps {
  onCreateSource: (data: BrowserCreateSourceForm) => Promise<BrowserCreatedSource | null | void> | BrowserCreatedSource | null | void;
  onStartJob?: (sourceId: string) => Promise<void> | void;
}

function inferCollection(path: string): string {
  const p = path.toLowerCase();
  if (p === 'docs' || p.startsWith('docs/') || p === 'specs' || p.startsWith('specs/') || p === 'inbox' || p.startsWith('inbox/')) return 'devel-docs';
  if (p === 'config' || p.startsWith('config/') || p === 'configs' || p.startsWith('configs/') || p.includes('docker-compose') || p.endsWith('.env')) return 'devel-config';
  if (p === 'data' || p.startsWith('data/') || p === 'datasets' || p.startsWith('datasets/')) return 'devel-data';
  return 'devel-code';
}

function inferFileTypes(path: string): string {
  const collection = inferCollection(path);
  switch (collection) {
    case 'devel-docs':
      return '.md,.txt,.org,.rst,.adoc';
    case 'devel-config':
      return '.json,.jsonc,.yaml,.yml,.toml,.ini,.cfg,.conf,.env,.properties';
    case 'devel-data':
      return '.jsonl,.csv,.tsv,.parquet';
    default:
      return '.clj,.cljs,.cljc,.edn,.ts,.tsx,.js,.jsx,.py,.go,.rs,.java,.kt,.sh,.sql';
  }
}

function folderName(path: string): string {
  if (!path) return 'devel';
  const parts = path.split('/').filter(Boolean);
  return parts[parts.length - 1] || 'devel';
}

export function WorkspaceBrowserCard({ onCreateSource, onStartJob }: WorkspaceBrowserCardProps) {
  const [currentPath, setCurrentPath] = useState('');
  const [browseData, setBrowseData] = useState<BrowseResponse | null>(null);
  const [previewData, setPreviewData] = useState<PreviewResponse | null>(null);
  const [loadingBrowse, setLoadingBrowse] = useState(false);
  const [loadingPreview, setLoadingPreview] = useState(false);
  const [creating, setCreating] = useState(false);
  const [creatingAndStarting, setCreatingAndStarting] = useState(false);
  const [collection, setCollection] = useState('devel-docs');
  const [fileTypes, setFileTypes] = useState('.md,.txt,.org,.rst,.adoc');
  const [excludePatterns, setExcludePatterns] = useState('**/.git/**,**/node_modules/**,**/dist/**,**/coverage/**,**/*.png,**/*.jpg,**/*.jpeg,**/*.gif,**/*.pdf,**/*.zip,**/*.tar.gz');
  const [entryFilter, setEntryFilter] = useState('');
  const [showHidden, setShowHidden] = useState(false);
  const [dirsOnly, setDirsOnly] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const breadcrumbs = useMemo(() => currentPath.split('/').filter(Boolean), [currentPath]);
  const filteredEntries = useMemo(() => {
    let entries = browseData?.entries || [];
    if (!showHidden) {
      entries = entries.filter((entry) => !entry.name.startsWith('.'));
    }
    if (dirsOnly) {
      entries = entries.filter((entry) => entry.type === 'dir');
    }
    const q = entryFilter.trim().toLowerCase();
    if (!q) return entries;
    return entries.filter((entry) =>
      entry.name.toLowerCase().includes(q) || entry.path.toLowerCase().includes(q)
    );
  }, [browseData?.entries, entryFilter, showHidden, dirsOnly]);

  const quickRoots = [
    { label: 'docs', path: 'docs' },
    { label: 'specs/drafts', path: 'specs/drafts' },
    { label: 'packages', path: 'packages' },
    { label: 'services', path: 'services' },
    { label: 'orgs', path: 'orgs' },
  ];

  const loadDirectory = useCallback(async (path = '') => {
    setLoadingBrowse(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (path) params.set('path', path);
      const resp = await fetch(`/api/ingestion/browse?${params.toString()}`);
      if (!resp.ok) throw new Error(`Browse failed: ${resp.status}`);
      const data = (await resp.json()) as BrowseResponse;
      setBrowseData(data);
      setCurrentPath(data.current_path || '');
      const guessedCollection = inferCollection(data.current_path || '');
      setCollection(guessedCollection);
      setFileTypes(inferFileTypes(data.current_path || ''));
      setPreviewData(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoadingBrowse(false);
    }
  }, []);

  async function previewFile(path: string) {
    setLoadingPreview(true);
    setError(null);
    try {
      const params = new URLSearchParams({ path });
      const resp = await fetch(`/api/ingestion/file?${params.toString()}`);
      if (!resp.ok) throw new Error(`Preview failed: ${resp.status}`);
      const data = (await resp.json()) as PreviewResponse;
      setPreviewData(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoadingPreview(false);
    }
  }

  useEffect(() => {
    void loadDirectory('docs');
  }, [loadDirectory]);

  async function createSourceFromCurrentFolder() {
    setCreating(true);
    setError(null);
    try {
      await onCreateSource({
        driver_type: 'local',
        name: `${folderName(currentPath)} → ${collection}`,
        config: { root_path: currentPath ? `/app/workspace/devel/${currentPath}` : '/app/workspace/devel' },
        collections: [collection],
        file_types: fileTypes.split(',').map((v) => v.trim()).filter(Boolean),
        exclude_patterns: excludePatterns.split(',').map((v) => v.trim()).filter(Boolean),
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setCreating(false);
    }
  }

  async function createSourceAndStart() {
    if (!onStartJob) {
      await createSourceFromCurrentFolder();
      return;
    }
    setCreatingAndStarting(true);
    setError(null);
    try {
      const created = await onCreateSource({
        driver_type: 'local',
        name: `${folderName(currentPath)} → ${collection}`,
        config: { root_path: currentPath ? `/app/workspace/devel/${currentPath}` : '/app/workspace/devel' },
        collections: [collection],
        file_types: fileTypes.split(',').map((v) => v.trim()).filter(Boolean),
        exclude_patterns: excludePatterns.split(',').map((v) => v.trim()).filter(Boolean),
      });
      if (created?.source_id) {
        await onStartJob(created.source_id);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setCreatingAndStarting(false);
    }
  }

  const upPath = breadcrumbs.length > 0 ? breadcrumbs.slice(0, -1).join('/') : null;

  return (
    <div className="grid gap-4 xl:grid-cols-[minmax(320px,420px),minmax(0,1fr)]">
      <Card
        variant="elevated"
        title="Workspace Browser"
        extra={
          <div className="flex items-center gap-2 text-xs text-slate-500">
            <span>{currentPath || 'devel/'}</span>
          </div>
        }
        footer={
          <div className="flex w-full items-center gap-2">
            <Button variant="secondary" size="sm" disabled={!upPath && currentPath === ''} onClick={() => loadDirectory(upPath ?? '')}>
              Up
            </Button>
            <Button variant="primary" size="sm" loading={creating} onClick={createSourceFromCurrentFolder}>
              Create Source Here
            </Button>
            <Button variant="secondary" size="sm" loading={creatingAndStarting} onClick={createSourceAndStart}>
              Create + Start Sync
            </Button>
          </div>
        }
      >
        <div className="space-y-3">
          <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500">
            <span className="font-medium text-slate-700 dark:text-slate-200">Path:</span>
            <button type="button" className="underline-offset-2 hover:underline" onClick={() => loadDirectory('')}>devel</button>
            {breadcrumbs.map((crumb, idx) => {
              const path = breadcrumbs.slice(0, idx + 1).join('/');
              return (
                <span key={path} className="flex items-center gap-2">
                  <span>/</span>
                  <button type="button" className="underline-offset-2 hover:underline" onClick={() => loadDirectory(path)}>
                    {crumb}
                  </button>
                </span>
              );
            })}
          </div>

          <div className="grid gap-3">
            <Input value={entryFilter} onChange={(e: ChangeEvent<HTMLInputElement>) => setEntryFilter(e.target.value)} placeholder="Filter entries..." />
            <Input value={collection} onChange={(e: ChangeEvent<HTMLInputElement>) => setCollection(e.target.value)} placeholder="Collection (e.g. devel-docs)" />
            <Input value={fileTypes} onChange={(e: ChangeEvent<HTMLInputElement>) => setFileTypes(e.target.value)} placeholder=".md,.txt,.clj" />
            <Input value={excludePatterns} onChange={(e: ChangeEvent<HTMLInputElement>) => setExcludePatterns(e.target.value)} placeholder="Exclude globs" />
          </div>

          <div className="flex flex-wrap gap-2">
            {quickRoots.map((root) => (
              <Button key={root.path} size="sm" variant="ghost" onClick={() => loadDirectory(root.path)}>
                {root.label}
              </Button>
            ))}
            <Button size="sm" variant={showHidden ? 'secondary' : 'ghost'} onClick={() => setShowHidden((v) => !v)}>
              {showHidden ? 'Hide hidden' : 'Show hidden'}
            </Button>
            <Button size="sm" variant={dirsOnly ? 'secondary' : 'ghost'} onClick={() => setDirsOnly((v) => !v)}>
              {dirsOnly ? 'Show files' : 'Dirs only'}
            </Button>
          </div>

          {error ? (
            <div className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          ) : null}

          <div className="max-h-[520px] overflow-auto rounded-md border border-slate-200 dark:border-slate-700">
            {loadingBrowse && !browseData ? (
              <div className="flex items-center justify-center p-8">
                <Spinner label="Loading files..." />
              </div>
            ) : (
              <div className="divide-y divide-slate-100 dark:divide-slate-800">
                <div className="px-3 py-2 text-xs text-slate-500">
                  {filteredEntries.length} item(s)
                </div>
                {filteredEntries.map((entry) => (
                  <button
                    key={`${entry.type}:${entry.path}`}
                    type="button"
                    onClick={() => (entry.type === 'dir' ? loadDirectory(entry.path) : entry.previewable ? previewFile(entry.path) : undefined)}
                    className="flex w-full items-center justify-between gap-3 px-3 py-2 text-left hover:bg-slate-50 dark:hover:bg-slate-900/40"
                  >
                    <div className="min-w-0">
                      <div className="truncate text-sm font-medium text-slate-800 dark:text-slate-100">
                        {entry.type === 'dir' ? '📁' : '📄'} {entry.name}
                      </div>
                      <div className="truncate text-xs text-slate-500">
                        {entry.path}
                      </div>
                    </div>
                    <div className="shrink-0 text-xs text-slate-500">
                      {entry.type === 'file' ? `${Math.max(1, Math.round((entry.size || 0) / 1024))} KB` : 'dir'}
                    </div>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      </Card>

      <Card variant="default" title="File Preview">
        {loadingPreview ? (
          <div className="flex items-center justify-center p-8">
            <Spinner label="Loading preview..." />
          </div>
        ) : previewData ? (
          <div className="space-y-3">
            <div className="text-xs text-slate-500">
              <div>{previewData.path}</div>
              <div>{Math.max(1, Math.round(previewData.size / 1024))} KB{previewData.truncated ? ' • truncated' : ''}</div>
            </div>
            <pre className="max-h-[620px] overflow-auto rounded-md bg-slate-950 px-4 py-3 text-xs leading-6 text-slate-100">
              {previewData.content}
            </pre>
          </div>
        ) : (
          <div className="p-8 text-sm text-slate-500">
            Select a previewable file to inspect its contents before creating an ingestion source.
          </div>
        )}
      </Card>
    </div>
  );
}

export default WorkspaceBrowserCard;
