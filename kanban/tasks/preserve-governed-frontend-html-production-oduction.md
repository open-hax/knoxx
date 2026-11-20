---
uuid: "knoxx-369-html-production"
title: "Preserve governed frontend HTML production"
status: "incoming"
type: "task"
priority: "P1"
labels: "frontend, migration, html"
parent: "knoxx-frontend-helix-migration-vite-retirement"
dependency: []
category: "tasks"
write-id: "1793662291414-0.6hxqxrszqhtnrufm9yt"
created_at: "2026-11-02T23:31:31.414Z"
---

# Preserve production HTML ownership

## Outcome

The governed frontend application cannot omit its authored HTML entry from a production build while the migration inventory is accepted.

## Scope

Destination: frontend migration inspection and its ClojureScript tests. Parent epic: frontend Helix migration and Vite retirement.

## Non-goals

Changing retired generic configurations, authentication, or live deployment.

## Acceptance criteria

- The real application build accepts its HTML phase before Shadow and fails when that phase is removed or reordered.
- Removing the governed application HTML entry is rejected by the actual inventory entry point.
- Generic retired and no-HTML fixtures remain accepted.

## Verification

Run the frontend ClojureScript suite and migration inventory checks; disclose failures and the separate pending browser tour.
