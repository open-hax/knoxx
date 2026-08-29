---
uuid: "knoxx-voice-tools-remote-transport"
title: "Rework the voice MCP tools for a remote client instead of an owned realtime harness"
status: incoming
priority: P3
labels: ["tasks", "8sp", "has-parent", "voice", "mcp"]
created_at: "2026-08-04T00:00:00Z"
points: 8
category: tasks
---
# Rework the voice MCP tools for remote use

> Parent epic: `knoxx-decouple-into-katamorph-contracts`

## Purpose

The voice tools were written against a harness we owned, where the caller could
queue and steer a live session. An MCP client cannot do that: it makes discrete
request/response calls, and Knoxx's MCP transport is stateless per request — each
POST builds a fresh server, so there is no session to steer.

## The actual constraint

`mcp-handle-post!` constructs a new `McpServer` and transport per request with
stateless options. `GET /mcp` and `DELETE /mcp` — the resumable-stream and
session-teardown routes — read `mcp-sessions*`, which **nothing populates**, so
they cannot work today (see the note in `knoxx-tool-namespace-boundary-audit`).
Any design that assumes a steerable live session needs that fixed first, and it
should be a deliberate choice rather than a side effect.

## Scope

- Decide the interaction model honestly:
  - **fire-and-forget**: `voice_tts` returns an artifact reference; no steering.
  - **job handle**: start returns an id; separate tools poll or cancel.
  - **true session**: requires real MCP session support (stateful transport plus
    populated `mcp-sessions*`), and only then can `GET /mcp` stream.
- Re-shape `voice_tts`, `voice_tts_stream`, and the Discord voice tools to the
  chosen model.
- Remove or clearly mark the tools that cannot work remotely, rather than
  advertising them and failing at call time.

## Done when

- Every advertised voice tool works from a remote MCP client, or is not
  advertised.
- Streaming, if kept, works through a mechanism that exists rather than one
  assumed.
