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
    expect(screen.getAllByText("docs/guide.md").length).toBeGreaterThan(0);
    expect(screen.getByText("Rendered markdown result")).toBeInTheDocument();
    expect(screen.queryByText(/"content"/)).not.toBeInTheDocument();
  });

  it("renders tool inputs even when the preview is json without a path/query/url", () => {
    render(
      <ToolReceiptBlock
        receipt={{
          id: "tool-2",
          tool_name: "custom",
          status: "completed",
          input_preview: JSON.stringify({ foo: "bar", limit: 3 }),
          result_preview: "ok",
        }}
      />,
    );

    expect(screen.getByText("Inputs")).toBeInTheDocument();
    expect(screen.getByText("foo")).toBeInTheDocument();
    expect(screen.getByText("bar")).toBeInTheDocument();
    const limitNode = screen.getByText("limit");
    expect(limitNode.closest("li")?.textContent).toContain("3");
  });

  it("renders truncated json tool output by extracting text fragments", () => {
    const truncatedJson = `{
  "content": [
    { "type": "text", "text": "hello\\nworld" }
  ]`;

    render(
      <ToolReceiptBlock
        receipt={{
          id: "tool-3",
          tool_name: "read",
          status: "completed",
          input_preview: JSON.stringify({ path: "docs/guide.md" }),
          result_preview: truncatedJson,
        }}
      />,
    );

    expect(screen.getByText("hello")).toBeInTheDocument();
    expect(screen.getByText("world")).toBeInTheDocument();
    expect(screen.queryByText(/\"content\"/)).not.toBeInTheDocument();
  });

  it("treats string null tool inputs as missing (renders a placeholder)", () => {
    render(
      <ToolReceiptBlock
        receipt={{
          id: "tool-4",
          tool_name: "bash",
          status: "completed",
          input_preview: "null",
          result_preview: "ok",
        }}
      />,
    );

    expect(screen.getByText("Inputs")).toBeInTheDocument();
    expect(screen.getByText(/inputs unavailable/)).toBeInTheDocument();
    expect(screen.queryByText(/^null$/)).not.toBeInTheDocument();
  });
});
