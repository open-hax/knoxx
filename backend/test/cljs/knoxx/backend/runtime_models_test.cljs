(ns knoxx.backend.runtime-models-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.runtime.models :as models]))

(def test-config
  (models/enrich-config {:contracts-dir "contracts"
                         :proxx-base-url "http://127.0.0.1:8787"
                         :proxx-default-model "glm-5"}))

(deftest provider-model-config-routes-gpt-family-through-responses
  (testing "gpt-family models use OpenAI Responses with reasoning enabled"
    (let [model (models/provider-model-config test-config "gpt-5")]
      (is (= "openai-responses" (:api model)))
      (is (true? (:reasoning model))))))

(deftest provider-model-config-keeps-gemma-on-chat-completions
  (testing "non-responses families stay on OpenAI-compatible chat completions"
    (let [model (models/provider-model-config test-config "gemma4:31b")]
      (is (= "openai-completions" (:api model)))
      (is (false? (:reasoning model))))))