---
uuid: website-manifest-contract-tests
title: Website — assert the manifest contract in the reader's own tests
status: ready
priority: P2
points: 3
labels:
  - tasks
  - website
  - testing
  - has-parent
---

# Website — assert the manifest contract in the reader's own tests

> Parent epic: `knoxx-translated-publication-to-website`
> Repository: `open-hax/website`

## Purpose

A writer and a reader that only run together against live infrastructure drift
unseen. That is the recorded diagnosis behind Knoxx's own layer-enforcement
work — eight defects from getting `/mcp` working, five of them the same shape,
every one an undeclared boundary. The publication manifest is a new instance of
exactly that shape: written by Knoxx CLJS, read by website CLJS, with a
filesystem and a deploy in between and no shared compile.

## Dependencies

`website-published-content-source`.

## Work

- Declare the manifest shape the website depends on, in the website repo, as the
  reader's stated expectation rather than a copy of the writer's schema.
- Test against fixtures committed here: a manifest with one locale, with several,
  empty, and absent.
- Assert the failure modes explicitly, not just the happy path: an unknown field
  is ignored rather than fatal; a missing required field fails loudly rather than
  rendering a blank page; a locale with no entries is not offered.
- Run in the website's existing `shadow-cljs` `:test` build, which is already
  wired with `:autorun` — no new test infrastructure.
- Keep the fixtures small enough to read. A fixture nobody reads is a second
  implementation nobody maintains.
- Cross-reference the writer: the fixtures' provenance is a comment naming the
  Knoxx namespace that produces them, so the next person knows where to look
  when they diverge.

## Definition of Done

- Manifest fixtures live in the website repo and its test build asserts against
  them.
- Absent, empty, single-locale and multi-locale manifests each have a stated
  expected rendering.
- A required-field omission fails a test rather than a page.
- The tests run in CI on every website PR.
