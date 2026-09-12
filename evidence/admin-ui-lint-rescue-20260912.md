# Unverified admin and document-view lint recovery

Status: INCOMPLETE, UNVERIFIED RESCUE ONLY. Do not merge this branch or describe its files as passing current gates.

Base: open-hax/knoxx commit 377892dcf4908c31b74ccc9349db04356a5d9181, tree b48631ff09563175a961d375e241bed3eb6c0eca. PR305 is deliberately unchanged by this rescue. The five code/test entries below are preserved separately from the published checkpoint.

The executor workspace disappeared during a bounded frontend lint refactor. The executor then returned HTTP409 environment_offline, confirmed by the coordinator. No build, test, lint, browser or screenshot result verifies this recovered tree. Before disappearance, the draft production slice had a scoped zero-error/zero-warning lint result, but those draft bytes and that local log were lost. Reconstruction is not byte-identical proof. The two test sources were preserved from surviving transient copies before the execution service disconnected; they were not compiled.

## Saved files

| Path | Blob | Provenance |
| --- | --- | --- |
| frontend/src/cljs/knoxx/frontend/admin/event_agent_editor.cljs | 67bb434bc0abab4663b197ee6d170e9da36fefde | Reconstructed from published base plus retained change description |
| frontend/src/cljs/knoxx/frontend/api/event_agents.cljs | e22a1509d90c7e3da7f87594ff91aec5e6362a0e | Reconstructed native client preserving existing endpoint wire |
| frontend/src/cljs/knoxx/frontend/admin/event_agents.cljs | a92eb8749e719dbef0e9f93af40d6d9dfa853b17 | Reconstructed schedule-card split; unparsed and uncompiled |
| frontend/test/cljs/knoxx/frontend/admin/event_agent_panel_interaction_test.cljs | 7dbdfe1f814c365dc536eed0b9ae03c258f4730b | Surviving draft test bytes; not compiled |
| frontend/test/cljs/knoxx/frontend/pages/source_doc/forum_view_interaction_test.cljs | 6024c80d7bc3e67891d69df48140eb9d4e381522 | Surviving draft test bytes, including manual delimiter repair; not compiled |

The forum test requires a new forum-view namespace that is NOT included in this rescue. The original panel and document-view implementation remain at the base version. This is intentionally an incomplete checkpoint, not a runnable branch.

## Scope and contracts to retain

The coordinator assigned event_agent_editor.cljs, event_agents.cljs, event_agents_panel.cljs and pages/source_doc/view.cljs. Split named responsibilities below the existing 30-line warning threshold; do not suppress the threshold. Tidy namespace/docs/refer warnings in touched files. Native event-agent API replacement was additionally authorized because the existing bridge prevented meaningful Node/JSDOM panel interaction tests. Do not broaden unrelated API clients.

The editor splits job-form into source, trigger, and agent fields, preserving all update wire keys. Every field receives an explicit accessible label. Qualified Helix macros replace refer usage. Input type binding is renamed to input-type to avoid shadowing core/type.

The schedule component splits contract label, heading, badges, runtime status, details and card; job-field-patch preserves enabled, trigger-kind, source-kind, source-mode, cadence, event-kinds, role, model, thinking-level, description and system-prompt update shapes.

The native API retains:
- GET and PUT /api/admin/config/discord; PUT body {discordBotToken: token}.
- GET and PUT /api/admin/config/events; PUT receives the control object directly.
- POST /api/admin/config/events/jobs/:encoded-id/run and /api/admin/triggers/:encoded-id/fire.
- POST /api/admin/config/events/dispatch with the event object directly.
- POST /api/admin/config/events/runtime/stop, start and reset.
- GET /api/admin/tools; preserve existing normalization defaults for id, label, description, and first-present riskLevel/risk-level/risk_level aliases.

## Remaining panel reconstruction

Create frontend/src/cljs/knoxx/frontend/admin/event_agent_commands.cljs and move the first command/pure helper group from published event_agents_panel.cljs there. Keep the old panel public names as documented compatibility aliases:
load-data, update-job, handle-save-token, parse-control-for-save, update-json-draft, handle-save-control, handle-run-job, handle-dispatch-event, handle-stop-runtime, handle-start-runtime, handle-reset-runtime.

Use the repository's native async conventions instead of unobserved promise chains. Preserve exact argument lists and notice strings from the published base. A private async perform! takes busy setter, idle value, error setter, notice setter and async operation; it sets busy true, clears error/notice, awaits operation, reports errors visibly, and always clears busy in finally. The run-job busy adapter sets the selected job id on true and nil on completion. install-control! updates status, draft and seeded JSON drafts.

load-data concurrently reads Discord metadata and event control, explicitly merges configured/tokenPreview, resets draft token, seeds job JSON drafts, and selects github if supported, otherwise first source or manual. Always clear loading in finally. Keep recursive deep-merge and JSON parse errors with their existing job-specific details.

Split runtime start/stop toggle and full reset buttons into short components preserving disabled state, titles, classes and wire. Retain sidebar-controller and public compatibility contracts.

Replace the giant panel with use-panel-state, derive-panel, bind-panel, use-panel-controller, panel-job-list, panel-sidebar, and panel-content:
- Consolidated CLJS state retains loading, saving-control, running-job-id, toggling-runtime, resetting-runtime, notice, error, status, draft, json-drafts, selected-job-id, job-search, draft-token and event-source-kind.
- Stable memoized field setters accept either direct values or updater functions.
- Derived state retains filtered jobs, current selection fallback, runtime lookup and event/channel counts.
- Bind reload, update, run and sidebar command callbacks.
- Preserve effects: initial load; select first filtered job when current selection disappears; report selected job changes.
- Remove only unused local token/event dispatch state that had no rendered controls; public command APIs remain.
- Destructure component props explicitly into CLJS data. Never merge an opaque native JS props object into a CLJS map.
- Verify spread props through real Helix tests before accepting the reconstruction.

The preserved panel interaction test uses the real native API boundary and a fetch transport fixture. It checks model editing and exact save wire with cadence preserved; a refused save keeps the draft and re-enables controls; read-only controls issue no commands; encoded job/trigger identifiers and direct token/event bodies preserve API contracts.

## Remaining document/forum reconstruction

Create pages/source_doc/forum_state.cljs and forum_view.cljs. The former requires only string, hooks and pure forum-thread logic; the latter uses Helix DOM and those native namespaces, avoiding the Vite Markdown bridge so interactions can run in Node/JSDOM.

forum-state/use-pages:
- Start page1, 40 posts per page, image filter false.
- Memoize prepared posts, image-filtered posts, visible page slice and distinct thread image URLs.
- Reset pagination/filter when threadId/threadUrl/posts change and clamp current page when total changes.
- Return state plus setters, visible-count, total-pages, paged-posts and thread-image-urls.

forum-state/use-images:
- State contains expanded-images, failed-images, image-retry-nonce, zoom-gallery, zoom-index=-1 and zoom-failed.
- Toggle expansion, record image failures, retry by clearing failure and incrementing nonce.
- build-src appends retry=N with ? or & only when nonce is nonzero.
- Open gallery from distinct thread URLs, fallback to the selected URL when empty; find selected index.
- Step wraps modulo gallery count, close resets gallery/index/failure; keyboard Escape/ArrowRight/ArrowLeft and listener cleanup.
- Reset image state on thread change.
- Avoid local key shadowing core/key.

forum-view components preserve existing markup/classes/content from published source_doc/view.cljs:
forum-pagination, failed-image, post-image, post-images, forum-header, forum-filters, post-header, forum-post, zoom-content, forum-gallery, forum-thread-view.
Keep exact accessible labels and actual hx/<> fragment macro (a literal :<> tag is invalid). Add aria-label Posts per page. Keep image-loading opt-in, limits of12 external links/8 previews, original URL links, failure/retry controls, referrerPolicy, gallery wrapping and close/escape behavior. Pass image state explicitly as a CLJS map.

Make source_doc/view.cljs a thin router/Markdown composition:
- Preserve public forum-thread-view and forum-pagination compatibility aliases.
- Keep document-content URL encoding per path segment.
- Use an active-request cleanup guard for document loads so obsolete path results cannot replace the current document.
- Preserve loading/error text, source path title, Back to Chat, forum parsing, Markdown rendering and ordinary preformatted text.
- Preserve fragment smooth scroll, external new-window links, relative document resolution and encoded /docs route navigation.
- Keep Vite bridge Markdown only in this thin page, not the testable forum modules.

The preserved forum interaction test checks 45-post pagination, page-size changes, image filtering, no automatic preview loads, failed-image retry URL, two-image gallery wrap, and Escape closure. Its manually repaired delimiters require a real parser before any success claim.

## Resume and validation

Resume in the same sandbox, language and project once the executor reconnects. Fetch this immutable rescue commit into an isolated checkout; reconcile with the coordinator's then-current PR305 tree, read repository instructions, and reconstruct the missing source modules. Do not overwrite another lane or production dist during browser tours.

Run configured scoped clj-kondo with zero warnings/errors, a dedicated frontend CLJS test build (not frontend/dist), and meaningful interaction tests using the actual API/DOM boundaries. Run the full frontend CLJS suite and existing TypeScript/Vitest gates as appropriate. The previous full suite had a Git HEAD vs migration-ledger provenance mismatch from API-published files not yet adopted locally; fix the actual history/source mismatch rather than suppressing that check.

Only after source/tests are complete and gates pass should these files be promoted to PR305. Rebuild current production artifacts and repeat the real browser tour under the coordinator's artifact lease. Historical screenshots and vanished artifacts do not prove these new bytes.
