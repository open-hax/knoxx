---
uuid: services-website-content-root
title: Services — declare the published content root and its single writer
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

# Services — declare the published content root and its single writer

> Parent epic: `knoxx-translated-publication-to-website`
> Repository: `open-hax/services`

## Purpose

Deciding where published bytes land is a deployment decision, not an application
one, and it is the decision the static-site adapter's transport depends on. It
must be answered before that adapter is written, not discovered while writing it.

## The question that decides the epic

Knoxx production runs on the Promethean host (`proxx.promethean.rest`).
`services#19` proposes the website on that same host, but the deployment model
says new services are declared for the DigitalOcean lane, which is a **different
host**. Those give different adapters:

```text
same host       -> a shared directory, read-only to the reader; a filesystem adapter
different hosts -> object storage or an SSH push; a transport adapter, more work,
                   and the artifact write is no longer atomic-by-rename
```

Pick one and record why. Everything in `knoxx-publication-static-site-target`
follows from it.

## Dependencies

`services:docs/deployment-model.md` §4 and §7 — this card is where §7's first
open question is answered.

## Work

- Answer the host question above and record the decision with its consequences.
- Declare the content root in the service descriptor: path, owner, single writer,
  and read-only reader mount.
- Place it under the host contract's `stateRoot`. It is state, not build output:
  a website redeploy that `rsync --delete`s its docroot must not be able to erase
  published translations. This is why the model separates `build.output` from
  `serve.docroot`.
- State the permissions concretely — which uid writes, which mounts read-only —
  rather than leaving it to whichever process gets there first.
- Bound the disk. Say what happens when publication fills the volume, even if the
  answer is an alert and a manual sweep.
- Back it up, or state explicitly that published content is reproducible by
  re-running reconciliation and therefore is not backed up.
- No secrets. This repo is public and the content root's declaration carries
  names and paths only.

## Definition of Done

- The host decision is recorded with its consequence for the adapter.
- The content root is declared with one writer and a read-only reader.
- It lives under `stateRoot` and survives a website redeploy.
- Disk bound and recovery posture are both stated.
