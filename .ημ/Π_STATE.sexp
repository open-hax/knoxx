(fork-tax-state
  (timestamp "2026-07-10T20:06:39Z")
  (repo "/home/err/spaces/knoxx")
  (branch "main")
  (remote "origin" "git@github.com:open-hax/knoxx.git")
  (base "origin/main")
  (scope "OpenPlanner direct-client integration: in-process Mongo mode via @open-hax/openplanner-sdk; contract-runtime source path alignment; PRINCIPLE sync")
  (changes
    (principle
      "Modified .ημ/PRINCIPLE.edn")
    (tooling
      "Modified backend/.clj-kondo/config.edn"
      "Modified backend/package.json"
      "Modified backend/pnpm-lock.yaml"
      "Modified pnpm-lock.yaml (root lockfile)")
    (build
      "Modified backend/shadow-cljs.edn (contract-runtime source path, openplanner-sdk test resolution)")
    (runtime
      "Modified backend/src/cljs/knoxx/backend/bootstrap.cljs"
      "Modified backend/src/cljs/knoxx/backend/infra/clients/openplanner.cljs"
      "Modified backend/src/cljs/knoxx/backend/infra/config.cljs")
    (new-files
      "Added backend/src/cljs/knoxx/backend/extern/openplanner_sdk.cljs"
      "Added backend/src/cljs/knoxx/backend/infra/clients/openplanner_mongo.cljs"
      "Added backend/test/cljs/knoxx/backend/extern_openplanner_sdk_test.cljs"
      "Added backend/test/js/openplanner_sdk_test_stub.mjs"))
  (concurrent-dirt
    "none; all working tree changes are owned by this snapshot")
  (blocked-paths
    ".claude/ (agent runtime state — excluded)"
    ".shadow-cljs/ (build artifact directory — excluded)"
    "backend/.shadow-cljs/ (build artifact directory — excluded)"
    "node_modules/ (installed dependencies — excluded)"
    "backend/node_modules/ (installed dependencies — excluded)")
  (verification
    (passed "pnpm -C backend exec shadow-cljs compile server: success, 0 warnings")
    (failed "pnpm -C backend exec shadow-cljs compile test: 1 failure in knoxx.backend.infra.store-test — Store document failed schema validation"))
  (destructive-cleanup false)
  (commit "144cd66280c9aa468c9fa252dac0fa7321cf404c")
  (tag "Π/20260710T200639Z-openplanner-mongo-direct-client")
  (deployment
    "pending: commit + tag + push to origin"))
