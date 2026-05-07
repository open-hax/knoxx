# Broadcast Studio Contract UI + Audio Sub-Agent Orchestration

Status: phase-1 implemented
Owner: Knoxx / Broadcast Studio
Created: 2026-05-06
Frozen prerequisite: `pi-fork-tax-broadcast-audio-20260506T231041Z`

## Problem

Broadcast Studio now has a proven direct audio-hearing path for `gemma4:e4b`, but its user-facing workflow is still only halfway contract-native. The page can launch fixed audio analysis buttons, yet the frontend still knows too much about which actions exist and how they should appear. The next step is to make Broadcast Studio a pilot for contract-bound UI elements:

- page actor chooses the primary agent;
- contracts expose user-facing actions/buttons;
- primary agent delegates audio perception to specialized sub-agents;
- graph mutations, especially label writes, happen through explicit tools and policy gates.

## Goals

1. Make the Broadcast Studio page bind to a primary actor/agent contract instead of hardcoded action definitions.
2. Let agent contracts dynamically populate page actions such as buttons, menu items, and quick prompts.
3. Introduce a `gemma4:31b` Broadcast Studio primary agent that can converse about the current audio and call fast `gemma4:e4b` audio sub-agents.
4. Preserve the stable `gemma4:e4b` direct audio route by keeping audio sub-agents lean and tool-free unless a specific variant requires tools.
5. Allow the primary agent to propose and then apply OpenPlanner graph labels through explicit label tools after user confirmation.
6. Establish this as a reusable pattern for user-facing UI surfaces bound to actors/contracts.

## Non-goals

- Do not reintroduce frontend-authored task prompts for audio analysis.
- Do not expose thinking-level controls in the Broadcast Studio UI.
- Do not make `e4b` audio sub-agents carry the full primary-agent tool/schema surface.
- Do not silently write labels or metadata without a contract/policy path and clear user intent.
- Do not depend on filename/title/path metadata as evidence of audio hearing.

## Current proven foundation

The following behavior is already proven and should be treated as a prerequisite, not re-litigated in this spec:

- `gemma4:e4b` hears audio via llama.cpp `/v1/chat/completions` with OpenAI-compatible chat content.
- Knoxx materializes relative `/api/studio/stream?...` URLs into base64 audio bytes before eta-mu.
- Empty direct-start requests can fall back to contract `:prompts :task`.
- `{ctx.name}` / `{ctx.email}` task prompt templating works from auth context.
- Broadcast audio attachments use generic model-facing markers like `attached-audio.mp3`.
- Long Cathedral MP3 direct-start runs complete after llama.cpp runtime tuning.

## Target architecture

```text
BroadcastStudioPage.tsx
  -> resolves page actor: broadcast_studio
  -> loads contract UI actions for actor/page surface
  -> embeds chat bound to primary agent
  -> sends current media attachment + selected action id

broadcast_studio actor
  -> default agent: broadcast_studio_primary

broadcast_studio_primary agent (gemma4:31b)
  -> conversational producer/librarian
  -> tools:
     - call sub-agent: transcribe audio
     - call sub-agent: describe audio
     - call sub-agent: suggest labels
     - read current graph labels
     - apply graph labels after confirmation
     - optionally write transcript/description metadata later

broadcast_studio_audio_* sub-agents (gemma4:e4b)
  -> direct llama.cpp provider
  -> receive only task prompt + current audio bytes
  -> return compact structured text/JSON
```

## Contract extensions

### 1. Contract UI action declaration

Add a small UI action schema to agent contracts or a dedicated page-surface contract. Proposed shape:

```edn
:ui/actions
[{:id "audio.transcribe"
  :label "Transcribe"
  :kind :button
  :surface :broadcast-studio/now-playing
  :icon "captions"
  :intent :agent.run
  :agent/contract "broadcast_studio_audio_transcriber"
  :agent/actor "broadcast_studio_audio_task"
  :media/from :page.current-audio
  :requires [:media/audio]
  :mode :direct
  :confirm? false}

 {:id "audio.labels.suggest"
  :label "Suggest labels"
  :kind :button
  :surface :broadcast-studio/now-playing
  :intent :agent.run
  :agent/contract "broadcast_studio_audio_labeler"
  :agent/actor "broadcast_studio_audio_task"
  :media/from :page.current-audio
  :mode :direct
  :confirm? false}

 {:id "labels.apply"
  :label "Apply selected labels"
  :kind :button
  :surface :broadcast-studio/label-suggestions
  :intent :tool.call
  :tool/id "openplanner.graph.labels.apply"
  :requires [:selection/labels :media/current]
  :confirm? true}]
```

Rules:

- Frontend renders `:label`, placement, enabled/disabled state, and confirmation affordance.
- Frontend does not synthesize model prompts.
- Backend resolves action id to contract/tool behavior.
- `:media/from` controls what page state becomes content parts.
- `:confirm? true` means the frontend must show/record an explicit user confirmation before mutation.

### 2. Actor page binding

Add a page binding contract or actor metadata:

```edn
{:actor/id "broadcast_studio"
 :actor/kind :page
 :actor/default-agent "broadcast_studio_primary"
 :ui/surfaces [:broadcast-studio/now-playing
               :broadcast-studio/queue
               :broadcast-studio/library
               :broadcast-studio/chat]
 :ui/default-media :page.current-audio}
```

The page loads this binding and then asks the backend for UI actions available for:

- actor id;
- route/page id;
- user auth context;
- current media state;
- policy permissions.

### 3. Sub-agent invocation contract

The primary agent needs a narrow tool for sub-agent calls. Proposed logical tool:

```edn
{:tool/id "agent.sub_agent.invoke"
 :description "Invoke a contract-bound sub-agent with selected media and compact task input."
 :parameters
 {:contract_id :string
  :actor_id :string
  :message :string
  :content_parts :array
  :mode [:enum "direct" "rag"]}}
```

Policy constraints:

- Primary Broadcast Studio agent may invoke only whitelisted sub-agent contracts.
- Sub-agent calls inherit current user/org auth context.
- Sub-agent call logs must include parent run id and child run id.
- Sub-agent outputs are returned to the primary agent as tool results and optionally shown in the UI as expandable evidence.

## Agent roster

### Primary agent

`broadcast_studio_primary`

- Model: `gemma4:31b`
- Actor: `broadcast_studio`
- Role: producer/librarian/label curator
- Tools:
  - sub-agent invocation for allowed audio agents;
  - OpenPlanner label read/apply tools;
  - current studio state read;
  - maybe media-library read only.
- Prompt intent:
  - help the user understand the current track;
  - ask clarifying questions;
  - call fast audio sub-agents when perception is needed;
  - propose labels/transcripts/descriptions;
  - apply graph labels only after user asks or confirms.

### Existing lean audio sub-agents

- `broadcast_studio_audio_transcriber`
- `broadcast_studio_audio_describer`
- `broadcast_studio_audio_labeler`

These remain low-context, direct, no-tool `gemma4:e4b` contracts.

### Tool-capable variants

Add variants only when there is a specific reason:

- `broadcast_studio_audio_labeler_graph_context`
  - can read existing label taxonomy but not write;
  - useful for suggestions aligned to current vocabulary.
- `broadcast_studio_audio_segmenter`
  - emits rough sections/time ranges if model/audio support is good enough.
- `broadcast_studio_audio_broadcast_safety`
  - checks intelligible speech/explicit content/production risk.
- `broadcast_studio_audio_metadata_writer`
  - probably not direct `e4b`; use primary agent/tool path with confirmation.

## Backend API additions

### GET contract UI actions

```http
GET /api/contracts/ui-actions?actor=broadcast_studio&surface=broadcast-studio/now-playing
```

Returns resolved, policy-filtered actions:

```json
{
  "actor_id": "broadcast_studio",
  "surface": "broadcast-studio/now-playing",
  "actions": [
    {
      "id": "audio.describe",
      "label": "Describe",
      "kind": "button",
      "enabled": true,
      "intent": "agent.run",
      "requires": ["media/audio"]
    }
  ]
}
```

### POST execute UI action

```http
POST /api/contracts/ui-actions/execute
```

Request:

```json
{
  "actor_id": "broadcast_studio",
  "action_id": "audio.describe",
  "surface": "broadcast-studio/now-playing",
  "page_state": {
    "current_audio_url": "/api/studio/stream?path=...",
    "mime_type": "audio/mpeg"
  },
  "confirmation": null
}
```

Backend resolves the action to either:

- direct agent start;
- tool call;
- primary-agent message with attached media;
- confirmation challenge.

## Frontend changes

1. Replace hardcoded `BROADCAST_STUDIO_AGENT_ACTIONS` with actions loaded from backend contracts.
2. Bind embedded chat default actor/contract to `broadcast_studio` / `broadcast_studio_primary`.
3. Keep current media attachment creation in frontend, but only as page-state/media binding, not prompt construction.
4. Render action buttons by `surface`:
   - now playing;
   - label suggestions;
   - queue item;
   - library item later.
5. Show sub-agent results as chat/evidence cards:
   - transcript;
   - description;
   - suggested labels;
   - child run id;
   - model/provider used.
6. For label suggestions, render chips with two paths:
   - click chip to stage/select;
   - explicit “Apply selected labels” action calls graph write.

## OpenPlanner graph label flow

Read path:

- primary agent/tool can list current labels for the audio node;
- labeler variant can optionally receive existing label vocabulary.

Suggest path:

- `e4b` labeler returns suggested labels grouped by content/mood/function/production.
- UI parses suggestions into staged chips.
- Primary agent can discuss/merge/rename labels.

Write path:

- explicit tool: `openplanner.graph.labels.apply`;
- input: target node/media identity + label names/ids;
- result: applied labels + created labels + skipped duplicates;
- all writes logged as run events.

## Security and policy

- UI action resolution must be policy-filtered by auth context.
- Write actions require `agent.chat.use` plus graph label write permission.
- Sub-agent invocation should enforce an allowlist from the primary agent contract.
- Media source must remain inside allowed workspace/media roots.
- Model-facing prompts must not include local paths/titles unless the user explicitly asks for path-level management.

## Observability

Every UI action execution should log:

- action id;
- actor id;
- contract id/tool id;
- parent run id;
- child run id if sub-agent;
- media materialization summary:
  - media part count;
  - omitted count;
  - content type;
  - generic attachment marker;
- graph mutations if any.

## Phased plan

### Phase 1 — Contract/UI action schema

Implemented 2026-05-06:

- Added validator schema support for `:ui/actions` on actor and agent contracts.
- Added `contracts.resolve/ui-actions-for-actor` for actor/default-agent action resolution.
- Added `GET /api/contracts/ui-actions?actor=...&surface=...`.
- Added Broadcast Studio now-playing actions to `contracts/actors/broadcast_studio.edn`.
- Action responses include resolved agent contract, actor, and model.

Verification:

- `GET /api/contracts/ui-actions?actor=broadcast_studio&surface=broadcast-studio/now-playing` returns three enabled actions: transcribe, describe, suggest labels.
- `clj-kondo` on touched CLJS files reports only pre-existing route/contract warnings.
- `shadow-cljs compile server` exits 0 with existing infer warnings.

### Phase 2 — Broadcast Studio action rendering

Partially implemented 2026-05-06:

- Broadcast Studio now loads now-playing actions from `/api/contracts/ui-actions`.
- The hardcoded frontend action list was removed.
- Existing working direct audio payload shape is preserved.
- Frontend still sends blank user text, media content part, `direct`, and `omitSystemPrompt`.

Remaining verification:

- Browser-click a contract-rendered action and confirm backend logs `media_parts_count=1`, `omitted_count=0`, `content_type=multipart`.

### Phase 3 — Primary Broadcast Studio agent

- Add `broadcast_studio_primary` contract using `gemma4:31b`.
- Bind `broadcast_studio` actor default agent to primary.
- Give primary agent read-only studio/media/label tools first.

Verification:

- embedded chat starts as primary agent;
- primary can discuss current track metadata/state without writing.

### Phase 4 — Sub-agent invocation tool

- Implement narrow `agent.sub_agent.invoke` tool/action.
- Restrict primary agent to allowed audio sub-agent contracts.
- Return child run output as structured tool result and chat evidence.

Verification:

- primary agent calls transcriber/describer/labeler on current audio;
- child runs log `media_parts_count=1`, `omitted_count=0`;
- primary summarizes outputs without rerouting audio through `31b` if unnecessary.

### Phase 5 — Confirmed graph label writes

- Add label apply tool or expose existing OpenPlanner label endpoint as a contract tool.
- Add confirmation UX for staged labels.
- Let primary agent propose, refine, and apply labels after user confirmation.

Verification:

- suggested labels can be staged;
- confirmed apply writes OpenPlanner graph labels;
- labels immediately appear in Broadcast Studio and Labels page.

## Acceptance criteria

- Broadcast Studio buttons are populated from contract-resolved UI actions.
- The page primary agent is `broadcast_studio_primary` by default.
- The primary agent can invoke `e4b` audio sub-agents for transcript/description/labels.
- Sub-agent audio calls preserve the proven direct, generic, materialized audio path.
- Label writes happen only via explicit graph label tool/action.
- Labels written by the agent appear in the same OpenPlanner graph label system used by `LabelsPage.tsx`.
- All runs are traceable from UI action -> parent run -> child run/tool events.

## Open questions

1. Should UI action declarations live directly in agent contracts, actor contracts, or a separate page-surface contract?
2. Should the primary agent call sub-agents through a general tool, a backend action namespace, or eta-mu native sub-agent support?
3. Should label application require a modal confirmation every time, or is chat confirmation enough when the user says “apply those”?
4. Should transcripts/descriptions be persisted as graph node properties, separate document nodes, or only chat/session artifacts initially?
5. Should `gemma4:31b` ever receive the raw audio directly, or should it always delegate audio perception to `e4b` unless explicitly requested?

## Recommended first implementation slice

Start with Phase 1 and Phase 2 only:

- add `:ui/actions` schema/resolver;
- move existing Broadcast Studio buttons into contract declarations;
- render buttons dynamically;
- keep behavior identical to the frozen working path.

Only after the UI action contract is stable should we add the primary `31b` agent and sub-agent invocation tool.
