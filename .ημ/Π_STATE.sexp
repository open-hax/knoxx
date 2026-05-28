(fork-tax-state
  (timestamp "2026-05-28T16:30:34Z")
  (repo "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx")
  (branch "pi/fork-tax/20260526T204054Z-knoxx-host-services")
  (remote "origin" "git@github.com:open-hax/knoxx.git")
  (snapshot-base-head "c066cf0b206961ef70d46bd869b7db006b314d6f")
  (dirty-entries-before-artifact-refresh 95)
  (scope "full working-tree snapshot requested by user via fork tax; includes backend CLJS lint remediation, async test repairs, Docker/runtime metadata, contracts, specs, docs, and receipts")
  (concurrent-dirt "none separated; all visible repo-relevant dirt intentionally absorbed into requested snapshot after path review and secret heuristic scan")
  (blocked-paths ())
  (verification
    (secret-heuristic-scan "passed: dirty text scan found only code/test references to token/secret/password/api-key labels; no literal secret material printed or staged intentionally")
    (targeted-mcp-http-kondo "passed: pnpm -C backend exec clj-kondo --lint test/cljs/knoxx/backend/mcp_http_test.cljs; 0 errors; 0 warnings")
    (git-diff-check "passed")
    (backend-test "passed: pnpm -C backend exec shadow-cljs compile test; 449 tests; 1320 assertions; 0 failures; 0 errors; 0 warnings")
    (backend-server-compile "passed: pnpm -C backend exec shadow-cljs compile server; 0 warnings")
    (backend-lint "blocked: pnpm -C backend run lint exits 3 with 11 function-length errors and 1749 warnings; see /tmp/knoxx-lint.log"))
  (residual-errors
    "gw-start-voice-listener (171)"
    "ensure-schema! (294)"
    "start-document-ingestion! (163)"
    "index-run-memory-legacy! (181)"
    "register-routes! (188)"
    "register-auth-routes (184)"
    "register-model-routes! (193)"
    "register-proxy-routes! (263)"
    "register-translation-routes! (229)"
    "register-user-admin-routes! (214)"
    "register-voice-routes! (190)")
  (logs
    "/tmp/knoxx-lint.log")
  (destructive-cleanup false)
  (tag "pi/fork-tax/20260528T163034Z/knoxx-lint-remediation"))
