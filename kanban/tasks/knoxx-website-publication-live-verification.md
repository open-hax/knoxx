---
uuid: knoxx-website-publication-live-verification
title: Publication — one live run to the website, end to end
status: ready
priority: P2
points: 2
labels:
  - tasks
  - publication
  - translations
  - verification
  - website
  - has-parent
---

# Publication — one live run to the website, end to end

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

The epic's E2E proves the whole journey with no hosted backend running, which is
exactly the right thing for a test and is not evidence that anything reached
production. This card is the one live run: a real document, translated, approved,
materialized, and fetched over HTTPS from `open-hax.promethean.rest`.

## Why this is separate from the E2E

The stack's own history is the argument. `scripts/verify-publication-epic.sh`
found, on its first live execution, that **every** publication route answered 500
to every real request: Fastify builds `request.params` with `Object.create(null)`,
`js->clj` returns a null-prototype object unchanged, and the closed
`DecodedRequest` schema rejected it. 980 tests passed over it because every
fixture built params with `clj->js`, which produces an ordinary `Object`.

A test that constructs its own inputs cannot find that class of defect. Only a
live run can.

## Dependencies

Everything. This is the last card.

## Work

- Extend `scripts/verify-publication-epic.sh` rather than writing a second
  verifier, and keep it aligned with
  `knoxx-publication-live-verification-contract`'s rules: explicit PASS / WARN /
  FAIL, restricted success sets, proof of which deployment is under test, and
  nonzero exit on signal and internal-error paths.
- The run: create intent → observe the blockers → dispatch translation → record
  the translation receipt → approve the revision → reconcile → fetch the public
  URL over HTTPS and assert the translated bytes and the `lang` attribute.
- Then replay: reconcile again, assert `:noop` and that nothing was rewritten.
- Then remove: withdraw the intent, reconcile, assert the URL stops serving.
- Bound the side effects and clean up the fixture publication, or retain it
  deliberately and say so — a permanent test document on a public site is a
  decision, not a leftover.
- Record the receipt chain from the run as the evidence artifact, and the exact
  commands, so the result is reproducible rather than reported.

## Definition of Done

- A translated document is fetched over HTTPS from the production hostname,
  published through the contract path.
- The receipt chain from intent to served bytes is walkable with no gap.
- Replay changes nothing.
- Withdrawal makes the URL stop serving.
- The verifier fails on any unexpected 4xx/5xx and cannot false-green.
- Evidence and commands are recorded.
