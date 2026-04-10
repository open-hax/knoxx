import { describe, expect, it, vi, afterEach } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ChatComposer from "./ChatComposer";

afterEach(() => {
  cleanup();
});

describe("ChatComposer", () => {
  const noop = () => {};
  const noopAsync = () => Promise.resolve();

  describe("rendering", () => {
    it("renders textarea with placeholder", () => {
      render(<ChatComposer onSend={noop} isSending={false} />);
      expect(screen.getByPlaceholderText(/Send a prompt/)).toBeTruthy();
    });

    it("renders Send button when not sending", () => {
      render(<ChatComposer onSend={noop} isSending={false} />);
      expect(screen.getByText("Send")).toBeTruthy();
    });

    it("renders Sending... when isSending is true", () => {
      render(<ChatComposer onSend={noop} isSending={true} />);
      expect(screen.getByText("Sending...")).toBeTruthy();
    });

    it("does not render End Turn button when not sending", () => {
      render(<ChatComposer onSend={noop} isSending={false} onAbort={noop} />);
      expect(screen.queryByText("End Turn")).toBeNull();
    });

    it("renders End Turn button when isSending and onAbort provided", () => {
      render(<ChatComposer onSend={noop} isSending={true} onAbort={noop} />);
      expect(screen.getByText("End Turn")).toBeTruthy();
    });

    it("does not render End Turn button when onAbort not provided", () => {
      render(<ChatComposer onSend={noop} isSending={true} />);
      expect(screen.queryByText("End Turn")).toBeNull();
    });
  });

  describe("End Turn button", () => {
    it("calls onAbort when clicked", async () => {
      const onAbort = vi.fn();
      render(<ChatComposer onSend={noop} isSending={true} onAbort={onAbort} />);

      const endTurnButton = screen.getByText("End Turn");
      await userEvent.click(endTurnButton);

      expect(onAbort).toHaveBeenCalledTimes(1);
    });

    it("is disabled when disabled prop is true", () => {
      render(<ChatComposer onSend={noop} isSending={true} onAbort={noop} disabled={true} />);
      const endTurnButton = screen.getByText("End Turn") as HTMLButtonElement;
      expect(endTurnButton.disabled).toBe(true);
    });
  });

  describe("Send button", () => {
    it("is disabled when isSending is true", () => {
      render(<ChatComposer onSend={noop} isSending={true} />);
      const sendButton = screen.getByText("Sending...") as HTMLButtonElement;
      expect(sendButton.disabled).toBe(true);
    });
  });
});
