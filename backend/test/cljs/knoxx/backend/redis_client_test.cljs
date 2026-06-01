(ns knoxx.backend.redis-client-test
  (:require [cljs.test :refer [async deftest is testing]]
            [knoxx.backend.infra.redis-client :as redis]))

(deftest ^:async redis-command-wrappers-coerce-non-string-values
  (testing "set-key and sadd serialize CLJS values into Redis-safe strings"
    (let [calls* (atom [])
          client #js {:set (fn [key value opts]
                             (swap! calls* conj [:set key value (when opts (aget opts "EX"))])
                             (js/Promise.resolve "OK"))
                      :sAdd (fn [key member]
                              (swap! calls* conj [:sAdd key member])
                              (js/Promise.resolve 1))}]
      (await (redis/set-key client "knoxx:conversation_to_session:test" {:session/id 42} 3600))
      (await (redis/sadd client "knoxx:active_sessions" {:session/id 42}))
      (is (= [[:set "knoxx:conversation_to_session:test" "{\"id\":42}" 3600]
              [:sAdd "knoxx:active_sessions" "{\"id\":42}"]]
             @calls*)))))
