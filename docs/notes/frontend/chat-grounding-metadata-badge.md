---
original_name: "2026.05.07.10.28.02.md"
title: "Chat Grounding Metadata Badge"
summary: "Explanation of frontend grounding metadata badges for assistant messages."
category: "frontend"
created: "2026-05-07"
---

 Signal

 “Grounding metadata” means the assistant message has either:

 - sources: structured grounding/citation sources: { title, url, section? }
 - contextRows: auto-injected context rows: { id, source_path, snippet, text, project, session, kind, ... }

 If neither array has entries, the frontend badge renders: No grounding metadata.

 Evidence

 In frontend/src/components/chat-page/ChatMessageList.tsx, the badge checks:

 - message.sources?.length
 - message.contextRows?.length
 - otherwise: "No grounding metadata"

 The relevant TypeScript shapes are in frontend/src/lib/types.ts:

 - ChatMessage.sources?: AgentSource[]
 - ChatMessage.contextRows?: GroundedContextRow[]
 - AgentSource is title, url, optional section
 - GroundedContextRow is the older grounded-answer/context-row shape with source_path, snippet, text, etc.

 Backend-side, backend/src/cljs/knoxx/backend/agent_hydration.cljs turns passive semantic hydration results into sources:

 ```clojure
   {:title (:name result)
    :url (:path result)
    :section (:snippet result)}
 ```

 Then backend/src/cljs/knoxx/backend/agents/turn.cljs attaches those as :sources on completed runs.

 The catch: historical/frontend-normalized chat messages from memoryRowsToMessages currently only reconstruct
 role/content/model/runId/traceBlocks. They do not graft sources or contextRows back onto the assistant message.

 Frames

 1. UI-label frame: “No grounding metadata” is just a provenance badge, not a model capability warning.
 2. RAG/provenance frame: it is supposed to mean “this answer has source snippets/paths attached, so the UI can show what
 grounded it.”
 3. Bug/serialization frame: live runs may have run.sources, but persisted/reconstructed chat messages often lose that
 linkage, so all historical messages look ungrounded even if the run had passive hydration.

 Countermoves

 Rename the badge if it is confusing: “No attached sources” would be more accurate.

 If the desired behavior is source display on old messages, the frontend needs to join reconstructed messages to their
 runId and copy over run.sources or derive sources from run.resources.passiveHydration.results.

 Next

 Wire run.sources / run.resources.passiveHydration.results into reconstructed assistant messages by runId.
