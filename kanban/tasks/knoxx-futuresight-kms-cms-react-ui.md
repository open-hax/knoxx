---
uuid: "knoxx-futuresight-kms-cms-react-ui"
title: "Build Layer 2 CMS React UI components for futuresight-kms"
status: breakdown
priority: P2
labels: ["tasks", "4sp", "has-parent"]
created_at: "2026-05-30T00:00:00Z"
points: 4
category: tasks
---
# Build Layer 2 CMS React UI components for futuresight-kms

> Parent epic: `knoxx-knowledge-ops-chat-widget-layers`
> Points: 4

## Purpose

Implement the knowledge-worker-facing CMS dashboard (Layer 2) as React components so content can be curated, AI-drafted, and published to the public corpus through a controlled UI boundary.

## Scope

Create the following files in `packages/futuresight-kms/frontend/components/`:

- `CmsDashboard.tsx` — full-page CMS layout: left nav (Drafts, Review, Public, Internal, Archive), content library table with visibility-filter tabs (`[All] [Internal] [Review] [Public]`), and bulk-action toolbar (`[Draft with AI]`, `[Move to Review]`, `[Publish]`); fetches from `GET /api/cms/documents`
- `CmsDraftAssistant.tsx` — right-panel AI draft assistant: topic input, tone selector, audience selector, sources selector (internal docs / existing FAQ), `[Generate Draft]` button calling `POST /api/cms/draft`; renders streaming draft output with `[Edit]`, `[Send to Review]`, `[Publish Directly]` actions
- `CmsContentRow.tsx` — single document row: title, visibility badge (colour-coded by state), source indicator (`manual` / `ai-drafted` / `ingested`), relative timestamp, and per-row action menu

All components must use `@open-hax/uxx` and `@open-hax/uxx/tokens` — no hardcoded colours or bespoke design tokens.

## Definition of done

- `CmsDashboard` renders a filterable content list from the CMS API; filter tabs correctly constrain the `GET /api/cms/documents` query param
- `CmsDraftAssistant` calls `POST /api/cms/draft` and displays the returned draft; the `[Publish Directly]` action calls `POST /api/cms/publish/{id}` and updates the row visibility badge without a full page reload
- `CmsContentRow` visibility badge renders four distinct visual states for `internal`, `review`, `public`, and `archived` using `@open-hax/uxx/tokens` semantic colour tokens
- All three components pass TypeScript type-check (`pnpm typecheck` in frontend) and stay within the 350-line soft file-size budget

## Notes

Split from parent epic `knoxx-knowledge-ops-chat-widget-layers` on 2026-05-30.
