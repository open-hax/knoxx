---
uuid: "knoxx-knowledge-ops-product-line-exposure-monitor-specs"
title: "Exposure Monitor spec authoring (exposure-monitor-*.md)"
status: icebox
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---
# Exposure Monitor spec authoring (exposure-monitor-*.md)

> Parent epic: `knoxx-knowledge-ops-product-line`
> Points: 3

## Purpose

Author the missing `exposure-monitor-*.md` spec files that the parent epic explicitly flags as "to be created," covering discovery, verification, contact resolution, lead management, and outreach components of the Exposure Monitor product.

## Scope

- Create `specs/exposure-monitor-discovery.md` — Shodan queries, Tor probing, masscan integration
- Create `specs/exposure-monitor-verification.md` — host probing for GPU specs, model lists, latency, geo
- Create `specs/exposure-monitor-contact-resolution.md` — RDAP lookups, security.txt parsing, TLS cert extraction
- Create `specs/exposure-monitor-lead-management.md` — scoring, clustering, approval, export workflows
- Create `specs/exposure-monitor-outreach.md` — campaign clusters by ASN/region/org
- Each spec must follow the established `knowledge-ops-*.md` format and live alongside existing specs in the knoxx specs directory

## Definition of done

- All five `exposure-monitor-*.md` spec files are written and committed under the knoxx specs directory
- Each spec contains Purpose, Architecture, Data Model, API/Interface, and Open Questions sections consistent with existing knowledge-ops specs
- The parent epic's "Specs: `exposure-monitor-*.md` (to be created)" note can be updated to reference the real file list

## Notes

Split from parent epic `knoxx-knowledge-ops-product-line` on 2026-05-30.
