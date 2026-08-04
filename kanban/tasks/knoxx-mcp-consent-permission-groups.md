---
uuid: "knoxx-mcp-consent-permission-groups"
title: "Group tool permissions on the MCP consent page"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent", "mcp", "ux"]
created_at: "2026-08-04T00:00:00Z"
points: 3
category: tasks
---
# Group tool permissions on the MCP consent page

> Parent epic: `knoxx-decouple-into-katamorph-contracts`

## Purpose

The consent page lists every tool individually. A real authorization carried ~60
checkboxes in one flat list (observed in the authorize/confirm query on
2026-08-04), which makes granting or revoking a capability tedious and makes the
page a poor description of what is being granted.

## Scope

- Group tools into capability sets: Discord, Bluesky, translations, sandbox,
  memory/graph, voice, web, files.
- Group-level select/deselect, with per-tool override retained.
- Derive the grouping from **data**, not a hand-maintained list in the HTML —
  a Katamorph-style capability declaration is the target shape, so this card is
  a natural first contract.
- Show each group's risk honestly using the annotations from
  `law.mcp-tool-annotations` (read-only vs destructive vs open-world) rather
  than presenting all tools as equivalent.

## Notes

- Related: `knoxx-mcp-actor-ascription` adds actor selection to the same page.
  Sequence them so the page is restructured once.
- Only 9 of ~60 tools currently have declared annotations; the rest fall back to
  MCP's pessimistic defaults. Grouping will make that gap visible in the UI,
  which is useful pressure to finish the table.

## Done when

- Granting "all Discord tools" is one action.
- The grouping comes from a declaration, not markup.
- Revoking a group revokes every tool in it, verified by a test.

## Prior art on this board

- **`knoxx-tenant-policy-backed-tool-authz`** (status: review) — *Replace
  Hardcoded Tool Authorization with Policy-DB Check*. Grouping should read the
  same policy source that card establishes, not a second list. Land or at least
  settle that one first, or the groups will be hardcoded twice.
