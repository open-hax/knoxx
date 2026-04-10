import { FormEvent, KeyboardEvent, useState } from "react";

interface ChatComposerProps {
  onSend: (text: string) => void;
  isSending: boolean;
  disabled?: boolean;
  onAbort?: () => void;
}

function ChatComposer({ onSend, isSending, disabled = false, onAbort }: ChatComposerProps) {
  const [value, setValue] = useState("");

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    const trimmed = value.trim();
    if (!trimmed || disabled) {
      return;
    }
    onSend(trimmed);
    setValue("");
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      const trimmed = value.trim();
      if (trimmed && !disabled) {
        onSend(trimmed);
        setValue("");
      }
    }
  };

  return (
    <form className="mt-3 flex gap-2" onSubmit={handleSubmit}>
      <textarea
        rows={3}
        className="input min-h-20 flex-1 resize-y"
        value={value}
        onChange={(event) => setValue(event.target.value)}
        onKeyDown={handleKeyDown}
        placeholder="Send a prompt, test edge cases, compare behavior..."
        disabled={disabled}
      />
      <div className="flex flex-col gap-1">
        <button type="submit" className="btn-primary h-fit" disabled={disabled || isSending}>
          {isSending ? "Sending..." : "Send"}
        </button>
        {isSending && onAbort && (
          <button type="button" className="btn-danger h-fit" onClick={onAbort}>
            End Turn
          </button>
        )}
      </div>
    </form>
  );
}

export default ChatComposer;
