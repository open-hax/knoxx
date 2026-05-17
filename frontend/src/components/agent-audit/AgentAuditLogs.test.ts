import { describe, expect, it } from "vitest";

import { memorySessionsPageLimit } from "./AgentAuditLogs";

describe("AgentAuditLogs", () => {
  it("uses the normal page size for unscoped audit history", () => {
    expect(memorySessionsPageLimit()).toBe(40);
  });

  it("keeps scoped contract pages bounded to avoid expensive session scans", () => {
    expect(memorySessionsPageLimit({ builtInContractId: "ussyverse_social_creative" })).toBe(40);
  });

  it("keeps actor-filtered pages bounded to avoid expensive session scans", () => {
    expect(memorySessionsPageLimit({ actorId: "discord_automation" })).toBe(40);
  });
});
