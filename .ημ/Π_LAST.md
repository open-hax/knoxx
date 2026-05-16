# Π Fork Tax Snapshot — Knoxx

- Timestamp: 2026-05-16T03:45:00Z
- Branch: feat/discord-attachments
- Base: cf9d6138f9f5968f59c30839fcfc117595a3c2ba
- Scope: Knoxx backend stability, memory sessions hot cache, UI/backend surface tests, and receipts.

## Verification carried forward

- `node -c ecosystem.config.cjs` passed.
- `pnpm -C backend exec shadow-cljs compile server-dev` passed with existing infer warnings.
- `pnpm -C backend exec shadow-cljs compile test` passed for the memory cache work earlier in the receipt trail.
- Targeted frontend Vitest suites and `pnpm -C frontend typecheck` passed for the UI/backend coverage work earlier in the receipt trail.
- Runtime smoke: `knoxx-backend` remained online after controlled PM2 restart; `/api/proxx/health` returned 200.

## Residual dirt intentionally not absorbed

These paths were left out because they are unrelated to the OpenPlanner/Kafka/Knoxx stability fork-tax scope or lack fresh provenance in this turn:

- `docs/actor-realtime-socket-io-spec.md`
- `voice/stt-npu/server.py`
- `uploads/openutau/the-frame-of-absence/`

No destructive cleanup was performed.
