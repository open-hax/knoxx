(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))]
    (shadow/compile*
     {:build-id :local-model-recovery-live
      :target :node-library
      :output-to "target/local-model-recovery-live/library.cjs"
      :js-options (get-in config [:builds :test-ci :js-options])
      :compiler-options (get-in config [:builds :test-ci :compiler-options])
      :exports '{:generate knoxx.backend.infra.writing-model/generate!
                 :embed knoxx.backend.infra.clients.local-embedding/embed!
                 :complete knoxx.backend.infra.translation-agent-structured-output/complete-turn!
                 :admitted knoxx.backend.infra.translation-agent-structured-output-test/admitted!
                 :baseDeps knoxx.backend.infra.translation-agent-structured-output-test/base-deps
                 :candidates knoxx.backend.infra.translation-split-store/candidate-splits-for-turn!}}
     {}))
  nil)
