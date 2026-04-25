# Skill: Sing the songs of your people (repo-lore synthesis)

Goal: produce a *beautiful but truthful* synthesis of the workspace's existing lore.

This skill is intentionally tool-agnostic. It can be executed with only `read` + `bash` (for searching) + `write` (to output a report).

## Inputs

- A scope (one of):
  - a package path (recommended), e.g. `orgs/open-hax/openplanner/packages/agents/knoxx`
  - a list of files
  - a time window (“last 2 weeks”)

## Evidence-first protocol

1) Gather anchors:

```bash
rg -n "receipts|decision|dod|manifest" <SCOPE>
rg -n "TODO|FIXME|NOTE:" <SCOPE>
git -C <SCOPE> log -n 40 --oneline
```

2) Read the top 5–10 most relevant files:

- `README.md`, `docs/**`, `specs/**`
- receipts / decisions logs
- a representative slice of core code

3) Only then write the synthesis.

## Output format (recommended)

- “Motifs” (recurring ideas / constraints)
- “Characters” (modules/agents/services that keep showing up)
- “Rituals” (workflows, scripts, deployment patterns)
- “Tensions” (tradeoffs and known pain)
- “Oaths” (invariants the code seems to defend)

## Rules

- Do not invent history.
- If you make an inference, label it as inference and point to the file(s) that suggest it.
- Prefer quoting short snippets (≤ 8 lines) over paraphrasing when precision matters.
