export type Notice = { tone: 'success' | 'error'; text: string } | null;
export type ToolDraftEffect = 'inherit' | 'allow' | 'deny';

export type UserFormState = {
  email: string;
  displayName: string;
  roleSlugs: string[];
};

export type RoleFormState = {
  name: string;
  slug: string;
  permissionCodes: string[];
  toolIds: string[];
};

export type OrgFormState = {
  name: string;
  slug: string;
  kind: string;
};

export type LakeFormState = {
  name: string;
  slug: string;
  kind: string;
  workspaceRoot: string;
};
