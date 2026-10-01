---
title: "Session Resume After Restart Not Working"
category: ops
created: 2026-04-23
original: 2026.04.23.11.44.42.md
status: note
---

> **Status (2026-09-30): historical — implemented.** The requested isolated module exists as `knoxx.backend.infra.agent.resume` (`backend/src/cljs/knoxx/backend/infra/agent/resume.cljs`): stale threshold 10 minutes (`resume.cljs:26`), stale sessions aborted (`abort-stale-session!`, `resume.cljs:120`), non-blocking resume after listen (`infra/core.cljs:248-252`), and shutdown waits for turns and flushes (`infra/graceful_shutdown.cljs:65`). Persistence is Mongo (`infra/stores/mongo_session_store`), not Redis as written below.

knoxx's backend session resume after restarts is not working.

The system knows they were active, but they are never actually resumed, resulting in the interface
showing a bunch of active sessions that are not running.

The system has to wait for all requests to proxx through the pi sdk to complete, so a tool call, reasoning trace, or assistant message is completed and persisted to redis before the system is allowed to shut down.

Then when it starts back up, the system has to check redis for active agent sessions.

If an active session exists, and it's last event was recent (last 10 minutes) it must be resumed.
this resuming cannot block the start up of the app.

If a session marked as active is encountered and it is stale, then it must be stopped,
I think that you would do this via an abort action.


I want all of the logic for this in an isolated module something like agent_resume
