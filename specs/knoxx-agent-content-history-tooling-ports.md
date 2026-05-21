# Knoxx Agent Content, History, and Tooling Ports

Date: 2026-05-21
Status: done
Parent epic: `knoxx-agent-service-protocol-split-epic.md`
Source report: `docs/notes/architecture/agent-service-protocol-split.md`
Story points: 5

## Purpose

Move content/media conversion, transcript/history rehydration, and tool catalog/policy resolution out of `session.cljs` and `turn.cljs` into focused ports and pure codecs.

## Problem

Session and turn construction currently braid together:

- stored message rehydration
- transcript building
- context pruning
- media materialization and base64 fetching
- eta-mu attachment conversion
- visible tool signatures
- allowed tool/custom tool selection
- tool policy and auth-derived tool availability

These are distinct behaviors with different test fixtures and failure modes.

## Target ports/codecs

```clojure
(defprotocol IMessageHistory
  (load-history [history request])
  (append-message! [history message]))

(defprotocol ITranscriptCodec
  (stored->provider-messages [codec stored-messages context-policy])
  (provider->stored-message [codec provider-message]))

(defprotocol IContentCodec
  (content-parts->provider [codec content-parts])
  (provider->content-parts [codec provider-content]))

(defprotocol IMediaMaterializer
  (materialize-media! [materializer content-part auth-context]))

(defprotocol IToolCatalog
  (available-tools [catalog context]))

(defprotocol IToolPolicyResolver
  (allowed-tools [resolver auth-context agent-spec requested-tools]))
```

Exact names may change, but history/content/tool concerns should be independently testable.

## Deliverables

1. Focused namespace for transcript/history behavior using existing `IMessageSource` where appropriate.
2. Focused namespace for content/media conversion and materialization.
3. Focused namespace for tool catalog and tool policy resolution.
4. Pure tests for content part conversion, provider message conversion, and context pruning behavior.
5. Tests for tool visibility/signature stability under representative auth and agent-spec fixtures.
6. Transitional delegations from old session/turn helper names.

## Non-goals

1. Replacing existing tool implementations.
2. Rewriting all context-policy logic beyond extraction.
3. Changing provider-visible content format except through a compatibility-preserving codec.
4. Introducing cross-domain imports between unrelated tool slices.

## Implementation notes

- Prefer domain slices such as `tools.discord`, `tools.music`, and `tools.openplanner`; shared helpers belong in `tools.shared` or a clear agent tooling namespace.
- Keep media loading/materialization effectful; keep content shape conversion pure.
- Existing `IMessageSource` is a good precedent and should not be duplicated unnecessarily.

## Acceptance criteria

1. History/transcript, content/media, and tooling concerns are separable in tests.
2. `session.cljs` and `turn.cljs` no longer need to know low-level media or tool signature construction details directly, or delegate them through named functions.
3. Existing provider message payloads are preserved for representative text and multimodal fixtures.
4. Existing allowed-tool behavior is preserved for representative auth contexts.

## Implementation result

Completed on 2026-05-21.

Touched files:

- `backend/src/cljs/knoxx/backend/infra/agent/history.cljs`
- `backend/src/cljs/knoxx/backend/infra/agent/content_codec.cljs`
- `backend/src/cljs/knoxx/backend/infra/agent/tool_catalog.cljs`
- `backend/src/cljs/knoxx/backend/infra/agent/session.cljs`
- `backend/test/cljs/knoxx/backend/agents/content_history_tooling_ports_test.cljs`

Notes:

- Added history/transcript ports: `IMessageHistory` and `ITranscriptCodec`, with message-source backed loading, context pruning, and session-manager rehydration.
- Added content/media ports: `IContentCodec` and `IMediaMaterializer`, with compatibility-preserving media materialization for data URLs, raw base64, and remote URLs.
- Added tool ports: `IToolCatalog` and `IToolPolicyResolver`, with allowed-tool resolution, tool-auth-context projection, visible session signatures, built-in tool resolution, and custom-tool lookup behind a focused namespace.
- `session.cljs` now delegates context pruning, rehydration, media materialization, visible session signatures, allowed-tool resolution, and built-in/custom tool lookup to the new port/codecs while preserving the existing public helper names.
- Added tests for pruning, history rehydration through `IMessageSource`, media materialization, and tool visibility/signature stability with fake resolvers.

## Verification

```bash
pnpm -C backend exec shadow-cljs compile test
pnpm -C backend exec shadow-cljs compile server
```

Results:

- `compile test`: exit 0; 336 tests, 859 assertions, 0 failures, 0 errors; 220 existing warnings.
- `compile server`: exit 0; build completed; 301 existing warnings.
- `git diff --check`: passed for touched tracked files plus no-index checks for new history/content/tool/test namespaces.
