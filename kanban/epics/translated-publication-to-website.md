---
uuid: knoxx-translated-publication-to-website
title: Translated publication to open-hax/website — the first real publication target
status: breakdown
priority: P1
points: 78
labels:
  - epics
  - publication
  - translations
  - integration
  - deployment
  - website
---

# Translated publication to open-hax/website — the first real publication target

## Signal

`knoxx-contract-owned-publication-pipeline` built desired state, the gate, the
plan laws, the effect boundary, receipts, and an E2E — and every one of them is
correct. None of them publishes anything.

The evidence, from the branch at the tip of the stack:

```text
IPublicationTarget implementations   1   (publication-target-memory)
production callers of the effect     0   (only the target + tests require it)
adapters that write bytes anywhere   0
things that call plan -> effects     0   (no reconciler runs, anywhere)
```

`infra/publication_effects.cljs` is required by exactly two files:
`infra/publication_target_memory.cljs` and its tests. The routes are registered
(`infra/routes/app.cljs`) and answer, so **desired state is real and readable in
production**. The other half of the seam — observed state produced by an effect
against a target — has no production instance at all.

The translation half has the same shape. The gate emits translation work; the
ingestion worker (`ingestion/src/kms_ingestion/translation/worker.clj`) has no
publication awareness and nothing dispatches to it. Approval receipts exist only
as test fixtures; no surface records a human approval.

This epic closes that, once, against one real target: **`open-hax/website`**.

## Why website is the right first target

- It is the smallest honest target. Static bytes at a path, no auth, no editor,
  no round trip — exactly what `IPublicationTarget` was designed around, and the
  cheapest way to find out whether the protocol survives contact.
- It has genuine translatable content and no translation of any kind today: all
  copy is hardcoded in `src/cljs/open_hax/website/sections/*.cljs`, in one
  language, with no locale routing and no content source.
- It is already half-declared as a deployment (`services#19`), so the deployment
  question is answerable rather than hypothetical — and answering it settled the
  lane question for the whole constellation: everything deploys to DigitalOcean.
- Nothing depends on it. A wrong artifact on the website is embarrassing; a wrong
  artifact in the CMS is a data-integrity incident.

## Ownership rule

```text
knoxx resources         own which documents are published, in which locales
knoxx law/domain        own admissibility, planning, and blockers
knoxx receipts          own what was actually materialized
the target adapter      owns bytes-at-a-path and nothing else
open-hax/website        owns rendering, routing, and locale selection in the UI
open-hax/services       owns where the content root is and who may write it
```

The website never asks Knoxx anything at request time. It reads a manifest and
files from a directory. If Knoxx is down, the site still serves what was last
published — which is the entire point of choosing a static target first.

## Laws

1. A publication target adapter performs effects and holds no publication
   semantics; swapping the memory target for the site target changes no decision.
2. Serving is idempotent under replay: the same intent at the same concrete
   revision materializes once, and a second run is a `:noop`.
3. Published bytes are addressed by document × locale × concrete revision. A
   selector (`:source/current`) never reaches an adapter or a served path.
4. A locale the target does not accept is a blocker, not a partial publish.
5. The website renders only what the manifest declares. An artifact present on
   disk but absent from the manifest is not public.
6. An empty or absent content root is a valid state that serves the site
   correctly, not an error.
7. Published content has exactly one writer.

## Waves

```text
W0 — unblock          the stack is stranded and nothing else can land ....  2
W1 — the missing edge  registry, artifact contract, site adapter, runner .. 19
W2 — the translation half  dispatch, approval surface, locale catalog .... 10
W3 — the website       content source, locale routing, contract tests ..... 11
W4 — deployment        content root, gated service, lane retirement, live . 36
```

## Children

### W0 — unblock

1. **P0 / ready / 2sp** `knoxx-publication-stack-relink` — the nine-PR chain is
   stranded behind an auto-closed base. Nothing below can start until it lands.

### W1 — the missing production edge

2. **P1 / ready / 3sp** `knoxx-publication-artifact-contract` — say what a
   materialized artifact *is*. The memory target stores `:route/artifact`
   opaquely; no adapter can be written against "opaque".
3. **P1 / ready / 5sp** `knoxx-publication-target-registry` — select an adapter
   from resources instead of constructing one at a call site.
4. **P1 / ready / 8sp** `knoxx-publication-static-site-target` — the first
   non-memory adapter: content root, manifest, atomic swap.
5. **P1 / ready / 3sp** `knoxx-publication-reconciler-runtime` — something that
   actually calls plan → effects, on a trigger, with receipts.

### W2 — the translation half

6. **P1 / ready / 5sp** `knoxx-translation-work-dispatch` — the gate's derived
   work reaches the ingestion worker and comes back as a receipt.
7. **P1 / ready / 3sp** `knoxx-translation-approval-surface` — a human can
   record a revision-specific approval through a real, authorized route.
8. **P2 / ready / 2sp** `knoxx-publication-locale-catalog` — which locales a
   target accepts, declared in resources.

### W3 — the website

9. **P1 / ready / 8sp** `website-published-content-source` — the site renders
   published content and locale-routes, instead of hardcoded English.
10. **P2 / ready / 3sp** `website-manifest-contract-tests` — the reader asserts
    the manifest contract it depends on, in its own repo's tests.

### W4 — deployment

11. **P1 / ready / 3sp** `services-website-content-root` — declare the content
    root on the DigitalOcean host, its single writer, and the read-only mount.
12. **P1 / ready / 5sp** `services-website-as-gated-service` — website becomes a
    gated DigitalOcean service shipped as an image, replacing `services#19`.
13. **P1 / breakdown / 0sp roll-up** `services-promethean-lane-retirement` — one
    lane, so the next service does not face this choice again. Eleven children,
    31sp: two dispositions (`services-openplanner-lane-disposition`,
    `services-caddy-hostname-scale-decision`), seven per-service
    (`services-axxium-digitalocean-migration`, `services-staging-slot-pattern`,
    `services-knoxx-staging-migration`, `services-proxx-staging-migration`,
    `services-promethean-ingress-decommission`,
    `services-local-proxx-bridge-decommission`,
    `services-stale-promethean-definitions-removal`), and two closing
    (`services-promethean-dns-cutover`, `services-promethean-lane-deletion`).
14. **P2 / ready / 2sp** `knoxx-website-publication-live-verification` — one
    live run: intent → translate → approve → materialize → fetch over HTTPS.

## Build order

```text
publication-stack-relink
  -> publication-artifact-contract
       -> publication-target-registry
       -> publication-static-site-target ---+
  -> translation-work-dispatch              |
  -> translation-approval-surface           |
       -> publication-reconciler-runtime <--+
  -> services-website-content-root
       -> services-website-as-gated-service
            -> website-published-content-source
                 -> website-manifest-contract-tests
                      -> knoxx-website-publication-live-verification
publication-locale-catalog joins any time after the artifact contract.
```

`services-promethean-lane-retirement` and its eleven children run alongside;
nothing here waits on them. Its own order is on that roll-up card — dispositions
first, then per-service, then DNS, then deletion, with DNS moving one hostname at
a time as each service becomes ready rather than as a flag day.

The dependency that was easy to get wrong is now closed. Everything deploys to
DigitalOcean, so Knoxx and the website are compose projects on one host and
`publication-static-site-target` is a **filesystem** adapter over a bind-mounted
content root — rename is atomic, so the manifest swap is a primitive rather than
a protocol. `services-website-content-root` still precedes it, for the declared
path, mount and uids rather than for the transport choice. See
`services:docs/deployment-model.md` §4.

## Explicit non-goals

- Do not make the website call Knoxx at request time. The seam is a manifest on
  disk, not an API.
- Do not build a translation editor. The approval surface records a decision; it
  does not host the review UX (`knoxx-translation-review-chat-panel` owns that).
- Do not machine-translate as part of publication. The gate blocks on evidence;
  producing that evidence is the worker's job and is asynchronous.
- Do not promote adapter identity into desired state. Which target published
  something is a receipt fact.
- Do not generalize to a second target in this epic. One real adapter is the
  proof; the second is what makes the protocol honest, and it comes after.
- Do not migrate every Promethean service as part of this epic. The lane
  retirement card owns the inventory and the order; publication does not wait on
  it.
- Do not put website source content into Knoxx resources for its own sake. Only
  what is genuinely published and translated moves.

## Done when

- A document authored in `en` is published to `open-hax.promethean.rest` in a
  second locale, through the contract path, with a receipt chain that is walkable
  end to end.
- Replaying the reconciler changes nothing and emits `:noop`.
- Unpublishing removes the route; the site stops serving it.
- The website serves correctly against an empty content root.
- `verify.sh` for the website fails when the manifest contract is broken.
- No adapter-specific identifier appears above the effect boundary.
- The memory target is still the E2E's target — the site adapter did not require
  changing a single decision above the boundary.
