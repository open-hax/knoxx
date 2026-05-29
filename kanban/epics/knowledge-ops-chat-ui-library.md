---
uuid: "knoxx-knowledge-ops-chat-ui-library"
title: "Knowledge Ops — Shared Chat UI Library Spec"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.381Z"
source: "specs/epics/knowledge-ops-chat-ui-library.md"
points: null
category: epics
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
