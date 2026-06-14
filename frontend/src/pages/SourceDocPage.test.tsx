import { describe, it, expect, vi, beforeEach } from "vitest";
import { act, render, screen, waitFor } from "@testing-library/react";
import SourceDocPage from "./SourceDocPage";

describe("SourceDocPage (shadow-cljs loader)", () => {
  beforeEach(() => {
    (window as unknown as Record<string, unknown>).knoxx = undefined;
  });

  it("shows a loading state while waiting for the CLJS module", () => {
    render(<SourceDocPage />);
    expect(screen.getByText(/Loading document viewer/i)).toBeInTheDocument();
  });

  it("shows a loud integration error when the CLJS module never appears", async () => {
    vi.useFakeTimers();
    render(<SourceDocPage />);
    await act(async () => {
      vi.advanceTimersByTime(2000);
    });
    vi.useRealTimers();
    await waitFor(() => {
      expect(screen.getByText(/Document Viewer \(shadow-cljs\) failed to load/i)).toBeInTheDocument();
    });
  });

  it("mounts the CLJS component from the expected window.knoxx namespace path", () => {
    const mockComponent = vi.fn(() => <div data-testid="cljs-source-doc">CLJS Source Doc</div>);
    // This path MUST match knoxx.frontend.pages.source-doc.view/source-doc-page
    // as munged by shadow-cljs; the shim's getCljsComponent walks exactly this.
    (window as unknown as Record<string, unknown>).knoxx = {
      frontend: {
        pages: {
          source_doc: {
            view: {
              source_doc_page: mockComponent,
            },
          },
        },
      },
    };

    render(<SourceDocPage />);
    expect(screen.getByTestId("cljs-source-doc")).toBeInTheDocument();
    expect(mockComponent).toHaveBeenCalled();
  });
});
