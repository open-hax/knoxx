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
  metadata: { garden_publications: [{ garden_id: "garden-a" }] },
};

function installCmsFetchMock(doc = cmsDoc) {
  const requests: Array<{ url: string; init?: RequestInit }> = [];
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    requests.push({ url, init });

    if (url.startsWith("/api/ingestion/browse")) {
      return jsonResponse({ current_path: ".", entries: [] });
    }
    if (url === "/api/openplanner/v1/gardens") {
      return jsonResponse({ ok: true, gardens: [{ garden_id: "garden-a", title: "Garden A", status: "active" }] });
    }
    if (url === "/api/ingestion/sources") {
      return jsonResponse([{ source_id: "workspace", name: "devel workspace", config: { root_path: "/app/workspace/devel" } }]);
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
      metadata: { garden_publications: [{ garden_id: "garden-a" }] },
    });
    expect(screen.getByDisplayValue("Updated CMS body")).toBeInTheDocument();
  });

  it("publishes and unpublishes the selected CMS document for the selected garden", async () => {
    const unpublishedDoc = { ...cmsDoc, metadata: { garden_publications: [] } };
    const publishedHarness = installCmsFetchMock(unpublishedDoc);

    const { unmount } = renderCmsPage();

    fireEvent.click(await screen.findByRole("button", { name: "Publish" }));
    await screen.findByText("Published");
    expect(publishedHarness.requests.some((request) => (
      request.url === "/api/openplanner/v1/cms/publish/cms-doc-1/garden-a?skip_translation=true&defer_index=true"
      && request.init?.method === "POST"
    ))).toBe(true);

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
