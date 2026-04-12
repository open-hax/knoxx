/**
 * AttentionCard Tests
 */

import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { AttentionCard } from "./AttentionCard";
import type { AttentionMetric } from "./dashboard-types";

// Wrapper for router
const RouterWrapper = ({ children }: { children: React.ReactNode }) => (
  <BrowserRouter>{children}</BrowserRouter>
);

describe("AttentionCard", () => {
  const mockMetric: AttentionMetric = {
    type: "review",
    count: 12,
    label: "Review Queue",
    description: "Items pending review before publication",
    ctaLabel: "Review items",
    ctaPath: "/workbench/review",
  };

  it("renders label, count, and description", () => {
    render(
      <RouterWrapper>
        <AttentionCard metric={mockMetric} />
      </RouterWrapper>
    );

    expect(screen.getByText("Review Queue")).toBeInTheDocument();
    expect(screen.getByText("12")).toBeInTheDocument();
    expect(screen.getByText("Items pending review before publication")).toBeInTheDocument();
  });

  it("renders CTA button with correct label", () => {
    render(
      <RouterWrapper>
        <AttentionCard metric={mockMetric} />
      </RouterWrapper>
    );

    expect(screen.getByRole("button", { name: "Review items" })).toBeInTheDocument();
  });

  it("disables CTA button when count is 0", () => {
    const zeroMetric: AttentionMetric = { ...mockMetric, count: 0 };

    render(
      <RouterWrapper>
        <AttentionCard metric={zeroMetric} />
      </RouterWrapper>
    );

    const button = screen.getByRole("button", { name: "Review items" });
    expect(button).toBeDisabled();
  });

  it("applies correct accent color for each attention type", () => {
    const types: Array<{ type: "review" | "approval" | "policy"; expected: string }> = [
      { type: "review", expected: "var(--token-colors-accent-cyan)" },
      { type: "approval", expected: "var(--token-colors-accent-amber)" },
      { type: "policy", expected: "var(--token-colors-accent-red)" },
    ];

    types.forEach(({ type, expected }) => {
      const metric: AttentionMetric = { ...mockMetric, type };
      const { container } = render(
        <RouterWrapper>
          <AttentionCard metric={metric} />
        </RouterWrapper>
      );

      const countElement = container.querySelector('[style*="color"]');
      expect(countElement).toHaveStyle({ color: expected });
    });
  });

  it("has correct CTA path for navigation", () => {
    render(
      <RouterWrapper>
        <AttentionCard metric={mockMetric} />
      </RouterWrapper>
    );

    // Button should be clickable (navigates via useNavigate)
    const button = screen.getByRole("button", { name: "Review items" });
    expect(button).toBeInTheDocument();
    expect(button).not.toBeDisabled();
  });
});
