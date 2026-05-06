# Π Fork Tax — Broadcast Studio audio hearing freeze

Timestamp: 20260506T231041Z
Repo: /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx
Branch: feat/discord-attachments
Base HEAD: d7bd14ac
Tag target: pi-fork-tax-broadcast-audio-20260506T231041Z

## Preserved working behavior
- Broadcast Studio direct-start audio jobs route to contract/model/actor without frontend task prompts.
- Empty direct-start messages fall back to backend-resolved `:prompts :task`.
- Task prompt templates render `{ctx.name}` / `{ctx.email}` from auth context.
- Relative `/api/studio/stream?...` audio URLs are materialized as actual bytes before eta-mu.
- Model-facing audio metadata uses generic `attached-audio.mp3` and does not leak local titles/paths.
- `gemma4:e4b` direct llama.cpp audio route is contract/provider driven.

## Verification evidence
- `clj-kondo --lint backend/src/cljs/knoxx/backend/agents/turn.cljs` returned only pre-existing unresolved-var/arity findings.
- `npx shadow-cljs compile server` exited 0 with existing infer warnings.
- Template smoke run `fe24608e-a572-40b6-a76a-d8f850cdbc1f` rendered `Pi` / `pi@open-hax.local`.
- Cathedral relative URL runs logged `media_parts_count=1`, `omitted_count=0`, `content_type=multipart`, generic `attached-audio.mp3`.
- Long Cathedral runs completed with audio-derived production notes after llama.cpp service tuning: `4098f31d-aefb-4f80-96f0-755bc93b5ec4`, `063e8999-3e6c-4b32-be59-1ed84252626b`.

## Owned paths in this commit
See `.ημ/Π_MANIFEST.sha256`.

## Concurrent dirt intentionally not absorbed
This rank-3 submodule has unrelated live changes around discord/event-agent/sub-agent/voice/music/admin routes and generated creative assets. They are not staged for this fork tax.

## Parent workspace dependency
The companion parent repo snapshot preserves `services/llamacpp-stack` compose/script defaults for 16k context and disabled prompt cache.
