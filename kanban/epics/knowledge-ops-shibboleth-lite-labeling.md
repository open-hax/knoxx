---
uuid: "knoxx-knowledge-ops-shibboleth-lite-labeling"
title: "Knowledge Ops — Shibboleth-Lite Labeling Workflow Spec"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.392Z"
source: "specs/epics/knowledge-ops-shibboleth-lite-labeling.md"
points: null
category: epics
---

# Knowledge Ops — Shibboleth-Lite Labeling Workflow Spec

> Source: `specs/epics/knowledge-ops-shibboleth-lite-labeling.md`

> *Same DSL mechanics as adversarial safety labeling, repurposed for corporate knowledge QA and translation review.*

---

## Purpose

Define the labeling schema, task definitions, and review workflow for the "Shibboleth-lite" subsystem — the human-in-the-loop expert review layer that produces training-grade labeled data for the multi-tenant knowledge platform.

---

## Context

Shibboleth (https://github.com/octave-commons/shibboleth) is a Clojure-based generative DSL + pipeline for building multilingual adversarial prompt evaluation datasets. It defines attack taxonomies, harm categories, transforms, and pipelines as first-class macros.

For the knowledge ops platform, we reuse the **mechanics** (DSL, MT, clustering, provenance, labeling UI) but change **semantics** from adversarial safe
