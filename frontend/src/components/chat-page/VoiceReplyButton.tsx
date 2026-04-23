import { VoiceInputButton } from "./VoiceInputButton";

type VoiceReplyButtonProps = {
  disabled?: boolean;
  onTranscript: (text: string) => void;
};

export function VoiceReplyButton({ disabled, onTranscript }: VoiceReplyButtonProps) {
  return (
    <VoiceInputButton
      disabled={disabled}
      onTranscript={onTranscript}
      idleLabel="Reply by voice"
      recordingLabel="Stop recording"
      transcribingLabel="Transcribing…"
    />
  );
}
