import { describe, expect, it } from "vitest";

import { parseMongoJsonText } from "./DataPage";

describe("parseMongoJsonText", () => {
  it("returns the provided default for blank input", () => {
    expect(parseMongoJsonText("Projection", "", undefined)).toBeUndefined();
    expect(parseMongoJsonText("Filter", "   ", { status: "active" })).toEqual({ status: "active" });
  });

  it("parses valid Mongo JSON text", () => {
    expect(parseMongoJsonText("Filter", '{"status":"active"}', {})).toEqual({ status: "active" });
    expect(parseMongoJsonText("Sort", '{"_id":-1}', {})).toEqual({ _id: -1 });
  });

  it("throws a labeled error for invalid Mongo JSON before a query can be sent", () => {
    expect(() => parseMongoJsonText("Filter", '{"status":', {})).toThrow(/Filter is not valid JSON/);
  });
});
