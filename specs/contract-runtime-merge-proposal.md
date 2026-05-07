# Contract Runtime Unification Proposal

**Status:** draft
**Created:** 2026-05-06
**Scope:** Merge proxx, knoxx, and eta-mu extension/contract runtimes into a single canonical system

---

## 1. Architecture Analysis

### 1.1 Proxx — Provider Policy Engine

| Dimension | Detail |
|---|---|
| **Language** | ClojureScript (compiled to ESM via shadow-cljs), TypeScript host wrapper |
| **Contract format** | EDN maps with `:contract/id` (keyword), `:contract/kind` |
| **Schema system** | Malli schemas in `proxx.schema` |
| **Contract kinds** | `:policy`, `:strategy`, `:model-family`, `:model`, `:provider`, `:provider-endpoint`, `:provider-credential` |
| **Eval engine** | Custom `proxx.policy.eval/eval-form*` — handles `=`, `not`, `and`, `or`, `get`, `get-in`, `contract/apply` |
| **Policy tree** | Nested nodes: `:policy/condition`, `:policy/filters`, `:policy/outcome` (`:next`, `:reduce`, `:apply`, `:try`), `:policy/children`, `:policy/sort`, `:policy/project` |
| **Router** | `proxx.policy.router/route-request!` — walks tree, first-pass-wins backtracking |
| **Strategy registry** | Dynamic atom: `register-strategy!` maps symbols to CLJS fns |
| **Manifest** | Ordered phase files: `00-domain.edn` → `90-router.edn` under `resources/policies/runtime/` |
| **Validation boundary** | `schema/assert!` at ingest (JSON seeder), runtime `validate-entity` for smoke checks |
| **JS bridge** | `cljs-runtime.ts` exports `normalizeKeys`, `validateEntity`, `projectPheromone`, `routePolicy`, `previewPolicyDecision` |
| **Purpose** | Provider routing, model selection, credential filtering, quota scoring, pheromone-based adaptation |

**Key strengths:** Mature policy-tree evaluator with backtracking, strategy dispatch, filter/projection/sort operations, trace recording.

**Key gaps:** No event/extension model, no agent/actor/role abstraction, no prompt injection, no tool-call gating.

---

### 1.2 Knoxx — Agent Contract Runtime

| Dimension | Detail |
|---|---|
| **Language** | ClojureScript (shadow-cljs), with backward-compat shims in `runtime/` |
| **Contract format** | EDN files on disk under `contracts/{kind}/{id}.edn` |
| **Schema system** | Malli schemas in `contracts/validator.cljs` (per-kind schemas) |
| **Contract kinds** | `agents`, `actors`, `roles`, `capabilities`, `policies`, `model_families`, `models`, `ingest_sources`, `actions`, `pipelines`, `triggers` |
| **Loader** | `contracts/loader.cljs` — recursive `readdir`, first-wins dedup on `[kind id]`, multi-root resolution |
| **Validator** | `contracts/validator.cljs/validate` — kind-dispatched Malli validation |
| **Resolver** | `contracts/resolve.cljs/resolve-agent-contract` — composes actor→role→capability→tool-id chain |
| **Actor scoping** | `contracts/actor_scope.cljs` — wildcard actor `:*`, actor-allowed? gating |
| **Extension runtime** | `extension_runtime.cljs` — event bus (`session_start`, `turn_start`, `before_agent_start`, `context`, `turn_end`, `session_shutdown`), slash-command registry, prompt injection |
| **Context building** | `build-extension-ctx` produces JS context object with `:cwd`, `:model`, `:sessionId`, etc. |
| **Purpose** | Agent persona configuration, RBAC, model/thinking selection, tool permission, prompt assembly, event dispatch |

**Key strengths:** Comprehensive agent/actor/role/capability RBAC chain, mature EDN loader with multi-root discovery, prompt injection via extension events.

**Key gaps:** Extension runtime is a thin stub (empty built-in list), no policy evaluation engine, no tool-call gating, no fulfillment scoring.

---

### 1.3 Eta-mu — Extension Contract Runtime

| Dimension | Detail |
|---|---|
| **Language** | ClojureScript with `eta-mu.core` macro DSL, compiled to Pi and OpenCode targets |
| **Extension API** | `em/defextension` → `:name`, `:description`, `:init`, `:commands`, `:tools`, `:events` |
| **Contract Runtime v1** | `contract_runtime.cljs` — discovers `CONTRACT.edn` from skill directories, evaluates `fulfillment-score` forms via custom CLJS evaluator |
| **Contract Runtime v2** | `contract_runtime_v2.cljs` — upward-walk CONTRACT.edn discovery, EDN dispatch (actor/policy/fulfillment/capability/role), policy gate on `before_tool_call`, fulfillment notify/audit on `after_tool_call`, PRINCIPLE.edn bootstrap, prompt section injection |
| **Eval engine v1** | Custom `eval-expr` with `builtin-env` (arithmetic, collections, `fn`, `let`, `if`, `do`) |
| **Eval engine v2** | Pure data-driven: `policy-matches?` on `:policy/match` maps, `fulfillment-matches?` on `:fulfillment/match` maps |
| **Output Contract Gate** | TypeScript: `output-contract-gate` — EDN contract compilation, markdown AST validation, repair prompts |
| **Signal Contracts** | TypeScript Zod schemas: `EtaBelief`, `MuCandidate`, `EtaMuState`, `EtaMuPlanningContext` |
| **State management** | Per-cwd atom registry on `globalThis`, SHA-based cache with TTL |
| **Purpose** | Agent extension lifecycle, tool-call policy gating, fulfillment auditing, skill contract evaluation, prompt injection |

**Key strengths:** Mature extension macro DSL, tool-call gating (before/after hooks), fulfillment notification, upward-walk discovery, prompt section injection, SHA caching.

**Key gaps:** Contract format is unstructured EDN (no Malli schemas), no agent/actor/role model, policy matching is flat (no tree backtracking), no strategy dispatch.

---

## 2. Convergence Map

### 2.1 What overlaps

| Concept | Proxx | Knoxx | Eta-mu |
|---|---|---|---|
| EDN contracts with `:contract/id` | ✅ | ✅ | ✅ (partial) |
| Malli schema validation | ✅ | ✅ | ❌ (custom) |
| Policy evaluation | ✅ (tree) | ❌ | ✅ (flat) |
| Event/extension model | ❌ | ✅ (stub) | ✅ (mature) |
| Agent/actor/role model | ❌ | ✅ | ❌ |
| Tool-call gating | ❌ | ❌ | ✅ |
| Fulfillment scoring | ❌ | ❌ | ✅ |
| Strategy dispatch | ✅ | ❌ | ❌ |
| Prompt injection | ❌ | ✅ (events) | ✅ (sections) |
| File discovery | ❌ (manifest) | ✅ (recursive) | ✅ (walk-up) |
| JS interop bridge | ✅ | ✅ (shims) | ✅ (macros) |

### 2.2 What's unique to each

| System | Unique contribution |
|---|---|
| **Proxx** | Policy tree with backtracking (`:reduce`/`:some`), strategy registry, filter/project/sort operations, pheromone scoring |
| **Knoxx** | Actor→role→capability→tool RBAC chain, contract-scoped actor resolution, action/pipeline/trigger orchestration |
| **Eta-mu** | Extension macro DSL, before/after tool-call hooks, fulfillment notification, upward-walk discovery, SHA caching, PRINCIPLE.edn bootstrap |

---

## 3. Unified Architecture Proposal

### 3.1 Layer Model

```
┌─────────────────────────────────────────────────────────┐
│  Layer 4: Extension Runtime (eta-mu macros)             │
│  defextension, commands, tools, event hooks             │
├─────────────────────────────────────────────────────────┤
│  Layer 3: Agent Runtime (knoxx resolve)                 │
│  actor → role → capability → tool-id resolution         │
│  prompt assembly, model/thinking selection              │
├─────────────────────────────────────────────────────────┤
│  Layer 2: Policy Engine (proxx eval + eta-mu v2 gate)   │
│  policy tree eval, tool-call gating, fulfillment audit  │
│  strategy dispatch, filter/project/sort                 │
├─────────────────────────────────────────────────────────┤
│  Layer 1: Contract Store (knoxx loader + eta-mu walk)   │
│  EDN file discovery, Malli validation, SHA caching      │
│  multi-root, upward-walk, class normalization           │
└─────────────────────────────────────────────────────────┘
```

### 3.2 Unified Contract Format

All contracts are EDN maps. The unified schema registry:

```clojure
(ns open-hax.contracts.schema
  "Unified contract schema registry."
  (:require [malli.core :as m]))

;; ── Primitives ──────────────────────────────────────────

(def ContractId    [:or string? keyword?])
(def ToolId        :string)
(def ISODuration   :string)
(def EvalOp        [:enum :all :some :none :not :assert])
(def PolicyOutcome [:enum :apply :try :next :reduce :block :warn :note :allow])

;; ── Eval nodes (from proxx) ─────────────────────────────

(def EvalNode
  [:map
   [:eval/op EvalOp]
   [:eval/target {:optional true} :keyword]
   [:eval/forms [:vector :any]]])

;; ── Policy match (from eta-mu v2) ───────────────────────

(def PolicyMatch
  [:map
   [:tool/name  {:optional true} :string]
   [:tool/params {:optional true} :map]])

;; ── Fulfillment match ───────────────────────────────────

(def FulfillmentMatch
  [:map
   [:tool/name   {:optional true} :string]
   [:tool/params {:optional true} :map]
   [:tool/output {:optional true} :any]
   [:tool/error? {:optional true} :boolean]])

;; ── Unified contract kinds ──────────────────────────────

;; 1. Agent — persona binding
(def AgentContract
  [:map {:closed false}
   [:contract/id      string?]
   [:contract/kind    [:= :agent]]
   [:contract/version {:optional true} int?]
   [:contract/actors  {:optional true} [:vector string?]]
   [:enabled          {:optional true} :boolean]
   [:trigger-kind     {:optional true} keyword?]
   [:agent            {:optional true}
    [:map {:closed false}
     [:role     {:optional true} keyword?]
     [:model    {:optional true} string?]
     [:thinking {:optional true} keyword?]]]
   [:prompts          {:optional true}
    [:map {:closed false}
     [:system {:optional true} :string]
     [:task   {:optional true} :string]]]
   [:hooks            {:optional true} :map]
   [:actor            {:optional true}
    [:map {:closed false}
     [:capabilities {:optional true} [:vector keyword?]]]]
   [:memory           {:optional true} :map]
   [:context          {:optional true} :map]
   [:data             {:optional true} :map]])

;; 2. Actor — principal identity
(def ActorContract
  [:map {:closed false}
   [:actor/id         string?]
   [:actor/kind       [:enum :agent :user]]
   [:actor/default-agent {:optional true} string?]
   [:actor/roles      {:optional true} [:vector keyword?]]
   [:actor/capabilities {:optional true} [:vector keyword?]]])

;; 3. Role — capability set
(def RoleContract
  [:map {:closed false}
   [:role/id           keyword?]
   [:role/name         {:optional true} string?]
   [:role/capabilities {:optional true} [:vector keyword?}]
   [:role/permissions  {:optional true} [:vector string?]]
   [:prompts           {:optional true} :map]])

;; 4. Capability — tool affordance
(def CapabilityContract
  [:map {:closed false}
   [:cap/id    keyword?]
   [:cap/tools {:optional true} [:vector any?]]
   [:cap/user-surfaces {:optional true} [:vector :map]]])

;; 5. Policy — tree-shaped evaluation (proxx-style)
(def PolicyContract
  [:map {:closed false}
   [:contract/id     :keyword]
   [:contract/kind   [:= :policy]]
   [:policy/condition {:optional true} [:ref :unified/eval-node]]
   [:policy/filters   {:optional true} [:vector [:ref :unified/eval-node]]]
   [:policy/outcome   PolicyOutcome]
   [:policy/strategy  {:optional true} :symbol]
   [:policy/children  {:optional true} [:vector [:ref :unified/policy]]]
   [:policy/sort      {:optional true} [:ref :unified/eval-node]]
   [:policy/project   {:optional true} [:vector :map]]
   [:policy/doc       {:optional true} :string]])

;; 6. Policy Gate — flat tool-call gating (eta-mu v2 style)
(def PolicyGateContract
  [:map {:closed false}
   [:contract/id      :string]
   [:contract/kind    [:= :policy-gate]]
   [:policy/match     PolicyMatch]
   [:policy/action    [:enum :block :warn :note :allow]]
   [:policy/reason    {:optional true} :string]
   [:policy/ttl-ms    {:optional true} int?]])

;; 7. Fulfillment — post-tool-call notification
(def FulfillmentContract
  [:map {:closed false}
   [:contract/id         :string]
   [:contract/kind       [:= :fulfillment]]
   [:fulfillment/match   FulfillmentMatch]
   [:fulfillment/mode    [:enum :notify :audit]]
   [:fulfillment/message :string]
   [:fulfillment/level   {:optional true} [:enum :info :warn :error]]])

;; 8. Action — deterministic executor
(def ActionContract
  [:map {:closed false}
   [:contract/kind  [:= :action]]
   [:contract/id    ContractId]
   [:action/handler :string]
   [:enabled        {:optional true} :boolean]
   [:data           {:optional true} :map]])

;; 9. Pipeline — ordered steps
(def PipelineContract
  [:map {:closed false}
   [:contract/kind   [:= :pipeline]]
   [:contract/id     ContractId]
   [:pipeline/steps  [:vector :map]]
   [:enabled         {:optional true} :boolean]])

;; 10. Trigger — schedule/event source
(def TriggerContract
  [:map {:closed false}
   [:contract/kind    [:= :trigger]]
   [:contract/id      ContractId]
   [:trigger/kind     [:enum :cron :event :webhook :manual]]
   [:trigger/target   ContractId]
   [:trigger/schedule {:optional true} :string]
   [:enabled          {:optional true} :boolean]])

;; 11. Model / Model Family (from knoxx + proxx)
(def ModelFamilyContract
  [:map {:closed false}
   [:model-family/id         string?]
   [:model-family/provider   {:optional true} keyword?]
   [:model-family/prefixes   [:sequential string?]]
   [:model-family/allowlisted {:optional true} :boolean]
   [:model-family/reasoning   {:optional true} :boolean]])

(def ModelContract
  [:map {:closed false}
   [:model/id         string?]
   [:model-family/id  {:optional true} string?]
   [:model/provider   {:optional true} keyword?]
   [:model/label      {:optional true} string?]
   [:model/allowlisted {:optional true} :boolean]])

;; ── Registry ────────────────────────────────────────────

(def registry
  {:unified/eval-node    EvalNode
   :unified/policy       PolicyContract
   :unified/policy-match PolicyMatch
   :unified/fulfillment-match FulfillmentMatch
   :agent        AgentContract
   :actor        ActorContract
   :role         RoleContract
   :capability   CapabilityContract
   :policy       PolicyContract
   :policy-gate  PolicyGateContract
   :fulfillment  FulfillmentContract
   :action       ActionContract
   :pipeline     PipelineContract
   :trigger      TriggerContract
   :model-family ModelFamilyContract
   :model        ModelContract})
```

### 3.3 Unified Contract Loader

Merge knoxx's multi-root recursive discovery with eta-mu's upward-walk and SHA caching:

```clojure
(ns open-hax.contracts.loader
  "Unified contract file discovery and loading."
  (:require [cljs.reader :as reader]
            [open-hax.contracts.schema :as schema]
            [open-hax.contracts.bracket :as bracket]
            ["node:fs" :as fs]
            ["node:path" :as path]
            ["node:crypto" :as crypto]))

;; ── Discovery modes ─────────────────────────────────────

;; Mode 1: Knoxx-style recursive scan from configured roots
(defn discover-from-roots!
  "Scan contract roots recursively for .edn files.
   Returns Promise<vector<absolute-path>>."
  [roots]
  ;; ... recursive readdir, filter .edn, dedup
  )

;; Mode 2: Eta-mu-style upward walk from cwd
(defn discover-upward!
  "Walk from cwd upward to stop-dir, collecting CONTRACT.edn files.
   Returns vector<absolute-path>."
  [start-dir stop-dir]
  ;; ... walk-up-paths logic from eta-mu v2
  )

;; Mode 3: Manifest-ordered (proxx-style)
(defn discover-from-manifest!
  "Load files in manifest order from a root directory.
   Returns vector<absolute-path>."
  [manifest-path]
  ;; ... read manifest, resolve relative paths
  )

;; ── Unified load ────────────────────────────────────────

(defn load-contract!
  "Parse + bracket-repair + validate a single EDN file.
   Returns {:ok? bool :contract map :validation map :file-path string}."
  [file-path]
  (-> (fs/readFile file-path "utf8")
      (.then (fn [text]
               (let [{:keys [text]} (bracket/repair text)
                     raw (reader/read-string text)
                     kind (infer-contract-class raw)
                     validation (schema/validate kind raw)]
                 {:ok? (:ok validation)
                  :contract raw
                  :validation validation
                  :file-path file-path})))))

(defn load-all!
  "Discover + load all contracts. Dedup on [kind id], first-wins.
   SHA-cache supported for upward-walk mode.
   Returns Promise<vector<contract-record>>."
  [config]
  ;; Combines all discovery modes, deduplicates, validates
  )
```

### 3.4 Unified Policy Engine

Merge proxx's tree evaluator with eta-mu's flat gating:

```clojure
(ns open-hax.contracts.policy
  "Unified policy evaluation engine."
  (:require [open-hax.contracts.policy.eval :as eval]
            [open-hax.contracts.policy.gate :as gate]
            [open-hax.contracts.policy.fulfillment :as fulfill]))

;; ── Tree evaluation (proxx heritage) ────────────────────

(defn evaluate-tree
  "Walk a policy tree. Returns first passing strategy result or nil.
   Records trace for debugging."
  [policies ctx trace]
  ;; ... proxx router/route-request! logic
  )

;; ── Flat gate evaluation (eta-mu v2 heritage) ───────────

(defn evaluate-gates
  "Evaluate all policy-gate contracts against a tool-call.
   Returns {:action :block|:warn|:note|:allow :reason string?}."
  [gates tool-call]
  ;; ... eta-mu evaluate-policies logic
  )

;; ── Fulfillment evaluation (eta-mu v2 heritage) ─────────

(defn evaluate-fulfillments
  "Evaluate all fulfillment contracts against a tool-result.
   Returns vector of {:mode :notify|:audit :message string :level keyword}."
  [fulfills tool-result]
  ;; ... eta-mu evaluate-fulfillments logic
  )

;; ── Unified dispatch ────────────────────────────────────

(defn before-tool-call!
  "Run policy gates before a tool call. Returns nil or block-map."
  [contracts tool-call ctx]
  (let [gates (filter #(= :policy-gate (:contract/kind %)) contracts)]
    (evaluate-gates gates tool-call)))

(defn after-tool-call!
  "Run fulfillments after a tool call. Returns action seq."
  [contracts tool-result ctx]
  (let [fulfills (filter #(= :fulfillment (:contract/kind %)) contracts)]
    (evaluate-fulfillments fulfills tool-result)))

(defn route-request!
  "Route a request through policy trees (proxx-style).
   Returns strategy result or throws."
  [contracts ctx]
  (let [policies (filter #(= :policy (:contract/kind %)) contracts)
        trace (atom [])]
    (or (evaluate-tree policies ctx trace)
        (throw (ex-info "Policy tree exhausted" {:trace @trace})))))
```

### 3.5 Unified Extension Runtime

Merge eta-mu's macro DSL with knoxx's event bus:

```clojure
;; eta-mu.core macros remain as-is — they are already cross-platform.
;; The unified runtime wraps them with knoxx's agent resolution.

(ns open-hax.contracts.extension-runtime
  "Unified extension runtime."
  (:require [open-hax.contracts.resolve :as resolve]
            [open-hax.contracts.policy :as policy]))

;; Extension registry (from knoxx extension_runtime.cljs)
(defonce extensions* (atom []))
(defonce command-handlers* (atom {}))

;; Event dispatch with policy gating (merged)
(defn dispatch-event
  "Dispatch event to all registered handlers.
   For before_tool_call events, also runs policy gates.
   For after_tool_call events, also runs fulfillments."
  [event-name event ctx contracts]
  (let [handlers (event-handlers event-name)]
    ;; 1. Run policy gates (if before_tool_call)
    (when (= event-name "before_tool_call")
      (let [block (policy/before-tool-call! contracts
                     {:tool/name (aget event "toolName")
                      :tool/params (js->clj (aget event "params"))}
                     ctx)]
        (when block (throw (ex-info "Policy blocked" block)))))

    ;; 2. Run extension handlers (eta-mu style)
    (reduce-handlers handlers event ctx)

    ;; 3. Run fulfillments (if after_tool_call)
    (when (= event-name "after_tool_call")
      (policy/after-tool-call! contracts
        {:tool/name (aget event "toolName")
         :tool/output (aget event "output")}
        ctx))))
```

### 3.6 File Layout

```
contracts/                          # Unified contract root
├── agents/                         # Knoxx agent contracts
│   ├── developer_agent.edn
│   └── ...
├── actors/                         # Knoxx actor contracts
├── roles/                          # Knoxx role contracts
├── capabilities/                   # Knoxx capability contracts
├── policies/                       # Merged: tree + gate
│   ├── runtime/                    # Proxx policy tree files
│   │   ├── 00-manifest.edn
│   │   ├── 05-provider-seed.edn
│   │   └── ...
│   └── gates/                      # Eta-mu v2 policy gates
│       └── dangerous-tools.edn
├── fulfillments/                   # Eta-mu v2 fulfillment contracts
├── actions/                        # Knoxx action contracts
├── pipelines/                      # Knoxx pipeline contracts
├── triggers/                       # Knoxx trigger contracts
├── model_families/                 # Merged knoxx + proxx
├── models/                         # Merged knoxx + proxx
└── _defaults.edn                   # Knoxx defaults
```

---

## 4. Migration Strategy

### Phase 1: Foundation (Week 1-2)

1. **Create `open-hax.contracts` namespace** in a shared package (e.g., `packages/shared/contracts/`)
2. **Port unified schema registry** — merge proxx `schema.cljs`, knoxx `contracts/validator.cljs`, eta-mu v2 match schemas
3. **Port unified loader** — merge knoxx `contracts/loader.cljs` + eta-mu `walk-up-paths` + SHA caching
4. **Port bracket repair** — from knoxx `contract.bracket` spec (already designed, not yet implemented)

### Phase 2: Policy Engine (Week 3-4)

5. **Port proxx policy evaluator** — `proxx.policy.eval`, `proxx.policy`, `proxx.policy.router`
6. **Port eta-mu v2 gate** — `contract_runtime_v2/core.cljs` → `policy.gate`
7. **Port eta-mu v2 fulfillment** — `contract_runtime_v2/core.cljs` → `policy.fulfillment`
8. **Add strategy registry** — from proxx `policy/register-strategy!`

### Phase 3: Agent Runtime (Week 5-6)

9. **Port knoxx resolver** — `contracts/resolve.cljs`, `contracts/actor_scope.cljs`, `contracts/roles.cljs`
10. **Port knoxx extension runtime** — `extension_runtime.cljs`
11. **Wire extension runtime into policy engine** — before/after tool-call hooks
12. **Port prompt injection** — merge knoxx event-based + eta-mu section-based

### Phase 4: Integration (Week 7-8)

13. **Proxx adapter** — route proxx `resources/policies/` through unified loader, map to `:policy` kind
14. **Knoxx adapter** — existing `contracts/` tree works as-is, add gate/fulfillment kinds
15. **Eta-mu adapter** — port `CONTRACT.edn` skill files to unified format, register as extensions
16. **JS bridge** — unified `loadCljsRuntime` that exposes all three layers

### Phase 5: Cutover (Week 9-10)

17. **Shadow old namespaces** — `knoxx.backend.contracts.*` → shim to `open-hax.contracts.*`
18. **Shadow proxx namespaces** — `proxx.policy.*` → shim to `open-hax.contracts.policy.*`
19. **Shadow eta-mu namespaces** — `eta-mu.extensions.contract-runtime*` → shim to unified
20. **Remove shims** — after all consumers migrated

---

## 5. Unified Contract Vocabulary

```
:contract/id          — unique identifier (string or keyword)
:contract/kind        — discriminator keyword
:contract/version     — integer version
:contract/actors      — vector of actor ids (wildcard :* allowed)
:contract/doc         — human-readable description
:contract/uses        — vector of dependency contract ids
:enabled              — boolean toggle
:data                 — extension map (source, filters, tools, context, output)
```

**Kinds:**

| Kind | Source | Purpose |
|---|---|---|
| `:agent` | knoxx | Agent persona binding (model, role, prompts) |
| `:actor` | knoxx | Principal identity (human/service) |
| `:role` | knoxx | Capability set + permissions |
| `:capability` | knoxx | Tool affordance |
| `:policy` | proxx | Tree-shaped evaluation (conditions, filters, outcomes, children) |
| `:policy-gate` | eta-mu v2 | Flat tool-call gating (match → action) |
| `:fulfillment` | eta-mu v2 | Post-tool-call notification |
| `:action` | knoxx | Deterministic executor |
| `:pipeline` | knoxx | Ordered steps |
| `:trigger` | knoxx | Schedule/event source |
| `:strategy` | proxx | Bound CLJS function for policy dispatch |
| `:model-family` | merged | Model group with routing hints |
| `:model` | merged | Individual model binding |
| `:ingest_source` | knoxx | Data ingestion configuration |

---

## 6. Key Design Decisions

### 6.1 Policy tree vs flat gate — both survive

The proxx policy tree (`:policy` kind) handles complex routing with backtracking, filter/project/sort, and strategy dispatch. The eta-mu flat gate (`:policy-gate` kind) handles simple tool-call blocking with match patterns. They serve different use cases and both should exist in the unified system. The policy engine dispatches to the appropriate evaluator based on `:contract/kind`.

### 6.2 Extension macros — eta-mu wins

The `eta-mu.core/defextension` macro DSL is the most mature extension abstraction. Knoxx's `extension_runtime.cljs` should adopt it rather than maintaining a parallel event bus. The macro generates platform-specific init wrappers, making extensions portable across Pi, OpenCode, and Knoxx.

### 6.3 Agent resolution — knoxx wins

The actor→role→capability→tool-id chain in knoxx `contracts/resolve.cljs` is the most complete RBAC model. Proxx and eta-mu don't have agent/actor abstractions. The unified system uses knoxx's resolver as-is.

### 6.4 File discovery — all three modes

- **Recursive scan** (knoxx) for `contracts/` tree
- **Upward walk** (eta-mu) for per-project `CONTRACT.edn`
- **Manifest order** (proxx) for policy phase files

All three modes feed into a single dedup+validate pipeline.

### 6.5 Schema validation — Malli everywhere

Proxx and knoxx already use Malli. Eta-mu's custom eval and matchers should be wrapped in Malli schemas for consistency. The bracket repair pass (from the knoxx spec) sits before Malli validation.

---

## 7. Risk Assessment

| Risk | Mitigation |
|---|---|
| Breaking proxx provider routing | Shadow shims + adapter layer; proxx routes through unified policy engine |
| Breaking knoxx agent resolution | Existing `contracts/` tree works unchanged; resolver is additive |
| Breaking eta-mu extensions | `defextension` macro unchanged; init wrappers regenerated |
| Performance regression from unified loader | SHA caching from eta-mu v2; lazy loading per-kind |
| Schema conflicts between dialects | Unified schema registry with `:closed false` maps; dialect-specific fields tolerated |

---

## 8. Success Criteria

1. All existing proxx policy tests pass through unified engine
2. All existing knoxx contract tests pass through unified loader
3. All existing eta-mu extensions load via unified runtime
4. New contracts can mix kinds freely (e.g., a `:policy-gate` in the knoxx `contracts/` tree)
5. Single `loadCljsRuntime` call exposes all three layers to TypeScript hosts
6. Agent-authored EDN benefits from bracket repair + Malli validation + whitelist check
