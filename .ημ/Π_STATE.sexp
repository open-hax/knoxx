(fork-tax-state
  (timestamp "2026-06-04T18:33:20Z")
  (repo "/home/err/devel/orgs/open-hax/openplanner/packages/agents/knoxx")
  (branch "fix/frontend-es2022-lib")
  (remote "origin" "git@github.com:open-hax/knoxx.git")
  (base "origin/staging")
  (scope "Backend trigger test suite, domain condition registry refactor, event dispatch contract loading, source runtime discord integration")
  (changes
    (domain-condition-registry
      "Refactored backend/src/cljs/knoxx/backend/domain/condition/registry.cljs"
      "Refactored backend/test/cljs/knoxx/backend/domain/condition/registry_test.cljs")
    (contracts-loader
      "Modified backend/src/cljs/knoxx/backend/domain/contracts/loader.cljs")
    (discord-source
      "Modified backend/src/cljs/knoxx/backend/domain/discord/source.cljs")
    (event-dispatch
      "Modified backend/src/cljs/knoxx/backend/domain/event/dispatch.cljs")
    (source-runtime
      "Modified backend/src/cljs/knoxx/backend/domain/source/runtime.cljs")
    (trigger-tests
      "New backend/test/cljs/knoxx/backend/triggers/action_invocation_test.cljs"
      "New backend/test/cljs/knoxx/backend/triggers/contract_root_mismatch_test.cljs"
      "New backend/test/cljs/knoxx/backend/triggers/contracts_discovery_test.cljs"
      "New backend/test/cljs/knoxx/backend/triggers/error_propagation_test.cljs"
      "New backend/test/cljs/knoxx/backend/triggers/event_deduplication_test.cljs"
      "New backend/test/cljs/knoxx/backend/triggers/production_scenario_test.cljs"
      "New backend/test/cljs/knoxx/backend/triggers/real_contracts_dispatch_test.cljs"
      "New backend/test/cljs/knoxx/backend/triggers/source_dispatch_test.cljs"
      "New backend/test/cljs/knoxx/backend/triggers/trigger_loading_test.cljs"
      "New backend/test/cljs/knoxx/backend/triggers/trigger_matching_test.cljs"
      "New backend/test/cljs/knoxx/backend/triggers/trigger_validation_test.cljs")
    (session-note
      "New docs/notes/2026.06.04.09.47.41.md"))
  (concurrent-dirt
    "none; all working tree changes are owned by this snapshot")
  (blocked-paths ())
  (verification
    (backend-tests "passed: pnpm -C backend exec shadow-cljs compile test => 523 tests, 1528 assertions, 0 failures, 0 errors"))
  (destructive-cleanup false)
  (deployment
    "pending: PR fix/frontend-es2022-lib -> staging")
  (tag "Π/20260604T183320Z-knoxx-trigger-tests-domain-refactor"))
