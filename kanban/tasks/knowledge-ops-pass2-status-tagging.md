---
uuid: "knoxx-knowledge-ops-pass2-status-tagging"
title: "Knowledge-ops consistency pass 2: status tagging"
status: incoming
priority: P2
labels: ["tasks", "2sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 2
category: tasks
---

# Knowledge-ops consistency pass 2: status tagging

> Parent epic: `knoxx-knowledge-ops-consistency-review`
> Points: 2

## Purpose

Ensure every `knowledge-ops-*.md` spec has a valid, consistent `status` frontmatter field drawn from the project's defined vocabulary, eliminating blanks, misspellings, and ad-hoc values.

## Scope

- Read the `status:` field in all 34 spec files under `orgs/open-hax/knoxx/specs/`
- Map any non-standard values to the nearest valid status (`icebox`, `incoming`, `accepted`, `breakdown`, `ready`, `todo`, `in_progress`, `review`, `document`, `done`, `rejected`)
- Update files in-place with corrected values

## Definition of done

- All 34 spec files have a `status:` field set to one of the defined valid values
- No spec has a blank, null, or unrecognised status value

## Notes

Split from parent epic `knoxx-knowledge-ops-consistency-review` on 2026-05-30.
