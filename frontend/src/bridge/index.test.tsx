import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import React from "react";
import { EmptyState } from "../components/EmptyState";

describe("Bridge exports", () => {
  it("EmptyState renders with props", () => {
    render(
      <EmptyState
        title="Test Title"
        message="Test message"
        icon="🧪"
      />
    );

    expect(screen.getByText("Test Title")).toBeInTheDocument();
    expect(screen.getByText("Test message")).toBeInTheDocument();
    expect(screen.getByText("🧪")).toBeInTheDocument();
  });

  it("EmptyState renders with action", () => {
    const handleAction = vi.fn();
    render(
      <EmptyState
        title="No data"
        message="Add some data to get started"
        actionLabel="Add Data"
        onAction={handleAction}
      />
    );

    const button = screen.getByRole("button", { name: /Add Data/i });
    expect(button).toBeInTheDocument();
  });
});