# Historical Documents and lake workspace lint recovery

This record describes the cumulative [PR #305 source checkpoint](https://github.com/open-hax/knoxx/tree/1d3b207ab2e5bfc6b2fa149836b855d458a9c997), not the isolated build/test layer in PR #327. At that layer, `knoxx.frontend.pages.documents.api` still owns `session-request`, `upload-documents`, and the endpoint functions, and requires `knoxx.frontend.lib.api`. The Documents implementation and tests below belong to later slices of the stack; their historical results do not verify PR #327.

The Documents page and its interaction tests contained 101 warnings under the existing strict frontend configuration. The cleanup keeps the existing endpoint paths, session header, request payloads, confirmations, restart messages, polling interval, and rendered controls.

In the cumulative checkpoint, session-aware JSON and multipart transport lives in the named `knoxx.frontend.lib.documents` browser boundary, and the page API delegates to it. Native async/await replaces Promise chains. Ingestion reset/resume handling, lake operations, state hooks, and reusable card/table fragments are split into named functions without changing the UI's data model. The test fixture restores its confirmation stub after each test.

Two transport regressions check encoded lake identities, JSON payloads, session headers, server-text and empty-body errors, multipart files, browser-generated multipart content types, and upload refusals. The five existing document interaction tests still exercise initial loading, selection/ingestion, profile creation, a disappeared ingestion run, and confirmed deletion.

Historical verification recorded on 2026-09-12 for that implementation:

- Scoped clj-kondo: 0 errors, 0 warnings across the source and tests, down from 101 warnings.
- Combined advertised `pnpm test:cljs`: 512 tests, 2,231 assertions, 0 failures, 0 errors; 281 files, 0 compiler warnings. This includes the concurrent admin and agent-audit changes.
- Evidence: `frontend-documents-admin-audit-full.log` in the runtime evidence directory.

Production release and the live browser walkthrough are coordinated by the encompassing stack verification task. This source/test checkpoint does not claim a separate completed browser tour.
