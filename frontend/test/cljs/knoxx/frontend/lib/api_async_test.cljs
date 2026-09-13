(ns knoxx.frontend.lib.api-async-test
  "Async HTTP errors retain the native transport's public rejection contract."
  (:require [cljs.test :as test]
            [knoxx.frontend.lib.api :as api]))

(test/deftest ^:async native-transport-preserves-server-and-empty-body-errors
  (let [original-fetch js/fetch
        status (atom 403)
        body (atom "contract role required")]
    (try
      (set! (.-fetch js/globalThis)
            (fn [& _]
              (js/Promise.resolve (js/Response. @body #js {:status @status}))))
      (test/is (= "contract role required"
                  (try (await (api/request "/api/forbidden")) nil
                       (catch :default cause (.-message cause)))))
      (reset! status 502)
      (reset! body "")
      (test/is (= "Request to /api/unavailable failed (502)"
                  (try (await (api/request "/api/unavailable")) nil
                       (catch :default cause (.-message cause)))))
      (finally (set! (.-fetch js/globalThis) original-fetch)))))
