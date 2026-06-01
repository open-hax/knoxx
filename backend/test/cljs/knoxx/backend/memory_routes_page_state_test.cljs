(ns knoxx.backend.memory-routes-page-state-test
  (:require [cljs.test :refer [deftest is]]
            [knoxx.backend.shape.memory-sessions :as memory-shape]))

(deftest memory-sessions-page-state-builds-visible-window
  (let [cache {:hit false :tier "miss"}
        opts {:offset 1
              :limit 2
              :actor-id "chat_primary"
              :exclude-actor-ids ["pi"]
              :contract-id "creative_music_studio"}
        state (memory-shape/page-state
               {:rows [{:session "s1"}
                       {:session "s2"}
                       {:session "s3"}
                       {:session "s4"}]
                :has_more false}
               opts
               cache)]
    (is (= [{:session "s2"} {:session "s3"}] (:page-rows state)))
    (is (= 4 (:visible-total state)))
    (is (true? (:page-has-more state)))
    (is (= "chat_primary" (:actor-id state)))
    (is (= ["pi"] (:exclude-actor-ids state)))
    (is (= "creative_music_studio" (:contract-id state)))
    (is (= cache (:cache state)))))
