# Documents and lake workspace lint recovery

The Documents page and its interaction tests contained 101 warnings under the existing strict frontend configuration. The cleanup keeps the existing endpoint paths, session header, request payloads, confirmations, restart messages, polling interval, and rendered controls.

The existing session-aware JSON and multipart transport now lives in the named `knoxx.frontend.lib.documents` browser boundary. The page API delegates to it. Native async/await replaces Promise chains. Ingestion reset/resume handling, lake operations, state hooks, and reusable card/table fragments are split into named functions without changing the UI's data model. The test fixture restores its confirmation stub after each test.

Two transport regressions check encoded lake identities, JSON payloads, session headers, server-text and empty-body errors, multipart files, browser-generated multipart content types, and upload refusals. The five existing document interaction tests still exercise initial loading, selection/ingestion, profile creation, a disappeared ingestion run, and confirmed deletion.

Fresh verification on 2026-09-12:

- Scoped clj-kondo: 0 errors, 0 warnings across the source and tests, down from 101 warnings.
- Combined advertised `pnpm test:cljs`: 512 tests, 2,231 assertions, 0 failures, 0 errors; 281 files, 0 compiler warnings. This includes the concurrent admin and agent-audit changes.
- Evidence: `frontend-documents-admin-audit-full.log` in the runtime evidence directory.

Production release and the live browser walkthrough are coordinated by the encompassing stack verification task. This source/test checkpoint does not claim a separate completed browser tour.
