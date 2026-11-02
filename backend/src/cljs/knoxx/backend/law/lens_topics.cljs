(ns knoxx.backend.law.lens-topics
  "Malli parameter contract for KnoxxLens topic refinement (`lens_refine_topics`).
   Pure schema declaration only — no I/O."
  (:require [malli.core :as m]))

(def topic-item-schema
  [:map
   [:term {:description "Crisp noun, noun phrase, or proper noun (1-4 words, e.g. 'OpenPlanner', 'WebSocket relay', 'BetterDiscord'). Never verbs, prepositions, or adjectives."} :string]
   [:kind {:optional true :description "Part-of-speech class: 'proper_noun' for named entities/products/people/places, or 'noun' for concrete concepts."} [:enum "noun" "proper_noun"]]
   [:category {:optional true :description "Optional semantic category such as 'project', 'person', 'org', 'location', 'protocol', or 'concept'."} :string]
   [:salience {:optional true :description "Relevance score in 0.0..1.0 (highest = primary topic of discussion)."} [:double {:min 0.0 :max 1.0}]]])

(def refine-topics-params
  [:map
   [:topics {:description "Ranked list of 1 to 8 refined noun and proper-noun topics actively discussed in the Discord channel."}
    [:vector {:min 1 :max 12} topic-item-schema]]
   [:droppedTerms {:optional true :description "Noisy non-topic words removed from heuristic candidates (e.g. 'found', 'using', 'via', 'work', 'fascinating')."}
    [:vector :string]]
   [:channelId {:optional true :description "Discord channel snowflake id from templateContext when available."} :string]
   [:channelName {:optional true :description "Discord channel name when available."} :string]])

(defn valid-params?
  [params]
  (m/validate refine-topics-params params))
