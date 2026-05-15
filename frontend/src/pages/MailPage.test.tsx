import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import MailPage from "./MailPage";

const mockListActorMailbox = vi.fn();
const mockAcknowledgeActorMailboxEntry = vi.fn();

vi.mock("@open-hax/uxx", () => ({
  Badge: ({ children }: { children: ReactNode }) => <span>{children}</span>,
  Button: ({ children, loading, ...props }: { children: ReactNode; loading?: boolean } & ButtonHTMLAttributes<HTMLButtonElement>) => (
    <button {...props}>{loading ? "Loading…" : children}</button>
  ),
}));

vi.mock("./useAuth", () => ({
  useAuth: () => ({
    actor: { id: "chat_primary" },
    membership: { actorId: "chat_primary" },
  }),
}));

vi.mock("../lib/api/runtime", () => ({
  listActorMailbox: (...args: unknown[]) => mockListActorMailbox(...args),
  acknowledgeActorMailboxEntry: (...args: unknown[]) => mockAcknowledgeActorMailboxEntry(...args),
}));

function mailboxResponse(status = "pending") {
  return {
    ok: true,
    box: "inbox",
    actorId: "chat_primary",
    entries: [{
      id: "mail-1",
      kind: "actor-message",
      status,
      source: { actorId: "fork_tales_creative_director" },
      target: { actorId: "chat_primary" },
      delivery: { mode: "message", attempts: 1 },
      contentRef: { runId: "run-1", sessionId: "session-1" },
      metadata: {},
      preview: "Fork Tales handoff is ready.",
      createdAt: "2026-05-15T00:00:00.000Z",
    }],
  };
}

function renderMailPage() {
  return render(
    <MemoryRouter>
      <MailPage />
    </MemoryRouter>,
  );
}

describe("MailPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockListActorMailbox.mockResolvedValue(mailboxResponse());
    mockAcknowledgeActorMailboxEntry.mockResolvedValue({ ok: true, entry: mailboxResponse("acknowledged").entries[0] });
  });

  it("loads mailbox entries and reloads when box/status filters change", async () => {
    renderMailPage();

    expect(await screen.findByText("Fork Tales handoff is ready.")).toBeInTheDocument();
    expect(mockListActorMailbox).toHaveBeenCalledWith("inbox", "all");

    fireEvent.change(screen.getByLabelText("Status"), { target: { value: "pending" } });
    await waitFor(() => expect(mockListActorMailbox).toHaveBeenCalledWith("inbox", "pending"));

    fireEvent.click(screen.getByRole("button", { name: "outbox" }));
    await waitFor(() => expect(mockListActorMailbox).toHaveBeenCalledWith("outbox", "pending"));
  });

  it("acknowledges an inbox entry and refreshes the mailbox", async () => {
    renderMailPage();

    await screen.findByText("Fork Tales handoff is ready.");
    fireEvent.click(screen.getByRole("button", { name: "Acknowledge" }));

    await waitFor(() => expect(mockAcknowledgeActorMailboxEntry).toHaveBeenCalledWith("mail-1"));
    expect(mockListActorMailbox).toHaveBeenCalledTimes(2);
  });
});
