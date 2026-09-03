(fork-tax-state
  (timestamp "2026-08-13T06:29:41Z")
  (repo "/home/err/spaces/knoxx")
  (branch "feat/contract-publication-e2e")
  (remote "origin" "git@github.com:open-hax/knoxx.git")
  (base "origin/feat/contract-publication-e2e")
  (scope "Epic completion: contract-owned publication pipeline with twelve cards implemented")
  (changes
    (kanban
      "Modified kanban/.events/ledger.edn (added epic completion comment)"
      "Modified kanban/epics/knoxx-contract-owned-publication-pipeline.md (updated write-id and completion summary)"))
  (concurrent-dirt
    "none; all working tree changes are owned by this snapshot")
  (blocked-paths
    ".claude/ (agent runtime state — excluded)"
    ".shadow-cljs/ (build artifact directory — excluded)"
    "backend/.shadow-cljs/ (build artifact directory — excluded)"
    "node_modules/ (installed dependencies — excluded)"
    "backend/node_modules/ (installed dependencies — excluded)")
  (verification
    (passed "kanban changes only — no code verification required")
    (skipped "epic completion summary does not require compilation"))
  (destructive-cleanup false)
  (commit "e0955849")
  (tag "pi/e0955849-20260813T062941")
  (deployment
    "complete: commit + tag + push to origin"))