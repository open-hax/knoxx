(ns knoxx.frontend.infra.migration-package-imports-test
  (:require ["node:fs" :as fs]
            ["node:os" :as os]
            ["node:path" :as node-path]
            ["typescript" :as ts]
            [cljs.test :as t]
            [knoxx.frontend.infra.migration-imports :as imports]))

(defn- write-fixture! [root path source]
  (let [absolute-path (node-path/join root path)]
    (fs/mkdirSync (node-path/dirname absolute-path) #js {:recursive true})
    (fs/writeFileSync absolute-path source)))

(defn- write-package! [root path]
  (write-fixture! root (str path "/package.json")
                  "{\"name\":\"@fixture/page\",\"version\":\"1.0.0\",\"main\":\"index.ts\"}")
  (write-fixture! root (str path "/index.ts") "export const Page = 1;"))

(defn- assert-fixture-resolves! [root]
  (let [path (node-path/join root "frontend/src/page.ts")
        options #js {:moduleResolution (.-Bundler ts/ModuleResolutionKind)
                     :module (.-ESNext ts/ModuleKind)}
        ^js resolution (ts/resolveModuleName "@fixture/page" path options ts/sys)]
    (when-not (and (.-resolvedModule resolution)
                   (.-isExternalLibraryImport ^js (.-resolvedModule resolution)))
      (throw (ex-info "Fixture package must resolve as a TypeScript external library" {:path path})))))

(defn- inspect-package! [{:keys [dependency source-path linked?]}]
  (let [root (fs/mkdtempSync (node-path/join (os/tmpdir) "knoxx-package-import-"))
        installed-path "frontend/node_modules/@fixture/page"
        source "import { Page } from '@fixture/page'; export { Page };"
        local-path (or source-path installed-path)]
    (try
      (write-fixture! root "frontend/package.json"
                      (str "{\"name\":\"fixture\",\"type\":\"module\",\"dependencies\":{"
                           "\"@fixture/page\":\"" dependency "\"}}"))
      (write-fixture! root "frontend/tsconfig.json"
                      "{\"compilerOptions\":{\"moduleResolution\":\"Bundler\",\"module\":\"ESNext\"},\"include\":[\"src\"]}")
      (write-fixture! root "frontend/src/page.ts" source)
      (write-package! root local-path)
      (if linked?
        (let [installed (node-path/join root installed-path)]
          (fs/mkdirSync (node-path/dirname installed) #js {:recursive true})
          (fs/symlinkSync (node-path/join root local-path) installed "dir"))
        (write-package! root installed-path))
      (assert-fixture-resolves! root)
      (imports/assert-contained! (imports/resolver root {}) "frontend/src/page.ts" source)
      (finally (fs/rmSync root #js {:recursive true :force true})))))

(t/deftest workspace-and-link-packages-cannot-import-source-outside-the-governed-tree
  (doseq [dependency ["workspace:*" "link:./legacy"]]
    (t/is (thrown-with-msg?
            js/Error #"Local import leaves governed frontend source tree"
            (inspect-package! {:dependency dependency
                               :source-path "frontend/legacy"
                               :linked? true})))))

(t/deftest copied-file-dependencies-retain-the-declared-source-boundary
  (t/is (thrown-with-msg?
          js/Error #"Local import leaves governed frontend source tree"
          (inspect-package! {:dependency "file:./legacy"
                             :source-path "frontend/legacy"
                             :linked? false}))))

(t/deftest installed-registry-packages-remain-supported
  (t/is (nil? (inspect-package! {:dependency "^1.0.0" :linked? false}))))

(t/deftest contained-local-packages-remain-supported
  (doseq [[dependency linked?] [["workspace:*" true] ["file:./src/local-package" false]]]
    (t/is (nil? (inspect-package! {:dependency dependency
                                  :source-path "frontend/src/local-package"
                                  :linked? linked?})))))
