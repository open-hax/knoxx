import { describe, it, expect, vi, beforeEach } from "vitest";
import {
  getDiscordConfig,
  updateDiscordConfig,
  getEventAgentControl,
  updateEventAgentControl,
  runEventAgentJob,
  dispatchEventAgentEvent,
  stopEventAgentRuntime,
  startEventAgentRuntime,
  resetEventAgentRuntime,
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

  it("getDiscordConfig fetches discord config", async () => {
    const mockResponse = { configured: true, tokenPreview: "tok_***" };
    mockRequest.mockResolvedValueOnce(mockResponse);

    const result = await getDiscordConfig();
    expect(mockRequest).toHaveBeenCalledWith("/api/admin/config/discord");
    expect(result).toEqual(mockResponse);
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
});