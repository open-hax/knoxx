---
title: "Contract Runtime — Validation Layer Design"
category: contracts
created: 2026-04-17
original: 2026.04.17.10.11.44.md
status: note
---

> **Status (2026-09-30):** Historical design. Only part of it was realised. Agents get `contract.list`, `contract.read`, `contract.write` and `contract.validate` (`backend/src/cljs/knoxx/backend/infra/registry/tools.cljs:99-102`, `domain/contracts/tools.cljs`, `contracts/capabilities/cap_contract_write.edn`). There is no `contract.repair`, and no bracket-repair (`contract.bracket`) or SCI-whitelist (`contract.sci`) namespace exists in `backend/src`. The `Contract` schema here (required `:trigger-kind`/`:source-kind`, `:cadence-min`, `:hooks`) is superseded by `open_hax/contracts/schema.cljs` and by the trigger-field removal note (`agent-contract-trigger-field-removal.md`).

## Signal

The current runtime reads jobs from `control-config` as raw JS-shaped maps and never validates them.  The new surface needs three layers stacked in front of `start-agent-run!`: a **bracket repair pass**, a **parse pass**, and a **schema validation pass** — and the tool calling surface exposed to agents should return all three layers' feedback as structured data in one response.

***

## A — `contract.edn` Malli Schemas

```clojure
(ns knoxx.backend.contract.schema
  "Malli schemas for agent contracts.
   These are the canonical shapes — both the HTTP API and the
   agent tool surface validate against these.
   
   Field registry is separate (contract.field-registry) and drives
   the admin UI widget layer."
  (:require [malli.core :as m]))

;; ── Primitives ────────────────────────────────────────────────────────────────

(def ContractId
  [:and :string [:min-count 1] [:max-count 128]])

(def Version
  [:int {:min 1 :max 9999}])

(def TriggerKind
  [:enum :cron :event :manual])

(def SourceKind
  [:enum :discord :github :cron :manual :http :rss])

(def ThinkingLevel
  [:enum :off :minimal :low :medium :high :xhigh])

(def EventKind
  ;; Open keyword — validated loosely; exhaustive list lives in the registry.
  [:and :keyword [:fn {:error/message "must be a namespaced keyword like :discord/mention"}
                  #(namespace %)]])

;; ── Expr ──────────────────────────────────────────────────────────────────────
;; An expr is a quoted form validated at read time, not execution time.
;; Execution-time whitelisting is in contract.sci.

(def ExprNode
  [:map {:closed false}
   [:expr {:optional true} :any]
   [:fn-ref {:optional true} :keyword]])

;; ── Prompts ───────────────────────────────────────────────────────────────────

(def PromptValue
  [:or :string ExprNode])

(def Prompts
  [:map {:closed false}
   [:system  {:optional true} PromptValue]
   [:task    {:optional true} PromptValue]
   [:user    {:optional true} PromptValue]])

;; ── Events ────────────────────────────────────────────────────────────────────

(def EventsBlock
  [:map {:closed false}
   [:always {:optional true} [:vector EventKind]]
   [:maybe  {:optional true} [:vector EventKind]]])

;; ── Hook ──────────────────────────────────────────────────────────────────────

(def HookNode
  [:map {:closed false}
   [:expr    {:optional true} :any]
   [:fn-ref  {:optional true} :keyword]])

(def HookMap
  [:map-of :keyword HookNode])

(def Hooks
  [:map {:closed false}
   [:before {:optional true} HookMap]
   [:after  {:optional true} HookMap]])

;; ── Agent block ───────────────────────────────────────────────────────────────

(def AgentBlock
  [:map {:closed false}
   [:role    {:optional true} :keyword]
   [:model   {:optional true} :string]
   [:thinking {:optional true} ThinkingLevel]])

;; ── Data block ────────────────────────────────────────────────────────────────

(def DataBlock
  [:map {:closed false}
   [:source  {:optional true} [:map-of :keyword :any]]
   [:filters {:optional true} [:map-of :keyword :any]]
   [:tools   {:optional true} [:vector :any]]])

;; ── Top-level Contract ────────────────────────────────────────────────────────

(def Contract
  [:map {:closed false}
   [:contract/id      ContractId]
   [:contract/version {:optional true} Version]
   [:enabled          {:optional true} :boolean]
   [:trigger-kind     TriggerKind]
   [:source-kind      SourceKind]
   [:source-mode      {:optional true} :string]
   [:cadence-min      {:optional true} [:int {:min 1 :max 10080}]]
   [:agent            {:optional true} AgentBlock]
   [:prompts          {:optional true} Prompts]
   [:events           {:optional true} EventsBlock]
   [:data             {:optional true} DataBlock]
   [:hooks            {:optional true} Hooks]])
```

***

> **Moved 2026-09-30:** Agent-authored EDN repair and SCI expression whitelist now lives in Foresight at [`docs/research/agent-authored-edn-repair-and-expression-whitelist.md`](https://github.com/open-hax/foresight/blob/main/docs/research/agent-authored-edn-repair-and-expression-whitelist.md) (relationship: alpha, katamorph).

## D — Agent Tool Surface: `contract-runtime` tools

These replace the current `event_agents` tool calling surface.  The old surface takes a job-id and fires — no validation, no feedback. The new surface takes EDN text and returns structured results at each pipeline stage.

```clojure
(ns knoxx.backend.contract.tools
  "MCP/tool-route handlers for the contract runtime.
   
   Replaces the job-centric surface in event-agents with a
   contract-centric surface. Agents interact with contracts
   as EDN text, not as JSON job specs.
   
   Tool schema (for MCP bridge / tool-routes):
   
     contract/validate   — parse + bracket + schema validation
     contract/repair     — attempt autocorrect, return diff
     contract/save       — validate then persist
     contract/run        — validate then execute immediately
     contract/list       — list active contracts with status
     contract/get        — fetch one contract as EDN text
     contract/delete     — delete contract by id
   
   All tools return a standard ContractResult envelope."
  (:require [knoxx.backend.contract.bracket :as bracket]
            [knoxx.backend.contract.schema  :as schema]
            [knoxx.backend.contract.sci     :as sci]
            [malli.core  :as m]
            [malli.error :as me]
            [clojure.string :as str]))

;; ── ContractResult envelope ───────────────────────────────────────────────────
;;
;; Every tool returns this shape. Agents should check :ok first.
;; On failure, :errors + :advice tell them exactly what to fix.
;; On success, :contract-id + :contract let them reference the result.

(defn- result
  ([ok contract-id]
   {:ok ok :contract-id contract-id})
  ([ok contract-id extra]
   (merge {:ok ok :contract-id contract-id} extra)))

;; ── contract/validate ─────────────────────────────────────────────────────────

(defn tool-validate
  "Parse and validate EDN contract text. Does NOT persist.
   
   Input:  {:edn-text str}
   Output: ContractResult with :stages {
             :bracket {:ok bool :errors [...] :diagnostic str}
             :parse   {:ok bool :error str | nil}
             :schema  {:ok bool :errors [...] | nil}
             :sci     {:ok bool :violations [...] | nil}
           }
   
   Agent advice: the :advice key on any failing stage gives a direct
   instruction the agent can follow to fix the problem."
  [{:keys [edn-text]}]
  (let [;; Stage 1 — bracket balance
        bk-report  (bracket/diagnose edn-text)
        bk-diag    (bracket/format-diagnostic bk-report)

        ;; If brackets are broken, skip parse and schema
        parse-result
        (when (:ok bk-report)
          (try {:ok true :value (cljs.reader/read-string edn-text)}
               (catch :default e {:ok false :error (ex-message e)})))

        ;; Stage 3 — schema validation
        schema-result
        (when (and parse-result (:ok parse-result))
          (let [raw (:value parse-result)]
            (if (m/validate schema/Contract raw)
              {:ok true}
              {:ok     false
               :errors (-> (m/explain schema/Contract raw) me/humanize)})))

        ;; Stage 4 — sci whitelist check on all :expr nodes
        sci-result
        (when (and parse-result (:ok parse-result))
          (let [raw    (:value parse-result)
                exprs  (for [path [[:prompts :user] [:prompts :task] [:prompts :system]
                                   [:hooks :before] [:hooks :after]]
                             :let [v (get-in raw path)]
                             :when (and (map? v) (or (:expr v) (:fn-ref v)))]
                         v)
                violations (mapcat (fn [expr-node]
                                     (:violations (sci/check-whitelist (:expr expr-node))))
                                   exprs)]
            (if (empty? violations)
              {:ok true}
              {:ok         false
               :violations violations
               :advice     "Use only whitelisted contract ops. Run contract/list-ops for the full list."})))

        all-ok (and (:ok bk-report)
                    (some-> parse-result :ok)
                    (some-> schema-result :ok)
                    (some-> sci-result :ok) true)

        contract-id (when (and parse-result (:ok parse-result))
                      (:contract/id (:value parse-result)))]

    (result all-ok contract-id
            {:stages
             {:bracket (assoc bk-report :diagnostic bk-diag)
              :parse   (or parse-result {:ok :skipped :reason "bracket errors present"})
              :schema  (or schema-result {:ok :skipped :reason "parse not attempted"})
              :sci     (or sci-result {:ok :skipped :reason "parse not attempted"})}
             :advice
             (cond
               (not (:ok bk-report))
               (str "Fix bracket errors first:\n" bk-diag
                    "\n\nTip: run contract/repair to attempt autocorrect.")

               (and parse-result (not (:ok parse-result)))
               (str "EDN parse failed: " (:error parse-result)
                    "\nCheck for: unquoted symbols, missing commas in maps, "
                    "invalid keyword syntax.")

               (and schema-result (not (:ok schema-result)))
               (str "Schema errors:\n"
                    (str/join "\n" (map #(str "  • " %) (flatten (vals (:errors schema-result)))))
                    "\n\nRequired top-level keys: :contract/id, :trigger-kind, :source-kind")

               (and sci-result (not (:ok sci-result)))
               (str "Whitelist violations in :expr blocks:\n"
                    (str/join "\n" (map #(str "  • " (:sym %) ": " (:reason %))
                                        (:violations sci-result)))
                    "\n\n" (:advice sci-result))

               :else nil)})))

;; ── contract/repair ───────────────────────────────────────────────────────────

(defn tool-repair
  "Attempt to autocorrect bracket errors in EDN text.
   Returns the repaired text + a list of changes made.
   
   Input:  {:edn-text str}
   Output: {:ok bool :repaired-text str :changes [...] :validate-after ContractResult}
   
   Always re-validates after repair so the agent knows what's still broken."
  [{:keys [edn-text]}]
  (let [{:keys [text changes]} (bracket/repair edn-text)
        validate-result        (tool-validate {:edn-text text})]
    {:ok              (:ok validate-result)
     :repaired-text   text
     :changes         changes
     :validate-after  validate-result
     :advice          (if (:ok validate-result)
                        "Repair successful — contract is valid. Run contract/save to persist."
                        (str "Repair fixed bracket structure but further errors remain:\n"
                             (:advice validate-result)))}))

;; ── contract/list-ops ────────────────────────────────────────────────────────

(defn tool-list-ops
  "List all allowed ops in contract :expr blocks.
   Use this to know what you can call before writing an :expr."
  [_]
  {:ok      true
   :allowed-ops
   (sort (map str (keys sci/WHITELIST)))
   :advice
   "Use :fn-ref :your.ns/fn-name to call custom fns registered in the fn-registry."})
```

***

## E — What Changes in `event-agents`

The current [`event-agents/start-agent-run!`](https://github.com/open-hax/knoxx/blob/e8ae642c9cb3e1c74a1094e8eeafbf806c408a60/backend/src/cljs/knoxx/backend/event_agents.cljs#L1) builds a JS body map directly from raw job fields.  Under the new surface that function gets replaced by a thin adapter:

```clojure
;; In event_agents.cljs — replaces start-agent-run!
;; The contract is already validated by the time it arrives here.

(defn- contract->run-body
  "Translate a loaded contract map into the /api/knoxx/direct/start body."
  [contract event now]
  (let [agent     (:agent contract {})
        prompts   (:prompts contract {})
        run-id    (str "contract-" (:contract/id contract) "-" now)
        conv-id   (str "contract-" (:contract/id contract) "-" now)
        user-msg  (let [user-prompt (:user prompts)]
                    (cond
                      (string? user-prompt) user-prompt
                      (map? user-prompt)
                      ;; eval the :expr form with event in ctx
                      (let [r (sci/eval-expr user-prompt
                                             {:event event :state {} :result {}}
                                             {:contract-id (:contract/id contract)})]
                        (if (:ok r) (:value r) (event-summary-text event)))
                      :else (event-summary-text event)))]
    #js {:conversation_id conv-id
         :session_id      (str "session-" run-id)
         :run_id          run-id
         :message         user-msg
         :agent_spec
         #js {:role          (name (or (:role agent) :knowledge-worker))
              :system_prompt (let [sp (:system prompts)]
                               (if (string? sp) sp "You are a Knoxx contract agent."))
              :model         (or (:model agent) "glm-5")
              :thinking_level (name (or (:thinking agent) :off))
              :tool_policies (clj->js (get-in contract [:data :tools] []))}
         :model (or (:model agent) "glm-5")}))
```

The net change is: 

| Old surface | New surface |
|---|---|
| `upsert-job!` takes a camelCase JS map | `contract/save` takes EDN text, validates all four stages first |
| Errors surface as raw JS exceptions at `start-agent-run!` time | Errors surface immediately as structured `:stages` data at save time |
| No bracket awareness at all | Bracket scanner + autocorrect before parse |
| No whitelist — any fn in `:expr` runs | Explicit whitelist enforced before sci eval |
| Agents write programs to count parens | `contract/repair` + `contract/validate` are the tool calls instead |

***

## Frames

The repair pass being a **tool call that returns structured data** — not a side-effecting "fix and save" — is important. Agents need to *see the diff* between what they wrote and what was repaired before trusting the save. The `tool-repair` output gives them `repaired-text` + `changes` + `validate-after` in one shot, so they can confirm or reject the repair explicitly.
