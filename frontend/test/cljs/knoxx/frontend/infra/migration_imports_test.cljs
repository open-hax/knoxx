(ns knoxx.frontend.infra.migration-imports-test
  (:require [cljs.test :as t]
            [knoxx.frontend.infra.migration-imports :as imports]))

(defn- inspect-imports! [source]
  (imports/assert-contained!
    {:root "/repository"
     :source-root "/repository/frontend/src"
     :options #js {}
     :aliases {}}
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
