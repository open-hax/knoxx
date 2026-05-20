(fork-tax-state
  (timestamp "2026-05-20T23:29:55Z")
  (repo "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx")
  (branch "chore/great-renamspacing")
  (base-head "b08439cb5132c53b9e391465973e42f938f40e4e")
  (remote "origin git@github.com:open-hax/knoxx.git")
  (scope "all visible Knoxx repo dirt requested by user via pay the fork tax")
  (verification
    (backend-test-compile (command "pnpm -C backend exec shadow-cljs compile test") (exit 0) (log "/tmp/knoxx-fork-tax-test.log") (sha256 "3844314700ac98f2fa0a4c41fd33ec9ef94728d64998a245d8dcb7a910d848ec") (note "compile completed with warnings"))
    (backend-server-compile (command "pnpm -C backend exec shadow-cljs compile server") (exit 0) (log "/tmp/knoxx-fork-tax-server.log") (sha256 "b9ded89fa18f43bc4208069905a89f8a87712fdf16aec437eda8a7e9c94b5645") (note "compile completed with warnings")))
  (concurrent-dirt "none classified; user requested repository snapshot; ignored .env left untouched")
  (blockers "none before commit/push"))
