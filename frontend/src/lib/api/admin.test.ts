import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  listAdminPermissions,
  listAdminTools,
  listAdminOrgs,
  listOrgRoles,
  listOrgActors,
  createOrgActor,
  updateAdminActor,
  getDiscordConfig,
  updateDiscordConfig,
  getEventAgentControl,
  updateEventAgentControl,
  runEventAgentJob,
  dispatchEventAgentEvent,
  stopEventAgentRuntime,
  startEventAgentRuntime,
  resetEventAgentRuntime,
  upsertAdminActorCredential,
} from "./admin";

// Mock the core request module
const mockRequest = vi.fn();
vi.mock("./core", () => ({
  request: (...args: unknown[]) => mockRequest(...args),
}));

describe("Admin API", () => {
  beforeEach(() => {
    mockRequest.mockReset();
  });

  it("preserves verified binding and unbound enrollment flags from the directory", async () => {
    mockRequest.mockResolvedValueOnce({users: [
      {id: "bound", principalId: "principal", identityBound: true, identityEnrollmentRequired: false, memberships: []},
      {id: "unbound", identityBound: false, identityEnrollmentRequired: true, memberships: []},
    ]});
    const {users} = await listOrgActors("org");
    expect(users[0]).toMatchObject({principalId: "principal", identityBound: true, identityEnrollmentRequired: false});
    expect(users[1]).toMatchObject({identityBound: false, identityEnrollmentRequired: true});
  });
  it("sends an explicit principal without inventing email or actor ID", async () => {
    mockRequest.mockResolvedValueOnce({user: {id: "user", principalId: "principal", identityBound: true, memberships: []}});
    await createOrgActor("org", {axxiumPrincipalId: "principal", roleSlugs: ["basic-user"]});
    const [url, init] = mockRequest.mock.calls[0];
    expect(url).toBe("/api/admin/orgs/org/actors");
    expect(JSON.parse(init.body)).toEqual({axxiumPrincipalId: "principal", roleSlugs: ["basic-user"]});
  });

  it("keeps a name-only profile unbound instead of assigning a blank actor ID", async () => {
    mockRequest.mockResolvedValueOnce({user: {id: "user", identityBound: false, identityEnrollmentRequired: true, memberships: []}});
    await updateAdminActor("user", {orgId: "org", displayName: "Revised name", actorId: "  "});
    const [url, init] = mockRequest.mock.calls[0];
    expect(url).toBe("/api/admin/actors/user");
    expect(JSON.parse(init.body)).toEqual({orgId: "org", displayName: "Revised name"});
  });
  it("preserves an explicit nonblank actor ID and profile status changes", async () => {
    mockRequest.mockResolvedValueOnce({user: {id: "user", memberships: []}});
    await updateAdminActor("user", {orgId: "org", actorId: " agent-one ", status: "disabled"});
    expect(JSON.parse(mockRequest.mock.calls[0][1].body)).toEqual({orgId: "org", actorId: "agent-one", status: "disabled"});
  });

  it("getDiscordConfig fetches discord config", async () => {
    const mockResponse = { configured: true, tokenPreview: "tok_***" };
    mockRequest.mockResolvedValueOnce(mockResponse);

    const result = await getDiscordConfig();
    expect(mockRequest).toHaveBeenCalledWith("/api/admin/config/discord");
    expect(result).toEqual(mockResponse);
  });

  it("normalizes policy admin wire keys before roles render", async () => {
    mockRequest.mockResolvedValueOnce({
      roles: [{
        id: "role-1",
        slug: "system-admin",
        name: "System admin",
        "org-id": "org-1",
        "scope-kind": "org",
        "built-in": true,
        "system-managed": false,
        permissions: ["platform.org.read"],
        "tool-policies": [{ "tool-id": "websearch", effect: "allow" }],
      }],
    });

    const result = await listOrgRoles("org-1");

    expect(mockRequest).toHaveBeenCalledWith("/api/admin/orgs/org-1/roles");
    expect(result.roles[0]).toMatchObject({
      id: "role-1",
      orgId: "org-1",
      scopeKind: "org",
      builtIn: true,
      systemManaged: false,
      permissions: ["platform.org.read"],
      toolPolicies: [{ toolId: "websearch", effect: "allow" }],
    });
  });

  it("fills permission identity fields for key-safe rendering", async () => {
    mockRequest.mockResolvedValueOnce({ permissions: [{ code: "org.roles.read" }] });

    const result = await listAdminPermissions();

    expect(result.permissions[0]).toEqual({
      id: "org.roles.read",
      code: "org.roles.read",
      resourceKind: "org",
      action: "read",
      description: "org.roles.read",
    });
  });

  it("normalizes tools, orgs, and actor memberships from kebab-case payloads", async () => {
    mockRequest
      .mockResolvedValueOnce({ tools: [{ id: "read", label: "Read", description: "Read docs", "risk-level": "low" }] })
      .mockResolvedValueOnce({ orgs: [{ id: "org-1", slug: "open-hax", name: "Open Hax", kind: "internal", "is-primary": true, "member-count": 2 }] })
      .mockResolvedValueOnce({
        users: [{
          id: "user-1",
          email: "agent@example.test",
          displayName: "Agent",
          status: "active",
          memberships: [{
            id: "membership-1",
            orgId: "org-1",
            actorId: "agent",
            status: "active",
            roles: [{ id: "role-1", slug: "basic-user", name: "Basic user" }],
            toolPolicies: [{ toolId: "read", effect: "deny" }],
          }],
        }],
      });

    await expect(listAdminTools()).resolves.toEqual({
      tools: [{ id: "read", label: "Read", description: "Read docs", riskLevel: "low" }],
    });
    await expect(listAdminOrgs()).resolves.toMatchObject({
      orgs: [{ id: "org-1", isPrimary: true, memberCount: 2 }],
    });
    await expect(listOrgActors("org-1")).resolves.toMatchObject({
      users: [{
        id: "user-1",
        displayName: "Agent",
        memberships: [{ orgId: "org-1", actorId: "agent", toolPolicies: [{ toolId: "read", effect: "deny" }] }],
      }],
    });
  });

  it("updateDiscordConfig sends PUT with token", async () => {
    const mockResponse = { configured: true, tokenPreview: "tok_***", ok: true };
    mockRequest.mockResolvedValueOnce(mockResponse);

    await updateDiscordConfig("new-token");
    expect(mockRequest).toHaveBeenCalledWith("/api/admin/config/discord", {
      method: "PUT",
      body: JSON.stringify({ discordBotToken: "new-token" }),
    });
  });

  it("getEventAgentControl fetches events config", async () => {
    const mockResponse = {
      configured: true,
      availableRoles: [],
      availableSourceKinds: [],
      availableTriggerKinds: [],
      control: { sources: {}, jobs: [] },
      runtime: { running: false, configured: false, jobs: [] },
    };
    mockRequest.mockResolvedValueOnce(mockResponse);

    const result = await getEventAgentControl();
    expect(mockRequest).toHaveBeenCalledWith("/api/admin/config/events");
    expect(result).toEqual(mockResponse);
  });

  it("updateEventAgentControl sends PUT with control data", async () => {
    const controlData = { sources: {}, jobs: [] };
    mockRequest.mockResolvedValueOnce({ ok: true });

    await updateEventAgentControl(controlData);
    expect(mockRequest).toHaveBeenCalledWith("/api/admin/config/events", {
      method: "PUT",
      body: JSON.stringify(controlData),
    });
  });

  it("runEventAgentJob posts to job run endpoint", async () => {
    mockRequest.mockResolvedValueOnce({ ok: true, jobId: "test-job" });

    await runEventAgentJob("test-job");
    expect(mockRequest).toHaveBeenCalledWith(
      "/api/admin/config/events/jobs/test-job/run",
      { method: "POST" }
    );
  });

  it("dispatchEventAgentEvent posts event payload", async () => {
    const event = { sourceKind: "github", eventKind: "issues.opened", payload: {} };
    mockRequest.mockResolvedValueOnce({ ok: true, matchedJobs: [], event: {} });

    await dispatchEventAgentEvent(event);
    expect(mockRequest).toHaveBeenCalledWith("/api/admin/config/events/dispatch", {
      method: "POST",
      body: JSON.stringify(event),
    });
  });

  it("runtime controls post to correct endpoints", async () => {
    mockRequest.mockResolvedValue({ ok: true, action: "test" });

    await stopEventAgentRuntime();
    expect(mockRequest).toHaveBeenCalledWith(
      "/api/admin/config/events/runtime/stop",
      { method: "POST" }
    );

    await startEventAgentRuntime();
    expect(mockRequest).toHaveBeenCalledWith(
      "/api/admin/config/events/runtime/start",
      { method: "POST" }
    );

    await resetEventAgentRuntime();
    expect(mockRequest).toHaveBeenCalledWith(
      "/api/admin/config/events/runtime/reset",
      { method: "POST" }
    );
  });

  it("upsertAdminActorCredential sends secretJson in body", async () => {
    mockRequest.mockResolvedValueOnce({
      credential: { id: "cred-1", provider: "discord_bot", configuredFields: ["botToken", "applicationId"] },
    });

    const result = await upsertAdminActorCredential("user-1", "discord_bot", {
      orgId: "org-1",
      kind: "bot-token",
      accountIdentifier: "app-123",
      secretJson: { botToken: "tok", applicationId: "123" },
    });

    expect(mockRequest).toHaveBeenCalledWith("/api/admin/actors/user-1/credentials/discord_bot", {
      method: "PUT",
      body: JSON.stringify({
        orgId: "org-1",
        kind: "bot-token",
        accountIdentifier: "app-123",
        secretJson: { botToken: "tok", applicationId: "123" },
      }),
    });
    expect(result.credential).toMatchObject({
      id: "cred-1",
      configuredFields: ["botToken", "applicationId"],
    });
  });

  it("upsertAdminActorCredential omits accountIdentifier when blank", async () => {
    mockRequest.mockResolvedValueOnce({ credential: { id: "cred-2" } });

    await upsertAdminActorCredential("user-2", "bluesky", {
      orgId: "org-1",
      kind: "app-password",
      secretJson: { identifier: "handle.bsky.social", appPassword: "pass" },
    });

    expect(mockRequest).toHaveBeenCalledWith("/api/admin/actors/user-2/credentials/bluesky", {
      method: "PUT",
      body: JSON.stringify({
        orgId: "org-1",
        kind: "app-password",
        secretJson: { identifier: "handle.bsky.social", appPassword: "pass" },
      }),
    });
  });
});
