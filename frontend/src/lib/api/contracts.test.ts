import { beforeEach, describe, expect, it, vi } from "vitest";

const mockRequest = vi.fn();

vi.mock("./core", () => ({
  request: (...args: unknown[]) => mockRequest(...args),
}));

import { copyContract, getContract, listContracts, saveContract, validateContract } from "./contracts";

describe("contracts API", () => {
  beforeEach(() => {
    mockRequest.mockReset();
  });

  it("lists all contracts when no class is provided", async () => {
    mockRequest.mockResolvedValueOnce({ contracts: [] });

    await listContracts();

    expect(mockRequest).toHaveBeenCalledWith("/api/admin/contracts");
  });

  it("passes an explicit contract class filter", async () => {
    mockRequest.mockResolvedValueOnce({ contracts: [] });

    await listContracts("triggers");

    expect(mockRequest).toHaveBeenCalledWith("/api/admin/contracts?kind=triggers");
  });

  it("gets a contract with the requested class", async () => {
    mockRequest.mockResolvedValueOnce({ contract: {}, ednText: "", validation: { ok: true, errors: [], warnings: [] } });

    await getContract("agent one", "agents");

    expect(mockRequest).toHaveBeenCalledWith("/api/admin/contracts/agent%20one?kind=agents");
  });

  it("validates EDN with the selected class", async () => {
    mockRequest.mockResolvedValueOnce({ ok: true, errors: [], warnings: [] });

    await validateContract("{:contract/id \"a\"}", "agents");

    expect(mockRequest).toHaveBeenCalledWith("/api/admin/contracts/validate", {
      method: "POST",
      body: JSON.stringify({ ednText: "{:contract/id \"a\"}", kind: "agents" }),
    });
  });

  it("saves EDN to the encoded contract id", async () => {
    mockRequest.mockResolvedValueOnce({ ok: true, contract: {}, ednText: "", validation: { ok: true, errors: [], warnings: [] } });

    await saveContract("agent one", "{:contract/id \"agent one\"}", "agents");

    expect(mockRequest).toHaveBeenCalledWith("/api/admin/contracts/agent%20one", {
      method: "PUT",
      body: JSON.stringify({ ednText: "{:contract/id \"agent one\"}", kind: "agents" }),
    });
  });

  it("copies a source contract to a new id", async () => {
    mockRequest.mockResolvedValueOnce({ ok: true, contract: {}, ednText: "", validation: { ok: true, errors: [], warnings: [] } });

    await copyContract("source agent", "target_agent", "agents");

    expect(mockRequest).toHaveBeenCalledWith("/api/admin/contracts/source%20agent/copy", {
      method: "POST",
      body: JSON.stringify({ newId: "target_agent", kind: "agents" }),
    });
  });
});
