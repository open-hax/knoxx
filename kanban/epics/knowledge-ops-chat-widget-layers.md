---
uuid: "knoxx-knowledge-ops-chat-widget-layers"
title: "Knowledge Ops — Platform Layer Architecture Spec"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.382Z"
source: "specs/epics/knowledge-ops-chat-widget-layers.md"
points: null
category: epics
---

# Knowledge Ops — Platform Layer Architecture Spec

> Source: `specs/epics/knowledge-ops-chat-widget-layers.md`

> *Five layers. The CMS is the boundary between public and internal. The widget is the tip of the spear.*

---

## Purpose

Define the full LLM layer stack for the futuresight-kms platform, with the chat widget scoped to Layer 1 (public assistant only), and an agent-aware CMS as the critical curation boundary between public and internal knowledge.

All UI surfaces described here use:
- `orgs/open-hax/uxx/tokens` (`@open-hax/uxx/tokens`)
- `orgs/open-hax/uxx/react` (`@open-hax/uxx`)

The widget, CMS, and workbench should share one visual language and one keyboard system.

---
