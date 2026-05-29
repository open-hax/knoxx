---
uuid: "knoxx-knowledge-ops-exposure-monitor"
title: "Exposure Monitor — Product Spec"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.384Z"
source: "specs/epics/knowledge-ops-exposure-monitor.md"
points: null
category: epics
---

# Exposure Monitor — Product Spec

> Source: `specs/epics/knowledge-ops-exposure-monitor.md`

> *Find exposed AI. Resolve contacts. Generate outreach. Defend the perimeter.*

---

## Purpose

Define the Exposure Monitor product — an external AI infrastructure exposure detection and lead generation platform built on the existing `our-gpus` codebase.

---

## What It Is

A platform that:
1. **Discovers** exposed AI endpoints across the internet (Ollama, OpenAI-compatible proxies, custom inference servers)
2. **Verifies** each endpoint (GPU specs, model lists, latency, geo data)
