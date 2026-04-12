/**
 * GardensTab Tests
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {
  GardensTab,
  DependencyGardenPlaceholder,
  TruthGardenPlaceholder,
  type GardenTab,
} from "./GardensTab";

describe("GardensTab", () => {
  const mockOnTabChange = vi.fn();

  beforeEach(() => {
    mockOnTabChange.mockClear();
    localStorage.clear();
  });

  it("renders all three tabs", () => {
    render(
      <GardensTab activeTab="events" onTabChange={mockOnTabChange}>
        <div>Content</div>
      </GardensTab>
    );

    expect(screen.getByRole("tab", { name: "Events" })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Dependency Garden" })).toBeInTheDocument();
    expect(screen.getByRole("tab", { name: "Truth Garden" })).toBeInTheDocument();
  });

  it("shows active tab as selected", () => {
    render(
      <GardensTab activeTab="dependency" onTabChange={mockOnTabChange}>
        <div>Content</div>
      </GardensTab>
    );

    expect(screen.getByRole("tab", { name: "Dependency Garden" })).toHaveAttribute(
      "aria-selected",
      "true"
    );
    expect(screen.getByRole("tab", { name: "Events" })).toHaveAttribute(
      "aria-selected",
      "false"
    );
  });

  it("calls onTabChange when tab is clicked", async () => {
    const user = userEvent.setup();

    render(
      <GardensTab activeTab="events" onTabChange={mockOnTabChange}>
        <div>Content</div>
      </GardensTab>
    );

    await user.click(screen.getByRole("tab", { name: "Truth Garden" }));

    expect(mockOnTabChange).toHaveBeenCalledWith("truth");
  });

  it("renders children in content area", () => {
    render(
      <GardensTab activeTab="events" onTabChange={mockOnTabChange}>
        <div>Test Content</div>
      </GardensTab>
    );

    expect(screen.getByText("Test Content")).toBeInTheDocument();
  });

  it("persists tab state to localStorage", () => {
    render(
      <GardensTab activeTab="truth" onTabChange={mockOnTabChange}>
        <div>Content</div>
      </GardensTab>
    );

    expect(localStorage.getItem("ops-active-tab")).toBe("truth");
  });
});

describe("DependencyGardenPlaceholder", () => {
  it("renders title and description", () => {
    render(<DependencyGardenPlaceholder />);

    expect(screen.getByText("Dependency Garden")).toBeInTheDocument();
    expect(screen.getByText(/Workspace dependency topology/)).toBeInTheDocument();
  });

  it("shows feature list", () => {
    render(<DependencyGardenPlaceholder />);

    expect(screen.getByText("Filter by package name")).toBeInTheDocument();
    expect(screen.getByText("See dependency chain")).toBeInTheDocument();
    expect(screen.getByText("Identify circular dependencies")).toBeInTheDocument();
  });
});

describe("TruthGardenPlaceholder", () => {
  it("renders title and description", () => {
    render(<TruthGardenPlaceholder />);

    expect(screen.getByText("Truth Garden")).toBeInTheDocument();
    expect(screen.getByText(/Control-plane state/)).toBeInTheDocument();
  });

  it("shows feature list", () => {
    render(<TruthGardenPlaceholder />);

    expect(screen.getByText("Current system state")).toBeInTheDocument();
    expect(screen.getByText("Recent state changes")).toBeInTheDocument();
    expect(screen.getByText("Audit trail")).toBeInTheDocument();
  });
});
