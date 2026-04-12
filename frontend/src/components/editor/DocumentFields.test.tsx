/**
 * DocumentFields Tests
 */

import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { DocumentFields } from "./DocumentFields";
import { STATUS_CONFIG, VISIBILITY_CONFIG, MOCK_COLLECTIONS } from "./editor-types";

describe("DocumentFields", () => {
  const defaultProps = {
    collectionId: "col-1",
    collections: MOCK_COLLECTIONS,
    visibility: "internal" as const,
    status: "draft" as const,
    onCollectionChange: vi.fn(),
    onVisibilityChange: vi.fn(),
    statusConfig: STATUS_CONFIG,
    visibilityConfig: VISIBILITY_CONFIG,
  };

  it("renders status badge with correct label", () => {
    render(<DocumentFields {...defaultProps} />);

    expect(screen.getByText("Draft")).toBeInTheDocument();
  });

  it("renders collection selector with options", () => {
    render(<DocumentFields {...defaultProps} />);

    const select = screen.getByLabelText("Collection");
    expect(select).toBeInTheDocument();

    // Check all options are present
    expect(screen.getByRole("option", { name: "Documentation" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Blog Posts" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Internal Notes" })).toBeInTheDocument();
  });

  it("shows collection description for selected collection", () => {
    render(<DocumentFields {...defaultProps} />);

    expect(screen.getByText("Technical docs and guides")).toBeInTheDocument();
  });

  it("calls onCollectionChange when collection is changed", async () => {
    const user = userEvent.setup();
    const onCollectionChange = vi.fn();

    render(<DocumentFields {...defaultProps} onCollectionChange={onCollectionChange} />);

    const select = screen.getByLabelText("Collection");
    await user.selectOptions(select, "col-2");

    expect(onCollectionChange).toHaveBeenCalledWith("col-2");
  });

  it("renders visibility selector with options", () => {
    render(<DocumentFields {...defaultProps} />);

    const select = screen.getByLabelText("Visibility");
    expect(select).toBeInTheDocument();

    expect(screen.getByRole("option", { name: "Private" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Internal" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "Public" })).toBeInTheDocument();
  });

  it("shows visibility description for selected visibility", () => {
    render(<DocumentFields {...defaultProps} />);

    expect(screen.getByText("Team members only")).toBeInTheDocument();
  });

  it("calls onVisibilityChange when visibility is changed", async () => {
    const user = userEvent.setup();
    const onVisibilityChange = vi.fn();

    render(<DocumentFields {...defaultProps} onVisibilityChange={onVisibilityChange} />);

    const select = screen.getByLabelText("Visibility");
    await user.selectOptions(select, "public");

    expect(onVisibilityChange).toHaveBeenCalledWith("public");
  });

  it("shows review status with amber color", () => {
    render(<DocumentFields {...defaultProps} status="review" />);

    const badge = screen.getByText("In Review");
    expect(badge).toHaveStyle({ color: "var(--token-colors-accent-amber)" });
  });

  it("shows published status with green color", () => {
    render(<DocumentFields {...defaultProps} status="published" />);

    const badge = screen.getByText("Published");
    expect(badge).toHaveStyle({ color: "var(--token-colors-accent-green)" });
  });
});
