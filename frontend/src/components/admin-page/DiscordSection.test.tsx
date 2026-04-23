import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { DiscordSection } from "./DiscordSection";
import type { EventAgentControlResponse } from "../../lib/api/admin";

const apiMocks = vi.hoisted(() => ({
  getDiscordConfig: vi.fn(),
  getEventAgentControl: vi.fn(),
  updateDiscordConfig: vi.fn(),
  updateEventAgentControl: vi.fn(),
  runEventAgentJob: vi.fn(),
  dispatchEventAgentEvent: vi.fn(),
}));

vi.mock("../../lib/api/admin", () => ({
  ...apiMocks,
}));

const {
  getDiscordConfig,
  getEventAgentControl,
  updateDiscordConfig,
  updateEventAgentControl,
  runEventAgentJob,
  dispatchEventAgentEvent,
} = apiMocks;

const response: EventAgentControlResponse = {
  configured: true,
  tokenPreview: "tok_***",
  availableRoles: ["knowledge_worker", "system_admin"],
  availableSourceKinds: ["discord", "github", "manual"],
  availableTriggerKinds: ["cron", "event"],
  control: {
    sources: {
      discord: {
        botUserId: "123",
        defaultChannels: ["chan-a"],
        targetKeywords: ["knoxx"],
      },
      github: {},
      cron: {},
    },
    jobs: [
      {
        id: "discord_patrol",
        name: "discord_patrol",
        enabled: true,
        description: "Patrol Discord for fresh messages.",
        contractSourceId: "discord_patrol",
        contractHash: 101,
        trigger: { kind: "cron", cadenceMinutes: 5, eventKinds: [] },
        source: { kind: "discord", mode: "patrol", config: { maxMessages: 25 } },
        filters: { channels: ["chan-a"] },
        agentSpec: {
          role: "knowledge_worker",
          model: "gemma4:31b",
          thinkingLevel: "off",
          systemPrompt: "contract system patrol",
          taskPrompt: "contract task patrol",
          toolPolicies: [{ toolId: "discord.read", effect: "allow" }],
        },
      },
      {
        id: "github_watch",
        name: "github_watch",
        enabled: true,
        description: "Watch GitHub issues and react.",
        trigger: { kind: "event", cadenceMinutes: 5, eventKinds: ["issues.opened"] },
        source: { kind: "github", mode: "respond", config: {} },
        filters: { repositories: ["open-hax/openplanner"] },
        agentSpec: {
          role: "system_admin",
          model: "glm-5",
          thinkingLevel: "minimal",
          systemPrompt: "github system",
          taskPrompt: "github task",
          toolPolicies: [{ toolId: "websearch", effect: "allow" }],
        },
      },
    ],
  },
  runtime: {
    running: true,
    configured: true,
    sources: {
      recentEvents: [{ id: "evt-1" }],
      discord: { lastSeenChannels: ["chan-a"] },
    },
    jobs: [
      {
        id: "discord_patrol",
        name: "discord_patrol",
        enabled: true,
        scheduleLabel: "every 5 minutes",
        running: false,
        runCount: 3,
        lastStatus: "ok",
        lastFinishedAt: 1710000000000,
      },
      {
        id: "github_watch",
        name: "github_watch",
        enabled: true,
        scheduleLabel: "issues.opened",
        running: true,
        runCount: 7,
        lastStatus: "running",
      },
    ],
  },
};

describe("DiscordSection", () => {
  beforeEach(() => {
    getDiscordConfig.mockReset();
    getEventAgentControl.mockReset();
    updateDiscordConfig.mockReset();
    updateEventAgentControl.mockReset();
    runEventAgentJob.mockReset();
    dispatchEventAgentEvent.mockReset();

    getDiscordConfig.mockResolvedValue({ configured: true, tokenPreview: "tok_***" });
    getEventAgentControl.mockResolvedValue(response);
    updateDiscordConfig.mockResolvedValue({ ok: true, configured: true, tokenPreview: "tok_***" });
    updateEventAgentControl.mockResolvedValue({ ...response, ok: true });
    runEventAgentJob.mockResolvedValue({ ok: true, jobId: "discord_patrol" });
    dispatchEventAgentEvent.mockResolvedValue({ ok: true, matchedJobs: [], event: {} });
  });

  it("shows one selected event agent at a time and switches via the sidebar", async () => {
    const user = userEvent.setup();

    render(<DiscordSection canManage tools={[{ id: "discord.read", label: "Discord Read", description: "Read Discord", riskLevel: "low" }]} />);

    expect(await screen.findByDisplayValue("contract system patrol")).toBeInTheDocument();
    expect(screen.getByText(/Contract-backed from/i)).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: /github_watch/i }));

    expect(await screen.findByDisplayValue("github system")).toBeInTheDocument();
    expect(screen.queryByDisplayValue("contract system patrol")).not.toBeInTheDocument();
  });

  it("filters the sidebar with the search box", async () => {
    const user = userEvent.setup();

    render(<DiscordSection canManage tools={[]} />);

    await screen.findByDisplayValue("contract system patrol");

    const searchInput = screen.getByLabelText("Search");
    await user.type(searchInput, "github");

    await waitFor(() => {
      expect(screen.queryByRole("button", { name: /discord_patrol/i })).not.toBeInTheDocument();
    });

    expect(screen.getByRole("button", { name: /github_watch/i })).toBeInTheDocument();
    expect(screen.getByDisplayValue("github system")).toBeInTheDocument();
  });
});
