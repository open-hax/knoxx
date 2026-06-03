(fork-tax-state
  (timestamp "2026-06-03T20:21:42Z")
  (repo "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx")
  (branch "test/coverage-improvement")
  (remote "origin" "git@github.com:open-hax/knoxx.git")
  (base "origin/staging")
  (scope "Backend domain/law/shape test coverage, parse regex fix, frontend test env, label-gated testing deploy workflow")
  (changes
    (parse-fix
      "shape/parse.cljs parse-positive-int: regex #\"\\\\.\" (literal backslash+dot) corrected to #\"\\.\" so decimal strings are rejected as NaN instead of truncated by parseInt")
    (coverage-tests
      "New backend/test/cljs/knoxx/backend/domain/condition/builtin_test.cljs"
      "New backend/test/cljs/knoxx/backend/domain/node/ tests"
      "New backend/test/cljs/knoxx/backend/domain/time_test.cljs"
      "New backend/test/cljs/knoxx/backend/law/ tests"
      "New backend/test/cljs/knoxx/backend/shape/ tests")
    (frontend-test-env
      "frontend/package.json: NODE_ENV=test for vitest run/coverage/watch")
    (deploy-testing-workflow
      ".github/workflows/deploy-testing.yml: label-gated (testing) PR-head deploy to the shared staging slot via open-hax/services/.github/workflows/deploy-promethean.yml@main service=knoxx"
      "eligibility: same-repo head, non-draft, owner in TESTING_ALLOWED_OWNER_LOGINS"
      "concurrency group knoxx-staging shared with deploy-staging; queues, no cancel"))
  (concurrent-dirt
    "none; all working tree changes are owned by this snapshot")
  (blocked-paths ())
  (verification
    (backend-tests "passed: pnpm -C backend run test:coverage => exit 0")
    (frontend-tests "passed: pnpm -C frontend run test:coverage => exit 0")
    (workflow-lint "passed: actionlint .github/workflows/deploy-testing.yml")
    (secret-heuristic-scan "passed: additions are tests, package.json scripts, and a workflow using vars/secrets indirection only"))
  (destructive-cleanup false)
  (deployment
    "pending: PR test/coverage-improvement -> staging"
    "pending: testing label -> deploy-testing.yml -> services module deploy of PR head to staging slot")
  (tag "Π/20260603T202142Z-knoxx-coverage-tests-testing-deploy"))
