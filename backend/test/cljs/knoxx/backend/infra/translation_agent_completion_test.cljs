(ns knoxx.backend.infra.translation-agent-completion-test
  (:require [cljs.test :refer [deftest is]]
            [clojure.string :as str]
            [knoxx.backend.domain.models :as models]
            [knoxx.backend.extern.opencode-writing :as opencode]
            [knoxx.backend.extern.translation-agent-structured-output :as xstructured]
            [knoxx.backend.infra.translation-agent-completion :as sut]))

(def ^:private model "onnx-community/Qwen2.5-0.5B-Instruct")
(def ^:private config {:wiki-model-base-url "http://127.0.0.1:4333/v1"})
(def ^:private turn {:translation-turn/execution {:translation-execution/model model
                                                 :translation-execution/system-prompt "Admitted immutable policy."}
                    :translation-turn/manifest {:split-manifest/source-locale :en
                                                 :split-manifest/target-locale :es}
                    :translation-turn/memory {:translation-memory-snapshot/examples []}})
(def ^:private source {:split/id "split-1" :split/index 0 :split/source-text "Hello, world."})
(def ^:private response
  {:ok true :status 200
   :body {:model model :choices [{:finish_reason "stop"
                                 :message {:role "assistant" :content "{\"translated_text\":\"Hola, mundo.\"}"}}]
          :local_generation {:provider "transformers-js" :offline true
                             :structured_output "transport_wrapped_generated_text"}}})

(deftest ^:async local-translation-declares-transport-wrapper-and-preserves-prompt
  (let [seen (atom nil)]
    (with-redefs [models/resolve-model-contract (fn [_ id] {:id id :provider "transformers-js"})]
      (is (= model (sut/model! config turn)))
      (is (= "Hola, mundo."
             (await (sut/translate! config {:request! #(do (reset! seen %) response)} model turn source 500)))))
    (is (= "Admitted immutable policy." (get-in @seen [:opts :json :messages 0 :content])))
    (is (= "Hello, world." (get-in (xstructured/decode-response-content
                                    (get-in @seen [:opts :json :messages 1 :content])) [:split :source_text])))
    (is (= ["translated_text"] (get-in @seen [:opts :json :response_format :json_schema :schema :required])))
    (is (str/includes? (:instruction (xstructured/decode-response-content
                                     (get-in @seen [:opts :json :messages 1 :content])))
                       "Return only the translated text, without JSON"))
    (is (nil? (get-in @seen [:opts :json :tools])))))

(deftest ^:async local-translation-fails-closed-before-sink-authority
  (with-redefs [models/resolve-model-contract (fn [_ id] {:id id :provider "transformers-js"})]
    (doseq [bad [(assoc-in response [:body :model] "other")
                 (assoc-in response [:body :choices 0 :finish_reason] "length")
                 (assoc-in response [:body :local_generation :offline] false)
                 (assoc-in response [:body :local_generation :structured_output] "none")
                 (assoc-in response [:body :choices 0 :message :function_call] {:name "save"})
                 (assoc-in response [:body :choices 0 :message :content] "{\"translated_text\":\"Hola\",\"org_id\":\"attacker\"}")
                 (assoc-in response [:body :choices 0 :message :content] "{\"translated_text\":null}")]]
      (try (await (sut/translate! config {:request! (constantly bad)} model turn source 500))
           (is false "Invalid model output was accepted")
           (catch :default error (is (some? (:translation-agent-structured-output/error (ex-data error)))))))
    (let [calls (atom 0)]
      (try (await (sut/translate! (assoc config :wiki-model-base-url "https://external.example/v1")
                                 {:request! #(do (swap! calls inc) %)} model turn source 500))
           (is false "External endpoint was accepted")
           (catch :default error (is (= :local-base-url-invalid (:translation-agent-structured-output/error (ex-data error))))))
      (is (zero? @calls)))))

(deftest ^:async opencode-uses-exact-admitted-prompt-and-one-value-schema
  (let [seen (atom nil)]
    (with-redefs [models/resolve-model-contract (fn [_ id] {:id id :provider "opencode"})
                  opencode/complete! (fn [_ request] (reset! seen request)
                                       {:content "{\"translated_text\":\"Hola, mundo.\"}"})]
      (is (= "Hola, mundo." (await (sut/translate! {} {} "opencode/big-pickle" turn source 500)))))
    (is (= "Admitted immutable policy." (:system-prompt @seen)))
    (is (= 500 (:timeout-ms @seen)))
    (is (nil? (:org_id (xstructured/decode-response-content (:prompt @seen)))))))
