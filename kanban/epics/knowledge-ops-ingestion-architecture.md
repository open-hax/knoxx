---
uuid: "knoxx-knowledge-ops-ingestion-architecture"
title: "Knowledge Ops Ingestion Architecture"
status: in_progress
priority: P2
labels: ["epics"]
created_at: "2026-05-28T22:40:14.386Z"
source: "specs/epics/knowledge-ops-ingestion-architecture.md"
points: null
category: epics
---

# Knowledge Ops Ingestion Architecture

> Source: `specs/epics/knowledge-ops-ingestion-architecture.md`

**Status**: Active
**Last Updated**: 2026-04-11
**Context**: Knoxx ingestion replacement and graph-weaver coordination

## Overview

This document clarifies the separation of concerns between Knoxx ingestion, OpenPlanner, and graph-weaver to prevent duplicate work and ensure a clear data flow.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Data Sources                                 │
├─────────────────────────────────────────────────────────────────────┤
│  Knoxx Docs/      │  Chat Sessions   │  External Docs  │  Code Repo │
