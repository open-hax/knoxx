# Actor Realtime Bus: Socket.IO Pub/Sub Spec

Status: draft spec 2026-05-15
Scope: `frontend/`, Knoxx backend realtime surfaces, actor mailbox, trigger/action runtime

## 1. Recovered intent

The immediate pain is not only that `/api/memory/sessions` is slow. The deeper problem is architectural:

1. Several frontend surfaces independently poll the same backend facts.
2. Polling is component-local, so two mounted components can issue basically identical requests without knowing about each other.
3. Some pages fetch a broad list and then filter client-side. That is why the selected-agent audit pane can fail to show sessions: the wanted session may be outside the first page, and `/api/memory/sessions` is not being asked for an exact actor/contract-indexed projection.
4. Realtime information already exists in the system (`/ws/stream`, ingestion job websockets, actor mailbox, trigger runtime), but it is not unified under the actor model.
5. The system vocabulary should become literal computer-science vocabulary:
   - actors own mailboxes;
   - messages are sent to actors;
   - actions are causally attached to messages;
   - events are immutable facts emitted by actors;
   - projections are derived state;
   - triggers observe the same event stream the UI observes.

The target shape: **Socket.IO is the easy transport, not the architecture.** The architecture is an actor-message bus with durable envelopes, mailbox auditability, trigger visibility, server-side projection snapshots, and frontend subscription de-duplication.

## 2. Non-negotiable doctrine

### 2.1 Actor means actor

An actor is a principal that can own state and receive messages.

Required actor kinds:

| Actor kind | Examples | Mailbox owner? | Notes |
|---|---|---:|---|
| Human actor | signed-in user, admin operator | yes | Browser sockets are endpoints for this actor, not independent actors. |
| Agent actor | `discord_automation`, `fork_tales_creative_director`, `eta-mu` | yes | An agent is not merely a prompt; it is an actor executing under contracts. |
| Session manager actor | `system.session-manager` | yes | Owns session index, session status, active conversation routing. |
| Trigger runtime actor | `system.trigger-runtime` | yes | Observes event stream and launches actions/pipelines. |
| Ingestion actor | `system.ingestion`, per-source workers | yes | Owns source/job progress events. |
| Data/graph actor | `system.data`, `system.graph-weaver` | yes | Owns data health/embedding coverage/graph projections. |
| Proxx actor | `system.proxx-gateway` | yes | Owns provider/model health observations. |
| Studio actor | `broadcast-studio`, `system.studio-persistence` | yes | Owns player/playlist/audio-library state. |

### 2.2 Every action must have a mailbox trail

A client action is not allowed to be "just a socket event". It must become an actor message.

Minimum command path:

1. Client emits `command` to Socket.IO.
2. Backend resolves authenticated source actor.
3. Backend writes a mailbox entry or action event with `source_actor_id`, `target_actor_id`, `mailbox_id`, `correlation_id`, and `causation_id`.
4. Backend dispatches to the action/trigger runtime.
5. Backend publishes accepted/progress/completed events to subscribed rooms.
6. Trigger/action systems observe the same envelope stream.

### 2.3 Polling is allowed only at actor boundaries

Browser-local polling is the anti-pattern. Backend actor polling is acceptable when the upstream has no push mechanism, but it must be:

- centralized once per service/tenant, not once per browser component;
- represented as observations from a system actor;
- coalesced before broadcast;
- written to a durable or replayable stream when semantically important.

Examples:

- GPU/process stats can be volatile backend observations.
- Proxx health can be a backend `system.proxx-gateway` observation every N seconds.
- Session/run/agent state should be event-driven from the runtime, not polled.

## 3. Current frontend polling inventory

This sweep used `rg` over `frontend/src` for `setInterval`, `setTimeout`, `poll`, websocket, and API calls. Line numbers are current approximate anchors and may drift.

### 3.1 Network polling / repeated remote writes to migrate

| Current location | Frequency / trigger | Current requests | Problem | Socket.IO model |
|---|---:|---|---|---|
| `frontend/src/components/agent-audit/AgentAuditLogs.tsx:660-692` | every 10s while `autoRefresh` | `GET /api/memory/sessions`, `GET /api/admin/agents/active` or `/api/knoxx/agents/active` | Duplicate with chat/sidebar audit; broad sessions list filtered client-side. | Subscribe `session.index` and `run.active` with `{actorId?, contractId?, mode}`. Server sends `session.index.snapshot`, then `session.index.patch`, `run.started`, `run.status`, `run.completed`. |
| `frontend/src/components/agent-audit/AgentAuditLogs.tsx:780-851` | every 10s for selected session | `GET /api/runs/:id`, `GET /api/knoxx/session/status`, `GET /api/knoxx/run/:runId/events`, `GET /api/openplanner/v1/sessions/:id` | Re-fetches a timeline that should be append-only events. | Subscribe `session.timeline` / `run.timeline`; server sends replay from `since_event_id` and live events. |
| `frontend/src/components/agent-audit/AgentAuditSessionList.tsx:220-248` | on mount + every 60s + scroll-bottom refresh | `GET /api/memory/sessions`, active-agent list | Second independent session poller on the same page; selected-agent sessions can be missed if outside first page. | Shared `useSessionIndex({contractId})` store with subscription de-dupe. Server-side contract/actor index, not client-side first-page filtering. |
| `frontend/src/components/chat-page/chat-runtime-effects.ts:323-329` | every 4s while sending | `GET /api/runs/:runId` | Existing websocket streams events/tokens, but final run detail is polled. | Runtime emits `run.snapshot` on material changes and `run.completed` with final detail refs. Client asks replay once on reconnect. |
| `frontend/src/components/chat-page/chat-runtime-effects.ts:339-342` | every 30s | `GET /api/memory/sessions` via `refreshRecentSessions` | Global recent session list is repeatedly pulled even while idle. | Subscribe `actor.sessions` / `session.index` for current actor/filter. |
| `frontend/src/components/chat-page/chat-runtime-effects.ts:355-358` | every 10s | `GET /api/ingestion/sources`, `GET /api/ingestion/jobs?...` via `refreshWorkspaceStatus` | Repeated workspace status fetch per chat page. | Subscribe `ingestion.source.changed`, `ingestion.job.changed`, `workspace.status.snapshot`. |
| `frontend/src/components/chat-page/hooks.ts:701-723` (`useProxxStatusPolling`) | every 5s | `GET /api/proxx/health`, `GET /api/proxx/models` | Per-client health/model polling; expensive when multiple tabs open. | Backend `system.proxx-gateway` actor publishes `proxx.health.snapshot`, `proxx.models.changed`. Client subscribes `proxx.status`. |
| `frontend/src/pages/DocumentsPage.tsx:63-88` | every 2s | `GET /api/documents/ingestion-progress`; sometimes reloads ingestion history | Old docs ingestion progress poll. | Subscribe `documents.ingestion.progress`; completion emits `documents.ingestion.history.changed`. |
| `frontend/src/components/SidebarOpsStatus.tsx:48-62` | every 2.5s | `GET /api/documents/ingestion-progress` | Duplicates `DocumentsPage` ingestion polling. Stats already come through `/ws/stream`. | Subscribe same `documents.ingestion.progress` room; combine with `system.stats`. |
| `frontend/src/pages/DataPage.tsx:1530-1570` | on mount + every 15s | `GET /api/ingestion/sources`, `/api/ingestion/jobs`, `/api/data/health`, `/api/data/mongo/collections`, `/api/data/op/v1/graph/embedding-coverage` | Dashboard fanout poll; each tab/browser repeats it. | Subscribe `data.dashboard` snapshot and patch streams: `data.health.changed`, `ingestion.jobs.changed`, `graph.embedding_coverage.changed`, `mongo.collections.changed`. |
| `frontend/src/components/admin-page/DataLakesSection.tsx:91-108` | existing websocket + completion fetches | `/api/ingestion/ws/jobs/:jobId`, then `GET /api/ingestion/jobs`, `GET /api/ingestion/jobs/:id` | Already realtime, but custom WS and still fetches snapshots on terminal events. | Fold into Socket.IO `job:{jobId}` room; terminal event includes enough job summary, or emits `ingestion.job.snapshot`. |
| `frontend/src/pages/BroadcastStudioPage.tsx:776-783` | every 1s | `POST /api/studio/state` for player position/volume/current path | High-frequency remote write loop; not polling, but same traffic smell. | Emit volatile `studio.player.tick` over Socket.IO; backend coalesces and persists at lower cadence plus on `pause`, `track-change`, `visibilitychange`, and disconnect. |
| `frontend/src/lib/ws.ts:44-72` | reconnect timer | reconnects `/ws/stream` | Necessary transport retry, but custom. | Socket.IO handles reconnect, ack, missed-event replay, and connection state. |

### 3.2 Repeated but not interval polling

These are not timers, but they become part of the shared subscription/snapshot model to avoid duplicate loads across surfaces.

| Current location | Current behavior | Socket.IO model |
|---|---|---|
| `frontend/src/pages/CmsPage.tsx:157-244`, `822-854` | Loads browse data, recent sessions, gardens, ingestion sources/jobs on mount and manual selection. | Use shared `workspace.browser`, `session.index`, `garden.index`, and `ingestion.status` stores. |
| `frontend/src/components/chat-page/hooks.ts:490-580` (`useChatSessionRecovery`) | One-shot delayed session-status/run recovery. | `subscribe(session:{id}, since?)` returns `session.status.snapshot` immediately. |
| `frontend/src/components/chat-page/chat-runtime-effects.ts:110-135` | On WS reconnect, GETs missed run events since timestamp. | Socket.IO subscribe includes `since_event_id`; server replays missed durable events before live attach. |
| `frontend/src/components/admin-page/DataLakesSection.tsx:41-88` | Loads sources/jobs and source audit when panel/source opens. | Keep first snapshot request, then subscribe to source/job/audit invalidations. |

### 3.3 Timers that are not server polling

Do not migrate these blindly; they are UI, local media, or debounce timers.

| Location | Purpose | Keep / change |
|---|---|---|
| `ContractsPage.tsx:176` | blur delay for combobox closing | keep local |
| `SignupPage.tsx:34`, `LoginPage.tsx:65` | post-success navigation delay | keep local |
| `ChatComposer.tsx:314`, `GraphExplorer.tsx:300`, `EventAgentsPanel.tsx:69` | focus/debounce/component-load timeout | keep local |
| `chat-runtime-actions.ts:116` | UI defer | keep local |
| `useVoiceRecorder.ts:284-318`, `ConversationVoiceButton.tsx:180-202` | recording max-duration, STT chunk timer, audio analyser silence detection | keep local for analyser; optionally move STT chunks to Socket.IO binary stream later |
| `BroadcastStudioPage.tsx:789-792` | playlist save debounce | keep but route through coalesced store if server write |
| `frontend/e2e/knoxx_e2e_support.cjs:519` | test polling helper | keep test-only |

## 4. Socket.IO actor bus architecture

### 4.1 Transport

Add backend dependencies:

```json
{
  "dependencies": {
    "socket.io": "^4.x",
    "@socket.io/redis-adapter": "^8.x",
    "socket.io-client": "^4.x"
  }
}
```

Socket.IO attaches to the existing Fastify server:

```clojure
(ns knoxx.backend.realtime.socket-io
  (:require [knoxx.backend.auth-session :as auth-session]
            [knoxx.backend.actor-mailbox :as actor-mailbox]
            [knoxx.backend.events.dispatch :as events-dispatch]
            ["socket.io" :refer [Server]]))

(defn attach! [app runtime config]
  (let [io (Server. (.-server app)
                    #js {:path "/socket.io"
                         :cors #js {:origin true :credentials true}})]
    ;; middleware resolves auth context and actor id
    io))
```

Keep `/ws/stream` during migration. The first Socket.IO adapter should mirror existing `tokens`, `events`, `stats`, and `lounge` channels so frontend migration is incremental.

### 4.2 One namespace, many authorized rooms

Use a single namespace `/rt` or the default namespace with typed rooms. Prefer one namespace at first to avoid ACL drift.

Canonical rooms:

| Room | Meaning | Who may subscribe |
|---|---|---|
| `org:{orgId}` | org-wide admin/control-plane events | org admins/operators |
| `actor:{actorId}` | actor presence and actor-scoped projections | that actor, admin, explicitly delegated actors |
| `actor:{actorId}:mailbox` | actor inbox/outbox events | that actor and authorized operators |
| `session:{sessionId}` | browser/runtime session status | owner actor or operator |
| `conversation:{conversationId}` | conversation/session timeline | participants, owner actor, operator |
| `run:{runId}` | run tokens/events/snapshots | participants/operator |
| `contract:{contractId}:sessions` | sessions indexed by contract | users allowed to view that contract/actor |
| `ingestion:tenant:{tenantId}` | ingestion sources/jobs summary | tenant/org operators |
| `job:{jobId}` | ingestion job progress | source/job-visible operators |
| `data:dashboard:{tenantId}` | data page dashboard projections | data-plane viewers |
| `proxx:status` | model/provider health projection | authenticated users with chat access |
| `studio:{surface}` | broadcast studio state/actions | actors allowed for that studio surface |
| `system:stats` | CPU/RAM/GPU stats | authenticated users, optionally admin-only |
| `lounge` | lounge messages | authenticated users with lounge access |

### 4.3 Envelope schema

Every durable or semantically relevant event uses the same envelope.

```ts
type ActorId = string;
type ISOInstant = string;

type ActorRef = {
  actorId: ActorId;
  kind: "human" | "agent" | "system" | "worker" | "unknown";
  orgId?: string;
  membershipId?: string;
  contractId?: string;
};

type MailboxRef = {
  mailboxId: string;
  sourceBox?: "outbox";
  targetBox?: "inbox";
  sequence?: number;
};

type RealtimeEnvelope<T = unknown> = {
  envelopeVersion: 1;
  eventId: string;
  eventType: string;
  occurredAt: ISOInstant;
  source: ActorRef;
  target?: ActorRef | { room: string; selector?: Record<string, unknown> };
  mailbox?: MailboxRef;
  correlationId: string;
  causationId?: string;
  sessionId?: string;
  conversationId?: string;
  runId?: string;
  contractId?: string;
  visibility: "public" | "org" | "actor" | "session" | "admin";
  durability: "ephemeral" | "replayable" | "ledgered";
  payload: T;
  redactions?: Array<{ path: string; reason: string }>;
};
```

Rules:

- `eventId` is monotonic enough for replay ordering; use ULID or DB sequence-backed IDs for durable streams.
- `correlationId` links a user command through mailbox entry, trigger dispatch, run, tool calls, and UI updates.
- `causationId` points to the event/message that caused this event.
- Durable actor messages must have `mailbox.mailboxId`.
- Ephemeral stats can omit mailbox but must still have `source.actorId`.

### 4.4 Client protocol

Client connects once per browser tab/session.

```ts
socket.emit("hello", {
  clientId,
  lastSeenEventIds: {
    "actor:riatzukiza:mailbox": "evt_...",
    "session:abc": "evt_..."
  }
});
```

Subscribe:

```ts
socket.emit("subscribe", {
  requestId: "sub_123",
  selector: "session.index",
  filter: { actorId: "fork_tales_creative_director", contractId: "fork_tales_creative_director", limit: 40 },
  sinceEventId: "evt_optional"
}, ack);
```

Server responds:

```ts
socket.emit("snapshot", envelope({
  eventType: "session.index.snapshot",
  source: { actorId: "system.session-manager", kind: "system" },
  payload: { rows, total, hasMore, selectorHash }
}));
```

Then live patches:

```ts
socket.emit("event", envelope({
  eventType: "session.index.patch",
  payload: { upsert: [summary], remove: [] }
}));
```

Commands:

```ts
socket.emit("command", {
  commandType: "agent.run.requested",
  target: { actorId: "fork_tales_creative_director" },
  payload: { message, contractId, conversationId },
  idempotencyKey: "client-generated"
}, ack);
```

Backend turns this into an actor mailbox entry and dispatches it. The ack means accepted into the mailbox, not completed.

### 4.5 Frontend singleton store

Add `frontend/src/lib/realtime/`:

- `socket.ts` — owns one `socket.io-client` connection.
- `selectors.ts` — canonical selector hashing and room/filter keys.
- `store.ts` — small in-memory external store with ref-counted subscriptions.
- `hooks.ts` — `useRealtimeSelector`, `useActorMailbox`, `useSessionIndex`, `useRunTimeline`, `useIngestionStatus`, `useDataDashboard`, `useProxxStatus`.

Important constraint: **components never instantiate their own polling loop**. Components declare data needs; the store de-dupes them.

Example:

```ts
const { rows, loading } = useSessionIndex({ contractId, actorId, limit: 40 });
```

If both `AgentAuditLogs` and `AgentAuditSessionList` ask for the same selector, only one Socket.IO subscription and one snapshot exist.

### 4.6 Backend canonical projections

Do not make the UI filter broad lists.

Required projections:

| Projection | Owned by | Indexed by | Source events |
|---|---|---|---|
| `session_index` | `system.session-manager` | `org_id`, `actor_id`, `contract_id`, `session_id`, `conversation_id`, `last_ts`, `status` | run started/completed, memory ingest, mailbox route updates |
| `active_run_index` | `system.session-manager` | `actor_id`, `contract_id`, `run_id`, `conversation_id`, `status` | runtime run events |
| `actor_mailbox_projection` | actor mailbox runtime | source actor, target actor, status, updated_at | mailbox create/ack/delivery |
| `ingestion_job_index` | `system.ingestion` | tenant, source_id, job_id, status | ingestion worker events |
| `data_dashboard_projection` | `system.data` | tenant | data health, graph embedding coverage, mongo collections |
| `proxx_status_projection` | `system.proxx-gateway` | global/model/provider | health/model refresh actor observations |
| `studio_state_projection` | `system.studio-persistence` | actor/surface/key | studio state patches |

This directly fixes the selected-agent sessions bug: `contract:{contractId}:sessions` should come from `session_index WHERE contract_id = $1 OR actor claims contain $1`, not from first-page `/api/memory/sessions` plus client filtering.

## 5. Event taxonomy

Use dotted event names. Domain first, noun second, lifecycle last.

### Session/run

- `session.index.snapshot`
- `session.index.patch`
- `session.status.changed`
- `session.timeline.snapshot`
- `session.timeline.appended`
- `run.active.snapshot`
- `run.started`
- `run.token.delta`
- `run.reasoning.delta`
- `run.tool.started`
- `run.tool.updated`
- `run.tool.completed`
- `run.completed`
- `run.failed`
- `run.snapshot`

### Mailbox/action/trigger

- `mailbox.entry.created`
- `mailbox.entry.delivered`
- `mailbox.entry.acknowledged`
- `mailbox.entry.failed`
- `action.requested`
- `action.accepted`
- `action.started`
- `action.completed`
- `action.failed`
- `trigger.matched`
- `trigger.dispatched`
- `trigger.skipped`

### Ingestion/data

- `ingestion.source.snapshot`
- `ingestion.source.changed`
- `ingestion.job.snapshot`
- `ingestion.job.started`
- `ingestion.job.progress`
- `ingestion.job.completed`
- `ingestion.job.failed`
- `data.health.changed`
- `data.mongo.collections.changed`
- `graph.embedding_coverage.changed`

### Proxx/model health

- `proxx.health.snapshot`
- `proxx.health.changed`
- `proxx.models.snapshot`
- `proxx.models.changed`

### Studio

- `studio.player.snapshot`
- `studio.player.tick` (volatile)
- `studio.player.persisted`
- `studio.playlist.changed`
- `studio.library.changed`
- `studio.discord_scan.started`
- `studio.discord_scan.progress`
- `studio.discord_scan.completed`

### System

- `system.stats.snapshot` (volatile)
- `lounge.message.created`

## 6. Current endpoint-to-topic migration map

| HTTP / WS surface | Keep as bootstrap? | Socket.IO replacement | Durability |
|---|---:|---|---|
| `GET /api/memory/sessions` | yes, temporary/manual refresh | `session.index.snapshot`, `session.index.patch` | replayable |
| `GET /api/openplanner/v1/sessions/:id?mode=full` | yes, deep history fallback | `session.timeline.snapshot`, `session.timeline.appended` | ledgered/replayable |
| `GET /api/admin/agents/active` | temporary | `run.active.snapshot`, run lifecycle events | replayable |
| `GET /api/knoxx/agents/active` | temporary | scoped `run.active.snapshot` | replayable |
| `GET /api/runs/:id` | yes for cold deep load | `run.snapshot`, `run.completed`, `run.failed` | replayable |
| `GET /api/knoxx/run/:runId/events` | no after replay | `run.timeline` replay by `sinceEventId` | replayable |
| `GET /api/knoxx/session/status` | temporary | `session.status.changed` + snapshot on subscribe | replayable |
| `GET /api/documents/ingestion-progress` | temporary | `documents.ingestion.progress` | replayable while active |
| `GET /api/ingestion/sources` | yes bootstrap | `ingestion.source.snapshot/changed` | replayable |
| `GET /api/ingestion/jobs` | yes bootstrap | `ingestion.job.snapshot/changed` | replayable |
| `WS /api/ingestion/ws/jobs/:jobId` | no after migration | `job:{jobId}` room, `ingestion.job.*` | replayable while active |
| `GET /api/data/health` | temporary | `data.health.changed` | ephemeral/replayable latest |
| `GET /api/data/mongo/collections` | temporary | `data.mongo.collections.changed` | replayable latest |
| `GET /api/data/op/v1/graph/embedding-coverage` | temporary | `graph.embedding_coverage.changed` | replayable latest |
| `GET /api/proxx/health` | temporary | `proxx.health.changed` | replayable latest |
| `GET /api/proxx/models` | temporary | `proxx.models.changed` | replayable latest |
| `POST /api/studio/state` every second | no high-frequency loop | `studio.player.tick` volatile + coalesced persist | volatile + periodic persisted snapshot |
| `WS /ws/stream` | migration bridge | Socket.IO `run.*`, `system.stats.*`, `lounge.*` | mixed |

## 7. Trigger/action integration

The realtime bus must not become a UI-only side channel.

### 7.1 Inbound commands

All inbound socket commands become normalized events before execution:

```edn
{:event/type :action.requested
 :event/id "evt_..."
 :source/actor-id "riatzukiza"
 :target/actor-id "fork_tales_creative_director"
 :mailbox/id "..."
 :correlation/id "..."
 :action/type :agent.run
 :payload {...}}
```

Then:

1. `actor-mailbox/create-entry!`
2. `events.dispatch/dispatch!`
3. action/pipeline/agent executor
4. mailbox delivery updates
5. Socket.IO publishes progress/completion

### 7.2 Outbound events

Backend code should publish through one function:

```clojure
(actor-bus/publish! runtime envelope)
```

`publish!` responsibilities:

- validate envelope shape;
- redact according to actor/room visibility;
- append durable/replayable events when needed;
- deliver to Socket.IO rooms;
- notify trigger runtime if the event kind is trigger-visible;
- optionally mirror to legacy `/ws/stream` during migration.

### 7.3 Trigger visibility

Every event has a `triggerVisible` flag or is matched by event type. Trigger contracts subscribe to the same event bus internally:

```edn
{:contract/kind :trigger
 :contract/id "when-studio-track-changes"
 :trigger/kind :event
 :trigger/source {:kind :actor-bus
                  :event-types ["studio.player.persisted" "studio.playlist.changed"]}
 :trigger/target "broadcast_studio_curator"}
```

## 8. Backend module plan

Add/reshape namespaces:

| Namespace | Responsibility |
|---|---|
| `knoxx.backend.realtime.envelope` | schema creation, validation, redaction helpers |
| `knoxx.backend.realtime.actor-bus` | `publish!`, `subscribe!`, replay API, trigger fanout |
| `knoxx.backend.realtime.socket-io` | transport attach, auth middleware, client subscribe/command handlers |
| `knoxx.backend.realtime.projections.session-index` | session/active-run materialized projection |
| `knoxx.backend.realtime.projections.ingestion` | source/job projection publisher |
| `knoxx.backend.realtime.projections.data-dashboard` | data page projection publisher |
| `knoxx.backend.realtime.projections.proxx` | backend-only Proxx polling actor and publisher |
| `knoxx.backend.realtime.projections.studio` | studio state coalescer/publisher |
| `knoxx.backend.realtime.legacy-ws-bridge` | temporary bridge from actor bus to `/ws/stream` envelopes |

## 9. Frontend migration plan

### 9.1 Shared realtime provider

Mount once near the app root:

```tsx
<RealtimeActorBusProvider actorId={auth.actorId} orgId={auth.orgId}>
  <App />
</RealtimeActorBusProvider>
```

### 9.2 Replace component polling with declarative selectors

Target hooks:

```ts
useSessionIndex({ actorId, contractId, excludeActorIds, limit });
useRunTimeline(runId);
useConversationTimeline(conversationId);
useActorMailbox(actorId, { box: "inbox" });
useIngestionJobs({ tenantId, sourceId });
useDataDashboard({ tenantId });
useProxxStatus();
useStudioPlayer(surface);
```

### 9.3 De-dup contract

Selector key = stable JSON of `{selector, filter}`. The provider:

- opens one Socket.IO connection;
- sends one subscribe per selector key;
- ref-counts subscribers;
- replays cached snapshot immediately to late subscribers;
- unsubscribes when ref count hits zero;
- supports manual refresh by `socket.emit("refresh", selectorKey)` instead of GET loops.

## 10. Phased implementation plan

### Phase 0 — guardrails and measurement

- Add network-budget instrumentation in dev/test: count `/api/memory/sessions` per page idle minute.
- Add a regression test for the selected-agent sessions bug: selected contract with session outside first generic page still appears.
- Add this spec to the docs.

Definition of done:

- Idle Agents/Audit page makes at most one initial session snapshot request and zero repeated `/api/memory/sessions` requests after stabilization.

### Phase 1 — Socket.IO mirror of existing stream

- Add Socket.IO server/client dependencies.
- Attach Socket.IO to Fastify.
- Mirror existing `/ws/stream` channels: `tokens`, `events`, `stats`, `lounge`.
- Add frontend `RealtimeActorBusProvider` and migrate chat stream from `connectStream` to Socket.IO behind a feature flag.

Definition of done:

- Chat token streaming and runtime events work through Socket.IO.
- Legacy `/ws/stream` still works.

### Phase 2 — session index projection and selected-agent fix

- Build `session_index` projection from OpenPlanner rows + active Redis/runtime sessions.
- Add server-side filter support by `actorId`, `contractId`, `excludeActorIds` without full first-page client filtering.
- Publish `session.index.snapshot/patch`.
- Replace `AgentAuditSessionList` and `AgentAuditLogs` session polling with `useSessionIndex`.

Definition of done:

- Selected agent sessions appear even if not in generic first page.
- No duplicate identical `/api/memory/sessions` requests from one page.

### Phase 3 — run/session timeline replay

- Publish durable run events with sequence IDs.
- Replace 4s `getRun` polling and 10s selected-session reload in `AgentAuditLogs`.
- Add `sinceEventId` replay on subscribe/reconnect.

Definition of done:

- Active and archived run timelines update without interval reload.

### Phase 4 — ingestion/data/proxx projections

- Migrate docs ingestion progress, data dashboard, ingestion sources/jobs, and Proxx health/model polling.
- Keep backend-originated polling where upstream has no push, but one poller per process/tenant only.

Definition of done:

- `DocumentsPage`, `SidebarOpsStatus`, `DataPage`, and chat workspace status have no browser intervals for remote state.

### Phase 5 — actor commands and mailbox-first UI actions

- Route socket `command` through actor mailbox and `events.dispatch/dispatch!`.
- Convert live controls (`abort`, `steer`, `follow-up`, `run agent`, mailbox ack) to mailbox-backed commands.
- Publish mailbox updates to actor rooms.

Definition of done:

- Every UI action has source actor, target actor, mailbox id, correlation id, and audit ledger entry.

### Phase 6 — studio and voice cleanup

- Replace 1s studio state POST loop with volatile ticks + coalesced persistence.
- Optional: move STT audio chunks to Socket.IO binary stream. Keep local analyser timers.

Definition of done:

- Broadcast studio no longer writes player state every second per tab.

### Phase 7 — scale and deletion

- Add Redis adapter for multi-instance Socket.IO rooms.
- Remove legacy custom websocket routes after a deprecation window.
- Delete component-level polling hooks and forbid new `setInterval(fetch...)` patterns with lint/test checks.

## 11. Verification plan

### Unit tests

- Envelope validator rejects missing actor on durable/action events.
- Subscribe ACL denies unauthorized actor/contract rooms.
- Selector store de-dupes identical subscriptions and ref-counts unsubscribe.
- Session-index projection returns selected contract sessions even outside generic first page.

### Component tests

- Agents/Audit page with both session list components mounted subscribes once.
- Selected agent session appears from `session.index.snapshot` without generic `/api/memory/sessions` response containing it.
- Run timeline appends socket events without calling `getRun` every 4s.

### Backend integration tests

- Socket connects with auth cookie/context.
- `subscribe(session.index)` returns snapshot and joins correct room.
- Publishing `run.started` updates `active_run_index` and emits to `contract:{contractId}:sessions`.
- Socket command creates mailbox entry and dispatches trigger-visible event.

### E2E/network tests

- Load Agents page, wait 65s idle, assert zero repeated identical `/api/memory/sessions` calls after initial bootstrap.
- Start a run, assert tokens/events update through Socket.IO.
- Disconnect/reconnect, assert missed events replay by `sinceEventId`.

## 12. Acceptance criteria

1. Idle frontend pages do not repeatedly call the same data endpoints.
2. The selected-agent audit/session pane shows correct sessions from a server-side actor/contract session projection.
3. Socket.IO has one connection per tab and shared selector subscriptions across components.
4. Every command/action is mailbox-backed and actor-attributed.
5. Trigger/action systems observe the same event envelopes the UI observes.
6. Existing `/ws/stream` behavior is preserved until migrated.
7. Durable events can be replayed after reconnect; volatile events can be dropped safely.
8. No socket payload leaks credentials or hidden tool inputs.

## 13. The shortest safe first cut

If we want the fastest high-value path:

1. Add Socket.IO and a frontend singleton provider.
2. Mirror existing run events/tokens/stats into Socket.IO.
3. Build only `session.index` first.
4. Replace `AgentAuditSessionList`, `AgentAuditLogs`, and chat recent-session polling with `useSessionIndex`.
5. Add the network-budget regression test for `/api/memory/sessions`.

That solves the visible pain while establishing the real actor bus shape instead of building another bespoke websocket island.
