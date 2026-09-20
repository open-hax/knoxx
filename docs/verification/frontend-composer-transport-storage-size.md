# Composer, session transport and storage ownership

Chat composition keeps draft submission and live steering. Its existing media
boundary owns attachment encoding, the voice button module owns the level gauge,
and the workspace parent owns the composer prop contract. Static glyph, toolbar
and preview-container styles now live in the existing stylesheet consumed by the
native application's Tailwind build. Composer/voice/parent/media are
399/365/245/217 lines. No controls or attachment fallback behavior were removed.
An isolated Tailwind build confirms all five selectors reach app.css; production
output remained frozen for the browser walkthrough.

Translation requests now live beside their existing review wire contracts in the
OpenPlanner API module. The common API retains compatibility exports. Session
proxy transport now lives in API core, retaining its distinct URL handling,
session header and ProxyApiError class. Existing ingestion types own the matching
wire shapes; the smaller proxy audit remains a separate type from the richer
direct ingestion audit. Common/translation/session facade/core/types are
399/280/398/213/100 lines. Their error bodies, empty-body fallbacks, JSON payloads
and multipart content boundaries remain unchanged.

Browser storage now owns the existing session snapshot/index migration and
pinned context persistence. The scratchpad component owns scratchpad persistence;
chat configuration owns model availability polling. Session hooks keep restoration
and runtime recovery with compatible public exports. Hooks/storage/scratchpad/
configuration/chat types are 356/298/202/132/368 lines. The original persisted
keys, migration precedence, transcript bounds and cancellation logic remain.

Fourteen new regressions cover:

- Composer draft submission, live steering and voice threshold/toggle controls.
- Browser FileReader encoding, preview metadata and expired recording fallback.
- Translation selectors, review payloads, text exports and failure responses.
- Session headers, encoded ingestion selectors, multipart uploads and error type.
- Storage access denial and migration that protects current snapshots.

The combined full Vitest run passes **45 files / 254 executed tests / 0 failures**,
including the independently owned Contracts controller migration. The **41
existing TODO cases remain explicitly unexecuted**. Complete typecheck, isolated
legacy emission and all fourteen source-file size gates pass. No thresholds were
changed and no TypeScript source paths were added. Native production output was
not rebuilt during this checkpoint.
