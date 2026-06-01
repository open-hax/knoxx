---
uuid: "knoxx-knowledge-ops-chat-ui-library"
title: "Knowledge Ops — Shared Chat UI Library Spec"
status: "icebox"
priority: "P2"
labels: "["epics"]"
created_at: "2026-05-28T22:40:14.381Z"
source: "specs/epics/knowledge-ops-chat-ui-library.md"
points: null
category: "epics"
---

# Knowledge Ops — Shared Chat UI Library Spec

> Source: `specs/epics/knowledge-ops-chat-ui-library.md`

> *One library, five layers. Configure by layer, don't reimplement per layer.*

---
## Problem

Chat UI code is duplicated across 4+ implementations with incompatible types, different streaming approaches, and no shared components. Every layer would add another copy of the same bugs.

| Implementation | Framework | Streaming | Source Citations | Duplicated patterns |
|---------------|-----------|-----------|-----------------|-------------------|
| Ragussy ChatPage.tsx | React + Tailwind | REST only | Collapsible cards | Message bubbles, composer, error handling |
| Ragussy ChatLabPage.tsx | React + Tailwind | WebSocket (`ws.ts`) | None | Message bubbles, composer, error handling |
| Shibboleth ChatLab.tsx | React + CSS | Polling (2.5s interval) | None | Message bubbles, error handling |
|

Triage 2026-05-29: Well-specified epic with clear problem (4+ incompatible chat UI duplications across Ragussy, Shibboleth, futuresight-kms, fork-tales), a concrete solution (shared @workspace/chat-ui package with unified types, useChat hook, pluggable transports, and ChatPanel composition), a files-to-create list, and a three-phase migration path. No external blockers — all dependencies are internal (@open-hax/uxx, react peer). Ready to proceed to breakdown to split into implementable child tasks. Verdict: accepted (P2). --tasks-dir /home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx/kanban
---
