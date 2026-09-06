(ns knoxx.frontend.infra.migration-imports-test
  (:require [cljs.test :as t]
            [knoxx.frontend.infra.migration-imports :as imports]))

(defn- inspect-imports! [source]
  (imports/assert-contained!
    {:root "/repository"
     :source-root "/repository/frontend/src"
     :options #js {}
     :aliases {"@" "/repository/frontend/src"}}
    "frontend/src/pages/page.ts" source))

(t/deftest vite-globs-cannot-conceal-relocated-source
  (doseq [source ["const pages = import.meta.glob('../../legacy/*.ts');"
                  "const pages = import.meta.glob(['./*.ts', '../../legacy/*.tsx']);"
                  "const pages = import.meta.glob('legacy/*.ts', { eager: true });"
                  "const pages = import.meta.glob('./*.ts', { base: '../../legacy' });"
                  "const pages = import.meta['glob']('../../legacy/*.ts');"
                  "const pages = import.meta.globEager('../../legacy/*.ts');"]]
    (t/is (thrown-with-msg?
            js/Error #"Vite glob imports are outside the supported migration grammar"
            (inspect-imports! source)))))

(t/deftest contained-globs-also-report-the-unsupported-grammar
  (t/is (thrown-with-msg?
          js/Error #"Vite glob imports are outside the supported migration grammar"
          (inspect-imports! "const pages = import.meta.glob('./*.ts');"))))

(t/deftest glob-text-and-ordinary-import-meta-do-not-create-dependencies
  (doseq [source ["// import.meta.glob('../../legacy/*.ts')\nexport const Page = 1;"
                  "const help = \"import.meta.glob('../../legacy/*.ts')\";"
                  "const help = `import.meta.glob('../../legacy/*.ts')`;"
                  "const mode = import.meta.env.MODE; const url = import.meta.url;"
                  "const pages = other.glob('../../legacy/*.ts');"]]
    (t/is (nil? (inspect-imports! source)))))

(t/deftest worker-url-dependencies-cannot-leave-the-governed-source-tree
  (doseq [constructor ["Worker" "SharedWorker"]
          target ["'../../legacy/worker.ts'" "`../../legacy/worker.ts`"
                  "'@/../../legacy/worker.ts'"]]
    (t/is (thrown-with-msg?
            js/Error #"Local import leaves governed frontend source tree"
            (inspect-imports!
              (str "new " constructor "(new URL(" target
                   ", import.meta.url), {type: 'module'});"))))))

(t/deftest root-relative-worker-urls-use-the-vite-project-root
  (t/is (nil? (inspect-imports! "new Worker(new URL('/src/workers/task.ts', import.meta.url));")))
  (t/is (thrown-with-msg?
          js/Error #"Local import leaves governed frontend source tree"
          (inspect-imports! "new Worker(new URL('/legacy/task.ts', import.meta.url));"))))

(t/deftest contained-worker-urls-retain-supported-source-boundaries
  (doseq [source ["new Worker(new URL('./worker.ts', import.meta.url));"
                  "new SharedWorker(new URL('../workers/task.ts', import.meta.url));"
                  "new Worker(new URL('@/workers/task.ts', import.meta.url));"
                  "new Worker(new URL(`./worker.ts`, import.meta.url));"]]
    (t/is (nil? (inspect-imports! source)))))

(t/deftest dynamic-worker-urls-fail-explicitly
  (t/is (thrown-with-msg?
          js/Error #"Unsupported dynamic worker URL in migration source"
          (inspect-imports! "new Worker(new URL(`./${name}.ts`, import.meta.url));"))))

(t/deftest ordinary-url-values-and-worker-example-text-remain-supported
  (doseq [source ["const url = new URL(base);"
                  "const worker = new Worker(new URL(absoluteUrl));"
                  "const url = new URL(url, window.location.origin);"
                  "// new Worker(new URL('../../legacy/worker.ts', import.meta.url))"
                  "const help = \"new Worker(new URL('../../legacy/worker.ts', import.meta.url))\";"]]
    (t/is (nil? (inspect-imports! source)))))

(t/deftest variable-dynamic-imports-cannot-conceal-relocated-source
  (doseq [source ["import(`../../legacy/${name}.ts`);"
                  "import('../../legacy/' + name + '.ts');"
                  "import(modulePath);"
                  "import(`./${name}.ts`);"]]
    (t/is (thrown-with-msg?
            js/Error #"Non-literal dynamic imports are outside the supported migration grammar"
            (inspect-imports! source)))))

(t/deftest literal-dynamic-imports-retain-source-containment
  (doseq [source ["import('../../legacy/page.ts');"
                  "import(`../../legacy/page.ts`);"]]
    (t/is (thrown-with-msg?
            js/Error #"Local import leaves governed frontend source tree"
            (inspect-imports! source))))
  (doseq [source ["import('./page.ts');" "import(`./page.ts`);"
                  "const help = 'import(`../../legacy/${name}.ts`)';"
                  "// import(`../../legacy/${name}.ts`)\nexport const Page = 1;"
                  "type Page = import('./page').Page;"]]
    (t/is (nil? (inspect-imports! source)))))
