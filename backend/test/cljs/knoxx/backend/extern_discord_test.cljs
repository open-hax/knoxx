(ns knoxx.backend.extern-discord-test
  (:require [cljs.test :refer [deftest is async]]
            [knoxx.backend.extern.discord :as xdiscord]))

(deftest normalize-tool-params-keywordizes-runtime-objects
  (let [params (xdiscord/normalize-tool-params #js {:channel_id "c1"
                                                    :channelId "c2"
                                                    :attachment_urls #js [" a " ""]})]
    (is (= "c1" (:channel_id params)))
    (is (= "c2" (:channelId params)))
    (is (= [" a " ""] (:attachment_urls params)))))

(deftest promise-all-vector-resolves-cljs-vector
  (async done
    (-> (xdiscord/promise-all-vector [(js/Promise.resolve {:id "a"})
                                      (js/Promise.resolve {:id "b"})])
        (.then (fn [results]
                 (is (= [{:id "a"} {:id "b"}] results))))
        (.catch (fn [err]
                  (is nil (str "unexpected rejection: " err))))
        (.finally done))))

(deftest message-form-data-builds-discord-payload-and-files
  (let [form (xdiscord/message-form-data
              {:payload {:content "hello" :message_reference {:message_id "m1"}}
               :files [{:name "note.txt"
                        :mimeType "text/plain"
                        :buffer (js/Buffer.from "abc")}]} )]
    (is (true? (instance? js/FormData form)))
    (is (= "{\"content\":\"hello\",\"message_reference\":{\"message_id\":\"m1\"}}"
           (.get form "payload_json")))
    (is (= "note.txt" (.-name (.get form "files[0]"))))))

(deftest gateway-started-reads-sdk-status-object
  (let [manager #js {:status (fn [] #js {:ready true})}]
    (is (true? (xdiscord/gateway-started? manager)))))
