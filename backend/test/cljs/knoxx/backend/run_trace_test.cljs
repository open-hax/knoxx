(ns knoxx.backend.run-trace-test
  (:require [cljs.test :refer [deftest is testing]]
            [knoxx.backend.domain.action.run-trace :as trace]))

(defn- text-block [id kind content]
  {:id id :kind kind :content content :status "streaming" :at "before"})

(deftest corrections-preserve-prefix-tools-and-other-channel
  (testing "a coalesced message prefix uses UTF-16 offsets and keeps interleaved history"
    (doseq [kind [:agent_message :reasoning]]
      (let [prefix "Earlier 😀.\n\n"
            first-block (text-block "first" kind (str prefix "Draft"))
            tool {:id "tool:lookup" :kind :tool_call :status "done" :outputPreview "Keep me"}
            other (text-block "other" (if (= kind :agent_message) :reasoning :agent_message) "Unchanged")
            blocks [first-block tool other (text-block "suffix" kind " stale suffix")]
            corrected (trace/replace-text-tail blocks kind (count prefix) (str prefix "Knoxx\n\nReviewed.") "after")]
        (is (= [(assoc first-block :content (str prefix "Knoxx\n\nReviewed.") :at "after") tool other]
               corrected))
        (is (= corrected (trace/replace-text-tail corrected kind (count prefix) (str prefix "Knoxx\n\nReviewed.") "after")))
        (is (= [(assoc first-block :content prefix :at "after") tool other]
               (trace/replace-text-tail corrected kind (count prefix) prefix "later")))))))

(deftest corrections-at-provider-message-boundaries
  (testing "earlier blocks remain byte-for-byte while current message blocks are replaced"
    (let [previous (text-block "previous" :agent_message "Earlier.")
          tool {:id "tool:lookup" :kind :tool_call :status "done"}
          current (text-block "current" :agent_message "Draft")
          blocks [previous tool current (text-block "tail" :agent_message " suffix")]]
      (is (= [previous tool (assoc current :content "Correct" :at "after")]
             (trace/replace-text-tail blocks :agent_message 8 "Earlier.Correct" "after")))
      (is (= [previous tool] (trace/replace-text-tail blocks :agent_message 8 "Earlier." "after")))
      (is (= [previous tool {:id "agent_message:2" :kind :agent_message :status "streaming"
                            :content "Restored" :at "restored"}]
             (trace/replace-text-tail [previous tool] :agent_message 8 "Earlier.Restored" "restored"))))))

(deftest first-message-correction-can-remove-all-text
  (let [tool {:id "tool:lookup" :kind :tool_call :status "done"}
        blocks [(text-block "draft" :agent_message "Wrong") tool
                (text-block "reasoning" :reasoning "Retain reasoning")]]
    (is (= (subvec (vec blocks) 1)
           (trace/replace-text-tail blocks :agent_message 0 "" "after")))
    (is (= [{:id "reasoning:0" :kind :reasoning :status "streaming" :content "New" :at "after"}]
           (trace/replace-text-tail [] :reasoning 0 "New" "after")))))

(deftest deleting-another-channel-cannot-reuse-a-surviving-text-id
  (let [blocks [(text-block "reasoning:0" :reasoning "R1")
                (text-block "agent_message:1" :agent_message "A1")
                (text-block "reasoning:2" :reasoning "R2")
                (text-block "agent_message:3" :agent_message "A2")
                {:id "tool:t" :kind :tool_call :status "done"}]
        cleared (trace/replace-text-tail blocks :reasoning 0 "" "clear")
        appended (trace/replace-text-tail cleared :agent_message 4 "A1A2Next" "after")]
    (is (= ["agent_message:1" "agent_message:3" "tool:t" "agent_message:4"] (mapv :id appended)))
    (is (= "A1A2Next" (apply str (map :content (filter #(= :agent_message (:kind %)) appended)))))
    (is (= (count appended) (count (set (map :id appended)))))
    (is (= (subvec (vec blocks) 4) (filterv #(= :tool_call (:kind %)) appended)))))

(deftest full-snapshots-converge-missing-partial-or-stale-trace-prefixes
  (doseq [kind [:agent_message :reasoning]
          retained [nil "Earlier " "Stale prefix with enough characters" "Earlier 😀.\n\n"]]
    (let [prefix "Earlier 😀.\n\n"
          snapshot (str prefix "Corrected\n\nhaha")
          tool {:id "tool:t" :kind :tool_call :status "done"}
          other (text-block "other" (if (= kind :agent_message) :reasoning :agent_message) "Keep other")
          blocks (cond-> [tool other] retained (conj (text-block "retained" kind retained)))
          projected (trace/replace-text-tail blocks kind (count prefix) snapshot "after")]
      (is (= snapshot (apply str (map :content (filter #(= kind (:kind %)) projected)))))
      (is (= [tool other] (filterv #(not= kind (:kind %)) projected)))
      (is (= (count projected) (count (set (map :id projected))))))))
