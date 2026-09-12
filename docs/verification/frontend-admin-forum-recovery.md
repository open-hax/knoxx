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

## Complete admin lint gate

The remaining admin rendering helpers now use qualified Helix names, sorted
imports and short components for source fields, event-kind inputs and runtime
footers. This preserves the existing input keys and public component props.
The entire admin source/test directory passes strict clj-kondo with zero errors
and zero warnings. Its focused runtime gate passed **17 tests / 66 assertions**
with zero failures, errors or compiler warnings; the shared fatal-error guard
and test-summary validator were both enabled.

The isolated gate writes to `dist-verification`, preserving the production
browser files. Inspection of Shadow's installed test selector revealed that
`:ns-regex` is ignored: its supported key is `:ns-regexp`, with a string value.
The checked-in test configuration now uses that supported key and the same
frontend namespace scope. CLI overrides must target the build map directly,
for example `{:output-to "dist-verification/admin-lint-tests.cjs"
:ns-regexp "^knoxx.frontend.admin.*-test$"}`. The full exposed frontend lint
gate still reports inherited warnings outside this completed admin slice.

## Initial load failure and retry

Self-review found that the panel treated missing data as ongoing loading even
after the initial GET had failed. That hid the error and offered no way to try
again. A new interaction regression reproduced the stuck loading screen before
the fix. The panel now distinguishes a pending request from unavailable data,
shows the read failure and provides an explicit retry. Write controls appear
only after a successful read supplies the control data.

The regression's first run exposed an unhandled testing-library rejection while
Shadow still printed zero failures/errors. The uncaught-error preload emitted
its fatal marker and the shared summary validator correctly returned exit 1.
After the UI correction, all **18 admin tests / 69 assertions** passed with zero
failures, errors and compiler warnings, and strict admin source/test lint stayed
at zero errors/warnings. Browser acceptance remains owned by the full stack tour.
