# Epistemic Kernel — Actor / Contract Integration

> **Fit-in-your-head invariant:**  
> The actor answers *who is allowed to act*.  
> The contract answers *how this actor behaves when it acts*.  
> Everything they produce (inferences, attestations, judgments) lives in the epistemic store — the source of truth.

---

## Layer map

```
┌─────────────────────────────────────────────────────┐
│  VIEWS / ADAPTERS                                   │
│  Fastify routes · graph/garden · search · Discord   │
│  Never own truth — project and mutate only          │
├─────────────────────────────────────────────────────┤
│  CONTROLLERS                                        │
│  Contracts + actors (Knoxx / cephalon runtime)      │
│  Contract kinds are rules over the epistemic store  │
│  Agent runs are inference episodes                  │
├─────────────────────────────────────────────────────┤
│  MODEL — EPISTEMIC KERNEL                           │
│  openplanner-core / promptdb-core                   │
│  Primitives: fact · obs · inference                 │
│              attestation · judgment                 │
│  Indexed via Datalog (XTDB / datascript-esque)      │
│  THIS is the source of truth                        │
└─────────────────────────────────────────────────────┘
```

---

## Epistemic primitives

Every other system (contracts, actors, events, receipts) emits one of these.

```clojure
;; promptdb-core — all records are EDN, validated via Malli

(fact
  {:ctx    keyword?       ;; asserting principal  (己 = self)
   :claim  any?           ;; the proposition
   :src    any?           ;; provenance: event-id, actor-id, URL …
   :p      [:double {:min 0.0 :max 1.0}]
   :time   inst?})

(obs
  {:ctx    keyword?       ;; who perceived it
   :about  any?           ;; what was sensed
   :signal any?           ;; raw signal shape
   :p      [:double {:min 0.0 :max 1.0}]})

(inference
  {:from  [:vector [:or :fact :obs]]   ;; evidence chain
   :rule  keyword?                     ;; contract-id that fired
   :actor keyword?                     ;; who executed the contract
   :claim any?                         ;; derived proposition
   :p     [:double {:min 0.0 :max 1.0}]})

(attestation
  {:actor    keyword?     ;; who is attesting
   :did      any?         ;; what they claim they did
   :run-id   uuid?
   :causedby uuid?        ;; what triggered the run
   :p        [:double {:min 0.0 :max 1.0}]})

(judgment
  {:of      uuid?         ;; which inference or attestation
   :verdict [:enum :held :failed :partial]
   :auditor keyword?      ;; who or what judged
   :p       [:double {:min 0.0 :max 1.0}]})
```

---

## Layer / epistemic role table

| Layer kind | Epistemic role | What it does |
|---|---|---|
| `role` | `fact` | Asserts: principal P has role R in org O |
| `actor` | `fact` | Asserts: P is provisioned and may execute contracts |
| `event` / `obs` | `obs` | Something happened — raw signal, not yet validated |
| `trigger` | — | Promotes `obs` → eligible for contract matching |
| `policy` | side-condition | Must hold for an inference rule to fire |
| `tool-call` | capability grant | Which `attestation` types this actor may emit |
| `agent` | `inference-rule` | obs + actor-facts → inference + attestation |
| `fulfillment` | `judgment` | World's verdict — did the claim hold? |

---

## Execution cycle

```
obs  (event arrives)
  → actor-fact  (principal is permitted to process it)
    → contract  (inference-rule: if obs matches pattern → claim C)
      → inference  (claim C, p=0.9, src=event-id+actor-id)
        → action  (world-effect: HTTP call, file write, message…)
          → attestation  (actor says: I did X, run-id, causedby)
            → judgment  (did claim C actually hold?)
              → new obs  (world changed)
```

This cycle **is a path in the Datalog + event graph**.  
Openplanner's job: store, index, and make that cycle queryable.

---

## Actor ↔ contract binding

The missing seam between identity and behavior:

```clojure
;; Actor principal record (one row in epistemic store / DB)
{:actor/id          "agent.discord-patrol"
 :actor/user-id     "usr_123"     ;; real Knoxx user row
 :actor/org-id      "org_456"
 :actor/role        :agent/discord-patrol
 :actor/status      :active
 :actor/contract-id "discord-patrol"}

;; Agent contract (EDN, compiled by contract runtime)
{:contractid   "discord-patrol"
 :contractkind "agent"
 :contractuses ["policy/default-gate"
                "trigger/discord-event"
                "fulfillment/loose-json"
                "tool-call/semantic-search-allowed"]
 :actor        {:binding      :provisioned-user
                :tenant-scope :org
                :receipts     {:driver     :db
                               :emit-event? true}}
 :agent        {:role  :agent/discord-patrol
                :model "glm-5"
                :thinking :off}}
```

**Invariants:**
- Actor provisioning creates a real Knoxx principal with role membership.
- Contract compilation resolves behavior, tools, triggers, and fulfillment rules.
- Every run stamps `run-id`, `actor-id`, `org-id`, `contract-id`, and optional `causedby` on emitted events.
- Every attestation cross-links to an objective `EventRecord`.
- Actor role grants the **ceiling**; contract grants the **subset** for this behavior.

---

## Contract kind → epistemic kernel mapping

```clojure
;; trigger  — Datalog query / topic subscription → selects obs candidates
;; policy   — extra predicates (role, org, tool-policy) on inference firing
;; tool-call — capability grant: actor may emit attestations of shape T
;; agent    — strategy: obs + actor-facts ⟹ inference + attestation
;; fulfillment — consumes inference/attestation → emits judgment
;; role     — asserts initial fact giving actor its powers
```

---

## Concurrency model

Three independent processes, one shared epistemic schema:

| Process | Role | Writes |
|---|---|---|
| **Ingestion** | Files / promptdb / Discord / GitHub → `obs`/`fact` | EventBus |
| **Inference** | Contracts + actors subscribe, run in parallel episodes | `inference`, `attestation`, `EventRecord` |
| **Views** | UI / search / graph query epistemic/Datalog store | read-heavy |

---

## Promptdb as filesystem-shaped obs/fact

```
packages/promptdb-core/
  facts/          ← checked-in EDN fact records
  obs/            ← checked-in obs templates
  rules/          ← inference-rule skeletons (contract seeds)
  judgments/      ← known-good judgment fixtures
```

Ingestor gets a `source-kind :promptdb` driver:
1. Walk filesystem path.
2. Parse EDN.
3. Validate via the same Malli schemas as `openplanner-core`.
4. Write `obs`/`fact`/`inference` records straight into the epistemic/Datalog store.

The ingestor needs **special awareness of the promptdb FS** — not just chunking to text/embeddings, but schema-aware structured ingestion.

---

## openplanner-core package shape (CLJS target)

```
packages/openplanner-core/
  src/
    epistemic.cljc     ← fact / obs / inference / attestation / judgment defs + Malli
    datalog.cljc       ← thin Datalog wrapper (queries over primitives)
    contracts.cljc     ← compiled contract projection shape
  deps.edn
  shadow-cljs.edn
```

`.cljc` runs on both JVM (Knoxx backend) and Node (openplanner Fastify shell).  
JS/TS only at framework edges — not in the epistemic core.

---

## Do not

- **Collapse receipt river and EventBus into one log.**  
  Receipts are the actor's subjective narrative.  
  EventRecords are the platform's objective ledger.  
  That distinction is what lets you compare what the agent claims against what the platform can prove.

- **Let contracts mint authority.**  
  Actor role grants the ceiling; contract grants the subset. Always.

- **Own truth in views.**  
  Fastify routes, graph renderers, Discord adapters — they project and mutate. They never are the record.
