---
uuid: services-staging-slot-pattern
title: Services — what a staging slot is on a single host
status: ready
priority: P1
points: 3
labels:
  - tasks
  - deployment
  - lifecycle
  - has-parent
created_at: "2026-09-03T00:00:00Z"
category: tasks
---

# Services — what a staging slot is on a single host

> Parent epic: `knoxx-translated-publication-to-website`
> Repository: `open-hax/services`

## Purpose

> **Superseded in part, 2026-09-16.** A testing/staging controller now exists:
> `services#83` ("Add per-service HTTPS environments and gated promotion") adds
> `.github/workflows/deploy-service-environment.yml`, which admits only
> `testing|staging` for `knoxx|axxium` and deploys a per-PR environment. It is
> **not on `main`** — the PR is open. Knoxx `main` nevertheless already calls it:
> `#306` shipped `.github/workflows/environment-promotion.yml` pinned to
> `deploy-service-environment.yml@f9bfe172`, a commit on that unmerged branch.
>
> So this card is no longer "design staging from nothing". What remains is
> reviewing and landing `services#83`, and closing the pin hazard below.

Staging on the removed lane was a second set of compose projects on one machine —
`proxx-staging`, `knoxx-staging`, `openplanner-staging`, `axxium-staging`, each
with its own runtime root, project name, published port and `staging-` hostname.
That shape is worth carrying across rather than reinventing per service, and it
must exist before the promotion rule can be enforced: rule 3 requires a staging
deployment record to compare production against, and `services#44` measured
`main` at seventy commits ahead of the last thing staging ever saw.

Establish the pattern once; `services-knoxx-staging-migration` and
`services-proxx-staging-migration` then apply it.

## Dependencies

`services-caddy-hostname-scale-decision` — a staging slot per service roughly
doubles the hostname count, which is most of why that decision needs re-making.

## Work

- Define the slot in the service descriptor rather than as a parallel directory:
  one service definition, a phase parameter, and everything phase-varying derived
  from it — compose project name, state path under `stateRoot`, published port,
  and hostname. Two directories per service is two things to keep in sync.
- Name the collision hazards explicitly, because the old lane hit them: bare
  service aliases collide across projects on a shared Docker network — the reason
  `promethean/services.yaml` records that federation nginx must route to
  project-specific container names. Every container name and network alias must
  carry the phase.
- Decide the database posture per phase and write it down. `services#44` lists
  this as the question that shapes everything else and could not decide it:
  ephemeral-per-phase is honest and the most work; shared makes phases interfere.
- Confirm the deployment record is produced. GitHub populates the Deployments API
  for any job declaring `environment:`, so the staging record the promotion rule
  reads already exists for free — verify that against a real staging deploy
  rather than assuming it.
- State what staging is *for* per service. A static site with no backend has
  little to stage; that is an argument for a cheap slot, not for skipping the
  record the promotion rule needs.

## Definition of Done

- A service declares a staging phase by parameter, not by a duplicate directory.
- Container names, aliases, ports, state paths and hostnames all carry the phase.
- The database posture per phase is recorded.
- A staging deploy produces a Deployments API record the promotion check can read.

## The pin hazard this exposed

`knoxx` `main` depends on a reusable workflow revision that has never been on
`services` `main`:

```text
knoxx  .github/workflows/environment-promotion.yml
  uses: open-hax/services/.github/workflows/deploy-service-environment.yml@f9bfe172
                                                                            |
  f9bfe172 "Add isolated testing and staging deployment controller"          |
  reachable only from services branch codex/knoxx-nested-https  <------------+
  (services#83, open; branch head is 31cd6dc8, i.e. NEWER than the pin)
```

It resolves today, because GitHub accepts any SHA reachable in the repository
and branch commits are reachable. Three things follow, and none is theoretical:

1. **Knoxx promotion runs an unreviewed controller.** The pinned revision has
   not passed `services`' own merge gate. This is the same class of problem
   `services#45` fixed by deriving deploy authorization from the frozen
   merge-time payload rather than from mutable state.
2. **It runs an *older* controller than the PR's own head.** `f9bfe172` is
   behind `31cd6dc8`, so review findings already fixed on the branch are not
   what Knoxx executes.
3. **It breaks if the branch goes away.** Closing `services#83`, force-pushing
   the branch, or deleting it after a squash-merge can make `f9bfe172`
   unreachable, and Knoxx's testing/staging promotion stops resolving.

The fix is ordering, not cleverness: land `services#83`, then repin
`environment-promotion.yml` to the resulting `main` commit. Until that happens,
this is a live cross-repo dependency on an open pull request.
