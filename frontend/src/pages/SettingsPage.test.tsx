import { describe, it, expect, vi, beforeEach } from "vitest";
import { act, render, screen, waitFor } from "@testing-library/react";
import SettingsPage from "./SettingsPage";

describe("SettingsPage (shadow-cljs loader)", () => {
  beforeEach(() => {
    (window as unknown as Record<string, unknown>).knoxx = undefined;
  });

  it("shows a loading state while waiting for the CLJS module", () => {
    render(<SettingsPage />);
    expect(screen.getByText(/Loading settings/i)).toBeInTheDocument();
  });

  it("shows a loud integration error when the CLJS module never appears", async () => {
    vi.useFakeTimers();
    render(<SettingsPage />);
    await act(async () => {
      vi.advanceTimersByTime(2000);
    });
    vi.useRealTimers();
    await waitFor(() => {
      expect(screen.getByText(/Settings \(shadow-cljs\) failed to load/i)).toBeInTheDocument();
    });
  });

  it("mounts the CLJS component from the expected window.knoxx namespace path", () => {
    const mockComponent = vi.fn(() => <div data-testid="cljs-settings">CLJS Settings</div>);
    // This path MUST match knoxx.frontend.pages.settings.view/settings-page
    // as munged by shadow-cljs; the shim's getCljsComponent walks exactly this.
    (window as unknown as Record<string, unknown>).knoxx = {
      frontend: {
        pages: {
          settings: {
            view: {
              settings_page: mockComponent,
            },
          },
        },
      },
    };

    render(<SettingsPage />);
    expect(screen.getByTestId("cljs-settings")).toBeInTheDocument();
    expect(mockComponent).toHaveBeenCalled();
  });
});
