---
uuid: "big-pi-optimizely-review-publication"
title: "Connect Mu review evidence and publication boundaries"
status: incoming
priority: P2
labels: ["tasks", "has-parent", "cms", "optimizely"]
created_at: "2026-11-18T00:00:00Z"
points: 0
category: tasks
---
# Connect Mu review evidence and publication boundaries

> Parent epic: `big-pi-optimizely-compatibility`

As a technical reviewer, I can trace a translation to the selected source and make a review decision that publication can evaluate without treating an ordinary edit as approval.

**Acceptance**

- Bind source key, version, locale, ETag and payload to the resulting Knoxx revision and existing Mu review/terminology evidence; do not create a second review store or fabricate an approval.
- Return only a draft after exact source and current unconflicted revision checks; surface stale sources, concurrent edits and uncertain destination writes without automatic retry. State the remaining cross-system race.
- Specify and implement publication/delivery as separately authorized effects with auditable intent, policy and receipts; exercise denial and verify draft workflows never alter existing publication intent.

**Evidence**: live Knoxx round-trip plus authorized integration fixtures covering source drift, review decisions, conflict, denial, draft-only writes and publication gating; human-run verification artifact.
