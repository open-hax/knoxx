import React, { useEffect, useMemo, useState } from 'react';
import { getModelLabStatus, getSettings, listModels, updateSettings } from '../lib/nextApi';

type ModelOption = { id?: string; name?: string };

export default function SettingsPage() {
  const [settings, setSettings] = useState<any>(null);
  const [status, setStatus] = useState<any>(null);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [modelOptions, setModelOptions] = useState<ModelOption[]>([]);
  const [selectedLlmModel, setSelectedLlmModel] = useState('');
  const [selectedEmbedModel, setSelectedEmbedModel] = useState('');

  useEffect(() => {
    void loadSettings();
  }, []);

  const loadSettings = async () => {
    try {
      const [data, st] = await Promise.all([getSettings(), getModelLabStatus()]);
      setSettings(data);
      setStatus(st);
      try {
        const models = await listModels();
        setModelOptions(models || []);
      } catch {
        setModelOptions([]);
      }
      setSelectedLlmModel(data.llmModel || '');
      setSelectedEmbedModel(data.embedModel || '');
    } catch (error) {
      console.error(error);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setMessage('');
    try {
      await updateSettings(settings);
      setMessage('Settings saved successfully. Restart Knoxx server to apply changes.');
    } catch {
      setMessage('Error saving settings.');
    } finally {
      setSaving(false);
    }
  };

  const refreshModels = async () => {
    try {
      const models = await listModels();
      setModelOptions(models || []);
    } catch (error) {
      console.error('Failed to refresh model list', error);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    const { name, value, type } = e.target;
    const val = type === 'checkbox' ? (e.target as HTMLInputElement).checked : value;
    setSettings((s: any) => ({ ...s, [name]: val }));
  };

  const handleModelSelection = (field: 'llmModel' | 'embedModel', value: string) => {
    const next = value === '__custom__' ? '' : value;
    if (field === 'llmModel') {
      setSelectedLlmModel(value);
    } else {
      setSelectedEmbedModel(value);
    }
    setSettings((s: any) => ({ ...s, [field]: next }));
  };

  const llmModelChoices = useMemo(() => {
    const names = modelOptions
      .map((m) => m.id || m.name || '')
      .filter(Boolean);
    if (settings?.llmModel && !names.includes(settings.llmModel)) {
      names.unshift(settings.llmModel);
    }
    return Array.from(new Set(names));
  }, [modelOptions, settings?.llmModel]);

  const embedModelChoices = useMemo(() => {
    const names = modelOptions
      .map((m) => m.id || m.name || '')
      .filter(Boolean);
    if (settings?.embedModel && !names.includes(settings.embedModel)) {
      names.unshift(settings.embedModel);
    }
    return Array.from(new Set(names));
  }, [modelOptions, settings?.embedModel]);

  if (!settings) return <div className="p-6 text-slate-300">Loading settings...</div>;

  return (
    <div className="p-6 max-w-5xl mx-auto overflow-y-auto text-slate-100">
      <h1 className="text-2xl font-bold mb-6">Settings / Admin</h1>

      {message ? (
        <div className={`p-4 mb-6 rounded ${message.includes('Error') ? 'bg-rose-500/20 text-rose-200 border border-rose-500/30' : 'bg-emerald-500/20 text-emerald-200 border border-emerald-500/30'}`}>
          {message}
        </div>
      ) : null}

      {status ? (
        <div className="bg-slate-900 border border-slate-700 p-4 rounded shadow mb-6">
          <h2 className="text-lg font-semibold border-b pb-2 mb-4">Model Lab Integration Diagnostics</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
            <div><strong>Using Model Lab:</strong> {status.usingModelLab ? 'Yes' : 'No'}</div>
            <div><strong>Models Reachable:</strong> {status.modelsReachable ? 'Yes' : 'No'}</div>
            <div><strong>Embeddings Reachable:</strong> {status.embedReachable ? 'Yes' : 'No'}</div>
            <div><strong>Models Count:</strong> {status.modelsCount}</div>
          </div>
        </div>
      ) : null}

      <form onSubmit={handleSave} className="space-y-6">
        <div className="bg-slate-900 border border-slate-700 p-4 rounded shadow space-y-4">
          <div className="flex items-center justify-between border-b pb-2">
            <h2 className="text-lg font-semibold">Models and Limits</h2>
            <button type="button" onClick={refreshModels} className="px-3 py-1.5 rounded bg-slate-700 text-xs hover:bg-slate-600">
              Refresh Models
            </button>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1">LLM Model</label>
              <select
                className="field-input"
                value={selectedLlmModel || '__custom__'}
                onChange={(e) => handleModelSelection('llmModel', e.target.value)}
              >
                {llmModelChoices.map((m) => (
                  <option key={`llm-${m}`} value={m}>{m}</option>
                ))}
                <option value="__custom__">Custom model id...</option>
              </select>
              {selectedLlmModel === '__custom__' ? (
                <input
                  type="text"
                  name="llmModel"
                  value={settings.llmModel || ''}
                  onChange={handleChange}
                  className="field-input mt-2"
                  placeholder="Enter model id"
                />
              ) : null}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Embedding Model</label>
              <select
                className="field-input"
                value={selectedEmbedModel || '__custom__'}
                onChange={(e) => handleModelSelection('embedModel', e.target.value)}
              >
                {embedModelChoices.map((m) => (
                  <option key={`embed-${m}`} value={m}>{m}</option>
                ))}
                <option value="__custom__">Custom model id...</option>
              </select>
              {selectedEmbedModel === '__custom__' ? (
                <input
                  type="text"
                  name="embedModel"
                  value={settings.embedModel || ''}
                  onChange={handleChange}
                  className="field-input mt-2"
                  placeholder="Enter embedding model id"
                />
              ) : null}
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Max Context Tokens</label>
              <input type="number" name="maxContextTokens" value={settings.maxContextTokens ?? 4000} onChange={handleChange} className="field-input" min={256} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Max Response Length (tokens)</label>
              <input type="number" name="llmMaxTokens" value={settings.llmMaxTokens ?? 4096} onChange={handleChange} className="field-input" min={64} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">LLM Base URL</label>
              <input type="text" name="llmBaseUrl" value={settings.llmBaseUrl || ''} onChange={handleChange} className="field-input" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Embedding Base URL</label>
              <input type="text" name="embedBaseUrl" value={settings.embedBaseUrl || ''} onChange={handleChange} className="field-input" />
            </div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-700 p-4 rounded shadow space-y-4">
          <h2 className="text-lg font-semibold border-b pb-2">RAG and Retrieval</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1">Retrieval Top K</label>
              <input type="number" name="retrievalTopK" value={settings.retrievalTopK ?? 6} onChange={handleChange} className="field-input" min={1} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Retrieval Mode</label>
              <select name="retrievalMode" value={settings.retrievalMode || 'hybrid'} onChange={handleChange} className="field-input">
                <option value="dense">Dense</option>
                <option value="hybrid">Hybrid</option>
                <option value="hybrid_rerank">Hybrid + Rerank</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Vector Dim</label>
              <input type="number" name="vectorDim" value={settings.vectorDim ?? 1024} onChange={handleChange} className="field-input" min={1} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Chunk Target Tokens</label>
              <input type="number" name="chunkTargetTokens" value={settings.chunkTargetTokens ?? 500} onChange={handleChange} className="field-input" min={64} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Chunk Max Tokens</label>
              <input type="number" name="chunkMaxTokens" value={settings.chunkMaxTokens ?? 700} onChange={handleChange} className="field-input" min={64} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Chunk Overlap Tokens</label>
              <input type="number" name="chunkOverlapTokens" value={settings.chunkOverlapTokens ?? 75} onChange={handleChange} className="field-input" min={0} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Embedding Threads</label>
              <input type="number" name="embeddingThreads" value={settings.embeddingThreads ?? 4} onChange={handleChange} className="field-input" min={1} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Upsert Threads</label>
              <input type="number" name="upsertThreads" value={settings.upsertThreads ?? 2} onChange={handleChange} className="field-input" min={1} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Hybrid Final Top K</label>
              <input type="number" name="hybridTopKFinal" value={settings.hybridTopKFinal ?? 12} onChange={handleChange} className="field-input" min={1} />
            </div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-700 p-4 rounded shadow space-y-4">
          <h2 className="text-lg font-semibold border-b pb-2">Project and Security</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium mb-1">Project Name</label>
              <input type="text" name="projectName" value={settings.projectName || ''} onChange={handleChange} className="field-input" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Qdrant Collection</label>
              <input type="text" name="qdrantCollection" value={settings.qdrantCollection || ''} onChange={handleChange} className="field-input" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Docs Path</label>
              <input type="text" name="docsPath" value={settings.docsPath || ''} onChange={handleChange} className="field-input" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Docs Extensions</label>
              <input type="text" name="docsExtensions" value={settings.docsExtensions || ''} onChange={handleChange} className="field-input" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">LLM API Key</label>
              <input type="password" name="llmApiKey" value={settings.llmApiKey || ''} onChange={handleChange} className="field-input" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Embedding API Key</label>
              <input type="password" name="embedApiKey" value={settings.embedApiKey || ''} onChange={handleChange} className="field-input" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Knoxx API Key</label>
              <input type="password" name="apiKey" value={settings.apiKey || ''} onChange={handleChange} className="field-input" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Admin Token</label>
              <input type="password" name="adminToken" value={settings.adminToken || ''} onChange={handleChange} className="field-input" />
            </div>
            <div className="md:col-span-2">
              <label className="block text-sm font-medium mb-1">Custom System Prompt</label>
              <textarea name="customSystemPrompt" value={settings.customSystemPrompt || ''} onChange={handleChange} className="field-input min-h-28" />
            </div>
          </div>
        </div>

        <div className="bg-slate-900 border border-slate-700 p-4 rounded shadow space-y-4">
          <h2 className="text-lg font-semibold border-b pb-2">Forum Mode Controls</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <label className="flex items-center space-x-2 md:col-span-3">
              <input type="checkbox" id="forumMode" name="forumMode" checked={Boolean(settings.forumMode)} onChange={handleChange} className="h-4 w-4" />
              <span className="text-sm font-medium">Enable Forum Mode</span>
            </label>
            <div>
              <label className="block text-sm font-medium mb-1">Forum Max Tokens</label>
              <input type="number" name="forumMaxTokens" value={settings.forumMaxTokens ?? 800} onChange={handleChange} className="field-input" min={64} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Forum Embedding Threads</label>
              <input type="number" name="forumEmbeddingThreads" value={settings.forumEmbeddingThreads ?? 6} onChange={handleChange} className="field-input" min={1} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Forum Upsert Threads</label>
              <input type="number" name="forumUpsertThreads" value={settings.forumUpsertThreads ?? 4} onChange={handleChange} className="field-input" min={1} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Forum Max Posts Per Thread</label>
              <input type="number" name="forumMaxPostsPerThread" value={settings.forumMaxPostsPerThread ?? 10} onChange={handleChange} className="field-input" min={1} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Forum Retrieval Count</label>
              <input type="number" name="forumRetrievalCount" value={settings.forumRetrievalCount ?? 30} onChange={handleChange} className="field-input" min={1} />
            </div>
            <label className="flex items-center space-x-2">
              <input type="checkbox" name="forumSkipUnchanged" checked={Boolean(settings.forumSkipUnchanged)} onChange={handleChange} className="h-4 w-4" />
              <span className="text-sm">Skip unchanged posts</span>
            </label>
          </div>
        </div>

        <div className="flex justify-end">
          <button type="submit" disabled={saving} className="bg-blue-600 text-white px-6 py-2 rounded shadow hover:bg-blue-700 disabled:opacity-50">
            {saving ? 'Saving...' : 'Save Settings'}
          </button>
        </div>
      </form>
    </div>
  );
}
