(ns knoxx.backend.session-store-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.session-store :as session-store]))

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