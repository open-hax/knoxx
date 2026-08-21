---
uuid: website-published-content-source
title: Website — render published content and route by locale
status: ready
priority: P1
points: 8
labels:
  - tasks
  - website
  - translations
  - integration
  - has-parent
---

# Website — render published content and route by locale

> Parent epic: `knoxx-translated-publication-to-website`
> Repository: `open-hax/website`

## Purpose

`open-hax/website` has no content source and no translation of any kind. Every
string is hardcoded in `src/cljs/open_hax/website/sections/*.cljs` — the hero
copy, the section headings, the calls to action — in one language, with no locale
routing and no way for anything outside the repo to contribute content. It cannot
be a translation target until it can render content it did not compile in.

## Dependencies

`knoxx-publication-static-site-target` (produces the manifest this reads) and
`services-website-content-root` (decides where it is mounted). The manifest shape
must be agreed before this starts.

## Work

- Read the published manifest at load and render from it. The manifest is the
  authority on what exists; a file present but unlisted is not rendered.
- Serve correctly with **no** manifest and with an **empty** one. First deploy,
  and every deploy before the first publication, is that state. It must render
  the site's own static sections, not an error.
- Locale routing: a path prefix per non-default locale, the default locale at the
  root, and unknown locales resolving to the default rather than 404ing.
- Only offer locales the manifest actually carries. A language switcher listing a
  locale with no published content is worse than no switcher.
- Set `lang` on the document element per rendered locale — the shell currently
  hardcodes `lang="en"`.
- Keep hardcoded sections and published content clearly separated. The hero and
  the asset galleries are the site's own; published documents are not. Do not
  migrate the existing sections into Knoxx as part of this card.
- Static hosting means client-side routing needs an SPA fallback, and an SPA
  fallback means a bad path returns 200 with the shell. Decide deliberately
  whether published document paths should be pre-rendered instead, and record why.
- Do not fetch from Knoxx. The seam is files, so the site keeps working when
  Knoxx is down — which is the reason this target was chosen first.

## Definition of Done

- With an empty content root, the site renders exactly as it does today.
- With a manifest carrying one document in two locales, both render at their
  routes with the correct `lang`.
- The language switcher offers only published locales.
- An unknown locale prefix resolves to the default locale.
- Removing a document from the manifest stops it rendering.
- No request from the site reaches a Knoxx origin.
