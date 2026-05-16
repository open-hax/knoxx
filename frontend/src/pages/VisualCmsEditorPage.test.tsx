import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { VisualCmsEditorPage } from "./VisualCmsEditorPage";

vi.mock("../components/cms/VisualEditor", () => ({
  VisualEditor: ({ contract, onChange }: { contract: { view_title: string }; onChange: (next: Record<string, unknown>) => void }) => (
    <section data-testid="visual-editor">
      <h2>{contract.view_title}</h2>
      <button
        type="button"
        onClick={() => onChange({
          ...contract,
          view_title: "Edited Visual Draft",
          blocks: [{ id: "edited-block", type: "rich-text", zone: "body", props: { text: "Edited" } }],
        })}
      >
        Change visual draft
      </button>
    </section>
  ),
}));

vi.mock("../components/cms/SourcePanel", () => ({
  SourcePanel: ({ contract, contentMd, validationErrors }: { contract: { view_title: string }; contentMd?: string; validationErrors: string[] }) => (
    <aside data-testid="source-panel">
      <div>Source: {contract.view_title}</div>
      <pre>{contentMd}</pre>
      <span>Validation errors: {validationErrors.length}</span>
    </aside>
  ),
}));

const viewContractEdn = `
{:view-id "draft-1"
 :view-title "Visual Draft"
 :view-kind :article-page
 :view-schema-version 1
 :view-status :draft
 :layout {:template :article-page
          :zones [{:id :body :label "Body" :accepts [:rich-text]}]}
 :blocks [{:id "body-1" :type :rich-text :zone :body :props {:text "Hello"}}]
 :editor {:locked false :allowed-actions ["drag" "drop" "edit-props"] :default-panel "layout"}
 :publishing {:defer-index false :skip-translation true}
 :settings {:language "en" :allow-comments true}}
`;

function jsonResponse(body: unknown, init: ResponseInit = {}) {
  return new Response(JSON.stringify(body), {
    status: init.status ?? 200,
    statusText: init.statusText,
    headers: { "Content-Type": "application/json", ...(init.headers ?? {}) },
  });
}

function installVisualCmsFetchMock() {
  const requests: Array<{ url: string; init?: RequestInit }> = [];
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    requests.push({ url, init });

    if (url === `/api/ingestion/file?path=${encodeURIComponent("drafts/test/view-contract.edn")}`) {
      return jsonResponse({ path: "drafts/test/view-contract.edn", content: viewContractEdn });
    }
    if (url === `/api/ingestion/file?path=${encodeURIComponent("drafts/test/content.md")}`) {
      return jsonResponse({ path: "drafts/test/content.md", content: "# Markdown Source\n\nBody copy" });
    }
    if (url === "/api/ingestion/file" && init?.method === "PUT") {
      return jsonResponse({ ok: true });
    }

    return jsonResponse({ error: `Unexpected ${url}` }, { status: 404 });
  });
  vi.stubGlobal("fetch", fetchMock);
  return { requests };
}

function renderVisualCmsEditor(path = "/cms/editor/drafts/test") {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/cms/editor/*" element={<VisualCmsEditorPage />} />
        <Route path="/cms" element={<div>CMS index</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("VisualCmsEditorPage", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("loads visual draft files and renders the editor plus source panel", async () => {
    const { requests } = installVisualCmsFetchMock();

    renderVisualCmsEditor();

    expect(await screen.findAllByText("Visual Draft")).toHaveLength(2);
    expect(screen.getByTestId("visual-editor")).toBeInTheDocument();
    expect(screen.getByTestId("source-panel")).toHaveTextContent("# Markdown Source");
    expect(requests.map((request) => request.url)).toEqual(expect.arrayContaining([
      `/api/ingestion/file?path=${encodeURIComponent("drafts/test/view-contract.edn")}`,
      `/api/ingestion/file?path=${encodeURIComponent("drafts/test/content.md")}`,
    ]));
  });

  it("saves the edited visual contract to the draft view-contract path", async () => {
    const { requests } = installVisualCmsFetchMock();

    renderVisualCmsEditor();

    await screen.findAllByText("Visual Draft");
    fireEvent.click(screen.getByRole("button", { name: "Change visual draft" }));
    expect(screen.getByText("Unsaved changes")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => {
      const putRequest = requests.find((request) => request.url === "/api/ingestion/file" && request.init?.method === "PUT");
      expect(putRequest).toBeTruthy();
      expect(JSON.parse(String(putRequest?.init?.body))).toMatchObject({
        path: "drafts/test/view-contract.edn",
      });
      expect(JSON.parse(String(putRequest?.init?.body)).content).toContain(':view-title "Edited Visual Draft"');
    });
    await waitFor(() => expect(screen.queryByText("Unsaved changes")).not.toBeInTheDocument());
  });
});
