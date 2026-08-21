---
uuid: services-knoxx-deploy-path-unification
title: Services — one production deploy path for Knoxx
status: ready
priority: P0
points: 3
labels:
  - tasks
  - deployment
  - knoxx
  - has-parent
---

# Services — one production deploy path for Knoxx

> Parent: `services-promethean-lane-retirement`
> Repositories: `open-hax/knoxx` (workflows), `open-hax/services` (the module)

## Purpose

Knoxx has two production deploy paths, with different authorization models, and
the one that fires automatically is aimed at the host that no longer serves it.

```text
push to knoxx main
  -> knoxx deploy-production.yml
       -> services deploy-promethean.yml  (service: knoxx, environment: production)
            -> proxx.promethean.rest, 104.130.159.19   LEGACY HOST

merged PR carrying the `deploy` label
  -> services deploy-stack.yml -> deploy-stack-chain.yml -> deploy-digitalocean.yml
       -> 157.245.125.134                                  WHERE knoxx.promethean.rest RESOLVES
```

`knoxx.promethean.rest` resolves to 157.245.125.134. Every merge to Knoxx `main`
therefore runs a full preflight and then deploys to a box nothing routes to.

**P0 because it is in the way.** The publication ladder is ten merges to `main`,
and each one exercises this path.

## Dependencies

None. This is the first thing to fix in the lane retirement.

## Work

- Establish what the legacy path currently does when it runs, before changing it.
  A deploy to the old host is at best wasted; if that host still holds production
  credentials or reaches the production database, it is worse than wasted, and
  that must be answered rather than assumed.
- Decide the target shape and write down why:
  - **Remove the auto-deploy.** `services#45` deliberately moved deployment
    authorization to the merge-time `deploy` label, using the frozen closed
    `pull_request_target` payload as the authorization record, after a label
    could ship an unreviewed revision. A push-triggered deploy in the app repo
    is the design that decision replaced. Removing it makes the label the single
    authorization, which is the model.
  - **Or repoint it** at the DigitalOcean lane, keeping push-to-main deploys and
    accepting two authorization models for one service.
  The first is consistent with the model; the second needs an argument.
- Keep the preflight either way. `production-preflight` runs typecheck, tests
  with coverage, and the frontend build on every push to `main` — that is
  valuable independently of deployment and should not be deleted along with the
  deploy job.
- Do the same analysis for `deploy-staging.yml` and the label-gated
  `deploy-testing.yml`, both of which call the same legacy module.
  `services-knoxx-staging-migration` owns where they end up pointing; this card
  owns not leaving them aimed at a retired host in the meantime.
- Verify after: a push to `main` produces no deploy to 104.130.159.19, and a
  `deploy`-labelled merge still reaches DigitalOcean and passes `verify.sh`.

## Definition of Done

- Knoxx has exactly one production deploy path, and it targets the host its
  hostname resolves to.
- The choice between removing and repointing is recorded with its reasoning.
- The preflight still runs on every push to `main`.
- Staging and testing workflows are not left pointing at a retired host.
- Verified by an actual merge: no legacy deploy fires, the intended path does.
