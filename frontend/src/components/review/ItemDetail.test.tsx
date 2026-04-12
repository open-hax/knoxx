/**
 * ItemDetail Tests
 */

import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ItemDetail } from "./ItemDetail";
import { ITEM_TYPE_CONFIG } from "./review-types";

describe("ItemDetail", () => {
  const mockItem = {
    doc_id: "test-1",
    id: "test-1",
    tenant_id: "devel",
    title: "Test Document",
    content_preview: "This is the full content of the test document.",
    summary: "This is the full content of the test document.",
    visibility: "review" as const,
    source: "ai-drafted" as const,
    ai_drafted: true,
    type: "synthesis" as const,
    status: "pending",
    confidence: 0.72,
    created_at: "2024-01-15T14:00:00Z",
    updated_at: "2024-01-15T14:00:00Z",
    source_count: 5,
    agent_name: "test-agent",
  };

  const defaultProps = {
    item: mockItem,
    itemTypeConfig: ITEM_TYPE_CONFIG,
    onApprove: vi.fn(),
    onReject: vi.fn(),
    onFlag: vi.fn(),
  };

  it("renders item title and type badge", () => {
    render(<ItemDetail {...defaultProps} />);

    expect(screen.getByText("Test Document")).toBeInTheDocument();
    expect(screen.getByText("Synthesis")).toBeInTheDocument();
  });

  it("shows output content", () => {
    render(<ItemDetail {...defaultProps} />);

    expect(screen.getByText(/This is the full content/)).toBeInTheDocument();
  });

  it("shows confidence with correct color for medium confidence", () => {
    render(<ItemDetail {...defaultProps} />);

    const confidence = screen.getByText("72%");
    expect(confidence).toBeInTheDocument();
    expect(confidence).toHaveAttribute("data-confidence", "medium");
  });

  it("shows confidence with high color for high confidence", () => {
    const highConfidenceItem = { ...mockItem, confidence: 0.89 };
    render(<ItemDetail {...defaultProps} item={highConfidenceItem} />);

    const confidence = screen.getByText("89%");
    expect(confidence).toHaveAttribute("data-confidence", "high");
  });

  it("shows confidence with low color for low confidence", () => {
    const lowConfidenceItem = { ...mockItem, confidence: 0.35 };
    render(<ItemDetail {...defaultProps} item={lowConfidenceItem} />);

    const confidence = screen.getByText("35%");
    expect(confidence).toHaveAttribute("data-confidence", "low");
  });

  it("shows source count", () => {
    render(<ItemDetail {...defaultProps} />);

    expect(screen.getByText("5")).toBeInTheDocument();
  });

  it("shows agent name when present", () => {
    render(<ItemDetail {...defaultProps} />);

    expect(screen.getByText("test-agent")).toBeInTheDocument();
  });

  it("shows AI Drafted status", () => {
    render(<ItemDetail {...defaultProps} />);

    expect(screen.getByText("Yes")).toBeInTheDocument();
  });

  it("shows action buttons", () => {
    render(<ItemDetail {...defaultProps} />);

    expect(screen.getByRole("button", { name: "Approve" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Reject" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Flag" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Needs Edit" })).toBeInTheDocument();
  });

  it("calls onApprove when Approve button is clicked", async () => {
    const user = userEvent.setup();
    const onApprove = vi.fn();

    render(<ItemDetail {...defaultProps} onApprove={onApprove} />);

    await user.click(screen.getByRole("button", { name: "Approve" }));

    expect(onApprove).toHaveBeenCalled();
  });

  it("calls onReject when Reject button is clicked", async () => {
    const user = userEvent.setup();
    const onReject = vi.fn();

    render(<ItemDetail {...defaultProps} onReject={onReject} />);

    await user.click(screen.getByRole("button", { name: "Reject" }));

    expect(onReject).toHaveBeenCalled();
  });

  it("calls onFlag when Flag button is clicked", async () => {
    const user = userEvent.setup();
    const onFlag = vi.fn();

    render(<ItemDetail {...defaultProps} onFlag={onFlag} />);

    await user.click(screen.getByRole("button", { name: "Flag" }));

    expect(onFlag).toHaveBeenCalled();
  });

  it("shows label form when Needs Edit is clicked", async () => {
    const user = userEvent.setup();

    render(<ItemDetail {...defaultProps} />);

    // Initially no label form
    expect(screen.queryByText("Labels")).not.toBeInTheDocument();

    // Click Needs Edit
    await user.click(screen.getByRole("button", { name: "Needs Edit" }));

    // Label form should appear
    expect(screen.getByText("Labels")).toBeInTheDocument();
    expect(screen.getByText("Accuracy")).toBeInTheDocument();
    expect(screen.getByText("Completeness")).toBeInTheDocument();
  });

  it("disables action buttons for non-pending items", () => {
    const approvedItem = { ...mockItem, status: "approved", visibility: "public" as const };
    render(<ItemDetail {...defaultProps} item={approvedItem} />);

    expect(screen.getByRole("button", { name: "Approve" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Reject" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Needs Edit" })).toBeDisabled();
  });

  it("shows MT label dimensions for MT type items", async () => {
    const user = userEvent.setup();
    const mtItem = { ...mockItem, type: "MT" as const, source: "import" as const };

    render(<ItemDetail {...defaultProps} item={mtItem} />);

    await user.click(screen.getByRole("button", { name: "Needs Edit" }));

    expect(screen.getByText("Fluency")).toBeInTheDocument();
    expect(screen.getByText("Adequacy")).toBeInTheDocument();
    expect(screen.getByText("Terminology")).toBeInTheDocument();
  });

  it("shows ingestion label dimensions for ingestion type items", async () => {
    const user = userEvent.setup();
    const ingestionItem = { ...mockItem, type: "ingestion" as const, source: "ingestion" as const };

    render(<ItemDetail {...defaultProps} item={ingestionItem} />);

    await user.click(screen.getByRole("button", { name: "Needs Edit" }));

    expect(screen.getByText("Quality")).toBeInTheDocument();
    expect(screen.getByText("Deduplication")).toBeInTheDocument();
    expect(screen.getByText("Formatting")).toBeInTheDocument();
  });
});
