---
uuid: "knoxx-knowledge-ops-pass1-path-canonicalization"
title: "Knowledge-ops consistency pass 1: path canonicalization"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Knowledge-ops consistency pass 1: path canonicalization

> Parent epic: `knoxx-knowledge-ops-consistency-review`
> Points: 3

## Purpose

Audit all 34 `knowledge-ops-*.md` specs and update any remaining cross-references that still point at `specs/drafts/knowledge-ops-*.md` instead of the canonical `orgs/open-hax/knoxx/specs/` location.

## Scope

- Grep all spec files under `orgs/open-hax/knoxx/specs/` for `specs/drafts/knowledge-ops-` references
- Replace stale draft paths with canonical paths in each affected file
- Verify the `source:` frontmatter field in each spec points to the correct canonical path

## Definition of done

- No spec file under `orgs/open-hax/knoxx/specs/` contains a reference to `specs/drafts/knowledge-ops-`
- All `source:` frontmatter fields reflect the `orgs/open-hax/knoxx/specs/` location

## Notes

Split from parent epic `knoxx-knowledge-ops-consistency-review` on 2026-05-30.
