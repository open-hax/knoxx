import { describe, expect, it, afterEach, vi } from "vitest";
import { render, screen, cleanup, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ChatComposer from "./ChatComposer";

afterEach(() => {
  cleanup();
});

// ─── Rendering ──────────────────────────────────────────────────

describe("ChatComposer rendering", () => {
  it("renders textarea with placeholder", () => {
    render(<ChatComposer onSend={() => {}} isSending={false} />);
    const textarea = screen.getByPlaceholderText(/Send a prompt/) as HTMLTextAreaElement;
    expect(textarea).toBeTruthy();
  });

  it("renders Send button when not sending", () => {
    render(<ChatComposer onSend={() => {}} isSending={false} />);
    expect(screen.getByText("Send")).toBeTruthy();
  });

  it("renders Sending... when isSending is true", () => {
    render(<ChatComposer onSend={() => {}} isSending={true} />);
    expect(screen.getByText("Sending...")).toBeTruthy();
  });

  it("disables textarea when disabled prop is true", () => {
    render(<ChatComposer onSend={() => {}} isSending={false} disabled={true} />);
    const textarea = screen.getByPlaceholderText(/Send a prompt/) as HTMLTextAreaElement;
    expect(textarea.disabled).toBe(true);
  });

  it("disables button when disabled prop is true", () => {
    render(<ChatComposer onSend={() => {}} isSending={false} disabled={true} />);
    const button = screen.getByRole("button") as HTMLButtonElement;
    expect(button.disabled).toBe(true);
  });

  it("enables textarea when not disabled", () => {
    render(<ChatComposer onSend={() => {}} isSending={false} disabled={false} />);
    const textarea = screen.getByPlaceholderText(/Send a prompt/) as HTMLTextAreaElement;
    expect(textarea.disabled).toBe(false);
  });

  it("enables button when not disabled", () => {
    render(<ChatComposer onSend={() => {}} isSending={false} disabled={false} />);
    const button = screen.getByRole("button") as HTMLButtonElement;
    expect(button.disabled).toBe(false);
  });
});

// ─── Form submission ────────────────────────────────────────────

describe("ChatComposer form submission", () => {
  it("calls onSend with trimmed text on form submit", async () => {
    const onSend = vi.fn();
    render(<ChatComposer onSend={onSend} isSending={false} />);

    const textarea = screen.getByPlaceholderText(/Send a prompt/) as HTMLTextAreaElement;
    await userEvent.type(textarea, "  hello world  ");

    const button = screen.getByRole("button", { name: /Send/ });
    await userEvent.click(button);

    expect(onSend).toHaveBeenCalledTimes(1);
    expect(onSend).toHaveBeenCalledWith("hello world");
  });

  it("clears textarea after successful send", async () => {
    const onSend = vi.fn();
    render(<ChatComposer onSend={onSend} isSending={false} />);

    const textarea = screen.getByPlaceholderText(/Send a prompt/) as HTMLTextAreaElement;
    await userEvent.type(textarea, "test message");

    const button = screen.getByRole("button", { name: /Send/ });
    await userEvent.click(button);

    expect(textarea.value).toBe("");
  });

  it("does not call onSend when textarea is empty", async () => {
    const onSend = vi.fn();
    render(<ChatComposer onSend={onSend} isSending={false} />);

    const form = document.querySelector("form")!;
    fireEvent.submit(form);

    expect(onSend).not.toHaveBeenCalled();
  });

  it("does not call onSend when textarea has only whitespace", async () => {
    const onSend = vi.fn();
    render(<ChatComposer onSend={onSend} isSending={false} />);

    const textarea = screen.getByPlaceholderText(/Send a prompt/) as HTMLTextAreaElement;
    await userEvent.type(textarea, "   ");

    const form = document.querySelector("form")!;
    fireEvent.submit(form);

    expect(onSend).not.toHaveBeenCalled();
  });

  it("does not call onSend when disabled", async () => {
    const onSend = vi.fn();
    render(<ChatComposer onSend={onSend} isSending={false} disabled={true} />);

    const form = document.querySelector("form")!;
    fireEvent.submit(form);

    expect(onSend).not.toHaveBeenCalled();
  });
});

// ─── Keyboard submission ────────────────────────────────────────

describe("ChatComposer keyboard submission", () => {
  it("sends on Enter key (without Shift)", async () => {
    const onSend = vi.fn();
    render(<ChatComposer onSend={onSend} isSending={false} />);

    const textarea = screen.getByPlaceholderText(/Send a prompt/) as HTMLTextAreaElement;
    await userEvent.type(textarea, "hello");

    fireEvent.keyDown(textarea, { key: "Enter", shiftKey: false });

    expect(onSend).toHaveBeenCalledTimes(1);
    expect(onSend).toHaveBeenCalledWith("hello");
  });

  it("clears textarea after Enter key send", async () => {
    const onSend = vi.fn();
    render(<ChatComposer onSend={onSend} isSending={false} />);

    const textarea = screen.getByPlaceholderText(/Send a prompt/) as HTMLTextAreaElement;
    await userEvent.type(textarea, "hello");

    fireEvent.keyDown(textarea, { key: "Enter", shiftKey: false });

    expect(textarea.value).toBe("");
  });

  it("does not send on Shift+Enter (allows newlines)", async () => {
    const onSend = vi.fn();
    render(<ChatComposer onSend={onSend} isSending={false} />);

    const textarea = screen.getByPlaceholderText(/Send a prompt/) as HTMLTextAreaElement;
    await userEvent.type(textarea, "hello");

    fireEvent.keyDown(textarea, { key: "Enter", shiftKey: true });

    expect(onSend).not.toHaveBeenCalled();
  });

  it("does not send on Enter when textarea is empty", async () => {
    const onSend = vi.fn();
    render(<ChatComposer onSend={onSend} isSending={false} />);

    const textarea = screen.getByPlaceholderText(/Send a prompt/) as HTMLTextAreaElement;
    fireEvent.keyDown(textarea, { key: "Enter", shiftKey: false });

    expect(onSend).not.toHaveBeenCalled();
  });

  it("does not send on Enter when disabled", async () => {
    const onSend = vi.fn();
    render(<ChatComposer onSend={onSend} isSending={false} disabled={true} />);

    const textarea = screen.getByPlaceholderText(/Send a prompt/) as HTMLTextAreaElement;
    fireEvent.keyDown(textarea, { key: "Enter", shiftKey: false });

    expect(onSend).not.toHaveBeenCalled();
  });

  it("does not send on other keys like Tab", async () => {
    const onSend = vi.fn();
    render(<ChatComposer onSend={onSend} isSending={false} />);

    const textarea = screen.getByPlaceholderText(/Send a prompt/) as HTMLTextAreaElement;
    await userEvent.type(textarea, "hello");

    fireEvent.keyDown(textarea, { key: "Tab" });

    expect(onSend).not.toHaveBeenCalled();
  });
});

// ─── Typing ─────────────────────────────────────────────────────

describe("ChatComposer typing", () => {
  it("updates textarea value on typing", async () => {
    render(<ChatComposer onSend={() => {}} isSending={false} />);

    const textarea = screen.getByPlaceholderText(/Send a prompt/) as HTMLTextAreaElement;
    await userEvent.type(textarea, "hello world");

    expect(textarea.value).toBe("hello world");
  });
});
