(ns knoxx.frontend.pages.cms.api-test
  "Authenticated command wire regressions, including namespaced resource identities."
  (:require [cljs.test :as t]
            [knoxx.frontend.lib.api :as http]
            [knoxx.frontend.pages.cms.api :as api]
            [knoxx.frontend.pages.cms.logic-test :as fixture]))

(t/deftest ^:async source-writes-preserve-the-entire-document-identity
  (let [calls (atom []) original http/request
        payload {:operation_id "repair/1" :expected_revision "old" :content "Revised"}]
    (set! http/request (fn ([path] (swap! calls conj [path]) (js/Promise.resolve fixture/snapshot))
                         ([path opts] (swap! calls conj [path opts])
                          (js/Promise.resolve {:review fixture/snapshot}))))
    (try
      (await (api/save-source "wiki/qualified-name" payload))
      (t/is (= [["/api/publications/documents/wiki%2Fqualified-name/source"
                 {:method "PATCH" :body payload}]] @calls))
      (finally (set! http/request original)))))

(t/deftest ^:async publication-target-and-source-identities-remain-distinct
  (let [calls (atom []) original http/request
        payload {:publication "placements/spanish" :expected_revision "source-digest"}]
    (set! http/request (fn ([path] (swap! calls conj [path]) (js/Promise.resolve fixture/snapshot))
                         ([path opts] (swap! calls conj [path opts])
                          (js/Promise.resolve {:review fixture/snapshot :publication {} :receipt {}}))))
    (try
      (await (api/publish "wiki/source" payload))
      (t/is (= [["/api/publications/documents/wiki%2Fsource/publish"
                 {:method "POST" :body payload}]] @calls))
      (finally (set! http/request original)))))

(t/deftest queue-overflow-and-reconnect-refresh-markers-are-admitted
  (t/is (= {:document nil} (api/decode-change "{\"document\":null}")))
  (t/is (= {:document "wiki/name"} (api/decode-change "{\"document\":\"wiki/name\"}")))
  (t/is (nil? (api/decode-change "{}")))
  (t/is (nil? (api/decode-change "{\"document\":42}")))
  (t/is (nil? (api/decode-change "invalid JSON"))))
