import matrix from "./ui-backend-surface-matrix.json";

export type UiBackendTestKind =
  | "unit"
  | "api-client"
  | "component-integration"
  | "ava-puppeteer-e2e"
  | "backend-route-contract"
  | "ws-contract";

export interface UiBackendTestStub {
  kind: UiBackendTestKind;
  name: string;
  priority: "P0" | "P1" | "P2";
  assertion: string;
  status?: "planned" | "implemented";
  implementedBy?: string[];
}

export interface UiBackendSurface {
  id: string;
  route: string;
  owner: "tsx" | "cljs" | "mixed";
  surface: string;
  backend: string[];
  behavior: string[];
  stubs: UiBackendTestStub[];
}


const testKinds = new Set(["unit", "api-client", "component-integration", "ava-puppeteer-e2e", "backend-route-contract", "ws-contract"]);
const priorities = new Set(["P0", "P1", "P2"]);
const owners = new Set(["tsx", "cljs", "mixed"]);

function record(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function text(value: unknown): value is string {
  return typeof value === "string" && value.trim().length > 0;
}

function textList(value: unknown): value is string[] {
  return Array.isArray(value) && value.every(text);
}

function testStub(value: unknown): value is UiBackendTestStub {
  return record(value) && typeof value.kind === "string" && testKinds.has(value.kind)
    && typeof value.priority === "string" && priorities.has(value.priority)
    && text(value.name) && text(value.assertion)
    && (value.status === undefined || value.status === "planned" || value.status === "implemented")
    && (value.implementedBy === undefined || textList(value.implementedBy))
    && (value.status !== "implemented" || (textList(value.implementedBy) && value.implementedBy.length > 0));
}

function surface(value: unknown): value is UiBackendSurface {
  return record(value) && text(value.id) && text(value.route) && text(value.surface)
    && typeof value.owner === "string" && owners.has(value.owner)
    && textList(value.backend) && value.backend.length > 0
    && textList(value.behavior) && value.behavior.length > 0
    && Array.isArray(value.stubs) && value.stubs.length > 0 && value.stubs.every(testStub);
}

/** Decode coverage data without silently changing planned or implemented status. */
export function decodeUiBackendSurfaceMatrix(value: unknown): UiBackendSurface[] {
  if (!Array.isArray(value)) throw new Error("UI surface matrix must be an array");
  const ids = new Set<string>();
  return value.map((entry, index) => {
    if (!surface(entry)) throw new Error(`Invalid UI surface matrix row ${index}`);
    if (ids.has(entry.id)) throw new Error(`Duplicate UI surface matrix id: ${entry.id}`);
    ids.add(entry.id);
    return entry;
  });
}

export const uiBackendSurfaceMatrix = decodeUiBackendSurfaceMatrix(matrix);
