# Wiki content and identity walkthrough

The live CMS is `/cms`; `/cms/editor/*` mounts the same native Helix wiki. It reads real resource identities, saved source bytes and revision-bound review history. Humans invoke the same authenticated commands available to agents with their roles. A writing suggestion remains a proposal until a writer explicitly adopts and saves it.

`scripts/wiki-browser-tour.mjs` exports `tour(page, config)` for the isolated stack supervisor. The supervisor must start the actual services, seed a disposable organization and active garden, authenticate the browser, verify which checkout is being served, and remove its fixtures and processes on success, failure or interruption. The stage does not provision its own permissions or replace HTTP responses.

```javascript
await tour(page, {
  baseUrl,
  outputDir: absoluteScreenshotDirectory,
  verifyCheckout,
  garden: fixtureGardenIdentity,
  targetLocales: ['es'],
  agentCommand,
  beforeSourceAcceptance,
  translationTour,
});
```

`agentCommand` receives either `{kind: 'review', document, action: 'comment', notes}` or `{kind: 'source', document, content}`. It must resolve the current source revision/history head and invoke the authorized shared command service. The browser waits for the actual SSE event to update the open view. Injecting a browser event would not prove that transport.

`beforeSourceAcceptance(page, {document, snapshot, shot})` verifies the publication gate after concurrent source changes invalidate acceptance. `translationTour(page, {document, snapshot, shot})` performs real translation dispatch, correction, review, revision, learning, acceptance and publication. Both callbacks are required and their return values are retained in `wiki-tour.json`. Missing source-gate, translation, or canonical memory verification fails the full tour. Visiting the translation page does not establish publication.

| Capture | Required evidence |
| --- | --- |
| 01 | Real resource inventory and granted controls |
| 02 | Persisted page identity, garden/locales and source revision |
| 03 | Real model response awaiting adoption |
| 04 | Requested source revision and reusable writing lesson |
| 05 | Exact-revision acceptance and remembered lesson |
| 06 | Agent comment appears in the open discussion through SSE |
| 07 | Concurrent agent source preserves unfinished human text and blocks stale saving |
| 08 | New source bytes invalidate previous acceptance |
| 09 | Existing translation workspace ready for the next cycle |

Screenshot annotations are temporary labelled DOM overlays over the actual website. They are removed after capture; no screenshot pixels or page contents are fabricated. Place regenerated screenshots in the gitignored `docs/verification/screenshots/` directory.

For identity, `/account` provides authenticated credential enrollment and management through Axxium. Password signup requires distinct username/email fields and a 12–1024-character password. Password login accepts either identifier. Passkeys use native WebAuthn and PGP uses exact detached-signature challenges; a browser tour should enroll, sign out and sign in again with each credential. External GitHub, Discord, Google and ATProto OAuth round trips require registered provider configuration and a consenting account. Unavailable providers must remain visibly unavailable. The identity tour and its exact verification coverage are maintained with the account implementation.

The source editor follows clean server updates immediately. Dirty text retains its base revision and cannot be saved after the server source changes. Review notes separately retain source revision and history head; typed feedback cannot silently move to a different discussion. Initial page text, source drafts and review feedback all participate in link, indexed Back/Forward, sign-out and browser-exit protection. Foreign history entries without BrowserRouter's integer `idx` cannot be safely restored by the same-document traversal guard.

Translation review refreshes both its inventory and open details after a publication event or reconnect. Pending corrections retain their original candidate and review basis. Stale submissions stay disabled until the reviewer explicitly discards the local review and loads the newest state.

Each garden/locale placement has an explicit Publish control. Requested state and observed artifact evidence remain separate: an accepted HTTP command can persist desired publication while reconciliation reports blockers. The interface does not invent materialization from command success. Exact command denials remain separate from broad role capabilities.

`pnpm -C frontend build` must emit the HTML, canonical Shadow application and Tailwind stylesheet into `frontend/dist`. The canonical Vite HTML phase precedes Shadow. All Vite configurations disable public-directory copying so tracked historical compiled files cannot replace or duplicate current output. The migration gate accepts genuine qualified Helix route constructors and preserves its rejection of unrelated aliases.

## Recovery verification status

The scratch cleanup removed the locally committed golden checkout. Its saved screenshots and earlier green tests document historical behavior; they do not verify reconstructed source. Recovery used the published baseline, preserved transformation scripts, recorded SHA-256 hashes, retained API contracts and original browser screenshots. All five migrated translation interaction test files, the shared translation review panel, and six build/bridge files matched golden hashes exactly. The CMS, live review orchestration, navigation guard, browser helper and additional regression tests were reconstructed and require fresh execution.

The integrated reconstructed frontend passed the configured `clj-kondo` hooks with zero errors and zero warnings. The production build emitted HTML, both bridges, the authoritative Shadow application (160 files, 114 compiled, zero warnings), and Tailwind CSS. TypeScript checking passed. Vitest passed 217 tests in 45 files, with 41 pre-existing todo cases. The CLJS suite compiled without warnings and ran 482 tests / 2125 assertions; its sole remaining failure was the deliberate comparison of the regenerated migration ledger with the previous Git HEAD, pending the recovery checkpoint. Two new interaction scenarios verify clean open-review refresh and preservation of dirty corrections under concurrent agent updates. Full suite success after that checkpoint and a new live browser cycle remain required. The machine-readable recovery manifest distinguishes exact and reconstructed bytes.

One historical command remains independently broken: `pnpm -C frontend lint:size` imports the missing root `size-lint.config.mjs`. Recovery does not invent replacement thresholds. The configured CLJS file/function size hooks remain enabled in the actual lint check.

## Service and translation verification helpers

`scripts/wiki-stack-services.mjs` starts the built backend, Vite preview, pinned Qwen2.5 0.5B generation and MiniLM embeddings from the warmed offline cache. It selects the EDN providers, hashes build files before startup, verifies the served frontend bytes, and stops only its owned process groups and model handles. It never deletes fixture or evidence directories. The parent supervisor owns isolated contract fixtures, the manifest-only artifact server, browser accounts and interruption cleanup. Returned environment settings contain bootstrap credentials and must not be serialized.

`scripts/wiki-translation-tour.mjs` exports `verifySourceUnaccepted` and `translationTour`. The latter accepts the authenticated page, document/source snapshot, screenshot callback, artifact server URL and required `verifyMemory` callback. It dispatches actual model work, records correction A, revises and accepts correction B, checks immutable history, accepts the full translation, publishes every placement, and fetches the served HTML. New source content then requires another source acceptance and a distinct translation candidate. The Clio callback resolves both candidate sets to distinct immutable turn IDs and verifies the second turn admitted the exact accepted correction and review reference. Missing evidence fails the tour; model quality is assessed by the human review rather than inferred from structural completion.
