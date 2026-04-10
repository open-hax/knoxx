import { describe, expect, it, afterEach } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import ConsolePanel from "./ConsolePanel";

afterEach(() => {
  cleanup();
});

describe("ConsolePanel", () => {
  it("renders Console Stream heading", () => {
    render(<ConsolePanel lines={[]} />);
    expect(screen.getByText("Console Stream")).toBeTruthy();
  });

  it("shows line count", () => {
    render(<ConsolePanel lines={["line1", "line2", "line3"]} />);
    expect(screen.getByText("3 lines")).toBeTruthy();
  });

  it("shows 0 lines for empty array", () => {
    render(<ConsolePanel lines={[]} />);
    expect(screen.getByText("0 lines")).toBeTruthy();
  });

  it("shows Waiting for logs... when no lines", () => {
    render(<ConsolePanel lines={[]} />);
    expect(screen.getByText("Waiting for logs...")).toBeTruthy();
  });

  it("renders each line", () => {
    render(<ConsolePanel lines={["hello", "world"]} />);
    expect(screen.getByText("hello")).toBeTruthy();
    expect(screen.getByText("world")).toBeTruthy();
  });

  it("renders duplicate lines", () => {
    render(<ConsolePanel lines={["repeat", "repeat"]} />);
    const repeats = screen.getAllByText("repeat");
    expect(repeats.length).toBe(2);
  });

  it("renders single line", () => {
    render(<ConsolePanel lines={["only one"]} />);
    expect(screen.getByText("only one")).toBeTruthy();
    expect(screen.queryByText("Waiting for logs...")).toBeNull();
  });

  it("does not show waiting message when lines exist", () => {
    render(<ConsolePanel lines={["log entry"]} />);
    expect(screen.queryByText("Waiting for logs...")).toBeNull();
  });
});
