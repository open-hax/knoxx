import { describe, it, expect, vi, beforeEach } from "vitest";
import { act, render, screen, waitFor } from "@testing-library/react";
import DocumentsPage from "./DocumentsPage";

describe("DocumentsPage (shadow-cljs loader)", () => {
  beforeEach(() => {
    (window as unknown as Record<string, unknown>).knoxx = undefined;
  });

  it("shows a loading state while waiting for the CLJS module", () => {
    render(<DocumentsPage />);
    expect(screen.getByText(/Loading data lakes/i)).toBeInTheDocument();
  });

  it("shows a loud integration error when the CLJS module never appears", async () => {
    vi.useFakeTimers();
    render(<DocumentsPage />);
    await act(async () => {
      vi.advanceTimersByTime(2000);
    });
    vi.useRealTimers();
    await waitFor(() => {
      expect(screen.getByText(/Data Lakes \(shadow-cljs\) failed to load/i)).toBeInTheDocument();
    });
  });

  it("mounts the CLJS component from the expected window.knoxx namespace path", () => {
    const mockComponent = vi.fn(() => <div data-testid="cljs-documents">CLJS Documents</div>);
    // This path MUST match knoxx.frontend.pages.documents.page/documents-page
    // as munged by shadow-cljs; the shim's getCljsComponent walks exactly this.
    (window as unknown as Record<string, unknown>).knoxx = {
      frontend: {
        pages: {
          documents: {
            page: {
              documents_page: mockComponent,
            },
          },
        },
      },
    };

    render(<DocumentsPage />);
    expect(screen.getByTestId("cljs-documents")).toBeInTheDocument();
    expect(mockComponent).toHaveBeenCalled();
  });
});
