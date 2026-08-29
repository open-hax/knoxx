---
uuid: "knoxx-tool-vocabulary-rename"
title: "Fix the tool vocabulary — say what each tool searches, not that it is 'semantic'"
status: incoming
priority: P3
labels: ["tasks", "3sp", "has-parent", "naming", "mcp"]
created_at: "2026-08-04T00:00:00Z"
points: 3
category: tasks
---
# Fix the tool vocabulary

> Parent epic: `knoxx-decouple-into-katamorph-contracts`

## Purpose

The tool names do not tell a caller — human or model — what they search.
"semantic" is the worst offender: `semantic_query`, `semantic_read`,
`semantic_compaction`, "semantic memory". It names an implementation technique
(embeddings) rather than a subject, and three different tools use it for three
different corpora.

An MCP client sees only the name, description and schema. The name is doing real
work here, and it is currently doing it badly.

## What each actually is, today

- `semantic_query` — vector search over the **active document corpus**
- `memory_search` — search over **prior sessions and actions**
- `memory_session` — retrieve **one conversation**
- `graph_query` — entities and edges across **lakes** (workspace, web, bluesky,
  knoxx-session)

Three of the four are "search something"; the distinction is *which corpus*, and
none of the names say so.

## Scope

- Choose names that name the subject. Sketch, not a decision:
  `documents_search`, `sessions_search`, `session_read`, `graph_search`.
- Decide the convention deliberately — `<subject>_<verb>` reads better in a tool
  list than `<verb>_<subject>`, because clients sort alphabetically and grouping
  by subject is what a user scans for.
- Keep the old names working. `sanitize-custom-tool-name` already sets
  `originalName`, so aliasing is cheap; an installed connector must not break on
  a rename.
- Fix the descriptions at the same time — they should say which corpus is
  searched in the first clause.

## Sequencing

Do this **after** `knoxx-mcp-consent-permission-groups` and the annotation table
is complete. Renaming across a boundary that has no declaration is how the
`web.read` → `web_read` annotation lookup silently broke on #218; a rename wants
the contract in place first.

## Done when

- Every advertised tool's name identifies its subject.
- Old names still resolve, with a test.
- No tool description begins by describing its algorithm.
