import { describe, expect, it, vi, afterEach } from "vitest";
import { makeId } from "./make-id";

describe("makeId", () => {
  const originalCrypto = globalThis.crypto;

  afterEach(() => {
    Object.defineProperty(globalThis, "crypto", {
      value: originalCrypto,
      writable: true,
      configurable: true,
    });
  });

  it("returns a UUID when crypto.randomUUID is available", () => {
    const mockRandomUUID = vi.fn(() => "12345678-1234-1234-1234-123456789abc");
    Object.defineProperty(globalThis, "crypto", {
      value: { randomUUID: mockRandomUUID },
      writable: true,
      configurable: true,
    });

    const result = makeId();
    expect(result).toBe("12345678-1234-1234-1234-123456789abc");
    expect(mockRandomUUID).toHaveBeenCalledTimes(1);
  });

  it("returns fallback ID when crypto.randomUUID is not a function", () => {
    Object.defineProperty(globalThis, "crypto", {
      value: { randomUUID: "not a function" },
      writable: true,
      configurable: true,
    });

    const result = makeId();
    expect(result).toMatch(/^id-\d+-[a-z0-9]+$/);
  });

  it("returns fallback ID when crypto is undefined", () => {
    Object.defineProperty(globalThis, "crypto", {
      value: undefined,
      writable: true,
      configurable: true,
    });

    const result = makeId();
    expect(result).toMatch(/^id-\d+-[a-z0-9]+$/);
  });

  it("fallback ID contains timestamp", () => {
    Object.defineProperty(globalThis, "crypto", {
      value: undefined,
      writable: true,
      configurable: true,
    });

    const before = Date.now();
    const result = makeId();
    const after = Date.now();

    const timestampMatch = result.match(/^id-(\d+)-/);
    expect(timestampMatch).toBeTruthy();
    const timestamp = parseInt(timestampMatch![1], 10);
    expect(timestamp).toBeGreaterThanOrEqual(before);
    expect(timestamp).toBeLessThanOrEqual(after);
  });

  it("fallback ID has random suffix", () => {
    Object.defineProperty(globalThis, "crypto", {
      value: undefined,
      writable: true,
      configurable: true,
    });

    const results = new Set<string>();
    for (let i = 0; i < 10; i++) {
      results.add(makeId());
    }
    // All 10 IDs should be unique due to random suffix
    expect(results.size).toBe(10);
  });
});
