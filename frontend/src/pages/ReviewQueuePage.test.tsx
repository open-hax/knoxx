/**
 * ReviewQueuePage Tests
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { ReviewQueuePage } from "./ReviewQueuePage";
import type { ReviewItem, ReviewStats } from "../components/review/review-types";

// Mock useNavigate
const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// Mock useReviewQueue hook
const mockApprove = vi.fn();
const mockReject = vi.fn();
const mockFlag = vi.fn();
const mockBatchAction = vi.fn();
const mockRefresh = vi.fn();

const MOCK_STATS: ReviewStats = {
  pending: 5,
  flagged: 0,
  approved_today: 0,
  rejected_today: 0,
};

const MOCK_REVIEW_ITEMS: ReviewItem[] = [
  {
    doc_id: "rev-1",
    tenant_id: "devel",
    title: "API Documentation Summary",
    content_preview: "Generated summary of REST API endpoints for v2 release.",
    visibility: "review",
    source: "ai-drafted",
    ai_drafted: true,
    confidence: 0.72,
    created_at: "2024-01-15T14:00:00Z",
    updated_at: "2024-01-15T14:00:00Z",
    source_count: 12,
    agent_name: "synthesizer-v3",
  },
  {
    doc_id: "rev-2",
    tenant_id: "devel",
    title: "German Translation: Getting Started",
    content_preview: "Machine translation of onboarding guide to German.",
    visibility: "review",
    source: "import",
    ai_drafted: false,
    confidence: 0.45,
    created_at: "2024-01-15T13:30:00Z",
    updated_at: "2024-01-15T13:30:00Z",
    source_count: 1,
    agent_name: "mt-de-v2",
  },
  {
    doc_id: "rev-3",
    tenant_id: "devel",
    title: "Changelog Extraction",
    content_preview: "Extracted changelog entries from commit history.",
    visibility: "review",
    source: "ingestion",
    ai_drafted: false,
    confidence: 0.89,
    created_at: "2024-01-15T12:00:00Z",
    updated_at: "2024-01-15T12:00:00Z",
    source_count: 48,
    agent_name: null,
  },
  {
    doc_id: "rev-4",
    tenant_id: "devel",
    title: "Architecture Decision Record",
    content_preview: "Generated ADR for database migration strategy.",
    visibility: "review",
    source: "ai-drafted",
    ai_drafted: true,
    confidence: 0.38,
    created_at: "2024-01-15T11:00:00Z",
    updated_at: "2024-01-15T11:00:00Z",
    source_count: 8,
    agent_name: "synthesizer-v3",
  },
  {
    doc_id: "rev-5",
    tenant_id: "devel",
    title: "Japanese Translation: API Reference",
    content_preview: "Machine translation of API reference to Japanese.",
    visibility: "review",
    source: "import",
    ai_drafted: false,
    confidence: 0.61,
    created_at: "2024-01-15T10:00:00Z",
    updated_at: "2024-01-15T10:00:00Z",
    source_count: 1,
    agent_name: "mt-ja-v2",
  },
];

vi.mock("../components/review/useReviewQueue", () => ({
  useReviewQueue: () => ({
    items: MOCK_REVIEW_ITEMS,
    stats: MOCK_STATS,
    loading: false,
    error: null,
    approve: mockApprove,
    reject: mockReject,
    flag: mockFlag,
    batchAction: mockBatchAction,
    refresh: mockRefresh,
  }),
  getItemStatus: (item: ReviewItem) => {
    if (item.visibility === "public") return "approved";
    if (item.visibility === "internal") return "rejected";
    return "pending";
  },
}));

// Wrapper for router
const RouterWrapper = ({ children }: { children: React.ReactNode }) => (
  <BrowserRouter>{children}</BrowserRouter>
);

describe("ReviewQueuePage", () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    mockApprove.mockClear();
    mockReject.mockClear();
    mockFlag.mockClear();
    mockBatchAction.mockClear();
    mockRefresh.mockClear();
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

    expect(screen.getByRole("option", { name: /Approve all pending/ })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: /Reject all pending/ })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: /Flag for review/ })).toBeInTheDocument();
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

  it("calls approve API when Approve button is clicked", async () => {
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

    expect(mockApprove).toHaveBeenCalledWith("rev-3");
  });

  it("calls reject API when Reject button is clicked", async () => {
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

    expect(mockReject).toHaveBeenCalledWith("rev-3");
  });

  it("calls flag API when Flag button is clicked", async () => {
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

    expect(mockFlag).toHaveBeenCalledWith("rev-3");
  });

  it("calls batch approve API for approve all action", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ReviewQueuePage />
      </RouterWrapper>
    );

    const batchSelect = screen.getByRole("combobox");
    await user.selectOptions(batchSelect, "approve-all");

    // Verify batchAction was called with approve action and all doc IDs
    expect(mockBatchAction).toHaveBeenCalledTimes(1);
    const callArgs = mockBatchAction.mock.calls[0];
    expect(callArgs[0]).toBe("approve");
    expect(callArgs[1]).toHaveLength(5);
    expect(callArgs[1]).toEqual(expect.arrayContaining(["rev-1", "rev-2", "rev-3", "rev-4", "rev-5"]));
  });

  it("calls batch reject API for reject all action", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ReviewQueuePage />
      </RouterWrapper>
    );

    const batchSelect = screen.getByRole("combobox");
    await user.selectOptions(batchSelect, "reject-all");

    expect(mockBatchAction).toHaveBeenCalled();
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
