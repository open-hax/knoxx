import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ToolReceiptBlock } from "./ToolReceiptBlock";

describe("ToolReceiptBlock", () => {
  it("renders structured tool output as content instead of raw json", () => {
    render(
      <ToolReceiptBlock
        receipt={{
          id: "tool-1",
          tool_name: "read",
          status: "completed",
          input_preview: JSON.stringify({ path: "docs/guide.md" }),
          result_preview: JSON.stringify({ content: "# Guide\n\nRendered markdown result" }),
        }}
      />,
    );

    expect(screen.getByText("read")).toBeInTheDocument();
    expect(screen.getByText("docs/guide.md")).toBeInTheDocument();
    expect(screen.getByText("Rendered markdown result")).toBeInTheDocument();
    expect(screen.queryByText(/"content"/)).not.toBeInTheDocument();
  });
});
