(ns kms-ingestion.api.routes-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clj-http.client :as http]
   [kms-ingestion.api.routes :as routes]
   [kms-ingestion.config :as config]))

(deftest call-proxx-chat-uses-configured-timeouts
  (testing "Proxx chat requests honor the configured timeout budget"
    (let [captured (atom nil)]
      (with-redefs [config/proxx-url (constantly "http://proxx.test")
                    config/proxx-auth-token (constantly "secret-token")
                    config/proxx-default-model (constantly "glm-5")
                    config/proxx-connection-timeout-ms (constantly 1500)
                    config/proxx-socket-timeout-ms (constantly 3200)
                    http/post (fn [url opts]
                                (reset! captured {:url url :opts opts})
                                {:status 503
                                 :body {:error {:message "upstream timeout"}}})]
        (let [result (routes/call-proxx-chat {:messages [{:role "user" :content "hello"}]})]
          (is (= false (:ok result)))
          (is (= 503 (:status result)))
          (is (= "http://proxx.test/v1/chat/completions" (get @captured :url)))
          (is (= 1500 (get-in @captured [:opts :connection-timeout])))
          (is (= 3200 (get-in @captured [:opts :socket-timeout])))
          (is (= "Bearer secret-token" (get-in @captured [:opts :headers "Authorization"]))))))))

(deftest answer-handler-surfaces-proxx-failure
  (testing "The chat answer endpoint returns a clear upstream error when Proxx is unavailable"
    (with-redefs [routes/federated-fts (fn [_]
                                         {:projects ["devel-docs"]
                                          :count 1
                                          :rows [{:project "devel-docs"
                                                  :source "kms-ingestion"
                                                  :kind "docs"
                                                  :ts "2026-04-03T20:00:00Z"
                                                  :message "docs/reference/chat.md"
                                                  :snippet "Chat fallback context"}]})
                  routes/call-proxx-chat (fn [_]
                                           {:ok false
                                            :status 0
                                            :error "timed out waiting for Proxx"})]
      (let [response (routes/answer-handler {:params {}
                                             :query-params {}
                                             :body "{\"q\":\"why is chat slow?\",\"role\":\"workspace\"}"})]
        (is (= 504 (:status response)))
        (is (= "proxx_timeout" (get-in response [:body :error_code])))
        (is (= "timed out waiting for Proxx" (get-in response [:body :model_error])))
        (is (= ["devel-docs"] (get-in response [:body :projects])))
        (is (= 1 (get-in response [:body :count])))
        (is (re-find #"timed out" (get-in response [:body :error])))))))

(deftest devel-answer-system-prompt-pushes-synthesis-over-counting
  (let [prompt (routes/devel-answer-system-prompt {:projects ["devel-docs" "devel-code"]
                                                   :context-found? true})]
    (is (re-find #"Synthesize across snippets" prompt))
    (is (re-find #"Do not default to frequency-counting" prompt))
    (is (re-find #"## Answer, ## Why it matters, ## Evidence" prompt))
    (is (re-find #"devel-docs, devel-code" prompt))))

(deftest build-answer-user-prompt-enforces-structured-grounded-output
  (let [prompt (routes/build-answer-user-prompt
                {:q "What should we do next?"
                 :projects ["devel-docs" "devel-code"]
                 :rows [{:project "devel-docs"
                         :kind "docs"
                         :source_path "docs/plan.md"
                         :snippet "Prioritize grounded synthesis before architecture churn."}
                        {:project "devel-code"
                         :kind "code"
                         :source_path "src/app.ts"
                         :snippet "Prompt builder currently emits raw context."}]})]
    (is (re-find #"## Answer" prompt))
    (is (re-find #"## Why it matters" prompt))
    (is (re-find #"Do not pad with counts" prompt))
    (is (re-find #"docs/plan.md" prompt))
    (is (re-find #"src/app.ts" prompt))))
