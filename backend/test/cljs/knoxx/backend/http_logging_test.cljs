(ns knoxx.backend.http-logging-test
  "Observe actual Fastify/Pino request records through an isolated memory sink."
  (:require ["fastify" :default fastify]
            ["node:stream" :refer [Writable]]
            [cljs.test :as test]
            [clojure.string :as str]
            [knoxx.backend.extern.http-logging :as logging]))

(test/deftest ^:async request-logs-retain-path-without-callback-or-credential-data
  (let [chunks (atom [])
        output (Writable. #js {:write (fn [chunk _encoding done]
                                       (swap! chunks conj (.toString chunk))
                                       (done))})
        app (fastify #js {:logger (logging/options output)})]
    (.get app "/api/auth/axxium/callback" (fn [_request reply] (.send reply "fixture")))
    (try
      (await (.inject app #js {:method "GET"
                               :url "/api/auth/axxium/callback?code=fixture-code-secret&state=fixture-state-secret"
                               :headers #js {:authorization "Bearer fixture-bearer-secret"
                                             :cookie "knoxx_session=fixture-cookie-secret"}}))
      (let [raw (apply str @chunks)
            records (map #(js->clj (js/JSON.parse %) :keywordize-keys true) (str/split-lines raw))
            request (:req (first (filter :req records)))]
        (test/is (= "/api/auth/axxium/callback" (:path request)))
        (test/is (= "GET" (:method request)))
        (test/is (not (contains? request :url)))
        (test/is (not (re-find #"fixture-(code|state|bearer|cookie)-secret" raw))))
      (finally
        (await (.close app))
        (.end output)))))
