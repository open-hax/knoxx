(ns knoxx.backend.extern-proxx-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.json :as xjson]
            [knoxx.backend.extern.js :as xjs]
            [knoxx.backend.extern.proxx :as proxx]))

(deftest parse-object-keywordizes-json
  (testing "JSON strings become CLJS maps at the extern boundary"
    (is (= {:title "Boundary Cleanup"}
           (xjson/parse-object "{\"title\":\"Boundary Cleanup\"}"))))
  (testing "CLJS maps pass through unchanged"
    (is (= {:already true}
           (xjson/parse-object {:already true}))))
  (testing "invalid JSON resolves to nil instead of leaking JS exceptions"
    (is (nil? (xjson/parse-object "not json")))))

(deftest ^:async chat-completion-maps-openai-shaped-response
  (let [seen* (atom nil)
        fetch-json (fn [url opts]
                     (reset! seen* {:url url :opts opts})
                     (js/Promise.resolve
                      (xjs/object
                       {:ok true
                        :status 200
                        :body {:model "title-model"
                               :choices [{:message {:content "Boundary Cleanup"
                                                    :reasoning_content "I will go with \"Boundary Cleanup\""}}]}})))]
    (let [result (await (proxx/chat-completion-with-fetch!
                         fetch-json
                         {:proxx-base-url "http://proxx.local"
                          :proxx-auth-token "token"}
                         {:model "auto:cheapest"
                          :messages [{:role "user" :content "hello"}]
                          :temperature 0.1
                          :max_tokens 24
                          :stream false}))]
      (is (= "http://proxx.local/v1/chat/completions" (:url @seen*)))
      (is (= {:ok? true
              :status 200
              :model "title-model"
              :content "Boundary Cleanup"
              :reasoning-content "I will go with \"Boundary Cleanup\""}
             (select-keys result [:ok? :status :model :content :reasoning-content]))))))
