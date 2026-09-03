---
category: "tasks"
labels: ["tasks", "has-parent", "publication", "website", "verification", "https", "wave-4"]
write-id: "1787011200006-0.249681"
points: "2"
title: "Website publication — live verification"
priority: "P2"
status: "ready"
uuid: "knoxx-website-publication-live-verification"
created_at: "2026-08-22T00:00:00Z"
---

# Website publication — live verification

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

Prove the full seam once against deployed services: publication intent becomes a
translation, an authorized revision approval, a materialized artifact and
manifest entry, and a document fetched from the website over HTTPS.

## Dependencies

`knoxx-publication-reconciler-runtime`, `knoxx-translation-work-dispatch`,
`knoxx-translation-approval-surface`, `knoxx-publication-locale-catalog`,
`services-website-as-gated-service`, `website-published-content-source`, and
`website-manifest-contract-tests`.

## Work

- Add a repeatable, environment-safe live verification procedure that seeds a
  unique publication intent, waits for the translation receipt, records an
  authorized revision-specific approval, triggers reconciliation, and cleans up
  its test data when possible.
- Fetch the resulting website route and `manifest.edn` over HTTPS, asserting the
  route's locale, revision, bytes, media type, and manifest artifact path agree
  with the materialization receipt.
- Verify the cross-repo contract's reader rules in the live environment: an
  absent or empty manifest serves the site, while malformed required fields and
  unsupported versions fail loudly.
- Explicitly verify or file completion follow-ups for the website verifier's
  known findings: artifact insertion via `innerHTML` can lose `<head>` content,
  and the missing-manifest response has Content-Type asymmetry.
- Record URLs, non-secret correlation identities, observed receipts, and any
  environment preconditions so a reviewer can reproduce the run.

## Definition of Done

- One recorded live run proves intent → translation → authorized approval →
  materialization → HTTPS fetch for a non-default locale.
- The fetched route's bytes and locale agree with the manifest and materialized
  artifact receipt; it never exposes an artifact with a disagreeing locale.
- The verification output proves the website made no request to a Knoxx origin.
- The `innerHTML` head-content and missing-manifest Content-Type findings are
  each verified as fixed/acceptable or linked to an explicit follow-up card.
- The procedure fails non-zero on a missing receipt, failed authorization,
  malformed manifest, incorrect HTTPS response, or leftover seeded public route.
