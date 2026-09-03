---
uuid: knoxx-translated-publication-to-website
title: Translated publication to open-hax/website — the first real publication target
status: review
priority: P1
points: 10
labels:
  - epics
  - publication
  - translations
  - integration
  - deployment
  - website
---

# Translated publication to open-hax/website — the first real publication target

> **Shipped.** `https://open-hax.promethean.rest/` serves the site from the
> DigitalOcean droplet, and `/es/`, `/fr/`, `/de/`, `/ja/` each answer 200 with
> their own `lang`. Verified 2026-09-03 against the live origin.
>
> This epic is kept open for the three slices below that are genuinely not done.
> Everything else is a record of what landed and why.

## The signal it opened on

The `knoxx-contract-owned-publication-pipeline` epic built desired state, the
gate, the plan laws, the effect boundary, receipts and an E2E — and published
nothing:

```text
IPublicationTarget implementations   1   (publication-target-memory)
production callers of the effect     0   (only the target + tests required it)
things that called plan -> effects   0
```

Desired state was readable in production; observed state had no producer, so
drift was permanently the whole of desired state. The translation half matched:
the gate derived work and the ingestion worker had no publication awareness.

That is closed.

## What landed

| Slice | Landed as |
|---|---|
| Unblock the stranded PR ladder | `#247`, then `#233`–`#243` merged in order |
| Artifact contract | `#248` |
| Target registry | `#251` |
| Locale catalog | `#250` |
| Static-site target adapter | `#252` |
| Translation work dispatch | `#253` |
| Revision-specific approval surface | `#254` |
| Reconciler runtime, trigger contract, authorized route | `#255` |
| One production deploy path for Knoxx | `#291` |
| The published content root | `services:docs/published-content-root.md` |
| Website as a gated service | `services:digitalocean/services/website/` |
| Locale routing, manifest reader, contract tests | `website#1`, `website#2` |
| Retirement of the second deploy lane | `services#67` |

The deployment model those definitions are built against is
`services:docs/deployment-model.md`; `service.yaml` cites its §2 and
`published-content-root.md` instantiates its §4.

## Laws — unchanged, and now enforced by shipped code

1. A target adapter performs effects and holds no publication semantics.
2. Serving is idempotent under replay; a second run is a `:noop`.
3. Published bytes are addressed by document × locale × concrete revision; a
   selector never reaches an adapter or a served path.
4. A locale the target does not accept is a blocker, not a partial publish.
5. The website renders only what the manifest declares.
6. An empty or absent content root is a valid state that serves correctly.
7. Published content has exactly one writer.

## What is not done

1. **P2 / ready / 2sp** `knoxx-website-publication-live-verification` — the one
   live end-to-end run: intent → translate → approve → materialize → fetch over
   HTTPS, then replay for `:noop`, then withdraw and confirm the route stops
   serving. The site serving is not the same as the seam being exercised.
2. **P1 / ready / 2sp** `services-caddy-hostname-scale-decision` — the header in
   `caddy/compose.yaml` still reasons from "three hostnames and HTTP-01 need
   neither" a wildcard nor resident DNS credentials. That compose file now
   declares **six** host variables. The decision was right at three and has not
   been re-made at six.
3. **P1 / ready / 8sp** staging, which exists nowhere: `services-staging-slot-pattern`
   (3sp), then `services-knoxx-staging-migration` (3sp) and
   `services-proxx-staging-migration` (2sp). Without a staging record the
   promotion rule in `deployment-model.md` §3 cannot be enforced, which is the
   seventy-commit hole `services#44` measured and nothing has closed.

## Explicit non-goals

- Do not make the website call Knoxx at request time. The seam is a manifest on
  disk, and that is why the site keeps serving when Knoxx is down.
- Do not pre-render published document paths — decided in
  `website:docs/decisions/0001-spa-fallback-over-prerendering.md`.
- Do not build a translation editor.
- Do not promote adapter identity into desired state.
- Do not generalize to a second target in this epic.

## Done when

The three unfinished slices above are complete and the live verification passes
against the production origin.
