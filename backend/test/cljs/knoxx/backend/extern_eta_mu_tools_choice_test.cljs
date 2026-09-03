(ns knoxx.backend.extern-eta-mu-tools-choice-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.extern.eta-mu :as eta-mu]))

(deftest ^:async required-first-only-forces-the-initiating-ollama-user-request
  (let [calls* (atom [])
        payload-calls* (atom [])
        original-on-payload (fn [payload model]
                              (swap! payload-calls* conj {:payload payload
                                                         :model model})
                              (aset payload "originalHook" true)
                              (js/Promise.resolve payload))
        original-stream (fn [model context options]
                          (swap! calls* conj {:model model
                                             :context context
                                             :options options})
                          :stream-result)
        raw-agent #js {:streamFn original-stream}
        raw-session #js {:agent raw-agent}
        create-session (fn [_options]
                         (js/Promise.resolve #js {:session raw-session}))
        user-context #js {:messages #js [#js {:role "user"}]
                          :tools #js [#js {:name "save_publication_draft"}]}
        post-tool-context #js {:messages #js [#js {:role "user"}
                                              #js {:role "assistant"}
                                              #js {:role "toolResult"}]}
        model #js {:provider "ollama"
                   :id "gemma4:e2b"}
        initial-options #js {:temperature 0.2
                             :onPayload original-on-payload}
        post-tool-options #js {:temperature 0.3}
        request-payload #js {:messages #js []}]
    (with-redefs [eta-mu/create-agent-session-fn (fn [] create-session)]
      (await (eta-mu/create-session! {:tools-choice :required-first}))
      (let [wrapped-stream (aget raw-agent "streamFn")]
        (is (= :stream-result
               (wrapped-stream model user-context initial-options)))
        (is (= :stream-result
               (wrapped-stream model post-tool-context post-tool-options)))))
    (let [[initial-call post-tool-call] @calls*
          forced-options (:options initial-call)
          unforced-options (:options post-tool-call)
          seeded-payload (await ((aget forced-options "onPayload") request-payload model))]
      (testing "the initial user turn receives a cloned required choice"
        (is (not (identical? initial-options forced-options)))
        (is (= "function" (some-> forced-options
                                   (aget "toolChoice")
                                   (aget "type"))))
        (is (= "save_publication_draft"
               (some-> forced-options
                       (aget "toolChoice")
                       (aget "function")
                       (aget "name"))))
        (is (= 0 (aget forced-options "temperature")))
        (is (= 0.2 (aget initial-options "temperature")))
        (is (nil? (aget initial-options "toolChoice")))
        (is (identical? original-on-payload (aget initial-options "onPayload"))))
      (testing "the composed payload hook preserves the original hook and forces the seed"
        (is (identical? request-payload seeded-payload))
        (is (= true (aget seeded-payload "originalHook")))
        (is (= 0 (aget seeded-payload "seed")))
        (is (= [{:payload request-payload
                 :model model}]
               @payload-calls*)))
      (testing "after toolResult eta-mu receives its original options unchanged"
        (is (identical? post-tool-options unforced-options))
        (is (nil? (aget unforced-options "toolChoice")))))))

(deftest required-first-uses-the-provider-generic-choice-for-multiple-tools
  (let [seen* (atom nil)
        original-options #js {:temperature 0.1}
        raw-agent
        #js {:streamFn
             (fn [_model _context options]
               (reset! seen* options)
               :stream-result)}
        context #js {:messages #js [#js {:role "user"}]
                     :tools #js [#js {:name "first_tool"}
                                  #js {:name "second_tool"}]}]
    (eta-mu/configure-tools-choice! raw-agent :required-first)
    (is (= :stream-result
           ((aget raw-agent "streamFn") #js {:provider "ollama"} context original-options)))
    (is (= "required" (aget @seen* "toolChoice")))
    (is (= 0 (aget @seen* "temperature")))
    (is (not (identical? original-options @seen*)))
    (is (nil? (aget original-options "toolChoice")))
    (is (= 0.1 (aget original-options "temperature")))))

(deftest required-first-does-not-alter-non-ollama-options
  (let [seen* (atom nil)
        original-on-payload (fn [payload _model] payload)
        original-options #js {:temperature 0.7
                              :onPayload original-on-payload}
        raw-agent
        #js {:streamFn
             (fn [_model _context options]
               (reset! seen* options)
               :stream-result)}
        context #js {:messages #js [#js {:role "user"}]
                     :tools #js [#js {:name "save_translation"}]}
        model #js {:provider "openai"
                   :id "gpt-5.5"}]
    (eta-mu/configure-tools-choice! raw-agent :required-first)
    (is (= :stream-result
           ((aget raw-agent "streamFn") model context original-options)))
    (is (identical? original-options @seen*))
    (is (= 0.7 (aget @seen* "temperature")))
    (is (identical? original-on-payload (aget @seen* "onPayload")))
    (is (nil? (aget @seen* "toolChoice")))))
