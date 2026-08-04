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
# Hierarchical tool grants on the MCP consent page

> Parent epic: `knoxx-decouple-into-katamorph-contracts`

## Purpose

The consent page lists every tool individually. A real authorization carried ~60
checkboxes in one flat list (observed in the authorize/confirm query on
2026-08-04), which makes granting or revoking a capability tedious and makes the
page a poor description of what is being granted.

## The model: a hierarchy, like GitHub permissions

Tick one top-level box for `discord` and every discord tool is granted. Not a
flat list with a select-all convenience — an actual hierarchy, where the parent
is the thing granted and the children are its expansion.

The shape for this already exists upstream and should be adopted rather than
invented. `katamorph.schema/CapabilityContract`:

```clojure
{:cap/id keyword? :cap/tools [any] :cap/user-surfaces [UserSurface]}
```

`:cap/id` is a **keyword**, so namespacing gives the hierarchy for free:
`:cap/id :discord` expands to `:discord/send`, `:discord/read`, … and an actor
holds `:actor/capabilities [:discord :bluesky]`. Granting a parent means
granting the namespace.

That also means the grant page stops being a tool list and becomes a projection
of capabilities — which is what makes it data rather than markup.

## Scope

- Namespace the tool ids under capability parents: discord, bluesky,
  translations, sandbox, memory/graph, voice, web, files.
- Grant at any level; a parent grant expands to its children at token-mint time,
  not at render time, so a tool added later is covered by an existing grant.
- Store the grant as the capability set, not the expanded tool list. Storing the
  expansion is what makes a grant go stale.
- Derive the grouping from **data**, not a hand-maintained list in the HTML.
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
- **`capability-schema-reconciliation`** in *eta-mu* (status: **ready**) —
  reconciles muse's capability shape with katamorph's `CapabilityContract`; its
  blueprint holds that *"a capability is the primitive."* Tool groups **are**
  capabilities, so grouping before that decision lands would define a third
  competing capability shape — exactly the drift the upstream P0 cutover exists
  to stop. **Blocked on it.**

