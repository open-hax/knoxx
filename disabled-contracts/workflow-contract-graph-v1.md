# Workflow Contract Graph v1

Status: draft canonical spec
Scope: deterministic agent-work workflow kernel and cross-DSL contract vocabulary
Non-goal: resurrecting `skill-graph-aco` as an authoritative workflow engine

## Intent

Build a deterministic workflow/contract kernel where agent work moves through explicit, valid states and every state transition is justified by machine-checkable contracts and recorded evidence.

The kernel separates three graph classes:

1. **Workflow FSM graph** — authoritative process law.
2. **Capability/skill graph** — advisory execution help.
3. **Contract/proof graph** — proof-bearing typed relations among policies, actors, agents, tasks, predicates, evidence, and permissions.

A learned, abductive, probabilistic, embedding, pheromone, or co-occurrence edge may propose a relation, but it must remain a `:candidate` relation until accepted by deterministic validation.

## Grounding evidence

- `orgs/octave-commons/gates-of-aker/docs/reference/process.md` defines the task lifecycle as `Incoming -> Accepted -> Breakdown -> Ready -> Todo -> In Progress -> In Review -> Testing -> Document -> Done` and requires verification before `Done`.
- `orgs/open-hax/eta-mu/packages/kanban/src/types.ts` defines durable Kanban task state and status tokens such as `incoming`, `accepted`, `breakdown`, `ready`, `todo`, `in_progress`, `review`, `document`, `done`, `blocked`, `icebox`, and `rejected`.
- `orgs/open-hax/eta-mu/pi/agent/skills/kanban-fsm/CONTRACT.edn` already treats the Kanban FSM as data with normalization, gates, invariants, and frontmatter rules.
- `orgs/open-hax/proxx/resources/policies/runtime/*.edn` uses `:contract/id`, `:contract/kind`, ordered manifests, policy programs, strategy bindings, derived facts, filters, and outcomes.
- `contracts/` is the canonical Knoxx contract root and contains actor, agent, role, capability, policy, action, pipeline, trigger, model, and model-family contracts.
- Eta-mu skills and skill contracts define operator behavior and local process doctrine, but they do not authorize workflow transitions by usage frequency.

## Graph classes

### 1. Workflow FSM graph

Authoritative. Answers:

```text
Can task T move from state A to state B?
```

Edges must be authored in, or mechanically derived from, trusted FSM contracts. They cannot be inferred from skill usage, semantic similarity, ACO pheromones, or historical co-occurrence.

A workflow edge is valid only when:

- the task source state matches the edge `:from` state;
- the target state matches an allowed `:to` state;
- the task file is the current durable state record;
- required frontmatter and task fields are present;
- WIP, blocker, priority, scope, and acceptance gates pass;
- required evidence is recorded;
- no forbidding predicate is true.

Example shape:

```clojure
{:edge/id :kanban.transition/in-progress->review
 :edge/type :fsm.transition
 :edge/authority :authoritative
 :from :kanban.status/in-progress
 :to :kanban.status/review
 :requires [:task.has-reviewable-change
            :task.evidence.recorded]
 :forbids [:task.blocker/open]
 :proof-mode :deterministic}
```

### 2. Capability/skill graph

Advisory. Answers:

```text
Which skills, tools, agents, or actions might help in this phase?
```

This graph may use heuristics, rankings, graph traversal, embeddings, ACO, historical success, or semantic matching. It may recommend an operator or capability. It must not authorize state transitions.

Example shape:

```clojure
{:edge/id :skill.suggest/regression-triage-for-testing-failure
 :edge/type :recommendation
 :edge/authority :advisory
 :from :kanban.status/testing
 :to :skill/regression-triage
 :basis [:task.test-failure.present]
 :proof-mode :heuristic}
```

### 3. Contract/proof graph

Proof-bearing. Answers:

```text
Which contracts, roles, permissions, predicates, policies, evidence records, or invariants prove that this action or transition is allowed?
```

This graph connects workflow, policy, and runtime contracts without collapsing their dialects into one file format. It records typed relations such as:

- `:defines-state`
- `:defines-transition`
- `:defines-predicate`
- `:defines-role`
- `:defines-permission`
- `:defines-action`
- `:defines-evidence`
- `:requires-proof`
- `:satisfies-proof`
- `:denies-action`
- `:recommends-capability`
- `:candidate-relation`

Example shape:

```clojure
{:edge/id :proof/in-progress-review-requires-evidence
 :edge/type :proof-obligation
 :edge/authority :authoritative
 :from :kanban.transition/in-progress->review
 :to :task.evidence.recorded
 :proof-mode :deterministic
 :validator :workflow.validators/task-has-evidence}
```

## Contract kinds

The kernel recognizes these meta-contract kinds. Existing dialects may project into them without changing their runtime immediately.

| Kind | Purpose | Typical source |
|---|---|---|
| `:workflow-fsm` | states, transitions, ordering, gates, invariants | Kanban FSM specs, process docs |
| `:task` | durable work item identity, state, priority, refs, acceptance criteria, evidence | Kanban task files |
| `:transition-gate` | deterministic predicates for a state move | Kanban gates, workflow validators |
| `:predicate` | named Boolean or structured condition | Kanban gates, Proxx filters, policy clauses |
| `:policy-program` | ordered routing/enforcement phases and backtracking semantics | Proxx runtime policies |
| `:policy-clause` | condition/filter/outcome rule | Proxx policy clauses, Knoxx policies |
| `:actor` | human/service/system principal | Knoxx actors |
| `:agent` | runnable AI persona/model/prompt binding | Knoxx agents, eta-mu agent config |
| `:role` | role claims and capability set | Knoxx roles |
| `:capability` | tool/action affordance with optional surfaces | Knoxx capabilities, eta-mu skills |
| `:action` | declarative executable operation | Knoxx actions, eta-mu skills |
| `:evidence` | proof artifact, receipt, test run, review note, build log, report | task refs, receipts, CI output |
| `:proof` | relation showing a claim or transition obligation is satisfied | verifier output, receipts |
| `:candidate` | abductive/probabilistic proposed relation awaiting validation | LLM/prover/ACO/embedding proposals |

## Authority levels

Every edge and contract relation must declare authority.

| Authority | Meaning | May authorize transition? |
|---|---|---|
| `:authoritative` | authored or derived from trusted contracts and validators | yes |
| `:derived` | mechanically derived from authoritative data with deterministic code | yes, if derivation is trusted |
| `:advisory` | recommendation or ranking for execution help | no |
| `:candidate` | proposed by abductive/probabilistic/LLM/ACO machinery | no |
| `:rejected` | known invalid relation retained for audit | no |

## Transition verification contract

A transition request has this abstract shape:

```clojure
{:transition/request-id "..."
 :task/ref "..."
 :from :kanban.status/in-progress
 :to :kanban.status/review
 :actor/ref :actor/pi
 :agent/ref :agent/developer_agent
 :evidence/refs ["tests/..." "receipts.edn#..."]}
```

The verifier must return one of:

```clojure
{:result :accepted
 :transition/ref :kanban.transition/in-progress->review
 :proof/refs [...]}
```

```clojure
{:result :blocked
 :transition/ref :kanban.transition/in-progress->review
 :missing [:task.evidence.recorded]
 :failed-predicates [:workspace.tests.selected-pass]}
```

```clojure
{:result :rejected
 :reason :transition.not-declared
 :from :kanban.status/review
 :to :kanban.status/done}
```

`"You are not done"` becomes a typed `:blocked` or `:rejected` verifier result, not a conversational scolding loop.

## Initial dialect mappings

### Kanban

- Kanban process docs project to `:workflow-fsm`.
- Kanban status aliases project to normalization rules.
- Kanban task files project to `:task` instances.
- Kanban gates and invariants project to `:transition-gate` and `:predicate` contracts.
- Board generation remains a projection from task files, not a separate source of truth.

### Proxx policy

- Runtime manifests project to `:policy-program` and ordered phase contracts.
- `:policy/condition`, `:policy/filters`, and `:policy/outcome` project to `:predicate`, `:policy-clause`, and `:action` relations.
- Strategy bindings project to policy/action bindings.
- Fallback and backtracking are policy-program semantics, not workflow-FSM transitions.

### Knoxx

- Actor contracts project to `:actor`.
- Agent contracts project to `:agent` and model/prompt/runtime bindings.
- Role contracts project to `:role`.
- Capability contracts project to `:capability`.
- Policy contracts project to `:policy-clause` and denial relations.
- Action/pipeline/trigger contracts project to `:action` and orchestration relations.

### Eta-mu skills/contracts

- Skills project to `:capability` or `:action` depending on whether they describe an affordance or an executable protocol.
- Skill contracts and local doctrine project to predicates, process obligations, and advisory recommendations.
- Skill invocation history may update recommendation weights only; it must not create authoritative FSM edges.

## Abductive/probabilistic boundary

Abductive systems, LLM mappers, embeddings, graph-learning, or ACO may propose:

- likely task state;
- likely next useful skill;
- missing predicate candidates;
- candidate mappings between dialects;
- possible duplicate or drifted contracts.

They must emit `:candidate` relations. Promotion from `:candidate` to `:authoritative` or `:derived` requires deterministic acceptance by a validator with recorded evidence.

## Minimal implementation path

1. Keep existing Kanban, Proxx, Knoxx, and eta-mu runtimes unchanged.
2. Add a registry that indexes contract records by `[:contract/id :contract/kind]` or dialect-specific equivalent.
3. Add projection adapters from each dialect into the meta-contract vocabulary above.
4. Add a workflow verifier for declared Kanban transitions and gates.
5. Add an evidence ledger interface that can attach receipts, test runs, review notes, and task refs to transition requests.
6. Add advisory skill recommendation as a separate service that consumes task state but cannot write task status.
7. Only after deterministic verification exists, allow a controller/RALPH-like loop to ask: current state, blocked gate, next valid action, evidence needed.

## Normative rules

- `skill graph edge != workflow transition`.
- Task files are durable workflow state.
- Board views are generated projections.
- `Done` requires deterministic evidence.
- Back-edges from `Done` create follow-up tasks unless a trusted FSM contract explicitly permits reopening.
- ACO, embeddings, LLM mappings, and historical co-occurrence are advisory or candidate signals only.
- Authoritative FSM transitions require declared edges and deterministic gate verification.
- Contract dialects should be projected into a shared vocabulary before attempting schema unification.
