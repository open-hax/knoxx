---
uuid: knoxx-frontend-shadow-six-baseline-warnings
title: Clear the six baseline frontend Shadow warnings
status: incoming
priority: P1
points: 2
labels: tasks, 2sp, frontend, cljs, warnings, websocket
created_at: 2026-08-30T01:19:13.727Z
category: tasks
---

# Clear the six baseline frontend Shadow warnings

> GitHub issue: [#285](https://github.com/open-hax/knoxx/issues/285)

## Signal

Exact-head CI for PR #265 passed 155 frontend tests and 623 assertions with zero failures or
errors, then emitted six Shadow compiler warnings in files outside that PR. The revision-bound
baseline is head `7762d8232539de08b4162dcc891a51bdf2420d11` on main
`1226601588308fd31e68cd984301871fbf6fc8f7`, hosted run
[`33271450212`](https://github.com/open-hax/knoxx/actions/runs/33271450212).

The six warnings are:

- one target-type inference warning at
  `frontend/src/cljs/knoxx/frontend/components/ops_status/logic.cljs:28`;
- one wrong-arity call to `ws/connect-stream` at
  `frontend/src/cljs/knoxx/frontend/components/ops_status/sidebar.cljs:68`; and
- four target-type inference warnings in the WebSocket test double at
  `frontend/test/cljs/knoxx/frontend/lib/ws_test.cljs:16,17,21,22`.

## Scope

- Make the ops-status subscription conform to the current `connect-stream` API.
- Add explicit safe JavaScript interop typing in ops-status logic and the WebSocket test double.
- Preserve current ops-status behavior and WebSocket coverage.
- Keep warning settings at least as strict as the baseline.

## Acceptance

- Focused ops-status and WebSocket tests pass.
- The full frontend ClojureScript suite passes with zero failures/errors.
- The frontend Shadow test build emits zero warnings.
- Strict clj-kondo over every changed file emits zero errors and zero warnings.
- Receipt River records the exact implementation head and hosted run IDs.

## Non-goals

- Weakening or suppressing compiler warnings.
- Folding unrelated frontend cleanup into this bounded warning ratchet.

## Relationship

This was discovered while closing #265. It is an independently owned warning-baseline repair,
not a blocker to the landed title-convergence/garden-publication work.
