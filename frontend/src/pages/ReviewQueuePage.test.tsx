/**
 * ReviewQueuePage Tests
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { ReviewQueuePage } from "./ReviewQueuePage";

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Wrapper for router
const RouterWrapper = ({ children }: { children: React.ReactNode }) => (
  <BrowserRouter>{children}</BrowserRouter>
);

describe("ReviewQueuePage", () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it("renders queue with pending items", () => {
    render(
      <RouterWrapper>
        <ReviewQueuePage />
      </RouterWrapper>
    );

    expect(screen.getByText("Review Queue")).toBeInTheDocument();
    expect(screen.getByText("5 pending")).toBeInTheDocument();
  });

  it("shows batch actions dropdown", () => {
    render(
      <RouterWrapper>
        <ReviewQueuePage />
      </RouterWrapper>
    );

    const batchSelect = screen.getByRole("combobox");
    expect(batchSelect).toBeInTheDocument();

    expect(screen.getByRole("option", { name: "Approve all pending" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Reject all pending" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Flag for review" })).toBeInTheDocument();
  });

  it("shows all pending items in the queue", () => {
    render(
      <RouterWrapper>
        <ReviewQueuePage />
      </RouterWrapper>
    );

    expect(screen.getByText("API Documentation Summary")).toBeInTheDocument();
    expect(screen.getByText("German Translation: Getting Started")).toBeInTheDocument();
    expect(screen.getByText("Changelog Extraction")).toBeInTheDocument();
  });

  it("shows item detail when item is selected", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ReviewQueuePage />
      </RouterWrapper>
    );

    // Initially should show empty detail panel
    expect(screen.getByText("Select an item")).toBeInTheDocument();

    // Click on an item
    const itemTitle = screen.getByText("Changelog Extraction");
    await user.click(itemTitle);

    // Detail panel should show item details
    expect(screen.queryByText("Select an item")).not.toBeInTheDocument();
    expect(screen.getAllByText("Changelog Extraction").length).toBeGreaterThan(1);
  });

  it("shows approve/reject/flag buttons for selected item", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ReviewQueuePage />
      </RouterWrapper>
    );

    // Click on an item
    await user.click(screen.getByText("Changelog Extraction"));

    expect(screen.getByRole("button", { name: "Approve" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Reject" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Flag" })).toBeInTheDocument();
  });

  it("approves item when Approve button is clicked", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ReviewQueuePage />
      </RouterWrapper>
    );

    // Click on an item
    await user.click(screen.getByText("Changelog Extraction"));

    // Click Approve
    await user.click(screen.getByRole("button", { name: "Approve" }));

    // Pending count should decrease
    expect(screen.getByText("4 pending")).toBeInTheDocument();
  });

  it("rejects item when Reject button is clicked", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ReviewQueuePage />
      </RouterWrapper>
    );

    // Click on an item
    await user.click(screen.getByText("Changelog Extraction"));

    // Click Reject
    await user.click(screen.getByRole("button", { name: "Reject" }));

    // Pending count should decrease
    expect(screen.getByText("4 pending")).toBeInTheDocument();
  });

  it("flags item when Flag button is clicked", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ReviewQueuePage />
      </RouterWrapper>
    );

    // Click on an item
    await user.click(screen.getByText("Changelog Extraction"));

    // Click Flag
    await user.click(screen.getByRole("button", { name: "Flag" }));

    // Pending count should decrease
    expect(screen.getByText("4 pending")).toBeInTheDocument();
  });

  it("approves all pending items with batch action", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ReviewQueuePage />
      </RouterWrapper>
    );

    const batchSelect = screen.getByRole("combobox");
    await user.selectOptions(batchSelect, "approve-all");

    // All items should be approved (0 pending)
    expect(screen.getByText("0 pending")).toBeInTheDocument();
  });

  it("rejects all pending items with batch action", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ReviewQueuePage />
      </RouterWrapper>
    );

    const batchSelect = screen.getByRole("combobox");
    await user.selectOptions(batchSelect, "reject-all");

    expect(screen.getByText("0 pending")).toBeInTheDocument();
  });

  it("shows type badge with correct color in detail panel", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ReviewQueuePage />
      </RouterWrapper>
    );

    // Click on a synthesis item
    await user.click(screen.getByText("API Documentation Summary"));

    // Type badge in detail should have cyan color
    const typeBadges = screen.getAllByText("Synthesis");
    const detailBadge = typeBadges.find((badge) => badge.style.color);
    expect(detailBadge).toHaveStyle({ color: "var(--token-colors-accent-cyan)" });
  });

  it("shows confidence in detail panel", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ReviewQueuePage />
      </RouterWrapper>
    );

    // Click on item with 89% confidence
    await user.click(screen.getByText("Changelog Extraction"));

    // Confidence should appear in detail panel
    const confidences = screen.getAllByText("89%");
    expect(confidences.length).toBeGreaterThan(0);
  });
});
