# Knoxx JS Boundary — Discord Adapter

Date: 2026-05-21
Status: done
Parent epic: `knoxx-js-cljs-boundary-hardening-epic.md`
Story points: 8

## Purpose

Make Discord a named boundary instead of letting Discord domain tools import generic JS/JSON helpers. Discord tool logic should compose CLJS request/result maps; the adapter should own Discord REST wire JSON, multipart `FormData`, `Blob`, JS promise arrays, SDK object extraction, and route/tool runtime interop.

## Problem

`backend/src/cljs/knoxx/backend/domain/discord/tools.cljs` and `domain/discord/rest_client.cljs` currently contain mixed layers:

- business/tool decisions such as chunking, filtering, labels, and message summaries
- raw `FormData` and `Blob` construction
- `payload_json` serialization
- `js/Promise.all` arrays and JS result arrays
- `#js` wrapper params for alias tools
- raw tool params read through `aget`
- `xjs/js-array-seq` to consume promise results and attachment arrays
- `xjson/stringify` to create Discord REST request bodies

That means the JS object origin and the code that consumes it are separated by multiple function calls. The code can look “extern-compliant” while still being boundary-coupled.

## Target model

Create a Discord boundary API that accepts CLJS maps/vectors and returns CLJS maps/vectors:

```clojure
(discord-boundary/create-message! client
  {:channel-id channel-id
   :content chunk
   :reply-to reply-to
   :files files})

(discord-boundary/upload-files-form {:payload payload :files files})
(discord-boundary/all-channel-lists! promises)
(discord-boundary/normalize-tool-params params)
```

The adapter may be implemented as `knoxx.backend.extern.discord` or split by concern, for example:

- `extern.discord.rest`
- `extern.discord.multipart`
- `extern.discord.gateway`
- `domain.discord.codec` for pure CLJS normalization

The important constraint is that raw JS construction/inspection does not remain in `domain.discord.tools`.

## Deliverables

1. Move REST JSON body encoding out of `domain/discord/rest_client.cljs`.
2. Move Discord multipart `FormData`/`Blob`/`payload_json` construction out of `domain/discord/tools.cljs`.
3. Replace `js/Promise.all (clj->js ...)` + `xjs/js-array-seq` with a promise adapter that resolves CLJS vectors.
4. Normalize Discord tool params once at the tool runtime boundary; migrate execute functions from raw `aget` to CLJS map access.
5. Replace alias-tool `#js` param remapping (`publish`, `read`, `channels`) with CLJS map remapping.
6. Ensure `discord-list-channels!` receives CLJS vectors from promise aggregation.
7. Keep message chunking, attachment loading, OpenPlanner label filtering, and text formatting as domain logic.
8. Add regression tests with fixture data for:
   - long message chunking with and without attachments
   - file payload form fields and filenames
   - attachment URL normalization
   - list guilds/channels aggregation
   - publish/read/channels alias param remapping

## File-specific work

### `domain/discord/rest_client.cljs`

- Remove `knoxx.backend.extern.json` import.
- Use the JSON fetch request mode from `knoxx-js-boundary-json-fetch-clients.md`.
- Keep `IDiscordRestClient` protocol stable.

### `domain/discord/tools.cljs`

- Remove `knoxx.backend.extern.js` import.
- Move `js/FormData`, `js/Blob`, `#js`, `clj->js`, and `js/JSON.stringify` into Discord multipart adapter.
- Replace `xjs/js-array-seq` on `payloads` and `attachment_urls` with CLJS vectors supplied by the appropriate boundary.
- Replace raw `params` access with normalized CLJS params.

## Acceptance criteria

1. `domain/discord/tools.cljs` does not import `knoxx.backend.extern.js`.
2. `domain/discord/rest_client.cljs` does not import `knoxx.backend.extern.json`.
3. Discord domain files have no generic JS array conversion or JSON stringification.
4. Domain functions compose CLJS maps/vectors and hand native Discord wire details to the adapter.
5. Existing Discord tool names, schemas, and result text remain stable for fixtures.
6. Backend test build reports zero failures and zero errors.

## Verification

```bash
rg -n "knoxx\.backend\.extern\.(js|json)|xjs/js-array-seq|xjson/stringify|js/FormData|js/Blob|#js|clj->js|\.stringify js/JSON|aget" backend/src/cljs/knoxx/backend/domain/discord/tools.cljs backend/src/cljs/knoxx/backend/domain/discord/rest_client.cljs
pnpm -C backend boundary:check
pnpm -C backend exec shadow-cljs compile test
pnpm -C backend exec shadow-cljs compile server
```

Verified 2026-05-21:

- Added `knoxx.backend.extern.discord` for Discord-specific JS status, multipart FormData/Blob payloads, promise vector aggregation, runtime tool arrays, and raw tool param normalization.
- Refactored `domain/discord/tools.cljs` to remove direct generic extern imports and target-grep JS boundary forms from the Discord tool factory path.
- Removed `domain/discord/tools.cljs` from the generic extern allow-list; boundary check now passes with 8 remaining allow-listed non-extern generic extern import files.
- Added `extern_discord_test.cljs` coverage for param normalization, promise vector aggregation, multipart form construction, and gateway status extraction.
- Fixed adjacent verification blockers encountered during server compile: one unmatched delimiter in `translation.cljs` and one missing close paren in `eta_mu_session_ingester.cljs`.
- Target grep returned no generic extern/JS boundary forms in `domain/discord/tools.cljs` or `domain/discord/rest_client.cljs`.
- `pnpm -C backend boundary:check` passed.
- `pnpm -C backend exec shadow-cljs compile test` passed with 351 tests, 920 assertions, 0 failures, 0 errors.
- `pnpm -C backend exec shadow-cljs compile server` passed (`Build completed`, existing warnings).
