# UI ↔ Backend Surface Test Plan

Status: draft coverage plan and executable TODO stubs
Generated for: Knoxx frontend shadow-cljs + TS bridge migration
Primary stub source: `frontend/src/test/ui-backend-surface-matrix.ts`
Executable TODO harness: `frontend/src/test/ui-backend-surface-stubs.test.ts`

## Scope

This plan covers the current routed Knoxx UI surfaces from the shadow-cljs router and embedded TS bridge pages/components:

- App/auth shell surfaces.
- Main nav routes: `/`, `/mail`, `/studio`, `/cms`, `/cms/editor/*`, `/contracts`, `/data/*`, `/gardens`, `/translations/*`, `/events`, `/agents`.
- Ops subroutes mounted under `/ops/*`.
- Shared embedded surfaces that interact with backend APIs, especially `ChatWorkspacePane`, `ContextBar`, admin integrations, and studio/CMS/data panels.

Known caveat: this is a route-and-component inventory, not proof that every backend endpoint is covered by production user traffic. The matrix intentionally includes endpoint families and route patterns rather than every possible query-string permutation.

## Test kind legend

| Kind | Meaning | Primary tools |
| --- | --- | --- |
| `unit` | Pure helpers, reducers, serialization, id/status mapping, no DOM. | Vitest |
| `api-client` | API wrapper path/method/body/error normalization. | Vitest with `fetch` mock |
| `component-integration` | Render component/page with mocked backend, interact with UI, assert request/DOM state. | Vitest + Testing Library / jsdom |
| `ava-puppeteer-e2e` | Browser-level route flow through shadow-cljs router and TS bridge bundles. | AVA + Puppeteer |
| `backend-route-contract` | Backend route schema/shape expectations independent of UI. | CLJS backend tests or contract fixtures |
| `ws-contract` | WebSocket/SSE/progress stream event ordering and recovery. | Mock WebSocket in Vitest or AVA fixture |

## Priority gates

| Priority | Must prove before claiming UI correctness |
| --- | --- |
| P0 | Critical create/read/update, auth, chat streaming, contract saves, admin/runtime controls, destructive safety. |
| P1 | Secondary interactions, partial failures, refresh/poll behavior, previews, labels, copies. |
| P2 | Long-tail browser flows, diagnostics, public preview links, route-to-route cross-links. |

## Surface inventory

### 1. Auth boundary, login, signup, logout

- UI: `AuthBoundary`, `LoginPage`, `SignupPage`, shell user menu.
- Routes: all routes, `/login`, `/signup`.
- Backend:
  - `GET /api/auth/context`
  - `GET /api/auth/config`
  - `POST /api/auth/invite/redeem`
  - `POST /api/auth/signup`
  - `POST /api/auth/logout`
- Behavior:
  - App boot blocks protected rendering until auth context resolves.
  - Unauthenticated users get login/signup surfaces.
  - Invite redemption/signup refreshes auth state.
  - Logout clears auth state and returns to unauthenticated rendering.
- Tests to write:
  - P0 component integration: unauthenticated context renders login.
  - P0 component integration: invite redemption calls redeem then context refresh.
  - P1 AVA/Puppeteer: logout from shell returns to login.

### 2. Main chat workspace runtime

- UI: `ChatPage`, `ChatWorkspacePane`, `ChatMainPane`, `ChatRuntimePanel`.
- Route: `/`; also embedded in contracts/events/agents audit.
- Backend:
  - `GET /api/config`
  - `GET /api/proxx/models`
  - `GET /api/tools/catalog`
  - `GET /api/knoxx/agents/catalog`
  - `POST /api/knoxx/chat/start`
  - `POST /api/knoxx/direct/start`
  - `GET /api/knoxx/session/status`
  - `GET /api/knoxx/run/:runId/events`
  - `WS /ws/stream`
  - `POST /api/knoxx/steer`
  - `POST /api/knoxx/follow-up`
  - `POST /api/knoxx/abort`
  - `POST /api/knoxx/session/undo`
  - `POST /api/shibboleth/handoff`
- Behavior:
  - Config, models, tools, and contract catalog hydrate composer/runtime affordances.
  - Send starts or resumes a run and consumes streaming updates.
  - Runtime panel shows session status, run events, witnesses, and receipts.
  - Control actions mutate active conversation safely.
- Tests to write:
  - P0 unit: controller keeps conversation/session/run ids monotonic.
  - P0 component integration: send calls chat start and renders queued state.
  - P0 ws-contract: cumulative/overlapping deltas collapse to one final assistant message.
  - P1 AVA/Puppeteer: start, stream, abort, recover.

### 3. Chat context browser and scratchpad

- UI: `ContextBar`, `ChatWorkspaceSidebar`, `WorkspaceBrowserCard`, scratchpad actions.
- Routes: `/` and embedded chat surfaces.
- Backend:
  - `GET /api/memory/sessions`
  - `GET /api/memory/sessions/:sessionId`
  - `POST /api/memory/search`
  - `GET /api/documents/content/:path`
  - `GET /api/ingestion/browse`
  - `GET /api/ingestion/file`
  - `POST /api/ingestion/search`
  - `POST /api/cms/documents`
  - `POST /api/data/op/labels/records/:recordId/reaction`
- Behavior:
  - Session/file/search context loads without blocking chat.
  - Pinning and scratchpad state persist locally.
  - File reads and CMS document creation handle success/error.
- Tests to write:
  - P1 unit: workspace action normalization.
  - P1 component integration: pin file injects controller context.
  - P2 AVA/Puppeteer: open file, pin it, create scratchpad CMS doc.

### 4. Chat voice and multimodal

- UI: `VoiceInputButton`, `VoiceReplyButton`, `ConversationVoiceButton`, `MultimodalInput`.
- Routes: `/` and embedded chat surfaces.
- Backend:
  - `POST /api/voice/stt`
  - `POST /api/voice/tts`
  - `GET /api/workspace-media/raw`
  - `POST /api/knoxx/chat/start` with `contentParts`
- Behavior:
  - Recorded audio posts multipart STT and inserts transcript.
  - Assistant text can synthesize playable TTS blobs.
  - Attachments normalize into multimodal content parts.
- Tests to write:
  - P1 component integration: STT success/failure.
  - P1 component integration: TTS blob and object URL cleanup.
  - P1 unit: content part normalization.

### 5. Mail actor mailbox

- UI: `MailPage`.
- Route: `/mail`.
- Backend:
  - `GET /api/actors/mailbox?box=&status=&limit=100`
  - `POST /api/actors/mailbox/:mailboxId/ack`
- Behavior:
  - Inbox/outbox/status filters reload entries.
  - Entry cards link to audit sessions/runs/events.
  - Acknowledge posts ack and refreshes.
- Tests to write:
  - P1 component integration: filters query correctly.
  - P1 component integration: acknowledge then refresh.
  - P2 AVA/Puppeteer: mailbox links navigate into audit/events.

### 6. Studio audio library, playlists, labels

- UI: `BroadcastStudioPage`, player, playlist queue, label controls.
- Route: `/studio`.
- Backend:
  - `GET /api/studio/audio-library`
  - `POST /api/studio/audio-library/ensure-dir`
  - `POST /api/studio/audio-library/rename`
  - `GET /api/studio/stream`
  - `GET/PUT /api/studio/playlist`
  - `POST /api/studio/save-m3u`
  - `GET /api/studio/load-m3u`
  - `GET /api/studio/playlists`
  - `GET/POST /api/studio/labels*`
  - `GET/POST /api/studio/audio-asset*`
- Behavior:
  - Browsing, playback, rename, playlist persistence, and labels stay in sync.
  - Audio stream URLs use backend paths safely.
  - Waveform/spectrogram assets save and reload.
- Tests to write:
  - P0 component integration: load library and selected file stream URL.
  - P1 component integration: playlist save/load round-trip.
  - P1 component integration: label add/remove refreshes chips.
  - P2 AVA/Puppeteer: playback queue survives route reload.

### 7. Studio graph/CMS actions and Discord scans

- UI: studio graph labels, CMS draft actions, Discord scan actions, contract-driven UI actions.
- Route: `/studio`.
- Backend:
  - `GET/POST /api/openplanner/v1/graph/labels`
  - `POST /api/openplanner/v1/graph/edges/query`
  - `GET /api/ingestion/audio-context`
  - `GET/POST /api/openplanner/v1/cms/documents`
  - `GET /api/openplanner/v1/gardens`
  - `GET /api/contracts/ui-actions`
  - `POST /api/studio/discord-audio-scan`
  - `POST /api/studio/discord-image-scan`
- Behavior:
  - Graph labels/edges organize audio and prompt context.
  - CMS/garden actions create or publish studio-adjacent documents.
  - Discord scans report counts, manifests, skipped, and failed items.
- Tests to write:
  - P1 component integration: contract UI actions render enabled/disabled states.
  - P1 component integration: scan summary with failures.
  - P2 backend-route-contract: audio context response schema.

### 8. CMS workbench documents and publish

- UI: `CmsPage` workspace browser, CMS document editor, templates, publish controls.
- Route: `/cms`.
- Backend:
  - `GET /api/ingestion/browse`
  - `GET/PUT /api/ingestion/file`
  - `POST /api/ingestion/search`
  - `GET /api/ingestion/sources`
  - `GET /api/ingestion/jobs`
  - `GET/POST/PATCH /api/openplanner/v1/cms/documents`
  - `POST/DELETE /api/openplanner/v1/cms/publish/:docId/:gardenId`
  - `GET /api/openplanner/v1/gardens`
  - `GET /api/memory/sessions`
  - `GET /api/studio/stream`
- Behavior:
  - Files, templates, gardens, sources/jobs, and CMS documents load independently.
  - Draft create/update writes OpenPlanner docs and optional workspace files.
  - Publish toggles selected garden state.
- Tests to write:
  - P0 component integration: document list/detail hydrates editor.
  - P0 component integration: existing save uses PATCH and preserves doc id.
  - P1 component integration: publish toggle POST/DELETE.
  - P2 AVA/Puppeteer: create draft from workspace file and publish.

### 9. Visual CMS editor

- UI: `VisualCmsEditorPage`.
- Route: `/cms/editor/*`.
- Backend:
  - `GET /api/ingestion/file?path=:draft/view-contract.edn`
  - `GET /api/ingestion/file?path=:draft/content.md`
  - `PUT /api/ingestion/file`
- Behavior:
  - Reads view contract and content markdown.
  - Saves edited content to file API.
  - Missing draft files show recoverable errors.
- Tests to write:
  - P1 component integration: loads draft files and renders editor.
  - P1 component integration: save writes expected path/body.

### 10. Contracts page admin editor

- UI: `ContractsPage` TS editor, runtime side panel, embedded chat.
- Route: `/contracts`.
- Backend:
  - `GET /api/admin/contracts?kind=:class`
  - `GET /api/admin/contracts/:id?kind=:class`
  - `POST /api/admin/contracts/validate`
  - `PUT /api/admin/contracts/:id`
  - `POST /api/admin/contracts/:id/copy`
  - `GET /api/admin/config/events`
  - `POST /api/admin/triggers/:id/fire`
- Behavior:
  - Class filters and selection hydrate EDN + structured fields.
  - Validate/save sends current EDN and renders validation/notices.
  - Copy creates a new contract without overwriting source.
  - Runtime panel maps triggers/jobs to selected contract.
- Tests to write:
  - P0 component integration: class switch reloads and clears stale selection.
  - P0 component integration: validate/save posts current EDN.
  - P1 component integration: copy focuses returned contract.
  - P1 AVA/Puppeteer: browser save/validate flow.

### 11. Agents contracts tab CLJS editor

- UI: shadow-cljs `AgentsPage` contracts tab.
- Route: `/agents?tab=contracts`.
- Backend:
  - `GET /api/admin/contracts?kind=agents|roles|triggers|pipelines`
  - `GET /api/admin/contracts/:id?kind=agents`
  - `POST /api/admin/contracts/validate`
  - `PUT /api/admin/contracts/:id`
  - `GET /api/admin/config/events`
  - `POST /api/admin/triggers/:id/fire`
- Behavior:
  - Agent list, editor draft, EDN, roles, triggers, and schedules are CLJS-owned.
  - Selected agent drives side-panel selected triggers and all-trigger partitioning.
  - Validate/save bridges CLJS editor state through TS API clients.
- Tests to write:
  - P0 AVA/Puppeteer: already implemented save/validate edited id flow.
  - P1 AVA/Puppeteer: new agent button initializes clean draft.
  - P1 component/unit: CLJS helper tests for trigger projection.

### 12. Agents audit tab chat viewer

- UI: `AgentsPage` audit tab, `AgentAuditSessionList`, `ChatWorkspacePane`.
- Route: `/agents?tab=audit`.
- Backend:
  - `GET /api/admin/contracts?kind=agents`
  - `GET /api/admin/agents/active`
  - `GET /api/memory/sessions`
  - `GET /api/memory/sessions/:sessionId`
  - `GET /api/admin/config/events`
- Behavior:
  - Active runs and memory sessions merge into one side-panel list.
  - Selected contract filters sessions and trigger cards.
  - Selecting history resumes transcript into normal chat UI.
- Tests to write:
  - P0 unit: merge/dedupe/status helper tests.
  - P0 component integration: active/history load and contract filter.
  - P0 AVA/Puppeteer: already implemented session resume.

### 13. Events runtime control workbench

- UI: CLJS `EventsPage`, `EventAgentsPanel`, `AgentAuditLogs`, Events Chat.
- Route: `/events`.
- Backend:
  - `GET /api/admin/tools`
  - `GET/PUT /api/admin/config/events`
  - `POST /api/admin/config/events/jobs/:id/run`
  - `POST /api/admin/config/events/dispatch`
  - `POST /api/admin/config/events/runtime/start|stop|reset`
  - `GET /api/admin/agents/active`
  - `GET /api/memory/sessions`
- Behavior:
  - Permission gates hide runtime controls and tool catalog.
  - Runtime jobs can be selected, manually run, and pinned into chat context.
  - Dispatch/start/stop/reset update runtime state and audit logs.
- Tests to write:
  - P0 component integration: permission denied hides controls.
  - P0 component integration: manual run posts job id and refreshes runtime.
  - P1 AVA/Puppeteer: dispatch/start/stop/reset happy path.

### 14. Data overview, sources, services

- UI: `DataPage` overview/sources/services tabs.
- Routes: `/data`, `/data/overview`, `/data/sources`, `/data/services`.
- Backend:
  - `GET /api/ingestion/sources`
  - `GET /api/ingestion/jobs`
  - `POST /api/ingestion/jobs`
  - `POST /api/ingestion/jobs/:jobId/cancel`
  - `GET /api/data/health`
  - `POST /api/data/jobs/build-semantic-edges`
  - `POST /api/data/jobs/build-semantic-edges/incremental`
  - `GET /api/data/op/v1/graph/embedding-coverage`
- Behavior:
  - Overview polls source/job/service/embedding data.
  - Source/service controls trigger or cancel jobs.
  - Partial backend failures degrade cards independently.
- Tests to write:
  - P0 component integration: partial failure still renders fulfilled cards.
  - P1 component integration: sync posts ingestion job and refreshes.
  - P1 backend-route-contract: ingestion job response schema.

### 15. Data files, documents, graph, labels

- UI: `DataPage` files/documents/graph/labels tabs.
- Routes: `/data/files`, `/data/documents`, `/data/graph`, `/data/labels`.
- Backend:
  - `GET /api/ingestion/browse`
  - `GET /api/ingestion/file`
  - `GET /api/data/op/documents`
  - `GET /api/graph/export`
  - `GET/POST/PATCH/DELETE /api/openplanner/v1/graph/labels`
  - `GET /api/openplanner/v1/graph/labels/:id/nodes`
  - `POST /api/openplanner/v1/graph/labels/:id/apply`
  - `POST /api/openplanner/v1/graph/labels/:id/remove`
- Behavior:
  - Files browse and preview workspace file contents.
  - Documents query OpenPlanner rows with filters.
  - Graph export lazy-loads only on graph tab.
  - Labels CRUD/apply/remove refresh selected view.
- Tests to write:
  - P1 component integration: graph tab lazy-loads once.
  - P1 component integration: labels CRUD/apply/remove refreshes.
  - P2 AVA/Puppeteer: document search to graph label workflow.

### 16. Data database console

- UI: `DataPage` database tab.
- Route: `/data/database`.
- Backend:
  - `GET /api/data/mongo/collections`
  - `POST /api/data/mongo/query`
  - `GET /api/data/pg/tables`
  - `POST /api/data/pg/query`
  - `POST /api/data/graphql`
- Behavior:
  - Lists Mongo collections and SQL tables.
  - Mongo JSON validates client-side before POST.
  - Query rows/errors render without leaking credentials.
- Tests to write:
  - P0 unit: invalid Mongo JSON blocks network call.
  - P1 component integration: Mongo rows and selected detail.
  - P1 component integration: PG error is sanitized.

### 17. Gardens CRUD and public preview

- UI: `GardensPage`.
- Route: `/gardens`.
- Backend:
  - `GET /api/openplanner/v1/gardens`
  - `POST /api/openplanner/v1/gardens`
  - `PATCH /api/openplanner/v1/gardens/:gardenId`
  - `DELETE /api/openplanner/v1/gardens/:gardenId`
  - `GET /api/openplanner/v1/public/gardens/:gardenId/html`
- Behavior:
  - Garden list loads publication destinations and public preview links.
  - Create/update/delete preserve slug/id semantics.
  - Delete requires confirmation and refreshes list.
- Tests to write:
  - P1 component integration: create posts form and refreshes.
  - P1 component integration: delete confirms before DELETE.
  - P2 AVA/Puppeteer: public preview link generated.

### 18. Translations review workbench

- UI: `TranslationReviewPage`, translation cards, model config section.
- Routes: `/translations`, `/translations/:documentId/:targetLang`.
- Backend:
  - `GET /api/translations/segments`
  - `GET /api/translations/segments/:segmentId`
  - `POST /api/translations/segments/:segmentId/labels`
  - `GET /api/translations/export/manifest`
  - `GET /api/translations/export/sft`
  - `GET /api/translations/documents`
  - `GET /api/translations/documents/:documentId/:targetLang`
  - `POST /api/translations/documents/:documentId/:targetLang/review`
  - `GET /api/translations/batches`
  - `POST /api/translations/batches`
  - `GET/PATCH /api/openplanner/v1/translations/config`
  - `GET /api/proxx/models`
- Behavior:
  - Lists segments/documents/batches and drills into review.
  - Label/review posts structured payloads and updates status.
  - Manifest/SFT export respects project/language.
  - Model config loads Proxx models and persists selected model.
- Tests to write:
  - P0 component integration: document review posts overall and overrides.
  - P1 component integration: segment label updates row status.
  - P1 component integration: model config saves PATCH.

### 19. Admin orgs, actors, roles, lakes

- UI: `AdminLayout` overview/orgs/actors/roles/lakes tabs.
- Route: `/ops/admin/*`.
- Backend:
  - `GET /api/auth/context`
  - `GET /api/admin/bootstrap`
  - `GET /api/admin/permissions`
  - `GET /api/admin/tools`
  - `GET/POST /api/admin/orgs`
  - `GET/POST /api/admin/orgs/:orgId/actors`
  - `PATCH /api/admin/actors/:userId`
  - `PUT /api/admin/actors/:userId/credentials/:provider`
  - `GET/POST /api/admin/orgs/:orgId/roles`
  - `PATCH /api/admin/roles/:roleId/tool-policies`
  - `PATCH /api/admin/memberships/:membershipId/roles`
  - `PATCH /api/admin/memberships/:membershipId/tool-policies`
  - `GET/POST /api/admin/orgs/:orgId/data-lakes`
- Behavior:
  - Admin bootstrap loads auth, permissions, tools, orgs, selected-org resources.
  - Create/update forms enforce permissions and refresh scoped resources.
  - Tool-policy editors preserve allow/deny effects.
- Tests to write:
  - P0 component integration: bootstrap partial failure uses scoped org fallback.
  - P0 component integration: create actor posts selected org id/roles.
  - P1 component integration: membership role/tool save refreshes org data.

### 20. Admin integrations and observability

- UI: `DiscordSection`, `DataLakesSection`, `ProxxObservabilitySection`, `TranslationModelSection`.
- Routes: `/ops/admin/integrations` and admin component tabs.
- Backend:
  - `GET/PUT /api/admin/config/discord`
  - `GET/PUT /api/admin/config/events`
  - `POST /api/admin/config/events/jobs/:id/run`
  - `POST /api/admin/config/events/dispatch`
  - `POST /api/admin/config/events/runtime/start|stop|reset`
  - `GET/POST/DELETE /api/ingestion/sources`
  - `GET /api/ingestion/sources/:sourceId/audit`
  - `GET/POST/POST cancel /api/ingestion/jobs`
  - `WS /api/ingestion/ws/jobs/:jobId`
  - `GET /api/proxx/observability/dashboard/overview`
  - `GET /api/proxx/observability/analytics/provider-model`
  - `GET /api/proxx/observability/request-logs`
  - `GET/PATCH /api/openplanner/v1/translations/config`
  - `GET /api/proxx/models`
- Behavior:
  - Integrations configure Discord/events, ingestion, Proxx observability, translation model.
  - Ingestion websocket updates progress and falls back to polling.
  - Tokens render previews only; raw values never echo after save.
- Tests to write:
  - P0 component integration: Discord token save sends raw input but renders preview only.
  - P1 ws-contract: ingestion websocket progress and polling fallback.
  - P1 component integration: Proxx observability window reloads all cards.

### 21. Ops documents, vectors, settings, graph export

- UI: `DocumentsPage`, `VectorsPage`, `SettingsPage`, `RawGraphExportPage`, `SourceDocPage`, `SidebarOpsStatus`.
- Routes: `/ops/documents`, `/ops/vectors`, `/ops/settings`, `/ops/graph-export-debug`.
- Backend:
  - `GET/POST/DELETE /api/documents`
  - `POST /api/documents/upload`
  - `POST /api/documents/ingest`
  - `POST /api/documents/ingest/restart`
  - `GET /api/documents/ingestion-status`
  - `GET /api/documents/ingestion-progress`
  - `GET /api/documents/content/:path`
  - `GET /api/documents/ingestion-history`
  - `GET/PUT /api/settings`
  - `GET /api/settings/knoxx-status`
  - `GET/POST/PATCH/DELETE /api/settings/databases`
  - `POST /api/settings/databases/activate`
  - `GET /api/retrieval/stats`
  - `POST /api/chat/retrieval-debug`
  - `GET /api/graph/export`
- Behavior:
  - Documents upload/delete/ingest flows preserve selected state.
  - Settings forms read/write app settings and database configs.
  - Vector/retrieval and graph export diagnostics expose stats/data.
- Tests to write:
  - P1 component integration: upload then ingest updates progress.
  - P1 component integration: database activation posts selected id.
  - P2 component integration: graph export debug renders node/edge counts.

## Current implemented coverage anchors

The AVA/Puppeteer harness already covers these browser flows:

- `/agents?tab=audit` bridges CLJS shell, TS session list, and TS chat UI.
- Audit history card resumes transcript into `ChatWorkspacePane`.
- `/agents?tab=contracts` validates and saves edited CLJS contract editor state through API mocks.

The P0 chat runtime unit/component/stream-contract anchors now cover:

- `createChatRuntimeActions.handleSend` preserves session/conversation/run identifiers while a queued run hydrates.
- `ChatMainPane` renders a queued user turn and disables composer controls while sending.
- `novelAppendedText` keeps streamed assistant text monotonic across overlapping/cumulative websocket chunks.

The P0 auth boundary anchors now cover:

- `AuthBoundary` renders the lazy login surface instead of protected content when `/api/auth/context` returns 401.
- Invite redemption posts code/email to `/api/auth/invite/redeem` and refreshes `/api/auth/context` into authenticated protected rendering.

The contracts page P0 anchors now cover:

- `ContractsPage` loads a selected contract, validates the current EDN draft via `/api/admin/contracts/validate`, and saves the current EDN via `PUT /api/admin/contracts/:id` using the inferred `:contract/id`.
- Selecting a contract from a different class loads `/api/admin/contracts/:id?kind=triggers` semantics and updates the editor draft.
- Copying a contract posts the source id/new id/class through the contract API and focuses the returned draft.

The agents audit P0 anchors now cover:

- `mergeAuditSessions`/status helpers dedupe active/history records and sort active-first.
- `AgentAuditSessionList` loads memory history plus active runs, filters by selected contract, search-filters cards, and resumes the selected session.
- The AVA/Puppeteer harness verifies that an audit history card resumes transcript rows into the chat pane.

The data database P0 anchor now covers:

- Mongo filter/sort/projection JSON parsing is isolated in `parseMongoJsonText`; invalid JSON raises a labeled error before the Mongo query request can be built.

The translation model config anchor now covers:

- `TranslationModelSection` loads `/api/proxx/models` plus `/api/openplanner/v1/translations/config`, edits the selected model, and persists it via `PATCH /api/openplanner/v1/translations/config`.

The mail actor mailbox anchors now cover:

- `MailPage` loads inbox/outbox entries, changes box/status filters with the expected query arguments, and acknowledges inbox entries before refreshing the mailbox.

The gardens CRUD anchors now cover:

- `GardensPage` creates a garden with the expected POST payload and refreshes the list.
- `GardensPage` confirms deletion, issues DELETE for the selected garden id, and refreshes the list.

The CMS workbench anchors now cover:

- `CmsPage` loads gardens and CMS document lists, opens a selected CMS document by doc id, hydrates title/body/editor state, and pins document context.
- Saving an existing CMS document uses `PATCH /api/openplanner/v1/cms/documents/:docId`, preserves the selected doc id/source path, and renders the saved notice.
- The publish toggle posts `POST /api/openplanner/v1/cms/publish/:docId/:gardenId?skip_translation=true&defer_index=true` for unpublished documents and `DELETE /api/openplanner/v1/cms/publish/:docId/:gardenId` for already-published documents.

The Visual CMS editor anchors now cover:

- `VisualCmsEditorPage` loads `view-contract.edn` and optional `content.md`, then renders the visual editor and source panel with parsed draft data.
- Editing the visual contract marks the draft dirty and saving writes serialized EDN back to `PUT /api/ingestion/file` with the correct `draft/view-contract.edn` path.

The Broadcast Studio audio/library anchors now cover:

- `BroadcastStudioPage` loads `/api/studio/audio-library`, renders file entries, and plays a selected file via `/api/studio/stream?path=...`.
- Saved M3U playlists load through `/api/studio/load-m3u`, populate the queue in order, and save back through `POST /api/studio/save-m3u` with ordered queue items.
- Studio label editing applies and removes OpenPlanner graph labels for the selected audio file, then refreshes the applied chip list from graph edges.

Relevant files:

- `frontend/e2e/knoxx_e2e_support.cjs`
- `frontend/src/e2e/knoxx/frontend/e2e/ava_runner.cljs`
- `frontend/src/components/chat-page/chat-runtime-actions.test.ts`
- `frontend/src/components/chat-page/ChatMainPane.test.tsx`
- `frontend/src/components/chat-page/utils.test.ts`
- `frontend/src/pages/AuthContext.test.tsx`
- `frontend/src/pages/ContractsPage.test.tsx`
- `frontend/src/components/agent-audit/AgentAuditSessionList.test.tsx`
- `frontend/src/pages/DataPage.test.ts`
- `frontend/src/components/admin-page/TranslationModelSection.test.tsx`
- `frontend/src/pages/MailPage.test.tsx`
- `frontend/src/pages/GardensPage.test.tsx`
- `frontend/src/pages/CmsPage.test.tsx`
- `frontend/src/pages/VisualCmsEditorPage.test.tsx`
- `frontend/src/pages/BroadcastStudioPage.test.tsx`

## Recommended execution plan

1. Keep `uiBackendSurfaceMatrix` as the canonical checklist and make every new UI/backend route add or update one matrix entry.
2. Convert P0 `test.todo` entries into real tests before raising coverage gates.
3. Prefer component-integration tests for most surfaces; reserve AVA/Puppeteer for route-level CLJS/TS bridge seams and critical workflows.
4. Add backend-route-contract tests where the UI expects non-trivial payload shapes, especially ingestion jobs, audio context, translation review, and auth/admin bootstrap.
5. Add ws-contract tests for `/ws/stream` and ingestion job websocket behavior before relying on browser-only validation.
6. After P0 is green, enable coverage thresholds per package instead of one immediate global 80% gate.

## Commands

Run only the surface stub matrix:

```bash
cd frontend && node ./node_modules/vitest/vitest.mjs run --config vitest.config.ts src/test/ui-backend-surface-stubs.test.ts
```

Run the current browser e2e harness:

```bash
pnpm -C frontend test:e2e
```

Run TypeScript validation:

```bash
pnpm -C frontend typecheck
```
