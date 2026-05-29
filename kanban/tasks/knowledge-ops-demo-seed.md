---
uuid: "knoxx-knowledge-ops-demo-seed"
title: "Knowledge Ops — Demo Seed Spec"
status: incoming
priority: P2
labels: ["tasks"]
created_at: "2026-05-28T22:40:14.397Z"
source: "specs/tasks/knowledge-ops-demo-seed.md"
points: null
category: tasks
---

# Knowledge Ops — Demo Seed Spec

> Source: `specs/tasks/knowledge-ops-demo-seed.md`

> *One script turns the devel workspace into a working tenant with populated collections and a review queue.*

---

## Purpose

Define what `seed_devel_tenant.py` creates so the devel workspace demonstrates all five layers end-to-end.

---

## What the Seed Script Does

### Step 1: Create the `devel` tenant

```python
