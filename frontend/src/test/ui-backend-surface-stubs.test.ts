import { describe, expect, test } from "vitest";
import { uiBackendSurfaceMatrix } from "./ui-backend-surface-matrix";

describe("UI ↔ backend surface coverage matrix", () => {
  test("every surface has backend routes, behavior, and executable stub metadata", () => {
    expect(uiBackendSurfaceMatrix.length).toBeGreaterThan(0);
    const ids = new Set<string>();
    for (const surface of uiBackendSurfaceMatrix) {
      expect(surface.id, `${surface.id} id`).toMatch(/^[a-z0-9-]+$/);
      expect(ids.has(surface.id), `${surface.id} is unique`).toBe(false);
      ids.add(surface.id);
      expect(surface.route, `${surface.id} route`).not.toEqual("");
      expect(surface.surface, `${surface.id} UI surface`).not.toEqual("");
      expect(surface.backend.length, `${surface.id} backend routes`).toBeGreaterThan(0);
      expect(surface.behavior.length, `${surface.id} behavior bullets`).toBeGreaterThan(0);
      expect(surface.stubs.length, `${surface.id} test stubs`).toBeGreaterThan(0);
      for (const stub of surface.stubs) {
        expect(stub.name, `${surface.id} stub name`).not.toEqual("");
        expect(stub.assertion, `${surface.id} stub assertion`).not.toEqual("");
        if (stub.status === "implemented") {
          expect(stub.implementedBy?.length, `${surface.id} ${stub.name} implementation refs`).toBeGreaterThan(0);
        }
      }
    }
  });

  for (const surface of uiBackendSurfaceMatrix) {
    const plannedStubs = surface.stubs.filter((entry) => entry.status !== "implemented");
    describe(`${surface.id} (${surface.route})`, () => {
      if (plannedStubs.length === 0) {
        test("all planned stubs for this surface are implemented", () => {
          expect(surface.stubs.every((stub) => stub.status === "implemented")).toBe(true);
        });
      }
      for (const stub of plannedStubs) {
        test.todo(`[${stub.priority}] [${stub.kind}] ${stub.name} — ${stub.assertion}`);
      }
    });
  }
});
