---
uuid: "knoxx-knowledge-ops-product-line-kanban-grooming"
title: "Groom and accept product-line subtasks into the kanban backlog"
status: icebox
priority: P2
labels: ["tasks", "1sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 1
category: tasks
---
# Groom and accept product-line subtasks into the kanban backlog

> Parent epic: `knoxx-knowledge-ops-product-line`
> Points: 1

## Purpose

Review all subtasks split from `knoxx-knowledge-ops-product-line`, assign correct priorities and story-point labels, move them to `accepted` status, and verify no overlap with existing kanban tasks across the four product areas.

## Scope

- For each of the four child tasks (`knowledge-ops-product-line-exposure-monitor-specs`, `knowledge-ops-product-line-shared-infra-scaffold`, `knowledge-ops-product-line-cross-link-roadmap`, this task): validate frontmatter, confirm points estimate, move status from `incoming` to `accepted`
- Search existing kanban tasks for overlapping work in exposure-monitor, shared-infra, and roadmap areas; flag or merge duplicates
- Confirm the parent epic `knoxx-knowledge-ops-product-line` frontmatter references the four child task UUIDs (or a `children` label list) so the board reflects the split

## Definition of done

- All four child task files have status `accepted` in their frontmatter
- No unaddressed duplicate tasks exist for exposure-monitor spec authoring or shared-infra scaffolding
- The parent epic file is updated with a `children` note or comment listing the four child UUIDs

## Notes

Split from parent epic `knoxx-knowledge-ops-product-line` on 2026-05-30.
