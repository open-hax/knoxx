# Recovered event-agent and forum interfaces

This slice replaces the incomplete admin rescue at `47aceb3e` with executable
ClojureScript. The event-agent editor retains the existing endpoint wire while
using the native `lib.api` boundary, so its real HTTP calls and React controls can
be exercised together in Node/JSDOM.

The runtime panel separates awaited commands, editable state and rendering.
Refused writes retain the draft and release busy controls. Job edits deep-merge
individual fields, preserving the cadence and other configuration. Existing
public command entry points remain compatibility aliases. Success notices are
set after refresh so refresh cannot immediately erase them.

The forum reader separates pagination, optional image state and rendering from
the Markdown bridge. It retains complete post text, filtering, pagination,
explicit preview loading, retries, original links and keyboard gallery controls.
Document navigation now ignores obsolete responses after its path changes.
The native image component uses `:image-index` for its ordinal: the migration
inventory deliberately reserves the ambiguous `:index` component prop as a
possible route marker. The inventory rules remain unchanged.

The mailbox EventSource now connects to `/api/actors/mailbox/events/stream`,
matching the backend's correction for message ID `changes` colliding with the
former static stream endpoint.

## Current validation

On the recovered checkout, `shadow-cljs compile test` executed **497 tests and
2,181 assertions with zero failures and zero errors**; it compiled 271 files,
including the changed test dependencies, with zero compiler warnings. The
initial run exposed the image `:index` ambiguity and reported one error; the
second full run passed after the explicit prop rename. Both runs used the
backend uncaught-error preload, and the emitted summaries were inspected rather
than trusting the compiler's process status.

The five additional interaction tests exercise actual Helix rendering and the
native HTTP boundary: exact save bodies, failure retention, permission-disabled
controls, encoded command paths, page size/filtering, image retry URLs and
keyboard gallery wrapping. Existing mailbox transport tests cover the new
EventSource path. Scoped repository clj-kondo checks report zero errors/warnings
for all source and tests in this slice.

Production browser acceptance and full frontend lint are separate required gates.
The first restored full lint inventory had 3 errors and 573 warnings; this slice
does not suppress or raise any lint threshold. The separate Agents-page
refactor and remaining inherited warning cleanup are still in progress.

## Sandbox installation obstacle

Running `pnpm install` from the frontend initially exited successfully while
installing only the three packages selected by Foresight's current generated
workspace. It did not create the frontend's `node_modules`. The independently
buildable child installation used `pnpm install --ignore-workspace` with the
same shared pnpm store. This restored the actual frontend toolchain without
creating a second package store or changing the child manifest's dependencies.
The cold Maven fetch used the recovered shared cache and then selected
ClojureScript 1.12.145 and Shadow 3.4.11 for native async compilation.
