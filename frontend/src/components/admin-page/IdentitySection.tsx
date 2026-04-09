import type React from 'react';
import type { KnoxxAuthContext, KnoxxAuthIdentity } from '../../lib/types';
import { Badge, SectionCard } from './common';

export function IdentitySection({
  identityForm,
  setIdentityForm,
  context,
  onApplyIdentity,
}: {
  identityForm: KnoxxAuthIdentity;
  setIdentityForm: React.Dispatch<React.SetStateAction<KnoxxAuthIdentity>>;
  context: KnoxxAuthContext | null;
  onApplyIdentity: (event: React.FormEvent) => void | Promise<void>;
}) {
  return (
    <SectionCard
      title="Current actor"
      description="Header-based request identity for the live admin surface."
    >
      <form className="grid gap-4 md:grid-cols-[1fr_1fr_auto]" onSubmit={onApplyIdentity}>
        <label className="flex flex-col gap-2 text-sm text-slate-300">
          User email
          <input
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            value={identityForm.userEmail}
            onChange={(event) => setIdentityForm((current) => ({ ...current, userEmail: event.target.value }))}
            placeholder="system-admin@open-hax.local"
          />
        </label>
        <label className="flex flex-col gap-2 text-sm text-slate-300">
          Org slug
          <input
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            value={identityForm.orgSlug}
            onChange={(event) => setIdentityForm((current) => ({ ...current, orgSlug: event.target.value }))}
            placeholder="open-hax"
          />
        </label>
        <div className="flex items-end">
          <button
            type="submit"
            className="w-full rounded-lg border border-cyan-500/30 bg-cyan-500/10 px-4 py-2 text-sm font-medium text-cyan-200 hover:bg-cyan-500/20"
          >
            Apply actor
          </button>
        </div>
      </form>

      {context ? (
        <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
            <div className="text-xs uppercase tracking-wide text-slate-500">Resolved user</div>
            <div className="mt-2 text-sm font-semibold text-slate-100">{context.user.displayName}</div>
            <div className="text-sm text-slate-400">{context.user.email}</div>
          </div>
          <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
            <div className="text-xs uppercase tracking-wide text-slate-500">Resolved org</div>
            <div className="mt-2 text-sm font-semibold text-slate-100">{context.org.name}</div>
            <div className="text-sm text-slate-400">{context.org.slug}</div>
          </div>
          <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
            <div className="text-xs uppercase tracking-wide text-slate-500">Primary role</div>
            <div className="mt-2 text-sm font-semibold text-slate-100">{context.primaryRole}</div>
            <div className="mt-2 flex flex-wrap gap-2">
              {context.isSystemAdmin ? <Badge tone="warn">system admin</Badge> : null}
              <Badge tone="info">{context.permissions.length} permissions</Badge>
            </div>
          </div>
          <div className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
            <div className="text-xs uppercase tracking-wide text-slate-500">Membership</div>
            <div className="mt-2 text-sm font-semibold text-slate-100">{context.membership.id}</div>
            <div className="text-sm text-slate-400">{context.membership.status}</div>
          </div>
        </div>
      ) : null}

      {context ? (
        <div className="mt-4 flex flex-wrap gap-2">
          {context.roleSlugs.map((role) => (
            <Badge key={role} tone={role === 'system_admin' ? 'warn' : 'default'}>{role}</Badge>
          ))}
        </div>
      ) : null}
    </SectionCard>
  );
}
