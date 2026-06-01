---
uuid: "knoxx-futuresight-kms-chat-widget"
title: "Implement Layer 1 public chat widget for futuresight-kms"
status: incoming
priority: P2
labels: ["tasks", "3sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 3
category: tasks
---

# Implement Layer 1 public chat widget for futuresight-kms

> Parent epic: `knoxx-knowledge-ops-chat-widget-layers`
> Points: 3

## Purpose

Deliver the customer-facing floating chat widget (Layer 1) for the futuresight-kms platform, scoped exclusively to the curated public knowledge corpus so internal documents are never exposed.

## Scope

Create the two files specified in the epic:

- `packages/futuresight-kms/frontend/components/ChatWidget.tsx` — floating button (bottom-right), expandable chat panel with suggested prompts, message input, and streaming response display; queries `Ragussy /api/ragussy/chat` filtered to the `public_docs` Qdrant collection; uses `@open-hax/uxx` and `@open-hax/uxx/tokens` for all styling primitives
- `packages/futuresight-kms/frontend/components/ChatWidget.css` — widget-specific styles (z-index layering, open/closed animation, panel dimensions)

The widget must be embeddable in `services/futuresight-kms/config/html/index.html` and `services/portal/index.html`.

## Definition of done

- `ChatWidget.tsx` renders a floating button that opens a chat panel; component mounts without TypeScript errors
- Submitting a query hits `Ragussy /api/ragussy/chat` with the `public_docs` collection scoped; widget never queries `devel_docs` or `devel_specs`
- Suggested quick-action prompts render in the welcome state (at minimum: "What services do you offer?", "Book a consultation", "How does pricing work?")
- Widget styles use `@open-hax/uxx/tokens` CSS custom properties; no hardcoded colour values
- Both files pass the repo file-size budget check (`node scripts/lint-file-sizes.mjs` — warn 350, error 500 lines)

## Notes

Split from parent epic `knoxx-knowledge-ops-chat-widget-layers` on 2026-05-30.
