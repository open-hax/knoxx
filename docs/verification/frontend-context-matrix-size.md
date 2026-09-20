# Context explorer and executable coverage metadata

The context sidebar now delegates filters to its existing explorer owner. Shared
context types own the unchanged input contracts. The runtime history module owns
semantic hit grouping and guarded pagination, shared with the recent-chat panel.
Static header, filter, select, navigation, entry and preview presentation lives in
the existing stylesheet compiled into app.css. Dynamic selection backgrounds,
widths, ingestion colors and resize behavior remain at their original controls.
Sidebar/explorer/types/runtime-history are below the unchanged 400-line limit.

The filter selects and scroll region now have accessible names. Four new UI
regressions cover CMS filters and creation actions, actor exclusion and grouped
session hits, file opening versus preview, and pagination while loading. The
workspace interaction suite passes all 14 tests. An isolated Tailwind build
includes the extracted selectors without writing to the served production tree.

The coverage matrix was a literal table embedded in a 772-line TypeScript file.
Its records now live in JSON data, with a typed decoder validating test kinds,
ownership, priorities, required text, implementation evidence and unique IDs.
An exact structural comparison against the evaluated original literal confirms
all **21 surfaces / 66 records / 41 planned cases** are preserved. Three new
negative decoder tests reject unknown enumerations, missing implementation
references, duplicate IDs and malformed top-level data. No planned case was
marked implemented as part of this extraction.

The combined full Vitest run passes **45 files / 271 executed tests / 0 failures**,
with **41 existing TODO cases explicitly unexecuted**. Full typecheck, isolated
legacy emission and all five source size gates pass. JSON remains coverage data;
no lint threshold, source exemption, legacy path allowance or TODO policy was
changed. Production output stayed frozen for the ongoing browser walkthrough.
