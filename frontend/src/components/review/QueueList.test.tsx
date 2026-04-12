/**
 * QueueList Tests
 */

import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueueList } from "./QueueList";
import { ITEM_TYPE_CONFIG, ITEM_STATUS_CONFIG, MOCK_REVIEW_ITEMS } from "./review-types";

describe("QueueList", () => {
  const defaultProps = {
    items: MOCK_REVIEW_ITEMS,
    selectedId: undefined,
    onSelect: vi.fn(),
    itemTypeConfig: ITEM_TYPE_CONFIG,
    itemStatusConfig: ITEM_STATUS_CONFIG,
  };

  it("renders all items in the list", () => {
    render(<QueueList {...defaultProps} />);

    expect(screen.getByText("API Documentation Summary")).toBeInTheDocument();
    expect(screen.getByText("German Translation: Getting Started")).toBeInTheDocument();
    expect(screen.getByText("Changelog Extraction")).toBeInTheDocument();
  });

  it("sorts items by confidence (lowest first)", () => {
    const { container } = render(<QueueList {...defaultProps} />);

    const items = container.querySelectorAll("li");
    const confidences = Array.from(items).map((item) => {
      const confidenceText = item.querySelector('[class*="confidence"]')?.textContent;
      return parseInt(confidenceText?.replace("%", "") || "0", 10);
    });

    // Check that confidences are in ascending order
    for (let i = 1; i < confidences.length; i++) {
      expect(confidences[i]).toBeGreaterThanOrEqual(confidences[i - 1]);
    }
  });

  it("shows type badges for each item", () => {
    render(<QueueList {...defaultProps} />);

    // Type badges appear multiple times (in list items), use getAllByText
    const synthesisBadges = screen.getAllByText("Synthesis");
    const mtBadges = screen.getAllByText("MT Pipeline");
    const ingestionBadges = screen.getAllByText("Ingestion");

    expect(synthesisBadges.length).toBeGreaterThan(0);
    expect(mtBadges.length).toBeGreaterThan(0);
    expect(ingestionBadges.length).toBeGreaterThan(0);
  });

  it("shows confidence percentage for each item", () => {
    render(<QueueList {...defaultProps} />);

    // Check for specific confidence values from mock data
    expect(screen.getByText("72%")).toBeInTheDocument();
    expect(screen.getByText("45%")).toBeInTheDocument();
    expect(screen.getByText("89%")).toBeInTheDocument();
  });

  it("shows source count for each item", () => {
    render(<QueueList {...defaultProps} />);

    expect(screen.getByText("12 sources")).toBeInTheDocument();
    expect(screen.getByText("48 sources")).toBeInTheDocument();
  });

  it("shows agent name when present", () => {
    render(<QueueList {...defaultProps} />);

    const synthesizerAgents = screen.getAllByText("synthesizer-v3");
    const mtAgents = screen.getAllByText("mt-de-v2");

    expect(synthesizerAgents.length).toBeGreaterThan(0);
    expect(mtAgents.length).toBeGreaterThan(0);
  });

  it("calls onSelect when item is clicked", async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();

    render(<QueueList {...defaultProps} onSelect={onSelect} />);

    const firstItem = screen.getByText("Architecture Decision Record").closest("li");
    await user.click(firstItem!);

    expect(onSelect).toHaveBeenCalled();
    const selectedItem = onSelect.mock.calls[0][0];
    expect(selectedItem.title).toBe("Architecture Decision Record");
  });

  it("highlights selected item", () => {
    const { container } = render(
      <QueueList {...defaultProps} selectedId="rev-3" />
    );

    const selectedItem = screen.getByText("Changelog Extraction").closest("li");
    expect(selectedItem?.className).toMatch(/Selected/);
  });

  it("shows empty state when no items", () => {
    render(<QueueList {...defaultProps} items={[]} />);

    expect(screen.getByText("No items in the review queue.")).toBeInTheDocument();
  });

  it("supports keyboard navigation", async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn();

    render(<QueueList {...defaultProps} onSelect={onSelect} />);

    const firstItem = screen.getByText("Architecture Decision Record").closest("li");
    firstItem?.focus();

    await user.keyboard("{Enter}");

    expect(onSelect).toHaveBeenCalled();
  });
});
