(ns release-contracts-proof
  (:require [clojure.edn :as edn]
            [shadow.cljs.devtools.api :as shadow]))

(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))
        build (-> (get-in config [:builds :app])
                  (assoc :build-id :contracts-app-proof :output-dir "dist-verification/contracts-cljs")
                  (assoc-in [:js-options :resolve "@open-hax/knoxx-app-bridge" :file]
                            "dist-verification/contracts-bridge/knoxx-app-bridge.es.js")
                  (assoc-in [:js-options :resolve "@open-hax/knoxx-frontend-bridge" :file]
                            "dist-verification/contracts-bridge/knoxx-frontend-bridge.es.js"))]
    (shadow/release* build {}))
  nil)
