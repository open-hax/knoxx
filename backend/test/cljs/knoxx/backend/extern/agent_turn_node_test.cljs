(ns knoxx.backend.extern.agent-turn-node-test
  "Initial session diagnostics cannot disclose provider payloads or credentials."
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.extern.agent-turn-node :as node]))

(deftest initial-session-errors-log-only-classified-diagnostics
  (let [original (.-error js/console)
        calls (atom [])]
    (set! (.-error js/console)
          (fn [_label fields] (swap! calls conj (js->clj fields :keywordize-keys true))))
    (try
      (node/log-initial-session-failure!
       "session" (ex-info "private provider connection details"
                          {:status 503 :code "provider_unavailable"
                           :password "synthetic-test-secret" :query {:payload "private content"}}))
      (node/log-initial-session-failure! "session" (js/Error. "private native error"))
      (is (= [{:session-id "session" :error-data {:status 503 :code "provider_unavailable"}}
              {:session-id "session" :error-data {}}] @calls))
      (finally (set! (.-error js/console) original)))))
