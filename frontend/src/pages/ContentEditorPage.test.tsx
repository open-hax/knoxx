/**
 * ContentEditorPage Tests
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { ContentEditorPage } from "./ContentEditorPage";

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

describe("ContentEditorPage", () => {
  beforeEach(() => {
    mockNavigate.mockClear();
  });

  it("renders document title as editable input", () => {
    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    const titleInput = screen.getByPlaceholderText("Untitled document");
    expect(titleInput).toHaveValue("Getting Started with Knowledge Ops");
  });

  it("renders body editor with document content", () => {
    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    const bodyEditor = screen.getByPlaceholderText("Start writing...");
    expect(bodyEditor).toBeInTheDocument();
    expect(bodyEditor.value).toContain("# Getting Started with Knowledge Ops");
  });

  it("shows unsaved changes indicator when title is edited", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    // Find title input by placeholder
    const titleInput = screen.getByPlaceholderText("Untitled document");
    await user.clear(titleInput);
    await user.type(titleInput, "New Title");

    expect(screen.getByText("Unsaved changes")).toBeInTheDocument();
  });

  it("shows unsaved changes indicator when body is edited", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    const bodyEditor = screen.getByPlaceholderText("Start writing...");
    await user.type(bodyEditor, "\n\nNew paragraph");

    expect(screen.getByText("Unsaved changes")).toBeInTheDocument();
  });

  it("disables save button when no changes", () => {
    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    const saveButton = screen.getByRole("button", { name: "Save" });
    expect(saveButton).toBeDisabled();
  });

  it("enables save button when there are changes", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    const titleInput = screen.getByPlaceholderText("Untitled document");
    await user.type(titleInput, "!");

    const saveButton = screen.getByRole("button", { name: "Save" });
    expect(saveButton).not.toBeDisabled();
  });

  it("shows 'Submit for Review' button for draft documents", () => {
    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    // Button is now in the PublishWorkflow component in the sidebar
    expect(screen.getByRole("button", { name: "Submit for Review" })).toBeInTheDocument();
  });

  it("updates status to review when Submit for Review is clicked", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    const submitButton = screen.getByRole("button", { name: "Submit for Review" });
    await user.click(submitButton);

    // Workflow should now show "Review" as active step
    expect(screen.getByText(/Ready for review/)).toBeInTheDocument();
    // Button should now say "Publish"
    expect(screen.getByRole("button", { name: "Publish" })).toBeInTheDocument();
  });

  it("shows confirmation dialog when Publish is clicked", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    // First submit for review
    await user.click(screen.getByRole("button", { name: "Submit for Review" }));

    // Then click publish - this should show the confirmation dialog
    await user.click(screen.getByRole("button", { name: "Publish" }));

    // Confirmation dialog should appear
    expect(screen.getByText("Confirm Publication")).toBeInTheDocument();
  });

  it("updates status to published when Publish is confirmed", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    // First submit for review
    await user.click(screen.getByRole("button", { name: "Submit for Review" }));

    // Click publish button in workflow
    await user.click(screen.getByRole("button", { name: "Publish" }));

    // Confirm publication in dialog - there are two "Publish" buttons now
    const publishButtons = screen.getAllByRole("button", { name: "Publish" });
    await user.click(publishButtons[publishButtons.length - 1]); // Use the last one (dialog)

    // Should show published status
    expect(screen.getByText("✓ Published and visible")).toBeInTheDocument();
  });

  it("cancels publish when Cancel is clicked in dialog", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    // First submit for review
    await user.click(screen.getByRole("button", { name: "Submit for Review" }));

    // Click publish button
    await user.click(screen.getByRole("button", { name: "Publish" }));

    // Cancel in dialog
    await user.click(screen.getByRole("button", { name: "Cancel" }));

    // Should still be in review state
    expect(screen.getByRole("button", { name: "Publish" })).toBeInTheDocument();
    expect(screen.queryByText("Confirm Publication")).not.toBeInTheDocument();
  });

  it("renders collection selector in sidebar", () => {
    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    const collectionSelect = screen.getByLabelText("Collection");
    expect(collectionSelect).toBeInTheDocument();
  });

  it("renders visibility selector in sidebar", () => {
    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    const visibilitySelect = screen.getByLabelText("Visibility");
    expect(visibilitySelect).toBeInTheDocument();
  });

  it("clears dirty indicator after save", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    // Make a change
    const titleInput = screen.getByPlaceholderText("Untitled document");
    await user.type(titleInput, "!");

    expect(screen.getByText("Unsaved changes")).toBeInTheDocument();

    // Save
    await user.click(screen.getByRole("button", { name: "Save" }));

    expect(screen.queryByText("Unsaved changes")).not.toBeInTheDocument();
  });
});
