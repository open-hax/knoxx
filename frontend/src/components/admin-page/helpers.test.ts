import { describe, expect, it } from "vitest";
import {
  errorMessage,
  groupPermissions,
  membershipForOrg,
  toolDraftMap,
  toolPoliciesFromDraft,
  toggleListValue,
  hydrateRoleDrafts,
  hydrateMembershipDrafts,
} from "./helpers";
import type { AdminPermissionDefinition, AdminUserSummary, AdminRoleSummary, AdminToolPolicy } from "../../lib/types";
import type { ToolDraftEffect } from "./types";

describe("errorMessage", () => {
  it("returns message for Error instances", () => {
    const error = new Error("test error");
    expect(errorMessage(error)).toBe("test error");
  });

  it("returns string for non-Error values", () => {
    expect(errorMessage("string error")).toBe("string error");
    expect(errorMessage(123)).toBe("123");
    expect(errorMessage(null)).toBe("null");
    expect(errorMessage(undefined)).toBe("undefined");
  });
});

describe("groupPermissions", () => {
  it("groups permissions by resourceKind", () => {
    const permissions: AdminPermissionDefinition[] = [
      { code: "read", resourceKind: "docs" },
      { code: "write", resourceKind: "docs" },
      { code: "delete", resourceKind: "files" },
    ] as AdminPermissionDefinition[];

    const result = groupPermissions(permissions);
    expect(result.length).toBe(2);
    expect(result[0][0]).toBe("docs");
    expect(result[1][0]).toBe("files");
  });

  it("puts permissions without resourceKind in misc group", () => {
    const permissions: AdminPermissionDefinition[] = [
      { code: "admin" } as AdminPermissionDefinition,
    ];
    const result = groupPermissions(permissions);
    expect(result[0][0]).toBe("misc");
  });

  it("sorts groups alphabetically", () => {
    const permissions: AdminPermissionDefinition[] = [
      { code: "a", resourceKind: "zebra" },
      { code: "b", resourceKind: "alpha" },
    ] as AdminPermissionDefinition[];
    const result = groupPermissions(permissions);
    expect(result[0][0]).toBe("alpha");
    expect(result[1][0]).toBe("zebra");
  });

  it("sorts permissions within groups by code", () => {
    const permissions: AdminPermissionDefinition[] = [
      { code: "write", resourceKind: "docs" },
      { code: "read", resourceKind: "docs" },
      { code: "admin", resourceKind: "docs" },
    ] as AdminPermissionDefinition[];
    const result = groupPermissions(permissions);
    const docsGroup = result.find(([key]) => key === "docs");
    expect(docsGroup![1].map((p) => p.code)).toEqual(["admin", "read", "write"]);
  });

  it("returns empty array for empty input", () => {
    expect(groupPermissions([])).toEqual([]);
  });
});

describe("membershipForOrg", () => {
  it("returns membership for matching orgId", () => {
    const user = {
      memberships: [
        { id: "m1", orgId: "org-a", roles: [], toolPolicies: [] },
        { id: "m2", orgId: "org-b", roles: [], toolPolicies: [] },
      ],
    } as AdminUserSummary;

    const result = membershipForOrg(user, "org-b");
    expect(result?.id).toBe("m2");
  });

  it("returns null when no matching orgId", () => {
    const user = {
      memberships: [{ id: "m1", orgId: "org-a", roles: [], toolPolicies: [] }],
    } as AdminUserSummary;

    expect(membershipForOrg(user, "org-b")).toBeNull();
  });

  it("returns null when memberships is empty", () => {
    const user = { memberships: [] } as AdminUserSummary;
    expect(membershipForOrg(user, "org-a")).toBeNull();
  });
});

describe("toolDraftMap", () => {
  it("creates map from policies", () => {
    const policies: AdminToolPolicy[] = [
      { toolId: "tool-a", effect: "allow" },
      { toolId: "tool-b", effect: "deny" },
    ];
    const result = toolDraftMap(policies);
    expect(result["tool-a"]).toBe("allow");
    expect(result["tool-b"]).toBe("deny");
  });

  it("returns empty object for empty input", () => {
    expect(toolDraftMap([])).toEqual({});
  });
});

describe("toolPoliciesFromDraft", () => {
  it("converts draft to policies array", () => {
    const draft: Record<string, ToolDraftEffect> = {
      "tool-a": "allow",
      "tool-b": "deny",
    };
    const result = toolPoliciesFromDraft(draft);
    expect(result.length).toBe(2);
    expect(result.find((p) => p.toolId === "tool-a")?.effect).toBe("allow");
    expect(result.find((p) => p.toolId === "tool-b")?.effect).toBe("deny");
  });

  it("skips entries with invalid effect", () => {
    const draft: Record<string, ToolDraftEffect> = {
      "tool-a": "allow",
      "tool-b": "invalid" as ToolDraftEffect,
      "tool-c": undefined as unknown as ToolDraftEffect,
    };
    const result = toolPoliciesFromDraft(draft);
    expect(result.length).toBe(1);
    expect(result[0].toolId).toBe("tool-a");
  });

  it("returns empty array for empty draft", () => {
    expect(toolPoliciesFromDraft({})).toEqual([]);
  });
});

describe("toggleListValue", () => {
  it("removes value if present", () => {
    expect(toggleListValue(["a", "b", "c"], "b")).toEqual(["a", "c"]);
  });

  it("adds value if not present", () => {
    expect(toggleListValue(["a", "c"], "b")).toEqual(["a", "c", "b"]);
  });

  it("handles empty list", () => {
    expect(toggleListValue([], "a")).toEqual(["a"]);
  });

  it("handles single toggle", () => {
    expect(toggleListValue(["a"], "a")).toEqual([]);
  });
});

describe("hydrateRoleDrafts", () => {
  it("creates draft map from roles", () => {
    const roles: AdminRoleSummary[] = [
      {
        id: "role-1",
        slug: "admin",
        name: "Admin",
        toolPolicies: [
          { toolId: "tool-a", effect: "allow" },
          { toolId: "tool-b", effect: "deny" },
        ],
      },
      {
        id: "role-2",
        slug: "user",
        name: "User",
        toolPolicies: [{ toolId: "tool-a", effect: "allow" }],
      },
    ] as AdminRoleSummary[];

    const result = hydrateRoleDrafts(roles);
    expect(result["role-1"]["tool-a"]).toBe("allow");
    expect(result["role-1"]["tool-b"]).toBe("deny");
    expect(result["role-2"]["tool-a"]).toBe("allow");
  });

  it("returns empty object for empty input", () => {
    expect(hydrateRoleDrafts([])).toEqual({});
  });
});

describe("hydrateMembershipDrafts", () => {
  it("creates drafts from user memberships", () => {
    const users: AdminUserSummary[] = [
      {
        memberships: [
          {
            id: "mem-1",
            orgId: "org-a",
            roles: [{ id: "r1", slug: "admin", name: "Admin", toolPolicies: [] }],
            toolPolicies: [{ toolId: "tool-a", effect: "allow" }],
          },
        ],
      } as AdminUserSummary,
    ];

    const result = hydrateMembershipDrafts(users, "org-a");
    expect(result.roleDrafts["mem-1"]).toEqual(["admin"]);
    expect(result.toolDrafts["mem-1"]["tool-a"]).toBe("allow");
  });

  it("skips users without membership for org", () => {
    const users: AdminUserSummary[] = [
      {
        memberships: [{ id: "mem-1", orgId: "org-b", roles: [], toolPolicies: [] }],
      } as AdminUserSummary,
    ];

    const result = hydrateMembershipDrafts(users, "org-a");
    expect(Object.keys(result.roleDrafts)).toHaveLength(0);
    expect(Object.keys(result.toolDrafts)).toHaveLength(0);
  });

  it("returns empty drafts for empty input", () => {
    const result = hydrateMembershipDrafts([], "org-a");
    expect(result.roleDrafts).toEqual({});
    expect(result.toolDrafts).toEqual({});
  });
});
