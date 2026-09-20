(require '[shadow.cljs.devtools.api :as shadow])
(try
  (shadow/with-runtime
    (shadow/compile*
     {:build-id :openplanner-process-proof
      :target :node-library
      :output-to "target/openplanner-process-proof/probe.cjs"
      :exports {:prepare 'knoxx.backend.extern.openplanner-admission-probe/prepare!
                :write 'knoxx.backend.extern.openplanner-admission-probe/write!
                :verify 'knoxx.backend.extern.openplanner-admission-probe/verify!}
      :compiler-options {:output-feature-set :es-next}}
     {})
    nil)
  (catch Throwable error
    (binding [*out* *err*] (println (ex-message error)))
    (binding [*warn-on-reflection* true] (System/exit 1))))
