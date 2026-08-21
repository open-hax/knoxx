---
uuid: services-website-content-root
title: Services — declare the published content root on the DigitalOcean host
status: ready
priority: P1
points: 3
labels:
  - tasks
  - deployment
  - publication
  - website
  - has-parent
---

# Services — declare the published content root on the DigitalOcean host

> Parent epic: `knoxx-translated-publication-to-website`
> Repository: `open-hax/services`

## Purpose

Deciding where published bytes land is a deployment decision, and it is the one
the static-site adapter's transport depends on. It is now answered: everything
deploys to DigitalOcean, so Knoxx and the website are compose projects on one
host and the content root is a directory both mount.

## The decision, and what it buys

```text
host    open-hax-services-production (157.245.125.134)
root    /srv/open-hax/state/website/content
writer  knoxx     (read-write)
reader  website   (read-only)
```

A single filesystem means the adapter writes files and renames them into place.
Rename within a filesystem is atomic, so the manifest swap the adapter needs is a
primitive rather than a protocol — no object store, no transport, no credentials,
and no partially visible manifest. This is why the adapter card can be sized as
one adapter rather than an adapter plus a transport.

## Dependencies

`services:docs/deployment-model.md` §4. Blocks
`knoxx-publication-static-site-target`.

## Work

- Declare the content root in the website service descriptor: path, single
  writer, read-only reader mount, and the uid each container uses. State the uids
  rather than leaving ownership to whichever container creates the directory
  first — the writer is Knoxx's backend container and the reader is nginx.
- Place it under the host contract's `stateRoot`
  (`/srv/open-hax/state`, per `digitalocean/hosts/production.yaml`). It is state:
  a website release replaces an image and must not be able to replace published
  translations. This is why the model separates `image.output` from the content
  mount.
- Add the read-write mount to the Knoxx compose project and the read-only mount
  to the website's. One writer, enforced by the mount, not by convention.
- Create the directory in host bootstrap with the right ownership, so a first
  deploy does not race to create it.
- Bound the disk. Say what happens when publication fills the volume, even if the
  answer is an alert and a manual sweep.
- Back it up, or state explicitly that published content is reproducible by
  re-running reconciliation and is therefore deliberately not backed up.
- No secrets. This repo is public and the declaration carries names and paths
  only.

## Definition of Done

- The content root is declared with one read-write writer and read-only readers,
  with uids stated.
- It lives under `stateRoot` and survives replacing the website image.
- The mounts exist in both compose projects and the reader's is `:ro`.
- Host bootstrap creates it with correct ownership.
- Disk bound and recovery posture are both stated.
