(ns knoxx.backend.token-event-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.shape.agent.runtime :as runtime]))

(def ^:private packet
  {:run_id "run" :conversation_id "conv" :session_id "session"
   :kind "assistant_message" :token "🌱 prior. Corrected"})

(deftest replacement-metadata-must-be-explicit-and-bounded
  (testing "legacy delta and valid empty/Unicode replacements retain their protocol"
    (is (runtime/valid? runtime/TokenEvent packet))
    (is (runtime/valid? runtime/TokenEvent (assoc packet :operation "replace" :offset (count "🌱 prior. "))))
    (is (runtime/valid? runtime/TokenEvent (assoc packet :operation "replace" :offset 0 :token "")))
    (is (runtime/valid? runtime/TokenEvent (assoc packet :operation "replace" :offset (count (:token packet))))))
  (testing "malformed metadata cannot acquire ordinary append semantics"
    (doseq [changes [{:offset 0} {:operation nil} {:operation "append"}
                     {:operation "replace"} {:operation "replace" :offset nil}
                     {:operation "replace" :offset "0"} {:operation "replace" :offset -1}
                     {:operation "replace" :offset 0.5} {:operation "replace" :offset 9007199254740992}
                     {:operation "replace" :offset 99} {:operation "replace" :offset 0 :token nil}
                     {:operation "replace" :offset 0 :kind "tool_call"}]]
      (is (not (runtime/valid? runtime/TokenEvent (merge packet changes))) (pr-str changes)))))
