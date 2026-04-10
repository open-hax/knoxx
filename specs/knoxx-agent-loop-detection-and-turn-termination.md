# Knoxx Agent Loop Detection and Turn Termination

## Problem Statement

Agents can get stuck in loops during execution, particularly when:
1. Tool calls keep failing and retrying with the same parameters
2. The agent enters a reasoning cycle that repeats the same thought pattern
3. External dependencies (APIs, databases) return intermittent errors that trigger retry loops
4. The agent's response generation gets stuck in a repetition pattern

Currently, users have no way to forcefully terminate a stuck agent turn, and the system has no automatic detection of looping behavior.

## Session Reference

Session `bb5aecb1-5ca8-4ca6-8bdf-8f485e3ef061` (4/10/2026, 1PM) exhibited looping behavior where the agent got stuck and could not be stopped by the user.

## Proposed Solution

### 1. End Turn Button (Frontend)

Add an "End Turn" button to the ChatComposer that:
- Appears when `isSending` is true
- Allows the user to forcefully terminate the current agent turn
- Sends an abort signal to the backend
- Clears the streaming state and resets `isSending` to false
- Preserves any partial response already received

**UI Location:**
- Replace or augment the "Send" button when `isSending` is true
- Show as a "Stop" or "End Turn" button with distinct styling (warning/error color)

**Frontend Changes:**

```typescript
// In ChatComposer.tsx
interface ChatComposerProps {
  onSend: (text: string) => void;
  isSending: boolean;
  disabled?: boolean;
  onEndTurn?: () => void;  // NEW: callback to end current turn
}

// In the form:
<button type="submit" className="btn-primary h-fit" disabled={disabled}>
  {isSending ? "Sending..." : "Send"}
</button>

{isSending && onEndTurn && (
  <button type="button" className="btn-danger h-fit" onClick={onEndTurn}>
    End Turn
  </button>
)}
```

### 2. Abort Endpoint (Backend)

Add a new endpoint to abort the current turn:

```clojure
;; In app_routes.cljs
(route! app "POST" "/api/knoxx/abort"
        (fn [request reply]
          (let [session-id (or (aget request "body" "session_id") "")
                conversation-id (or (aget request "body" "conversation_id") "")]
            ;; Find active agent session
            ;; Set abort flag
            ;; Clean up streaming state
            ;; Return success
            )))
```

**Abort Flow:**
1. Find the active agent session by conversation_id
2. Set an abort flag that the streaming loop checks
3. Close any active streams/connections
4. Update session status to "aborted"
5. Broadcast abort event via WebSocket
6. Return success response

### 3. Loop Detection (Backend)

Implement automatic loop detection in the agent runtime:

**Detection Patterns:**

1. **Tool Call Repetition**
   - Same tool called with same parameters N times in a row (threshold: 3)
   - Track tool call signatures in current turn
   - Trigger abort when repetition threshold exceeded

2. **Response Token Repetition**
   - Same phrase repeated N times in streaming output
   - Use rolling window of last M tokens
   - Detect if same sequence appears multiple times

3. **Time-based Detection**
   - Turn exceeds maximum duration (configurable, default: 5 minutes)
   - No progress (no new tokens) for N seconds (default: 30)

4. **Error Loop Detection**
   - Same error type occurring N times consecutively
   - Agent keeps retrying same failing operation

**Implementation:**

```clojure
;; In agent_turns.cljs or new loop_detection.cljs

(defn detect-tool-loop?
  "Returns true if the same tool has been called with identical params 3+ times"
  [tool-history]
  (let [recent (take 5 tool-history)
        groups (group-by identity recent)]
    (some #(>= (count %) 3) (vals groups))))

(defn detect-response-loop?
  "Returns true if the response is repeating patterns"
  [token-buffer]
  ;; Check for repeated phrases in last N tokens
  )

(defn check-loop-conditions
  "Runs all loop detection checks, returns :abort if any triggered"
  [session-state]
  (cond
    (detect-tool-loop? (:tool-history session-state))
    {:abort true :reason "Tool call loop detected"}

    (detect-response-loop? (:token-buffer session-state))
    {:abort true :reason "Response pattern loop detected"}

    (> (turn-duration session-state) (max-turn-duration))
    {:abort true :reason "Maximum turn duration exceeded"}

    (no-progress? session-state)
    {:abort true :reason "No progress detected"}

    :else nil))
```

### 4. WebSocket Integration

Broadcast loop detection events to connected clients:

```clojure
;; When loop detected:
(broadcast-ws-session! session-id "agent:loop_detected"
                       {:reason "Tool call loop detected"
                        :tool_name "read"
                        :call_count 3})

;; When abort requested:
(broadcast-ws-session! session-id "agent:turn_aborted"
                       {:reason "User requested abort"
                        :partial_response captured-response})
```

### 5. Frontend Event Handling

Handle abort events in the WebSocket connection:

```typescript
// In ws.ts or hooks.ts
ws.onmessage = (event) => {
  const envelope = JSON.parse(event.data);

  switch (envelope.channel) {
    case "agent:loop_detected":
      // Show warning notification
      // Optionally auto-abort or prompt user
      break;

    case "agent:turn_aborted":
      // Update UI state
      // Show partial response if any
      // Reset isSending
      setIsSending(false);
      break;
  }
};
```

## Configuration

Add configuration options for loop detection thresholds:

```clojure
;; In runtime_config.cljs
(def loop-detection-config
  {:tool-repetition-threshold 3      ; Max identical tool calls
   :response-repetition-threshold 3  ; Max identical phrase repetitions
   :max-turn-duration-ms 300000      ; 5 minutes
   :no-progress-timeout-ms 30000     ; 30 seconds
   :auto-abort-on-loop false})       ; If true, auto-abort; if false, warn only
```

## Implementation Phases

### Phase 1: End Turn Button (Priority: High)
- [ ] Add `onEndTurn` prop to ChatComposer
- [ ] Show "End Turn" button when isSending
- [ ] Create `/api/knoxx/abort` endpoint
- [ ] Wire up abort flow from frontend to backend
- [ ] Test manual abort functionality

### Phase 2: Loop Detection (Priority: Medium)
- [ ] Implement tool call loop detection
- [ ] Implement time-based detection (max duration, no progress)
- [ ] Add WebSocket broadcast for loop events
- [ ] Add configuration options
- [ ] Test with simulated loop scenarios

### Phase 3: Response Pattern Detection (Priority: Low)
- [ ] Implement token buffer tracking
- [ ] Add phrase repetition detection
- [ ] Add sliding window analysis
- [ ] Test with various repetition patterns

## Testing Scenarios

1. **Manual Abort**: Start a long-running turn, click End Turn, verify it stops cleanly
2. **Tool Loop**: Agent calls same tool with same params 3+ times, verify detection
3. **Max Duration**: Run a turn that exceeds 5 minutes, verify auto-abort
4. **No Progress**: Agent hangs mid-stream, verify detection after 30s
5. **WebSocket Events**: Verify clients receive loop_detected and turn_aborted events

## Security Considerations

- Only the session owner should be able to abort their own turns
- Abort endpoint should validate session/conversation ownership
- Loop detection should not be bypassable by malicious input

## Metrics to Track

- Number of manual aborts per session
- Number of auto-aborts due to loop detection
- Average turn duration before abort
- Most common loop patterns (for improving detection)
