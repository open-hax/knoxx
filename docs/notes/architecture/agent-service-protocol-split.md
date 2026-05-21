---
original_name: "2026.05.20.22.16.34.md"
title: "Agent Service Protocol Split"
summary: "Cleanup plan splitting agent session logic into a service protocol, ports, schemas, reducers, and codecs."
category: "architecture"
created: "2026-05-20"
---

Signal

 (己, p=0.9) The main cleanup move is: do not make one giant “agent session
 protocol.”
 Split the current implicit agent surface into:

 1. A tiny consumer-facing Agent Service
 2. Behavioral protocols for effectful ports
 3. Malli/data schemas for all maps
 4. Pure reducers/codecs for stream + content normalization

 The code currently has good boundaries trying to emerge, but session.cljs,
 turn.cljs, and stream.cljs are acting as “gravity wells.”

 A clean consumer surface could be:

 ```clojure
   (defprotocol IAgentService
     (start-turn! [svc turn-request])
     (queue-turn! [svc turn-request])
     (control-turn! [svc control-request])
     (resume-turn! [svc recovery-request])
     (active-turn [svc conversation-id]))
 ```

 Everything outside infra.agent should mostly talk to that.

 Right now consumers leak across too many functions:

 - send-agent-turn!
 - spawn-direct!
 - queue-agent-control!
 - active-agent-session
 - ensure-session-id
 - ensure-conversation-access!
 - validate-chat-policy!
 - resume-recovered-session!

 That should collapse into a smaller facade, probably something like:

 ```clojure
   (knoxx.backend.infra.agent.service/start-turn! svc req)
   (knoxx.backend.infra.agent.service/queue-turn! svc req)
   (knoxx.backend.infra.agent.service/control-turn! svc req)
   (knoxx.backend.infra.agent.service/resume! svc req)
 ```

 The highest-value protocol candidates are:

 ┌─────────────────────────┬──────────────────────────────┬──────────────────┐
 │ Concept                 │ Why it deserves a protocol   │ Current gravity  │
 │                         │                              │ well             │
 ├─────────────────────────┼──────────────────────────────┼──────────────────┤
 │ IAgentProviderAdapter   │ Eta-mu/proxx interop should  │ session.cljs,    │
 │                         │ be swappable and testable    │ turn.cljs,       │
 │                         │                              │ extern/*         │
 ├─────────────────────────┼──────────────────────────────┼──────────────────┤
 │ IActiveSessionRegistry  │ In-process cache, TTL,       │ session.cljs     │
 │                         │ switching, eviction are      │                  │
 │                         │ separate from provider       │                  │
 │                         │ sessions                     │                  │
 ├─────────────────────────┼──────────────────────────────┼──────────────────┤
 │ IMessageHistory /       │ Rehydration/transcript       │ already partly   │
 │ ITranscriptCodec        │ building is core behavior    │ IMessageSource,  │
 │                         │ with multiple sources        │ plus             │
 │                         │                              │ session.cljs,    │
 │                         │                              │ transcript.cljs  │
 ├─────────────────────────┼──────────────────────────────┼──────────────────┤
 │ IToolCatalog /          │ Tool                         │ session.cljs,    │
 │ IToolPolicyResolver     │ availability/signature/auth  │ hydration.cljs,  │
 │                         │ is policy, not session       │ tooling          │
 │                         │ construction                 │                  │
 ├─────────────────────────┼──────────────────────────────┼──────────────────┤
 │ IContentCodec /         │ Content/media conversion is  │ content.cljs,    │
 │ IMediaMaterializer      │ duplicated and               │ turn.cljs,       │
 │                         │ provider-sensitive           │ session.cljs,    │
 │                         │                              │ message.cljs     │
 ├─────────────────────────┼──────────────────────────────┼──────────────────┤
 │ IStreamEventNormalizer  │ Provider JS events should    │ stream.cljs      │
 │                         │ normalize once into          │                  │
 │                         │ canonical CLJS events        │                  │
 ├─────────────────────────┼──────────────────────────────┼──────────────────┤
 │ IStreamReducer          │ Text/reasoning/tool          │ stream.cljs      │
 │                         │ lifecycle/death spiral logic │                  │
 │                         │ should be mostly pure        │                  │
 ├─────────────────────────┼──────────────────────────────┼──────────────────┤
 │ IRunEventSink           │ Run state, Redis,            │ stream.cljs,     │
 │                         │ OpenPlanner, WS broadcasting │ turn.cljs,       │
 │                         │ are sinks, not stream        │ run-state.cljs   │
 │                         │ semantics                    │                  │
 ├─────────────────────────┼──────────────────────────────┼──────────────────┤
 │ IPolicyEngine           │ The comment in policy.cljs   │ policy.cljs,     │
 │                         │ is correct: policy wants to  │ authz, contracts │
 │                         │ move toward contract DSL     │                  │
 ├─────────────────────────┼──────────────────────────────┼──────────────────┤
 │ IHydrationSource        │ RAG and memory hydration are │ hydration.cljs   │
 │                         │ pluggable sources with       │                  │
 │                         │ common result shape          │                  │
 ├─────────────────────────┼──────────────────────────────┼──────────────────┤
 │ IRecoveryCoordinator    │ Startup/periodic/shutdown    │ resume.cljs,     │
 │                         │ recovery is a lifecycle      │ recovery.cljs    │
 │                         │ service                      │                  │
 └─────────────────────────┴──────────────────────────────┴──────────────────┘

 For session.cljs, I’d split it like this:

 ┌───────────────────────────────────┬───────────────────────────────────────┐
 │ Current concern in session.cljs   │ Better home                           │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ sessions*, TTL sweep, eviction,   │ infra.agent.session-registry          │
 │ active-agent-session, remove      │ implementing IActiveSessionRegistry   │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ ensure-eta-mu-runtime!, model     │ infra.agent.provider.eta-mu           │
 │ registry, model lookup            │ implementing IAgentProviderAdapter    │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ visible-session-signature,        │ domain.agent.tools /                  │
 │ allowed tools, custom tools       │ infra.agent.tool-catalog              │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ rehydrate-session-manager!        │ infra.agent.history using existing    │
 │                                   │ IMessageSource                        │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ prune-session-messages / context  │ domain.agent.context-policy           │
 │ policy                            │                                       │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ materialize!, fetch-b64!,         │ domain.agent.content-codec or         │
 │ media-map                         │ infra.agent.media-materializer        │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ extension lifecycle dispatch      │ infra.agent.lifecycle-events          │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ actor live route registration     │ domain.actor.live-route or an event   │
 │                                   │ sink                                  │
 └───────────────────────────────────┴───────────────────────────────────────┘

 For stream.cljs, I’d split it like this:

 ┌───────────────────────────────────┬───────────────────────────────────────┐
 │ Current concern in stream.cljs    │ Better home                           │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ Provider JS event shape parsing   │ infra.agent.stream.provider-events    │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ diff-appended-text,               │ domain.agent.text-delta               │
 │ cumulative/replay suppression     │                                       │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ <think> tag routing               │ shared with message.cljs; probably    │
 │                                   │ domain.agent.reasoning                │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ tool lifecycle start/update/end   │ domain.agent.tool-lifecycle           │
 │ normalization                     │                                       │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ death spiral detection            │ domain.agent.turn-guards              │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ run-state mutation + trace blocks │ infra.agent.run-sink                  │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ WS broadcasting/session-store     │ infra.agent.event-sinks               │
 │ streaming flags                   │                                       │
 ├───────────────────────────────────┼───────────────────────────────────────┤
 │ build-subscribe-handler           │ thin composition only                 │
 └───────────────────────────────────┴───────────────────────────────────────┘

 (己, p=0.86) Data shapes I see worth naming explicitly:

 ┌─────────────────────────┬─────────────────────────────────────────────────┐
 │ Shape                   │ Canonical keys / notes                          │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ AgentSpec               │ :contract-id, :actor-id, :contract-actors,      │
 │                         │ :role, :model, :thinking-level, :system-prompt, │
 │                         │ :task-prompt, :tool-policies,                   │
 │                         │ :resource-policies, :sources,                   │
 │                         │ :memory-hydration, :context-policy,             │
 │                         │ :sub-agent-id, :parent-agent-id,                │
 │                         │ :parent-run-id, :spawn-kind                     │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ TurnRequest             │ :conversation-id, :session-id, :run-id,         │
 │                         │ :message, :content-parts, :template-context,    │
 │                         │ :model, :mode, :auth-context, :thinking-level,  │
 │                         │ :agent-spec                                     │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ DirectStartPayload      │ external snake/camel/kebab input normalized by  │
 │                         │ runner.cljs                                     │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ AuthContext             │ :orgId, :orgSlug, :userId, :userEmail,          │
 │                         │ :membershipId, :actorId, :roleSlugs,            │
 │                         │ :permissions, :toolPolicies,                    │
 │                         │ :membershipToolPolicies, :isSystemAdmin; also   │
 │                         │ nested :user, :org, :membership in some places  │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ ToolPolicy              │ {:toolId string :effect string}                 │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ ContextPolicy           │ :max-messages, :max-chars, :preserve-system     │
 │                         │ plus camel/snake aliases                        │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ StoredMessage           │ :role, :content, optional :summary,             │
 │                         │ :content-parts, :usage, :tokensBefore; roles    │
 │                         │ include "user", "assistant", "system",          │
 │                         │ "compactionSummary"                             │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ ProviderAgentMessage    │ JS eta-mu message: "role", "content" string or  │
 │                         │ array, "text", "summary", "usage", "reasoning"  │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ ContentPart             │ :type `"text"                                   │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ EtaMuAttachment         │ :type, raw base64 :data, :mimeType, optional    │
 │                         │ :filename                                       │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ HydrationResult         │ semantic/RAG result with :results, :elapsedMs,  │
 │                         │ :query, :tokens, :database                      │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ MemoryHydrationResult   │ :hits, :mode, :elapsedMs, :conversationId,      │
 │                         │ :query                                          │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ ActiveSessionEntry      │ :session, :model-id, :tool-signature,           │
 │                         │ :session-id, :actor-id, :last-accessed          │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ RuntimeSetup            │ :auth-storage, :model-registry,                 │
 │                         │ :settings-manager, :loader, :runtime-dir        │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ VisibleSessionSignature │ printable map of :tools, :contract-id,          │
 │                         │ :actor-id, :role, :system-prompt, :task-prompt  │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ StreamState             │ mutable map of atoms: :chunks,                  │
 │                         │ :reasoning-chunks, :last-assistant-text*,       │
 │                         │ :last-reasoning-text*, :ttft-recorded?,         │
 │                         │ :aborting?, :tool-loop*, etc.                   │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ ProviderStreamEvent     │ JS event types: "message_update",               │
 │                         │ "message_end", "tool_execution_start",          │
 │                         │ "tool_execution_update", "tool_execution_end",  │
 │                         │ "turn_end", "agent_end"                         │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ CanonicalRunEvent       │ :run_id, :conversation_id, :session_id, :type,  │
 │                         │ :at, plus event-specific fields                 │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ TokenEvent              │ WS token event: `:kind "assistant_message"      │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ ToolReceipt             │ :id, :tool_name, :status, :started_at,          │
 │                         │ :ended_at, :is_error, :input, :input_preview,   │
 │                         │ :result, :result_preview, :updates,             │
 │                         │ :content_parts                                  │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ TraceBlock              │ text/reasoning blocks and tool-call blocks;     │
 │                         │ currently mixed camel/kebab keys like           │
 │                         │ :toolName, :toolCallId, :inputPreview           │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ KnoxxRun                │ full run state: ids, status, model,             │
 │                         │ timing/tokens, answer, reasoning, content       │
 │                         │ parts, events, trace blocks, tool receipts,     │
 │                         │ request messages, settings, resources           │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ SessionStoreRecord      │ Redis session state: ids, status, model, mode,  │
 │                         │ thinking level, timestamps, active stream flag, │
 │                         │ messages, auth snapshot, agent spec,            │
 │                         │ answer/error                                    │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ TurnResponse            │ :answer, :run_id, :runId, :conversation_id,     │
 │                         │ :conversationId, :session_id, :model,           │
 │                         │ :content_parts, :sources, :message_parts,       │
 │                         │ :compare                                        │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ AcceptedResponse        │ async queue response: :ok, :queued, :run_id,    │
 │                         │ :conversation_id, :session_id, :model           │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ RecoveryRequest/Result  │ session-derived resume input; result has        │
 │                         │ :session_id, :conversation_id, :resumed,        │
 │                         │ optional :reason, :error, :wait_for             │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ ControlRequest/Response │ :conversation-id, :session-id, :run-id,         │
 │                         │ :message, `:kind "follow_up"                    │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ TemplateContext         │ :agent, :auth, :actor-id, :actorId,             │
 │                         │ :contract-id, :contractId, :role, :model, plus  │
 │                         │ event/source/payload variants                   │
 ├─────────────────────────┼─────────────────────────────────────────────────┤
 │ PolicyConstraints       │ :allowedModels/:allowed-models/:models,         │
 │                         │ :maxRequests/:max-requests,                     │
 │                         │ :windowSeconds/:window-seconds                  │
 └─────────────────────────┴─────────────────────────────────────────────────┘

 (己, p=0.82) I would not make these data shapes into protocols. Make them
 Malli schemas under something like:

 ```text
   backend/src/cljs/knoxx/backend/shape/agent_runtime.cljs
 ```

 or expand:

 ```text
   backend/src/cljs/knoxx/backend/shape/session_persistence.cljs
 ```

 Then reserve defprotocol for replaceable behavior: provider, registry, sinks,
 tool catalog, hydration, policy, recovery.

 One concrete smell: shape/session_persistence.cljs already defines some of
 this, but it appears narrower than the runtime reality. Example: ToolReceipt
 schema says statuses are "running"|"done"|"error", while stream.cljs writes
 "running"|"completed"|"failed". That is exactly the kind of drift a canonical
 shape namespace should kill.

 Evidence

 (世, p=0.93) Reviewed these files:

 - backend/src/cljs/knoxx/backend/domain/agent/agent_context.cljs
 - backend/src/cljs/knoxx/backend/domain/agent/agent_templates.cljs
 - backend/src/cljs/knoxx/backend/domain/agent/content.cljs
 - backend/src/cljs/knoxx/backend/infra/agent/session.cljs
 - backend/src/cljs/knoxx/backend/infra/agent/stream.cljs
 - backend/src/cljs/knoxx/backend/infra/agent/turn.cljs
 - backend/src/cljs/knoxx/backend/infra/agent/hydration.cljs
 - backend/src/cljs/knoxx/backend/infra/agent/runner.cljs
 - backend/src/cljs/knoxx/backend/infra/agent/runtime.cljs
 - backend/src/cljs/knoxx/backend/infra/agent/recovery.cljs
 - backend/src/cljs/knoxx/backend/infra/agent/resume.cljs
 - backend/src/cljs/knoxx/backend/infra/agent/message.cljs
 - backend/src/cljs/knoxx/backend/infra/agent/transcript.cljs
 - backend/src/cljs/knoxx/backend/infra/agent/tools.cljs
 - backend/src/cljs/knoxx/backend/infra/agent/policy.cljs
 - backend/src/cljs/knoxx/backend/infra/agent/provider.cljs
 - backend/src/cljs/knoxx/backend/shape/agent.cljs
 - backend/src/cljs/knoxx/backend/shape/session_persistence.cljs

 (世, p=0.9) Existing good precedent:

 - backend/src/cljs/knoxx/backend/shape/agent.cljs already has IAgentSession.
 - backend/src/cljs/knoxx/backend/infra/stores/message_source.cljs already has
 IMessageSource.
 - backend/src/cljs/knoxx/backend/shape/session_persistence.cljs already uses
 Malli plus ISessionStore.

 (世, p=0.86) Specific “nasty because mixed concerns” hotspots:

 - session.cljs combines session registry, eta-mu runtime setup, model
 resolution, tool policy/signature, media materialization, history
 rehydration, extension events, and actor live routes.
 - stream.cljs combines provider event parsing, text delta correction,
 reasoning extraction, tool lifecycle, death-spiral abort, run-state mutation,
 Redis/session-store mutation, and WS broadcasting.
 - turn.cljs combines request normalization, auth, policy, hydration, media
 materialization, run creation, prompt sending, finalization, memory indexing,
 and session cleanup.

 (世, p=0.78) There is probable public-surface drift: infra/routes/app.cljs
 calls ensure-session-id with two args, while infra/agent/turn.cljs defines
 ensure-session-id with one arg. Even if this is hidden by compile state or
 another branch, it is the kind of mismatch a facade would prevent.

 Frames

 (己, p=0.82) Frame 1: This is primarily an application service boundary
 problem. The system has too many consumers importing internal helpers
 directly, so internals become de facto API.

 (己, p=0.78) Frame 2: This is a ports/adapters problem. Eta-mu, Redis,
 OpenPlanner, WS, auth, and contract policy are all effectful ports currently
 braided into session and stream logic.

 (己, p=0.74) Frame 3: This is a shape drift problem. Many maps are passed
 through opaque code without one canonical schema, so aliases, statuses,
 camel/kebab keys, and JS objects leak across layers.

 Countermoves

 (己, p=0.86) Do not protocolize every helper. Use protocols for effectful
 things you may swap or mock; use schemas and pure functions for
 maps/text/content.

 (己, p=0.83) Do not start by rewriting send-agent-turn!. First freeze the
 public request/response shapes, then carve out pure reducers/codecs where
 tests can lock behavior.

 (己, p=0.81) Be careful with status names before introducing schemas.
 Existing code uses both "completed"/"failed" and schema-era "done"/"error"
 language, so schemas should reflect reality or include explicit translation
 points.

 Next

 (己, p=0.88) Create a first-pass
 backend/src/cljs/knoxx/backend/shape/agent_runtime.cljs Malli schema
 namespace for AgentSpec, TurnRequest, ContentPart, StreamState, RunEvent,
 ToolReceipt, TraceBlock, SessionStoreRecord, and TurnResponse.
