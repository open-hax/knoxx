# Ingestion Contract Surfaces

This document enumerates every configurable surface in the current Knoxx ingestion runtime that should be promoted into contract EDN.

## Goal

Make ingestion tenant-scoped, source-scoped, modular, and portable. Runtime env vars should remain infrastructure defaults, while client behavior should be described by contracts.

## Current configurable surfaces

### Infrastructure defaults

These remain deploy/runtime concerns and should not generally vary per client contract:

- `:port`
- `:database-url`
- `:redis-url`
- `:workspace-path`
- `:knoxx-backend-url`
- `:knoxx-api-key`
- `:knoxx-user-email`
- `:qdrant-url`
- `:proxx-url`
- `:proxx-auth-token`
- `:proxx-connection-timeout-ms`
- `:proxx-socket-timeout-ms`
- `:openplanner-url`
- `:openplanner-api-key`
- `:ragussy-url`

These currently come from `kms_ingestion.config/config`.

### Source contract surfaces

These should be represented in a source contract.

#### Identity

- `:contract/id`
- `:contract/type`
- `:contract/version`
- `:tenant/id`
- `:source/id`
- `:source/name`
- `:source/enabled?`
- `:source/driver`

#### Driver configuration

These are driver-specific and currently live in `:config` JSON blobs or are implicit:

- `:source/config`
- `:source/config :root-path`
- `:source/config :root-folder-id`
- `:source/config :credentials-secret`
- `:source/config :scopes`
- `:source/config :api-base-url`
- `:source/config :supports-watch?`

The contract runtime should treat `:source/config` as a typed per-driver map.

#### Discovery rules

These are currently split between DB fields, local driver heuristics, and watcher logic:

- `:source/discovery :file-types`
- `:source/discovery :include-patterns`
- `:source/discovery :exclude-patterns`
- `:source/discovery :text-extensions`
- `:source/discovery :text-filenames`
- `:source/discovery :skip-dirs`
- `:source/discovery :skip-files`
- `:source/discovery :skip-extensions`
- `:source/discovery :hidden-policy`
- `:source/discovery :follow-symlinks?`

These are currently hardcoded in `drivers/local.clj` and partially overridden via source fields.

#### Scheduling

These are currently derived from `sync_interval_minutes`, scheduler polling, or passive watch defaults:

- `:source/schedule :mode` ; `:poll`, `:watch`, `:manual`, `:hybrid`
- `:source/schedule :sync-interval-minutes`
- `:source/schedule :scheduler-poll-ms`
- `:source/schedule :passive-watch-enabled?`
- `:source/schedule :passive-watch-poll-ms`
- `:source/schedule :passive-watch-debounce-ms`
- `:source/schedule :bootstrap?`

#### Ingest execution

These are currently global env vars but should become source-level controls with infra defaults:

- `:source/ingest :batch-size`
- `:source/ingest :batch-parallelism`
- `:source/ingest :batch-delay-ms`
- `:source/ingest :throttle-enabled?`
- `:source/ingest :max-load-per-core`
- `:source/ingest :throttle-sleep-ms`
- `:source/ingest :retry-failed?`

#### Sink routing

The worker currently decides between OpenPlanner and Ragussy based on environment and driver type. This should be explicit contract data.

- `:source/sink :type` ; `:openplanner`, `:ragussy`, `:event-log`, future `:qdrant`
- `:source/sink :target-ref`
- `:source/sink :collections`
- `:source/sink :lake`
- `:source/sink :visibility`
- `:source/sink :source-label`
- `:source/sink :created-by`
- `:source/sink :language`

#### Semantic graph enrichment

These are currently global env functions in `config.clj` or hardcoded in `ingest_support.clj`.

- `:source/semantic :enabled?`
- `:source/semantic :build-mode` ; `:incremental`, `:off`
- `:source/semantic :k`
- `:source/semantic :min-similarity`
- `:source/semantic :emit-graph-events?`

#### Translation

These are currently global:

- `:source/translation :enabled?`
- `:source/translation :model`
- `:source/translation :poll-ms`

#### OpenPlanner event/document projection

These values are embedded in payload assembly and should be overridable per tenant/source:

- `:source/projection :domain-strategy` ; `:first-path-segment`, `:fixed`, future fn ref
- `:source/projection :document-kind-strategy`
- `:source/projection :visibility`
- `:source/projection :language`
- `:source/projection :created-by`
- `:source/projection :source`
- `:source/projection :metadata-template`

#### Backpressure policy

OpenPlanner backpressure is currently hardcoded exponential delay behavior.

- `:source/backpressure :strategy` ; `:exponential`, `:fixed`, `:none`
- `:source/backpressure :base-delay-ms`
- `:source/backpressure :max-delay-ms`
- `:source/backpressure :failure-window`
- `:source/backpressure :respect-remote?`

## Hardcoded values that need extraction

### `drivers/local.clj`

- `skip-dirs`
- `skip-files`
- `skip-extensions`
- `default-text-extensions`
- `default-text-filenames`
- hidden-directory exceptions such as `.github`

### `jobs/control.clj`

- executor thread count `4`
- pacing thresholds in `control-delay-ms`
- OpenPlanner backpressure cap `60000`
- exponential shift schedule in `note-openplanner-failure!`

### `jobs/ingest_support.clj`

- OpenPlanner `/v1/documents`
- OpenPlanner `/v1/events`
- semantic edge endpoint `/v1/jobs/build-semantic-edges/incremental`
- semantic edge defaults `:k 8`, `:minSimilarity 0.5`
- event batch size `20` for pi sessions
- document defaults `visibility="internal"`, `language="en"`, `createdBy="kms-ingestion"`, `source="kms-ingestion"`
- `derive-domain` heuristic based on first path segment

### `server.clj`

- default workspace source bootstrap
- default workspace root `/app/workspace/devel`
- default pi sessions root `/home/err/.pi/agent/sessions`
- default workspace file types and exclude patterns
- source creation bootstrap assumptions for tenants `devel` and `knoxx-session`

## Contract layering model

Recommended precedence:

1. Runtime infrastructure defaults
2. Global contract defaults (`contracts/_defaults.edn`)
3. Tenant defaults (`contracts/<tenant>/_defaults.edn`)
4. Source contract (`contracts/<tenant>/sources/<source>.edn`)
5. Manual per-job overrides

## Recommended contract split

- `:runtime/*` for deploy-time infra defaults
- `:target/*` for reusable sink/service definitions
- `:source/*` for tenant/source behavior
- `:policy/*` for org-wide rules shared across sources

## First migration target

The first refactor should move these surfaces into contract EDN immediately:

- discovery filters
- scheduling/watch behavior
- ingest batching/throttle policy
- sink routing
- semantic edge policy
- projection defaults

Driver-specific auth and external credentials should be referenced indirectly through secret refs such as `:env/FOO`, never stored inline.
