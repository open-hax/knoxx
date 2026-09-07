(ns knoxx.frontend.infra.migration-worker-imports-test
  (:require [cljs.test :as t]
            [knoxx.frontend.infra.migration-imports :as imports]))

(defn- inspect-imports! [source]
  (imports/assert-contained!
    {:root "/repository" :source-root "/repository/frontend/src"
     :options #js {} :aliases {}}
    "frontend/src/bridge/app.ts" source))

(t/deftest plain-public-worker-paths-cannot-hide-relocated-implementation
  (doseq [source ["navigator.serviceWorker.register('/bridge/legacy.js');"
                  "navigator.serviceWorker.register('/bridge/cljs/legacy.js');"
                  "navigator['serviceWorker']['register']('/bridge/legacy.js');"
                  "window.navigator.serviceWorker.register('/bridge/legacy.js');"
                  "globalThis.navigator.serviceWorker.register('/bridge/legacy.js');"
                  "new Worker('/bridge/legacy.js');"
                  "new SharedWorker(`/bridge/legacy.js`);"
                  "new window.Worker('/bridge/legacy.js');"
                  "new globalThis.SharedWorker('/bridge/legacy.js');"
                  "new self['Worker']('/bridge/legacy.js');"]]
    (t/is (thrown-with-msg? js/Error #"Worker source is outside the supported migration URL grammar"
                           (inspect-imports! source)) source)))

(t/deftest unresolved-worker-source-values-cannot-bypass-dependency-inspection
  (doseq [source ["navigator.serviceWorker.register(workerUrl);"
                  "new Worker(workerUrl);"
                  "new Worker(new URL(absoluteUrl));"
                  "new SharedWorker(new URL(absoluteUrl));"
                  "navigator.serviceWorker.register(new URL('/bridge/legacy.js', location.origin));"]]
    (t/is (thrown-with-msg? js/Error #"Worker source is outside the supported migration URL grammar"
                           (inspect-imports! source)) source)))

(t/deftest governed-worker-source-urls-retain-existing-containment
  (doseq [source ["navigator.serviceWorker.register(new URL('./sw.ts', import.meta.url));"
                  "new window.Worker(new URL('./worker.ts', import.meta.url));"
                  "new SharedWorker(new URL(`./worker.ts`, import.meta.url));"]]
    (t/is (nil? (inspect-imports! source)) source))
  (t/is (thrown-with-msg? js/Error #"Local import leaves governed frontend source tree"
                         (inspect-imports!
                           "navigator.serviceWorker.register(new URL('../../public/legacy.js', import.meta.url));"))))

(t/deftest type-only-imports-do-not-exempt-runtime-browser-globals
  (doseq [source ["import type { Worker } from 'worker-types'; new Worker('/bridge/legacy.js');"
                  "import { type SharedWorker } from 'worker-types'; new SharedWorker('/bridge/legacy.js');"]]
    (t/is (thrown-with-msg? js/Error #"Worker source is outside the supported migration URL grammar"
                           (inspect-imports! source)) source)))

(t/deftest browser-worker-aliases-cannot-hide-the-source-call
  (doseq [source ["const workers = navigator.serviceWorker; workers.register('/bridge/legacy.js');"
                  "const workers = window.navigator['serviceWorker']; workers.register('/bridge/cljs/legacy.js');"
                  "const register = navigator.serviceWorker.register; register('/bridge/legacy.js');"
                  "const register = navigator.serviceWorker.register.bind(navigator.serviceWorker); register('/bridge/legacy.js');"
                  "const {register} = navigator.serviceWorker; register('/bridge/legacy.js');"
                  "const MakeWorker = Worker; new MakeWorker('/bridge/legacy.js');"
                  "const MakeWorker = globalThis.SharedWorker; new MakeWorker('/bridge/legacy.js');"
                  "const constructors = {Worker}; new constructors.Worker('/bridge/legacy.js');"]]
    (t/is (thrown-with-msg? js/Error #"Browser worker reference is outside the supported migration grammar"
                           (inspect-imports! source)) source)))

(t/deftest ordinary-urls-package-constructors-and-registration-methods-are-unchanged
  (doseq [source ["const url = new URL('/bridge/legacy.js', location.origin);"
                  "const url = new URL('./icon.svg', import.meta.url);"
                  "registry.register('/bridge/legacy.js');"
                  "import { Worker } from 'node:worker_threads'; new Worker('/server.js');"
                  "import SharedWorker from 'worker-package'; new SharedWorker(workerUrl);"
                  "type BrowserWorker = Worker; type Constructor = typeof Worker;"
                  "const available = typeof Worker !== 'undefined';"
                  "const available = typeof navigator.serviceWorker.register === 'function';"
                  "navigator.serviceWorker.getRegistration('/scope');"
                  "navigator.serviceWorker.getRegistrations();"
                  "const constructorNames = {Worker: 'browser', SharedWorker: 'shared'};"
                  "// navigator.serviceWorker.register('/bridge/legacy.js');"
                  "const example = \"new Worker('/bridge/legacy.js')\";"]]
    (t/is (nil? (inspect-imports! source)) source)))
