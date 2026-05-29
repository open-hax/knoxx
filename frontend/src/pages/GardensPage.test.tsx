import { fireEvent, render, screen } from "@testing-library/react";
import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import GardensPage from "./GardensPage";

vi.mock("@open-hax/uxx", () => ({
  Button: ({ children, loading, ...props }: { children: ReactNode; loading?: boolean } & ButtonHTMLAttributes<HTMLButtonElement>) => (
    <button {...props}>{loading ? "Loading…" : children}</button>
  ),
  Card: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  Input: (props: InputHTMLAttributes<HTMLInputElement>) => <input {...props} />,
}));

function jsonResponse(body: unknown, init: ResponseInit = {}) {
  return new Response(JSON.stringify(body), {
    status: init.status ?? 200,
    statusText: init.statusText,
    headers: { "Content-Type": "application/json", ...(init.headers ?? {}) },
  });
}

function gardensBody(gardens = [{
  garden_id: "fork-garden",
  title: "Fork Garden",
  description: "Existing garden",
  status: "active",
  theme: "monokai",
  target_languages: ["es"],
  auto_translate: true,
}]) {
  return { ok: true, count: gardens.length, gardens };
}

describe("GardensPage", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("creates a garden by posting the form payload and refreshing the list", async () => {
    const requests: Array<{ url: string; init?: RequestInit }> = [];
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      requests.push({ url, init });
      if (url === "/api/openplanner/v1/gardens" && !init?.method) return jsonResponse(gardensBody([]));
      if (url === "/api/openplanner/v1/gardens" && init?.method === "POST") return jsonResponse({ ok: true });
      return jsonResponse({ error: `Unexpected ${url}` }, { status: 404 });
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<GardensPage />);

    fireEvent.click(await screen.findByRole("button", { name: "+ New Garden" }));
    fireEvent.change(screen.getByPlaceholderText("my-garden-id"), { target: { value: "new-garden" } });
    fireEvent.change(screen.getByPlaceholderText("My Garden"), { target: { value: "New Garden" } });
    fireEvent.change(screen.getByPlaceholderText("Brief description of this garden"), { target: { value: "A test garden" } });
    fireEvent.click(screen.getByRole("button", { name: "Español" }));
    fireEvent.click(screen.getByRole("button", { name: "Create Garden" }));

    await screen.findByText('Garden "New Garden" created successfully');
    const createRequest = requests.find((request) => request.init?.method === "POST");
    expect(createRequest?.url).toBe("/api/openplanner/v1/gardens");
    expect(JSON.parse(String(createRequest?.init?.body))).toMatchObject({
      garden_id: "new-garden",
      title: "New Garden",
      description: "A test garden",
      theme: "monokai",
      target_languages: ["es"],
      auto_translate: true,
    });
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it("requires inline confirmation before deleting a garden and refreshes after DELETE", async () => {
    const requests: Array<{ url: string; init?: RequestInit }> = [];
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      requests.push({ url, init });
      if (url === "/api/openplanner/v1/gardens" && !init?.method) return jsonResponse(gardensBody());
      if (url === "/api/openplanner/v1/gardens/fork-garden" && init?.method === "DELETE") return jsonResponse({ ok: true });
      return jsonResponse({ error: `Unexpected ${url}` }, { status: 404 });
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<GardensPage />);

    await screen.findByText("Fork Garden");
    fireEvent.click(screen.getByRole("button", { name: "Delete" }));

    // No DELETE request should fire until the inline confirmation is clicked.
    fireEvent.click(await screen.findByRole("button", { name: "Confirm Delete" }));

    await screen.findByText('Garden "fork-garden" deleted');
    expect(requests.some((request) => request.url === "/api/openplanner/v1/gardens/fork-garden" && request.init?.method === "DELETE")).toBe(true);
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });
});
