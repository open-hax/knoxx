(ns knoxx.backend.domain.agent.reasoning
  "Pure helpers for extracting/routing provider reasoning text."
  (:require [clojure.string :as str]))

(def ^:private think-open "<think>")
(def ^:private think-close "</think>")

(defn split-think-tags
  "Extract a leading <think>...</think> block from assistant text."
  [text]
  (let [text (str (or text ""))
        open-idx (.indexOf text think-open)
        close-idx (.indexOf text think-close)]
    (if (and (>= open-idx 0)
             (>= close-idx 0)
             (< open-idx 64)
             (> close-idx open-idx))
      (let [thinking (subs text (+ open-idx (count think-open)) close-idx)
            after (subs text (+ close-idx (count think-close)))
            before (subs text 0 open-idx)
            answer (str (or before "") (or after ""))]
        {:reasoning (str/trim thinking)
         :answer (str/trim answer)
         :hadThinkTags true})
      {:reasoning ""
       :answer text
       :hadThinkTags false})))

(defn route-think-delta
  "Route one provider text delta through the <think> state machine.

   Returns {:mode <next-mode> :emissions [{:kind :agent_message|:reasoning
                                           :delta string}]}."
  [{:keys [mode last-assistant-text delta]}]
  (let [mode (or mode :off)
        delta (str (or delta ""))
        last-assistant-text (str (or last-assistant-text ""))]
    (cond
      (str/blank? delta)
      {:mode mode :emissions []}

      (= mode :off)
      (let [idx (.indexOf delta think-open)]
        (if (and (>= idx 0)
                 (str/blank? last-assistant-text)
                 (< idx 64))
          (let [before (subs delta 0 idx)
                after (subs delta (+ idx (count think-open)))
                routed-after (route-think-delta {:mode :thinking
                                                 :last-assistant-text last-assistant-text
                                                 :delta after})
                before-emissions (cond-> []
                                   (seq (str/trim before))
                                   (conj {:kind :agent_message :delta before}))]
            {:mode (:mode routed-after)
             :emissions (into before-emissions (:emissions routed-after))})
          {:mode mode
           :emissions [{:kind :agent_message :delta delta}]}))

      (= mode :thinking)
      (let [idx (.indexOf delta think-close)]
        (if (>= idx 0)
          (let [thinking (subs delta 0 idx)
                after (subs delta (+ idx (count think-close)))
                emissions (cond-> []
                            (seq thinking) (conj {:kind :reasoning :delta thinking})
                            (seq after) (conj {:kind :agent_message :delta after}))]
            {:mode :done
             :emissions emissions})
          {:mode mode
           :emissions [{:kind :reasoning :delta delta}]}))

      :else
      {:mode mode
       :emissions [{:kind :agent_message :delta delta}]})))
