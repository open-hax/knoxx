---
uuid: "knoxx-events-agent-runtime-separation"
title: "Events, Triggers, Actions, Schedules, and Agents Runtime Separation"
status: incoming
priority: P2
labels: ["epics"]
created_at: "2026-05-06T00:00:00Z"
source: "specs/epics/events-agent-runtime-separation.md"
points: null
category: epics
---

# Events, Triggers, Actions, Schedules, and Agents Runtime Separation

> Source: `specs/epics/events-agent-runtime-separation.md`

Date: 2026-05-06
Status: in-progress
Repo: `packages/agents/knoxx`

## Goal

Separate event reaction from agent execution so Knoxx has:

1. one agent runtime,
2. one event dispatch path,
3. explicit trigger/action/schedule/generator resources,
4. no blended runtime class that pretends scheduling, subscription, action, and agent execution are one thing.

## Problem

