---
uuid: services-openplanner-lane-disposition
title: Services — OpenPlanner, migrate or retire
status: ready
priority: P1
points: 5
labels:
  - tasks
  - deployment
  - migration
  - openplanner
  - has-parent
---

# Services — OpenPlanner, migrate or retire

> Parent: `services-promethean-lane-retirement`
> Repository: `open-hax/services`

## Purpose

The first disposition to settle, because it changes what several other children
need. Right now the constellation says three contradictory things about
OpenPlanner:

- `digitalocean/hosts/production.yaml` lists `openplanner` in `roles` — with **no**
  `digitalocean/services/openplanner/` directory behind it.
- `deploy-stack.yml` records its HTTP service as *deliberately* absent: Knoxx runs
  the data plane in-process through `@open-hax/openplanner-sdk` straight against
  Atlas, so chat, sessions and vector search need no OpenPlanner service. "Graph,
  translation and document-stats operations still delegate to REST and degrade
  until one is deployed."
- `promethean/services.yaml` defines `openplanner-production` and
  `openplanner-staging` on the old host.

A declared role with no service behind it reads as deployed to anyone checking
the inventory, and is not.

## Work

- Establish which REST operations still have live callers. `law.publication-surface`
  already names the known ones with their exact caller files —
  `/api/openplanner/v1/gardens` from the Gardens page, `/api/openplanner/v1/cms/publish`
  from `CmsPage.tsx` — and asserts that list is *exactly* the callers, so it is a
  starting inventory rather than a guess. Confirm it still holds.
- Decide, and write down which:
  - **Migrate** — add `digitalocean/services/openplanner/` with compose, env
    template, `verify.sh`, and an image in `build-images.yml` (which already lists
    `openplanner` as a buildable service). Then the health gate stops degrading.
  - **Retire** — remove `openplanner` from `roles`, and hand the remaining callers
    to `knoxx-gardens-openplanner-rest-decoupling` as the blocking work.
- Whichever way: `OPENPLANNER_API_KEY` is still in Knoxx's required env because
  the Gardens page proxies through the backend. `services#47` deliberately kept
  it and said why. It leaves only when the last caller does.
- Update `ROADMAP.md` §1, which predicts the flag and the key retire together.
  They did not.

## Definition of Done

- One disposition is recorded with the caller evidence behind it.
- If migrated: a service directory, a gate, and a role that matches reality.
- If retired: the role is gone, and every remaining caller is named with the card
  that owns it.
- The three contradictory statements are reconciled to one.
