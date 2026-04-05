import type React from 'react';
import type { AdminDataLakeSummary } from '../../lib/types';
import { Badge, SectionCard } from './common';
import type { LakeFormState } from './types';

export function DataLakesSection({
  selectedOrgName,
  canCreateDataLakes,
  lakeForm,
  setLakeForm,
  creatingLake,
  dataLakes,
  dataLakeKindOptions,
  onCreateLake,
}: {
  selectedOrgName: string;
  canCreateDataLakes: boolean;
  lakeForm: LakeFormState;
  setLakeForm: React.Dispatch<React.SetStateAction<LakeFormState>>;
  creatingLake: boolean;
  dataLakes: AdminDataLakeSummary[];
  dataLakeKindOptions: string[];
  onCreateLake: (event: React.FormEvent) => void | Promise<void>;
}) {
  return (
    <SectionCard
      title="Org data lakes"
      description="Control-plane owned data-lake inventory for the selected org."
    >
      {canCreateDataLakes ? (
        <form className="mb-5 space-y-3 rounded-xl border border-slate-800 bg-slate-900/80 p-4" onSubmit={onCreateLake}>
          <div className="grid gap-3 md:grid-cols-2">
            <input
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
              placeholder="Lake name"
              value={lakeForm.name}
              onChange={(event) => setLakeForm((current) => ({ ...current, name: event.target.value }))}
            />
            <input
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
              placeholder="lake-slug"
              value={lakeForm.slug}
              onChange={(event) => setLakeForm((current) => ({ ...current, slug: event.target.value }))}
            />
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            <select
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
              value={lakeForm.kind}
              onChange={(event) => setLakeForm((current) => ({ ...current, kind: event.target.value }))}
            >
              {dataLakeKindOptions.map((kind) => (
                <option key={kind} value={kind}>{kind}</option>
              ))}
            </select>
            <input
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
              placeholder="workspace root"
              value={lakeForm.workspaceRoot}
              onChange={(event) => setLakeForm((current) => ({ ...current, workspaceRoot: event.target.value }))}
            />
          </div>
          <div className="flex justify-end">
            <button
              type="submit"
              disabled={creatingLake || !lakeForm.name.trim()}
              className="rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-4 py-2 text-sm font-medium text-emerald-200 hover:bg-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {creatingLake ? 'Creating…' : `Create data lake${selectedOrgName ? ` for ${selectedOrgName}` : ''}`}
            </button>
          </div>
        </form>
      ) : null}

      <div className="space-y-4">
        {dataLakes.length === 0 ? (
          <div className="rounded-xl border border-dashed border-slate-800 px-4 py-6 text-sm text-slate-400">No org data lakes registered yet.</div>
        ) : dataLakes.map((lake) => (
          <div key={lake.id} className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
              <div>
                <div className="text-sm font-semibold text-slate-100">{lake.name}</div>
                <div className="text-sm text-slate-400">{lake.slug}</div>
              </div>
              <div className="flex flex-wrap gap-2">
                <Badge>{lake.kind}</Badge>
                <Badge tone={lake.status === 'active' ? 'success' : 'danger'}>{lake.status}</Badge>
              </div>
            </div>
            <pre className="mt-3 overflow-auto rounded-lg border border-slate-800 bg-slate-950/70 p-3 text-xs text-slate-300">
{JSON.stringify(lake.config, null, 2)}
            </pre>
          </div>
        ))}
      </div>
    </SectionCard>
  );
}
