(ns knoxx.backend.session-store-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.sessions.session-store :as session-store]))

(deftest session-can-send-blocks-all-running-sessions
  (testing "running sessions stay write-locked even before the first streamed token"
    (is (= {:can-send false
            :reason "Session is already processing. Use steer, follow-up, abort, or wait."}
           (session-store/session-can-send? {:status "running"
                                             :has_active_stream false})))
    (is (= {:can-send false
            :reason "Session is actively streaming. Use steer or wait."}
           (session-store/session-can-send? {:status "running"
                                             :has_active_stream true})))))

(deftest rewind-messages-drops-latest-user-turn
  (testing "it preserves the session seed while removing the latest exchange"
    (is (= [{:role "system" :content "seed"}
            {:role "user" :content "first"}
            {:role "assistant" :content "first answer"}]
           (session-store/rewind-messages
            [{:role "system" :content "seed"}
             {:role "user" :content "first"}
             {:role "assistant" :content "first answer"}
             {:role "user" :content "second"}
             {:role "assistant" :content "second answer"}]
            1)))))

(deftest rewind-messages-can-remove-multiple-turns
  (testing "it can rewind multiple user turns at once"
    (is (= [{:role "system" :content "seed"}]
           (session-store/rewind-messages
            [{:role "system" :content "seed"}
             {:role "user" :content "first"}
             {:role "assistant" :content "first answer"}
             {:role "user" :content "second"}
             {:role "assistant" :content "second answer"}]
            2)))))

(deftest rewind-messages-noops-when-no-user-turns-exist
  (testing "it leaves system-only sessions untouched"
    (is (= [{:role "system" :content "seed"}]
           (session-store/rewind-messages [{:role "system" :content "seed"}] 1)))))
