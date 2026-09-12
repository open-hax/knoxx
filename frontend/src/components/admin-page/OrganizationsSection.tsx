import { createAdminOrg } from "../../lib/nextApi";
import { errorMessage } from "./helpers";
import { SelectedOrgSection } from "./SelectedOrgSection";
import type { AdminCtx } from "../../pages/AdminLayout";
import type React from 'react';
import type { AdminOrgSummary, KnoxxAuthContext } from '../../lib/types';
import { Badge, SectionCard, classNames } from './common';
import type { OrgFormState } from './types';

export function OrganizationsSection({
  context,
  orgs,
  selectedOrgId,
  setSelectedOrgId,
  canCreateOrgs,
  orgForm,
  setOrgForm,
  creatingOrg,
  onCreateOrg,
  orgKindOptions,
}: {
  context: KnoxxAuthContext | null;
  orgs: AdminOrgSummary[];
  selectedOrgId: string;
  setSelectedOrgId: React.Dispatch<React.SetStateAction<string>>;
  canCreateOrgs: boolean;
  orgForm: OrgFormState;
  setOrgForm: React.Dispatch<React.SetStateAction<OrgFormState>>;
  creatingOrg: boolean;
  onCreateOrg: (event: React.FormEvent) => void | Promise<void>;
  orgKindOptions: string[];
}) {
  return (
    <SectionCard
      title="Organizations"
      description="Platform-wide org inventory for system admins, or the current scoped org for org admins."
      actions={<div className="text-xs text-slate-500">{context?.isSystemAdmin ? 'Platform scope' : 'Org scope'}</div>}
    >
      <div className="space-y-3">
        {orgs.map((org) => {
          const active = org.id === selectedOrgId;
          return (
            <button
              key={org.id}
              type="button"
              onClick={() => setSelectedOrgId(org.id)}
              className={classNames(
                'w-full rounded-xl border p-4 text-left transition',
                active ? 'border-cyan-500/40 bg-cyan-500/10' : 'border-slate-800 bg-slate-900/70 hover:bg-slate-900',
              )}
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <div className="text-sm font-semibold text-slate-100">{org.name}</div>
                  <div className="text-sm text-slate-400">{org.slug}</div>
                </div>
                <div className="flex flex-wrap justify-end gap-2">
                  {org.isPrimary ? <Badge tone="warn">primary</Badge> : null}
                  <Badge>{org.kind}</Badge>
                </div>
              </div>
              <div className="mt-3 flex flex-wrap gap-3 text-xs text-slate-400">
                {typeof org.memberCount === 'number' ? <span>{org.memberCount} members</span> : null}
                {typeof org.roleCount === 'number' ? <span>{org.roleCount} roles</span> : null}
                {typeof org.dataLakeCount === 'number' ? <span>{org.dataLakeCount} lakes</span> : null}
              </div>
            </button>
          );
        })}
      </div>

      {canCreateOrgs ? (
        <form className="mt-5 space-y-3 rounded-xl border border-slate-800 bg-slate-900/80 p-4" onSubmit={onCreateOrg}>
          <div className="text-sm font-semibold text-slate-100">Create org</div>
          <div className="grid gap-3 md:grid-cols-2">
            <input
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
              placeholder="Org name"
              value={orgForm.name}
              onChange={(event) => setOrgForm((current) => ({ ...current, name: event.target.value }))}
            />
            <input
              className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
              placeholder="org-slug"
              value={orgForm.slug}
              onChange={(event) => setOrgForm((current) => ({ ...current, slug: event.target.value }))}
            />
          </div>
          <select
            className="w-full rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            value={orgForm.kind}
            onChange={(event) => setOrgForm((current) => ({ ...current, kind: event.target.value }))}
          >
            {orgKindOptions.map((kind) => (
              <option key={kind} value={kind}>{kind}</option>
            ))}
          </select>
          <button
            type="submit"
            disabled={creatingOrg || !orgForm.name.trim()}
            className="rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-4 py-2 text-sm font-medium text-emerald-200 hover:bg-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {creatingOrg ? 'Creating…' : 'Create org'}
          </button>
        </form>
      ) : null}
    </SectionCard>
  );
}

const ORG_KIND_OPTIONS = ['platform_owner', 'customer', 'internal', 'partner'];

export function AdminOrgsPage({ ctx }: { ctx: AdminCtx }) {
  const handleCreateOrg = async (e: React.FormEvent) => {
    e.preventDefault(); if (!ctx.hasPermission('platform.org.create')) return;
    ctx.setCreatingOrg(true); ctx.setNotice(null);
    try {
      const r = await createAdminOrg({ name: ctx.orgForm.name.trim(), slug: ctx.orgForm.slug.trim() || undefined, kind: ctx.orgForm.kind });
      ctx.setOrgForm({ name: '', slug: '', kind: 'customer' });
      ctx.setNotice({ tone: 'success', text: `Created ${r.org.name}.` }); await ctx.refresh(); ctx.setSelectedOrgId(r.org.id);
    } catch (e) { ctx.setNotice({ tone: 'error', text: errorMessage(e) }); } finally { ctx.setCreatingOrg(false); }
  };

  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_1fr]">
      <OrganizationsSection context={ctx.context} orgs={ctx.orgs} selectedOrgId={ctx.selectedOrgId} setSelectedOrgId={ctx.setSelectedOrgId}
        canCreateOrgs={ctx.hasPermission('platform.org.create')} orgForm={ctx.orgForm} setOrgForm={ctx.setOrgForm}
        creatingOrg={ctx.creatingOrg} onCreateOrg={handleCreateOrg} orgKindOptions={ORG_KIND_OPTIONS} />
      <SelectedOrgSection selectedOrg={ctx.selectedOrg} context={ctx.context} />
    </div>
  );
}
