# Agent workbench responsibilities

The former `pages/agents.cljs` held 1,275 lines, including a 387-line component.
The page now composes the contract library, editor, runtime projection and chat.
Its supporting namespaces own distinct responsibilities:

| Namespace | Responsibility |
| --- | --- |
| `agents.model` | Contract identity, roles, raw EDN and structured editor projections |
| `agents.runtime-model` | Trigger and pipeline target projections into schedule rows |
| `agents.fields` / `agents.editor` | Accessible structured fields and raw EDN editing |
| `agents.runtime-view` | Contract selection, schedule facts and permitted actions |
| `agents.commands` | Awaited service commands with explicit clients and visible refusals |
| `agents.controller` | React state, current capabilities, selection and chat context |

The unreferenced legacy sidebar and duplicate unused agent-list rendering were
removed. The workbench keeps its existing layout, contract API transport,
notices, role references and runtime command endpoints. The command layer checks
save/runtime capabilities before calling its clients, retains draft text on
failure and clears the corresponding busy indicator in `finally`.

Contract loading also checks that its selected identity is still current before
installing a response. A response from a previously selected contract cannot
replace a newer selection or clear its loading indicator. The chat effects
retain the existing semantic dependencies: adding the bridge controller object
to those dependency arrays would risk repeatedly pinning context when the
bridge recreates its wrapper.

The regression tests cover stale response ordering, refused save retention,
capability-denied service calls, invalid raw EDN, runtime failure cleanup,
pipeline target projections, visible item counts, disabled run actions and
role removal. Current source and test namespaces pass the unchanged repository
clj-kondo rules with zero errors and warnings. Full-suite and production build
results are recorded after the coordinated frontend run; no historical browser
screenshot is evidence for this reconstruction.

The coordinated full frontend suite passed **507 tests / 2,207 assertions**, with
zero failures/errors and zero compiler warnings (279 files; 21.83 seconds).
The advertised `test:cljs` command now uses the existing shared CI runner, which
rejects failed/missing summaries and uncaught errors even when Shadow exits zero.

The production release initially rejected unannotated native identity properties
in the extracted controller. Explicit `^js` annotations on its actual native
authentication, location and chat handles restored advanced compilation. The
app release then completed with **173 files and zero warnings** (26.46 seconds).
The warning-as-error setting remains unchanged.
