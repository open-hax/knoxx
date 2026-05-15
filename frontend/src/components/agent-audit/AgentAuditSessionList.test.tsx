import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mockListMemorySessions = vi.fn();
const mockListAdminActiveAgents = vi.fn();
const mockListActiveAgents = vi.fn();

vi.mock("@open-hax/uxx", () => ({
  Badge: ({ children }: { children: ReactNode }) => <span>{children}</span>,
  Button: ({ children, loading, ...props }: { children: ReactNode; loading?: boolean } & ButtonHTMLAttributes<HTMLButtonElement>) => (
    <button {...props}>{loading ? "Loading" : children}</button>
  ),
}));

vi.mock("../../lib/api/common", () => ({
  listMemorySessions: (...args: unknown[]) => mockListMemorySessions(...args),
  listAdminActiveAgents: (...args: unknown[]) => mockListAdminActiveAgents(...args),
  listActiveAgents: (...args: unknown[]) => mockListActiveAgents(...args),
}));

import {
  AgentAuditSessionList,
  activeRunToAuditSession,
  auditSessionMatchesContract,
  auditSessionStatus,
  mergeAuditSessions,
} from "./AgentAuditSessionList";
import type { ActiveAgentSummary, MemorySessionSummary } from "../../lib/types";

function activeRun(overrides: Partial<ActiveAgentSummary> = {}): ActiveAgentSummary {
  return {
    run_id: "run-1",
    session_id: "sid-1",
    conversation_id: "conv-1",
    status: "running",
    model: "gemma4:31b",
    created_at: "2026-05-14T00:00:00.000Z",
    updated_at: "2026-05-14T00:01:00.000Z",
    request_messages: [],
    settings: {},
    resources: {},
    agent_spec: { contractId: "fork_tales_creative_director", subAgentId: "fork_tales_creative_director" },
    ...overrides,
  } as ActiveAgentSummary;
}

function memorySession(overrides: Partial<MemorySessionSummary> = {}): MemorySessionSummary {
  return {
    project: "knoxx-session",
    session: "conv-history",
    title: "Fork history",
    last_ts: "2026-05-14T00:00:00.000Z",
    event_count: 7,
    contract_id: "fork_tales_creative_director",
    actor_id: "fork_tales_creative_director",
    is_active: false,
    active_status: "completed",
    has_active_stream: false,
    ...overrides,
  };
}

beforeEach(() => {
  vi.clearAllMocks();
  mockListMemorySessions.mockResolvedValue({
    ok: true,
    rows: [
      memorySession(),
      memorySession({ session: "conv-other", title: "Other history", contract_id: "other_agent", actor_id: "other_agent" }),
    ],
    total: 2,
    offset: 0,
    limit: 40,
    has_more: false,
  });
  mockListAdminActiveAgents.mockResolvedValue([
    activeRun({
      conversation_id: "conv-active",
      session_id: "sid-active",
      latest_user_message: "Advance Fork Tales lore.",
    }),
  ]);
  mockListActiveAgents.mockResolvedValue([]);
});

describe("AgentAuditSessionList utilities", () => {
  it("matches sessions by selected contract id and contract actors", () => {
    expect(auditSessionMatchesContract({ session: "a", contract_id: "agent_a" }, "agent_a")).toBe(true);
    expect(auditSessionMatchesContract({ session: "b", contract_actors: ["agent_b"] }, "agent_b")).toBe(true);
    expect(auditSessionMatchesContract({ session: "c", contract_id: "agent_c" }, "agent_b")).toBe(false);
  });

  it("turns an active run into a live audit session card source", () => {
    const session = activeRunToAuditSession(activeRun());
    expect(session?.session).toBe("conv-1");
    expect(session?.auditSource).toBe("active");
    expect(session?.contract_id).toBe("fork_tales_creative_director");
    expect(session?.active_session_id).toBe("sid-1");
  });

  it("merges active and history into one active-first list", () => {
    const memory: MemorySessionSummary[] = [
      { session: "conv-1", title: "Archived", contract_id: "fork_tales_creative_director", event_count: 7, last_ts: "2026-05-13T00:00:00.000Z" },
      { session: "conv-2", title: "Other", contract_id: "other_agent", event_count: 2, last_ts: "2026-05-14T00:00:00.000Z" },
    ];

    const merged = mergeAuditSessions(memory, [activeRun()], "fork_tales_creative_director");

    expect(merged).toHaveLength(1);
    expect(merged[0].session).toBe("conv-1");
    expect(merged[0].auditSource).toBe("active");
    expect(merged[0].event_count).toBe(7);
    expect(auditSessionStatus(merged[0]).label).toBe("Active");
  });
});

describe("AgentAuditSessionList component", () => {
  it("loads active and history sessions, filters by selected contract, and resumes the selected card", async () => {
    const resumeMemorySession = vi.fn();

    render(
      <AgentAuditSessionList
        builtInContractId="fork_tales_creative_director"
        controller={{
          conversationId: null,
          sessionId: "sid-active",
          loadingMemorySessionId: null,
          resumeMemorySession,
        }}
      />,
    );

    expect(await screen.findByText("Fork history")).toBeInTheDocument();
    expect(screen.getByText("sub-agent fork_tales_creative_director")).toBeInTheDocument();
    expect(screen.queryByText("Other history")).not.toBeInTheDocument();
    expect(mockListMemorySessions).toHaveBeenCalledWith({ limit: 40 });
    expect(mockListAdminActiveAgents).toHaveBeenCalledWith(250);

    fireEvent.change(screen.getByLabelText("Search audit sessions"), { target: { value: "fork history" } });
    await waitFor(() => expect(screen.queryByText("sub-agent fork_tales_creative_director")).not.toBeInTheDocument());

    fireEvent.click(screen.getByRole("button", { name: /Fork history/ }));
    expect(resumeMemorySession).toHaveBeenCalledWith("conv-history");
  });
});
