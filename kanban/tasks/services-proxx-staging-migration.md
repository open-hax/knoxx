---
uuid: services-proxx-staging-migration
title: Services — proxx staging slot on DigitalOcean
status: ready
priority: P1
points: 2
labels:
  - tasks
  - deployment
  - proxx
  - has-parent
created_at: "2026-09-03T00:00:00Z"
category: tasks
---

# Services — proxx staging slot on DigitalOcean

> Parent epic: `knoxx-translated-publication-to-website`
> Repository: `open-hax/services`

## Purpose

> **Superseded in part, 2026-09-16.** A testing/staging controller now exists:
> `services#83` ("Add per-service HTTPS environments and gated promotion") adds
> `.github/workflows/deploy-service-environment.yml`, which admits only
> `testing|staging` for `knoxx|axxium`. It deploys into a **shared** environment
> per service and phase, not one per pull request: the `deploy` job takes
> `environment: ${{ inputs.environment }}` verbatim and its concurrency group is
> `promethean-<service>-<environment>`, so the PR number gates admission and
> selects the source commit but does not partition the slot. It is
> **not on `main`** — the PR is open. Knoxx `main` nevertheless already calls it:
> `#306` shipped `.github/workflows/environment-promotion.yml` pinned to
> `deploy-service-environment.yml@f9bfe172`, a commit on that unmerged branch.
>
> So this card is no longer "design staging from nothing". What remains is
> reviewing and landing `services#83`, and closing the pin hazard below.

Proxx had **no staging phase at all** when this card was written — the slot went with the lane
`services#67` removed, and nothing replaced it. As with Knoxx this is a
creation, and the promotion rule needs its record.

**The new controller does not reach Proxx, and not by omission.** Its admission
step rejects the service outright:

```bash
[[ "$SERVICE" =~ ^(knoxx|axxium)$ ]]
test "$SOURCE_REPOSITORY" = "open-hax/$SERVICE"
```

So Proxx cannot be promoted through `deploy-service-environment.yml` as pinned,
and it has no caller of its own — `environment-promotion.yml` exists only in
`open-hax/knoxx` and hardcodes `service: knoxx`. Landing `services#83` therefore
resolves the Knoxx staging card and leaves this one where it was. Decide which:
extend the controller's service set and its source-repository check to admit
Proxx, or give Proxx a staging path that does not run through this controller.
That decision is a precondition of the work below, not a detail inside it.

## Dependencies

`services-staging-slot-pattern`. Sequence before `services-knoxx-staging-migration`
if staging Knoxx is to point at staging Proxx.

## Work

- Apply the staging-slot pattern to `digitalocean/services/proxx/`.
- The container-name collision is recorded history here, not a hypothetical:
  `promethean/services.yaml` notes on both proxx entries that federation nginx
  must route to project-specific container names because bare service aliases
  collide with the other phase on the shared Docker network. That was recorded
  on the removed lane's definitions; carry it forward rather than rediscovering
  it.
- Credentials are the real constraint. Proxx brokers provider access, and the
  host contract lists `REQUESTY_API_KEY` under `providerSecrets`. Decide whether
  staging holds its own provider credentials or shares production's — sharing
  means staging traffic spends production quota and appears in production
  provider logs.
- The production entry notes that Proxx should broker OpenAI OAuth through the
  localhost bridge rather than storing local refresh tokens, and
  `local-proxx-bridge` is being decommissioned. Confirm whether staging needs an
  equivalent, or whether that arrangement died with the lane that hosted it.
- `verify.sh` runs unchanged against staging.

## Definition of Done

- Proxx staging deploys to DigitalOcean and its gate passes.
- The deploy writes a Deployments API record the promotion check can read. This
  is the point of the card: rule 3 compares production against a staging record,
  and a deploy that leaves no record satisfies the mechanics without satisfying
  the rule.
- Container names and aliases carry the phase; no cross-phase collision.
- The provider-credential posture for staging is recorded.
- The bridge question is answered rather than inherited.
