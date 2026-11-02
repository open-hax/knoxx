---
uuid: "knoxx-discord-voice-teardown-347"
title: "Clean up Discord voice resources on gateway teardown"
status: "incoming"
type: "task"
priority: "P1"
dependency: []
category: "tasks"
write-id: "1793661992768-0.mhkylp9igdb586suna"
created_at: "2026-11-02T23:26:32.768Z"
---

## Outcome
Stop active voice listeners and connections when the Discord gateway is stopped, restarted or replaced.

## Destination
Standalone story for Discord gateway extraction, PR https://github.com/open-hax/knoxx/pull/347; no existing Discord epic found.

## Scope
Offline lifecycle and voice cleanup only. No credentials, option policy, deployment or live Discord access.

## Acceptance criteria
- Stop, restart and token replacement clean listeners and voice connections.
- Caller stop remains safe after manager teardown; late listener registration cannot escape shutdown.
- Leave waits for its operation and propagates rejection.
- Offline tests cover lifecycle transitions and listener race without network access.

## Verification
Run guarded focused tests, backend test compile, production compile and typecheck. Report live verification separately.
