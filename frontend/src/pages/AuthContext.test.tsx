import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";

import AuthBoundary from "./AuthContext";

function jsonResponse(body: unknown, init: ResponseInit = {}) {
  return new Response(JSON.stringify(body), {
    status: init.status ?? 200,
    statusText: init.statusText,
    headers: { "Content-Type": "application/json", ...(init.headers ?? {}) },
  });
}

function authContextBody() {
  return {
    actor: { id: "chat_primary" },
    user: {
      id: "user-1",
      email: "system-admin@open-hax.local",
      displayName: "System Admin",
      status: "active",
    },
    org: { id: "org-1", slug: "open-hax", name: "Open Hax", isPrimary: true },
    membership: { id: "membership-1", actorId: "chat_primary", status: "active", isDefault: true },
    roleSlugs: ["system-admin"],
    permissions: ["agent.chat.use"],
    isSystemAdmin: true,
    authProvider: "test",
  };
}

afterEach(() => {
  vi.restoreAllMocks();
  window.history.pushState({}, "", "/");
});

describe("AuthBoundary", () => {
  it("renders the login surface instead of protected content when auth context returns 401", async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.endsWith("/api/auth/context")) {
        return jsonResponse({ error: "Not authenticated" }, { status: 401, statusText: "Unauthorized" });
      }
      if (url.endsWith("/api/auth/config")) {
        return jsonResponse({ githubEnabled: false, loginUrl: null });
      }
      return jsonResponse({ error: `Unexpected ${url}` }, { status: 404 });
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<AuthBoundary><div>Protected Knoxx workspace</div></AuthBoundary>);

    expect(await screen.findByText("Knowledge operations platform")).toBeInTheDocument();
    expect(screen.getByText("GitHub OAuth is not configured. Contact your administrator.")).toBeInTheDocument();
    expect(screen.queryByText("Protected Knoxx workspace")).not.toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledWith("/api/auth/context", expect.objectContaining({ credentials: "include" }));
  });

  it("refreshes auth context after invite redemption succeeds", async () => {
    const requests: Array<{ url: string; init?: RequestInit }> = [];
    let contextCalls = 0;
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      requests.push({ url, init });
      if (url.endsWith("/api/auth/context")) {
        contextCalls += 1;
        if (contextCalls === 1) {
          return jsonResponse({ error: "Not authenticated" }, { status: 401, statusText: "Unauthorized" });
        }
        return jsonResponse(authContextBody());
      }
      if (url.endsWith("/api/auth/config")) {
        return jsonResponse({ githubEnabled: false, loginUrl: null });
      }
      if (url.endsWith("/api/auth/invite/redeem")) {
        return jsonResponse({ ok: true });
      }
      return jsonResponse({ error: `Unexpected ${url}` }, { status: 404 });
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<AuthBoundary><div>Protected Knoxx workspace</div></AuthBoundary>);

    fireEvent.change(await screen.findByLabelText("Email"), { target: { value: "system-admin@open-hax.local" } });
    fireEvent.change(screen.getByLabelText("Invite code"), { target: { value: "invite-123" } });
    fireEvent.click(screen.getByRole("button", { name: "Redeem invite" }));

    expect(await screen.findByText("Invite accepted! Redirecting…")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText("Protected Knoxx workspace")).toBeInTheDocument(), { timeout: 1500 });

    const redeemRequest = requests.find((request) => request.url.endsWith("/api/auth/invite/redeem"));
    expect(redeemRequest?.init).toMatchObject({ method: "POST", credentials: "include" });
    expect(JSON.parse(String(redeemRequest?.init?.body))).toEqual({
      code: "invite-123",
      email: "system-admin@open-hax.local",
    });
    expect(contextCalls).toBe(2);
  });
});
