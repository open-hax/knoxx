---
uuid: "knoxx-streaming-behavior-action-args"
title: "Implement streamingBehavior as action argument"
status: done
priority: "P1"
labels: ["tasks", "3sp", "action-scope-and-pipeline-collapse"]
created_at: "2026-06-09T00:00:00Z"
source: "docs/design/action-scope-and-pipeline-collapse.md"
points: 3
category: "tasks"
---
# Implement streamingBehavior as Action Argument

> Parent epic: `knoxx-action-scope-and-pipeline-collapse`
> **Blocked by:** `knoxx-action-scope-injection` — requires scope map to call steer/follow-up actions

## Context

The `streamingBehavior` / `steamingBehavior` field on agent contracts is dead code. `start_agent_session.cljs` never reads it. With scope injection, it becomes a clean action argument.

## Work

### 1. Create `:actions/agent-control` action

Instead of two separate actions, create one parameterized action:

```clojure
(defmethod run-action! :actions/agent-control
  [ctx action]
  (let [kind (get-in action [:action/with :kind]) ;; "steer" or "follow_up"
        conversation-id (:conversation-id ctx)
        message (or (get-in action [:action/with :message])
                    (get-in ctx [:event :event/payload :content])
                    (get-in ctx [:event :event/payload :text])
                    "")
        session-id (some-> conversation-id active-agent-session .-sessionId)
        run-id (get-in ctx [:event :event/id])]
    (await (queue-agent-control! nil nil
              {:conversation-id conversation-id
               :session-id session-id
               :run-id run-id
               :message message
               :kind kind}))))
```

This avoids duplicating event logging, run-state updates, and WebSocket broadcast logic. The `:action/scope` on `start-agent-session` includes `:actions/agent-control`.

**Message sourcing:** The message to steer/follow-up with comes from the event payload's `:content` or `:text` (the Discord message text that triggered the event). This is the natural source — the user's message becomes the steer content.

### 2. Update `:actions/start-agent-session`

Before calling `spawn-direct!`, check session state:

```clojure
(let [streaming-behavior (keyword (get-in action [:action/with :streaming-behavior] :queue))
      sticky? (boolean (get-in action [:action/with :sticky-session?] false))
      conversation-id (:conversation-id ctx)
      agent-session (active-agent-session conversation-id)]
  (cond
    ;; Session active + steer/follow-up requested
    (and agent-session (streaming? agent-session)
         (#{:steer :follow-up} streaming-behavior))
    (try
      ;; Attempt steer/follow-up
      ((get-in ctx [:scope :actions/agent-control])
       (assoc ctx :action/with {:kind (name streaming-behavior)}))
      (catch :default err
        ;; TOCTOU fallback: session finished between check and call
        (if (str/includes? (.-message err) "No active running turn")
          ;; Fall back to spawning a new session
          (spawn-direct! ...)
          (throw err))))
    
    ;; Drop
    (= streaming-behavior :drop)
    {:ok false :dropped true}
    
    ;; Queue or default — spawn directly (will hit busy-gate if sticky session exists)
    ;; Note: :queue for sticky sessions results in busy error (same as current default)
    :else
    (spawn-direct! ...)))
```

**Key:** Use `active-agent-session` (in-memory) for the steer/follow-up check, not `session-store/get-session` (document). This avoids attempting to steer a session that `queue-agent-control!` would reject.

### 3. Move runtime fields out of agent contracts

For `contracts/agents/ussyverse_social_replies.edn`:
- Remove `:steamingBehavior` from `:data`
- Remove `:stickySession` from `:data`
- Remove `:sessionMaxMessages` from `:data`
- Keep only: system prompt, roles, model, capabilities, tool policies

For `contracts/agents/ussyverse_social_creative.edn`:
- Same cleanup

### 4. Add runtime fields to trigger contracts

For `contracts/triggers/ussyverse_social_replies_event.edn`:
- Add `:trigger/with {:streaming-behavior :steer :sticky-session? true :session-max-messages 1000}`

For `ussyverse_social_creative`:
- The only trigger is `ussyverse_social_creative_cron.edn` (schedule trigger)
- **Decision needed:** Should cron-fired sessions be sticky? If not, no trigger changes needed for that agent

### 5. Update `start-agent-session` source config

`sticky-session-source?` currently reads from agent contract `:data :source`. Change to read from `(:action/with action)`:
- `:sticky-session?` from `:action/with` (default false)
- `:session-max-messages` from `:action/with` (default nil, use context policy)

## Definition of Done

- Triggered run with sticky session + `:steer` queues a steer instead of busy error
- Triggered run with `:follow-up` queues follow-up
- TOCTOU race handled: steer failure falls back to spawn
- Agent contracts cleaned of `:steamingBehavior`, `:stickySession`, `:sessionMaxMessages`
- `pnpm -C backend exec shadow-cljs compile test` passes
- Manual smoke test passes

## Risks

- Changing session busy-gate logic could break non-sticky sessions — test with non-sticky triggers
- `:queue` behavior: sticky sessions with `:queue` will continue to get busy errors (same as current default)
- `ussyverse_social_creative` cron trigger: decide if it needs sticky-session changes
