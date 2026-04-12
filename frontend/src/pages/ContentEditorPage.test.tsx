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

    // Status should now be "In Review"
    expect(screen.getByText("In Review")).toBeInTheDocument();
    // Button should now say "Publish"
    expect(screen.getByRole("button", { name: "Publish" })).toBeInTheDocument();
  });

  it("updates status to published when Publish is clicked", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    // First submit for review
    const submitButton = screen.getByRole("button", { name: "Submit for Review" });
    await user.click(submitButton);

    // Then publish
    const publishButton = screen.getByRole("button", { name: "Publish" });
    await user.click(publishButton);

    // Status should now be "Published" - use getAllByText since it appears in multiple places
    const publishedElements = screen.getAllByText("Published");
    expect(publishedElements.length).toBeGreaterThan(0);
  });

  it("disables publish button when document is already published", async () => {
    const user = userEvent.setup();

    render(
      <RouterWrapper>
        <ContentEditorPage />
      </RouterWrapper>
    );

    // Submit for review
    await user.click(screen.getByRole("button", { name: "Submit for Review" }));

    // Publish
    await user.click(screen.getByRole("button", { name: "Publish" }));

    // Button should be disabled
    const publishedButton = screen.getByRole("button", { name: "Published" });
    expect(publishedButton).toBeDisabled();
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
