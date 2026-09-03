import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import CmsPage from "./CmsPage";

const mockListMemorySessions = vi.fn();
const mockPinContextItem = vi.fn();
const mockUnpinContextItem = vi.fn();
const mockResumeMemorySession = vi.fn();

vi.mock("@open-hax/uxx", () => ({
  Badge: ({ children }: { children: ReactNode }) => <span>{children}</span>,
  Button: ({ children, loading, ...props }: { children: ReactNode; loading?: boolean } & ButtonHTMLAttributes<HTMLButtonElement>) => (
    <button {...props}>{loading ? "Loading…" : children}</button>
  ),
}));

vi.mock("react-markdown", () => ({
  default: ({ children }: { children: ReactNode }) => <div data-testid="markdown-preview">{children}</div>,
}));

vi.mock("../components/context-bar", () => ({
  ContextBar: ({ onNewDocument }: { onNewDocument?: () => void }) => (
    <aside data-testid="context-bar">
      <button onClick={onNewDocument}>New document</button>
    </aside>
  ),
}));

vi.mock("../components/chat-page/ChatWorkspacePane", () => ({
  ChatWorkspacePane: () => <aside data-testid="cms-chat-pane" />,
}));

vi.mock("../components/chat-page/useChatWorkspaceController", () => ({
  useChatWorkspaceController: () => ({
    pinnedContext: [],
    pinContextItem: mockPinContextItem,
    unpinContextItem: mockUnpinContextItem,
    pinSemanticResult: vi.fn(),
    resumeMemorySession: mockResumeMemorySession,
    openSourceInPreview: vi.fn(),
  }),
}));

vi.mock("../components/chat-page/sidebar-resize", () => ({
  createSidebarResizeHandlers: () => ({
    startSidebarPaneResize: vi.fn(),
    startSidebarWidthResize: vi.fn(),
  }),
}));

vi.mock("../components/CollapsedPanelTab", () => ({
  CollapsedPanelTab: ({ label }: { label: string }) => <button>{label}</button>,
}));

vi.mock("../components/cms/PublicationBlocksRenderer", () => ({
  PublicationBlocksRenderer: () => <div data-testid="publication-blocks" />,
  extractPublicationBlocks: () => [],
}));

vi.mock("../components/cms/CreateVisualDraftModal", () => ({
  CreateVisualDraftModal: () => null,
}));

vi.mock("../lib/api/common", () => ({
  listMemorySessions: (...args: unknown[]) => mockListMemorySessions(...args),
}));

function jsonResponse(body: unknown, init: ResponseInit = {}) {
  return new Response(JSON.stringify(body), {
    status: init.status ?? 200,
    statusText: init.statusText,
    headers: { "Content-Type": "application/json", ...(init.headers ?? {}) },
  });
}

const cmsDoc = {
  doc_id: "cms-doc-1",
  title: "Existing CMS Doc",
  content: "Initial CMS body",
  source_path: "docs/existing.md",
  visibility: "internal",
  // No `garden_publications`. Publication state is no longer read from document
  // metadata — it comes from the resource-backed publication topology.
  metadata: {},
};

function publicationTopology(desired: "published" | "withheld") {
  return {
    documents: [
      {
        document: {
          id: "knoxx.docs/existing",
          title: "Existing CMS Doc",
          "source-locale": "en",
          source: { path: "docs/existing.md" },
        },
        publications: [
          {
            id: "knoxx.docs/existing-en",
            document: "knoxx.docs/existing",
            garden: "garden-a",
            locale: "en",
            revision: "source/current",
            path: "/existing",
            desired,
            observed: desired === "published" ? "abc123" : null,
            blockers: [],
          },
        ],
      },
    ],
    gardens: [{ id: "garden-a", title: "Garden A", status: "active" }],
  };
}

function installCmsFetchMock(doc = cmsDoc, initialDesired: "published" | "withheld" = "published") {
  const requests: Array<{ url: string; init?: RequestInit }> = [];
  // Stateful on purpose: a PATCH changes the resource, and the page re-reads the
  // topology rather than predicting the new state locally. A fixed-response mock
  // would let a page that never re-read still pass.
  let desired = initialDesired;
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    requests.push({ url, init });

    if (url.startsWith("/api/ingestion/browse")) {
      return jsonResponse({ current_path: ".", entries: [] });
    }
    if (url === "/api/cms/publications/documents") {
      return jsonResponse(publicationTopology(desired));
    }
    if (url.startsWith("/api/cms/publications/intents/")) {
      const patched = JSON.parse(String(init?.body ?? "{}")) as { state?: string };
      if (patched.state === "published" || patched.state === "withheld") {
        desired = patched.state;
      }
      return jsonResponse(publicationTopology(desired).documents[0].publications[0]);
    }
    if (url === "/api/ingestion/sources") {
      return jsonResponse([{ source_id: "workspace", name: "workspace", config: { root_path: "/app/workspace", workspace_source: true } }]);
    }
    if (url.startsWith("/api/ingestion/jobs")) {
      return jsonResponse([]);
    }
    if (url === "/api/ingestion/file?path=contracts/cms-templates.edn") {
      return jsonResponse({ content: ':article-page {:label "Article"}' });
    }
    if (url.startsWith("/api/openplanner/v1/cms/documents?")) {
      return jsonResponse({ documents: [doc], total: 1 });
    }
    if (url === "/api/openplanner/v1/cms/documents/cms-doc-1" && init?.method === "PATCH") {
      return jsonResponse({ ...doc, ...(JSON.parse(String(init.body)) as Record<string, unknown>) });
    }
    if (url === "/api/openplanner/v1/cms/documents/cms-doc-1") {
      return jsonResponse(doc);
    }
    if (url.startsWith("/api/openplanner/v1/cms/publish/cms-doc-1/garden-a")) {
      return jsonResponse({ ok: true });
    }

    return jsonResponse({ error: `Unexpected ${url}` }, { status: 404 });
  });
  vi.stubGlobal("fetch", fetchMock);
  return { fetchMock, requests };
}

function renderCmsPage(initialEntry = "/cms?doc=cms-doc-1") {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <CmsPage />
    </MemoryRouter>,
  );
}

describe("CmsPage CMS document backend interactions", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    vi.clearAllMocks();
    localStorage.clear();
    mockListMemorySessions.mockResolvedValue({ rows: [], total: 0, has_more: false });
  });

  it("loads CMS document list and hydrates the selected CMS document into the editor", async () => {
    installCmsFetchMock();

    renderCmsPage();

    expect(await screen.findByDisplayValue("Existing CMS Doc")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Initial CMS body")).toBeInTheDocument();
    expect(screen.getByText("Garden CMS documents")).toBeInTheDocument();
    expect(await screen.findByRole("button", { name: /Existing CMS Doc/ })).toBeInTheDocument();
    expect(mockPinContextItem).toHaveBeenCalledWith(expect.objectContaining({
      id: "docs/existing.md",
      path: "docs/existing.md",
      title: "Existing CMS Doc",
    }));
  });

  it("saves an existing CMS document with PATCH and preserves the selected doc id", async () => {
    const { requests } = installCmsFetchMock();

    renderCmsPage();

    const bodyEditor = await screen.findByDisplayValue("Initial CMS body");
    fireEvent.change(bodyEditor, { target: { value: "Updated CMS body" } });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await screen.findByText("Saved CMS draft");
    const patchRequest = requests.find((request) => request.url === "/api/openplanner/v1/cms/documents/cms-doc-1" && request.init?.method === "PATCH");
    expect(patchRequest).toBeTruthy();
    expect(JSON.parse(String(patchRequest?.init?.body))).toMatchObject({
      title: "Existing CMS Doc",
      content: "Updated CMS body",
      source_path: "docs/existing.md",
      visibility: "public",
    });
    expect(screen.getByDisplayValue("Updated CMS body")).toBeInTheDocument();
  });

  it("reads publication state from the resource topology, not from the legacy surface", async () => {
    const harness = installCmsFetchMock(cmsDoc, "published");
    renderCmsPage();

    // Badge state comes from the topology's `desired`, and the fixture document
    // carries NO garden_publications metadata at all — so if the page still read
    // metadata for publication state, this badge could not say "Published".
    await screen.findByText("Published");
    expect(cmsDoc.metadata).toEqual({});

    await waitFor(() => expect(harness.requests.some((request) => (
      request.url === "/api/cms/publications/documents"
    ))).toBe(true));

    // The legacy garden surface is never consulted.
    expect(harness.requests.some((request) => (
      request.url === "/api/openplanner/v1/gardens"
    ))).toBe(false);
  });

  it("publishes and unpublishes through the publication intent resource", async () => {
    const publishedHarness = installCmsFetchMock(cmsDoc, "withheld");

    const { unmount } = renderCmsPage();

    fireEvent.click(await screen.findByRole("button", { name: "Publish" }));
    await screen.findByText("Published");

    // The semantic result is a state change on the publication RESOURCE, not a
    // write into document metadata.
    const patchIntent = publishedHarness.requests.find((request) => (
      request.url.startsWith("/api/cms/publications/intents/")
      && request.init?.method === "PATCH"
    ));
    expect(patchIntent).toBeTruthy();
    expect(JSON.parse(String(patchIntent?.init?.body))).toEqual({ state: "published" });
    // Identity must not travel in a state edit.
    const patchedBody = JSON.parse(String(patchIntent?.init?.body));
    for (const identityField of ["document", "garden", "locale", "revision"]) {
      expect(patchedBody).not.toHaveProperty(identityField);
    }

    unmount();
    const unpublishedHarness = installCmsFetchMock(cmsDoc);
    renderCmsPage();

    fireEvent.click(await screen.findByRole("button", { name: "Unpublish" }));
    await waitFor(() => expect(unpublishedHarness.requests.some((request) => (
      request.url === "/api/openplanner/v1/cms/publish/cms-doc-1/garden-a"
      && request.init?.method === "DELETE"
    ))).toBe(true));
    expect(await screen.findByRole("button", { name: "Publish" })).toBeInTheDocument();
  });
});
