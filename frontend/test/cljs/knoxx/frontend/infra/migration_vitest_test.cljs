(ns knoxx.frontend.infra.migration-vitest-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-vitest :as vitest]))

(defn- inspect-fixture! [{:keys [config scripts extra-files]
                         :or {scripts {} extra-files {}}}]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-vitest-scope-"))
        files (cond-> (merge {"package.json" (js/JSON.stringify (clj->js {:scripts scripts}))}
                              extra-files)
                config (assoc "vitest.config.ts" config))]
    (try
      (doseq [[path source] files]
        (let [absolute-path (node-path/join root "frontend" path)]
          (fs/mkdirSync (node-path/dirname absolute-path) #js {:recursive true})
          (fs/writeFileSync absolute-path source)))
      (vitest/assert-config! root)
      (finally (fs/rmSync root #js {:recursive true :force true})))))

(defn- config-source [test-properties]
  (str "import { defineConfig } from 'vitest/config';\n"
       "export default defineConfig({cacheDir:'.vite-vitest',test:{" test-properties "}});"))

(def ^:private current-scope
  "include:['src/**/*.test.{ts,tsx}'],setupFiles:['./src/test/setup.ts'],environment:'jsdom',globals:true,coverage:{provider:'v8',reportsDirectory:'coverage',reporter:['text','json-summary','lcov']}")

(t/deftest current-vitest-configuration-and-package-commands-remain-supported
  (t/is (nil? (inspect-fixture!
                {:config (config-source current-scope)
                 :scripts {"test" "NODE_ENV=test vitest run --config vitest.config.ts"
                           "test:coverage" "NODE_ENV=test vitest run --config vitest.config.ts --coverage"
                           "test:watch" "NODE_ENV=test vitest --config vitest.config.ts"}}))))

(t/deftest relocated-running-tests-cannot-leave-the-inventory
  (doseq [pattern ["tests/**/*.test.ts" "../legacy/**/*.test.ts"
                   "**/*.test.ts" "src/../legacy/**/*.test.ts"
                   "src/{pages,../../legacy}/**/*.test.ts"]]
    (t/is (thrown-with-msg?
            js/Error #"Vitest source scope leaves governed frontend source tree"
            (inspect-fixture! {:config (config-source (str "include:['" pattern "']"))})))))

(t/deftest alternative-vitest-source-loaders-obey-the-same-boundary
  (doseq [field ["includeSource" "setupFiles" "globalSetup"]]
    (t/is (thrown-with-msg?
            js/Error #"Vitest source scope leaves governed frontend source tree"
            (inspect-fixture!
              {:config (config-source (str "include:['src/**/*.test.ts']," field ":['legacy/**/*.ts']"))})))
    (t/is (nil? (inspect-fixture!
                  {:config (config-source (str "include:['src/**/*.test.ts']," field ":['src/test/setup.ts']"))})))))

(t/deftest dynamic-and-overridden-vitest-scopes-fail-closed
  (doseq [properties ["include:patterns" "include:[...patterns]" "...settings"
                      "include:['src/**/*.test.ts'],root:'legacy'"
                      "include:['src/**/*.test.ts'],dir:'legacy'"
                      "include:['src/**/*.test.ts'],workspace:'projects.ts'"
                      "include:['src/**/*.test.ts'],projects:[]"
                      "include:['src/**/*.test.ts'],typecheck:{include:['legacy/**/*.ts']}"]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Vitest migration configuration"
            (inspect-fixture! {:config (config-source properties)}))))
  (t/is (thrown-with-msg?
          js/Error #"Unsupported Vitest migration configuration"
          (inspect-fixture! {:config "export default {root:'legacy',test:{include:['src/**/*.test.ts']}};"}))))

(t/deftest active-vitest-configs-cannot-be-missing-or-renamed
  (doseq [[config command] [[nil "vitest run --config vitest.config.ts"]
                            [(config-source current-scope) "vitest run --config relocated.config.ts"]
                            [nil "vitest run"]
                            [(config-source current-scope) "vitest run --config vitest.config.ts --root legacy"]]]
    (t/is (thrown-with-msg?
            js/Error #"Unsupported Vitest migration configuration"
            (inspect-fixture! {:config config :scripts {"test" command}
                               :extra-files {"relocated.config.ts" (config-source "include:['legacy/**/*.test.ts']")}}))))
  (t/is (nil? (inspect-fixture! {}))))

(t/deftest automatically-selected-workspaces-cannot-expand-test-scope
  (doseq [path ["vitest.workspace.ts" "vitest.projects.json"]]
    (t/testing path
      (t/is (thrown-with-msg?
              js/Error #"Unsupported Vitest migration configuration"
              (inspect-fixture! {:config (config-source current-scope)
                                 :extra-files {path "[]"}}))))))
