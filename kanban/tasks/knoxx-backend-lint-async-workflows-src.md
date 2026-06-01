---
uuid: "knoxx-knoxx-backend-lint-async-workflows-src"
title: "Knoxx Backend Lint — Async Workflow Refactor for Source Namespaces"
status: "in_progress"
priority: "P1"
labels: ["tasks", "5sp"]
created_at: "2026-05-27T00:00:00Z"
source: "specs/tasks/knoxx-backend-lint-async-workflows-src.md"
points: 5
category: "tasks"
---

# Knoxx Backend Lint — Async Workflow Refactor for Source Namespaces

> Source: `specs/tasks/knoxx-backend-lint-async-workflows-src.md`
> Points: 5

Date: 2026-05-27
Status: todo
Parent epic: `specs/epics/knoxx-backend-cljs-lint-remediation.md`
Story points: 5

## Purpose

Replace raw `.then` and `.catch` Promise chains in backend source namespaces with named async workflows that match Knoxx's current CLJS style: `^:async` functions, `await`, plain context maps, and small stage functions.

## Problem

The current lint run reports 1502 Promise-chain warnings. The old lint message and older async notes pointed developers toward `promesa.core/p/let`; that guidance is now too narrow and can pull new code away from the house style documented in AGENTS and route macro notes.

The target is not syntactic churn. Promise-chain cleanup should make workflows easier to test and should expose domain/shape/law/infra boundaries that are currently hid

---
2026-05-29 progress: picked up after function-length card closed. Baseline pnpm -C backend lint => errors 0 warnings 1463; warning classes include 1236 raw Promise-chain warnings. Starting with small source namespaces (law.guards, infra.temp-memory, infra.svg-render) plus law.url namespace repair before larger route/db surfaces. --tasks-dir .

2026-05-29 async slice: converted law.guards preHandlers, infra.temp-memory API, and infra.svg-render browser lifecycle from raw .then/.catch to ^:async/await; repaired law.url missing ns require. Verification: targeted clj-kondo 0/0, typecheck 0 warnings, full test 449/1319 0 failures, full lint now errors 0 warnings 1450 (-13 from 1463). --tasks-dir .

2026-05-29 async slice: converted infra.redis-client Promise wrappers and init flow to ^:async/await. Verification: redis_client targeted clj-kondo 0/0; typecheck 0 warnings; full test 449/1319 0 failures; full lint errors 0 warnings 1425 (-25 from previous, -38 total today). --tasks-dir .

2026-05-29 async continuation: converted infra.stores.session-store, composite-session-store, openplanner-message-source, redis-message-source, session-flush, composite-message-source, infra.source.opencode-session-ingester; split openplanner-session-store event helper to clear function-length warning. Verification: targeted clj-kondo slices 0/0; typecheck 0 warnings; full test 449/1319 0 failures; full lint errors 0 warnings 1389 (-36 from 1425 at start of continuation). --tasks-dir .
---
2026-05-29 async routes slice: converted infra.routes.translation and infra.routes.voice STT/TTS flows from raw `.then`/`.catch` chains to named `^:async`/`await` helpers, preserved the translation `route!` shadow-cljs workaround, split voice TTS payload helpers, and removed unused voice websocket helpers. Verification: targeted clj-kondo translation.cljs 0/0 and voice.cljs 0/0; backend typecheck 0 warnings; full shadow test 449 tests / 1319 assertions / 0 failures; full lint errors 0 warnings 1290 (exit 2 until zero-warning gate). Note: `eta-mu kanban comment` was unavailable because @open-hax/kanban-legacy is not installed, so this comment was appended directly. --tasks-dir .
---
2026-05-29 route arity follow-up: replaced shorthand translation route operation lambdas with explicit 3-arg fns so the shared executor can call every operation as `(request ctx handlers)` without runtime arity errors. Re-ran targeted translation clj-kondo 0/0, backend typecheck 0 warnings, full lint errors 0 warnings 1290, and force-spawn shadow tests 449/1319 0 failures. --tasks-dir .
---
2026-05-29 policy-db async slice: converted `infra.db.policy` role/tool policy setup, contract role projection sync, request context hydration, bootstrap org/user setup, org/role/user/membership/data-lake APIs, and actor contract best-effort write handling from Promise chains to named `^:async` helpers. Targeted policy clj-kondo warnings dropped 137 -> 49 with 0 errors. Backend typecheck stayed 0 warnings. Full backend lint now errors 0 warnings 1202 (exit 2 until zero-warning gate). Force-spawn shadow tests stayed 449 tests / 1319 assertions / 0 failures. --tasks-dir .
---
2026-05-29 app-routes async slice: converted the main `infra.routes.app` proxy/data/health/session/run/admin helper flows plus chat/direct start dispatch from raw Promise chains to named `^:async`/`await` helpers. Targeted app clj-kondo dropped from 123 warnings to 23 with 0 errors. Backend typecheck stayed 0 warnings. Full backend lint now errors 0 warnings 1102 (exit 2 until zero-warning gate). Force-spawn shadow tests stayed 449 tests / 1319 assertions / 0 failures. --tasks-dir .
---
2026-05-29 bluesky async slice: converted `domain.bluesky.bluesky` auth/session/search/profile/publish/social/chat helper and execute flows from raw Promise chains to `^:async`/`await`, removed the now-unused promise helper require, and extracted the tool factory list to clear the long `create-bluesky-custom-tools` warning. Targeted bluesky clj-kondo dropped from 54 warnings to 0 with 0 errors. Backend typecheck stayed 0 warnings. Full backend lint now errors 0 warnings 1048 (exit 2 until zero-warning gate). Force-spawn shadow tests stayed 449 tests / 1319 assertions / 0 failures. --tasks-dir .
---
2026-05-29 memory-routes async slice: converted `infra.routes.memory` memory session cache, Redis cache hits, authorization paging, actor-hit filtering, title warmup, row enrichment, memory-session routes, backfill/session/search routes from Promise chains to `^:async`/`await`, removed unused imports, and split long active/session-search helpers. Targeted memory clj-kondo dropped from 49 warnings to 0 with 0 errors. Backend typecheck stayed 0 warnings. Full backend lint now errors 0 warnings 999 (exit 2 until zero-warning gate). Force-spawn shadow tests stayed 449 tests / 1319 assertions / 0 failures. --tasks-dir .
---
2026-05-29 policy-db completion slice: converted remaining `infra.db.policy` session, invite, credential, bootstrap initialization, session secret, and allowlist bootstrap flows from Promise chains to `^:async`/`await`; extracted session/invite/credential response and policy bootstrap helpers. Targeted policy clj-kondo dropped from 49 warnings to 0 with 0 errors. Backend typecheck stayed 0 warnings. Full backend lint now errors 0 warnings 950 (exit 2 until zero-warning gate). Force-spawn shadow tests stayed 449 tests / 1319 assertions / 0 failures. --tasks-dir .
---
2026-05-30 discord-tools async slice: converted `domain.discord.tools` token/client, OpenPlanner label attach, channel/DM fetch/search, upload attachment/SVG render, send/react/thread/guild/channel helpers, and tool execute flows from raw Promise chains to `^:async`/`await`; removed unused gateway manager. Targeted discord tools clj-kondo dropped from 47 warnings to 0 with 0 errors. Backend typecheck stayed 0 warnings. Full backend lint now errors 0 warnings 903 (exit 2 until zero-warning gate). Force-spawn shadow tests stayed 449 tests / 1319 assertions / 0 failures. --tasks-dir .
---
2026-05-30 discord-gateway async slice: converted `domain.discord.gateway` reaction handling, client start/stop/restart, server/channel/message fetch/search/send, voice join/listing, manager factory, and actor gateway start flows from raw Promise chains to `^:async`/`await`; split manager construction and method tables to clear length warnings. Targeted discord gateway clj-kondo dropped from 42 warnings to 0 with 0 errors. Backend typecheck stayed 0 warnings. Full backend lint now errors 0 warnings 861 (exit 2 until zero-warning gate). Force-spawn shadow tests stayed 449 tests / 1319 assertions / 0 failures. --tasks-dir .
---
2026-05-30 auth-session async slice: converted `infra.auth.session` persistent session secret recovery, DB/Redis store/load/delete, GitHub email/OAuth callback, cookie/API-key context resolution, session creation, invite email, and hook hydration flows from raw Promise chains to `^:async`/`await`; removed unused bootstrap role helpers and split invite/hook helpers. Targeted auth session clj-kondo dropped from 38 warnings to 0 with 0 errors. Backend typecheck stayed 0 warnings. Full backend lint now errors 0 warnings 823 (exit 2 until zero-warning gate). Force-spawn shadow tests stayed 449 tests / 1319 assertions / 0 failures. --tasks-dir .
---
