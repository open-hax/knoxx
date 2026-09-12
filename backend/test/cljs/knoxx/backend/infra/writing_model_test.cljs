(ns knoxx.backend.infra.writing-model-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as str]
            [knoxx.backend.extern.fetch :as xfetch]
            [knoxx.backend.extern.promise :as xpromise]
            [knoxx.backend.infra.writing-model :as sut]
            [knoxx.backend.law.writing-model :as law]))

(def ^:private input {:instruction "Improve clarity." :content "A draft." :lessons ["Keep precise names."]})
(def ^:private config {:wiki-model-provider :openai-compatible :wiki-model "local-model"
                      :wiki-model-base-url "http://127.0.0.1:4321/v1/"})
(def ^:private response {:ok true :status 200 :body {:model "local-model"
                                                    :choices [{:finish_reason "stop"
                                                               :message {:role "assistant" :content "A clearer draft."}}]}})

(deftest bounded-explicit-context
  (is (str/includes? (sut/prompt input) "A draft."))
  (is (str/includes? (sut/prompt input) "Keep precise names."))
  (is (thrown? cljs.core/ExceptionInfo (sut/prompt (assoc input :content nil))))
  (is (thrown? cljs.core/ExceptionInfo (sut/prompt (assoc input :instruction (apply str (repeat 10001 "x")))))))

(deftest ^:async writing-preserves-explicit-request-and-complete-output
  (let [seen (atom nil)]
    (with-redefs [xfetch/default-client (reify xfetch/IHttpClient (json! [_ request] (reset! seen request) response))]
      (is (= "A clearer draft." (:content (await (sut/generate! config input)))))
    (is (= "http://127.0.0.1:4321/v1/chat/completions" (:url @seen)))
    (is (= (sut/prompt input) (get-in @seen [:opts :json :messages 1 :content])))
    (is (false? (get-in @seen [:opts :json :stream])))
    (is (nil? (get-in @seen [:opts :json :tools]))))))

(deftest ^:async writing-refuses-incomplete-or-tool-output
  (doseq [body [(assoc (:body response) :model "different")
                (assoc-in (:body response) [:choices 0 :finish_reason] "length")
                (assoc-in (:body response) [:choices 0 :message :tool_calls] [{:name "save"}])
                (assoc-in (:body response) [:choices 0 :message :function_call] {:name "save"})
                (assoc-in (:body response) [:choices 0 :message :content] "")]]
    (with-redefs [xfetch/default-client (reify xfetch/IHttpClient (json! [_ _] (assoc response :body body)))]
      (try (await (sut/generate! config input)) (is false "Invalid provider result was accepted")
           (catch :default error (is (= "writing_model_invalid_output" (:code (ex-data error)))))))))

(deftest transcript-contract-rejects-tool-events-and-truncation
  (let [events [{:type "text" :sessionID "s1" :part {:text "Proposal"}}
                {:type "step_finish" :sessionID "s1" :part {:reason "stop"}}]]
    (is (= "Proposal" (:content (law/transcript-result events "opencode/big-pickle"))))
    (testing "Exit success alone has no authority"
      (is (thrown? cljs.core/ExceptionInfo (law/transcript-result [] "opencode/big-pickle")))
      (is (thrown? cljs.core/ExceptionInfo (law/transcript-result (assoc-in events [1 :part :reason] "length") "opencode/big-pickle")))
      (is (thrown? cljs.core/ExceptionInfo (law/transcript-result (assoc-in events [0 :type] "tool_use") "opencode/big-pickle"))))))

(deftest ^:async writing-deadline-includes-response-body
  (with-redefs [xfetch/default-client (reify xfetch/IHttpClient
                                       (json! [_ _] (xpromise/reject-after 100 "Late response body")))]
    (try (await (sut/generate! (assoc config :wiki-model-timeout-ms 5) input))
         (is false "Late response body escaped completion deadline")
         (catch :default error
           (is (= "writing_model_timeout" (:code (ex-data error))))
           (is (= 504 (:status (ex-data error))))))))
