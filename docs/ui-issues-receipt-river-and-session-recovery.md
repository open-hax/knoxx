# Knoxx UI Issues: Receipt River Display and Session Recovery

## Issue 1: Receipt River Display

### Problem
The current implementation displays tool calls and reasoning events in a separate "Receipt River" section within an "Agent Runtime" card at the top of the chat area. This is confusing compared to other LLM chat platforms (ChatGPT, Claude, etc.) which show tools/reasoning **inline** between each assistant/user message block.

### Current Behavior (Before Fix)
```
+---------------------------------------------+
| Agent Runtime Card                          |
| +-- Presence                                |
| +-- Witness Thread                          |
| +-- Receipt River (tool events shown here)  |
| +-- Tool Receipts                           |
+---------------------------------------------+

+---------------------------------------------+
| User Message                                |
+---------------------------------------------+

+---------------------------------------------+
| Assistant Message                           |
| (tool receipts in collapsed <details>)      |
+---------------------------------------------+
```

### New Behavior (After Fix)
```
+---------------------------------------------+
| User Message                                |
+---------------------------------------------+

+---------------------------------------------+
| Tool: read (completed)                      |
| +-- input: path="docs/README.md"            |
| +-- output: # README...                     |
+---------------------------------------------+

+---------------------------------------------+
| Tool: semantic_query (running...)           |
| +-- input: query="knoxx architecture"       |
+---------------------------------------------+

+---------------------------------------------+
| Assistant Message                           |
| (final response with grounding sources)     |
+---------------------------------------------+
```

### Implementation

1. **Created `ToolReceiptBlock` component** (`frontend/src/components/ToolReceiptBlock.tsx`)
   - Shows tool name, status (running/completed/failed)
   - Displays input preview in collapsible section
   - Displays output preview when completed
   - Shows live updates for streaming tools
   - Color-coded by status (cyan=running, green=completed, red=failed)

2. **Created `ToolReceiptGroup` component**
   - Groups multiple tool receipts together
   - Merges live events with completed receipts
   - Filters to show only relevant receipts

3. **Modified `ChatPage.tsx` message rendering**
   - Added `liveToolReceipts` memo for active streaming message
   - Added `liveToolEvents` memo for real-time tool updates
   - Inline display of tool receipts for streaming assistant messages
   - Collapsible tool calls section for completed messages

---

## Issue 2: Browser Session Refresh / Agent Running State

### Problem
When the browser is refreshed while an agent request is running:
1. The UI appears idle (no visible activity)
2. When the user sends a new message, the backend returns an error: "agent is already running"
3. There's no reconnection to the ongoing run's event stream

### Root Cause Analysis

**Frontend State Persistence:**
- Session state persisted to `localStorage` including `isSending` flag
- On page refresh, `isSending` restored but no WebSocket reconnection
- `activeRunIdRef.current` restored but not connected to active run

**Backend Session Tracking:**
- Active agent sessions tracked in memory atom `agent-sessions*`
- WebSocket subscriptions are per-session-id
- No database persistence of active run state

### Implementation

1. **Added session recovery on mount** (`ChatPage.tsx`)
   - New `isRecovering` state for UI indication
   - `recoverRun()` async function checks run status via API
   - If run still "running"/"queued", continues polling
   - If run completed while away, updates pending message with result
   - Shows "recovering" badge in Agent Runtime card

2. **Updated ChatComposer**
   - Disabled during recovery: `isSending={isSending || isRecovering || !selectedModel}`

3. **Console logging for debugging**
   - Recovery progress logged to console panel
   - Shows run ID and status during recovery

---

## Files Modified

- `orgs/open-hax/knoxx/frontend/src/pages/ChatPage.tsx`
  - Added `ToolReceiptGroup` import
  - Added `isRecovering` state
  - Added `liveToolReceipts` and `liveToolEvents` memos
  - Added session recovery effect on mount
  - Added inline tool receipt rendering in message map
  - Added "recovering" badge in Agent Runtime card

- `orgs/open-hax/knoxx/frontend/src/components/ToolReceiptBlock.tsx` (NEW)
  - `ToolReceiptBlock` component for individual tool display
  - `ToolReceiptGroup` component for grouping receipts

---

## Testing

### Session Recovery Testing
1. Start a long-running agent request
2. Refresh the browser before completion
3. Expected: UI shows "recovering" badge, reconnects to run
4. Expected: If run completed, shows final result
5. Expected: If run still active, continues streaming

### Inline Tool Receipts Testing
1. Start an agent request that uses tools
2. Expected: Tool receipts appear inline above assistant message
3. Expected: Running tools show cyan border and "running" badge
4. Expected: Completed tools show green border
5. Expected: Collapsible input/output sections

---

## Future Improvements

1. **Backend: Add run events API endpoint**
   - `GET /api/runs/:runId/events` for SSE reconnection
   - Return recent events for clients that missed some

2. **Frontend: Better event merging**
   - Deduplicate events on reconnection
   - Show timeline of all events for completed runs

3. **Backend: Store active sessions in database**
   - Survive backend restarts
   - Allow multi-instance deployment
