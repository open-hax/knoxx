---
uuid: "knoxx-event-agent-transport-359"
title: "Preserve configured event agent transport"
status: "incoming"
type: "task"
priority: "P1"
dependency: []
category: "tasks"
write-id: "1793663416711-0.dwywvky7bbw5lnjr62h"
created_at: "2026-11-02T23:50:16.711Z"
---

## Outcome
Preserve configured event-agent transport behavior for the frontend.

## Destination
Standalone story for frontend agents/contracts migration, PR https://github.com/open-hax/knoxx/pull/359.

## Scope
Restore the existing transport bridge and verify actual emitted bridge behavior offline. No authorization policy or credential changes.

## Acceptance criteria
- API base, existing identity defaults and credentials survive event-agent calls.
- CLJS wrapper passes bodies and decodes responses through the actual emitted bridge.
- Errors and encoded identifiers are covered; missing module or zero executed tests cannot pass.

## Verification
Build bridge and execute guarded tests with synthetic configuration and mocked fetch; run relevant frontend checks.
