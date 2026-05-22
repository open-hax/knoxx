(ns knoxx.backend.extern-extension-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.extension :as extension]))

(deftest extension-event-payload-builds-js-event-objects
  (testing "agent session orchestration can pass CLJS lifecycle event data to the extension boundary"
    (let [payload (extension/event-payload {:conversationId "conv-1"
                                            :sessionId "sess-1"})]
      (is (= "conv-1" (aget payload "conversationId")))
      (is (= "sess-1" (aget payload "sessionId"))))))

(deftest empty-event-payload-builds-empty-js-object
  (testing "session shutdown can build an opaque empty runtime placeholder"
    (let [payload (extension/empty-event-payload)]
      (is (= 0 (.-length (.keys js/Object payload)))))))
