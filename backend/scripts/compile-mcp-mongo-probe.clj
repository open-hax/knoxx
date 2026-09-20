(require '[shadow.cljs.devtools.api :as shadow])
(try
  (shadow/with-runtime
    (shadow/compile*
     {:build-id :mcp-mongo-probe
      :target :node-library
      :output-to "target/review-proof/mongo-probe.cjs"
      :exports {:verify 'knoxx.backend.extern.mcp-token-inventory-probe/verify!}
      :compiler-options {:output-feature-set :es-next}}
     {})
    nil)
  (catch Throwable error
    (binding [*out* *err*] (println (ex-message error)))
    (binding [*warn-on-reflection* true] (System/exit 1))))
