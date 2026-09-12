import React, {useState, type ComponentProps} from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {describe, expect, it, vi} from 'vitest';
import {UsersMembershipsSection} from './UsersMembershipsSection';
import type {UserFormState} from './types';
import type {AdminCtx} from '../../pages/AdminLayout';
import {AdminOrgsPage} from './OrganizationsSection';

const {createAdminOrg} = vi.hoisted(() => ({createAdminOrg: vi.fn()}));
vi.mock('../../lib/nextApi', () => ({createAdminOrg}));

type Props = ComponentProps<typeof UsersMembershipsSection>;
function mount(bound = true) {
  const save = vi.fn(async () => {});
  const user: Props['users'][number] = {id: 'user-1', email: bound ? 'member@wiki.test' : '', displayName: 'Research member',
    principalId: bound ? 'principal-1' : undefined, identityBound: bound, identityEnrollmentRequired: !bound, status: 'active',
    memberships: [{id: 'membership-1', orgId: 'org-1', actorId: bound ? 'principal-1' : 'directory-1', status: 'active', roles: [], toolPolicies: []}]};
  function Harness() {
    const [userForm, setUserForm] = useState<UserFormState>({actorId: '', email: '', displayName: '', axxiumPrincipalId: '', roleSlugs: []});
    return <UsersMembershipsSection selectedOrgId="org-1" selectedOrgName="Research" users={[user]} roles={[]} tools={[]}
      canCreateUsers canUpdateMemberships canUpdateUserPolicies canUpdateGlobalStatus={false}
      userForm={userForm} setUserForm={setUserForm} membershipRoleDrafts={{}} setMembershipRoleDrafts={vi.fn()}
      membershipToolDrafts={{}} setMembershipToolDrafts={vi.fn()} creatingUser={false} savingMembershipId={null}
      onCreateUser={vi.fn()} onSaveActorProfile={save} onSaveActorCredential={vi.fn()} onSaveMembershipRoles={vi.fn()} onSaveMembershipPolicies={vi.fn()} />;
  }
  render(<Harness />);
  return {save};
}
describe('Axxium actor directory controls', () => {
  it('protects bound identity and global status while allowing profile edits', async () => {
    const {save} = mount();
    fireEvent.click(screen.getByRole('button', {name: /Research member/}));
    expect(screen.getByLabelText('Actor ID')).toHaveAttribute('readonly');
    expect(screen.getByLabelText('Identity email (Axxium)')).toHaveAttribute('readonly');
    expect(screen.getByLabelText('Global actor status')).toBeDisabled();
    fireEvent.change(screen.getByLabelText('Display name', {exact: true}), {target: {value: 'Editor'}});
    fireEvent.click(screen.getByRole('button', {name: 'Save actor profile'}));
    await waitFor(() => expect(save).toHaveBeenCalledWith('user-1', {actorId: 'principal-1', email: 'member@wiki.test', displayName: 'Editor', status: 'active'}));
  });
  it('allows an unbound directory actor without inventing an email or login', () => {
    mount(false);
    fireEvent.change(screen.getByLabelText('New actor display name'), {target: {value: 'Research actor'}});
    expect(screen.getByRole('button', {name: 'Create actor in Research'})).toBeEnabled();
    fireEvent.click(screen.getByRole('button', {name: /Research member/}));
    expect(screen.getByText('Identity enrollment required')).toBeInTheDocument();
    expect(screen.getByLabelText('Directory contact email')).toHaveValue('');
    expect(screen.getByRole('button', {name: 'Save actor profile'})).toBeEnabled();
  });
  it('uses the verified principal instead of conflicting identity entry fields', () => {
    mount();
    fireEvent.change(screen.getByLabelText('Existing Axxium principal ID (optional)'), {target: {value: 'principal-2'}});
    expect(screen.getByLabelText('New actor ID')).toBeDisabled();
    expect(screen.getByLabelText('New actor contact email')).toBeDisabled();
    expect(screen.getByRole('button', {name: 'Create actor in Research'})).toBeEnabled();
  });
});

function orgContext(allowed = true) {
  createAdminOrg.mockReset();
  return {
    context: null, orgs: [], selectedOrg: null, selectedOrgId: '',
    orgForm: {name: ' Research ', slug: ' research ', kind: 'customer'},
    creatingOrg: false, hasPermission: () => allowed,
    setCreatingOrg: vi.fn(), setNotice: vi.fn(), setOrgForm: vi.fn(),
    setSelectedOrgId: vi.fn(), refresh: vi.fn(async () => {}),
  } as unknown as AdminCtx;
}

describe('Organization directory orchestration', () => {
  it('submits the original normalized payload and selects the refreshed organization', async () => {
    const ctx = orgContext();
    createAdminOrg.mockResolvedValue({org: {id: 'org-new', name: 'Research'}});
    render(<AdminOrgsPage ctx={ctx} />);
    fireEvent.click(screen.getByRole('button', {name: 'Create org'}));
    await waitFor(() => expect(ctx.setSelectedOrgId).toHaveBeenCalledWith('org-new'));
    expect(createAdminOrg).toHaveBeenCalledWith({name: 'Research', slug: 'research', kind: 'customer'});
    expect(ctx.refresh).toHaveBeenCalledTimes(1);
    expect(ctx.setOrgForm).toHaveBeenCalledWith({name: '', slug: '', kind: 'customer'});
    expect(ctx.setCreatingOrg).toHaveBeenLastCalledWith(false);
  });

  it('reports a failed creation without replacing the selection or refreshing', async () => {
    const ctx = orgContext();
    createAdminOrg.mockRejectedValue(new Error('directory unavailable'));
    render(<AdminOrgsPage ctx={ctx} />);
    fireEvent.click(screen.getByRole('button', {name: 'Create org'}));
    await waitFor(() => expect(ctx.setNotice).toHaveBeenCalledWith({tone: 'error', text: 'directory unavailable'}));
    expect(ctx.setCreatingOrg).toHaveBeenLastCalledWith(false);
    expect(ctx.refresh).not.toHaveBeenCalled();
    expect(ctx.setSelectedOrgId).not.toHaveBeenCalled();
  });

  it('does not expose organization creation without its capability', () => {
    render(<AdminOrgsPage ctx={orgContext(false)} />);
    expect(screen.queryByRole('button', {name: 'Create org'})).not.toBeInTheDocument();
    expect(createAdminOrg).not.toHaveBeenCalled();
  });
});
