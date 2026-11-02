---
uuid: "knoxx-mailbox-delivery-receipts-333"
title: "Preserve mailbox claim receipts and delivery errors"
status: "incoming"
type: "task"
priority: "P1"
dependency: []
category: "tasks"
write-id: "1793663726614-0.0pqd4f2of7c9mr5l2hee"
created_at: "2026-11-02T23:55:26.614Z"
---

## Outcome
Preserve mailbox claim replay and expose accurate delivery failure state.

## Destination
Standalone story for mailbox providers, PR https://github.com/open-hax/knoxx/pull/333.

## Scope
Durable empty claim receipts, delivery error projection, and successful retry state. No authorization, credentials, external delivery, or deployment changes.

## Acceptance criteria
- An empty claim with an operation ID replays after restart and later entry creation.
- Failed delivery exposes its stored error and successful retry removes stale error.
- Contradictory outcome/error semantics and replay compatibility are demonstrated before a behavior change.

## Verification
Guarded focused domain and file-backed Clio tests; full backend test and server compilation where dependencies permit. No live delivery.
