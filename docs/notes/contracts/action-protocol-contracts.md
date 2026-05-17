---
title: "μ Action Protocol — Contracts, Pipelines, Short-Term Memory"
category: contracts
created: 2026-04-27
original: 2026.04.27.13.21.01.md
status: note
---

I have everything I need — the full contract loader, `control_config`, the existing `discord_cron`, and the shape of the `:data` field. Here are the specs.

***

## #μ Action Protocol — Contracts, Pipelines, Short-Term Memory

**Design frame:** Discord is one *source-driver*. Cron is one *trigger-type*. Agents are one *executor-type*. These three axes must be orthogonal and composable via a pipeline contract that wires them together.

***

## Contract Taxonomy (Extended)

```
:contract/kind
  :agent        — existing, LLM executor
  :action       — NEW: deterministic, non-LLM executor (pure fn / tool call)
  :pipeline     — NEW: ordered sequence of steps (actions + agents)
  :trigger      — NEW: fires a pipeline on schedule or event (was: baked into :agent)
  :capability   — existing, tool surface
  :role         — existing, capability bundle
  :actor        — existing, principal identity
```

***

## #μ Malli Schemas

```clojure
(ns knoxx.contracts.schema
  (:require [malli.core :as m]))

;; ─── Shared primitives ────────────────────────────────────────────────────────

(def ContractId :string)
(def ToolId     :string)
(def ISODuration :string)  ;; "PT5M", "PT1H", etc.

(def ToolPolicy
  [:map
   [:toolId ToolId]
   [:effect [:enum "allow" "deny"]]])

(def DataShape
  "The :data field is now typed. Every contract kind
   extends this base with kind-specific sub-keys."
  [:map
   [:tools {:optional true} [:vector ToolId]]
   [:filters {:optional true}
    [:map
     [:channels     {:optional true} [:vector :string]]
     [:keywords     {:optional true} [:vector :string]]
     [:guildIds     {:optional true} [:vector :string]]
     [:repositories {:optional true} [:vector :string]]]]
   [:source {:optional true} :map]   ;; kind-specific
   [:context {:optional true} :map]  ;; static data injected into prompt/action
   [:output  {:optional true} :map]  ;; where to write results (key + ttl)
   ])

;; ─── :action contract ─────────────────────────────────────────────────────────
;; Non-LLM, deterministic. Executes a registered handler fn or tool call.

(def ActionContract
  [:map
   [:contract/kind   [:= :action]]
   [:contract/id     ContractId]
   [:action/handler  :string]   ;; namespaced handler key, e.g. "discord.read"
   [:action/params   {:optional true} :map]
   [:enabled         {:optional true} :boolean]
   [:data
    [:map
     [:context {:optional true} :map]
     [:output  {:optional true}
      [:map
       [:key   :string]    ;; short-term-memory key to write result into
       [:ttl   {:optional true} ISODuration]
       [:merge {:optional true} :boolean]]]]]])

;; ─── :pipeline contract ───────────────────────────────────────────────────────
;; Ordered sequence of steps. Each step refs a :action or :agent contract.
;; Data flows forward: each step can read :context from prior :output keys.

(def PipelineStep
  [:map
   [:step/id          :string]
   [:step/contract    ContractId]         ;; ref to :action or :agent contract id
   [:step/depends-on  {:optional true} [:vector :string]]  ;; step/ids
   [:step/condition   {:optional true} :string]  ;; SCI expr over context
   [:step/data        {:optional true}   ;; step-local data overrides
    [:map
     [:context {:optional true} :map]    ;; merged over pipeline-level context
     [:output  {:optional true}
      [:map
       [:key :string]
       [:ttl {:optional true} ISODuration]]]]]])

(def PipelineContract
  [:map
   [:contract/kind   [:= :pipeline]]
   [:contract/id     ContractId]
   [:pipeline/steps  [:vector PipelineStep]]
   [:enabled         {:optional true} :boolean]
   [:data
    [:map
     [:context {:optional true} :map]   ;; static seed context for all steps
     [:output  {:optional true}
      [:map [:key :string] [:ttl {:optional true} ISODuration]]]]]])

;; ─── :trigger contract ────────────────────────────────────────────────────────
;; Fires a pipeline (or single agent/action) on a schedule or event.
;; Completely decoupled from Discord.

(def TriggerContract
  [:map
   [:contract/kind     [:= :trigger]]
   [:contract/id       ContractId]
   [:trigger/kind      [:enum :cron :event :webhook :manual]]
   [:trigger/target    ContractId]  ;; :pipeline | :agent | :action contract id
   [:trigger/schedule  {:optional true} :string]  ;; cron expr or cadence-min int
   [:trigger/source    {:optional true}
    [:map
     [:kind    [:enum :discord :github :http :manual]]
     [:config  {:optional true} :map]]]
   [:enabled           {:optional true} :boolean]
   [:data
    [:map
     [:context {:optional true} :map]   ;; injected into pipeline/agent context
     [:filters {:optional true} :map]]]])
```

***

## #μ Short-Term Memory Tool

The tool is registered in the tools registry as `memory.temp`. Backed by Redis with TTL; no Redis = in-process `atom` fallback.

```clojure
(ns knoxx.backend.tools.temp-memory
  "Short-term memory tool. Agents read/write keyed blobs with TTL.
   Backed by Redis when available; falls back to a process-local atom."
  (:require [knoxx.backend.redis-client :as redis]
            ["shadow.cljs.modern" :refer [js-await]]))

(defonce ^:private local-store* (atom {}))

(def ^:private default-ttl-ms (* 60 60 1000)) ;; 1 hour

(defn- parse-ttl-ms
  "Accept ISO-8601 duration string or integer seconds."
  [ttl]
  (cond
    (int? ttl)    (* ttl 1000)
    (string? ttl) (let [[_ h m s] (re-find #"PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?" ttl)]
                    (+ (* (or (parse-long (str h)) 0) 3600000)
                       (* (or (parse-long (str m)) 0)  60000)
                       (* (or (parse-long (str s)) 0)   1000)))
    :else         default-ttl-ms))

(defn- now-ms [] (.now js/Date))

;; ─── local fallback ──────────────────────────────────────────────────────────

(defn- local-set! [k v ttl-ms]
  (swap! local-store* assoc k {:value v :expires-at (+ (now-ms) ttl-ms)})
  nil)

(defn- local-get [k]
  (let [{:keys [value expires-at]} (get @local-store* k)]
    (when (and value (> expires-at (now-ms)))
      value)))

(defn- local-del! [k]
  (swap! local-store* dissoc k)
  nil)

;; ─── public API ──────────────────────────────────────────────────────────────

(defn set!
  "Write `value` under `key` with optional `ttl` (ISO-8601 or seconds).
   Returns a Promise resolving to {:key k :written true}."
  [k v & [{:keys [ttl]}]]
  (let [ttl-ms (parse-ttl-ms (or ttl default-ttl-ms))]
    (if-let [client (redis/get-client)]
      (let [ttl-sec (max 1 (js/Math.ceil (/ ttl-ms 1000)))]
        (-> (redis/set-json client (str "temp-mem:" k) v {:ex ttl-sec})
            (.then (fn [_] {:key k :written true}))
            (.catch (fn [_]
                      (local-set! k v ttl-ms)
                      {:key k :written true :backend :local}))))
      (do (local-set! k v ttl-ms)
          (js/Promise.resolve {:key k :written true :backend :local})))))

(defn get
  "Read the value at `key`. Returns Promise<value | nil>."
  [k]
  (if-let [client (redis/get-client)]
    (-> (redis/get-json client (str "temp-mem:" k))
        (.catch (fn [_] (local-get k))))
    (js/Promise.resolve (local-get k))))

(defn del!
  "Delete `key`. Returns Promise<{:key k :deleted true}>."
  [k]
  (if-let [client (redis/get-client)]
    (-> (.del client (str "temp-mem:" k))
        (.then (fn [_] {:key k :deleted true}))
        (.catch (fn [_]
                  (local-del! k)
                  {:key k :deleted true :backend :local})))
    (do (local-del! k)
        (js/Promise.resolve {:key k :deleted true :backend :local}))))

;; ─── tool registration ───────────────────────────────────────────────────────

(def tool-spec
  {:id "memory.temp"
   :label "Temporary Memory"
   :description "Read or write short-lived keyed data with a TTL. Use for passing state between pipeline steps or caching within a session."
   :params {:op   {:type "string" :enum ["set" "get" "del"] :required true}
            :key  {:type "string" :required true}
            :value {:type "any"   :required false}
            :ttl  {:type "string" :description "ISO-8601 duration or integer seconds. Default PT1H."}}
   :handler (fn [{:keys [op key value ttl]}]
              (case op
                "set" (set! key value {:ttl ttl})
                "get" (get key)
                "del" (del! key)))})
```

***

## #μ Concrete Example Contracts

### Trigger (cron — no Discord coupling)

```edn
;; contracts/triggers/daily_synthesis.edn
{:contract/kind   :trigger
 :contract/id     "daily_synthesis"
 :trigger/kind    :cron
 :trigger/target  "synthesis_pipeline"
 :trigger/schedule "0 9 * * *"           ;; 09:00 daily, standard cron expr
 :enabled         true
 :data
 {:context {:timezone "America/Chicago"
            :report-window-hours 24}}}
```

### Action (fetch data, no LLM)

```edn
;; contracts/actions/fetch_discord_digest.edn
{:contract/kind   :action
 :contract/id     "fetch_discord_digest"
 :action/handler  "discord.read"
 :action/params   {:limit 50}
 :data
 {:context  {:channelId "{{trigger.context.channelId}}"}
  :output   {:key  "discord_digest"
             :ttl  "PT2H"}}}
```

### Pipeline (action → agent)

```edn
;; contracts/pipelines/synthesis_pipeline.edn
{:contract/kind  :pipeline
 :contract/id    "synthesis_pipeline"
 :enabled        true
 :pipeline/steps
 [{:step/id       "fetch"
   :step/contract "fetch_discord_digest"
   :step/data
   {:context {:channelId "1234567890"}
    :output  {:key "discord_digest" :ttl "PT2H"}}}

  {:step/id       "synthesize"
   :step/contract "daily_synthesis_agent"
   :step/depends-on ["fetch"]
   :step/data
   {:context {:digest "{{memory.temp:discord_digest}}"}}}]
 :data
 {:context  {:source "scheduled-synthesis"}
  :output   {:key "synthesis_result" :ttl "PT6H"}}}
```

### Agent (reads context populated by pipeline)

```edn
;; contracts/agents/daily_synthesis_agent.edn
{:contract/kind   :agent
 :contract/id     "daily_synthesis_agent"
 :trigger-kind    :manual            ;; fired by pipeline, not a self-scheduling cron
 :enabled         true
 :prompts
 {:system "You are Knoxx's synthesis agent. You receive a digest of recent activity and produce a concise structured summary."
  :task   "Analyze the provided digest and produce a structured synthesis. Use memory.temp to store key findings under 'synthesis_result'."}
 :data
 {:tools   ["memory.temp" "memory_search" "graph_query"]
  :context {}   ;; populated at runtime by pipeline runner from step context
  :output  {:key "synthesis_result" :ttl "PT6H"}}}
```

***

## What `:data` Now Means

| Sub-key | Meaning | Who reads it |
|---|---|---|
| `:data/tools` | Tool allowlist override (instead of role-derived) | `control-config/explicit-tool-ids` (already wired) |
| `:data/filters` | Source-level filters (channels, keywords, repos) | `control-config/filters-from-contract` (already wired) |
| `:data/source` | Source driver config (maxMessages, stickySession) | `contract->event-agent-job` (already wired) |
| `:data/context` | **NEW** — static KV injected into agent prompt and action params at runtime | Pipeline runner, trigger executor |
| `:data/output` | **NEW** — where to write the result (`:key` + `:ttl`); pipeline runner writes to `memory.temp` | Pipeline runner |

The existing `:data/tools`, `:data/filters`, and `:data/source` parsing is already present in `control_config.cljs` . `:data/context` and `:data/output` are additive — the pipeline runner resolves `{{memory.temp:key}}` template interpolations in `:context` maps before passing them to each step executor.

***

## Architectural Separation Summary

```
discord_cron.cljs  ──→  REMOVE job hardcoding
                         keep: discord I/O helpers (read-channel!, patrol-channel!, etc.)
                         rename: discord_io.cljs

trigger_runner.cljs  ──→  NEW: reads :trigger contracts, schedules setInterval/cron
                            dispatches to pipeline_runner.cljs or agent directly

pipeline_runner.cljs ──→  NEW: executes :pipeline/steps in dependency order
                            resolves {{memory.temp:*}} context templates
                            writes :data/output keys via memory.temp tool

temp_memory.cljs     ──→  NEW: above implementation
```

The `discord_cron.cljs` `patrol`/`mentions`/`deep-synthesis` hardcoded jobs become three `:trigger` + `:pipeline` (or `:agent`) contract files — admin-dashboard tunable, version-controlled, and completely non-special .
