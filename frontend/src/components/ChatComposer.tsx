import { FormEvent, KeyboardEvent, useState } from "react";

interface ChatComposerProps {
  onSend: (text: string) => void;
  isSending: boolean;
  disabled?: boolean;
}

function ChatComposer({ onSend, isSending, disabled = false }: ChatComposerProps) {
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
      <button type="submit" className="btn-primary h-fit" disabled={disabled}>
        {isSending ? "Sending..." : "Send"}
      </button>
    </form>
  );
}

export default ChatComposer;
