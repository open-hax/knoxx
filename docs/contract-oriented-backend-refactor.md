# Contract-Oriented Backend Refactor

## Goal

Make the Knoxx backend contract-oriented by moving contract meaning, validation, resolution, and materialization into a dedicated `knoxx.backend.contracts.*` domain.

Boundary rule:

> `knoxx.backend.contracts.*` owns loading, validating, resolving, compiling, indexing, watching, and storing contracts. `knoxx.backend.runtime.*` owns live process state only.

That means the backend should treat contracts as first-class program inputs, not just static EDN files that one subsystem partially reads.

## Why this refactor exists

The current backend already contains the shape of a contract system, but the boundaries have drifted:

- `backend/src/cljs/knoxx/backend/runtime/contract_loader.cljs`
- `backend/src/cljs/knoxx/backend/runtime/contract_validator.cljs`
- `backend/src/cljs/knoxx/backend/runtime/roles.cljs`
- `backend/src/cljs/knoxx/backend/contracts_routes.cljs`
- `backend/src/cljs/knoxx/backend/tooling.cljs`
- `backend/src/cljs/knoxx/backend/policy_db.cljs`

These files collectively do contract loading, validation, actor resolution, role/capability expansion, indexing, file watching, and persistence-adjacent work. But that logic is currently split across `runtime`, `tooling`, route handlers, and the PostgreSQL control-plane layer.

That split makes the system harder to reason about because:

- `runtime` is named like process execution, but contains contract catalog semantics
- policy persistence currently mixes with contract discovery and actor-contract sync
- route handlers contain indexing and file-watcher orchestration
- tool selection depends on contract resolution, but that logic sits in a tooling namespace instead of a contract domain
- only some contract kinds are enforced consistently

If contracts are the organizing center, the code should say so directly.

## Design doctrine

### 1. Contracts are the center

Knoxx should be able to do as much work as possible from contract EDN definitions.

Contracts should define:

- agents
- users / actors
- roles
- capabilities
- policies
- model families
- models
- trigger/job specs where appropriate
- driver/materialization metadata where appropriate

Runtime code should consume compiled contract state rather than rebuild policy meaning ad hoc in many places.

### 2. Runtime is not the contract domain

`runtime` should mean live process concerns such as:

- env/config loading
- mutable runtime state
- SDK process/session lifecycle
- websocket/runtime session management
- process-local caches that do not define business meaning

Anything that answers “what does this contract mean?” belongs in `contracts`, not `runtime`.

### 3. Storage is an implementation detail

Contracts should not care whether they are stored or materialized via:

- EDN
- MongoDB
- PostgreSQL
- JSON
- YAML

A driver system should make the same contract model work against different targets.

### 4. Validation lives above the database

Malli/Malli-style validation is the authoritative schema boundary for contract data.

If a relational schema is added, it should exist for operational reasons only:

- indexing
- query speed
- reporting
- migration support
- interoperability with existing systems

It should not become a second semantic authority that duplicates the contract model.

### 5. Source of truth must be explicit

The preferred long-term shape is:

- EDN contracts are canonical authoring format
- contract resolution/compilation is canonical semantic layer
- storage backends are projections, caches, or materializations

That allows local repo-native authoring while still supporting database-backed indexing and querying.

## Recommended namespace map

## Top-level backend domains

These are reasonable domain roots for the application:

- `knoxx.backend.auth`
- `knoxx.backend.users`
- `knoxx.backend.agents`
- `knoxx.backend.routes`
- `knoxx.backend.tools`
- `knoxx.backend.triggers`
- `knoxx.backend.memory`
- `knoxx.backend.mcp`
- `knoxx.backend.policy`
- `knoxx.backend.actors`
- `knoxx.backend.capabilities`
- `knoxx.backend.voice`
- `knoxx.backend.translation`
- `knoxx.backend.discord`
- `knoxx.backend.contracts`
- `knoxx.backend.runtime`

However, `actors` and `capabilities` should remain under `contracts.*` unless they gain substantial non-contract behavior. Otherwise the refactor will just spread contract meaning back across the codebase.

## Contract domain

The contract system should get its own subtree.

### Core contract namespaces

- `knoxx.backend.contracts.loader`
- `knoxx.backend.contracts.validator`
- `knoxx.backend.contracts.catalog`
- `knoxx.backend.contracts.resolve`
- `knoxx.backend.contracts.compile`
- `knoxx.backend.contracts.index`
- `knoxx.backend.contracts.watch`
- `knoxx.backend.contracts.store`
- `knoxx.backend.contracts.drivers.edn`
- `knoxx.backend.contracts.drivers.mongodb`
- `knoxx.backend.contracts.drivers.postgresql`
- `knoxx.backend.contracts.drivers.json`
- `knoxx.backend.contracts.drivers.yaml`

### Contract model namespaces

- `knoxx.backend.contracts.agents`
- `knoxx.backend.contracts.actors`
- `knoxx.backend.contracts.users`
- `knoxx.backend.contracts.roles`
- `knoxx.backend.contracts.capabilities`
- `knoxx.backend.contracts.policies`
- `knoxx.backend.contracts.models`
- `knoxx.backend.contracts.triggers`

These namespaces should hold contract-specific normalization, ids, reference resolution, and materialization logic for each contract kind.

## Runtime domain

The runtime subtree should become deliberately small and literal.

Recommended keepers:

- `knoxx.backend.runtime.config`
- `knoxx.backend.runtime.state`
- `knoxx.backend.runtime.sdk` or `knoxx.backend.runtime.agent-session`

Potentially keep:

- env-only config loading
- mutable atoms for live process state
- process bootstrap adapters

Move out of runtime:

- contract loading
- contract validation
- role/capability expansion
- actor-scoped contract interpretation
- contract directory indexing
- contract file watching

## Concrete moves from the current codebase

## Move out of `runtime`

### Current

- `knoxx.backend.runtime.contract-loader`
- `knoxx.backend.runtime.contract-validator`
- `knoxx.backend.runtime.roles`
- `knoxx.backend.runtime.actor-scope`
- most contract-driven pieces of `knoxx.backend.runtime.models`

### Target

- `knoxx.backend.contracts.loader`
- `knoxx.backend.contracts.validator`
- `knoxx.backend.contracts.roles`
- `knoxx.backend.contracts.actor-scope`
- `knoxx.backend.contracts.models`

`runtime.models` should only remain if it represents process/runtime model configuration. Contract-backed model catalogs belong under `contracts.models`.

## Split `tooling.cljs`

`backend/src/cljs/knoxx/backend/tooling.cljs` currently does at least four jobs:

- resolve actors
- resolve agent contracts
- expand roles into capabilities and tools
- assemble effective tool policy surfaces

Recommended split:

- `knoxx.backend.contracts.resolve`
  - actor resolution
  - default actor selection
  - effective contract selection
  - role/capability/tool expansion
- `knoxx.backend.tools.catalog`
  - tool registry/crosswalk helpers
- `knoxx.backend.tools.runtime`
  - runtime tool factories
- `knoxx.backend.tools.authz`
  - enforcement helpers if still needed

The rule should be simple: if logic is about what a contract grants or means, it belongs in `contracts.resolve`.

## Split `contracts_routes.cljs`

`backend/src/cljs/knoxx/backend/contracts_routes.cljs` currently combines:

- HTTP handlers
- contract validation
- Redis index sync
- file watching
- event-agent reload wiring

Recommended split:

- `knoxx.backend.routes.contracts`
  - HTTP handlers only
- `knoxx.backend.contracts.watch`
  - fs watchers and debounce logic
- `knoxx.backend.contracts.index`
  - Redis index sync / metadata snapshots
- `knoxx.backend.contracts.admin`
  - shared save/copy/validate operations if desired

Routes should orchestrate. They should not own watch/index mechanics.

## Split `policy_db.cljs`

`backend/src/cljs/knoxx/backend/policy_db.cljs` is the clearest mixed-responsibility file.

It currently combines:

- static permission catalogs
- static tool catalogs
- PostgreSQL persistence logic
- bootstrap/seeding logic
- user actor contract synchronization helpers
- contract-directory lookup helpers

Recommended split:

- `knoxx.backend.policy.catalog`
  - permission atoms
  - tool definitions
- `knoxx.backend.policy.store`
  - policy repository protocol
- `knoxx.backend.policy.drivers.postgresql`
  - current SQL implementation
- `knoxx.backend.policy.drivers.mongodb`
  - future document implementation
- `knoxx.backend.contracts.users.sync`
  - actor/user contract upsert or projection logic
- `knoxx.backend.users.provisioning`
  - onboarding-specific flows if they remain app behavior instead of contract behavior

## Split `agent_turns.cljs`

`backend/src/cljs/knoxx/backend/agent_turns.cljs` is too large because it contains distinct concerns in one file.

Recommended split:

- `knoxx.backend.agents.turns`
- `knoxx.backend.agents.recovery`
- `knoxx.backend.agents.policy`
- `knoxx.backend.agents.streaming`
- `knoxx.backend.agents.previews`

Suggested mapping:

- `send-agent-turn!` starts in `agents.turns`, with helpers moved into smaller namespaces
- `resume-recovered-session!` moves into `agents.recovery`
- model and rate-limit enforcement moves into `agents.policy`
- preview/render helpers move into `agents.previews`

## Split `event_agents.cljs`

`backend/src/cljs/knoxx/backend/event_agents.cljs` currently acts as both trigger engine and source adapter host.

Recommended split:

- `knoxx.backend.triggers.runtime`
- `knoxx.backend.triggers.jobs`
- `knoxx.backend.triggers.dispatch`
- `knoxx.backend.triggers.state`
- `knoxx.backend.triggers.sources.discord`

This keeps source adapters, scheduling, and dispatch logic from accumulating into one monolith.

## Storage direction: PostgreSQL to MongoDB

## Recommendation

Prefer a contract-store driver protocol first, then migrate selected workloads from PostgreSQL to MongoDB.

Do **not** start by rewriting the SQL file in place.

### Why MongoDB is attractive here

MongoDB is a better fit for contract-oriented data if:

- contract shapes will evolve frequently
- the system already relies on Malli validation for meaning
- the team already operates MongoDB
- avoiding dual maintenance of EDN schema plus SQL schema is a goal

MongoDB is especially well-suited for:

- contract catalogs
- compiled/materialized contract documents
- trigger/job documents
- policy envelopes that evolve over time
- denormalized resolution outputs

### Why keep a driver boundary anyway

PostgreSQL may still be useful for:

- hard relational reporting
- certain admin tables
- joins across stable product-control-plane entities
- transitional coexistence during migration

The abstraction should therefore be:

- contract semantics are storage-agnostic
- a driver persists or loads them
- a compiler/materializer generates query-friendly shapes

## Proposed contract store protocol

A minimal contract-store protocol should support these operations:

- load one contract by kind/id
- list contracts by kind
- save contract
- delete contract
- validate before save
- watch or publish change events
- sync indexes/materializations
- read compiled/materialized records

Illustrative shape:

```clojure
(defprotocol ContractStore
  (load-contract [store kind id])
  (list-contracts [store kind])
  (save-contract! [store kind id contract opts])
  (delete-contract! [store kind id opts])
  (load-materialized [store view-name key])
  (sync-materialized! [store scope opts])
  (watch-contracts! [store callback]))
```

Driver examples:

- `drivers.edn` reads/writes repo files
- `drivers.mongodb` stores canonical or compiled documents in Mongo collections
- `drivers.postgresql` supports legacy or reporting-backed use cases
- `drivers.json` and `drivers.yaml` support import/export or external integration paths

## Contract pipeline

The target flow should be explicit:

1. load source contracts
2. validate contract shape
3. normalize ids/references
4. resolve transitive references
5. compile/materialize contract meaning
6. publish/cache/index the result
7. let routes, tools, auth, and triggers consume the compiled view

In shorthand:

`contract source -> validate -> normalize -> resolve -> compile -> materialize -> consume`

This is the core of a contract-oriented backend.

## What contracts should directly drive

As the refactor matures, the following behaviors should become contract-driven:

- which actors/users exist and how they map to agents
- which agent contracts are available to which actors
- which roles expand to which capabilities
- which capabilities expand to which tools
- which policies gate tools, models, routes, and resources
- which models and families are allowed and how they behave
- which triggers/jobs exist and what source filters they use
- which runtime presets are available

The goal is not to move every line of code into EDN. The goal is to move as much policy and configuration meaning as possible into contracts so the code becomes a stable interpreter of declared rules.

## Large-file decomposition priorities

Highest-value decomposition order:

1. `policy_db.cljs`
2. `tooling.cljs`
3. `contracts_routes.cljs`
4. `agent_turns.cljs`
5. `event_agents.cljs`
6. `app_routes.cljs`

Rationale:

- `policy_db.cljs` contains the most serious domain mixing
- `tooling.cljs` is where contract meaning leaks into unrelated runtime surfaces
- `contracts_routes.cljs` can cleanly separate orchestration from contract services
- `agent_turns.cljs` and `event_agents.cljs` are large but can be split more safely after the contract boundary is clearer
- `app_routes.cljs` should be split after its downstream route/domain namespaces are stronger

## Migration plan

## Phase 1: Establish the contract boundary

- create `knoxx.backend.contracts.*` namespaces
- move loader/validator/role-resolution code out of `runtime`
- move contract watch/index logic out of route handlers
- keep public API behavior unchanged

Success condition:

- no namespace under `runtime` contains contract catalog semantics

## Phase 2: Extract contract resolution as a service

- move actor/agent/role/capability resolution out of `tooling.cljs`
- create one contract-resolution layer used by tools, routes, authz, and triggers
- remove duplicate “how do contracts work” logic from app code

Success condition:

- the backend has one authoritative path for effective contract resolution

## Phase 3: Introduce store/driver protocols

- define `contracts.store` and `policy.store`
- preserve current PostgreSQL path as one driver
- add MongoDB driver
- keep EDN file driver as canonical authoring path or first-class local mode

Success condition:

- routes and services consume protocols rather than concrete file/db implementations

## Phase 4: Materialize compiled contract views

- compile role/capability/tool graphs
- compile actor-to-agent availability
- compile model allowlists and defaults
- compile trigger/job views
- expose compiled snapshots for admin/debug UI

Success condition:

- request-time code stops recomputing the same expansions repeatedly

## Phase 5: Migrate selected persistence to MongoDB

- move flexible contract/policy materializations to MongoDB where it reduces friction
- keep relational storage only where it still buys something real
- retire duplicated SQL schema where contract validation already does the job

Success condition:

- MongoDB removes operational/schema friction without changing contract semantics

## Naming guidance

### Use `contracts`, not `contract_runtime`, for the domain

`contract_runtime` is only a good name if it specifically means “the live executor of compiled contracts.”

For the broader subsystem, `contracts` is clearer because it covers:

- authoring
- loading
- validation
- resolution
- compilation
- storage
- indexing
- watch/reload

That is a domain, not just a runtime.

### Use `runtime` only for live process concerns

This keeps future drift easy to detect.

If a namespace under `runtime` starts answering questions about contract meaning, it is probably in the wrong place.

## Immediate next implementation slice

The first practical slice should be:

1. create `knoxx.backend.contracts.loader`
2. create `knoxx.backend.contracts.validator`
3. create `knoxx.backend.contracts.resolve`
4. move watch/index logic into `contracts.watch` and `contracts.index`
5. update `tooling.cljs` and route handlers to depend on the new contract namespaces
6. leave behavior unchanged while shrinking the semantic role of `runtime`

That slice gives the backend a truthful architectural center before the larger MongoDB and large-file decomposition work begins.
