(fork-tax-state
  (repo "knoxx")
  (branch "feat/discord-attachments")
  (base "cf9d6138f9f5968f59c30839fcfc117595a3c2ba")
  (timestamp "2026-05-16T03:45:00Z")
  (scope "backend-stability memory-sessions-cache ui-backend-coverage receipts")
  (verification
    "node -c ecosystem.config.cjs passed"
    "pnpm -C backend exec shadow-cljs compile server-dev passed with existing warnings"
    "prior targeted backend/frontend tests recorded in receipts.edn")
  (residual
    "docs/actor-realtime-socket-io-spec.md"
    "voice/stt-npu/server.py"
    "uploads/openutau/the-frame-of-absence/"))
