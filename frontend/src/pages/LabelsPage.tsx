import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { opsRoutes } from '../lib/app-routes';

interface LabelNode {
  label_id: string;
  label: string;
  emoji: string | null;
  description: string;
  color: string | null;
  tenant_id: string;
  project: string | null;
  created_by: string | null;
  createdAt: string;
  updatedAt: string;
}

interface LabeledNode {
  node_id: string;
  event: any;
}

async function request<T>(path: string, opts?: RequestInit): Promise<T> {
  const res = await fetch(path, opts);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

export default function LabelsPage() {
  const [labels, setLabels] = useState<LabelNode[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(false);
  const [selectedLabel, setSelectedLabel] = useState<LabelNode | null>(null);
  const [labeledNodes, setLabeledNodes] = useState<LabeledNode[]>([]);
  const [showCreate, setShowCreate] = useState(false);
  const [createForm, setCreateForm] = useState({ label: '', slug: '', description: '', emoji: '', color: '#3b82f6' });
  const [editForm, setEditForm] = useState<Partial<LabelNode>>({});
  const [applyNodeId, setApplyNodeId] = useState('');

  const loadLabels = useCallback(async () => {
    setLoading(true);
    try {
      const res = await request<{ ok: boolean; labels: LabelNode[] }>(`/api/openplanner/v1/graph/labels?search=${encodeURIComponent(search)}&limit=200`);
      setLabels(res.labels ?? []);
    } catch (e) {
      console.error('Failed to load labels', e);
    } finally {
      setLoading(false);
    }
  }, [search]);

  const loadLabelDetail = useCallback(async (labelId: string) => {
    try {
      const [labelRes, nodesRes] = await Promise.all([
        request<{ ok: boolean; label: LabelNode }>(`/api/openplanner/v1/graph/labels/${encodeURIComponent(labelId)}`),
        request<{ ok: boolean; nodes: LabeledNode[] }>(`/api/openplanner/v1/graph/labels/${encodeURIComponent(labelId)}/nodes?limit=100`),
      ]);
      setSelectedLabel(labelRes.label);
      setLabeledNodes(nodesRes.nodes ?? []);
      setEditForm(labelRes.label);
    } catch (e) {
      console.error('Failed to load label detail', e);
    }
  }, []);

  useEffect(() => {
    void loadLabels();
  }, [loadLabels]);

  const handleCreate = async () => {
    try {
      await request('/api/openplanner/v1/graph/labels', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          label: createForm.label,
          slug: createForm.slug || createForm.label,
          description: createForm.description,
          emoji: createForm.emoji || undefined,
          color: createForm.color || undefined,
        }),
      });
      setShowCreate(false);
      setCreateForm({ label: '', slug: '', description: '', emoji: '', color: '#3b82f6' });
      await loadLabels();
    } catch (e) {
      alert('Failed to create label: ' + (e as Error).message);
    }
  };

  const handleUpdate = async () => {
    if (!selectedLabel) return;
    try {
      await request(`/api/openplanner/v1/graph/labels/${encodeURIComponent(selectedLabel.label_id)}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          label: editForm.label,
          description: editForm.description,
          emoji: editForm.emoji,
          color: editForm.color,
        }),
      });
      await loadLabelDetail(selectedLabel.label_id);
      await loadLabels();
    } catch (e) {
      alert('Failed to update label: ' + (e as Error).message);
    }
  };

  const handleDelete = async () => {
    if (!selectedLabel) return;
    if (!confirm(`Delete label "${selectedLabel.label}"? This will remove all label assignments.`)) return;
    try {
      await request(`/api/openplanner/v1/graph/labels/${encodeURIComponent(selectedLabel.label_id)}`, { method: 'DELETE' });
      setSelectedLabel(null);
      await loadLabels();
    } catch (e) {
      alert('Failed to delete label: ' + (e as Error).message);
    }
  };

  const handleApply = async () => {
    if (!selectedLabel || !applyNodeId.trim()) return;
    try {
      await request(`/api/openplanner/v1/graph/labels/${encodeURIComponent(selectedLabel.label_id)}/apply`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ node_id: applyNodeId.trim() }),
      });
      setApplyNodeId('');
      await loadLabelDetail(selectedLabel.label_id);
    } catch (e) {
      alert('Failed to apply label: ' + (e as Error).message);
    }
  };

  const handleRemove = async (nodeId: string) => {
    if (!selectedLabel) return;
    try {
      await request(`/api/openplanner/v1/graph/labels/${encodeURIComponent(selectedLabel.label_id)}/remove`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ node_id: nodeId }),
      });
      await loadLabelDetail(selectedLabel.label_id);
    } catch (e) {
      alert('Failed to remove label: ' + (e as Error).message);
    }
  };

  return (
    <div className="h-full flex flex-col">
      <div className="flex items-center justify-between px-6 py-4 border-b border-slate-200 dark:border-slate-700">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-slate-100">Labels</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">Manage categorical label nodes and their graph connections</p>
        </div>
        <button
          onClick={() => setShowCreate(!showCreate)}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium transition-colors"
        >
          {showCreate ? 'Cancel' : 'Create Label'}
        </button>
      </div>

      <div className="flex flex-1 overflow-hidden">
        {/* Label List */}
        <div className="w-80 border-r border-slate-200 dark:border-slate-700 flex flex-col">
          <div className="p-4 border-b border-slate-200 dark:border-slate-700">
            <input
              type="text"
              placeholder="Search labels..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          <div className="flex-1 overflow-y-auto">
            {loading ? (
              <div className="p-4 text-center text-slate-500">Loading...</div>
            ) : labels.length === 0 ? (
              <div className="p-4 text-center text-slate-500">No labels found</div>
            ) : (
              <ul className="divide-y divide-slate-100 dark:divide-slate-700">
                {labels.map((label) => (
                  <li
                    key={label.label_id}
                    onClick={() => loadLabelDetail(label.label_id)}
                    className={`p-4 cursor-pointer hover:bg-slate-50 dark:hover:bg-slate-700/50 transition-colors ${
                      selectedLabel?.label_id === label.label_id ? 'bg-blue-50 dark:bg-blue-500/10' : ''
                    }`}
                  >
                    <div className="flex items-center gap-3">
                      <span className="text-2xl">{label.emoji || '🏷️'}</span>
                      <div className="flex-1 min-w-0">
                        <div className="font-medium text-slate-900 dark:text-slate-100 truncate">{label.label}</div>
                        <div className="text-xs text-slate-500 dark:text-slate-400 truncate">{label.description || 'No description'}</div>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>

        {/* Detail Panel */}
        <div className="flex-1 overflow-y-auto p-6">
          {showCreate ? (
            <div className="max-w-lg">
              <h2 className="text-lg font-semibold mb-4">Create New Label</h2>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Label Name</label>
                  <input
                    type="text"
                    value={createForm.label}
                    onChange={(e) => setCreateForm({ ...createForm, label: e.target.value })}
                    className="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100"
                    placeholder="e.g., Good Output"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Slug (optional)</label>
                  <input
                    type="text"
                    value={createForm.slug}
                    onChange={(e) => setCreateForm({ ...createForm, slug: e.target.value })}
                    className="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100"
                    placeholder="good-output"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Description</label>
                  <textarea
                    value={createForm.description}
                    onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
                    className="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100"
                    rows={3}
                    placeholder="Describe what this label means..."
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Emoji</label>
                    <input
                      type="text"
                      value={createForm.emoji}
                      onChange={(e) => setCreateForm({ ...createForm, emoji: e.target.value })}
                      className="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100"
                      placeholder="✅"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Color</label>
                    <input
                      type="color"
                      value={createForm.color}
                      onChange={(e) => setCreateForm({ ...createForm, color: e.target.value })}
                      className="w-full h-10 rounded-lg border border-slate-300 dark:border-slate-600"
                    />
                  </div>
                </div>
                <button
                  onClick={handleCreate}
                  disabled={!createForm.label.trim()}
                  className="w-full px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-slate-400 text-white rounded-lg font-medium transition-colors"
                >
                  Create Label
                </button>
              </div>
            </div>
          ) : selectedLabel ? (
            <div>
              <div className="flex items-start justify-between mb-6">
                <div className="flex items-center gap-4">
                  <span className="text-4xl">{selectedLabel.emoji || '🏷️'}</span>
                  <div>
                    <h2 className="text-2xl font-bold">{selectedLabel.label}</h2>
                    <code className="text-xs text-slate-500">{selectedLabel.label_id}</code>
                  </div>
                </div>
                <button
                  onClick={handleDelete}
                  className="px-3 py-1.5 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-lg text-sm font-medium transition-colors"
                >
                  Delete
                </button>
              </div>

              <div className="space-y-6">
                {/* Edit Form */}
                <div className="bg-white dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700 p-4">
                  <h3 className="font-semibold mb-3">Edit Label</h3>
                  <div className="grid grid-cols-2 gap-4 mb-4">
                    <div>
                      <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Name</label>
                      <input
                        type="text"
                        value={editForm.label ?? ''}
                        onChange={(e) => setEditForm({ ...editForm, label: e.target.value })}
                        className="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Emoji</label>
                      <input
                        type="text"
                        value={editForm.emoji ?? ''}
                        onChange={(e) => setEditForm({ ...editForm, emoji: e.target.value })}
                        className="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100"
                      />
                    </div>
                  </div>
                  <div className="mb-4">
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Description</label>
                    <textarea
                      value={editForm.description ?? ''}
                      onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
                      className="w-full px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100"
                      rows={3}
                    />
                  </div>
                  <div className="flex items-center gap-4">
                    <div className="flex-1">
                      <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">Color</label>
                      <input
                        type="color"
                        value={editForm.color ?? '#3b82f6'}
                        onChange={(e) => setEditForm({ ...editForm, color: e.target.value })}
                        className="w-full h-10 rounded-lg border border-slate-300 dark:border-slate-600"
                      />
                    </div>
                    <button
                      onClick={handleUpdate}
                      className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium transition-colors mt-6"
                    >
                      Save Changes
                    </button>
                  </div>
                </div>

                {/* Apply Label */}
                <div className="bg-white dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700 p-4">
                  <h3 className="font-semibold mb-3">Apply to Node</h3>
                  <div className="flex gap-2">
                    <input
                      type="text"
                      value={applyNodeId}
                      onChange={(e) => setApplyNodeId(e.target.value)}
                      placeholder="node_id or event_id"
                      className="flex-1 px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 text-slate-900 dark:text-slate-100"
                    />
                    <button
                      onClick={handleApply}
                      disabled={!applyNodeId.trim()}
                      className="px-4 py-2 bg-green-600 hover:bg-green-700 disabled:bg-slate-400 text-white rounded-lg font-medium transition-colors"
                    >
                      Apply
                    </button>
                  </div>
                </div>

                {/* Labeled Nodes */}
                <div className="bg-white dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700 p-4">
                  <h3 className="font-semibold mb-3">Labeled Nodes ({labeledNodes.length})</h3>
                  {labeledNodes.length === 0 ? (
                    <p className="text-slate-500 text-sm">No nodes have this label yet</p>
                  ) : (
                    <ul className="space-y-2">
                      {labeledNodes.map((node) => (
                        <li key={node.node_id} className="flex items-center justify-between p-2 bg-slate-50 dark:bg-slate-700/50 rounded-lg">
                          <div className="min-w-0">
                            <code className="text-xs text-slate-600 dark:text-slate-400">{node.node_id}</code>
                            {node.event && (
                              <p className="text-sm text-slate-700 dark:text-slate-300 truncate">{node.event.message || node.event.text?.slice(0, 100)}</p>
                            )}
                          </div>
                          <button
                            onClick={() => handleRemove(node.node_id)}
                            className="px-2 py-1 text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded text-sm"
                          >
                            Remove
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>
            </div>
          ) : (
            <div className="flex items-center justify-center h-full text-slate-500">
              Select a label to view details
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
