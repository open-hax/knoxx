# Agent Runtime Templates

## Overview

The Agent Runtime Templates system provides a DSL for defining reusable agent personas that can be instantiated into concrete event-agent jobs. This replaces the static configuration approach with a dynamic, template-driven architecture.

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ Template DSL    │────▶│ Job Instance     │────▶│ Redis (Hot)     │
│ (agent-templates│     │ (instantiate-job)│     │ event-agent:    │
│  .cljs)         │     │                  │     │ job-spec:*      │
└─────────────────┘     └──────────────────┘     └────────┬────────┘
                                                          │
                                                          ▼
                                                 ┌─────────────────┐
                                                 │ SQL (Cold)      │
                                                 │ Write-behind    │
                                                 │ (5min flush)    │
                                                 └─────────────────┘
```

## Key Concepts

### Model Profiles

Decouple templates from specific model strings to allow global updates:

```clojure
{:local-fast   {:model "gemma4:e4b" :thinking-level "off"}
 :local-mid    {:model "gemma4:31b" :thinking-level "off"}
 :cloud-heavy  {:model "glm-5"      :thinking-level "high"}}
```

### Templates

Reusable agent personas with prompts and policies:

```clojure
{:yap-bot
 {:role "creative_catalyst"
  :system-prompt "You are the Frankie-Infinite-Yap Bot..."
  :model-profile :local-fast
  :tool-policies [{:toolId "discord.publish" :effect "allow"}]
  :thinking-level "off"}}
```

### Job Instances

Concrete jobs created from templates with specific triggers and filters.

## Usage

### Template-Based Job Creation

```clojure
;; Using the event_agents.upsert_job tool
{
  "job_id": "frankie-yap-bot",
  "job_json": {
    "templateId": "yap-bot",
    "trigger": {
      "kind": "event",
      "cadenceMinutes": 1,
      "eventKinds": ["discord.message.mention", "discord.message.keyword"]
    },
    "source": {
      "kind": "discord",
      "mode": "respond",
      "config": {"maxMessages": 12}
    },
    "filters": {
      "channels": ["123456789"],
      "keywords": ["frankie", "knoxx"]
    }
  }
}
```

### Direct Spec (Advanced)

```clojure
;; Bypass templates for full control
{
  "job_id": "custom-bot",
  "job_json": {
    "id": "custom-bot",
    "enabled": true,
    "trigger": {"kind": "cron", "cadenceMinutes": 10},
    "agentSpec": {
      "role": "executive",
      "model": "glm-5",
      "thinkingLevel": "off",
      "systemPrompt": "Custom prompt...",
      "toolPolicies": [{"toolId": "discord.publish", "effect": "allow"}]
    }
  }
}
```

### Programmatic API (ClojureScript)

```clojure
(require '[knoxx.backend.event-agents :as event-agents])
(require '[knoxx.backend.agent-templates :as templates])

;; List available templates
(event-agents/list-templates)
;; => [:yap-bot :sentinel :summarizer :patrol-observer :mention-responder]

;; Create a job from template
(event-agents/upsert-job! 
  "frankie-yap-bot"
  {:templateId :yap-bot
   :trigger {:kind "event" 
             :cadenceMinutes 1 
             :eventKinds ["discord.message.mention"]}
   :filters {:channels ["123456"] :keywords ["frankie"]}})

;; Get a job spec (loads from Redis)
(event-agents/get-job "frankie-yap-bot")

;; Delete a job (removes Redis override)
(event-agents/delete-job! "frankie-yap-bot")
```

## Persistence Model

### Redis (Hot Store)
- **Source of truth** for running configuration
- Stores complete job specs: `event-agent:job-spec:<job-id>`
- Stores operational state: `event-agent:job-state:<job-id>`
- Stores Discord markers: `event-agent:discord-last-seen:<channel-id>`
- Dirty queue for SQL flush: `event-agent:job-dirty` (Set)

### SQL (Cold Store)
- Durable archive (future implementation)
- Write-behind flush every 5 minutes
- Prevents data loss on Redis flush/restart

### Config File (Seed)
- Baseline defaults only
- **Overridden by Redis** at runtime
- Used only when no Redis spec exists

## Preventing the "Reasoning Reset" Bug

The previous architecture loaded job specs from config files on every restart, losing runtime overrides. The new system:

1. **Writes to Redis first** via `update-job-spec!`
2. **Marks jobs dirty** for SQL flush
3. **Recovers from Redis** on startup (before config file)
4. **Normalizes thinking-level** explicitly in `normalize-job-for-persistence`

This ensures that `thinkingLevel: "off"` settings persist across restarts.

## Available Templates

| Template | Role | Model Profile | Thinking | Use Case |
|----------|------|---------------|----------|----------|
| `:yap-bot` | creative_catalyst | :local-fast | off | Creative, chaotic Discord presence |
| `:sentinel` | security_monitor | :cloud-heavy | high | Security monitoring, alerting |
| `:summarizer` | knowledge_synthesizer | :local-mid | off | Conversation synthesis |
| `:patrol-observer` | knowledge_worker | :local-fast | off | Silent channel observation |
| `:mention-responder` | executive | :local-fast | off | Targeted Discord responses |

## Available Model Profiles

| Profile | Model | Thinking | Use Case |
|---------|-------|----------|----------|
| `:local-fast` | gemma4:e4b | off | Low-latency responses |
| `:local-mid` | gemma4:31b | off | Better quality, local |
| `:local-heavy` | gemma4:31b | minimal | Local with light reasoning |
| `:cloud-heavy` | glm-5 | high | Complex reasoning tasks |
| `:cloud-fast` | glm-5-fast | off | Fast cloud responses |
| `:cloud-balanced` | glm-5 | minimal | Balanced cloud tasks |

## Migration Notes

### From Static Config to Templates

**Before:**
```clojure
;; runtime_config.cljs - hardcoded job
{:id "frankie-yap-bot"
 :agentSpec {:model "gemma4:e4b"
             :thinkingLevel "off"  ;; Lost on restart!
             ...}}
```

**After:**
```clojure
;; agent_templates.cljs - reusable template
{:yap-bot
 {:model-profile :local-fast  ;; Decoupled from specific model
  :thinking-level "off"}}     ;; Explicit, normalized

;; Runtime - instantiate with persistence
(event-agents/upsert-job! "frankie-yap-bot"
  {:templateId :yap-bot
   :filters {:channels ["123"]}})
```

## Future Enhancements

- [ ] SQL persistence implementation (currently logs to console)
- [ ] Template versioning and rollback
- [ ] Per-client template libraries
- [ ] Template inheritance/extension
- [ ] Admin UI for template management
- [ ] Template testing/simulation mode
