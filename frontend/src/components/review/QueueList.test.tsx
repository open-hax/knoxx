/**
 * QueueList Tests
 */

import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueueList } from "./QueueList";
import { ITEM_TYPE_CONFIG, ITEM_STATUS_CONFIG, type ReviewItem } from "./review-types";

// Create mock items matching the new API structure
const MOCK_REVIEW_ITEMS: Array<ReviewItem & { id: string; type: "synthesis" | "MT" | "ingestion"; status: string; summary: string }> = [
  {
    doc_id: "rev-1",
    id: "rev-1",
    tenant_id: "devel",
    title: "API Documentation Summary",
    content_preview: "Generated summary of REST API endpoints for v2 release.",
    summary: "Generated summary of REST API endpoints for v2 release.",
    visibility: "review",
    source: "ai-drafted",
    ai_drafted: true,
    type: "synthesis",
    status: "pending",
    confidence: 0.72,
    created_at: "2024-01-15T14:00:00Z",
    updated_at: "2024-01-15T14:00:00Z",
    source_count: 12,
    agent_name: "synthesizer-v3",
  },
  {
    doc_id: "rev-2",
    id: "rev-2",
    tenant_id: "devel",
    title: "German Translation: Getting Started",
    content_preview: "Machine translation of onboarding guide to German.",
    summary: "Machine translation of onboarding guide to German.",
    visibility: "review",
    source: "import",
    ai_drafted: false,
    type: "MT",
    status: "pending",
    confidence: 0.45,
    created_at: "2024-01-15T13:30:00Z",
    updated_at: "2024-01-15T13:30:00Z",
    source_count: 1,
    agent_name: "mt-de-v2",
  },
  {
    doc_id: "rev-3",
    id: "rev-3",
    tenant_id: "devel",
    title: "Changelog Extraction",
    content_preview: "Extracted changelog entries from commit history.",
    summary: "Extracted changelog entries from commit history.",
    visibility: "review",
    source: "ingestion",
    ai_drafted: false,
    type: "ingestion",
    status: "pending",
    confidence: 0.89,
    created_at: "2024-01-15T12:00:00Z",
    updated_at: "2024-01-15T12:00:00Z",
    source_count: 48,
    agent_name: null,
  },
  {
    doc_id: "rev-4",
    id: "rev-4",
    tenant_id: "devel",
    title: "Architecture Decision Record",
    content_preview: "Generated ADR for database migration strategy.",
    summary: "Generated ADR for database migration strategy.",
    visibility: "review",
    source: "ai-drafted",
    ai_drafted: true,
    type: "synthesis",
    status: "pending",
    confidence: 0.38,
    created_at: "2024-01-15T11:00:00Z",
    updated_at: "2024-01-15T11:00:00Z",
    source_count: 8,
    agent_name: "synthesizer-v3",
  },
  {
    doc_id: "rev-5",
    id: "rev-5",
    tenant_id: "devel",
    title: "Japanese Translation: API Reference",
    content_preview: "Machine translation of API reference to Japanese.",
    summary: "Machine translation of API reference to Japanese.",
    visibility: "review",
    source: "import",
    ai_drafted: false,
    type: "MT",
    status: "pending",
    confidence: 0.61,
    created_at: "2024-01-15T10:00:00Z",
    updated_at: "2024-01-15T10:00:00Z",
    source_count: 1,
    agent_name: "mt-ja-v2",
  },
];

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
