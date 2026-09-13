import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { MemoryRouter, useNavigate } from "react-router-dom";
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
  revision: "revision-1",
  revision_heads: ["revision-1"],
  conflicted: false,
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

function installCmsFetchMock(
  doc = cmsDoc,
  initialDesired: "published" | "withheld" = "published",
  respond?: (url: string, init?: RequestInit) => Response | Promise<Response> | undefined,
) {
  const requests: Array<{ url: string; init?: RequestInit }> = [];
  // Stateful on purpose: a PATCH changes the resource, and the page re-reads the
  // topology rather than predicting the new state locally. A fixed-response mock
  // would let a page that never re-read still pass.
  let desired = initialDesired;
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    requests.push({ url, init });
    const overridden = await respond?.(url, init);
    if (overridden) return overridden;

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
    if (url.startsWith("/api/cms/documents?")) {
      return jsonResponse({ documents: [doc], total: 1 });
    }
    if (url === "/api/cms/documents/cms-doc-1" && init?.method === "PATCH") {
      return jsonResponse({ ...doc, ...(JSON.parse(String(init.body)) as Record<string, unknown>) });
    }
    if (url === "/api/cms/documents/cms-doc-1") {
      return jsonResponse(doc);
    }


    return jsonResponse({ error: `Unexpected ${url}` }, { status: 404 });
  });
  vi.stubGlobal("fetch", fetchMock);
  return { fetchMock, requests };
}

function TestNavigation() {
  const navigate = useNavigate();
  return <button onClick={() => navigate("/cms?doc=other-doc")}>Navigate to another document</button>;
}

function renderCmsPage(initialEntry = "/cms?doc=cms-doc-1", navigation = false) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <CmsPage />
      {navigation ? <TestNavigation /> : null}
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
    const patchRequest = requests.find((request) => request.url === "/api/cms/documents/cms-doc-1" && request.init?.method === "PATCH");
    expect(patchRequest).toBeTruthy();
    expect(JSON.parse(String(patchRequest?.init?.body))).toMatchObject({
      title: "Existing CMS Doc",
      content: "Updated CMS body",
      source_path: "docs/existing.md",
      visibility: "review",
      parents: ["revision-1"],
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

    expect(publishedHarness.requests.some((request) => request.url.startsWith("/api/openplanner/v1/cms/publish"))).toBe(false);
    unmount();
    const unpublishedHarness = installCmsFetchMock(cmsDoc);
    renderCmsPage();

    fireEvent.click(await screen.findByRole("button", { name: "Unpublish" }));
    await waitFor(() => expect(unpublishedHarness.requests.some((request) => (
      request.url.startsWith("/api/cms/publications/intents/")
      && request.init?.method === "PATCH"
      && JSON.parse(String(request.init.body)).state === "withheld"
    ))).toBe(true));
    expect(await screen.findByRole("button", { name: "Publish" })).toBeInTheDocument();
  });

  it("keeps the editor's observed parent when a document-list refresh sees another writer", async () => {
    let otherWriterSaved = false;
    const harness = installCmsFetchMock(cmsDoc, "withheld", (url) => {
      if (url.startsWith("/api/cms/documents?") && otherWriterSaved) {
        return jsonResponse({ documents: [{ ...cmsDoc, content: "Other writer's edit", revision: "revision-2", revision_heads: ["revision-2"] }] });
      }
    });
    renderCmsPage();
    const editor = await screen.findByRole("textbox", { name: "Document content" });
    fireEvent.change(editor, { target: { value: "My concurrent edit" } });
    otherWriterSaved = true;
    fireEvent.click(screen.getByRole("button", { name: "Refresh" }));
    await waitFor(() => expect(screen.getByRole("button", { name: "Refresh" })).toBeEnabled());
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await screen.findByText("Saved CMS draft");
    const patch = harness.requests.find(({ init }) => init?.method === "PATCH");
    expect(JSON.parse(String(patch?.init?.body))).toMatchObject({ content: "My concurrent edit", parents: ["revision-1"] });
  });

  it("preserves unsaved content and its original parent after a failed append", async () => {
    let failAppend = true;
    const errorLog = vi.spyOn(console, "error").mockImplementation(() => undefined);
    const harness = installCmsFetchMock(cmsDoc, "withheld", (url, init) => {
      if (url === "/api/cms/documents/cms-doc-1" && init?.method === "PATCH" && failAppend) {
        return new Response("Ledger temporarily unavailable", { status: 500 });
      }
    });
    renderCmsPage();
    const editor = await screen.findByRole("textbox", { name: "Document content" });
    fireEvent.change(editor, { target: { value: "Keep my unsaved revision" } });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await screen.findByText("Ledger temporarily unavailable");
    expect(editor).toHaveValue("Keep my unsaved revision");
    expect(screen.getByText("Unsaved changes")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Save" })).toBeEnabled();
    failAppend = false;
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await screen.findByText("Saved CMS draft");
    const patches = harness.requests.filter(({ init }) => init?.method === "PATCH");
    expect(patches).toHaveLength(2);
    for (const patch of patches) {
      expect(JSON.parse(String(patch.init?.body))).toMatchObject({ parents: ["revision-1"], content: "Keep my unsaved revision" });
    }
    errorLog.mockRestore();
  });

  it("shows complete revision history and joins only explicitly reviewed current versions", async () => {
    const heads = ["revision-2", "revision-3"];
    const revisions = [
      { ...cmsDoc, revision: "revision-2", content: "First writer's full Markdown", actor: "alice", at: "2026-09-13T10:00:00.000Z", parents: ["revision-1"] },
      { ...cmsDoc, revision: "revision-3", content: "Second writer's full Markdown", actor: "bob", at: "2026-09-13T10:00:00.000Z", parents: ["revision-1"] },
    ];
    const harness = installCmsFetchMock(cmsDoc, "withheld", (url, init) => {
      if (url.endsWith("/history")) return jsonResponse({ document: { ...cmsDoc, revision: "revision-2", revision_heads: heads, conflicted: true }, revisions });
      if (url === "/api/cms/documents/cms-doc-1" && init?.method === "PATCH") {
        return jsonResponse({ ...cmsDoc, ...JSON.parse(String(init.body)), revision: "revision-4", revision_heads: ["revision-4"], conflicted: false });
      }
    });
    renderCmsPage();
    const editor = await screen.findByRole("textbox", { name: "Document content" });
    fireEvent.change(editor, { target: { value: "Combined both writers' ideas" } });
    fireEvent.click(screen.getByRole("button", { name: "History" }));
    const version = await screen.findByRole("combobox", { name: "History version" });
    const resolve = screen.getByRole("button", { name: "Save resolution from editor" });
    expect(editor).toHaveValue("Combined both writers' ideas");
    expect(screen.getByRole("button", { name: "Publish" })).toBeDisabled();
    expect(resolve).toBeDisabled();
    fireEvent.change(version, { target: { value: "revision-2" } });
    expect(screen.getByText("First writer's full Markdown")).toBeInTheDocument();
    expect(screen.getByText("alice ·")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Mark this version reviewed" }));
    expect(resolve).toBeDisabled();
    fireEvent.change(version, { target: { value: "revision-3" } });
    expect(screen.getByText("Second writer's full Markdown")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Mark this version reviewed" }));
    expect(resolve).toBeEnabled();
    fireEvent.click(resolve);
    await screen.findByText("Saved CMS draft");
    const patch = harness.requests.find(({ init }) => init?.method === "PATCH");
    expect(JSON.parse(String(patch?.init?.body))).toMatchObject({ content: "Combined both writers' ideas", parents: heads });
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("ignores a late history response after the user opens a different draft", async () => {
    let finishHistory: (response: Response) => void = () => undefined;
    installCmsFetchMock(cmsDoc, "withheld", (url) => {
      if (url.endsWith("/history")) return new Promise<Response>((resolve) => { finishHistory = resolve; });
    });
    renderCmsPage();
    await screen.findByRole("textbox", { name: "Document content" });
    fireEvent.click(screen.getByRole("button", { name: "History" }));
    await screen.findByText("Loading history…");
    fireEvent.click(screen.getByRole("button", { name: "New document" }));
    await act(async () => finishHistory(jsonResponse({
      document: { ...cmsDoc, conflicted: true, revision_heads: ["old-head-a", "old-head-b"] },
      revisions: [],
    })));
    expect(screen.getByRole("textbox", { name: "Document content" })).toHaveValue("");
    expect(screen.queryByRole("region", { name: "Document history" })).not.toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("does not replace intervening typing with an older document-load response", async () => {
    let deferReload = false;
    let finishReload: (response: Response) => void = () => undefined;
    const harness = installCmsFetchMock(cmsDoc, "withheld", (url, init) => {
      if (url === "/api/cms/documents/cms-doc-1" && !init?.method && deferReload) {
        return new Promise<Response>((resolve) => { finishReload = resolve; });
      }
    });
    renderCmsPage();
    const editor = await screen.findByRole("textbox", { name: "Document content" });
    deferReload = true;
    fireEvent.click(await screen.findByRole("button", { name: /Existing CMS Doc/ }));
    fireEvent.change(editor, { target: { value: "Typing while the reload was pending" } });
    await act(async () => finishReload(jsonResponse({ ...cmsDoc, content: "New remote body", revision: "revision-2" })));
    expect(editor).toHaveValue("Typing while the reload was pending");
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await screen.findByText("Saved CMS draft");
    const patch = harness.requests.find(({ init }) => init?.method === "PATCH");
    expect(JSON.parse(String(patch?.init?.body))).toMatchObject({ parents: ["revision-1"], content: "Typing while the reload was pending" });
  });

  it("keeps the newly navigated document when a previous document's save finishes", async () => {
    let finishSave: (response: Response) => void = () => undefined;
    const other = { ...cmsDoc, doc_id: "other-doc", title: "Other document", content: "Other document body", revision: "other-revision", source_path: "other.md" };
    installCmsFetchMock(cmsDoc, "withheld", (url, init) => {
      if (url === "/api/cms/documents/cms-doc-1" && init?.method === "PATCH") {
        return new Promise<Response>((resolve) => { finishSave = resolve; });
      }
      if (url === "/api/cms/documents/other-doc") return jsonResponse(other);
    });
    renderCmsPage("/cms?doc=cms-doc-1", true);
    const editor = await screen.findByRole("textbox", { name: "Document content" });
    fireEvent.change(editor, { target: { value: "Saved on the previous document" } });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await screen.findByRole("button", { name: "Saving…" });
    fireEvent.click(screen.getByRole("button", { name: "Navigate to another document" }));
    await screen.findByDisplayValue("Other document body");
    await act(async () => finishSave(jsonResponse({ ...cmsDoc, content: "Saved on the previous document", revision: "saved-revision" })));
    expect(editor).toHaveValue("Other document body");
    expect(screen.getByRole("textbox", { name: "Document title" })).toHaveValue("Other document");
    expect(screen.queryByText("Saved CMS draft")).not.toBeInTheDocument();
  });

  it("loads the current ledger body and revision together when a URL names an older source path", async () => {
    const current = { ...cmsDoc, source_path: ".ημ/snapshots/new/content.md", source_paths: ["docs/old.md"], content: "Current ledger content", revision: "revision-7", revision_heads: ["revision-7"] };
    const harness = installCmsFetchMock(current, "withheld");
    renderCmsPage("/cms?path=docs%2Fold.md");
    const editor = await screen.findByRole("textbox", { name: "Document content" });
    await waitFor(() => expect(editor).toHaveValue("Current ledger content"));
    fireEvent.change(editor, { target: { value: "Edit based on current content" } });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await screen.findByText("Saved CMS draft");
    const patch = harness.requests.find(({ init }) => init?.method === "PATCH");
    expect(JSON.parse(String(patch?.init?.body))).toMatchObject({ parents: ["revision-7"], content: "Edit based on current content" });
    expect(harness.requests.some(({ url }) => url.startsWith("/api/ingestion/file?path=docs"))).toBe(false);
  });

  it("publishes a new document through the intent created by its save and writes content only once", async () => {
    let created = false;
    let published = false;
    const snapshot = ".ημ/snapshots/created/content.md";
    const harness = installCmsFetchMock(cmsDoc, "withheld", (url, init) => {
      if (url === "/api/cms/publications/documents") {
        const topology = publicationTopology(published ? "published" : "withheld");
        topology.documents[0].document.source.path = snapshot;
        return jsonResponse({ ...topology, documents: created ? topology.documents : [] });
      }
      if (url.startsWith("/api/cms/documents?")) return jsonResponse({ documents: [] });
      if (url === "/api/cms/documents" && init?.method === "POST") {
        created = true;
        return jsonResponse({ ...cmsDoc, ...JSON.parse(String(init.body)), source_path: snapshot });
      }
      if (url.startsWith("/api/cms/publications/intents/") && init?.method === "PATCH") {
        published = true;
        return jsonResponse(publicationTopology("published").documents[0].publications[0]);
      }
    });
    renderCmsPage("/cms");
    await screen.findByRole("option", { name: "Garden A" });
    fireEvent.click(screen.getByRole("button", { name: "New document" }));
    fireEvent.change(screen.getByRole("textbox", { name: "Document title" }), { target: { value: "New article" } });
    fireEvent.change(screen.getByRole("textbox", { name: "Document content" }), { target: { value: "New article body" } });
    fireEvent.click(screen.getByRole("button", { name: "Publish" }));
    await screen.findByText("Publication requested");
    expect(harness.requests.filter(({ url, init }) => url === "/api/cms/documents" && init?.method === "POST")).toHaveLength(1);
    expect(harness.requests.filter(({ url, init }) => url.startsWith("/api/cms/documents/") && init?.method === "PATCH")).toHaveLength(0);
    const create = harness.requests.find(({ init }) => init?.method === "POST");
    expect(JSON.parse(String(create?.init?.body))).toMatchObject({ parents: [], content: "New article body" });
    expect(published).toBe(true);
  });

  it("does not report a publication success when the saved document has no matching intent", async () => {
    const errorLog = vi.spyOn(console, "error").mockImplementation(() => undefined);
    const harness = installCmsFetchMock(cmsDoc, "withheld", (url) => {
      if (url === "/api/cms/publications/documents") return jsonResponse({ ...publicationTopology("withheld"), documents: [] });
    });
    renderCmsPage();
    await screen.findByRole("textbox", { name: "Document content" });
    fireEvent.click(screen.getByRole("button", { name: "Publish" }));
    await screen.findByText("No publication is configured for this document and garden.");
    expect(harness.requests.some(({ init }) => init?.method === "PATCH")).toBe(false);
    expect(screen.queryByText("Publication requested")).not.toBeInTheDocument();
    errorLog.mockRestore();
  });
});
