(ns knoxx.backend.extern-agent-turn-node-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.agent-turn-node :as agent-turn-node]
            [knoxx.backend.extern.js :as xjs]))

(deftest random-uuid-returns-rfc4122-shaped-string
  (let [uuid (agent-turn-node/random-uuid!)]
    (is (string? uuid))
    (is (some? (re-matches #"[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}" uuid)))))

(deftest ^:async file-data-url-with-fs-reads-base64-file-content
  (testing "the fs/stat/readFile JS boundary returns a data URL string"
    (let [stat (xjs/object {:isFile (fn [] true)
                            :size 3})
          buffer (xjs/object {:toString (fn [encoding]
                                          (is (= "base64" encoding))
                                          "YWJj")})
          fs (xjs/object {:stat (fn [path]
                                  (is (= "/tmp/example.txt" path))
                                  (js/Promise.resolve stat))
                          :readFile (fn [path]
                                      (is (= "/tmp/example.txt" path))
                                      (js/Promise.resolve buffer))})
          result (await (agent-turn-node/file-data-url-with-fs!
                         fs
                         "/tmp/example.txt"
                         "text/plain"
                         "document"
                         16))]
      (is (= "data:text/plain;base64,YWJj" result)))))
