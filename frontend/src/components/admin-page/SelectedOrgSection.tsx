import type { AdminOrgSummary, KnoxxAuthContext } from '../../lib/types';
import { SectionCard } from './common';

export function SelectedOrgSection({
  selectedOrg,
  context,
}: {
  selectedOrg: AdminOrgSummary | null;
  context: KnoxxAuthContext | null;
}) {
  return (
    <SectionCard
      title={selectedOrg ? `Selected org: ${selectedOrg.name}` : 'Selected org'}
      description="Scoped management surface for memberships, roles, and data lakes."
    >
      {selectedOrg ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
            <div className="text-xs uppercase tracking-wide text-slate-500">Slug</div>
            <div className="mt-2 text-sm font-semibold text-slate-100">{selectedOrg.slug}</div>
          </div>
          <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
            <div className="text-xs uppercase tracking-wide text-slate-500">Kind</div>
            <div className="mt-2 text-sm font-semibold text-slate-100">{selectedOrg.kind}</div>
          </div>
          <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
            <div className="text-xs uppercase tracking-wide text-slate-500">Status</div>
            <div className="mt-2 text-sm font-semibold text-slate-100">{selectedOrg.status}</div>
          </div>
          <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
            <div className="text-xs uppercase tracking-wide text-slate-500">Current actor</div>
            <div className="mt-2 text-sm font-semibold text-slate-100">{context?.user.email || 'Unknown'}</div>
          </div>
        </div>
      ) : (
        <p className="text-sm text-slate-400">No org is currently selected.</p>
      )}
    </SectionCard>
  );
}
