import { describe, it, expect, vi, beforeEach } from "vitest";
import { act, render, screen, waitFor } from "@testing-library/react";
import { EventAgentsPanel } from "./EventAgentsPanel";

describe("EventAgentsPanel", () => {
  beforeEach(() => {
    // Clear any window.knoxx namespace
    (window as unknown as Record<string, unknown>).knoxx = undefined;
  });

  it("shows loading state while waiting for CLJS module", () => {
    render(
      <EventAgentsPanel
        canManage={true}
        tools={[{ id: "test", label: "Test Tool", description: "", riskLevel: "low" }]}
      />
    );

    expect(screen.getByText(/Loading runtime jobs/i)).toBeInTheDocument();
  });

  it("shows a loud integration error when the CLJS module is not available after timeout", async () => {
    vi.useFakeTimers();

    render(
      <EventAgentsPanel
        canManage={true}
        tools={[{ id: "test", label: "Test Tool", description: "", riskLevel: "low" }]}
      />
    );

    // Fast-forward past the 1.5s timeout
    await act(async () => {
      vi.advanceTimersByTime(2000);
    });
    vi.useRealTimers();

    await waitFor(() => {
      expect(screen.getByText(/Runtime jobs \(shadow-cljs\) failed to load/i)).toBeInTheDocument();
    });
  });

  it("renders CLJS component when namespace is available", () => {
    const mockComponent = vi.fn(() => <div data-testid="cljs-panel">CLJS Panel</div>);
    
    // Set up the window.knoxx namespace as shadow-cljs would
    (window as unknown as Record<string, unknown>).knoxx = {
      frontend: {
        admin: {
          event_agents_panel: {
            event_agents_panel: mockComponent,
          },
        },
      },
    };

    render(
      <EventAgentsPanel
        canManage={true}
        tools={[{ id: "test", label: "Test Tool", description: "", riskLevel: "low" }]}
        onSelectedJobChange={vi.fn()}
      />
    );

    expect(screen.getByTestId("cljs-panel")).toBeInTheDocument();
    expect(mockComponent).toHaveBeenCalledWith(
      expect.objectContaining({
        canManage: true,
        tools: [{ id: "test", label: "Test Tool", description: "", riskLevel: "low" }],
      }),
      expect.anything()
    );
  });
});