---
uuid: "knoxx-knowledge-ops-federated-lakes"
title: "Knowledge Ops — Federated Lakes Spec"
status: icebox
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.384Z"
source: "specs/epics/knowledge-ops-federated-lakes.md"
points: null
category: epics
---
# Knowledge Ops — Federated Lakes Spec

> Source: `specs/epics/knowledge-ops-federated-lakes.md`

> *`devel` is not the only lake. The system must speak to the lakes already alive in the ecosystem.*

---

## Purpose

Define the federated lake model for the platform. The system should not only query the new `devel-*` lakes created for workspace self-management; it should also be able to query the existing OpenPlanner-backed lakes already in use by the wider Promethean ecosystem.

This makes the platform real:
- it is not a greenfield toy
- it sits on top of live operational data
- it proves the platform can unify multiple working systems

---

