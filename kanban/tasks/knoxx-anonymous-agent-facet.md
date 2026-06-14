---
uuid: "knoxx-anonymous-agent-facet"
title: "Anonymous Agent Facet for start-agent-session"
status: pending
priority: "P2"
labels: ["tasks", "5sp", "contract-runtime-deployment"]
created_at: "2026-06-10T00:00:00Z"
source: "docs/design/resource-architecture.md"
points: 5
category: "tasks"
---
# Anonymous Agent Facet for start-agent-session

> Parent epic: `knoxx-contract-runtime-deployment`

## Context

The grammar's ownership rule: `:agent/*` fields without `:agent/id` on a
trigger declare an anonymous agent owned by that trigger. The loader already
records `:resource/anonymous-facets`; no interpreter consumes the agent facet
yet.

## Work

1. `:actions/start-agent-session` reads an inline agent spec from the owning
   resource (`:agent/model`, `:agent/prompts`, `:agent/role`) when
   `:trigger/with` has no `:agent-id`.
2. **Capability gating designed first**: anonymous agents must not escalate —
   they inherit at most the owning trigger's actor capabilities; no
   capability or role grants beyond the actor baseline.
3. Deprecated-fallback warnings preserved (`error-observatory`).
4. Tests: inline facet spawns; gating enforced; `:agent-id` reference wins
   when both are present.

## Definition of Done

- [ ] Inline `:agent/*` facet starts a session without a registered agent
- [ ] Capability gating enforced and tested
- [ ] Tests pass

## Risks

- Security: anonymous agents bypassing contract resolution — gate first
