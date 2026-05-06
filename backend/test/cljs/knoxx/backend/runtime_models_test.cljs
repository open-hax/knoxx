(ns knoxx.backend.runtime-models-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.runtime.models :as models]))

(def test-config
  (models/enrich-config {:contracts-dir "contracts"
                         :proxx-base-url "http://127.0.0.1:8787"
                         :proxx-default-model "glm-5"}))

(deftest models-config-preserves-eta-mu-registry-inputs
  (testing "models.json keeps eta-mu-supported text/image/audio inputs from contracts"
    (let [config (models/models-config test-config ["gpt-5" "gemma4:31b"])
          provider (get-in config [:providers :proxx])
          model-inputs (into {}
                             (map (juxt :id :input))
                             (:models provider))]
      (is (= ["text" "image" "audio"] (get model-inputs "gpt-5")))
      (is (= ["text" "image"] (get model-inputs "gemma4:31b"))))))

(deftest models-config-includes-contract-models-when-proxx-discovery-omits-them
  (testing "eta-mu can create sessions for contract-selected models even if /v1/models did not list them"
    (let [config (models/models-config test-config ["glm-5"])
          model-ids (->> (get-in config [:providers :proxx :models])
                         (map :id)
                         set)]
      (is (contains? model-ids "glm-5"))
      (is (contains? model-ids "gemma4:31b")))))

(deftest provider-model-config-routes-gpt-family-through-responses
  (testing "gpt-family models use OpenAI Responses with reasoning enabled"
    (let [model (models/provider-model-config test-config "gpt-5")]
      (is (= "openai-responses" (:api model)))
      (is (true? (:reasoning model)))
      (is (= ["text" "image" "audio"] (:input model))))))

(deftest provider-model-config-keeps-gemma-on-chat-completions
  (testing "gemma-family models stay on OpenAI-compatible chat completions while exposing reasoning"
    (let [model (models/provider-model-config test-config "gemma4:31b")]
      (is (= "openai-completions" (:api model)))
      (is (true? (:reasoning model))))))

(deftest gemma-family-contract-enables-thinking-levels
  (testing "family contracts allow gemma4 variants to request Knoxx thinking levels"
    (let [model (models/provider-model-config test-config "gemma4:e4b")]
      (is (= "openai-completions" (:api model)))
      (is (true? (:reasoning model)))
      (is (= ["text" "image"] (:input model)))
      (is (= {:supportsDeveloperRole false
              :supportsReasoningEffort true}
             (models/per-model-compat test-config "gemma4:e4b")))
      (is (= "off" (models/effective-thinking-level test-config "gemma4:e4b" nil)))
      (is (= "minimal" (models/effective-thinking-level test-config "gemma4:e4b" "minimal")))
      (is (= "xhigh" (models/effective-thinking-level test-config "gemma4:e4b" "xhigh"))))))
