(ns knoxx.frontend.infra.migration-vitest-modules-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-vitest :as vitest]))

(defn- inspect-settings! [settings]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-vitest-modules-"))
        frontend (node-path/join root "frontend")]
    (try
      (fs/mkdirSync frontend)
      (fs/writeFileSync (node-path/join frontend "package.json")
                        "{\"scripts\":{\"test\":\"vitest run --config vitest.config.ts\"}}")
      (fs/writeFileSync (node-path/join frontend "vitest.config.ts")
                        (str "import {defineConfig} from 'vitest/config';"
                             "export default defineConfig({test:{include:['src/**/*.test.ts'],"
                             settings "}});"))
      (vitest/assert-config! root)
      (finally (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest custom-vitest-reporters-cannot-run-outside-the-inventory
  (doseq [settings ["reporters:['../legacy/reporter.ts']"
                    "reporters:'../legacy/reporter.ts'"
                    "reporters:[['../legacy/reporter.ts',{}]]"
                    "reporters:['./src/custom-reporter.ts']"
                    "reporters:['custom-reporter-package']"]]
    (t/is (thrown-with-msg? js/Error #"Custom Vitest modules require explicit migration inventory support"
                           (inspect-settings! settings)) settings)))

(t/deftest supported-module-loading-options-share-the-same-admission
  (doseq [settings ["runner:'../legacy/runner.ts'"
                    "environment:'../legacy/environment.ts'"
                    "pool:'../legacy/pool.ts'"
                    "environmentMatchGlobs:[['src/**','../legacy/environment.ts']]"
                    "poolMatchGlobs:[['src/**','../legacy/pool.ts']]"
                    "snapshotEnvironment:'../legacy/snapshots.ts'"
                    "snapshotSerializers:['../legacy/serializer.ts']"
                    "diff:'../legacy/diff.ts'"
                    "sequence:{sequencer:'../legacy/sequencer.ts'}"
                    "coverage:{provider:'custom',customProviderModule:'../legacy/provider.ts'}"
                    "coverage:{provider:'v8',customProviderModule:'../legacy/provider.ts'}"
                    "coverage:{reporter:['../legacy/reporter.cjs']}"
                    "browser:{provider:'../legacy/provider.ts'}"
                    "benchmark:{reporters:['../legacy/reporter.ts']}"]]
    (t/is (thrown-with-msg? js/Error #"Custom Vitest modules require explicit migration inventory support"
                           (inspect-settings! settings)) settings)))

(t/deftest forwarded-node-arguments-cannot-preload-uninventoried-modules
  (doseq [pool ["forks" "threads" "vmForks" "vmThreads"]]
    (t/is (thrown-with-msg? js/Error #"Custom Vitest modules require explicit migration inventory support"
                           (inspect-settings! (str "poolOptions:{" pool ":{execArgv:['--require','../legacy/preload.cjs']}}"))))))

(t/deftest builtin-loaders-and-current-coverage-reporters-remain-supported
  (doseq [settings ["environment:'jsdom',coverage:{provider:'v8',reporter:['text','json-summary','lcov']}"
                    "reporters:['default','basic','verbose','dot','json','tap','tap-flat','junit','hanging-process','github-actions','html']"
                    "reporters:[['json',{outputFile:'results.json'}]],coverage:{reporter:[['text',{skipEmpty:true}]]}"
                    "environment:'node',pool:'forks',environmentMatchGlobs:[['src/**','happy-dom']],poolMatchGlobs:[['src/**','threads']]"
                    "runner:null,snapshotEnvironment:null,snapshotSerializers:[],diff:{truncateThreshold:20}"
                    "browser:{provider:'playwright'},benchmark:{reporters:['default','verbose']},sequence:{shuffle:true}"
                    "poolOptions:{forks:{singleFork:true,execArgv:[]},threads:{maxThreads:2}}"]]
    (t/is (nil? (inspect-settings! settings)) settings)))

(t/deftest uninspectable-module-selectors-fail-closed
  (doseq [settings ["reporters:[42]" "environment:['jsdom']" "coverage:'v8'"
                    "snapshotSerializers:{}" "poolMatchGlobs:['threads']"]]
    (t/is (thrown-with-msg? js/Error #"Custom Vitest modules require explicit migration inventory support"
                           (inspect-settings! settings)) settings)))
