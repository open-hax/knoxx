import { useEffect, useState } from "react";
import { Button, Card, Input } from '@open-hax/uxx';

type GardenTheme = 'monokai' | 'night-owl' | 'proxy-console';

type Garden = {
  garden_id: string;
  title: string;
  description: string;
  domain: string;
  status: string;
  theme?: GardenTheme;
  nav?: {
    items: Array<{
      label: string;
      path: string;
      children?: Array<{ label: string; path: string }>;
    }>;
  };
  source_filter?: { projects?: string[] };
  target_languages?: string[];
  auto_translate?: boolean;
};

type GardensResponse = {
  ok: boolean;
  count: number;
  gardens: Garden[];
};

const THEMES: Array<{ value: GardenTheme; label: string; colors: { bg: string; text: string; accent: string } }> = [
  { 
    value: 'monokai', 
    label: 'Monokai', 
    colors: { bg: '#272822', text: '#f8f8f2', accent: '#a6e22e' } 
  },
  { 
    value: 'night-owl', 
    label: 'Night Owl', 
    colors: { bg: '#011627', text: '#d6deeb', accent: '#82aaff' } 
  },
  { 
    value: 'proxy-console', 
    label: 'Proxy Console', 
    colors: { bg: '#0a0a0a', text: '#e0e0e0', accent: '#00d4ff' } 
  },
];

const gardenLinks: Record<string, string> = {
  "devel-deps-garden": "http://127.0.0.1:8798/",
  "truth-workbench": "http://127.0.0.1:8790/workbench",
  query: "/query",
  ingestion: "/ingestion",
};

function ThemeBadge({ theme }: { theme?: GardenTheme }) {
  const themeInfo = THEMES.find(t => t.value === theme);
  if (!themeInfo) return null;
  
  return (
    <span 
      className="rounded px-2 py-0.5 text-xs font-medium"
      style={{ 
        backgroundColor: themeInfo.colors.bg, 
        color: themeInfo.colors.accent,
        border: `1px solid ${themeInfo.colors.accent}40`
      }}
    >
      {themeInfo.label}
    </span>
  );
}

function ThemePreview({ theme }: { theme: GardenTheme }) {
  const themeInfo = THEMES.find(t => t.value === theme);
  if (!themeInfo) return null;
  
  return (
    <div 
      className="rounded-lg p-4 text-sm"
      style={{ 
        backgroundColor: themeInfo.colors.bg, 
        color: themeInfo.colors.text,
        fontFamily: 'monospace'
      }}
    >
      <div style={{ color: themeInfo.colors.accent }}># Preview</div>
      <p className="mt-2 opacity-80">
        This is how your garden will look with the {themeInfo.label} theme.
      </p>
      <pre className="mt-2 rounded p-2 opacity-90" style={{ background: 'rgba(255,255,255,0.05)' }}>
{`const greeting = "Hello";
console.log(greeting);`}
      </pre>
    </div>
  );
}

export default function GardensPage() {
  const [data, setData] = useState<GardensResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [editingGarden, setEditingGarden] = useState<Garden | null>(null);
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  
  // Form state
  const [formGardenId, setFormGardenId] = useState('');
  const [formTitle, setFormTitle] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formTheme, setFormTheme] = useState<GardenTheme>('monokai');
  const [formDomain, setFormDomain] = useState('general');
  const [formStatus, setFormStatus] = useState('active');

  useEffect(() => {
    loadGardens();
  }, []);

  async function loadGardens() {
    setLoading(true);
    setError(null);
    try {
      const resp = await fetch("/api/openplanner/v1/gardens");
      if (!resp.ok) throw new Error(`Failed to load gardens: ${resp.status}`);
      const body = await resp.json() as GardensResponse;
      setData(body);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }

  function resetForm() {
    setFormGardenId('');
    setFormTitle('');
    setFormDescription('');
    setFormTheme('monokai');
    setFormDomain('general');
    setFormStatus('active');
    setEditingGarden(null);
    setShowCreateForm(false);
  }

  function startEdit(garden: Garden) {
    setEditingGarden(garden);
    setFormGardenId(garden.garden_id);
    setFormTitle(garden.title);
    setFormDescription(garden.description);
    setFormTheme(garden.theme || 'monokai');
    setFormDomain(garden.domain);
    setFormStatus(garden.status);
    setShowCreateForm(true);
  }

  async function handleSave() {
    if (!formGardenId.trim() || !formTitle.trim()) {
      setError('Garden ID and title are required');
      return;
    }

    setSaving(true);
    setError(null);
    setNotice(null);

    try {
      const body = {
        title: formTitle,
        description: formDescription,
        theme: formTheme,
        domain: formDomain,
        status: formStatus,
      };

      const resp = await fetch(`/api/openplanner/v1/gardens/${encodeURIComponent(formGardenId)}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });

      if (!resp.ok) {
        const text = await resp.text();
        throw new Error(text || `Failed to save garden: ${resp.status}`);
      }

      setNotice(`Garden "${formTitle}" saved successfully`);
      resetForm();
      await loadGardens();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(gardenId: string) {
    if (!confirm(`Are you sure you want to delete garden "${gardenId}"?`)) {
      return;
    }

    setSaving(true);
    setError(null);

    try {
      const resp = await fetch(`/api/openplanner/v1/gardens/${encodeURIComponent(gardenId)}`, {
        method: 'DELETE',
      });

      if (!resp.ok) {
        throw new Error(`Failed to delete garden: ${resp.status}`);
      }

      setNotice(`Garden "${gardenId}" deleted`);
      await loadGardens();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold">Gardens</h1>
          <p className="mt-1 text-sm text-slate-500">
            Publishable operator views with themed markdown rendering.
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => {
            resetForm();
            setShowCreateForm(true);
          }}
        >
          + New Garden
        </Button>
      </div>

      {notice ? (
        <div className="rounded-lg border border-green-200 bg-green-50 p-4 text-sm text-green-700 dark:border-green-900/40 dark:bg-green-950/30 dark:text-green-300">
          {notice}
        </div>
      ) : null}

      {loading ? (
        <Card variant="default" padding="md">
          <div className="text-sm text-slate-500">Loading gardens...</div>
        </Card>
      ) : null}

      {error ? (
        <div className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700 dark:border-red-900/40 dark:bg-red-950/30 dark:text-red-300">
          {error}
        </div>
      ) : null}

      {showCreateForm ? (
        <Card variant="elevated" padding="lg">
          <div className="flex items-start justify-between mb-4">
            <h2 className="text-lg font-semibold">
              {editingGarden ? 'Edit Garden' : 'Create Garden'}
            </h2>
            <Button variant="ghost" size="sm" onClick={resetForm}>✕</Button>
          </div>

          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="mb-1 block text-sm font-medium">Garden ID</label>
              <Input
                value={formGardenId}
                onChange={(e) => setFormGardenId(e.target.value)}
                placeholder="my-garden-id"
                disabled={!!editingGarden}
              />
              <p className="mt-1 text-xs text-slate-500">
                Unique identifier (slug format, cannot be changed after creation)
              </p>
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium">Title</label>
              <Input
                value={formTitle}
                onChange={(e) => setFormTitle(e.target.value)}
                placeholder="My Garden"
              />
            </div>

            <div className="md:col-span-2">
              <label className="mb-1 block text-sm font-medium">Description</label>
              <textarea
                value={formDescription}
                onChange={(e) => setFormDescription(e.target.value)}
                placeholder="Brief description of this garden"
                rows={2}
                className="w-full rounded-lg border border-slate-300 p-2 text-sm dark:border-slate-600 dark:bg-slate-800"
              />
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium">Theme</label>
              <select
                value={formTheme}
                onChange={(e) => setFormTheme(e.target.value as GardenTheme)}
                className="w-full rounded-lg border border-slate-300 p-2 text-sm dark:border-slate-600 dark:bg-slate-800"
              >
                {THEMES.map(theme => (
                  <option key={theme.value} value={theme.value}>{theme.label}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium">Status</label>
              <select
                value={formStatus}
                onChange={(e) => setFormStatus(e.target.value)}
                className="w-full rounded-lg border border-slate-300 p-2 text-sm dark:border-slate-600 dark:bg-slate-800"
              >
                <option value="active">Active</option>
                <option value="inactive">Inactive</option>
              </select>
            </div>

            <div>
              <label className="mb-1 block text-sm font-medium">Domain</label>
              <Input
                value={formDomain}
                onChange={(e) => setFormDomain(e.target.value)}
                placeholder="general"
              />
            </div>

            <div className="md:col-span-2">
              <label className="mb-1 block text-sm font-medium">Theme Preview</label>
              <ThemePreview theme={formTheme} />
            </div>
          </div>

          <div className="mt-6 flex gap-2">
            <Button variant="primary" onClick={handleSave} loading={saving}>
              {editingGarden ? 'Save Changes' : 'Create Garden'}
            </Button>
            <Button variant="ghost" onClick={resetForm}>
              Cancel
            </Button>
          </div>
        </Card>
      ) : null}

      <div className="grid gap-4 md:grid-cols-2">
        {data?.gardens?.map((garden) => {
          const gardenHtmlUrl = `/api/openplanner/v1/public/gardens/${garden.garden_id}/html`;
          
          return (
            <Card
              key={garden.garden_id}
              variant="elevated"
              padding="md"
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="text-lg font-semibold">{garden.title}</h2>
                  <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
                    {garden.description}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <ThemeBadge theme={garden.theme} />
                  <span className="rounded-full bg-slate-100 px-2 py-1 text-xs font-medium text-slate-700 dark:bg-slate-700 dark:text-slate-200">
                    {garden.garden_id}
                  </span>
                </div>
              </div>

              <div className="mt-4 flex items-center gap-4 text-sm">
                <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                  garden.status === 'active' 
                    ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300' 
                    : 'bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-300'
                }`}>
                  {garden.status}
                </span>
                <span className="text-slate-600 dark:text-slate-400">
                  Domain: {garden.domain}
                </span>
              </div>

              <div className="mt-5 flex gap-2">
                <Button
                  onClick={() => window.open(gardenHtmlUrl, '_blank')}
                  variant="primary"
                  size="sm"
                >
                  View Garden
                </Button>
                <Button
                  onClick={() => startEdit(garden)}
                  variant="secondary"
                  size="sm"
                >
                  Edit
                </Button>
                <Button
                  onClick={() => handleDelete(garden.garden_id)}
                  variant="ghost"
                  size="sm"
                >
                  Delete
                </Button>
              </div>
            </Card>
          );
        })}
      </div>

      {data?.gardens?.length === 0 && !loading ? (
        <Card variant="default" padding="lg" className="text-center">
          <p className="text-slate-500">No gardens yet.</p>
          <p className="mt-1 text-sm text-slate-400">
            Create a garden to start publishing markdown documents with themes.
          </p>
        </Card>
      ) : null}
    </div>
  );
}
