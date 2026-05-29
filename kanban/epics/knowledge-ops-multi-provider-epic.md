---
uuid: "knoxx-knowledge-ops-multi-provider-epic"
title: "The Lake — Multi-Provider Epic"
status: accepted
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.389Z"
source: "specs/epics/knowledge-ops-multi-provider-epic.md"
points: null
category: epics
---
# The Lake — Multi-Provider Epic

> Source: `specs/epics/knowledge-ops-multi-provider-epic.md`

> *One platform. Four deployment targets. The provider is a configuration detail.*

---

## Purpose

Define the epic that delivers the multi-provider knowledge platform: abstract interfaces + domain logic + four provider implementations. The product is the integration layer and domain logic. The provider is selected at deploy time, not at build time.

---

## Why Multi-Provider

The platform is open source. You don't control where it gets deployed. The client chooses the cloud. Your job is to make it work on all of them.

| Client type | Provider | Why |
