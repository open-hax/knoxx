import type React from "react";
import { setKnoxxAuthIdentity } from "../../lib/nextApi";
import { IdentitySection } from "./IdentitySection";
import type { AdminCtx } from "../../pages/AdminLayout";
import type { AdminBootstrapContext, AdminOrgSummary } from '../../lib/types';
import { SectionCard } from './common';

export function SummarySection({
  orgs,
  toolsCount,
  permissionsCount,
  bootstrap,
}: {
  orgs: AdminOrgSummary[];
  toolsCount: number;
  permissionsCount: number;
  bootstrap: AdminBootstrapContext | null;
}) {
  return (
    <SectionCard
      title="Control-plane summary"
      description="What this actor can see right now."
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
          <div className="text-xs uppercase tracking-wide text-slate-500">Org scope</div>
          <div className="mt-2 text-2xl font-semibold text-slate-100">{orgs.length}</div>
          <div className="text-sm text-slate-400">Visible org entries</div>
        </div>
        <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
          <div className="text-xs uppercase tracking-wide text-slate-500">Catalog</div>
          <div className="mt-2 text-2xl font-semibold text-slate-100">{toolsCount}</div>
          <div className="text-sm text-slate-400">Tool definitions</div>
        </div>
        <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
          <div className="text-xs uppercase tracking-wide text-slate-500">Permission atoms</div>
          <div className="mt-2 text-2xl font-semibold text-slate-100">{permissionsCount}</div>
          <div className="text-sm text-slate-400">Available policy building blocks</div>
        </div>
        <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
          <div className="text-xs uppercase tracking-wide text-slate-500">Bootstrap</div>
          {bootstrap?.primaryOrg ? (
            <>
              <div className="mt-2 text-sm font-semibold text-slate-100">{bootstrap.primaryOrg.name}</div>
              <div className="text-sm text-slate-400">{bootstrap.bootstrapUser?.email}</div>
            </>
          ) : (
            <div className="mt-2 text-sm text-slate-400">Platform bootstrap is only visible to platform-scoped admins.</div>
          )}
        </div>
      </div>
    </SectionCard>
  );
}

export function AdminOverviewPage({ ctx }: { ctx: AdminCtx }) {
  const handleApplyIdentity = async (e: React.FormEvent) => {
    e.preventDefault();
    const resolved = setKnoxxAuthIdentity(ctx.identityForm);
    ctx.setIdentityForm(resolved);
    ctx.setNotice({ tone: 'success', text: `Switched to ${resolved.userEmail} in ${resolved.orgSlug}.` });
    await ctx.refresh();
  };

  return (
    <div className="space-y-4">
      <div className="grid gap-4 lg:grid-cols-2">
        <IdentitySection identityForm={ctx.identityForm} setIdentityForm={ctx.setIdentityForm} context={ctx.context} onApplyIdentity={handleApplyIdentity} />
        <SummarySection orgs={ctx.orgs} toolsCount={ctx.tools.length} permissionsCount={ctx.permissions.length} bootstrap={ctx.bootstrap} />
      </div>
    </div>
  );
}
