(ns knoxx.backend.law.writing-model
  "Contracts for writing proposals and completed model transcripts."
  (:require [clojure.string :as str]))

(defn failure
  "Create a classified provider failure without exposing private prompt contents."
  [code message]
  (ex-info message {:status 502 :code code}))

(defn require-input!
  "Require bounded plain writing context before any provider receives it."
  [input]
  (when-not (and (map? input)
                 (string? (:instruction input))
                 (<= 1 (count (:instruction input)) 10000)
                 (string? (:content input))
                 (<= (count (:content input)) 200000)
                 (or (nil? (:lessons input)) (sequential? (:lessons input)))
                 (<= (count (pr-str (:lessons input))) 40000))
    (throw (ex-info "Writing request is not valid bounded text"
                    {:status 400 :code "writing_model_invalid_input"})))
  input)

(defn require-content!
  "A successful transport must also return a bounded, nonempty writing proposal."
  [content]
  (when-not (and (string? content) (not (str/blank? content))
                 (<= (count content) 250000))
    (throw (failure "writing_model_invalid_output" "Writing model returned invalid content")))
  content)

(defn transcript-result
  "Accept one completed tool-free OpenCode turn, never an exit code alone."
  [events model]
  (when-not (and (seq events) (every? map? events)
                 (every? #{"step_start" "text" "reasoning" "step_finish"} (map :type events)))
    (throw (failure "writing_model_invalid_output" "Writing model returned an error or tool event")))
  (let [finishes (filter #(= "step_finish" (:type %)) events)
        sessions (set (map :sessionID events))
        texts (filter #(= "text" (:type %)) events)
        finish (first finishes)]
    (when-not (and (= 1 (count sessions)) (string? (first sessions))
                   (not (str/blank? (first sessions)))
                   (= 1 (count finishes)) (= "stop" (get-in finish [:part :reason]))
                   (= "step_finish" (:type (last events)))
                   (seq texts) (every? #(string? (get-in % [:part :text])) texts))
      (throw (failure "writing_model_incomplete" "Writing model did not finish one complete response")))
    {:content (require-content! (str/join "\n" (map #(get-in % [:part :text]) texts)))
     :model model :provider :opencode
     :usage {:tokens (get-in finish [:part :tokens]) :cost (get-in finish [:part :cost])}}))
