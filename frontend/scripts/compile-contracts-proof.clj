(ns compile-contracts-proof
  (:require [clojure.edn :as edn]
            [shadow.cljs.devtools.api :as shadow]))

(shadow/with-runtime
  (let [config (edn/read-string (slurp "shadow-cljs.edn"))]
    (shadow/compile*
     (-> (get-in config [:builds :test])
         (assoc :build-id :contracts-proof
                :output-to "dist-verification/contracts-tests.cjs"
                :ns-regexp "^knoxx\\.frontend\\.pages\\.contracts\\..*-test$")) {}))
  nil)
