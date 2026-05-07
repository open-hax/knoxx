import { useEffect, useMemo, useState } from 'react';
import type React from 'react';
import type { AdminActorCredentialSummary, AdminRoleSummary, AdminToolDefinition, AdminUserSummary } from '../../lib/types';
import { membershipForOrg, toggleListValue, toolDraftMap } from './helpers';
import { Badge, SectionCard } from './common';
import type { ToolDraftEffect, UserFormState } from './types';

type ActorProfileDraft = {
  actorId: string;
  displayName: string;
  email: string;
  status: string;
};

type ActorCredentialDraft = {
  kind: string;
  accountIdentifier: string;
  credentials: Record<string, string>;
};

type CredentialDescriptor = {
  provider: string;
  label: string;
  kind: string;
  accountPlaceholder: string;
  fields: Array<{ key: string; label: string; secret?: boolean; placeholder?: string }>;
};

const CREDENTIAL_DESCRIPTORS: CredentialDescriptor[] = [
  {
    provider: 'bluesky',
    label: 'Bluesky',
    kind: 'app-password',
    accountPlaceholder: 'handle.bsky.social',
    fields: [
      { key: 'identifier', label: 'Identifier / handle', placeholder: 'handle.bsky.social' },
      { key: 'appPassword', label: 'App password', secret: true, placeholder: 'xxxx-xxxx-xxxx-xxxx' },
    ],
  },
  {
    provider: 'twitch',
    label: 'Twitch',
    kind: 'oauth-login',
    accountPlaceholder: 'twitch username',
    fields: [
      { key: 'username', label: 'Username', placeholder: 'channel_or_login' },
      { key: 'oauthToken', label: 'OAuth token', secret: true, placeholder: 'oauth:… or raw token' },
    ],
  },
  {
    provider: 'discord_bot',
    label: 'Discord bot',
    kind: 'bot-token',
    accountPlaceholder: 'bot application/client id',
    fields: [
      { key: 'botToken', label: 'Bot token', secret: true, placeholder: 'Bot token' },
      { key: 'applicationId', label: 'Application ID', placeholder: 'Discord application id' },
      { key: 'publicKey', label: 'Public key', placeholder: 'Optional interactions public key' },
    ],
  },
  {
    provider: 'discord_oauth',
    label: 'Discord OAuth login',
    kind: 'oauth-login',
    accountPlaceholder: 'discord user id or username',
    fields: [
      { key: 'clientId', label: 'Client ID', placeholder: 'OAuth client id' },
      { key: 'clientSecret', label: 'Client secret', secret: true, placeholder: 'OAuth client secret' },
      { key: 'accessToken', label: 'Access token', secret: true, placeholder: 'Optional current access token' },
      { key: 'refreshToken', label: 'Refresh token', secret: true, placeholder: 'Optional refresh token' },
    ],
  },
];

function credentialForProvider(credentials: AdminActorCredentialSummary[] | undefined, provider: string): AdminActorCredentialSummary | null {
  return credentials?.find((credential) => credential.provider === provider) ?? null;
}

function credentialKey(userId: string, provider: string): string {
  return `${userId}:${provider}`;
}

function draftProfileForUser(user: AdminUserSummary, selectedOrgId: string): ActorProfileDraft {
  const membership = membershipForOrg(user, selectedOrgId);
  return {
    actorId: membership?.actorId ?? '',
    displayName: user.displayName ?? '',
    email: user.email ?? '',
    status: user.status ?? 'active',
  };
}

function draftCredentialForDescriptor(
  user: AdminUserSummary,
  descriptor: CredentialDescriptor,
): ActorCredentialDraft {
  const current = credentialForProvider(user.credentials, descriptor.provider);
  return {
    kind: current?.kind || descriptor.kind,
    accountIdentifier: current?.accountIdentifier || '',
    credentials: descriptor.fields.reduce<Record<string, string>>((acc, field) => {
      acc[field.key] = '';
      return acc;
    }, {}),
  };
}

function configuredFieldLabel(current: AdminActorCredentialSummary | null, fieldKey: string): string {
  return current?.configuredFields.includes(fieldKey) ? 'configured' : 'not set';
}

export function UsersMembershipsSection({
  selectedOrgId,
  selectedOrgName,
  canCreateUsers,
  canUpdateMemberships,
  canUpdateUserPolicies,
  users,
  roles,
  tools,
  userForm,
  setUserForm,
  membershipRoleDrafts,
  setMembershipRoleDrafts,
  membershipToolDrafts,
  setMembershipToolDrafts,
  creatingUser,
  savingMembershipId,
  onCreateUser,
  onSaveActorProfile,
  onSaveActorCredential,
  onSaveMembershipRoles,
  onSaveMembershipPolicies,
}: {
  selectedOrgId: string;
  selectedOrgName: string;
  canCreateUsers: boolean;
  canUpdateMemberships: boolean;
  canUpdateUserPolicies: boolean;
  users: AdminUserSummary[];
  roles: AdminRoleSummary[];
  tools: AdminToolDefinition[];
  userForm: UserFormState;
  setUserForm: React.Dispatch<React.SetStateAction<UserFormState>>;
  membershipRoleDrafts: Record<string, string[]>;
  setMembershipRoleDrafts: React.Dispatch<React.SetStateAction<Record<string, string[]>>>;
  membershipToolDrafts: Record<string, Record<string, ToolDraftEffect>>;
  setMembershipToolDrafts: React.Dispatch<React.SetStateAction<Record<string, Record<string, ToolDraftEffect>>>>;
  creatingUser: boolean;
  savingMembershipId: string | null;
  onCreateUser: (event: React.FormEvent) => void | Promise<void>;
  onSaveActorProfile: (userId: string, draft: ActorProfileDraft) => void | Promise<void>;
  onSaveActorCredential: (userId: string, provider: string, draft: ActorCredentialDraft) => void | Promise<void>;
  onSaveMembershipRoles: (membershipId: string) => void | Promise<void>;
  onSaveMembershipPolicies: (membershipId: string) => void | Promise<void>;
}) {
  const [actorDrafts, setActorDrafts] = useState<Record<string, ActorProfileDraft>>({});
  const [credentialDrafts, setCredentialDrafts] = useState<Record<string, ActorCredentialDraft>>({});
  const [savingActorId, setSavingActorId] = useState<string | null>(null);
  const [savingCredential, setSavingCredential] = useState<string | null>(null);

  useEffect(() => {
    setActorDrafts(Object.fromEntries(users.map((user) => [user.id, draftProfileForUser(user, selectedOrgId)])));
    setCredentialDrafts(Object.fromEntries(users.flatMap((user) => CREDENTIAL_DESCRIPTORS.map((descriptor) => [
      credentialKey(user.id, descriptor.provider),
      draftCredentialForDescriptor(user, descriptor),
    ]))));
  }, [selectedOrgId, users]);

  const orgActorLabel = useMemo(() => selectedOrgName || 'selected org', [selectedOrgName]);

  const saveActorProfile = async (userId: string) => {
    const draft = actorDrafts[userId];
    if (!draft) return;
    setSavingActorId(userId);
    try {
      await onSaveActorProfile(userId, draft);
    } finally {
      setSavingActorId(null);
    }
  };

  const saveCredential = async (userId: string, provider: string) => {
    const key = credentialKey(userId, provider);
    const draft = credentialDrafts[key];
    if (!draft) return;
    setSavingCredential(key);
    try {
      await onSaveActorCredential(userId, provider, draft);
    } finally {
      setSavingCredential(null);
    }
  };

  return (
    <SectionCard
      title="Actors and credentials"
      description="Create machine or human actors, bind actor IDs to memberships, and store per-actor Bluesky, Twitch, Discord bot, and Discord OAuth credentials. Secrets are write-only: saved fields are shown as configured, never echoed back."
    >
      {selectedOrgId && canCreateUsers ? (
        <form className="mb-5 grid gap-3 rounded-xl border border-slate-800 bg-slate-900/80 p-4 md:grid-cols-4" onSubmit={onCreateUser}>
          <input
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            placeholder="actor id, e.g. discord_automation"
            value={userForm.actorId}
            onChange={(event) => setUserForm((current) => ({ ...current, actorId: event.target.value }))}
          />
          <input
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            placeholder="email or leave blank for @actors.local"
            value={userForm.email}
            onChange={(event) => setUserForm((current) => ({ ...current, email: event.target.value }))}
          />
          <input
            className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            placeholder="Display name"
            value={userForm.displayName}
            onChange={(event) => setUserForm((current) => ({ ...current, displayName: event.target.value }))}
          />
          <div>
            <div className="mb-2 text-xs uppercase tracking-wide text-slate-500">Initial roles</div>
            <div className="flex flex-wrap gap-2">
              {roles.map((role) => (
                <label key={role.id} className="inline-flex items-center gap-2 rounded-full border border-slate-700 px-3 py-1 text-xs text-slate-200">
                  <input
                    type="checkbox"
                    checked={userForm.roleSlugs.includes(role.slug)}
                    onChange={() => setUserForm((current) => ({
                      ...current,
                      roleSlugs: toggleListValue(current.roleSlugs, role.slug),
                    }))}
                  />
                  {role.slug}
                </label>
              ))}
            </div>
          </div>
          <div className="md:col-span-4 flex justify-end">
            <button
              type="submit"
              disabled={creatingUser || (!userForm.email.trim() && !userForm.actorId.trim())}
              className="rounded-lg border border-emerald-500/30 bg-emerald-500/10 px-4 py-2 text-sm font-medium text-emerald-200 hover:bg-emerald-500/20 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {creatingUser ? 'Creating…' : `Create actor in ${orgActorLabel}`}
            </button>
          </div>
        </form>
      ) : null}

      <div className="space-y-4">
        {users.length === 0 ? (
          <div className="rounded-xl border border-dashed border-slate-800 px-4 py-6 text-sm text-slate-400">
            No actors are visible in this org.
          </div>
        ) : users.map((user) => {
          const membership = membershipForOrg(user, selectedOrgId);
          if (!membership) {
            return null;
          }
          const roleDraft = membershipRoleDrafts[membership.id] || membership.roles.map((role) => role.slug);
          const toolDraft = membershipToolDrafts[membership.id] || toolDraftMap(membership.toolPolicies);
          const actorDraft = actorDrafts[user.id] || draftProfileForUser(user, selectedOrgId);
          return (
            <div key={user.id} className="rounded-xl border border-slate-800 bg-slate-900/80 p-4">
              <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                <div>
                  <div className="text-sm font-semibold text-slate-100">{user.displayName}</div>
                  <div className="text-sm text-slate-400">{user.email}</div>
                  <div className="mt-2 flex flex-wrap gap-2">
                    <Badge tone="info">actor:{membership.actorId || 'workspace_user'}</Badge>
                    <Badge tone={membership.status === 'active' ? 'success' : 'danger'}>{membership.status}</Badge>
                    {membership.isDefault ? <Badge tone="info">default membership</Badge> : null}
                  </div>
                </div>
                <div className="flex flex-wrap gap-2">
                  {membership.roles.map((role) => (
                    <Badge key={role.id}>{role.slug}</Badge>
                  ))}
                  {(user.credentials || []).map((credential) => (
                    <Badge key={credential.id} tone="warn">
                      {credential.label || credential.provider}:{credential.configuredFields.length} fields
                    </Badge>
                  ))}
                  {membership.toolPolicies.map((policy) => (
                    <Badge key={`${membership.id}-${policy.toolId}`} tone={policy.effect === 'deny' ? 'danger' : 'warn'}>
                      {policy.toolId}:{policy.effect}
                    </Badge>
                  ))}
                </div>
              </div>

              <div className="mt-4 grid gap-4 xl:grid-cols-2">
                <div className="rounded-xl border border-slate-800 bg-slate-950/70 p-4">
                  <div className="mb-3 text-sm font-semibold text-slate-100">Actor profile</div>
                  <div className="grid gap-3 sm:grid-cols-2">
                    <label className="flex flex-col gap-1 text-xs text-slate-300">
                      Actor ID
                      <input
                        className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
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
                        value={actorDraft.displayName}
                        onChange={(event) => setActorDrafts((current) => ({
                          ...current,
                          [user.id]: { ...actorDraft, displayName: event.target.value },
                        }))}
                      />
                    </label>
                    <label className="flex flex-col gap-1 text-xs text-slate-300">
                      Email / login identifier
                      <input
                        className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
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
                      disabled={savingActorId === user.id || !actorDraft.actorId.trim() || !actorDraft.email.trim()}
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
                                  value={draft.credentials[field.key] || ''}
                                  onChange={(event) => setCredentialDrafts((currentDrafts) => ({
                                    ...currentDrafts,
                                    [key]: {
                                      ...draft,
                                      credentials: { ...draft.credentials, [field.key]: event.target.value },
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

              <div className="mt-4 grid gap-4 xl:grid-cols-2">
                <div className="rounded-xl border border-slate-800 bg-slate-950/70 p-4">
                  <div className="mb-3 text-sm font-semibold text-slate-100">Membership roles</div>
                  <div className="flex flex-wrap gap-2">
                    {roles.map((role) => (
                      <label key={`${membership.id}-${role.id}`} className="inline-flex items-center gap-2 rounded-full border border-slate-700 px-3 py-1 text-xs text-slate-200">
                        <input
                          type="checkbox"
                          checked={roleDraft.includes(role.slug)}
                          onChange={() => setMembershipRoleDrafts((current) => ({
                            ...current,
                            [membership.id]: toggleListValue(current[membership.id] || [], role.slug),
                          }))}
                        />
                        {role.slug}
                      </label>
                    ))}
                  </div>
                  {canUpdateMemberships ? (
                    <button
                      type="button"
                      onClick={() => void onSaveMembershipRoles(membership.id)}
                      disabled={savingMembershipId === membership.id}
                      className="mt-4 rounded-lg border border-cyan-500/30 bg-cyan-500/10 px-4 py-2 text-sm font-medium text-cyan-200 hover:bg-cyan-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {savingMembershipId === membership.id ? 'Saving…' : 'Save membership roles'}
                    </button>
                  ) : null}
                </div>

                <div className="rounded-xl border border-slate-800 bg-slate-950/70 p-4">
                  <div className="mb-3 text-sm font-semibold text-slate-100">Membership tool overrides</div>
                  <div className="grid gap-3 sm:grid-cols-2">
                    {tools.map((tool) => (
                      <label key={`${membership.id}-tool-${tool.id}`} className="flex flex-col gap-2 text-xs text-slate-300">
                        <span className="font-medium text-slate-200">{tool.id}</span>
                        <select
                          className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
                          value={toolDraft[tool.id] || 'inherit'}
                          onChange={(event) => setMembershipToolDrafts((current) => ({
                            ...current,
                            [membership.id]: {
                              ...(current[membership.id] || {}),
                              [tool.id]: event.target.value as ToolDraftEffect,
                            },
                          }))}
                        >
                          <option value="inherit">inherit</option>
                          <option value="allow">allow</option>
                          <option value="deny">deny</option>
                        </select>
                      </label>
                    ))}
                  </div>
                  {canUpdateUserPolicies ? (
                    <button
                      type="button"
                      onClick={() => void onSaveMembershipPolicies(membership.id)}
                      disabled={savingMembershipId === membership.id}
                      className="mt-4 rounded-lg border border-amber-500/30 bg-amber-500/10 px-4 py-2 text-sm font-medium text-amber-200 hover:bg-amber-500/20 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      {savingMembershipId === membership.id ? 'Saving…' : 'Save tool overrides'}
                    </button>
                  ) : null}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </SectionCard>
  );
}
