/**
 * EventTable Tests
 */

import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { EventTable } from "./EventTable";
import type { OpsEvent } from "./ops-types";

// Mock events data
const MOCK_EVENTS: OpsEvent[] = [
  {
    id: "evt-1",
    time: new Date("2026-04-12T10:00:00Z"),
    type: "ingestion",
    status: "done",
    summary: "Test ingestion event",
    duration: 12340,
  },
  {
    id: "evt-2",
    time: new Date("2026-04-12T09:00:00Z"),
    type: "embedding",
    status: "error",
    summary: "Test embedding error",
    error: "Connection failed",
  },
  {
    id: "evt-3",
    time: new Date("2026-04-11T10:00:00Z"),
    type: "policy",
    status: "warn",
    summary: "Policy violation detected",
  },
];

describe("EventTable", () => {
  it("renders all events by default", () => {
    render(<EventTable events={MOCK_EVENTS} />);

    expect(screen.getByText("Test ingestion event")).toBeInTheDocument();
    expect(screen.getByText("Test embedding error")).toBeInTheDocument();
    expect(screen.getByText("Policy violation detected")).toBeInTheDocument();
  });

  it("shows event count in footer", () => {
    render(<EventTable events={MOCK_EVENTS} />);

    expect(screen.getByText("3 events")).toBeInTheDocument();
  });

  it("filters by type when type filter is clicked", async () => {
    const user = userEvent.setup();
    render(<EventTable events={MOCK_EVENTS} />);

    // Click "Ingestion" filter
    const ingestionButton = screen.getByRole("button", { name: "Ingestion" });
    await user.click(ingestionButton);

    // Should show only ingestion events
    expect(screen.getByText("Test ingestion event")).toBeInTheDocument();
    expect(screen.queryByText("Test embedding error")).not.toBeInTheDocument();
    expect(screen.queryByText("Policy violation detected")).not.toBeInTheDocument();

    // Footer should show filtered count
    expect(screen.getByText("1 event (filtered from 3)")).toBeInTheDocument();
  });

  it("filters by status when status filter is clicked", async () => {
    const user = userEvent.setup();
    render(<EventTable events={MOCK_EVENTS} />);

    // Click "error" filter (find by the button containing the error status)
    const errorButtons = screen.getAllByRole("button", { name: /error/i });
    await user.click(errorButtons[0]);

    // Should show only error events
    expect(screen.queryByText("Test ingestion event")).not.toBeInTheDocument();
    expect(screen.getByText("Test embedding error")).toBeInTheDocument();
    expect(screen.queryByText("Policy violation detected")).not.toBeInTheDocument();
  });

  it("clears all filters when clear button is clicked", async () => {
    const user = userEvent.setup();
    render(<EventTable events={MOCK_EVENTS} />);

    // Apply a filter
    const ingestionButton = screen.getByRole("button", { name: "Ingestion" });
    await user.click(ingestionButton);

    // Clear filters
    const clearButton = screen.getByRole("button", { name: "Clear filters" });
    await user.click(clearButton);

    // Should show all events again
    expect(screen.getByText("Test ingestion event")).toBeInTheDocument();
    expect(screen.getByText("Test embedding error")).toBeInTheDocument();
    expect(screen.getByText("Policy violation detected")).toBeInTheDocument();

    // Footer should show unfiltered count
    expect(screen.getByText("3 events")).toBeInTheDocument();
  });

  it("shows empty state when no events match filters", async () => {
    const user = userEvent.setup();
    render(<EventTable events={MOCK_EVENTS} />);

    // Click "running" filter (find by the button containing the running status)
    const runningButtons = screen.getAllByRole("button", { name: /running/i });
    await user.click(runningButtons[0]);

    expect(screen.getByText("No events match the current filters.")).toBeInTheDocument();
  });

  it("calls onSelectEvent when row is clicked", async () => {
    const user = userEvent.setup();
    const onSelectEvent = vi.fn();

    render(<EventTable events={MOCK_EVENTS} onSelectEvent={onSelectEvent} />);

    // Click first event row
    const ingestionEvent = screen.getByText("Test ingestion event").closest("tr");
    if (ingestionEvent) {
      await user.click(ingestionEvent);
    }

    expect(onSelectEvent).toHaveBeenCalledWith(MOCK_EVENTS[0]);
  });

  it("highlights selected event row", () => {
    render(
      <EventTable events={MOCK_EVENTS} selectedEventId="evt-1" />
    );

    const selectedRow = screen.getByText("Test ingestion event").closest("tr");
    // CSS modules generate unique class names, so we check for the presence of the selected row
    expect(selectedRow).toBeTruthy();
    // The selected row should have the selected class (contains "Selected")
    expect(selectedRow?.className).toMatch(/Selected/);
  });

  it("shows status icons with correct colors", () => {
    const { container } = render(<EventTable events={MOCK_EVENTS} />);

    // Check for status icons
    const doneIcon = container.querySelector('[style*="var(--token-colors-accent-green)"]');
    const errorIcon = container.querySelector('[style*="var(--token-colors-accent-red)"]');
    const warnIcon = container.querySelector('[style*="var(--token-colors-accent-amber)"]');

    expect(doneIcon).toBeInTheDocument();
    expect(errorIcon).toBeInTheDocument();
    expect(warnIcon).toBeInTheDocument();
  });

  it("shows type labels correctly", () => {
    render(<EventTable events={MOCK_EVENTS} />);

    // Type labels appear in both filter buttons and table cells
    const typeLabels = screen.getAllByText("Ingestion");
    expect(typeLabels.length).toBeGreaterThan(0);

    const embeddingLabels = screen.getAllByText("Embedding");
    expect(embeddingLabels.length).toBeGreaterThan(0);

    const policyLabels = screen.getAllByText("Policy Check");
    expect(policyLabels.length).toBeGreaterThan(0);
  });
});
