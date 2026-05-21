(ns knoxx.backend.extern-agent-turn-media-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.agent-turn-media :as agent-turn-media]
            [knoxx.backend.extern.json :as xjson]))

(deftest media-url-primitives-normalize-values
  (testing "relative media URLs resolve to the local Knoxx API origin"
    (is (= "http://127.0.0.1:8000/api/studio/stream?path=audio%2Fclip.wav"
           (agent-turn-media/resolve-media-url "/api/studio/stream?path=audio%2Fclip.wav"))))
  (testing "data URL helpers expose raw base64 payloads without metadata"
    (is (true? (agent-turn-media/data-url? "data:audio/wav;base64,UklGRg==")))
    (is (= "UklGRg=="
           (agent-turn-media/strip-data-url "data:audio/wav;base64,UklGRg==")))))

(deftest studio-stream-path-extracts-workspace-path
  (is (= "audio/clip.wav"
         (agent-turn-media/studio-stream-path "/api/studio/stream?path=audio%2Fclip.wav"))))

(deftest local-auth-headers-stay-local
  (testing "local Knoxx URLs receive the authenticated user/org context"
    (is (= {:x-knoxx-user-email "agent@example.test"
            :x-knoxx-org-slug "open-hax"
            :x-knoxx-membership-id "mem-1"}
           (xjson/to-cljs
            (agent-turn-media/auth-headers
             {:userEmail "agent@example.test"
              :orgSlug "open-hax"
              :membershipId "mem-1"}
             "http://127.0.0.1:8000/api/studio/stream?path=clip.wav")))))
  (testing "external URLs do not receive Knoxx auth headers"
    (is (= {}
           (xjson/to-cljs
            (agent-turn-media/auth-headers
             {:userEmail "agent@example.test"
              :orgSlug "open-hax"
              :membershipId "mem-1"}
             "https://example.invalid/image.png"))))))
