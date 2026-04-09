# Knoxx Agent Runtime Workbench

## Why this shape

Knoxx should not feel like a thin chat box taped onto a retrieval API.
It should feel like a **workbench**: an agent runtime with memory, witness trails, explicit tools, and a visible working surface.

This design borrows the most useful motifs from the surrounding corpus without turning the UI into unreadable lore.

- **Presence** = conversation continuity and runtime liveness
- **Witness Thread** = grounded sources, passive hydration, and evidence surfaces
- **Receipt River** = run history, tool receipts, and event timeline
- **File Sentinel** = context explorer / left rail / active corpus navigation
- **Mycelial Loom** = scratchpad + pinned context + artifact growth surface
- **Gates of Truth** = explicit errors, no fake fallback success, visible receipts

Source anchors that shaped this frame:
- `docs/sing.v3.md`
- `/home/err/docs/notes/research/creative-protocol-manifest.md`
- `/home/err/docs/notes/poetry/prelude-to-epiphany.md`
- `docs/reports/ui-pattern-extraction-analysis.md`

## Runtime contract

A full Knoxx turn should expose all of these layers:

1. **Continuity**
   - conversation id
   - session id
   - model used
   - websocket/runtime presence state

2. **Agent execution**
   - run id
   - status
   - time-to-first-token
   - total runtime
   - tool execution receipts
   - turn boundary events

3. **Grounding**
   - passive hydration results
   - explicit semantic tool calls
   - readable source previews
   - user pinning into the scratchpad

4. **Artifact production**
   - scratchpad as the active working surface
   - insert/open/pin actions from context and receipts
   - email/export as optional downstream actions, not the core loop

## Landed shape

The current workbench now has these properties:

- root `/` chat uses the CLJS agent runtime instead of the old stateless query endpoint
- multi-turn memory is keyed by `conversation_id`
- websocket token streaming can update the pending assistant message while the HTTP request is in flight
- runs now store:
  - `events`
  - `tool_receipts`
  - `session_id`
  - `conversation_id`
  - passive hydration metadata
  - source receipts
- the root page now shows:
  - **Agent Runtime** panel
  - **Presence** section
  - **Witness Thread** section
  - **Receipt River** section
  - **Tool Receipts** section
- nginx is configured to allow long-running `/api/` agent turns instead of timing out at default short proxy windows
- Proxx upstream request and stream-bootstrap patience is widened substantially for long cold-start / remote mountain-latency turns
- the root workbench now exposes live intervention controls:
  - steer current turn
  - queue follow-up

## Interaction doctrine

### 1. Chat is orchestration
The center pane is not just for Q&A. It is where the user steers an agentic turn.

### 2. The left rail is not navigation chrome
It is the **context bar**:
- browse corpus
- inspect preview
- semantic search
- pin working context
- see what the agent had access to

### 3. The right rail is not an email composer
It is the **scratchpad**:
- collect snippets
- shape deliverables
- stage long-form edits
- optionally deliver outward

### 4. Receipts are first-class UI
The user should be able to answer:
- what did the agent look at?
- what tools did it call?
- what took time?
- what was inferred vs witnessed?

without reading server logs.

## Next upgrades

### Near-term
- add explicit streaming assistant state in the main transcript, not just final reconciliation
- expose turn-level tool results more cleanly than raw preview blobs
- allow opening a run receipt directly into the scratchpad
- show when a response was passive-hydration-only vs explicit-tool-grounded
- add cancel / abort controls alongside steer and follow-up

### Medium-term
- add `steer` / `follow_up` controls for live intervention during a running turn
- support session branching / forked conversations from a prior run
- surface compaction and context-pressure indicators before memory gets muddy
- allow runtime presets by role/mode rather than one freeform steering note

### Full Promethean form
- the workbench becomes a visible agent loop:
  - observe
  - gather witnesses
  - act with tools
  - emit receipts
  - synthesize into artifact
  - leave the artifact in the scratchpad

That is the point where Knoxx stops being “chat over docs” and becomes a genuine local agent workstation.
