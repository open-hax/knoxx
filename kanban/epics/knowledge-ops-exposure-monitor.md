---
uuid: "knoxx-knowledge-ops-exposure-monitor"
title: "Exposure Monitor — Product Spec"
status: "icebox"
priority: "P2"
labels: ["epics"]
created_at: "2026-05-28T22:40:14.384Z"
source: "specs/epics/knowledge-ops-exposure-monitor.md"
points: null
category: "epics"
---

# Exposure Monitor — Product Spec

> Source: `specs/epics/knowledge-ops-exposure-monitor.md`

> *Find exposed AI. Resolve contacts. Generate outreach. Defend the perimeter.*

---
## Purpose

Define the Exposure Monitor product — an external AI infrastructure exposure detection and lead generation platform built on the existing `our-gpus` codebase.
---

## What It Is

A platform that:
1. **Discovers** exposed AI endpoints across the internet (Ollama, OpenAI-compatible proxies, custom inference servers)
2. **Verifies** each endpoint (GPU specs, model lists, latency, geo data)

---
Triage 2026-05-29: This is a multi-subsystem epic product spec (Discovery + Verification + Contact Resolution + Lead Management + Outreach) with existing working code in `orgs/shuv/our-gpus/` and written specs, but far exceeds 5sp as a single unit — it needs breakdown into bounded subtasks before any implementation can begin. Verdict: icebox (P2). Epic label already present; split into ≤5sp incoming subtasks covering each engine/subsystem to unblock real work. --tasks-dir orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
