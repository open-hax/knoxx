# Frontend production capture five

The combined native suite passes **526 tests / 2,282 assertions / zero failures
or errors**, including the five native Contracts interaction tests. Compilation
covered 294 files with zero warnings. The fatal asynchronous-error guard remained
enabled. Full frontend native lint reports zero errors and zero warnings.

The first combined run found that the Contracts wrapper hid its remaining legacy
controller behind a project namespace. The migration census correctly rejected
that ambiguous ownership. The route now passes the controller, editor and chat
components explicitly into the native view wrapper. Contracts remains classified
as a legacy route until that controller is migrated. No census rule was relaxed.

Regenerating the inventory added the existing EdnEditor bridge export and its
source ownership. The inventory gate also compares the file against Git HEAD;
the correction therefore had to be committed before its final full verification.
Both obstacles and the final green run are retained in the runtime evidence logs.

The later shell review found another incorrect native-route claim: `/cms/editor/*`
had been pointed at the generic Wiki page, which does not consume the draft path
or load and save its visual contract. The shell correction restores the existing
`VisualCmsEditorPage` bridge route. Its generated inventory row is therefore
legacy again (seven legacy routes, thirteen native); this corrects the capability
claim rather than recording a completed native editor migration. No migration
ratchet is weakened. Apply the shell route correction before checking this inventory.

The follow-up production review found that restoring the route also required
restoring the named `VisualCmsEditorPage` app-bridge export and the HTML link to
the CSS extracted by that bridge. The earlier shell check reused a prebuilt
bridge, so it did not establish that the repaired route survived regeneration.
Run `pnpm -C frontend test:production-bridge` to build the app bridge and HTML in
an empty temporary directory, import the emitted module, mount its real loading
state under the draft-path route, and check the emitted layout rules and stylesheet
link. This check does not mock the bridge or editor and removes its output after
completion. Run `pnpm -C frontend build` separately for the full production build
and `pnpm -C frontend migration:check` for the regenerated ownership inventory.
The main frontend CI path runs the production bridge regression after typecheck;
it rebuilds only the app bridge and HTML, without repeating the Shadow release.
The live browser walkthrough remains a separate qualification requirement.

The current manifest check passes. The revision-relative ratchet against the
preceding shell layer still rejects the already-present `EdnEditor` export and
`UsersMembershipsSection.test.tsx` inventory additions. Those partition prerequisites
remain open; restoring the visual editor changes no ratchet rule or exemption.

The full advertised frontend build passes: both Vite bridges, HTML entry, advanced
Shadow application and Tailwind stylesheet. The advanced build covers 181 files
with zero warnings. It includes the restored frontend, native Contracts view,
and the legacy controller, context, actor, graph, transport and editor extractions
verified by the **274 executed legacy tests**, with **41 TODO cases unexecuted**.

This capture precedes the planned Data and GraphExplorer native view extractions.
All production output is frozen for the next browser walkthrough; source changes
after this capture require a later rebuilt browser verification. The repository
size gate still reports the remaining large pages and backend source warnings.
