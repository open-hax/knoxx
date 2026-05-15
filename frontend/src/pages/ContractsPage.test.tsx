import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import type { ContractListItem } from "../lib/api/contracts";
import ContractsPage from "./ContractsPage";

const mockListContracts = vi.fn();
const mockGetContract = vi.fn();
const mockValidateContract = vi.fn();
const mockSaveContract = vi.fn();
const mockCopyContract = vi.fn();
const mockGetEventAgentControl = vi.fn();
const mockPinContextItem = vi.fn();

const palette = {
  bg: { default: "#111", darker: "#000" },
  fg: { default: "#eee", muted: "#999", subtle: "#333", soft: "#bbb" },
  accent: { cyan: "#66d9ef", green: "#a6e22e", red: "#f92672", orange: "#fd971f" },
};

vi.mock("@open-hax/uxx", () => ({
  useResolvedTheme: () => ({ palette, colors: {} }),
  tokens: {
    fontSize: { xs: "12px", sm: "14px", base: "16px", lg: "18px" },
    radius: { xs: "3px", sm: "4px", md: "6px" },
  },
}));

vi.mock("../components/admin-page/EdnEditor", () => ({
  EdnEditor: ({ value, onChange }: { value: string; onChange: (next: string) => void }) => (
    <textarea aria-label="Full contract EDN" value={value} onChange={(event) => onChange(event.target.value)} />
  ),
}));

vi.mock("../components/chat-page/useChatWorkspaceController", () => ({
  useChatWorkspaceController: () => ({ pinContextItem: mockPinContextItem }),
}));

vi.mock("../components/chat-page/ChatWorkspacePane", () => ({
  ChatWorkspacePane: () => <div data-testid="contracts-chat-pane" />,
}));

vi.mock("../lib/api/contracts", async () => {
  const actual = await vi.importActual<typeof import("../lib/api/contracts")>("../lib/api/contracts");
  return {
    ...actual,
    listContracts: (...args: unknown[]) => mockListContracts(...args),
    getContract: (...args: unknown[]) => mockGetContract(...args),
    validateContract: (...args: unknown[]) => mockValidateContract(...args),
    saveContract: (...args: unknown[]) => mockSaveContract(...args),
    copyContract: (...args: unknown[]) => mockCopyContract(...args),
  };
});

vi.mock("../lib/api/admin", () => ({
  getEventAgentControl: (...args: unknown[]) => mockGetEventAgentControl(...args),
}));

const existingEdn = `{:contract/id "existing_agent"
 :contract/kind :agent
 :contract/version 1
 :enabled true
 :agent {:role :knowledge_worker :model "gemma4:31b" :thinking :off}}`;

const editedEdn = `{:contract/id "saved_agent"
 :contract/kind :agent
 :contract/version 1
 :enabled true
 :agent {:role :knowledge_worker :model "gemma4:31b" :thinking :off}}`;

function ednFor(contractId: string, kind = "agent") {
  return `{:contract/id "${contractId}"
 :contract/kind :${kind}
 :contract/version 1
 :enabled true
 :agent {:role :knowledge_worker :model "gemma4:31b" :thinking :off}}`;
}

const existingContract = {
  "contract/id": "existing_agent",
  "contract/kind": "agent",
  "contract/version": 1,
  enabled: true,
  agent: { role: "knowledge_worker", model: "gemma4:31b", thinking: "off" },
} as const;

function listItem(id: string, contractClass: ContractListItem["contractClass"] = "agents"): ContractListItem {
  return {
    id,
    contractClass,
    kind: contractClass === "agents" ? "agent" : "trigger",
    version: 1,
    enabled: true,
    ednHash: 1,
    compiledAt: null,
    updatedAt: "2026-05-15T00:00:00.000Z",
  };
}

function eventControl() {
  return {
    configured: true,
    tokenPreview: "mock",
    availableRoles: ["knowledge_worker"],
    availableSourceKinds: ["manual"],
    availableTriggerKinds: ["manual"],
    control: { sources: {}, jobs: [] },
    runtime: { running: true, configured: true, sources: {}, jobs: [] },
  };
}

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }));
  mockListContracts.mockResolvedValue({ contracts: [listItem("existing_agent"), listItem("trigger_contract", "triggers")] });
  mockGetEventAgentControl.mockResolvedValue(eventControl());
  mockGetContract.mockImplementation(async (contractId: string, contractClass: string) => ({
    contractClass,
    contract: {
      ...existingContract,
      "contract/id": contractId,
      "contract/kind": contractClass === "triggers" ? "trigger" : "agent",
    },
    ednText: contractId === "existing_agent" ? existingEdn : ednFor(contractId, contractClass === "triggers" ? "trigger" : "agent"),
    validation: { ok: true, errors: [], warnings: [] },
  }));
  mockValidateContract.mockResolvedValue({ ok: true, errors: [], warnings: [], contract: existingContract });
  mockSaveContract.mockImplementation(async (contractId: string, ednText: string, contractClass: string) => ({
    ok: true,
    contractClass,
    contract: { ...existingContract, "contract/id": contractId },
    ednText,
    validation: { ok: true, errors: [], warnings: [] },
  }));
  mockCopyContract.mockImplementation(async (_sourceId: string, newId: string, contractClass: string) => ({
    ok: true,
    contractClass,
    contract: { ...existingContract, "contract/id": newId },
    ednText: ednFor(newId),
    validation: { ok: true, errors: [], warnings: [] },
  }));
});

describe("ContractsPage backend interactions", () => {
  it("validates and saves the current EDN draft with the inferred contract id", async () => {
    render(<ContractsPage />);

    const editor = await screen.findByLabelText("Full contract EDN") as HTMLTextAreaElement;
    await waitFor(() => expect(editor.value).toContain('"existing_agent"'));

    fireEvent.change(editor, { target: { value: editedEdn } });
    fireEvent.click(screen.getByRole("button", { name: "✓ Validate" }));

    await screen.findByText("Validation passed.");
    expect(mockValidateContract).toHaveBeenCalledWith(editedEdn, "agents");

    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await screen.findByText("Saved saved_agent.");
    expect(mockSaveContract).toHaveBeenCalledWith("saved_agent", editedEdn, "agents");
    expect(mockPinContextItem).toHaveBeenCalledWith(expect.objectContaining({
      id: "contract:existing_agent",
      path: "/ops/contracts/agents/existing_agent",
    }));
  });

  it("switches selected contract class when a trigger contract is selected", async () => {
    render(<ContractsPage />);

    const editor = await screen.findByLabelText("Full contract EDN") as HTMLTextAreaElement;
    await waitFor(() => expect(editor.value).toContain('"existing_agent"'));
    fireEvent.click(screen.getByRole("button", { name: /trigger_contract/ }));

    await waitFor(() => expect(mockGetContract).toHaveBeenCalledWith("trigger_contract", "triggers"));
    await waitFor(() => expect(editor.value).toContain(":contract/kind :trigger"));
  });

  it("copies the selected contract to a new id and focuses the returned draft", async () => {
    render(<ContractsPage />);

    const editor = await screen.findByLabelText("Full contract EDN") as HTMLTextAreaElement;
    await waitFor(() => expect(editor.value).toContain('"existing_agent"'));
    fireEvent.click(screen.getAllByRole("button", { name: "Clone" })[0]);
    fireEvent.change(screen.getByPlaceholderText("new-contract-id"), { target: { value: "copy_agent" } });
    fireEvent.click(screen.getAllByRole("button", { name: "Clone" })[1]);

    await screen.findByText("Copied existing_agent → copy_agent.");
    expect(mockCopyContract).toHaveBeenCalledWith("existing_agent", "copy_agent", "agents");
    await waitFor(() => expect(editor.value).toContain('"copy_agent"'));
  });
});
