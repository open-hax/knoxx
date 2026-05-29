---
uuid: "knoxx-folder-backed-visual-cms-design-spec"
title: "Folder-Backed Visual CMS Design Spec"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.379Z"
source: "specs/epics/folder-backed-visual-cms-design-spec.md"
points: null
category: epics
---

# Folder-Backed Visual CMS Design Spec

> Source: `specs/epics/folder-backed-visual-cms-design-spec.md`

Status: draft  
Owner: Knoxx / OpenPlanner CMS / Broadcast Studio  
Created: 2026-05-07  
Related:
- `specs/epics/broadcast-studio-playlist-publication-and-block-cms.md`
- `specs/epics/knowledge-ops-cms-data-model.md`
- `specs/epics/knowledge-ops-gardens.md`
- `contracts/agents/page_defaults/cms_default.edn`
- `contracts/actors/cms_chat.edn`

---

## Purpose

Define the architecture, file structure, data model, and UI/UX for a folder-backed visual CMS where `view-contract.edn` is the human-readable source of truth, the page is a folder on disk, and the editing UI is a visual page builder over that contract.
