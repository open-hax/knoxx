(ns knoxx.backend.error-observatory-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [knoxx.backend.domain.error-observatory :as errors]))

(deftest safe-json-preserves-namespaced-context-keys
  (let [encoded (errors/safe-json {:context {:run/id "run-1"
                                             :conversation/id "conv-1"
                                             :session/id "session-1"
                                             :actor/id "actor-1"
                                             :model "gemma4:31b"}})]
    (is (str/includes? encoded "\"run/id\""))
    (is (str/includes? encoded "\"conversation/id\""))
    (is (str/includes? encoded "\"session/id\""))
    (is (str/includes? encoded "\"actor/id\""))
    (is (str/includes? encoded "\"model\""))))
