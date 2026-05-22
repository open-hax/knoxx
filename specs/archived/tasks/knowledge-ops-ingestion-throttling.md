# Knowledge Ops — Ingestion Throttling Spec

> *Ingestion is background work, not a denial-of-service attack against your own machine.*

---

## Purpose

Define the load-aware throttling model for ingestion. Ingesting large corpora must never monopolize the workstation or host while it is happening.

This matters most for the canonical workspace tenant, `devel`, where the operator is also actively using the machine.

---

## Requirement

Ingestion must:
- run in the background
- be cancellable
- back off under load
- expose progress
- avoid turning a developer workstation into a frozen box

---

## Current Implementation

Implemented in:
- `services/futuresight-kms/kms-ingestion/src/kms_ingestion/jobs/worker.clj`

### Added controls

| Env var | Default | Purpose |
|---------|---------|---------|
| `INGEST_BATCH_SIZE` | `10` | Files per processing batch |
| `INGEST_THROTTLE_ENABLED` | `true` | Enable load-aware throttling |
| `INGEST_MAX_LOAD_PER_CORE` | `0.80` | Target fraction of total host CPU capacity |
| `PASSIVE_WATCH_ENABLED` | `true` | Enable passive filesystem change queue |
| `PASSIVE_WATCH_POLL_MS` | `60000` | Watch registration resync interval |
| `PASSIVE_WATCH_DEBOUNCE_MS` | `5000` | Debounce bursty filesystem events into one job |

### Load metric

Current worker now uses:
- cgroup CPU usage (`/sys/fs/cgroup/cpu.stat`) when available
- converted into **raw CPU cores consumed**
- compared against `host_cores * INGEST_MAX_LOAD_PER_CORE`

For example on a 22-core host:
- target = `22 * 0.80 = 17.6 cores`
- if ingestion uses 3.0 cores, it is still below target

This is a better operator contract than per-container percentages because the user asked for:

> “80% of the total system's capacity.”

---

## Operational Model

```
watch filesystem events -> debounce -> queue incremental job

for each file in each batch:
  read cgroup CPU usage
  smooth usage with EMA
  compute target host-core budget
  sleep for a small control delay
  extract file
  post to OpenPlanner
  let OpenPlanner index via Proxx -> Ollama -> Chroma

for initial crawl:
  lazily stream candidate files
  partition into bounded batches
  update progress continuously
```

This is now a closed-loop pacing model rather than a simple stop/go throttle.

---

## Future Improvements

### 1. Control-theory smoothing

Current pacing is proportional + smoothed EMA. Future improvement:
- explicit PI controller on host-core budget
- bounded integral term
- optional per-source rate weights

### 2. Memory-aware throttling

Pause when free memory falls below a threshold.

### 3. Queue fairness

If multiple jobs exist:
- round-robin by source
- avoid one job starving all others

### 4. Dynamic batch sizing

Still needed:
- shrink batch size when files are large
- grow batch size when CPU is far below target
- include OpenPlanner / embed latency in the control loop

### 5. UI exposure

Surface throttle state in the ingestion UI:
- `running`
- `throttled`
- `paused`
- `cancelled`

---

## Existing Code References

| File | What it does |
|------|--------------|
| `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/config.clj` | throttle + passive watch env configuration |
| `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/jobs/worker.clj` | streaming ingest and host-core control loop |
| `orgs/open-hax/knoxx/ingestion/src/kms_ingestion/server.clj` | WatchService event queue + bootstrap |
| `orgs/open-hax/knoxx/frontend/src/pages/ChatPage.tsx` | current operator-facing ingestion visibility |

---

## Operator Positioning

The important claim is:

> “The system can ingest live corpora in the background without freezing the machine. It knows how to back off under load.”

That is a meaningful differentiator from naive local RAG systems because it respects the operator's machine.

---

## Status

Specified 2026-04-02. Updated 2026-04-03.

Current state:
- streaming initial crawl: **implemented**
- passive change queue: **implemented** (WatchService)
- GPU embedding path: **implemented** (OpenPlanner -> Proxx -> Ollama)
- host-capacity-aware throttling: **implemented**
- true constant-load control loop: **partial**
