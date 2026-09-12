(require '[clojure.edn :as edn]
         '[shadow.cljs.devtools.api :as shadow])
(set! *warn-on-reflection* true)
(try
 (shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        build (-> (get-in config [:builds :test-ci])
                  (assoc :build-id :app-proof
                         :output-to "target/app-proof/tests.cjs"
                         :ns-regexp "knoxx\\.backend\\.(infra\\.routes\\.app-(data-mongo|lifecycle)-test|defroute-async-test|shape\\.agent-chat-input-test)$"))]
    (shadow/compile* build {}))
  nil)
 (catch Throwable error
   (binding [*out* *err*] (println (ex-message error)))
   (System/exit 1)))
