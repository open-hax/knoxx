# Administration and graph label ownership

The legacy administration layout now keeps routing and shared context in 375
lines. Its existing summary, organization, role and data-lake sections own their
respective subpage actions and composition (73, 132, 241 and 352 lines). The
organization capability check, payload normalization, refresh ordering,
selection and failure notices remain unchanged. Three new rendered directory
regressions cover successful creation, failed creation and missing permission.
The existing actor-directory cases also pass: **6 focused tests total**.

Graph label metadata now joins the existing graph wire declarations in
`lib/types.ts`; the Labels page retains its exact requests and rendering in 386
lines. The shared type file remains below the limit at 292 lines.

All seven production paths pass the unchanged size gate with zero errors and
warnings. Full typecheck and full isolated legacy emission pass. The combined
full Vitest run passes **45 files / 235 executed tests**, with **41 existing
TODO cases explicitly unexecuted**. This run also includes the separately
documented chat workspace and administration API extractions.

No new TypeScript files, removed routes, expanded bridge exports or threshold
changes were used. Production artifacts remained frozen for the browser tour.
