import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { WorkbenchShell } from "./WorkbenchShell";

describe("WorkbenchShell", () => {
  it("owns the viewport-bounded overflow contract for workbench pages", () => {
    const { container } = render(
      <div style={{ display: "flex", flexDirection: "column", height: 480 }}>
        <div>Header</div>
        <WorkbenchShell style={{ background: "black" }}>
          <main>center</main>
        </WorkbenchShell>
      </div>,
    );

    const shell = container.querySelector("main")?.parentElement as HTMLDivElement;
    expect(shell.style.display).toBe("flex");
    expect(shell.style.flex).toBe("1 1 0%");
    expect(shell.style.minHeight).toBe("0");
    expect(shell.style.minWidth).toBe("0");
    expect(shell.style.overflow).toBe("hidden");
    expect(shell.style.background).toBe("black");
  });
});
