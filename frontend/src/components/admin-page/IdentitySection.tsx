import type { ActorProfileDraft, ActorCredentialDraft } from "./types";
import { CREDENTIAL_DESCRIPTORS, credentialKey, credentialForProvider, draftCredentialForDescriptor, configuredFieldLabel } from "./helpers";
import type React from 'react';
import type { AdminUserSummary, KnoxxAuthContext, KnoxxAuthIdentity } from '../../lib/types';
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

export function ActorIdentityEditors({
  user, actorDraft, setActorDrafts, canUpdateGlobalStatus, canUpdateMemberships,
  canUpdateUserPolicies, savingActorId, saveActorProfile, credentialDrafts,
  setCredentialDrafts, savingCredential, saveCredential,
}: {
  user: AdminUserSummary;
  actorDraft: ActorProfileDraft;
  setActorDrafts: React.Dispatch<React.SetStateAction<Record<string, ActorProfileDraft>>>;
  canUpdateGlobalStatus: boolean;
  canUpdateMemberships: boolean;
  canUpdateUserPolicies: boolean;
  savingActorId: string | null;
  saveActorProfile: (userId: string) => Promise<void>;
  credentialDrafts: Record<string, ActorCredentialDraft>;
  setCredentialDrafts: React.Dispatch<React.SetStateAction<Record<string, ActorCredentialDraft>>>;
  savingCredential: string | null;
  saveCredential: (userId: string, provider: string) => Promise<void>;
}) {
  return (
  <div className="mt-4 grid gap-4 xl:grid-cols-2">
    <div className="rounded-xl border border-slate-800 bg-slate-950/70 p-4">
      <div className="mb-3 text-sm font-semibold text-slate-100">Actor profile</div>
      <p className="mb-3 text-xs text-cyan-200">{user.identityBound ? 'Identity bound to Axxium' : 'Identity enrollment required'}</p>
      <div className="grid gap-3 sm:grid-cols-2">
        <label className="flex flex-col gap-1 text-xs text-slate-300">
          Actor ID
          <input
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            aria-label="Actor ID"
            readOnly={Boolean(user.identityBound)}
            value={actorDraft.actorId}
            onChange={(event) => setActorDrafts((current) => ({
              ...current,
              [user.id]: { ...actorDraft, actorId: event.target.value },
            }))}
          />
        </label>
        <label className="flex flex-col gap-1 text-xs text-slate-300">
          Display name
          <input
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            aria-label="Display name"
            value={actorDraft.displayName}
            onChange={(event) => setActorDrafts((current) => ({
              ...current,
              [user.id]: { ...actorDraft, displayName: event.target.value },
            }))}
          />
        </label>
        <label className="flex flex-col gap-1 text-xs text-slate-300">
          {user.identityBound ? 'Identity email (Axxium)' : 'Directory contact email'}
          <input
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            aria-label={user.identityBound ? 'Identity email (Axxium)' : 'Directory contact email'}
            readOnly
            value={actorDraft.email}
            onChange={(event) => setActorDrafts((current) => ({
              ...current,
              [user.id]: { ...actorDraft, email: event.target.value },
            }))}
          />
        </label>
        <label className="flex flex-col gap-1 text-xs text-slate-300">
          Status
          <select
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            aria-label="Global actor status"
            disabled={!canUpdateGlobalStatus}
            value={actorDraft.status}
            onChange={(event) => setActorDrafts((current) => ({
              ...current,
              [user.id]: { ...actorDraft, status: event.target.value },
            }))}
          >
            <option value="active">active</option>
            <option value="disabled">disabled</option>
          </select>
        </label>
      </div>
      {canUpdateMemberships ? (
        <button
          type="button"
          onClick={() => void saveActorProfile(user.id)}
          disabled={savingActorId === user.id || !actorDraft.displayName.trim()}
          className="mt-4 rounded-lg border border-cyan-500/30 bg-cyan-500/10 px-4 py-2 text-sm font-medium text-cyan-200 hover:bg-cyan-500/20 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {savingActorId === user.id ? 'Saving…' : 'Save actor profile'}
        </button>
      ) : null}
    </div>

    <div className="rounded-xl border border-slate-800 bg-slate-950/70 p-4">
      <div className="mb-3 text-sm font-semibold text-slate-100">Actor credentials</div>
      <div className="space-y-4">
        {CREDENTIAL_DESCRIPTORS.map((descriptor) => {
          const key = credentialKey(user.id, descriptor.provider);
          const draft = credentialDrafts[key] || draftCredentialForDescriptor(user, descriptor);
          const current = credentialForProvider(user.credentials, descriptor.provider);
          return (
            <div key={descriptor.provider} className="rounded-lg border border-slate-800 bg-slate-900/70 p-3">
              <div className="mb-2 flex items-center justify-between gap-3">
                <div>
                  <div className="text-xs font-semibold uppercase tracking-wide text-slate-300">{descriptor.label}</div>
                  <div className="text-[11px] text-slate-500">{current ? `Saved ${current.configuredFields.length} field(s)` : 'No credential saved yet'}</div>
                </div>
                <Badge tone={current ? 'success' : 'default'}>{current ? 'configured' : 'empty'}</Badge>
              </div>
              <div className="grid gap-2 sm:grid-cols-2">
                <label className="flex flex-col gap-1 text-[11px] text-slate-400 sm:col-span-2">
                  Account identifier
                  <input
                    className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100"
                    placeholder={descriptor.accountPlaceholder}
                    value={draft.accountIdentifier}
                    onChange={(event) => setCredentialDrafts((currentDrafts) => ({
                      ...currentDrafts,
                      [key]: { ...draft, accountIdentifier: event.target.value },
                    }))}
                  />
                </label>
                {descriptor.fields.map((field) => (
                  <label key={field.key} className="flex flex-col gap-1 text-[11px] text-slate-400">
                    <span className="flex items-center justify-between gap-2">
                      {field.label}
                      <span className="text-[10px] text-slate-500">{configuredFieldLabel(current, field.key)}</span>
                    </span>
                    <input
                      type={field.secret ? 'password' : 'text'}
                      className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-xs text-slate-100"
                      placeholder={field.placeholder || (field.secret ? 'leave blank to keep saved value' : '')}
                      value={draft.secretJson[field.key] || ''}
                      onChange={(event) => setCredentialDrafts((currentDrafts) => ({
                        ...currentDrafts,
                        [key]: {
                          ...draft,
                          secretJson: { ...draft.secretJson, [field.key]: event.target.value },
                        },
                      }))}
                    />
                  </label>
                ))}
              </div>
              {canUpdateUserPolicies ? (
                <button
                  type="button"
                  onClick={() => void saveCredential(user.id, descriptor.provider)}
                  disabled={savingCredential === key}
                  className="mt-3 rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-1.5 text-xs font-medium text-amber-200 hover:bg-amber-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {savingCredential === key ? 'Saving…' : `Save ${descriptor.label}`}
                </button>
              ) : null}
            </div>
          );
        })}
      </div>
    </div>
  </div>

  );
}
