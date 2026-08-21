---
uuid: knoxx-publication-locale-catalog
title: Publication — declare which locales a target accepts
status: ready
priority: P2
points: 2
labels:
  - tasks
  - publication
  - translations
  - contracts
  - has-parent
---

# Publication — declare which locales a target accepts

> Parent epic: `knoxx-translated-publication-to-website`

## Purpose

Nothing constrains the locale on a publication intent. A target that renders a
navigation shell, a language switcher, and a set of routes can only serve locales
it was built to serve, and discovering an unsupported locale at materialization
time means either a broken page or a silently dropped publication.

## Dependencies

`knoxx-publication-artifact-contract`. Consumed by
`knoxx-publication-static-site-target` and by the website's locale routing.

## Work

- Declare the accepted locale set as a property of the publication target
  resource, not as a global. Two targets may legitimately accept different sets.
- An intent for an unaccepted locale is a blocker with the locale and the target
  named, computed by the gate alongside the translation and review blockers, and
  therefore visible before any effect runs.
- Blocked, not dropped. An unsupported locale must appear in the CMS projection
  as a blocker a person can act on.
- The source locale comes from the document, never defaulted — the gate already
  refuses to default source language and this card does not introduce one.
- Use one locale representation throughout and pin it. Ad-hoc case and separator
  variants of the same locale are two identities to every receipt lookup and
  every served path.

## Definition of Done

- Accepted locales are a resource fact on the target.
- An unaccepted locale is a named blocker, never a partial publish.
- The blocker is visible in the CMS projection.
- Locale representation is canonical and asserted at the boundary.
