(fork-tax-state
  (timestamp "2026-05-24T21:15:45Z")
  (repo "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx")
  (branch "main")
  (remote "origin" "git@github.com:open-hax/knoxx.git")
  (snapshot-base-head "ffc731e3799aea4b6abf2d705dd20e27abd6ff25")
  (dirty-entries-before-artifact-refresh 72)
  (scope "full working-tree snapshot requested by user via fork tax")
  (concurrent-dirt "none separated; all visible repo-relevant dirt intentionally absorbed into requested snapshot")
  (blocked-paths ())
  (verification
    (git-diff-check "passed")
    (backend-test "passed: pnpm -C backend test; 389 tests; 1071 assertions; 0 failures; 0 errors; 237 existing warnings")
    (backend-server-compile "passed: pnpm -C backend exec shadow-cljs compile server; build completed; 264 existing warnings"))
  (logs
    "/tmp/knoxx-fork-tax-test-20260524T211242Z.log"
    "/tmp/knoxx-fork-tax-server-20260524T211339Z.log")
  (destructive-cleanup false)
  (tag "pi/fork-tax-20260524T211545Z"))
